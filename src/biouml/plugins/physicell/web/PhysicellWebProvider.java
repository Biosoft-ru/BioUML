package biouml.plugins.physicell.web;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.plugins.physicell.CellDefinitionProperties;
import biouml.plugins.physicell.MulticellEModel;
import biouml.plugins.physicell.document.DensityState;
import biouml.plugins.physicell.document.PhysicellSimulationResult;
import biouml.plugins.physicell.document.StateVisualizer2D;
import biouml.plugins.server.access.AccessProtocol;
import biouml.plugins.simulation.document.InputParameter;
import biouml.plugins.simulation.document.InteractiveSimulation;
import ru.biosoft.access.TextDataElement;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.WebException;
import ru.biosoft.server.servlets.webservices.WebServicesServlet;
import ru.biosoft.server.servlets.webservices.WebSession;
import ru.biosoft.server.servlets.webservices.providers.WebDiagramsProvider;
import ru.biosoft.server.servlets.webservices.providers.WebJSONProviderSupport;

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
            sendSimulationImage( arguments.getString( AccessProtocol.KEY_DE ), response );
        }
        else if( "timestep".equals( action ) )
        {
            doStep( arguments.getString( AccessProtocol.KEY_DE ), response );
        }
    }

    public static PhysicellSimulationResult getSimulationResult(String simulationDe) throws WebException
    {
        Object cachedObj = WebServicesServlet.getSessionCache().getObject( simulationDe );
        if( cachedObj instanceof PhysicellSimulationResult )
            return (PhysicellSimulationResult)cachedObj;
        else
            throw new WebException( "EX_QUERY_NO_ELEMENT", simulationDe );
    }

    private static void sendSimulationImage(String simulationDe, JSONResponse response) throws Exception
    {
        PhysicellSimulationResult simulation = getSimulationResult( simulationDe );
        //        PlotsInfo plotsInfo = DiagramUtility.getPlotsInfo( simulation.getDiagram() );
        //        PlotInfo[] plotInfos = plotsInfo.getActivePlots();
        //        String[] imageNames = null;
        //        Map<String, double[]> values = simulation.getResult();
        //        BufferedImage[] resultImages = StreamEx.of( plotInfos ).map( p -> {
        //            WebSimplePlotPane wp = new WebSimplePlotPane( 700, 500, p );
        //            wp.redrawChart( values );
        //            return wp;
        //        } ).map( p -> p.getImage() ).toArray( BufferedImage[]::new );
        StateVisualizer2D visualizer = new StateVisualizer2D();
        visualizer.setResult( simulation );
        visualizer.getOptions().getOptions2D().setSlice( 750 );
        TextDataElement tde = simulation.getPoint( simulation.getOptions().getTime() );
        visualizer.readAgents( tde.getContent(), tde.getName() ); 
        visualizer.setDensityState( simulation.getDensity( simulation.getOptions().getTime() ) );
        
        BufferedImage image = visualizer.draw();
        if( image != null )
        {
            //            imageNames = new String[resultImages.length];
            //            for( int i = 0; i < resultImages.length; i++ )
            //            {
            //                imageNames[i] = simulationDe + "_img_" + i;
            WebSession.getCurrentSession().putImage("physicell_image", image);
            //            }
            response.sendStringArray(new String[] { "physicell_image" });
        }
        else
            response.sendStringArray( new String[0]);
    }

    private static void createPhysicellDocument(DataCollection dc, JSONResponse response) throws Exception
    {
        String simulationName = "Simulation " + dc.getName();
        //TODO: think of nicer naming of simulation documents
        String completeSimulationName = DataElementPath.create( simulationName ).getChildPath( dc.getCompletePath().getPathComponents() )
                .toString();
        PhysicellSimulationResult simulation = new PhysicellSimulationResult( dc.getName() + " Simulation", dc );
        simulation.init();
        //        PhysicellResultDocument document = new PhysicellResultDocument( simulation );
        WebServicesServlet.getSessionCache().addObject( completeSimulationName, simulation, true );
        response.sendString( completeSimulationName );
    }
    
    private void doStep(String simulationDe, JSONResponse response)
            throws IOException, WebException
    {
        PhysicellSimulationResult simulation = getSimulationResult( simulationDe );
        int step = simulation.getStep();
        int curTime = simulation.getOptions().getTime();
        simulation.getOptions().setTime( curTime + step );
//        asList.stream().forEach( parameter -> {
//            InputParameter selected = simulation.getParameter( parameter );
//            selected.setValue( selected.getValue() + selected.getValueStep() );
//            simulation.updateValue( selected );
//        } );
//        simulation.doSimulation();
        response.sendString( "ok" );
    }

}
