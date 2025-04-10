package ru.biosoft.access.generic;

import java.io.File;
import java.security.InvalidParameterException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import com.developmentontheedge.application.ApplicationUtils;

import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import one.util.streamex.EntryStream;
import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.DefaultQuerySystem;
import ru.biosoft.access.Entry;
import ru.biosoft.access.EntryCollection;
import ru.biosoft.access.FileEntryCollection2;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.LocalRepository;
import ru.biosoft.access.Repository;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataCollectionInfo;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementCreatingException;
import ru.biosoft.access.core.DataElementDescriptor;
import ru.biosoft.access.core.DataElementPutException;
import ru.biosoft.access.core.DataElementReadException;
import ru.biosoft.access.core.DerivedDataCollection;
import ru.biosoft.access.core.QuerySystem;
import ru.biosoft.access.core.RepositoryAccessDeniedException;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.exception.DataElementExistsException;
import ru.biosoft.access.exception.ParameterNotAcceptableException;
import ru.biosoft.access.history.HistoryFacade;
import ru.biosoft.access.security.CodePrivilege;
import ru.biosoft.access.security.CodePrivilegeType;
import ru.biosoft.access.security.NetworkRepository;
import ru.biosoft.access.security.Permission;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.sql.SqlConnectionHolder;
import ru.biosoft.access.sql.SqlConnectionPool;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.InternalException;
import ru.biosoft.exception.MissingParameterException;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.ExProperties;
import ru.biosoft.util.HashMapWeakValues;
import ru.biosoft.util.TextUtil2;

/**
 * Special implementation of DataCollection to store different type's elements in one collection.
 */
@CodePrivilege(CodePrivilegeType.REPOSITORY)
public class GenericDataCollection extends DerivedDataCollection<ru.biosoft.access.core.DataElement,Entry> implements SqlConnectionHolder, FolderCollection
{
    protected static final Logger log = Logger.getLogger(GenericDataCollection.class.getName());

    public static final String PREFERED_TABLE_IMPLEMENTATION_PROPERTY = "PreferedTableImplementation";

    public static final String SKIP_PARENT = "skip-parent";
    public static final String SKIP_UPDATE_SIZES = "skip-update-sizes";

    protected DataElementInfoTransformer entryTransformer;
    //cache for opened base collections
    protected Map<String, DataCollection> collectionCache = new ConcurrentHashMap<>();
    private final Object lock = new Object();
    private final Set<String> removingNames = ConcurrentHashMap.newKeySet();
    private final Set<String> savingNames = ConcurrentHashMap.newKeySet();
    protected volatile long diskSize = -1;
    private final TObjectLongMap<String> childSizes = new TObjectLongHashMap<>(10, 0.75f, -1);

    public GenericDataCollection(DataCollection<?> parent, Properties properties) throws Exception
    {
        super(parent, properties);
        entryTransformer = new DataElementInfoTransformer();
        entryTransformer.init(primaryCollection, null);
        getInfo().getProperties().setProperty(QuerySystem.INDEX_LIST, "title");
        getInfo().getProperties().setProperty("index.title", GenericTitleIndex.class.getName());
        DefaultQuerySystem qs = new DefaultQuerySystem(this);
        getInfo().setQuerySystem(qs);
        initSize();
    }

    public void setDescription(String description)
    {
        getInfo().setDescription( TextUtil2.nullToEmpty( description ) );
        try
        {
            getCompletePath().save(getCompletePath().getDataElement());
        }
        catch( Exception e )
        {
            ExceptionRegistry.log(e);
        }
    }

    public String getRootDirectory()
    {
        String dir = getInfo().getProperties().getProperty(DataCollectionConfigConstants.FILE_PATH_PROPERTY, getInfo().getProperty(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY));
        if( dir == null )
        {
            DataCollection parent = getOrigin();
            if( parent instanceof GenericDataCollection )
            {
                dir = ( (GenericDataCollection)parent ).getRootDirectory() + getName();
            }
        }
        if( dir == null )
            throw new DataElementReadException(this, DataCollectionConfigConstants.CONFIG_PATH_PROPERTY);
        if( !dir.endsWith(File.separator) )
            dir += File.separator;
        return dir;
    }

    List<String> nameList;

    @Override
    public @Nonnull List<String> getNameList()
    {
        if(nameList == null)
        {
            ArrayList<String> result;
            try
            {
                result = new ArrayList<>(super.getNameList());
            }
            catch( Exception e1 )
            {
                throw new DataElementReadException(e1, this);
            }
            Collections.sort(result, (name1, name2) -> {
                try
                {
                    int priority1 = 0, priority2 = 0;
                    DataElementInfo dei1 = getChildInfo(name1);
                    DataElementInfo dei2 = getChildInfo(name2);
                    if(dei1.getProperty(DataCollectionConfigConstants.CLASS_PROPERTY).equals(GenericDataCollection.class.getName()))
                        priority1 = 1;
                    if(dei2.getProperty(DataCollectionConfigConstants.CLASS_PROPERTY).equals(GenericDataCollection.class.getName()))
                        priority2 = 1;
                    String displayName1 = dei1.getProperty(DataCollectionConfigConstants.DISPLAY_NAME_PROPERTY);
                    if(displayName1 == null) displayName1 = name1;
                    String displayName2 = dei2.getProperty(DataCollectionConfigConstants.DISPLAY_NAME_PROPERTY);
                    if(displayName2 == null) displayName2 = name2;
                    return priority1 != priority2 ? priority2 - priority1 : displayName1.compareToIgnoreCase(displayName2);
                }
                catch( Exception e )
                {
                    return name1.compareTo(name2);
                }
            });
            nameList = result;
        }
        return nameList;
    }

    protected DataElementTypeDriver lookForDriver(DataElement de) throws Exception
    {
        return lookForDriver(de.getClass());
    }

    protected DataElementTypeDriver lookForDriver(Class<? extends DataElement> childClass) throws Exception
    {
        if( TableDataCollection.class.equals(childClass) )
        {
            childClass = getPreferedTableImplementation().getTableClass();
        }
        return DataElementTypeRegistry.lookForDriver(childClass);
    }

    public DataCollection getTypeSpecificCollection(Class<? extends DataElement> child)
    {
        try
        {
            return getTypeSpecificCollection(lookForDriver(child));
        }
        catch( Exception e )
        {
            return null;
        }
    }

    /**
     * Returns real parent for child elements. Useful when collection is wrapped to some DerivedDataCollection or something like this
     */
    public DataCollection getRealParent()
    {
        String skipParent = getInfo().getProperty(SKIP_PARENT);
        if(!Boolean.parseBoolean(skipParent)) return this;
        return getOrigin();
    }

    public DataCollection getTypeSpecificCollection(DataElementTypeDriver driver)
    {
        return collectionCache.computeIfAbsent( driver.getClass().getName(), k -> {
            DataCollection<?> collection = driver.createBaseCollection( this );
            if( collection != GenericDataCollection.this )
            {
                collection.setNotificationEnabled( false );
                collection.setPropagationEnabled( false );
            }
            return collection;
        } );
    }

    @Override
    public @Nonnull Class<? extends DataElement> getDataElementType()
    {
        return ru.biosoft.access.core.DataElement.class;
    }

    @Override
    public DataElement doGet(String name) throws Exception
    {
        DataElementInfo dei = getChildInfo(name);
        return dei == null ? null : DataElementTypeRegistry.doGet(this, dei);
    }

    @Override
    public void removeFromCache(String dataElementName)
    {
        super.removeFromCache( dataElementName );
        for(DataCollection<?> baseCollection : collectionCache.values())
        {
            if(baseCollection != this && baseCollection instanceof AbstractDataCollection)
            {
                ( (AbstractDataCollection<?>)baseCollection ).removeFromCache( dataElementName );
            }
        }
        childInfoCache.remove( dataElementName );
    }

    /**
     * returns DataElementInfo object by given child name or null if no such child found
     */
    private final HashMapWeakValues childInfoCache = new HashMapWeakValues();
    protected DataElementInfo getChildInfo(String name) throws Exception
    {
        DataElementInfo result = (DataElementInfo)childInfoCache.get(name);
        if(result == null)
        {
            DataElement entry = super.doGet(name);
            if(entry != null)
                result = entryTransformer.transformInput((Entry)entry);
            if(result != null)
                childInfoCache.put(name, result);
        }
        return result;
    }

    protected void initSize()
    {
        diskSize = -1;
        try
        {
            diskSize = Long.parseLong(getInfo().getProperty(DataCollectionConfigConstants.ELEMENT_SIZE_PROPERTY));
        }
        catch( Exception e )
        {
        }
        if( diskSize >= 0 )
            return;
        long size = 0;
        for( String name : getNameList() )
        {
            size += estimateSize(name, false);
        }
        System.gc();
        diskSize = size;
        updateSize(diskSize, false);
    }

    public long getDiskSize()
    {
        return diskSize;
    }

    protected void updateSize(long size, boolean propagate)
    {
        if(Boolean.parseBoolean(getInfo().getProperty(SKIP_UPDATE_SIZES))) return;
        diskSize = size;
        getInfo().getProperties().setProperty(DataCollectionConfigConstants.ELEMENT_SIZE_PROPERTY, String.valueOf(size));
        try
        {
            if(propagate)
                getCompletePath().save(getCompletePath().getDataElement());
            else
                storeConfigs();
        }
        catch( Exception e )
        {
        }
    }

    public long recalculateSize() throws Exception
    {
        Permission permissions = SecurityManager.getPermissions( getCompletePath() );
        //        if(!permissions.isAdminAllowed())
        if( !permissions.isWriteAllowed() )
            throw new RepositoryAccessDeniedException( getCompletePath(), SecurityManager.getSessionUser(),
                    DataCollectionUtils.permissionToString( permissions ) );
        synchronized( lock )
        {
            long size = 0;
            for( String name : getNameList() )
            {
                DataElementInfo dei = getChildInfo(name);
                if(dei == null) continue;
                Class<? extends DataElement> deClass = ClassLoading.loadSubClass( dei.getStrictProperty(DataElementInfo.ELEMENT_CLASS), dei.getProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY), DataElement.class );
                DataElementTypeDriver driver = lookForDriver(deClass);
                if(driver == null) continue;
                long childSize = driver.estimateSize(this, dei, true);
                if(childSize < 0) continue;
                synchronized( childSizes )
                {
                    childSizes.put(name, childSize);
                }
                size += childSize;
            }
            System.gc();
            diskSize = size;
            updateSize(diskSize, false);
        }
        return diskSize;
    }

    /**
     * Estimate disk size of given child
     * @param name child name
     * @param update whether to update cached size
     * @return size in bytes
     */
    protected long estimateSize(String name, boolean update)
    {
        try
        {
            if(!update)
            {
                synchronized(childSizes)
                {
                    long size = childSizes.get(name);
                    if(size >= 0) return size;
                }
            }
            DataElementInfo dei = getChildInfo(name);
            if(dei == null) return -1;
            Class<? extends DataElement> deClass = ClassLoading.loadSubClass( dei.getStrictProperty(DataElementInfo.ELEMENT_CLASS), dei.getProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY), DataElement.class );
            DataElementTypeDriver driver = lookForDriver(deClass);
            if(driver == null) return -1;
            long size = driver.estimateSize(this, dei, false);
            if(size >= 0)
            {
                synchronized( childSizes )
                {
                    childSizes.put(name, size);
                }
            }
            return size;
        }
        catch( Exception e )
        {
            return -1;
        }
    }

    protected void storeConfigs() throws Exception
    {
        File folder = new File(getRootDirectory());
        if( !folder.exists() )
        {
            folder.mkdirs();
        }
        File configFile = new File(folder, DataCollectionConfigConstants.DEFAULT_CONFIG_FILE);
        File primaryFile = new File(folder, getName()+DataCollectionConfigConstants.DEFAULT_FORMAT_CONFIG_SUFFIX);

        Properties primary = getPrimaryProperties(getName());
        ExProperties.store(primary, primaryFile);
        Properties base = new ExProperties();
        base.putAll(getInfo().getProperties());
        EntryStream.of(getBaseProperties(getName())).forKeyValue( base::put );
        base.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, getClass().getName());
        base.remove(DataCollectionConfigConstants.FILE_PATH_PROPERTY);
        base.remove(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY);
        base.remove(DataCollectionConfigConstants.DEFAULT_CONFIG_FILE);
        base.remove(LocalRepository.PARENT_COLLECTION);
        ExProperties.store(base, configFile);
    }

    protected DataElementInfo updateChildInfo(DataElement de, DataElementInfo oldInfo) throws Exception
    {
        de = DataCollectionUtils.fetchPrimaryElement(de, Permission.WRITE);
        DataElementTypeDriver driver = lookForDriver(de);
        DataElementInfo dei = oldInfo == null?new DataElementInfo(de.getName(), this):oldInfo;
        dei.setProperty(DataElementInfo.DRIVER_CLASS, driver.getClass().getName());
        dei.setProperty(DataElementInfo.ELEMENT_CLASS, de.getClass().getName());
        ExProperties.addPlugin(dei.getProperties(), de.getClass());
        if( de instanceof DataCollection )
        {
            DataCollectionInfo dcInfo = ((DataCollection)de).getInfo();
            if(! (de instanceof GenericDataCollection))
            {
                Properties properties = dcInfo.getProperties();
                for( Map.Entry<Object, Object> entry : properties.entrySet() )
                {
                    Object propertyName = entry.getKey();
                    if(propertyName.equals(DataCollectionConfigConstants.FILE_PATH_PROPERTY) || propertyName.equals(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY))
                        continue;
                    Object value = entry.getValue();
                    if(value != null)
                        dei.getProperties().put(propertyName, value);
                }
                if(!dcInfo.getDisplayName().equals(de.getName()))
                {
                    dei.setProperty(DataCollectionConfigConstants.DISPLAY_NAME_PROPERTY, dcInfo.getDisplayName());
                } else
                {
                    dei.getProperties().remove(DataCollectionConfigConstants.DISPLAY_NAME_PROPERTY);
                }
            }
        }
        return dei;
    }
    
    
    
    
    //    @Override
    //    public DataElement put(DataElement element) throws DataElementPutException
    //    {
    //        DataElement res = super.put( element );
    //        removeFromCache( element.getName() );
    //        return res;
    //    }
    //    
    @Override
    protected void doPut(DataElement element, boolean isNew) throws Exception
    {
        if( removingNames.contains( element.getName() ) )
            return;
        synchronized( lock )
        {
            try
            {
                DataCollectionUtils.checkQuota( getCompletePath() );
                long oldSize = ( isNew || diskSize < 0 ) ? 0 : estimateSize( element.getName(), false );
                DataElement unprotectedElement = DataCollectionUtils.fetchPrimaryElement( element, Permission.INFO );
                DataElementTypeDriver driver = lookForDriver( unprotectedElement );
                if( contains( unprotectedElement ) )
                {
                    // Remove element with the same name if it was created by different driver
                    DataElementInfo deiOld = getChildInfo( unprotectedElement.getName() );
                    String driverClassName = deiOld.getProperty( DataElementInfo.DRIVER_CLASS );
                    if( !driver.getClass().getName().equals( driverClassName ) )
                    {
                        DataElementTypeDriver oldDriver = lookForDriver( get( unprotectedElement.getName() ) );
                        if( oldDriver instanceof DataElementGenericCollectionTypeDriver )
                        {
                            throw new InvalidParameterException(
                                    "For safety replacing of folder with new item is disabled: delete folder first." );
                        }
                        remove( unprotectedElement.getName() );
                        isNew = true;
                    }
                }
                savingNames.add( element.getName() );
                DataElementInfo dei = updateChildInfo( unprotectedElement, getChildInfo( unprotectedElement.getName() ) );
                String time = String.valueOf( System.currentTimeMillis() );
                dei.setProperty( "modifiedDate", time );
                if( isNew )
                    dei.setProperty( "createdDate", time );
                driver.doPut( this, unprotectedElement, dei );
                super.doPut( entryTransformer.transformOutput( dei ), isNew );
                long newSize = estimateSize( element.getName(), true );
                if( diskSize >= 0 && newSize != oldSize )
                {
                    updateSize( diskSize - oldSize + newSize, true );
                }
                nameList = null;
            }
            finally
            {
                savingNames.remove( element.getName() );
            }
        }
    }

    @Override
    protected void doRemove(String name) throws Exception
    {
        if( savingNames.contains( name ) )
            throw new DataElementCreatingException( getCompletePath().getChildPath( name ) );
        synchronized( lock )
        {
            long oldSize = diskSize < 0 ? 0 : estimateSize(name, false);
            removingNames.add(name);
            try
            {
                DataElementInfo dei = getChildInfo(name);
                DataElementTypeRegistry.doRemove(this, dei);
                getPrimaryCollection().setNotificationEnabled(false);
                getPrimaryCollection().setPropagationEnabled(false);
                super.doRemove(name);
                childInfoCache.remove(name);
                nameList = null;
                if(diskSize >= 0)
                {
                    long newTotalSize = Math.max(0, diskSize - oldSize);
                    if(isEmpty()) newTotalSize = 0;
                    if(newTotalSize != diskSize)
                        updateSize(newTotalSize, true);
                }
                synchronized( childSizes )
                {
                    childSizes.remove(name);
                }
            }
            catch(Exception e)
            {
                long newSize = estimateSize(name, true);
                if(diskSize >= 0 && newSize != oldSize)
                {
                    updateSize(diskSize-oldSize+newSize, true);
                }
                throw e;
            }
            finally
            {
                removingNames.remove(name);
            }
        }
    }

    public String getChildProperty(String childName, String propertyName) throws Exception
    {
        DataElementInfo dei = getChildInfo( childName );
        return dei.getProperty( propertyName );
    }

    public void setChildProperty(String childName, String propertyName, String propertyValue) throws Exception
    {
        DataElementInfo dei = getChildInfo( childName );
        dei.setProperty( propertyName, propertyValue );
        super.doPut( entryTransformer.transformOutput( dei ), false );
    }

    @Override
    public void close() throws Exception
    {
        for( DataCollection dc : collectionCache.values() )
        {
            if( dc != this )
                dc.close();
        }
        super.close();
    }

    public static Properties getPrimaryProperties(String name)
    {
        Properties result = new ExProperties();
        result.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, name);
        result.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, FileEntryCollection2.class.getName());
        result.setProperty(DataCollectionConfigConstants.FILE_PROPERTY, "default.dat");
        result.setProperty(EntryCollection.ENTRY_START_PROPERTY, "ID");
        result.setProperty(EntryCollection.ENTRY_ID_PROPERTY, "ID");
        result.setProperty(EntryCollection.ENTRY_END_PROPERTY, "//");
        result.setProperty(EntryCollection.ENTRY_DELIMITERS_PROPERTY, "\"; \t\"");
        result.setProperty(EntryCollection.ENTRY_KEY_FULL, "true");
        return result;
    }

    public static Properties getBaseProperties(String name)
    {
        Properties result = new Properties();
        result.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, name);
        result.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, GenericDataCollection.class.getName());
        //result.setProperty(DataCollectionConfigConstants.FILE_PATH_PROPERTY, getRootDirectory()+name);
        result.setProperty(DataCollectionConfigConstants.NEXT_CONFIG, name + DataCollectionConfigConstants.DEFAULT_FORMAT_CONFIG_SUFFIX);
        return result;
    }

    /** Connection to DBMS. */
    private final ThreadLocal<Connection> conn = new ThreadLocal<>();
    /**
     *  Return active connection.
     *  Connect if necessary.
     *  @return Active connection.
     *  @throws java.sql.SQLException If operation on DBMS failed.
     */
    @Override
    public Connection getConnection() throws BiosoftSQLException
    {
        if( conn.get() == null )
        {
            conn.set(SqlConnectionPool.getConnection(this));
        }
        return conn.get();
    }

    public String getDatabaseURL()
    {
        return getInfo().getProperties().getProperty(SqlDataCollection.JDBC_URL_PROPERTY);
    }

    public void setDatabaseURL(String url)
    {
        if(url == null)
            getInfo().getProperties().remove(SqlDataCollection.JDBC_URL_PROPERTY);
        else
            getInfo().getProperties().setProperty(SqlDataCollection.JDBC_URL_PROPERTY, url);
        try
        {
            getCompletePath().save(this);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
        conn.set(null);
    }

    public String getDatabaseUser()
    {
        return getInfo().getProperties().getProperty(SqlDataCollection.JDBC_USER_PROPERTY);
    }

    public void setDatabaseUser(String user)
    {
        if(user == null)
            getInfo().getProperties().remove(SqlDataCollection.JDBC_USER_PROPERTY);
        else
            getInfo().getProperties().setProperty(SqlDataCollection.JDBC_USER_PROPERTY, user);
        try
        {
            getCompletePath().save(this);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
        conn.set(null);
    }

    public String getDatabasePassword()
    {
        return getInfo().getProperties().getProperty(SqlDataCollection.JDBC_PASSWORD_PROPERTY);
    }

    public void setDatabasePassword(String password)
    {
        if(password == null)
            getInfo().getProperties().remove(SqlDataCollection.JDBC_PASSWORD_PROPERTY);
        else
            getInfo().getProperties().setProperty(SqlDataCollection.JDBC_PASSWORD_PROPERTY, password);
        try
        {
            getCompletePath().save(this);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
        conn.set(null);
    }

    public void setPreferedTableImplementation(TableImplementationRecord tableImplementation)
    {
        getInfo().getProperties().setProperty(PREFERED_TABLE_IMPLEMENTATION_PROPERTY, tableImplementation.toString());
        try
        {
            getCompletePath().save(this);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    public TableImplementationRecord getPreferedTableImplementation()
    {
        return TableImplementationRecord.getPreferedTableImplementation(getInfo().getProperties().getProperty(PREFERED_TABLE_IMPLEMENTATION_PROPERTY));
    }

    /**
     * Returns description of child data element without its instantiating.
     * 
     * {@link DataElementDescriptor} properties contains all properties from {@link DataElementInfo}.
     * 
     * If data element is transformed from file then path to original file and transformer are also stored in properties. 
     */
    @Override
    public DataElementDescriptor getDescriptor(final String name)
    {
        DataElementInfo childInfo = null;
        try
        {
            childInfo = getChildInfo(name);
        }
        catch( Exception e1 ) {}
        if(childInfo == null) 
        	return null;
        
        Properties childProperties = childInfo.getProperties();
        if(childProperties == null)
        	return null;
        
        Class<? extends DataElement> deClass;
        try
        {
            deClass = ClassLoading.loadSubClass( childProperties.getProperty(DataElementInfo.ELEMENT_CLASS), childProperties.getProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY), DataElement.class );
        }
        catch( Exception e )
        {
            new DataElementReadException(getCompletePath().getChildPath(name), DataElementInfo.ELEMENT_CLASS).log();
            return null;
        }
        
        boolean leaf = false;
        DataElementTypeDriver driver = null;
        if( !GenericDataCollection.class.isAssignableFrom(deClass) )
        {
            leaf = true;
            String driverName = childProperties.getProperty(DataElementInfo.DRIVER_CLASS);
            driver = DataElementTypeRegistry.getDriver(driverName);
            if(driver == null)
            {
                try
                {
                    driver = lookForDriver(deClass);
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE, "Cannot get driver for "+getCompletePath().getChildPath(name), e);
                }
            }
            if( driver != null )
            {
                leaf = driver.isLeafElement(this, childInfo);
            }
        }
        
        Map<String, String> properties = new HashMap<>();
        EntryStream.of(childProperties).selectKeys(String.class).selectValues(String.class).forKeyValue(properties::put);

        if( driver instanceof DataElementFileTypeDriver )
        {
        	properties.put(DataCollectionConfigConstants.FILE_PATH_PROPERTY, getRootDirectory() + DataElementFileTypeDriver.FOLDER_NAME + File.separatorChar + name);
        }
        
        return new DataElementDescriptor(deClass, leaf, properties)
        {
            @Override
            public String getValue(String key)
            {
                if(key.equals(DataCollectionConfigConstants.ELEMENT_SIZE_PROPERTY))
                {
                    long size = estimateSize(name, false);
                    return size >= 0 ? String.valueOf(size) : null;
                }
                return super.getValue(key);
            }
        };
    }

    
    @Override
    public DataCollection createSubCollection(String name, Class<? extends FolderCollection> clazz)
    {
        synchronized( lock )
        {
            if( name == null || name.isEmpty() )
                throw new MissingParameterException( "Name" );
            if( contains( name ) )
                throw new DataElementExistsException( getCompletePath().getChildPath( name ) );
            if( !name.trim().equals( name ) )
                throw new ParameterNotAcceptableException(
                        new IllegalArgumentException( "Name should not start or end with white-space characters." ), "Name", name );
            if( !GenericDataCollection.class.isAssignableFrom( clazz ) )
                clazz = GenericDataCollection.class;
            DataCollection repository = this;
            while( repository != null && ! ( repository instanceof Repository ) )
            {
                repository = repository.getOrigin();
            }
            if( repository == null )
                throw new IllegalArgumentException( "Cannot find repository: failed to create sub-collection in " + getCompletePath() );
            try
            {
                DataCollection folder = createGenericCollection( this, (Repository)repository, name, null,
                        (Class<? extends GenericDataCollection>)clazz );
                String historyCollection = getInfo().getProperty( HistoryFacade.HISTORY_COLLECTION );
                if( historyCollection != null )
                    folder.getInfo().getProperties().setProperty( HistoryFacade.HISTORY_COLLECTION, historyCollection );
                put( folder );
                return folder;
            }
            catch( Exception ex )
            {
                throw ExceptionRegistry.translateException( ex );
            }
        }
    }

    static public DataCollection createGenericCollection(DataCollection parent, Repository repository, String name, String subDir)
            throws Exception
    {
        return createGenericCollection(parent, repository, name, subDir, GenericDataCollection.class);
    }

    private static DataCollection createGenericCollection(DataCollection parent, Repository repository, String name, String subDir,
            Class<? extends GenericDataCollection> type) throws Exception
    {
        DataCollection primaryParent = DataCollectionUtils.fetchPrimaryCollection(parent, Permission.WRITE);
        Repository primaryRepository = ( repository instanceof NetworkRepository ) ? (Repository) ( (NetworkRepository)repository )
                .getPrimaryCollection() : repository;
        if( subDir == null && primaryParent instanceof GenericDataCollection && primaryRepository instanceof LocalRepository )
        {
            File dir1 = new File( ( (GenericDataCollection)primaryParent ).getRootDirectory());
            File dir2 = ( (LocalRepository)primaryRepository ).getRootDirectory();
            subDir = ApplicationUtils.getRelativeFilePath(dir2, dir1);
            if( subDir == null )
                subDir = ApplicationUtils.getRelativeFilePath(dir2.getAbsoluteFile(), dir1.getAbsoluteFile());
            if( subDir == null )
                subDir = ApplicationUtils.getRelativeFilePath(dir2.getCanonicalFile(), dir1.getCanonicalFile());
            if( subDir == null )
            {
                throw new InternalException("Unable to create collection " + name + ": cannot find subPath for "
                        + ( (GenericDataCollection)primaryParent ).getRootDirectory());

            }
            subDir = subDir.isEmpty() ? name : (subDir + File.separator + name);
        }
        Properties primary = new Properties();
        primary.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, name);
        primary.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, FileEntryCollection2.class.getName());
        primary.setProperty(DataCollectionConfigConstants.FILE_PROPERTY, "default.dat");
        primary.setProperty(EntryCollection.ENTRY_START_PROPERTY, "ID");
        primary.setProperty(EntryCollection.ENTRY_ID_PROPERTY, "ID");
        primary.setProperty(EntryCollection.ENTRY_END_PROPERTY, "//");
        primary.setProperty(EntryCollection.ENTRY_DELIMITERS_PROPERTY, "\"; \"");
        primary.setProperty(EntryCollection.ENTRY_KEY_FULL, "true");

        Properties derived = new Properties();
        derived.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, name);
        derived.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, type.getName());
        derived.setProperty(DataCollectionConfigConstants.ELEMENT_SIZE_PROPERTY, "0");

        String plugins = ClassLoading.getPluginForClass( type );
        if( plugins != null )
        {
            derived.setProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY, plugins);
        }
        derived.setProperty(DataCollectionConfigConstants.DEFAULT_CONFIG_FILE, DataCollectionConfigConstants.DEFAULT_CONFIG_FILE);
        derived.setProperty(LocalRepository.PARENT_COLLECTION, parent.getCompletePath().toString());
        derived.setProperty(LocalRepository.PUT_TO_REPOSITORY, "false");

        if( primaryParent instanceof GenericDataCollection )
        {
            derived.setProperty(PREFERED_TABLE_IMPLEMENTATION_PROPERTY, ( (GenericDataCollection)primaryParent )
                    .getPreferedTableImplementation().toString());
        }

        return CollectionFactoryUtils.createDerivedCollection(repository, name, primary, derived, subDir);
    }

    @Override
    public boolean isAcceptable(Class<? extends DataElement> clazz)
    {
        if(TableDataCollection.class.isAssignableFrom( clazz ))
            return clazz == TableDataCollection.class || clazz == getPreferedTableImplementation().getTableClass();
        return getTypeSpecificCollection(clazz) != null;
    }
}
