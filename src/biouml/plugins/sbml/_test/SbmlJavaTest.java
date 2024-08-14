package biouml.plugins.sbml._test;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.LogManager;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import biouml.model.Diagram;
import biouml.plugins.sbml.SbmlModelFactory;
import biouml.plugins.simulation.OdeSimulationEngine;
import biouml.plugins.simulation.java.JavaSimulationEngine;

/** Batch unit test for biouml.model package. */
public class SbmlJavaTest
    extends TestCase
{
    /** Standart JUnit constructor */
    public SbmlJavaTest(String name)
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
    public static Test suite()
    {
        TestSuite suite = new TestSuite(SbmlReaderTest.class.getName());
  //      suite.addTest(new SbmlJavaTest("testJavaBig"));
        return suite;
    }

    //////////////////////////////////////////////////////////////////
    // Test cases
    //

    protected void testJavaModel(String name) throws Exception
    {
        File file = new File("./biouml/plugins/sbml/_test/" + name + ".xml");
        assertTrue("Can not find file: " + file.getCanonicalPath(), file.exists());

        Diagram diagram = SbmlModelFactory.readDiagram(file, null, null);

        OdeSimulationEngine java = new JavaSimulationEngine();
        java.setOutputDir("./biouml/plugins/sbml/_test/");
        java.setDiagram(diagram);
        java.generateModel(true);

    }

    public void testJavaBig() throws Exception
    {
        testJavaModel("100Yeast");
    }

    public void testJava_Tyson() throws Exception
    {
        testJavaModel("tyson2");
    }

    public void testJava_singleGene() throws Exception
    {
        testJavaModel("singleGene");
    }

    public void testJava_SBML21() throws Exception
    {
        testJavaModel("semantic-test-suite/algebraicRules/fastReactionExample/algebraicRules-fastReactionExample-l2");
    }

}


