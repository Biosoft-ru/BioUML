package biouml.plugins.bindingregions.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import org.apache.commons.lang.ArrayUtils;

import biouml.plugins.bindingregions.model.SiteModelFactory;
import biouml.plugins.bindingregions.utils.StatUtil.DensityEstimation;
import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.task.TaskPool;
import ru.biosoft.analysis.Util;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.LinearSequence;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceRegion;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.bsa.analysis.IPSSiteModel;
import ru.biosoft.bsa.analysis.LogIPSSiteModel;
import ru.biosoft.bsa.analysis.LogWeightMatrixModelWithModeratePseudocounts;
import ru.biosoft.bsa.analysis.MatchSiteModel;
import ru.biosoft.bsa.analysis.WeightMatrixModel;
import ru.biosoft.bsa.analysis.motifcompare.ModelComparison;
import ru.biosoft.bsa.analysis.motifcompare.MotifCompare;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.ObjectExtensionRegistry;

/**
 * @author yura
 *
 */
public class SiteModelsComparison
{
    public static final String WEIGHT_MATRIX_MODEL = "Common additive model";
    public static final String LOG_WEIGHT_MATRIX_MODEL_WITH_MODERATE_PSEUDOCOUNTS = "Common multiplicative model";
    public static final String MATCH_SITE_MODEL = "MATCH model";
    public static final String IPS_SITE_MODEL = "IPS model";
    public static final String LOG_IPS_SITE_MODEL = "Multiplicative IPS model";

    private static final ObjectExtensionRegistry<SiteModelFactory> siteModelFactoryRegistry = new ObjectExtensionRegistry<>("biouml.plugins.bindingregions.siteModelFactory", "name", SiteModelFactory.class);
    private final SiteModel[] siteModels;
    private SitePrediction sitePrediction;

    public SiteModelsComparison(SiteModel[] siteModels)
    {
        this.siteModels = siteModels;
    }

    public SiteModelsComparison(FrequencyMatrix frequencyMatrix)
    {
        this.siteModels = set_5_SiteModels(frequencyMatrix);
    }

    public SiteModelsComparison(String[] siteModelTypes, FrequencyMatrix frequencyMatrix)
    {
        this.siteModels = StreamEx.of(siteModelTypes).map(type -> getSiteModel(type, frequencyMatrix, 0.01, null)).toArray(SiteModel[]::new);
    }

    public SiteModelsComparison(String siteModelType, Integer window, FrequencyMatrix[] matrices)
    {
        SiteModel[] siteModels = new SiteModel[matrices.length];
        for( int i = 0; i < matrices.length; i++ )
            siteModels[i] = getSiteModel(siteModelType, matrices[i], 0.01, window);
        this.siteModels = siteModels;
        this.sitePrediction = new SitePrediction(siteModelType, window, SitePrediction.ALL_MATRICES, matrices, null);
    }

    public SiteModelsComparison(FrequencyMatrix frequencyMatrix, Sequence[] sequences, double fdr)
    {
        SiteModel[] models = set_5_SiteModels(frequencyMatrix);
        double[] scoreThresholds = getScoreThresholds(models, sequences, fdr);
        this.siteModels = set_5_SiteModels(frequencyMatrix, scoreThresholds);
    }

    // TODO: To remove?
    private SiteModel[] set_5_SiteModels(FrequencyMatrix frequencyMatrix, double[] scoreThresholds)
    {
        SiteModel[] models = new SiteModel[5];
        FrequencyMatrix matrix0 = new FrequencyMatrix(null, "", frequencyMatrix);
        models[0] = new WeightMatrixModel(WEIGHT_MATRIX_MODEL, null, matrix0, scoreThresholds[0]);
        FrequencyMatrix matrix1 = new FrequencyMatrix(null, "", frequencyMatrix);
        models[1] = new LogWeightMatrixModelWithModeratePseudocounts(LOG_WEIGHT_MATRIX_MODEL_WITH_MODERATE_PSEUDOCOUNTS, null, matrix1, scoreThresholds[1]);
        FrequencyMatrix matrix2 = new FrequencyMatrix(null, "", frequencyMatrix);
        models[2] = new MatchSiteModel(MATCH_SITE_MODEL, null, matrix2, scoreThresholds[2], 0.0);
        FrequencyMatrix matrix3 = new FrequencyMatrix(null, "", frequencyMatrix);
        WeightMatrixModel weightMatrixModel = new WeightMatrixModel(WEIGHT_MATRIX_MODEL, null, matrix3, scoreThresholds[0]);
        WeightMatrixModel[] matrixArray = new WeightMatrixModel[] {weightMatrixModel};
        models[3] = new IPSSiteModel(IPS_SITE_MODEL, null, matrixArray, scoreThresholds[3]);
        FrequencyMatrix matrix4 = new FrequencyMatrix(null, "", frequencyMatrix);
        WeightMatrixModel weightMatrixModel4 = new LogWeightMatrixModelWithModeratePseudocounts(LOG_WEIGHT_MATRIX_MODEL_WITH_MODERATE_PSEUDOCOUNTS, null, matrix4, scoreThresholds[1]);
        WeightMatrixModel[] matrixArray4 = new WeightMatrixModel[]{weightMatrixModel4};
        models[4] = new LogIPSSiteModel(LOG_IPS_SITE_MODEL, null, matrixArray4, scoreThresholds[4], IPSSiteModel.DEFAULT_DIST_MIN);
        return models;
    }

    // it is necessary to replace MotifCompare.compareModels
    // with the help getROCcurve() in SiteModelsComparisonUtils.java
    // and add thresholds into getROCcurve()
    // TODO: To remove?
    private double[] getScoreThresholds(SiteModel[] models, Sequence[] sequences, double fdr)
    {
        double[] result = new double[models.length];
        Sequence[] sequences1 = StreamEx.of(sequences).map( LinearSequence::new ).toArray( Sequence[]::new );
        ModelComparison modelComparison = MotifCompare.compareModels(models, sequences1);
        for( int j = 0; j < models.length; j++ )
        {
            double[] thresholds = modelComparison.getThresholds(j);
            double[] FDRs = modelComparison.getFPRs(j);
            result[j] = thresholds[IntStreamEx.ofIndices( thresholds ).minBy( i -> Math.abs(FDRs[i] - fdr) ).getAsInt()];
        }
        return result;
    }

    public int getNumberOfSiteModels()
    {
        return siteModels.length;
    }

    public static String[] getSiteModelTypes()
    {
        return siteModelFactoryRegistry.names().sorted().toArray(String[]::new);
    }

    public static SiteModel getSiteModel(String siteModelType, FrequencyMatrix frequencyMatrix, double scoreThreshold, Integer window)
    {
        SiteModelFactory extension = siteModelFactoryRegistry.getExtension(siteModelType);
        return extension == null ? null : extension.create(siteModelType, frequencyMatrix, scoreThreshold, window);
    }

    private SiteModel[] set_5_SiteModels(FrequencyMatrix matrix)
    {
        SiteModel[] siteModels = new SiteModel[5];
        siteModels[0] = getSiteModel(WEIGHT_MATRIX_MODEL, matrix, 0.01, null);
        siteModels[1] = getSiteModel(LOG_WEIGHT_MATRIX_MODEL_WITH_MODERATE_PSEUDOCOUNTS, matrix, -Double.MAX_VALUE, null);
        siteModels[2] = getSiteModel(MATCH_SITE_MODEL, matrix, 0.0, null);
        siteModels[3] = getSiteModel(IPS_SITE_MODEL, matrix, 0.01, null);
        siteModels[4] = getSiteModel(LOG_IPS_SITE_MODEL, matrix, 0.01, null);
        return siteModels;
    }

    public String[] getModelsNames()
    {
        String[] names;
        if( sitePrediction != null )
            names = sitePrediction.getMatrixNames();
        else
        {
            names = new String[siteModels.length];
            for( int i = 0; i < siteModels.length; i++ )
                names[i] = siteModels[i].getName();
        }
        return names;
    }

    /***
     *
     * @param sequences
     * @param isAucCalculated
     * @param areAllMatrices
     * @return Object[]; Object[0] - chart with ROC curves;
     * Object[1] = Map(String, Double) - site model name and AUC
     */
    public Object[] getChartWithROCcurves(final Sequence[] sequences, boolean areBothStrands, boolean isAucCalculated, boolean areAllMatrices)
    {
        int lengthForTruncation = getLengthForTruncation();
        final Sequence[] truncatedSequences = lengthForTruncation > 0 ? getTruncatedSequences(sequences, lengthForTruncation) : null;
        List<List<double[]>> curves;
        try
        {
            curves = TaskPool.getInstance().map(Arrays.asList(siteModels), siteModel ->
                {
                    Sequence[] seqs = truncatedSequences != null && getWindow(siteModel) == 0 ? truncatedSequences : sequences;
                    SitePrediction sp = new SitePrediction("siteModelType", "siteName", new SiteModel[]{siteModel}, new String[]{"matrixName"});
                    List<double[]> rocCurve = sp.getROCcurve(seqs, areBothStrands, 10, 0);
                    return TableUtils.recalculateCurve(rocCurve.get(0), rocCurve.get(1));
                }
            );
        }
        catch( InterruptedException | ExecutionException e )
        {
            throw ExceptionRegistry.translateException(e);
        }
        Map<String, Double> siteModelNameAndAUC = new HashMap<>();
        List<double[]> xValuesForCurves = new ArrayList<>();
        List<double[]> yValuesForCurves = new ArrayList<>();
        String[] names = getModelsNames();
        for( int i = 0; i < siteModels.length; i++ )
        {
            List<double[]> rocCurve = curves.get(i);
            xValuesForCurves.add(i, rocCurve.get(0));
            yValuesForCurves.add(i, rocCurve.get(1));
            if( isAucCalculated )
                siteModelNameAndAUC.put(names[i], SiteModelsComparisonUtils.getAUC(rocCurve));
        }
        if( sitePrediction != null && areAllMatrices )
        {
//          List<double[]> rocCurve = SiteModelsComparisonUtils.recalculateRocCurve(sitePrediction.getROCcurve(sequences, 10, 0));
            List<double[]> rocCurve = sitePrediction.getROCcurve(sequences, areBothStrands, 10, 0);
            rocCurve = TableUtils.recalculateCurve(rocCurve.get(0), rocCurve.get(1));
            xValuesForCurves.add(siteModels.length, rocCurve.get(0));
            yValuesForCurves.add(siteModels.length, rocCurve.get(1));
//          names = addNewElement(names, SitePrediction.ALL_MATRICES);
            names = (String[])ArrayUtils.add(names, names.length, SitePrediction.ALL_MATRICES);
            if( isAucCalculated )
                siteModelNameAndAUC.put(names[names.length - 1], SiteModelsComparisonUtils.getAUC(rocCurve));
        }
        Chart chart = TableUtils.createChart(xValuesForCurves, yValuesForCurves, names, null, null, null, 0.0, 1.0, 0.0, 1.0, "False discovery rate", "Sensitivity");
        return new Object[]{chart, siteModelNameAndAUC};
    }

    // better to use String[] names = (String[])ArrayUtils.add(names, names.length, SitePrediction.ALL_MATRICES);
    private String[] addNewElement(String[] array, String newElement)
    {
        return StreamEx.of(array).append( newElement ).toArray( String[]::new );
    }

    /***
     *
     * @return (window - length) if set of site models contains IPS_SITE_MODEL or LOG_IPS_SITE_MODEL, otherwise return 0
     */
    private int getLengthForTruncation()
    {
        int result = 0;
        if( sitePrediction != null ) return result;
        for( SiteModel model : siteModels )
            switch(model.getName())
            {
                case IPS_SITE_MODEL     : return ((IPSSiteModel)model).getWindow() - ((IPSSiteModel)model).getMatrices()[0].getLength();
                case LOG_IPS_SITE_MODEL : return ((LogIPSSiteModel)model).getWindow() - ((LogIPSSiteModel)model).getMatrices()[0].getLength();
            }
        return result;
    }

    /***
     * Return truncated sequence. Both flanks (of the length 'lengthForTruncation / 2') are truncated in initial sequence
     * @param sequence
     * @param lengthForTruncation
     * @return
     */
    private static Sequence getTruncatedSequence(Sequence sequence, int lengthForTruncation)
    {
        if( sequence.getLength() <= lengthForTruncation || lengthForTruncation == 0) return sequence;
        else return new SequenceRegion(sequence, sequence.getStart() + lengthForTruncation / 2, sequence.getLength() - lengthForTruncation, false, false);
    }

    /***
     * Return truncated sequences. Both flanks (of the length 'lengthForTruncation / 2') are truncated in each initial sequence
     * @param sequences
     * @param lengthForTruncation
     * @return
     */
    public static Sequence[] getTruncatedSequences(Sequence[] sequences, int lengthForTruncation)
    {
        return StreamEx.of(sequences).map( seq -> getTruncatedSequence( seq, lengthForTruncation ) ).toArray( Sequence[]::new );
    }

    // TODO: to remove it after testing findBestSite(Sequence sequence, boolean areBothStrands)
    // It is not necessary to recalculate site positions in the case of truncating sequence
    // when sequence = new SequenceRegion(...) because SequenceRegion() automatically recalculates positions
    // for initial non-truncated sequence.
    private Site[] findBestSite(Sequence sequence)
    {
        int lengthForTruncation = getLengthForTruncation();
        Sequence truncatedSequence = lengthForTruncation > 0 ? getTruncatedSequence( sequence, lengthForTruncation ) : null;
        return StreamEx.of( siteModels )
                .map( siteModel ->
                    SequenceRegion.withReversed( truncatedSequence != null && getWindow( siteModel ) == 0 ? truncatedSequence : sequence )
                        .map( siteModel::findBestSite )
                        .maxByDouble( Site::getScore ).get() )
                .toArray( Site[]::new );
    }

    // It is not necessary to recalculate site positions in the case of truncating sequence
    // when sequence = new SequenceRegion(...) because SequenceRegion() automatically recalculates positions
    // for initial non-truncated sequence.
    public Site[] findBestSite(Sequence sequence, boolean areBothStrands)
    {
        int lengthForTruncation = getLengthForTruncation();
        Sequence truncatedSequence = lengthForTruncation > 0 ? getTruncatedSequence( sequence, lengthForTruncation ) : null;
        Site[] result = new Site[siteModels.length];
        for( int j = 0; j < siteModels.length; j++ )
        {
            Sequence seq = truncatedSequence != null && getWindow(siteModels[j]) == 0 ? truncatedSequence : sequence;
            result[j] = siteModels[j].findBestSite(seq);
            if( areBothStrands )
            {
                SequenceRegion reverseSeq = SequenceRegion.getReversedSequence(seq);
                Site site = siteModels[j].findBestSite(reverseSeq);
                if( site.getScore() > result[j].getScore() )
                    result[j] = site;
            }
        }
        return result;
    }

    /***
     *
     * @param sequences
     * @return bestSites bestSites[i][j] = best site for i-th sequence calculated by j-th SiteModel
     */
    // TODO: to remove it after testing findBestSite(Sequence[] sequences, boolean areBothStrands
    private Site[][] findBestSite(Sequence[] sequences)
    {
        try
        {
            List<Site[]> result = TaskPool.getInstance().map(Arrays.asList(sequences), this::findBestSite);
            return result.toArray(new Site[result.size()][]);
        }
        catch( InterruptedException | ExecutionException e )
        {
            throw ExceptionRegistry.translateException(e);
        }
    }

    private Site[][] findBestSite(Sequence[] sequences, boolean areBothStrands)
    {
        try
        {
//          List<Site[]> result = TaskPool.getInstance().map(Arrays.asList(sequences), this::findBestSite);
            List<Site[]> result = TaskPool.getInstance().map(Arrays.asList(sequences), source -> findBestSite(source, areBothStrands));
            return result.toArray(new Site[result.size()][]);
        }
        catch( InterruptedException | ExecutionException e )
        {
            throw ExceptionRegistry.translateException(e);
        }
    }

    /***
     *
     * @param sequences
     * @return bestScores bestScores[i][j] = best score for i-th sequence calculated by j-th SiteModel
     */
    private double[][] getBestScores(Sequence[] sequences, boolean areBothStrands)
    {
        Site[][] bestSites = findBestSite(sequences, areBothStrands);
        return getBestScores(bestSites);
    }

    private double[][] getBestScores(Site[][] bestSites)
    {
        return StreamEx.of( bestSites ).map( sites -> StreamEx.of(sites).mapToDouble( Site::getScore ).toArray()).toArray( double[][]::new );
    }

    /***
     *
     * @param scores scores[i][j] = best score calculated for i-th sequence by j-th siteModel
     * @param percentage
     * @return bestScoreThresholds bestScoreThresholds[j] = score threshold for j-th siteModel;
     * 'percentage' % of sequences have scores that are equal or greater than bestScoreThresholds[j]
     */
    private double[] getBestScoreThresholds(double[][] scores, int percentage)
    {
        int index = (100 - percentage) * scores.length / 100;
        return IntStreamEx.ofIndices( siteModels )
                .mapToDouble(
                        j -> StreamEx.of( scores ).mapToDouble( score -> score[j] ).sorted().skip( index ).findFirst().getAsDouble() )
                .toArray();
    }
    
    // my old version
//    private double[] getBestScoreThresholds(double[][] scores, int percentage)
//    {
//        int index = (100 - percentage) * scores.length / 100;
//        // return IntStreamEx.ofIndices( siteModels ).mapToDouble(j -> StreamEx.of(scores).mapToDouble score -> score[j]).sorted().skip(index).findFirst().getAsDouble()).toArray();
//        for( int j = 0; j < siteModels.length; j++ )
//        {
//            List<Double> list = new ArrayList<Double>();
//            for( double[] score : scores )
//                list.add(score[j]);
//            Collections.sort(list);
//            result[j] = list.get(index);
//        }
//        return result;
//    }


    // TODO: to refactoring : to add boolean areBothStrands into getBestScores() and findBestSite()
    public Sequence[] getUnionOfBestSequences(Sequence[] sequences, boolean areBothStrands, int percentage)
    {
        if( percentage >= 100 ) return sequences;
        double[][] bestScores = getBestScores(sequences, areBothStrands);
        double bestScoreThresholds[] = getBestScoreThresholds(bestScores, percentage);
        List<Sequence> list = new ArrayList<>();
        for( int i = 0; i < sequences.length; i++ )
        {
            boolean best = false;
            for( int j = 0; j < siteModels.length; j++ )
                if( bestScores[i][j] >= bestScoreThresholds[j] )
                {
                    best = true;
                    break;
                }
            if( best )
                list.add(sequences[i]);
        }
        if( list.isEmpty() ) return null;
        return list.toArray(new Sequence[list.size()]);
    }

    public List<Sequence> getSequencesOfBestSites(Sequence[] sequences, boolean areBothStrands, int percentage)
    {
        Site[][] bestSites = findBestSite(sequences, areBothStrands);
        double[][] bestScores = getBestScores(bestSites);
        double bestScoreThresholds[] = getBestScoreThresholds(bestScores, percentage);
        List<Sequence> list = new ArrayList<>();
        for( int i = 0; i < sequences.length; i++ )
        {
            List<Integer> positions = new ArrayList<>();
            for( int j = 0; j < siteModels.length; j++ )
            {
                if( bestScores[i][j] < bestScoreThresholds[j] ) continue;
                Integer pos = bestSites[i][j].getFrom();
                if( positions.contains(pos) ) continue;
                positions.add(pos);
                list.add(bestSites[i][j].getSequence());
            }
        }
        if( list.isEmpty() ) return null;
        return list;
    }

    /***
     * @param sequences sequences[i] = i-th sequence
     * @param Sequence[] subset of sequences that contain identical best sites identified by all SiteModels
     * @return
     */
    public Sequence[] getSequencesWithIdenticalBestSites(Sequence[] sequences, boolean areBothStrands)
    {
        if( siteModels.length == 1 ) return sequences;
        List<Sequence> result = new ArrayList<>();
        Site[][] bestSites = findBestSite(sequences, areBothStrands);
        for( int i = 0; i < sequences.length; i++ )
        {
            Set<Integer> set = new HashSet<>();
            for( int j = 0; j < siteModels.length; j++ )
                set.add(bestSites[i][j].getFrom());
            if( set.size() == 1 )
                result.add(sequences[i]);
        }
        if( result.isEmpty() ) return null;
        return result.toArray(new Sequence[result.size()]);
    }

    public List<Sequence> getSequencesOfIdenticalBestSites(Sequence[] sequences, boolean areBothStrands)
    {
        if( siteModels.length <= 1 ) return null;
        Site[][] bestSites = findBestSite(sequences, areBothStrands);
        List<Sequence> result = StreamEx.of(bestSites)
            .filter( sites -> StreamEx.of(sites).map( site -> site.getSequence().toString() ).distinct().count() == 1)
            .map( sites -> sites[0].getSequence() )
            .toList();
        if( result.isEmpty() ) return null;
        return result;
    }

    private int getLength(int modelIndex)
    {
        switch( siteModels[modelIndex].getName() )
        {
            case IPS_SITE_MODEL     : return ((IPSSiteModel)siteModels[modelIndex]).getMatrices()[0].getLength();
            case LOG_IPS_SITE_MODEL : return ((LogIPSSiteModel)siteModels[modelIndex]).getMatrices()[0].getLength();
            default                 : return siteModels[modelIndex].getLength();
        }
    }

    public static int getWindow(SiteModel siteModel)
    {
        switch( siteModel.getName() )
        {
            case IPS_SITE_MODEL     : return ((IPSSiteModel)siteModel).getWindow();
            case LOG_IPS_SITE_MODEL : return ((LogIPSSiteModel)siteModel).getWindow();
            default                 : return 0;
        }
    }

    /***
     *
     * @param sequences
     * @param bestSites bestSites[i][j] = best site of j-th SiteModel in i-th sequence
     * @param siteModelIndex = index of SiteModel
     * @return result[]; result[i] = normalized position of best site of 'siteModelIndex'-th model
     *                   in i-th sequence; 0 <= result[i] <= 100(%);
     */
    private double[] getSitePositionsNormalized(Sequence[] sequences, Site[][] bestSites, int siteModelIndex)
    {
        int window = getWindow(siteModels[siteModelIndex]);
        double[] result = new double[bestSites.length];
        for( int i = 0; i < bestSites.length; i++ )
        {
            Interval siteInterval = bestSites[i][siteModelIndex].getInterval();
            if(window > 0)
                siteInterval = siteInterval.zoomToLength(window);
            Interval seqInterval = sequences[i].getInterval();
            result[i] = seqInterval.getIntervalPos(siteInterval.fit(seqInterval)) * 100;
        }
        return result;
    }

    private double[] getDistancesBetweenSitesAndSummits(Site[][] bestSites, int[] summits, int siteModelIndex)
    {
        double[] result = new double[bestSites.length];
        for( int i = 0; i < bestSites.length; i++ )
            result[i] = (bestSites[i][siteModelIndex].getFrom() + bestSites[i][siteModelIndex].getLength() / 2.0) - summits[i];
        return result;
    }

    /***
     *
     * @param bestSites bestSites[i][j] = best site of j-th SiteModel in i-th sequence
     * @param summits summits[i] = summit for i-th sequence
     * @param siteModelIndex index of SiteModel
     * @return result[][]; result[i][0] = distance between best site and  summit in i-th sequence;
     *                      result[i][1] = score of best site in i-th sequence
     */
    private double[][] getDistanceBetweenSiteAndSummitAndScore(Site[][] bestSites, int[] summits, int siteModelIndex)
    {
        double[][] result = new double[bestSites.length][2];
        for( int i = 0; i < bestSites.length; i++ )
        {
            result[i][0] = bestSites[i][siteModelIndex].getInterval().getCenter() - summits[i];
            result[i][1] = bestSites[i][siteModelIndex].getScore();
        }
        return result;
    }

    public Map<String, Chart> getChartsWithBestSitesLocationDistributions (Sequence[] sequences, boolean areBothStrands, int[] summits)
    {
        Map<String, Chart> result = new HashMap<>();
        Site[][] bestSites = findBestSite(sequences, areBothStrands);

        // 1. Calculation of chart : "Best site locations along normalized sequences";
        String[] modelNames = getModelsNames();
        Map<String, double[]> map = new HashMap<>();
        for( int j = 0; j < modelNames.length; j++ )
            map.put(modelNames[j], getSitePositionsNormalized(sequences, bestSites, j));
        SampleComparison sc = new SampleComparison(map, "Normalized positions of best sites (in %)");
        result.put("Best site locations along normalized sequences", sc.chartWithSmoothedDensities(false, null, DensityEstimation.WINDOW_WIDTH_01, null));
        if( summits == null ) return result;

        // 2. Calculation of chart : "Best site locations around summits";
        for( int j = 0; j < modelNames.length; j++ )
            map.put(modelNames[j], getDistancesBetweenSitesAndSummits(bestSites, summits, j));
        sc = new SampleComparison(map, "Positions of best sites around summits");
        result.put("Best site locations around summits", sc.chartWithSmoothedDensities(false, null, null, 30.0));

        // 3. Calculation of chart : "Smoothed scores of best sites around summit";
        List<double[]> xValuesForCurves = new ArrayList<>();
        List<double[]> yValuesForCurves = new ArrayList<>();
        for( int j = 0; j < bestSites[0].length; j++ )
        {
            double[][] distanceAndScore = getDistanceBetweenSiteAndSummitAndScore(bestSites, summits, j);
            List<double[]> smoothedCurve = Util.nwAverage(distanceAndScore, 20.0);
            xValuesForCurves.add(j, smoothedCurve.get(0));
            yValuesForCurves.add(j, smoothedCurve.get(1));
        }
        Chart chart = TableUtils.createChart(xValuesForCurves, yValuesForCurves, getModelsNames(), null, null, null, -1000.0, 1000.0, null, null, "Positions of best sites around summits", "Scores of best sites");
        result.put("Smoothed scores of best sites around summits", chart);
        return result;
    }

    public void writeChartsWithBestSitesLocationDistributions(Sequence[] sequences, boolean areBothStrands, int[] summits, DataElementPath pathToTable)
    {
        Site[][] bestSites = findBestSite(sequences, areBothStrands);

        // 1. Calculation of chart : "Best site locations along normalized sequences";
        String[] modelNames = getModelsNames();
        Map<String, double[]> map = new HashMap<>();
        for( int j = 0; j < modelNames.length; j++ )
            map.put(modelNames[j], getSitePositionsNormalized(sequences, bestSites, j));
        SampleComparison sc = new SampleComparison(map, "Normalized positions of best sites (in %)");
        TableUtils.addChartToTable("Best site locations along normalized sequences", sc.chartWithSmoothedDensities(false, null, DensityEstimation.WINDOW_WIDTH_01, null), pathToTable);
        if( summits == null ) return;

        // 2. Calculation of chart : "Best site locations around summits";
        map.clear();
        for( int j = 0; j < modelNames.length; j++ )
            map.put(modelNames[j], getDistancesBetweenSitesAndSummits(bestSites, summits, j));
        sc = new SampleComparison(map, "Positions of best sites around summits");
        TableUtils.addChartToTable("Best site locations around summits", sc.chartWithSmoothedDensities(false, null, null, 30.0), pathToTable);

        // 3. Calculation of chart : "Smoothed scores of best sites around summit";
        List<double[]> xValuesForCurves = new ArrayList<>();
        List<double[]> yValuesForCurves = new ArrayList<>();
        for( int j = 0; j < bestSites[0].length; j++ )
        {
            double[][] distanceAndScore = getDistanceBetweenSiteAndSummitAndScore(bestSites, summits, j);
            List<double[]> smoothedCurve = Util.nwAverage(distanceAndScore, 20.0);
            xValuesForCurves.add(j, smoothedCurve.get(0));
            yValuesForCurves.add(j, smoothedCurve.get(1));
        }
        Chart chart = TableUtils.createChart(xValuesForCurves, yValuesForCurves, getModelsNames(), null, null, null, -1000.0, 1000.0, null, null, "Positions of best sites around summits", "Scores of best sites");
        TableUtils.addChartToTable("Smoothed scores of best sites around summits", chart, pathToTable);
    }

    /***
     * Calculate best sites and write them into table
     * remark: the positions of best sites are relative to peaks (i.e. not chromosomal positions)
     * @param chromosomeAndPeaks    Chip-Seq peaks
     * @param sequences nucleotide sequences of peaks
     * @param pathToTable path to the creating table
     * @throws Exception
     */
    public void writeBestSitesIntoTable(Map<String, List<ChipSeqPeak>> chromosomeAndPeaks, Sequence[] sequences, boolean areBothStrands, AnalysisJobControl jobControl, int from, int to, DataElementPath pathToTable) throws Exception
    {
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(pathToTable);
        String[] modelNames = getModelsNames();
        table.getColumnModel().addColumn("chromosome", String.class);
        table.getColumnModel().addColumn("peakStartPosition", Integer.class);
        table.getColumnModel().addColumn("peakEndPosition", Integer.class);
        for( String name : modelNames )
        {
            table.getColumnModel().addColumn("relative site start(" + name + ")", Integer.class);
            table.getColumnModel().addColumn("relative site finish(" + name + ")", Integer.class);
            String s = name.equals(LOG_IPS_SITE_MODEL) ? "lg-score" : "score";
            table.getColumnModel().addColumn(s + "(" + name + ")", Double.class);
            table.getColumnModel().addColumn("sequence(" + name + ")", String.class);
        }
        int index = 0, indexJobControl = 0, n = chromosomeAndPeaks.size(), difference = to - from;
        for( Entry<String, List<ChipSeqPeak>> entry : chromosomeAndPeaks.entrySet() )
        {
            String chromosome = entry.getKey();
            for( ChipSeqPeak peak : entry.getValue() )
            {
                Site[] bestSites = findBestSite(sequences[index], areBothStrands);
                Object[] row = StreamEx.of( bestSites )
                        .flatMap( site -> Stream.<Object>of(site.getFrom(), site.getTo(), site.getScore(), site.getSequence().toString()) )
                        .prepend( chromosome, peak.getStartPosition(), peak.getFinishPosition() ).toArray();
                TableDataCollectionUtils.addRow(table, Integer.toString(++index), row);
            }
            jobControl.setPreparedness(from + ++indexJobControl * difference / n);
        }
        table.finalizeAddition();
        CollectionFactoryUtils.save(table);
    }
}
