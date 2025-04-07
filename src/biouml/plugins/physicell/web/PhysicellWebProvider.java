package biouml.plugins.physicell.web;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import org.json.JSONArray;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.plugins.physicell.CellDefinitionProperties;
import biouml.plugins.physicell.MulticellEModel;
import biouml.plugins.physicell.PhysicellResultWriter;
import biouml.plugins.physicell.VideoGenerator;
import biouml.plugins.physicell.document.PhysicellSimulationResult;
import biouml.plugins.physicell.document.StateVisualizer;
import biouml.plugins.physicell.document.StateVisualizer2D;
import biouml.plugins.physicell.document.StateVisualizer3D;
import biouml.plugins.physicell.document.ViewOptions;
import biouml.plugins.server.access.AccessProtocol;
import ru.biosoft.access.TextDataElement;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.server.JSONUtils;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.WebException;
import ru.biosoft.server.servlets.webservices.WebServicesServlet;
import ru.biosoft.server.servlets.webservices.WebSession;
import ru.biosoft.server.servlets.webservices.providers.WebDiagramsProvider;
import ru.biosoft.server.servlets.webservices.providers.WebJSONProviderSupport;
import ru.biosoft.util.TempFiles;

public class PhysicellWebProvider extends WebJSONProviderSupport
{

    @Override
    public void process(BiosoftWebRequest arguments, JSONResponse response) throws Exception
    {
        String action = arguments.getAction();

        if( "add_rule".equals( action ) || "remove_rule".equals( action ) )
        {
            Diagram diagram = WebDiagramsProvider.getDiagramChecked( arguments.getDataElementPath() );
            String nodeName = arguments.getString( "node" );
            DiagramElement de = diagram.get( nodeName );
            if( de == null || de.getRole() == null || ! ( de.getRole() instanceof CellDefinitionProperties ) )
            {
                response.error( "Please, select proper element for this action" );
                return;
            }

            CellDefinitionProperties props = (CellDefinitionProperties)de.getRole();
            if( "add_rule".equals( action ) )
            {
                props.getRulesProperties().addRule();
                response.sendString( "ok" );
            }
            else if( "remove_rule".equals( action ) )
            {
                int index = arguments.getInt( "index" );
                props.getRulesProperties().removeRule( index );
                response.sendString( "ok" );
            }
        }
        else if( "add_scheme".equals( action ) )
        {
            Diagram diagram = WebDiagramsProvider.getDiagramChecked( arguments.getDataElementPath() );
            diagram.getRole( MulticellEModel.class ).addColorScheme();
            response.sendString( "ok" );
        }
        else if( "remove_scheme".equals( action ) )
        {
            Diagram diagram = WebDiagramsProvider.getDiagramChecked( arguments.getDataElementPath() );
            int index = arguments.getInt( "index" );
            diagram.getRole( MulticellEModel.class ).removeColorScheme( index );
            response.sendString( "ok" );
        }
        else if( "add_visualizer".equals( action ) )
        {
            Diagram diagram = WebDiagramsProvider.getDiagramChecked( arguments.getDataElementPath() );
            diagram.getRole( MulticellEModel.class ).getVisualizerProperties().addVisualizer();
            response.sendString( "ok" );
        }
        else if( "remove_visualizer".equals( action ) )
        {
            Diagram diagram = WebDiagramsProvider.getDiagramChecked( arguments.getDataElementPath() );
            int index = arguments.getInt( "index" );
            diagram.getRole( MulticellEModel.class ).getVisualizerProperties().removeVisualizer( index );
            response.sendString( "ok" );
        }
        else if( "physicell_document_create".equals( action ) )
        {
            createPhysicellDocument( arguments.getDataElement( DataCollection.class ), response );
        }
        else if( "physicell_document_image".equals( action ) )
        {   
            String simulationDe = arguments.getString( AccessProtocol.KEY_DE );
            PhysicellSimulationResult result = getSimulationResult( simulationDe);
            ViewOptions options = result.getOptions();

            try
            {
                JSONArray optionsJson = arguments.getJSONArray( "options" );
                JSONUtils.correctBeanOptions( options, optionsJson );
            }
            catch( Exception ex )
            {

            }
            sendSimulationImage( result, arguments.getString( AccessProtocol.KEY_DE ), response, arguments );
        }
        else if( "timestep".equals( action ) )
        {
            doStep( arguments.getString( AccessProtocol.KEY_DE ), response );
        }
        else if( "record".equals( action ) )
        {
            startRecord(arguments.getString( AccessProtocol.KEY_DE ));
            response.sendString( "ok" );
        }
        else if( "record_stop".equals( action ) )
        {
            stopRecord(arguments.getString( AccessProtocol.KEY_DE ));
            response.sendString( "ok" );
        }
        else if( "rotate_left".equals( action ) )
        {
            rotateHead(-20, arguments.getString( AccessProtocol.KEY_DE ), response, arguments);
        }
        else if( "rotate_right".equals( action ) )
        {
            rotateHead(20, arguments.getString( AccessProtocol.KEY_DE ), response, arguments);
        }
    }
    
    private static void rotateHead(int headAddon, String simulationDe, JSONResponse response, BiosoftWebRequest arguments)  throws Exception
    {    
        PhysicellSimulationResult result = getSimulationResult( simulationDe);    
        if (result.getOptions().is2D())
            return;
        ViewOptions options = result.getOptions();

        try
        {
            JSONArray optionsJson = arguments.getJSONArray( "options" );
            JSONUtils.correctBeanOptions( options, optionsJson );
        }
        catch( Exception ex )
        {

        }
       
        if (result.getOptions().is2D())
            return;
        int head =  result.getOptions().getOptions3D().getHead();
        head += headAddon;
        result.getOptions().getOptions3D().setHead( head );
        sendSimulationImage( result, arguments.getString( AccessProtocol.KEY_DE ), response, arguments );
    }
    
    private static void startRecord(String simulationDe)  throws WebException, IOException
    {
        PhysicellSimulationResult result = getSimulationResult( simulationDe);
        result.getOptions().setSaveResult( true );
        File tempVideoFile = TempFiles.file( "Video.mp4" );
        VideoGenerator videoGenerator = new VideoGenerator( tempVideoFile , result.getOptions().getFps());
        WebServicesServlet.getSessionCache().addObject(  simulationDe+"_video_generator" , videoGenerator, true );
        WebServicesServlet.getSessionCache().addObject(  simulationDe+"_video_file" , tempVideoFile, true );
        videoGenerator.init();
    }
    
    private static void stopRecord(String simulationDe)  throws Exception
    {
        PhysicellSimulationResult result = getSimulationResult( simulationDe);
        result.getOptions().setSaveResult( false );
        VideoGenerator videoGenerator = (VideoGenerator)WebServicesServlet.getSessionCache().getObject( simulationDe+"_video_generator" );
        File tempVideoFile = (File)WebServicesServlet.getSessionCache().getObject( simulationDe+"_video_file" );
        videoGenerator.finish();
        PhysicellResultWriter.uploadMP4( tempVideoFile, result.getOptions().getResult().getParentCollection(), result.getOptions().getResult().getName() );
    }


    public static PhysicellSimulationResult getSimulationResult(String simulationDe) throws WebException
    {
        Object cachedObj = WebServicesServlet.getSessionCache().getObject( simulationDe );
        if( cachedObj instanceof PhysicellSimulationResult )
            return (PhysicellSimulationResult)cachedObj;
        else
            throw new WebException( "EX_QUERY_NO_ELEMENT", simulationDe );
    }

    private static void sendSimulationImage(String simulationDe, JSONResponse response, BiosoftWebRequest arguments) throws Exception
    {
        PhysicellSimulationResult result = getSimulationResult( simulationDe );
        sendSimulationImage( result, simulationDe, response, arguments );
    }
    
    private static void sendSimulationImage(PhysicellSimulationResult result, String simulationDe, JSONResponse response,
            BiosoftWebRequest arguments) throws Exception
    {
        ViewOptions options = result.getOptions();
        StateVisualizer visualizer = options.is3D() ? new StateVisualizer3D() : new StateVisualizer2D();
        visualizer.setResult( result );
        TextDataElement tde = result.getPoint( result.getOptions().getTime() );
        visualizer.readAgents( tde.getContent(), tde.getName() );
        visualizer.setDensityState( result.getDensity( result.getOptions().getTime(), result.getOptions().getSubstrate() ) );
        BufferedImage image = visualizer.draw();
        if( image != null )
        {
            WebSession.getCurrentSession().putImage("physicell_image", image);
            response.sendStringArray(new String[] { "physicell_image" });
            
            if (options.isSaveResult())
            {
                VideoGenerator videoGenerator = (VideoGenerator)WebServicesServlet.getSessionCache().getObject( simulationDe+"_video_generator" );
                videoGenerator.update( image );
            }
        }
        else
            response.sendStringArray( new String[0] );
    }

    private static void createPhysicellDocument(DataCollection dc, JSONResponse response) throws Exception
    {
        String simulationName = "Simulation " + dc.getName();
        //TODO: think of nicer naming of simulation documents
        String completeSimulationName = DataElementPath.create( simulationName ).getChildPath( dc.getCompletePath().getPathComponents() )
                .toString();
        PhysicellSimulationResult simulation = new PhysicellSimulationResult( dc.getName() + " Simulation", dc );
        simulation.init();
        WebServicesServlet.getSessionCache().addObject( completeSimulationName, simulation, true );
        response.sendString( completeSimulationName );
    }

    private void doStep(String simulationDe, JSONResponse response) throws IOException, WebException
    {
        PhysicellSimulationResult simulation = getSimulationResult( simulationDe );
        int step = simulation.getOptions().getTimeStep();
        int curTime = simulation.getOptions().getTime();
        simulation.getOptions().setTime( curTime + step );
        if( simulation.getOptions().getMaxTime() < curTime + step )
            response.sendString( "stop" );
        else
            response.sendString( "ok" );
    }
}