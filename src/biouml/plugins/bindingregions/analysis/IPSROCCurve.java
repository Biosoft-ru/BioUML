package biouml.plugins.bindingregions.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import one.util.streamex.DoubleStreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.task.TaskPool;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.bsa.analysis.IPSSiteModel;
import ru.biosoft.bsa.analysis.LogIPSSiteModel;
import ru.biosoft.bsa.analysis.WeightMatrixModel;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.util.bean.BeanInfoEx2;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.bindingregions.fiveSiteModels.AbstractFiveSiteModelsParameters;
import biouml.plugins.bindingregions.resources.MessageBundle;
import biouml.plugins.bindingregions.utils.ChipSeqPeak;
import biouml.plugins.bindingregions.utils.SiteModelsComparison;
import biouml.plugins.bindingregions.utils.SiteModelsComparisonUtils;
import biouml.plugins.bindingregions.utils.SitePrediction;
import biouml.plugins.bindingregions.utils.TableUtils;

/**
 * @author lan
 * "ROC-curves: influence of common score selection and window selection on IPSSiteModel and logIPSSiteModel
 */
public class IPSROCCurve extends AnalysisMethodSupport<IPSROCCurve.IPSROCCurveParameters>
{
    public IPSROCCurve(DataCollection<?> origin, String name)
    {
        super(origin, name, new IPSROCCurveParameters());
    }

    @Override
    public DataCollection<?> justAnalyzeAndPut() throws Exception
    {
        log.info("ROC-curves: influence of common score selection and window selection on IPSSiteModel and logIPSSiteModel");
        DataElementPath pathToSequences = parameters.getDbSelector().getSequenceCollectionPath();
        DataElementPath pathToChipSeqTrack = parameters.getTrackPath();
        int minimalLengthOfSequenceRegion = parameters.getMinRegionLength();
        boolean isAroundSummit = parameters.isAroundSummit();
        DataElementPath pathToMatrix = parameters.getMatrixPath();
        boolean isIpsModel = parameters.getIsIpsModel();
        DataElementPath pathToOutputs = parameters.getOutputPath();

        double[] multipliersForIPS = new double[]{0.4, 0.5, 0.6, 0.7, 0.8, 0.9};
        double[] multipliersForLogIPS = new double[]{2.0, 5.0, 10.0, 20.0, 30.0, 50.0};
        int[] windows = new int[]{30, 50, 75, 100, 150, 200};
        boolean areBothStrands = true;

        // 1.
        log.info("Read ChIP-Seq peaks and their sequences");
        Object[] objects = ChipSeqPeak.getLinearSequencesForChipSeqPeaks(pathToChipSeqTrack, pathToSequences, isAroundSummit, minimalLengthOfSequenceRegion, jobControl, 0, 30);
        Sequence[] sequences = (Sequence[])objects[0];
        isAroundSummit = (boolean)objects[1];

        // 2.
        log.info("Calculate results and write them");
        FrequencyMatrix matrix = pathToMatrix.getDataElement(FrequencyMatrix.class);
        SiteModelsComparison smc = getSiteModelsForScoreThresholdStudy(matrix, isIpsModel, multipliersForIPS, multipliersForLogIPS);
        DataCollectionUtils.createFoldersForPath(pathToOutputs.getChildPath(""));
        writeResultsOfScoreThresholdStudy(smc, sequences, areBothStrands, isAroundSummit, isIpsModel, pathToOutputs);
        jobControl.setPreparedness(65);
        if( jobControl.isStopped() ) return null;
        SiteModel[] siteModels = getSiteModelsForWindowdStudy(matrix, isIpsModel, windows);
        writeResultsOfWindowStudy(siteModels, windows, sequences, areBothStrands, isAroundSummit, isIpsModel, pathToOutputs);
        jobControl.setPreparedness(100);
        return pathToOutputs.getDataCollection();
    }

    private SiteModelsComparison getSiteModelsForScoreThresholdStudy(FrequencyMatrix matrix, boolean isIpsModel,
            double[] multipliersForIPS, double[] multipliersForLogIPS)
    {
        double[] multipliers = isIpsModel ? multipliersForIPS : multipliersForLogIPS;
        SiteModel[] models = DoubleStreamEx.of( multipliers )
                .mapToObj( multiplier -> createModelForMultiplier( matrix, isIpsModel, multiplier ) ).toArray( SiteModel[]::new );
        return new SiteModelsComparison( models );
    }

    private IPSSiteModel createModelForMultiplier(FrequencyMatrix matrix, boolean isIpsModel, double multiplier)
    {
        WeightMatrixModel weightMatrixModel = new WeightMatrixModel(" ", null, matrix, 0.01);
        String name = "multiplier = " + Double.toString(multiplier);
        if( isIpsModel )
        {
            double matrixMaxScore = weightMatrixModel.getMaxScore();
            weightMatrixModel.setThreshold(matrixMaxScore * multiplier);
            return new IPSSiteModel(name, null, new WeightMatrixModel[]{weightMatrixModel}, 0.01);
        }
        return new LogIPSSiteModel(name, null, new WeightMatrixModel[]{weightMatrixModel}, 0.01, multiplier);
    }

    private void writeResultsOfScoreThresholdStudy(SiteModelsComparison smc, Sequence[] sequences, boolean areBothStrands, boolean isAroundSummit, boolean isIpsModel, DataElementPath pathToOutputs)
    {
        Object[] objects = smc.getChartWithROCcurves(sequences, areBothStrands, true, false);
        String name = "thresholds";
        name += isIpsModel ? "_ips" : "_logIps";
        if( isAroundSummit )
            name += "_summit";
        TableUtils.addChartToTable(name, (Chart)objects[0], pathToOutputs.getChildPath(name + "_chart"));
        Map<String, Double> siteModelTypeAndAUC = (Map<String, Double>)objects[1];
        int n = siteModelTypeAndAUC.size(), index = 0;
        double[][] data = new double[n][1];
        String[] names = new String[n];
        for( Entry<String, Double> entry : siteModelTypeAndAUC.entrySet() )
        {
            data[index][0] = entry.getValue();
            names[index++] = entry.getKey();
        }
        TableUtils.writeDoubleTable(data, names, new String[]{SiteModelsComparisonUtils.AUC}, pathToOutputs, name + "_" + SiteModelsComparisonUtils.AUC);
    }

    private SiteModel[] getSiteModelsForWindowdStudy(FrequencyMatrix matrix, boolean isIpsModel, int[] windows)
    {
        List<SiteModel> models = new ArrayList<>();
        for( int window : windows )
        {
            if( matrix.getLength() >= window ) continue;
            WeightMatrixModel weightMatrixModel = new WeightMatrixModel(" ", null, matrix, 0.01);
            String name = "window = " + Integer.toString(window);
            if( isIpsModel )
                models.add(new IPSSiteModel(name, null, new WeightMatrixModel[]{weightMatrixModel}, 0.01, IPSSiteModel.DEFAULT_DIST_MIN, window));
            else
                models.add(new LogIPSSiteModel(name, null, new WeightMatrixModel[]{weightMatrixModel}, 0.01, window, LogIPSSiteModel.DEFAULT_MULTIPLIER));
        }
        return models.toArray(new SiteModel[models.size()]);
    }

    private void writeResultsOfWindowStudy(SiteModel[] siteModels, int[] windows, Sequence[] sequences, boolean areBothStrands, boolean isAroundSummit, boolean isIpsModel, DataElementPath pathToOutputs)
    {
        int maximalWindow = windows[0];
        for( int i = 1; i < windows.length; i++ )
            maximalWindow = Math.max(maximalWindow, windows[i]);
        Object[] objects = getChartWithROCcurves(sequences, areBothStrands, siteModels, windows, maximalWindow, isIpsModel);
        String name = "windows";
        name += isIpsModel ? "_ips" : "_logIps";
        if( isAroundSummit )
            name += "_summit";
        TableUtils.addChartToTable(name, (Chart)objects[0], pathToOutputs.getChildPath(name + "_chart"));
        Map<String, Double> siteModelTypeAndAUC = (Map<String, Double>)objects[1];
        int n = siteModelTypeAndAUC.size(), index = 0;
        double[][] data = new double[n][1];
        String[] names = new String[n];
        for( Entry<String, Double> entry : siteModelTypeAndAUC.entrySet() )
        {
            data[index][0] = entry.getValue();
            names[index++] = entry.getKey();
        }
        TableUtils.writeDoubleTable(data, names, new String[]{SiteModelsComparisonUtils.AUC}, pathToOutputs, name + "_" + SiteModelsComparisonUtils.AUC);
    }

    // truncation of sequences for each site models is implemented according to windows
    private Object[] getChartWithROCcurves(final Sequence[] sequences, boolean areBothStrands, SiteModel[] siteModels, int[] windows, final int maximalWindow, final boolean isIpsModel)
    {
        List<List<double[]>> curves;
        try
        {
            curves = TaskPool.getInstance().map(Arrays.asList(siteModels), siteModel -> {
                int lengthForTruncation = isIpsModel ? maximalWindow - ((IPSSiteModel)siteModel).getWindow() : maximalWindow - ((LogIPSSiteModel)siteModel).getWindow();
                Sequence[] seqs = lengthForTruncation == 0 ? sequences : SiteModelsComparison.getTruncatedSequences(sequences, lengthForTruncation);
                SitePrediction sp = new SitePrediction("siteModelType", "siteName", new SiteModel[]{siteModel}, new String[]{"matrixName"});
//              return SiteModelsComparisonUtils.recalculateRocCurve(sp.getROCcurve(seqs, 10, 0));
                List<double[]> rocCurve = sp.getROCcurve(seqs, areBothStrands, 10, 0);
                rocCurve = TableUtils.recalculateCurve(rocCurve.get(0), rocCurve.get(1));
                rocCurve.get( 1 )[0] = 0;
                return rocCurve;
                    }, 1 );
        }
        catch( InterruptedException | ExecutionException e )
        {
            throw ExceptionRegistry.translateException(e);
        }
        Map<String, Double> siteModelNameAndAUC = new HashMap<>();
        List<double[]> xValuesForCurves = new ArrayList<>();
        List<double[]> yValuesForCurves = new ArrayList<>();
        String[] names = new String[siteModels.length];
        for( int i = 0; i < siteModels.length; i++ )
        {
            names[i] = siteModels[i].getName();
            List<double[]> rocCurve = curves.get(i);
            xValuesForCurves.add(i, rocCurve.get(0));
            yValuesForCurves.add(i, rocCurve.get(1));
            siteModelNameAndAUC.put(names[i], SiteModelsComparisonUtils.getAUC(rocCurve));
        }
        Chart chart = TableUtils.createChart(xValuesForCurves, yValuesForCurves, names, null, null, null, 0.0, 1.0, 0.0, 1.0, "False discovery rate", "Sensitivity");
        return new Object[]{chart, siteModelNameAndAUC};
    }

    public static class IPSROCCurveParameters extends AbstractFiveSiteModelsParameters
    {
        private boolean isIpsModel = true;

        @PropertyName(MessageBundle.PN_IS_IPS_MODEL)
        @PropertyDescription(MessageBundle.PD_IS_IPS_MODEL)
        public boolean getIsIpsModel()
        {
            return isIpsModel;
        }
        public void setIsIpsModel(boolean isIpsModel)
        {
            Object oldValue = this.isIpsModel;
            this.isIpsModel = isIpsModel;
            firePropertyChange("isIpsModel", oldValue, isIpsModel);
        }
    }

    public static class IPSROCCurveParametersBeanInfo extends BeanInfoEx2<IPSROCCurveParameters>
    {
        public IPSROCCurveParametersBeanInfo()
        {
            super(IPSROCCurveParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            add("dbSelector");
            property( "trackPath" ).inputElement( Track.class ).add();
            add("minRegionLength");
            add("aroundSummit");
            property( "matrixPath" ).inputElement( FrequencyMatrix.class ).add();
            add( "isIpsModel" );
            property( "outputPath" ).outputElement( FolderCollection.class ).add();
        }
    }
}
