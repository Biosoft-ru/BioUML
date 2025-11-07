package biouml.plugins.wdl;

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.TextDataElement;
import ru.biosoft.util.StreamGobbler;

public class CWLRunner
{

    private static final Logger log = Logger.getLogger( CWLRunner.class.getName() );


    public static Process run(String fileName, String parametersName, String outputDir, boolean useWsl) throws IOException
    {
        List<String> commands = new ArrayList<>();
        if( useWsl )
        {
            String parent = new File( outputDir ).getAbsolutePath().replace( "\\", "/" );
            commands.add( "wsl" );
            commands.add( "--cd" );
            commands.add( parent );
        }
        commands.add( "cwltool" );
        commands.add( "--outdir" );
        commands.add( "./cwl_results" );
        commands.add( fileName );
        if( parametersName != null )
            commands.add( parametersName );

        ProcessBuilder builder = new ProcessBuilder( commands.toArray( String[]::new ) );

        if( !useWsl )
            builder.directory( new File( outputDir ) );

        return builder.start();
    }

    public static void logProcess(Process process) throws Exception
    {
        StreamGobbler inputReader = new StreamGobbler( process.getInputStream(), true );
        StreamGobbler errorReader = new StreamGobbler( process.getErrorStream(), true );
        process.waitFor();

        if( process.exitValue() == 0 )
        {
            String outStr = inputReader.getData();
            if( !outStr.isEmpty() )
                log.info( outStr );
            //for some reason cwl-runner outputs everything into error stream
            String errorStr = errorReader.getData();
            if( !errorStr.isEmpty() )
                log.info( errorStr );
        }
        else
        {
            //for some reason cwl-runner outputs everything into error stream
            String errorStr = errorReader.getData();
            log.info( errorStr );
            throw new Exception( "CWL executed with error: " + errorStr );
        }
    }

    public static void run(Diagram diagram, String cwl, WorkflowSettings settings, String outputDir, boolean useWsl) throws Exception
    {
        if( settings.getOutputPath() == null )
            throw new InvalidParameterException( "Output path not specified" );

        new File( outputDir ).mkdirs();
        DataCollectionUtils.createSubCollection( settings.getOutputPath() );

        File json = settings.generateParametersJSON2( outputDir );

        settings.exportCollections( outputDir );

        if( cwl == null )
            cwl = new CWLGenerator().generate( diagram );

        String name = diagram.getName();
        File f = new File( outputDir, name + ".cwl" );
        ApplicationUtils.writeString( f, cwl );

        Process process = run( f.getName(), json.getName(), outputDir, useWsl );

        //        new Thread( new Runnable()
        //        {
        //            public void run()
        //            {
        //                BufferedReader input = new BufferedReader( new InputStreamReader( process.getInputStream() ) );
        //                String line = null;
        //
        //                try
        //                {
        //                    while ( (line = input.readLine()) != null )
        //                        log.info( line );
        //                }
        //                catch (IOException e)
        //                {
        //                    e.printStackTrace();
        //                }
        //                //                
        //                //for some reason cwl-runner outputs everything into error stream
        //                BufferedReader err = new BufferedReader( new InputStreamReader( process.getErrorStream() ) );
        //                line = null;
        //
        //                try
        //                {
        //                    while ( (line = err.readLine()) != null )
        //                        log.info( line );
        //                }
        //                catch (IOException e)
        //                {
        //                    e.printStackTrace();
        //                }
        //            }
        //        } ).start();
        //
        //        process.waitFor();

        //importResults( diagram, settings, outputDir );
        StreamGobbler inputReader = new StreamGobbler( process.getInputStream(), true );
        StreamGobbler errorReader = new StreamGobbler( process.getErrorStream(), true );
        process.waitFor();

        if( process.exitValue() == 0 )
        {
            String outStr = inputReader.getData();
            if( !outStr.isEmpty() )
                log.info( outStr );
            //for some reason cwl-runner outputs everything into error stream
            String errorStr = errorReader.getData();
            if( !errorStr.isEmpty() )
                log.info( errorStr );
            importResults( settings, outputDir );
        }
        else
        {
            //for some reason cwl-runner outputs everything into error stream
            String errorStr = errorReader.getData();
            log.info( errorStr );
            throw new Exception( "Nextflow executed with error: " + errorStr );
        }
    }

    public static void importResults(WorkflowSettings settings, String outputDir) throws Exception
    {
        if( settings.getOutputPath() == null )
            return;
        DataCollection dc = settings.getOutputPath().getDataCollection();

        File folder = new File( outputDir, "cwl_results" );
        if( !folder.exists() || !folder.isDirectory() )
        {
            log.info( "No results found" );
            return;
        }
        for( File f : folder.listFiles() )
        {
            String data = ApplicationUtils.readAsString( f );
            dc.put( new TextDataElement( f.getName(), dc, data ) );
        }
    }
}
