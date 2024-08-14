package biouml.plugins.hemodynamics.server;

import java.util.Map;

import biouml.model.Diagram;
import biouml.model.xml.XmlDiagramType;
import biouml.plugins.hemodynamics.HemodynamicsEModel;
import biouml.plugins.server.access.AccessProtocol;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.security.SessionCache;
import ru.biosoft.access.security.SessionCacheManager;
import ru.biosoft.server.Connection;
import ru.biosoft.server.Response;
import ru.biosoft.server.Service;
import ru.biosoft.server.SynchronizedServiceSupport;
import ru.biosoft.table.access.TableResolver;

public class HemodynamicsService extends HemodynamicsProtocol implements Service
{
    protected SynchronizedServiceSupport ss;
    protected Response connection;
    protected Map arguments;

    public HemodynamicsService()
    {
        ss = new SynchronizedServiceSupport()
        {
            @Override
            protected boolean processRequest(int command) throws Exception
            {
                return HemodynamicsService.this.processRequest(command);
            }
        };
    }

    @Override
    public void processRequest(Integer command, Map data, Response out)
    {
        ss.processRequest(command, data, out);
    }

    protected boolean processRequest(int command) throws Exception
    {
        connection = ss.getSessionConnection();
        arguments = ss.getSessionArguments();
        switch( command )
        {
            case HemodynamicsProtocol.HD_IS_VISIBLE:
                sendCanExplore();
                break;
            case HemodynamicsProtocol.HD_GET_TABLE:
                putTableResolverToCache();
                break;
            default:
                return false;
        }
        return true;
    }

    //////////////////////////////////////////////
    // Protocol implementation functions
    //
    protected void sendCanExplore() throws Exception
    {
        Object dePath = arguments.get(AccessProtocol.KEY_DE);
        if( dePath == null )
        {
            connection.error("didn't send diagram name");
            return;
        }
        String completeName = dePath.toString();
        DataElement de = CollectionFactory.getDataElement(completeName);
        if( de instanceof Diagram )
        {
            if( ( (Diagram)de ).getType() instanceof XmlDiagramType )
            {
                String notationName = ( (XmlDiagramType) ( (Diagram)de ).getType() ).getName();
                if( ( notationName != null ) && ( notationName.equals("arterialTree.xml") ) )
                {
                    connection.send("ok".getBytes("UTF-16BE"), Connection.FORMAT_SIMPLE);
                    return;
                }
            }
        }
        connection.error("diagram is not hemodynamics");
    }

    protected void putTableResolverToCache() throws Exception
    {
        String sessionID = ( arguments.get(SecurityManager.SESSION_ID) ).toString();
        SessionCache sessionCache = SessionCacheManager.getSessionCache(sessionID);
        String completeName = HD_TABLE_RESOLVER;
        Object resolverObj = sessionCache.getObject(completeName);
        if( resolverObj == null )
        {
            HemodynamicsTableResolver resolver = new HemodynamicsTableResolver(sessionID);
            sessionCache.addObject(completeName, resolver, true);
        }
        connection.send(completeName.getBytes("UTF-16BE"), Connection.FORMAT_GZIP);
    }


    public static DataCollection getVesselTableDataCollection(DataElementPath diagramPath)
    {
        DataElement de = diagramPath.optDataElement();
        if( ! ( de instanceof Diagram ) )
        {
            return null;
        }
        
        Diagram diagram = (Diagram)de;
        if (!(diagram.getRole() instanceof HemodynamicsEModel))
        {
            return null;
        }

        return diagram.getRole(HemodynamicsEModel.class).getVessels();
    }

    //Table resolver
    public static class HemodynamicsTableResolver extends TableResolver
    {
        private final String sessionID;
        public HemodynamicsTableResolver(String sID)
        {
            sessionID = sID;
        }

        @Override
        public DataCollection getTable(DataElement de)
        {
            DataCollection table = null;
            DataElementPath diagramPath = de.cast( Diagram.class ).getCompletePath();
            SessionCache sessionCache = SessionCacheManager.getSessionCache(sessionID);
            String completeName = HD_VESSELSTABLE + diagramPath;
            Object tableObj = sessionCache.getObject(completeName);
            if( tableObj == null )
            {
                table = getVesselTableDataCollection(diagramPath);
                sessionCache.addObject(completeName, table, true);
            }
            else if( tableObj instanceof DataCollection )
            {
                table = (DataCollection)tableObj;
            }
            return table;
        }
    }
}
