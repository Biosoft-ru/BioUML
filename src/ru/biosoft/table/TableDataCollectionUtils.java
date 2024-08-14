package ru.biosoft.table;

import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.apache.commons.lang.ArrayUtils;

import com.developmentontheedge.beans.swing.PropertyInspector;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementCreateException;
import ru.biosoft.access.core.DataElementGetException;
import ru.biosoft.access.core.DataElementNotFoundException;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementReadException;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.access.core.SortableDataCollection;
import ru.biosoft.access.core.filter.FilteredDataCollection;
import ru.biosoft.access.security.Permission;
import ru.biosoft.access.support.DataCollectionRowModelAdapter;
import ru.biosoft.analysis.aggregate.NumericAggregator;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.table.exception.TableNoColumnException;

/**
 *
 */
public abstract class TableDataCollectionUtils
{
    protected static final Logger log = Logger.getLogger(TableDataCollectionUtils.class.getName());
    private static final Pattern IDS_SPLIT_PATTERN = Pattern.compile(",\\s*");
    public static final char ID_SEPARATOR_CHAR = ',';
    public static final String ID_SEPARATOR = ID_SEPARATOR_CHAR+"";

    // Order or classes is important
    private static final List<Class<? extends TableDataCollection>> TABLE_IMPLEMENTATION_CLASSES = Arrays.asList(
        StandardTableDataCollection.class, FileTableDataCollection.class, SqlTableDataCollection.class);

    public static final int INNER_JOIN = 2; //SQL join
    public static final int LEFT_SUBSTRACTION = 4; //Contains only elements from left table
    public static final int RIGHT_SUBSTRACTION = 1; // Contains only elements from right table
    public static final int LEFT_JOIN = INNER_JOIN | LEFT_SUBSTRACTION; // SQL left join
    public static final int RIGHT_JOIN = INNER_JOIN | RIGHT_SUBSTRACTION; // SQL right join
    public static final int OUTER_JOIN = INNER_JOIN | LEFT_SUBSTRACTION | RIGHT_SUBSTRACTION; // SQL full join
    public static final int SYMMETRIC_DIFFERENCE = LEFT_SUBSTRACTION | RIGHT_SUBSTRACTION; // Contains elements which belongs only to one table

    public static @Nonnull TableDataCollection createTableDataCollection(DataElementPath path)
    {
        return createTableDataCollection(path.getParentCollection(), path.getName());
    }

    public static @Nonnull TableDataCollection createTableDataCollection(@Nonnull DataCollection<?> parent, @Nonnull String name)
    {
        Properties properties = new Properties();
        properties.put(DataCollectionConfigConstants.NAME_PROPERTY, name);
        properties.put(DataCollectionConfigConstants.PLUGINS_PROPERTY, "ru.biosoft.table");
        if( parent.contains(name) )
        {
            try
            {
                parent.remove(name);
            }
            catch( Exception e )
            {
                ExceptionRegistry.log(e);
            }
        }
        for(Class<? extends TableDataCollection> clazz : TABLE_IMPLEMENTATION_CLASSES)
        {
            if(parent.isAcceptable( clazz ))
            {
                properties.put( DataCollectionConfigConstants.CLASS_PROPERTY, clazz.getName() );
                break;
            }
        }
        if(!properties.containsKey( DataCollectionConfigConstants.CLASS_PROPERTY ))
            throw new DataElementCreateException( DataElementPath.create(parent, name), TableDataCollection.class );
        return (TableDataCollection)CollectionFactory.createCollection(parent, properties);
    }

    public static TableDataCollection join(int joinType, TableDataCollection t1, TableDataCollection t2, DataElementPath output)
    {
        return join(joinType, t1, t2, output, getColumnNames(t1), getColumnNames(t2));
    }

    public static TableDataCollection join(int joinType, TableDataCollection t1, TableDataCollection t2, DataElementPath output,
            String[] t1Columns, String[] t2Columns)
    {
        return join(joinType, t1, t2, output, t1Columns, t2Columns, t1Columns, t2Columns);
    }

    public static TableDataCollection join(int joinType, TableDataCollection t1, TableDataCollection t2, DataElementPath output,
            String[] t1Columns, String[] t2Columns, String[] t1NewColumns, String[] t2NewColumns)
    {
        return join(joinType, t1, t2, output, t1Columns, t2Columns, t1NewColumns, t2NewColumns, new String[0], new String[0], new String[0], null);
    }

    /**
     * Join two tables
     * @param joinType - type of join - inner, outer, left, right etc.
     * @param t1 - source table 1
     * @param t2 - source table 2
     * @param output - output path
     * @param t1Columns - unique columns of table 1
     * @param t2Columns - unique columns of table 2
     * @param t1NewColumns - new names for unique columns of table 1
     * @param t2NewColumns - new names for unique columns of table 2
     * @param commonColumns - new columns, common for both tables
     * @param commonColumns1 - old columns from table 1, that will be mapped into common columns
     * @param commonColumns2 - old columns from table 2, that will be mapped into common columns
     * @param aggregator - NumericAggregator, if null, simple inner join is used, otherwise aggregator merges values for common numerical columns
     *                     if aggregator==null and common columns are specified, values from the table 1 will be taken as commons
     * @return
     */
    public static TableDataCollection join(int joinType, TableDataCollection t1, TableDataCollection t2, DataElementPath output,
            String[] t1Columns, String[] t2Columns, String[] t1NewColumns, String[] t2NewColumns, String[] commonColumns,
            String[] commonColumns1, String[] commonColumns2, NumericAggregator aggregator)
    {
        TableDataCollection result = null;
        if( output == null )
        {
            result = new StandardTableDataCollection(null, t1.getName() + t2.getName() + "tempJoin");
        }
        else
        {
            try
            {
                DataCollection<?> parent = output.getParentCollection();
                String name = output.getName();
                if( parent.contains(name) )
                {
                    log.warning("Old table '" + output.toString() + "' was removed");
                    parent.remove(name);
                }
                result = createTableDataCollection(parent, name);
                ReferenceTypeRegistry.copyCollectionReferenceType(result, t1);
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Can not init output element", e);
                throw new RuntimeException("While creating table " + output + ": " + e);
            }
        }

        String[] t1AllColumns;
        String[] t2AllColumns;
        String[] t1AllNewColumns;
        if( commonColumns.length == 0 )
        {
            t1AllColumns = t1Columns;
            t2AllColumns = t2Columns;
            t1AllNewColumns = t1NewColumns;
        }
        else
        {
            t1AllColumns = new String[t1Columns.length + commonColumns.length];
            System.arraycopy(t1Columns, 0, t1AllColumns, 0, t1Columns.length);
            System.arraycopy(commonColumns1, 0, t1AllColumns, t1Columns.length, commonColumns.length);

            t2AllColumns = new String[t2Columns.length + commonColumns.length];
            System.arraycopy(commonColumns2, 0, t2AllColumns, 0, commonColumns.length);
            System.arraycopy(t2Columns, 0, t2AllColumns, commonColumns.length, t2Columns.length);

            t1AllNewColumns = new String[t1AllColumns.length + commonColumns.length];
            System.arraycopy(t1AllColumns, 0, t1AllNewColumns, 0, t1AllColumns.length);
            System.arraycopy(commonColumns, 0, t1AllNewColumns, t1AllColumns.length, commonColumns.length);
        }

        addColumns(result, t1, t1AllColumns, t1AllNewColumns);
        addColumns(result, t2, t2Columns, t2NewColumns);

        if( isEmpty(t1) && isEmpty(t2) )
        {
            return result;
        }

        // Trivial case: second table is empty. Making right join only from
        // first table`s data
        if( isEmpty(t2) && ( joinType & LEFT_JOIN ) == LEFT_JOIN )
        {
            return makeTrivialLeftJoin(result, t2Columns.length, getKeys(t1), t1, t1AllColumns);
        }

        // Trivial case: first table is empty. Making right join only from
        // second table`s data
        if( isEmpty(t1) && ( joinType & RIGHT_JOIN ) == RIGHT_JOIN )
        {
            return makeTrivialRightJoin(result, t1Columns.length, getKeys(t2), t2, t2AllColumns);
        }

        String[] t1Keys = getKeys(t1);
        String[] t2Keys = getKeys(t2);

        int i = 0;
        int j = 0;

        while( i < t1Keys.length && j < t2Keys.length )
        {
            Object[] rowBuffer = new Object[t1Columns.length + t2Columns.length + commonColumns.length];

            setBufferToNulls(rowBuffer);

            String left = t1Keys[i];
            String right = t2Keys[j];
            String key = null;

            if( left.compareTo(right) < 0 )
            {
                if( ( joinType & LEFT_SUBSTRACTION ) == LEFT_SUBSTRACTION )
                {
                    key = left;
                    setLeftJoinData(rowBuffer, key, t1, t1AllColumns);
                }

                i++;
            }
            else if( left.compareTo(right) > 0 )
            {
                if( ( joinType & RIGHT_SUBSTRACTION ) == RIGHT_SUBSTRACTION )
                {
                    key = right;
                    setRightJoinData(rowBuffer, key, t2, t2AllColumns, t1Columns.length);
                }

                j++;
            }
            else
            // (left == right)
            {
                if( ( joinType & INNER_JOIN ) == INNER_JOIN )
                {
                    key = left;

                    if( aggregator == null )
                        if(commonColumns.length == 0)
                            setInnerJoinData(rowBuffer, key, t1, t1Columns, t2, t2Columns);
                        else //this case normally should not happen
                            setInnerJoinData(rowBuffer, key, t1, t1AllColumns, t2, t2Columns);
                    else
                    {

                        setLeftJoinData(rowBuffer, key, t1, t1Columns);
                        setRightJoinData(rowBuffer, key, t2, t2Columns, t1AllColumns.length);
                        setMergeJoinData(rowBuffer, key, t1, t2, commonColumns1, commonColumns2, t1Columns.length, aggregator);
                    }
                }
                i++;
                j++;
            }

            if( key != null )
            {
                addRow(result, key, rowBuffer);
            }
        }

        while( i < t1Keys.length )
        {
            Object[] rowBuffer = new Object[t1Columns.length + t2Columns.length + commonColumns.length];

            String key = t1Keys[i++];

            if( ( joinType & LEFT_SUBSTRACTION ) == LEFT_SUBSTRACTION )
            {
                setBufferToNulls(rowBuffer);
                setLeftJoinData(rowBuffer, key, t1, t1AllColumns);
                addRow(result, key, rowBuffer);
            }
        }

        while( j < t2Keys.length )
        {
            Object[] rowBuffer = new Object[t1Columns.length + t2Columns.length + commonColumns.length];

            String key = t2Keys[j++];

            if( ( joinType & RIGHT_SUBSTRACTION ) == RIGHT_SUBSTRACTION )
            {
                setBufferToNulls(rowBuffer);
                setRightJoinData(rowBuffer, key, t2, t2AllColumns, t1Columns.length);
                addRow(result, key, rowBuffer);
            }
        }

        return result;
    }

    private static void setMergeJoinData(Object[] rowBuffer, String key, TableDataCollection t1, TableDataCollection t2,
            String[] commonColumns1, String[] commonColumns2, int length, NumericAggregator aggreagator)
    {
        ColumnModel cm1 = t1.getColumnModel();
        ColumnModel cm2 = t2.getColumnModel();
        Object[] values1;
        Object[] values2;
        try
        {
            values1 = ( t1.get(key) ).getValues();
            values2 = ( t2.get(key) ).getValues();
        }
        catch( Exception e )
        {
            return;
        }
        for(int i=0; i<commonColumns1.length; i++)
        {
            String name1 = commonColumns1[i];
            String name2 = commonColumns2[i];
            TableColumn column = cm1.getColumn(name1);

            Object value1 = values1[cm1.getColumnIndex(name1)];
            Object value2 = values2[cm2.getColumnIndex(name2)];
            if(column.getValueClass() == String.class)
            {
                Set<String> result = new TreeSet<>();

                if(value1 instanceof String && !( (String)value1 ).isEmpty())
                    result.add(value1.toString());
                if(value2 instanceof String && !( (String)value2 ).isEmpty())
                    result.add(value2.toString());

                rowBuffer[length+i] = String.join(",", result);
                continue;
            }
            if(column.getValueClass() == StringSet.class)
            {
                Set<String> result = new TreeSet<>();
                if( value1 instanceof StringSet )
                    result.addAll((StringSet)value1);
                if( value2 instanceof StringSet )
                    result.addAll((StringSet)value2);
                rowBuffer[length + i] = new StringSet( result );
                continue;
            }
            if( column.getType().isNumeric() )
            {
                double[] colValues = new double[2];
                if( ! ( value1 instanceof Number ) )
                    colValues[0] = Double.NaN;
                else
                    colValues[0] = ( (Number) ( value1 ) ).doubleValue();
                if( ! ( value2 instanceof Number ) )
                    colValues[1] = Double.NaN;
                else
                    colValues[1] = ( (Number) ( value2 ) ).doubleValue();

                double result = aggreagator.aggregate(colValues);
                rowBuffer[length + i] = column.getType().convertValue(result);
                continue;
            }
        }
    }

    protected static boolean isEmpty(TableDataCollection result)
    {
        return result == null || result.isEmpty();
    }

    /**
     * Get column names as string array
     * @param table table
     */
    public static String[] getColumnNames(TableDataCollection table)
    {
        return table.columns().map( TableColumn::getName ).toArray( String[]::new );
    }

    public static DoubleSummaryStatistics findMinMax(TableDataCollection tableData)
    {
        return StreamEx.of( tableData.stream() ).flatMap( row -> Arrays.stream( row.getValues() ) )
                .select( Number.class ).mapToDouble( Number::doubleValue )
                .remove( Double::isNaN ).remove( Double::isInfinite ).summaryStatistics();
    }

    public static DoubleSummaryStatistics findMinMax(TableDataCollection tableData, String[] columns)
    {
        ColumnModel cm = tableData.getColumnModel();
        int[] indexes = StreamEx.of(columns).mapToInt(cm::optColumnIndex).atLeast( 0 ).toArray();
        return StreamEx.of( tableData.stream() ).map( RowDataElement::getValues )
                .flatMap( rowData -> IntStreamEx.of( indexes ).elements( rowData ) )
                .select( Number.class ).mapToDouble( Number::doubleValue )
                .remove( Double::isNaN ).remove( Double::isInfinite ).summaryStatistics();
    }

    private static TableDataCollection makeTrivialLeftJoin(TableDataCollection result, int t2Length, String[] t1Keys,
            TableDataCollection t1, String[] t1Columns)
    {
        //        Object[] rowBuffer = new Object[t1Columns.length + t2Length];
        for( String key : t1Keys )
        {
            Object[] rowBuffer = new Object[t1Columns.length + t2Length];
            setBufferToNulls(rowBuffer);

            setLeftJoinData(rowBuffer, key, t1, t1Columns);
            addRow(result, key, rowBuffer);
        }

        return result;
    }

    private static TableDataCollection makeTrivialRightJoin(TableDataCollection result, int t1Length, String[] t2Keys,
            TableDataCollection t2, String[] t2Columns)
    {
        Object[] rowBuffer = new Object[t1Length + t2Columns.length];
        for( String key : t2Keys )
        {
            setBufferToNulls(rowBuffer);

            setRightJoinData(rowBuffer, key, t2, t2Columns, t1Length);
            addRow(result, key, rowBuffer);
        }

        return result;
    }

    private static void setBufferToNulls(Object[] rowBuffer)
    {
        Arrays.fill( rowBuffer, null );
    }

    private static void setLeftJoinData(Object[] rowBuffer, String key, TableDataCollection t1, String[] t1Columns)
    {
        Object[] leftRowBuffer = getRowValues(t1, key, t1Columns);
        System.arraycopy(leftRowBuffer, 0, rowBuffer, 0, t1Columns.length);
    }

    private static void setRightJoinData(Object[] rowBuffer, String key, TableDataCollection t2, String[] t2Columns, int t1ColumnCount)
    {
        Object[] rightRowBuffer = getRowValues(t2, key, t2Columns);
        System.arraycopy(rightRowBuffer, 0, rowBuffer, t1ColumnCount, t2Columns.length);
    }

    private static void setInnerJoinData(Object[] rowBuffer, String key, TableDataCollection t1, String[] t1Columns,
            TableDataCollection t2, String[] t2Columns)
    {
        Object[] leftRowBuffer = getRowValues(t1, key, t1Columns);
        System.arraycopy(leftRowBuffer, 0, rowBuffer, 0, t1Columns.length);

        Object[] rightRowBuffer = getRowValues(t2, key, t2Columns);
        System.arraycopy(rightRowBuffer, 0, rowBuffer, t1Columns.length, t2Columns.length);
    }

    public static void addColumns(TableDataCollection t, TableDataCollection from, String[] columns, String[] newColumns)
    {
        for( int i = 0; i < columns.length; i++ )
        {
            TableColumn col = from.getColumnModel().getColumn(columns[i]);
            String columnName = t.getColumnModel().generateUniqueColumnName(newColumns[i]);
            t.getColumnModel().addColumn(t.getColumnModel().cloneTableColumn(col, columnName));
        }
    }

    private static String[] getKeys(TableDataCollection t)
    {
        return t.names().sorted().toArray( String[]::new );
    }

    public static String[] getKeysUnsorted(TableDataCollection t)
    {
        return t.names().toArray( String[]::new );
    }

    public static String getStringDescription(int... indices)
    {
        if( indices == null )
            return "";
        return IntStreamEx.of( indices ).map( i -> i + 1 ).boxed().distinct().sorted()
                .<String>intervalMap( (i, j) -> j - i == 1, (i, j) -> i == j ? i.toString() : j - i == 1 ? i + "," + j : i + "-" + j )
                .joining( "," );
    }

    /**
     *  Analyzing string: splitting int by "\n" , ",\n" and "," ;
     *  @return indices of columns which are presents in text area by their Name, SampleGroup , index or indices interval
     *  for example: "1-3,3,7,Column5,Group1" will be transformed to {1,2,3,5,...(columns from Group1)}
     */
    protected static Map<Integer, String> parseStringToColumnsMap(String input, TableDataCollection experiment) throws Exception
    {
        if( experiment == null )
            throw new Exception("No experiment found");

        Map<Integer, String> columns = new TreeMap<>();

        String[] line = input.trim().replaceAll(";", ",").replaceAll(",\n", ",").replaceAll("\t", "").replaceAll("\n", ",").split(",");

        if( line == null )
            throw new Exception("Unknown columns or groups");

        for( String str : line )
        {
            str = str.trim();

            if( str.isEmpty() )
                continue;

            //Check if it is name of group;
            if( experiment.getGroups().contains(str) )
            {
                String pattern = experiment.getGroups().get(str).getPattern();
                columns.putAll(parseStringToColumnsMap(pattern, experiment));
                continue;
            }

            // Check if it is name of column
            if( experiment.getColumnModel().hasColumn(str) )
            {
                for( int j = 0; j < experiment.getColumnModel().getColumnCount(); j++ )
                {
                    if( experiment.getColumnModel().getColumn(j).getName().equalsIgnoreCase(str) )
                        columns.put(j, str);
                }
                continue;
            }

            //Check if it is column index
            try
            {
                int j = Integer.parseInt(str) - 1;
                columns.put(j, experiment.getColumnModel().getColumn(j).getName());
                continue;
            }
            catch( Exception ex )
            {
            }

            //If string has type "i-j" replace it by string i,i+1,...,j
            try
            {
                if( str.contains("-") )
                {
                    int columnFrom = Integer.parseInt(str.substring(0, str.indexOf("-")));
                    int columnTo = Integer.parseInt(str.substring(str.indexOf("-") + 1));
                    for( int j = columnFrom - 1; j < columnTo; j++ )
                        columns.put(j, experiment.getColumnModel().getColumn(j).getName());
                }
            }
            catch( Exception ex )
            {
            }
        }

        if( columns.size() == 0 )
            throw new Exception("Unknown columns or groups");

        return columns;
    }

    public static String[] parseStringToColumnNames(String columns, TableDataCollection table) throws Exception
    {
        Map<Integer, String> map = parseStringToColumnsMap(columns, table);
        return map.values().toArray(new String[map.size()]);
    }

    public static int[] parseStringToColumnIndices(String columns, TableDataCollection table) throws Exception
    {
        Map<Integer, String> map = parseStringToColumnsMap(columns, table);
        int[] indices = new int[map.size()];
        int i = 0;
        Iterator<Integer> iter = map.keySet().iterator();
        while( iter.hasNext() )
        {
            indices[i++] = iter.next();
        }
        return indices;
    }

    /**
     * Get array of column indexes by array of column names
     * @param table table
     * @param columnNames array of column names
     */
    public static int[] getColumnIndexes(TableDataCollection table, String[] columnNames) throws TableNoColumnException
    {
        return StreamEx.of(columnNames).mapToInt( table.getColumnModel()::getColumnIndex ).toArray();
    }

    /**
     * Get row values as array of {@link Object}
     * @param table table
     * @param rowName row name
     * @throws BiosoftRepositoryException if row is not found or cannot be obtained for some other reason
     */
    public static @Nonnull Object[] getRowValues(TableDataCollection table, String rowName) throws RepositoryException
    {
        try
        {
            RowDataElement de = table.get(rowName);
            if( de == null )
                throw new DataElementNotFoundException(table.getCompletePath().getChildPath(rowName));
            return de.getValues();
        }
        catch( Throwable t )
        {
            throw new DataElementGetException(t, table.getCompletePath().getChildPath(rowName));
        }
    }

    /**
     * Get table row as array of {@link Object} with selected columns
     * @param table table
     * @param id row index
     * @param columnNames name of columns
     * @throws BiosoftRepositoryException if row is not found or cannot be obtained for some other reason
     */
    public static @Nonnull Object[] getRowValues(TableDataCollection table, String id, String[] columnNames) throws RepositoryException
    {
        Object[] rowValues = getRowValues(table, id);
        return StreamEx.of(columnNames).mapToInt( table.getColumnModel()::getColumnIndex ).elements( rowValues ).toArray();
    }

    public static void addRow(TableDataCollection table, String key, @Nonnull Object[] rowValues)
    {
        addRow(table, key, rowValues, false);
    }

    /**
     * Create new row by name and values array
     * @param table table
     * @param key unique row name
     * @param rowValues array of row values
     */
    public static void addRow(TableDataCollection table, String key, @Nonnull Object[] rowValues, boolean isBatch) throws DataElementCreateException
    {
        RowDataElement rde = new RowDataElement(key, table);
        rde.setValues(rowValues);
        try
        {
            table.addRow(rde);
            if( !isBatch )
                table.finalizeAddition();
        }
        catch( Exception e )
        {
            throw new DataElementCreateException(e, table.getCompletePath().getChildPath(key), RowDataElement.class);
        }

    }

    /**
     * Get table as double matrix with equal row lengths
     * @param table table
     */
    public static double[][] getMatrix(TableDataCollection table)
    {
        int[] indices = IntStreamEx.range( table.getColumnModel().getColumnCount() ).toArray();
        return getMatrix(table, indices, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    public static double[][] getMatrix(TableDataCollection table, int[] columnIndices)
    {
        return getMatrix(table, columnIndices, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    /**
     * Gets matrix with equal number of elements in each row, if element is missing or out of boundaries it is replaced by NaN
     * @param table
     * @param columnIndices
     * @param lowBoundary
     * @param highBoundary
     * @return
     */
    public static double[][] getMatrix(TableDataCollection table, int[] columnIndices, double lowerBoundary, double upperBoundary)
    {
        int n = table.getSize();
        double[][] result = new double[n][];
        for( int i = 0; i < n; i++ )
        {
            result[i] = getDoubleRow(table, columnIndices, table.getName(i), lowerBoundary, upperBoundary);
        }
        return result;
    }

    /**
     * Util method to create table data collection containing data from given matrix. May be used in tests (no origin is required)
     */
    public static TableDataCollection createTable(String name, double[][] data, String[] columnNames)
    {
        String[] autoRowNames = IntStreamEx.range(data.length).mapToObj(String::valueOf).toArray(String[]::new);
        return createTable(name, data, columnNames, autoRowNames);
    }

    public static TableDataCollection createTable(String name, double[][] data, String[] columnNames, String[] rowNames)
    {
        if (data[0].length != columnNames.length)
            throw new IllegalArgumentException("Numerical data and column names dimensions do not agree");

        if (data.length != rowNames.length)
            throw new IllegalArgumentException("Numerical data and row names dimensions do not agree");

        TableDataCollection result = new StandardTableDataCollection(null, name);
        for (String columnName: columnNames)
            result.getColumnModel().addColumn(columnName, DataType.Float);

        for( int i = 0; i < data.length; i++ )
            addRow(result, rowNames[i], ArrayUtils.toObject(data[i]));

        return result;
    }


    /**
     * Get selected table columns as double matrix with unequal row lengths
     * @param table table
     * @param columnIndices column indexes
     * @param threshold - threshold for outlines handling
     * @param handling - missed values and outlines handling method
     */
    public static double[][] getComplicatedMatrix(TableDataCollection table, int[] columnIndices, double lowerBoundary, double upperBoundary)
    {
        int n = table.getSize();
        double[][] result = new double[n][];
        for( int i = 0; i < n; i++ )
        {
            result[i] = getComplicatedDoubleRow(table, columnIndices, table.getName(i), lowerBoundary, upperBoundary);
        }
        return result;
    }


    /**
     * Get selected table row as double array
     * @param table table
     * @param columnIndices column indexes
     * @param rowName name
     */
    public static double[] getComplicatedDoubleRow(TableDataCollection table, int[] columnIndices, String rowName, double lowerBoundary,
            double upperBoundary)
    {
        Object[] objs;
        try
        {
            objs = ( table.get(rowName) ).getValues();
        }
        catch( Exception ex )
        {
            return null;
        }
        TDoubleList rowValues = new TDoubleArrayList();
        for( int columnIndex : columnIndices )
        {
            double value;
            try
            {
                value = Double.parseDouble( ( objs[columnIndex].toString() ));
                if( Math.abs(value) <= lowerBoundary || Math.abs(value) >= upperBoundary )
                    continue;
            }
            catch( Exception e )
            {
                continue;
            }
            rowValues.add(value);
        }
        return rowValues.toArray();
    }

    public static double[] getDoubleRow(TableDataCollection table, String rowName)
    {
        Object[] objs;
        try
        {
            objs = ( table.get(rowName) ).getValues();
        }
        catch( Exception ex )
        {
            return null;
        }

        return StreamEx.of( objs ).map( Object::toString ).mapToDouble( Double::parseDouble ).toArray();
    }

    public static double[] getDoubleRow(TableDataCollection table, int[] columnIndices, String rowName)
    {
        return getDoubleRow(table, columnIndices, rowName, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    public static double[] getDoubleRow(TableDataCollection table, int[] columnIndices, String rowName, double lowerBoundary,
            double upperBoundary)
    {
        int n = columnIndices.length;
        double[] result = new double[n];
        Object[] objs;
        try
        {
            objs = ( table.get(rowName) ).getValues();
        }
        catch( Exception ex )
        {
            return null;
        }
        for( int j = 0; j < n; j++ )
        {
            double value;
            try
            {
                value = Double.parseDouble( ( objs[columnIndices[j]].toString() ));
                if( Math.abs(value) <= lowerBoundary || Math.abs(value) >= upperBoundary )
                    value = Double.NaN;
            }

            catch( Exception e )
            {
                value = Double.NaN;
            }
            result[j] = value;
        }
        return result;
    }

    /**
     * Get table column as double array
     * @param table table
     * @param columnName column name
     */
    public static @Nonnull double[] getColumn(TableDataCollection table, String columnName) throws RepositoryException
    {
        double[] result = new double[table.getSize()];
        int idx = table.getColumnModel().getColumnIndex(columnName);
        for( int i = 0; i < table.getSize(); i++ )
        {
            RowDataElement row = table.getAt(i);
            Object value;
            try
            {
                value = DataType.Float.convertValue(row.getValues()[idx]);
            }
            catch( Exception e )
            {
                throw new DataElementReadException(e, row, columnName);
            }
            if(!(value instanceof Double))
                throw new DataElementReadException(row, columnName);
            result[i] = (Double)value;
        }
        return result;
    }

    public static @Nonnull Object[] getColumnObjects(TableDataCollection table, String columnName)
    {
        Object[] result = new Object[table.getSize()];
        int idx = table.getColumnModel().getColumnIndex(columnName);
        for( int i = 0; i < table.getSize(); i++ )
            result[i] = table.getValueAt(i, idx);
        return result;
    }

    public static boolean isNumericalColumn(TableColumn column)
    {
        return column != null && column.getType().isNumeric();
    }

    public static void setSortOrder(TableDataCollection table, String columnName, boolean ascending)
    {
        int sortColumnIndex = table.getColumnModel().optColumnIndex(columnName);
        if(sortColumnIndex >= 0)
        {
            table.sortTable(sortColumnIndex, ascending);
            table.new SortOrder(sortColumnIndex, ascending).set();
        }
    }

    public static void copySortOrder(TableDataCollection source, TableDataCollection target)
    {
        String sortOrderStr = source.getInfo().getProperty( TableDataCollection.SORT_ORDER_PROPERTY );
        if( sortOrderStr != null )
        {
            target.new SortOrder( sortOrderStr ).set();
        }
    }

    public static StreamEx<String> splitToStream(String ids)
    {
        return StreamEx.split( ids, IDS_SPLIT_PATTERN );
    }

    public static String[] splitIds(String ids)
    {
        if(ids.isEmpty())
            return new String[0];
        return IDS_SPLIT_PATTERN.split(ids);
    }

    /**
     * Get column model for data collection
     */
    public static com.developmentontheedge.beans.swing.table.ColumnModel getColumnModel(DataCollection<?> dc)
    {
        com.developmentontheedge.beans.swing.table.ColumnModel columnModel = null;
        if(dc instanceof FilteredDataCollection)
        {
            dc = ((FilteredDataCollection<?>)dc).getPrimaryCollection();
        }
        if( dc instanceof TableDataCollection )
        {
            columnModel = ( (TableDataCollection)dc ).getColumnModel().getSwingColumnModel();
        }
        else if( dc.getSize() > 0 )
        {
            try
            {
                columnModel = new BeanColumnModelEx(dc.iterator().next());
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Unable to initialize column model for " + dc, e);
            }
        }
        else
        {
            try
            {
                columnModel = new BeanColumnModelEx(dc.getDataElementType().newInstance(), PropertyInspector.SHOW_USUAL);
            }
            catch( Exception e )
            {
            }
        }
        if( columnModel == null )
        {
            try
            {
                columnModel = new BeanColumnModelEx(dc.getDataElementType().getConstructor( String.class, DataCollection.class ).newInstance("", null), PropertyInspector.SHOW_USUAL);
            }
            catch( Exception e )
            {
            }
        }
        if( columnModel == null )
            columnModel = new BeanColumnModelEx(new Object(), PropertyInspector.SHOW_USUAL);
        return columnModel;
    }

    public static BiosoftTableModel getTableModel(DataCollection<?> dc, com.developmentontheedge.beans.swing.table.ColumnModel columnModel)
    {
        DataCollection<?> primary = DataCollectionUtils.fetchPrimaryCollection( dc, Permission.READ );
        if(primary instanceof SortableDataCollection)
        {
            return new SortedCollectionModel((SortableDataCollection<? extends DataElement>)primary, columnModel);
        }
        return new SortedBeanTableModelEx(new DataCollectionRowModelAdapter(dc), columnModel);
    }

    public static TableDataCollection createTableLike(TableDataCollection another, DataElementPath targetPath)
    {
        TableDataCollection res = TableDataCollectionUtils.createTableDataCollection( targetPath );
        DataCollectionUtils.copyPersistentInfo( res, another );
        ColumnModel columnModel = another.getColumnModel();
        ColumnModel resColumnModel = res.getColumnModel();
        for( int i = 0; i < columnModel.getColumnCount(); i++ )
        {
            TableColumn col = columnModel.getColumn( i );
            resColumnModel.addColumn( resColumnModel.cloneTableColumn( col ) );
        }
        return res;
    }

}
