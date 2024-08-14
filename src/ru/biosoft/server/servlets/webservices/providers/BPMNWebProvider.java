package ru.biosoft.server.servlets.webservices.providers;

import java.io.InputStream;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bpmn.BPMNDataElement;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.WebException;

public class BPMNWebProvider extends WebJSONProviderSupport
{
    private static final String BPMN_CREATE = "create";
    private static final String BPMN_SAVE = "save";

    @Override
    public void process(BiosoftWebRequest arguments, JSONResponse response) throws Exception
    {
        String action = arguments.getAction();
        if( BPMN_SAVE.equals( action ) )
        {
            DataElementPath path = arguments.getDataElementPath();
            DataElement de = path.getDataElement();
            if( de != null && de instanceof BPMNDataElement )
            {
                String data = arguments.getOrDefault( "data", "" );
                ( (BPMNDataElement)de ).setContent( data );
                path.save( de );
                response.sendString( "ok" );
                return;
            }
        }
        else if( BPMN_CREATE.equals( action ) )
        {
            DataElementPath path = arguments.getDataElementPath();
            DataCollection<? extends DataElement> dc = getDataElement( path.getParentPath(), DataCollection.class );
            if( !dc.isMutable() )
            {
                throw new WebException( "EX_ACCESS_READ_ONLY", path.getParentPath() );
            }
            InputStream is = BPMNDataElement.class.getResourceAsStream( "resources/emptyDiagram.bpmn" );
            String data = ApplicationUtils.readAsString( is );
            BPMNDataElement de = new BPMNDataElement( path.getName(), dc, data );
            try
            {
                path.save( de );
            }
            catch( Exception e )
            {
                throw new WebException( "EX_INTERNAL_CREATE_SCRIPT", path );
            }
            response.sendString( "ok" );
            return;
        }
    }

}
