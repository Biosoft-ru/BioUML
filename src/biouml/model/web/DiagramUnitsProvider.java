package biouml.model.web;

import java.util.Map;

import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.standard.type.Unit;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.providers.WebBeanProvider;
import ru.biosoft.server.servlets.webservices.providers.WebJSONProviderSupport;

public class DiagramUnitsProvider extends WebJSONProviderSupport
{

    @Override
    public void process(BiosoftWebRequest arguments, JSONResponse response) throws Exception
    {
        DataElementPath dePath = arguments.getDataElementPath();
        String action = arguments.getAction();

        Object de = WebBeanProvider.getBean( dePath.toString() );
        if( ! ( de instanceof Diagram ) )
            throw new IllegalArgumentException( "Object is not a diagram: " + dePath );

        Diagram diagram = (Diagram)de;
        EModel eModel = diagram.getRole( EModel.class );
        if( "add".equals( action ) )
        {
            //Add unit
            String name = arguments.getString( "name" );
            String title = arguments.getOrDefault( "title", name );
            String comment = arguments.get( "comment" );
            Unit unit = new Unit( null, name );
            unit.setTitle( title );
            if( comment != null )
                unit.setComment( comment );
            eModel.addUnit( unit );
            //TODO: base units
            response.sendString( "" );
            return;
        }
        else if( "remove".equals( action ) )
        {
            //Remove unit
            String names = arguments.getString( "names" );
            for( String name : names.split( "," ) )
                eModel.removeUnit( name );
            response.sendString( "" );
            return;

        }
        else if( "edit".equals( action ) )
        {

        }
        else if( "add_new".equals( action ) )
        {
            Map<String, Unit> units = eModel.getUnits();
            String baseUintName = "unit_";
            int i = 1;
            while( units.containsKey( baseUintName + i ) )
                i++;
            String name = baseUintName + i;
            Unit unit = new Unit( null, name );
            eModel.addUnit( unit );
            response.sendString( name );
            return;
        }
    }

}
