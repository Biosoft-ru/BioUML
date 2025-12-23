package biouml.plugins.sbml._test;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.LogManager;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.plugins.sbml.SbmlModelFactory;
import biouml.plugins.simulation.OdeSimulatorOptions;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.plugins.simulation.java.RunTimeCompiler;
import biouml.standard.simulation.ResultListener;

/** Batch unit test for biouml.model package. */
public class SbmlJavaMain
{
    public static void main(String[] args)
    {
        File configFile = new File( "./biouml/plugins/sbml/_test/log.lcf" );
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
            testJavaModel(args);
        }
        catch( IllegalArgumentException ex )
        {
            System.err.println(ex.getMessage());
        }
        catch( Exception ex )
        {
            System.out.println("Error occured: " + ex);
            ex.printStackTrace();
        }
    }

    protected static void testJavaModel(String[] args) throws Exception
    {
        Options options = new Options();
        options.addOption(new Option("l", false, "load model"));
        options.addOption(new Option("g", false, "generate model"));
        options.addOption(new Option("c", false, "compile model"));
        options.addOption(new Option("s", false, "simulate"));
        options.addOption(new Option("r", false, "save simualtion results"));

        options.addOption(new Option("f", true, "file name (without extension"));
        options.addOption(new Option("d", true, "output directory, default ../java_out"));
        options.addOption(new Option("a", true, "Java absolute tolerance value"));
        options.addOption(new Option("r", true, "Java relative tolerance value"));

        CommandLine commandLine = ( new PosixParser() ).parse(options, args);

        String defValue = null;
        String name = commandLine.getOptionValue( "f", defValue );
        if( name == null )
        {
            throw new IllegalArgumentException("File name is not specified.");
        }

        String outputDir = commandLine.getOptionValue("d", "../java_out");

        JavaSimulationEngine java = new JavaSimulationEngine()
        {
            @Override
            public String simulate(File[] files, boolean compile, ResultListener[] listeners) throws Exception
            {
                modelType = EModel.ODE_TYPE;

                return super.simulate(files, compile, null);
            }

            @Override
            public double getInitialTime()
            {
                return 0;
            }
            @Override
            public double getCompletionTime()
            {
                return 1;
            }

        };

        java.setOutputDir(outputDir);
        java.setNeedToShowPlot(false);

        long start;

        Diagram diagram = null;

        double absTolerance = 1e-6;
        double relTolerance = 1e-3;

        if( commandLine.hasOption("a") )
            ( (OdeSimulatorOptions)java.getSimulator().getOptions() ).setAtol(Double.parseDouble(commandLine.getOptionValue("a")));

        if( commandLine.hasOption("r") )
            ( (OdeSimulatorOptions)java.getSimulator().getOptions() ).setRtol(Double.parseDouble(commandLine.getOptionValue("r")));

        if( commandLine.hasOption("l") )
        {

            File file = new File(name + ".xml");
            if( !file.exists() )
                file = new File("biouml/plugins/sbml/_test/" + name + ".xml");

            System.out.println("Load model: " + file.getAbsolutePath());

            start = System.currentTimeMillis();
            diagram = SbmlModelFactory.readDiagram(file, null, null);
            java.setDiagram(diagram);
            System.out.println("loading time: " + ( System.currentTimeMillis() - start ) / 1000);
        }

        if( commandLine.hasOption("g") )
        {
            start = System.currentTimeMillis();
            java.generateModel(true);
            System.out.println("model generation time: " + ( System.currentTimeMillis() - start ) / 1000);
        }

        if( commandLine.hasOption("c") )
        {
            File file = new File(outputDir + "/" + name + ".java");
            if( !file.exists() )
                file = new File(outputDir + "/a" + name + ".java");

            RunTimeCompiler comp = new RunTimeCompiler(outputDir, outputDir, new String[] {file.getAbsolutePath()});
            start = System.currentTimeMillis();
            comp.execute();
            System.out.println("compiling time: " + ( System.currentTimeMillis() - start ) / 1000);
        }

        if( commandLine.hasOption("s") )
        {
            File file = new File(outputDir + "/" + name + ".java");
            if( !file.exists() )
                file = new File(outputDir + "/a" + name + ".java");

            System.out.println("Simulate, class: " + file.getAbsolutePath() + ", name=" + file.getName());

            start = System.currentTimeMillis();
            String status = java.simulate(new File[] {file}, false, null);
            System.out.println("simulation time: " + ( System.currentTimeMillis() - start ) / 1000 + ", status=" + status);
        }

    }
}
