/* $Id$ */

package biouml.plugins.machinelearning.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang.ArrayUtils;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.plugins.machinelearning.utils.UtilsGeneral.ChartUtils;
import biouml.plugins.machinelearning.utils.UtilsGeneral.UtilsForArray;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.FileDataElement;
import ru.biosoft.access.FileImporter;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.TempFiles;
import ru.biosoft.util.TextUtil2;

/**
 * @author yura
 *
 */


// TODO: To use method writeStringToFile() in other methods for writing to File !!!
public class TableAndFileUtils
{
    public static final String NAN = "NaN";
    public static final String DOUBLE_TYPE = "Double";
    public static final String CHAR_TYPE = "Char";
    public static final String STRING_TYPE = "String";
    public static final String INT_TYPE = "Integer";


    /*************************************************/
    /**************** Utils for table ****************/
    /*************************************************/

    // Add double or String row to table
    public static void addRowToTable(double[] row, String[] rowString, String rowName, String[] columnNames, DataElementPath pathToOutputFolder, String tableName)
    {
        DataElementPath pathToTable = pathToOutputFolder.getChildPath(tableName);
        TableDataCollection table = pathToTable.optDataElement(TableDataCollection.class);
        if( table == null )
        {
            table = TableDataCollectionUtils.createTableDataCollection(pathToTable);
            for( String s : columnNames )
                if( row != null )
                    table.getColumnModel().addColumn(s.replace('/', '|'), Double.class);
                else
                    table.getColumnModel().addColumn(s.replace('/', '|'), String.class);
        }
        Object[] objects = row != null ? UtilsForArray.transformDoubleToObject(row) : UtilsForArray.transformStringToObject(rowString);
        TableDataCollectionUtils.addRow(table, rowName, objects, true);
        table.finalizeAddition();
        CollectionFactoryUtils.save(table);
    }

    public static String[] getColumnNamesInTable(TableDataCollection table)
    {
        List<String> names = new ArrayList<>();
        for( TableColumn tableColumn : table.columns().toList() )
            names.add(tableColumn.getName());
        return names.toArray(new String[0]);
    }
    
    public static String[] getRowNamesInTable(TableDataCollection table)
    {
        return table.names().toArray(String[]::new);
    }
    
    public static String[] getRowNamesInTable(DataElementPath pathToTable)
    {
        return getRowNamesInTable(pathToTable.getDataElement(TableDataCollection.class));
    }
    
    public static String[] getRowNamesInFile(DataElementPath pathToFile)
    {
        String[] fileLines = readLinesInFile(pathToFile), result = new String[fileLines.length - 1];
        for( int i = 0; i < result.length; i++ )
            result[i] = TextUtil2.split(fileLines[i + 1], '\t')[0];
        return result;
    }
    
    // If columnNames == null then read whole matrix else read corresponding submatrix.
    // matrixType (- {"Double", "String"}
    public static Object[] readMatrixOrSubmatixInTable(DataElementPath pathToTable, String[] columnNames, String matrixType)
    {
        // 1. Calculate columnNamesNew and indices of columnNamesNew.
        TableDataCollection table = pathToTable.getDataElement(TableDataCollection.class);
        String[] rowNames = getRowNamesInTable(table);
        String[] columnNamesInMatrix = getColumnNamesInTable(table);
        String[] columnNamesNew = columnNames == null ? columnNamesInMatrix : columnNames;
        int[] indices = new int[columnNamesNew.length];
        if( columnNames == null )
            for( int i = 0; i < indices.length; i++ )
                indices[i]  = i;
        else
            for( int i = 0; i < indices.length; i++ )
            {
                indices[i] = ArrayUtils.indexOf(columnNamesInMatrix, columnNames[i]);
                if( indices[i] < 0 ) return null;
            }
        
        // 2. Read matrix.
        String[][] matrixString = matrixType.equals(STRING_TYPE) ? new String[rowNames.length][indices.length] : null;
        double[][] matrixDouble = matrixType.equals(DOUBLE_TYPE) ? new double[rowNames.length][indices.length] : null;
        for( int i = 0; i < rowNames.length; i++ )
        {
            Object[] row = table.getAt(i).getValues();
            switch( matrixType )
            {
                case STRING_TYPE : for( int j = 0; j < indices.length; j++ )
                                       matrixString[i][j] = (String)row[indices[j]];
                                   break;
                case DOUBLE_TYPE : for( int j = 0; j < indices.length; j++ )
                                       matrixDouble[i][j] = (double)DataType.Float.convertValue(row[indices[j]]);
                                   break;
                default          : return null;
            }
        }
        
        // 3. Output results.
        switch( matrixType )
        {
            case STRING_TYPE : return new Object[]{rowNames, columnNamesNew, matrixString};
            case DOUBLE_TYPE : return new Object[]{rowNames, columnNamesNew, matrixDouble};
        }
        return null;
    }

    // TODO: Convert to DataMatrixString
    public static TableDataCollection writeColumnToStringTable(String[] rowNames, String columnName, String[] column, DataElementPath pathToOutputFolder, String tableName)
    {
        return writeDoubleAndStringMatricesToTable(rowNames, null, null, new String[]{columnName}, MatrixUtils.transformVectorToMatrixWithSingleColumn(column), pathToOutputFolder, tableName);
    }
    
    public static TableDataCollection writeColumnToDoubleTable(String[] rowNames, String columnName, double[] column, DataElementPath pathToOutputFolder, String tableName)
    {
        return writeDoubleAndStringMatricesToTable(rowNames, new String[]{columnName}, MatrixUtils.transformVectorToMatrixWithSingleColumn(column), null, null, pathToOutputFolder, tableName);
    }
    
    public static TableDataCollection writeDoubleAndStringMatricesToTable(String[] rowNames, String[] columnNamesDouble, double[][] matrixDouble, String[] columnNamesString, String[][] matrixString, DataElementPath pathToOutputFolder, String tableName)
    {
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(pathToOutputFolder.getChildPath(tableName));

        // 1. Create column models. 
        int numberOfDoubleColumns = matrixDouble == null ? 0 : columnNamesDouble.length, numberOfStringColumns = matrixString == null ? 0 : columnNamesString.length;
        if( matrixDouble != null )
            for( int j = 0; j < numberOfDoubleColumns; j++ )
                table.getColumnModel().addColumn(columnNamesDouble[j].replace('/', '_'), Double.class);
        if( matrixString != null )
            for( int j = 0; j < numberOfStringColumns; j++ )
                table.getColumnModel().addColumn(columnNamesString[j].replace('/', '_'), String.class);

        // 2. Create rows.
        for( int i = 0; i < rowNames.length; i++ )
        {
            Object[] row = new Object[numberOfDoubleColumns + numberOfStringColumns];
            for( int j = 0; j < numberOfDoubleColumns; j++ )
                row[j] = matrixDouble[i][j];
            for( int j = 0; j < numberOfStringColumns; j++ )
                row[j + numberOfDoubleColumns] = matrixString[i][j]; 
            TableDataCollectionUtils.addRow(table, rowNames[i].replace('/', '_'), row, true);
        }
        
        // 3. Save table.
        table.finalizeAddition();
        CollectionFactoryUtils.save(table);
        return table;
    }
    
    public static TableDataCollection writeIntegerMatrxToTable(String[] rowNames, String[] columnNames, int[][] matrix, DataElementPath pathToOutputFolder, String tableName)
    {
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(pathToOutputFolder.getChildPath(tableName));
        
        // 1. Create column models. 
        for( int i = 0; i < columnNames.length; i++ )
            table.getColumnModel().addColumn(columnNames[i].replace('/', '_'), Integer.class);
        
        // 2. Create rows.
        for( int i = 0; i < rowNames.length; i++ )
        {
            Object[] row = new Object[columnNames.length];
            for( int j = 0; j < columnNames.length; j++ )
                row[j] = matrix[i][j];
            TableDataCollectionUtils.addRow(table, rowNames[i].replace('/', '_'), row, true);
        }
        
        // 3. Save table.
        table.finalizeAddition();
        CollectionFactory.save(table);
        return table;
    }
    
    public static void addChartToTable(String chartName, Chart chart, DataElementPath pathToTable)
    {
        // TableDataCollection table = pathToTable.getDataElement(TableDataCollection.class);
        TableDataCollection table = pathToTable.optDataElement(TableDataCollection.class);
        if( table == null )
        {
            table = TableDataCollectionUtils.createTableDataCollection(pathToTable);
            table.getColumnModel().addColumn(ChartUtils.CHART, Chart.class);
        }
        TableDataCollectionUtils.addRow(table, chartName, new Object[]{chart});
        table.finalizeAddition();
        CollectionFactoryUtils.save(table);
    }

    /************************************************/
    /**************** Utils for file ****************/
    /************************************************/
    
    // If columnNames == null then read whole matrix else read corresponding submatrix.
    // matrixType (- {"Double", "Char", "String", "Integer"}
    public static Object[] readMatrixOrSubmatixInFile(DataElementPath pathToFile, String[] columnNames, String matrixType)
    {
        // 1. Read 'fileLines' and calculate 'columnNamesOutput'.
        String[] fileLines = readLinesInFile(pathToFile), columnNamesInMatrix = TextUtil2.split(fileLines[0], '\t');
        if( columnNamesInMatrix.length < 2 || fileLines.length < 2 ) return null;
        String[] columnNamesOutput = columnNames == null ? (String[])ArrayUtils.remove(columnNamesInMatrix, 0) : columnNames;
        
        // 2. Calculate indices of columnNamesOutput.
        int[] indices = new int[columnNamesOutput.length];
        if( columnNames == null )
            for( int i = 0; i < indices.length; i++ )
                indices[i]  = i + 1;
        else
            for( int i = 0; i < indices.length; i++ )
            {
                indices[i] = ArrayUtils.indexOf(columnNamesInMatrix, columnNames[i]);
                if( indices[i] < 0 ) return null;
            }
        
        // 3. Calculate matrix and rowNames.
        String[] rowNames = new String[fileLines.length - 1];
        double[][] matrixDouble = matrixType.equals(DOUBLE_TYPE) ? new double[fileLines.length - 1][] : null;
        char[][] matrixChar = matrixType.equals(CHAR_TYPE) ? new char[fileLines.length - 1][] : null;
        String[][] matrixString = matrixType.equals(STRING_TYPE) ? new String[fileLines.length - 1][] : null;
        int[][] matrixInteger = matrixType.equals(INT_TYPE) ? new int[rowNames.length][indices.length] : null;
        for( int i = 0; i < rowNames.length; i++ )
        {
            String[] tokens = TextUtil2.split(fileLines[i + 1], '\t');
            if( tokens.length < 1 + columnNamesOutput.length ) return null;
            rowNames[i] = tokens[0];
            switch( matrixType )
            {
                case DOUBLE_TYPE : double[] rowDouble = new double[columnNamesOutput.length];
                                   for( int j = 0; j < indices.length; j++ )
                                       rowDouble[j] = tokens[indices[j]].equals(NAN) ? Double.NaN : Double.parseDouble(tokens[indices[j]]);
                                   matrixDouble[i] = rowDouble; break;
                case CHAR_TYPE   : char[] rowChar = new char[columnNamesOutput.length];
                                   for( int j = 0; j < indices.length; j++ )
                                       rowChar[j] = tokens[indices[j]].charAt(0);
                                   matrixChar[i] = rowChar; break;
                case STRING_TYPE : String[] rowString = new String[columnNamesOutput.length];
                                   for( int j = 0; j < indices.length; j++ )
                                       rowString[j] = tokens[indices[j]];
                                   matrixString[i] = rowString; break;
                case INT_TYPE    : int[] rowInteger = new int[columnNamesOutput.length];
                                   for( int j = 0; j < indices.length; j++ )
                                       rowInteger[j] = Integer.parseInt(tokens[indices[j]]);
                                   matrixInteger[i] = rowInteger; break;
                default          : return null;
            }
        }
        switch( matrixType )
        {
            case DOUBLE_TYPE : return new Object[]{rowNames, columnNamesOutput, matrixDouble};
            case CHAR_TYPE   : return new Object[]{rowNames, columnNamesOutput, matrixChar};
            case STRING_TYPE : return new Object[]{rowNames, columnNamesOutput, matrixString};
            case INT_TYPE    : return new Object[]{rowNames, columnNamesOutput, matrixInteger};
        }
        return null;
    }
    
    public static String[] getColumnNamesInFile(DataElementPath pathToFile)
    {
        File file = pathToFile.getDataElement(FileDataElement.class).getFile();
        BufferedReader reader = null;
        try
        {
            reader = ApplicationUtils.asciiReader(file);
        }
        catch( IOException e )
        {
            e.printStackTrace();
        }
        String line = null;
        try
        {
            line = reader.readLine();
        }
        catch( IOException e )
        {
            e.printStackTrace();
        }
        String[] tokens = TextUtil2.split(line, '\t');
        return (String[])ArrayUtils.remove(tokens, 0);
    }

    public static String[] readLinesInFile(DataElementPath pathToFile)
    {
        List<String> fileLines = new ArrayList<>();
        if( ! pathToFile.exists() ) return null;
        File file = pathToFile.getDataElement(FileDataElement.class).getFile();
        try( BufferedReader reader = ApplicationUtils.asciiReader(file) )
        {
            while( reader.ready() )
            {
                String line = reader.readLine();
                if( line == null ) break;
                fileLines.add(line);
            }
            reader.close();
        }
        catch( IOException e )
        {
            e.printStackTrace();
        }
        return fileLines.toArray(new String[0]);
    }

    public static File writeDoubleAndStringMatricesToFile(String[] rowNames, String[] columnNamesDouble, double[][] matrixDouble, String[] columnNamesString, String[][] matrixString, DataElementPath pathToOutputFolder, String fileName, Logger log)
    {
        int nDouble = columnNamesDouble == null ? 0 : columnNamesDouble.length, nString = columnNamesString == null ? 0 : columnNamesString.length;
        File file = null;
        try
        {
            file = TempFiles.file("");
        }
        catch( IOException e1 )
        {
            e1.printStackTrace();
        }
        try( BufferedWriter bw = new BufferedWriter(new FileWriter(file)) )
        {
            String[] columnNamesAll = (String[])ArrayUtils.addAll(columnNamesDouble, columnNamesString);
            bw.write("ID\t" + convertStringArrayToTabDelimitedLine(columnNamesAll));
            bw.close();
        }
        catch( Exception e )
        {
            try
            {
                throw e;
            }
            catch( Exception e1 )
            {
                e1.printStackTrace();
            }
        }
        try( BufferedWriter bw = new BufferedWriter(new FileWriter(file, true)) )
        {
            for( int i = 0; i < rowNames.length; i++ )
            {
                String[] rowElements = new String[nDouble + nString];
                if( matrixDouble != null )
                    for( int j = 0; j < columnNamesDouble.length; j++ )
                        rowElements[j] = Double.isNaN(matrixDouble[i][j]) ? NAN : Double.toString(matrixDouble[i][j]);
                if( matrixString != null )
                    for( int j = 0; j < columnNamesString.length; j++ )
                        rowElements[nDouble + j] = matrixString[i][j];
                bw.write("\n" + rowNames[i] + "\t" + convertStringArrayToTabDelimitedLine(rowElements));
            }
            bw.close();
        }
        catch (Exception e )
        {
            try
            {
                throw e;
            }
            catch( Exception e1 )
            {
                e1.printStackTrace();
            }
        }
        FileImporter importer = new FileImporter();
        try
        {
            importer.doImport(pathToOutputFolder.getChildPath(fileName).getParentCollection(), file, fileName, null, log);
        }
        catch( RepositoryException e )
        {
            e.printStackTrace();
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
        return file;
    }
    
    public static File writeStringToFile(String string, DataElementPath pathToOutputFolder, String fileName, Logger log)
    {
        File file = null;
        try
        {
            file = TempFiles.file("");
        }
        catch( IOException e1 )
        {
            e1.printStackTrace();
        }
        try( BufferedWriter bw = new BufferedWriter(new FileWriter(file)) )
        {
            bw.write(string);
            bw.close();
        }
        catch( Exception e )
        {
            try
            {
                throw e;
            }
            catch( Exception e1 )
            {
                e1.printStackTrace();
            }
        }
        FileImporter importer = new FileImporter();
        try
        {
            importer.doImport(pathToOutputFolder.getChildPath(fileName).getParentCollection(), file, fileName, null, log);
        }
        catch( RepositoryException e )
        {
            e.printStackTrace();
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
        return file;
    }
    
    public static File writeCharMatrixToFile(String[] rowNames, String[] columnNames, char[][] matrix, DataElementPath pathToOutputFolder, String fileName, Logger log)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("ID");
        for( String s : columnNames )
            builder.append("\t").append(s);
        for( int i = 0; i < rowNames.length; i++ )
        {
            builder.append("\n").append(rowNames[i]);
            for( int j = 0; j < columnNames.length; j++ )
                builder.append("\t").append(new String(new char[]{matrix[i][j]}));
        }
        return writeStringToFile(builder.toString(), pathToOutputFolder, fileName, log);
    }
    
    public static File writeIntegerMatrixToFile(String[] rowNames, String[] columnNames, int[][] matrix, DataElementPath pathToOutputFolder, String fileName, Logger log)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("ID");
        for( String s : columnNames )
            builder.append("\t").append(s);
        for( int i = 0; i < rowNames.length; i++ )
        {
            builder.append("\n").append(rowNames[i]);
            for( int j = 0; j < columnNames.length; j++ )
                builder.append("\t").append(Integer.toString(matrix[i][j]));
        }
        return writeStringToFile(builder.toString(), pathToOutputFolder, fileName, log);
    }

    // old version
//    private static String convertStringArrayToTabDelimitedLine(String[] array)
//    {
//        String result = "";
//        for( int i = 0; i < array.length; i++ )
//        {
//            if( i > 0 )
//                result += "\t";
//            result += array[i];
//        }
//        return result;
//    }
    
    // TODO: To move to appropriate Class
    // new version
    private static String convertStringArrayToTabDelimitedLine(String[] array)
    {
        StringBuilder builder = new StringBuilder();
        for( int i = 0; i < array.length; i++ )
        {
            if( i > 0 )
                builder.append("\t");
            builder.append(array[i]);
        }
        return builder.toString();
    }
    
    /*************************************************************/
    /**************** Utils for file or/and table ****************/
    /*************************************************************/
    
    public static void writeDoubleAndStringMatrices(boolean doWriteToFile, String[] rowNames, String[] columnNamesDouble, double[][] matrixDouble, String[] columnNamesString, String[][] matrixString, DataElementPath pathToOutputFolder, String fileOrTableName, Logger log)
    {
        if( doWriteToFile )
            writeDoubleAndStringMatricesToFile(rowNames, columnNamesDouble, matrixDouble, columnNamesString, matrixString, pathToOutputFolder, fileOrTableName, log);
        else
            writeDoubleAndStringMatricesToTable(rowNames, columnNamesDouble, matrixDouble, columnNamesString, matrixString, pathToOutputFolder, fileOrTableName);
    }
    
    public static Object[] readMatrixOrSubmatix(DataElementPath pathToMatrix, String[] columnNames, String matrixType)
    {
        return pathToMatrix.getDataElement() instanceof TableDataCollection ? readMatrixOrSubmatixInTable(pathToMatrix, columnNames, matrixType) : readMatrixOrSubmatixInFile(pathToMatrix, columnNames, matrixType);
    }
    
    public static String[] getColumnNames(DataElementPath pathToMatrix)
    {
        return ! (pathToMatrix.getDataElement() instanceof TableDataCollection) ? getColumnNamesInFile(pathToMatrix) : getColumnNamesInTable(pathToMatrix.getDataElement(TableDataCollection.class));  
    }
    
    private static Logger log = Logger.getLogger(TableAndFileUtils.class.getName());
}
