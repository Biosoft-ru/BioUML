package biouml.plugins.servermonitor;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

/**
 * Wrapper for async-profiler integration.
 * Handles profiler detection, auto-download, and invocation.
 */
public class AsyncProfilerWrapper {

    private static final Logger log = Logger.getLogger(AsyncProfilerWrapper.class.getName());

    // async-profiler release URL for Linux x64
    private static final String PROFILER_VERSION = "v3.0";
    private static final String PROFILER_URL = "https://github.com/async-profiler/async-profiler/releases/download/"
            + PROFILER_VERSION + "/async-profiler-3.0-linux-x64.tar.gz";
    // Directory name inside the tarball (matches tarball name minus .tar.gz)
    private static final String PROFILER_DIR_NAME = "async-profiler-3.0-linux-x64";

    /** Timeout for the `asprof stop` command. Prevents a hung stop from blocking the monitoring thread. */
    private static final int STOP_TIMEOUT_SECONDS = 30;
    /** Extra buffer (seconds) added to the profiling duration for the run timeout. */
    private static final int RUN_PROFILER_TIMEOUT_BUFFER_SECONDS = 60;
    /** Bounded wait (seconds) after destroyForcibly() to confirm the process actually exited. */
    private static final int KILL_WAIT_SECONDS = 5;
    /** Bounded wait (seconds) to join the process-I/O drain thread after the child exits. */
    private static final int IO_DRAIN_JOIN_SECONDS = 2;
    /**
     * Maximum characters of child-process output kept for diagnostics. The
     * profiler output is only ever logged on a failure path (non-zero exit),
     * so the tail is enough to diagnose the failure; capping it keeps memory
     * bounded even if a hung/verbose profiler streams a lot to the pipe.
     */
    private static final int MAX_OUTPUT_CAPTURE_CHARS = 16384;

    /**
     * Optional test override for the per-run timeout (seconds). When > 0 the
     * run timeout is this value INSTEAD OF {@code duration + buffer}, letting
     * unit tests exercise the hang/destroy path in seconds. The production
     * path never sets this (it stays 0), so behavior is unchanged.
     */
    private volatile int testRunTimeoutOverride = 0;

    /**
     * Test-only: force the per-run timeout to {@code timeoutSeconds}.
     * A value <= 0 clears the override and restores the normal
     * {@code duration + RUN_PROFILER_TIMEOUT_BUFFER_SECONDS} timeout.
     * Not part of the production API.
     */
    public void setTestRunTimeout(int timeoutSeconds) {
        this.testRunTimeoutOverride = timeoutSeconds;
    }

    private final ServerMonitorConfig config;
    private String profilerPath;
    private volatile boolean profilerAvailable = false;
    private volatile String activeProfileOutputPath = null;

    public AsyncProfilerWrapper(ServerMonitorConfig config) {
        this.config = config;
        this.profilerPath = resolveProfilerPath();
    }

    /**
     * Initialize the profiler: check for existing binary or download it.
     * @return true if profiler is available, false otherwise
     */
    public boolean init() {
        // First try to resolve the configured path
        profilerPath = resolveProfilerPath();
        if (profilerPath != null && new File(profilerPath).exists()) {
            profilerAvailable = true;
            log.info("AsyncProfilerWrapper: profiler found at " + profilerPath);
            return true;
        }

        // Try to download
        try {
            return downloadProfiler();
        } catch (Exception e) {
            log.log(Level.WARNING, "AsyncProfilerWrapper: failed to download profiler", e);
            profilerAvailable = false;
            return false;
        }
    }

    /**
     * Check if the profiler is available.
     * @return true if profiler binary is found and executable
     */
    public boolean isAvailable() {
        return profilerAvailable && profilerPath != null && new File(profilerPath).exists();
    }

    /**
     * Start profiling specified threads.
     * Generates tree format (primary) plus optional extra formats.
     * @param threadIds thread IDs to profile
     * @param format primary output format (tree, flamegraph, collapsed, flat, traces, jfr)
     * @return ProfilerResult with output paths and timing
     */
    public ProfilerResult start(long[] threadIds, String format) {
        return start(threadIds, format, true);
    }

    /**
     * Start profiling specified threads with control over secondary output generation.
     * @param threadIds thread IDs to profile
     * @param format primary output format (tree, flamegraph, collapsed, flat, traces, jfr)
     * @param generateSecondary whether to also generate extra formats for AI agent use
     * @return ProfilerResult with output paths and timing
     */
    public ProfilerResult start(long[] threadIds, String format, boolean generateSecondary) {
        if (!isAvailable()) {
            return new ProfilerResult("async-profiler is not available");
        }

        // No explicit stop() here: runProfiler() decides per-invocation
        // whether an in-progress session needs to be cleared first (see the
        // pre-run guard there). A blanket `asprof stop` before every run was
        // causing hangs (strange_gx) and, on ict, a 30s timeout wait on every
        // 60s monitoring cycle when the agent was stuck. If a previous
        // session is somehow still running, the new `asprof -d` fails with a
        // clear error rather than hanging.

        // Get JVM PID
        long jvmPid = getJvmPid();
        if (jvmPid <= 0) {
            return new ProfilerResult("Could not determine JVM PID");
        }

        // Build thread ID string
        String threadIdStr = "";
        if (threadIds != null && threadIds.length > 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < threadIds.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(threadIds[i]);
            }
            threadIdStr = sb.toString();
        }

        long startTime = System.currentTimeMillis();
        int duration = config.getProfileDuration();
        String baseName = buildBaseName();

        // Format name is used directly as both -o value and file extension
        String primaryPath = buildOutputPath(baseName, format);

        try {
            // Run 1: Primary format. runProfiler() sets activeProfileOutputPath
            // while its `asprof -d` runs and clears it in a finally block, so
            // getProfileStatus() reflects the live state and the pre-run stop
            // guard knows when a session is actually in progress.
            boolean primaryOk = runProfiler(jvmPid, threadIdStr, duration, primaryPath, format);

            // Run 2: Generate extra formats for AI agent use. Each extra run
            // is a fresh `asprof -d` invocation with the same pre-run guard.
            String[] extraPaths = null;
            if (generateSecondary && primaryOk) {
                String extra = config.getExtraFormats();
                if (extra != null && !extra.trim().isEmpty()) {
                    String[] formats = extra.split(",");
                    List<String> paths = new ArrayList<>();
                    for (String f : formats) {
                        f = f.trim();
                        if (f.isEmpty()) continue;
                        // Skip if it's the same as primary
                        if (f.equals(format)) continue;
                        String extraPath = buildOutputPath(baseName, f);
                        runProfiler(jvmPid, threadIdStr, duration, extraPath, f);
                        paths.add(extraPath);
                    }
                    if (!paths.isEmpty()) {
                        extraPaths = paths.toArray(new String[0]);
                    }
                }
            }

            long endTime = System.currentTimeMillis();

            if (primaryOk && new File(primaryPath).exists()) {
                // Profiler runs synchronously — once we return the process has
                // exited and runProfiler's finally block has cleared the
                // active marker, so getProfileStatus() reports "stopped".
                String[] tidStrs = threadIdStr.isEmpty() ? new String[0] : threadIdStr.split(",");
                return new ProfilerResult(primaryPath, startTime, endTime,
                        tidStrs.length, tidStrs, format, extraPaths);
            } else {
                return new ProfilerResult("Profiler exited with code " + (primaryOk ? 0 : 1));
            }
        } catch (IOException | InterruptedException e) {
            long endTime = System.currentTimeMillis();
            log.log(Level.SEVERE, "AsyncProfilerWrapper: profiling error", e);
            return new ProfilerResult("Profiling error: " + e.getMessage());
        }
    }

    /**
     * Run a single profiler invocation.
     * @return true if profiler exited successfully
     */
    private boolean runProfiler(long jvmPid, String threadIdStr, int duration, String outputPath, String outputFormat)
            throws IOException, InterruptedException {
        // Clear a previous profiling session only if a run is actually
        // in progress in this JVM (start() sets activeProfileOutputPath
        // while its `asprof -d` runs, and clears it when the run returns).
        //
        // async-profiler keeps the agent (libasyncProfiler.so) installed in
        // the JVM between runs. If a prior session's auto-stop did not fully
        // release it (e.g. its CLI was killed mid-teardown, or the duration
        // stop was interrupted), the agent stays "active" and every new
        // `asprof -d` fails with code 200 "Profiler already started" — with
        // nothing to clear it short of a restart. `asprof stop` tells the
        // agent to release, so run it first.
        //
        // When NO run is active there is nothing to stop — and calling
        // `asprof stop` then anyway is what the pre-run guard was doing on
        // every 60s monitoring cycle on ict: when the agent is stuck (a
        // prior CLI was SIGKILLed during teardown), every one of those stops
        // hangs the full STOP_TIMEOUT_SECONDS before being destroyed, which
        // both spams the log and delays every profiling run by 30s. Skipping
        // the stop in the idle case avoids that; a genuinely active session
        // still gets the bounded stop + lingering-process fallback below.
        //
        // stop() is bounded (STOP_TIMEOUT_SECONDS + destroyForcibly), so it
        // cannot hang the monitoring thread — the reason it was previously
        // removed from this path. When it exits cleanly we KNOW the agent is
        // released, so proceed. When it times out / errors, the agent may
        // still be active; fall back to killing lingering CLI processes (they
        // hold the agent too) and only start if we can confirm the tree is
        // clean — otherwise skip rather than risk a SIGKILL racing another
        // in-progress profiler.
        if (activeProfileOutputPath != null) {
            boolean stopClean = stop();
            if (!stopClean) {
                log.info("AsyncProfilerWrapper: stop() did not exit cleanly; "
                        + "checking for lingering profiler processes");
                if (!killLingeringProfilerProcesses(jvmPid)) {
                    log.warning("AsyncProfilerWrapper: profiler state could not be cleared "
                            + "(stop timed out and lingering process still alive); "
                            + "not starting a new profiler");
                    return false;
                }
            }
        }

        List<String> command = new ArrayList<>();
        command.add(profilerPath);
        command.add("-d");
        command.add(String.valueOf(duration));
        command.add("-f");
        command.add(outputPath);
        command.add("-e");
        command.add("cpu");
        command.add("-o");
        command.add(outputFormat);

        if (!threadIdStr.isEmpty()) {
            command.add("-t");
            command.add(threadIdStr);
        }

        // PID is a positional argument at the end (not -j)
        command.add(String.valueOf(jvmPid));

        ProcessBuilder pb = new ProcessBuilder(command);
        // Set LD_LIBRARY_PATH to include the profiler's lib directory
        String profilerDir = new File(profilerPath).getParent();
        String libPath = profilerDir + "/lib";
        Map<String, String> env = pb.environment();
        String existingLibPath = env.get("LD_LIBRARY_PATH");
        if (existingLibPath != null) {
            env.put("LD_LIBRARY_PATH", libPath + ":" + existingLibPath);
        } else {
            env.put("LD_LIBRARY_PATH", libPath);
        }
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // Advertise the in-progress run while its `asprof -d` is alive, so a
        // concurrent start() sees an active session (and the pre-run guard in
        // this method knows a stop is needed when a prior run is interrupted).
        // start() is serialized by MonitoringService's profilerLock, so no two
        // runs overlap; the flag simply spans this blocking call.
        activeProfileOutputPath = outputPath;
        try {
            // Drain the process output on a daemon thread instead of reading it
            // to EOF inline: a hung profiler keeps its pipe open, so an inline
            // read would block forever (before the timeout was even reached),
            // and a verbose/hung profiler that keeps writing >64KB of output
            // fills the OS pipe and deadlocks the child once it stops
            // responding. The daemon thread keeps the pipe draining until the
            // child dies (destroyForcibly closes the stream and ends the
            // reader), and only the last MAX_OUTPUT_CAPTURE_CHARS are kept for
            // diagnostics — bounded memory by construction.
            StringBuilder output = new StringBuilder();
            java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(1);
            Thread ioThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        synchronized (output) {
                            if (output.length() < MAX_OUTPUT_CAPTURE_CHARS) {
                                output.append(line).append("\n");
                            }
                        }
                    }
                } catch (IOException ignored) {
                    // Stream closed by destroyForcibly() — expected on the
                    // timeout path; the captured output so far is still used.
                } finally {
                    done.countDown();
                }
            });
            ioThread.setName("AsyncProfilerWrapper-io-" + jvmPid);
            ioThread.setDaemon(true);
            ioThread.start();

            // Use a timeout (duration + buffer) so a hung profiler process
            // cannot block the monitoring thread indefinitely. A test override
            // can shrink this for fast unit tests of the hang path.
            int override = testRunTimeoutOverride;
            long timeoutSeconds = override > 0
                    ? override
                    : duration + RUN_PROFILER_TIMEOUT_BUFFER_SECONDS;
            if (!waitForProcessExit(process, timeoutSeconds, "profiler run")) {
                return false;
            }
            // Reap the I/O thread so a daemon cannot outlive this run.
            done.await(IO_DRAIN_JOIN_SECONDS, java.util.concurrent.TimeUnit.SECONDS);

            String stderr = output.toString();
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.log(Level.WARNING, "AsyncProfilerWrapper: profiler exited with code " + exitCode + ". stderr: " + stderr);
                return false;
            }

            // Verify the output file was actually created
            File outputFile = new File(outputPath);
            if (!outputFile.exists()) {
                log.log(Level.WARNING,
                        "AsyncProfilerWrapper: profiler exited successfully (code " + exitCode + ") "
                                + "but output file was not created — path=" + outputPath
                                + " exists=" + outputFile.exists()
                                + " canWrite=" + outputFile.getParentFile().canWrite()
                                + " parentExists=" + outputFile.getParentFile().exists());
                return false;
            }

            return true;
        } finally {
            // Clear the in-progress marker even on error paths so the next
            // run's pre-run guard sees a clean (idle) state.
            activeProfileOutputPath = null;
        }
    }

    /**
     * Stop the current profiling session.
     * @return true if the stop command exited cleanly within the timeout
     *         (profiler was cleanly stopped or not running); false if it timed
     *         out and was destroyed, errored, or the profiler is unavailable
     *         (the agent may still be active — a caller that needs to start a
     *         new run should treat this as "not confirmed stopped").
     */
    public boolean stop() {
        if (profilerPath == null || !isAvailable()) {
            return false;
        }

        long jvmPid = getJvmPid();
        if (jvmPid <= 0) {
            return false;
        }

        // Run the bounded stop. Returns true only if the stop process exited
        // on its own within the timeout (i.e. the profiler was cleanly stopped
        // or already not running). A timeout + destroyForcibly means the agent
        // may still be active, so we return false so a caller can fall back.
        boolean exitedCleanly;
        try {
            List<String> command = new ArrayList<>();
            command.add(profilerPath);
            command.add("stop");
            command.add(String.valueOf(jvmPid));

            ProcessBuilder pb = new ProcessBuilder(command);
            String profilerDir = new File(profilerPath).getParent();
            String libPath = profilerDir + "/lib";
            Map<String, String> env = pb.environment();
            String existingLibPath = env.get("LD_LIBRARY_PATH");
            if (existingLibPath != null) {
                env.put("LD_LIBRARY_PATH", libPath + ":" + existingLibPath);
            } else {
                env.put("LD_LIBRARY_PATH", libPath);
            }
            pb.redirectErrorStream(true);
            Process process = pb.start();
            // Use a timeout so a hung `asprof stop` (e.g. when the agent is
            // stuck after a prior CLI was killed mid-teardown) cannot block
            // the monitoring thread indefinitely.  The hang was observed on
            // strange_gx and ict (stop timed out on every 60s monitoring
            // cycle).  Bounded here (STOP_TIMEOUT_SECONDS + destroyForcibly),
            // so this stop is safe to call as a pre-run guard (see runProfiler).
            if (!waitForProcessExit(process, STOP_TIMEOUT_SECONDS, "stop")) {
                exitedCleanly = false;
            } else {
                exitedCleanly = true;
                log.info("AsyncProfilerWrapper: profiling stopped");
            }
        } catch (IOException e) {
            log.log(Level.WARNING, "AsyncProfilerWrapper: error stopping profiler", e);
            exitedCleanly = false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.log(Level.WARNING, "AsyncProfilerWrapper: interrupted while stopping profiler", e);
            exitedCleanly = false;
        }

        activeProfileOutputPath = null;
        return exitedCleanly;
    }

    /**
     * Wait for a profiler child process to exit within a bounded timeout,
     * draining its output pipe in the meantime (see runProfiler).
     * On timeout the process is destroyed forcefully and we make one
     * bounded attempt to confirm it actually exited — a child that ignores
     * SIGKILL is exceptional and is reported, not waited on forever.
     *
     * @param process the child process to wait for
     * @param timeoutSeconds the maximum time to wait for a clean exit
     * @param label short label for log messages (e.g. "stop", "profiler run")
     * @return true if the process exited cleanly within the timeout; false
     *         on timeout (after destroyForcibly) or if the process could not
     *         be confirmed dead
     */
    private boolean waitForProcessExit(Process process, long timeoutSeconds, String label)
            throws InterruptedException {
        if (!process.waitFor(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS)) {
            log.warning("AsyncProfilerWrapper: " + label
                    + " timed out after " + timeoutSeconds + "s, destroying process");
            process.destroyForcibly();
            if (!process.waitFor(KILL_WAIT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)) {
                log.warning("AsyncProfilerWrapper: " + label + " process did not exit within "
                        + KILL_WAIT_SECONDS + "s after destroyForcibly");
                return false;
            }
            return false;
        }
        return true;
    }

    /**
     * Kill any lingering asprof child processes of the current JVM that may
     * be holding the profiler agent from a previous run, and wait for them
     * to exit before the new profiler starts.  Uses ProcessHandle (Java 9+)
     * to find child processes whose executable matches the profiler binary
     * path.
     *
     * @return true if no matching profiler process remains alive;
     *         false if a matching process remains alive or the state
     *         could not be determined (caller should not start a new
     *         profiler in that case)
     */
    private boolean killLingeringProfilerProcesses(long jvmPid) {
        boolean allExited = true;
        try {
            java.nio.file.Path profilerBin = java.nio.file.Paths.get(profilerPath)
                    .toAbsolutePath().normalize();
            List<java.lang.ProcessHandle> toKill = new ArrayList<>();
            java.lang.ProcessHandle.of(jvmPid).ifPresent(handle ->
                handle.descendants().forEach(ph -> {
                    String cmd = ph.info().command().orElse("");
                    if (cmd.isEmpty()) return;
                    try {
                        java.nio.file.Path cmdPath = java.nio.file.Paths.get(cmd)
                                .toAbsolutePath().normalize();
                        if (sameExecutable(cmdPath, profilerBin)) {
                            log.info("AsyncProfilerWrapper: killing lingering profiler process PID "
                                    + ph.pid() + " (" + cmd + ")");
                            toKill.add(ph);
                        }
                    } catch (Exception e) {
                        // Not a valid path — ignore
                    }
                })
            );
            for (java.lang.ProcessHandle ph : toKill) {
                ph.destroyForcibly();
                // ProcessHandle has no bounded waitFor — poll isAlive().
                // Use nanoTime for the deadline (monotonic, unaffected
                // by NTP/clock changes).
                long deadline = System.nanoTime()
                        + java.util.concurrent.TimeUnit.SECONDS.toNanos(KILL_WAIT_SECONDS);
                while (ph.isAlive() && System.nanoTime() < deadline) {
                    try { Thread.sleep(100); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
                if (ph.isAlive()) {
                    allExited = false;
                    log.warning("AsyncProfilerWrapper: lingering profiler process PID "
                            + ph.pid() + " did not exit within " + KILL_WAIT_SECONDS
                            + "s after destroyForcibly");
                }
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "AsyncProfilerWrapper: could not check for lingering profiler processes", e);
            return false;
        }
        return allExited;
    }

    /**
     * Compare two paths for identity, resolving symlinks where possible.
     * Falls back to normalized absolute path comparison if toRealPath
     * fails (e.g. file doesn't exist).
     */
    private static boolean sameExecutable(java.nio.file.Path a, java.nio.file.Path b) {
        try {
            return a.toRealPath().equals(b.toRealPath());
        } catch (java.io.IOException e) {
            return a.toAbsolutePath().normalize().equals(b.toAbsolutePath().normalize());
        }
    }

    /**
     * Get the current profiling status.
     * @return "stopped", "profiling", or "error"
     */
    public String getProfileStatus() {
        if (!isAvailable()) {
            return "error";
        }
        return activeProfileOutputPath != null ? "profiling" : "stopped";
    }

    /**
     * Resolve the path to the profiler binary.
     * Checks config path, then common locations.
     * @return resolved path or null
     */
    private String resolveProfilerPath() {
        // Check configured path first
        String configPath = config.getProfilerPath();
        if (configPath != null && !configPath.isEmpty()) {
            File f = new File(configPath);
            if (f.exists() && f.canExecute()) {
                return f.getAbsolutePath();
            }
            // Also try if it exists but isn't executable (make it so later)
            if (f.exists()) {
                return f.getAbsolutePath();
            }
        }

        // Check common locations
        String[] commonPaths = {
            "./profiling/async-profiler-3.0-linux-x64/bin/asprof",
            "./profiling/async-profiler-3.0-linux-x64/bin/profiler.sh",
            "./profiler/profiler.sh",
            "./profiler/bin/profiler.sh",
            "/usr/local/bin/profiler.sh",
            "/opt/profiler/profiler.sh"
        };

        for (String path : commonPaths) {
            File f = new File(path);
            if (f.exists()) {
                return f.getAbsolutePath();
            }
        }

        return null;
    }

    /**
     * Download async-profiler from GitHub releases with retry logic.
     * @return true if download and extraction succeeded
     */
    private boolean downloadProfiler() {
        String downloadDir = config.getProfilerDir();
        File dir = new File(downloadDir);

        try {
            if (!dir.exists()) {
                // Directory should already exist after ensureProfilerDir(),
                // but create it here as a safety net.
                if (!dir.mkdirs()) {
                    File parent = dir.getParentFile();
                    if (parent != null) {
                        log.log(Level.SEVERE,
                                "AsyncProfilerWrapper: failed to create profiler directory " + dir.getAbsolutePath()
                                        + "; parent folder=" + parent.getAbsolutePath()
                                        + " exists=" + parent.exists()
                                        + " canWrite=" + parent.canWrite()
                                        + " — falling back to /tmp/profiling");
                    } else {
                        log.log(Level.SEVERE,
                                "AsyncProfilerWrapper: failed to create profiler directory " + dir.getAbsolutePath()
                                        + " (no parent folder) — falling back to /tmp/profiling");
                    }
                    // Trigger fallback
                    config.ensureProfilerDir();
                    downloadDir = config.getProfilerDir();
                    dir = new File(downloadDir);
                }
            }

            // Download tarball with retries
            File tarball = new File(dir, "async-profiler-" + PROFILER_VERSION + "-linux-x64.tar.gz");
            int maxRetries = 3;
            int retryDelay = 5000; // 5 seconds
            IOException lastException = null;

            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                log.info("AsyncProfilerWrapper: downloading profiler from " + PROFILER_URL + " (attempt " + attempt + "/" + maxRetries + ")");
                try {
                    downloadFile(PROFILER_URL, tarball);
                    break; // Success
                } catch (IOException e) {
                    lastException = e;
                    log.log(Level.WARNING, "AsyncProfilerWrapper: download attempt " + attempt + " failed", e);
                    if (attempt < maxRetries) {
                        log.info("AsyncProfilerWrapper: retrying in " + (retryDelay / 1000) + " seconds...");
                        try {
                            Thread.sleep(retryDelay);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                        retryDelay *= 2; // Exponential backoff
                    }
                }
            }

            if (lastException != null && !tarball.exists()) {
                log.log(Level.SEVERE, "AsyncProfilerWrapper: all " + maxRetries + " download attempts failed", lastException);
                return false;
            }

            // Extract tarball
            extractTarball(tarball, dir);

            // Clean up tarball
            tarball.delete();

            // Find profiler binary (tarball extracts to async-profiler-3.0-linux-x64/bin/asprof)
            File profilerBin = new File(dir, PROFILER_DIR_NAME + "/bin/asprof");
            if (!profilerBin.exists()) {
                // Try alternate extraction locations
                profilerBin = new File(dir, PROFILER_DIR_NAME + "/bin/profiler.sh");
                if (!profilerBin.exists()) {
                    profilerBin = new File(dir, PROFILER_DIR_NAME + "/profiler.sh");
                    if (!profilerBin.exists()) {
                        profilerBin = new File(dir, "profiler.sh");
                    }
                }
            }

            if (profilerBin.exists()) {
                profilerBin.setExecutable(true);
                profilerPath = profilerBin.getAbsolutePath();
                profilerAvailable = true;
                log.info("AsyncProfilerWrapper: profiler downloaded to " + profilerPath);
                return true;
            } else {
                log.warning("AsyncProfilerWrapper: profiler binary not found after extraction");
                return false;
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "AsyncProfilerWrapper: download error", e);
            return false;
        }
    }

    /**
     * Download a file from a URL.
     */
    private void downloadFile(String urlString, File destination) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);
        conn.setRequestMethod("GET");

        try (InputStream in = conn.getInputStream();
             FileOutputStream out = new FileOutputStream(destination)) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Extract a tar.gz archive using Apache Commons Compress.
     */
    private void extractTarball(File tarball, File destDir) throws IOException {
        log.info("AsyncProfilerWrapper: extracting tarball with Apache Commons Compress");
        try (InputStream fis = Files.newInputStream(tarball.toPath());
             GZIPInputStream gzis = new GZIPInputStream(fis);
             TarArchiveInputStream tais = new TarArchiveInputStream(gzis)) {
            TarArchiveEntry entry;
            int entryCount = 0;
            while ((entry = tais.getNextTarEntry()) != null) {
                String name = entry.getName();
                // Sanitize: strip leading ./ or /
                while (name.startsWith("./") || name.startsWith("/")) {
                    name = name.substring(1);
                }
                if (name.isEmpty()) continue;

                File outFile = new File(destDir, name);
                File parent = outFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }

                if (entry.isDirectory()) {
                    outFile.mkdirs();
                    log.fine("AsyncProfilerWrapper: extracted dir: " + outFile.getPath());
                } else {
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = tais.read(buf)) != -1) {
                            fos.write(buf, 0, n);
                        }
                    }
                    log.fine("AsyncProfilerWrapper: extracted file: " + outFile.getPath() + " (" + entry.getSize() + " bytes)");
                }
                entryCount++;
            }
            log.info("AsyncProfilerWrapper: extracted " + entryCount + " entries");
        }
    }

    /**
     * Build an output file path for the profile using a shared base name.
     * @param baseName the base name (e.g., "profile_123456")
     * @param format the file extension (html, collapsed, txt)
     */
    private String buildOutputPath(String baseName, String format) {
        File dir = new File(config.getProfilerDir());
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                File parent = dir.getParentFile();
                if (parent != null) {
                    log.log(Level.WARNING,
                            "AsyncProfilerWrapper: failed to create profiler directory " + dir.getAbsolutePath()
                                    + "; parent folder=" + parent.getAbsolutePath()
                                    + " exists=" + parent.exists()
                                    + " canWrite=" + parent.canWrite()
                                    + " — falling back to /tmp/profiling");
                } else {
                    log.log(Level.WARNING,
                            "AsyncProfilerWrapper: failed to create profiler directory " + dir.getAbsolutePath()
                                    + " (no parent folder) — falling back to /tmp/profiling");
                }
                config.ensureProfilerDir();
                dir = new File(config.getProfilerDir());
            }
        }
        return dir.getAbsolutePath() + "/" + baseName + "." + format;
    }

    /**
     * Build a base name for the profile output files.
     */
    private String buildBaseName() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        return "profile_" + timestamp;
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

    /**
     * Execute a command and return the process.
     */
    private Process executeCommand(List<String> command) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        return pb.start();
    }

    /**
     * Read file content as UTF-8 string.
     */
    private String readFileContent(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), "UTF-8");
    }
}
