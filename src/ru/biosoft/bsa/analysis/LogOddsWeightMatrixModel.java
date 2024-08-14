package ru.biosoft.bsa.analysis;

import java.util.Arrays;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.Alphabet;

public class LogOddsWeightMatrixModel extends WeightMatrixModel
{
    private double[] background;

    public LogOddsWeightMatrixModel(String name, DataCollection<?> origin, FrequencyMatrix frequencyMatrix, double threshold,
            double[] background)
    {
        super(name, origin, threshold);

        int numberOfBasicLetters = frequencyMatrix.getAlphabet().basicSize();
        if( background.length != numberOfBasicLetters )
            throw new IllegalArgumentException("background letter frequencies should be of length " + numberOfBasicLetters);

        double sum = 0;
        for( int i = 0; i < background.length; i++ )
            sum += background[i];
        this.background = new double[background.length];
        for( int i = 0; i < background.length; i++ )
            this.background[i] = background[i] / sum;

        setFrequencyMatrix(frequencyMatrix);
    }

    @Override
    protected void initWeights()
    {
        super.initWeights();

        for( int i = 0; i < weights.length; i++ )
        {
            Alphabet alphabet = getAlphabet();
            byte[] basicCodes = alphabet.basicCodes();
            for( int j = 0; j < basicCodes.length; j++ )
            {
                int code = basicCodes[j];
                weights[i][code] = Math.log(weights[i][code] / background[j]) / Math.log(2);
            }
        }

        updateAmbiguousLetters();
    }

    public double[] getBackground()
    {
        return background;
    }

    public void setBackground(double[] background)
    {
        this.background = background;
        initWeights();
    }

    @Override
    public LogOddsWeightMatrixModel clone(DataCollection origin, String name) throws CloneNotSupportedException
    {
        LogOddsWeightMatrixModel clone = (LogOddsWeightMatrixModel)super.clone(origin, name);
        clone.background = Arrays.copyOf(background, background.length);
        return clone;
    }
}
