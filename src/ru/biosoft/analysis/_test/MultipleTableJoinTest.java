package ru.biosoft.analysis._test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

import ru.biosoft.access.FolderVectorCollection;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.analysis.MultipleTableJoin;
import ru.biosoft.analysis.MultipleTableJoinParameters;
import ru.biosoft.analysis.aggregate.Sum;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table._test.CountingTable;

/**
 * {@link MultipleTableJoin} reads the rows it needs from each input table at once: in one pass, or by key when it needs few rows
 * of a large table. Its result is compared with a join that looks up every key in every table, as the analysis did before:
 * for SQL tables that was one query per key and table.
 */
public class MultipleTableJoinTest extends AbstractBioUMLTest
{
    // indexes of the join types in MultipleTableJoinParameters
    private static final int INNER_JOIN = 0;
    private static final int OUTER_JOIN = 1;

    private FolderVectorCollection folder;
    private int resultCount;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        folder = new FolderVectorCollection( "test", null );
        CollectionFactory.registerRoot( folder );
    }

    public void testOuterJoinOfManyTables() throws Exception
    {
        List<CountingTable> tables = createTables( 12, 300, 0.6, 1 );
        List<Object[]> expected = joinKeyByKey( tables, false );

        TableDataCollection result = join( tables, OUTER_JOIN, false, 0 );

        assertSameRows( expected, result );
        assertReadOncePerPass( tables, 1 );
    }

    public void testInnerJoinOfManyTables() throws Exception
    {
        List<CountingTable> tables = createTables( 12, 300, 0.9, 2 );
        List<Object[]> expected = joinKeyByKey( tables, true );
        assertTrue( "Tables should share keys", expected.size() > 10 );

        TableDataCollection result = join( tables, INNER_JOIN, false, 0 );

        assertSameRows( expected, result );
        assertReadOncePerPass( tables, 1 );
    }

    public void testSeveralPassesGiveTheSameResult() throws Exception
    {
        for( boolean mergeColumns : new boolean[] {false, true} )
        {
            List<CountingTable> tables = createTables( 6, 200, 0.5, 3 );
            TableDataCollection onePass = join( tables, OUTER_JOIN, mergeColumns, 0 );
            int rowsPerPass = 17;
            TableDataCollection severalPasses = join( tables, OUTER_JOIN, mergeColumns, rowsPerPass );

            assertEquals( onePass.getNameList(), severalPasses.getNameList() );
            for( String key : onePass.getNameList() )
                assertEquals( key, Arrays.asList( onePass.get( key ).getValues() ), Arrays.asList( severalPasses.get( key ).getValues() ) );
            int passes = ( onePass.getSize() + rowsPerPass - 1 ) / rowsPerPass;
            for( CountingTable table : tables )
            {
                assertTrue( table.getName() + " passes: " + table.passes, table.passes > 1 && table.passes <= passes );
                assertEquals( table.getName() + " lookups by key", 0, table.lookups );
            }
        }
    }

    public void testMergedColumns() throws Exception
    {
        CountingTable t1 = createTable( "t1", new String[] {"Name", "Score", "A"}, new Class<?>[] {String.class, Double.class, Integer.class} );
        TableDataCollectionUtils.addRow( t1, "k1", new Object[] {"x", 1.0, 10} );
        TableDataCollectionUtils.addRow( t1, "k2", new Object[] {"y", 2.0, 20} );
        CountingTable t2 = createTable( "t2", new String[] {"Name", "Score", "B"}, new Class<?>[] {String.class, Double.class, String.class} );
        TableDataCollectionUtils.addRow( t2, "k3", new Object[] {"", 7.0, "b3"} );
        TableDataCollectionUtils.addRow( t2, "k2", new Object[] {"z", 5.0, "b2"} );

        TableDataCollection result = join( Arrays.asList( t1, t2 ), OUTER_JOIN, true, 0 );

        assertEquals( Arrays.asList( "A", "Name", "Score", "B" ), Arrays.asList( TableDataCollectionUtils.getColumnNames( result ) ) );
        assertEquals( Arrays.asList( "k1", "k2", "k3" ), result.getNameList() );
        assertEquals( Arrays.asList( 10, "x", 1.0, null ), Arrays.asList( result.get( "k1" ).getValues() ) );
        assertEquals( Arrays.asList( 20, "y,z", 7.0, "b2" ), Arrays.asList( result.get( "k2" ).getValues() ) );
        assertEquals( Arrays.asList( null, "", 7.0, "b3" ), Arrays.asList( result.get( "k3" ).getValues() ) );
        assertReadOncePerPass( Arrays.asList( t1, t2 ), 1 );
    }

    public void testFewRowsOfLargeTableAreLookedUp() throws Exception
    {
        CountingTable small = createTable( "t1", new String[] {"A"}, new Class<?>[] {Double.class} );
        CountingTable large = createTable( "t2", new String[] {"B"}, new Class<?>[] {Double.class} );
        for( int k = 0; k < 1000; k++ )
            TableDataCollectionUtils.addRow( large, String.format( "G%04d", k ), new Object[] {(double)k}, true );
        for( int k = 0; k < 1000; k += 100 )
            TableDataCollectionUtils.addRow( small, String.format( "G%04d", k ), new Object[] {-(double)k}, true );
        List<CountingTable> tables = Arrays.asList( small, large );
        List<Object[]> expected = joinKeyByKey( tables, true );

        TableDataCollection result = join( tables, INNER_JOIN, false, 0 );

        assertSameRows( expected, result );
        // 10 of 1000 rows: cheaper to look up one by one than to read the whole table
        assertEquals( "passes over the small table", 1, small.passes );
        assertEquals( "passes over the large table", 0, large.passes );
        assertEquals( "lookups in the large table", 10, large.lookups );
    }

    private TableDataCollection join(List<CountingTable> tables, int joinType, boolean mergeColumns, int rowsPerPass) throws Exception
    {
        MultipleTableJoin analysis = rowsPerPass == 0 ? new MultipleTableJoin( null, "join" ) : new MultipleTableJoin( null, "join" )
        {
            @Override
            protected int getRowsPerPass(int numColumns)
            {
                return rowsPerPass;
            }
        };
        MultipleTableJoinParameters parameters = analysis.getParameters();
        DataElementPathSet paths = new DataElementPathSet();
        for( TableDataCollection table : tables )
            paths.add( DataElementPath.create( table ) );
        parameters.setTablePaths( paths );
        parameters.setOutputPath( DataElementPath.create( folder, "result " + ++resultCount ) );
        parameters.setJoinType( joinType );
        parameters.setMergeColumns( mergeColumns );
        parameters.setAggregator( new Sum() );
        for( CountingTable table : tables )
            table.resetCounts();
        return analysis.justAnalyzeAndPut();
    }

    /**
     * The join as the analysis did it before: for every key in sorted order, look the key up in every table
     * @return rows as key followed by the values of all tables
     */
    private static List<Object[]> joinKeyByKey(List<? extends TableDataCollection> tables, boolean innerJoin) throws Exception
    {
        SortedSet<String> keys = new TreeSet<>();
        int width = 1;
        for( TableDataCollection table : tables )
        {
            keys.addAll( table.getNameList() );
            width += table.getColumnModel().getColumnCount();
        }
        List<Object[]> rows = new ArrayList<>();
        for( String key : keys )
        {
            Object[] row = new Object[width];
            row[0] = key;
            int shift = 1;
            boolean inAllTables = true;
            for( TableDataCollection table : tables )
            {
                if( table.contains( key ) )
                {
                    Object[] values = table.get( key ).getValues();
                    System.arraycopy( values, 0, row, shift, values.length );
                }
                else
                    inAllTables = false;
                shift += table.getColumnModel().getColumnCount();
            }
            if( inAllTables || !innerJoin )
                rows.add( row );
        }
        return rows;
    }

    private static void assertSameRows(List<Object[]> expected, TableDataCollection result) throws Exception
    {
        assertEquals( "Rows", expected.size(), result.getSize() );
        List<String> names = result.getNameList();
        ColumnModel columns = result.getColumnModel();
        for( int i = 0; i < expected.size(); i++ )
        {
            Object[] row = expected.get( i );
            assertEquals( "Row " + i, row[0], names.get( i ) );
            // a table stores values converted to the column type, e.g. a missing Float value as NaN
            Object[] values = new Object[columns.getColumnCount()];
            for( int c = 0; c < values.length; c++ )
                values[c] = columns.getColumn( c ).getType().convertValue( row[c + 1] );
            assertEquals( (String)row[0], Arrays.asList( values ), Arrays.asList( result.get( names.get( i ) ).getValues() ) );
        }
    }

    private static void assertReadOncePerPass(List<CountingTable> tables, int passes)
    {
        for( CountingTable table : tables )
        {
            assertEquals( table.getName() + " passes", passes, table.passes );
            assertEquals( table.getName() + " lookups by key", 0, table.lookups );
        }
    }

    /**
     * Tables t00, t01... with columns Label, Value and Count, which all tables have, and one column of their own.
     * Each table has a random part of the keys, added in random order.
     */
    private List<CountingTable> createTables(int count, int keys, double share, long seed) throws Exception
    {
        Random random = new Random( seed );
        List<CountingTable> tables = new ArrayList<>();
        for( int i = 0; i < count; i++ )
        {
            CountingTable table = createTable( String.format( "t%02d", i ), new String[] {"Label", "Value", "Count", "Own " + i},
                    new Class<?>[] {String.class, Double.class, Integer.class, Double.class} );
            List<Integer> order = new ArrayList<>();
            for( int k = 0; k < keys; k++ )
                order.add( k );
            Collections.shuffle( order, random );
            for( int k : order )
            {
                if( random.nextDouble() < share )
                    TableDataCollectionUtils.addRow( table, String.format( "G%04d", k ), new Object[] {
                            random.nextInt( 3 ) == 0 ? "" : "L" + random.nextInt( 5 ), random.nextDouble(), random.nextInt( 100 ),
                            random.nextGaussian()}, true );
            }
            tables.add( table );
        }
        return tables;
    }

    private CountingTable createTable(String name, String[] columns, Class<?>[] types) throws Exception
    {
        CountingTable table = new CountingTable( folder, name );
        for( int i = 0; i < columns.length; i++ )
            table.getColumnModel().addColumn( columns[i], types[i] );
        folder.put( table );
        return table;
    }
}
