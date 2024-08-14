
package biouml.plugins.bindingregions.mrnaanalysis;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;

import biouml.plugins.bindingregions.mrnaanalysis.AbstractMrnaAnalysisParameters.FullDataSetEditor;
import biouml.plugins.bindingregions.mrnaanalysis.AbstractMrnaAnalysisParameters.MatrixTypeSelector;
import biouml.plugins.bindingregions.mrnaanalysis.AbstractMrnaAnalysisParameters.StartCodonTypeSelector;
import biouml.plugins.bindingregions.mrnautils.GeneTranscript;
import biouml.plugins.bindingregions.mrnautils.ParticularRiboSeq;
import biouml.plugins.bindingregions.resources.MessageBundle;
import biouml.plugins.bindingregions.utils.EnsemblUtils;
import biouml.plugins.bindingregions.utils.SiteModelsComparison;

import com.developmentontheedge.beans.PropertyDescriptorEx;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.bsa.analysis.IPSSiteModel;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.util.OptionEx;
import ru.biosoft.util.bean.BeanInfoEx2;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

/**
 * @author yura
 * Create data matrix with Ribo-Seq and mRNA features and sequence sample and write it as TableDataCollection
 */
public class RiboSeqAndMrnaFeaturesForming extends AnalysisMethodSupport<RiboSeqAndMrnaFeaturesForming.RiboSeqAndMrnaFeaturesFormingParameters>
{
    public RiboSeqAndMrnaFeaturesForming(DataCollection<?> origin, String name)
    {
        super(origin, name, new RiboSeqAndMrnaFeaturesFormingParameters());
    }
    
    @Override
    public TableDataCollection justAnalyzeAndPut() throws Exception
    {
        log.info("Create data matrix with Ribo-Seq and mRNA features and sequence sample and write it as TableDataCollection");
        String dataSetName = parameters.getDataSetName();
        DataElementPath pathToFolderWithDataSets = parameters.getPathToFolderWithDataSets();
        DataElementPath pathToSequences = parameters.getDbSelector().getSequenceCollectionPath();
        TEfromTwoTablesParameters tEfromTwoTablesParameters = parameters.getTeFromTwoTablesParameters();
        String startCodonType = parameters.getStartCodonType();
        int orderOfStartCodon = Math.max(1, parameters.getOrderOfStartCodon());
        String[] mrnaAndRiboSeqFeatureNames = GeneTranscript.insertOligsIntoFeatureNames(parameters.getMrnaAndRiboSeqFeatureNames());
        boolean doExcludeMissingData = parameters.getDoExcludeMissingData();
        SequenceSampleParameters sequenceSampleParameters = parameters.getSequenceSampleParameters();
        PathToMatrix[] pathsToMatrices = parameters.getPathsToMatrices();
        boolean arePathsToMatricesHidden = parameters.arePathsToMatricesHidden();
        DataElementPath pathToOutputTable = parameters.getPathToOutputTable();
               
        // 1. Input parameter correction on hiddenness
        if( parameters.isDbSelectorHidden() ) pathToSequences = null;
        if( parameters.isPathToFolderWithDataSetsHidden() ) pathToFolderWithDataSets = null;
        if( parameters.isStartCodonTypeHidden() ) startCodonType = null;
        if( parameters.isOrderOfStartCodonHidden() ) orderOfStartCodon = 0;
        if( parameters.areSequenceSampleParametersHidden() ) sequenceSampleParameters = null;
        boolean areFragmentsNearStartCodons = sequenceSampleParameters == null ? false : sequenceSampleParameters.getAreFragmentsNearStartCodons();
        int leftBoundaryOfMrnaFragments = sequenceSampleParameters == null ? 0 : sequenceSampleParameters.getLeftBoundaryOfMrnaFragments();
        int rightBoundaryOfMrnaFragments = sequenceSampleParameters == null ? 0 : sequenceSampleParameters.getRightBoundaryOfMrnaFragments();

        // 2. Data matrix creation
        boolean doCreateSequenceSample = false;
        if( ArrayUtils.contains(mrnaAndRiboSeqFeatureNames, EnsemblUtils.SEQUENCE_SAMPLE) )
        {
            doCreateSequenceSample = true;
            mrnaAndRiboSeqFeatureNames = (String[])ArrayUtils.removeElement(mrnaAndRiboSeqFeatureNames, EnsemblUtils.SEQUENCE_SAMPLE);
        }
        Object[] objects = createSiteModels(pathsToMatrices, arePathsToMatricesHidden, mrnaAndRiboSeqFeatureNames);
        ParticularRiboSeq prs = new ParticularRiboSeq(dataSetName, pathToFolderWithDataSets, tEfromTwoTablesParameters.getPathToTableWithMrnaSeqData(), tEfromTwoTablesParameters.getColumnNameWithMrnaSeqReadsNumber(), tEfromTwoTablesParameters.getMrnaSeqReadsThreshold(), tEfromTwoTablesParameters.getPathToTableWithRiboSeqData(), tEfromTwoTablesParameters.getColumnNameWithRiboSeqReadsNumber(), tEfromTwoTablesParameters.getRiboSeqReadsThreshold(), tEfromTwoTablesParameters.getColumnNameWithStartCodonPositions(), tEfromTwoTablesParameters.getColumnNameWithTranscriptNames(), pathToSequences, startCodonType, orderOfStartCodon, jobControl, 0, 50);
        objects = prs.getMrnaAndRiboseqFeaturesDataMatrixAndFeatureNamesAndSequenceSample((String[])objects[0], doExcludeMissingData, doCreateSequenceSample, areFragmentsNearStartCodons, leftBoundaryOfMrnaFragments, rightBoundaryOfMrnaFragments - leftBoundaryOfMrnaFragments + 1, (Map<String, IPSSiteModel>)objects[1], jobControl, 50, 95);
        double[][] dataMatrix = (double[][])objects[0];
        String[] transcriptNames = (String[])objects[1];
        String[] newMrnaAndRiboseqFeatureNames = (String[])objects[2];
        String[] sequenceSample = (String[])objects[3];
        // TODO : to correct this condition
        // if( dataMatrix == null && sequenceSample == null ) throw new Exception("Unable to compose data matrix or/and sequence sample");
        if( dataMatrix != null )
            log.info(" dimension = " + dataMatrix.length + " x " + dataMatrix[0].length);
        TableDataCollection table = ParticularRiboSeq.writeDataMatrixAndSequenceSampleIntoTable(dataMatrix, transcriptNames, newMrnaAndRiboseqFeatureNames, sequenceSample, areFragmentsNearStartCodons, leftBoundaryOfMrnaFragments, rightBoundaryOfMrnaFragments, pathToOutputTable.getParentPath(), pathToOutputTable.getName());
        jobControl.setPreparedness(100);
        return table;
    }
    
    private Object[] createSiteModels(PathToMatrix[] pathsToMatrices, boolean arePathsToMatricesHidden, String[] mrnaAndRiboSeqFeatureNames)
    {
        Map<String, IPSSiteModel> nameAndSiteModel = new HashMap<>();
        if( arePathsToMatricesHidden || pathsToMatrices == null ) return new Object[]{mrnaAndRiboSeqFeatureNames, nameAndSiteModel};
        String[] newMrnaAndRiboSeqFeatureNames = (String[])ArrayUtils.removeElement(mrnaAndRiboSeqFeatureNames, GeneTranscript.MATRICES_SCORES);
        int window = 50;
        for( PathToMatrix pathToMatrix : pathsToMatrices )
        {
            FrequencyMatrix matrix = pathToMatrix.getPathToMatrix().getDataElement(FrequencyMatrix.class);
            String typeOfMatrix = pathToMatrix.getTypeOfMatrix();
            String matrixName = matrix.getName();
            nameAndSiteModel.put(matrixName, (IPSSiteModel)SiteModelsComparison.getSiteModel(SiteModelsComparison.IPS_SITE_MODEL, matrix, 0.01, window));
            newMrnaAndRiboSeqFeatureNames = (String[])ArrayUtils.add(newMrnaAndRiboSeqFeatureNames, typeOfMatrix + matrixName);
        }
        return new Object[]{newMrnaAndRiboSeqFeatureNames, nameAndSiteModel};
    }

    public static class SequenceSampleParameters extends AbstractAnalysisParameters
    {
        private boolean areFragmentsNearStartCodons;
        private int leftBoundaryOfMrnaFragments = -100;
        private int rightBoundaryOfMrnaFragments = 0;
        
        @PropertyName(MessageBundle.PN_ARE_NEAR_START_CODONS)
        @PropertyDescription(MessageBundle.PD_ARE_NEAR_START_CODONS)
        public boolean getAreFragmentsNearStartCodons()
        {
            return areFragmentsNearStartCodons;
        }
        public void setAreFragmentsNearStartCodons(boolean areFragmentsNearStartCodons)
        {
            Object oldValue = this.areFragmentsNearStartCodons;
            this.areFragmentsNearStartCodons = areFragmentsNearStartCodons;
            firePropertyChange("areFragmentsNearStartCodons", oldValue, areFragmentsNearStartCodons);
        }
        
        @PropertyName(MessageBundle.PN_LEFT_BOUNDARY)
        @PropertyDescription(MessageBundle.PD_LEFT_BOUNDARY)
        public int getLeftBoundaryOfMrnaFragments()
        {
            return leftBoundaryOfMrnaFragments;
        }
        public void setLeftBoundaryOfMrnaFragments(int leftBoundaryOfMrnaFragments)
        {
            Object oldValue = this.leftBoundaryOfMrnaFragments;
            this.leftBoundaryOfMrnaFragments = leftBoundaryOfMrnaFragments;
            firePropertyChange("leftBoundaryOfMrnaFragments", oldValue, leftBoundaryOfMrnaFragments);
        }

        @PropertyName(MessageBundle.PN_RIGHT_BOUNDARY)
        @PropertyDescription(MessageBundle.PD_RIGHT_BOUNDARY)
        public int getRightBoundaryOfMrnaFragments()
        {
            return rightBoundaryOfMrnaFragments;
        }
        public void setRightBoundaryOfMrnaFragments(int rightBoundaryOfMrnaFragments)
        {
            Object oldValue = this.rightBoundaryOfMrnaFragments;
            this.rightBoundaryOfMrnaFragments = rightBoundaryOfMrnaFragments;
            firePropertyChange("rightBoundaryOfMrnaFragments", oldValue, rightBoundaryOfMrnaFragments);
        }
    }
    
    public static class SequenceSampleParametersBeanInfo extends BeanInfoEx2<SequenceSampleParameters>
    {
        public SequenceSampleParametersBeanInfo()
        {
            super(SequenceSampleParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            add("areFragmentsNearStartCodons");
            add("leftBoundaryOfMrnaFragments");
            add("rightBoundaryOfMrnaFragments");
        }
    }

    public static class TEfromTwoTablesParameters extends AbstractMrnaAnalysisParameters
    {
        public boolean areColumnNamesWithStartCodonPositionsAndTranscriptNamesHidden()
        {
            return ! ((RiboSeqAndMrnaFeaturesFormingParameters)getParent()).getDataSetName().equals(ParticularRiboSeq.TWO_TABLES_FOR_TIE);
        }
    }
    
    public static class TEfromTwoTablesParametersBeanInfo extends BeanInfoEx2<TEfromTwoTablesParameters>
    {
        public TEfromTwoTablesParametersBeanInfo()
        {
            super(TEfromTwoTablesParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            add(DataElementPathEditor.registerInput("pathToTableWithMrnaSeqData", beanClass, TableDataCollection.class, true));
            add(ColumnNameSelector.registerNumericSelector("columnNameWithMrnaSeqReadsNumber", beanClass, "pathToTableWithMrnaSeqData", false));
            add("mrnaSeqReadsThreshold");
            add(DataElementPathEditor.registerInput("pathToTableWithRiboSeqData", beanClass, TableDataCollection.class, true));
            add(ColumnNameSelector.registerNumericSelector("columnNameWithRiboSeqReadsNumber", beanClass, "pathToTableWithRiboSeqData", false));
            add("riboSeqReadsThreshold");
            addHidden(ColumnNameSelector.registerNumericSelector("columnNameWithStartCodonPositions", beanClass, "pathToTableWithRiboSeqData", true), "areColumnNamesWithStartCodonPositionsAndTranscriptNamesHidden");
            addHidden(ColumnNameSelector.registerSelector("columnNameWithTranscriptNames", beanClass, "pathToTableWithRiboSeqData", true), "areColumnNamesWithStartCodonPositionsAndTranscriptNamesHidden");
        }
    }

    public static class PathToMatrix extends OptionEx
    {
        private DataElementPath pathToMatrix; // = DataElementPath.create("");
        private String typeOfMatrix;
        
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
        
        @PropertyName(MessageBundle.PN_TYPE_OF_MATRIX)
        @PropertyDescription(MessageBundle.PD_TYPE_OF_MATRIX)
        public String getTypeOfMatrix()
        {
            return typeOfMatrix;
        }
        public void setTypeOfMatrix(String typeOfMatrix)
        {
            Object oldValue = this.typeOfMatrix;
            this.typeOfMatrix = typeOfMatrix;
            firePropertyChange("typeOfMatrix", oldValue, typeOfMatrix);
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
            // add(DataElementPathEditor.registerInput("pathToMatrix", beanClass, FrequencyMatrix.class));
            // add(DataElementPathEditor.registerInputChild("pathToMatrix", beanClass, FrequencyMatrix.class, true));
            add(DataElementPathEditor.registerInput("pathToMatrix", beanClass, FrequencyMatrix.class, true));
            add(new PropertyDescriptorEx("typeOfMatrix", beanClass), MatrixTypeSelector.class);
        }
    }

    public static class RiboSeqAndMrnaFeaturesFormingParameters extends AbstractMrnaAnalysisParameters
    {
        private boolean doExcludeMissingData;
        private SequenceSampleParameters sequenceSampleParameters;
        private TEfromTwoTablesParameters teFromTwoTablesParameters;
        private PathToMatrix[] pathsToMatrices = new PathToMatrix[]{new PathToMatrix()};
        
        public RiboSeqAndMrnaFeaturesFormingParameters()
        {
            setSequenceSampleParameters(new SequenceSampleParameters());
            setTeFromTwoTablesParameters(new TEfromTwoTablesParameters());
        }
        
        @PropertyName(MessageBundle.PN_DO_EXCLUDE_MISSING_DATA)
        @PropertyDescription(MessageBundle.PD_DO_EXCLUDE_MISSING_DATA)
        public boolean getDoExcludeMissingData()
        {
            return doExcludeMissingData;
        }
        public void setDoExcludeMissingData(boolean doExcludeMissingData)
        {
            Object oldValue = this.doExcludeMissingData;
            this.doExcludeMissingData = doExcludeMissingData;
            firePropertyChange("doExcludeMissingData", oldValue, doExcludeMissingData);
        }

        @PropertyName("Sequence sample")
        public SequenceSampleParameters getSequenceSampleParameters()
        {
            return sequenceSampleParameters;
        }
        public void setSequenceSampleParameters(SequenceSampleParameters sequenceSampleParameters)
        {
            Object oldValue = this.sequenceSampleParameters;
            this.sequenceSampleParameters = withPropagation(this.sequenceSampleParameters, sequenceSampleParameters);
            firePropertyChange("sequenceSampleParameters", oldValue, sequenceSampleParameters);
        }
        
        @PropertyName("Two tables for calculation of translation (initiation) efficiency, TE (TIE)")
        public TEfromTwoTablesParameters getTeFromTwoTablesParameters()
        {
            return teFromTwoTablesParameters;
        }
        public void setTeFromTwoTablesParameters(TEfromTwoTablesParameters teFromTwoTablesParameters)
        {
            Object oldValue = this.teFromTwoTablesParameters;
            this.teFromTwoTablesParameters = withPropagation(this.teFromTwoTablesParameters, teFromTwoTablesParameters);
            firePropertyChange("teFromTwoTablesParameters", oldValue, teFromTwoTablesParameters);
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
        
        public boolean isTeFromTwoTablesParametersHidden()
        {
            String dataSetName = getDataSetName();
            return ! (dataSetName.equals(ParticularRiboSeq.TWO_TABLES_FOR_TE) || dataSetName.equals(ParticularRiboSeq.TWO_TABLES_FOR_TIE));
        }
        
        public boolean areSequenceSampleParametersHidden()
        {
            return( ! ArrayUtils.contains(getMrnaAndRiboSeqFeatureNames(), EnsemblUtils.SEQUENCE_SAMPLE));
        }
        
        public boolean arePathsToMatricesHidden()
        {
            return( ! ArrayUtils.contains(getMrnaAndRiboSeqFeatureNames(), GeneTranscript.MATRICES_SCORES));
        }
    }

    public static class FeatureNamesSelector extends GenericMultiSelectEditor
    {
        @Override
        protected String[] getAvailableValues()
        {
            try
            {
                String dataSetName = ((RiboSeqAndMrnaFeaturesFormingParameters)getBean()).getDataSetName();
                String[] featureNames = GeneTranscript.getMrnaFeatureNames();
                return ParticularRiboSeq.getAvailableRiboSeqFeatureNames(dataSetName).prepend(featureNames).prepend(EnsemblUtils.SEQUENCE_SAMPLE).toArray(String[]::new);
            }
            catch( Exception e )
            {
                return new String[] {"(please select feature names)"};
            }
        }
    }

    
    public static class RiboSeqAndMrnaFeaturesFormingParametersBeanInfo extends BeanInfoEx2<RiboSeqAndMrnaFeaturesFormingParameters>
    {
        public RiboSeqAndMrnaFeaturesFormingParametersBeanInfo()
        {
            super(RiboSeqAndMrnaFeaturesFormingParameters.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add(new PropertyDescriptorEx("dataSetName", beanClass), FullDataSetEditor.class);
            addHidden("dbSelector", "isDbSelectorHidden");
            addHidden("teFromTwoTablesParameters", "isTeFromTwoTablesParametersHidden");
            addHidden(DataElementPathEditor.registerInputChild("pathToFolderWithDataSets", beanClass, DataCollection.class, true), "isPathToFolderWithDataSetsHidden");
            addHidden(new PropertyDescriptorEx("startCodonType", beanClass), StartCodonTypeSelector.class, "isStartCodonTypeHidden");
            addHidden("orderOfStartCodon", "isOrderOfStartCodonHidden");
            add("mrnaAndRiboSeqFeatureNames", FeatureNamesSelector.class);
            addHidden("sequenceSampleParameters", "areSequenceSampleParametersHidden");
            addHidden("pathsToMatrices", "arePathsToMatricesHidden");
            add("doExcludeMissingData");
            add(DataElementPathEditor.registerOutput("pathToOutputTable", beanClass, TableDataCollection.class, false));
        }
    }
}
