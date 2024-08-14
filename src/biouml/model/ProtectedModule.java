package biouml.model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nonnull;

import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.LocalRepository;
import ru.biosoft.access.ProtectedElement;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataCollectionEvent;
import ru.biosoft.access.core.DataCollectionInfo;
import ru.biosoft.access.core.DataCollectionListener;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementDescriptor;
import ru.biosoft.access.core.QuerySystem;
import ru.biosoft.access.core.RepositoryAccessDeniedException;
import ru.biosoft.access.security.CodePrivilege;
import ru.biosoft.access.security.CodePrivilegeType;
import ru.biosoft.access.security.Permission;
import ru.biosoft.access.security.ProtectedDataCollectionInfo;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.util.ExProperties;

/**
 * Module with security support
 * 
 * @author tolstyh
 */
@CodePrivilege(CodePrivilegeType.REPOSITORY)
public class ProtectedModule extends Module implements ProtectedElement
{
    public static final String NEXT_CONFIG_NAME = "default.primary.config";
    private List<DataCollectionListener> initListeners;
    
    protected void check(String method) throws RepositoryAccessDeniedException
    {
        SecurityManager.check(primaryCollection.getCompletePath(), method);
    }

    /**
     * Adds protection configuration to the given module
     * @param module
     * @throws Exception
     */
    public static ProtectedModule protect(Module module, int protectionStatus) throws Exception
    {
        File root = ((LocalRepository)module.getPrimaryCollection()).getRootDirectory();
        File config = new File(root, DataCollectionConfigConstants.DEFAULT_CONFIG_FILE);
        File nextConfig = new File(root, NEXT_CONFIG_NAME);
        if(!config.renameTo(nextConfig))
        {
            throw new IOException( "Unable to rename "+config+" to "+nextConfig );
        }
        Properties properties = new Properties();
        properties.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, module.getName());
        String plugins = module.getInfo().getProperty( DataCollectionConfigConstants.PLUGINS_PROPERTY );
        if(plugins != null)
            properties.setProperty( DataCollectionConfigConstants.PLUGINS_PROPERTY, plugins );
        properties.setProperty( DataCollectionConfigConstants.CLASS_PROPERTY, ProtectedModule.class.getName() );
        properties.setProperty(DataCollectionConfigConstants.NEXT_CONFIG, NEXT_CONFIG_NAME);
        properties.setProperty("protectionStatus", String.valueOf(protectionStatus));
        ExProperties.store(properties, config);
        DataCollection repository = module.getOrigin().getCompletePath().getDataCollection();
        properties.put(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, root.getPath());
        DataCollection result = CollectionFactory.createCollection( repository, properties );
        repository.put(result);
        return (ProtectedModule)result;
    }
    
    /**
     * Constructor
     */
    public ProtectedModule(DataCollection<?> parent, Properties properties) throws Exception
    {
        super(parent, properties);
        Iterator<Object> iter = properties.keySet().iterator();
        while( iter.hasNext() )
        {
            String key = iter.next().toString();
            primaryCollection.getInfo().getProperties().put(key, properties.getProperty(key));
        }
        if(initListeners != null)
        {
            for(DataCollectionListener listener: initListeners)
            {
                addDataCollectionListener(listener);
            }
        }
        initListeners = null;
    }
    
    @Override
    protected void initModule()
    {
        // Copying properties for protected module is unnecessary
    }

    @Override
    public int getSize()
    {
        check("getSize");
        return primaryCollection.getSize();
    }

    @Override
    public @Nonnull Class<? extends DataElement> getDataElementType()
    {
        check("getDataElementType");
        return primaryCollection.getDataElementType();
    }

    @Override
    public boolean isMutable()
    {
        Permission permission = SecurityManager.getPermissions(getCompletePath());
        return permission.isAllowed(Permission.WRITE) && primaryCollection.isMutable();
    }

    @Override
    public @Nonnull List<String> getNameList()
    {
        check("getNameList");
        return primaryCollection.getNameList();
    }

    @Override
    public DataCollectionInfo getInfo()
    {
        if( primaryCollection == null )
        {
            //this code using when primary data collection not initialized yet
            return super.getInfo();
        }
        check("getInfo");
        return new ProtectedDataCollectionInfo( this, primaryCollection.getInfo() );
    }

    @Override
    public boolean contains(DataElement element)
    {
        check("contains");
        return primaryCollection.contains(element);
    }

    @Override
    public boolean contains(String name)
    {
        check("contains");
        return primaryCollection.contains(name);
    }

    @Override
    public DataElement get(String name) throws Exception
    {
        Permission permission = SecurityManager.getPermissions(primaryCollection.getCompletePath());
        if(permission.isMethodAllowed("get"))
        {
            return primaryCollection.get(name);
        } else if(permission.isInfoAllowed())
        {
            Permission childPermission = SecurityManager.getPermissions(primaryCollection.getCompletePath().getChildPath(name));
            if(childPermission.isInfoAllowed()) return primaryCollection.get(name);
        }
        throw new RepositoryAccessDeniedException( primaryCollection.getCompletePath(), SecurityManager.getSessionUser(), "Read" );
    }

    @Override
    public DataElement put(DataElement element)
    {
        check("put");
        return primaryCollection.put(element);
    }

    @Override
    public void remove(String name) throws Exception, UnsupportedOperationException
    {
        check("remove");
        primaryCollection.remove(name);
    }

    @Override
    public @Nonnull Iterator<DataElement> iterator()
    {
        check("iterator");
        return primaryCollection.iterator();
    }

    @Override
    public void close() throws Exception
    {
        check("close");
        primaryCollection.close();
    }

    @Override
    public void release(String name)
    {
        check("release");
        primaryCollection.release(name);
    }

    @Override
    public DataElement getFromCache(String dataElementName)
    {
        check("getFromCache");
        return primaryCollection.getFromCache(dataElementName);
    }

    @Override
    public void addDataCollectionListener(DataCollectionListener listener)
    {
        if(primaryCollection == null)
        {
            if( initListeners == null )
                initListeners = new ArrayList<>();
            initListeners.add(listener);
        }
        else
        {
            check("addDataCollectionListener");
            primaryCollection.addDataCollectionListener(listener);
        }
    }

    @Override
    public void removeDataCollectionListener(DataCollectionListener listener)
    {
        check("removeDataCollectionListener");
        primaryCollection.removeDataCollectionListener(listener);
    }

    @Override
    public void propagateElementWillChange(DataCollection source, DataCollectionEvent primaryEvent)
    {
        check("propagateElementWillChange");
        primaryCollection.propagateElementWillChange(source, primaryEvent);
    }
    @Override
    public void propagateElementChanged(DataCollection source, DataCollectionEvent primaryEvent)
    {
        check("propagateElementChanged");
        primaryCollection.propagateElementChanged(source, primaryEvent);
    }

    @Override
    public boolean isPropagationEnabled()
    {
        check("isPropagationEnabled");
        return primaryCollection.isPropagationEnabled();
    }

    @Override
    public void setPropagationEnabled(boolean propagationEnabled)
    {
        check("setPropagationEnabled");
        primaryCollection.setPropagationEnabled(propagationEnabled);
    }

    @Override
    public boolean isNotificationEnabled()
    {
        check("isNotificationEnabled");
        return primaryCollection.isNotificationEnabled();
    }

    @Override
    public void setNotificationEnabled(boolean isEnabled)
    {
        check("setNotificationEnabled");
        primaryCollection.setNotificationEnabled(isEnabled);
    }

    @Override
    public ModuleType getType()
    {
        check("getInfo");
        return ((Module)primaryCollection).getType();
    }

    @Override
    public String getVersion()
    {
        check("getInfo");
        return ((Module)primaryCollection).getVersion();
    }

    @Override
    protected void applyType(String className) throws Exception
    {

    }

    @Override
    public <T extends DataElement> DataCollection<T> getCategory(Class<T> c)
    {
        check("get");
        return ((Module)primaryCollection).getCategory(c);
    }

    @Override
    public File getPath()
    {
        check("getInfo");
        return ((Module)primaryCollection).getPath();
    }

    @Override
    public DataElement getKernel(String relativeName)
    {
        check("get");
        return ((Module)primaryCollection).getKernel(relativeName);
    }

    @Override
    public <T extends DataElement> T getKernel(Class<T> c, String name) throws Exception
    {
        check("get");
        return ((Module)primaryCollection).getKernel(c, name);
    }

    @Override
    public void putKernel(DataElement kernel) throws Exception
    {
        check("put");
        ((Module)primaryCollection).putKernel(kernel);
    }

    @Override
    public DataCollection<Diagram> getDiagrams()
    {
        check("get");
        return ((Module)primaryCollection).getDiagrams();
    }

    @Override
    public void putDiagram(Diagram diagram) throws Exception
    {
        check("put");
        ((Module)primaryCollection).putDiagram(diagram);
    }

    @Override
    public List<CollectionDescription> getExternalTypes()
    {
        check("getInfo");
        return ((Module)primaryCollection).getExternalTypes();
    }

    @Override
    public CollectionDescription[] getExternalCategories(Class<?> c) throws Exception
    {
        check("getInfo");
        return ((Module)primaryCollection).getExternalCategories(c);
    }

    @Override
    public QuerySystem[] getExternalLuceneFacades()
    {
        check("getInfo");
        return ((Module)primaryCollection).getExternalLuceneFacades();
    }

    @Override
    public @Nonnull String[] getExternalModuleNames()
    {
        check("getInfo");
        return ((Module)primaryCollection).getExternalModuleNames();
    }

    @Override
    public Module[] getExternalModules(String relativeName)
    {
        check("getInfo");
        return ((Module)primaryCollection).getExternalModules(relativeName);
    }

    @Override
    public boolean isAcceptable(Class clazz)
    {
        check("isAcceptable");
        return primaryCollection.isAcceptable(clazz);
    }

    @Override
    public DataElement getUnprotectedElement(int access) throws RepositoryAccessDeniedException
    {
        if(access == 0) return primaryCollection;
        Permission permission = SecurityManager.getPermissions(getCompletePath());
        if(!permission.isAllowed(access))
            throw new RepositoryAccessDeniedException( getCompletePath(), SecurityManager.getSessionUser(),
                    DataCollectionUtils.permissionToString( permission ) );
        return primaryCollection;
    }

    @Override
    public DataCollection<DataElement> getPrimaryCollection()
    {
        throw new java.lang.SecurityException("Direct access to primary collection is disabled");
    }

    @Override
    public DataElementDescriptor getDescriptor(final String name)
    {
        check("getDescriptor");
        return primaryCollection.getDescriptor(name);
    }
}
