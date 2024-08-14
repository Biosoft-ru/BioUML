
package ru.biosoft.bsa.analysis;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.analysis.Stat;
import ru.biosoft.bsa.Sequence;

/**
 * @author yura
 *
 */
public class LogIPSSiteModel extends IPSSiteModel
{
    public static final double DEFAULT_MULTIPLIER = 2.0;

    public LogIPSSiteModel(String name, DataCollection<?> origin, WeightMatrixModel[] matrices, double critIPS, double multiplierForCommonScoreThreshold)
    {
        super(name, origin, matrices, critIPS);
        setCommonScoreThreshold(multiplierForCommonScoreThreshold);
    }
    
    public LogIPSSiteModel(String name, DataCollection<?> origin, WeightMatrixModel[] matrices, double critIPS, int window, double multiplierForCommonScoreThreshold)
    {
        super(name, origin, matrices, critIPS, IPSSiteModel.DEFAULT_DIST_MIN, window);
        setCommonScoreThreshold(multiplierForCommonScoreThreshold);
    }
    
    private void setCommonScoreThreshold(double multiplierForCommonScoreThreshold)
    {
        for(int i = 0; i < matrices.length; i++ )
        {
            double threshold = matrices[i].getMinScore() * multiplierForCommonScoreThreshold;
            matrices[i].setThreshold(threshold);
        }
    }
    
    @Override
    public double getScore(Sequence sequence, int position)
    {
        double bestScore = -Double.MAX_VALUE;
        lastMatrix.set(0);
        for( int i = 0; i < matrices.length; i++ )
        {
            WeightMatrixModel matrix = matrices[i];
            double score = matrix.getScore(sequence, position + window / 2 - matrix.getLength() / 2);
            if( score < matrix.getThreshold() )
                continue;

            int[] c = getNucleotideFrequencies(sequence, position, position + window - 1);
            double totalCount = c[0] + c[1] + c[2] + c[3];
            if( totalCount == 0 )
                continue;
            double nCounts[] = {c[0] / totalCount, c[1] / totalCount, c[2] / totalCount, c[3] / totalCount};
            double prob = 0.0;
            double x = getScorePValue(matrix, nCounts, score);
            if( x > 0.0 )
                prob = -Math.log10(x);
            if( prob > bestScore )
            {
                bestScore = prob;
                lastMatrix.set(i);
                lastCommonScore.set(score);
            }
        }
        if( bestScore < 0.0 )
            bestScore = 0.0;
        return bestScore;
    }
    
    private double getScorePValue(WeightMatrixModel weightMatrix, double[] nucleotideFrequencies, double score)
    {
        double[][] weights = weightMatrix.getWeights();
        double mean = 0, var = 0;
        for( int code = 0; code < nucleotideFrequencies.length; code++ )
        {
            double curSum = 0, curSumSq = 0;
            for( int position = 0; position < weightMatrix.getLength(); position++ )
            {
                double x = weights[position][code];
                curSum += x;
                curSumSq += x * x;
            }
            mean += curSum * nucleotideFrequencies[code];
            var += curSumSq * nucleotideFrequencies[code];
        }
        for( int position = 0; position < weightMatrix.getLength(); position++ )
        {
            double x = 0;
            for( int code = 0; code < nucleotideFrequencies.length; code++ )
//              x += nucleotideFrequencies[code] * weightMatrix.getFrequency(position, (byte)code);
                x += nucleotideFrequencies[code] * weights[position][code];
            var -= x * x;
        }
        return var <= 0 ? 1 : Stat.normalDistribution(2 * mean - score, mean, var);
    }
}
