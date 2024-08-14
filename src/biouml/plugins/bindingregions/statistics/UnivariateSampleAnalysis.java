
package biouml.plugins.bindingregions.statistics;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.commons.lang.ArrayUtils;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.bindingregions.resources.MessageBundle;
import biouml.plugins.bindingregions.statistics.AbstractStatisticalAnalysisParameters.SmoothingParameters;
import biouml.plugins.bindingregions.utils.MatrixUtils;
import biouml.plugins.bindingregions.utils.StatUtil;
import biouml.plugins.bindingregions.utils.TableUtils;
import biouml.plugins.bindingregions.utils.StatUtil.DensityEstimation;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

/**
 * @author yura
 * Univariate sample analysis : analysis of univariate sample represented by given column in the input data matrix
 */
public class UnivariateSampleAnalysis extends AnalysisMethodSupport<UnivariateSampleAnalysis.UnivariateSampleAnalysisParameters>
{
    public static final String OUTPUT_1_CHART_WITH_VARIABLE_DENSITY = "Write chart with variable density into table";
    public static final String OUTPUT_2_NORMAL_MIXTURE = "Identification of normal mixture";
    
    public UnivariateSampleAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new UnivariateSampleAnalysisParameters());
    }
    
    @Override
    public DataCollection<?> justAnalyzeAndPut() throws Exception
    {
        log.info("Univariate sample analysis : analysis of univariate sample represented by given column in the input data matrix");
        
        DataElementPath pathToTableWithSample = parameters.getPathToTableWithSample();
        String columnNameWithSample = parameters.getColumnNameWithSample();
        String[] outputOptions = parameters.getOutputOptions();
        int mixtureComponentsNumber = Math.max(2, parameters.getMixtureComponentsNumber());
        SmoothingParameters smoothingParameters = parameters.getSmoothingParameters();
        DataElementPath pathToOutputs = parameters.getOutputPath();

        // 1. Input parameter correction on hiddenness
        if( parameters.isMixtureComponentsNumberHidden() )
            mixtureComponentsNumber = 0;
        
        // 2.
        log.info("Read sample in table column");
        TableDataCollection table = pathToTableWithSample.getDataElement(TableDataCollection.class);
        double[] sample = TableUtils.readGivenColumnInDoubleTableAsArray(table, columnNameWithSample);
        String[] sampleElementNames = TableUtils.readRowNamesInTable(table);
        Object[] objects = MatrixUtils.removeObjectsWithMissingData(sampleElementNames, sample);
        jobControl.setPreparedness(20);
        if( jobControl.isStopped() ) return null;

        // 3. Implementation
        log.info("Perform the analysis");
        DataCollectionUtils.createFoldersForPath(pathToOutputs.getChildPath(""));
        implementAnalysis((double[])objects[1], (String[])objects[0], outputOptions, columnNameWithSample, smoothingParameters.getSmoothingWindowWidthType(), smoothingParameters.getGivenSmoothingWindowWidth(), mixtureComponentsNumber, pathToOutputs, jobControl, 20, 100);
        return pathToOutputs.getDataCollection();
    }

    private void implementAnalysis(double[] sample, String[] sampleElementNames, String[] outputOptions, String columnNameWithSample, String smoothingWindowWidthType, double givenWindow, int mixtureComponentsNumber, DataElementPath pathToOutputs, AnalysisJobControl jobControl, int from, int to) throws Exception
    {
        int difference = to - from; 
        for( int i = 0; i < outputOptions.length; i++ )
        {
            switch( outputOptions[i] )
            {
                case OUTPUT_1_CHART_WITH_VARIABLE_DENSITY : writeChartWithSampleDensity(columnNameWithSample, sample, true, smoothingWindowWidthType, givenWindow, pathToOutputs, "chart_density"); break;
                case OUTPUT_2_NORMAL_MIXTURE              : inplementNormalMixture(columnNameWithSample, sample, sampleElementNames, mixtureComponentsNumber, true, smoothingWindowWidthType, givenWindow, pathToOutputs, "mixtureComponents", "chart_density_normal_components_" + mixtureComponentsNumber, "indicesOfMixtureComponents"); break;
                default                                   : throw new Exception(outputOptions[i] + " : is not supported in our analysis currently");
                
            }
            if( jobControl != null )
                jobControl.setPreparedness(from + (i + 1) * difference / outputOptions.length);
        }
    }

    private void inplementNormalMixture(String variableName, double[] sample, String[] sampleElementNames, int mixtureComponentsNumber, Boolean doAddTwoZeroPoints, String windowSelector, Double givenWindow, DataElementPath pathToOutputs, String mixtureContentTableName, String chartTableName, String indicesOfComponentsTableName)
    {
        Map<Integer, Object[]> indexAndObjects = StatUtil.DistributionMixture.getNormalMixture(sample, mixtureComponentsNumber, 1000, new Random(1));
        writeTableAboutMixtureComponents(indexAndObjects, pathToOutputs, mixtureContentTableName);
        writeTableWithIndicesOfComponents(indexAndObjects, sample, sampleElementNames, pathToOutputs, indicesOfComponentsTableName);
        writeChartWithComponentDensities(variableName, sample, indexAndObjects, doAddTwoZeroPoints, windowSelector, givenWindow, pathToOutputs.getChildPath(chartTableName));
    }
    
    private void writeTableWithIndicesOfComponents(Map<Integer, Object[]> indexAndObjects, double[] sample, String[] sampleElementNames, DataElementPath pathToOutputs, String tableName)
    {
        int[] indicesOfMixtureComponents = getIndicesOfMixtureComponents(indexAndObjects, sample);
        TableUtils.writeIntegerTable(indicesOfMixtureComponents, sampleElementNames, "Indices of mixture components", pathToOutputs, tableName);
    }

    private int[] getIndicesOfMixtureComponents(Map<Integer, Object[]> indexAndObjects, double[] sample)
    {
        int[] result = new int[sample.length];
        for( Entry<Integer, Object[]> entry : indexAndObjects.entrySet() )
        {
            int indexOfComponent = entry.getKey();
            Object[] objects = entry.getValue();
            if( objects.length != 1 )
            {
                int[] indicesForGivenComponent = (int[])objects[4];
                for( int i = 0; i < indicesForGivenComponent.length; i++ )
                    result[indicesForGivenComponent[i]] = indexOfComponent; 
            }
        }
        return result;
    }
    
    private void writeTableAboutMixtureComponents(Map<Integer, Object[]> indexAndObjects, DataElementPath pathToOutputs, String mixtureContentTable)
    {
        int numberOfComponents = indexAndObjects.size() - 1, index = 0;
        double[][] dataTable = new double[numberOfComponents][6];
        String[] rowNames = new String[numberOfComponents];
        for( Entry<Integer, Object[]> entry : indexAndObjects.entrySet() )
        {
            int componentIndex = entry.getKey();
            Object[] objects = entry.getValue();
            if( objects.length == 1 )
            {
                log.info("Number of implemented iterations = " + (int)objects[0]);
                continue;
            }
            rowNames[index] = "Component_" + Integer.toString(componentIndex);
            dataTable[index][0] = ((double[])objects[3]).length;
            dataTable[index][1] = (double)objects[0];
            dataTable[index][2] = ((double[])objects[1])[0];
            dataTable[index][3] = ((double[])objects[1])[1];
            dataTable[index][4] = ((double[])objects[2])[0];
            dataTable[index++][5] = ((double[])objects[2])[1];
        }
        TableUtils.writeDoubleTable(dataTable, rowNames, new String[]{"Component size", "Component probability", "Mean of Component", "Sigma of component", "Simulated mean of Component", "Simulated sigma of component"}, pathToOutputs, mixtureContentTable);
    }
    
    private void writeChartWithComponentDensities(String variableName, double[] sample, Map<Integer, Object[]> indexAndObjectsWithMixtureComponents, Boolean doAddTwoZeroPoints, String windowSelector, Double givenWindow, DataElementPath pathToChartTable)
    {
        Map<String, double[]> componentNameAndValues = new HashMap<>();
        componentNameAndValues.put(variableName + " : total sample", sample);
        Map<String, Double> nameAndMultipliers = new HashMap<>();
        for( Entry<Integer, Object[]> entry : indexAndObjectsWithMixtureComponents.entrySet() )
        {
            int indexOfMixture = entry.getKey();
            Object[] objects = entry.getValue();
            if( objects.length == 1 ) continue;
            String componentName = "Mixture component " + indexOfMixture;
            double[] componentSample = (double[])objects[3];
            log.info("Mixture component size = " + componentSample.length);
            componentNameAndValues.put(componentName, componentSample);
            nameAndMultipliers.put(componentName, (double)componentSample.length / sample.length);
        }
        Chart chart = DensityEstimation.chartWithSmoothedDensities(componentNameAndValues, variableName, doAddTwoZeroPoints, nameAndMultipliers, windowSelector, givenWindow);
        TableUtils.addChartToTable(variableName, chart, pathToChartTable);
    }
    
    private void writeChartWithSampleDensity(String variableName, double[] sample, Boolean doAddTwoZeroPoints, String windowSelector, Double givenWindow, DataElementPath pathToOutputs, String tableName)
    {
        Map<String, double[]> sampleNameAndSample = new HashMap<>();
        sampleNameAndSample.put("", sample);
        Chart chart = DensityEstimation.chartWithSmoothedDensities(sampleNameAndSample, variableName, doAddTwoZeroPoints, null, windowSelector, givenWindow);
        TableUtils.addChartToTable(variableName, chart, pathToOutputs.getChildPath(tableName));
    }
    
    public static class UnivariateSampleAnalysisParameters extends AbstractStatisticalAnalysisParameters
    {
        private DataElementPath pathToTableWithSample;
        private String columnNameWithSample;
        private int mixtureComponentsNumber;
        
        public UnivariateSampleAnalysisParameters()
        {
            setSmoothingParameters(new SmoothingParameters());
        }
  
        @PropertyName(MessageBundle.PN_PATH_TO_TABLE_WITH_SAMPLE)
        @PropertyDescription(MessageBundle.PD_PATH_TO_TABLE_WITH_SAMPLE)
        public DataElementPath getPathToTableWithSample()
        {
            return pathToTableWithSample;
        }
        public void setPathToTableWithSample(DataElementPath pathToTableWithSample)
        {
            Object oldValue = this.pathToTableWithSample;
            this.pathToTableWithSample = pathToTableWithSample;
            firePropertyChange("pathToTableWithSample", oldValue, pathToTableWithSample);
            setColumnNameWithSample(ru.biosoft.table.columnbeans.ColumnNameSelector.getNumericColumn(pathToTableWithSample, getColumnNameWithSample()));
        }
        
        @PropertyName(MessageBundle.PN_COLUMN_NAME_WITH_SAMPLE)
        @PropertyDescription(MessageBundle.PD_COLUMN_NAME_WITH_SAMPLE)
        public String getColumnNameWithSample()
        {
            return columnNameWithSample;
        }
        public void setColumnNameWithSample(String columnNameWithSample)
        {
            Object oldValue = this.columnNameWithSample;
            this.columnNameWithSample = columnNameWithSample;
            firePropertyChange("columnNameWithSample", oldValue, columnNameWithSample);
        }

        @PropertyName(MessageBundle.PN_MIXTURE_COMPONENTS_NUMBER)
        @PropertyDescription(MessageBundle.PD_MIXTURE_COMPONENTS_NUMBER)
        public int getMixtureComponentsNumber()
        {
            return mixtureComponentsNumber;
        }
        public void setMixtureComponentsNumber(int mixtureComponentsNumber)
        {
            Object oldValue = this.mixtureComponentsNumber;
            this.mixtureComponentsNumber = mixtureComponentsNumber;
            firePropertyChange("mixtureComponentsNumber", oldValue, mixtureComponentsNumber);
        }
        
        public boolean isMixtureComponentsNumberHidden()
        {
            String[] outputOptions = getOutputOptions();
            return outputOptions == null || ! ArrayUtils.contains(outputOptions, OUTPUT_2_NORMAL_MIXTURE);
        }
    }

    public static class OutputOptionsSelector extends GenericMultiSelectEditor
    {
        @Override
        protected String[] getAvailableValues()
        {
            try
            {
                return new String[]{OUTPUT_1_CHART_WITH_VARIABLE_DENSITY, OUTPUT_2_NORMAL_MIXTURE};
            }
            catch( Exception e )
            {
                return new String[] {"(please select output options)"};
            }
        }
    }

    public static class UnivariateSampleAnalysisParametersBeanInfo extends BeanInfoEx2<UnivariateSampleAnalysisParameters>
    {
        public UnivariateSampleAnalysisParametersBeanInfo()
        {
            super(UnivariateSampleAnalysisParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            add(DataElementPathEditor.registerInput("pathToTableWithSample", beanClass, TableDataCollection.class, false));
            add(ColumnNameSelector.registerNumericSelector("columnNameWithSample", beanClass, "pathToTableWithSample", false));
            add("outputOptions", OutputOptionsSelector.class);
            add("smoothingParameters");
            addHidden("mixtureComponentsNumber", "isMixtureComponentsNumberHidden");
            add(DataElementPathEditor.registerOutput("outputPath", beanClass, FolderCollection.class, false));
        }
    }
}
