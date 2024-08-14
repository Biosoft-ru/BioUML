package ru.biosoft.table._test;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.table.TableDataCollection;

/**
 * Test table load test
 */
public class TableLoadSpeedTest extends TestCase
{
    public static final String repositoryPath = "../data_resources";
    public static final DataElementPath dcPath = DataElementPath.create("data/microarray");
    public static final String elementName = "table011_5.fac";
    //public static final String elementName = "Selivanova_mas5.fac";
    
    public TableLoadSpeedTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TableLoadSpeedTest.class.getName());

        suite.addTest(new TableLoadSpeedTest("testLoad"));

        return suite;
    }

    public static void main(String[] args) throws Exception
    {
        junit.textui.TestRunner.run(suite());
    }

    public void testLoad() throws Exception
    {
        List<DataElement> results = new ArrayList<>();
        CollectionFactory.createRepository(repositoryPath);
        DataCollection baseCollection = dcPath.getDataCollection();
        startFreeSize = Runtime.getRuntime().freeMemory();
        
        long time = System.currentTimeMillis();
        DataElement de = baseCollection.get(elementName);

        System.out.println("Load time:\t" + ( System.currentTimeMillis() - time ) + " Memory: " + getMemorySize()+"kb");
        assertNotNull("Element not found: " + elementName, de);

        TableDataCollection table = (TableDataCollection)de;

        System.out.println("Table size = " + table.getSize());

        time = System.currentTimeMillis();
        for( int i = 100; i < 200; i++ )
        {
            results.add(table.getAt(i));
        }
        System.out.println("Get 100 time:\t" + ( System.currentTimeMillis() - time ) + " Memory: " + getMemorySize()+"kb");

        time = System.currentTimeMillis();
        for( int i = 1000; i < 2000; i++ )
        {
            results.add(table.getAt(i));
        }
        System.out.println("Get 1000 time:\t" + ( System.currentTimeMillis() - time ) + " Memory: " + getMemorySize()+"kb");

        time = System.currentTimeMillis();
        for( int i = 10000; i < 20000; i++ )
        {
            results.add(table.getAt(i));
        }
        System.out.println("Get 10000 time:\t" + ( System.currentTimeMillis() - time ) + " Memory: " + getMemorySize()+"kb");

        table.close();
    }

    protected long startFreeSize;
    protected long getMemorySize()
    {
        return ( startFreeSize - Runtime.getRuntime().freeMemory() )/1024;
    }
}
