/* $Id$ */

package biouml.plugins.machinelearning.distribution_mixture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import biouml.plugins.machinelearning.utils.PrimitiveOperations;
import biouml.plugins.machinelearning.utils.StatUtils.Distributions.NormalDistribution;
import biouml.plugins.machinelearning.utils.StatUtils.UnivariateSample;
import biouml.plugins.machinelearning.utils.UtilsGeneral;

/**
 * @author yura
 * specificParametersOfComponents[i] = double[] meanAndSigma for i-th component;
 *
 */
public class NormalMixture extends DistributionMixture
{
    public NormalMixture(double[] sample, int numberOfComponents, double[] initialProbabilitiesOfComponents, Object[] initialApproximationOfParameters, int maximalNumberOfIterations)
    {
        super(DistributionMixture.MIXTURE_1_NORMAL, sample, numberOfComponents, initialProbabilitiesOfComponents, initialApproximationOfParameters, maximalNumberOfIterations);
    }
    
    @Override
    protected Object[] calculateInitialParameters()
    {
        double[] meanAndSigma = UnivariateSample.getMeanAndSigma(sample), minAndMax = PrimitiveOperations.getMinAndMax(sample);
        double h = (minAndMax[1] - minAndMax[0]) / (double)(numberOfComponents + 1);
        Object[] meansAndSigmas = new Object[numberOfComponents];
        for( int i = 0; i < numberOfComponents; i++ )
            meansAndSigmas[i] = new double[]{(double)(i + 1) * h, meanAndSigma[1]};
        return meansAndSigmas;
    }
    
    @Override
    protected double getDensity(double x, Object specificParametersOfComponent)
    {
        double[] meanAndSigma = (double[])specificParametersOfComponent;
        return NormalDistribution.getDensity(x, meanAndSigma[0], meanAndSigma[1]);
    }
    
    @Override
    protected void calculateSpecificParametersOfComponents()
    {
        int n = probabilitiesPij.length, k = probabilitiesPij[0].length;
        for( int j = 0; j < k; j++ )
        {
            double sum1 = 0.0, sum2 = 0.0, sum3 = 0.0;
            for( int i = 0; i < n; i++ )
            {
                sum1 += probabilitiesPij[i][j];
                double xx = probabilitiesPij[i][j] * sample[i];
                sum2 += xx;
                sum3 += xx * sample[i];
            }
            double mean = sum1 == 0.0 ? Double.NaN : sum2 / sum1;
            specificParametersOfComponents[j] = sum1 == 0.0 ? null : new double[]{mean,  Math.sqrt((sum3 - 2.0 * mean * sum2) / sum1 + mean * mean)};

            // 07.03.22
            if( Double.isNaN(((double[])(specificParametersOfComponents[j]))[0]) || Double.isNaN(((double[])(specificParametersOfComponents[j]))[1]) )
            	specificParametersOfComponents[j] = null;
        }
    }
    
    @Override
    protected int[] determineDegenerateComponents()
    {
        List<Integer> result = new ArrayList<>();
        for( int i = 0; i < specificParametersOfComponents.length; i++ )
            if( (double[])specificParametersOfComponents[i] == null )
                result.add(i);
        if( result.isEmpty() ) return null;
        Collections.sort(result);
        return UtilsGeneral.fromListIntegerToArray(result);
    }
    
    @Override
    protected String[] getNamesOfSpecificParameters()
    {
        return new String[]{"Mean value", "Sigma"};
    }
}
