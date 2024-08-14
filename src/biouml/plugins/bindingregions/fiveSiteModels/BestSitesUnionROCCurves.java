package biouml.plugins.bindingregions.fiveSiteModels;

import java.util.List;
import java.util.Map;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.bindingregions.resources.MessageBundle;
import biouml.plugins.bindingregions.utils.ChipSeqPeak;
import biouml.plugins.bindingregions.utils.SiteModelTypesSelector;
import biouml.plugins.bindingregions.utils.SiteModelsComparison;
import biouml.plugins.bindingregions.utils.SiteModelsComparisonUtils;
import biouml.plugins.bindingregions.utils.TableUtils;
import biouml.plugins.bindingregions.utils.TrackInfo;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceRegion;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.bsa.analysis.WeightMatrixCollection;
import ru.biosoft.bsa.analysis.WeightMatrixModel;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.bean.BeanInfoEx2;

/**
 * @author lan
 * FiveSiteModels/0. Best sites: ROC-curves; union of best sites in single chip-seq track; summit(yes/no); filtration (yes/no); matrix derivation;
 */
public class BestSitesUnionROCCurves extends AnalysisMethodSupport<BestSitesUnionROCCurves.BestSitesUnionROCCurvesParameters>
{
    public BestSitesUnionROCCurves(DataCollection<?> origin, String name)
    {
        super(origin, name, new BestSitesUnionROCCurvesParameters());
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        super.validateParameters();
        checkNotEmpty("siteModelTypes");
        checkRange("bestSitesPercentage", 1, 100);
    }

    @Override
    public DataCollection<?> justAnalyzeAndPut() throws Exception
    {
        log.info("Best sites: ROC-curves; union of best sites in single ChIP-Seq track; summit(yes/no); filtration (yes/no); matrix derivation;");
        DataElementPath pathToTrack = parameters.getTrackPath();
        DataElementPath pathToSequences = parameters.getDbSelector().getSequenceCollectionPath();
        DataElementPath pathToMatrix = parameters.getMatrixPath();
        int minimalLengthOfSequenceRegion = parameters.getMinRegionLength();
        int percentage = parameters.getBestSitesPercentage();
        DataElementPath pathToFiltrationMatrix = parameters.getFiltrationMatrixPath();
        String[] siteModelTypes = parameters.getSiteModelTypes();
        DataElementPath pathToOutputs = parameters.getOutputPath();
        boolean isAroundSummit = parameters.isAroundSummit();
        boolean areBothStrands = true;

        // 1.
        log.info("Read ChIP-Seq peaks in track and their sequences");
        Object[] objects = ChipSeqPeak.getLinearSequencesForChipSeqPeaks(pathToTrack, pathToSequences, isAroundSummit,
                minimalLengthOfSequenceRegion, jobControl, 0, 30);
        Sequence[] sequences = (Sequence[])objects[0];
        if( pathToFiltrationMatrix != null )
        {
            int size = sequences.length;
            FrequencyMatrix matrix = pathToFiltrationMatrix.getDataElement(FrequencyMatrix.class);
            sequences = getFilteredSequences(sequences, matrix);
            log.info(100 * ( size - sequences.length ) / size + "% of sequences were filtered out; size of filtered sample = "
                    + sequences.length);
        }

        // 2.
        log.info("Calculate ROC-curves for given matrix");
        String tableNameSuffix = "";
        isAroundSummit = (boolean)objects[1];
        if( isAroundSummit )
            tableNameSuffix += "_summit";
        if( pathToFiltrationMatrix != null )
            tableNameSuffix += "_filtration";
        FrequencyMatrix newMatrix = calculateAndWriteROCcurvesOnBestSites(pathToMatrix.getDataElement(FrequencyMatrix.class), siteModelTypes, sequences, areBothStrands, percentage, pathToTrack, pathToOutputs, tableNameSuffix);
        jobControl.setPreparedness(65);
        if( jobControl.isStopped() ) return null;

        // 3.
        log.info("Calculate ROC-curves for revised matrix");
        if( newMatrix != null )
            calculateAndWriteROCcurvesOnBestSites(newMatrix, siteModelTypes, sequences, areBothStrands, percentage, pathToTrack, pathToOutputs, tableNameSuffix);
        jobControl.setPreparedness(100);
        return pathToOutputs.getDataCollection();
    }

    private FrequencyMatrix calculateAndWriteROCcurvesOnBestSites(FrequencyMatrix matrix, String[] siteModelTypes, Sequence[] sequences, boolean areBothStrands, int percentage, DataElementPath pathToTrack, DataElementPath pathToOutputs, String tableNameSuffix) throws Exception
    {
        SiteModelsComparison smc = new SiteModelsComparison(siteModelTypes, matrix);
        Sequence[] seqs = smc.getUnionOfBestSequences(sequences, true, percentage);
        if( seqs == null || seqs.length < 5 )
            return null;
        Object[] objects = smc.getChartWithROCcurves(seqs, areBothStrands, true, false);
        TrackInfo trackInfo = new TrackInfo(pathToTrack.getDataElement(Track.class));
        String tableName = "ROCcurve" + "_for_" + matrix.getName() + "_in_" + trackInfo.getTfClass() + "_" + pathToTrack.getName()
                + "_percentage_" + percentage + "_size_" + seqs.length + tableNameSuffix;
        DataCollectionUtils.createFoldersForPath(pathToOutputs.getChildPath(""));
        TableUtils.addChartToTable(tableName, (Chart)objects[0], pathToOutputs.getChildPath(tableName));
        log.info("Write AUCs into table");
        Map<String, Double> siteModelTypeAndAUC = (Map<String, Double>)objects[1];
        tableName = matrix.getName().contains(SiteModelsComparisonUtils.REVISED) ? SiteModelsComparisonUtils.AUCS_REVISED
                : SiteModelsComparisonUtils.AUCS;
        reWriteTableWithAUCs(pathToOutputs, tableName, trackInfo.getTfClass(), pathToTrack.getName(), trackInfo.getNumberOfSites(),
                matrix.getName(), percentage, siteModelTypeAndAUC);
        log.info("Identification of matrix from best sites...");
        List<Sequence> sitesSeqs = smc.getSequencesOfBestSites(sequences, areBothStrands, percentage);
        if( sitesSeqs == null || sitesSeqs.size() < 5 )
            return null;
        DataCollection<FrequencyMatrix> matrixLib = WeightMatrixCollection.createMatrixLibrary(pathToOutputs.getChildPath("Matrix"), log);
        String newMatrixName = matrix.getName() + "_" + SiteModelsComparisonUtils.REVISED + "_" + pathToTrack.getName() + "_percentage_"
                + percentage;
        FrequencyMatrix newMatrix = new FrequencyMatrix(matrixLib, newMatrixName, matrix);
        newMatrix.updateFromSequences(sitesSeqs);
        matrixLib.put(newMatrix);
        CollectionFactoryUtils.save(matrixLib);
        return newMatrix;
    }

    public static void reWriteTableWithAUCs(DataElementPath pathToTables, String tableName, String tfClass, String nameOfSequenceSet,
            int sizeOfSequenceSet, String matrixName, int percentage, Map<String, Double> siteModelTypeAndAUC) throws Exception
    {
        DataElementPath pathToTable = pathToTables.getChildPath(tableName);
        TableDataCollection table = pathToTable.optDataElement(TableDataCollection.class);
        if( table == null )
        {
            table = TableDataCollectionUtils.createTableDataCollection(pathToTable);
            table.getColumnModel().addColumn("TF-class", String.class);
            table.getColumnModel().addColumn("Name of sequence set", String.class);
            table.getColumnModel().addColumn(SiteModelsComparisonUtils.SIZE_OF_SEQUENCE_SET, Integer.class);
            table.getColumnModel().addColumn("Matrix name", String.class);
            table.getColumnModel().addColumn(SiteModelsComparisonUtils.PERCENTAGE_OF_BEST_SITES, Integer.class);
            for( String siteModelType : siteModelTypeAndAUC.keySet() )
                table.getColumnModel().addColumn(SiteModelsComparisonUtils.AUC_FOR + siteModelType, Double.class);
        }
        Object[] objects = new Object[5 + siteModelTypeAndAUC.size()];
        objects[0] = tfClass;
        objects[1] = nameOfSequenceSet;
        objects[2] = sizeOfSequenceSet;
        objects[3] = matrixName;
        objects[4] = percentage;
        int i = 4;
        for( Double auc : siteModelTypeAndAUC.values() )
            objects[++i] = auc;
        TableDataCollectionUtils.addRow(table, Integer.toString(table.getSize()), objects);
        table.finalizeAddition();
        CollectionFactoryUtils.save(table);
    }

    private Sequence[] getFilteredSequences(Sequence[] sequences, FrequencyMatrix filtrationMatrix)
    {
        if( filtrationMatrix == null )
            return sequences;
        FrequencyMatrix matrix = new FrequencyMatrix(null, filtrationMatrix.getName() + "I dont use this name", filtrationMatrix);
        WeightMatrixModel weightMatrixModel = new WeightMatrixModel("I dont use this name", null, matrix, 0.0);
        double threshold = weightMatrixModel.getMaxScore() * 0.9;
        return SequenceRegion.withReversed( sequences ).filter( seq -> weightMatrixModel.findBestSite( seq ).getScore() <= threshold )
                .toArray( Sequence[]::new );
    }

    public static class BestSitesUnionROCCurvesParameters extends AbstractFiveSiteModelsParameters
    {
        private DataElementPath filtrationMatrixPath;

        @PropertyName ( MessageBundle.PN_FILTRATION_MATRIX_PATH )
        @PropertyDescription ( MessageBundle.PD_FILTRATION_MATRIX_PATH )
        public DataElementPath getFiltrationMatrixPath()
        {
            return filtrationMatrixPath;
        }

        public void setFiltrationMatrixPath(DataElementPath filtrationMatrixPath)
        {
            Object oldValue = this.filtrationMatrixPath;
            this.filtrationMatrixPath = filtrationMatrixPath;
            firePropertyChange("filtrationMatrixPath", oldValue, filtrationMatrixPath);
        }
    }

    public static class BestSitesUnionROCCurvesParametersBeanInfo extends BeanInfoEx2<BestSitesUnionROCCurvesParameters>
    {
        public BestSitesUnionROCCurvesParametersBeanInfo()
        {
            super(BestSitesUnionROCCurvesParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            property( "trackPath" ).inputElement( Track.class ).add();
            add("dbSelector");
            add("aroundSummit");
            add("minRegionLength");
            add("bestSitesPercentage");
            add("siteModelTypes", SiteModelTypesSelector.class);
            property( "matrixPath" ).inputElement( FrequencyMatrix.class ).add();
            property( "filtrationMatrixPath" ).inputElement( FrequencyMatrix.class ).canBeNull().add();
            property( "outputPath" ).outputElement( FolderCollection.class ).add();
        }
    }
}
