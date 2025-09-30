package biouml.plugins.wdl;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.logging.Logger;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Compartment;
import biouml.model.Diagram;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.TextDataElement;
import ru.biosoft.util.StreamGobbler;

public class CWLRunner
{

    private static final Logger log = Logger.getLogger( CWLRunner.class.getName() );

    public static void runNextFlow(Diagram diagram, String cwl, WorkflowSettings settings, String outputDir, boolean useWsl)
            throws Exception
    {
        if( settings.getOutputPath() == null )
            throw new InvalidParameterException( "Output path not specified" );

        new File( outputDir ).mkdirs();
        DataCollectionUtils.createSubCollection( settings.getOutputPath() );

        //        File config = new File( outputDir, "nextflow.config" );
        //        ApplicationUtils.writeString( config, "docker.enabled = true" );

        File json = settings.generateParametersJSON2( outputDir );

        settings.exportCollections( outputDir );

        if( cwl == null )
            cwl = new CWLGenerator().generate( diagram );

        String name = diagram.getName();
        File f = new File( outputDir, name + ".cwl" );
        ApplicationUtils.writeString( f, cwl );

        ProcessBuilder builder;
        //        if( useWsl )
        //        {
        String parent = new File( outputDir ).getAbsolutePath().replace( "\\", "/" );
        builder = new ProcessBuilder( "wsl", "--cd", parent, "cwltool", "--outdir", "./cwl_results", f.getName(), json.getName() );
        //        }
        //        else
        //        {
        //            builder = new ProcessBuilder( "nextflow", f.getName(), "-c", "nextflow.config", "-params-file", json.getName() );
        //            builder.directory( new File( outputDir ) );
        //        }

        Process process = builder.start();

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
            importResults( diagram, settings, outputDir );
        }
        else
        {
            //for some reason cwl-runner outputs everything into error stream
            String errorStr = errorReader.getData();
            log.info( errorStr );
            throw new Exception( "Nextflow executed with error: " + errorStr );
        }

    }

    public static void importResults(Diagram diagram, WorkflowSettings settings, String outputDir) throws Exception
    {
        if( settings.getOutputPath() == null )
            return;
        DataCollection dc = settings.getOutputPath().getDataCollection();

        //        for ( Compartment n : WorkflowUtil.getAllCalls( diagram ) )
        //        {
        //            if( WorkflowUtil.getDiagramRef( n ) != null )
        //            {
        //                String ref = WorkflowUtil.getDiagramRef( n );
        //                Diagram externalDiagram = (Diagram) diagram.getOrigin().get( ref );
        //                importResults( externalDiagram, settings, outputDir );
        //                continue;
        //            }
        //            String taskRef = WorkflowUtil.getTaskRef( n );
        //            String folderName = (taskRef);
        File folder = new File( outputDir, "cwl_results" );
        if( !folder.exists() || !folder.isDirectory() )
        {
            log.info( "No results found" );
            return;
        }
        //            DataCollection nested = DataCollectionUtils.createSubCollection( dc.getCompletePath().getChildPath( folderName ) );
        for( File f : folder.listFiles() )
        {
            String data = ApplicationUtils.readAsString( f );
            dc.put( new TextDataElement( f.getName(), dc, data ) );
        }
        //        }
    }
}
