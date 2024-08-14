package biouml.plugins.simulation_test._test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.LogManager;

import biouml.plugins.simulation_test.SemanticSimulatorTest;


public class RunSemanticSimulatorTest extends TestCase
{
    /** Standart JUnit constructor */
    public RunSemanticSimulatorTest(String name)
    {
        super(name);

        File configFile = new File( "biouml/plugins/simulation_test/_test/log.lcf" );
        try( FileInputStream fis = new FileInputStream( configFile ) )
        {
            LogManager.getLogManager().readConfiguration( fis );
        }
        catch( Exception e1 )
        {
            System.err.println( "Error init logging: " + e1.getMessage() );
        }
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(RunSemanticSimulatorTest.class.getName());
        suite.addTest(new RunSemanticSimulatorTest("runTests"));
        return suite;
    }


    public void runTests() throws Exception
    {
        new SemanticSimulatorTest().executeSemanticTests("SemanticTests");
    }
}
