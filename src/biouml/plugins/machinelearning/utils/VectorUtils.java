/* $Id$ */

package biouml.plugins.machinelearning.utils;

import java.util.logging.Logger;

import org.apache.commons.lang.ArrayUtils;

import ru.biosoft.analysis.Util;

/**
 * @author yura
 *
 */
public class VectorUtils
{
    /****************** Norm : begin  **********************************/
    public static class Norm
    {
        // It is particular case of Minkowski norm : exponent = 2, i.e. it is L2-norm.
        public static double getEuclideanL2(double[] vector)
        {
            return Math.sqrt(PrimitiveOperations.getSumOfSquares(vector));
        }
        
        // It is particular case of Minkowski norm : exponent = 1, i.e. it is L1-norm.
        public static double getManhattanL1(double[] vector)
        {
            double result = 0.0;
            for( double x : vector )
                result += Math.abs(x);
            return result;
        }
        
        // It is Lp-norm.
        public static double getMinkowskiLp(double[] vector, double exponent)
        {
            double result = 0.0;
            for( double x : vector )
                result += Math.pow(Math.abs(x), exponent);
            return Math.pow(result, 1.0 / exponent);
        }
    }
    /****************** Norm : end  ******************************************/
    
    /****************** Distance : start  *************************************/
    public static class Distance
    {
        public static final String DISTANSCE_1_MANHATTAN = "Manhattan";
        public static final String DISTANSCE_2_EUCLIDEAN = "Euclidean";
        public static final String DISTANSCE_3_EUCLIDEAN_SQUARED = "Euclidean squared";
        
        public static double getManhattan(double[] vector1, double[] vector2)
        {
            double result = 0.0;
            for( int i = 0; i < vector1.length; i++ )
                result += Math.abs(vector1[i] - vector2[i]);
            return result;
        }
        
        public static double getEuclidean(double[] vector1, double[] vector2)
        {
            return Math.sqrt(getEuclideanSquared(vector1, vector2));
        }
        
        public static double getEuclideanSquared(double[] vector1, double[] vector2)
        {
            double result = 0.0;
            for( int i = 0; i < vector1.length; i++ )
            {
                double x = vector1[i] - vector2[i];
                result += x * x;
            }
            return result;
        }
        
        public static double getDistance(String distanceType, double[] vector1, double[] vector2)
        {
        	switch(distanceType)
        	{
        	    case DISTANSCE_1_MANHATTAN 		   : return getManhattan(vector1, vector2);
        	    case DISTANSCE_2_EUCLIDEAN		   : return getEuclidean(vector1, vector2);
        	    case DISTANSCE_3_EUCLIDEAN_SQUARED : return getEuclideanSquared(vector1, vector2);
        	}
        	return Double.NaN;
        }
    }
    /****************** Distance : end  *************************************/
    
    /****************** VectorTransformation : start  ***********************/
    public static class VectorTransformation
    {
        public static double[] toZeroAndOneRange(double[] vector)
        {
            double[] result = new double[vector.length];
            double[] minAndMax = PrimitiveOperations.getMinAndMax(vector);
            double diff = minAndMax[1] - minAndMax[0];
            for( int i = 0; i < vector.length; i++ )
                result[i] = (vector[i] - minAndMax[0]) / diff;
            return result;
        }
    }
    /****************** VectorTransformation : end  *************************/
    
    /****************** VectorOperations : begin  ***************************/
    public static class VectorOperations
    {
        public static double getInnerProduct(double[] vector1, double[] vector2)
        {
            double result = 0.0;
            for( int i = 0; i < vector1.length; i++ )
                result += vector1[i] * vector2[i];
            return result;
        }
        
        public static double[] getInverseVector(double[] vector)
        {
            double[] result = new double[vector.length];
            for( int i = 0; i < vector.length; i++ )
            {
                if( vector[i] == 0.0 ) return null;
                result[i] = 1.0 / vector[i];
            }
            return result;
        }
        
        public static double[] getProductOfVectorAndScalar(double[] vector, double scalar)
        {
            double[] result = new double[vector.length];
            for( int i = 0; i < vector.length; i++ )
                result[i] = vector[i] * scalar;
            return result;
        }
        
        public static double[] getSubtractionOfVectors(double[] vector1, double[] vector2)
        {
            double[] result = new double[vector1.length];
            for( int i = 0; i < vector1.length; i++ )
                result[i] = vector1[i] - vector2[i];
            return result;
        }
        
        public static double[] getSum(double[] vector, double scalar)
        {
            double[] result = new double[vector.length];
            for( int i = 0; i < vector.length; i++ )
                result[i] = vector[i] + scalar;
            return result;
        }
        
        public static double[] getSumOfVectors(double[] vector1, double[] vector2)
        {
            double[] result = new double[vector1.length];
            for( int i = 0; i < vector1.length; i++ )
                result[i] = vector1[i] + vector2[i];
            return result;
        }
        
        public static void printVector(Logger log, String name, double[] vector)
        {
            if( name != null )
                log.info(name + " dim = " + vector.length);
            StringBuilder sb = new StringBuilder();
            for( int i = 0; i < vector.length; i++ )
                sb.append(" ").append(i).append(") ").append(vector[i]);
            log.info(sb.toString());
        }
        
        /***
         * 
         * @param vector
         * @return Object[] objects: objects[0] = double[] ranks;
         *                           objects[1] = tieCorrection1 = sum (ki(ki - 1)), where ki is the size of i-th tie;
         *                           objects[2] = tieCorrection2 = sum (ki(ki - 1)(ki + 1));
         */
        public static Object[] getRanksWithTieCorrections(double[] vector)
        {
            double[] preRanks = new double[vector.length], ranks = new double[vector.length];
            for( int i = 0; i < vector.length; i++ )
                preRanks[i] = (double)(i + 1);
            double[] x = ArrayUtils.clone(vector);
            int[] pos = Util.sortHeap(x);
            double tieCorrection1 = 0.0, tieCorrection2 = 0.0;
            for( int i = 0; i < vector.length; i++ )
            {
                int subSize = 1, j = i;
                double sum = preRanks[i];
                while( ++j < vector.length )
                {
                    if( x[i] != x[j] ) break;
                    subSize++;
                    sum += preRanks[j];
                }
                if( subSize == 1 ) continue;
                double average = sum / (double)subSize;
                for( j = 0; j < subSize; j++ )
                    preRanks[i + j] = average;
                double y = (double)subSize * (double)(subSize - 1);
                tieCorrection1 += y;
                tieCorrection2 += y * (double)(subSize + 1);
                i += subSize - 1;
            }
            for( int i = 0; i < vector.length; i++ )
                ranks[pos[i]] = preRanks[i];
            return new Object[]{ranks, tieCorrection1, tieCorrection2};
        }
        
        public static double[] getRanks(double[] vector)
        {
            return (double[])getRanksWithTieCorrections(vector)[0];
        }
    }
    /****************** VectorOperations : end  ***********************/
}
