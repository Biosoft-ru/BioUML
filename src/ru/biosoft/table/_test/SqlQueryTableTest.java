package ru.biosoft.table._test;

import java.sql.Connection;

import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.sql.Connectors;
import ru.biosoft.access.sql.SqlConnectionHolder;
import ru.biosoft.table.SqlQueryTableDataCollection;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author lan
 *
 */
public class SqlQueryTableTest extends TestCase implements SqlConnectionHolder
{
    public SqlQueryTableTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(SqlQueryTableTest.class.getName());

        suite.addTest(new SqlQueryTableTest("testQuery"));
        return suite;
    }

    public void testQuery() throws Exception
    {
        SqlQueryTableDataCollection tdc = new SqlQueryTableDataCollection("", this, "SELECT * FROM gene_stable_id");
        assertEquals(5, tdc.getColumnModel().getColumnCount());
        assertEquals(Integer.class, tdc.getColumnModel().getColumn(0).getValueClass());
        assertEquals(String.class, tdc.getColumnModel().getColumn(1).getValueClass());
        assertEquals(37435, tdc.getSize());
        assertEquals(33979, (tdc.get("1")).getValues()[0]);
    }
    
    @Override
    public Connection getConnection() throws BiosoftSQLException
    {
        return Connectors.getConnection( "ensembl_human_52" );
    }
}
