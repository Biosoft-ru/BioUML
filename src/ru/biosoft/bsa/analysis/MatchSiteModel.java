package ru.biosoft.bsa.analysis;

import java.beans.PropertyDescriptor;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.Alphabet;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import ru.biosoft.util.bean.StaticDescriptor;

import com.developmentontheedge.beans.DynamicProperty;


/**
 * Match model described in Nucleic Acids Res. 2003 Jul 1;31(13):3576-9.
 * MATCH: A tool for searching transcription factor binding sites in DNA sequences.
 * Kel AE, Go'ssling E, Reuter I, Cheremushkin E, Kel-Margoulis OV, Wingender E.
 * 
 * @author ivan
 *
 */
public class MatchSiteModel extends WeightMatrixModel
{
    public static final String CORE_SCORE_PROPERTY = "coreScore";
    private static final PropertyDescriptor CORE_SCORE_PD = StaticDescriptor.create(CORE_SCORE_PROPERTY);

    public static final int DEFAULT_CORE_LENGHT = 5;

    private double coreCutoff;
    private int coreStart;
    private int coreLength;

    private double minScore;
    private double maxScore;
    private double maxCoreScore;
    private double minCoreScore;

    public MatchSiteModel(String name, DataCollection<?> origin, FrequencyMatrix frequencyMatrix, double cutoff, double coreCutoff)
    {
        this(name, origin, frequencyMatrix, cutoff, coreCutoff, getCoreStart(frequencyMatrix, defaultCoreLength(frequencyMatrix)),
                defaultCoreLength(frequencyMatrix));
    }

    public MatchSiteModel(String name, DataCollection<?> origin, FrequencyMatrix frequencyMatrix, double cutoff, double coreCutoff,
            int coreStart, int coreLength)
    {
        super(name, origin, frequencyMatrix, cutoff);
        if( coreStart < 0 || coreLength <= 0 || coreStart + coreLength > frequencyMatrix.getLength() )
            throw new IllegalArgumentException("Invalid matrix core");
        this.coreStart = coreStart;
        this.coreLength = coreLength;
        this.coreCutoff = coreCutoff;

        minScore = getMinScore();
        maxScore = getMaxScore();
        minCoreScore = getMinScore(coreStart, coreLength);
        maxCoreScore = getMaxScore(coreStart, coreLength);
    }

    @Override
    protected void initWeights()
    {
        super.initWeights();
        Alphabet alphabet = getAlphabet();
        double[] ic = frequencyMatrix.informationContent();
        for( int i = 0; i < weights.length; i++ )
            for( byte code : alphabet.basicCodes() )
            {
                weights[i][code] = ic[i] * weights[i][code];
            }

        updateAmbiguousLetters();
    }

    @Override
    public double getScore(Sequence sequence, int position)
    {
        if( getCoreScore(sequence, position) < coreCutoff )
            return 0;
        if( maxScore - minScore == 0 )
            return 0;
        return ( getScore(sequence, position, 0, getLength()) - minScore ) / ( maxScore - minScore );
    }

    private ThreadLocal<Double> coreScore = new ThreadLocal<>();

    private double getCoreScore(Sequence sequence, int position)
    {
        if( maxCoreScore - minCoreScore == 0 )
            return 0;
        double coreScore = ( getScore(sequence, position, coreStart, coreLength) - minCoreScore ) / ( maxCoreScore - minCoreScore );
        this.coreScore.set(coreScore);
        return coreScore;
    }

    @Override
    public Site constructSite(Sequence sequence, int position, double score)
    {
        Site s = super.constructSite(sequence, position, score);
        s.getProperties().add(new DynamicProperty(CORE_SCORE_PD, Double.class, coreScore.get()));
        return s;
    }

    @PropertyName ( "Core cutoff" )
    @PropertyDescription ( "Minimal weight of the core" )
    public double getCoreCutoff()
    {
        return coreCutoff;
    }

    public void setCoreCutoff(double coreCutoff)
    {
        this.coreCutoff = coreCutoff;
    }

    @PropertyName ( "Core start" )
    @PropertyDescription ( "Start position of the core" )
    public int getCoreStart()
    {
        return coreStart;
    }

    public void setCoreStart(int coreStart)
    {
        this.coreStart = coreStart;
    }

    @PropertyName ( "Core length" )
    @PropertyDescription ( "Length of the core" )
    public int getCoreLength()
    {
        return coreLength;
    }

    public void setCoreLength(int coreLength)
    {
        this.coreLength = coreLength;
    }

    public static int defaultCoreLength(FrequencyMatrix matrix)
    {
        return Math.min(DEFAULT_CORE_LENGHT, matrix.getLength());
    }

    public static int getCoreStart(FrequencyMatrix matrix)
    {
        return getCoreStart(matrix, defaultCoreLength(matrix));
    }

    /**
     * Finds the start of the core, such that the sum of information content in the positions of the core is maximized.
     * @param matrix
     * @param coreLength
     * @return
     */
    public static int getCoreStart(FrequencyMatrix matrix, int coreLength)
    {
        double[] ic = matrix.informationContent();

        int bestPos = -1;
        double bestICSum = -Double.MAX_VALUE;
        for( int pos = 0; pos <= matrix.getLength() - coreLength; pos++ )
        {
            double curICSum = 0;
            for( int i = pos; i < pos + coreLength; i++ )
                curICSum += ic[i];
            if( curICSum > bestICSum )
            {
                bestPos = pos;
                bestICSum = curICSum;
            }
        }
        return bestPos;
    }

    @Override
    public void setThresholdTemplate(String name) throws IllegalArgumentException
    {
        String newThresholds = thresholdTemplates.get(name);
        if( newThresholds == null )
            throw new IllegalArgumentException();
        String[] fields = newThresholds.split("\\|");
        setThreshold(Double.parseDouble(fields[0]));
        setCoreCutoff(Double.parseDouble(fields[1]));
    }
}
