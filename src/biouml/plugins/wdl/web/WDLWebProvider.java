package biouml.plugins.wdl.web;

import java.io.OutputStream;
import java.io.StringReader;
import java.util.logging.Level;

import org.json.JSONArray;
import org.json.JSONObject;

import biouml.model.Diagram;
import biouml.plugins.wdl.cwl.CWLGenerator;
import biouml.plugins.wdl.nextflow.NextFlowGenerator;
import biouml.plugins.wdl.WDLGenerator;
import biouml.plugins.wdl.nextflow.NextFlowRunner;
import biouml.plugins.wdl.WorkflowSettings;
import biouml.plugins.wdl.diagram.WDLImporter;
import biouml.plugins.wdl.diagram.WDLLayouter;
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
        if( DIAGRAM_TO_WDL.equals( action ) )
        {
            DataElementPath diagramPath = arguments.getDataElementPath();
            Diagram diagram = WebDiagramsProvider.getDiagram( diagramPath.toString(), false );
            String wdl = new WDLGenerator().generate( diagram );
            String nextflow = new NextFlowGenerator().generate( diagram );
            String cwl = "";
            try
            {
                cwl = new CWLGenerator().generate( diagram );
            }
            catch( Exception ex )
            {

            }
            JSONObject res = new JSONObject();
            res.put( "wdl", wdl );
            res.put( "nextflow", nextflow );
            res.put( "cwl", cwl );
            response.sendJSON( res );

        }
        else if( WDL_TO_DIAGRAM.equals( action ) )
        {
            DataElementPath diagramPath = arguments.getDataElementPath();
            Diagram diagram = WebDiagramsProvider.getDiagram( diagramPath.toString(), false );
            String text = arguments.get( "wdl" );
            WDLImporter wdlImporter = new WDLImporter();
            diagram = wdlImporter.generateDiagram( text, diagram );
            new WDLLayouter().layout( diagram );
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
                String log = NextFlowRunner.runNextFlowByDiagram( diagram, settings, outputDir, false );
                JSONObject res = new JSONObject();
                res.put( "result", settings.getOutputPath().toString() );
                res.put( "log", log );
                response.sendJSON( res );
            }
            catch( Exception e )
            {
                log.log( Level.SEVERE, e.getMessage() );
                response.error( e.getMessage() );
            }
        }
    }
}
