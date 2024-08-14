/* $Id$ */

package biouml.plugins.machinelearning.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import biouml.plugins.machinelearning.classification_models.ClassificationModel;
import biouml.plugins.machinelearning.classification_models.FisherDiscriminantModel;
import biouml.plugins.machinelearning.utils.UtilsGeneral.UtilsForArray;

/**
 * @author yura
 *
 */
public class ModelApplication
{
    public static class DivideSampleByClassification
    {
        public static DataMatrix thresholdDetermination(DataMatrix dataMatrix, double[] sample)
        {
            int nIterations = 10; 
            double[] distinctValues = UtilsGeneral.getDistinctValues(sample);
            UtilsForArray.sortInAscendingOrder(distinctValues);
            int[] boundaryIndices = new int[]{0, distinctValues.length - 1};
            double threshold = distinctValues[(boundaryIndices[0] + boundaryIndices[1]) / 2];
            double tcr = getTcr(dataMatrix, sample, threshold);
            log.info("threshold = " + threshold + " TCR = " + tcr);
            List<double[]> matrix = new ArrayList<>();
            List<String> rowNames = new ArrayList<>();
            rowNames.add(Integer.toString(0));
            matrix.add(new double[]{threshold, tcr, 100.0 * (double)PrimitiveOperations.countSmallValues(sample, threshold) / (double)sample.length});
            for( int i = 1; i < nIterations; i++ )
            {
                int j = (boundaryIndices[0] + boundaryIndices[1]) / 2;
                int[] boundaryIndices1 = new int[]{boundaryIndices[0], j}, boundaryIndices2 = new int[]{j + 1, boundaryIndices[1]};
                double threshold1 = distinctValues[(boundaryIndices1[0] + boundaryIndices1[1]) / 2], threshold2 = distinctValues[(boundaryIndices2[0] + boundaryIndices2[1]) / 2];
                double tcr1 = getTcr(dataMatrix, sample, threshold1), tcr2 = getTcr(dataMatrix, sample, threshold2);
                if( Double.isNaN(tcr1) || Double.isNaN(tcr2) ) break;
                log.info("threshold1 = " + threshold1 + " TCR1 = " + tcr1 + " threshold2 = " + threshold2 + " TCR2 = " + tcr2);
                boundaryIndices = tcr1 < tcr2 ? boundaryIndices2 : boundaryIndices1; 
                double max = Math.max(tcr1, tcr2);
                if( max <= tcr ) continue;
                threshold = tcr1 < tcr2 ? threshold2 : threshold1;
                tcr = max;
                rowNames.add(Integer.toString(rowNames.size()));
                matrix.add(new double[]{threshold, tcr, 100.0 * (double)PrimitiveOperations.countSmallValues(sample, threshold) / (double)sample.length});
            }
            return new DataMatrix(rowNames.toArray(new String[0]), new String[]{"threshold", "True_Classification_Rate", "percentage_of_less_scores"}, matrix.toArray(new double[matrix.size()][]));
        }
        
        // Calculate TCR: True Classification Rate.
        private static double getTcr(DataMatrix dataMatrix, double[] sample, double threshold)
        {
            Object[] additionalInputParameters = new Object[]{MatrixUtils.DEFAULT_MAX_NUMBER_OF_ROTATIONS, MatrixUtils.DEFAULT_EPS_FOR_ROTATIONS, MatrixUtils.DEFAULT_MAX_NUMBER_OF_ITERATIONS_IN_LYUSTERNIK_METHOD, MatrixUtils.DEFAULT_EPS_IN_LYUSTERNIK_METHOD};
            String[] response = new String[sample.length];
            for( int i = 0; i < response.length; i++ )
                response[i] = sample[i] <= threshold ? "Small" : "Large";
            Object[] objects = PrimitiveOperations.countFrequencies(response);
            int[] frequencies = (int[])objects[1];
            int columnNameNumber = dataMatrix.getColumnNames().length;
            if( frequencies[0] <= columnNameNumber || frequencies[1] <= columnNameNumber ) return Double.NaN;
            ClassificationModel classificationModel = new FisherDiscriminantModel("BinaryScore", response, dataMatrix, additionalInputParameters, false);
            int[] predictedIndices = classificationModel.predict(dataMatrix);
            objects = UtilsForArray.getDistinctStringsAndIndices(response);
            DataMatrix trueClassificationRates = ModelUtils.getTrueClassificationRates((int[])objects[1], predictedIndices, (String[])objects[0]);
            double[] array = trueClassificationRates.getColumn("True classification rates");
            return array[array.length - 1];
        }
    }
    
    static Logger log = Logger.getLogger(ModelApplication.class.getName());


}
