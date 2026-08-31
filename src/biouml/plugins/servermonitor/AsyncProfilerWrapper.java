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
     * Maximum number of characters of child-process output kept for
     * diagnostics. Only the TAIL of this size is retained (the error is
     * usually at the end), so memory stays bounded even if a hung/verbose
     * profiler streams a lot to the pipe.
     */
    private static final int MAX_OUTPUT_TAIL_CHARS = 16384;

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
    /**
     * Output path of the profile this wrapper instance is currently
     * generating, or null when idle. Drives {@link #getProfileStatus()}.
     * Cleared in {@link #stop()} and after a run completes.
     */
    private volatile String activeProfileOutputPath = null;
    /**
     * Whether this wrapper instance currently has an in-progress
     * {@code asprof -d} run. Set/cleared around the synchronous run in
     * {@link #runProfiler}. This is the pre-run stop guard's signal — it is
     * deliberately LOCAL to this instance (it does not probe the agent), so
     * it can go stale only if this JVM was killed mid-run and the agent was
     * left installed; that case is handled by the run failing with the real
     * async-profiler error until a restart clears the agent.
     */
    private volatile boolean profilerRunActive = false;

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
            // Run 1: Primary format. runProfiler() sets profilerRunActive and
            // activeProfileOutputPath while its `asprof -d` runs and clears
            // them in a finally block, so getProfileStatus() reflects the live
            // state and the pre-run stop guard knows when a session is
            // actually in progress.
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
        // Clear a previous profiling session only if this wrapper instance
        // has a run in progress (profilerRunActive, set/cleared around the
        // synchronous `asprof -d` below).
        //
        // async-profiler keeps the agent (libasyncProfiler.so) installed in
        // the JVM between runs. If a prior session's auto-stop did not fully
        // release it (e.g. its CLI was killed mid-teardown, or the duration
        // stop was interrupted), the agent stays "active" and every new
        // `asprof -d` fails with code 200 "Profiler already started" — with
        // nothing to clear it short of a restart. `asprof stop` tells the
        // agent to release, so run it first.
        //
        // This is deliberately LOCAL knowledge: profilerRunActive only says
        // "this instance started a run that is still in flight", it does NOT
        // probe the agent. So if this JVM was killed mid-run and the agent
        // was left installed, a fresh instance sees profilerRunActive ==
        // false and skips the stop; the subsequent `asprof -d` then fails
        // with the real async-profiler error (rather than hanging on a
        // useless `asprof stop` that would time out on a stuck agent every
        // 60s monitoring cycle — the ict log-spam regression). A genuinely
        // in-progress run still gets the bounded stop + lingering-process
        // fallback below.
        //
        // stop() is bounded (STOP_TIMEOUT_SECONDS + destroyForcibly), so it
        // cannot hang the monitoring thread — the reason it was previously
        // removed from this path. When it exits cleanly we KNOW the agent is
        // released, so proceed. When it times out / errors, the agent may
        // still be active; fall back to killing lingering CLI processes (they
        // hold the agent too) and only start if we can confirm the tree is
        // clean — otherwise skip rather than risk a SIGKILL racing another
        // in-progress profiler.
        if (profilerRunActive) {
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

        // Advertise the in-progress run while its `asprof -d` is alive, so a
        // concurrent start() sees an active session (and the pre-run guard in
        // this method knows a stop is needed when a prior run is interrupted).
        // start() is serialized by MonitoringService's profilerLock, so no two
        // runs overlap; the flags simply span this blocking call.
        profilerRunActive = true;
        activeProfileOutputPath = outputPath;
        try {
            // Use a timeout (duration + buffer) so a hung profiler process
            // cannot block the monitoring thread indefinitely. A test override
            // can shrink this for fast unit tests of the hang path.
            int override = testRunTimeoutOverride;
            long timeoutSeconds = override > 0
                    ? override
                    : duration + RUN_PROFILER_TIMEOUT_BUFFER_SECONDS;
            BoundedProcess run = executeBounded(command, timeoutSeconds, "profiler run");

            String stderr = run.output.toString();
            if (run.timedOut) {
                return false;
            }
            int exitCode = run.exitCode;
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
            // Clear the in-progress markers even on error paths so the next
            // run's pre-run guard sees a clean (idle) state and
            // getProfileStatus() reports "stopped".
            profilerRunActive = false;
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

        // Run the bounded stop through the same shared executor as runProfiler
        // (output drained on a daemon thread, bounded wait, destroyForcibly on
        // timeout, process destroyed on interruption). Returns true only if
        // the stop process exited on its own within the timeout (i.e. the
        // profiler was cleanly stopped or already not running). A timeout +
        // destroyForcibly means the agent may still be active, so we return
        // false so a caller can fall back to killing lingering processes.
        // The hang was observed on strange_gx and ict (stop timed out on every
        // 60s monitoring cycle when the agent was stuck); bounding it here
        // keeps it safe to call as a pre-run guard.
        boolean exitedCleanly;
        try {
            List<String> command = new ArrayList<>();
            command.add(profilerPath);
            command.add("stop");
            command.add(String.valueOf(jvmPid));

            BoundedProcess stop = executeBounded(command, STOP_TIMEOUT_SECONDS, "stop");
            if (!stop.timedOut) {
                exitedCleanly = true;
                log.info("AsyncProfilerWrapper: profiling stopped");
            } else {
                exitedCleanly = false;
            }
        } catch (IOException e) {
            log.log(Level.WARNING, "AsyncProfilerWrapper: error stopping profiler", e);
            exitedCleanly = false;
        } catch (InterruptedException e) {
            // executeBounded() already destroyed the stop process (and its
            // tree) and re-set the interrupt flag before rethrowing; nothing
            // further to do here.
            log.log(Level.WARNING, "AsyncProfilerWrapper: interrupted while stopping profiler", e);
            exitedCleanly = false;
        }

        return exitedCleanly;
    }

    /**
     * Result of a bounded child-process execution.
     */
    private static final class BoundedProcess {
        /** True if the process was destroyed on timeout/interruption (not a clean exit). */
        final boolean timedOut;
        /** Exit code, valid only when {@link #timedOut} is false. */
        final int exitCode;
        /** Tail of the process output (stdout+stderr) for diagnostics. */
        final TailBuffer output;

        BoundedProcess(boolean timedOut, int exitCode, TailBuffer output) {
            this.timedOut = timedOut;
            this.exitCode = exitCode;
            this.output = output;
        }
    }

    /**
     * Run a profiler child process with bounded, non-blocking I/O.
     *
     * <p>Both {@link #runProfiler} and {@link #stop} go through here so that
     * the I/O-drain / bounded-wait / destroy-on-timeout behavior is shared and
     * cannot drift. The child's combined stdout+stderr is drained on a daemon
     * thread (keeping the OS pipe from filling, which would otherwise deadlock
     * a child that keeps writing, and so the caller is never blocked reading
     * before the timeout is even reached). Only the TAIL of
     * {@link #MAX_OUTPUT_TAIL_CHARS} is retained for diagnostics.
     *
     * <p>On timeout the process (and, best-effort, its descendants) is
     * destroyed forcefully and one bounded attempt is made to confirm it
     * exited — a child that ignores SIGKILL is exceptional and reported, not
     * waited on forever.
     *
     * @param command the command to run (profiler binary + args)
     * @param timeoutSeconds the maximum time to wait for a clean exit
     * @param label short label for log messages (e.g. "stop", "profiler run")
     * @return a {@link BoundedProcess}; {@code timedOut} is true on timeout or
     *         interruption (in which case {@code exitCode} is meaningless)
     * @throws IOException if the process could not be started
     * @throws InterruptedException if the wait was interrupted; the process is
     *         destroyed and the interrupt flag re-set before rethrowing
     */
    private BoundedProcess executeBounded(List<String> command, long timeoutSeconds, String label)
            throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        // Set LD_LIBRARY_PATH to include the profiler's lib directory.
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

        // Drain the combined output on a daemon thread. The pipe is kept
        // draining until the child dies, so a hung/verbose child can neither
        // block the caller (we never read inline) nor deadlock itself by
        // filling the pipe. The tail buffer caps memory.
        TailBuffer tail = new TailBuffer(MAX_OUTPUT_TAIL_CHARS);
        java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(1);
        Thread ioThread = new Thread(() -> {
            // Read fixed-size chunks with Reader.read(char[]) rather than
            // readLine(): a pathological child that streams a huge amount with
            // no newlines would otherwise make readLine() accumulate an
            // unbounded single line in memory, defeating the bounded-memory
            // guarantee. A fixed read buffer bounds memory to the buffer size.
            char[] chunk = new char[4096];
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                int n;
                while ((n = reader.read(chunk)) != -1) {
                    synchronized (tail) {
                        tail.append(new String(chunk, 0, n));
                    }
                }
            } catch (IOException ignored) {
                // Stream closed (child destroyed, or it exited) — expected;
                // whatever was captured so far is still used.
            } finally {
                done.countDown();
            }
        });
        ioThread.setName("AsyncProfilerWrapper-io-" + label);
        ioThread.setDaemon(true);
        ioThread.start();

        try {
            if (!process.waitFor(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS)) {
                log.warning("AsyncProfilerWrapper: " + label
                        + " timed out after " + timeoutSeconds + "s, destroying process");
                destroyProcessTree(process);
                if (!process.waitFor(KILL_WAIT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)) {
                    log.warning("AsyncProfilerWrapper: " + label + " process did not exit within "
                            + KILL_WAIT_SECONDS + "s after destroyForcibly");
                }
                done.await(IO_DRAIN_JOIN_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
                return new BoundedProcess(true, -1, tail);
            }
            int exitCode = process.exitValue();
            // Reap the I/O thread so a daemon cannot outlive this run.
            done.await(IO_DRAIN_JOIN_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
            return new BoundedProcess(false, exitCode, tail);
        } catch (InterruptedException e) {
            // A cleanup method must not leave its child running: destroy it
            // (and descendants) before propagating the interruption.
            destroyProcessTree(process);
            done.await(IO_DRAIN_JOIN_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    /**
     * Destroy a process and, best-effort, its descendants. {@code destroyForcibly}
     * kills the direct child but not necessarily its children (e.g. a shell
     * wrapper's {@code sleep}), which can keep the output pipe open and leak
     * orphans.
     *
     * <p>Ordering matters: the descendant set is captured <em>before</em> the
     * root is killed. Killing the root first can reparent/reap the children
     * before {@code descendants()} is even evaluated, so they would never be
     * reached. Once captured, the descendants are killed deepest-first (a child
     * after its own children) so a lower process doesn't outlive the parent
     * that is about to be torn down. This is best-effort — a descendant spawned
     * concurrently with the kill can still be missed — but it is materially
     * more reliable than killing the root first.
     */
    private void destroyProcessTree(Process process) {
        // Capture the tree while the root is still alive so the enumeration is
        // complete, then tear it down.
        List<java.lang.ProcessHandle> tree = new ArrayList<>();
        try {
            java.lang.ProcessHandle root = process.toHandle();
            root.descendants().forEach(tree::add);
        } catch (Exception ignored) {
            // Enumerating is best-effort; the direct kill below is the primary
            // teardown.
        }
        // Kill deepest-first: a process only after all of its descendants.
        // The captured list is in pre-order (parent before child), so iterating
        // it in reverse yields children before their parents.
        for (int i = tree.size() - 1; i >= 0; i--) {
            tree.get(i).destroyForcibly();
        }
        process.destroyForcibly();
    }

    /**
     * A bounded output sink that retains only the trailing
     * {@code capacity} characters, discarding the oldest beyond that. Used to
     * keep child-process output memory bounded while preserving the tail
     * (where errors usually appear).
     *
     * <p>Implemented as a small deque of fixed-size chunks rather than a
     * single {@code StringBuilder}: appending is O(1) (no repeated shifting
     * of up to {@code capacity} characters on every chunk), which matters when
     * a verbose/hung process streams a lot.
     */
    private static final class TailBuffer {
        private static final int CHUNK = 4096;
        private final int capacity;
        private final java.util.ArrayDeque<String> chunks = new java.util.ArrayDeque<>();
        private int total;

        TailBuffer(int capacity) {
            this.capacity = capacity;
        }

        synchronized void append(CharSequence s) {
            // Split the input into CHUNK-sized pieces and enqueue them.
            int len = s.length();
            for (int i = 0; i < len; ) {
                int n = Math.min(CHUNK, len - i);
                chunks.addLast(s.subSequence(i, i + n).toString());
                i += n;
            }
            total += len;
            // Trim oldest chunks until the retained total is within capacity.
            while (total > capacity && chunks.size() > 1) {
                String oldest = chunks.removeFirst();
                total -= oldest.length();
            }
            // If a single chunk still exceeds capacity (only possible if one
            // append carried more than capacity), collapse to its own tail.
            if (total > capacity) {
                String only = chunks.removeFirst();
                total = only.length();
                if (total > capacity) {
                    only = only.substring(only.length() - capacity);
                    total = capacity;
                }
                chunks.addLast(only);
            }
        }

        @Override
        public synchronized String toString() {
            StringBuilder sb = new StringBuilder(total);
            for (String c : chunks) {
                sb.append(c);
            }
            return sb.toString();
        }
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
