package biouml.plugins.server.access;

import static ru.biosoft.access.core.DataCollectionConfigConstants.CHILDREN_NODE_IMAGE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import biouml.model.Module;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementDescriptor;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementReadException;
import ru.biosoft.access.core.RepositoryAccessDeniedException;
import ru.biosoft.access.exception.BiosoftNetworkException;
import ru.biosoft.access.exception.CollectionLoginException;
import ru.biosoft.access.security.Permission;
import ru.biosoft.access.security.ProtectedDataCollection;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.security.SingleSignOnSupport;
import ru.biosoft.access.security.SingleSignOnSupport.ModuleProperties;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.InternalException;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.server.ClientConnection;
import ru.biosoft.server.ClientConnectionHolder;
import ru.biosoft.server.ConnectionPool;
import ru.biosoft.server.Request;
import ru.biosoft.util.HashMapSoftValues;
import ru.biosoft.util.ListUtil;

/**
 * Data collection to access to server.
 */
public class ClientDataCollection<T extends DataElement> extends AbstractDataCollection<T> implements ClientConnectionHolder, Client
{
    protected static final Logger log = Logger.getLogger(ClientDataCollection.class.getName());

    public static final String SERVER_URL = "host";
    public static final String SERVER_DATA_COLLECTION_NAME = "server-dc-name";
    public static final String COMMON_LOGIN_FLAG = "common-login";
    public static final String AUTH_PROPERTY = "auth";

    public static final String NO_CLIENT_CONNECTION = "no-client-connection";

    /** Name of DataCollection class on the server. */ 
    public static final String CLASS_ON_SERVER_PROPERTY = "server-class";

    public static final String CLIENT_DATA_ELEMENT_CLASS_PROPERTY = "client-data-element-class";

    protected AccessClient connection;

    protected List<String> nameList;

    protected DataElementPath serverDCname;

    protected Boolean isMutable = null;
    
    public ClientDataCollection(DataCollection<?> parent, Properties properties) throws Exception
    {
        super(parent, properties);

        serverDCname = DataElementPath.create(properties.getProperty(SERVER_DATA_COLLECTION_NAME));
        if( serverDCname == null )
            throw new IllegalArgumentException("Missing " + SERVER_DATA_COLLECTION_NAME + " property.");
        
        v_cache = new HashMapSoftValues();
        
        // initialize connection
        if( parent instanceof ClientDataCollection )
        {
            connection = ((ClientDataCollection)parent).connection;
        }
        else if( properties.getProperty( NO_CLIENT_CONNECTION ) == null )
        {        
            ClientConnection conn;

            // try to load connection from properties
            if( properties.containsKey(SERVER_URL) )
            {
                conn = ConnectionPool.getConnection(properties);
            }
            else // try to load ClientCollection from module
            {
                Module module = Module.optModule(this);
            
                // Probably this collection is not in the repository (created on the fly)
                if(module == null)
                    module = Module.optModule(getOrigin());
            
                if(module == null)
                    throw new InternalException( "No module found for " + this );
            
                conn = module.cast( ClientModule.class ).getClientConnection();
            }

            connection = new AccessClient(new Request(conn, log), log);
        }

    }

   
    // ////////////////////////////////////////////////////////////////////////
    
    /**
     * Clear all cash
     */
    public void releaseCache()
    {
        nameList = null;
        v_cache.clear();
    }

    @Override
    public void cachePut(T de)
    {
        super.cachePut(de);
    }

    /**
     * Preload in cache all required data elements
     */
    public List<DataElement> preload(Collection<String> names)
    {
        List<DataElement> des = null;
        if( !canMethodAccess("get") )
        {
            throw new RepositoryAccessDeniedException( getCompletePath(), SecurityManager.getSessionUser(),
                    DataCollectionUtils.permissionToString( permission ) );
        }
        try
        {
            des = connection.getSet(serverDCname, names, this);
        }
        catch( Throwable e )
        {
            log.log(Level.SEVERE, "Cannot preload data elements", e);
        }
        if( des == null )
            des = new ArrayList<>();
        return des;
    }

    // //////////////////////////////////////////////////////////////////////////
    // Public methods for ru.biosoft.access.core.DataCollection
    //

    @Override
    public boolean isMutable()
    {
        if( !isValid() )
            return false;
        if( !canMethodAccess("isMutable") )
        {
            throw new RepositoryAccessDeniedException( getCompletePath(), SecurityManager.getSessionUser(),
                    DataCollectionUtils.permissionToString( permission ) );
        }
        if( isMutable == null )
        {
            try
            {
                isMutable = connection.checkMutable(serverDCname);
            }
            catch( BiosoftNetworkException e )
            {
                isMutable = false;
                throw e;
            }
        }
        return connection.getConnection().isMutable() && isMutable && ( permission == null || permission.isMethodAllowed("put") );
    }

    @Override
    public int getSize()
    {
        if(!isValid())
            return 0;
        if( !canMethodAccess("getSize") )
        {
            throw new RepositoryAccessDeniedException( getCompletePath(), SecurityManager.getSessionUser(),
                    DataCollectionUtils.permissionToString( permission ) );
        }
        if( nameList == null )
            getNameList();

        if( nameList != null )
            return nameList.size();
        return 0;
    }

    @Override
    public @Nonnull List<String> getNameList()
    {
        if(!isValid())
            return ListUtil.emptyList();
        if( nameList == null )
        {
            if( !canMethodAccess("getNameList") )
            {
                throw new RepositoryAccessDeniedException( getCompletePath(), SecurityManager.getSessionUser(),
                        DataCollectionUtils.permissionToString( permission ) );
            }
            try
            {
                nameList = connection.getNameList(serverDCname);
            }
            catch( BiosoftNetworkException e )
            {
                valid = false;
                throw new DataElementReadException(e, this, "names");
            }
        }
        return Collections.unmodifiableList(nameList);
    }

    @Override
    public boolean contains(String name)
    {
        if( !canMethodAccess("contains") )
            throw new RepositoryAccessDeniedException( getCompletePath(), SecurityManager.getSessionUser(),
                    DataCollectionUtils.permissionToString( permission ) );
        if( nameList == null )
            getNameList();

        if( nameList != null )
            return nameList.indexOf(name) >= 0;

        try
        {
            return connection.containsEntry(serverDCname, name);
        }
        catch( BiosoftNetworkException e )
        {
            ExceptionRegistry.log(e);
        }
        return false;
    }

    @Override
    public @Nonnull Iterator<T> iterator()
    {
        if(!isValid())
            return ListUtil.emptyIterator();
        if( !canMethodAccess("iterator") )
            throw new RepositoryAccessDeniedException( getCompletePath(), SecurityManager.getSessionUser(),
                    DataCollectionUtils.permissionToString( permission ) );
        getNameList();
        if( nameList != null )
        {
            loadAllElementsToCache();
            return AbstractDataCollection.createDataCollectionIterator( this, nameList.iterator() );
        }
        return ListUtil.emptyIterator();
    }

    //optimizations for opening big data collections in RepositoryPane
    private Class<? extends DataElement> dataElementType = null;
    @Override
    public @Nonnull Class<? extends DataElement> getDataElementType()
    {
        if( dataElementType == null )
        {
            dataElementType = super.getDataElementType();
        }
        return dataElementType;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected T doGet(String name) throws Exception
    {
        if( name == null )
            return null;

        if( !canMethodAccess("get") )
        {
            throw new RepositoryAccessDeniedException( getCompletePath(), SecurityManager.getSessionUser(),
                    DataCollectionUtils.permissionToString( permission ) );
        }
        
        if( getDataElementType().getName().equals(DataCollection.class.getName()) )
        {
            Properties properties = getChildCollectionProperties(name);
            if( properties != null )
            {
                T loadedCollection = (T)CollectionFactory.createCollection(this, properties);
                return loadedCollection;
            }
        }
        else
        {
            if( contains(name) )
            {
                String entry = connection.getEntry(serverDCname, name);
                if( entry != null && !entry.equals("null") )
                {
                    DataElement de = connection.convertToDataElement(this, name, entry);
                    return (T)de;
                }
            }
        }

        return null;
    }

    protected Properties getChildCollectionProperties(String name) throws Exception
    {
        Properties properties = connection.getClientCollectionProperties(serverDCname, name);
        if( properties != null )
        {
            properties.put( DataCollectionConfigConstants.CLASS_PROPERTY, ClientDataCollectionResolver.getCounterpart( properties ) );

            // pending - for compatibility
            if( "biouml.plugins.server.access.DataClientCollection".equals( properties.get( DataCollectionConfigConstants.CLASS_PROPERTY)) )
            {
                properties.put( DataCollectionConfigConstants.CLASS_PROPERTY, ClientDataCollection.class.getName() );
            }
        }

        return properties;
    }
    

    @Override
    protected void doPut(T obj, boolean isNew) throws Exception
    {
        if( !canMethodAccess("put") )
        {
            throw new RepositoryAccessDeniedException( getCompletePath(), SecurityManager.getSessionUser(),
                    DataCollectionUtils.permissionToString( permission ) );
        }
        String entry = connection.convertToString(obj);
        connection.writeEntry(serverDCname, obj.getName(), entry);
        if( nameList != null && !nameList.contains(obj.getName()) )
            nameList.add(obj.getName());
    }

    @Override
    protected void doRemove(String name) throws Exception
    {
        if( !canMethodAccess("remove") )
        {
            throw new RepositoryAccessDeniedException( getCompletePath(), SecurityManager.getSessionUser(),
                    DataCollectionUtils.permissionToString( permission ) );
        }
        connection.removeEntry(serverDCname, name);
        release(name);
        nameList.remove(name);
    }

    @Override
    public void close() throws Exception
    {
        connection.close();
        super.close();
    }

    protected void loadAllElementsToCache()
    {
        Set<String> toLoadList = new LinkedHashSet<>(nameList);
        toLoadList.removeAll( v_cache.keySet() );
        preload(toLoadList);
    }

   
    // ////////////////////////////////////////////////////////////////////////
    // Protection and permission issues
    // 

    protected Integer protectedStatus = null;
    public boolean isProtected() 
    {
        return getProtectionStatus() > 0;
    }

    public int getProtectionStatus()
    {
        if( !isValid() )
            return ProtectedDataCollection.PROTECTION_NOT_APPLICABLE;
        try
        {
            if( protectedStatus == null )
            {
                protectedStatus = connection.checkProtected(serverDCname);
            }
            return protectedStatus;
        }
        catch( BiosoftNetworkException e )
        {
            e.log();
            protectedStatus = ProtectedDataCollection.PROTECTION_PROTECTED_READ_ONLY;
            return protectedStatus;
        }
    }
    
    protected Permission permission = null;
    public Permission getPermission()
    {
        return permission;
    }

    protected boolean setPermission(Permission newPermission)
    {
        Permission oldPermission = permission;
        if( oldPermission != null && ru.biosoft.access.core.DataCollection.class.isAssignableFrom(getDataElementType()) )
        {
            if( v_cache != null )
            {
                try
                {
                    for( Object childDC : v_cache.values() )
                    {
                        if( childDC instanceof ClientDataCollection )
                        {
                            Permission p = ( (ClientDataCollection)childDC ).getPermission();
                            if( p == null || p == oldPermission )
                            {
                                ( (ClientDataCollection)childDC ).setPermission(newPermission);
                            }
                        }
                    }
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE, "can not change permissions for child", e);
                }
            }
        }

        //refresh name list after login
        nameList = null;

        this.permission = newPermission;
        return ( oldPermission == null || oldPermission.getPermissions() != newPermission.getPermissions() || !oldPermission.getUserName()
                .equals(newPermission.getUserName()) );
    }

    /**
     * 
     */
    public void initPermission() throws LoggedException
    {
        try
        {
            if( !isPrincipal() )
            {
                // get parent permissions
                DataCollection parent = this;
                while( (parent = parent.getOrigin()) != null )
                {
                    if( parent instanceof Client )
                    {
                        permission = ((Client)parent).getPermission();
                        break;
                    }
                }
                
                permission = connection.getPermissions(serverDCname, permission);
                return;
            }
            
            // @pending - old code for compatibility, needs refactoring
            // @pending relogin
            if( isProtected() && ( permission == null || permission.getExpirationTime() < System.currentTimeMillis() + timeBeforeExpire ) )
            {
                SingleSignOnSupport.ModuleProperties moduleProperties = null;
                if( SingleSignOnSupport.isSSOUsed() )
                {
                    if( SingleSignOnSupport.getActiveUser() == null )
                    {
                        //cancel initialization if SSO user not logged in
                        return;
                    }
                    moduleProperties = SingleSignOnSupport.getModuleProperties(serverDCname);
                    login(moduleProperties.getUsername(), moduleProperties.getPassword());
                }
                if( moduleProperties == null )
                {
                    Module currentModule = Module.optModule(this);
                    if( currentModule instanceof ClientModule )
                    {
                        Object val = currentModule.getInfo().getTransientValue(AUTH_PROPERTY);
                        if(val instanceof ModuleProperties)
                        {
                            moduleProperties = (ModuleProperties)val;
                            login(moduleProperties.getUsername(), moduleProperties.getPassword());
                        } 
                        else
                        {
                            login("", "");
                        }
                    }
                    else
                    {
                        login("", "");
                    }
                }
            }
        }
        catch( Throwable t )
        {
            valid = false;
            throw new DataElementReadException(t, this, "permissions");
        }
    }

    public boolean canMethodAccess(String methodName)
    {
        initPermission();

        return ( !isProtected() || ( permission != null && permission.isMethodAllowed(methodName) ) );
    }

    // ////////////////////////////////////////////////////////////////////////
    // Principal and credential issues
    // 


    private static final long timeBeforeExpire = 5000;//if expiration time is less than 5 second from current time than we should relogin
    private String user;
    private String password;

    private boolean principal;
    public boolean isPrincipal()
    {
        return principal;
    }

    public @Nonnull ClientConnection getClientConnection() throws BiosoftNetworkException
    {
        return connection.getConnection().getConnection();
    }
    
    /**
     * Login to data collection
     */
    public void login(String username, String password) throws CollectionLoginException
    {
        // store this information for relogin
        principal = true;
        user      = user;
        password  = password; 
        
        try
        {
            if( isProtected() )
            {
                Permission permission = null;
                String commonLoginFlag = getInfo().getProperty(COMMON_LOGIN_FLAG);
                if( Boolean.parseBoolean(commonLoginFlag) )
                {
                    Permission p = connection.login(null, username, password);
                    if(!p.isInfoAllowed())
                        throw new CollectionLoginException(this, username);
                    permission = new Permission(Permission.READ | Permission.INFO, p.getUserName(), p.getSessionId(), p.getExpirationTime());
                }
                else
                {
                    Permission p = connection.login(serverDCname, username, password);
                    if(!p.isInfoAllowed())
                        throw new CollectionLoginException(this, username);

                    String nameOnServer = serverDCname.getName();
                    if( "data".equals(nameOnServer) || "databases".equals(nameOnServer))
                        permission = new Permission(Permission.READ | Permission.INFO, p.getUserName(), p.getSessionId(), p.getExpirationTime());
                    else
                        permission = p;
                }
                
                setPermission(permission);

                //save username and password to SSO structure if necessary
                if( SingleSignOnSupport.isSSOUsed() )
                {
                    SingleSignOnSupport.setModuleProperties(new SingleSignOnSupport.ModuleProperties(serverDCname.toString(), username,
                            password));
                }
            }
        }
        catch( CollectionLoginException e )
        {
            throw e;
        }
        catch( Exception e )
        {
            throw new CollectionLoginException(e, this, username);
        }
    }

    // ////////////////////////////////////////////////////////////////
    // Used by ClientModule
    // 
    
    public Properties getSubCollectionProperties(String name) throws Exception
    {
        initPermission();
        return connection.getClientCollectionProperties(serverDCname, name);
    }


    @Override
    public DataElementDescriptor getDescriptor(String name)
    {
        Class<? extends DataElement> type;
        try
        {
            type = get(name).getClass();
        }
        catch( Exception e )
        {
            throw new RuntimeException(e);
        }
        boolean leaf = !DataCollection.class.isAssignableFrom( type );
        return new DataElementDescriptor( type, getInfo().getProperty(CHILDREN_NODE_IMAGE), leaf );
    }
    
    @Override
    public boolean isAcceptable(Class<? extends DataElement> clazz)
    {
        return true;
    }
}
