package ru.biosoft.table.csv._test;

import java.io.File;
import junit.framework.TestCase;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.csv.TableCSVReader;

public class TableCSVReaderTest extends TestCase
{
    public TableCSVReaderTest(String name)
    {
        super(name);
    }

    public void testReadTable() throws Exception
    {
        File testFile = new File("ru\\biosoft\\table\\csv\\_test\\test.csv");
        TableCSVReader reader = new TableCSVReader();
        TableDataCollection tdc = reader.readTable(null, "test", testFile);
        assertNotNull("Table is null", tdc);
        assertEquals("Incorrect table size", tdc.getSize(), 20);
        Object[] objects = TableDataCollectionUtils.getRowValues(tdc, tdc.getName(3));
        assertEquals("Incorrect row elements count", objects.length, 15);
    }
}
