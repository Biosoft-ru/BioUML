package ru.biosoft.analysis._test;

import ru.biosoft.analysis.Stat;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class WilcoxonTest extends TestCase
{
    public WilcoxonTest(String name)
    {
        super(name);
    }
    
    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(WilcoxonTest.class.getName());
        suite.addTest(new WilcoxonTest("test"));
        return suite;
    }

    /**
     * @throws Exception
     */
    public void test() throws Exception
    {
        {
            /*            for(double i=0; i<200; i++)
            {
                System.out.println(i+": "+Stat.wilcoxonDistribution(9, 5, i, true)+": "+Stat.wilcoxonDistribution(9, 5, i, false));
            }*/
            double stat = Stat.wilcoxonTest(new double[]{1,4,3,2.1}, new double[]{2.4,3.5,7.1,5.1,7,6});
            double upTail = Stat.wilcoxonDistributionFast(10, 4, stat, true);
            double downTail = Stat.wilcoxonDistributionFast(10, 4, stat, false);
            stat = Stat.wilcoxonTest(new double[]{1,4,3,2.1,4.2,2,3.1,5,3.2,2.3,0.3,0.8}, new double[]{2.4,3.5,7.1,5.1,1.2,6,7.3,6.1,5.2,4.1,1.5,1.7});
            double upTail1 = Stat.wilcoxonDistributionFast(24, 12, stat, true);
            double downTail1 = Stat.wilcoxonDistributionFast(24, 12, stat, false);
            assertEquals("wilcoxon", 0.9809523809523809, upTail, 0.00001);
            assertEquals("wilcoxon", 0.03333333333333333, downTail, 0.00001);
            assertEquals("wilcoxonFast", 0.9775, upTail1, 0.0005);
            assertEquals("wilcoxonFast", 0.0259, downTail1, 0.0005);
        }

/*        {
            double stat = Stat.wilcoxonTest(new double[]{1,4,3,2.1,4.2,2,3.1,5,3.2,2.3}, new double[]{2.4,3.5,7.1,5.1,7,6,7.3,6.1,5.2,4.1});
            double upTail = Stat.wilcoxonDistribution(20, 10, stat, true);
            double downTail = Stat.wilcoxonDistribution(20, 10, stat, false);
            assertEquals("wilcoxon", 0.435981301, upTail, 0.001);
            assertEquals("wilcoxon", 0.435981301, downTail, 0.001);
        }*/
    }
}
