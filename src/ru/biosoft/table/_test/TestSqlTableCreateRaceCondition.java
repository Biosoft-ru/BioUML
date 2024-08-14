package ru.biosoft.table._test;

import java.sql.Connection;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.TestCase;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.sql.Connectors;
import ru.biosoft.access.sql.SqlConnectionHolder;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.table.SqlTableDataCollection;

public class TestSqlTableCreateRaceCondition extends TestCase
{
    public static class Root<T extends DataElement> extends VectorDataCollection<T> implements SqlConnectionHolder
    {
        public Root()
        {
            super("root");
            CollectionFactory.registerRoot( this );
        }

        @Override
        public Connection getConnection() throws BiosoftSQLException
        {
            return Connectors.getConnection( "db_for_tests" );
        }
        
    }
    
    private Root<SqlTableDataCollection> origin;
    
    @Override
    protected void setUp() throws Exception
    {
        origin = new Root<>();
    }
    
    public void test() throws Exception
    {
        AtomicInteger id = new AtomicInteger( 0 );
        AtomicReference<Exception> exception = new AtomicReference<>();
        
        Runnable r = new Runnable()
        {
            @Override
            public void run()
            {
                if(exception.get() != null)
                    return;
                Properties properties = new Properties();
                String name = "someVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongName"
                        + id.incrementAndGet();
                properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, name );
                try
                {
                    SqlTableDataCollection table = new SqlTableDataCollection( origin, properties );
                    origin.put( table );
                    table.getColumnModel().addColumn( "C1", Integer.class );
                }
                catch( Exception e )
                {
                    if(exception.get()==null)
                        exception.set( e );
                }
            }
        };

        ExecutorService executor = Executors.newFixedThreadPool( 2 );
        for(int i = 0; i < 100; i++)
            executor.execute( r );
        executor.shutdown();
        if( !executor.awaitTermination( 120, TimeUnit.SECONDS ) )
            fail( "Unable to finish in 120 seconds" );
        
        if(exception.get() != null)
            throw exception.get();
        
        assertEquals( 100, origin.getSize() );
    }
    
    @Override
    protected void tearDown() throws Exception
    {
        Connection c = origin.getConnection();
        for( SqlTableDataCollection t : origin )
        {
            SqlUtil.dropTable( c, t.getTableId() );
        }
        //origin.stream().forEach( t->SqlUtil.dropTable( c, t.getTableId() ) );
    }

}
