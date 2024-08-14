package biouml.plugins.machinelearning.utils;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataElementPath;

public class DataMatrixInteger
{
    private String[] rowNames, columnNames;
    private int[][] matrix;
    
    public DataMatrixInteger(String[] rowNames, String[] columnNames, int[][] matrix)
    {
        if( (rowNames != null && rowNames.length != matrix.length) || (columnNames != null && columnNames.length != matrix[0].length) )
        {
            this.rowNames = this.columnNames = null;
            this.matrix = null;
        }
        else
        {
            this.rowNames = rowNames;
            this.columnNames = columnNames;
            this.matrix = matrix;
        }
    }
    
    public DataMatrixInteger(String[] rowNames, String columnName, int[] column)
    {
        this( rowNames, new String[]{columnName}, MatrixUtils.transformVectorToMatrixWithSingleColumn(column) );
    }
    
    public int[] getColumn(int columnIndex)
    {
        return MatrixUtils.getColumn(matrix, columnIndex);
    }
    
    public void writeDataMatrix(DataElementPath pathToOutputFolder, String tableName)
    {
    	// TODO: ???
    	if( rowNames.length > 50000)
    		TableAndFileUtils.writeIntegerMatrixToFile(rowNames, columnNames, matrix, pathToOutputFolder, tableName, log);
    	else
    		TableAndFileUtils.writeIntegerMatrxToTable(rowNames, columnNames, matrix, pathToOutputFolder, tableName);
    }
    
    static Logger log = Logger.getLogger(DataMatrixInteger.class.getName());

}
