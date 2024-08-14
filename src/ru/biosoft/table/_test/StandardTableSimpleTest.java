package ru.biosoft.table._test;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

public class StandardTableSimpleTest extends TestCase
{
    public StandardTableSimpleTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(StandardTableSimpleTest.class.getName());

        suite.addTest(new StandardTableSimpleTest("testCreateTable"));
        suite.addTest(new StandardTableSimpleTest("testAddRows"));
        suite.addTest(new StandardTableSimpleTest("testGetElements"));
        suite.addTest(new StandardTableSimpleTest("testReadOnlyThreadSafe"));

        return suite;
    }

    private static final TableDataCollection table = new StandardTableDataCollection(null, "test");

    public void testCreateTable() throws Exception
    {
        table.getColumnModel().addColumn("column1", String.class);
        table.getColumnModel().addColumn("column2", Integer.class);
        table.getColumnModel().addColumn("column2", Double.class);
        assertEquals("Incorrect column count", table.getColumnModel().getColumnCount(), 3);
    }

    public void testAddRows() throws Exception
    {
        TableDataCollectionUtils.addRow(table, "row1", new Object[] {"test string", 3, 5.0});
        TableDataCollectionUtils.addRow(table, "row2", new Object[] {"test string 2", 5, 5.5});
        TableDataCollectionUtils.addRow(table, "row3", new Object[] {"test string 3", 60, 2.67});
        TableDataCollectionUtils.addRow(table, "row4", new Object[] {"test string 4", 1, 0.001});
        assertEquals("Incorrect table size", table.getSize(), 4);
    }

    public void testGetElements() throws Exception
    {
        Object v1 = table.getValueAt(1, 2);
        assertEquals("Incorrect value at (1,2)", v1, 5.5);

        Object v2 = table.getValueAt(3, 1);
        assertEquals("Incorrect value at (3,1)", v2, 1);

        Object v3 = table.getValueAt(2, 0);
        assertEquals("Incorrect value at (2,0)", v3, "test string 3");
    }
    
    public void testReadOnlyThreadSafe() throws Exception
    {
        final TableDataCollection inputTable = new StandardTableDataCollection( null, "input" );
        inputTable.getColumnModel().addColumn( "Value", Integer.class );
        for( int i = 1; i <= 10; i++ )
            TableDataCollectionUtils.addRow( inputTable, String.valueOf( i ), new Object[] {i} );
        ExecutorService pool = Executors.newFixedThreadPool( 10 );
        List<Future> futures = new ArrayList<>();
        final List<String> errors = new Vector<>();
        for( int i = 1; i <= 20; i++ )
        {
            Runnable task = () -> {
                List<String> names = new ArrayList<>( inputTable.getNameList() );
                if(names.size() != 10)
                    errors.add("names.size(): expected 10, got "+names.size());
                for( int j = 1; j <= 10; j++ )
                {
                    if(!String.valueOf( j ).equals( names.get( j - 1 ) ))
                    {
                        errors.add( "names[" + ( j - 1 ) + "]: expected " + j + "; got: " + names.get( j - 1 ) );
                    }
                }
            };
            Future<?> future = pool.submit( task );
            futures.add( future );
        }
        for( Future<?> future : futures )
            future.get();
        pool.shutdown();
        if(!errors.isEmpty())
            fail(errors.toString());
    }
}
