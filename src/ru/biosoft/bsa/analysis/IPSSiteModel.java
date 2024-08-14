package ru.biosoft.bsa.analysis;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;

import one.util.streamex.StreamEx;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.analysis.Stat;
import ru.biosoft.bsa.Alphabet;
import ru.biosoft.bsa.Basis;
import ru.biosoft.bsa.BindingElement;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceRegion;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.SiteType;
import ru.biosoft.bsa.SubSequence;
import ru.biosoft.bsa.WritableTrack;
import ru.biosoft.util.bean.StaticDescriptor;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

public class IPSSiteModel extends SiteModel
{
    public static final String MATRIX_MODEL_PROPERTY = "martixModel";
    public static final String COMMON_SCORE_PROPERTY = "commonScore";
    
    protected static final PropertyDescriptor MATRIX_MODEL_PD = StaticDescriptor.create(MATRIX_MODEL_PROPERTY);
    protected static final PropertyDescriptor COMMON_SCORE_PD = StaticDescriptor.create(COMMON_SCORE_PROPERTY);
    
    public static final int DEFAULT_DIST_MIN = 13;
    public static final int DEFAULT_WINDOW = 100;
    public static final double DEFAULT_CRIT_IPS = 4;

    protected WeightMatrixModel[] matrices;

    private int distMin;
    protected int window;

    public IPSSiteModel(String name, DataCollection<?> origin, WeightMatrixModel[] matrices, double critIPS)
    {
        this(name, origin, matrices, critIPS, DEFAULT_DIST_MIN, DEFAULT_WINDOW);
    }

    public IPSSiteModel(String name, DataCollection<?> origin, WeightMatrixModel[] matrices, double critIPS, int distMin, int window)
    {
        super(name, origin, critIPS);
        this.distMin = distMin;
        this.window = window;
        setMatrices(matrices);
    }
    
    public IPSSiteModel(String name, DataCollection<?> origin, FrequencyMatrix[] matrices, double critIPS, int distMin, int window)
    {
        this(name, origin, constructDefaultWeightMatrixModels(matrices), critIPS, distMin, window);
    }
    
    public IPSSiteModel(String name, DataCollection<?> origin, FrequencyMatrix[] matrices, double critIPS)
    {
        this(name, origin, matrices, critIPS, DEFAULT_DIST_MIN, DEFAULT_WINDOW);
    }
    
    private static WeightMatrixModel[] constructDefaultWeightMatrixModels(FrequencyMatrix[] matrices)
    {
        WeightMatrixModel[] models = new WeightMatrixModel[matrices.length];
        for(int i = 0; i < matrices.length; i++)
        {
            models[i] = new WeightMatrixModel(matrices[i].getName() + "_default_model", null, matrices[i], 0);
            models[i].setThreshold(models[i].getMaxScore() / 2);
        }
        return models;
    }

    public DataElementPathSet getMatrixPaths()
    {
        return StreamEx.of(matrices).map(WeightMatrixModel::getMatrixPath).toCollection( DataElementPathSet::new );
    }

    public WeightMatrixModel[] getMatrices()
    {
        return matrices;
    }

    public void setMatrices(WeightMatrixModel[] matrices)
    {
        if( matrices.length == 0 )
            throw new IllegalArgumentException("No weight matrix models to construct IPS site model");
        Alphabet alphabet = matrices[0].getAlphabet();
        for( int i = 1; i < matrices.length; i++ )
            if( matrices[i].getAlphabet() != alphabet )
                throw new IllegalArgumentException("Alphabet of matrix models should be the same for all matrices in IPS site model");
        this.matrices = matrices;
        //TODO: check that binding elements of matrices are same
    }

    public int getDistMin()
    {
        return distMin;
    }

    public void setDistMin(int distMin)
    {
        this.distMin = distMin;
    }

    public int getWindow()
    {
        return window;
    }

    public void setWindow(int window)
    {
        this.window = window;
    }

    @Override
    public int getLength()
    {
        return window;
    }

    @Override
    public Alphabet getAlphabet()
    {
        return matrices[0].getAlphabet();
    }


    @Override
    public BindingElement getBindingElement()
    {
        return matrices[0].getBindingElement();
    }

    protected ThreadLocal<Integer> lastMatrix = new ThreadLocal<>();
    protected ThreadLocal<Double> lastCommonScore = new ThreadLocal<>();

    @Override
    public double getScore(Sequence sequence, int position)
    {
        double bestScore = 10E-25;
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
            double prob = -Math.log10(getScorePValue(matrix.getFrequencyMatrix(), nCounts, score));
            if( !Double.isInfinite(prob) && prob > bestScore )
            {
                bestScore = prob;
                lastMatrix.set(i);
                lastCommonScore.set(score);
            }
        }
        
        return bestScore;
    }
    
    private static class NucleotideFrequecies
    {
        Sequence seq;
        int from;
        int to;
        int[] counts = new int[4];
        
        NucleotideFrequecies(Sequence seq, int from, int to)
        {
            this.seq = seq;
            this.from = from;
            this.to = to;
            countNucleotides();
        }
        
        void countNucleotides()
        {
            for(int i = 0; i < counts.length; i++)
                counts[i] = 0;
            for(int i = from; i <= to; i++)
            {
                int letterCode = seq.getLetterCodeAt(i);
                if(letterCode < 0 || letterCode >= 4)
                    continue;
                counts[letterCode]++;
            }
        }
        
        void moveForward(int newFrom)
        {
            int newTo = newFrom + to - from;
            
            if(newFrom * 2 >= from + to)
            {
                this.to = newTo;
                this.from = newFrom;
                countNucleotides();
                return;
            }
            
            for(int i = from;i < newFrom; i++)
            {
                int letterCode = seq.getLetterCodeAt(i);
                if(letterCode >= 0 && letterCode <= 3)
                    counts[letterCode]--;
            }
            
            for(int i = to + 1; i <= newTo; i++)
            {
                int letterCode = seq.getLetterCodeAt(i);
                if(letterCode >= 0 && letterCode <= 3)
                    counts[letterCode]++;
            }
            
            this.to = newTo;
            this.from = newFrom;
        }
        
    }
    
    private ThreadLocal<NucleotideFrequecies> cachedNucleotideFrequencies = new ThreadLocal<>();
    
    protected int[] getNucleotideFrequencies(Sequence sequence, int from, int to)
    {
        NucleotideFrequecies cached = cachedNucleotideFrequencies.get();
        if( cached != null && sequence == cached.seq
                && cached.from <= from && cached.to <= to
                && to - from == cached.to - cached.from)
            cached.moveForward(from);
        else
            cachedNucleotideFrequencies.set(new NucleotideFrequecies(sequence, from, to));
        return cachedNucleotideFrequencies.get().counts;
    }

    @Override
    public void findAllSites(Sequence sequence, WritableTrack track) throws Exception
    {
        List<Site> sites = new ArrayList<>();
        for( int i = sequence.getStart(); i < sequence.getStart() + sequence.getLength() - getLength() + 1; i++ )
        {
            double score = getScore(sequence, i);
            if( score >= getThreshold() )
            {
                Site newSite = constructSite(sequence, i, score);

                boolean neighbourFound = false;
                int pos = newSite.getInterval().getCenter();
                for( int j = 0; j < sites.size(); j++ )
                {
                    Site s = sites.get(j);
                    if( Math.abs(s.getInterval().getCenter() - pos) >= distMin )
                        continue;
                    neighbourFound = true;
                    if( score > s.getScore() )
                        sites.set(j, newSite);
                }
                if( !neighbourFound )
                    sites.add(newSite);
            }
        }

        for( Site s : sites )
            track.addSite(s);
    }

    private static double getScorePValue(FrequencyMatrix weightMatrix, double[] nucleotideFrequencies, double score)
    {
        double mean = 0, var = 0;
        for( int code = 0; code < nucleotideFrequencies.length; code++ )
        {
            double curSum = 0, curSumSq = 0;
            for( int position = 0; position < weightMatrix.getLength(); position++ )
            {
                double x = weightMatrix.getFrequency(position, (byte)code);
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
                x += nucleotideFrequencies[code] * weightMatrix.getFrequency(position, (byte)code);
            var -= x * x;
        }
        
        return var <= 0 ? 1 : Stat.normalDistribution(2 * mean - score, mean, var);
    }

    @Override
    public Site constructSite(Sequence sequence, int position, double score)
    {
        DynamicPropertySet properties = new DynamicPropertySetAsMap();
        properties.add(new DynamicProperty(Site.SCORE_PD, Double.class, score));
        properties.add(new DynamicProperty(SITE_MODEL_PD, SiteModel.class, this));
        properties.add(new DynamicProperty(MATRIX_MODEL_PD, Integer.class, lastMatrix.get()));
        properties.add(new DynamicProperty(COMMON_SCORE_PD, Double.class, lastCommonScore.get()));
        WeightMatrixModel matrix = matrices[lastMatrix.get()];
        //the name of site come from sequence name?
        Site newSite = new SiteImpl(null, sequence.getName(), SiteType.TYPE_TRANSCRIPTION_FACTOR, Basis.BASIS_PREDICTED, position + window / 2 - matrix.getLength() / 2, matrix.getLength(), Site.PRECISION_EXACTLY,
                Site.STRAND_PLUS, sequence, properties);
        if(sequence instanceof SequenceRegion)
        {
            SubSequence subSequence = new SubSequence(sequence);
            newSite = subSequence.translateSiteBack(newSite);
        }
        return newSite;
    }

    @Override
    public IPSSiteModel clone(DataCollection origin, String name) throws CloneNotSupportedException
    {
        IPSSiteModel clone = (IPSSiteModel)super.clone(origin, name);
        clone.lastMatrix = new ThreadLocal<>();
        clone.lastCommonScore = new ThreadLocal<>();
        clone.cachedNucleotideFrequencies = new ThreadLocal<>();
        if(matrices != null)
        {
            clone.matrices = new WeightMatrixModel[matrices.length];
            for(int i=0; i<matrices.length; i++)
            {
                clone.matrices[i] = matrices[i].clone(null, matrices[i].getName());
            }
        }
        return clone;
    }
}
