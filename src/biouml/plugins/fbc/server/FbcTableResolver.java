package biouml.plugins.fbc.server;

import biouml.model.Diagram;
import biouml.plugins.fbc.table.FbcBuilderDataTableAnalysis;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.security.SessionCache;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.WebException;
import ru.biosoft.server.servlets.webservices.WebServicesServlet;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.access.TableResolver;

//Table resolver for Flux Balance web viewpart
public class FbcTableResolver extends TableResolver
{
    boolean useCache = true;
    String tableType;

    public FbcTableResolver(BiosoftWebRequest arguments) throws WebException
    {
        useCache = arguments.getBoolean( "useCache" );
        tableType = arguments.getString( "tableType" );
    }

    @Override
    public DataCollection getTable(DataElement de)
    {
        TableDataCollection table = null;
        Diagram diagram = (Diagram)de;
        DataElementPath diagramPath = diagram.getCompletePath();
        SessionCache sessionCache = WebServicesServlet.getSessionCache();

        if( tableType.equals( "input" ) )
        {
            String completeName = FbcService.FBC_TABLE + diagramPath;
            Object tableObj = sessionCache.getObject( completeName );
            if( !useCache || tableObj == null )
            {
                FbcBuilderDataTableAnalysis analysis = new FbcBuilderDataTableAnalysis( null, null );
                table = analysis.getFbcData( diagram );
                sessionCache.addObject( completeName, table, true );
            }
            else if( tableObj instanceof DataCollection )
            {
                table = (TableDataCollection)tableObj;
            }
        }
        else if( tableType.equals( "result" ) )
        {
            String completeName = FbcService.FBC_TABLE_RESULT + diagramPath;
            Object tableObj = sessionCache.getObject( completeName );
            if( tableObj != null && tableObj instanceof DataCollection )
            {
                table = (TableDataCollection)tableObj;
            }
        }
        return table;
    }
}