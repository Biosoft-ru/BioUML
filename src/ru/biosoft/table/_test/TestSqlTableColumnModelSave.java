package ru.biosoft.table._test;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONObject;

import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementPutException;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.sql.SqlConnectionHolder;
import ru.biosoft.access.sql.SqlConnectionPool;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.table.SqlTableDataCollection;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollectionUtils;

/**
 * A table without a column_info id keeps its column model in its info, stored when the table is saved. Building a
 * table column by column used to save it for every column; now a table with no row yet is saved once, when its first
 * row is added or at finalizeAddition, and a table with rows is still saved at each column change.
 *
 * <p>Needs MySQL: BIOUML_TEST_MYSQL_URL (jdbc:mysql://host:port/db), BIOUML_TEST_MYSQL_USER and
 * BIOUML_TEST_MYSQL_PASSWORD. Without them it checks nothing (CI has no MySQL).</p>
 */
public class TestSqlTableColumnModelSave extends AbstractBioUMLTest
{
    private static final String URL = System.getenv( "BIOUML_TEST_MYSQL_URL" );

    /**
     * A parent collection that records each save of a table: the column names in its info at that moment, and a copy
     * of the info properties, which is what a save stores (re-opening a table from that copy shows what survives).
     */
    public static class Root extends VectorDataCollection<SqlTableDataCollection> implements SqlConnectionHolder
    {
        final List<List<String>> saves = new ArrayList<>();
        final List<Properties> stored = new ArrayList<>();
        /** The next saves to refuse, as a failing repository would. */
        int failNext;

        public Root()
        {
            super( "columnSaveTestRoot" );
            CollectionFactory.registerRoot( this ); // so that DataElementPath.save finds this parent
        }

        @Override
        public Connection getConnection() throws BiosoftSQLException
        {
            return SqlConnectionPool.getPersistentConnection( URL, System.getenv( "BIOUML_TEST_MYSQL_USER" ),
                    System.getenv( "BIOUML_TEST_MYSQL_PASSWORD" ) );
        }

        @Override
        public SqlTableDataCollection put(SqlTableDataCollection table) throws DataElementPutException
        {
            if( failNext > 0 )
            {
                failNext--;
                throw new IllegalStateException( "save refused by the test" );
            }
            saves.add( columnsInInfo( table ) );
            stored.add( (Properties)table.getInfo().getProperties().clone() );
            return super.put( table );
        }
    }

    private Root root;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        if( URL != null )
            root = new Root();
    }

    @Override
    protected void tearDown() throws Exception
    {
        if( root != null )
            for( SqlTableDataCollection table : root )
                SqlUtil.dropTable( root.getConnection(), table.getTableId() );
        super.tearDown();
    }

    private static List<String> columnsInInfo(SqlTableDataCollection table)
    {
        List<String> names = new ArrayList<>();
        String json = table.getInfo().getProperty( SqlTableDataCollection.COLUMN_MODEL_PROPERTY );
        if( json == null )
            return names;
        JSONArray columns = new JSONObject( json ).getJSONArray( "columns" );
        for( int i = 0; i < columns.length(); i++ )
            names.add( columns.getJSONObject( i ).getString( "name" ) );
        return names;
    }

    private SqlTableDataCollection newTable(String name) throws Exception
    {
        Properties properties = new Properties();
        properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, name );
        SqlTableDataCollection table = new SqlTableDataCollection( root, properties );
        root.put( table );
        root.saves.clear();
        root.stored.clear();
        return table;
    }

    private static List<String> columns(int count)
    {
        List<String> names = new ArrayList<>();
        for( int i = 1; i <= count; i++ )
            names.add( "C" + i );
        return names;
    }

    private boolean skipped()
    {
        if( URL == null )
        {
            System.out.println( getName() + ": no BIOUML_TEST_MYSQL_URL, nothing checked" );
            return true;
        }
        return false;
    }

    /** A table built column by column, then filled: one save, at the first row, with every column in its info. */
    public void testBuildingATableSavesItOnce() throws Exception
    {
        if( skipped() )
            return;
        SqlTableDataCollection table = newTable( "built" );
        for( String name : columns( 20 ) )
            table.getColumnModel().addColumn( name, Integer.class );
        assertEquals( "no save while the columns are added", 0, root.saves.size() );
        Object[] values = new Object[20];
        for( int i = 0; i < 20; i++ )
            values[i] = i;
        TableDataCollectionUtils.addRow( table, "r1", values, true );
        assertEquals( "saved when the first row is added, before it is written", 1, root.saves.size() );
        TableDataCollectionUtils.addRow( table, "r2", values, true );
        table.finalizeAddition();
        assertEquals( "one save", 1, root.saves.size() );
        assertEquals( columns( 20 ), root.saves.get( 0 ) );
        assertEquals( 2, table.getSize() );
    }

    /** Columns added and finalizeAddition without a row (an empty result table): one save. */
    public void testEmptyTableIsSavedAtFinalizeAddition() throws Exception
    {
        if( skipped() )
            return;
        SqlTableDataCollection table = newTable( "empty" );
        for( String name : columns( 5 ) )
            table.getColumnModel().addColumn( name, String.class );
        table.finalizeAddition();
        table.finalizeAddition();
        assertEquals( 1, root.saves.size() );
        assertEquals( columns( 5 ), root.saves.get( 0 ) );
    }

    /** A table with rows is saved at each column change, as before. */
    public void testTableWithRowsIsSavedAtEachColumn() throws Exception
    {
        if( skipped() )
            return;
        SqlTableDataCollection table = newTable( "filled" );
        table.getColumnModel().addColumn( "A", Integer.class );
        TableDataCollectionUtils.addRow( table, "r1", new Object[] {1}, false );
        root.saves.clear();
        table.getColumnModel().addColumn( "B", Integer.class );
        table.getColumnModel().addColumn( "C", Integer.class );
        assertEquals( 2, root.saves.size() );
        assertEquals( java.util.Arrays.asList( "A", "B", "C" ), root.saves.get( 1 ) );
    }

    /** A table whose save never came still has its columns: they are read from the database table. */
    public void testColumnsSurviveAMissingSave() throws Exception
    {
        if( skipped() )
            return;
        SqlTableDataCollection table = newTable( "unsaved" );
        table.getColumnModel().addColumn( "Count", Integer.class );
        table.getColumnModel().addColumn( "Label", String.class );
        Properties stored = (Properties)table.getInfo().getProperties().clone();
        stored.remove( SqlTableDataCollection.COLUMN_MODEL_PROPERTY ); // as stored by the last save: before the columns
        SqlTableDataCollection reread = new SqlTableDataCollection( root, stored );
        List<String> names = new ArrayList<>();
        for( TableColumn column : reread.getColumnModel() )
            names.add( column.getName() );
        assertEquals( java.util.Arrays.asList( "Count", "Label" ), names );
        assertEquals( Integer.class, reread.getColumnModel().getColumn( "Count" ).getValueClass() );
    }

    /** A save that fails leaves the column model pending: the next finalizeAddition saves it. */
    public void testFailedSaveIsRetried() throws Exception
    {
        if( skipped() )
            return;
        SqlTableDataCollection table = newTable( "retried" );
        table.getColumnModel().addColumn( "A", Integer.class );
        table.getColumnModel().addColumn( "B", Integer.class );
        root.failNext = 1;
        try
        {
            table.finalizeAddition();
            fail( "the refused save was not reported" );
        }
        catch( RuntimeException e )
        {
            // reported
        }
        assertEquals( 0, root.saves.size() );
        table.finalizeAddition();
        assertEquals( 1, root.saves.size() );
        assertEquals( java.util.Arrays.asList( "A", "B" ), root.saves.get( 0 ) );
        table.finalizeAddition();
        assertEquals( "saved once, not again", 1, root.saves.size() );
    }

    /** Metadata the database table does not hold (a description) is in what the save stores, and survives re-opening. */
    public void testDescriptionSurvivesReopening() throws Exception
    {
        if( skipped() )
            return;
        SqlTableDataCollection table = newTable( "described" );
        table.getColumnModel().addColumn( new TableColumn( "Score", "Score", "The weighted score", ru.biosoft.table.datatype.DataType.Float, null ) );
        table.finalizeAddition();
        assertEquals( 1, root.stored.size() );
        SqlTableDataCollection reopened = new SqlTableDataCollection( root, root.stored.get( 0 ) );
        assertEquals( "The weighted score", reopened.getColumnModel().getColumn( "Score" ).getShortDescription() );
    }

    /** Columns added and removed before the first row: the save stores the final columns, as the database has them. */
    public void testAddedThenRemovedBeforeTheFirstRow() throws Exception
    {
        if( skipped() )
            return;
        SqlTableDataCollection table = newTable( "reshaped" );
        for( String name : columns( 4 ) )
            table.getColumnModel().addColumn( name, Integer.class );
        table.getColumnModel().removeColumn( 1 ); // C2
        assertEquals( 0, root.saves.size() );
        table.finalizeAddition();
        assertEquals( 1, root.saves.size() );
        assertEquals( java.util.Arrays.asList( "C1", "C3", "C4" ), root.saves.get( 0 ) );
        SqlTableDataCollection reopened = new SqlTableDataCollection( root, root.stored.get( 0 ) );
        List<String> names = new ArrayList<>();
        for( TableColumn column : reopened.getColumnModel() )
            names.add( column.getName() );
        assertEquals( java.util.Arrays.asList( "C1", "C3", "C4" ), names );
    }

    /** A table opened from the repository (size not known, -1) is saved at each column change, as before. */
    public void testReopenedTableIsSavedAtOnce() throws Exception
    {
        if( skipped() )
            return;
        SqlTableDataCollection table = newTable( "reopenedEmpty" );
        table.getColumnModel().addColumn( "A", Integer.class );
        table.finalizeAddition();
        SqlTableDataCollection reopened = new SqlTableDataCollection( root, root.stored.get( 0 ) );
        root.saves.clear();
        reopened.getColumnModel().addColumn( "B", Integer.class );
        assertEquals( 1, root.saves.size() );
        assertEquals( java.util.Arrays.asList( "A", "B" ), root.saves.get( 0 ) );
    }

    /** Rows still buffered when a column is added: the rows are written, the table saved at once, nothing lost. */
    public void testBufferedRowsThenAColumn() throws Exception
    {
        if( skipped() )
            return;
        SqlTableDataCollection table = newTable( "buffered" );
        table.getColumnModel().addColumn( "A", Integer.class );
        TableDataCollectionUtils.addRow( table, "r1", new Object[] {1}, true );
        TableDataCollectionUtils.addRow( table, "r2", new Object[] {2}, true );
        root.saves.clear();
        table.getColumnModel().addColumn( "B", Integer.class );
        assertEquals( "a table with rows: saved at the column change", 1, root.saves.size() );
        table.finalizeAddition();
        assertEquals( 2, table.getSize() );
        assertEquals( 1, table.get( "r1" ).getValues()[0] );
        assertEquals( 2, table.getColumnModel().getColumnCount() );
    }
}
