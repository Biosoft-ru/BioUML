/* $Id$ */

package biouml.plugins.machinelearning.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang.ArrayUtils;

import biouml.plugins.machinelearning.utils.UtilsGeneral.UtilsForArray;
import ru.biosoft.access.core.DataElementPath;

/**
 * @author yura
 *
 */
public class DataMatrixString
{
    private String[] rowNames, columnNames;
    private String[][] matrix;
    
    public DataMatrixString(String[] rowNames, String[] columnNames, String[][] matrix)
    {
        this.rowNames = rowNames;
        this.columnNames = columnNames;
        this.matrix = matrix;
    }
    
    public DataMatrixString(String[] rowNames, String columnName, String[] column)
    {
        this(rowNames, new String[]{columnName}, MatrixUtils.transformVectorToMatrixWithSingleColumn(column));
    }
    
    public DataMatrixString(String rowName, String[] columnNames, String[] row)
    {
    	this( new String[]{rowName}, columnNames, MatrixUtils.transformVectorToMatrixWithSingleRow(row));
    }
    
    public DataMatrixString(DataElementPath pathToMatrix, String[] columnNames)
    {
        Object[] objects = TableAndFileUtils.readMatrixOrSubmatix(pathToMatrix, columnNames, TableAndFileUtils.STRING_TYPE);
        this.rowNames = (String[])objects[0];
        this.columnNames = (String[])objects[1];
        this.matrix = (String[][])objects[2];
    }
    
    public String[] getRowNames()
    {
        return rowNames;
    }
    
    public String[] getColumnNames()
    {
        return columnNames;
    }
    
    public String[][] getMatrix()
    {
        return matrix;
    }
    
    public int getSize()
    {
        return matrix.length;
    }
    
    public String[] getColumn(int index)
    {
        return MatrixUtils.getColumn(matrix, index);
    }
    
    public String[] getColumn(String columnName)
    {
        int index = ArrayUtils.indexOf(columnNames, columnName);
        return index < 0 ? null : getColumn(index);
    }
    
    public String[] getRow(String rowName)
    {
        int index = ArrayUtils.indexOf(rowNames, rowName);
        return index < 0 ? null : matrix[index];
    }
    
    public DataMatrixString getRow(int index)
    {
        return new DataMatrixString(rowNames[index], columnNames, matrix[index]);
    }
    
    public void addColumn(String columnName, String[] column, int columnIndex)
    {
        columnNames = (String[])ArrayUtils.add(columnNames, columnIndex, columnName);
        for( int i = 0; i < matrix.length; i++ )
            matrix[i] = (String[])ArrayUtils.add(matrix[i], columnIndex, column[i]);
    }
    
    public void removeRowsWithMissingData()
    {
        List<String> rowNamesNew = new ArrayList<>();
        List<String[]> matrixNew = new ArrayList<>();
        for( int i = 0; i < rowNames.length; i++ )
            if( ! UtilsForArray.doContainNull(matrix[i]) )
            {
                rowNamesNew.add(rowNames[i]);
                matrixNew.add(matrix[i]);
            }
        rowNames = rowNamesNew.toArray(new String[0]);
        matrix = matrixNew.toArray(new String[matrixNew.size()][]);
    }
    
    public void writeDataMatrixString(boolean doWriteToFile, DataElementPath pathToOutputFolder, String fileOrTableName, Logger log)
    {
        TableAndFileUtils.writeDoubleAndStringMatrices(doWriteToFile, rowNames, null, null, columnNames, matrix, pathToOutputFolder, fileOrTableName, log);
    }
    
    /************ DataMatrixStringConstructor : start ***************/
    public static class DataMatrixStringConstructor
    {
        String[] columnNames;
        List<String> rowNames;
        List<String[]> rows;
        
        public DataMatrixStringConstructor(String[] columnNames)
        {
            this.columnNames = columnNames;
            rowNames = new ArrayList<>();
            rows = new ArrayList<>();
        }
        
        public void addRow(String rowName, String[] rowValues)
        {
            rowNames.add(rowName);
            rows.add(rowValues);
        }
        
        public DataMatrixString getDataMatrixString()
        {
            return new DataMatrixString(rowNames.toArray(new String[0]), columnNames, rows.toArray(new String[rows.size()][]));
        }
    }
    /************ DataMatrixStringConstructor : end ***************/
    
    /************ DataMatrixChar : start **************************/
    public static class DataMatrixChar
    {
        private String[] rowNames, columnNames;
        private char[][] matrix;
        
        public DataMatrixChar(String[] rowNames, String[] columnNames, char[][] matrix)
        {
            this.rowNames = rowNames;
            this.columnNames = columnNames;
            this.matrix = matrix;
        }
        
        public DataMatrixChar(DataElementPath pathToMatrix, String[] columnNames)
        {
            Object[] objects = TableAndFileUtils.readMatrixOrSubmatix(pathToMatrix, columnNames, TableAndFileUtils.CHAR_TYPE);
            this.rowNames = (String[])objects[0];
            this.columnNames = (String[])objects[1];
            this.matrix = (char[][])objects[2];
        }
        
        public String[] getRowNames()
        {
            return rowNames;
        }
        
        public String[] getColumnNames()
        {
            return columnNames;
        }
        
        public char[][] getMatrix()
        {
            return matrix;
        }
        
        public int getSize()
        {
            return matrix.length;
        }
        
        public void writeDataMatrixChar(DataElementPath pathToOutputFolder, String fileName, Logger log)
        {
            TableAndFileUtils.writeCharMatrixToFile(rowNames, columnNames, matrix, pathToOutputFolder, fileName, log);
        }
        /********************* static methods *******************************/
        public static DataMatrixChar concatinateDataMatricesColumnWise(DataMatrixChar[] dmcs)
        {
            // 1. Calculate n, m, columnNames.
            int n = dmcs[0].getSize(), m = 0, index = 0;
            for( DataMatrixChar dmc : dmcs )
                m += dmc.getColumnNames().length;
            String[] columnNames = new String[m];
            for( DataMatrixChar dmc : dmcs )
            {
                String[] names = dmc.getColumnNames();
                UtilsForArray.copyIntoArray(names, columnNames, index);
                index += names.length;
            }

            // 2. Calculate matrix.
            char[][] matrix = new char[n][];
            for( int i = 0; i < n; i++ )
            {
                char[] row = new char[m];
                index = 0;
                for( DataMatrixChar dmc : dmcs )
                {
                    char[][] mat = dmc.getMatrix();
                    UtilsForArray.copyIntoArray(mat[i], row, index);
                    index += mat[i].length;
                }
                matrix[i] = row;
            }
            return new DataMatrixChar(dmcs[0].getRowNames(), columnNames, matrix);
        }
    }
    /************ DataMatrixChar : end *************************/
    
    private static Logger log = Logger.getLogger(DataMatrixString.class.getName());
}
