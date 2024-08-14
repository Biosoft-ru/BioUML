/* $Id$ */

package biouml.plugins.bindingregions.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.Stat;
import ru.biosoft.table.TableDataCollection;
import biouml.plugins.bindingregions.utils.TableUtils.FileUtils;

/**
 * @author yura
 *
 */
public class DataMatrix
{
    
    public static final String TRANSFORMATION_01 = "lg-transformation.";
    public static final String TRANSFORMATION_02 = "sqrt-transformation.";
    public static final String TRANSFORMATION_03 = "square-transformation.";
    
    ///////////// it is copied
    private String[] rowNames;
    private String[] columnNames;
    private double[][] matrix;

    ///////////// it is copied
    public DataMatrix(String[] rowNames, String[] columnNames, double[][] matrix)
    {
        this.rowNames = rowNames;
        this.columnNames = columnNames;
        this.matrix = matrix;
    }
    
    ///////////// it is copied
    public DataMatrix(String[] rowNames, String columnName, double[] column)
    {
        this.rowNames = rowNames;
        this.columnNames = new String[]{columnName};
        this.matrix = MatrixUtils.transformVectorToMatrixWithSingleColumn(column);
    }
    
    ////// it is copied
    public DataMatrix(DataElementPath pathToMatrix, String[] columnNames) throws IOException
    {
        Object[] objects = readDoubleMatrixOrSubmatrix(pathToMatrix, columnNames);
        this.rowNames = (String[])objects[0];
        this.columnNames = (String[])objects[1];
        this.matrix = (double[][])objects[2];
    }

    public DataMatrix(DataElementPath pathToMatrix) throws IOException
    {
        this(pathToMatrix, getColumnNames(pathToMatrix));
    }

    ///////////// it is copied
    public String[] getRowNames()
    {
        return rowNames;
    }
    
    ///////////// it is copied
    public String[] getColumnNames()
    {
        return columnNames;
    }
    
    ///////////// it is copied
    public double[][] getMatrix()
    {
        return matrix;
    }
    
    public double[] getRow(String rowName)
    {
        int index = ArrayUtils.indexOf(rowNames, rowName);
        return index < 0 ? null : matrix[index];
    }
    
    //// it is copied
    public double[] getColumn(String columnName)
    {
        int index = ArrayUtils.indexOf(columnNames, columnName);
        return index < 0 ? null : MatrixUtils.getColumn(matrix, index);
    }
    
    public boolean mergeWithAnotherDataMatrixColumnWise(DataMatrix dataMatrix)
    {
        List<String> rowNamesNew = new ArrayList<>();
        List<double[]> list = new ArrayList<>();
        String[] rowNames = dataMatrix.getRowNames();
        double[][] matrix = dataMatrix.getMatrix();
        for( int i = 0; i < this.rowNames.length; i++ )
        {
            int index = ArrayUtils.indexOf(rowNames, this.rowNames[i]);
            if( index < 0 ) continue;
            rowNamesNew.add(this.rowNames[i]);
            list.add(ArrayUtils.addAll(this.matrix[i], matrix[index]));
        }
        if( rowNamesNew.isEmpty() ) return false;
        this.rowNames = rowNamesNew.toArray(new String[0]);
        this.columnNames = (String[])ArrayUtils.addAll(this.columnNames, dataMatrix.getColumnNames());
        this.matrix = list.toArray(new double[list.size()][]);
        return true;
    }
    
    public boolean addAnotherDataMatrixColumnWise(DataMatrix dataMatrix)
    {
        if( ! MatrixUtils.equals(dataMatrix.getRowNames(), rowNames) ) return false;
        columnNames = (String[])ArrayUtils.addAll(columnNames, dataMatrix.getColumnNames());
        double[][] additionalMatrix = dataMatrix.getMatrix();
        for( int i = 0; i < matrix.length; i++ )
            matrix[i] = ArrayUtils.addAll(matrix[i], additionalMatrix[i]);
        return true;
    }
    
    // it is copied
    public boolean addColumn(String columnName, double[] column)
    {
        return addAnotherDataMatrixColumnWise(new DataMatrix(rowNames, columnName, column));
    }
    
    // it is copied
    public boolean addAnotherDataMatrixRowWise(DataMatrix dataMatrix)
    {
        if( ! MatrixUtils.equals(dataMatrix.getColumnNames(), columnNames) ) return false;
        int n1 = rowNames.length, n2 = dataMatrix.getRowNames().length;
        double[][] matrix = dataMatrix.getMatrix(), newMatrix = new double[n1 + n2][];
        for( int i = 0; i < n1; i++ )
            newMatrix[i] = this.matrix[i];
        for( int i = 0; i < n2; i++ )
            newMatrix[i + n1] = matrix[i];
        this.matrix =  newMatrix;
        rowNames = (String[])ArrayUtils.addAll(rowNames, dataMatrix.getRowNames());
        return true;
    }
    
    public void transformDataMatrix(String transformationType)
    {
        switch( transformationType )
        {
            case TRANSFORMATION_01 : for( int j = 0; j < columnNames.length; j++ )
                                     {
                                         columnNames[j] += "_lg";
                                         for( int i = 0; i < rowNames.length; i++ )
                                             matrix[i][j] = matrix[i][j] >= 1.0 ? Math.log10(matrix[i][j]) : 0.0;                                              
                                     } break;
            case TRANSFORMATION_02 : for( int j = 0; j < columnNames.length; j++ )
                                     {
                                         columnNames[j] += "_sqrt";
                                         for( int i = 0; i < rowNames.length; i++ )
                                             matrix[i][j] = Math.sqrt(matrix[i][j]);
                                     } break;
            case TRANSFORMATION_03 : for( int j = 0; j < columnNames.length; j++ )
                                    {
                                         columnNames[j] += "_square";
                                         for( int i = 0; i < rowNames.length; i++ )
                                             matrix[i][j] = matrix[i][j] * matrix[i][j];
                                    } break;
        }
        
    }

    //// It is copied
    public void writeDataMatrix(boolean doWriteToFile, DataElementPath pathToOutputs, String fileOrTableName, Logger log) throws Exception
    {
        if( doWriteToFile )
            FileUtils.writeDoubleMatrixToFile(rowNames, columnNames, matrix, pathToOutputs.getChildPath(fileOrTableName), log);
        else
            TableUtils.writeDoubleTable(matrix, rowNames, columnNames, pathToOutputs, fileOrTableName);
    }
    
    public DataMatrix getMeansAndSigmas()
    {
        double[][] matrixNew = new double[2][matrix[0].length];
        for( int i = 0; i < matrix[0].length; i++ )
        {
            double[] meanAndSigma = Stat.getMeanAndSigma(MatrixUtils.getColumn(matrix, i));
            matrixNew[0][i] = meanAndSigma[0];
            matrixNew[1][i] = meanAndSigma[1];
        }
        return new DataMatrix(new String[]{"mean", "sigma"}, columnNames,  matrixNew);
    }

    
    /********************************************************************************************/
    
    // it is copied
    // TODO: Some initial matrices are changed. It is not good!  
    public static DataMatrix[] calculateAveragesOfMatricesThatHaveSameRowAndColumnNames(DataMatrix[] dataMatrices)
    {
        if( dataMatrices == null || dataMatrices.length <= 1) return dataMatrices;
        List<DataMatrix> result = new ArrayList<>();
        int[] indicators = new int[dataMatrices.length];
        for( int i = 0; i < dataMatrices.length; i++ )
        {
            // 1. Identify the list of appropriate dataMatrices.
            if( indicators[i] == 1 ) continue;
            List<DataMatrix> list = new ArrayList<>();
            list.add(dataMatrices[i]);
            for( int ii = i + 1; ii < dataMatrices.length; ii++ )
                if( indicators[ii] == 0 && doDataMatricesHaveSameRowAndColumnNames(dataMatrices[i], dataMatrices[ii]) )
                {
                    indicators[ii] = 1;
                    list.add(dataMatrices[ii]);
                }
            
            // 2. Calculate the average of dataMatrices from list.
            if( list.size() == 1 )
                result.addAll(list);
            else
            {
                double[][] matrix = list.get(0).getMatrix();
                for( int j = 1; j < list.size(); j++ )
                    matrix = MatrixUtils.getSumOfMatrices(matrix, list.get(j).getMatrix());
                matrix = MatrixUtils.getProductOfMatrixAndScalar(matrix, 1.0 / (double)list.size());
                DataMatrix dm = list.get(0);
                result.add(new DataMatrix(dm.getRowNames(), dm.getColumnNames(), matrix));
            }
        }
        return result.toArray(new DataMatrix[0]);
    }
    
    // it is copied
    public static boolean doDataMatricesHaveSameRowAndColumnNames(DataMatrix dataMatrix1, DataMatrix dataMatrix2)
    {
        if( dataMatrix1 == null || dataMatrix2 == null ) return false;
        String[] rowNames1 = dataMatrix1.getRowNames(), rowNames2 = dataMatrix2.getRowNames(), columnNames1 = dataMatrix1.getColumnNames(), columnNames2 = dataMatrix2.getColumnNames();
        if( rowNames1 == null || rowNames2 == null || columnNames1 == null || columnNames2 == null) return false;
        if( ! MatrixUtils.equals(rowNames1, rowNames2) ) return false;
        if( ! MatrixUtils.equals(columnNames1, columnNames2) ) return false;
        return true;
    }
    
    /***
     * If columnNames != null then read whole matrix else read corresponding submatrix.
     * @param pathToMatrix
     * @param columnNames
     * @return Object[] array : array[0] = String[] rowNames; array[1] = String[] columnNames; array[2] = double[][] matrix;
     * @throws IOException
     ***/
    public static Object[] readDoubleMatrixOrSubmatrix(DataElementPath pathToMatrix, String[] columnNames) throws IOException
    {
        if( ! (pathToMatrix.getDataElement() instanceof TableDataCollection) )
        {
            int index = columnNames == null ? -1 : ArrayUtils.indexOf(columnNames, LinearRegression.INTERCEPT);
            if( index < 0 )
                return FileUtils.readMatrixOrSubmatix(pathToMatrix, columnNames, FileUtils.DOUBLE_TYPE);
            String[] columnNamesNew = (String[])ArrayUtils.remove(columnNames, index);
            Object[] objects = FileUtils.readMatrixOrSubmatix(pathToMatrix, columnNamesNew, FileUtils.DOUBLE_TYPE);
            MatrixUtils.addColumnToMatrix((double[][])objects[2], MatrixUtils.getConstantVector(((double[][])objects[2]).length, 1.0), index);
            return new Object[]{(String[])objects[0], columnNames, (double[][])objects[2]};
        }
        return TableUtils.readDoubleMatrixOrSubmatrix(pathToMatrix, columnNames);
    }
    
    /***
     * If columnNames != null then read whole matrix else read corresponding submatrix.
     * @param pathToMatrix
     * @param columnNames
     * @return Object[] array : array[0] = String[] rowNames; array[1] = String[] columnNames; array[2] = String[][] matrix;
     * @throws IOException
     ***/
    public static Object[] readStringMatrixOrSubmatrix(DataElementPath pathToMatrix, String[] columnNames) throws IOException
    {
        if( ! (pathToMatrix.getDataElement() instanceof TableDataCollection) )
            return FileUtils.readMatrixOrSubmatix(pathToMatrix, columnNames, FileUtils.STRING_TYPE);
        return TableUtils.readStringMatrixOrSubmatrix(pathToMatrix, columnNames);
    }
    
    // it is copied
    public static String[] getColumnNames(DataElementPath pathToMatrix) throws IOException
    {
        if( ! (pathToMatrix.getDataElement() instanceof TableDataCollection) )
            return FileUtils.getColumnNames(pathToMatrix);
        return TableUtils.getColumnNamesInTable(pathToMatrix.getDataElement(TableDataCollection.class));
    }
    
    // TODO: To move this method to appropriate Class
    public static double[] readDoubleColumn(DataElementPath pathToMatrix, String columnName) throws IOException
    {
        Object[] objects = readDoubleMatrixOrSubmatrix(pathToMatrix, new String[]{columnName});
        double[][] matrix = (double[][])objects[2];
        double[] result = new double[matrix.length];
        for( int i = 0; i < matrix.length; i++ )
            result[i] = matrix[i][0];
        return result;
    }
}
