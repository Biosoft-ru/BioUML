package biouml.plugins.servermonitor;

import java.io.IOException;
import java.lang.ProcessHandle;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Detects long-running external sub-processes spawned by the server (perl / R /
 * nextflow / git / etc.) and reports their full command line (program + all
 * parameters).
 *
 * <p>async-profiler only samples the JVM itself, so when a task's time is spent
 * inside an external script the CPU profile is empty (just futex waits). This
 * monitor complements it by walking the OS process tree under the server JVM and
 * flagging any descendant that has been alive longer than
 * {@link ServerMonitorConfig#getSubProcessThreshold()}.
 *
 * <p>The full parameter list is read from {@code /proc/<pid>/cmdline} (Linux),
 * which is why it works even when the work is launched through a shell wrapper
 * (e.g. {@code glaunch.sh "<command>"}) — the real {@code perl script.pl --opt1}
 * process is a grandchild whose own cmdline carries the true argv. On non-Linux
 * hosts we fall back to {@link ProcessHandle.Info#arguments()}.
 *
 * <p>This is OS-level discovery, so it needs no changes to task / analysis code
 * and covers every launch path uniformly.
 */
public class SubProcessMonitor {

    private static final Logger log = Logger.getLogger(SubProcessMonitor.class.getName());

    /**
     * Command-line prefixes that are pure plumbing, not the actual work. These
     * processes (and, transitively, their wrappers) are skipped so the report
     * focuses on the real long-running program rather than the shell that
     * launched it.
     */
    private static final Set<String> WRAPPER_BASENAMES = new LinkedHashSet<>();
    static {
        WRAPPER_BASENAMES.add("glaunch.sh");
        WRAPPER_BASENAMES.add("bash");
        WRAPPER_BASENAMES.add("sh");
        WRAPPER_BASENAMES.add("dash");
        WRAPPER_BASENAMES.add("zsh");
    }

    private final ServerMonitorConfig config;

    /**
     * pids we have already logged as exceeding the threshold, so we log once per
     * process instead of spamming the log on every monitor cycle.
     */
    private final Set<Long> alreadyReported = ConcurrentHashMap.newKeySet();

    public SubProcessMonitor(ServerMonitorConfig config) {
        this.config = config;
    }

    /**
     * A snapshot of a sub-process worth reporting.
     */
    public static class SubProcess {
        public final long pid;
        public final long ageSeconds;
        public final String command;
        public final boolean slow; // age >= threshold
        /** First time this pid was observed (ms); 0 for a single-scan snapshot. */
        public final long firstSeenMs;
        /** Last time this pid was observed (ms); 0 for a single-scan snapshot. */
        public final long lastSeenMs;
        /** Age (s) at first observation; 0 for a single-scan snapshot. */
        public final long firstAgeSec;
        /** Age (s) at last observation. */
        public final long lastAgeSec;

        SubProcess(long pid, long ageSeconds, String command, boolean slow) {
            this.pid = pid;
            this.ageSeconds = ageSeconds;
            this.command = command;
            this.slow = slow;
            this.firstSeenMs = 0;
            this.lastSeenMs = 0;
            this.firstAgeSec = 0;
            this.lastAgeSec = 0;
        }

        SubProcess(long pid, long ageSeconds, long firstSeenMs, long lastSeenMs,
                   long firstAgeSec, long lastAgeSec, boolean slow, String command) {
            this.pid = pid;
            this.ageSeconds = ageSeconds;
            this.command = command;
            this.slow = slow;
            this.firstSeenMs = firstSeenMs;
            this.lastSeenMs = lastSeenMs;
            this.firstAgeSec = firstAgeSec;
            this.lastAgeSec = lastAgeSec;
        }

        /**
         * Estimated total lifetime of the process in seconds, or -1 if this is a
         * single-scan snapshot that carries no age.
         *
         * <p>{@code ageSeconds} is the wall-clock time since the process started
         * (see {@link #check()}, which computes it as {@code now - startInstant}),
         * so the age at the <em>last</em> observation is already a total-lifetime
         * estimate. Do NOT add the observation-interval wall-clock delta on top of
         * it: the process was running for the whole interval, and the age delta
         * over that same interval would double-count it.
         */
        public long estimatedLifetimeSec() {
            if (lastAgeSec <= 0) {
                return -1;
            }
            return lastAgeSec;
        }
    }

    /**
     * Scan the process tree and return all sub-processes older than the minimum
     * age, flagging those that have also exceeded the slow threshold. Logs a
     * SEVERE line the first time each sub-process crosses the threshold.
     *
     * @return list of reported sub-processes (slow first, then by age, descending)
     */
    public List<SubProcess> check() {
        List<SubProcess> result = new ArrayList<>();

        if (!config.isSubProcessEnabled()) {
            return result;
        }

        long jvmPid = getJvmPid();
        if (jvmPid <= 0) {
            return result;
        }

        Instant now = Instant.now();
        long thresholdSec = config.getSubProcessThreshold();
        long minAgeSec = config.getSubProcessMinAge();

        ProcessHandle.of(jvmPid).ifPresent(root ->
            root.descendants().forEach(ph -> {
                try {
                    ProcessHandle.Info info = ph.info();
                    if (!info.startInstant().isPresent()) {
                        return;
                    }
                    long pid = ph.pid();
                    long ageSec = Duration.between(info.startInstant().get(), now).getSeconds();

                    // Ignore the JVM itself and anything younger than the minimum age.
                    if (pid == jvmPid || ageSec < minAgeSec) {
                        return;
                    }

                    // Resolve the full command line. On Linux prefer /proc cmdline
                    // (real argv); fall back to ProcessHandle arguments.
                    String cmdline = resolveCommandLine(ph, info);
                    if (cmdline == null || cmdline.isEmpty()) {
                        return;
                    }

                    // Skip pure wrapper/shell plumbing processes; keep the actual
                    // program they launched.
                    if (isWrapper(cmdline)) {
                        return;
                    }

                    boolean slow = ageSec >= thresholdSec;
                    result.add(new SubProcess(pid, ageSec, cmdline, slow));

                    if (slow && alreadyReported.add(pid)) {
                        log.log(Level.SEVERE,
                                "SubProcessMonitor: sub-process running " + ageSec + "s (threshold "
                                + thresholdSec + "s): PID " + pid + " — " + cmdline);
                    }
                } catch (Exception e) {
                    // A single unreadable process must not abort the whole scan.
                    log.log(Level.FINE, "SubProcessMonitor: could not inspect process", e);
                }
            })
        );

        // Drop bookkeeping for pids that are no longer alive.
        if (!alreadyReported.isEmpty()) {
            Set<Long> alive = new java.util.HashSet<>();
            for (SubProcess sp : result) {
                alive.add(sp.pid);
            }
            for (Long pid : new ArrayList<>(alreadyReported)) {
                if (!alive.contains(pid)) {
                    alreadyReported.remove(pid);
                }
            }
        }

        // Slow first, then newest-slowest / oldest first.
        result.sort((a, b) -> {
            if (a.slow != b.slow) {
                return a.slow ? -1 : 1;
            }
            return Long.compare(b.ageSeconds, a.ageSeconds);
        });

        return result;
    }

    /**
     * Build a one-line summary suitable for embedding in profile metadata or a
     * status payload.
     */
    public static String summarize(List<SubProcess> subProcesses) {
        if (subProcesses == null || subProcesses.isEmpty()) {
            return "no long sub-processes";
        }
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (SubProcess sp : subProcesses) {
            if (n > 0) sb.append("; ");
            sb.append(sp.slow ? "SLOW " : "").append("pid=").append(sp.pid)
                    .append(" age=").append(sp.ageSeconds).append("s cmd=[").append(sp.command).append("]");
            if (++n >= 10) {
                sb.append("; …(").append(subProcesses.size() - n).append(" more)");
                break;
            }
        }
        return sb.toString();
    }

    /**
     * Resolve the full command line for a process. Prefers {@code /proc/<pid>/cmdline}
     * (space-joining NUL-separated argv) on Linux; otherwise uses
     * {@link ProcessHandle.Info#command()} + {@link ProcessHandle.Info#arguments()}.
     */
    private String resolveCommandLine(ProcessHandle ph, ProcessHandle.Info info) {
        // Linux /proc/<pid>/cmdline
        try {
            Path cmdline = Paths.get("/proc", String.valueOf(ph.pid()), "cmdline");
            if (Files.isReadable(cmdline)) {
                byte[] raw = Files.readAllBytes(cmdline);
                if (raw.length > 0) {
                    // argv entries are NUL-separated; the last element may lack a
                    // trailing NUL.
                    String joined = new String(raw, StandardCharsets.UTF_8).replace((char)0, ' ');
                    String trimmed = joined.trim();
                    if (!trimmed.isEmpty()) {
                        return trimmed;
                    }
                }
            }
        } catch (IOException e) {
            // fall through to ProcessHandle
        }

        // Fallback (non-Linux, or /proc not readable). arguments() returns
        // Optional<String[]>.
        String[] args = info.arguments().orElse(null);
        if (args == null || args.length == 0) {
            return info.command().orElse("");
        }
        StringBuilder sb = new StringBuilder(info.command().orElse(""));
        for (String a : args) {
            if (a == null) continue;
            sb.append(' ').append(a);
        }
        return sb.toString().trim();
    }

    /**
     * True if the command is a pure shell/wrapper (e.g. the {@code glaunch.sh}
     * launcher or an intermediate {@code bash -c}), not the actual program.
     */
    private boolean isWrapper(String cmdline) {
        // Take the first token (the executable) and compare its basename.
        int sp = cmdline.indexOf(' ');
        String first = (sp > 0 ? cmdline.substring(0, sp) : cmdline).trim();
        int slash = first.lastIndexOf('/');
        String base = slash >= 0 ? first.substring(slash + 1) : first;
        return WRAPPER_BASENAMES.contains(base.toLowerCase());
    }

    /**
     * Get the current JVM's PID.
     */
    private long getJvmPid() {
        String pidStr = ManagementFactory.getRuntimeMXBean().getName();
        int idx = pidStr.indexOf('@');
        if (idx > 0) {
            try {
                return Long.parseLong(pidStr.substring(0, idx));
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }
}
