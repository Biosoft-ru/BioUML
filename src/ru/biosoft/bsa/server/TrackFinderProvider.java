package ru.biosoft.bsa.server;

import java.util.List;

import com.eclipsesource.json.JsonArray;

import ru.biosoft.access.BeanRegistry;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.finder.TrackFinder;
import ru.biosoft.bsa.finder.TrackFinderRegistry;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.WebServicesServlet;
import ru.biosoft.server.servlets.webservices.providers.WebJSONProviderSupport;

public class TrackFinderProvider extends WebJSONProviderSupport
{
    @Override
    public void process(BiosoftWebRequest arguments, JSONResponse response) throws Exception
    {
        String action = arguments.getAction();
        switch( action )
        {
            case "list-databases":
            {
                DataElementPath genome = arguments.getDataElementPath( "genome" );
                List<ru.biosoft.access.core.DataElementPath> databases = TrackFinderRegistry.instance.getDatabasesForGenome( genome );
                JsonArray jsonArray = new JsonArray();
                for(DataElementPath db : databases)
                    jsonArray.add( db.getName() );
                response.sendJSON( jsonArray );
            }
                break;
            case "search":
            {
                String databaseName = arguments.getString( "databaseName" );
                DataElementPath genome = arguments.getDataElementPath( "genome" );
                TrackFinder trackFinder = (TrackFinder)BeanRegistry.getBean( "trackFinder/parameters/" + databaseName + "/" + genome, WebServicesServlet.getSessionCache() );
                DataCollection<?> result = trackFinder.findTracks();
                String resultPath = "beans/trackFinder/results/" + databaseName + "/" + genome;
                WebServicesServlet.getSessionCache().addObject( resultPath, result, true );
                response.sendString( resultPath );
            }
                break;
            default:
                response.error( "Unknown command" );
        }
    }
}
