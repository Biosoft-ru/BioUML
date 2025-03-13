package biouml.plugins.physicell.web;

import java.util.HashSet;
import java.util.Set;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.dynamics.plot.Curve;
import biouml.model.dynamics.plot.PlotInfo;
import biouml.model.dynamics.plot.PlotVariable;
import biouml.model.dynamics.plot.PlotsInfo;
import biouml.plugins.physicell.CellDefinitionProperties;
import biouml.plugins.physicell.MulticellEModel;
import biouml.plugins.physicell.document.PhysicellResultDocument;
import biouml.plugins.physicell.document.PhysicellSimulationResult;
import biouml.plugins.simulation.document.InteractiveSimulation;
import biouml.standard.diagram.DiagramUtility;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.generic.GenericDataCollection;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.WebServicesServlet;
import ru.biosoft.server.servlets.webservices.providers.WebDiagramsProvider;
import ru.biosoft.server.servlets.webservices.providers.WebJSONProviderSupport;

public class PhysicellWebProvider extends WebJSONProviderSupport
{

    @Override
    public void process(BiosoftWebRequest arguments, JSONResponse response) throws Exception
    {
        String action = arguments.getAction();
        Diagram diagram = WebDiagramsProvider.getDiagramChecked( arguments.getDataElementPath() );
        

        if( "add_rule".equals( action ) || "remove_rule".equals( action ) )
        {
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
        else if ("add_scheme".equals( action ))
        {
            diagram.getRole( MulticellEModel.class ).addColorScheme();
            response.sendString( "ok" );
        }
        else if ("remove_scheme".equals( action ))
        {
            int index = arguments.getInt( "index" );
            diagram.getRole( MulticellEModel.class ).removeColorScheme( index );
            response.sendString( "ok" );
        }
        else if ("add_visualizer".equals(action))
        {
            diagram.getRole( MulticellEModel.class ).getVisualizerProperties().addVisualizer();
            response.sendString( "ok" );
        }
        else if ("remove_visualizer".equals(action))
        {
            int index = arguments.getInt( "index" );
            diagram.getRole( MulticellEModel.class ).getVisualizerProperties().removeVisualizer( index );
            response.sendString( "ok" );
        }
        else if ("simulation_document_create".equals(action))
        {
            createPhysicellDocument( arguments.getDataElement( DataCollection.class ), response );
        }
            
    }
    
    private static void createPhysicellDocument(DataCollection dc, JSONResponse response) throws Exception
    {
        String simulationName = "Simulation " + dc.getName();
        //TODO: think of nicer naming of simulation documents
        String completeSimulationName = DataElementPath.create( simulationName )
                .getChildPath( dc.getCompletePath().getPathComponents() ).toString();
        PhysicellSimulationResult simulation = new PhysicellSimulationResult( dc.getName() + " Simulation", dc );
//        PhysicellResultDocument document = new PhysicellResultDocument( simulation );
        WebServicesServlet.getSessionCache().addObject( completeSimulationName, simulation, true );
        response.sendString( completeSimulationName );
    }

}
