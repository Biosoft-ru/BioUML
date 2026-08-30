package biouml.plugins.simulation.java._test;

import biouml.plugins.simulation.java.JavaBaseModel;

/**
 * Minimal model for exercising {@link JavaBaseModel#delay(int, double)} under
 * arbitrary (including non-monotonic) call ordering.
 *
 * <p>The recorded history value is the quadratic {@code f(t) = t*t}. Because the
 * function is non-linear, linear interpolation between two bracketing recorded
 * times is NOT exact, and -- crucially -- interpolating from the WRONG pair of
 * bracketing times (the failure mode of the monotonic-cursor bug) yields a
 * measurably different value. That makes the differential test below able to
 * catch a cursor that fails to fall back on a backward t.
 *
 * <p>The expected interpolated value at a requested time t (with recorded integer
 * times) is computed analytically in the test from the two bracketing f-values.
 */
public class DelayCursorModel extends JavaBaseModel
{
    public static double f(double t)
    {
        return t * t;
    }

    public double[] getInitialValues() throws Exception
    {
        return new double[] { 0.0 };
    }

    @Override
    public double[] getCurrentHistory()
    {
        return new double[] { f(time) };
    }

    @Override
    public double getPrehistory(double time, int i)
    {
        // constant 0 prehistory before the first recorded time
        return 0.0;
    }
}
