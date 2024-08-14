
package biouml.plugins.bindingregions.statistics;

import java.util.Arrays;

import org.apache.commons.lang.ArrayUtils;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import com.developmentontheedge.beans.editors.StringTagEditor;

import biouml.plugins.bindingregions.statisticsutils.RegressionEngine;
import biouml.plugins.bindingregions.utils.DataMatrix;
import biouml.plugins.bindingregions.utils.LinearRegression;
import biouml.plugins.bindingregions.utils.LinearRegression.LSregression;
import biouml.plugins.bindingregions.utils.MatrixUtils;
import biouml.plugins.bindingregions.utils.TableUtils;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysis.Stat;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.ConstantResourceBundle;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

/**
 * @author yura
 * Extended LS-regression analysis
 */
public class LSregressionAnalysis extends AnalysisMethodSupport<LSregressionAnalysis.LSregressionAnalysisParameters>
{
    public static final String OPTION_01 = "Stepwise LS-regression with backward elimination";
    public static final String OPTION_02 = "Iterative detection of outliers";
    public static final String OPTION_03 = "Analysis of eigen values of covariance matrix";
    public static final String OPTION_04 = "Stepwise LS-regression with forward addition";
    public static final String OPTION_05 = "Under construction : Calculation of VIFs (variance inflation factors) for all regression coefficients";

    public LSregressionAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new LSregressionAnalysisParameters());
    }
    
    @Override
    public DataCollection<?> justAnalyzeAndPut() throws Exception
    {
        log.info("Extended LS-regression analysis.");
        String option = parameters.getOption();
        DataElementPath pathToDataMatrix = parameters.getPathToDataMatrix();
        String[] variableNames = parameters.getVariableNames();
        String responseName = parameters.getResponseName();
        DataElementPath pathToOutputFolder = parameters.getPathToOutputFolder();
        
        DataCollectionUtils.createFoldersForPath(pathToOutputFolder.getChildPath(""));

//        String[] dataMatrixExtensions = parameters.getDataMatrixExtensions();
//        String[] outputOptions = parameters.getOutputOptions();
//        double pValue = parameters.getPValue();
//        int maxIterations = parameters.getMaxIterations();
        
        // 1.
        log.info("Input data processing");
        Object[] objects = RegressionEngine.readDataSubMatrixAndResponse(pathToDataMatrix, variableNames, responseName);
        String[] objectNames = (String[])objects[0];
        double[][] dataMatrix = (double[][])objects[1];
        double[] response = (double[])objects[2];
        if( ! option.equals(OPTION_04) )
            variableNames = LinearRegression.addInterceptToRegression(variableNames, dataMatrix);

//        objects = LinearRegression.extendDataMatrix(variableNames, dataMatrix, dataMatrixExtensions);
//        variableNames = (String[])objects[0];
//        dataMatrix = (double[][])objects[1];

        // 2.
        log.info("Implementation of regression analysis");
//        int maxNumberOfIterations = MatrixUtils.DEFAULT_MAX_NUMBER_OF_ITERATIONS_FOR_INVERSE;
//        double eps = MatrixUtils.DEFAULT_EPS_FOR_INVERSE;
        int maxNumberOfIterations = 30000;
        double eps = 0.000000001;  // double eps = 1.0e-15;
        switch( option )
        {
            case OPTION_01 : implementStepwiseBackwardLSregression(dataMatrix, variableNames, response, maxNumberOfIterations, eps, pathToOutputFolder, 0, 100); break;
            case OPTION_02 : int numberOfSteps = 5;
                             // TODO: Consider the generalization of parameter thresholdCoefficient
                             double thresholdCoefficient = 3.0;
                             implementOutlierDetection(objectNames, variableNames, dataMatrix, response, numberOfSteps, maxNumberOfIterations, eps, thresholdCoefficient, pathToOutputFolder, 0, 100); break;
            case OPTION_03 : analysisOfEigenValues(dataMatrix, variableNames, maxNumberOfIterations, eps, pathToOutputFolder); break;
            case OPTION_04 : int maxNumberOfVariables = Math.max(1, parameters.getMaxNumberOfVariables());
                             implementStepwiseForwardLSregression(dataMatrix, variableNames, response, maxNumberOfVariables, maxNumberOfIterations, eps, pathToOutputFolder, 0, 100); break;
            case OPTION_05 : //TODO : under construction
            default        : throw new Exception("This  option '" + option + "' is not supported in our analysis currently");
        }
        
        return pathToOutputFolder.getDataCollection();
    }
    
    // For OPTION_04
    // INTERCEPT mast be absent in the input dataMatrix !!! 
    private void implementStepwiseForwardLSregression(double[][] dataMatrix, String[] variableNames, double[] response, int maxNumberOfVariables, int maxNumberOfIterations, double eps, DataElementPath pathToOutputFolder, int from, int to) throws Exception
    {
        int difference = to - from;
        double maxCorrelationOld = -1.1;
        if( dataMatrix[0].length < maxNumberOfVariables ) return;
        String[] variableNamesSelected = new String[]{LinearRegression.INTERCEPT};
        double[][] dataMatrixSelected = MatrixUtils.getMatrixWithEqualElements(dataMatrix.length, 1, 1.0);
        for( int j = 1; j <= maxNumberOfVariables; j++ )
        {
            variableNamesSelected = (String[])ArrayUtils.add(variableNamesSelected, "");
            MatrixUtils.addColumnToMatrix(dataMatrixSelected, response);
            double maxCorrelation = -1.1;
            int indexOfMaximalCorrelation = -1;
            
            // 1. Search for best correlation by adding iteratively single variable 
            for( int jj = 0; jj < variableNames.length; jj++ )
            {
                for( int i = 0; i < response.length; i++ )
                    dataMatrixSelected[i][j] = dataMatrix[i][jj];
                variableNamesSelected[j] = variableNames[jj];
                LSregression lsr = new LSregression(variableNamesSelected, null, dataMatrixSelected, null, response);
                
                //////// temp
                String s = "";
                for( int iii = 0; iii < variableNamesSelected.length; iii++)
                    s += " " + variableNamesSelected[iii];
                log.info("Variables : " + s);

                
                Object[] objects = lsr.getMultipleLinearRegressionByJacobiMethod(maxNumberOfIterations, eps, false);
                double correlation = Stat.pearsonCorrelation(response, (double[])objects[2]);
                if( correlation > maxCorrelation )
                {
                    maxCorrelation = correlation;
                    indexOfMaximalCorrelation = jj;
                }
            }
            
            // 2. Save result for best variable and remove best variable from available variables.
            log.info(" j = " + j + " maxCorrelation = " + maxCorrelation + " best additional variable = " + variableNames[indexOfMaximalCorrelation]);
            for( int i = 0; i < response.length; i++ )
                dataMatrixSelected[i][j] = dataMatrix[i][indexOfMaximalCorrelation];
            variableNamesSelected[j] = variableNames[indexOfMaximalCorrelation];
            DataElementPath dep = pathToOutputFolder.getChildPath("Variables_" + String.valueOf(j));
            DataCollectionUtils.createSubCollection(dep);
            LSregression lsr = new LSregression(variableNamesSelected, null, dataMatrixSelected, null, response);
            lsr.getResultsOfLSregression(maxNumberOfIterations, eps, dep);
            variableNames = (String[])ArrayUtils.remove(variableNames, indexOfMaximalCorrelation);
            MatrixUtils.removeGivenColumn(dataMatrix, indexOfMaximalCorrelation);
            jobControl.setPreparedness(from + j * difference / maxNumberOfVariables);
            if( maxCorrelationOld >= maxCorrelation ) return;
            maxCorrelationOld = maxCorrelation;
        }
    }

    // For OPTION_03
    private void analysisOfEigenValues(double[][] dataMatrix, String[] variableNames, int maxNumberOfIterations, double eps, DataElementPath pathToOutputFolder)
    {
        double[][] matrix = MatrixUtils.getProductOfTransposedMatrixAndMatrix(dataMatrix);
        // double[][] covarianceMatrix = MatrixUtils.getInverseSymmetricMatrixByJacobiMethod(matrix, maxNumberOfIterations, eps);
        Object[] objects = MatrixUtils.getSpectralDecompositionOfSymmetricMatrixByJacobiMethod(matrix, maxNumberOfIterations, eps);
        double[] eigenValues = (double[])objects[1];
        log.info("numberOfIterations = " + (int)objects[0]);
        TableUtils.writeDoubleTable(eigenValues, variableNames, "eigen_values", pathToOutputFolder, "eigen_values");
    }
    
    // For OPTION_01
    private void implementStepwiseBackwardLSregression(double[][] dataMatrix, String[] variableNames, double[] response, int maxNumberOfIterations, double eps, DataElementPath pathToOutputFolder, int from, int to) throws Exception
    {
        int difference = to - from, m = dataMatrix[0].length;
        if( m <= 2 ) return;
        String[] names = new String[m - 2];
        double[][] data = new double[m - 2][];
        for( int i = m; i > 1; i-- )
        {
            DataElementPath dep = pathToOutputFolder.getChildPath(String.valueOf(i));
            DataCollectionUtils.createSubCollection(dep);
            LSregression lsr = new LSregression(variableNames, null, dataMatrix, null, response);
            Object[] objects = lsr.getResultsOfLSregression(maxNumberOfIterations, eps, dep);
            jobControl.setPreparedness(from + (m + 1 - i) * difference / (m - 1));
            if( i == 2 )
            {
                TableUtils.writeDoubleTable(data, names, new String[]{"number of variables", "coefficient", "statistic", "p-values"}, pathToOutputFolder, "summaryOnRemovedVariables");
                return;
            }
            double[] coefficients = (double[])objects[0], statistics = (double[])objects[4], pValues = (double[])objects[5];
            double min = Math.abs(statistics[0]);
            int index = 0;
            for( int j = 1; j < statistics.length; j++ )
            {
                double x = Math.abs(statistics[j]);
                if( x < min )
                {
                    min = x;
                    index = j;
                }
            }
            names[m - i] = variableNames[index];
            data[m - i] = new double[] {i, coefficients[index], statistics[index], pValues[index]};
            variableNames = (String[])ArrayUtils.remove(variableNames, index);
            MatrixUtils.removeGivenColumn(dataMatrix, index);
        }
    }

    // For OPTION_02
    private void implementOutlierDetection(String[] objectNames, String[] variableNames, double[][] dataMatrix, double[] response, int numberOfSteps, int maxNumberOfIterations, double eps, double thresholdCoefficient, DataElementPath pathToOutputFolder, int from, int to) throws Exception
    {
        int difference = to - from, n = dataMatrix.length;
        if( n <= 2 ) return;
        char[] isOutlier1 = new char[n], isOutlier2 = new char[n];
        Arrays.fill(isOutlier1, '-');
        char switcher = '+';
        for( int i = 1; i <= numberOfSteps; i++ )
        {
            jobControl.setPreparedness(from + i * difference / numberOfSteps);
            DataElementPath dep = pathToOutputFolder.getChildPath("Step_" + String.valueOf(i));
            DataCollectionUtils.createSubCollection(dep);
            
            // 1. Calculate visOutlierInitial, isOutlierFinal; recalculate switcher.
            char[] isOutlierInitial = switcher == '+' ? isOutlier1 : isOutlier2;
            char[] isOutlierFinal = switcher == '+' ? isOutlier2 : isOutlier1;
            switcher = switcher == '+' ? '-' : '+';
            
            // 2. Calculate dataMatrixNew and responseNew.
            int nn = 0, index = 0;
            for( int j = 0; j < n; j++ )
                if( isOutlierInitial[j] == '-' )
                    nn++;
            
            // temp
            log.info("n = " + n + " nn = " + nn);
            
            if( nn <= variableNames.length ) return;
            double[][] dataMatrixNew = new double[nn][];
            double[] responseNew = new double[nn];
            for( int j = 0; j < n; j++ )
                if( isOutlierInitial[j] == '-' )
                {
                    dataMatrixNew[index] = dataMatrix[j];
                    responseNew[index] = response[j];
                    index++;
                }
            
            // 3. Estimate regression parameters and calculate isOutlierFinal
            LSregression lsr = new LSregression(variableNames, null, dataMatrixNew, null, responseNew);
            Object[] objects = lsr.getResultsOfLSregression(maxNumberOfIterations, eps, dep);
            double[] coefficients = (double[])objects[0];
            double[] predictions = LinearRegression.getPredictions(dataMatrix, coefficients);
            double sigma = Math.sqrt((double)objects[3]);
            for( int j = 0; j < n; j++ )
                isOutlierFinal[j] = Math.abs(predictions[j] - response[j]) > thresholdCoefficient * sigma ? '+' : '-';

            // 4. Write predictions into table.
            TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(dep.getChildPath("_predictions"));
            table.getColumnModel().addColumn("Response", Double.class);
            table.getColumnModel().addColumn("Prediction", Double.class);
            table.getColumnModel().addColumn("isOutlierInitial", String.class);
            table.getColumnModel().addColumn("isOutlierFinal", String.class);
            for( int j = 0; j < n; j++ )
            {
                Object[] row = new Object[]{response[j], predictions[j], new String(new char[]{isOutlierInitial[j]}), new String(new char[]{isOutlierFinal[j]})};
                TableDataCollectionUtils.addRow(table, objectNames[j], row, true);
            }
            table.finalizeAddition();
            CollectionFactoryUtils.save(table);
        }
    }

    public class MessageBundle extends ConstantResourceBundle
    {
        public static final String PN_OPTION = "Option";
        public static final String PD_OPTION = "Select option (the concrete session of given analysis).";
        
        public static final String PN_PATH_TO_DATA_MATRIX = "Path to data matrix";
        public static final String PD_PATH_TO_DATA_MATRIX = "Path to table or file with data matrix";
        
        public static final String PN_VARIABLE_NAMES = "Variable names";
        public static final String PD_VARIABLE_NAMES = "Select variable names";
        
        public static final String PN_RESPONSE_NAME = "Response name";
        public static final String PD_RESPONSE_NAME = "Select response name";

        public static final String PN_MAX_NUMBER_OF_VARIABLES = "Maximal number of variables";
        public static final String PD_MAX_NUMBER_OF_VARIABLES = "Maximal number of variables";

        public static final String PN_PATH_TO_OUTPUT_FOLDER = "Path to output folder";
        public static final String PD_PATH_TO_OUTPUT_FOLDER = "Path to output folder";
    }

    public static class AllParameters extends AbstractAnalysisParameters
    {
        private String option = OPTION_01;
        private DataElementPath pathToDataMatrix;
        private String[] variableNames;
        private String responseName;
        private int maxNumberOfVariables = 2; 
        private DataElementPath pathToOutputFolder;
        
        @PropertyName(MessageBundle.PN_OPTION)
        @PropertyDescription(MessageBundle.PD_OPTION)
        public String getOption()
        {
            return option;
        }
        public void setOption(String option)
        {
            Object oldValue = this.option;
            this.option = option;
            firePropertyChange("*", oldValue, option);
        }
        
        @PropertyName(MessageBundle.PN_PATH_TO_DATA_MATRIX)
        @PropertyDescription(MessageBundle.PD_PATH_TO_DATA_MATRIX)
        public DataElementPath getPathToDataMatrix()
        {
            return pathToDataMatrix;
        }
        public void setPathToDataMatrix(DataElementPath pathToDataMatrix)
        {
            Object oldValue = this.pathToDataMatrix;
            this.pathToDataMatrix = pathToDataMatrix;
            firePropertyChange("pathToDataMatrix", oldValue, pathToDataMatrix);
        }
        
        @PropertyName(MessageBundle.PN_VARIABLE_NAMES)
        @PropertyDescription(MessageBundle.PD_VARIABLE_NAMES)
        public String[] getVariableNames()
        {
            return variableNames;
        }
        public void setVariableNames(String[] variableNames)
        {
            Object oldValue = this.variableNames;
            this.variableNames = variableNames;
            firePropertyChange("variableNames", oldValue, variableNames);
        }
        
        @PropertyName(MessageBundle.PN_RESPONSE_NAME)
        @PropertyDescription(MessageBundle.PD_RESPONSE_NAME)
        public String getResponseName()
        {
            return responseName;
        }
        public void setResponseName(String responseName)
        {
            Object oldValue = this.responseName;
            this.responseName = responseName;
            firePropertyChange("responseName", oldValue, responseName);
        }
        
        @PropertyName(MessageBundle.PN_MAX_NUMBER_OF_VARIABLES)
        @PropertyDescription(MessageBundle.PD_MAX_NUMBER_OF_VARIABLES)
        public int getMaxNumberOfVariables()
        {
            return maxNumberOfVariables;
        }
        public void setMaxNumberOfVariables(int maxNumberOfVariables)
        {
            Object oldValue = this.maxNumberOfVariables;
            this.maxNumberOfVariables = maxNumberOfVariables;
            firePropertyChange("maxNumberOfVariables", oldValue, maxNumberOfVariables);
        }
        
        @PropertyName(MessageBundle.PN_PATH_TO_OUTPUT_FOLDER)
        @PropertyDescription(MessageBundle.PD_PATH_TO_OUTPUT_FOLDER)
        public DataElementPath getPathToOutputFolder()
        {
            return pathToOutputFolder;
        }
        public void setPathToOutputFolder(DataElementPath pathToOutputFolder)
        {
            Object oldValue = this.pathToOutputFolder;
            this.pathToOutputFolder = pathToOutputFolder;
            firePropertyChange("pathToOutputFolder", oldValue, pathToOutputFolder);
        }
        
        public boolean isMaxNumberOfVariablesHidden()
        {
            return(! getOption().equals(OPTION_04));
        }
    }
    
    public static class ColumnNamesSelector extends GenericMultiSelectEditor
    {
        @Override
        protected String[] getAvailableValues()
        {
            try
            {
                DataElementPath pathToDataMatrix = ((LSregressionAnalysisParameters)getBean()).getPathToDataMatrix();
                // return DataMatrix.getColumnNames(pathToDataMatrix);
                String[] columnNames = DataMatrix.getColumnNames(pathToDataMatrix);
                Arrays.sort(columnNames, String.CASE_INSENSITIVE_ORDER);
                return columnNames;
            }
            catch( RepositoryException e )
            {
                return new String[]{"(please select table (or file) with data)"};
            }
            catch( Exception e )
            {
                return new String[]{"(table (or file) doesn't contain the columns)"};
            }
        }
    }
    
    public static class ColumnNameSelector extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            try
            {
                DataElementPath pathToDataMatrix = ((LSregressionAnalysisParameters)getBean()).getPathToDataMatrix();
                return DataMatrix.getColumnNames(pathToDataMatrix);
            }
            catch( RepositoryException e )
            {
                return new String[]{"(please select table (or file) with data)"};
            }
            catch( Exception e )
            {
                return new String[]{"(table (or file) doesn't contain the columns)"};
            }
        }
    }
    
    public static class OptionSelector extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            return getAvailableOptions();
        }
    }
    
    public static String[] getAvailableOptions()
    {
        return new String[]{OPTION_01, OPTION_04, OPTION_02, OPTION_03, OPTION_05};
    }
    
    // public static class LSregressionAnalysisParameters extends AbstractStatisticalAnalysisParameters
    public static class LSregressionAnalysisParameters extends AllParameters
    {}
    
    public static class LSregressionAnalysisParametersBeanInfo extends BeanInfoEx2<LSregressionAnalysisParameters>
    {
        public LSregressionAnalysisParametersBeanInfo()
        {
            super(LSregressionAnalysisParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            add(new PropertyDescriptorEx("option", beanClass), OptionSelector.class);
            add(DataElementPathEditor.registerInput("pathToDataMatrix", beanClass, DataElement.class, false));
            // Example from BioUML: ColumnNamesSelector.registerNumericSelector()
            // add(ColumnNamesSelector.registerNumericSelector("variableNames", beanClass, "pathToTableWithDataMatrix"));
            add("variableNames", ColumnNamesSelector.class);
            // Example from BioUML: ColumnNameSelector.registerNumericSelector()
            // add(ColumnNameSelector.registerNumericSelector("responseName", beanClass, "pathToTableWithDataMatrix", false));
            add(new PropertyDescriptorEx("responseName", beanClass), ColumnNameSelector.class);
            addHidden("maxNumberOfVariables", "isMaxNumberOfVariablesHidden");
            add(DataElementPathEditor.registerOutput("pathToOutputFolder", beanClass, FolderCollection.class, true));

//            add("dataMatrixExtensions", DataMatrixExtensionsSelector.class);
//            add("outputOptions", OutputOptionsSelector.class);
//            addHidden("pValue", "isPvalueHidden");
//            addHidden("maxIterations", "isMaxIterationsHidden");
//            add(DataElementPathEditor.registerOutput("outputPath", beanClass, FolderCollection.class, false));
        }
    }
    
    
    
}
