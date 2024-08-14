package ru.biosoft.analysis._test;

import java.io.File;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import com.developmentontheedge.application.ApplicationUtils;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import one.util.streamex.StreamEx;
import ru.biosoft.analysis.Util;

public class DensityEstimationTest extends TestCase
{
    
    private static final double ERROR = 1E-6;
    private static String SAMPLE_PATH = "resources/sample.txt";
    
    public DensityEstimationTest(String name)
    {
        super(name);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(DensityEstimationTest.class.getName());
        suite.addTest(new DensityEstimationTest("trivialTest"));
        suite.addTest(new DensityEstimationTest("test"));
        suite.addTest(new DensityEstimationTest("testNormalBandwith"));
        return suite;
    }
    
    
    /**
     * Test data acquired using R package "ks".
     * Script to generate initial data and perform estimation is in resources\densitiEstimation.r
     * Generated initial data is at resources\sample.txt
     */
    public void test() throws Exception
    {
        double[][] data = readSample(SAMPLE_PATH);
        double[][] w = estimatedBandwith;

        for( int i = 0; i < points.length; i++ )
        {
            double val = Util.estimateDensityNormal(points[i], data, w);
            assertEquals(density[i], val, ERROR);
//            System.out.println("[ " + points[i][0] + " , " + points[i][1] + " ]: " + val + " ( " + Math.abs(val - density[i]) + " )");
        }
    }
    
    public void testNormalBandwith() throws Exception
    {
        double[][] data = readSample(SAMPLE_PATH);
        double[][] w = Util.calculateKDEBandwidth(data);
        
        for (int i=0; i<w.length; i++)
            for (int j=0; j<w[0].length; j++)
            {
//                System.out.println(w[i][j] + " \t " + normalBandwith[i][j]);
                assertEquals(normalBandwith[i][j], w[i][j], ERROR);
            }
        
        for( int i = 0; i < points.length; i++ )
        {
            double val = Util.estimateDensityNormal(points[i], data, w);
            assertEquals(densityNormalBandwith[i], val, ERROR);
//            System.out.println("[ " + points[i][0] + " , " + points[i][1] + " ]: " + val + " ( " + Math.abs(val - density[i]) + " )");
        }
    }

    public void trivialTest()
    {
        double[][] data = {{0, 0}, {1, 1}};
        double[][] e = {{1, 0}, {0, 1}};
        double[] p = {0, 0};
        double val = Util.estimateDensityNormal(p, data, e);
        double expected = 0.10885238; 
        assertEquals(expected, val, ERROR);
    }
    
    public double[][] readSample(String fileName) throws Exception
    {
        File f = new File(getClass().getResource(fileName).getFile());
        List<String> list = ApplicationUtils.readAsList(f);
        double[][] result = new double[list.size()][];
            
        for (int i=0; i<list.size(); i++)
            result[i] = ArrayUtils.toPrimitive(StreamEx.of(list.get(i).split("\t")).map(s->Double.valueOf(s)).toArray(Double[]::new));
        
        return result;
    }

    public double[][] points = {{ -1, -1}, { -1, 0}, { -1, 1}, {0, -1}, {0, 0}, {0, 1}, {1, -1}, {1, 0}, {1, 1}};
    public double[][] estimatedBandwith =  {{0.5171236, -0.3709258}, { -0.3709258, 0.4668224}};
    public double[] density =  {0.005766299, 0.031606133, 0.061932335, 0.023187336, 0.051855849, 0.020377619, 0.050627115, 0.029748867, 0.004858827};
    
    public double[][] normalBandwith = {{0.7122256, -0.5674794}, {-0.5674794,  0.6905365}};
    public double[] densityNormalBandwith =  {0.007229047, 0.033101995, 0.058578098, 0.026126191, 0.051682062, 0.022960720, 0.049671115, 0.030440677, 0.006406271};
}
