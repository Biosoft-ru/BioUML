package ru.biosoft.table._test;

import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.access.AddRowTableAction;
import ru.biosoft.table.access.AddRowTableAction.AddRowProperties;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestAddRowTableAction extends TestCase
{
    public TestAddRowTableAction(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestAddRowTableAction.class.getName());
        suite.addTest(new TestAddRowTableAction("test"));
        return suite;
    }
    
    public void test() throws Exception
    {
        StandardTableDataCollection table = new StandardTableDataCollection(null, "test");  
        table.getColumnModel().addColumn("column1", String.class);
        table.getColumnModel().addColumn("column2", Integer.class);

        assertNotNull (table);
        AddRowTableAction action = new AddRowTableAction();
        AddRowProperties properties = action.getProperties( table, null );
        properties.setRowNames( new String[]{"added row"} );
        action.getJobControl( table, null, properties ).run();
        
        assertEquals (table.getSize(), 1);
        checkRow(table.get("added row"), new Object[]{String.valueOf( "" ), Integer.valueOf( 0 )});
        
        action = new AddRowTableAction();
        properties = action.getProperties( table, null );
        properties.setRowNames( new String[]{"one more row", "yet another row"} );
        action.getJobControl( table, null, properties ).run();
        
        assertEquals (table.getSize(), 3);
        checkRow(table.get("one more row"), new Object[]{String.valueOf( "" ), Integer.valueOf( 0 )});
        checkRow(table.get("yet another row"), new Object[]{String.valueOf( "" ), Integer.valueOf( 0 )});

    }
    
    private void checkRow(RowDataElement row, Object[] values)
    {
        assertNotNull(row);
        Object[] rowValues = row.getValues();
        assertEquals(rowValues.length, values.length);
        
        for (int i=0; i< values.length; i++)
        {
            assertEquals(values[i], rowValues[i]);
        }
    }
    
    

}
