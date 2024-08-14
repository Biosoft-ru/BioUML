package ru.biosoft.bsa.analysis;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.ImageElement;
import ru.biosoft.analysis.Util;
import ru.biosoft.bsa.Alphabet;
import ru.biosoft.bsa.BindingElement;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.graphics.View;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * Base class for all SiteModels which are based on single weight matrix.
 * This site model use letter frequencies as weights.
 * @author ivan
 */
public class WeightMatrixModel extends SiteModel implements ImageElement
{
    protected FrequencyMatrix frequencyMatrix;
    protected double[][] weights;
    
    protected double[][] superWeights;
    private volatile boolean initialized;
    protected static final int SUPER_ALPHABET_FACTOR = 6;

    public WeightMatrixModel(String name, DataCollection<?> origin, FrequencyMatrix frequencyMatrix, double threshold)
    {
        super(name, origin, threshold);
        setFrequencyMatrix(frequencyMatrix);
    }
    
    protected WeightMatrixModel(String name, DataCollection<?> origin, double threshold)
    {
        super(name, origin, threshold);
    }

    public double[][] getWeights()
    {
        return weights;
    }

    public FrequencyMatrix getFrequencyMatrix()
    {
        return frequencyMatrix;
    }

    public void setFrequencyMatrix(FrequencyMatrix weightMatrix)
    {
        this.frequencyMatrix = weightMatrix;
        initWeights();
        initialized = false;
        superWeights = null;
    }

    @Override
    public int getLength()
    {
        return frequencyMatrix.getLength()+getAlphabet().codeLength()-1;
    }

    @Override
    public double getScore(Sequence sequence, int position)
    {
        if(getAlphabet().codeLength() > 1) // TODO: optimize di/tri-nucleotide matching as well
            return getScore(sequence, position, 0, frequencyMatrix.getLength());
        return getScoreFast(sequence, position);
    }
    
    @Override
    public Alphabet getAlphabet()
    {
        return frequencyMatrix.getAlphabet();
    }

    @Override
    public BindingElement getBindingElement()
    {
        return frequencyMatrix.getBindingElement();
    }

    public double getMaxScore()
    {
        return getMaxScore(0, weights.length);
    }

    public double getMaxScore(int start, int length)
    {
        double totalMax = 0;
        for(int i = start; i < start + length; i++)
        {
            double max = -Double.MAX_VALUE;
            for(int j = 0; j < weights[i].length; j++)
                max = Math.max(max, weights[i][j]);
            totalMax += max;
        }
        return totalMax;
    }

    @Override
    public double getMinScore()
    {
        return getMinScore(0, weights.length);
    }

    public double getMinScore(int start, int length)
    {
        double totalMin = 0;
        for(int i = start; i < start + length; i++)
        {
            double min = Double.MAX_VALUE;
            for(int j = 0; j < weights[i].length; j++)
                min = Math.min(min, weights[i][j]);
            totalMin += min;
        }
        return totalMin;
    }

    @Override
    public WeightMatrixModel clone(DataCollection origin, String name) throws CloneNotSupportedException
    {
        WeightMatrixModel clone = (WeightMatrixModel)super.clone(origin, name);
        clone.weights = Util.copy(weights);
        clone.superWeights = Util.copy(superWeights);
        clone.initialized = initialized;
        clone.superLetterCache = new ThreadLocal<>();
        return clone;
    }

    @PropertyName("Matrix logo")
    @PropertyDescription("Matrix logo")
    public View getView()
    {
        return frequencyMatrix.getView();
    }

    @Override
    public BufferedImage getImage(Dimension dimension)
    {
        return frequencyMatrix.getImage(dimension);
    }

    @Override
    public Dimension getImageSize()
    {
        return frequencyMatrix.getImageSize();
    }

    @PropertyName("Matrix")
    @PropertyDescription("Matrix")
    public DataElementPath getMatrixPath()
    {
        return DataElementPath.create(frequencyMatrix);
    }

    protected void initWeights()
    {
        weights = new double[frequencyMatrix.getLength()][frequencyMatrix.getAlphabet().size()];
        for( int i = 0; i < weights.length; i++ )
            for( int j = 0; j < weights[i].length; j++ )
                weights[i][j] = frequencyMatrix.getFrequency(i, (byte)j);
        updateAmbiguousLetters();
    }

    protected void initSuperWeights()
    {
        int superLetterCount = 1;
        for(int i = 0; i < SUPER_ALPHABET_FACTOR; i++)
            superLetterCount *= getAlphabet().basicSize();
        superWeights = new double[weights.length / SUPER_ALPHABET_FACTOR + ( weights.length % SUPER_ALPHABET_FACTOR == 0 ? 0 : 1 )][superLetterCount];
        for( int pos = 0; pos < superWeights.length; pos++ )
            for( int superLetter = 0; superLetter < superLetterCount; superLetter++ )
            {
                byte[] letters = lettersOfSuperLetter(superLetter);
                double weightSum = 0;
                for(int i = 0; i < SUPER_ALPHABET_FACTOR; i++)
                {
                    int originalPos = pos * SUPER_ALPHABET_FACTOR + i;
                    if(originalPos >= weights.length)
                        break;
                    weightSum += weights[originalPos][letters[i]];
                }
                superWeights[pos][superLetter] = weightSum;
            }
    }

    private byte[] lettersOfSuperLetter(int superLetter)
    {
        byte[] result = new byte[SUPER_ALPHABET_FACTOR];
        int alphabetSize = getAlphabet().basicSize();
        for(int i = SUPER_ALPHABET_FACTOR - 1; i >= 0; i--)
        {
            result[i] = (byte)(superLetter %  alphabetSize);
            superLetter /= alphabetSize;
        }
        return result;
    }

    /**
     * Update weights for ambiguous letters based on values for basic letters.
     * The weight for ambiguous letter is the mean of values for corresponding basic letters.
     */
    protected void updateAmbiguousLetters()
    {
        Alphabet alphabet = getAlphabet();
        byte codesCount = alphabet.size();
        for( int i = 0; i < weights.length; i++ )
            for( byte code = 0; code < codesCount; code++ )
            {
                double value = 0;
                byte[] basicCodes = alphabet.basicCodes(code);
                for( byte basicCode :  basicCodes)
                    value += weights[i][basicCode];
                weights[i][code] = value / basicCodes.length;
            }
    }

    private class SuperLetterCache
    {
        Sequence sequence;
        int position;
        int[] superLetters = new int[superWeights.length];
        boolean valid;
        
        void encode(Sequence sequence, int position)
        {
            int alphabetSize = getAlphabet().basicSize();
            if( !valid || this.position + 1 != position || this.sequence != sequence)
            {
                for( int i = 0; i < superWeights.length; i++ )
                {
                    int superLetter = 0;
                    for( int j = 0; j < SUPER_ALPHABET_FACTOR; j++ )
                    {
                        byte letter = 0;
                        int originalPos = position + i * SUPER_ALPHABET_FACTOR + j;
                        if( originalPos < sequence.getStart() + sequence.getLength() )
                        {
                            letter = sequence.getLetterCodeAt(originalPos);
                            if( letter >= alphabetSize )//encode only basic letters
                            {
                                valid = false;
                                return;
                            }
                        }
                        superLetter = superLetter * alphabetSize + letter;
                    }
                    superLetters[i] = superLetter;
                }
            }
            else
            {
                for( int i = 0; i < superWeights.length; i++ )
                {
                    byte letter = 0;
                    int originalPos = position + ( i + 1 ) * SUPER_ALPHABET_FACTOR - 1;
                    if( originalPos < sequence.getStart() + sequence.getLength() )
                    {
                        letter = sequence.getLetterCodeAt(originalPos);
                        if( letter >= alphabetSize )//encode only basic letters
                        {
                            valid = false;
                            return;
                        }
                    }
                    superLetters[i] = superLetters[i] * alphabetSize + letter;
                    superLetters[i] %= superWeights[0].length;
                }
            }
            this.position = position;
            this.valid = true;
            this.sequence = sequence;
        }
    }
    
    private ThreadLocal<SuperLetterCache> superLetterCache = new ThreadLocal<>();
    
    private double getScoreFast(Sequence sequence, int position)
    {
        if( !initialized )
        {
            synchronized( this )
            {
                if( !initialized )
                {
                    initSuperWeights();
                    initialized = true;
                }
            }
        }
        SuperLetterCache cached = superLetterCache.get();
        if(cached == null)
        {
            cached = new SuperLetterCache();
            superLetterCache.set(cached);
        }
        cached.encode(sequence, position);
        if(!cached.valid)
            return getScore(sequence, position, 0, getLength());
        
        double result = 0;
        for(int i = 0; i < superWeights.length; i++)
            result += superWeights[i][cached.superLetters[i]];
        return result;
    }
    
    protected double getScore(Sequence sequence, int position, int matrixStart, int length)
    {
        double score = 0;

        for( int i = matrixStart; i < matrixStart + length; i++ )
            score += weights[i][sequence.getLetterCodeAt(position + i, getAlphabet())];

        return score;
    }
}
