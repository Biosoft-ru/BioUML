package biouml.plugins.bionetgen._test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import javax.imageio.ImageIO;
import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;

import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.util.TextUtil2;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.model.dynamics.plot.PlotsInfo;
import biouml.model.util.DiagramImageGenerator;
import biouml.model.dynamics.plot.Curve;
import biouml.model.dynamics.plot.PlotInfo;
import biouml.plugins.bionetgen.diagram.BionetgenConstants;
import biouml.plugins.bionetgen.diagram.BionetgenUtils;
import biouml.plugins.bionetgen.diagram.BionetgenDiagramDeployer;
import biouml.plugins.simulation.ArraySpan;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.simulation.SimulationResult;
import biouml.standard.type.Reaction;
import biouml.standard.type.Specie;
import com.developmentontheedge.application.ApplicationUtils;

public class BionetgenSimulationTest extends BionetgenDiagramGeneratorTest
{
    public static final String STATUS = "status";
    public static final String SIMULATION_TIME = "time";
    public static final String SIMULATOR = "simulator";
    public static final String ZERO = "zero";
    public static final String STEP = "step";
    public static final String MESSAGES = "messages";

    public BionetgenSimulationTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(BionetgenSimulationTest.class.getName());

        suite.addTest( new BionetgenSimulationTest( "testBionetgenSimulation" ) );

        return suite;
    }

    private final String outDirectory = AbstractBioUMLTest.getTestDir() + "/results/";
    private final String imageDirectory = outDirectory + "images/";
    private int molCounter = 0;
    private int reactCounter = 0;

    public void testBionetgenSimulation() throws Exception
    {
        boolean runtimeException = false;
        BionetgenTestUtility.initPreferences();
        List<String> testList = ApplicationUtils.readAsList(new File(testDirectory + "testList"));
        File dir = new File(outDirectory);
        if( !dir.exists() && !dir.mkdir() )
            throw new RuntimeException("Failed to create output directory");
        dir = new File(imageDirectory);
        if( !dir.exists() && !dir.mkdir() )
            throw new RuntimeException("Failed to create image directory");
        try
        {
            for( String testName : testList )
            {
                molCounter = 0;
                reactCounter = 0;
                long time = 0;
                long[] convertTime = new long[2];
                convertTime[0] = System.currentTimeMillis();
                Diagram diagram = generateDiagram(testName, true);
                convertTime[0] = System.currentTimeMillis() - convertTime[0];
                writeImage(diagram, imageDirectory + testName + "-template");
                for( Node node : diagram.getNodes() )
                {
                    if( BionetgenUtils.isReaction(node) )
                        reactCounter--;
                    else if( BionetgenUtils.isSpecies(node) && BionetgenUtils.isStartType(node) )
                        molCounter--;
                }
                convertTime[1] = System.currentTimeMillis();
                diagram = BionetgenDiagramDeployer.deployBNGDiagram(diagram, true);
                convertTime[1] = System.currentTimeMillis() - convertTime[1];
                writeImage(diagram, imageDirectory + testName);

                HashMap<String, double[]> testResult = BionetgenTestUtility.readResults(testDirectory + testName + ".gdat");

                ArraySpan span = new ArraySpan(testResult.get("time"));

                JavaSimulationEngine engine = (JavaSimulationEngine)DiagramUtility.getPreferredEngine( diagram );
                if( engine == null )
                {
                    engine = new JavaSimulationEngine();
                    engine.setDiagram( diagram );
                    engine.setAbsTolerance(1e-8);
                    engine.setRelTolerance(1e-8);
                }
                engine.setOutputDir(outDirectory + "java_out/");
                engine.setSpan(span);

                SimulationResult result = new SimulationResult(null, testName + "_result");
                time = System.currentTimeMillis();
                engine.simulate(result);
                time = System.currentTimeMillis() - time;

                String[] varNames = testResult.keySet().toArray(new String[testResult.size()]);
                double[][] bioumlResult = StreamEx.of( varNames ).map( name -> result.getValues( new String[] {name} )[0] )
                        .toArray( double[][]::new );

                writeSimulationResult(varNames, bioumlResult, outDirectory + testName + "-results.csv");
                writeInfoFile(outDirectory, engine, time);
                writeSettingsAndModelFiles(engine, outDirectory, molCounter, reactCounter, convertTime);
            }
        }
        catch( Throwable t )
        {
            runtimeException = true;
            throw ExceptionRegistry.translateException( t );
        }
        finally
        {
            deleteUnnecessaryFiles(outDirectory + "java_out/", runtimeException);
        }
        BionetgenStatisticsCalculator sc = new BionetgenStatisticsCalculator( testDirectory, outDirectory );
        sc.generateStatisticGroup();
        assertEquals( sc.getFailedTestsNumber() + " tests of " + testList.size() + " failed.", 0,
                sc.getFailedTestsNumber() );
    }

    public static void deleteUnnecessaryFiles(String outDir, boolean deleteJavaFiles)
    {
        File javaOutDir = new File(outDir);
        File[] files = javaOutDir.listFiles();
        if( files == null )
            return;
        for( File file : files )
        {
            if( file.isDirectory() )
            {
                deleteUnnecessaryFiles(file.getPath(), true);
                if( !file.delete() )
                    file.deleteOnExit();
            }
            else if( file.getName().endsWith(".class") && !file.delete() )
                file.deleteOnExit();
            else if( deleteJavaFiles && file.getName().endsWith(".java") && !file.delete() )
                file.deleteOnExit();
        }
    }

    public static void writeImage(Diagram diagram, String fileName) throws IOException
    {
        BufferedImage bi;
        File image = new File(fileName + ".png");
        if( diagram.getSize() < 1000 )
            bi = DiagramImageGenerator.generateDiagramImage( diagram, 1.0, true );
        else if( diagram.getSize() < 4000 )
            bi = DiagramImageGenerator.generateDiagramImage( diagram, 0.3, true );
        else
            bi = DiagramImageGenerator.generateDiagramImage( diagram, 0.1, true );
        ImageIO.write(bi, "PNG", image);
        bi.flush();
    }

    public static void writeSettingsAndModelFiles(JavaSimulationEngine engine, String path, int molNumber, int reactNumber, long[] time)
            throws IOException
    {
        Diagram diagram = engine.getDiagram();
        String name = TextUtil2.split(diagram.getName(), '.')[0];
        try (PrintWriter pw = new PrintWriter( new File( path + name + "-settings.txt" ), StandardCharsets.UTF_8.toString() ))
        {
            pw.println( "start: " + engine.getInitialTime() );
            pw.println( "duration: " + ( engine.getCompletionTime() - engine.getInitialTime() ) );
            pw.println( "steps: " + ( ( engine.getCompletionTime() - engine.getInitialTime() ) / engine.getTimeIncrement() ) );
            pw.println( "absolute: " + engine.getAbsTolerance() );
            pw.println( "relative: " + engine.getRelTolerance() );
            StringBuilder sb = new StringBuilder();
            PlotsInfo plotsInfo = DiagramUtility.getPlotsInfo( diagram );
            for( PlotInfo pi : plotsInfo.getPlots() )
            {
                for( Curve curve : pi.getYVariables() )
                    sb.append( curve.getName() ).append( ", " );
            }
            pw.println( "Observables: " + sb.toString().substring( 0, sb.toString().length() - 2 ) );
            pw.println( "Species in bngl model: " + ( -molNumber ) );
            pw.println( "Reaction templates in bngl model: " + ( -reactNumber ) );
            int reactionCounter = 0;
            int moleculeCounter = molNumber;
            for( Node node : diagram.getNodes() )
            {
                if( node.getKernel() instanceof Reaction )
                    reactionCounter++;
                else if( node.getKernel() instanceof Specie )
                    moleculeCounter++;
            }
            pw.println( "Species generated: " + moleculeCounter );
            pw.println( "Reactions generated: " + reactionCounter );
            pw.println( "Template model creation time: " + ( time[0] / 1000.0 ) + " s." );
            pw.println( "Result model generation time: " + ( time[1] / 1000.0 ) + " s." );
        }

        if( diagram.getComment() != null && !diagram.getComment().isEmpty() )
        {
            File model = new File(path + name + "-model.html");
            ApplicationUtils.writeString(model, diagram.getComment());
        }
    }

    public static void writeSimulationResult(String[] varNames, double[][] values, String name) throws IOException
    {
        try (PrintWriter pw = new PrintWriter( new File( name ), StandardCharsets.UTF_8.toString() ))
        {
            pw.println( String.join( " ", varNames ) );

            IntStreamEx.ofIndices( values[0] ).mapToObj( row -> StreamEx.of( values ).mapToDouble( vals -> vals[row] ).joining( " " ) )
                    .forEach( pw::println );
        }
    }

    protected static void writeInfoFile(String directory, JavaSimulationEngine engine, long time) throws Exception
    {
        String solverName = engine.getSimulator().getInfo().name;
        String name = TextUtil2.split( engine.getDiagram().getName(), '.' )[0];
        File target = new File(directory, name + ".info");
        if( !target.getParentFile().exists() && !target.getParentFile().mkdirs() )
            throw new Exception("Failed to create necessary directories");

        /* this data will be used to calculate statistics
         * e.g. this atol and rtol will be used in comparison with results
         * so they can not be the same as atol and rtol for simulation
         */
        double atol = Math.min( engine.getAbsTolerance() * 1e+4, 1e-6 );
        double rtol = Math.min( engine.getRelTolerance() * 1e+4, 1e-5 );
        try (PrintWriter pw = new PrintWriter( target, StandardCharsets.UTF_8.toString() ))
        {
            pw.println( SIMULATION_TIME + "=" + time );
            pw.println( SIMULATOR + "=" + solverName );
            pw.println( BionetgenConstants.ATOL_PARAM + "=" + atol );
            pw.println( BionetgenConstants.RTOL_PARAM + "=" + rtol );
            pw.println( STEP + "=" + engine.getTimeIncrement() );
        }
    }
}
