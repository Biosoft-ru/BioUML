package biouml.plugins.bionetgen.bnglparser._test;

import java.io.File;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import biouml.plugins.bionetgen._test.BionetgenTestUtility;
import biouml.plugins.bionetgen.bnglparser.BNGStart;
import biouml.plugins.bionetgen.diagram.BionetgenTextGenerator;

import com.developmentontheedge.application.ApplicationUtils;

public class BionetgenParserTest extends AbstractBioUMLTest
{
    public BionetgenParserTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(BionetgenParserTest.class.getName());

        suite.addTest(new BionetgenParserTest("testParsing"));
        suite.addTest(new BionetgenParserTest("testTextCompare"));

        return suite;
    }

    protected String dir = "../src/biouml/plugins/bionetgen/_test/test_suite/models/";
    private List<String> testList;

    public void testParsing() throws Exception
    {
        initTestList();
        for( String test : testList )
            BionetgenTestUtility.readDiagram(dir + test + ".bngl", test);
    }

    public void testTextCompare() throws Exception
    {
        initTestList();
        for( String test : testList )
            compare(dir + test + ".bngl", test);
    }

    private void compare(String fileName, String modelName) throws Exception
    {
        BNGStart start = BionetgenTestUtility.readDiagram(fileName, modelName);
        BionetgenTextGenerator textGenerator = new BionetgenTextGenerator(start);
        File testFile = BionetgenTestUtility.createTestFile(dir, textGenerator.generateText());
        assertFileEquals(modelName, new File(fileName), testFile);
        assertTrue("Failed to delete test file", testFile.delete());
    }

    private void initTestList() throws Exception
    {
        testList = ApplicationUtils.readAsList(new File(dir + "testList"));
        testList.add("blbr");
        testList.add("fceri_fyn_lig");
        testList.add("glycobiology");
        testList.add("egfr_simple");
    }

}
