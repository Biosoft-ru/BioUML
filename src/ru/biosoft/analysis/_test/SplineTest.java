package ru.biosoft.analysis._test;

import ru.biosoft.analysis.Util.CubicSpline;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class SplineTest extends TestCase
{
    public SplineTest(String name)
    {
        super(name);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(SplineTest.class.getName());
        suite.addTest(new SplineTest("testCubicSpline"));
//        suite.addTest(new SplineTest("splineDemo"));
        return suite;
    }

    private static final double ACCURACY = 1E-13;
    /**
     * @throws Exception
     */
    public void testCubicSpline() throws Exception
    {
        double[] x = new double[] {0, 10, 15, 16, 19, 21, 21.5, 30, 40, 42, 50};
        double[] y = new double[] {2, 3, 4, 9, 10, 21, 22, 23, 9, 10, 10};

        CubicSpline spline = new CubicSpline(x, y);

        for( int i = 0; i < x.length; i++ )
        {
            assertEquals(spline.getValue(x[i]), y[i], ACCURACY);
        }
        assertEquals(spline.getValue(12), -0.18024778357360616, ACCURACY);
    } 
}
