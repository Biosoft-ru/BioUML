package biouml.standard.simulation;

import java.util.Arrays;

import one.util.streamex.DoubleStreamEx;

//moved from biouml.plugins.simulation
public class MathUtils
{
    public static int[] multiBinarySearch(double[] x, double[] points) throws Exception
    {
        // Primitive loop instead of DoubleStreamEx to avoid object allocation per element.
        // Profiler shows this called from SimulationResult.interpolateLinear which is
        // invoked during result post-processing.
        int[] result = new int[points.length];
        for( int i = 0; i < points.length; i++ )
            result[i] = Arrays.binarySearch( x, points[i] );
        return result;
    }
    
    /**
     * 
     * @param x - ordered argument values
     * @param f - function values
     * @param point - points of interpolation
     * @return
     * @throws Exception
     */
    public static double[] interpolateLinear(double[] x, double[] f, double[] points, int[] indexes) throws Exception
    {
        if( x.length != f.length )
        {
            throw new Exception("Error: x.length == " + x.length + " but f.length == " + f.length);
        }
        if( x.length == 0 )
            return null;

        if( x.length == 1 )
            return new double[] {f[0]};

        double[] values = new double[points.length];
        for( int i = 0; i < points.length; i++ )
        {
            int k = indexes[i];
            if( k < 0 )
            {
                int index = -k - 1;
                if( index == 0 )
                {
                    values[i] = f[0];
                }
                else if( index == x.length )
                {
                    values[i] = f[f.length - 1];
                }
                else
                {
                    double x0 = x[index - 1];
                    double f0 = f[index - 1];

                    //Axec: added for correct infinite simulation values handling
                    if( Double.isInfinite(f0) && Double.isInfinite(f[index]) )
                        values[i] = f0;
                    else
                        values[i] = f0 + ( f[index] - f0 ) * ( points[i] - x0 ) / ( x[index] - x0 );
                }
            }
            else
            {
                values[i] = f[k];
            }
        }
        return values;
    }

    /**
     * 
     * @param x - ordered argument values
     * @param f - function values
     * @param point - points of interpolation
     * @return
     * @throws Exception
     */
    public static double[] interpolateLinear(double[] x, double[] f, double[] points) throws Exception
    {
        return interpolateLinear(x, f, points, multiBinarySearch(x, points));
    }
}
