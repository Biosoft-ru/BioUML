/* $Id$ */

package biouml.plugins.machinelearning.utils;

import java.util.logging.Logger;

import biouml.plugins.machinelearning.utils.UtilsGeneral.UtilsForArray;
import biouml.plugins.machinelearning.utils.VectorUtils.Distance;

/**
 * @author yura
 *
 */
public class StochasticProcesses
{
    public static class MarkovChains
    {
        // Return Object[] objects : object[0] = double[] probabilities, object[1] = int (number of processed iterations).
        public static Object[] getStationaryProbabilities(double[][] transitionMatrix, double[] initialApproximation, int maxNumberOfIterations, double eps)
        {
            double epsNew = eps / (double)transitionMatrix.length;
            double[] oldProbabilities = initialApproximation != null ? initialApproximation : UtilsForArray.getConstantArray(transitionMatrix.length, 1.0 / (double)transitionMatrix.length);
            for( int i = 0; i < maxNumberOfIterations; i++ )
            {
                double[] probabilities = MatrixUtils.getProductOfTransposedVectorAndRectangularMatrix(transitionMatrix, oldProbabilities);
                double distance = Distance.getManhattan(probabilities, oldProbabilities);
                log.info("i = " + i + " distance = " + distance);
                if( distance < epsNew ) return new Object[]{probabilities, i + 1};
                oldProbabilities = probabilities;

                // TODO: temp
                //VectorOperations.printVector(log, "i = " + Integer.toString(i) + " probabilities", probabilities);

            }
            return new Object[]{oldProbabilities, maxNumberOfIterations};
        }
        
        // The goal of modification is to ensure the existence of a unique stationary distribution.
        // The tuningParameter must be small.
        public static void modifyTransitionMatrix(double[][] transitionMatrix, double tuningParameter)
        {
            double x = 1.0 - tuningParameter, xx = tuningParameter / (double)transitionMatrix.length;
            for( int i = 0; i < transitionMatrix.length; i++ )
                for( int j = 0; j < transitionMatrix.length; j++ )
                    transitionMatrix[i][j] = x * transitionMatrix[i][j] + xx;
        }
    }
    
    private static Logger log = Logger.getLogger(StochasticProcesses.class.getName());
}
