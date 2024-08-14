/* $Id$ */

package biouml.plugins.bindingregions.statistics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import biouml.plugins.bindingregions.utils.DataMatrix;
import biouml.plugins.bindingregions.utils.MatrixUtils;
import biouml.plugins.bindingregions.utils.TableUtils;
import biouml.plugins.bindingregions.utils.TableUtils.FileUtils;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import com.developmentontheedge.beans.editors.StringTagEditor;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysis.Stat;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.util.ConstantResourceBundle;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

/**
 * @author yura
 *
 */
public class DataMatrixOperations extends AnalysisMethodSupport<DataMatrixOperations.DataMatrixOperationsParameters>
{
    public static final String OPTION_01 = "Concatenation (column-wise) of 2 data submatrices.";
    public static final String OPTION_09 = "Merger (column-wise) of 2 data submatrices.";
    public static final String OPTION_05 = "Concatenation (column-wise) of double and String submatrices.";
    public static final String OPTION_02 = "Transformation of some columns : lg-transformation.";
    public static final String OPTION_03 = "Transformation of some columns : sqrt-transformation.";
    public static final String OPTION_06 = "Transformation of some columns : square-transformation.";
    public static final String OPTION_04 = "Calculation of mean and mse for columns.";
    public static final String OPTION_07 = "Calculate indicator matrix (do belong to [x1, x2]?).";
    public static final String OPTION_08 = "Split (row-wise) data matrix (indicators for splitting are given in fixed column).";
    public static final String OPTION_10 = "Calculation of  product of column pairs.";


    public DataMatrixOperations(DataCollection<?> origin, String name)
    {
        super(origin, name, new DataMatrixOperationsParameters());
    }
    
    @Override
    public DataCollection<?> justAnalyzeAndPut() throws Exception
    {
        log.info("Operations with data matrices.");
        String option = parameters.getOption();
        DataElementPath pathToDataMatrix = parameters.getPathToDataMatrix();
        String[] columnNames = parameters.getColumnNames();
        DataElementPath pathToSecondDataMatrix = parameters.getPathToSecondDataMatrix();
        String[] columnNamesInSecondMatrix = parameters.getColumnNamesInSecondMatrix();
        String resultedDataMatrixName = parameters.getResultedDataMatrixName();
        DataElementPath pathToOutputFolder = parameters.getPathToOutputFolder();
        boolean doWriteToFile = parameters.getDoWriteToFile();
        
        DataCollectionUtils.createFoldersForPath(pathToOutputFolder.getChildPath(""));
        DataMatrix dataMatrix = new DataMatrix(pathToDataMatrix, columnNames);
        
        /// tamp !!!
        ///getFNCMs(dataMatrix, pathToOutputFolder);
        /// temp !!!
        
        
        switch( option )
        {
            case OPTION_01 : DataMatrix dataMatrixSecond = new DataMatrix(pathToSecondDataMatrix, columnNamesInSecondMatrix);
                             if( dataMatrix.addAnotherDataMatrixColumnWise(dataMatrixSecond) )
                                 dataMatrix.writeDataMatrix(doWriteToFile, pathToOutputFolder, resultedDataMatrixName, log); break;
            case OPTION_09 : dataMatrixSecond = new DataMatrix(pathToSecondDataMatrix, columnNamesInSecondMatrix);
                             if( dataMatrix.mergeWithAnotherDataMatrixColumnWise(dataMatrixSecond) )
                                 dataMatrix.writeDataMatrix(doWriteToFile, pathToOutputFolder, resultedDataMatrixName, log); break;
            case OPTION_02 : dataMatrix.transformDataMatrix(DataMatrix.TRANSFORMATION_01);
                             dataMatrix.writeDataMatrix(doWriteToFile, pathToOutputFolder, resultedDataMatrixName, log); break;
            case OPTION_03 : dataMatrix.transformDataMatrix(DataMatrix.TRANSFORMATION_02);
                             dataMatrix.writeDataMatrix(doWriteToFile, pathToOutputFolder, resultedDataMatrixName, log); break;
            case OPTION_06 : dataMatrix.transformDataMatrix(DataMatrix.TRANSFORMATION_03);
                             dataMatrix.writeDataMatrix(doWriteToFile, pathToOutputFolder, resultedDataMatrixName, log); break;
            case OPTION_04 : String newColumnName = parameters.getNewColumnName();
                             dataMatrix = calculateMeanAndMseForColumns(dataMatrix, newColumnName);
                             dataMatrix.writeDataMatrix(doWriteToFile, pathToOutputFolder, resultedDataMatrixName, log); break;
            case OPTION_05 : Object[] objects = DataMatrix.readStringMatrixOrSubmatrix(pathToSecondDataMatrix, columnNamesInSecondMatrix);
//                             if( doWriteToFile )
//                                 FileUtils.writeDoubleAndStringMatricesToFile(dataMatrix.getRowNames(), dataMatrix.getColumnNames(), dataMatrix.getMatrix(), (String[])objects[0], (String[])objects[1], (String[][])objects[2], pathToOutputFolder.getChildPath(resultedDataMatrixName), log);
//                             else
//                                 TableUtils.writeDoubleAndString(dataMatrix.getMatrix(), (String[][])objects[2], dataMatrix.getRowNames(), dataMatrix.getColumnNames(), (String[])objects[1], pathToOutputFolder, resultedDataMatrixName);
                             writeDoubleAndStringMatrices(doWriteToFile, dataMatrix.getRowNames(), dataMatrix.getColumnNames(), dataMatrix.getMatrix(), (String[])objects[0], (String[])objects[1], (String[][])objects[2], pathToOutputFolder, resultedDataMatrixName, log);
                             break;
            case OPTION_07 : double leftBoundary = parameters.getLeftBoundary(), rightBoundary = parameters.getRightBoundary();
                             String[][] indicatorMatrix = calculateIndicatorMatrix(dataMatrix.getMatrix(), leftBoundary, rightBoundary);
                             columnNames = dataMatrix.getColumnNames();
                             String[] indicatorNames = new String[columnNames.length];
                             for( int i = 0; i < columnNames.length; i++ )
                                 indicatorNames[i] = columnNames[i] + "_do_belong_to_" + Double.toString(leftBoundary)  + "_" + Double.toString(rightBoundary);
//                             if( doWriteToFile )
//                                 FileUtils.writeDoubleAndStringMatricesToFile(dataMatrix.getRowNames(), columnNames, dataMatrix.getMatrix(), dataMatrix.getRowNames(), indicatorNames, indicatorMatrix, pathToOutputFolder.getChildPath(resultedDataMatrixName), log);
//                             else
//                                 TableUtils.writeDoubleAndString(dataMatrix.getMatrix(), indicatorMatrix, dataMatrix.getRowNames(), dataMatrix.getColumnNames(), indicatorNames, pathToOutputFolder, resultedDataMatrixName);
                             writeDoubleAndStringMatrices(doWriteToFile, dataMatrix.getRowNames(), columnNames, dataMatrix.getMatrix(), dataMatrix.getRowNames(), indicatorNames, indicatorMatrix, pathToOutputFolder, resultedDataMatrixName, log);
                             break;
            case OPTION_08 : String columnName = parameters.getColumnName();
                             objects = DataMatrix.readStringMatrixOrSubmatrix(pathToDataMatrix, new String[]{columnName});
                             String[] indicatorsForSplitting = MatrixUtils.getColumn((String[][])objects[2], 0);
                             splitMatrixRowWiseAndWrite(dataMatrix, indicatorsForSplitting, doWriteToFile, pathToOutputFolder, resultedDataMatrixName, log); break;
            case OPTION_10 : calculateProductsOfColumnPairs(dataMatrix, doWriteToFile, pathToOutputFolder, resultedDataMatrixName); break;
            default        : throw new Exception("This  option '" + option + "' is not supported in our analysis currently");
        }
        return pathToOutputFolder.getDataCollection();
    }
    
    private void calculateProductsOfColumnPairs(DataMatrix dataMatrix, boolean doWriteToFile, DataElementPath pathToOutputFolder, String resultedDataMatrixName) throws Exception
    {
        double[][] matrix = dataMatrix.getMatrix(), matrixNew = new double[matrix.length][matrix[0].length * (matrix[0].length - 1) / 2];
        String[] columnNames = dataMatrix.getColumnNames(), columnNamesNew = new String[matrix[0].length * (matrix[0].length - 1) / 2];
        int index = 0;
        for( int j = 0; j < matrix[0].length - 1; j++ )
            for( int jj = j + 1; jj < matrix[0].length; jj++ )
            {
                for( int i = 0; i < matrix.length; i++ )
                    matrixNew[i][index] = matrix[i][j] * matrix[i][jj];
                columnNamesNew[index++] = columnNames[j] + "x" + columnNames[jj];
            }
        DataMatrix dm = new DataMatrix(dataMatrix.getRowNames(), columnNamesNew, matrixNew);
        dm.writeDataMatrix(doWriteToFile, pathToOutputFolder, resultedDataMatrixName, log);
    }
    
    public static void splitMatrixRowWiseAndWrite(DataMatrix dataMatrix, String[] indicatorsForSplitting, boolean doWriteToFile, DataElementPath pathToOutputFolder, String resultedDataMatrixName, Logger log) throws Exception
    {
        // 1. Calculate submatrices.
        Map<String, List<double[]>> submatrices = new HashMap<>();
        Map<String, List<String>> rowNamesAll = new HashMap<>();
        String[] rowNames = dataMatrix.getRowNames(), columnNames = dataMatrix.getColumnNames();
        double[][] matrix = dataMatrix.getMatrix();
        for( int i = 0; i < matrix.length; i++ )
        {
            submatrices.computeIfAbsent(indicatorsForSplitting[i], key -> new ArrayList<>()).add(matrix[i]);
            rowNamesAll.computeIfAbsent(indicatorsForSplitting[i], key -> new ArrayList<>()).add(rowNames[i]);
        }
        
        // 2. Write submatrices.
        for( Entry<String, List<double[]>> entry : submatrices.entrySet() )
        {
            String indicator = entry.getKey();
            List<double[]> list = entry.getValue();
            DataMatrix dm = new DataMatrix(rowNamesAll.get(indicator).toArray(new String[0]), columnNames, list.toArray(new double[list.size()][]));
            dm.writeDataMatrix(doWriteToFile, pathToOutputFolder, resultedDataMatrixName + "_" + indicator, log);
        }
    }
    
    public static void writeDoubleAndStringMatrices(boolean doWriteToFile, String[] rowNames, String[] columnNames, double[][] dataMatrix, String[] rowNamesString, String[] columnNamesString, String[][] dataMatrixString, DataElementPath pathToOutputFolder, String nameOfFileOrTable, Logger log) throws Exception
    {
        if( doWriteToFile )
            FileUtils.writeDoubleAndStringMatricesToFile(rowNames, columnNames, dataMatrix, rowNamesString, columnNamesString, dataMatrixString, pathToOutputFolder.getChildPath(nameOfFileOrTable), log);
        else
            TableUtils.writeDoubleAndString(dataMatrix, dataMatrixString, rowNames, columnNames, columnNamesString, pathToOutputFolder, nameOfFileOrTable);
    }

    private String[][] calculateIndicatorMatrix(double[][] dataMatrix, double leftBoundary, double rightBoundary)
    {
        String[][] indicatorMatrix = new String[dataMatrix.length][dataMatrix[0].length];
        for( int i = 0; i < dataMatrix.length; i++ )
            for( int j = 0; j < dataMatrix[0].length; j++ )
                indicatorMatrix[i][j] = leftBoundary <= dataMatrix[i][j] && dataMatrix[i][j] <= rightBoundary ? "+" : "-";  
        return indicatorMatrix;
    }

    private DataMatrix calculateMeanAndMseForColumns(DataMatrix dataMatrix, String newColumnName)
    {
        double[][] matrix = dataMatrix.getMatrix(), result = new double[matrix.length][];
        for( int i = 0; i < matrix.length; i++ )
            result[i] = Stat.getMeanAndSigma(matrix[i]);
        return new DataMatrix(dataMatrix.getRowNames(), new String[]{newColumnName + "_mean", newColumnName + "_mse"}, result);
    }
    
    public class MessageBundle extends ConstantResourceBundle
    {
        public static final String PN_OPTION = "Option";
        public static final String PD_OPTION = "Select option (the concrete session of given analysis).";
        
        public static final String PN_PATH_TO_DATA_MATRIX = "Path to data matrix";
        public static final String PD_PATH_TO_DATA_MATRIX = "Path to table or file with data matrix";
        
        public static final String PN_PATH_TO_SECOND_DATA_MATRIX = "Path to 2-nd data matrix";
        public static final String PD_PATH_TO_SECOND_DATA_MATRIX = "Path to table or file with 2-nd data matrix";
        
        public static final String PN_COLUMN_NAMES = "Column names";
        public static final String PD_COLUMN_NAMES = "Select column names";

        public static final String PN_COLUMN_NAMES_IN_SECOND_MATRIX = "Column names in 2-nd matrix";
        public static final String PD_COLUMN_NAMES_IN_SECOND_MATRIX = "Select column names in 2-nd matrix";

        public static final String PN_RESULTED_DATA_MATRIX_NAME = "Resulted data matrix name";
        public static final String PD_RESULTED_DATA_MATRIX_NAME = "Name of resulted data matrix";
        
        public static final String PN_DO_WRITE_TO_FILE = "Do write to file";
        public static final String PD_DO_WRITE_TO_FILE = "Do write result to file (otherwise - to table)?";
        
        public static final String PN_COLUMN_NAME = "Column name";
        public static final String PD_COLUMN_NAME = "Select column name ";
        
        public static final String PN_NEW_COLUMN_NAME = "New column name";
        public static final String PD_NEW_COLUMN_NAME = "Define name of new column";
        
        public static final String PN_LEFT_BOUNDARY = "Left boundary";
        public static final String PD_LEFT_BOUNDARY = "Define left boundary";
        
        public static final String PN_RIGHT_BOUNDARY = "Right boundary";
        public static final String PD_RIGHT_BOUNDARY = "Define right boundary";
        
        public static final String PN_PATH_TO_OUTPUT_FOLDER = "Path to output folder";
        public static final String PD_PATH_TO_OUTPUT_FOLDER = "Path to output folder";
    }

    public static class AllParameters extends AbstractAnalysisParameters
    {
        private String option = OPTION_01;
        private DataElementPath pathToDataMatrix;
        private DataElementPath pathToSecondDataMatrix;
        private String[] columnNames;
        private String[] columnNamesInSecondMatrix;
        private String resultedDataMatrixName;
        private boolean doWriteToFile = true;
        private String columnName;
        private String newColumnName;
        private double leftBoundary = -0.1;
        private double rightBoundary = 3.0;
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
        
        @PropertyName(MessageBundle.PN_PATH_TO_SECOND_DATA_MATRIX)
        @PropertyDescription(MessageBundle.PD_PATH_TO_SECOND_DATA_MATRIX)
        public DataElementPath getPathToSecondDataMatrix()
        {
            return pathToSecondDataMatrix;
        }
        public void setPathToSecondDataMatrix(DataElementPath pathToSecondDataMatrix)
        {
            Object oldValue = this.pathToSecondDataMatrix;
            this.pathToSecondDataMatrix = pathToSecondDataMatrix;
            firePropertyChange("pathToSecondDataMatrix", oldValue, pathToSecondDataMatrix);
        }
        
        @PropertyName(MessageBundle.PN_COLUMN_NAMES)
        @PropertyDescription(MessageBundle.PD_COLUMN_NAMES)
        public String[] getColumnNames()
        {
            return columnNames;
        }
        public void setColumnNames(String[] columnNames)
        {
            Object oldValue = this.columnNames;
            this.columnNames = columnNames;
            firePropertyChange("columnNames", oldValue, columnNames);
        }

        @PropertyName(MessageBundle.PN_COLUMN_NAMES_IN_SECOND_MATRIX)
        @PropertyDescription(MessageBundle.PD_COLUMN_NAMES_IN_SECOND_MATRIX)
        public String[] getColumnNamesInSecondMatrix()
        {
            return columnNamesInSecondMatrix;
        }
        public void setColumnNamesInSecondMatrix(String[] columnNamesInSecondMatrix)
        {
            Object oldValue = this.columnNamesInSecondMatrix;
            this.columnNamesInSecondMatrix = columnNamesInSecondMatrix;
            firePropertyChange("columnNamesInSecondMatrix", oldValue, columnNamesInSecondMatrix);
        }

        @PropertyName(MessageBundle.PN_RESULTED_DATA_MATRIX_NAME)
        @PropertyDescription(MessageBundle.PD_RESULTED_DATA_MATRIX_NAME)
        public String getResultedDataMatrixName()
        {
            return resultedDataMatrixName;
        }
        public void setResultedDataMatrixName(String resultedDataMatrixName)
        {
            Object oldValue = this.resultedDataMatrixName;
            this.resultedDataMatrixName = resultedDataMatrixName;
            firePropertyChange("resultedDataMatrixName", oldValue, resultedDataMatrixName);
        }
        
        @PropertyName(MessageBundle.PN_DO_WRITE_TO_FILE)
        @PropertyDescription(MessageBundle.PD_DO_WRITE_TO_FILE)
        public boolean getDoWriteToFile()
        {
            return doWriteToFile;
        }
        public void setDoWriteToFile(boolean doWriteToFile)
        {
            Object oldValue = this.doWriteToFile;
            this.doWriteToFile = doWriteToFile;
            firePropertyChange("doWriteToFile", oldValue, doWriteToFile);
        }
        
        @PropertyName(MessageBundle.PN_COLUMN_NAME)
        @PropertyDescription(MessageBundle.PD_COLUMN_NAME)
        public String getColumnName()
        {
            return columnName;
        }
        public void setColumnName(String columnName)
        {
            Object oldValue = this.columnName;
            this.columnName = columnName;
            firePropertyChange("columnName", oldValue, columnName);
        }
        
        @PropertyName(MessageBundle.PN_NEW_COLUMN_NAME)
        @PropertyDescription(MessageBundle.PD_NEW_COLUMN_NAME)
        public String getNewColumnName()
        {
            return newColumnName;
        }
        public void setNewColumnName(String newColumnName)
        {
            Object oldValue = this.newColumnName;
            this.newColumnName = newColumnName;
            firePropertyChange("newColumnName", oldValue, newColumnName);
        }
        
        @PropertyName(MessageBundle.PN_LEFT_BOUNDARY)
        @PropertyDescription(MessageBundle.PD_LEFT_BOUNDARY)
        public double getLeftBoundary()
        {
            return leftBoundary;
        }
        public void setLeftBoundary(double leftBoundary)
        {
            Object oldValue = this.leftBoundary;
            this.leftBoundary = leftBoundary;
            firePropertyChange("leftBoundary", oldValue, leftBoundary);
        }
        
        @PropertyName(MessageBundle.PN_RIGHT_BOUNDARY)
        @PropertyDescription(MessageBundle.PD_RIGHT_BOUNDARY)
        public double getRightBoundary()
        {
            return rightBoundary;
        }
        public void setRightBoundary(double rightBoundary)
        {
            Object oldValue = this.rightBoundary;
            this.rightBoundary = rightBoundary;
            firePropertyChange("rightBoundary", oldValue, rightBoundary);
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
        
        public boolean isColumnNameHidden()
        {
            return(! getOption().equals(OPTION_08));
        }
        
        public boolean isNewColumnNameHidden()
        {
            return(! getOption().equals(OPTION_04));
        }
        
        public boolean areBoundariesHidden()
        {
            return(! getOption().equals(OPTION_07));
        }
        
        public boolean isSecondDataMatrixHidden()
        {
            String option = getOption();
            return(! option.equals(OPTION_01) && ! option.equals(OPTION_05) && ! option.equals(OPTION_09));
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
    
    public static class ColumnNamesSelector extends GenericMultiSelectEditor
    {
        @Override
        protected String[] getAvailableValues()
        {
            try
            {
                DataElementPath pathToDataMatrix = ((DataMatrixOperationsParameters)getBean()).getPathToDataMatrix();
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
    
    public static class ColumnNamesSelectorSecond extends GenericMultiSelectEditor
    {
        @Override
        protected String[] getAvailableValues()
        {
            try
            {
                DataElementPath pathToSecondDataMatrix = ((DataMatrixOperationsParameters)getBean()).getPathToSecondDataMatrix();
                String[] columnNames = DataMatrix.getColumnNames(pathToSecondDataMatrix);
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
    
    public static String[] getAvailableOptions()
    {
        return new String[]{OPTION_01, OPTION_09, OPTION_05, OPTION_02, OPTION_03, OPTION_06, OPTION_04, OPTION_07, OPTION_08, OPTION_10};
    }
    
    public static class ColumnNameSelector extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
            try
            {
                DataElementPath pathToDataMatrix = ((DataMatrixOperationsParameters)getBean()).getPathToDataMatrix();
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
    
    public static class DataMatrixOperationsParameters extends AllParameters
    {}
    
    public static class DataMatrixOperationsParametersBeanInfo extends BeanInfoEx2<DataMatrixOperationsParameters>
    {
        public DataMatrixOperationsParametersBeanInfo()
        {
            super(DataMatrixOperationsParameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            add(new PropertyDescriptorEx("option", beanClass), OptionSelector.class);
            add(DataElementPathEditor.registerInput("pathToDataMatrix", beanClass, DataElement.class, false));
            add("columnNames", ColumnNamesSelector.class);
            addHidden(DataElementPathEditor.registerInput("pathToSecondDataMatrix", beanClass, DataElement.class, false), "isSecondDataMatrixHidden");
            addHidden("columnNamesInSecondMatrix", ColumnNamesSelectorSecond.class, "isSecondDataMatrixHidden");
            addHidden(new PropertyDescriptorEx("columnName", beanClass), ColumnNameSelector.class, "isColumnNameHidden");
            add("resultedDataMatrixName");
            add("doWriteToFile");
            addHidden("newColumnName", "isNewColumnNameHidden");
            addHidden("leftBoundary", "areBoundariesHidden");
            addHidden("rightBoundary", "areBoundariesHidden");
            add(DataElementPathEditor.registerOutput("pathToOutputFolder", beanClass, FolderCollection.class, true));
        }
    }
}
