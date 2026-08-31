package biouml.plugins.servermonitor._test;

import biouml.plugins.servermonitor.AsyncProfilerWrapper;
import biouml.plugins.servermonitor.ProfilerResult;
import biouml.plugins.servermonitor.ServerMonitorConfig;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.File;
import java.nio.file.Files;

/**
 * Tests {@link AsyncProfilerWrapper}'s child-process handling with a fake
 * profiler binary (a shell script standing in for asprof), without touching a
 * real JVM or the profiler agent:
 *
 * <ul>
 *   <li>a clean run exits promptly and is reported successful;</li>
 *   <li>when the "profiler" hangs past the timeout, the wrapper bounds the
 *       wait, destroys the child <em>and its descendants</em>, and does NOT
 *       block the caller indefinitely — the regression behind the ict
 *       "stop timed out after 30s" spam, where a hung process held the output
 *       pipe and blocked the reader;</li>
 *   <li>a non-zero exit is reported as a failed result;</li>
 *   <li>the pre-run guard skips {@code asprof stop} entirely when no session
 *       is active, so an idle monitoring cycle pays no stop wait (the ict
 *       log-spam amplifier: every 60s cycle ran a stop that hung 30s).</li>
 * </ul>
 *
 * <p>The fake binary's first argument selects its behavior:
 * {@code stop} → sleep forever (the agent-stuck symptom); {@code -d ...} →
 * sleep 0.2s then exit 0 and create the output file (a clean run); {@code -c
 * 1} → sleep 0.2s then exit 1 (a failed run). The hanging path uses
 * {@code exec sleep} so there is exactly one process to clean up (no orphaned
 * grandchild that would keep the output pipe open and mask a cleanup
 * failure).
 */
public class TestAsyncProfilerWrapper extends junit.framework.TestCase
{
    private File tmpDir;
    private ServerMonitorConfig config;

    public TestAsyncProfilerWrapper(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestAsyncProfilerWrapper.class.getName());
        suite.addTest(new TestAsyncProfilerWrapper("testCleanRunSucceeds"));
        suite.addTest(new TestAsyncProfilerWrapper("testHungProfilerIsBoundedAndDestroyed"));
        suite.addTest(new TestAsyncProfilerWrapper("testHungProcessTreeIsBoundedAndDestroyed"));
        suite.addTest(new TestAsyncProfilerWrapper("testNonZeroExitReportedAsFailure"));
        suite.addTest(new TestAsyncProfilerWrapper("testStopSkippedWhenNoSessionActive"));
        return suite;
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        tmpDir = Files.createTempDirectory("asprofwrapper-test").toFile();
        config = new ServerMonitorConfig();
        config.set(ServerMonitorConfig.PROFILER_DIR, tmpDir.getAbsolutePath());
    }

    @Override
    protected void tearDown() throws Exception
    {
        deleteRecursively(tmpDir);
        super.tearDown();
    }

    // --------------------------------------------------------------- tests

    /** A fast, well-behaved profiler run must be reported as successful. */
    public void testCleanRunSucceeds()
    {
        AsyncProfilerWrapper wrapper = makeWrapperWithFake("clean");
        ProfilerResult result = wrapper.start(new long[0], "collapsed");

        assertTrue("expected successful profile, got: " + result.getError(),
                result.isSuccess());
        assertNotNull("expected an output path on success",
                result.getOutputPath());
        assertTrue("expected output file to exist: " + result.getOutputPath(),
                new File(result.getOutputPath()).exists());
        // runProfiler clears the in-progress marker in a finally block.
        assertEquals("expected stopped status after run",
                "stopped", wrapper.getProfileStatus());
    }

    /**
     * A profiler that hangs must not hang the wrapper: the wait is bounded,
     * the child (and its descendants) is destroyed, and a failed result is
     * returned. We use the test-only timeout override to shrink the bound to a
     * few seconds so the hang path is exercised quickly. The property under
     * test is that the bound is honored, the caller is not blocked
     * indefinitely (the old failure mode was a blocked output reader / an
     * unbounded stop wait), and the hung process is actually torn down — not
     * left behind as an orphan.
     */
    public void testHungProfilerIsBoundedAndDestroyed() throws Exception
    {
        AsyncProfilerWrapper wrapper = makeWrapperWithFake("hung");
        config.set(ServerMonitorConfig.PROFILE_DURATION, 2);
        wrapper.setTestRunTimeout(3); // shrink the run timeout to ~3s for the test

        long start = System.currentTimeMillis();
        ProfilerResult result = wrapper.start(new long[0], "collapsed");
        long elapsedMs = System.currentTimeMillis() - start;

        assertFalse("expected failed profile, got success at "
                + result.getOutputPath(), result.isSuccess());
        // Intended bound: ~3s timeout + ~5s kill-confirm (+ small overhead).
        // 12s allows CI jitter while still catching a regression that lets the
        // call run for a minute (the old unbounded behavior).
        assertTrue("wrapper blocked too long: " + elapsedMs + "ms "
                + "(should be bounded by ~3s timeout + 5s kill-confirm)",
                elapsedMs < 12000);

        // The fake hang is `exec sleep 600` — exactly one process. It must be
        // gone shortly after start() returns; a leak here would mean the
        // wrapper failed to destroy the hung child (or its tree).
        String marker = "asprof-test-hung-" + tmpDir.getName();
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            if (listPidsRunningMarker(marker).isEmpty()) break;
            Thread.sleep(100);
        }
        java.util.List<String> leftover = listPidsRunningMarker(marker);
        assertTrue("expected the hung fake profiler process to be destroyed, "
                + "but found still running: " + leftover, leftover.isEmpty());
    }

    /**
     * A profiler whose hang is a real process tree (a shell wrapper that
     * spawns a child and waits for it, rather than {@code exec}-ing it) must
     * have the WHOLE tree torn down — not just the direct child. This is the
     * scenario the earlier review flagged: a shell's child (e.g. {@code sleep})
     * survives a plain {@code destroyForcibly()} of the shell, keeps the output
     * pipe open, and leaks.
     *
     * <p>The fake records the wrapper's own PID and the child's PID in a file,
     * so the test checks {@code /proc/<pid>} for both — a deterministic
     * identification of the processes the test itself created, rather than a
     * command-line scan.
     */
    public void testHungProcessTreeIsBoundedAndDestroyed() throws Exception
    {
        AsyncProfilerWrapper wrapper = makeWrapperWithFake("tree");
        config.set(ServerMonitorConfig.PROFILE_DURATION, 2);
        wrapper.setTestRunTimeout(3);

        long start = System.currentTimeMillis();
        ProfilerResult result = wrapper.start(new long[0], "collapsed");
        long elapsedMs = System.currentTimeMillis() - start;

        assertFalse("expected failed profile, got success at "
                + result.getOutputPath(), result.isSuccess());
        assertTrue("wrapper blocked too long: " + elapsedMs + "ms "
                + "(should be bounded by ~3s timeout + 5s kill-confirm)",
                elapsedMs < 12000);

        // The fake spawned a real `sleep 600` child and recorded the wrapper's
        // own PID plus the child's PID. Both must be gone; a leak of the child
        // means the process-tree teardown (descendants() kill) did not work.
        File pidFile = new File(tmpDir, "tree_pids");
        assertTrue("expected the fake profiler to record its PIDs in "
                + pidFile, pidFile.exists());
        String[] lines = new String(Files.readAllBytes(pidFile.toPath())).trim().split("\\R");
        assertTrue("expected two recorded PIDs, got: " + java.util.Arrays.toString(lines),
                lines.length == 2);
        long wrapperPid = Long.parseLong(lines[0].trim());
        long childPid = Long.parseLong(lines[1].trim());
        assertTrue("expected a plausible wrapper PID, got " + wrapperPid, wrapperPid > 0);
        assertTrue("expected a plausible child PID, got " + childPid, childPid > 0);

        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            if (!new File("/proc/" + wrapperPid).exists()
                    && !new File("/proc/" + childPid).exists()) break;
            Thread.sleep(100);
        }
        boolean wrapperAlive = new File("/proc/" + wrapperPid).exists();
        boolean childAlive = new File("/proc/" + childPid).exists();
        assertFalse("expected the hung fake profiler tree to be destroyed, but "
                + "wrapper PID " + wrapperPid + " alive=" + wrapperAlive
                + ", child PID " + childPid + " alive=" + childAlive,
                wrapperAlive || childAlive);
    }

    /** A profiler that exits non-zero is reported as a failed result. */
    public void testNonZeroExitReportedAsFailure()
    {
        AsyncProfilerWrapper wrapper = makeWrapperWithFake("fail");
        ProfilerResult result = wrapper.start(new long[0], "collapsed");

        assertFalse("expected failed profile", result.isSuccess());
        assertNotNull("expected an error message", result.getError());
    }

    /**
     * When no profiling session is active, start() must not invoke
     * {@code asprof stop} at all. The fake binary records every stop
     * invocation in a marker file; the idle path writes nothing there, while
     * a run always writes to the output file — so the absence of the marker
     * (with the output present) proves stop was skipped.
     */
    public void testStopSkippedWhenNoSessionActive()
    {
        AsyncProfilerWrapper wrapper = makeWrapperWithFake("clean");
        ProfilerResult result = wrapper.start(new long[0], "collapsed");

        assertTrue("expected successful profile, got: " + result.getError(),
                result.isSuccess());
        File stopMarker = new File(tmpDir, "stop_invoked");
        assertFalse("expected stop to be skipped when no session is active, "
                + "but the fake profiler recorded a stop invocation",
                stopMarker.exists());
    }

    // ----------------------------------------------------------------- utils

    /**
     * Build a wrapper whose configured profiler binary is a fake asprof shell
     * script whose behavior is selected by {@code mode}:
     * clean → fast success; hung → sleeps past the timeout; fail → exit 1.
     */
    private AsyncProfilerWrapper makeWrapperWithFake(String mode)
    {
        File binDir = new File(tmpDir, "fake-asprof");
        assertTrue(binDir.mkdirs());
        File script = new File(binDir, "asprof");
        try {
            Files.write(script.toPath(), fakeProfilerScript(mode).getBytes("UTF-8"));
        } catch (java.io.IOException e) {
            fail("could not write fake profiler script: " + e);
        }
        assertTrue("script must be executable", script.setExecutable(true));
        config.set(ServerMonitorConfig.PROFILER_PATH, script.getAbsolutePath());
        AsyncProfilerWrapper wrapper = new AsyncProfilerWrapper(config);
        assertTrue("expected fake profiler to be available", wrapper.init());
        return wrapper;
    }

    /**
     * The fake profiler script. Its first argument distinguishes a
     * {@code stop} invocation (sleeps forever — simulating the stuck agent)
     * from a run (first arg {@code -d} or {@code -c}):
     * <pre>
     *   stop [pid]      → record marker, exec sleep 600 (agent stuck)
     *   -d N -f F ...   → sleep 0.2, create F, exit 0          (mode: clean)
     *   -d N -f F ...   → exec sleep 600 (no exit)             (mode: hung)
     *   -d N -f F ...   → sleep 0.2, exit 1                    (mode: fail)
     *   -d N -f F ...   → spawn child + wait (a process tree) (mode: tree)
     *   -c 1 [pid]      → sleep 0.2, exit 1                    (any mode)
     * </pre>
     *
     * The {@code hung} path uses {@code exec} so the shell is replaced by the
     * sleeper — a single process (tests direct-child teardown). The {@code tree}
     * path deliberately keeps the shell alive and spawns a {@code sleep 600}
     * child, so the wrapper's process-<em>tree</em> teardown (the
     * {@code descendants()} kill) is what must reclaim both — a plain
     * {@code destroyForcibly()} of the shell alone would orphan the child.
     */
    private String fakeProfilerScript(String mode)
    {
        return "#!/bin/sh\n"
            + "MARKER=\"" + tmpDir.getAbsolutePath() + "/stop_invoked\"\n"
            + "if [ \"$1\" = \"stop\" ]; then\n"
            + "  touch \"$MARKER\"\n"
            + "  exec sleep 600\n"
            + "  exit 0\n"
            + "fi\n"
            + "if [ \"$1\" = \"-c\" ]; then\n"
            + "  sleep 0.2\n"
            + "  exit 1\n"
            + "fi\n"
            + "# a run: -d <N> -f <path> ...\n"
            + "F=\"\"\n"
            + "while [ $# -gt 0 ]; do\n"
            + "  if [ \"$1\" = \"-f\" ]; then shift; F=\"$1\"; fi\n"
            + "  shift\n"
            + "done\n"
            + "sleep 0.2\n"
            + "TREE_PID_FILE=\"" + tmpDir.getAbsolutePath() + "/tree_pids\"\n"
            + "case \"" + mode + "\" in\n"
            + "  hung) exec sleep 600 ;;\n"
            + "  tree) echo $$ > \"$TREE_PID_FILE\"; sleep 600 & echo $! >> \"$TREE_PID_FILE\"; wait ;;\n"
            + "  clean) [ -n \"$F\" ] && touch \"$F\"; exit 0 ;;\n"
            + "  fail) exit 1 ;;\n"
            + "esac\n";
    }

    /**
     * Return the PIDs of processes on this host whose command line contains
     * the given marker string. Used to confirm a hung fake-profiler process
     * was actually destroyed (the wrapper's process-tree teardown) rather
     * than merely that {@code start()} returned.
     */
    private java.util.List<String> listPidsRunningMarker(String marker) throws Exception
    {
        java.util.List<String> pids = new java.util.ArrayList<>();
        File procDir = new File("/proc");
        File[] entries = procDir.listFiles();
        if (entries == null) return pids;
        for (File e : entries) {
            String name = e.getName();
            if (name.isEmpty() || !name.chars().allMatch(Character::isDigit)) continue;
            File cmdline = new File(e, "cmdline");
            if (!cmdline.exists()) continue;
            byte[] data = Files.readAllBytes(cmdline.toPath());
            String cmd = new String(data, "UTF-8").replace('\u0000', ' ').trim();
            if (cmd.contains(marker)) {
                pids.add(name);
            }
        }
        return pids;
    }

    private static void deleteRecursively(File f)
    {
        if (f == null || !f.exists()) return;
        File[] children = f.listFiles();
        if (children != null) {
            for (File c : children) deleteRecursively(c);
        }
        f.delete();
    }
}
