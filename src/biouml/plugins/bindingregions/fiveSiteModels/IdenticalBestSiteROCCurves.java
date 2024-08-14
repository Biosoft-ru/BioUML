package biouml.plugins.bindingregions.fiveSiteModels;

import java.util.List;
import java.util.Map;

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
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.bsa.analysis.WeightMatrixCollection;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.bean.BeanInfoEx2;

/**
 * @author lan
 * "ROC-curves: identical best sites in ChIP-Seq peaks; summit(yes/no); matrix derivation"
 */
public class IdenticalBestSiteROCCurves extends AnalysisMethodSupport<IdenticalBestSiteROCCurves.IdenticalBestSiteROCCurvesParameters>
{
    public IdenticalBestSiteROCCurves(DataCollection<?> origin, String name)
    {
        super(origin, name, new IdenticalBestSiteROCCurvesParameters());
    }
    
    @Override
    public DataCollection<?> justAnalyzeAndPut() throws Exception
    {
        log.info("ROC-curves: identical best sites in ChIP-Seq peaks; summit(yes/no); matrix derivation");
        DataElementPath pathToSequences = parameters.getDbSelector().getSequenceCollectionPath();
        DataElementPath pathToChipSeqTrack = parameters.getTrackPath();
        boolean isAroundSummit = parameters.isAroundSummit();
        int minimalLengthOfSequenceRegion = parameters.getMinRegionLength();
        DataElementPath pathToMatrix = parameters.getMatrixPath();
        String[] siteModelTypes = parameters.getSiteModelTypes();
        DataElementPath pathToOutputs = parameters.getOutputPath();
        boolean areBothStrands = true;
        
        // 1.
        log.info("Read ChIP-Seq peaks and their sequences");
        Object[] objects = ChipSeqPeak.getLinearSequencesForChipSeqPeaks(pathToChipSeqTrack, pathToSequences, isAroundSummit, minimalLengthOfSequenceRegion, jobControl, 0, 30);
        Sequence[] sequences = (Sequence[])objects[0];
        isAroundSummit = (boolean)objects[1];
        
        // 2.
        log.info("Calculate ROC-curves for given matrix");
        FrequencyMatrix matrix = pathToMatrix.getDataElement(FrequencyMatrix.class);
        String tableNameSuffix = isAroundSummit ? "_summit" : "";
        DataCollectionUtils.createFoldersForPath(pathToOutputs.getChildPath(""));
        FrequencyMatrix newMatrix = calculateAndWriteROCcurves(matrix, siteModelTypes, sequences, areBothStrands, pathToChipSeqTrack, pathToOutputs, tableNameSuffix);
        jobControl.setPreparedness(65);
        if( jobControl.isStopped() ) return null;
        
        // 3.
        log.info("Calculate ROC-curves for revised matrix");
        if( newMatrix != null )
            calculateAndWriteROCcurves(newMatrix, siteModelTypes, sequences, areBothStrands, pathToChipSeqTrack, pathToOutputs, tableNameSuffix);
        jobControl.setPreparedness(100);
        return pathToOutputs.getDataCollection();
    }
    
    private FrequencyMatrix calculateAndWriteROCcurves(FrequencyMatrix matrix, String[] siteModelTypes, Sequence[] sequences, boolean areBothStrands, DataElementPath pathToTrack, DataElementPath pathToOutputs, String tableNameSuffix) throws Exception
    {
        SiteModelsComparison smc = new SiteModelsComparison(siteModelTypes, matrix);
        Sequence[] seqs = smc.getSequencesWithIdenticalBestSites(sequences, areBothStrands);
        if( seqs == null || seqs.length < 5 ) return null;
        Object[] objects = smc.getChartWithROCcurves(seqs, areBothStrands, true, false);
        String tfClass = (new TrackInfo(pathToTrack.getDataElement(Track.class))).getTfClass();
        String tableName = "ROCcurves_for_" + matrix.getName() + "_in_" + tfClass + "_" + pathToTrack.getName() + "_size_" + seqs.length + tableNameSuffix;
        TableUtils.addChartToTable(tableName, (Chart)objects[0], pathToOutputs.getChildPath(tableName));
        log.info("Write AUCs into table");
        tableName = matrix.getName().contains(SiteModelsComparisonUtils.REVISED) ? SiteModelsComparisonUtils.AUCS_REVISED : SiteModelsComparisonUtils.AUCS;
        reWriteTableWithAUCs(pathToOutputs, tableName, tfClass, pathToTrack.getName(), sequences.length, seqs.length, matrix.getName(), (Map<String, Double>)objects[1]);
        log.info("Identification of matrix from best sites...");
        List<Sequence> sitesSeqs = smc.getSequencesOfIdenticalBestSites(seqs, areBothStrands);
        if( sitesSeqs == null || sitesSeqs.size() < 5 ) return null;
        DataCollection<FrequencyMatrix> matrixLib = WeightMatrixCollection.createMatrixLibrary(pathToOutputs.getChildPath("Matrix"), log);
        String newMatrixName = matrix.getName() + "_" + SiteModelsComparisonUtils.REVISED + "_" + pathToTrack.getName();
        FrequencyMatrix newMatrix = new FrequencyMatrix(matrixLib, newMatrixName, matrix);
        newMatrix.updateFromSequences(sitesSeqs);
        matrixLib.put(newMatrix);
        CollectionFactoryUtils.save(matrixLib);
        return newMatrix;
    }

    private void reWriteTableWithAUCs(DataElementPath pathToTables, String tableName, String tfClass, String nameOfSequenceSet, int sizeOfSequenceSet, int sizeOfSelectedSequenceSet, String matrixName, Map<String, Double> siteModelTypeAndAUC) throws Exception
    {
        DataElementPath pathToTable = pathToTables.getChildPath(tableName);
        TableDataCollection table = pathToTable.optDataElement(TableDataCollection.class);
        if( table == null )
        {
            table = TableDataCollectionUtils.createTableDataCollection(pathToTable);
            table.getColumnModel().addColumn("TF-class", String.class);
            table.getColumnModel().addColumn("Name of sequence set", String.class);
            table.getColumnModel().addColumn(SiteModelsComparisonUtils.SIZE_OF_SEQUENCE_SET, Integer.class);
            table.getColumnModel().addColumn("Number of identical best sites", Integer.class);
            table.getColumnModel().addColumn("Matrix name", String.class);
            for( String siteModelType : siteModelTypeAndAUC.keySet() )
                table.getColumnModel().addColumn(SiteModelsComparisonUtils.AUC_FOR + siteModelType, Double.class);
        }
        Object[] objects = new Object[5 + siteModelTypeAndAUC.size()];
        objects[0] = tfClass;
        objects[1] = nameOfSequenceSet;
        objects[2] = sizeOfSequenceSet;
        objects[3] = sizeOfSelectedSequenceSet;
        objects[4] = matrixName;
        int i = 4;
        for( Double auc : siteModelTypeAndAUC.values() )
            objects[++i] = auc;
        TableDataCollectionUtils.addRow(table, Integer.toString(table.getSize()), objects);
        table.finalizeAddition();
        CollectionFactoryUtils.save(table);
    }

    public static class IdenticalBestSiteROCCurvesParameters extends AbstractFiveSiteModelsParameters
    {}
    
    public static class IdenticalBestSiteROCCurvesParametersBeanInfo extends BeanInfoEx2<IdenticalBestSiteROCCurvesParameters>
    {
        public IdenticalBestSiteROCCurvesParametersBeanInfo()
        {
            super(IdenticalBestSiteROCCurvesParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            add("dbSelector");
            property( "trackPath" ).inputElement( Track.class ).add();
            add("aroundSummit");
            add("minRegionLength");
            property( "matrixPath" ).inputElement( FrequencyMatrix.class ).add();
            add("siteModelTypes", SiteModelTypesSelector.class);
            property( "outputPath" ).outputElement( FolderCollection.class ).add();
        }
    }
}
