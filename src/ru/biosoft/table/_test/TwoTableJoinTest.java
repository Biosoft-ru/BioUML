package ru.biosoft.table._test;

import static ru.biosoft.table.TableDataCollectionUtils.INNER_JOIN;
import static ru.biosoft.table.TableDataCollectionUtils.LEFT_JOIN;
import static ru.biosoft.table.TableDataCollectionUtils.LEFT_SUBSTRACTION;
import static ru.biosoft.table.TableDataCollectionUtils.OUTER_JOIN;
import static ru.biosoft.table.TableDataCollectionUtils.RIGHT_JOIN;
import static ru.biosoft.table.TableDataCollectionUtils.RIGHT_SUBSTRACTION;
import static ru.biosoft.table.TableDataCollectionUtils.SYMMETRIC_DIFFERENCE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import one.util.streamex.StreamEx;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.analysis.aggregate.NumericAggregator;
import ru.biosoft.analysis.aggregate.Sum;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.StringSet;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

/**
 * {@link TableDataCollectionUtils#join} reads the rows it needs from each table at once: in one pass, or by key when it needs few
 * rows of a large table. Its result is compared with a join that looks up every key in both tables, as the method did before:
 * for SQL tables that was a query per row and table.
 */
public class TwoTableJoinTest extends AbstractBioUMLTest
{
    private static final int[] JOIN_TYPES = {INNER_JOIN, LEFT_JOIN, RIGHT_JOIN, OUTER_JOIN, LEFT_SUBSTRACTION, RIGHT_SUBSTRACTION,
            SYMMETRIC_DIFFERENCE};
    private static final String[] NONE = new String[0];

    public void testJoinTypes() throws Exception
    {
        CountingTable t1 = createTable( "t1", 300, 0.6, 1 );
        CountingTable t2 = createTable( "t2", 300, 0.6, 2 );
        String[] t1Columns = {"Name", "Score", "Own"};
        String[] t2Columns = {"Score", "Tags", "Own"};
        for( int joinType : JOIN_TYPES )
        {
            checkJoin( joinType, t1, t2, t1Columns, t2Columns, NONE, NONE, null, 0 );
            checkJoin( joinType, t1, t2, t1Columns, t2Columns, NONE, NONE, null, 7 );
        }
    }

    public void testCommonColumns() throws Exception
    {
        CountingTable t1 = createTable( "t1", 300, 0.6, 3 );
        CountingTable t2 = createTable( "t2", 300, 0.6, 4 );
        String[] own = {"Own"};
        String[] common = {"Name", "Score", "Count", "Tags"};
        for( int joinType : new int[] {INNER_JOIN, LEFT_JOIN, RIGHT_JOIN, OUTER_JOIN} )
        {
            for( NumericAggregator aggregator : new NumericAggregator[] {null, new Sum()} )
            {
                checkJoin( joinType, t1, t2, own, own, common, common, aggregator, 0 );
                checkJoin( joinType, t1, t2, own, own, common, common, aggregator, 7 );
            }
        }
    }

    public void testEmptyTable() throws Exception
    {
        CountingTable full = createTable( "full", 50, 1.0, 5 );
        CountingTable empty = createTable( "empty", 0, 1.0, 6 );
        String[] columns = {"Name", "Score"};
        for( int joinType : JOIN_TYPES )
        {
            checkJoin( joinType, full, empty, columns, columns, NONE, NONE, null, 0 );
            checkJoin( joinType, empty, full, columns, columns, NONE, NONE, null, 0 );
        }
    }

    public void testSelfJoinReadsTableOnce() throws Exception
    {
        // "Select columns" joins a table with itself
        CountingTable table = createTable( "t", 100, 1.0, 7 );
        List<Object[]> expected = joinKeyByKey( INNER_JOIN, table, table, new String[] {"Score", "Name"}, NONE, NONE, NONE, null );
        table.resetCounts();

        TableDataCollection result = TableDataCollectionUtils.join( INNER_JOIN, table, table, null, new String[] {"Score", "Name"}, NONE );

        assertSameRows( "self join", expected, result );
        assertEquals( "passes", 1, table.passes );
        assertEquals( "lookups by key", 0, table.lookups );
    }

    public void testFewRowsOfLargeTableAreLookedUp() throws Exception
    {
        CountingTable small = createTable( "small", 0, 1.0, 8 );
        CountingTable large = createTable( "large", 1000, 1.0, 9 );
        for( int k = 0; k < 1000; k += 100 )
            TableDataCollectionUtils.addRow( small, key( k ), new Object[] {"S", 1.0, k, new StringSet(), 2.0}, true );
        String[] columns = {"Name", "Score"};
        List<Object[]> expected = joinKeyByKey( INNER_JOIN, small, large, columns, columns, NONE, NONE, null );
        small.resetCounts();
        large.resetCounts();

        TableDataCollection result = TableDataCollectionUtils.join( INNER_JOIN, small, large, null, columns, columns );

        assertSameRows( "inner join of 10 and 1000 rows", expected, result );
        // 10 of 1000 rows: cheaper to look up one by one than to read the whole table
        assertEquals( "passes over the small table", 1, small.passes );
        assertEquals( "passes over the large table", 0, large.passes );
        assertEquals( "lookups in the large table", 10, large.lookups );
    }

    private static void checkJoin(int joinType, CountingTable t1, CountingTable t2, String[] t1Columns, String[] t2Columns,
            String[] common1, String[] common2, NumericAggregator aggregator, int rowsPerPass) throws Exception
    {
        String message = "join type " + joinType + ", " + t1.getName() + " with " + t2.getName() + ", common columns "
                + common1.length + ", aggregator " + aggregator + ", rows per pass " + rowsPerPass;
        List<Object[]> expected = joinKeyByKey( joinType, t1, t2, t1Columns, t2Columns, common1, common2, aggregator );
        t1.resetCounts();
        t2.resetCounts();

        TableDataCollection result = rowsPerPass == 0
                ? TableDataCollectionUtils.join( joinType, t1, t2, null, t1Columns, t2Columns, t1Columns, t2Columns, common1, common1,
                        common2, aggregator )
                : TableDataCollectionUtils.join( joinType, t1, t2, null, t1Columns, t2Columns, t1Columns, t2Columns, common1, common1,
                        common2, aggregator, rowsPerPass );

        assertSameRows( message, expected, result );
        if( rowsPerPass == 0 )
        {
            for( CountingTable table : new CountingTable[] {t1, t2} )
            {
                assertTrue( message + ": passes over " + table.getName() + ": " + table.passes, table.passes <= 1 );
                assertEquals( message + ": lookups in " + table.getName(), 0, table.lookups );
            }
        }
    }

    /**
     * The join as it was done before: for every key in sorted order, look the key up in both tables.
     * Unique columns of the first table come first, then the common columns, then unique columns of the second table.
     * @return rows as key followed by the values
     */
    private static List<Object[]> joinKeyByKey(int joinType, TableDataCollection t1, TableDataCollection t2, String[] t1Columns,
            String[] t2Columns, String[] common1, String[] common2, NumericAggregator aggregator) throws Exception
    {
        SortedSet<String> keys = new TreeSet<>( t1.getNameList() );
        keys.addAll( t2.getNameList() );
        int commonShift = 1 + t1Columns.length;
        int t2Shift = commonShift + common1.length;
        List<Object[]> rows = new ArrayList<>();
        for( String key : keys )
        {
            RowDataElement row1 = t1.contains( key ) ? t1.get( key ) : null;
            RowDataElement row2 = t2.contains( key ) ? t2.get( key ) : null;
            int kind = row1 != null && row2 != null ? INNER_JOIN : row1 != null ? LEFT_SUBSTRACTION : RIGHT_SUBSTRACTION;
            if( ( joinType & kind ) == 0 )
                continue;
            Object[] row = new Object[t2Shift + t2Columns.length];
            row[0] = key;
            for( int c = 0; c < t1Columns.length && row1 != null; c++ )
                row[1 + c] = row1.getValue( t1Columns[c] );
            for( int c = 0; c < t2Columns.length && row2 != null; c++ )
                row[t2Shift + c] = row2.getValue( t2Columns[c] );
            for( int c = 0; c < common1.length; c++ )
            {
                if( row1 != null && row2 != null && aggregator != null )
                    row[commonShift + c] = merge( t1.getColumnModel().getColumn( common1[c] ), row1.getValue( common1[c] ),
                            row2.getValue( common2[c] ), aggregator );
                else if( row1 != null )
                    row[commonShift + c] = row1.getValue( common1[c] );
                else
                    row[commonShift + c] = row2.getValue( common2[c] );
            }
            rows.add( row );
        }
        return rows;
    }

    private static Object merge(TableColumn column, Object value1, Object value2, NumericAggregator aggregator)
    {
        if( column.getValueClass() == String.class )
            return StreamEx.of( value1, value2 ).select( String.class ).remove( String::isEmpty ).sorted().distinct().joining( "," );
        if( column.getValueClass() == StringSet.class )
        {
            Set<String> values = new TreeSet<>();
            for( Object value : new Object[] {value1, value2} )
            {
                if( value instanceof StringSet )
                    values.addAll( (StringSet)value );
            }
            return new StringSet( values );
        }
        if( column.getType().isNumeric() )
        {
            double[] values = StreamEx.of( value1, value2 ).mapToDouble( v -> v instanceof Number ? ( (Number)v ).doubleValue() : Double.NaN )
                    .toArray();
            return column.getType().convertValue( aggregator.aggregate( values ) );
        }
        return null;
    }

    private static void assertSameRows(String message, List<Object[]> expected, TableDataCollection result) throws Exception
    {
        assertEquals( message + ": rows", expected.size(), result.getSize() );
        List<String> names = result.getNameList();
        ColumnModel columns = result.getColumnModel();
        for( int i = 0; i < expected.size(); i++ )
        {
            Object[] row = expected.get( i );
            assertEquals( message + ": row " + i, row[0], names.get( i ) );
            // a table stores values converted to the column type, e.g. a missing Float value as NaN
            Object[] values = new Object[columns.getColumnCount()];
            for( int c = 0; c < values.length; c++ )
                values[c] = columns.getColumn( c ).getType().convertValue( row[c + 1] );
            assertEquals( message + ": " + row[0], Arrays.asList( values ), Arrays.asList( result.get( names.get( i ) ).getValues() ) );
        }
    }

    /**
     * Table with columns Name, Score, Count, Tags and Own and a random part of the keys, added in random order
     */
    private static CountingTable createTable(String name, int keys, double share, long seed) throws Exception
    {
        CountingTable table = new CountingTable( null, name );
        ColumnModel columns = table.getColumnModel();
        columns.addColumn( "Name", String.class );
        columns.addColumn( "Score", Double.class );
        columns.addColumn( "Count", Integer.class );
        columns.addColumn( "Tags", StringSet.class );
        columns.addColumn( "Own", Double.class );
        Random random = new Random( seed );
        List<Integer> order = new ArrayList<>();
        for( int k = 0; k < keys; k++ )
            order.add( k );
        Collections.shuffle( order, random );
        for( int k : order )
        {
            if( random.nextDouble() >= share )
                continue;
            StringSet tags = new StringSet( StreamEx.of( "a", "b", "c", "d" ).filter( tag -> random.nextBoolean() ).toList() );
            TableDataCollectionUtils.addRow( table, key( k ), new Object[] {random.nextInt( 3 ) == 0 ? "" : "N" + random.nextInt( 4 ),
                    random.nextDouble(), random.nextInt( 100 ), tags, random.nextGaussian()}, true );
        }
        return table;
    }

    private static String key(int k)
    {
        return String.format( "G%04d", k );
    }
}
