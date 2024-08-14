package ru.biosoft.analysis._test;

import ru.biosoft.analysis.Stat;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class BinomialTest extends TestCase
{
    public BinomialTest(String name)
    {
        super(name);
    }
    
    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(BinomialTest.class.getName());
        suite.addTest(new BinomialTest("test"));
        suite.addTest(new BinomialTest( "testCumulativeBinomialInv" ));
        return suite;
    }

    /**
     * @throws Exception
     */
    public void test() throws Exception
    {
        assertEquals("C(n,k)", 10, Math.exp(Stat.logCnk(5, 2)), 0.001);
        assertEquals("logBinomialDistribution", 0.180017827, Math.exp(Stat.logBinomialDistribution(100,5,0.05)), 0.00001);
        assertEquals("cumulativeBinomial", 0.435981301, Stat.cumulativeBinomial(100,5,0.05)[0], 0.00001);
        assertEquals("cumulativeBinomial", 0.615999128, Stat.cumulativeBinomial(100,6,0.05)[0], 0.00001);
        assertEquals("cumulativeBinomial", 0.857710979, Stat.cumulativeBinomial(10000,3150,0.31)[0], 0.00001);
        assertEquals("cumulativeBinomial", 7.16572E-06, Stat.cumulativeBinomial(10000,2900,0.31)[0], 0.00001);
        assertEquals("cumulativeBinomial", 0.0, Stat.cumulativeBinomial(10000,7100,0.31)[1]);
        assertEquals("cumulativeBinomialFast", 0.435981301, Stat.cumulativeBinomialFast(100,5,0.05)[0], 0.00001);
        assertEquals("cumulativeBinomialFast", 0.615999128, Stat.cumulativeBinomialFast(100,6,0.05)[0], 0.00001);
        assertEquals("cumulativeBinomialFast", 0.857710979, Stat.cumulativeBinomialFast(10000,3150,0.31)[0], 0.00001);
        assertEquals("cumulativeBinomialFast", 7.16572E-06, Stat.cumulativeBinomialFast(10000,2900,0.31)[0], 0.00001);
        assertEquals("cumulativeBinomialFast", 0.0, Stat.cumulativeBinomialFast(10000,7100,0.31)[1]);
        
        for(int i=0; i<1000; i++)
        {
            assertEquals("Fast==Slow ("+i+")", Stat.cumulativeBinomial(1000,i,0.43)[0], Stat.cumulativeBinomialFast(1000,i,0.43)[0], 0.00001);
            assertEquals("Fast==Slow ("+i+")", Stat.cumulativeBinomial(1000,i,0.35)[1], Stat.cumulativeBinomialFast(1000,i,0.35)[1], 0.00001);
        }
        
        for(int i=1; i<2000; i++)
        {
            assertEquals("Fast==Slow ("+i+")", Stat.cumulativeBinomial(2*i,i,0.17)[0], Stat.cumulativeBinomialFast(2*i,i,0.17)[0], 0.00001);
        }
    }

    public void testCumulativeBinomialInv() throws Exception
    {
        int[] counts = {100000, 500000, 1000000, 5000000, 10000000, 15000000, 20000000, 50000000, 100000000};
        int[] expected = {1, 2, 2, 4, 5, 6, 7, 10, 14};

        for( int i = 0; i < counts.length; i++ )
            assertEquals( "N=" + counts[i], expected[i], Stat.cumulativeBinomialInv( 1 - 1e-5, counts[i], 1.0 / 2.7e7 ) );
    }
}
