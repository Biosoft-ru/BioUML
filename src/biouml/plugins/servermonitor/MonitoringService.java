package biouml.plugins.servermonitor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
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
     * <p>Writing is serialized against {@link #subProcessLogLock}; a rewrite is
     * performed (under the same lock) when the file exceeds the size/age limits
     * so the log stays bounded.
     */
    private void appendSubProcessLog(List<SubProcessMonitor.SubProcess> subs, long timestamp) {
        File logFile = getSubProcessLogFile();
        if (logFile == null) {
            return;
        }
        synchronized (subProcessLogLock) {
            try {
                // Purge old / excess lines first (cheap: only read when the
                // file is already large or the oldest line is past its age).
                if (logFile.exists() && logFile.length() > 0) {
                    long now = System.currentTimeMillis();
                    File tmp = File.createTempFile("subproc", ".jsonl", logFile.getParentFile());
                    boolean purged = purgeSubProcessLog(logFile, tmp, now);
                    if (purged) {
                        // tmp now holds the pruned content; rename over the original.
                        if (!logFile.delete() || !tmp.renameTo(logFile)) {
                            tmp.delete();
                        }
                    } else {
                        tmp.delete();
                    }
                }

                String line = buildSubProcessRecord(subs, timestamp);
                try (BufferedWriter writer = new BufferedWriter(
                        new java.io.OutputStreamWriter(new FileOutputStream(logFile, true), StandardCharsets.UTF_8))) {
                    writer.write(line);
                    writer.newLine();
                }
            } catch (Exception e) {
                log.log(Level.WARNING, "Error appending sub-process log " + logFile.getAbsolutePath(), e);
            }
        }
    }

    /**
     * Rewrite the sub-process log, dropping lines older than
     * {@link #MAX_SUB_PROCESS_LOG_AGE} and keeping only the most recent
     * {@link #MAX_SUB_PROCESS_LOG_LINES}. Returns true if any line was dropped
     * (i.e. the file content actually changed).
     */
    private boolean purgeSubProcessLog(File logFile, File tmp, long now) throws IOException {
        // Pass 1: read the log, dropping lines older than MAX_SUB_PROCESS_LOG_AGE.
        List<String> kept = new ArrayList<>();
        int total = 0;
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(new FileInputStream(logFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                total++;
                long ts = extractTimestamp(line);
                boolean tooOld = ts > 0 && (now - ts) > MAX_SUB_PROCESS_LOG_AGE;
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

        boolean changed = (kept.size() != total);
        if (changed) {
            // Pass 2: rewrite the pruned content to tmp (caller renames tmp over logFile).
            try (BufferedWriter tw = new BufferedWriter(
                    new java.io.OutputStreamWriter(new FileOutputStream(tmp), StandardCharsets.UTF_8))) {
                for (String l : kept) {
                    tw.write(l);
                    tw.newLine();
                }
            }
        }
        return changed;
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
            o.put("command", sp.command);
            arr.put(o);
        }
        record.put("subProcesses", arr);
        return record.toString();
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
     * Total number of records in the sub-process log (0 if none).
     */
    public int getSubProcessLogCount() {
        File logFile = getSubProcessLogFile();
        if (logFile == null || !logFile.exists() || logFile.length() == 0) {
            return 0;
        }
        synchronized (subProcessLogLock) {
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(new FileInputStream(logFile), StandardCharsets.UTF_8))) {
                int n = 0;
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) n++;
                }
                return n;
            } catch (Exception e) {
                return 0;
            }
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
