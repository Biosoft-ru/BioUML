package biouml.plugins.sbml._test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.Iterator;
import java.util.List;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import biouml.plugins.sbml._test.TestListParser.Category;
import biouml.plugins.simulation.OdeSimulationEngine;
import biouml.plugins.simulation.SimulationEnginePane;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.plugins.simulation.ode.DormandPrince;
import biouml.plugins.simulation.ode.ImexSD;

import ru.biosoft.jobcontrol.FunctionJobControl;

/** Batch unit test for biouml.model package. */
public class SbmlSemanticTest extends TestCase
{
    static String testDirectory      = "../test/semantic-test-suite/";
    static String outDirectory       = "../test/semantic-results/";
    static String javaOutDirectory   = "../test/semantic-results/java/";
    static String matlabOutDirectory = "C:/MATLAB6p5/work";

    /** Standart JUnit constructor */
    public SbmlSemanticTest(String name)
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
        TestSuite suite = new TestSuite(SbmlSemanticTest.class.getName());

//        suite.addTest(new SbmlSemanticTest("testJavaSimulationEngine_DP_11"));
//        suite.addTest(new SbmlSemanticTest("testJavaSimulationEngine_DP_21"));
//        suite.addTest(new SbmlSemanticTest("testJavaSimulationEngine_Imex_11"));
    //very long test
    //    suite.addTest(new SbmlSemanticTest("testJavaSimulationEngine_Imex_21"));

        return suite;
    }

    public void testJavaSimulationEngine_DP_11() throws Exception
    {
        Logger log = Logger.getLogger( SimulationEnginePane.class.getName() );

        JavaSimulationEngine engine = new JavaSimulationEngine();
        FunctionJobControl jobControl = new FunctionJobControl(log);
        (engine).setJobControl(jobControl);

        DormandPrince solver = new DormandPrince();
        engine.setSolver(solver);

//        engine.setSimulator(new EventLoopSimulator());
        (new File(javaOutDirectory)).mkdirs();
        engine.setOutputDir(javaOutDirectory);
        engine.setNeedToShowPlot(false);
        testSimulationEngine("AUTOMATION/testlist-11.txt", "Java-Dormand-Prince-l1", engine, "-l1");
    }

    public void testJavaSimulationEngine_DP_21() throws Exception
    {
        Logger log = Logger.getLogger( SimulationEnginePane.class.getName() );

        JavaSimulationEngine engine = new JavaSimulationEngine();
        FunctionJobControl jobControl = new FunctionJobControl(log);
        (engine).setJobControl(jobControl);

        DormandPrince solver = new DormandPrince();
        engine.setSolver(solver);
//        engine.setSimulator(new EventLoopSimulator());
        (new File(javaOutDirectory)).mkdirs();
        engine.setOutputDir(javaOutDirectory);
        engine.setNeedToShowPlot(false);
        testSimulationEngine("AUTOMATION/testlist-21.txt", "Java-Dormand-Prince-l2", engine, "-l2");
    }

    public void testJavaSimulationEngine_Imex_11() throws Exception
    {
        Logger log = Logger.getLogger( SimulationEnginePane.class.getName() );

        JavaSimulationEngine engine = new JavaSimulationEngine();
        FunctionJobControl jobControl = new FunctionJobControl(log);
        (engine).setJobControl(jobControl);

        engine.setSolver( new ImexSD() );
//        engine.setSimulator(new ImexSD());
        engine.setSolverName("Imex");
        (new File(javaOutDirectory)).mkdirs();
        engine.setOutputDir(javaOutDirectory);
        engine.setNeedToShowPlot(false);
        testSimulationEngine("AUTOMATION/testlist-11.txt", "Java-Imex-l1", engine, "-l1");
    }

    public void testJavaSimulationEngine_Imex_21() throws Exception
    {
        Logger log = Logger.getLogger( SimulationEnginePane.class.getName() );

        JavaSimulationEngine engine = new JavaSimulationEngine();
        FunctionJobControl jobControl = new FunctionJobControl(log);
        (engine).setJobControl(jobControl);

//        engine.setSimulator(new ImexSD());
        engine.setSolver( new ImexSD() );
        engine.setSolverName("Imex");
        (new File(javaOutDirectory)).mkdirs();
        engine.setOutputDir(javaOutDirectory);
        engine.setNeedToShowPlot(false);
        testSimulationEngine("AUTOMATION/testlist-21.txt", "Java-Imex-l2", engine, "-l2");
    }


    //////////////////////////////////////////////////////////////////
    // Main routine
    //

    public void testSimulationEngine(String testListName, String title,
                                     OdeSimulationEngine engine, String sbmlLevel) throws Exception
    {

        File file = new File(testDirectory + testListName);

        assertTrue("Can not find file: " + file.getCanonicalPath(), file.exists());

        List<Category> categories = (new TestListParser()).parseFile(new File(testDirectory + testListName));

        FileWriter writer = new FileWriter(outDirectory + "SemanticTests-" + title + ".html");
        TestLogger logger = new HtmlSemanticTestStatisticsLogger(title, categories, writer);

        SbmlTestSimulationEngineAdapter engineAdapter = new SbmlTestSimulationEngineAdapter();

        Iterator<Category> iter = categories.iterator();
        while ( iter.hasNext() )
        {
            TestListParser.Category category = iter.next();
            String categoryName = category.name;
            logger.categoryStarted(categoryName);

            Iterator<String> testIter = category.tests.iterator();
            while (testIter.hasNext())
            {
                String testName = testIter.next();
                try
                {
                    ((HtmlSemanticTestStatisticsLogger)logger).setSimulationEngineName(engine.getEngineDescription());
                    engineAdapter.processSemanticSuiteTest(engine, testDirectory, testName, sbmlLevel, logger, outDirectory);
                }
                catch (Exception e)
                {
                    logger.error(e);
                    logger.testCompleted();
                }
            }
        }

        logger.complete();
    }
}