package ru.biosoft.table._test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.table.TableDataCollection;

public class FileTableTest extends TestCase
{
    public static final String repositoryPath = "../data/test/ru/biosoft/table";
    public static final DataElementPath dePath = DataElementPath.create("data/tables/m0.fac");
    public static final DataElementPath emptyColumnTablePath = DataElementPath.create( "data/tables/empty_column.fac" );

    public FileTableTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(FileTableTest.class.getName());

        suite.addTest(new FileTableTest("testLoadTable"));
        suite.addTest( new FileTableTest( "testLoadTableWithEmptyElements" ) );

        return suite;
    }

    public void testLoadTable() throws Exception
    {
        CollectionFactory.createRepository(repositoryPath);
        DataElement de = dePath.optDataElement();
        assertNotNull("Element not found: " + dePath, de);
        assertTrue("Element is not table", ( de instanceof TableDataCollection ));

        TableDataCollection table = (TableDataCollection)de;
        assertEquals("Incorrect row count", table.getSize(), 8);
        assertEquals("Incorrect column count", table.getColumnModel().getColumnCount(), 4);
        
        assertEquals("Incorrect row name", table.getName(1), "B");
        assertEquals("Incorrect element value", table.getValueAt(0, 0), 1.0);
        assertEquals("Incorrect element value", table.getValueAt(2, 1), 0.2);
        assertEquals("Incorrect element value", table.getValueAt(5, 3), null);
    }

    public void testLoadTableWithEmptyElements() throws Exception
    {
        CollectionFactory.createRepository( repositoryPath );
        DataElement de = emptyColumnTablePath.optDataElement();
        assertNotNull( "Element not found: " + emptyColumnTablePath, de );
        assertTrue( "Element is not table", ( de instanceof TableDataCollection ) );

        TableDataCollection table = (TableDataCollection)de;
        assertEquals( "Incorrect row count", table.getSize(), 3 );
        assertEquals( "Incorrect column count", table.getColumnModel().getColumnCount(), 5 );

        assertEquals( "Incorrect row name", table.getName( 1 ), "R02" );
        assertEquals( "Incorrect element value", table.getValueAt( 0, 0 ), "0.0" );
        assertEquals( "Incorrect element value", table.getValueAt( 0, 1 ), "0.0" );
        assertEquals( "Incorrect element value", table.getValueAt( 0, 2 ), "" );
        assertEquals( "Incorrect element value", table.getValueAt( 0, 3 ), "1.0" );
        assertEquals( "Incorrect element value", table.getValueAt( 0, 4 ), 0.0 );
        assertEquals( "Incorrect element value", table.getValueAt( 1, 0 ), "0.0" );
        assertEquals( "Incorrect element value", table.getValueAt( 1, 1 ), "" );
        assertEquals( "Incorrect element value", table.getValueAt( 1, 2 ), "" );
        assertEquals( "Incorrect element value", table.getValueAt( 1, 3 ), "" );
        assertEquals( "Incorrect element value", table.getValueAt( 1, 4 ), 0.0 );
        assertEquals( "Incorrect element value", table.getValueAt( 2, 1 ), "" );
        assertEquals( "Incorrect element value", table.getValueAt( 2, 2 ), "" );
        assertEquals( "Incorrect element value", table.getValueAt( 2, 4 ), 1.0 );
    }
}
