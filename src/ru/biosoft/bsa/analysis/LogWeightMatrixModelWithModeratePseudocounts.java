
package ru.biosoft.bsa.analysis;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.Alphabet;

/**
 * @author yura
 * LogWeightMatrixModel used rough Pseudocounts (only to avoid log(0) operation)
 * In comparison with LogWeightMatrixModel,
 * this class LogWeightMatrixModel_withModeratePseudocounts uses more natural Pseudocounts
 * 
 */
public class LogWeightMatrixModelWithModeratePseudocounts extends WeightMatrixModel
{
    public LogWeightMatrixModelWithModeratePseudocounts(String name, DataCollection<?> origin, FrequencyMatrix frequencyMatrix, double threshold)
    {
        super(name, origin, frequencyMatrix, threshold);
    }
    
    @Override
    protected void initWeights()
    {
        super.initWeights();
        Alphabet alphabet = getAlphabet();
        int length = weights.length;
        byte[] basicCodes = alphabet.basicCodes();
        // Look for minimal positive weight
        double minPositive = Double.MAX_VALUE;
        for( int i = 0; i < length; i++ )
            for(byte code : basicCodes )
            {
                if( weights[i][code] > 0.0 && weights[i][code] < minPositive )
                    minPositive = weights[i][code];
            }

        // Calculate log-weights
        for( int i = 0; i < length; i++ )
        {
            for(byte code : basicCodes )
            {
                if( weights[i][code] == 0.0 )
                    weights[i][code] = minPositive;
            }
            double x = 0.0;
            for(byte code : basicCodes )
            {
                x += weights[i][code];
            }
            for(byte code : basicCodes )
            {
                weights[i][code] = Math.log(weights[i][code] / x);
            }
        }
        updateAmbiguousLetters();
    }
}
