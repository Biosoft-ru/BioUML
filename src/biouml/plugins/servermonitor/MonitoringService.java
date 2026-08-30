package biouml.plugins.servermonitor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import ru.biosoft.tasks.TaskInfo;
import ru.biosoft.tasks.TaskManager;

/**
 * Core monitoring service that runs as a background daemon thread.
 * Periodically checks for slow tasks and triggers JVM-wide profiling.
 */
public class MonitoringService {

    private static final Logger log = Logger.getLogger(MonitoringService.class.getName());

    private final ServerMonitorConfig config;
    private final AsyncProfilerWrapper profiler;
    private volatile boolean running = false;
    private volatile Thread monitorThread = null;
    private volatile long lastCheckTime = 0;
    private volatile long lastPeriodicTime = 0;
    private volatile int slowTaskCount = 0;
    private volatile List<String> slowTaskIds = new ArrayList<>();
    private final Map<String, ProfilerResult> activeProfiles = new ConcurrentHashMap<>();
    /**
     * Guards the entire profiler lifecycle (the blocking {@code profiler.start}
     * call). Multiple callers hit profiling concurrently — the manual
     * {@code profileNow} servlet action and the periodic / slow-task monitor
     * timer — and without this lock one run's {@code asprof stop} / lingering
     * kill can SIGKILL another run's CLI mid-teardown, leaving the in-JVM
     * agent stuck "already started" for every subsequent call. Serializing
     * makes that race impossible.
     */
    private final Object profilerLock = new Object();
    private final Random random = new Random();
    private final SubProcessMonitor subProcessMonitor;
    /**
     * Most recent sub-process scan result, for the profile API. Updated on each
     * monitor-loop cycle.
     */
    private volatile List<SubProcessMonitor.SubProcess> lastSubProcesses = new ArrayList<>();
    private volatile long lastSubProcessCheckTime = 0;
    private volatile boolean firstLoopIteration = true;

    /**
     * File name (within the profiler directory) used for the persistent
     * sub-process observation log. Each non-empty scan appends one JSON line,
     * so the full timeline of long-running external processes is retained even
     * after they exit — the {@code status} API only shows the live snapshot.
     */
    public static final String SUB_PROCESS_LOG_FILE = "subprocesses.jsonl";

    /** Maximum number of JSON lines kept in the sub-process log. */
    private static final int MAX_SUB_PROCESS_LOG_LINES = 5000;
    /** Maximum age (ms) of a sub-process log line before it is purged. */
    private static final long MAX_SUB_PROCESS_LOG_AGE = 7L * 24 * 60 * 60 * 1000; // 7 days
    /**
     * Purge the log at most once every this long (ms) unless the line cap is
     * exceeded. The age check needs a full scan, so it must not run on every
     * monitor cycle — only periodically, or when the log is already over the cap.
     */
    private static final long SUB_PROCESS_LOG_PURGE_INTERVAL = 10L * 60L * 1000L; // 10 min

    // In-memory record count for the sub-process log, so the status endpoint
    // does not need to scan the whole file. Lazily initialized from the
    // existing file on first use; thereafter maintained incrementally by the
    // append path. Approximate after an external edit; recomputed exactly on
    // every purge/compact.
    private int subProcessLogCount = -1;
    /** Last time the sub-process log was fully scanned for purging. */
    private long lastSubProcessLogPurgeTime = 0;

    // Guards append/rewrite of the sub-process log (single monitor thread writes,
    // but the API can read concurrently; keep the file in a consistent state).
    private final Object subProcessLogLock = new Object();

    public MonitoringService(ServerMonitorConfig config) {
        this.config = config;
        this.profiler = new AsyncProfilerWrapper(config);
        this.subProcessMonitor = new SubProcessMonitor(config);
    }

    /**
     * Start the monitoring service.
     * Creates a daemon thread and begins the monitoring loop.
     */
    public void start() {
        if (running) return;
        running = true;

        // Ensure profiler directory exists (falls back to /tmp/profiling if needed)
        config.ensureProfilerDir();

        // Initialize profiler
        profiler.init();

        monitorThread = new Thread(this::monitorLoop, "ServerMonitor");
        monitorThread.setDaemon(true);
        monitorThread.start();

        log.info("Monitoring service started (threshold=" + config.getSlowTaskThreshold() +
                "s, interval=" + config.getCheckInterval() + "s)");
    }

    /**
     * Stop the monitoring service.
     * Interrupts the monitor thread and stops any active profiling.
     */
    public void stop() {
        running = false;

        if (monitorThread != null) {
            monitorThread.interrupt();
            try {
                monitorThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Stop any active profiling
        for (String profileId : activeProfiles.keySet()) {
            try {
                profiler.stop();
            } catch (Exception e) {
                log.log(Level.WARNING, "Error stopping profiler for profile: " + profileId, e);
            }
        }
        activeProfiles.clear();

        log.info("Monitoring service stopped");
    }

    /**
     * Main monitoring loop.
     * Runs periodically, checking for slow tasks and triggering profiling.
     */
    private void monitorLoop() {
        while (running) {
            try {
                checkSlowTasks();
                checkPeriodicProfiling();
                checkSubProcesses();
                cleanupOldProfiles();

                lastCheckTime = System.currentTimeMillis();

                // After the first loop iteration, clear the startup guard so
                // periodic profiling respects the configured interval from
                // the next cycle onward.
                firstLoopIteration = false;

                long interval = config.getCheckInterval() * 1000L;
                Thread.sleep(Math.max(interval, 1000));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.log(Level.SEVERE, "Monitoring service error", e);
            }
        }
    }

    /**
     * Check for tasks exceeding the slow threshold.
     * Triggers JVM-wide profiling when a slow task is detected.
     */
    private void checkSlowTasks() {
        long thresholdMillis = config.getSlowTaskThreshold() * 1000L;
        long now = System.currentTimeMillis();

        List<TaskInfo> runningTasks = TaskManager.getInstance().getAllRunningTasks();
        if (runningTasks.isEmpty()) return;

        List<String> currentSlow = new ArrayList<>();

        for (TaskInfo ti : runningTasks) {
            long elapsed = now - ti.getStartTime();
            if (elapsed > thresholdMillis) {
                currentSlow.add(ti.getName());

                // Trigger JVM-wide profiling if not already profiling
                if (!activeProfiles.containsKey("jvm")) {
                    profileJvm(ti.getName());
                }
            }
        }

        slowTaskCount = currentSlow.size();
        slowTaskIds = currentSlow;
    }

    /**
     * Scan for long-running external sub-processes (perl/R/nextflow/...) and
     * record the result for the profile API. Logs a SEVERE line the first time
     * each sub-process crosses the slow threshold.
     */
    private void checkSubProcesses() {
        if (!config.isSubProcessEnabled()) {
            return;
        }
        try {
            List<SubProcessMonitor.SubProcess> subs = subProcessMonitor.check();
            lastSubProcesses = subs;
            lastSubProcessCheckTime = System.currentTimeMillis();

            // Persist every non-empty scan so the timeline of long-running
            // external processes survives process exit and is retrievable via
            // the API (the live status only shows the current snapshot).
            if (!subs.isEmpty()) {
                appendSubProcessLog(subs, lastSubProcessCheckTime);
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "Sub-process scan failed", e);
        }
    }

    /**
     * Append one JSON line describing a non-empty sub-process scan to the
     * persistent log. Each line is a self-contained record:
     * <pre>{"timestamp":..., "checkIntervalSec":..., "subProcesses":[{"pid":..,"ageSeconds":..,"slow":..,"command":..}, ...]}</pre>
     *
     * <p>Writing is serialized against {@link #subProcessLogLock}. The append is
     * the only per-scan I/O (the hot path never rewrites the file); a full
     * rewrite/compact runs only when the line cap is exceeded or at most every
     * {@link #SUB_PROCESS_LOG_PURGE_INTERVAL}, so the age bound is enforced
     * without scanning on every cycle.
     */
    private void appendSubProcessLog(List<SubProcessMonitor.SubProcess> subs, long timestamp) {
        File logFile = getSubProcessLogFile();
        if (logFile == null) {
            return;
        }
        synchronized (subProcessLogLock) {
            try {
                // Append first — the hot path must never scan the whole file.
                String line = buildSubProcessRecord(subs, timestamp);
                try (BufferedWriter writer = new BufferedWriter(
                        new java.io.OutputStreamWriter(new FileOutputStream(logFile, true), StandardCharsets.UTF_8))) {
                    writer.write(line);
                    writer.newLine();
                }

                long now = System.currentTimeMillis();
                if (subProcessLogCount < 0) {
                    subProcessLogCount = countSubProcessLogLines(logFile);
                }
                subProcessLogCount++;

                // Purge only when the line cap is exceeded or the age scan is
                // due. The age check is a full scan, so it must not run on
                // every monitor cycle.
                boolean overCap = subProcessLogCount > MAX_SUB_PROCESS_LOG_LINES;
                boolean ageDue = now - lastSubProcessLogPurgeTime >= SUB_PROCESS_LOG_PURGE_INTERVAL;
                if (overCap || ageDue) {
                    purgeAndCompactSubProcessLog(logFile, now);
                }
            } catch (Exception e) {
                log.log(Level.WARNING, "Error appending sub-process log " + logFile.getAbsolutePath(), e);
            }
        }
    }

    /**
     * Full scan of the sub-process log: drop lines older than
     * {@link #MAX_SUB_PROCESS_LOG_AGE} and malformed lines (no parseable
     * timestamp, which can't be bounded by age), keep the most recent
     * {@link #MAX_SUB_PROCESS_LOG_LINES}, and atomically replace the file.
     * Updates the in-memory line count.
     */
    private void purgeAndCompactSubProcessLog(File logFile, long now) throws IOException {
        lastSubProcessLogPurgeTime = now;
        if (!logFile.exists() || logFile.length() == 0) {
            subProcessLogCount = 0;
            return;
        }

        // Pass 1: read the log, dropping lines older than MAX_SUB_PROCESS_LOG_AGE.
        List<String> kept = new ArrayList<>();
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(new FileInputStream(logFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                long ts = extractTimestamp(line);
                // A line with no parseable timestamp is malformed: it can never
                // be dropped by age, so it would grow the log forever. Drop it.
                if (ts <= 0) continue;
                boolean tooOld = (now - ts) > MAX_SUB_PROCESS_LOG_AGE;
                if (!tooOld) {
                    kept.add(line);
                }
            }
        }

        // Keep only the most recent N lines.
        int excess = kept.size() - MAX_SUB_PROCESS_LOG_LINES;
        if (excess > 0) {
            kept.subList(0, excess).clear();
        }
        int newCount = kept.size();

        // Pass 2: rewrite the pruned content to a temp file, then replace the
        // original from it. The temp file is created in the same directory, so
        // the move is a rename on the same filesystem: ATOMIC_MOVE is used when
        // the platform supports it (the normal case on Linux ext4/xfs), and we
        // fall back to a NON-ATOMIC replace otherwise — a reader could then
        // briefly observe either the old or the new content, but never a
        // truncated file. The in-memory count is published only after the
        // replacement succeeds; if it fails the file is unchanged, so the count
        // is left as-is (it still reflects the on-disk lines) and the next
        // append re-compacts.
        File tmp = File.createTempFile("subproc", ".jsonl", logFile.getParentFile());
        try {
            try (BufferedWriter tw = new BufferedWriter(
                    new java.io.OutputStreamWriter(new FileOutputStream(tmp), StandardCharsets.UTF_8))) {
                for (String l : kept) {
                    tw.write(l);
                    tw.newLine();
                }
            }
            try {
                Files.move(tmp.toPath(), logFile.toPath(),
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                // Fallback is a plain (non-atomic) replace.
                Files.move(tmp.toPath(), logFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            subProcessLogCount = newCount;
        } finally {
            // Best-effort delete if the move did not consume the temp file.
            if (tmp.exists()) {
                tmp.delete();
            }
        }
    }

    /**
     * Count the non-empty lines in the sub-process log (full scan). Used once
     * at startup to seed the in-memory count; steady-state appends maintain it
     * incrementally so the status endpoint never scans the file.
     */
    private int countSubProcessLogLines(File logFile) throws IOException {
        int n = 0;
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(new FileInputStream(logFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) n++;
            }
        }
        return n;
    }

    /**
     * Extract the {@code "timestamp":<long>} field from a log line (no full JSON
     * parse — the field is written first and is always an integer).
     */
    private long extractTimestamp(String line) {
        int idx = line.indexOf("\"timestamp\":");
        if (idx < 0) return -1;
        int start = idx + "\"timestamp\":".length();
        int end = start;
        while (end < line.length() && (Character.isDigit(line.charAt(end)) || line.charAt(end) == '-')) {
            end++;
        }
        try {
            return Long.parseLong(line.substring(start, end));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Build the single JSON line for one sub-process scan.
     */
    private String buildSubProcessRecord(List<SubProcessMonitor.SubProcess> subs, long timestamp) {
        JSONObject record = new JSONObject();
        // Version the record schema so future readers can detect layout changes.
        record.put("version", 1);
        record.put("timestamp", timestamp);
        record.put("checkIntervalSec", config.getCheckInterval());
        record.put("thresholdSec", config.getSubProcessThreshold());
        record.put("minAgeSec", config.getSubProcessMinAge());
        record.put("count", subs.size());
        JSONArray arr = new JSONArray();
        for (SubProcessMonitor.SubProcess sp : subs) {
            JSONObject o = new JSONObject();
            o.put("pid", sp.pid);
            o.put("ageSeconds", sp.ageSeconds);
            o.put("slow", sp.slow);
            // The log is persisted (up to 7 days) and readable through the API,
            // so command lines are redacted: an external script can carry
            // credentials on its command line (--password=..., --token ...,
            // --api-key ...), which must not be written to disk or exposed by
            // the endpoint.
            o.put("command", redactCommand(sp.command));
            arr.put(o);
        }
        record.put("subProcesses", arr);
        return record.toString();
    }

    /**
     * Redact secret-looking arguments from a command line before it is
     * persisted to the sub-process log. Handles both argument forms:
     * {@code --password=secret} and the separate-token {@code --password secret}.
     * The matched value (and the quoted value in {@code --password="secret"} /
     * {@code --password "secret"}) is replaced with {@code ***} while the key is
     * kept, so the record stays useful for analysis.
     *
     * <p>The line is tokenized quote-aware (so a quoted value with spaces stays
     * one token); when no token is changed the original string is returned
     * verbatim. When a redaction happens the tokens are re-joined with single
     * spaces, which may collapse runs of whitespace in the input — acceptable
     * for a log, and only on lines that actually carried a secret.
     */
    private static String redactCommand(String command) {
        if (command == null || command.isEmpty()) {
            return command;
        }
        List<String> toks = tokenizeCommandLine(command);
        boolean changed = false;
        for (int i = 0; i < toks.size(); i++) {
            String t = toks.get(i);
            int eq = t.indexOf('=');
            if (eq > 0) {
                // key=value form: "--password=hunter2" -> "--password=***".
                String key = t.substring(0, eq);
                String value = t.substring(eq + 1);
                if (looksLikeSecretKey(bareKey(key)) && !isBooleanFlagValue(value)) {
                    toks.set(i, key + "=***");
                    changed = true;
                }
            } else if (looksLikeSecretKey(bareKey(t))) {
                // Separate-token form: "--password hunter2" -> "--password ***".
                // The value is the next token. Skip the two cases where the key
                // token is not a credential: (a) it carries an "=" value (handled
                // above), or (b) it is the final token with nothing after it (a
                // bare flag like "--use-token", not a secret). Otherwise redact
                // this token and the following one.
                if (i + 1 < toks.size()) {
                    toks.set(i, t + " ***");
                    toks.remove(i + 1);
                    i++;
                    changed = true;
                }
            }
        }
        return changed ? String.join(" ", toks) : command;
    }

    /**
     * True if the value looks like a boolean flag rather than a credential:
     * a bare digit, "true", "false", "yes", "no", "on", or "off". Such values
     * are not secrets even when the key name happens to contain a credential
     * stem (e.g. {@code --use-token=1}).
     */
    private static boolean isBooleanFlagValue(String value) {
        if (value.isEmpty()) return true;
        String v = value.toLowerCase();
        if (v.equals("true") || v.equals("false") || v.equals("yes")
                || v.equals("no") || v.equals("on") || v.equals("off")) {
            return true;
        }
        // A bare integer (0, 1, 2, …) is a flag/option, not a credential.
        for (int i = 0; i < v.length(); i++) {
            if (!Character.isDigit(v.charAt(i))) return false;
        }
        return true;
    }

    /** Strip a leading dash run ("--password" / "-password" -> "password"). */
    private static String bareKey(String token) {
        String s = token;
        while (s.startsWith("-")) {
            s = s.substring(1);
        }
        return s;
    }

    /**
     * Tokenize a command line respecting single and double quotes so that a
     * quoted value with spaces stays one token. Quotes are removed from the
     * result (matching how the OS passes a single argv element). This is a
     * deliberate approximation — not a full shell parser — and is only used to
     * decide which tokens are secrets, not to re-run the command.
     */
    private static List<String> tokenizeCommandLine(String command) {
        List<String> toks = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inToken = false;
        int i = 0;
        int n = command.length();
        while (i < n) {
            char c = command.charAt(i);
            if (Character.isWhitespace(c)) {
                if (inToken) {
                    toks.add(cur.toString());
                    cur.setLength(0);
                    inToken = false;
                }
                i++;
            } else if (c == '"' || c == '\'') {
                char quote = c;
                inToken = true;
                i++;
                while (i < n && command.charAt(i) != quote) {
                    cur.append(command.charAt(i));
                    i++;
                }
                i++; // skip closing quote (or run past the end if unbalanced)
            } else {
                cur.append(c);
                inToken = true;
                i++;
            }
        }
        if (inToken) {
            toks.add(cur.toString());
        }
        return toks;
    }

    /** True if a command-line argument key looks like a credential. */
    private static boolean looksLikeSecretKey(String key) {
        String k = key.toLowerCase();
        if (k.isEmpty()) {
            return false;
        }
        // Normalize hyphens so "--api-key" matches the "api_key" stem.
        k = k.replace("-", "_");
        // Named credential stems only — a long argument name is not evidence of
        // a secret (the value is what would be long, and we redact the whole
        // value, so no length heuristic is needed).
        if (k.contains("password") || k.contains("passwd") || k.contains("pwd")
                || k.contains("token") || k.contains("secret") || k.contains("apikey")
                || k.contains("api_key") || k.contains("accesskey") || k.contains("access_key")
                || k.contains("privatekey") || k.contains("private_key")
                || k.contains("credential") || k.contains("auth") || k.contains("bearer")
                || k.contains("clientsecret") || k.contains("client_secret")
                || k.contains("sessiontoken") || k.contains("session_token")
                || k.contains("refreshtoken") || k.contains("refresh_token")) {
            return true;
        }
        return false;
    }

    /**
     * Resolve the sub-process log file inside the profiler directory.
     * Returns null if the directory is not resolvable.
     */
    private File getSubProcessLogFile() {
        try {
            config.ensureProfilerDir();
            return new File(config.getProfilerDir(), SUB_PROCESS_LOG_FILE);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Read sub-process log lines whose timestamp falls within [since, until]
     * (inclusive; either bound may be 0 to mean unbounded). Returns raw JSON
     * lines (one per scan), most recent last.
     */
    public List<String> readSubProcessLog(long since, long until) {
        List<String> result = new ArrayList<>();
        File logFile = getSubProcessLogFile();
        if (logFile == null || !logFile.exists() || logFile.length() == 0) {
            return result;
        }
        synchronized (subProcessLogLock) {
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(new FileInputStream(logFile), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    long ts = extractTimestamp(line);
                    if (ts < 0) continue;
                    if (since > 0 && ts < since) continue;
                    if (until > 0 && ts > until) continue;
                    result.add(line);
                }
            } catch (Exception e) {
                log.log(Level.WARNING, "Error reading sub-process log " + logFile.getAbsolutePath(), e);
            }
        }
        return result;
    }

    /**
     * Total number of records in the sub-process log (0 if none). Served from
     * the in-memory count maintained by the append path — this does NOT scan
     * the file. If the count has not been seeded yet (no append since startup
     * and the file pre-dates this service instance) it falls back to a single
     * scan, then caches the result for subsequent calls. A transient read
     * failure returns 0 without caching so the next call retries.
     */
    public int getSubProcessLogCount() {
        synchronized (subProcessLogLock) {
            if (subProcessLogCount >= 0) {
                return subProcessLogCount;
            }
            File logFile = getSubProcessLogFile();
            if (logFile == null || !logFile.exists() || logFile.length() == 0) {
                subProcessLogCount = 0;
                return 0;
            }
            try {
                subProcessLogCount = countSubProcessLogLines(logFile);
            } catch (Exception e) {
                log.log(Level.WARNING, "Failed to count sub-process log lines " + logFile.getAbsolutePath(), e);
                // Do NOT cache the failure — leave the count at -1 so the
                // next call retries (the file may be temporarily locked).
                return 0;
            }
            return subProcessLogCount;
        }
    }

    /**
     * Absolute path of the sub-process log file (for API / diagnostics).
     */
    public String getSubProcessLogPath() {
        File f = getSubProcessLogFile();
        return f != null ? f.getAbsolutePath() : null;
    }

    /**
     * Force immediate profiling of the entire JVM.
     * @param taskId the task that triggered profiling, or null
     * @return ProfilerResult
     */
    public ProfilerResult profileNow(String taskId) {
        return profileJvm(taskId);
    }

    /**
     * Stop the current profiler session, serialized against any in-progress
     * profiling run via {@link #profilerLock}. Use this (not a fresh
     * {@link AsyncProfilerWrapper#stop()}) so a manual stop cannot race an
     * active run and SIGKILL its CLI mid-teardown.
     * @return true if the stop exited cleanly
     */
    public boolean stopProfiling() {
        synchronized (profilerLock) {
            return profiler.stop();
        }
    }

    /**
     * Force immediate profiling of all running tasks (profiles entire JVM).
     * @return list with a single ProfilerResult
     */
    public List<ProfilerResult> profileNowAll() {
        ProfilerResult result = profileJvm(null);
        List<ProfilerResult> results = new ArrayList<>();
        if (result != null) {
            results.add(result);
        }
        return results;
    }

    /**
     * Check for periodic profiling of the JVM.
     */
    private void checkPeriodicProfiling() {
        // Skip periodic profiling on the very first loop iteration (server startup).
        // During startup there are no user tasks, so profiling would only capture
        // the plugin-loading / OSGi bootstrap phase, which is not useful.
        if (firstLoopIteration) return;

        long periodicInterval = config.getPeriodicInterval() * 1000L;
        long now = System.currentTimeMillis();

        if (periodicInterval <= 0) return; // Disabled
        if (now - lastPeriodicTime < periodicInterval) return;

        lastPeriodicTime = now;

        List<TaskInfo> runningTasks = TaskManager.getInstance().getAllRunningTasks();

        if (runningTasks.isEmpty()) {
            // No running tasks - profile the entire JVM to capture idle CPU usage
            log.fine("No running tasks, profiling JVM");
            profileJvm(null);
            return;
        }

        // Profile the JVM if not already profiling
        if (!activeProfiles.containsKey("jvm")) {
            // Pick a representative task for metadata
            TaskInfo target;
            if ("random".equals(config.getPeriodicMode())) {
                target = runningTasks.get(random.nextInt(runningTasks.size()));
            } else {
                // Default: profile the longest-running task
                long nowMs = System.currentTimeMillis();
                long maxElapsed = 0;
                target = null;
                for (TaskInfo ti : runningTasks) {
                    long elapsed = nowMs - ti.getStartTime();
                    if (elapsed > maxElapsed) {
                        maxElapsed = elapsed;
                        target = ti;
                    }
                }
            }
            profileJvm(target != null ? target.getName() : null);
        }
    }

    /**
     * Profile the entire JVM process (all threads).
     * Retries up to 3 times if a zero-size profile file is detected.
     * @param triggeredTask the task name that triggered profiling, or null
     * @return ProfilerResult
     */
    private ProfilerResult profileJvm(String triggeredTask) {
        synchronized (profilerLock) {
            return profileJvmLocked(triggeredTask);
        }
    }

    private ProfilerResult profileJvmLocked(String triggeredTask) {
        // Check max concurrent profiles
        if (activeProfiles.size() >= 1) {
            log.warning("Max concurrent profiles reached, skipping JVM profiling");
            return new ProfilerResult("Max concurrent profiles reached");
        }

        ProfilerResult result = null;
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            result = profiler.start(new long[0], "collapsed");

            if (!result.isSuccess()) {
                log.warning("JVM profiling failed: " + result.getError() +
                        " (profiler available=" + profiler.isAvailable() + ")");
                // Do NOT add to activeProfiles — a failed attempt should not
                // block future profiling.  The slot stays free so the next
                // periodic check can retry.
                break;
            }

            // Check for zero-size profile files immediately after profiling
            if (!checkZeroSizeProfile(result, triggeredTask)) {
                // Success — non-zero profile file
                log.info("Started profiling JVM (all threads): " + result.getOutputPath());

                // Save metadata JSON
                saveProfileMetadata(triggeredTask, result);

                // Profiler.start() runs synchronously and blocks until the duration expires.
                // Once it returns the profiler process has exited, so remove "jvm" from
                // activeProfiles to allow the next periodic check to start a new profile.
                activeProfiles.remove("jvm");
                return result;
            }

            // Zero-size file detected — retry
            if (attempt < maxRetries) {
                log.info("Zero-size profile detected on attempt " + attempt + " (" + result.getOutputPath() +
                        "), retrying (" + (attempt + 1) + "/" + maxRetries + ")...");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } else {
                log.warning("JVM profiling produced zero-size file after " + maxRetries + " attempts: " + result.getOutputPath());
            }
        }

        return result;
    }

    /**
     * Clean up old profiles based on age and count limits.
     * Skips directories (e.g., the async-profiler installation) to avoid deleting the profiler binary.
     */
    private void cleanupOldProfiles() {
        long maxAgeMillis = config.getMaxProfileAge() * 1000L;
        long now = System.currentTimeMillis();
        File profileDir = new File(config.getProfilerDir());

        if (!profileDir.exists()) return;

        File[] files = profileDir.listFiles();
        if (files == null) return;

        // The sub-process observation log is self-bounding (purged by age/line
        // count in appendSubProcessLog) — never delete it here.
        String subProcLogName = SUB_PROCESS_LOG_FILE;

        // Age-based cleanup (skip directories like the async-profiler installation)
        for (File f : files) {
            if (f.isDirectory()) {
                log.fine("Skipping directory during cleanup: " + f.getName());
                continue;
            }
            if (subProcLogName.equals(f.getName())) {
                continue;
            }
            if (now - f.lastModified() > maxAgeMillis) {
                try {
                    f.delete();
                    log.info("Cleaned old profile: " + f.getName());
                } catch (Exception e) {
                    log.log(Level.WARNING, "Error deleting old profile: " + f.getName(), e);
                }
            }
        }

        // Count-based cleanup (skip directories to protect the profiler installation)
        files = profileDir.listFiles();
        if (files != null && files.length > config.getMaxProfiles()) {
            // Filter to only profile files (exclude directories like async-profiler-*)
            List<File> profileFiles = new ArrayList<>();
            for (File f : files) {
                if (!f.isDirectory() && !subProcLogName.equals(f.getName())) {
                    profileFiles.add(f);
                }
            }
            if (profileFiles.size() > config.getMaxProfiles()) {
                profileFiles.sort(Comparator.comparingLong(File::lastModified));
                int toDelete = profileFiles.size() - config.getMaxProfiles();
                for (int i = 0; i < toDelete; i++) {
                    File f = profileFiles.get(i);
                    try {
                        f.delete();
                        log.info("Cleaned excess profile: " + f.getName());
                    } catch (Exception e) {
                        log.log(Level.WARNING, "Error deleting excess profile: " + f.getName(), e);
                    }
                }
            }
        }
    }

    /**
     * Build a sanitized output path for a profile.
     */
    private String buildProfilePath(String taskId, String format) {
        File dir = new File(config.getProfilerDir());
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                File parent = dir.getParentFile();
                if (parent != null) {
                    log.log(Level.WARNING,
                            "MonitoringService: failed to create profiler directory " + dir.getAbsolutePath()
                                    + "; parent folder=" + parent.getAbsolutePath()
                                    + " exists=" + parent.exists()
                                    + " canWrite=" + parent.canWrite()
                                    + " — falling back to /tmp/profiling");
                } else {
                    log.log(Level.WARNING,
                            "MonitoringService: failed to create profiler directory " + dir.getAbsolutePath()
                                    + " (no parent folder) — falling back to /tmp/profiling");
                }
                config.ensureProfilerDir();
                dir = new File(config.getProfilerDir());
            }
        }

        // Sanitize task ID for filename
        String safeName = taskId != null ? taskId.replaceAll("[^a-zA-Z0-9._-]", "_") : "jvm";
        long timestamp = System.currentTimeMillis();
        return dir.getAbsolutePath() + "/" + safeName + "_" + timestamp + "." + format;
    }

    /**
     * Save profile metadata as a JSON sidecar file.
     */
    private void saveProfileMetadata(String triggeredTask, ProfilerResult result) {
        File metaFile = new File(result.getOutputPath().replace("." + getExtension(result.getOutputPath()), ".json"));

        try (FileWriter writer = new FileWriter(metaFile)) {
            writer.write("{\n");
            writer.write("  \"triggeredTask\": \"" + escapeJson(triggeredTask) + "\",\n");
            writer.write("  \"startTime\": " + result.getStartTime() + ",\n");
            writer.write("  \"endTime\": " + result.getEndTime() + ",\n");
            writer.write("  \"duration\": " + result.getDuration() + "\n");
            writer.write("}");
            log.fine("Saved profile metadata: " + metaFile.getAbsolutePath());
        } catch (IOException e) {
            log.log(Level.WARNING, "Error saving profile metadata", e);
        }
    }

    /**
     * Check for zero-size profile files and log debug info if found.
     * @return true if any zero-size file was detected
     */
    private boolean checkZeroSizeProfile(ProfilerResult result, String triggeredTask) {
        boolean foundZeroSize = false;

        // Check primary output file
        foundZeroSize |= checkSingleFile(result.getOutputPath(), triggeredTask, result.getDuration());

        // Check extra format files
        if (result.getExtraPaths() != null) {
            for (String extraPath : result.getExtraPaths()) {
                foundZeroSize |= checkSingleFile(extraPath, triggeredTask, result.getDuration());
            }
        }

        return foundZeroSize;
    }

    /**
     * Check a single profile file for zero size and log debug info.
     * @return true if the file is zero-size
     */
    private boolean checkSingleFile(String filePath, String triggeredTask, long duration) {
        File profileFile = new File(filePath);
        if (profileFile.exists() && profileFile.length() == 0) {
            File metaFile = new File(filePath.replace("." + getExtension(filePath), ".json"));
            String metaInfo = "no metadata";
            if (metaFile.exists()) {
                try {
                    readFileMetadata(metaFile);
                    metaInfo = "exists";
                } catch (Exception e) {
                    metaInfo = "malformed: " + e.getMessage();
                }
            }
            log.log(Level.WARNING,
                    "profileJvm: zero-size profile file detected — path=" + filePath
                            + " size=" + profileFile.length()
                            + " lastModified=" + profileFile.lastModified()
                            + " exists=" + profileFile.exists()
                            + " canRead=" + profileFile.canRead()
                            + " metadata=" + metaInfo
                            + " triggeredBy=" + triggeredTask
                            + " duration=" + duration + "ms");
            return true;
        }
        return false;
    }

    private String getExtension(String path) {
        int dot = path.lastIndexOf('.');
        return dot > 0 ? path.substring(dot + 1) : "";
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    /**
     * Read raw file content as a string (for metadata inspection).
     */
    private String readFileMetadata(File file) throws IOException {
        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file));
        try {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } finally {
            reader.close();
        }
    }

    // --- Status getters for API ---

    public boolean isRunning() {
        return running;
    }

    public int getSlowTaskCount() {
        return slowTaskCount;
    }

    public List<String> getSlowTaskIds() {
        return new ArrayList<>(slowTaskIds);
    }

    public Map<String, ProfilerResult> getActiveProfiles() {
        return new ConcurrentHashMap<>(activeProfiles);
    }

    public boolean isProfilerAvailable() {
        return profiler.isAvailable();
    }

    public ServerMonitorConfig getConfig() {
        return config;
    }

    public long getLastCheckTime() {
        return lastCheckTime;
    }

    /**
     * Most recent sub-process scan result (long-running external processes).
     */
    public List<SubProcessMonitor.SubProcess> getSubProcesses() {
        return lastSubProcesses;
    }

    public long getLastSubProcessCheckTime() {
        return lastSubProcessCheckTime;
    }

    public boolean isSubProcessEnabled() {
        return config.isSubProcessEnabled();
    }
}
