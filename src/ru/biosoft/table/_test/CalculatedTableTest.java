package ru.biosoft.table._test;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import ru.biosoft.plugins.javascript.JScriptContext;
import ru.biosoft.plugins.javascript.host.JavaScriptData;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

/**
 * Test for calculated columns
 */
public class CalculatedTableTest extends TestCase
{
    public CalculatedTableTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(CalculatedTableTest.class.getName());

        suite.addTest(new CalculatedTableTest("testCreateTable"));
        suite.addTest(new CalculatedTableTest("testAddRows"));
        suite.addTest(new CalculatedTableTest("testGetElements"));

        return suite;
    }

    private static final TableDataCollection table = new StandardTableDataCollection(null, "test");

    public void testCreateTable() throws Exception
    {
        table.getColumnModel().addColumn("a", Integer.class);
        table.getColumnModel().addColumn("b", Integer.class);
        table.getColumnModel().addColumn("c", Integer.class, "a+b");
        assertEquals("Incorrect column count", table.getColumnModel().getColumnCount(), 3);
    }

    public void testAddRows() throws Exception
    {
        TableDataCollectionUtils.addRow(table, "row1", new Object[] {1, 2, 0});
        TableDataCollectionUtils.addRow(table, "row2", new Object[] {4, 6, 0});
        TableDataCollectionUtils.addRow(table, "row3", new Object[] {3, 1, 0});
        TableDataCollectionUtils.addRow(table, "row4", new Object[] {5, 3, 0});
        TableDataCollectionUtils.addRow(table, "row5", new Object[] {2, 3, 0});
        assertEquals("Incorrect table size", table.getSize(), 5);
    }

    public void testGetElements() throws Exception
    {
        JScriptContext.getContext();
        Scriptable scope = JScriptContext.getScope();
        Scriptable scriptable = Context.toObject(new JavaScriptData(), scope);
        scope.put("data", scope, scriptable);

        assertEquals("Incorrect calculated value at row1", table.getValueAt(0, 2), 3);
        assertEquals("Incorrect calculated value at row2", table.getValueAt(1, 2), 10);
        assertEquals("Incorrect calculated value at row3", table.getValueAt(2, 2), 4);
        assertEquals("Incorrect calculated value at row4", table.getValueAt(3, 2), 8);
        assertEquals("Incorrect calculated value at row5", table.getValueAt(4, 2), 5);
    }
}
