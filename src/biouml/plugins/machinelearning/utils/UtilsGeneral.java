/* $Id$ */

package biouml.plugins.machinelearning.utils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.apache.commons.lang.ArrayUtils;

import ru.biosoft.analysis.Util;
import ru.biosoft.bsa.Interval;
import ru.biosoft.graphics.chart.AxisOptions;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.graphics.chart.ChartOptions;
import ru.biosoft.graphics.chart.ChartSeries;
import ru.biosoft.util.ColorUtils;

/**
 * @author yura
 *
 */

public class UtilsGeneral
{
    public static double[] fromListToArray(List<Double> list)
    {
        int n = list.size();
        double[] result = new double[n];
        for( int i = 0; i < n; i++ )
            result[i] = (double)list.get(i);
        return result;
    }

    public static int[] fromListIntegerToArray(List<Integer> list)
    {
        int n = list.size();
        int[] result = new int[n];
        for( int i = 0; i < n; i++ )
            result[i] = (int)list.get(i);
        return result;
    }
    
    public static double[] fromSetToArray(Set<Double> set)
    {
        double[] result = new double[set.size()];
        int i = 0;
        for( Double x : set )
            result[i++] = (double)x;
        return result;
    }
    
    public static int[] fromSetIntegerToArray(Set<Integer> set)
    {
        int[] result = new int[set.size()];
        int i = 0;
        for( Integer x : set )
            result[i++] = (int)x;
        return result;
    }
    
    public static boolean[][] fromListBooleanToMatrix(List<boolean[]> list)
    {
        int n = list.size();
        boolean[][] result = new boolean[n][];
        for( int i = 0; i < n; i++ )
            result[i] = list.get(i);
        return result;
    }
    
    public static double[] getDistinctValues(double[] array)
    {
        Set<Double> set = new HashSet<>();
        for( double x : array )
            set.add(x);
        return fromSetToArray(set);
    }
    
    public static String[] getDistinctValues(String[] array)
    {
        Set<String> set = new HashSet<>();
        for( String s : array )
            set.add(s);
        return set.toArray(new String[0]);
    }
    
    /****************** UtilsForArray : start ******************/
    public static class UtilsForArray
    {
        public static double[] getConstantArray(int dimension, double scalar)
        {
            double[] result = new double[dimension];
            Arrays.fill(result, scalar);
            return result;
        }
        
        public static boolean[] getConstantArray(int dimension, boolean scalar)
        {
            boolean[] result = new boolean[dimension];
            Arrays.fill(result, scalar);
            return result;
        }
        
        public static char[] getConstantArray(int dimension, char scalar)
        {
            char[] result = new char[dimension];
            Arrays.fill(result, scalar);
            return result;
        }
        
        public static String[] getConstantArray(int dimension, String scalar)
        {
            String[] result = new String[dimension];
            Arrays.fill(result, scalar);
            return result;
        }
        
        public static int[] getConstantArray(int dimension, int scalar)
        {
        	int[] result = new int[dimension];
            Arrays.fill(result, scalar);
            return result;
        }
        
        /***
         * 
         * @param dimension
         * @return array: 0, 1, 2, ... , dimension - 1
         */
        public static int[] getStandardIndices(int dimension)
        {
            int[] result = new int[dimension];
            for( int i = 0; i < dimension; i++ )
                result[i] = i;
            return result;
        }
        
        public static boolean doContainNan(double[] array)
        {
            for( double x : array )
                if( Double.isNaN(x) ) return true;
            return false;
        }
        
        public static boolean doContainNull(String[] array)
        {
            for( String x : array )
                if( x == null ) return true;
            return false;
        }
        
        public static boolean equal(String[] array1, String[] array2)
        {
            if( array1.length != array2.length ) return false;
            for( int i = 0; i < array1.length; i++ )
                if( ! array1[i].equals(array2[i]) ) return false;
            return true;
        }
        
        public static boolean equal(double[] array1, double[] array2)
        {
            if( array1.length != array2.length ) return false;
            for( int i = 0; i < array1.length; i++ )
                if( array1[i] != array2[i] ) return false;
            return true;
        }
        
        public static boolean equal(int[] array1, int[] array2)
        {
            if( array1.length != array2.length ) return false;
            for( int i = 0; i < array1.length; i++ )
                if( array1[i] != array2[i] ) return false;
            return true;
        }
        
        public static boolean equal(char[] array1, char[] array2)
        {
            if( array1.length != array2.length ) return false;
            for( int i = 0; i < array1.length; i++ )
                if( array1[i] != array2[i] ) return false;
            return true;
        }
        
        public static Object[] transformDoubleToObject(double[] array)
        {
            Object[] result = new Object[array.length];
            for( int i = 0; i < array.length; i++ )
                result[i] = array[i];
            return result;
        }
        
        public static Object[] transformStringToObject(String[] array)
        {
            Object[] result = new Object[array.length];
            for( int i = 0; i < array.length; i++ )
                result[i] = array[i];
            return result;
        }
        
        public static double[] transformIntToDouble(int[] array)
        {
            double[] result = new double[array.length];
            for( int i = 0; i < array.length; i++ )
                result[i] = (double)array[i];
            return result;
        }
        
        // Old version
//        public static double[] sortInAscendingOrder(double[] array)
//        {
//            double[] result = new double[array.length];
//            int[] positions = Util.sortHeap(array.clone());
//            for( int i = 0; i < array.length; i++ )
//                result[i] = array[positions[i]];
//            return result;
//        }
        
        public static void sortInAscendingOrder(double[] array)
        {
            Util.sortHeap(array);
        }
        
        public static int[] copySubarray(int[] array, int startPosition, int length)
        {
            int[] result = new int[length];
            for( int i = 0; i < length; i++ )
                result[i] = array[i + startPosition];
            return result; 
        }
        
        public static double[] copySubarray(double[] array, int startPosition, int length)
        {
            double[] result = new double[length];
            for( int i = 0; i < length; i++ )
                result[i] = array[i + startPosition];
            return result;
        }
        
        // array1 is copied into array2; dim(array1) < dim(array2)
        public static void copyIntoArray(double[] array1, double[] array2, int startPosition)
        {
            for( int i = 0; i < array1.length; i++ )
                array2[i + startPosition] = array1[i];
        }
        
        // array1 is copied into array2; dim(array1) < dim(array2)
        public static void copyIntoArray(char[] array1, char[] array2, int startPosition)
        {
            for( int i = 0; i < array1.length; i++ )
                array2[i + startPosition] = array1[i];
        }
        
        // array1 is copied into array2; dim(array1) < dim(array2)
        public static void copyIntoArray(String[] array1, String[] array2, int startPosition)
        {
            for( int i = 0; i < array1.length; i++ )
                array2[i + startPosition] = array1[i];
        }
        
        public static Object[] getDistinctStringsAndIndices(String[] array)
        {
            String[] distinctStrings = getDistinctValues(array);
            int[] indicesOfDistinctStrings = getIndicesOfStrings(array, distinctStrings);
            return new Object[]{distinctStrings, indicesOfDistinctStrings};
        }
        
        public static int[] getIndicesOfStrings(String[] array, String[] distinctStrings)
        {
            int[] indicesOfDistinctStrings = new int[array.length];
            for( int i = 0; i <array.length; i++ )
                indicesOfDistinctStrings[i] = ArrayUtils.indexOf(distinctStrings, array[i]);
            return indicesOfDistinctStrings;
        }
        
        public static int[] getIndicesOfString(String[] array, String givenString)
        {
            List<Integer> list = new ArrayList<>();
            for( int i = 0; i < array.length; i++ )
                if( array[i].equals(givenString) )
                    list.add(i);
            return fromListIntegerToArray(list);
        }

        public static int[] getIndicesOfUnequalElements(int[] array1, int[] array2)
        {
            List<Integer> list = new ArrayList<>();
            for( int i = 0; i < array1.length; i++ )
                if( array1[i] != array2[i] )
                    list.add(i);
            return fromListIntegerToArray(list);
        }
        
        public static String[] transformIntArrayToStringArray(int[] arrayInt, String[] substitutions)
        {
            String[] arrayString = new String[arrayInt.length];
            for(int i = 0; i < arrayInt.length; i++ )
                arrayString[i] = substitutions[arrayInt[i]];
            return arrayString;
        }
        
        // dim(subarraysNames) = dim(array)
        public static Object[] splitIntoSubarrays(String[] subarraysNames, double[] array)
        {
            Map<String, List<Double>> map = new HashMap<>();
            for( int i = 0; i < subarraysNames.length; i++ )
                map.computeIfAbsent(subarraysNames[i], key -> new ArrayList<>()).add(array[i]);
            double [][] samples = new double[map.size()][];
            String[] sampleNames = new String[map.size()];
            int i = 0;
            for( Entry<String, List<Double>> entry : map.entrySet() )
            {
                sampleNames[i] = entry.getKey();
                samples[i++] = UtilsGeneral.fromListToArray(entry.getValue());
            }            
            return new Object[]{sampleNames, samples};
        }

        public static String toString(String[] array, String separator)
        {
            String result = "";
            for( int i = 0; i < array.length; i++ )
            {
                result += array[i];
                if( i < array.length - 1 )
                    result += separator;
            }
            return result;
        }
        
        // Return indices1 and indices2: array1[indices1[i]] == array2[indices2[i]]. 
        public static Object[] compareArrays(String[] array1, String[] array2)
        {
            List<Integer> indices1 = new ArrayList<>(), indices2 = new ArrayList<>();
            for( int i = 0; i < array1.length; i++ )
            {
                int index = ArrayUtils.indexOf(array2, array1[i]);
                if( index >= 0 )
                {
                    indices1.add(i);
                    indices2.add(index);
                }
            }
            return new Object[]{fromListIntegerToArray(indices1), fromListIntegerToArray(indices2)};
        }
    }
    /****************** UtilsForArray : end ******************/
    
    /****************** ChartUtils : start *******************/
    public static class ChartUtils
    {
        public static final String CHART = "chart";

        public static Chart createChart(double[] xValuesForCurve, double[] yValuesForCurve, String curveName, double[] xValuesForCloud, double[] yValuesForCloud, String cloudName, double[] minAndMaxForXandY, String xName, String yName, boolean doRecalculateCurve)
        {
            double[][] xValuesForCurves = xValuesForCurve != null ? new double [][]{xValuesForCurve} : null, yValuesForCurves = new double [][]{yValuesForCurve};
            double[][] xValuesForClouds = xValuesForCloud != null ? new double [][]{xValuesForCloud} : null, yValuesForClouds = new double [][]{yValuesForCloud};
            String[] curveNames = curveName == null ? null : new String[]{curveName}, cloudNames = cloudName == null ? null : new String[]{cloudName};
            return createChart(xValuesForCurves, yValuesForCurves, curveNames, xValuesForClouds, yValuesForClouds, cloudNames, minAndMaxForXandY, xName, yName, doRecalculateCurve);
        }
        
        // Chart with curves and clouds
        // double[] minAndMaxForXandY = new double[]{xMin, xMax, yMin, yMax}
        public static Chart createChart(double[][] xValuesForCurves, double[][] yValuesForCurves, String[] curveNames, double[][] xValuesForClouds, double[][] yValuesForClouds, String[] cloudNames, double[] minAndMaxForXandY, String xName, String yName, boolean doRecalculateCurve)
        {
            Chart chart = new Chart();
            
            // 1. To set x,y-axis
            ChartOptions options = new ChartOptions();
            AxisOptions xAxis = new AxisOptions();
            xAxis.setLabel(xName);
            options.setXAxis(xAxis);
            AxisOptions yAxis = new AxisOptions();
            yAxis.setLabel(yName);
            if( minAndMaxForXandY != null )
            {
                xAxis.setMin(minAndMaxForXandY[0]);
                xAxis.setMax(minAndMaxForXandY[1]);
                yAxis.setMin(minAndMaxForXandY[2]);
                yAxis.setMax(minAndMaxForXandY[3]);
            }
            options.setYAxis(yAxis);
            chart.setOptions(options);
            
            // 2. To draw clouds 
            if( xValuesForClouds != null )
                for( int iCloud = 0; iCloud < xValuesForClouds.length; iCloud++ )
                {
                    double[] x = xValuesForClouds[iCloud], y = yValuesForClouds[iCloud];
                    ChartSeries series = new ChartSeries(x, y);
                    if( cloudNames != null )
                        series.setLabel(cloudNames[iCloud]);
                    Color color = ColorUtils.getDefaultColor(iCloud);
                    series.setColor(color);
                    series.getLines().setShow(false);
                    series.getLines().setShapesVisible(true);
                    chart.addSeries(series);
                }
            if( xValuesForCurves[0] == null ) return chart;
            
            // 3. To draw curves.
            int iClouds = xValuesForClouds == null ? 0 : xValuesForClouds.length;
            for( int iCurve = 0; iCurve < xValuesForCurves.length; iCurve++ )
            {
                double[] x = xValuesForCurves[iCurve], y = yValuesForCurves[iCurve];
                if( x.length <= 1 ) continue;
                // double[][] xAndY = MathUtils.recalculateCurve(x, y);
                double[][] xAndY = doRecalculateCurve ? MathUtils.recalculateCurve(x, y) :  new double[][] {x, y};
                if( xAndY[0].length <= 1 ) continue;
                ChartSeries series = new ChartSeries(xAndY[0], xAndY[1]);
                if( curveNames != null )
                    series.setLabel(curveNames[iCurve]);
                Color color = ColorUtils.getDefaultColor(iCurve + iClouds);
                series.setColor(color);
                chart.addSeries(series);
            }
            return chart;
        }
    }
    /****************** ChartUtils : end ******************/

    /****************** MathUtils : start*****************/
    public static class MathUtils
    {
        public static final double SQRT_OF_2PI = 2.5066282746310007;
        public static final double SQRT_OF_2 = 1.414213562373095048801688724210;

        // Goal of recalculation: to average y-values that correspond to same x-value
        public static double[][] recalculateCurve(double[] x, double[] y)
        {
            Map<Double, List<Double>> map = new TreeMap<>();
            for( int i = 0; i < x.length; i++ )
                map.computeIfAbsent(x[i], key -> new ArrayList<>()).add(y[i]);
            double xNew[] = new double[map.size()], yNew[] = new double[map.size()];
            int i = 0;
            for( Entry<Double, List<Double>> entry : map.entrySet() )
            {
                xNew[i] = entry.getKey();
                yNew[i++] = PrimitiveOperations.getAverage(fromListToArray(entry.getValue()));
            }
            return new double[][]{xNew, yNew};
        }
        
        public static boolean isOdd(int x)
        {
            return (x & 1) == 0 ? false : true;
        }
        
        public static double logisticFunction(double x)
        {
            return 1.0 / (1.0 + Math.exp(x));
        }
        
        public static Interval changeInterval(Interval interval, int minimalLengthOfInterval, int maximalLengthOfInterval)
        {
            int length = interval.getLength();
            if( minimalLengthOfInterval > 0 && length < minimalLengthOfInterval )
            {
                int left = Math.max(1, interval.getCenter() - minimalLengthOfInterval / 2);
                return new Interval(left, left + minimalLengthOfInterval - 1);
            }
            if( maximalLengthOfInterval > 0 && length > maximalLengthOfInterval )
            {
                int left = Math.max(1, interval.getCenter() - maximalLengthOfInterval / 2);
                return new Interval(left, left + maximalLengthOfInterval - 1);
            }
            return interval;
        }
        
        // It was tested
        /***
         * Gamma-function (MAT005); It Returns logarithm (ln) of gamma function.
         * 
         * @param double x > 0
         * @return logarithm (ln) of gamma function
         ***/
        public static double gammaFunctionLn(double x)
        {
            if( x <= 0.0 ) return Double.NaN;
            if( x >= 8.0 )
            {
                double y = 1.0 / x, z = y * y, g = (x - 0.5) * Math.log(x) - x + 0.918938533204672741780329739905 + 8.3333333333316923e-02 * y;
                y *= z;
                g -= 2.77777775657725e-03 * y;
                y *= z;
                g += 7.936431104845e-04 * y;
                y *= z;
                g -= 5.9409561052e-04 * y;
                y *= z;
                g += 7.66345188e-04 * y;
                return g;
            }
            int c = (int)x;
            double y = x - (double)c, y1 = 3786.0105034825718726 + 476.79386050368791516 * y, g = 3786.0105034825724548 + 2077.4597938941873210 * y, yy = y * y;
            y1 -= 867.23098753110299446 * yy;
            g += 893.58180452374981424 * yy;
            yy *= y;
            y1 += 83.550058667919769575 * yy;
            g += 222.11239616801179484 * yy;
            yy *= y;
            y1 += 50.788475328895409737 * yy;
            g += 48.954346227909938052 * yy;
            yy *= y;
            y1 -= 13.400414785781348263 * yy;
            g += 6.1260674503360842988 * yy;
            yy *= y;
            y1 += yy;
            g += 0.7780795856133005759 * yy;
            g /= y1;
            switch( c )
            {
                case 2  : break;
                case 1  : g /= x; break;
                case 0  : g /= x * (1.0 + x); break;
                default : for( int i = 2; i < c; i++ )
                              g *= (double)i + y;
            }
            return Math.log(g);
        }
        
        // It returns logarithm (ln) of Beta function
        public static double betaFunctionLn(double a, double b)
        {
            return gammaFunctionLn(a) + gammaFunctionLn(b) - gammaFunctionLn(a + b);
        }
        
        public static double argthFunction(double x)
        {
            return 0.5 * Math.log((1.0 + x) / (1.0 - x));
        }
        
        public static double thFunction(double x)
        {
            double y = Math.exp(2.0 * x);
            return (y - 1.0) / (y + 1.0);
        }
    }
    /****************** MathUtils : end ******************/
    
    /****************** ListInteger : start **************/
    public static class ListInteger
    {
        List<Integer> list;
        
        public ListInteger(List<Integer> list)
        {
            this.list = list;
        }
        
        public ListInteger(int number)
        {
            list = new ArrayList<>();
            list.add(number);
        }
        
        public List<Integer> getList()
        {
            return list;
        }
        
        /******************* static methods ************/
        // TODO: The input listInteger1 will be changed!!!
        public static ListInteger mergeLists(ListInteger listInteger1, ListInteger listInteger2)
        {
            List<Integer> list1 = listInteger1.getList(), list2 = listInteger2.getList();
            list1.addAll(list2);
            return new ListInteger(list1);
        }
    }
    /****************** ListInteger : end **************/
    
    static Logger log = Logger.getLogger(UtilsGeneral.class.getName());
}
