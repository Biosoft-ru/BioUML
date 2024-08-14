package ru.biosoft.access._test;

import java.io.File;
import ru.biosoft.access.JDBM2Index;
import ru.biosoft.util.TempFiles;
import junit.framework.TestSuite;

public class JDBM2IndexTest extends IndexTest
{

    public JDBM2IndexTest(String name)
    {
        super(name);
    }
    
    @Override
    public void setUp() throws Exception
    {
        File indexFile = TempFiles.dir("JDBM2Index");
        index = new JDBM2Index(indexFile);
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(JDBM2IndexTest.class.getName());
        suite.addTest(new JDBM2IndexTest("testPutRemovePut"));
        suite.addTest(new JDBM2IndexTest("testPut"));
        suite.addTest(new JDBM2IndexTest("testIterate"));
        suite.addTest(new JDBM2IndexTest("testRandomAccess"));
        return suite;
    }
 
}
