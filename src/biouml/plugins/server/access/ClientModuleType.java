package biouml.plugins.server.access;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.Repository;
import ru.biosoft.server.ClientConnection;
import ru.biosoft.server.Request;
import biouml.model.CollectionDescription;
import biouml.model.DiagramType;
import biouml.model.Module;
import biouml.model.ModuleType;
import biouml.plugins.server.MessageBundle;
import biouml.plugins.server.ModuleClient;

/**
 * <code>ClientModuleType</code> used by <code>ClientModule</code>
 */
public class ClientModuleType implements ModuleType
{

    protected MessageBundle messageBundle = new MessageBundle();

    protected Logger log = Logger.getLogger(ClientModuleType.class.getName());

    private ClientModule module;

    private ModuleClient connection;

    private String version;
    private Class<? extends DiagramType>[] diagramTypes;
    private Map<String, String> categories = new HashMap<>();

    private boolean isCategorySupported = false;
    private boolean isCategorySupportedInit = false;

    /**
     * ClientModuleType type can be applyed only for ClientModule
     */
    public ClientModuleType(ClientModule module)
    {
        this.module = module;
    }

    private void initConnection() throws Exception
    {
        if( connection == null )
        {
            ClientConnection conn = module.getClientConnection();
            connection = new ModuleClient(new Request(conn, log), log);
        }
    }

    /**
     * ClientModule cannot be empty
     */
    @Override
    public boolean canCreateEmptyModule()
    {
        return false;
    }

    //TODO - resolve problem of connection module and make this method
    @Override
    public Module createModule(Repository parent, String name) throws Exception
    {
        return null;
    }

    /**
     * Ask server
     */
    @Override
    public String getCategory(Class<? extends DataElement> aClass)
    {
        if( !categories.containsKey(aClass.getName()) )
        {
            try
            {
                initConnection();
                String category = connection.getModuleTypeCategory(module, aClass);
                if( category != null )
                {
                    categories.put(aClass.getName(), category);
                }
                else
                {
                    categories.put(aClass.getName(), "");
                }
            }
            catch( Throwable e )
            {
                log.log(Level.SEVERE, "Connection error: " + e.getMessage(), e);
                return null;
            }
        }
        String category = categories.get(aClass.getName());

        if( category.length() == 0 )
            return null;

        return category;
    }

    @Override
    @SuppressWarnings ( "unchecked" )
    public Class<? extends DiagramType>[] getDiagramTypes()
    {
        if( diagramTypes == null )
        {
            try
            {
                initConnection();
                diagramTypes = connection.getModuleTypeDiagramTypes(module);
            }
            catch( Throwable e )
            {
                log.log(Level.SEVERE, "Connection error: " + e.getMessage(), e);
                return new Class[0];
            }
        }
        return diagramTypes;
    }

    @Override
    public String[] getXmlDiagramTypes()
    {
        return null;
    }

    @Override
    public String getVersion()
    {
        if( version == null )
        {
            try
            {
                initConnection();
                version = connection.getModuleTypeVersion(module);
            }
            catch( Throwable e )
            {
                log.log(Level.SEVERE, "Connection error: " + e.getMessage(), e);
                return messageBundle.getResourceString("DATABASE_TYPE_UNKNOWN_VERSION");
            }
        }
        return version;
    }

    @Override
    public boolean isCategorySupported()
    {
        if( !isCategorySupportedInit )
        {
            try
            {
                initConnection();
                isCategorySupported = connection.isCategorySupported(module);
                isCategorySupportedInit = true;
            }
            catch( Throwable e )
            {
                log.log(Level.SEVERE, "Connection error: " + e.getMessage(), e);
            }
        }
        return isCategorySupported;
    }

    public List<CollectionDescription> getExternalCollections(String serverDCName) throws Exception
    {
        initConnection();
        return connection.getExternalCollections(serverDCName);
    }

    @Override
    public String getName()
    {
        return null;
    }

    @Override
    public DataCollection<?> getOrigin()
    {
        return null;
    }
}
