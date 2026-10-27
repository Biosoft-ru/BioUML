
package ru.biosoft.analysis;

import java.util.logging.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import one.util.streamex.StreamEx;

import org.apache.commons.lang.ArrayUtils;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysis.aggregate.NumericAggregator;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.StringSet;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.TextUtil2;

/**
 * Join several tables, take all columns into the result
 *
 */

@ClassIcon ( "resources/join-table-multi.gif" )
public class MultipleTableJoin extends AnalysisMethodSupport<MultipleTableJoinParameters>
{
    /** Result cells one pass over the input tables holds in memory */
    private static final int MAX_CELLS_PER_PASS = 5_000_000;

    /**
     * @param origin
     * @param name
     */
    public MultipleTableJoin(DataCollection<?> origin, String name)
    {
        super(origin, name, new MultipleTableJoinParameters());
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        checkNotEmpty("tablePaths");
        checkPaths();
        List<TableDataCollection> tables = getAllTables(parameters.getTablePaths());
        if( tables.size() < 1 )
            throw new IllegalArgumentException("Please, select at least one table to join");
    }

    @Override
    public TableDataCollection justAnalyzeAndPut() throws Exception
    {
        log.info("Table joining started");
        validateParameters();
        DataElementPath resultPath = parameters.getOutputPath();
        List<TableJoinObject> tables = getJoinableTables(parameters.getTablePaths());
        jobControl.setPreparedness(2);
        
        TableDataCollection result = join(tables, resultPath);
        if( result != null )
            resultPath.save(result);
        
        DataCollectionUtils.copyPersistentInfo(result, tables.get(0).table);
        return result;
    }

    private List<TableDataCollection> getAllTables(DataElementPathSet pathList)
    {
        List<TableDataCollection> tables = new ArrayList<>();
        if( pathList.size() == 1 )
        {
            DataElementPath first = pathList.first();
            if( ! ( first.optDataElement() instanceof TableDataCollection ) && ( first.optDataElement() instanceof DataCollection ) )
                pathList = first.getChildren();
        }
        Iterator<ru.biosoft.access.core.DataElementPath> iter = pathList.iterator();
        while( iter.hasNext() )
        {
            DataElement de = iter.next().optDataElement();
            if( ! ( de instanceof TableDataCollection ) )
            {
                log.warning( ( de == null ) ? "null" : de.getName() + " is not a table, skipping");
                continue;
            }
            tables.add((TableDataCollection)de);
        }
        return tables;
    }

    /**
     * Return list of table objects (table, old column names, new column names)
     * @param pathList - list of table paths
     */
    private List<TableJoinObject> getJoinableTables(DataElementPathSet pathList)
    {
        List<TableJoinObject> tables = new ArrayList<>();
        if( pathList.size() == 1 )
        {
            DataElementPath first = pathList.first();
            if( ! ( first.optDataElement() instanceof TableDataCollection ) && ( first.optDataElement() instanceof DataCollection ) )
                pathList = first.getChildren();
        }
        Iterator<ru.biosoft.access.core.DataElementPath> iter = pathList.iterator();
        boolean mergeColumns = parameters.isMergeColumns();
        Set<String> allNames = new HashSet<>();
        Set<String> commonNames = new HashSet<>();
        while( iter.hasNext() )
        {
            DataElement de = iter.next().optDataElement();
            if( ! ( de instanceof TableDataCollection ) )
            {
                log.warning( ( de == null ) ? "null" : de.getName() + " is not a table, skipping");
                continue;
            }

            String[] curNames = TableDataCollectionUtils.getColumnNames((TableDataCollection)de);
            String[] newNames = new String[curNames.length];
            System.arraycopy(curNames, 0, newNames, 0, curNames.length);
            for( String curName : curNames )
            {
                if( allNames.contains(curName) )
                    commonNames.add(curName);
                else
                    allNames.add(curName);
            }

            tables.add(new TableJoinObject((TableDataCollection)de, curNames, newNames));
        }

        if( commonNames.isEmpty() )
            return tables;
        for( TableJoinObject ob : tables )
        {
            String[] curNames = ob.columnNames;
            String suffix = " " + ob.table.getName();
            for( int i = 0; i < curNames.length; i++ )
            {
                if( commonNames.contains(curNames[i]) )
                {
                    if( mergeColumns )
                        ob.addMergeColumn(curNames[i]);
                    else
                        ob.newColumnNames[i] += suffix;
                }
            }
        }
        return tables;
    }

    private TableDataCollection join(List<TableJoinObject> inputs, DataElementPath output)
    {
        int joinType = parameters.getJoinTypeForAnalysis();
        NumericAggregator aggregator = parameters.getAggregator();
        boolean isMergeColumns = parameters.isMergeColumns();
        
        TableDataCollection result = null;
        try
        {
            DataCollection<DataElement> parent = output.getParentCollection();
            String name = output.getName();
            if( parent.contains(name) )
            {
                log.warning("Old table '" + output.toString() + "' was removed");
                parent.remove(name);
            }
            result = TableDataCollectionUtils.createTableDataCollection(parent, name);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not init output element", e);
            throw new RuntimeException("While creating table " + output + ": " + e);
        }
        Set<String> mergeColumns = new HashSet<>();
        int numEmpty = 0;
        for( TableJoinObject to : inputs )
        {
            if( to.table.isEmpty() )
                numEmpty++;
            if( isMergeColumns && to.mergeColumnNames.size() > 0 )
            {
                String[] colNames = new String[to.columnNames.length - to.mergeColumnNames.size()];
                int curC = 0;
                List<String> notAddedCommonColumns = new ArrayList<>();
                for( int c = 0; c < to.columnNames.length; c++ )
                {
                    if(colNames.length > 0 && ! to.mergeColumnNames.contains(to.columnNames[c]))
                    {
                        colNames[curC++] = to.columnNames[c];
                    }
                    else if( !mergeColumns.contains(to.columnNames[c]) )
                    {
                        mergeColumns.add(to.columnNames[c]);
                        notAddedCommonColumns.add(to.columnNames[c]);
                    }
                }
                if(colNames.length > 0)
                    TableDataCollectionUtils.addColumns(result, to.table, colNames, colNames);
                if( notAddedCommonColumns.size() > 0 )
                {
                    colNames = notAddedCommonColumns.toArray(new String[notAddedCommonColumns.size()]);
                    TableDataCollectionUtils.addColumns(result, to.table, colNames, colNames);
                }
            }
            else
                TableDataCollectionUtils.addColumns(result, to.table, to.columnNames, to.newColumnNames);
        }
        
        boolean isInnerJoin = joinType == TableDataCollectionUtils.INNER_JOIN;
        if( numEmpty == inputs.size() )
            return result;
        else if( isInnerJoin && numEmpty > 0 ) //inner join with empty table
            return result;

        KeysResult kr = getAllKeysWithSets( inputs );
        String[] keys = isInnerJoin
                ? StreamEx.of( kr.keys ).filter( key -> StreamEx.of( kr.namesSets ).allMatch( names -> names.contains( key ) ) )
                        .toArray( String[]::new )
                : kr.keys;
        ColumnModel columnModel = result.getColumnModel();
        int numColumns = columnModel.getColumnCount();
        boolean hasMergeColumns = isMergeColumns && mergeColumns.size() > 0;

        // Each pass reads every input table once and fills the result rows of that pass in memory:
        // getting rows by key would send a query per key and table to SQL tables
        int rowsPerPass = getRowsPerPass( numColumns );
        int passes = ( keys.length + rowsPerPass - 1 ) / rowsPerPass;
        for( int pass = 0; pass < passes; pass++ )
        {
            int from = pass * rowsPerPass;
            int to = Math.min( keys.length, from + rowsPerPass );
            Map<String, Object[]> rows = new HashMap<>();
            for( int i = from; i < to; i++ )
                rows.put( keys[i], new Object[numColumns] );
            int columnShift = 0;
            for( int j = 0; j < inputs.size(); j++ )
            {
                TableDataCollection t = inputs.get( j ).table;
                Set<String> tableKeys = kr.namesSets[j];
                Set<String> toRead = StreamEx.ofKeys( rows ).filter( tableKeys::contains ).toSet();
                if( !toRead.isEmpty() )
                {
                    for( RowDataElement row : t )
                    {
                        if( row != null && toRead.remove( row.getName() ) )
                            copyValues( row.getValues(), rows.get( row.getName() ), columnShift, t, columnModel, hasMergeColumns, mergeColumns );
                    }
                    // rows the table lists but its iterator did not return
                    for( String key : toRead )
                        copyValues( TableDataCollectionUtils.getRowValues( t, key ), rows.get( key ), columnShift, t, columnModel,
                                hasMergeColumns, mergeColumns );
                }
                columnShift += inputs.get( j ).columnNames.length;
                jobControl.setPreparedness( 2 + ( pass * inputs.size() + j + 1 ) * 95 / ( passes * inputs.size() ) );
            }

            for( int i = from; i < to; i++ )
            {
                Object[] rowBuffer = rows.get( keys[i] );
                if( hasMergeColumns )
                    mergeValues( rowBuffer, columnModel, mergeColumns, aggregator );
                TableDataCollectionUtils.addRow( result, keys[i], rowBuffer, true );
            }
        }
        result.finalizeAddition();
        ReferenceTypeRegistry.setCollectionReferenceType( result, ReferenceTypeRegistry.detectReferenceType( kr.keys ) );
        return result;
    }

    /**
     * Number of result rows one pass over the input tables fills. The rows of a pass are held in memory until the pass ends.
     */
    protected int getRowsPerPass(int numColumns)
    {
        return Math.max( 1, MAX_CELLS_PER_PASS / Math.max( 1, numColumns ) );
    }

    private static void copyValues(Object[] values, Object[] rowBuffer, int columnShift, TableDataCollection t, ColumnModel columnModel,
            boolean hasMergeColumns, Set<String> mergeColumns)
    {
        if( hasMergeColumns ) //we have merging columns, so will need to copy value by value
        {
            for( int c = 0; c < t.getColumnModel().getColumnCount(); c++ )
            {
                TableColumn tc = t.getColumnModel().getColumn( c );
                int ci = columnModel.getColumnIndex( tc.getName() );
                if( mergeColumns.contains( tc.getName() ) )
                {
                    if( rowBuffer[ci] == null )
                        rowBuffer[ci] = new ArrayList<>();
                    ( (List<Object>)rowBuffer[ci] ).add( values[c] );
                }
                else
                    rowBuffer[ci] = values[c];
            }
        }
        else
        {
            System.arraycopy( values, 0, rowBuffer, columnShift, values.length );
        }
    }

    private static void mergeValues(Object[] rowBuffer, ColumnModel columnModel, Set<String> mergeColumns, NumericAggregator aggregator)
    {
        for( String cName : mergeColumns )
        {
            int ci = columnModel.getColumnIndex( cName );
            if( rowBuffer[ci] == null )
                continue;
            TableColumn column = columnModel.getColumn( cName );

            List<Object> values = (List<Object>)rowBuffer[ci];
            if( column.getValueClass() == String.class )
            {
                rowBuffer[ci] = StreamEx.of( values ).select( String.class ).filter( TextUtil2::nonEmpty ).sorted().distinct()
                        .joining( "," );
                continue;
            }
            if( column.getValueClass() == StringSet.class )
            {
                Set<String> res = new TreeSet<>();
                for( Object o : values )
                {
                    if( o instanceof StringSet )
                        res.addAll( (StringSet)o );
                }
                rowBuffer[ci] = new StringSet( res );
                continue;
            }
            if( column.getType().isNumeric() )
            {
                List<Double> colValues = new ArrayList<>();
                for( Object o : values )
                {
                    if( ! ( o instanceof Number ) )
                        colValues.add( Double.NaN );
                    else
                        colValues.add( ( (Number) ( o ) ).doubleValue() );
                }
                Double[] resD = colValues.toArray( new Double[colValues.size()] );
                double res = aggregator.aggregate( ArrayUtils.toPrimitive( resD ) );
                rowBuffer[ci] = column.getType().convertValue( res );
                continue;
            }
        }
    }

    private static class KeysResult
    {
        String[] keys;
        Set<String>[] namesSets;

        KeysResult(String[] keys, Set<String>[] namesSets)
        {
            this.keys = keys;
            this.namesSets = namesSets;
        }
    }

    private KeysResult getAllKeysWithSets(List<TableJoinObject> inputs)
    {
        Set<String> allKeys = new HashSet<>();
        Set<String>[] namesSets = new Set[inputs.size()];
        for ( int i = 0; i < inputs.size(); i++ )
        {
            TableDataCollection tdc = inputs.get( i ).table;
            namesSets[i] = new HashSet<>( tdc.names().toList() );
            allKeys.addAll( namesSets[i] );
        }
        String[] keys = allKeys.stream().sorted().toArray( String[]::new );
        return new KeysResult( keys, namesSets );
    }

    private String[] getAllKeys(List<TableJoinObject> inputs)
    {
        return StreamEx.of( inputs ).flatMap( tjo -> tjo.table.names() ).sorted().distinct().toArray( String[]::new );
    }

    private static class TableJoinObject
    {
        public TableDataCollection table;
        public String[] columnNames;
        public String[] newColumnNames;
        public Set<String> mergeColumnNames;

        public TableJoinObject(TableDataCollection table, String[] columnNames, String[] newColumnNames)
        {
            this.table = table;
            this.columnNames = columnNames;
            this.newColumnNames = newColumnNames;
            mergeColumnNames = new HashSet<>();
        }

        public void addMergeColumn(String column)
        {
            mergeColumnNames.add(column);
        }
    }
}
