package biouml.plugins.physicell.web;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.plugins.physicell.CellDefinitionProperties;
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
        Diagram diagram = WebDiagramsProvider.getDiagramChecked(arguments.getDataElementPath());
        String nodeName = arguments.getString("node");
        DiagramElement de = diagram.get(nodeName);
        if ( de == null || de.getRole() == null || !(de.getRole() instanceof CellDefinitionProperties) )
        {
            response.error("Please, select proper element for this action");
            return;
        }

        CellDefinitionProperties props = (CellDefinitionProperties) de.getRole();
        if ( "add_rule".equals(action) )
        {
            props.getRulesProperties().addRule();
            response.sendString("ok");
        }
        else if ( "remove_rule".equals(action) )
        {
            int index = arguments.getInt("index");
            props.getRulesProperties().removeRule(index);
            response.sendString("ok");
        }

    }

}
