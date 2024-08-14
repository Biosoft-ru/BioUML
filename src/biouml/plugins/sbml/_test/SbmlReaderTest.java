package biouml.plugins.sbml._test;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.LogManager;

import ru.biosoft.access._test.AbstractBioUMLTest;

import junit.framework.TestSuite;
import biouml.model.Diagram;
import biouml.plugins.sbml.SbmlModelFactory;

/** Batch unit test for biouml.model package. */
public class SbmlReaderTest extends AbstractBioUMLTest
{
    /** Standart JUnit constructor */
    public SbmlReaderTest(String name)
    {
        super(name);

        // Setup log
        File configFile = new File( "./biouml/plugins/sbml/_test/log.lcf" );
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
    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(SbmlReaderTest.class.getName());

        //suite.addTest(new SbmlReaderTest("testComposite"));
//        suite.addTest(new SbmlReaderTest("testStub"));

        suite.addTest(new SbmlReaderTest("testReadModel_Tyson"));
        suite.addTest(new SbmlReaderTest("testReadModel_Tyson"));

        suite.addTest(new SbmlReaderTest("testReadModel_Glycolysis"));
        suite.addTest(new SbmlReaderTest("testReadModel_Ip3"));

        suite.addTest(new SbmlReaderTest("testReadModel_Simple_l2v2"));

        return suite;
    }

    //////////////////////////////////////////////////////////////////
    // Test cases
    //

    public void testComposite() throws Exception
    {
        testModel("01173-sbml-l3v1");
    }

    public void testReadModel_Tyson() throws Exception
    {
        testModel("tyson2");
        testModel("tyson2_");
    }

    public void testReadModel_Glycolysis() throws Exception
    {
        testModel("glycolysis1");
    }

    public void testReadModel_Ip3() throws Exception
    {
        testModel("ip3");
    }

    public void testReadModel_Simple_l2v2() throws Exception
    {
        testModel("simple_l2v2");
    }

    protected void testModel(String name) throws Exception
    {
        File file = new File("./biouml/plugins/sbml/_test/" + name + ".xml");
        assertTrue("Can not find file: " + file.getCanonicalPath(), file.exists());

        long start = System.currentTimeMillis();
        Diagram diagram = SbmlModelFactory.readDiagram(file, null, null);
        long readTime = System.currentTimeMillis() - start;

        file = new File(file.getParent() + "/" + name + "_.xml");
        start = System.currentTimeMillis();
        SbmlModelFactory.writeDiagram(file, diagram);
        long writeTime = System.currentTimeMillis() - start;

        System.out.println("Model " + name + " reading time: " + readTime + ", writingTime: " + writeTime);
    }
}
