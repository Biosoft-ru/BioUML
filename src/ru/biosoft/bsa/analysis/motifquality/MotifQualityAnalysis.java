package ru.biosoft.bsa.analysis.motifquality;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.LinearSequence;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

@ClassIcon( "resources/motif_quality.gif" )
public class MotifQualityAnalysis extends AnalysisMethodSupport<MotifQualityParameters>
{
    public MotifQualityAnalysis(DataCollection origin, String name)
    {
        super(origin, name, new MotifQualityParameters());
    }

    @Override
    public void validateParameters()
    {
        super.validateParameters();
        checkGreater("numberOfPoints", 1);
        checkGreater("shufflesCount", 0);
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        log.info("motif quality analysis started");
        jobControl.setPreparedness(0);
        
        Sequence[] sequences = getSequences();
        jobControl.setPreparedness(5);
        
        SiteModel model = getSiteModel();

        double[] scores = bestScores(sequences, model);

        Random rng = new Random(parameters.getSeed());

        double[] nullScores = new double[sequences.length * parameters.getShufflesCount()];

        for( int i = 0; i < parameters.getShufflesCount(); i++ )
        {
            jobControl.setPreparedness(5 + (i + 1) * 90 / (1 + parameters.getShufflesCount()));
            for( Sequence s : sequences )
                shuffle(s, rng);
            double[] s = bestScores(sequences, model);
            System.arraycopy(s, 0, nullScores, sequences.length * i, sequences.length);
        }

        double[][] result = rocCurve(scores, nullScores);

        TableDataCollection table = storeOutput(result[0], result[1], result[2]);
        
        jobControl.setPreparedness(100);
        log.info("motif quality analysis finished, result saved to '" + parameters.getOutput() + "'");
        
        return table;
    }
    
    private SiteModel getSiteModel()
    {
        return parameters.getSiteModel().getDataElement(SiteModel.class);
/*        if( model instanceof MatrixOptions )
        {
            MatrixOptions matchModel = (MatrixOptions)model;
            model = new MatrixOptions(null, matchModel.getCompleteName(), 0, matchModel.getCoreCutOff());
        }
        else if( model instanceof IPSSiteModel )
        {
            IPSSiteModel ipsModel = (IPSSiteModel)model;
            model = new IPSSiteModel(ipsModel.getName(), null, ipsModel.getMatrices(), 0, ipsModel.getDistMin(),
                    ipsModel.getWindow(), ipsModel.getStrand());
        }
        return model;
*/
    }

    private Sequence[] getSequences()
    {
        List<Sequence> sequences = new ArrayList<>();
        for( AnnotatedSequence as : parameters.getSequences().getDataCollection(AnnotatedSequence.class) )
        {
            Sequence seq = as.getSequence();
            byte[] bytes = new byte[seq.getLength()];
            for( int i = seq.getStart(); i < bytes.length + seq.getStart(); i++ )
                bytes[i - seq.getStart()] = seq.getLetterAt(i);
            seq = new LinearSequence(bytes, seq.getAlphabet());
            sequences.add(seq);
        }
        return sequences.toArray(new Sequence[sequences.size()]);
    }

    private double[] bestScores(Sequence[] sequences, SiteModel model) throws Exception
    {
        double[] scores = new double[sequences.length];
        for( int i = 0; i < scores.length; i++ )
            scores[i] = bestScore(sequences[i], model);
        return scores;
    }

    private double bestScore(Sequence sequence, SiteModel model) throws Exception
    {
 /*       WritableTrack matches = new TrackImpl("(none)", null);
        if( model instanceof IPSSiteModel )
        {
            IPSSiteModel ipsModel = (IPSSiteModel)model;
            ipsModel.setCritIPS(0);
            SiteSearchAnalysis.predictIPSSites(ipsModel, sequence, matches);
        }
        else if( model instanceof MatrixOptions )
        {
            SequenceAnalysisProfiler profiler = new SequenceAnalysisProfiler();
            DataCollection dc = new VectorDataCollection("(none)", null, new Properties());
            MatrixOptions matchModel = (MatrixOptions)model;
            matchModel.setCutOff(0);
            dc.put(matchModel);
            MatchProfile profile = new MatchProfile(dc);
            SiteSearchAnalysis.SiteSearchAnalysisJobControl siteSearchJobControl = new SiteSearchAnalysis(null, "(none)").new SiteSearchAnalysisJobControl(
                    log, 1, true);
            SiteSearchAnalysis.runThroughSequence(sequence, profile, matches, siteSearchJobControl, profiler);
            SiteSearchAnalysis.runThroughSequence(new SequenceRegion(sequence, sequence.getStart() + sequence.getLength() - 1, sequence
                    .getLength(), true, false), profile, matches, siteSearchJobControl, profiler);
        }

        double bestScore = -Double.MAX_VALUE;
        for( Site site : matches.getAllSites() )
        {
            Object scoreObject = site.getProperties().getValue(Site.SCORE_PROPERTY);
            if( ! ( scoreObject instanceof Double ) )
                continue;
            double score = (Double)scoreObject;
            if( score > bestScore )
                bestScore = score;
        }

        return bestScore;*/
        return model.findBestSite(sequence).getScore();
    }

    private double[] pvalues(double[] values, double[] thresholds)
    {
        double[] pvalues = new double[thresholds.length];

        for( int i = 0; i < thresholds.length; i++ )
            for( double v : values )
                if( v >= thresholds[i] )
                    pvalues[i] += 1;

        for( int i = 0; i < thresholds.length; i++ )
            pvalues[i] /= values.length;

        return pvalues;
    }

    private double[][] rocCurve(double[] scores, double[] nullScores)
    {
        Arrays.sort(scores);
        double[] sensitivityThresholds = getThresholds();
        double[] scoreThresholds = new double[sensitivityThresholds.length];
        for( int i = 0; i < sensitivityThresholds.length; i++ )
        {
            double sensitivityThreshold = sensitivityThresholds[i];
            scoreThresholds[i] = scores[(int)Math.ceil( ( scores.length - 1 ) * ( 1 - sensitivityThreshold ))];
        }
        double[] sensitivity = pvalues(scores, scoreThresholds);
        double[] fdr = pvalues(nullScores, scoreThresholds);
        return new double[][] {scoreThresholds, sensitivity, fdr};
    }

    private double[] getThresholds()
    {
        double[] result = new double[parameters.getNumberOfPoints()];
        double step = ( result.length <= 1 ) ? 0 : ( 1.0 / ( result.length - 1 ) );
        for( int i = 0; i < result.length; i++ )
            result[i] = i * step;
        return result;
    }

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

    private TableDataCollection storeOutput(double[] thresholds, double[] sensitivity, double[] fdr) throws Exception
    {
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(parameters.getOutput());
        table.getColumnModel().addColumn("Threshold", Double.class);
        table.getColumnModel().addColumn("Sensitivity", Double.class);
        table.getColumnModel().addColumn("FDR", Double.class);
        for( int i = 0; i < thresholds.length; i++ )
            TableDataCollectionUtils.addRow(table, String.valueOf(i), new Object[] {thresholds[i], sensitivity[i], fdr[i]}, true);
        table.finalizeAddition();
        parameters.getOutput().save(table);
        return table;
    }
}
