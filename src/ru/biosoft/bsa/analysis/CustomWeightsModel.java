package ru.biosoft.bsa.analysis;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.Alphabet;

/**
 * Site model which uses explicitly provided weights matrix.
 * @author ivan
 */
public class CustomWeightsModel extends WeightMatrixModel
{

    public CustomWeightsModel(String name, DataCollection<?> origin, FrequencyMatrix frequencyMatrix, double threshold, double[][] weights)
    {
        super( name, origin, frequencyMatrix, threshold );
        
        if(frequencyMatrix.getLength() == 0)
            throw new IllegalArgumentException("Empty frequency matrix");
        if(weights.length != frequencyMatrix.getLength())
            throw new IllegalArgumentException("Invalid weights matrix size");
        
        Alphabet alphabet = frequencyMatrix.getAlphabet();
        int letters = weights[0].length;
        if(letters != alphabet.size() && letters != alphabet.basicSize())
            throw new IllegalArgumentException("Invalid weight matrix dimiensions");
        
        this.weights = new double[frequencyMatrix.getLength()][alphabet.size()];
        for(int i = 0; i < weights.length; i++)
        {
            if(letters != weights[i].length)
                throw new IllegalArgumentException("Inconsistent weights matrix");
            for(int j = 0; j < weights[i].length; j++)
                this.weights[i][j] = weights[i][j];
        }
        
        if(letters != alphabet.size())
            updateAmbiguousLetters();
    }

    @Override
    public void setFrequencyMatrix(FrequencyMatrix frequencyMatrix)
    {
        this.frequencyMatrix = frequencyMatrix;
    }
}
