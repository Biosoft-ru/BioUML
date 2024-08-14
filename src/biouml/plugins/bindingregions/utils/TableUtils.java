package biouml.plugins.bindingregions.utils;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang.ArrayUtils;

import com.developmentontheedge.application.ApplicationUtils;

import one.util.streamex.DoubleStreamEx;
import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.FileDataElement;
import ru.biosoft.access.FileImporter;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementReadException;
import ru.biosoft.analysis.Stat;
import ru.biosoft.graphics.chart.AxisOptions;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.graphics.chart.ChartOptions;
import ru.biosoft.graphics.chart.ChartSeries;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.ColorUtils;
import ru.biosoft.util.TempFiles;
import ru.biosoft.util.TextUtil;

public class TableUtils
{
    public static final String CHART = "chart";

    public static TableDataCollection writeIntegerTable(int[][] data, String[] namesOfRows, String[] namesOfColumns, DataElementPath pathToOutputs, String tableName)
    {
        DataElementPath dep = pathToOutputs.getChildPath(tableName);
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(dep);
        int numberOfRows = namesOfRows.length, numberOfColumns = namesOfColumns.length;

        for( int j = 0; j < numberOfColumns; j++ )
        {
            String s = namesOfColumns[j].replace('/', '|');
            table.getColumnModel().addColumn(s, Integer.class);
        }
        for( int i = 0; i < numberOfRows; i++ )
        {
            Integer[] row = new Integer[numberOfColumns];
            for( int j = 0; j < numberOfColumns; j++ )
                row[j] = data[i][j];
            String s = namesOfRows[i].replace('/', '|');
            TableDataCollectionUtils.addRow(table, s, row, true);
        }
        table.finalizeAddition();
        CollectionFactoryUtils.save(table);
        return table;
    }
    
    public static TableDataCollection writeIntegerTable(int[] dataVector, String[] namesOfRows, String namesOfColumn, DataElementPath pathToOutputs, String tableName)
    {
        int[][] dataMatrix = new int[dataVector.length][1];
        for( int i = 0; i < dataVector.length; i++ )
            dataMatrix[i][0] = dataVector[i];
        return writeIntegerTable(dataMatrix, namesOfRows, new String[]{namesOfColumn}, pathToOutputs, tableName);
    }

    public static TableDataCollection writeStringTable(String[][] data, String[] namesOfRows, String[] namesOfColumns, DataElementPath dep)
    {
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(dep);
        int numberOfRows = namesOfRows.length, numberOfColumns = namesOfColumns.length;
        for( int j = 0; j < numberOfColumns; j++ )
        {
            String s = namesOfColumns[j].replace('/', '|');
            table.getColumnModel().addColumn(s, String.class);
        }
        for( int i = 0; i < numberOfRows; i++ )
        {
            String[] row = new String[numberOfColumns];
            for( int j = 0; j < numberOfColumns; j++ )
                row[j] = data[i][j];
            String s = namesOfRows[i].replace('/', '|');
            TableDataCollectionUtils.addRow(table, s, row, true);
        }
        table.finalizeAddition();
        CollectionFactoryUtils.save(table);
        return table;
    }
    
    public static TableDataCollection writeStringTable(String[] data, String[] namesOfRows, String namesOfColumn, DataElementPath dep)
    {
        String[][] newData = new String[data.length][1];
        for( int i = 0; i < data.length; i++ )
            newData[i][0] = data[i];
        return writeStringTable(newData, namesOfRows, new String[]{namesOfColumn}, dep);
    }

    // TODO: to remove?
    public static TableDataCollection writeFloatTable(Float[][] data, String[] namesOfRows, String[] namesOfColumns, DataElementPath dataElementPath, String nameOfTable)
    {
        DataElementPath dep = dataElementPath.getChildPath(nameOfTable);
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(dep);
        int numberOfRows = namesOfRows.length, numberOfColumns = namesOfColumns.length;
        for( int j = 0; j < numberOfColumns; j++ )
        {
            String s = namesOfColumns[j].replace('/', '|');
            table.getColumnModel().addColumn(s, Double.class);
        }
        for( int i = 0; i < numberOfRows; i++ )
        {
            Float[] row = new Float[numberOfColumns];
            for( int j = 0; j < numberOfColumns; j++ )
                row[j] = data[i][j];
            String s = namesOfRows[i].replace('/', '|');
            TableDataCollectionUtils.addRow(table, s, row, true);
        }
        table.finalizeAddition();
        CollectionFactoryUtils.save(table);
        return table;
    }

    // TODO: to remove?
    public static TableDataCollection writeFloatTable(Float[][] data, String[] namesOfColumns, DataElementPath dataElementPath, String nameOfTable)
    {
        int n = data.length;
        String[] namesOfRows = new String[n];
        for( int i = 1; i <= n; i++ )
            namesOfRows[i - 1] = String.valueOf(i);
        return writeFloatTable(data, namesOfRows, namesOfColumns, dataElementPath, nameOfTable);
    }

    public static TableDataCollection writeDoubleTable(double[][] data, String[] namesOfRows, String[] namesOfColumns, DataElementPath pathToOutputs, String tableName)
    {
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(pathToOutputs.getChildPath(tableName));
        int numberOfColumns = namesOfColumns.length;
        for( int j = 0; j < numberOfColumns; j++ )
        {
            String s = namesOfColumns[j].replace('/', '|');
            table.getColumnModel().addColumn(s, Double.class);
        }
        for( int i = 0; i < namesOfRows.length; i++ )
        {
            Object[] row = new Object[numberOfColumns];
            for( int j = 0; j < numberOfColumns; j++ )
                row[j] = data[i][j];
            String s = namesOfRows[i].replace('/', '|');
            TableDataCollectionUtils.addRow(table, s, row, true);
        }
        table.finalizeAddition();
        CollectionFactoryUtils.save(table);
        return table;
    }
    
    ///// it is copied
    public static TableDataCollection writeDoubleAndString(double[][] doubleData, String[][] stringData, String[] namesOfRows, String[] namesOfDoubleColumns, String[] namesOfStringColumns, DataElementPath pathToOutputs, String tableName)
    {
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(pathToOutputs.getChildPath(tableName));

        // 1. 
        int numberOfDoubleColumns = namesOfDoubleColumns.length;
        for( int j = 0; j < numberOfDoubleColumns; j++ )
        {
            String s = namesOfDoubleColumns[j].replace('/', '|');
            table.getColumnModel().addColumn(s, Double.class);
        }
        
        // 2.
        int numberOfStringColumns = namesOfStringColumns.length;
        for( int j = 0; j < numberOfStringColumns; j++ )
        {
            String s = namesOfStringColumns[j].replace('/', '|');
            table.getColumnModel().addColumn(s, String.class);
        }
        
        // 3.
        for( int i = 0; i < namesOfRows.length; i++ )
        {
            Object[] row = new Object[numberOfDoubleColumns + numberOfStringColumns];
            for( int j = 0; j < numberOfDoubleColumns; j++ )
                row[j] = doubleData[i][j];
            for( int j = 0; j < numberOfStringColumns; j++ )
                row[j + numberOfDoubleColumns] = stringData[i][j]; 
            String s = namesOfRows[i].replace('/', '|');
            TableDataCollectionUtils.addRow(table, s, row, true);
        }
        table.finalizeAddition();
        CollectionFactoryUtils.save(table);
        return table;
    }
    
    public static void writeTableWithSquareSymmetricMatrix(String[] variableNames, double[][] symmetricMatrix, DataElementPath pathToOutputs, String tableName)
    {
        double[][] squareSymmetricMatrix = MatrixUtils.transformSymmetricMatrixToSquareMatrix(symmetricMatrix);
        Object[] objects = MatrixUtils.rearrangeSquareSymmetricMatrix(variableNames, squareSymmetricMatrix);
        writeDoubleTable((double[][])objects[1], (String[])objects[0], (String[])objects[0], pathToOutputs, tableName);
    }
    
    public static TableDataCollection writeDoubleTable(double[] dataVector, String[] namesOfRows, String nameOfColumn, DataElementPath pathToOutputs, String tableName)
    {
        double[][] dataMatrix = new double[dataVector.length][1];
        for( int i = 0; i < dataVector.length; i++ )
            dataMatrix[i][0] = dataVector[i];
        return writeDoubleTable(dataMatrix, namesOfRows, new String[]{nameOfColumn}, pathToOutputs, tableName);
    }
    
    // It is copied
    public static void addRowToDoubleTable(double[] row, String nameOfRow, String[] namesOfColumns, DataElementPath dataElementPath, String nameOfTable)
    {
        DataElementPath pathToTable = dataElementPath.getChildPath(nameOfTable);
        TableDataCollection table = pathToTable.optDataElement(TableDataCollection.class);
        if( table == null )
        {
            table = TableDataCollectionUtils.createTableDataCollection(pathToTable);
            for( String namesOfColumn : namesOfColumns )
                table.getColumnModel().addColumn(namesOfColumn.replace('/', '|'), Double.class);
        }
        Object[] objects = new Object[row.length];
        for( int i = 0; i < row.length; i++ )
            objects[i] = row[i];
        TableDataCollectionUtils.addRow(table, nameOfRow, objects, true);
        table.finalizeAddition();
        CollectionFactoryUtils.save(table);
    }
    
    public static void addRowToTable(String[] typesOfColumns, String[] namesOfColumns, String rowName, Object[] rowElements, DataElementPath pathToOutputs, String tableName) throws Exception
    {
        DataElementPath pathToTable = pathToOutputs.getChildPath(tableName);
        TableDataCollection table = pathToTable.optDataElement(TableDataCollection.class);
        if( table == null )
        {
            table = TableDataCollectionUtils.createTableDataCollection(pathToTable);
            for( int i = 0; i < typesOfColumns.length; i++ )
                switch( typesOfColumns[i] )
                {
                    case "String"  : table.getColumnModel().addColumn(namesOfColumns[i], String.class); break;
                    case "Double"  : table.getColumnModel().addColumn(namesOfColumns[i], Double.class); break;
                    case "Integer" : table.getColumnModel().addColumn(namesOfColumns[i], Integer.class); break;
                    default        : throw new Exception("The column type '" + typesOfColumns[i] + "' is not supported currently");
                }
        }
        String name = rowName != null ? rowName : Integer.toString(table.getSize());
        TableDataCollectionUtils.addRow(table, name, rowElements, true);
        table.finalizeAddition();
        CollectionFactoryUtils.save(table);
    }

    public static Map<String, String> readGivenColumnInStringTable(DataElementPath pathToTable, String columnName)
    {
        if(pathToTable == null) return null;
        TableDataCollection table = pathToTable.optDataElement(TableDataCollection.class);
        if( table == null ) return null;
        return readGivenColumnInStringTable(table, table.getColumnModel().getColumnIndex(columnName));
    }

    private static Map<String, String> readGivenColumnInStringTable(TableDataCollection table, int columnIndex)
    {
        return table.stream().collect( Collectors.toMap( RowDataElement::getName, rde -> (String)rde.getValues()[columnIndex] ) );
    }
    
    ///// it is copied !!!
    public static String[] readGivenColumnInStringTable(TableDataCollection table, String columnName)
    {
        int columnIndex = table.getColumnModel().getColumnIndex(columnName);
        String[] result = table.stream().map(rde -> DataType.Text.convertValue(rde.getValues()[columnIndex])).toArray(String[]::new);
        for( int i = 0; i < result.length; i++ )
            if( result[i] != null && result[i].equals("null") )
                result[i] = null;
        return result;
    }
    
    public static Object[] readGivenColumnInStringTableWithRowNames(DataElementPath pathToTable, String columnName)
    {
        TableDataCollection table = pathToTable.getDataElement(TableDataCollection.class);
        String[] strings = readGivenColumnInStringTable(table, columnName), rowNames = readRowNamesInTable(table);
        return new Object[]{rowNames, strings};
    }

    public static List<String> readRowNamesInTable(DataElementPath pathToTable)
    {
        return pathToTable.getDataElement( TableDataCollection.class ).names().collect( Collectors.toList() );
    }
    
    ///// it is copied
    public static String[] readRowNamesInTable(TableDataCollection table)
    {
        return table.names().toArray(String[]::new);
    }

    /////////////////////////////////////////////////////////////////////////////////////
    
    private static double getDoubleValue(RowDataElement row, int columnIndex)
    {
        Object val = DataType.Float.convertValue(row.getValues()[columnIndex]);
        if( val instanceof Double ) return (Double)val;
        throw new DataElementReadException(new Exception("Number expected: " + val), row, "column#" + columnIndex);
    }
    
    private static int getIntegerValue(RowDataElement row, int columnIndex)
    {
        Object val = DataType.Integer.convertValue(row.getValues()[columnIndex]);
        if( val instanceof Integer ) return (Integer)val;
        throw new DataElementReadException(new Exception("Number expected: " + val), row, "column#" + columnIndex);
    }

    
    public static double[] readGivenColumnInDoubleTableAsArray(TableDataCollection table, String columnName)
    {
        int columnIndex = table.getColumnModel().getColumnIndex(columnName);
        return table.stream().mapToDouble(rde -> getDoubleValue(rde, columnIndex)).toArray();
    }
    
    public static int[] readGivenColumnInIntegerTable(TableDataCollection table, String columnName)
    {
        int columnIndex = table.getColumnModel().getColumnIndex(columnName);
        return table.stream().mapToInt(rde -> getIntegerValue(rde, columnIndex)).toArray();
    }

    
    public static Map<String, Double> readGivenColumnInDoubleTableAsMap(DataElementPath pathToTable, String columnName)
    {
        TableDataCollection table = pathToTable.getDataElement(TableDataCollection.class);
        int columnIndex = table.getColumnModel().getColumnIndex(columnName);
        return table.stream().collect( Collectors.toMap( RowDataElement::getName, rde -> getDoubleValue( rde, columnIndex ) ) );
    }
    
    public static Map<String, Double> readSignificantCoefficientNameAnValuesInDoubleColumn(TableDataCollection table, String coefficientsColumnName, String significanceColumnName, double pValueThreshold)
    {
        int significanceColumnIndex = table.getColumnModel().getColumnIndex(significanceColumnName);
        int coefficientsColumnIndex = table.getColumnModel().getColumnIndex(coefficientsColumnName);
        return table.stream()
                .filter(rde -> getDoubleValue(rde, significanceColumnIndex) <= pValueThreshold)
                .collect( Collectors.toMap( RowDataElement::getName, rde -> getDoubleValue( rde, coefficientsColumnIndex ) ) );
    }
    
    //// It is copied
    public static String[] getColumnNamesInTable(TableDataCollection table)
    {
        return table.columns().map(TableColumn::getName).toArray(String[]::new);
    }
    
    public static String[] getColumnNamesInTable(DataElementPath pathToTable)
    {
        return getColumnNamesInTable(pathToTable.getDataElement(TableDataCollection.class));
    }
    
    /***
     * 
     * @param pathToTable
     * @return Object[] array : array[0] = String[] rowNames; array[1] = String[] columnNames; array[2] = double[][] dataMatrix;
     */
    public static Object[] readDoubleMatrixInTable(DataElementPath pathToTable)
    {
        TableDataCollection table = pathToTable.getDataElement(TableDataCollection.class);
        int n = table.getSize();
        double[][] dataMatrix = new double[n][];
        String[] rowNames = new String[n];
        for( int i = 0; i < n; i++ )
        {
            RowDataElement row = table.getAt(i);
            rowNames[i] = row.getName();
            dataMatrix[i] = IntStreamEx.ofIndices(row.getValues()).mapToDouble(idx -> getDoubleValue(row, idx) ).toArray();
        }
        String[] columnNames = table.columns().map(TableColumn::getName).toArray(String[]::new);
        return new Object[]{rowNames, columnNames, dataMatrix};
    }
    
    public static Object[] readDoubleMatrixOrSubmatrix(DataElementPath pathToTable, String[] columnNames)
    {
        if( columnNames == null )
            return readDoubleMatrixInTable(pathToTable);
        Object[] objects = readDataSubMatrix(pathToTable, columnNames);
        return new Object[]{objects[0], columnNames, objects[1]};
    }
    
    public static Object[] readStringMatrixOrSubmatrix(DataElementPath pathToTable, String[] columnNames)
    {
        if( columnNames == null )
            return readStringMatrixInTable(pathToTable);
        Object[] objects = readStringDataSubMatrix(pathToTable, columnNames);
        return new Object[]{objects[0], columnNames, objects[1]};
    }

    /////// It is copied !!!
    public static Object[] readStringMatrixInTable(DataElementPath pathToTable)
    {
        TableDataCollection table = pathToTable.getDataElement(TableDataCollection.class);
        String[] columnNames = getColumnNamesInTable(table);
        int n = table.getSize(), m = columnNames.length;
        
        // temp
        log.info("dim(dataMatrix) = " + n + " x " + m);

        String[][] dataMatrix = new String[n][m];
        String[] rowNames = new String[n];
        for( int i = 0; i < n; i++ )
        {
            RowDataElement row = table.getAt(i);
            rowNames[i] = row.getName();
            Object[] rowDataElement = row.getValues();
            for( int j = 0; j < m; j++ )
                dataMatrix[i][j] = (String)rowDataElement[j];
            
            // temp
            log.info("i = " + i+ " dataMatrix[i][0] = " + dataMatrix[i][0]);
        }
        return new Object[]{rowNames, columnNames, dataMatrix};
    }
    
    /***
     *
     * @param pathToTableWithDataMatrix
     * @param columnNames
     * @return Object[] array; array[0] = String[] rowNames; array[1] = double[][] dataMatrix;
     */
    public static Object[] readDataSubMatrix(DataElementPath pathToTableWithDataMatrix, String[] columnNames)
    {
        TableDataCollection table = pathToTableWithDataMatrix.getDataElement(TableDataCollection.class);
        return readDataSubMatrix(table, columnNames);
    }
    
    /***
     * 
     * @param table
     * @param columnNames
     * @return Object[] array; array[0] = String[] rowNames; array[1] = double[][] dataMatrix;
     */
    public static Object[] readDataSubMatrix(TableDataCollection table, String[] columnNames)
    {
        int n = table.getSize(), m = columnNames.length;
        double[][] dataMatrix = new double[n][m];
        for( int j = 0; j < m; j++ )
            if( columnNames[j].equals(LinearRegression.INTERCEPT) )
                for( int i = 0; i < n; i++ )
                    dataMatrix[i][j] = 1.0;
            else
            {
                double[] columnValues = readGivenColumnInDoubleTableAsArray(table, columnNames[j]);
                for( int i = 0; i < n; i++ )
                    dataMatrix[i][j] = columnValues[i];
            }
        //List<String> rowNameList = readRowNamesInTable(pathToTableWithDataMatrix);
        //String[] rowNames = rowNameList.toArray(new String[rowNameList.size()]);
        // String[] rowNames = table.names().toArray(String[]::new);
        String[] rowNames = readRowNamesInTable(table);
        return new Object[]{rowNames, dataMatrix};
    }

    /***
     * 
     * @return Object[] array; array[0] = String[] rowNames; array[1] = double[][] dataMatrix; array[2] = String[] stringColumnElements;
     */
    public static Object[] readDataSubMatrixAndStringColumn(DataElementPath pathToTableWithDataMatrix, String[] columnNamesForSubMatrix, String stringColumnName)
    {
        TableDataCollection table = pathToTableWithDataMatrix.getDataElement(TableDataCollection.class);
        String[] stringColumnElements = readGivenColumnInStringTable(table, stringColumnName);
        Object[] objects = readDataSubMatrix(table, columnNamesForSubMatrix);
        return new Object[]{(String[])objects[0], (double[][])objects[1], stringColumnElements};
    }
    
    public static String[] readGivenRowInStringTable(TableDataCollection table, String rowName) throws Exception
        {return StreamEx.of(table.get(rowName).getValues()).map(Object::toString).toArray(String[]::new);}
    
    /***
     * 
     * @param table
     * @param columnNames
     * @return Object[] array; array[0] = String[] rowNames; array[1] = String[][] dataMatrix;
     */
    public static Object[] readStringDataSubMatrix(DataElementPath pathToTable, String[] columnNames)
    {
        TableDataCollection table = pathToTable.getDataElement(TableDataCollection.class);
        int n = table.getSize(), m = columnNames.length;
        String[][] dataMatrix = new String[n][m];
        for( int j = 0; j < m; j++ )
        {
            String[] columnValues = readGivenColumnInStringTable(table, columnNames[j]);
            for( int i = 0; i < n; i++ )
                dataMatrix[i][j] = columnValues[i];
        }
        return new Object[]{readRowNamesInTable(table), dataMatrix};
    }

    // old version: to replace in future by new version
    public static TableDataCollection writeChartsIntoTable(Map<String, Chart> namesAndCharts, String nameOfColumn, DataElementPath dep) throws Exception
    {
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(dep);
        table.getColumnModel().addColumn(nameOfColumn, Chart.class);
        for( Entry<String, Chart> entry : namesAndCharts.entrySet() )
            TableDataCollectionUtils.addRow(table, entry.getKey(), new Object[]{entry.getValue()});
        table.finalizeAddition();
        CollectionFactoryUtils.save(table);
        return table;
    }
    
    // new version
    public static void writeChartsIntoTable(Map<String, Chart> namesAndCharts, DataElementPath pathToTable) throws Exception
    {
        for( Entry<String, Chart> entry : namesAndCharts.entrySet() )
            addChartToTable(entry.getKey(), entry.getValue(), pathToTable);
    }
    
    ///// it is copied
    public static void addChartToTable(String chartName, Chart chart, DataElementPath pathToTable)
    {
        // TableDataCollection table = pathToTable.getDataElement(TableDataCollection.class);
        TableDataCollection table = pathToTable.optDataElement(TableDataCollection.class);
        if( table == null )
        {
            table = TableDataCollectionUtils.createTableDataCollection(pathToTable);
            table.getColumnModel().addColumn(CHART, Chart.class);
        }
        TableDataCollectionUtils.addRow(table, chartName, new Object[]{chart});
        table.finalizeAddition();
        CollectionFactoryUtils.save(table);
    }

    public static Chart createChart(double[] xValues, double[] yValues, String xName, String yName, String curveName, Color color)
    {
        return createChart(xValues, yValues, null, null, null, null, xName, yName, curveName, color);
    }

    public static Chart createChart(double[] xValues, double[] yValues, Double xMin, Double xMax, Double yMin, Double yMax, String xName, String yName, String curveName, Color color)
    {
        Chart chart = new Chart();
        ChartOptions options = new ChartOptions();
        AxisOptions xAxis = new AxisOptions();
        xAxis.setLabel(xName);
        if( xMin != null )
            xAxis.setMin(xMin);
        if( xMax != null )
            xAxis.setMax(xMax);
        options.setXAxis(xAxis);
        AxisOptions yAxis = new AxisOptions();
        yAxis.setLabel(yName);
        if( yMin != null )
            yAxis.setMin(yMin);
        if( yMax != null )
            yAxis.setMax(yMax);
        options.setYAxis(yAxis);
        chart.setOptions(options);
        ChartSeries series = new ChartSeries(xValues, yValues);
        if( curveName != null )
            series.setLabel(curveName);
        series.setColor(color);
        
        // in order to draw the histogram
        /****
                    series.getLines().setShow(false);
                    series.getBars().setShow(true);
                    series.getBars().setWidth(1.0);
        *****/

        chart.addSeries(series);
        return chart;
    }

    public static Chart createChart1(Map<Integer, List<Double>> xValuesAndyValues, String xName, String yName, String curveName, Color color)
    {
        return createChart1(xValuesAndyValues, xName, yName, null, null, curveName, color);
    }

    public static Chart createChart1(Map<Integer, List<Double>> xValuesAndyValues, String xName, String yName, Integer xMin, Integer xMax,
            String curveName, Color color)
    {
        Predicate<Integer> inside = xMin == null || xMax == null ? x -> true : x -> x >= xMin && x <= xMax;
        double[] xValues = StreamEx.ofKeys(xValuesAndyValues).filter(inside).mapToDouble(x -> x).toArray();
        if( xValues.length == 0 )
            return null;
        double[] yValues = StreamEx.ofValues(xValuesAndyValues, inside)
                .mapToDouble(val -> DoubleStreamEx.of(val).average().orElse(Double.NaN)).toArray();
        return createChart(xValues, yValues, xName, yName, curveName, color);
    }

    // it is copied
    // Chart with curves and clouds
    public static Chart createChart(List<double[]> xValuesForCurves, List<double[]> yValuesForCurves, String[] curveNames, List<double[]> xValuesForClouds, List<double[]> yValuesForClouds, String[] cloudNames, Double xMin, Double xMax, Double yMin, Double yMax, String xName, String yName)
    {
        Chart chart = new Chart();
        ChartOptions options = new ChartOptions();
        AxisOptions xAxis = new AxisOptions();
        xAxis.setLabel(xName);
        if( xMin != null )
            xAxis.setMin(xMin);
        if( xMax != null )
            xAxis.setMax(xMax);
        options.setXAxis(xAxis);
        AxisOptions yAxis = new AxisOptions();
        yAxis.setLabel(yName);
        if( yMin != null )
            yAxis.setMin(yMin);
        if( yMax != null )
            yAxis.setMax(yMax);
        options.setYAxis(yAxis);
        chart.setOptions(options);
        if( xValuesForClouds != null )
            for( int iCloud = 0; iCloud < xValuesForClouds.size(); iCloud++ )
            {
                double[] x = xValuesForClouds.get(iCloud), y = yValuesForClouds.get(iCloud);
                ChartSeries series = new ChartSeries(x, y);
                if( ! cloudNames[iCloud].equals("") )
                    series.setLabel(cloudNames[iCloud]);
                Color color = ColorUtils.getDefaultColor(iCloud);
                series.setColor(color);
                series.getLines().setShow(false);
                chart.addSeries(series);
            }
        if( xValuesForCurves == null ) return chart;
        int iClouds = xValuesForClouds == null ? 0 : xValuesForClouds.size();
        for( int iCurve = 0; iCurve < xValuesForCurves.size(); iCurve++ )
        {
            double[] x = xValuesForCurves.get(iCurve), y = yValuesForCurves.get(iCurve);
            if( x.length <= 1) continue;
//          List<double[]> list = SiteModelsComparisonUtils.recalculateRocCurve(Arrays.asList(x, y));
            List<double[]> list = recalculateCurve(x, y);
            x = list.get(0);
            y = list.get(1);
            if( x.length <= 1) continue;
            ChartSeries series = new ChartSeries(x, y);
            if( ! curveNames[iCurve].equals("") )
                series.setLabel(curveNames[iCurve]);
            Color color = ColorUtils.getDefaultColor(iCurve + iClouds);
            series.setColor(color);
            chart.addSeries(series);
        }
        return chart;
    }
    
    // it is copied
    /***
     * aim of recalculation: to average y-values that correspond to same x-value
     * @param x
     * @param y
     * @return list of two arrays; 1-st array = new x-values; 2-nd array = new y-values;
     */
    public static List<double[]> recalculateCurve(double[] x, double[] y)
    {
        Map<Double, List<Double>> map = IntStreamEx.ofIndices(x).mapToEntry(i -> x[i], i -> y[i]).grouping(TreeMap::new);
        double x1[] = new double[map.size()], y1[] = new double[map.size()];
        int i = 0;
        for( Entry<Double, List<Double>> entry : map.entrySet() )
        {
            x1[i] = entry.getKey();
            y1[i++] = Stat.mean(entry.getValue());
        }
        return Arrays.asList(x1, y1);
    }

    /////////// it is copied !!!!!
    public static Chart createChart(double[] xValuesForCurve, double[] yValuesForCurve, String curveName, double[] xValuesForCloud, double[] yValuesForCloud, String cloudName, Double xMin, Double xMax, Double yMin, Double yMax, String xName, String yName)
    {
        List<double[]> xValuesForCurves = new ArrayList<>(), yValuesForCurves = new ArrayList<>();
        xValuesForCurves.add(xValuesForCurve);
        yValuesForCurves.add(yValuesForCurve);
        String[] curveNames = new String[]{""};
        if( curveName != null )
            curveNames[0] = curveName;
        List<double[]> xValuesForClouds = new ArrayList<>(), yValuesForClouds = new ArrayList<>();
        xValuesForClouds.add(xValuesForCloud);
        yValuesForClouds.add(yValuesForCloud);
        String[] cloudNames = new String[]{""};
        if( cloudName != null )
            cloudNames[0] = cloudName;
        if( xValuesForCloud == null )
            xValuesForClouds = null;
        return createChart(xValuesForCurves, yValuesForCurves, curveNames, xValuesForClouds, yValuesForClouds, cloudNames, xMin, xMax, yMin, yMax, xName, yName);
    }

    /***
     * dim(xAndYvalues) = nx2; xAndYvalues[0][[0] <= xAndYvalues[1][[0] <= ... <= xAndYvalues[n-1][[0]
     * output: double[] xValuesGrouped, double[] yValuesGrouped; dim(xValuesGrouped) = dim(xValuesGrouped) = numberOfGroups;
     * @param xAndYvalues
     * @param numberOfGroups
     * @param xValuesGrouped
     * @param yValuesGrouped
     */
    public static void getGroupedGraph(double[][] xAndYvalues, int numberOfGroups, double[] xValuesGrouped, double[] yValuesGrouped)
    {
        int n = xAndYvalues.length;

        // calculation of outputs xValuesGrouped and yValuesGrouped
        int groupSize = n / numberOfGroups, remainder = n - groupSize * numberOfGroups;
        for( int i = 0; i < numberOfGroups; i++ )
        {
            xValuesGrouped[i] = yValuesGrouped[i] = 0;
            int jj = groupSize;
            if( i == numberOfGroups - 1 )
                jj += remainder;
            for( int j = 0; j < jj; j++ )
            {
                xValuesGrouped[i] += xAndYvalues[i * groupSize + j][0];
                yValuesGrouped[i] += xAndYvalues[i * groupSize + j][1];
            }
            xValuesGrouped[i] /= jj;
            yValuesGrouped[i] /= jj;
        }
    }
    
    /********************* ParticularTable : start *************************/
    public static class ParticularTable
    {
        public static final String NAME_OF_TABLE_WITH_VARIABLE_NAMES = "variableNames";
        public static final String VARIABLE_NAMES = "Variable names";
        
        public static void writeTableWithVariableNames(String[] variableNames, DataElementPath pathToOutputs, String tableName)
        {
            String[] namesOfColumns = new String[variableNames.length];
            for( int i = 0; i < variableNames.length; i++ )
                namesOfColumns[i] = "Name of variable " + Integer.toString(i);
            writeStringTable(new String[][]{variableNames}, new String[]{VARIABLE_NAMES}, namesOfColumns, pathToOutputs.getChildPath(tableName));
        }

        public static String[] readVariableNames(DataElementPath pathToFolderWithSavedModel, String tableName) throws Exception
        {
            TableDataCollection table = pathToFolderWithSavedModel.getChildPath(tableName).getDataElement(TableDataCollection.class);
            return readGivenRowInStringTable(table, VARIABLE_NAMES);
        }
    }
    /********************* ParticularTable : finish ************************/
    
    /************************** FileUtils : start **************************/
    public static class FileUtils
    {
        /////// It is copied.
        public static final String NAN = "NaN";
        public static final String DOUBLE_TYPE = "Double";
        public static final String CHAR_TYPE = "Char";
        public static final String STRING_TYPE = "String";
        
        //// it is copied
        public static String[] readLinesInFile(DataElementPath pathToFile) throws IOException
        {
            List<String> fileLines = new ArrayList<>();
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
            return fileLines.toArray(new String[0]);
        }
        
        public static String[][] readTabDelimitedFile(DataElementPath pathToFile) throws IOException
        {
            String[] lines = readLinesInFile(pathToFile);
            String[][] result = new String[lines.length][];
            for( int i = 0; i < lines.length; i++ )
                result[i] = TextUtil.split(lines[i], '\t');
            return result;
        }
        
        // It is copied
        public static String[] getColumnNames(DataElementPath pathToFile) throws IOException
        {
            File file = pathToFile.getDataElement(FileDataElement.class).getFile();
            BufferedReader reader = ApplicationUtils.asciiReader(file);
            String line = reader.readLine();
            String[] tokens = TextUtil.split(line, '\t');
            return (String[])ArrayUtils.remove(tokens, 0);
        }
        
        // It is copied
        // If columnNames != null then read whole matrix else read corresponding submatrix.
        // matrixType (- {"Double", "Char", "String"}
        public static Object[] readMatrixOrSubmatix(DataElementPath pathToFile, String[] columnNames, String matrixType) throws IOException
        {
            // 1. Read 'fileLines' and calculate 'columnNamesOutput'.
            String[] fileLines = readLinesInFile(pathToFile);
            String[] columnNamesInMatrix = TextUtil.split(fileLines[0], '\t');
            if( columnNamesInMatrix.length < 2 ) return null;
            if( fileLines.length < 2 ) return null;
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
            
            // 3. Calculate dataMatrix and rowNames.
            String[] rowNames = new String[fileLines.length - 1];
            double[][] dataMatrixDouble = matrixType.equals(DOUBLE_TYPE) ? new double[fileLines.length - 1][] : null;
            char[][] dataMatrixChar = matrixType.equals(CHAR_TYPE) ? new char[fileLines.length - 1][] : null;
            String[][] dataMatrixString = matrixType.equals(STRING_TYPE) ? new String[fileLines.length - 1][] : null;
            for( int i = 0; i < rowNames.length; i++ )
            {
                String[] tokens = TextUtil.split(fileLines[i + 1], '\t');
                if( tokens.length < 1 + columnNamesOutput.length ) return null;
                rowNames[i] = tokens[0];
                switch( matrixType )
                {
                    case DOUBLE_TYPE : double[] rowDouble = new double[columnNamesOutput.length];
                                       for( int j = 0; j < indices.length; j++ )
                                           rowDouble[j] = tokens[indices[j]].equals(NAN) ? Double.NaN : Double.parseDouble(tokens[indices[j]]);
                                       dataMatrixDouble[i] = rowDouble; break;
                    case CHAR_TYPE   : char[] rowChar = new char[columnNamesOutput.length];
                                       for( int j = 0; j < indices.length; j++ )
                                           rowChar[j] = tokens[indices[j]].charAt(0);
                                       dataMatrixChar[i] = rowChar; break;
                    case STRING_TYPE : String[] rowString = new String[columnNamesOutput.length];
                                       for( int j = 0; j < indices.length; j++ )
                                           rowString[j] = tokens[indices[j]];
                                       dataMatrixString[i] = rowString; break;
                    default          : return null;
                }
            }
            switch( matrixType )
            {
                case DOUBLE_TYPE : return new Object[]{rowNames, columnNamesOutput, dataMatrixDouble};
                case CHAR_TYPE   : return new Object[]{rowNames, columnNamesOutput, dataMatrixChar};
                case STRING_TYPE : return new Object[]{rowNames, columnNamesOutput, dataMatrixString};
            }
            return null;
        }
        
        public static void writeDoubleMatrixToFile(String[] rowNames, String[] columnNames, double[][] dataMatrix, DataElementPath pathToFile, Logger log) throws Exception
        {
            File file = TempFiles.file("");
            try( BufferedWriter bw = new BufferedWriter(new FileWriter(file)) )
            {
                bw.write("ID\t" + convertStringArrayToTabDelimitedLine(columnNames));
                bw.close();
            }
            catch( Exception e )
            {
                throw e;
            }
            try( BufferedWriter bw = new BufferedWriter(new FileWriter(file, true)) )
            {
                for( int i = 0; i < dataMatrix.length; i++ )
                {
                    String[] rowElements = new String[columnNames.length];
                    // rowElements[0] = rowNames[i];
                    for( int j = 0; j < columnNames.length; j++ )
                        rowElements[j] = Double.isNaN( dataMatrix[i][j] ) ? NAN : Double.toString( dataMatrix[i][j] );
                    bw.write("\n" + rowNames[i] + "\t" + convertStringArrayToTabDelimitedLine(rowElements));
                }
                bw.close();
            }
            catch( Exception e )
            {
                throw e;
            }
            FileImporter importer = new FileImporter();
            importer.doImport(pathToFile.getParentCollection(), file, pathToFile.getName(), null, log);
        }
        
        public static void writeCharMatrixToFile(String[] rowNames, String[] columnNames, char[][] dataMatrix, DataElementPath pathToFile, Logger log) throws Exception
        {
            File file = TempFiles.file("");
            try( BufferedWriter bw = new BufferedWriter(new FileWriter(file)) )
            {
                bw.write("ID\t" + convertStringArrayToTabDelimitedLine(columnNames));
                bw.close();
            }
            catch( Exception e )
            {
                throw e;
            }
            try( BufferedWriter bw = new BufferedWriter(new FileWriter(file, true)) )
            {
                for( int i = 0; i < dataMatrix.length; i++ )
                {
                    String[] rowElements = new String[columnNames.length];
                    for( int j = 0; j < columnNames.length; j++ )
                        rowElements[j] = new String(new char[]{dataMatrix[i][j]});
                    bw.write("\n" + rowNames[i] + "\t" + convertStringArrayToTabDelimitedLine(rowElements));
                }
                bw.close();
            }
            catch(Exception e)
            {
                throw e;
            }
            FileImporter importer = new FileImporter();
            importer.doImport(pathToFile.getParentCollection(), file, pathToFile.getName(), null, log);
        }
        
        //// It is copied
        public static void writeDoubleAndStringMatricesToFile(String[] rowNames, String[] columnNames, double[][] dataMatrix, String[] rowNamesString, String[] columnNamesString, String[][] dataMatrixString, DataElementPath pathToFile, Logger log) throws Exception
        {
            if( ! MatrixUtils.equals(rowNames, rowNamesString) ) return;
            File file = TempFiles.file("");
            try( BufferedWriter bw = new BufferedWriter(new FileWriter(file)))
            {
                String[] columnNamesAll = (String[])ArrayUtils.addAll(columnNames, columnNamesString);
                bw.write("ID\t" + convertStringArrayToTabDelimitedLine(columnNamesAll));
                bw.close();
            }
            catch(Exception e)
            {
                throw e;
            }
            try( BufferedWriter bw = new BufferedWriter(new FileWriter(file, true)))
            {
                for( int i = 0; i < dataMatrix.length; i++ )
                {
                    String[] rowElements = new String[columnNames.length];
                    for( int j = 0; j < columnNames.length; j++ )
                        rowElements[j] = Double.isNaN(dataMatrix[i][j]) ? NAN : Double.toString(dataMatrix[i][j]);
                    rowElements = (String[])ArrayUtils.addAll(rowElements, dataMatrixString[i]);
                    bw.write("\n" + rowNames[i] + "\t" + convertStringArrayToTabDelimitedLine(rowElements));
                }
                bw.close();
            }
            catch(Exception e)
            {
                throw e;
            }
            FileImporter importer = new FileImporter();
            importer.doImport(pathToFile.getParentCollection(), file, pathToFile.getName(), null, log);
        }

        //// it is copied
        private static String convertStringArrayToTabDelimitedLine(String[] array)
        {
            String result = "";
            for( int i = 0; i < array.length; i++ )
            {
                if( i > 0 )
                    result += "\t";
                result += array[i];
            }
            return result;
        }
    }
    /************************ FileUtils : finish ***************************/
    
    private static Logger log = Logger.getLogger(TableUtils.class.getName());
}