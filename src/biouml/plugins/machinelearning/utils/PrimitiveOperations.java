/* $Id$ */

package biouml.plugins.machinelearning.utils;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

/**
 * @author yura
 *
 */
public class PrimitiveOperations
{
    public static double getSum(double[] array)
    {
        double result = 0.0;
        for( double x : array )
            result += x;
        return result;
    }
    
    public static int getSum(int[] array)
    {
        int result = 0;
        for( int x : array )
            result += x;
        return result;
    }
    
    public static double getAverage(double[] array)
    {
        return getSum(array) / (double)array.length;
    }
    
    // Calculate mean for sub-array
    public static double getAverage(double[] array, int startPosition, int n)
    {
        double result = 0.0;
        for( int i = 0; i < n; i++ )
            result += array[startPosition + i];
        return result / (double)n;
    }
    
    public static Object[] getMin(double[] array)
    {
        int index = 0;
        double min = array[0];
        for( int i = 1; i < array.length; i++ )
            if( array[i] < min )
            {
                index = i;
                min = array[i];
            }
        return new Object[]{index, min};
    }
    
    public static Object[] getMax(double[] array)
    {
        int index = 0;
        double max = array[0];
        for( int i = 1; i < array.length; i++ )
            if( array[i] > max )
            {
                index = i;
                max = array[i];
            }
        return new Object[]{index, max};
    }
    
    public static Object[] getMax(int[] array)
    {
        int index = 0, max = array[0];
        for( int i = 1; i < array.length; i++ )
            if( array[i] > max )
            {
                index = i;
                max = array[i];
            }
        return new Object[]{index, max};
    }
    
    public static double[] getMinAndMax(double[] array)
    {
        return new double[]{(double)getMin(array)[1], (double)getMax(array)[1]};
    }
    
    public static double[] getAbs(double[] array)
    {
        double[] result = new double[array.length];
        for( int i = 0; i < array.length; i++ )
            result[i] = Math.abs(array[i]);
        return result;
    }
    
    public static double getGeometricMean(double[] array)
    {
        double result = 1.0;
        for( int i = 0; i < array.length; i++ )
            result *= array[i];
        return Math.pow(result, 1.0 / (double)array.length);
    }
    public static Object[] countFrequencies(String[] array)
    {
        TObjectIntMap<String> map = new TObjectIntHashMap<>();
        for( String s : array )
            map.adjustOrPutValue(s, 1, 1);
        String[] distinctValues = map.keys(new String[map.size()]);
        int[] frequencies = map.values();
        return new Object[]{distinctValues, frequencies};
    }
    
    public static int countSmallValues(double[] array, double threshold)
    {
        int n = 0;
        for( double x : array )
            if( x < threshold )
                n++;
        return n;
    }
    
    public static double[] getSumOfSquaresCentered(double[] array)
    {
        double mean = getAverage(array), sum = 0.0;
        for( double x : array )
        {
           double y = x - mean;
           sum += y * y;
        }
        return new double[]{mean, sum}; 
    }
    
    public static double getSumOfSquares(double[] array)
    {
        double result = 0.0;
        for( double x : array )
            result += x * x;
        return result;
    }
    
    // Calculate sum of squares for sub-sample.
    public static double[] getSumOfSquaresCentered(double[] array, int startPosition, int n)
    {
        double mean = getAverage(array, startPosition, n), sum = 0.0;
        for( int i = 0; i < n; i++ )
        {
           double y = array[startPosition + i] - mean;
           sum += y * y;
        }
        return new double[]{mean, sum}; 
    }
}
