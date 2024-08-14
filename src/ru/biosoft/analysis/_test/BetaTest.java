package ru.biosoft.analysis._test;

import ru.biosoft.analysis.Stat;

import junit.framework.TestCase;
import junit.framework.TestSuite;

public class BetaTest extends TestCase
{
    public BetaTest(String name)
    {
        super(name);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(BetaTest.class.getName());
        suite.addTest(new BetaTest("test"));
        return suite;
    }

    /**
     * @throws Exception
     */
    public void test() throws Exception
    {
        //test values are taken from www.solvemymath.com
        double accuracy = 1E-10;
        //simple case a=1 b = 2
        assertEquals(Stat.betaDistribution(0.0001, 1, 2)[0], 1.9999E-4, accuracy);
        assertEquals(Stat.betaDistribution(0.01, 1, 2)[0], 0.0199, accuracy);
        assertEquals(Stat.betaDistribution(0.1, 1, 2)[0], 0.19, accuracy);
        assertEquals(Stat.betaDistribution(0.2, 1, 2)[0], 0.36, accuracy);
        assertEquals(Stat.betaDistribution(0.5, 1, 2)[0], 0.75, accuracy);
        assertEquals(Stat.betaDistribution(0.6, 1, 2)[0], 0.84, accuracy);
        assertEquals(Stat.betaDistribution(0.7, 1, 2)[0], 0.91, accuracy);
        assertEquals(Stat.betaDistribution(0.8, 1, 2)[0], 0.96, accuracy);
        assertEquals(Stat.betaDistribution(1.0, 1, 2)[0], 1.0, accuracy);

        //another simple case a=2 b=1
        assertEquals(Stat.betaDistribution(0.0001, 2, 1)[0], 1E-8, accuracy);
        assertEquals(Stat.betaDistribution(0.01, 2, 1)[0], 1E-4, accuracy);
        assertEquals(Stat.betaDistribution(0.1, 2, 1)[0], 0.01, accuracy);
        assertEquals(Stat.betaDistribution(0.2, 2, 1)[0], 0.04, accuracy);
        assertEquals(Stat.betaDistribution(0.5, 2, 1)[0], 0.25, accuracy);
        assertEquals(Stat.betaDistribution(0.6, 2, 1)[0], 0.36, accuracy);
        assertEquals(Stat.betaDistribution(0.7, 2, 1)[0], 0.49, accuracy);
        assertEquals(Stat.betaDistribution(0.8, 2, 1)[0], 0.64, accuracy);
        assertEquals(Stat.betaDistribution(1.0, 2, 1)[0], 1.0, accuracy);

        //simple case a=b=5
        assertEquals(Stat.betaDistribution(0.01, 5, 5)[0], 1.2185368570000075E-8, 1E-16);
        assertEquals(Stat.betaDistribution(0.1, 5, 5)[0], 8.9092E-4, accuracy);
        assertEquals(Stat.betaDistribution(0.2, 5, 5)[0], 0.01958144, accuracy);
        assertEquals(Stat.betaDistribution(0.5, 5, 5)[0], 0.5, accuracy);
        assertEquals(Stat.betaDistribution(0.6, 5, 5)[0], 0.73343232, accuracy);
        assertEquals(Stat.betaDistribution(0.7, 5, 5)[0], 0.9011913399999999, accuracy);
        assertEquals(Stat.betaDistribution(0.8, 5, 5)[0], 0.98041856, accuracy);
        assertEquals(Stat.betaDistribution(1, 5, 5)[0], 1.0, accuracy);
        assertEquals(Stat.betaDistribution(0.0001, 5, 5)[0], 1.259580053996E-18, 1E-30);

        //a = 50 b = 65 larger values
        assertEquals(Stat.betaDistribution(0.0001, 50, 65)[0], 6.54975501386656E-168, 1E-178);
        assertEquals(Stat.betaDistribution(0.1, 50, 65)[0], 9.021936566665459E-21, 1E-30);
        assertEquals(Stat.betaDistribution(0.5, 50, 65)[0], 0.9201192461074964, accuracy);
        assertEquals(Stat.betaDistribution(0.6, 50, 65)[0], 0.9998211858106, accuracy);

        //a=0.1 b =0.01 very small
        assertEquals(Stat.betaDistribution(0.0001, 0.1, 0.01)[0], 0.03624708935924091, accuracy);
        assertEquals(Stat.betaDistribution(0.1, 0.1, 0.01)[0], 0.07300909908426984, accuracy);
        assertEquals(Stat.betaDistribution(0.9, 0.1, 0.01)[0], 0.1094141403373659, accuracy);

        //a=200 b = 400 extremely large
        assertEquals(Stat.betaDistribution(0.3, 200, 400)[0], 0.0397401415834828, accuracy);

        //complicated case a= 5.428 b = 0.54
        assertEquals(Stat.betaDistribution(0.3, 5.428, 0.54)[0], 4.5355735674237907E-4, accuracy);

        // a=1000 b=10000 extremely large
        assertEquals(Stat.betaDistribution(0.1, 1000, 10000)[0], 0.999384704432698, accuracy);

        // a=500 b=10000 extremely large
        assertEquals(Stat.betaDistribution(0.06, 500, 10000, 100)[0], 0.999999985262182, accuracy);
    }
}
