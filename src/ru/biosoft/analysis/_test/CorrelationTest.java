package ru.biosoft.analysis._test;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.analysis.Stat;

public class CorrelationTest extends TestCase
{
    public CorrelationTest(String name)
    {
        super(name);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(CorrelationTest.class.getName());
        suite.addTest(new CorrelationTest("test"));
        return suite;
    }

    /**
     * @throws Exception
     */
    public void test() throws Exception
    {
        double[][] data = new double[11][];
        data[0] = new double[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}; //linear simple
        data[1] = new double[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0}; //constant zero
        data[2] = new double[] {5.34, 5.34, 5.34, 5.34, 5.34, 5.34, 5.34, 5.34, 5.34, 5.34}; //constant double
        data[3] = new double[] { -11.89, -8.9, -5.6, -4.3, -3.2, -2.222, -2, -1, -0.5, -0.30}; //ascending negative
        data[4] = new double[] {100100, 98800, 140000, 56700, 56800, 0.001, 0.004, -20000, -150043, -76535}; //large numbers
        data[5] = new double[] {0.015, 0.016, 0.017, 0.18, 0.019, 0.02, 0.025, 0.027, 0.034345, 0.055}; // nonlinear simple ascending
        data[6] = new double[] {1, 2, 3, 4, 5, 6, 5, 4, 3, 2}; // peak
        data[7] = new double[] { -0.56, -0.1, 10, 109, 200, 50, 40, 30, -10, -100}; //peak2

        data[9] = new double[] {0, 5, 10, 15, 20, 25, 30, 35, 40, 45}; //nonlinear strict dependence
        data[10] = new double[] {0.415616974, 0.006357108, 5.12865E-06, 1.99272E-07, 1.72342E-09, 1.37486E-11, 6.19116E-14, 5.9692E-16,
                4.14682E-18, 1.4239E-20};

        assertEquals(Stat.pearsonCorrelation(data[9], data[10]), -0.5292884609229972);
        assertEquals(Stat.spearmanCorrelationPearson(data[9], data[10]), -1.0);

        assertEquals(Stat.pearsonCorrelation(data[0], data[1]), 0.0);
        assertEquals(Stat.spearmanCorrelationPearson(data[0], data[1]), 0.0);

        assertEquals(Stat.pearsonCorrelation(data[1], data[2]), 1.0);
        assertEquals(Stat.spearmanCorrelationPearson(data[1], data[2]), 1.0);

        assertEquals(Stat.pearsonCorrelation(data[0], data[3]), 0.9306859540182351);
        assertEquals(Stat.spearmanCorrelationPearson(data[0], data[3]), 1.0);

        assertEquals(Stat.pearsonCorrelation(data[6], data[7]), 0.6273178649772595);
        assertEquals(Stat.spearmanCorrelationPearson(data[6], data[7]), 0.8160123484559989);

        assertEquals(Stat.pearsonCorrelation(data[4], data[5]), -0.0389316455570848);
        assertEquals(Stat.spearmanCorrelationPearson(data[4], data[5]), -0.7575757575757576);

        assertEquals(Stat.pearsonCorrelation(data[4], data[3]), -0.7687365583563975);
        assertEquals(Stat.spearmanCorrelationPearson(data[4], data[3]), -0.9272727272727272);

        //testing significance (p-value) calculation
        assertEquals(Stat.pearsonSignificance(0.58, 29), 9.742232122100083E-4);
        assertEquals(Stat.pearsonSignificance(1, 29), 0.0);
        assertEquals(Stat.pearsonSignificance(0, 29), 1.0);
    }
}
