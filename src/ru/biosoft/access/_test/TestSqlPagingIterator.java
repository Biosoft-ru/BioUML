package ru.biosoft.access._test;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.sql.Connectors;
import ru.biosoft.access.sql.SqlConnectionHolder;
import ru.biosoft.access.sql.SqlPagingIterator;
import ru.biosoft.access.sql.SqlUtil;

public class TestSqlPagingIterator extends TestCase implements SqlConnectionHolder
{
    @Override
    protected void setUp() throws Exception
    {
        SqlUtil.execute( getConnection(), "CREATE TABLE test_sql_paging (myid INTEGER UNSIGNED NOT NULL AUTO_INCREMENT, val INTEGER NOT NULL, PRIMARY KEY(myid))" );
        SqlUtil.execute( getConnection(), "INSERT INTO test_sql_paging (val) VALUES(1),(2),(3),(4),(3),(1),(3)" );
    }
    
    @Override
    protected void tearDown() throws Exception
    {
        SqlUtil.dropTable( getConnection(), "test_sql_paging" );
    }
    public void test1()
    {
        try(SqlPagingIterator<Integer> it = new SqlPagingIterator<>( this, "test_sql_paging", "myid", "val != 3", 3, rs->rs.getInt("val") ))
        {
            List<Integer> resList = new ArrayList<>();
            while(it.hasNext())
            {
                Integer res = it.next();
                resList.add( res );
            }
            assertEquals( 4, resList.size() );
            assertEquals( Integer.valueOf(1), resList.get( 0 ) );
            assertEquals( Integer.valueOf(2), resList.get( 1 ) );
            assertEquals( Integer.valueOf(4), resList.get( 2 ) );
            assertEquals( Integer.valueOf(1), resList.get( 3 ) );
        }
    }

    @Override
    public Connection getConnection() throws BiosoftSQLException
    {
        return Connectors.getConnection( "db_for_tests" );
    }
}
