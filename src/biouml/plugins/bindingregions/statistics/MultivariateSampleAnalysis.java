
package biouml.plugins.bindingregions.statistics;

import org.apache.commons.lang.ArrayUtils;

import java.util.logging.Logger;

import com.developmentontheedge.beans.PropertyDescriptorEx;

import biouml.plugins.bindingregions.statistics.AbstractStatisticalAnalysisParameters.DataTransformationTypeSelector;
import biouml.plugins.bindingregions.statistics.AbstractStatisticalAnalysisParameters.VariableNamesSelector;
import biouml.plugins.bindingregions.utils.MatrixUtils;
import biouml.plugins.bindingregions.utils.MultivariateSample;
import biouml.plugins.bindingregions.utils.StatUtil.DensityEstimation;
import biouml.plugins.machinelearning.utils.DataMatrix;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

/**
 * @author yura
 * Analysis of multivariate sample represented by given data matrix
 *
 */
public class MultivariateSampleAnalysis extends AnalysisMethodSupport<MultivariateSampleAnalysis.MultivariateSampleAnalysisParameters>
{
    public static final String OUTPUT_1_WRITE_CORRELATION_MATRIX = "Write correlation matrix into table";
    public static final String OUTPUT_2_WRITE_CHARTS_WITH_VARIABLE_DENSITIES = "Write charts with variable densities into table";
    public static final String OUTPUT_3_WRITE_MEANS_AND_SIGMAS = "Write variable means and sigmas into table";
    public static final String OUTPUT_4_WRITE_COVARIANCE_MATRIX = "Write covariance matrix into table";
    public static final String OUTPUT_5_WRITE_MULTINORMALITY_TEST = "Write table with multinormality test (based on skewness and kurtosis)";
    public static final String OUTPUT_6_WRITE_TRANSFORMED_DATA = "Transform data and write table with transformed data matrix";
    
    public MultivariateSampleAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new MultivariateSampleAnalysisParameters());
    }
    
    @Override
    public DataCollection<?> justAnalyzeAndPut() throws Exception
    {
        log.info("Multivariate sample analysis : analysis of multivariate sample represented by given data matrix");
        DataElementPath pathToTableWithDataMatrix = parameters.getPathToTableWithDataMatrix();
        String[] variableNames = parameters.getVariableNames();
        String[] outputOptions = parameters.getOutputOptions();
        String dataTransformationType = parameters.getDataTransformationType();
        DataElementPath pathToOutputs = parameters.getOutputPath();

        DataCollectionUtils.createFoldersForPath(pathToOutputs.getChildPath(""));
        //Object[] objects = TableUtils.readDataSubMatrix(pathToTableWithDataMatrix, variableNames);
        DataMatrix dm = new DataMatrix(pathToTableWithDataMatrix, variableNames);
        Object[] objects = new Object[]{dm.getRowNames(), dm.getMatrix()};
        
        
        log.info("Dimension of initial data matrix : " + ((double[][])objects[1]).length + " x " + ((double[][])objects[1])[0].length);
        objects = MatrixUtils.removeRowsWithMissingData((String[])objects[0], (double[][])objects[1]);
        log.info("Dimension of final data matrix : " + ((double[][])objects[1]).length + " x " + ((double[][])objects[1])[0].length);
        jobControl.setPreparedness(10);
        if( jobControl.isStopped() ) return null;
        outputResults(outputOptions, (String[])objects[0], variableNames, (double[][])objects[1], dataTransformationType, pathToOutputs,  log, jobControl, 10, 100);
        return pathToOutputs.getDataCollection();
    }
    
    private static String[] getAvailableOutputTypes()
    {
        return new String[]{OUTPUT_1_WRITE_CORRELATION_MATRIX, OUTPUT_4_WRITE_COVARIANCE_MATRIX, OUTPUT_2_WRITE_CHARTS_WITH_VARIABLE_DENSITIES, OUTPUT_3_WRITE_MEANS_AND_SIGMAS, OUTPUT_5_WRITE_MULTINORMALITY_TEST, OUTPUT_6_WRITE_TRANSFORMED_DATA};
    }

    private void outputResults(String[] outputOptions, String[] objectNames, String[] variableNames, double[][] dataMatrix, String dataTransformationType, DataElementPath pathToOutputs,  Logger log, AnalysisJobControl jobControl, int from, int to) throws Exception
    {
        int difference = to - from;
        for( int i = 0; i < outputOptions.length; i++ )
        {
            switch( outputOptions[i] )
            {
                case OUTPUT_1_WRITE_CORRELATION_MATRIX             : log.info("Calculate correlation matrix and write it into table");
                                                                     MultivariateSample.writeTableWithCovarianceOrCorrelationMatrix(false, variableNames, dataMatrix, pathToOutputs, "correlationMatrix"); break;
                case OUTPUT_2_WRITE_CHARTS_WITH_VARIABLE_DENSITIES : log.info("Create charts with variable densities and write them into table");
                                                                     MultivariateSample.writeIndividualChartsWithSmoothedDensities(variableNames, dataMatrix, true, null, DensityEstimation.WINDOW_WIDTH_01, null, pathToOutputs, "charts_variableDensities"); break;
                case OUTPUT_3_WRITE_MEANS_AND_SIGMAS               : log.info("Calculate variable means and sigmas and write them into table");
                                                                     MultivariateSample.writeTableWithMeanAndSigma(variableNames, dataMatrix, pathToOutputs, "variableMeansAndSigmas"); break;
                case OUTPUT_4_WRITE_COVARIANCE_MATRIX              : log.info("Calculate covariance matrix and write it into table");
                                                                     MultivariateSample.writeTableWithCovarianceOrCorrelationMatrix(true, variableNames, dataMatrix, pathToOutputs, "covarianceMatrix"); break;
                case OUTPUT_5_WRITE_MULTINORMALITY_TEST            : log.info("Multinormality test");
                                                                     MultivariateSample.writeTableWithMultinormalityTest(dataMatrix, 1000, 0.00001, pathToOutputs, "multinormalityTest"); break;
                case OUTPUT_6_WRITE_TRANSFORMED_DATA               : log.info("Calculate transformed data matrix and write it into table");
                                                                     MultivariateSample.writeTableWithTransformedDataMatrix(dataTransformationType, objectNames, variableNames, dataMatrix, pathToOutputs, "dataMatrix_" + dataTransformationType); break;
            }
            if( jobControl != null )
                jobControl.setPreparedness(from + (i + 1) * difference / outputOptions.length);
        }
    }

    public static class MultivariateSampleAnalysisParameters extends AbstractStatisticalAnalysisParameters
    {
        @Override
        public boolean isDataTransformationTypeHidden()
        {
            String[] outputOptions = getOutputOptions();
            return ! ArrayUtils.contains(outputOptions, OUTPUT_6_WRITE_TRANSFORMED_DATA); 
        }
    }
    
    public static class OutputOptionsSelector extends GenericMultiSelectEditor
    {
        @Override
        protected String[] getAvailableValues()
        {
            try
            {
                return getAvailableOutputTypes();
            }
            catch( Exception e )
            {
                return new String[] {"(please select output options)"};
            }
        }
    }

    public static class MultivariateSampleAnalysisParametersBeanInfo extends BeanInfoEx2<MultivariateSampleAnalysisParameters>
    {
        public MultivariateSampleAnalysisParametersBeanInfo()
        {
            super(MultivariateSampleAnalysisParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            add(DataElementPathEditor.registerInput("pathToTableWithDataMatrix", beanClass, DataElement.class));
            add("variableNames", VariableNamesSelector.class);
            add("outputOptions", OutputOptionsSelector.class);
            addHidden(new PropertyDescriptorEx("dataTransformationType", beanClass), DataTransformationTypeSelector.class, "isDataTransformationTypeHidden");
            add(DataElementPathEditor.registerOutput("outputPath", beanClass, FolderCollection.class));
        }
    }
}
