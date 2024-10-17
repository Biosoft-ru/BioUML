package biouml.plugins.physicell.web;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.plugins.physicell.CellDefinitionProperties;
import biouml.plugins.physicell.MulticellEModel;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.providers.WebDiagramsProvider;
import ru.biosoft.server.servlets.webservices.providers.WebJSONProviderSupport;

public class PhysicellWebProvider extends WebJSONProviderSupport
{

    @Override
    public void process(BiosoftWebRequest arguments, JSONResponse response) throws Exception
    {
        String action = arguments.getAction();
        Diagram diagram = WebDiagramsProvider.getDiagramChecked( arguments.getDataElementPath() );
        String nodeName = arguments.getString( "node" );

        if( "add_rule".equals( action ) || "remove_rule".equals( action ) )
        {
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
    }

}
