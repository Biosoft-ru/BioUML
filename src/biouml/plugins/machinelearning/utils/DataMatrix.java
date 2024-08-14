/* $Id$ */

package biouml.plugins.machinelearning.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.apache.commons.lang.ArrayUtils;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.Util;
import biouml.plugins.machinelearning.utils.MatrixUtils;
import biouml.plugins.machinelearning.utils.StatUtils.MultivariateSample;
import biouml.plugins.machinelearning.utils.TableAndFileUtils;
import biouml.plugins.machinelearning.utils.UtilsGeneral;
import biouml.plugins.machinelearning.utils.UtilsGeneral.UtilsForArray;

/**
 * @author yura
 *
 */
public class DataMatrix
{
    private String[] rowNames, columnNames;
    private double[][] matrix;
    
    public DataMatrix(String[] rowNames, String[] columnNames, double[][] matrix)
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
    
    public DataMatrix(String[] rowNames, String columnName, double[] column)
    {
        this( rowNames, new String[]{columnName}, MatrixUtils.transformVectorToMatrixWithSingleColumn(column) );
    }
    
    public DataMatrix(String rowName, String[] columnNames, double[] row)
    {
        this( new String[]{rowName}, columnNames, MatrixUtils.transformVectorToMatrixWithSingleRow(row) );
    }
    
    public DataMatrix(DataElementPath pathToMatrix, String[] columnNames)
    {
        Object[] objects = TableAndFileUtils.readMatrixOrSubmatix(pathToMatrix, columnNames, TableAndFileUtils.DOUBLE_TYPE);
        //18.04.22
        if( objects != null )
        {
        	this.rowNames = (String[])objects[0];
            this.columnNames = (String[])objects[1];
            this.matrix = (double[][])objects[2];
        }
    }
    
    public void transpose()
    {
        String[] old = rowNames;
        rowNames = columnNames;
        columnNames = old;
        matrix = MatrixUtils.getTransposedMatrix(matrix);
    }
    
    public String[] getRowNames()
    {
        return rowNames;
    }
    
    public void setRowNames(String[] rowNames)
    {
        this.rowNames = rowNames;
    }
    
    public String[] getColumnNames()
    {
        return columnNames;
    }
    
    public double[][] getMatrix()
    {
        return matrix;
    }
    
    public int getSize()
    {
        return matrix.length;
    }
    
    public DataMatrix getSemiClone()
    {
        return new DataMatrix(rowNames, columnNames, MatrixUtils.getClone(matrix));
    }
    
    public double[] getColumn(int columnIndex)
    {
        return MatrixUtils.getColumn(matrix, columnIndex);
    }

    public double[] getColumn(String columnName)
    {
        int index = ArrayUtils.indexOf(columnNames, columnName);
        return index < 0 ? null : getColumn(index);
    }
    
    public double[] getRow(String rowName)
    {
        int index = ArrayUtils.indexOf(rowNames, rowName);
        return index < 0 ? null : matrix[index];
    }
    
    public DataMatrix getRow(int index)
    {
        return new DataMatrix(rowNames[index], columnNames, matrix[index]);
    }
    
    public void addColumn(String columnName, double[] column, int columnIndex)
    {
        columnNames = (String[])ArrayUtils.add(columnNames, columnIndex, columnName);
        for( int i = 0; i < matrix.length; i++ )
            matrix[i] = ArrayUtils.add(matrix[i], columnIndex, column[i]);
    }
    
    public void writeDataMatrix(boolean doWriteToFile, DataElementPath pathToOutputFolder, String fileOrTableName, Logger log)
    {
        TableAndFileUtils.writeDoubleAndStringMatrices(doWriteToFile, rowNames, columnNames, matrix, null, null, pathToOutputFolder, fileOrTableName, log);
    }
    
    public boolean writeDataMatrix(boolean doWriteToFile, DataMatrixString dms, DataElementPath pathToOutputFolder, String fileOrTableName, Logger log)
    {
        if( ! UtilsForArray.equal(dms.getRowNames(), rowNames) ) return false;
        TableAndFileUtils.writeDoubleAndStringMatrices(doWriteToFile, rowNames, columnNames, matrix, dms.getColumnNames(), dms.getMatrix(), pathToOutputFolder, fileOrTableName, log);
        return true;
    }

    public void removeColumn(String columnName)
    {
        int index = ArrayUtils.indexOf(columnNames, columnName);
        if( index >= 0 )
            removeColumn(index);
    }

    public void removeColumn(int columnIndex)
    {
        columnNames = (String[])ArrayUtils.remove(columnNames, columnIndex);
        MatrixUtils.removeColumn(matrix, columnIndex);
    }

    public void removeColumns(String[] columnNames)
    {
    	ArrayList<Integer> indexesToRemove = new ArrayList<Integer>();
    	for(String columnName : columnNames)
    	{
    		int index = ArrayUtils.indexOf(columnNames, columnName);
    		if( index >= 0 )
    		{
    			indexesToRemove.add(index);
    		}
    	}
    	removeColumns(indexesToRemove.stream().mapToInt(i -> i).toArray());
    }

    public void removeColumns(int[] columnIndexes)
    {
        columnNames = (String[])org.apache.commons.lang3.ArrayUtils.removeAll(columnNames, columnIndexes);
        MatrixUtils.removeColumns(matrix, columnIndexes);
    }
    
    // It remove rows with NaN values
    public void removeRowsWithMissingData()
    {
        List<String> rowNamesNew = new ArrayList<>();
        List<double[]> matrixNew = new ArrayList<>();
        for( int i = 0; i < rowNames.length; i++ )
            if( ! UtilsForArray.doContainNan(matrix[i]) )
            {
                rowNamesNew.add(rowNames[i]);
                matrixNew.add(matrix[i]);
            }
        rowNames = rowNamesNew.toArray(new String[0]);
        matrix = matrixNew.toArray(new double[matrixNew.size()][]);
    }
    
    public double[] removeRowsWithMissingData(double[] additionalColumn)
    {
        List<String> rowNamesNew = new ArrayList<>();
        List<double[]> matrixNew = new ArrayList<>();
        List<Double> additionalColumnNew = new ArrayList<>();
        for( int i = 0; i < rowNames.length; i++ )
            if( ! UtilsForArray.doContainNan(matrix[i]) && ! Double.isNaN(additionalColumn[i]) )
            {
                rowNamesNew.add(rowNames[i]);
                matrixNew.add(matrix[i]);
                additionalColumnNew.add(additionalColumn[i]);
            }
        rowNames = rowNamesNew.toArray(new String[0]);
        matrix = matrixNew.toArray(new double[matrixNew.size()][]);
        return UtilsGeneral.fromListToArray(additionalColumnNew);
    }
    
    public String[] removeRowsWithMissingData(String[] additionalColumn)
    {
        List<String> rowNamesNew = new ArrayList<>();
        List<double[]> matrixNew = new ArrayList<>();
        List<String> additionalColumnNew = new ArrayList<>();
        for( int i = 0; i < rowNames.length; i++ )
            if( ! UtilsForArray.doContainNan(matrix[i]) && additionalColumn[i] != null )
            {
                rowNamesNew.add(rowNames[i]);
                matrixNew.add(matrix[i]);
                additionalColumnNew.add(additionalColumn[i]);
            }
        rowNames = rowNamesNew.toArray(new String[0]);
        matrix = matrixNew.toArray(new double[matrixNew.size()][]);
        return additionalColumnNew.toArray(new String[0]);
    }
    
    public boolean addAnotherDataMatrixRowWise(DataMatrix dataMatrix)
    {
        if( ! UtilsForArray.equal(dataMatrix.getColumnNames(), columnNames) ) return false;
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
    
    public boolean addAnotherDataMatrixColumnWise(DataMatrix dataMatrix)
    {
        if( ! UtilsForArray.equal(dataMatrix.getRowNames(), rowNames) ) return false;
        columnNames = (String[])ArrayUtils.addAll(columnNames, dataMatrix.getColumnNames());
        double[][] matrix = dataMatrix.getMatrix();
        for( int i = 0; i < matrix.length; i++ )
            this.matrix[i] = ArrayUtils.addAll(this.matrix[i], matrix[i]);
        return true;
    }
    
    public DataMatrix getSubDataMatrixColumnWise(String[] columnNamesSelected)
    {
        int[] indices = new int[columnNamesSelected.length];
        for( int i = 0; i < indices.length; i++ )
            indices[i] = ArrayUtils.indexOf(columnNames, columnNamesSelected[i]);
        double[][] matrixNew = MatrixUtils.getSubMatrixColumnWise(indices, matrix);
        return new DataMatrix(rowNames, columnNamesSelected, matrixNew);
    }
    
    public DataMatrix getSubDataMatrixRowWise(int startIndexInclusive, int endIndexExclusive)
    {
        return new DataMatrix((String[])ArrayUtils.subarray(rowNames, startIndexInclusive, endIndexExclusive), columnNames, MatrixUtils.getSubMatrixRowWise(startIndexInclusive, endIndexExclusive, matrix));
    }
    
    // dim(doIncludeRow) = dim(rowNames), i.e. number of rows in data matrix
    public DataMatrix getSubDataMatrixRowWise(boolean[] doIncludeRow)
    {
    	DataMatrixConstructor dmc = new DataMatrixConstructor(columnNames);
    	for( int i = 0; i < rowNames.length; i++ )
    		if( doIncludeRow[i] )
    			dmc.addRow(rowNames[i], matrix[i]);
    	return dmc.getDataMatrix();
    }
    
    public void replaceColumnName(String oldName, String newName)
    {
        int index = ArrayUtils.indexOf(columnNames, oldName);
        if( index >= 0 )
            columnNames[index] = newName;
    }
    
    public void replaceColumnNames(String[] oldNames, String[] newNames)
    {
        for( int i = 0; i < oldNames.length; i++ )
        	replaceColumnName(oldNames[i], newNames[i]);
    }

    
    public void replaceRowNames(String[] rowNames)
    {
        this.rowNames = rowNames;
    }
    
    public void calculateAveragesOfRowsThatHaveSameRowNames()
    {
        String[] distinctRowNames = UtilsGeneral.getDistinctValues(rowNames);
        if( distinctRowNames.length == rowNames.length ) return;
        
        // 1. Calculate distinctNameAndIndices.
        Map<String, List<Integer>> distinctNameAndIndices = new HashMap<>();
        for( int i = 0; i < rowNames.length; i++ )
            distinctNameAndIndices.computeIfAbsent(rowNames[i], key -> new ArrayList<>()).add(i);
        
        // 2. Calculate matrixNew and rowNamesNew.
        double[][] matrixNew = new double[distinctNameAndIndices.size()][];
        String[] rowNamesNew = new String[distinctNameAndIndices.size()];
        int index = 0;
        for( Entry<String, List<Integer>> entry : distinctNameAndIndices.entrySet() )
        {
            rowNamesNew[index] = entry.getKey();
            List<Integer> indices = entry.getValue();
            if( indices.size() == 1 )
                matrixNew[index++] = matrix[indices.get(0)];
            else
            {
                int[] array = UtilsGeneral.fromListIntegerToArray(indices);
                double[][] submatrix = MatrixUtils.getSubMatrixRowWise(array, matrix);
                // TODO: to call MultivariateSample is not good???
                matrixNew[index++] = MultivariateSample.getMeanVector(submatrix);
            }
        }
        matrix = matrixNew;
        rowNames = rowNamesNew;
    }

    public void fillColumn(double[] columnValues, String columnName)
    {
        int index = ArrayUtils.indexOf(columnNames, columnName);
        fillColumn(columnValues, index);
    }

    public void fillColumn(double[] columnValues, int columnIndex)
    {
        MatrixUtils.fillColumn(matrix, columnValues, columnIndex);
    }
    
    /************ DataMatrixConstructor : start ***************/
    public static class DataMatrixConstructor
    {
        String[] columnNames;
        List<String> rowNames;
        List<double[]> rows;
        
        public DataMatrixConstructor(String[] columnNames)
        {
            this.columnNames = columnNames;
            rowNames = new ArrayList<>();
            rows = new ArrayList<>();
        }
        
        public void addRow(String rowName, double[] rowValues)
        {
            rowNames.add(rowName);
            rows.add(rowValues);
        }
        
        public DataMatrix getDataMatrix()
        {
            return new DataMatrix(rowNames.toArray(new String[0]), columnNames, rows.toArray(new double[rows.size()][]));
        }
    }
    /*************** DataMatrixConstructor : end ************************/

    /********************************************************************/
    /************************ static methods ****************************/
    /********************************************************************/
    
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
                double[][] matrix = MatrixUtils.getClone(list.get(0).getMatrix());
                for( int j = 1; j < list.size(); j++ )
                    matrix = MatrixUtils.getSumOfMatrices(matrix, list.get(j).getMatrix());
                matrix = MatrixUtils.getProductOfMatrixAndScalar(matrix, 1.0 / (double)list.size());
                DataMatrix dm = list.get(0);
                result.add(new DataMatrix(dm.getRowNames(), dm.getColumnNames(), matrix));
            }
        }
        return result.toArray(new DataMatrix[0]);
    }
    
    public static boolean doDataMatricesHaveSameRowAndColumnNames(DataMatrix dataMatrix1, DataMatrix dataMatrix2)
    {
        if( dataMatrix1 == null || dataMatrix2 == null ) return false;
        String[] rowNames1 = dataMatrix1.getRowNames(), rowNames2 = dataMatrix2.getRowNames(), columnNames1 = dataMatrix1.getColumnNames(), columnNames2 = dataMatrix2.getColumnNames();
        if( rowNames1 == null || rowNames2 == null || columnNames1 == null || columnNames2 == null ) return false;
        if( ! UtilsForArray.equal(rowNames1, rowNames2) ) return false;
        if( ! UtilsForArray.equal(columnNames1, columnNames2) ) return false;
        return true;
    }
    
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder(); 
        builder.append("ID");
        for( String s : columnNames )
            builder.append("\t").append(s);
        for( int i = 0; i < rowNames.length; i++ )
        {
            builder.append("\n").append(rowNames[i]);
            for( int j = 0; j < columnNames.length; j++ )
                builder.append("\t").append(String.valueOf(matrix[i][j])); 
        }
        return builder.toString();
    }
    
    public void sortByColumn(int columnIndex)
    {
        double[] vector = MatrixUtils.getColumn(matrix, columnIndex);
        int[] positions = Util.sortHeap(vector);
        String[] rowNamesNew = new String[rowNames.length];
        double[][] matrixNew = new double[matrix.length][];
        for( int j = 0; j < positions.length; j++ )
        {
            rowNamesNew[j] = rowNames[positions[j]];
            matrixNew[j] = matrix[positions[j]];
        }
        rowNames = rowNamesNew;
        matrix = matrixNew; 
    }
    
    /************************ static methods ******************/
    
    public static Object[] splitRowWise(DataMatrix dataMatrix, double[] array, String[] arrayString, int[] rowIndicesForFirstMatrix)
    {
        double[][] matrix = dataMatrix.getMatrix();
        String[] rowNames = dataMatrix.getRowNames(); 
        boolean[] isForFirstMatrix = new boolean[matrix.length];
        for( int i = 0; i < rowIndicesForFirstMatrix.length; i++ )
            isForFirstMatrix[rowIndicesForFirstMatrix[i]] = true;
        DataMatrixConstructor dmConstructor1 = new DataMatrixConstructor(dataMatrix.getColumnNames()), dmConstructor2 = new DataMatrixConstructor(dataMatrix.getColumnNames());
        List<Double> array1 = array != null ? new ArrayList<>() : null;
        List<Double> array2 = array != null ? new ArrayList<>() : null;
        List<String> arrayString1 = arrayString != null ? new ArrayList<>() : null;
        List<String> arrayString2 = arrayString != null ? new ArrayList<>() : null;
        for( int i = 0; i < isForFirstMatrix.length; i++)
        {
            if( isForFirstMatrix[i] )
            {
                dmConstructor1.addRow(rowNames[i], matrix[i]);
                if( array1 != null )
                    array1.add(array[i]);
                if( arrayString1 != null )
                    arrayString1.add(arrayString[i]);
            }
            else
            {
                dmConstructor2.addRow(rowNames[i], matrix[i]);
                if( array2 != null )
                    array2.add(array[i]);
                if( arrayString2 != null )
                    arrayString2.add(arrayString[i]);
            }
        }
        DataMatrix dm1 = dmConstructor1.getDataMatrix(), dm2 = dmConstructor2.getDataMatrix();
        double[] arr1 = array == null ? null : UtilsGeneral.fromListToArray(array1);
        double[] arr2 = array == null ? null : UtilsGeneral.fromListToArray(array2);
        String[] arrString1 = arrayString == null ? null : arrayString1.toArray(new String[0]);
        String[] arrString2 = arrayString == null ? null : arrayString2.toArray(new String[0]);
        return new Object[]{dm1, dm2, arr1, arr2, arrString1, arrString2};
    }
    
    public static DataMatrix concatinateDataMatricesRowWise(DataMatrix[] dataMatrices)
    {
        int n = 0, index = 0;
        for( DataMatrix dm : dataMatrices )
            n += dm.getSize();
        double[][] matrix = new double[n][];
        String[] rowNames = new String[n];
        for( DataMatrix dm : dataMatrices )
        {
            double[][] mat = dm.getMatrix();
            String[] names = dm.getRowNames();
            for( int j = 0; j < mat.length; j++ )
            {
                matrix[index] = mat[j];
                rowNames[index++] = names[j];
            }
        }
        return new DataMatrix(rowNames, dataMatrices[0].getColumnNames(), matrix);
    }
    
    // TODO: To optimize it, i.e create new version such as DataMatrixChar.concatinateDataMatricesColumnWise().
    public static DataMatrix concatinateDataMatricesColumnWise(DataMatrix[] dataMatrices)
    {
    	if( dataMatrices.length == 1 ) return dataMatrices[0];
        int n = dataMatrices[0].getSize();
        double[][] matrix = new double[n][];
        for( int i = 0; i < n; i++ )
        {
            matrix[i] = dataMatrices[0].getMatrix()[i];
            for( int j = 1; j < dataMatrices.length; j++ )
                matrix[i] = ArrayUtils.addAll(matrix[i], dataMatrices[j].getMatrix()[i]);
        }
        String[] columnNames = dataMatrices[0].getColumnNames();
        for( int j = 1; j < dataMatrices.length; j++ )
            columnNames = (String[])ArrayUtils.addAll(columnNames, dataMatrices[j].getColumnNames());
        return new DataMatrix(dataMatrices[0].getRowNames(), columnNames, matrix);
    }
    
    // If some row name of one dataMatrix does not exist in other matrix then this row is removed.
    public static DataMatrix mergeDataMatricesColumnWise(DataMatrix dataMatrix1, DataMatrix dataMatrix2)
    {
        String[] rowNames1 = dataMatrix1.getRowNames(), rowNames2 = dataMatrix2.getRowNames(), columnNames = dataMatrix1.getColumnNames();
        columnNames = (String[])ArrayUtils.addAll(columnNames, dataMatrix2.getColumnNames());
        Object[] objects = UtilsForArray.compareArrays(rowNames1, rowNames2);
        int[] indices1 = (int[])objects[0], indices2 = (int[])objects[1];
        DataMatrixConstructor dmc = new DataMatrixConstructor(columnNames);
        double[][] matrix1 = dataMatrix1.getMatrix(), matrix2 = dataMatrix2.getMatrix(); 
        for( int i = 0; i < indices1.length; i++ )
        {
            double[] row = ArrayUtils.addAll(matrix1[indices1[i]], matrix2[indices2[i]]);
            dmc.addRow(rowNames1[indices1[i]], row);
        }
        return dmc.getDataMatrix();
    }
    
    public static void printDataMatrix(DataMatrix dataMatrix)
    {
    	printDataMatrix(dataMatrix, dataMatrix.getSize());
    }
    
    public static void printDataMatrix(DataMatrix dataMatrix, int numberOfRowsForPrint)
    {
    	String[] columnNames = dataMatrix.getColumnNames(), rowNames = dataMatrix.getRowNames();
        double[][] matrix = dataMatrix.getMatrix();

		log.info("--- DataMatrix: dim = " + matrix.length +  " x " + matrix[0].length);
		String str = "--- columnNames = ";
        for( int i = 0; i < columnNames.length; i++ )
        	str += " " + columnNames[i];
        log.info(str);
        int n = Math.min(rowNames.length, numberOfRowsForPrint);
        for( int i = 0; i < n; i++ )
        {
        	str = "--- row[" + i + "] " + rowNames[i];
            for( int j = 0; j < columnNames.length; j++ )
            	str += " " + Double.toString(matrix[i][j]);
            log.info(str);
        }
    }

    static Logger log = Logger.getLogger(DataMatrix.class.getName());
}