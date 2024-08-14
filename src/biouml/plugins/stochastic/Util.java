

package biouml.plugins.stochastic;

import java.util.Set;

/**
 * Util class for stochastic simulations
 * @author axec
 *
 */
public class Util
{

    private Util()
    {
    }

    /**
     * Returns index of least element of <b>x</b> array
     * @param x
     * @return smallest i for which x[i] <= x[j] for all j
     */
    public static int indexOfMin(double[] x)
    {
        double min = x[0];
        int index = 0;
        for( int i = 1; i < x.length; i++ )
        {
            if( x[i] < min )
            {
                min = x[i];
                index = i;
            }
        }
        return index;
    }

    public static int indexOfMin(double[] x, Set<Integer> filter)
    {
        double min = Double.MAX_VALUE;
        int index = 0;
        for( int i: filter)
        {
            if( x[i] <= min )
            {
                min = x[i];
                index = i;
            }
        }
        return index;
    }

    public static int indexOfMin(double[] x, boolean[] filter)
    {
        double min = x[0];
        int index = 0;
        for( int i = 1; i < x.length; i++ )
        {
            if( (x[i] <= min) && (!filter[i]) )
            {
                min = x[i];
                index = i;
            }
        }
        return index;
    }
    
    /**
     * Partial sum
     * @param i - from index
     * @param j - to index
     * @param x - source array
     * @return sum: x[i] + ... +x[j - 1]
     */
    public static double sum(int i, int j, double[] x)
    {
        double res = 0;
        for( int k = i; k < j; k++ )
        {
            res += x[k];
        }
        return res;
    }

    /**
     * Full sum
     * @param x - source array
     * @return sum: x[0] + ... +x[x.length - 1]
     */
    public static double sum(double[] x)
    {
        double res = 0;
        for( double elem : x )
        {
            res += elem;
        }
        return res;
    }

    public static double sum(double[] x, Set<Integer> filter)
    {
        double result = 0;
        for( int i : filter )
            result += x[i];
        return result;
    }
    
    public static double sum(double[] x, boolean[] filter)
    {
        double result = 0;
        for( int i = 0; i < x.length; i++ )
           if (!filter[i]) result += x[i];
        return result;
    }

    /**
     * 
     * @return next reaction time
     */
    public static double calculateTau(double propensity)
    {
        return ( propensity != 0 ) ? -Math.log(Math.random()) / propensity : Double.POSITIVE_INFINITY;
    }


}
