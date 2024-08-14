package ru.biosoft.analysis._test;

import ru.biosoft.analysis.Stat;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class HypergeometricTest extends TestCase
{
    public HypergeometricTest(String name)
    {
        super(name);
    }
    
    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(HypergeometricTest.class.getName());
        suite.addTest(new HypergeometricTest("test"));
        return suite;
    }

    /**
     * @throws Exception
     */
    public void test() throws Exception
    {
        assertEquals("logHyperDistribution", 0.147367844, Math.exp(Stat.logHyperDistribution(500,100,50,10)), 0.0001);
        assertEquals("cumulativeHypergeometric", 0.585148364971593, Stat.cumulativeHypergeometric(500,100,50,10)[0], 0.0001);
    }
}
