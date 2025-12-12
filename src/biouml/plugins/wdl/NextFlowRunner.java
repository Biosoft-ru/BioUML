package biouml.plugins.wdl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.InvalidParameterException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Compartment;
import biouml.model.Diagram;
import one.util.streamex.StreamEx;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.TextFileImporter;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;

public class NextFlowRunner
{
    private static final String BIOUML_FUNCTIONS_NF = "resources/biouml_function.nf";
    private static final Logger log = Logger.getLogger( NextFlowRunner.class.getName() );

    public static File generateFunctions(String outputDir) throws IOException
    {
        InputStream inputStream = NextFlowRunner.class.getResourceAsStream( BIOUML_FUNCTIONS_NF );
        File result = new File( outputDir, "biouml_function.nf" );
        Files.copy( inputStream, result.toPath(), StandardCopyOption.REPLACE_EXISTING );
        return result;
    }


    public static String runNextFlow(Diagram diagram, String nextFlowScript, WorkflowSettings settings, String outputDir, boolean useWsl)
            throws Exception
    {
        if( settings.getOutputPath() == null )
            throw new InvalidParameterException( "Output path not specified" );

        new File( outputDir ).mkdirs();
        DataCollectionUtils.createSubCollection( settings.getOutputPath() );

        File config = new File( outputDir, "nextflow.config" );
        ApplicationUtils.writeString( config, "docker.enabled = true" );

        File json = settings.generateParametersJSON( outputDir );

        settings.exportCollections( outputDir );

        generateFunctions( outputDir );

        exportIncludes( diagram, outputDir );

        if( nextFlowScript == null )
            nextFlowScript = new NextFlowGenerator().generate( diagram );
        NextFlowPreprocessor preprocessor = new NextFlowPreprocessor();
        nextFlowScript = preprocessor.preprocess( nextFlowScript );

        String name = diagram.getName();
        File f = new File( outputDir, name + ".nf" );
        ApplicationUtils.writeString( f, nextFlowScript );

        ProcessBuilder builder;
        if( useWsl )
        {
            String parent = new File( outputDir ).getAbsolutePath().replace( "\\", "/" );
            builder = new ProcessBuilder( "wsl", "--cd", parent, "nextflow", f.getName(), "-c", "nextflow.config", "-params-file",
                    json.getName() );
        }
        else
        {
            builder = new ProcessBuilder( "nextflow", f.getName(), "-c", "nextflow.config", "-params-file", json.getName() );
            builder.directory( new File( outputDir ) );
        }

        System.out.println( "COMMAND: " + StreamEx.of( builder.command() ).joining( " " ) );
        Process process = builder.start();

        new Thread( new Runnable()
        {
            public void run()
            {
                BufferedReader input = new BufferedReader( new InputStreamReader( process.getInputStream() ) );
                String line = null;

                try
                {
                    while( ( line = input.readLine() ) != null )
                        log.info( line );
                }
                catch( IOException e )
                {
                    e.printStackTrace();
                }
                //                
                //for some reason cwl-runner outputs everything into error stream
                BufferedReader err = new BufferedReader( new InputStreamReader( process.getErrorStream() ) );
                line = null;

                try
                {
                    while( ( line = err.readLine() ) != null )
                        log.info( line );
                }
                catch( IOException e )
                {
                    e.printStackTrace();
                }
            }
        } ).start();

        process.waitFor();
        importResults( diagram, settings, outputDir );
        return "";
        //        StreamGobbler inputReader = new StreamGobbler( process.getInputStream(), true );
        //        StreamGobbler errorReader = new StreamGobbler( process.getErrorStream(), true );
        //        process.waitFor();
        //
        //        if( process.exitValue() == 0 )
        //        {
        //            String logString = "";
        //            String outStr = inputReader.getData();
        //            if( !outStr.isEmpty() )
        //            {
        //                logString += outStr;
        //                log.info( outStr );
        //            }
        //            //for some reason cwl-runner outputs everything into error stream
        //            String errorStr = errorReader.getData();
        //            if( !errorStr.isEmpty() )
        //            {
        //                logString += errorStr;
        //                log.info( errorStr );
        //            }
        //            importResults( diagram, settings, outputDir );
        //            return logString;
        //        }
        //        else
        //        {
        //            String errorStr = errorReader.getData();
        //            log.info( errorStr );
        //            throw new Exception( "Nextflow executed with error: " + errorStr );
        //        }

    }

    public static void importResults(Diagram diagram, WorkflowSettings settings, String outputDir) throws Exception
    {
        if( settings.getOutputPath() == null )
            return;
        DataCollection dc = settings.getOutputPath().getDataCollection();

        for( Compartment n : WorkflowUtil.getAllCalls( diagram ) )
        {
            if( WorkflowUtil.getDiagramRef( n ) != null )
            {
                String ref = WorkflowUtil.getDiagramRef( n );
                Diagram externalDiagram = (Diagram)diagram.getOrigin().get( ref );
                importResults( externalDiagram, settings, outputDir );
                continue;
            }
            String taskRef = WorkflowUtil.getTaskRef( n );
            String folderName = ( taskRef );
            File folder = new File( outputDir, folderName );
            if( !folder.exists() || !folder.isDirectory() )
            {
                log.info( "No results for " + n.getName() );
                continue;
            }
            DataCollection nested = DataCollectionUtils.createSubCollection( dc.getCompletePath().getChildPath( folderName ) );
            for( File f : folder.listFiles() )
            {
                TextFileImporter importer = new TextFileImporter();
                importer.doImport( nested, f, f.getName(), null, log );
            }
        }
    }

    public static void exportIncludes(Diagram diagram, String outputDir) throws Exception
    {
        for( Diagram d : getIncludes( diagram ) )
            WorkflowUtil.export( d, new File( outputDir ) );
    }

    public static Set<Diagram> getIncludes(Diagram diagram)
    {
        Set<Diagram> result = new HashSet<>();
        for( ImportProperties ip : WorkflowUtil.getImports( diagram ) )
        {
            DataElementPath dep = ip.getSource();
            if( dep != null )
            {
                DataElement de = dep.getDataElement();
                if( de instanceof Diagram )
                {
                    result.add( (Diagram)de );
                    continue;
                }
            }
            String name = ip.getSourceName();
            DataElement de = DataElementPath.create( diagram.getOrigin(), name ).getDataElement();
            if( de instanceof Diagram )
            {
                result.add( (Diagram)de );
            }
        }
        Set<Diagram> additionals = new HashSet<Diagram>();
        for( Diagram d : result )
            additionals.addAll( getIncludes( d ) );
        result.addAll( additionals );
        return result;
    }
}
