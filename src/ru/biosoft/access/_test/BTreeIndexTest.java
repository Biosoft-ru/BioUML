package ru.biosoft.access._test;

import java.io.File;

import ru.biosoft.access.BTreeIndex;
import ru.biosoft.util.TempFiles;
import junit.framework.TestSuite;

public class BTreeIndexTest extends IndexTest
{
    public BTreeIndexTest(String name)
    {
        super(name);
    }
    
    @Override
    public void setUp() throws Exception
    {
        File dataFile = TempFiles.file("data.txt");
        index = new BTreeIndex(dataFile, "id", dataFile.getAbsolutePath());
        dataFile.delete();
    }
    
    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(BTreeIndexTest.class.getName());
        suite.addTest(new BTreeIndexTest("testPutRemovePut"));
        suite.addTest(new BTreeIndexTest("testPut"));
        suite.addTest(new BTreeIndexTest("testIterate"));
        suite.addTest(new BTreeIndexTest("testRandomAccess"));
        return suite;
    }

}
