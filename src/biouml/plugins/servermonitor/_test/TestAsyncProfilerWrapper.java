package biouml.plugins.servermonitor._test;

import biouml.plugins.servermonitor.AsyncProfilerWrapper;
import biouml.plugins.servermonitor.ProfilerResult;
import biouml.plugins.servermonitor.ServerMonitorConfig;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests {@link AsyncProfilerWrapper}'s child-process handling with a fake
 * profiler binary (a shell script standing in for asprof), without touching a
 * real JVM or the profiler agent:
 *
 * <ul>
 *   <li>a clean run exits promptly and is reported successful;</li>
 *   <li>when the "profiler" hangs past the timeout, the wrapper bounds the
 *       wait, destroys the child, and does NOT block the caller indefinitely
 *       — the regression behind the ict "stop timed out after 30s" spam,
 *       where a hung process held the output pipe and blocked the reader;</li>
 *   <li>a non-zero exit is reported as a failed result;</li>
 *   <li>the pre-run guard skips {@code asprof stop} entirely when no session
 *       is active, so an idle monitoring cycle pays no stop wait (the ict
 *       log-spam amplifier: every 60s cycle ran a stop that hung 30s).</li>
 * </ul>
 *
 * <p>The fake binary's script location selects its behavior via argv[1]:
 * {@code stop} → sleep forever (the agent-stuck symptom); {@code -d N ...}
 * → sleep 0.2s then exit 0 and create the output file (a clean run);
 * {@code -c 1} → sleep 0.2s then exit 1 (a failed run).
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
     * the child is destroyed, and a failed result is returned. We use the
     * test-only timeout override to shrink the bound to a few seconds so the
     * hang path is exercised quickly; the property under test is that the
     * bound is honored and the caller is not blocked indefinitely (the old
     * failure mode was a blocked output reader / an unbounded stop wait).
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
        assertTrue("wrapper blocked too long: " + elapsedMs + "ms "
                + "(should be bounded by ~3s test timeout, not hang)",
                elapsedMs < 15000);
        // The hung child must have been destroyed, not left running.
        assertEquals("expected stopped status after timeout",
                "stopped", wrapper.getProfileStatus());
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
     * {@code asprof stop} at all. The fake binary records every invocation in
     * a marker file; the idle path writes nothing there, while a run always
     * writes to the output file — so the absence of the marker (with the
     * output present) proves stop was skipped.
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
     * The fake profiler script. Its first argument distinguishes a {@code stop}
     * invocation (sleeps forever — simulating the stuck agent) from a run
     * (first arg {@code -d} or {@code -c}):
     * <pre>
     *   stop [pid]      → record marker, sleep 600 (agent stuck)
     *   -d N -f F ...   → sleep 0.2, create F, exit 0          (mode: clean)
     *   -d N -f F ...   → sleep 0.2, exit 1                    (mode: fail)
     *   -c 1 [pid]      → sleep 0.2, exit 1                    (any mode)
     * </pre>
     */
    private String fakeProfilerScript(String mode)
    {
        return "#!/bin/sh\n"
            + "MARKER=\"" + tmpDir.getAbsolutePath() + "/stop_invoked\"\n"
            + "if [ \"$1\" = \"stop\" ]; then\n"
            + "  touch \"$MARKER\"\n"
            + "  sleep 600\n"
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
            + "case \"" + mode + "\" in\n"
            + "  hung) sleep 600 ;;\n"
            + "  clean) [ -n \"$F\" ] && touch \"$F\"; exit 0 ;;\n"
            + "  fail) exit 1 ;;\n"
            + "esac\n";
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
