package biouml.plugins.server.access;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.BeanInfoConstants;
import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.CollectionDescription;
import biouml.model.Module;
import biouml.plugins.server.EmptyQuerySystem;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementDescriptor;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.QuerySystem;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.access.exception.BiosoftNetworkException;
import ru.biosoft.access.security.CredentialsCollection;
import ru.biosoft.access.security.Permission;
import ru.biosoft.access.security.SingleSignOnSupport;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.server.ClientConnection;
import ru.biosoft.server.ClientConnectionHolder;
import ru.biosoft.server.ConnectionPool;

/**
 * Provides client access to databases and projects.
 * Each database or project corresponds to one ClientModule.
 */
public class ClientModule extends Module implements ClientConnectionHolder, CredentialsCollection, Client
{
    private ClientDataCollection cdc = null;
    protected DataElementPath pathOnServer;
    protected Permission permission = null;

    protected static Properties addStubPrimaryCollection(Properties properties)
    {
    	properties.put(DataCollectionConfigConstants.PRIMARY_COLLECTION, new VectorDataCollection<>(""));
    	return properties;
    }
    
    public ClientModule(DataCollection<?> parent, Properties properties) throws Exception
    {
        super(parent, addStubPrimaryCollection(properties));
        
        if( getInfo() != null && getInfo().getProperties() != null
                && getInfo().getProperties().getProperty(ClientDataCollection.SERVER_DATA_COLLECTION_NAME) != null )
        {
            pathOnServer = DataElementPath.create(getInfo().getProperties().getProperty(ClientDataCollection.SERVER_DATA_COLLECTION_NAME));
            if(pathOnServer.getName().isEmpty())
                pathOnServer = pathOnServer.getParentPath();
        }
        else
        {
            pathOnServer = getCompletePath();
        }
    }

    /**
     * Apply <code>ClientModuleType</code>
     */
    @Override
    protected void applyType(String className) throws Exception
    {
        type = new ClientModuleType(this);
    }

    @Override
    protected void init()
    {

    }

    @Override
    public List<CollectionDescription> getExternalTypes()
    {
        if( externalTypes == null )
        {
            try
            {
                String serverDCName = getInfo().getProperty(ClientDataCollection.SERVER_DATA_COLLECTION_NAME);
                externalTypes = ( (ClientModuleType)getType() ).getExternalCollections(serverDCName);
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "Can not fill external types list", t);
            }
        }
        return externalTypes;
    }

    /**
     * Init client data collection
     */
    protected void initCDC()
    {
        if(!isValid())
            return;
        try
        {
            if( cdc == null )
            {
                Properties properties = new Properties(getInfo().getProperties());
                properties.put(DataCollectionConfigConstants.CLASS_PROPERTY, ClientDataCollection.class.getName());
                properties.put(QuerySystem.QUERY_SYSTEM_CLASS, EmptyQuerySystem.class.getName());
                properties.put(DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY, DataCollection.class.getName());
                cdc = new ClientDataCollection(getOrigin(), properties);

                // PENDING: when it is used?
                //Properties serverProperties = cdc.getSubCollectionProperties(null);
                //if( serverProperties != null )
                //{
                //    getInfo().getProperties().put(DataCollectionConfigConstants.PLUGINS_PROPERTY,
                //            ExProperties.getPluginsString(serverProperties, "ru.biosoft.server.tomcat"));
                //}
            }

            if( cdc.getPermission() == null )
                cdc.initPermission();

            if( cdc.getPermission() != permission )
            {
                permission = cdc.getPermission();
            } 
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, getCompletePath()+": error in module initialization: "+ExceptionRegistry.log(t));
        }
    }

    @Override
    public DataElement doGet(String name) throws Exception
    {
        initCDC();
        return cdc.get(name);
    }

    @Override
    public @Nonnull List<String> getNameList()
    {
        initCDC();
        return cdc.getNameList();
    }

    @Override
    public int getSize()
    {
        initCDC();
        return cdc.getSize();
    }

    @Override
    public boolean isMutable()
    {
        initCDC();
        return cdc.isMutable();
    }

    @Override
    public boolean contains(String name)
    {
        initCDC();
        return cdc.contains(name);
    }

    @Override
    public @Nonnull Iterator iterator()
    {
        initCDC();
        return cdc.iterator();
    }

    @Override
    protected void doPut(DataElement element, boolean isNew) throws Exception
    {
        //Can not add new collection from client
    }
    @Override
    protected void doRemove(String name) throws Exception
    {
        //Can not remove collection on the server
    }

    /**
     * Preload necessary data from server in cache of data collections
     * using list of relative names. Return list of preload data elements
     * to keep from garbage collection
     */
    public List<DataElement> preload(List<String> names)
    {
        initCDC();
        if( cdc != null )
            try
            {
                return cdc.preload(names);
            }
            catch( Throwable t )
            {
                log.log(Level.SEVERE, "Preloading data error", t);
            }
        return new ArrayList<>();
    }

    @Override
    public DataElement getKernel(String relativeName)
    {
        initCDC();
        DataElementPath relativePath = DataElementPath.create(relativeName);
        if( relativePath.isDescendantOf(pathOnServer) )
        {
            relativePath = getCompletePath().getRelativePath(relativePath.getPathDifference(pathOnServer));
        }
        return super.getKernel(relativePath.toString());
    }

    public String getCompleteName(DataElement de)
    {
        initCDC();
        DataElementPath completePath = DataElementPath.create(de);
        if(completePath.isDescendantOf(getCompletePath()))
        {
            completePath = pathOnServer.getRelativePath(completePath.getPathDifference(getCompletePath()));
        }
        return completePath.toString();
    }

    // ////////////////////////////////////////////////////////////////////////
    // Client-server interactions
    //

    // implementation of interfaces - Client and ClientConnectionHolder
    
    private ClientConnection conn = null;
    public @Nonnull ClientConnection getClientConnection() throws BiosoftNetworkException
    {
        if(this.conn != null)
            return this.conn;

        try
        {
            conn = ConnectionPool.getConnection(getInfo().getProperties());
        }
        catch(Exception e)
        {
            log.log(Level.SEVERE, "Unable to get connection via props = \n" + getInfo().getProperties());
            throw new BiosoftNetworkException(e, getInfo().getProperties().getProperty(ClientDataCollection.SERVER_URL));
        }
        
        this.conn = conn;
        
        return conn;
    }

    private boolean principal;
    public boolean isPrincipal()
    {
    	return principal;
    }
    
    public void login(String username, String password)
    {
    	principal = true;
    	getInfo().setTransientValue(ClientDataCollection.AUTH_PROPERTY,
                new SingleSignOnSupport.ModuleProperties(pathOnServer.toString(), username, password));
        cdc = null;
        v_cache.clear();
        initCDC();

        cdc.login(username, password);

        //login to external data collections
        String[] externalModuleNames = getExternalModuleNames();
        for( String moduleName : externalModuleNames )
        {
            try
            {
                Module externalModule = (Module)getOrigin().get(moduleName);
                if( externalModule instanceof ClientModule )
                {
                    ( (ClientModule)externalModule ).login(username, password);
                }
            }
            catch( Exception e )
            {
                ExceptionRegistry.log(e);
            }
        }
    }

    public DataElementPath getServerPath()
    {
        return pathOnServer;
    }

    public boolean canMethodAccess(String methodName)
    {
        initCDC();
        return cdc.canMethodAccess(methodName);
    }

    public Permission getPermission()
    {
        initCDC();
        return permission;
    }

    public int getProtectionStatus()
    {
        initCDC();
        return cdc.getProtectionStatus();
    }

    @Override
    public boolean isValid()
    {
        return cdc == null || cdc.isValid();
    }

    @Override
    public void reinitialize() throws LoggedException
    {
        if(cdc != null)
            cdc.reinitialize();
    }

    @Override
    public String getVersion()
    {
        if( !isValid() )
            return null;
        return super.getVersion();
    }

    @Override
    public boolean needCredentials()
    {
        return !isValid() || getProtectionStatus() > 0;
    }

    @Override
    public Object getCredentialsBean()
    {
        return new ModuleCredentials(this);
    }

    @Override
    public void processCredentialsBean(Object bean)
    {
        ModuleCredentials credentials = (ModuleCredentials)bean;
        login( credentials.getLogin(), credentials.getPassword() );
    }

    // ////////////////////////////////////////////////////////////////////////
    // ModuleCredentials
    //

    /**
     * Each module can has its own credentials (server, login, password).
     * So different modules can be loaded from different servers.
     * Currently it is used only in BioUML workbench. 
     */
    public static class ModuleCredentials
    {
        private final DataElementPath path;
        private String login, password;

        public ModuleCredentials(ClientModule clientModule)
        {
            this.path = clientModule.getCompletePath();
        }

        @PropertyName("Login")
        public String getLogin()
        {
            return login;
        }

        public void setLogin(String login)
        {
            this.login = login;
        }

        @PropertyName("Password")
        public String getPassword()
        {
            return password;
        }

        public void setPassword(String password)
        {
            this.password = password;
        }

        public String getDescription()
        {
            return "Enter login and password to access to <b>"+path+"</b>";
        }
    }

    public static class ModuleCredentialsBeanInfo extends BeanInfoEx
    {
        public ModuleCredentialsBeanInfo()
        {
            super( ModuleCredentials.class, true );
        }

        @Override
        protected void initProperties() throws Exception
        {
            addHidden( new PropertyDescriptor( "descriptionHTML", beanClass, "getDescription", null ) );
            add("login");
            add("password");
            findPropertyDescriptor( "password" ).setValue( BeanInfoConstants.PASSWORD_FIELD, true );
        }
    }

    @Override
    public DataElementDescriptor getDescriptor(String name)
    {
        Class<? extends DataElement> type;
        try
        {
            type = get( name ).getClass();
        }
        catch( Exception e )
        {
            throw new RuntimeException( e );
        }
        boolean leaf = !DataCollection.class.isAssignableFrom( type );
        return new DataElementDescriptor( type, getInfo().getProperty( DataCollectionConfigConstants.CHILDREN_NODE_IMAGE ), leaf );
    }
}
