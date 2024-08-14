
package biouml.plugins.bindingregions.analysis;

// 08.04.22
import java.util.Map;
import java.util.Map.Entry;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.bindingregions.fiveSiteModels.AbstractFiveSiteModelsParameters;
import biouml.plugins.bindingregions.fiveSiteModels.AbstractFiveSiteModelsParameters.SiteModelTypeEditor;
import biouml.plugins.bindingregions.resources.MessageBundle;
import biouml.plugins.bindingregions.utils.SiteModelsComparison;
import biouml.plugins.bindingregions.utils.SiteModelsComparisonUtils;
import biouml.plugins.bindingregions.utils.SitePrediction;
import biouml.plugins.bindingregions.utils.TableUtils;
import biouml.plugins.gtrd.utils.EnsemblUtils;
import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.OptionEx;
import ru.biosoft.util.bean.BeanInfoEx2;

/**
 * @author yura
 *
 */

public class MatrixComparison extends AnalysisMethodSupport<MatrixComparison.MatrixComparisonParameters>
{
    public MatrixComparison(DataCollection<?> origin, String name)
    {
        super(origin, name, new MatrixComparisonParameters());
    }
    
    @Override
    public DataCollection<?> justAnalyzeAndPut()
    {
        log.info("Comparison of matrices by given site model on union of best sequences");
        
        String siteModelType = parameters.getSiteModelType();
        Integer window = parameters.getWindow();
        
        // 08.04.22
        // DataElementPath pathToTableWithSequenceSample = parameters.getPathToTableWithSequenceSample();
        // String nameOfTableColumnWithSequenceSample = parameters.getNameOfTableColumnWithSequenceSample();
        DataElementPath pathToSequences = parameters.getDbSelector().getSequenceCollectionPath();
        
        boolean areBothStrands = parameters.getAreBothStrands();
        int percentage = parameters.getBestSitesPercentage();
        boolean areAllMatrices = parameters.getAreAllMatrices();
        
        // 08.04.22
        DataElementPath trackPath = parameters.getTrackPath();
        
        PathToMatrix[] pathsToMatrices = parameters.getPathsToMatrices();
        DataElementPath pathToOutputs = parameters.getOutputPath();

        // 1.
        log.info("Create site model for each matrix");
        // DataElementPathSet matricesPathSet = parameters.getMatricesPathSet();
        // FrequencyMatrix[] frequencyMatrices = matricesPathSet.elements(FrequencyMatrix.class).toArray(FrequencyMatrix[]::new);
        FrequencyMatrix[] frequencyMatrices = new FrequencyMatrix[pathsToMatrices.length];
        for( int i = 0; i < pathsToMatrices.length; i++ )
            frequencyMatrices[i] = pathsToMatrices[i].getPathToMatrix().getDataElement(FrequencyMatrix.class);
        SiteModelsComparison smc = new SiteModelsComparison(siteModelType, window, frequencyMatrices);
        jobControl.setPreparedness(25);
        if( jobControl.isStopped() ) return null;

        // test
        for( FrequencyMatrix matrix : frequencyMatrices )
            log.info("matrix name = " + matrix.getName());

        // 2. Read sequence sample in track.
        // 08.04.22
        // Object[] objects = TableUtils.readGivenColumnInStringTableWithRowNames(pathToTableWithSequenceSample, nameOfTableColumnWithSequenceSample);
        // objects = SequenceSampleUtils.removeMissingDataInSequenceSample((String[])objects[0], (String[])objects[1]);
        // Sequence[] sequences = SequenceSampleUtils.transformSequenceSample((String[])objects[0], (String[])objects[1], frequencyMatrices[0].getAlphabet());
        log.info("Sequences are reading in track");
        Sequence[] sequences = getLinearSequencesWithGivenLengthForBestSites(trackPath, pathToSequences);
        log.info(" Sequences = ");
        for( int j = 0; j < 10; j++ )
        	log.info(" j = " + j + " sequences.length = " + sequences.length + " seq = " + sequences[j].toString());

        
        // 3.
        log.info("Create and write ROC-curves and AUCs");
        DataCollectionUtils.createFoldersForPath(pathToOutputs.getChildPath(""));

        // 0.8.04.22
        // createRocCurvesAndAUCs(smc, sequences, areBothStrands, percentage, areAllMatrices, nameOfTableColumnWithSequenceSample, pathToOutputs, "ROCcurves");
        createRocCurvesAndAUCs(smc, sequences, areBothStrands, percentage, areAllMatrices, "sequenceSampleName", pathToOutputs, "ROCcurves");
        
        return pathToOutputs.getDataCollection();
    }
    
	// 08.04.22
    private Sequence[] getLinearSequencesWithGivenLengthForBestSites(DataElementPath pathToInputTrack, DataElementPath pathToSequences)
    {
    	// For JUND
    	//DataElementPath pathToInputTrack = DataElementPath.create("data/Collaboration/yura_project/Data/Files/Cistrom_03/Ans01_metaClusters_by_IMETARA/JUND_no_treatment/Meta_clusters_for_all_peaks");

    	int numberOfBestSites = 3000, lengthOfSequenceRegion = 150;
    	return EnsemblUtils.getLinearSequencesWithGivenLengthForBestSites(pathToInputTrack, numberOfBestSites, lengthOfSequenceRegion, pathToSequences);
    }
    
    private void createRocCurvesAndAUCs(SiteModelsComparison smc, Sequence[] sequences, boolean areBothStrands, int percentage, boolean areAllMatrices, String sequenceSampleName, DataElementPath pathToOutputs, String tableName)
    {
        Sequence[] seqs = smc.getUnionOfBestSequences(sequences, areBothStrands, percentage);
        if( seqs == null || seqs.length < 5 ) return;
        log.info("Selected sequence sample size = " + seqs.length);
        jobControl.setPreparedness(50);
        Object[] objects = smc.getChartWithROCcurves(seqs, areBothStrands, true, areAllMatrices);
        jobControl.setPreparedness(95);
        Chart chart = (Chart)objects[0];
//      SiteModelsComparisonUtils.writeChartsIntoTable(tableName, chart, "chart", pathToOutputs, tableName);
        TableUtils.addChartToTable(tableName + "_percentage_" + percentage, chart, pathToOutputs.getChildPath(tableName));
        Map<String, Double> matrixNameAndAUC = (Map<String, Double>)objects[1];
        for( Entry<String, Double> entry : matrixNameAndAUC.entrySet() )
            log.info("name = " + entry.getKey() + " AUC = " + entry.getValue());
        matrixNameAndAUC.putIfAbsent(SitePrediction.ALL_MATRICES, Double.NaN);
        reWriteTableWithAUCs(pathToOutputs, SiteModelsComparisonUtils.AUCS, sequenceSampleName, seqs.length, percentage, matrixNameAndAUC);
        jobControl.setPreparedness(100);
    }

    private void reWriteTableWithAUCs(DataElementPath pathToOutputs, String tableName, String sequenceSampleName, int sequenceSampleSize, int percentage, Map<String, Double> matrixNameAndAUC)
    {
        DataElementPath pathToTable = pathToOutputs.getChildPath(tableName);
        TableDataCollection table = pathToTable.optDataElement(TableDataCollection.class);
        if( table == null )
        {
            table = TableDataCollectionUtils.createTableDataCollection(pathToTable);
            table.getColumnModel().addColumn("Name of sequence sample", String.class);
            table.getColumnModel().addColumn(SiteModelsComparisonUtils.SIZE_OF_SEQUENCE_SET, Integer.class);
            table.getColumnModel().addColumn(SiteModelsComparisonUtils.PERCENTAGE_OF_BEST_SITES, Integer.class);
            for( String matrixName : matrixNameAndAUC.keySet() )
                table.getColumnModel().addColumn(SiteModelsComparisonUtils.AUC_FOR + matrixName, Double.class);
        }
        Object[] objects = StreamEx.of(sequenceSampleName, sequenceSampleSize, percentage).append(matrixNameAndAUC.values()).toArray();
        TableDataCollectionUtils.addRow(table, Integer.toString(table.getSize()), objects);
        table.finalizeAddition();
        CollectionFactoryUtils.save(table);
    }

    public static class PathToMatrix extends OptionEx
    {
        private DataElementPath pathToMatrix;
        
        @PropertyName(MessageBundle.PN_MATRIX_PATH)
        @PropertyDescription(MessageBundle.PD_MATRIX_PATH)
        public DataElementPath getPathToMatrix()
        {
            return pathToMatrix;
        }
        public void setPathToMatrix(DataElementPath pathToMatrix)
        {
            Object oldValue = this.pathToMatrix;
            this.pathToMatrix = pathToMatrix;
            firePropertyChange("pathToMatrix", oldValue, pathToMatrix);
        }
    }
    
    public static class PathToMatrixBeanInfo extends BeanInfoEx2<PathToMatrix>
    {
        public PathToMatrixBeanInfo()
        {
            super(PathToMatrix.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add(DataElementPathEditor.registerInput("pathToMatrix", beanClass, FrequencyMatrix.class));
        }
    }
    
    public static class MatrixComparisonParameters extends AbstractFiveSiteModelsParameters
    {
        private boolean areAllMatrices;
        private PathToMatrix[] pathsToMatrices = new PathToMatrix[]{new PathToMatrix()};
        
        @PropertyName(MessageBundle.PN_ALL_MATRICES)
        @PropertyDescription(MessageBundle.PD_ALL_MATRICES)
        public boolean getAreAllMatrices()
        {
            return areAllMatrices;
        }
        public void setAreAllMatrices(boolean areAllMatrices)
        {
            Object oldValue = this.areAllMatrices;
            this.areAllMatrices = areAllMatrices;
            firePropertyChange("areAllMatrices", oldValue, areAllMatrices);
        }
        
        @PropertyName("Paths to matrices")
        public PathToMatrix[] getPathsToMatrices()
        {
            return pathsToMatrices;
        }
        public void setPathsToMatrices(PathToMatrix[] pathsToMatrices)
        {
            Object oldValue = this.pathsToMatrices;
            this.pathsToMatrices = pathsToMatrices;
            firePropertyChange("pathsToMatrices", oldValue, pathsToMatrices);
        }
    }

    public static class MatrixComparisonParametersBeanInfo extends BeanInfoEx2<MatrixComparisonParameters>
    {
        public MatrixComparisonParametersBeanInfo()
        {
            super(MatrixComparisonParameters.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
        	// 08.04.22
            add("dbSelector");

            add(new PropertyDescriptorEx("siteModelType", beanClass), SiteModelTypeEditor.class);
            addHidden("window", "isWindowHidden");
            
            // 08.04.22
//            add(DataElementPathEditor.registerInput("pathToTableWithSequenceSample", beanClass, TableDataCollection.class, false));
//            add(ColumnNameSelector.registerSelector("nameOfTableColumnWithSequenceSample", beanClass, "pathToTableWithSequenceSample", false));
            
            // 09.04.22
            //property("trackPath").inputElement(Track.class).add();
            add(DataElementPathEditor.registerInput("trackPath", beanClass, Track.class));

            add("areBothStrands");
            add("bestSitesPercentage");
            add("areAllMatrices");
            add("pathsToMatrices");
//          add(OptionEx.makeAutoProperty(DataElementPathEditor.registerOutput("outputPath", beanClass, FolderCollection.class), "$trackPath$"));
            add(DataElementPathEditor.registerOutput("outputPath", beanClass, FolderCollection.class, false));
            
            
        }
    }
}
