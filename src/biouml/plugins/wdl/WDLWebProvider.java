package biouml.plugins.wdl;

import org.json.JSONObject;

import biouml.model.Diagram;
import biouml.plugins.wdl.diagram.WDLDiagramTransformer;
import ru.biosoft.access.file.FileDataElement;
//import ru.biosoft.access.FileDataElement;
import ru.biosoft.graphics.View;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.providers.WebDiagramsProvider;
import ru.biosoft.server.servlets.webservices.providers.WebJSONProviderSupport;

public class WDLWebProvider extends WebJSONProviderSupport
{
    private static final String GET_DIAGRAM_VIEW = "get_diagram_view";

    @Override
    public void process(BiosoftWebRequest req, JSONResponse response) throws Exception
    {
        
        String action = req.getAction();
        if( GET_DIAGRAM_VIEW.equals( action ) )
        {
            WDLScript script = req.getDataElement( WDLScript.class );
            WDLDiagramTransformer transformer = new WDLDiagramTransformer();
            FileDataElement fde = new FileDataElement( script.getName(), null, script.getFile() );
            Diagram diagram = transformer.transformInput( fde );
            View view = WebDiagramsProvider.createView( diagram );
            JSONObject json = view.toJSON();
            response.sendJSON( json );
        }
    }

}
