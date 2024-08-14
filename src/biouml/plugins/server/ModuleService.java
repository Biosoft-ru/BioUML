package biouml.plugins.server;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.server.Connection;
import ru.biosoft.server.Response;
import ru.biosoft.server.SynchronizedServiceSupport;
import biouml.model.CollectionDescription;
import biouml.model.Module;

/**
 * Network supporting of ClientModuleType
 */
public class ModuleService extends SynchronizedServiceSupport
{

    protected Response connection;
    protected Map arguments;


    /** The data collection used for data transfer. */
    protected Module module;

    /**
     * Set up the specified data collection.<p>
     *
     * If such data collection is not loaded on the Server
     * the error message will be sent to the client.
     */
    protected boolean setModule() throws IOException
    {
        module = null;
        DataCollection dc = getDataCollection();
        if( dc instanceof Module )
        {
            module = (Module)dc;
            return true;
        }
        else
        {
            String name = dc == null ? "" : " " + dc.getCompletePath();
            connection.error("cannot find module" + name);
            return false;
        }
    }

    @Override
    protected boolean processRequest(int command) throws Exception
    {
        connection = getSessionConnection();
        arguments = getSessionArguments();

        switch( command )
        {
            case ModuleProtocol.DB_MODELE_TYPE_VERSION:
                sendVersion();
                break;
            case ModuleProtocol.DB_MODELE_TYPE_DIAGRAM_TYPES:
                sendDiagramTypes();
                break;
            case ModuleProtocol.DB_MODELE_TYPE_CATEGORY:
                sendCategory();
                break;
            case ModuleProtocol.DB_MODELE_TYPE_CHECK_SUPPORT:
                sendCheck();
                break;
            case ModuleProtocol.DB_MODELE_EXTERNAL_COLLECTIONS:
                sendExternalCollections();
                break;

            default:
                return false;
        }
        return true;
    }

    //////////////////////////////////////////////
    // Protocol implementation functions
    //

    protected void sendVersion() throws Exception
    {
        if( setModule() )
        {
            connection.send( ( "" + module.getVersion() ).getBytes("UTF-16BE"), Connection.FORMAT_SIMPLE);
        }
    }

    protected void sendDiagramTypes() throws Exception
    {
        if( setModule() )
        {
            Class[] types = module.getType().getDiagramTypes();
            if( types != null && types.length > 0 )
            {
                StringBuffer buffer = new StringBuffer();
                for( Class type : types )
                {
                    buffer.append(type.getName());
                    buffer.append("\n");
                }
                connection.send(buffer.toString().getBytes("UTF-16BE"), Connection.FORMAT_SIMPLE);
                return;
            }
            connection.send("null".getBytes("UTF-16BE"), Connection.FORMAT_SIMPLE);
        }
    }

    protected void sendCategory() throws Exception
    {
        if( setModule() )
        {
            Object className = arguments.get(ModuleProtocol.KEY_CLASS);
            if( !(className instanceof String) )
            {
                connection.error("didn't send class name");
                return;
            }
            Class<? extends DataElement> clazz = ClassLoading.loadSubClass( (String)className, DataElement.class );
            String types = module.getType().getCategory(clazz);
            connection.send( ( "" + types ).getBytes("UTF-16BE"), Connection.FORMAT_SIMPLE);
        }
    }

    protected void sendCheck() throws Exception
    {
        if( setModule() )
        {
            if( module.getType().isCategorySupported() )
                connection.send("true".getBytes("UTF-16BE"), Connection.FORMAT_SIMPLE);
            else
                connection.send("false".getBytes("UTF-16BE"), Connection.FORMAT_SIMPLE);
        }
    }

    public static class QueryOptionsEntry implements Serializable
    {
        public String className;
        public String entry;
        public QueryOptionsEntry(String className, String entry)
        {
            this.className = className;
            this.entry = entry;
        }
    }

    protected void sendExternalCollections() throws Exception
    {
        if( setModule() )
        {
            StringBuffer result = new StringBuffer();
            List<CollectionDescription> externalTypes = module.getExternalTypes();
            if( externalTypes != null )
            {
                for( CollectionDescription cd : externalTypes )
                {
                    result.append(cd.getModuleName());
                    result.append("@");
                    result.append(cd.getSectionName());
                    result.append("@");
                    result.append(cd.getTypeName());
                    result.append("@");
                    result.append(cd.isReadOnly());
                    result.append("\n\r");
                }
            }
            connection.send( ( "" + result.toString() ).getBytes("UTF-16BE"), Connection.FORMAT_SIMPLE);
        }
    }
}
