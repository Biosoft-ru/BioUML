package biouml.plugins.wdl.web;

import java.io.OutputStream;
import java.io.StringReader;
import java.util.logging.Level;

import org.json.JSONArray;
import org.json.JSONObject;

import biouml.model.Diagram;
import biouml.plugins.wdl.NextFlowGenerator;
import biouml.plugins.wdl.WDLGenerator;
import biouml.plugins.wdl.WDLRunner;
import biouml.plugins.wdl.WorkflowSettings;
import biouml.plugins.wdl.diagram.WDLImporter;
import biouml.plugins.wdl.parser.AstStart;
import biouml.plugins.wdl.parser.WDLParser;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.server.JSONUtils;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.providers.WebDiagramsProvider;
import ru.biosoft.server.servlets.webservices.providers.WebJSONProviderSupport;
import ru.biosoft.util.TempFiles;

public class WDLWebProvider extends WebJSONProviderSupport
{
    //private static final String GET_DIAGRAM_VIEW = "get_diagram_view";
    private static final String DIAGRAM_TO_WDL = "diagram2wdl";
    private static final String WDL_TO_DIAGRAM = "wdl2diagram";
    private static final String RUN_WDL = "run";

    String outputDir = TempFiles.path( "nextflow" ).getAbsolutePath();

    @Override
    public void process(BiosoftWebRequest arguments, JSONResponse response) throws Exception
    {
        
        String action = arguments.getAction();
        //        if( GET_DIAGRAM_VIEW.equals( action ) )
        //        {
        //            WDLScript script = arguments.getDataElement( WDLScript.class );
        //            WDLDiagramTransformer transformer = new WDLDiagramTransformer();
        //            FileDataElement fde = new FileDataElement( script.getName(), null, script.getFile() );
        //            Diagram diagram = transformer.transformInput( fde );
        //            View view = WebDiagramsProvider.createView( diagram );
        //            JSONObject json = view.toJSON();
        //            response.sendJSON( json );
        //        }
        //        else 
        if( DIAGRAM_TO_WDL.equals( action ) )
        {
            DataElementPath diagramPath = arguments.getDataElementPath();
            Diagram diagram = WebDiagramsProvider.getDiagram( diagramPath.toString(), false );
            String wdl = new WDLGenerator().generate( diagram );
            String nextflow = new NextFlowGenerator().generate( diagram );
            JSONObject res = new JSONObject();
            res.put( "wdl", wdl );
            res.put( "nextflow", nextflow );
            response.sendJSON( res );

        }
        else if( WDL_TO_DIAGRAM.equals( action ) )
        {
            DataElementPath diagramPath = arguments.getDataElementPath();
            Diagram diagram = WebDiagramsProvider.getDiagram( diagramPath.toString(), false );
            String text = arguments.get( "wdl" );
            text = text.replace( "<<<", "{" ).replace( ">>>", "}" );//TODO: fix parsing <<< >>>
            AstStart start = new WDLParser().parse( new StringReader( text ) );
            WDLImporter wdlImporter = new WDLImporter();
            diagram = wdlImporter.generateDiagram( start, diagram );
            wdlImporter.layout( diagram );
            diagramPath.save( diagram );
            OutputStream out = response.getOutputStream();
            WebDiagramsProvider.sendDiagramChanges( diagram, out, "json" );
        }
        else if( RUN_WDL.equals( action ) )
        {
            DataElementPath diagramPath = arguments.getDataElementPath();
            Diagram diagram = WebDiagramsProvider.getDiagram( diagramPath.toString(), false );
            WorkflowSettings settings = new WorkflowSettings();
            settings.initParameters( diagram );
            JSONArray jsonSettings = arguments.getJSONArray( "settings" );
            JSONUtils.correctBeanOptions( settings, jsonSettings );
            try
            {
                String log = WDLRunner.runNextFlow( diagram, null, settings, outputDir, false );
                JSONObject res = new JSONObject();
                res.put( "result", settings.getOutputPath().toString() );
                res.put( "log", log );
                response.sendJSON( res );
            }
            catch (Exception e)
            {
                log.log( Level.SEVERE, e.getMessage() );
                response.error( e.getMessage() );
            }
            //            String nextFlow = new NextFlowGenerator().generateNextFlow( diagram );
            //            JSONArray jsonSettings = arguments.getJSONArray( "settings" );
            //            JSONUtils.correctBeanOptions( settings, jsonSettings );
            //            try
            //            {
            //                if( settings.getOutputPath() == null )
            //                {
            //                    response.error( "Output path not specified" );
            //                    return;
            //                }
            //
            //                String name = diagram.getName();
            //
            //                new File( outputDir ).mkdirs();
            //                DataCollectionUtils.createSubCollection( settings.getOutputPath() );
            //
            //                File config = new File( outputDir, "nextflow.config" );
            //                ApplicationUtils.writeString( config, "docker.enabled = true" );
            //
            //                File json = settings.generateParametersJSON( outputDir );
            //
            //                settings.exportCollections( outputDir );
            //
            //                WDLUtil.generateFunctions( outputDir );
            //
            //                for ( DataElement de : StreamEx.of( WDLUtil.getImports( diagram ) ).map( f -> f.getSource().getDataElement() ) )
            //                    WDLUtil.export( de, new File( outputDir ) );
            //
            //                NextFlowPreprocessor preprocessor = new NextFlowPreprocessor();
            //                nextFlow = preprocessor.preprocess( nextFlow );
            //                File f = new File( outputDir, name + ".nf" );
            //                ApplicationUtils.writeString( f, nextFlow );
            //
            //                ProcessBuilder builder = new ProcessBuilder( "nextflow", f.getName(), "-c", "nextflow.config", "-params-file", json.getName() );
            //                builder.directory( new File( outputDir ) );
            //
            //                Process process = builder.start();
            //                StreamGobbler inputReader = new StreamGobbler( process.getInputStream(), true );
            //                StreamGobbler errorReader = new StreamGobbler( process.getErrorStream(), true );
            //                process.waitFor();
            //
            //                if( process.exitValue() == 0 )
            //                {
            //                    log.log( Level.INFO, inputReader.getData() );
            //                    importResults( diagram, settings );
            //                    response.sendString( settings.getOutputPath().toString() );
            //                }
            //                else
            //                {
            //                    String errorStr = errorReader.getData();
            //                    log.log( Level.SEVERE, "Nextflow executed with error: " + errorStr );
            //                    response.error( errorStr );
            //                }
            //            }
            //            catch (Exception ex)
            //            {
            //                response.error( ex.getMessage() );
            //            }

        }
    }

    //    public void importResults(Diagram diagram, WorkflowSettings settings) throws Exception
    //    {
    //        if( settings.getOutputPath() == null )
    //            return;
    //        DataCollection dc = settings.getOutputPath().getDataCollection();
    //
    //        for ( Compartment n : WDLUtil.getAllCalls( diagram ) )
    //        {
    //            String taskRef = WDLUtil.getTaskRef( n );
    //            String folderName = (taskRef);
    //            File folder = new File( outputDir, folderName );
    //            if( !folder.exists() || !folder.isDirectory() )
    //            {
    //                log.info( "No results for " + n.getName() );
    //                continue;
    //            }
    //            DataCollection nested = DataCollectionUtils.createSubCollection( dc.getCompletePath().getChildPath( folderName ) );
    //            for ( File f : folder.listFiles() )
    //            {
    //                TextFileImporter importer = new TextFileImporter();
    //                importer.doImport( nested, f, f.getName(), null, log );
    //            }
    //        }
    //    }

}
