package biouml.plugins.bindingregions.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.IntPredicate;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.plugins.bindingregions.utils.EnsemblUtils.Gap;
import one.util.streamex.DoubleStreamEx;
import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.LinearSequence;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceRegion;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.bsa.analysis.IPSSiteModel;

/**
 * @author yura
 * Site prediction: single site  but several matrices (site models)
 */
public class SitePrediction
{
    public static final String SITE_MODEL_TYPE_PROPERTY = "site model type";
    public static final String SITE_NAME_PROPERTY = "site name";
    public static final String MATRIX_NAMES_PROPERTY = "matrix names";
    public static final String MATRIX_NAME_PROPERTY = "matrix name";
    public static final String THRESHOLDS_PROPERTY = "thresholds";
    public static final String ALL_MATRICES = "All matrices";
    
    private final String siteModelType;
    private final String siteName; // in particular, siteName can be tfClass
    private final SiteModel[] siteModels;
    private final String[] matrixNames;
    
    public SitePrediction(String siteModelType, String siteName, SiteModel[] siteModels, String[] matrixNames)
    {
        this.siteModelType = siteModelType;
        this.siteName = siteName;
        this.siteModels = siteModels;
        this.matrixNames = matrixNames;
    }
    
    public SitePrediction(String siteModelType, Integer window, String siteName, FrequencyMatrix[] matrices, double[] scoreThresholds)
    {
        this(siteModelType, siteName, setSiteModels(siteModelType, window, matrices, scoreThresholds), setMatrixNames(matrices));
    }
    
    private static SiteModel[] setSiteModels(String siteModelType, Integer window, FrequencyMatrix[] matrices, double[] scoreThresholds)
    {
        SiteModel[] siteModels = new SiteModel[matrices.length];
        for( int i = 0; i < matrices.length; i++ )
        {
            double threshold = scoreThresholds != null ? scoreThresholds[i] : 0.0;
            siteModels[i] = SiteModelsComparison.getSiteModel(siteModelType, matrices[i], threshold, window);
        }
        return siteModels;
    }
    
    public String[] getMatrixNames()
    {
        return matrixNames;
    }
    
    private static String[] setMatrixNames(FrequencyMatrix[] matrices)
    {
        return StreamEx.of(matrices).map( FrequencyMatrix::getName ).toArray( String[]::new );
    }

    // TODO : to remove 'findBestSite(Sequence sequence)' with 'findBestScores(Sequence[] sequences)'
    // For site prediction in genome, here it is desirable that sequence is constructed as new SequenceRegion(...);
    // old version
    private Site findBestSite(Sequence sequence)
    {
        return SequenceRegion.withReversed( sequence ).cross( siteModels )
                .mapKeyValue( (seq, siteModel) -> siteModel.findBestSite( seq ) )
                .maxByDouble( Site::getScore ).orElse( null );
    }

    // For site prediction in genome, here it is desirable that sequence is constructed as new SequenceRegion(...);
    // new version
    public Site findBestSite(Sequence sequence, boolean areBothStrands)
    {
        StreamEx<Sequence> stream = areBothStrands ? SequenceRegion.withReversed(sequence) : StreamEx.of(sequence);
        return stream.cross(siteModels).mapKeyValue((seq, siteModel) -> siteModel.findBestSite(seq)).maxByDouble(Site::getScore).orElse(null);
    }
    
    public static Site findBestSite(SiteModel siteModel, Sequence sequence, boolean areBothStrands)
    {
        Site site = siteModel.findBestSite(sequence);
        if( ! areBothStrands ) return site;
        Site siteInReversedSequence = siteModel.findBestSite(SequenceRegion.getReversedSequence(sequence));
        return site.getScore() >= siteInReversedSequence.getScore() ? site : siteInReversedSequence;
    }

    // TODO : to remove 'findBestSite(Sequence sequence)' with 'findBestScores(Sequence[] sequences)'
    // old version
    // for ROC-curve
    private double[] findBestScores(Sequence[] sequences)
    {
        return StreamEx.of( sequences ).map( this::findBestSite ).mapToDouble( Site::getScore ).toArray();
    }

    // new version
    // for ROC-curve
    private double[] findBestScores(Sequence[] sequences, boolean areBothStrands)
    {
        double[] result = new double[sequences.length];
        for( int i = 0; i < sequences.length; i++ )
            result[i] = findBestSite(sequences[i], areBothStrands).getScore();
        return result;
    }
    
    /**
     * For each threshold compute fraction of scores greater or equal to the threshold.
     * @param scores
     * @param thresholds
     * @return array of the same length as thresholds
     */
    private double[] getSensitivities(double[] scores, double[] thresholds)
    {
        double[] result = new double[thresholds.length];
        for( int i = 0; i < thresholds.length; i++ )
            for( double score : scores )
                if( score >= thresholds[i] )
                    result[i] += 1.0 / scores.length;
        return result;
    }
    
    /**
     * Shuffles letters in sequence
     * @param s
     * @param rng
     */
    private void shuffle(Sequence s, Random rng)
    {
        for( int i = s.getLength() + s.getStart() - 1; i > s.getStart(); i-- )
        {
            int j = rng.nextInt(i + 1 - s.getStart()) + s.getStart();
            byte tmp = s.getLetterAt(i);
            s.setLetterAt(i, s.getLetterAt(j));
            s.setLetterAt(j, tmp);
        }
    }
    
    private double[] getFDRs(Sequence[] sequences, boolean areBothStrands, double[] thresholds, int numberOfPermutations, Random rng)
    {
        double[] fdrs = new double[thresholds.length];
        for( int step = 0; step < numberOfPermutations; step++ )
        {
            for(Sequence s : sequences)
                shuffle(s, rng);
            double[] scores = findBestScores(sequences, areBothStrands);
            double[] sensitivities = getSensitivities(scores, thresholds);
            for( int i = 0; i < sensitivities.length; i++ )
                fdrs[i] += sensitivities[i];
        }
        fdrs = MatrixUtils.getProductOfVectorAndScalar(fdrs, 1.0 / numberOfPermutations);
        return fdrs;
    }

    /***
     * default values: usually numberOfPermutations = 10, seed = 0;
     * @param sequences
     * @param numberOfPermutations
     * @param seed
     * @return list of two arrays; 1-st array = x-values (FDRs) for ROC-curve;
     *  2-nd array = y-values (sensitivities) for ROC-curve
     */
    public List<double[]> getROCcurve(Sequence[] sequences, boolean areBothStrands, int numberOfPermutations, long seed)
    {
        Sequence[] sequenceClones = StreamEx.of(sequences).map( LinearSequence::new ).toArray( Sequence[]::new );
//      ModelComparison modelComparison = MotifCompare.compareModels(models, sequencesClone);
        double[] scores = findBestScores(sequenceClones, areBothStrands);
        double[] thresholds = DoubleStreamEx.of( scores ).append( -Double.MAX_VALUE, Double.MAX_VALUE ).sorted().distinct().toArray();
        double[] sensitivities = getSensitivities(scores, thresholds);
        double[] FDRs = getFDRs(sequenceClones, areBothStrands, thresholds, numberOfPermutations, new Random(seed));
        return Arrays.asList( FDRs, sensitivities );
    }
    
    private void findAllSitesByOneSiteModel(SiteModel siteModel, String matrixName, SqlTrack track, List<Sequence> sequences, List<Sequence> reverseSequences, AnalysisJobControl jobControl, int from, int to) throws Exception
    {
        int difference = to - from;
        DataElementPath dep = track.getOrigin().getCompletePath();
        SqlTrack temporaryTrack = SqlTrack.createTrack(dep.getChildPath("temporary"), null);
        for( int i = 0; i < sequences.size(); i++ )
        {
            siteModel.findAllSites(sequences.get(i), temporaryTrack);
            siteModel.findAllSites(reverseSequences.get(i), temporaryTrack);
            if( jobControl != null )
                jobControl.setPreparedness(from + (i + 1) * difference / sequences.size());
        }
        temporaryTrack.finalizeAddition();
        DataCollection<Site> sites = temporaryTrack.getAllSites();
        for( Site site : sites )
        {
            Site newSite = new SiteImpl(null, siteName, siteName, Site.BASIS_PREDICTED, site.getStart(), site.getLength(), Site.PRECISION_NOT_KNOWN, site.getStrand(), site.getSequence(), null);
            DynamicPropertySet dps = newSite.getProperties();
            dps.add(new DynamicProperty(Site.SCORE_PD, Float.class, site.getScore()));
            dps.add(new DynamicProperty(MATRIX_NAME_PROPERTY, String.class, matrixName));
            DynamicPropertySet properties = site.getProperties();
            Float commonScore = (Float)properties.getValue(IPSSiteModel.COMMON_SCORE_PROPERTY);
            if( commonScore != null )
                dps.add(new DynamicProperty(IPSSiteModel.COMMON_SCORE_PROPERTY, Float.class, commonScore));
            track.addSite(newSite);
        }
    }
    
    public void findAllSites(List<Sequence> sequences, SqlTrack track, AnalysisJobControl jobControl, int from, int to) throws Exception
    {
        int h = (to - from) / siteModels.length;
        List<Sequence> reverseSequences = new ArrayList<>();
        for( Sequence sequence : sequences )
            reverseSequences.add(SequenceRegion.getReversedSequence(sequence));
        for( int i = 0; i < siteModels.length; i++ )
            findAllSitesByOneSiteModel(siteModels[i], matrixNames[i], track, sequences, reverseSequences, jobControl, from + i * h, from + (i + 1) * h);
        track.finalizeAddition();
        track.getInfo().getProperties().setProperty(SITE_MODEL_TYPE_PROPERTY, siteModelType);
        track.getInfo().getProperties().setProperty(SITE_NAME_PROPERTY, siteName);
        StringBuilder names = new StringBuilder(), thresholds = new StringBuilder();
        for( int i = 0; i < siteModels.length; i++ )
        {
            if( names.length() > 0 )
            {
                names.append(CisModule.SEPARATOR_BETWEEN_TFCLASSES);
                thresholds.append(CisModule.SEPARATOR_BETWEEN_TFCLASSES);
            }
            names.append(matrixNames[i]);
            thresholds.append(siteModels[i].getThreshold());
        }
        track.getInfo().getProperties().setProperty(MATRIX_NAMES_PROPERTY, names.toString());
        track.getInfo().getProperties().setProperty(THRESHOLDS_PROPERTY, thresholds.toString());
        CollectionFactoryUtils.save(track);
    }
    
    public static Track getShuffledTrack(Track track, DataElementPath pathToOutputs, Map<String, Integer> chromosomeAndLength, Map<String, List<Gap>> chromosomeNameAndGaps) throws Exception
    {
        SqlTrack newTrack = SqlTrack.createTrack(pathToOutputs.getChildPath(track.getName() + "_shuffled"), track);
        Random random = new Random(1);
        for( Site site : track.getAllSites() )
        {
            Sequence sequence = site.getSequence();
            String chromosome = sequence.getName();
            int length = site.getLength();
            int boundary = chromosomeAndLength.get(chromosome) - length;
            List<Gap> gaps = chromosomeNameAndGaps.get(chromosome);
            IntPredicate overlaps = start -> gaps.stream().map( Gap::getInterval )
                    .anyMatch( new Interval( start, start + length - 1 )::intersects );
            int start = IntStreamEx.of( random, 1, boundary ).dropWhile( overlaps ).findFirst().getAsInt();
            newTrack.addSite(new SiteImpl(null, site.getName(), site.getType(), Site.BASIS_PREDICTED, start, length, Site.PRECISION_NOT_KNOWN, site.getStrand(), sequence, null));
        }
        newTrack.finalizeAddition();
        CollectionFactoryUtils.save(newTrack);
        return newTrack;
    }
}
