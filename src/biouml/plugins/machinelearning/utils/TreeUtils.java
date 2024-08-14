/* $Id$ */

package biouml.plugins.machinelearning.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang.ArrayUtils;

import ru.biosoft.analysis.Util;
import ru.biosoft.util.TextUtil;
import biouml.plugins.machinelearning.utils.PrimitiveOperations;
import biouml.plugins.machinelearning.utils.StatUtils.UnivariateSample;
import biouml.plugins.machinelearning.utils.UtilsGeneral.UtilsForArray;


/**
 * @author yura
 *
 */
public class TreeUtils
{
    /*********************** RegressionNode : start *****************/
    public static class RegressionNode
    {
        private int[] variableIndices;
        private double[] breakValues, response;
        private boolean[] areLess;
        private DataMatrix dataMatrix;
        private double meanResponse;
        
        public RegressionNode(int[] variableIndices, double[] breakValues, boolean[] areLess, DataMatrix dataMatrix, double[] response)
        {
            this.variableIndices = variableIndices;
            this.breakValues = breakValues;
            this.areLess = areLess;
            this.dataMatrix = dataMatrix;
            this.response = response;
            meanResponse = PrimitiveOperations.getAverage(response);
        }
        
        public RegressionNode(int[] variableIndices, double[] breakValues, boolean[] areLess, double meanResponse)
        {
            this.variableIndices = variableIndices;
            this.breakValues = breakValues;
            this.areLess = areLess;
            this.meanResponse = meanResponse;
        }
        
        public Object[] getHistory()
        {
            return new Object[]{variableIndices, breakValues, areLess};
        }

        public DataMatrix getDataMatrix()
        {
            return dataMatrix;
        }
        
        public double[] getResponse()
        {
            return response;
        }
        
        public double predict(double[] variableValues)
        {
            for( int i = 0; i < variableIndices.length; i++ )
            {
                boolean isLess = variableValues[variableIndices[i]] < breakValues[i] ? true : false;
                if( isLess != areLess[i] ) return Double.NaN;

            }
            return meanResponse;
        }
        
        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder(); 
            builder.append("history_length\t").append(String.valueOf(variableIndices.length)).append("\tmean_response\t").append(String.valueOf(meanResponse)).append("\tni\t").append(String.valueOf(response.length));
            for( int i = 0; i < variableIndices.length; i++ )
            {
                String s = areLess[i] ? "true" : "false";
                builder.append("\n").append(String.valueOf(variableIndices[i])).append("\t").append(String.valueOf(breakValues[i])).append("\t").append(s);
            }
            return builder.toString();
        }
    }
    /*********************** RegressionNode : end *****************/
    
    /*********************** RegressionTree : start *****************/
    public static class RegressionTree
    {
        public static RegressionNode[] growTree(DataMatrix dataMatrix, double[] response, int minimalNodeSize, double minimalVariance)
        {
            List<RegressionNode> result = new ArrayList<>(), nodes = new ArrayList<>();
            nodes.add(new RegressionNode(new int[0], new double[0], new boolean[0], dataMatrix, response));
            while( ! nodes.isEmpty() )
            {
                RegressionNode node = nodes.get(0);
                DataMatrix dm = node.getDataMatrix();
                if( dm.getSize() <= minimalNodeSize || UnivariateSample.getMeanAndVariance(node.getResponse())[1] <= minimalVariance )
                {
                    result.add(node);
                    nodes.remove(0);
                    continue;
                }
                
                // 1. To find breakValue.
                double[] resp = node.getResponse();
                double[][] matrix = dm.getMatrix();
                Object[] objects = findBreakPoint(matrix, resp);
                int columnIndex = (int)objects[0];
                double  breakValue = (double)objects[1];
                if( Double.isNaN(breakValue) )
                {
                    result.add(node);
                    nodes.remove(0);
                    continue;
                }
                double[] column = MatrixUtils.getColumn(matrix, columnIndex);
                
                // 2. Calculate indicesForLess and split dataMatrix and response.
                List<Integer> list = new ArrayList<>();
                for( int i = 0; i < column.length; i++ )
                    if( column[i] < breakValue )
                        list.add(i);
                int[] indicesForLess = UtilsGeneral.fromListIntegerToArray(list);
                if( indicesForLess.length == 0 || indicesForLess.length == dm.getSize() )
                {
                    result.add(node);
                    nodes.remove(0);
                    continue;
                }
                objects = DataMatrix.splitRowWise(dm, resp, null, indicesForLess);
                DataMatrix dm1 = (DataMatrix)objects[0], dm2 = (DataMatrix)objects[1];
                double[] response1 = (double[])objects[2], response2 = (double[])objects[3];
                
                // 3. Split node into 2 nodes.
                objects = node.getHistory();
                int[] variableIndices = ArrayUtils.add((int[])objects[0], columnIndex);
                double[] breakValues = ArrayUtils.add((double[])objects[1], breakValue);
                boolean[] areLess = (boolean[])objects[2];
                nodes.add(new RegressionNode(variableIndices, breakValues, ArrayUtils.add(areLess, true), dm1, response1));
                nodes.add(new RegressionNode(variableIndices, breakValues, ArrayUtils.add(areLess, false), dm2, response2));
                nodes.remove(0);
            }
            return result.toArray(new RegressionNode[0]);
        }
        
        private static Object[] findBreakPoint(double[][] matrix, double[] response)
        {
            int columnIndex = -1;
            double breakValue = Double.NaN, residualSumOfSquares = Double.MAX_VALUE;
            for( int i = 0; i < matrix[0].length; i++ )
            {
                double[] column = MatrixUtils.getColumn(matrix, i);
                if( UtilsGeneral.getDistinctValues(column).length < 2 ) continue;
                
                // 1. Calculate columnSorted, responseReOdered and isCheckPoint.
                double[] columnSorted = column.clone();
                int[] positions = Util.sortHeap(columnSorted);
                double[] responseReOdered = new double[response.length];
                for( int j = 0; j < column.length; j++ )
                    responseReOdered[j] = response[positions[j]];
                boolean[] isCheckPoint = new boolean[column.length];
                isCheckPoint[isCheckPoint.length - 1] = true;
                if( isCheckPoint.length > 2 )
                    for( int j = 1; j < isCheckPoint.length - 1; j++ )
                        if( columnSorted[j] < columnSorted[j + 1] )
                            isCheckPoint[j] = true;
                
                // 2. Look for minimal residual sum of squares.
                for( int j = 1; j < column.length; j++ )
                {
                    if( ! isCheckPoint[j] ) continue;
                    double residualSumOfSquares1 = PrimitiveOperations.getSumOfSquaresCentered(responseReOdered, 0, j)[1];
                    double residualSumOfSquares2 = PrimitiveOperations.getSumOfSquaresCentered(responseReOdered, j, column.length - j)[1];
                    double x = residualSumOfSquares1 + residualSumOfSquares2;
                    if( x >= residualSumOfSquares ) continue;
                    residualSumOfSquares = x;
                    columnIndex = i;
                    breakValue = columnSorted[j];
                }
            }
            return new Object[]{columnIndex, breakValue};
        }

        public static double[] predict(double[][] matrix, RegressionNode[] leaves)
        
        {
            double[] result = UtilsForArray.getConstantArray(matrix.length, Double.NaN);
            for( int i = 0; i < matrix.length; i++ )
                for( RegressionNode leaf : leaves )
                {
                    double x = leaf.predict(matrix[i]);
                    if( ! Double.isNaN(x) )
                    {
                        result[i] = x;
                        break;
                    }
                }
            return result;
        }
        
        public static String writeToString(RegressionNode[] nodes)
        {
            StringBuilder builder = new StringBuilder(); 
            builder.append("nodes_number\t").append(String.valueOf(nodes.length));
            for( RegressionNode node : nodes )
                builder.append("\n").append(node.toString());
            return builder.toString();
        }
        
        public static RegressionNode[] readNodesInLines(String[] lines)
        {
            String[] tokens = TextUtil.split(lines[0], '\t');
            int n = Integer.parseInt(tokens[1]), index = 0;
            RegressionNode[] result = new RegressionNode[n];
            for( int i = 0; i < n; i++ )
            {
                tokens = TextUtil.split(lines[++index], '\t');
                int nodeSize = Integer.parseInt(tokens[1]);
                double meanResponse = Double.parseDouble(tokens[3]);
                int[] variableIndices = new int[nodeSize];
                double[] breakValues = new double[nodeSize];
                boolean[] areLess = new boolean[nodeSize];
                for( int j = 0; j < nodeSize; j++ )
                {
                    tokens = TextUtil.split(lines[++index], '\t');
                    variableIndices[j] = Integer.parseInt(tokens[0]);
                    breakValues[j] = Double.parseDouble(tokens[1]);
                    areLess[j] = tokens[2].equals("true") ? true : false;
                }
                result[i] = new RegressionNode(variableIndices, breakValues, areLess, meanResponse);
            }
            return result;
        }
    }
    /*********************** RegressionTree : end *****************/
    
    static Logger log = Logger.getLogger(TreeUtils.class.getName());
}
