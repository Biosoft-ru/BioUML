package biouml.plugins.simulation._test;

import biouml.standard.simulation.MathUtils;
import junit.framework.TestCase;

public class MathUtilsTest extends TestCase
{
    public void testInterpolateLinear() throws Exception
    {
        double[] x = new double[] { 1, 2, 3 };
        double[] f = new double[] { 5, 6, 7 };
        
        double[] v;
        
        
        v = MathUtils.interpolateLinear(x, f, new double[] { 2.5 });
        assertEquals(v[0], 6.5);
        
        v = MathUtils.interpolateLinear(x, f, new double[] { 2.2 });
        assertEquals(v[0], 6.2);
        
        v = MathUtils.interpolateLinear(x, f, new double[] { 0 });
        assertEquals(v[0], 5.0);
        
        v = MathUtils.interpolateLinear(x, f, new double[] { 4.5 });
        assertEquals(v[0], 7.0);
        
        v = MathUtils.interpolateLinear(x, f, new double[] { 1 });
        assertEquals(v[0], 5.0);
        
        v = MathUtils.interpolateLinear(x, f, new double[] { 2 });
        assertEquals(v[0], 6.0);
                        
        v = MathUtils.interpolateLinear(x, f, new double[] { 3 });
        assertEquals(v[0], 7.0);
    }

}
