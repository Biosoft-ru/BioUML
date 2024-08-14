package ru.biosoft.bsa.analysis.motifcompare;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.LinearSequence;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceRegion;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.analysis.JavaScriptBSA;
import ru.biosoft.graphics.chart.AxisOptions;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.graphics.chart.ChartOptions;
import ru.biosoft.graphics.chart.ChartSeries;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

public class MotifCompare extends AnalysisMethodSupport<MotifCompareParameters>
{
    public MotifCompare(DataCollection<?> origin, String name)
    {
        super(origin, name, JavaScriptBSA.class, new MotifCompareParameters());
    }
    
    @Override
    protected AnalysisJobControl createJobControl()
    {
        return new MotifCompareJobControl();
    }

    @Override
    public void validateParameters()
    {
        checkOutputs();
        checkNotEmpty("siteModels");
        checkNotEmpty("sequences");
        checkGreater("numberOfPermutations", 1);
    }

    @Override
    public TableDataCollection justAnalyzeAndPut() throws Exception
    {
        ( (MotifCompareJobControl)jobControl ).setStepsCount(parameters.getNumberOfPermutations() + 2);

        Sequence[] sequences = getSequences();
        SiteModel[] models = getModels();

        ModelComparison comparison = compareModels(models, sequences, parameters.getNumberOfPermutations(), parameters.getSeed(),
                parameters.getBackgroundSequences(), (MotifCompareJobControl)jobControl);

        TableDataCollection result = storeOutput(models, comparison.sensitivities, comparison.fprs);
        if( parameters.getModelFDR() >= 0 && parameters.getModelFDR() <= 1 )
            setModelThresholds(models, comparison.thresholds, comparison.fprs);
        return result;
    }
    
    public static ModelComparison compareModels(SiteModel[] models, Sequence[] sequences)
    {
        return compareModels(models, sequences, 10, 0, null, null);
    }
    
    public static ModelComparison compareModels(SiteModel[] models, Sequence[] sequences, int numberOfPermutations, long seed, DataElementPath bgSeqPath, MotifCompareJobControl jobControl)
    {
        double[][] thresholds = new double[models.length][];
        double[][] sensitivities = new double[models.length][];
        for( int i = 0; i < models.length; i++ )
        {
            double[] scores = getBestScores(sequences, models[i]);
            Arrays.sort(scores);
            double[] t = unique(scores);
            thresholds[i] = new double[t.length + 2];
            System.arraycopy(t, 0, thresholds[i], 1, t.length);
            thresholds[i][t.length] = Double.MAX_VALUE;
            
            // Ivan!! it seems likely that better to do as : thresholds[i][t.length +1] = Double.MAX_VALUE;
            thresholds[i][0] = -Double.MAX_VALUE;
            sensitivities[i] = getSensitivity(scores, thresholds[i]);
        }

        if( jobControl != null )
        {
            jobControl.stepCompleted();
            if( jobControl.isStopped() )
                return null;
        }

        //False Positive Rates
        double[][] fprs = getFPRs(sequences, models, thresholds, numberOfPermutations, new Random(seed), bgSeqPath, jobControl);

        return new ModelComparison(thresholds, sensitivities, fprs);
    }

    private Chart getChartForROCCurve(double[] sensitivity, double[] fpr)
    {
        ChartSeries series = new ChartSeries(samplePoints(fpr, 100), samplePoints(sensitivity, 100));
        series.setLabel("ROC curve");
        series.setColor(Color.BLUE);

        ChartOptions options = new ChartOptions();
        AxisOptions xAxis = new AxisOptions();
        xAxis.setLabel("False Positive Rate");
        options.setXAxis(xAxis);
        AxisOptions yAxis = new AxisOptions();
        yAxis.setLabel("True Positive Rate (sensitivity)");
        options.setYAxis(yAxis);

        Chart chart = new Chart();
        chart.setOptions(options);
        chart.addSeries(series);
        return chart;
    }

    private double[] samplePoints(double[] x, int maxPoints)
    {
        if( x.length <= maxPoints )
            return x;
        if( maxPoints == 0 )
            return new double[0];
        double[] result = new double[maxPoints];
        for( int i = 0; i < maxPoints; i++ )
            result[i] = x[i * x.length / maxPoints];
        result[maxPoints - 1] = x[x.length - 1];
        return result;
    }


    private double areaUnderROCCurve(double[] sensitivity, double[] fpr)
    {
        double area = 0;
        for( int i = 1; i < sensitivity.length; i++ )
            area += ( sensitivity[i - 1] + sensitivity[i] ) * ( fpr[i - 1] - fpr[i] ) / 2;
        return area;
    }

    private TableDataCollection storeOutput(SiteModel[] models, double[][] sensitivities, double[][] fprs) throws Exception
    {
        TableDataCollection resTable = TableDataCollectionUtils.createTableDataCollection(parameters.getOutput());

        resTable.getColumnModel().addColumn("Area under ROC curve", Double.class);
        resTable.getColumnModel().addColumn("Plot", Chart.class);

        for( int i = 0; i < models.length; i++ )
        {
            double area = areaUnderROCCurve(sensitivities[i], fprs[i]);
            Chart chart = getChartForROCCurve(sensitivities[i], fprs[i]);
            TableDataCollectionUtils.addRow(resTable, models[i].getName(), new Object[] {area, chart});
        }

        CollectionFactoryUtils.save(resTable);
        return resTable;
    }

    private Sequence[] getSequences() throws Exception
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
        if( sequences.isEmpty() )
            throw new Exception("No sequences");
        return sequences.toArray(new Sequence[sequences.size()]);
    }

    private SiteModel[] getModels() throws Exception
    {
        DataElementPathSet siteModelsPaths = parameters.getSiteModels();

        if( siteModelsPaths.size() == 1 )
        {
            DataElementPath first = siteModelsPaths.first();
            if( ! ( first.optDataElement() instanceof SiteModel ) && ( first.optDataElement() instanceof DataCollection ) )
                siteModelsPaths = first.getChildren();
        }
        
        SiteModel[] models = siteModelsPaths.elements( SiteModel.class ).toArray( SiteModel[]::new );

        if( models.length == 0 )
            throw new Exception("No site models");
        return models;
    }

    /**
     * For each sequence return greatest score of model in the sequence.
     * @param sequences
     * @param model
     * @return
     */
    private static double[] getBestScores(Sequence[] sequences, SiteModel model)
    {
        double[] result = new double[sequences.length];
        for( int i = 0; i < sequences.length; i++ )
        {
            Sequence forwardSequence = new SequenceRegion(sequences[i], 1, sequences[i].getLength(), false, false);
            Sequence reverseSequence = new SequenceRegion(sequences[i], sequences[i].getStart() + sequences[i].getLength() - 1,
                    sequences[i].getLength(), true, false);
            Site forwardSite = model.findBestSite(forwardSequence);
            Site reverseSite = model.findBestSite(reverseSequence);
            double forwardScore = forwardSite == null ? -Double.MAX_VALUE : forwardSite.getScore();
            double reverseScore = reverseSite == null ? -Double.MAX_VALUE : reverseSite.getScore();
            result[i] = Math.max(forwardScore, reverseScore);
        }
        return result;
    }

    /**
     * Shuffles letters in sequence
     * @param s
     * @param rng
     */
    private static void shuffle(Sequence s, Random rng)
    {
        for( int i = s.getLength() + s.getStart() - 1; i > s.getStart(); i-- )
        {
            int j = rng.nextInt(i + 1 - s.getStart()) + s.getStart();
            byte tmp = s.getLetterAt(i);
            s.setLetterAt(i, s.getLetterAt(j));
            s.setLetterAt(j, tmp);
        }
    }

    /**
     * For each threshold compute fraction of scores greater or equal to the threshold.
     * @param scores
     * @param thresholds
     * @return array of the same length as thresholds
     */
    private static double[] getSensitivity(double[] scores, double[] thresholds)
    {
        double[] result = new double[thresholds.length];
        for( int i = 0; i < thresholds.length; i++ )
            for( double score : scores )
                if( score >= thresholds[i] )
                    result[i] += 1.0 / scores.length;
        return result;
    }

    /**
     * For each threshold return the fraction of sequences which have
     * match of site model with this threshold or greater.
     * @param sequences
     * @param model
     * @param thresholds
     * @return
     */
    private static double[] getPvalues(Sequence[] sequences, SiteModel model, double[] thresholds)
    {
        double[] scores = getBestScores(sequences, model);
        return getSensitivity(scores, thresholds);
    }
    
    
    private static Sequence[] getBackgroundSequences(Sequence[] originalSequences, Random rng, DataElementPath bgSeqPath)
    {
        if(bgSeqPath == null) {
            for(Sequence s : originalSequences)
                shuffle(s, rng);
            return originalSequences;
        }
        
        DataCollection<AnnotatedSequence> backgroundSeqCollection = bgSeqPath.getDataCollection(AnnotatedSequence.class);
        Sequence[] backgroundSequences = new Sequence[backgroundSeqCollection.getSize()];
        int n = 0;
        for(AnnotatedSequence m: backgroundSeqCollection)
            backgroundSequences[n++] = m.getSequence();
        
        Comparator<Sequence> byLength = Comparator.comparingInt( Sequence::getLength );
        Arrays.sort(backgroundSequences, byLength);

        Sequence[] result = new Sequence[originalSequences.length];
        
        for(int i = 0; i < originalSequences.length; i++)
        {
            n = Arrays.binarySearch(backgroundSequences, originalSequences[i], byLength);
            int originalLength = originalSequences[i].getLength();
            while(n > 0 && backgroundSequences[n - 1].getLength() == originalLength) n--;
            if(n < 0) n = -n - 1;
            if(n == backgroundSequences.length)
                throw new RuntimeException("All background sequences shorter then " + originalLength);
            Sequence selectedSequence = backgroundSequences[n + rng.nextInt(backgroundSequences.length - n)];
            int from = rng.nextInt(selectedSequence.getLength() - originalLength + 1);
            SequenceRegion region = new SequenceRegion(selectedSequence, selectedSequence.getStart() + from, originalLength, false, false);
            if(rng.nextBoolean())
                region = SequenceRegion.getReversedSequence(region);
            result[i] = new LinearSequence(region.getBytes(), region.getAlphabet());
        }
        
        return result;
    }

    private static double[][] getFPRs(Sequence[] sequences, SiteModel[] models, double[][] thresholds, int numberOfPermutations, Random rng, DataElementPath bgSeqPath, MotifCompareJobControl jobControl)
    {
        double[][] fprs = new double[models.length][];
        for( int i = 0; i < models.length; i++ )
            fprs[i] = new double[thresholds[i].length];

        for( int step = 0; step < numberOfPermutations; step++ )
        {
            sequences = getBackgroundSequences(sequences, rng, bgSeqPath);
            for( int i = 0; i < models.length; i++ )
            {
                double[] modelFPRs = getPvalues(sequences, models[i], thresholds[i]);
                for( int j = 0; j < fprs[i].length; j++ )
                    fprs[i][j] += modelFPRs[j];
            }
            if( jobControl != null )
            {
                jobControl.stepCompleted();
                if( jobControl.isStopped() )
                    return null;
            }
        }
        for( int i = 0; i < models.length; i++ )
            for( int j = 0; j < fprs[i].length; j++ )
                fprs[i][j] /= numberOfPermutations;
        return fprs;
    }

    private static double[] unique(double[] x)
    {
        if( x.length == 0 )
            return new double[0];
        double[] result = new double[x.length];
        result[0] = x[0];
        int j = 1;
        for( int i = 1; i < x.length; i++ )
            if( result[j - 1] != x[i] )
                result[j++] = x[i];
        double[] unique = new double[j];
        System.arraycopy(result, 0, unique, 0, j);
        return unique;
    }
    
    private void setModelThresholds(SiteModel[] models, double[][] thresholds, double[][] fprs) throws Exception
    {
        for(int i = 0; i < models.length; i++)
        {
            double t = thresholds[i][thresholds[i].length-1];
            for(int j = 0; j < thresholds[i].length; j++)
                if(fprs[i][j] <= parameters.getModelFDR())
                {
                    t = thresholds[i][j];
                    break;
                }
            models[i].setThreshold(t);
            CollectionFactoryUtils.save(models[i]);
        }
    }


    private class MotifCompareJobControl extends AnalysisJobControl
    {
        private int nsteps;
        private int currentStep;
        public MotifCompareJobControl()
        {
            super(MotifCompare.this);
        }

        public void stepCompleted()
        {
            currentStep++;
            setPreparedness( ( 100 * currentStep ) / nsteps);
        }

        public void setStepsCount(int n)
        {
            nsteps = n;
        }
    }
}
