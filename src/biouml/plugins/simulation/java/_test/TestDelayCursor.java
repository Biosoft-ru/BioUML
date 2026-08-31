package biouml.plugins.simulation.java._test;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Regression test for the monotonic-cursor fast path in
 * {@link biouml.plugins.simulation.java.JavaBaseModel#delay(int, double)}.
 *
 * <p>The cursor is only valid when successive delay() calls request
 * non-decreasing t. Generated models violate this (several delay(..., time-d_i)
 * at the same simulation time, solver re-evaluation, etc.), so the implementation
 * must fall back to binary search when t moves backwards.
 *
 * <p>The history is quadratic (f(t)=t^2), so linear interpolation from the wrong
 * bracketing pair gives a measurably wrong answer. Each assertion checks
 * delay() against the analytically-computed interpolation at the *correct*
 * bracketing times, which is exactly what the original binary-search
 * implementation returns. The final test is a reference comparison against that
 * independent analytic formula over a long non-monotonic sequence.
 */
public class TestDelayCursor extends junit.framework.TestCase
{
    public TestDelayCursor(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestDelayCursor.class.getName());
        suite.addTest(new TestDelayCursor("testBackwardThenForward"));
        suite.addTest(new TestDelayCursor("testMixedOrdering"));
        suite.addTest(new TestDelayCursor("testMonotonicMatchesReference"));
        suite.addTest(new TestDelayCursor("testReferenceComparison"));
        suite.addTest(new TestDelayCursor("testCursorResetOnClear"));
        return suite;
    }

    private static final double TOL = 1e-9;

    /**
     * Analytic reference: linear interpolation of f(t)=t^2 at requested time t,
     * using the correct bracketing recorded (integer) times. This is exactly what
     * the original binary-search delay() returns for t in [0, LAST_TIME].
     */
    private static double referenceDelay(int lastTime, double t)
    {
        int i = (int) Math.ceil(t);          // first recorded index with time >= t
        if (i <= 0) return DelayCursorModel.f(0);
        if (i > lastTime) i = lastTime;       // clamp (t within [0, lastTime])
        double t1 = i, x1 = DelayCursorModel.f(i);
        double t2 = i - 1, x2 = DelayCursorModel.f(i - 1);
        return ((x2 - x1) / (t2 - t1)) * (t - t1) + x1;
    }

    private static DelayCursorModel newModel(int lastTime)
    {
        DelayCursorModel m = new DelayCursorModel();
        for (int t = 0; t <= lastTime; t++)
        {
            m.time = t;
            m.updateHistory(t);
        }
        // Put this.time just past the last recorded time so the "current values"
        // branch (i == history.size()) is well-defined; delay() for t in [0,
        // lastTime] never uses it, but keep it consistent.
        m.time = lastTime;
        return m;
    }

    private static void assertDelay(DelayCursorModel m, int lastTime, double t)
    {
        double expected = referenceDelay(lastTime, t);
        double actual = m.delay(0, t);
        assertEquals("delay(0," + t + ")", expected, actual, TOL);
    }

    /**
     * The exact scenario from the review: after delay(0, 4.5) the cursor advances
     * to the top; a subsequent delay(0, 2.5) must NOT reuse that cursor (it would
     * interpolate from the wrong bracketing pair on a quadratic history) and must
     * fall back to binary search.
     */
    public void testBackwardThenForward()
    {
        DelayCursorModel m = newModel(5);
        assertDelay(m, 5, 4.5);   // forward
        assertDelay(m, 5, 2.5);   // backward -> must fall back (bug returns wrong value)
        assertDelay(m, 5, 4.9);   // forward again (recovery)
    }

    /**
     * A sequence that moves back and forth many times; every result must equal the
     * analytic reference.
     */
    public void testMixedOrdering()
    {
        DelayCursorModel m = newModel(5);
        double[] ts = { 3.5, 1.5, 4.2, 0.7, 2.9, 3.1, 1.1, 4.9, 2.2, 3.7, 0.3, 4.4 };
        for (double t : ts)
        {
            assertDelay(m, 5, t);
        }
    }

    /**
     * Monotonic increasing calls must also be correct (the fast path itself),
     * verifying the fallback addition does not break the common case.
     */
    public void testMonotonicMatchesReference()
    {
        DelayCursorModel m = newModel(5);
        for (double t = 0.25; t <= 4.99; t += 0.25)
        {
            assertDelay(m, 5, t);
        }
    }

    /**
     * Reference comparison: drive a long non-monotonic sequence through the
     * optimized delay() and require exact agreement with the independent analytic
     * interpolation at the correct bracketing times, at every step. Any divergence
     * from the original binary-search semantics under arbitrary call ordering is
     * caught here.
     */
    public void testReferenceComparison()
    {
        int lastTime = 20;
        // Deterministic pseudo-random non-monotonic sequence in [0, lastTime].
        java.util.Random rng = new java.util.Random(42);
        DelayCursorModel m = newModel(lastTime);
        for (int k = 0; k < 5000; k++)
        {
            double t = (rng.nextDouble() * 0.999) * lastTime; // [0, lastTime)
            double expected = referenceDelay(lastTime, t);
            double actual = m.delay(0, t);
            assertEquals("step " + k + " t=" + t, expected, actual, 1e-8);
        }
    }

    /**
     * Regression for cursor-state reset: after clear() the history is rebuilt from
     * scratch. A forward-then-backward sequence on the rebuilt history must still
     * match the reference at every step, i.e. clear() must leave the model in a
     * correct, usable state (no stale timeCache values or cursor state leaking into
     * the smaller rebuilt history).
     */
    public void testCursorResetOnClear()
    {
        DelayCursorModel m = newModel(5);
        assertDelay(m, 5, 4.5);          // advance cursor into the [0,5] history

        // Reset the history to a smaller [0,2] range.
        m.clear();
        m.time = 0;
        m.updateHistory(0);
        m.time = 1;
        m.updateHistory(1);
        m.time = 2;
        m.updateHistory(2);

        // Forward-then-backward on the rebuilt history: every result must match the
        // reference, confirming clear() left the model in a correct state.
        assertDelay(m, 2, 1.5);
        assertDelay(m, 2, 0.4);
        assertDelay(m, 2, 1.9);
    }
}
