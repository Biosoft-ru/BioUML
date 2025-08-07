package biouml.plugins.wdl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.InvalidParameterException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Compartment;
import biouml.model.Diagram;
import one.util.streamex.StreamEx;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.TextFileImporter;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.util.StreamGobbler;

public class WDLRunner
{
    private static final String BIOUML_FUNCTIONS_NF = "resources/biouml_function.nf";
    private static final Logger log = Logger.getLogger( WDLRunner.class.getName() );

    public static File generateFunctions(String outputDir) throws IOException
    {
        InputStream inputStream = WDLRunner.class.getResourceAsStream( BIOUML_FUNCTIONS_NF );
        File result = new File( outputDir, "biouml_function.nf" );
        Files.copy( inputStream, result.toPath(), StandardCopyOption.REPLACE_EXISTING );
        return result;
    }


    public static void runNextFlow(Diagram diagram, WorkflowSettings settings, String outputDir, boolean useWsl) throws Exception
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

        for ( DataElement de : StreamEx.of( WDLUtil.getImports( diagram ) ).map( f -> f.getSource().getDataElement() ) )
            WDLUtil.export( de, new File( outputDir ) );

        String nextFlowScript = new NextFlowGenerator().generateNextFlow( diagram );
        NextFlowPreprocessor preprocessor = new NextFlowPreprocessor();
        nextFlowScript = preprocessor.preprocess( nextFlowScript );

        String name = diagram.getName();
        File f = new File( outputDir, name + ".nf" );
        ApplicationUtils.writeString( f, nextFlowScript );

        ProcessBuilder builder;
        if( useWsl )
        {
            String parent = new File( outputDir ).getAbsolutePath().replace( "\\", "/" );
            builder = new ProcessBuilder( "wsl", "--cd", parent, "nextflow", f.getName(), "-c", "nextflow.config", "-params-file", json.getName() );
        }
        else
        {
            builder = new ProcessBuilder( "nextflow", f.getName(), "-c", "nextflow.config", "-params-file", json.getName() );
            builder.directory( new File( outputDir ) );
        }

        Process process = builder.start();
        StreamGobbler inputReader = new StreamGobbler( process.getInputStream(), true );
        StreamGobbler errorReader = new StreamGobbler( process.getErrorStream(), true );
        process.waitFor();

        if( process.exitValue() == 0 )
        {
            log.log( Level.INFO, inputReader.getData() );
            importResults( diagram, settings, outputDir );

        }
        else
        {
            String errorStr = errorReader.getData();
            throw new Exception( "Nextflow executed with error: " + errorStr );
        }

    }

    public static void importResults(Diagram diagram, WorkflowSettings settings, String outputDir) throws Exception
    {
        if( settings.getOutputPath() == null )
            return;
        DataCollection dc = settings.getOutputPath().getDataCollection();

        for ( Compartment n : WDLUtil.getAllCalls( diagram ) )
        {
            String taskRef = WDLUtil.getTaskRef( n );
            String folderName = (taskRef);
            File folder = new File( outputDir, folderName );
            if( !folder.exists() || !folder.isDirectory() )
            {
                log.info( "No results for " + n.getName() );
                continue;
            }
            DataCollection nested = DataCollectionUtils.createSubCollection( dc.getCompletePath().getChildPath( folderName ) );
            for ( File f : folder.listFiles() )
            {
                TextFileImporter importer = new TextFileImporter();
                importer.doImport( nested, f, f.getName(), null, log );
            }
        }
    }

}
