package biouml.plugins.sbml._test;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import biouml.plugins.simulation.SimulationEnginePane;
import ru.biosoft.jobcontrol.FunctionJobControl;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.plugins.simulation.ode.ImexSD;


/** Batch unit test for biouml.model package. */
public class SbmlJavaConsoleTest
{
    static final String testDirectory = "semantic-test-suite/";
    static final String rootDirectory = "./biouml/plugins/sbml/_test/";

    static void testSBML_JavaSimulator(String testDirectory, String testName, String sbmlLevel, double absTol, double relTol)
            throws Exception
    {
        // prepare logger
        try
        {
            DefaultTestLogger logger = new DefaultTestLogger("Java-console-" + sbmlLevel);

            SbmlTestSimulationEngineAdapter engineAdapter = new SbmlTestSimulationEngineAdapter();

            JavaSimulationEngine javaSimulationEngine = new JavaSimulationEngine();

            Logger log = Logger.getLogger(SimulationEnginePane.class.getName());
            FunctionJobControl jobControl = new FunctionJobControl( log );
            javaSimulationEngine.setJobControl(jobControl);

            //            DormandPrince solver = new DormandPrince();
            ImexSD solver = new ImexSD();
            javaSimulationEngine.setSolver(solver);
            javaSimulationEngine.setSolverName("Imex");

            //        engine.setSimulator(new EventLoopSimulator());
            javaSimulationEngine.setNeedToShowPlot(false);
            javaSimulationEngine.setOutputDir("../java_out");
            javaSimulationEngine.setNeedToShowPlot(false);
            engineAdapter.processSemanticSuiteTest(javaSimulationEngine, testDirectory, testName, sbmlLevel, logger, null);
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
    }

    static void printUsage()
    {
        System.out.println("USAGE: ./<progname> <testname> [-d <testdir>] [-a absTolerance] [-r relTolerance] <testname>");
    }

    public static void main(String[] args)
    {
        // Setup log
        File configFile = new File( rootDirectory + "log.lcf" );
        try
        {
            LogManager.getLogManager().readConfiguration( new FileInputStream( configFile ) );
        }
        catch( Exception e1 )
        {
            System.err.println( "Error init logging: " + e1.getMessage() );
        }

        try
        {
            Options options = new Options();
            options.addOption(new Option("d", true, "test duirectory"));
            options.addOption(new Option("a", true, "Java absolute tolerance value"));
            options.addOption(new Option("r", true, "Java relative tolerance value"));
            options.addOption(new Option("L", true, "SBML Level"));

            CommandLine commandLine = ( new PosixParser() ).parse(options, args);

            String[] _args = commandLine.getArgs();

            double absTol = commandLine.hasOption("a") ? Double.parseDouble(commandLine.getOptionValue("a")) : 0.0;
            double relTol = commandLine.hasOption("r") ? Double.parseDouble(commandLine.getOptionValue("r")) : 0.0;
            String sbmlLevel = commandLine.hasOption("L") ? ( commandLine.getOptionValue("L").equals("1") ? "-l1" : ( commandLine
                    .getOptionValue("L").equals("2") ? "-l2" : "" ) ) : "";

            // _args[0] is a test name
            testSBML_JavaSimulator( ( commandLine.hasOption("d") ? commandLine.getOptionValue("d") : "./" ), _args[0], sbmlLevel, absTol,
                    relTol);
        }
        catch( Exception ex )
        {
            System.out.println("Error occured: " + ex);
            ex.printStackTrace();
        }
    }
}
