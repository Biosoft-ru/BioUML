package ru.biosoft.bsa.analysis;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.Alphabet;

public class LogWeightMatrixModel extends WeightMatrixModel
{
    public LogWeightMatrixModel(String name, DataCollection<?> origin, FrequencyMatrix frequencyMatrix, double threshold)
    {
        super(name, origin, frequencyMatrix, threshold);
    }
    
    @Override
    protected void initWeights()
    {
        super.initWeights();
        Alphabet alphabet = getAlphabet();
        for( int i = 0; i < weights.length; i++ )
            for(byte code : alphabet.basicCodes())
            {
                if(weights[i][code] == 0.0)
                    weights[i][code] = Double.MIN_VALUE;
                weights[i][code] = Math.log(weights[i][code]) / Math.log(2);
            }
        updateAmbiguousLetters();
    }
    
}
