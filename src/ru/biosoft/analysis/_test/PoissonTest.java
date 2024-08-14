package ru.biosoft.analysis._test;

import ru.biosoft.analysis.Stat;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class PoissonTest extends TestCase
{
    public PoissonTest(String name)
    {
        super(name);
    }
    
    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(PoissonTest.class.getName());
        suite.addTest(new PoissonTest("test"));
        return suite;
    }

    /**
     * @throws Exception
     */
    public void test() throws Exception
    {
        assertEquals("poissonDistribution", 0.615960655, Stat.poissonDistribution(5, 5, true), 0.001);
        assertEquals("poissonDistributionQ", 1-0.615960655, Stat.poissonDistribution(5, 5, false), 0.001);
        assertEquals("poissonDistribution", 0.997160234, Stat.poissonDistribution(10, 4, true), 0.001);
        assertEquals("poissonDistribution", 0.029252688, Stat.poissonDistribution(4, 10, true), 0.001);
        assertEquals("poissonDistributionLargeLambda", 0.508409367, Stat.poissonDistribution(1000, 1000, true), 0.001);
        assertEquals("poissonDistributionLargeLambda", 0.1598711822, Stat.poissonDistribution(9900, 10000, true), 0.001);
    }
}
