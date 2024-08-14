package biouml.plugins.sbml._test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.logging.LogManager;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import biouml.model.Diagram;
import biouml.plugins.sbml.SbmlModelFactory;
import biouml.plugins.sbml._test.TestListParser.Category;

/** Batch unit test for biouml.model package. */
public class SbmlWriterTest extends TestCase
{
    static final String testDirectory = "semantic-test-suite/";
    static final String rootDirectory = "./biouml/plugins/sbml/_test/";

    /** Standart JUnit constructor */
    public SbmlWriterTest(String name)
    {
        super(name);

        // Setup log
        File configFile = new File( rootDirectory + "log.lcf" );
        try( FileInputStream fis = new FileInputStream( configFile ) )
        {
            LogManager.getLogManager().readConfiguration( fis );
        }
        catch( Exception e1 )
        {
            System.err.println( "Error init logging: " + e1.getMessage() );
        }
    }

    /** Make suite if tests. */
    public static Test suite()
    {
        TestSuite suite = new TestSuite(SbmlWriterTest.class.getName());
        suite.addTest(new SbmlWriterTest("testSBML_Writer"));
        return suite;
    }

    //////////////////////////////////////////////////////////////////
    // Test case
    //

    static String stripSlashes(String testName)
    {
        int index = testName.lastIndexOf("/");
        return testName.substring(index + 1);
    }

    public void testSBML_Writer() throws Exception
    {
        String name = "AUTOMATION/testlist.txt";

        File file = new File(rootDirectory + testDirectory + name);

        assertTrue("Can not find file: " + file.getCanonicalPath(), file.exists());

        List<Category> categories = (new TestListParser()).parseFile(
            new File(rootDirectory + testDirectory + name));

        Iterator<Category> iter = categories.iterator();

        PrintWriter log = new PrintWriter(new FileOutputStream(new File(rootDirectory + "test_writer.log")));

        while (iter.hasNext())
        {
            TestListParser.Category category = iter.next();
            String categoryName = category.name;

            try
            {
                Iterator<String> testIter = category.tests.iterator();
                while (testIter.hasNext())
                {
                    String testName = testIter.next();

                    try
                    {
                        File sbmlModelFile = new File(rootDirectory + testDirectory + testName + "-l2.xml");
                        Diagram diagram = SbmlModelFactory.readDiagram(sbmlModelFile, null, null);

                        File sbmlOutputlFile = new File(rootDirectory + testDirectory + testName + "_intermediate-l1.xml");
                        SbmlModelFactory.writeDiagram(sbmlOutputlFile, diagram);

                        File sbmlModelFile2 = new File(rootDirectory + testDirectory + testName + "_intermediate-l1.xml");
                        Diagram diagram2 = SbmlModelFactory.readDiagram(sbmlModelFile2, null, null);

                        File sbmlOutputlFile2 = new File(rootDirectory + testDirectory + testName + "_output-l1.xml");
                        SbmlModelFactory.writeDiagram(sbmlOutputlFile2, diagram2);
                    }
                    catch (Exception ex)
                    {
                        log.println("CATEGORY: " + categoryName + " TEST: " + testName + ": ");
                        ex.printStackTrace(log);
                        log.flush();
                    }
                }
            }
            catch (Exception ex)
            {
            }
        }
    }
}
