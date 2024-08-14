package biouml.plugins.simulation_test._test;

import java.io.BufferedWriter;
import java.io.File;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutionException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.exception.InternalException;
import ru.biosoft.access.task.TaskPool;
import biouml.model.Diagram;
import biouml.plugins.sbml.SbmlModelFactory;
import biouml.plugins.sbml.validation.SBMLValidator;
import biouml.plugins.simulation_test.SemanticTestListParser;

import com.developmentontheedge.application.ApplicationUtils;

public class ReaderWriterTest extends TestCase
{
    private static final String testDirectory = "../data_resources/SBML tests/cases/semantic/";
    private static final String outDirectory = "../data_resources/SBML tests/cases/validation/";

    public ReaderWriterTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(ReaderWriterTest.class.getName());
        suite.addTest(new ReaderWriterTest("validate"));
        return suite;
    }

    public void validate() throws InterruptedException, ExecutionException
    {
        final List<String> failedTests = new Vector<>();
        File outDir = new File(outDirectory);
        ApplicationUtils.removeDir(outDir);
        if( !outDir.mkdirs() )
            throw new InternalException("Failed to create output directory: " + outDirectory);
        List<String> testList = SemanticTestListParser.parseTestList( new File( testDirectory + "testList.txt" ) );
        TaskPool.getInstance().iterate( testList, testName -> {
            try
            {
                int indexOf = testName.indexOf("/");
                String shortName = indexOf != -1 ? testName.substring( 0, indexOf ) : testName;
                String modelPath = testDirectory + testName + "-sbml-l3v1.xml";
                File sbmlModelFile = new File(modelPath);
                File writtenFile = new File(outDirectory + shortName + "-sbml-l3v1.xml");
                Diagram d = SbmlModelFactory.readDiagram(sbmlModelFile, null, null);
                SbmlModelFactory.writeDiagram(writtenFile, d);
                String report = SBMLValidator.validateSBML(writtenFile);
                if( report != null )
                {
                    failedTests.add(testName);
                    System.out.println("Failed: "+testName);
                    try (BufferedWriter bw = ApplicationUtils.utfWriter(outDirectory + "report_" + shortName + "-sbml-l3v1.txt"))
                    {
                        bw.write(report);
                    }
                }
                else
                    System.out.println("Success: "+testName);
            }
            catch( Exception e )
            {
                e.printStackTrace();
            }
        } );
        if(!failedTests.isEmpty())
        {
            fail("Failed tests: "+failedTests);
        }
    }
}
