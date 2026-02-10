package biouml.plugins.wdl._test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.plugins.wdl.cwl.CWLGenerator;
import biouml.plugins.wdl.cwl.CWLParser;
import biouml.plugins.wdl.cwl.CWLRunner;
import biouml.plugins.wdl.WorkflowSettings;
import biouml.plugins.wdl.diagram.WDLLayouter;
import ru.biosoft.util.StreamGobbler;
import ru.biosoft.util.TempFiles;

public class TestCWL
{

    private File testsDir;
    private File resultsDir;

    private static String[] list = new String[] {"hello", "scatter_range_2_steps", "scatter_simple", "scatter_range", "scatter_range2",
            "two_steps", "four_steps"};

    public static void main(String ... args) throws Exception
    {
        TestCWL tester = new TestCWL();
        tester.init( TestCWL.class.getResource( "../test_examples/cwl/" ) );
        tester.test( "aggregate" );
    }

    public void test(String name) throws Exception
    {
        String originalCWL = ApplicationUtils.readAsString( new File( testsDir, name + ".cwl" ) );
        Diagram diagram = new CWLParser().loadDiagram( originalCWL, null );
        new WDLLayouter().layout( diagram );
        TestWDL.exportImage( diagram, new File( resultsDir, name + ".png" ) );
        String cwl = new CWLGenerator().generate( diagram );

        File generatedCWLFile = new File( resultsDir, name + ".cwl" );
        ApplicationUtils.writeString( generatedCWLFile, cwl );
        System.out.println( cwl );
        validate( generatedCWLFile, resultsDir );
    }

    public void init(URL url) throws Exception
    {
        File suiteDir = new File( url.toURI() );
        testsDir = suiteDir;//new File( suiteDir, "tests" );
        resultsDir = new File( suiteDir, "results" );
        TestUtil.deleteDir( resultsDir );
        resultsDir.mkdir();
    }

    private static void runCWL(String name, String script)
    {
        runCWL( name, script, new ArrayList<>() );
    }

    private static int validate(File cwl, File outputDir) throws Exception
    {
        Process process = createProcess( cwl, outputDir );
        StringWriter infoWriter = new StringWriter();
        int code = logProcess2( process, infoWriter );
        if( code == 0 )
            System.out.println( "Success!" );
        else
            System.out.println( infoWriter.toString() );
        return code;
    }

    public static int logProcess2(Process process, Writer writer) throws Exception
    {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit( () -> {

            try (BufferedReader reader = new BufferedReader( new InputStreamReader( process.getInputStream() ) ))
            {
                String line;
                while( ( line = reader.readLine() ) != null )
                    writer.append( "\n" + line );
            }
            return "";
        } );
        int exitCode = process.waitFor();
        process.destroy();
        executor.shutdown();
        return exitCode;
    }

    public static int logProcess(Process process, Writer writer, Writer errorWriter) throws Exception
    {
        StreamGobbler inputReader = new StreamGobbler( process.getInputStream(), true );
        StreamGobbler errorReader = new StreamGobbler( process.getErrorStream(), true );
        process.waitFor();

        String outStr = inputReader.getData();
        if( !outStr.isEmpty() )
            writer.append( outStr );

        String errorStr = errorReader.getData();
        if( !errorStr.isEmpty() )
            errorWriter.append( errorStr );
        return process.exitValue();
    }

    private static Process createProcess(File cwl, File outputDir) throws IOException
    {
        boolean isWindows = System.getProperty( "os.name" ).startsWith( "Windows" );
        List<String> commands = new ArrayList<>();
        if( isWindows )
        {
            commands.add( "wsl" );
            commands.add( "--cd" );
            commands.add( cwl.getParentFile().getAbsolutePath().replace( "\\", "/" ) );
        }
        commands.add( "cwltool" );
        commands.add( "--validate" );
        commands.add( cwl.getName());
        ProcessBuilder builder = new ProcessBuilder( commands.toArray( String[]::new ) );

        if( !isWindows )
            builder.directory( outputDir );
        builder.redirectErrorStream( true );
        builder.environment().put( "PYTHONUNBUFFERED", "1" );
        Process process = builder.start();
        return process;
    }

    private static void runCWL(String name, String script, List<String> imports)
    {
        try
        {
            String outputDir = TempFiles.path( "cwl" ).getAbsolutePath();
            new File( outputDir ).mkdirs();

            for( String imported : imports )
            {
                URL url = TestWDL.class.getResource( "../test_examples/cwl/" + imported + ".cwl" );
                if( url == null )
                    throw new IllegalArgumentException( "No input file exists: " + name );

                File file = new File( url.getFile() );
                File copy = new File( outputDir, file.getName() );
                ApplicationUtils.copyFile( copy, file );
            }

            File f = new File( outputDir, name + ".cwl" );
            ApplicationUtils.writeString( f, script );
            Process process;
            URL url = TestWDL.class.getResource( "../test_examples/cwl/" + name + ".json" );
            if( url != null )
            {
                File parametersFile = new File( url.getFile() );
                File paremetersCopy = new File( outputDir, parametersFile.getName() );
                ApplicationUtils.copyFile( paremetersCopy, parametersFile );
                String content = ApplicationUtils.readAsString( parametersFile );
                Set<String> files = WorkflowSettings.getFileInputs( content );
                for( String file : files )
                {
                    URL inputURL = TestWDL.class.getResource( "../test_examples/cwl/" + file );
                    File inputFile = new File( inputURL.getFile() );
                    File copy = new File( outputDir, inputFile.getName() );
                    ApplicationUtils.copyFile( copy, inputFile );
                }
                process = CWLRunner.run( f.getName(), name + ".json", outputDir, false );
            }
            else
            {
                process = CWLRunner.run( f.getName(), null, outputDir, false );
            }
            CWLRunner.logProcess( process );
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
    }
}
