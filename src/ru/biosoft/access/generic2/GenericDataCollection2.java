package ru.biosoft.access.generic2;

import java.io.File;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import one.util.streamex.EntryStream;
import ru.biosoft.access.CreateDataCollectionController;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.DefaultQuerySystem;
import ru.biosoft.access.file.FileBasedCollection;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.ReadOnlyVectorCollection;
import ru.biosoft.access.Repository;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataCollectionInfo;
import ru.biosoft.access.core.DataCollectionVetoException;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementCreateException;
import ru.biosoft.access.core.DataElementDescriptor;
import ru.biosoft.access.core.DataElementGetException;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPutException;
import ru.biosoft.access.core.DataElementReadException;
import ru.biosoft.access.core.InvalidElement;
import ru.biosoft.access.core.QuerySystem;
import ru.biosoft.access.core.RepositoryAccessDeniedException;
import ru.biosoft.access.exception.BiosoftFileCreateException;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.exception.DataElementExistsException;
import ru.biosoft.access.exception.DataElementNotAcceptableException;
import ru.biosoft.access.generic.GenericTitleIndex;
import ru.biosoft.access.generic.TableImplementationRecord;
import ru.biosoft.access.security.CodePrivilege;
import ru.biosoft.access.security.CodePrivilegeType;
import ru.biosoft.access.security.Permission;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.access.sql.SqlConnectionHolder;
import ru.biosoft.access.sql.SqlConnectionPool;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.InternalException;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.ExProperties;
import ru.biosoft.util.TextUtil2;

/**
 * New implementation of GenericDataCollection (draft)
 * TODO: support quotas
 * TODO: support repository elements
 * TODO: (low pri) deal with config file opened on WinXP
 * TODO: (low pri) atomically write FileDataElements
 * TODO: (low pri) escape special symbols
 * @author lan
 */
@CodePrivilege(CodePrivilegeType.REPOSITORY)
public class GenericDataCollection2 extends AbstractDataCollection<DataElement> implements FileBasedCollection<DataElement>,
        SqlConnectionHolder, FolderCollection
{
    public static final String PREFERED_TABLE_IMPLEMENTATION_PROPERTY = "PreferedTableImplementation";
    protected static final String DRIVER_PROPERTY = "driver";

    private static enum Operation
    {
        GET, PUT, REMOVE, CREATE
    }
    // Map of active element operations
    private final ConcurrentMap<String, Operation> activeOperations = new ConcurrentHashMap<>();

    private class ElementLock implements AutoCloseable
    {
        private volatile String name;

        public ElementLock(String name, Operation operation)
        {
            this.name = name;
            while(true)
            {
                Operation op = activeOperations.putIfAbsent( name, Operation.PUT );
                if(op != Operation.GET)
                {
                    if( op != null && ( operation != Operation.GET || op == Operation.REMOVE ) )
                    {
                        // Somebody is putting or removing element with the same name in another thread
                        // Just pretend that another thread operation is finished later and overrides our operation
                        this.name = null;
                        return;
                    }
                    // This element name is free: lock acquired
                    break;
                }
                if(closed)
                    return;
                // Somebody is fetching this element: wait
                try
                {
                    Thread.sleep( ThreadLocalRandom.current().nextLong( 50 ) );
                }
                catch( InterruptedException e )
                {
                }
            }
        }

        public boolean isCancelled()
        {
            return name == null;
        }

        @Override
        public void close() throws Exception
        {
            if(name != null)
                activeOperations.remove(name);
        }
    }

    private volatile boolean closed = false;
    private File root;
    private List<String> nameList;
    private final Map<String, Reference<Properties>> childInfos = new ConcurrentHashMap<>();
    private static Map<String, GenericElementTypeDriver> drivers = new HashMap<String, GenericElementTypeDriver>()
    {{
        put("unknown", new UnknownElementDriver());
        put(GenericFileTypeDriver.class.getSimpleName(), new GenericFileTypeDriver());
        put(GenericEntryTypeDriver.class.getSimpleName(), new GenericEntryTypeDriver());
        put(GenericSQLTypeDriver.class.getSimpleName(), new GenericSQLTypeDriver());
        put(GenericCollectionTypeDriver.class.getSimpleName(), new GenericCollectionTypeDriver());
        put(GenericRepositoryTypeDriver.class.getSimpleName(), new GenericRepositoryTypeDriver());
    }};
    private final Repository repository = new RepositoryWrapper();

    public GenericDataCollection2(DataCollection<?> parent, Properties properties)
    {
        super( parent, properties );
        getInfo().getProperties().setProperty(QuerySystem.INDEX_LIST, "title");
        getInfo().getProperties().setProperty("index.title", GenericTitleIndex.class.getName());
        DefaultQuerySystem qs = new DefaultQuerySystem(this);
        getInfo().setQuerySystem(qs);
        String dir = getInfo().getProperties().getProperty(DataCollectionConfigConstants.FILE_PATH_PROPERTY, getInfo().getProperty(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY));
        root = dir == null ? null : new File(dir);
        if( root == null && parent instanceof GenericDataCollection2 )
        {
            root = new File( ( (GenericDataCollection2)parent ).getRootDirectory(), getName() );
        }
        if( root == null )
            throw new DataElementReadException(this, DataCollectionConfigConstants.CONFIG_PATH_PROPERTY);
    }

    protected File getRootDirectory()
    {
        return root;
    }

    @Override
    public @Nonnull List<String> getNameList()
    {
        if(nameList == null)
        {
            List<String> result = new ArrayList<>();
            for(File file : root.listFiles())
            {
                if(file.isDirectory() && new File(file, DataCollectionConfigConstants.DEFAULT_CONFIG_FILE).exists())
                    result.add( file.getName() );
            }
            Collections.sort(result, (name1, name2) -> {
                try
                {
                    int priority1 = 0, priority2 = 0;
                    Properties dei1 = getChildInfo(name1);
                    Properties dei2 = getChildInfo(name2);
                    if( dei1.getProperty( DataCollectionConfigConstants.CLASS_PROPERTY ).equals( GenericDataCollection2.class.getName() ) )
                        priority1 = 1;
                    if( dei2.getProperty( DataCollectionConfigConstants.CLASS_PROPERTY ).equals( GenericDataCollection2.class.getName() ) )
                        priority2 = 1;
                    String displayName1 = dei1.getProperty( DataCollectionConfigConstants.DISPLAY_NAME_PROPERTY );
                    if(displayName1 == null) displayName1 = name1;
                    String displayName2 = dei2.getProperty( DataCollectionConfigConstants.DISPLAY_NAME_PROPERTY );
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

    private Properties getChildInfo(String name)
    {
        Reference<Properties> ref = childInfos.get( name );
        Properties properties = ref == null ? null : ref.get();
        if(properties != null)
            return properties;
        try
        {
            File folder = new File(root, name);
            if(!folder.exists())
                return null;
            File config = new File(folder, DataCollectionConfigConstants.DEFAULT_CONFIG_FILE);
            if(!config.exists())
                return null;
            properties = new ExProperties( config );
            properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, name );
        }
        catch( IOException e )
        {
            throw ExceptionRegistry.translateException( e );
        }
        childInfos.put( name, new SoftReference<>( properties ) );
        return properties;
    }

    private GenericElementTypeDriver getDriver(Properties properties)
    {
        GenericElementTypeDriver driver = drivers.get( properties.getProperty( DRIVER_PROPERTY, "" ) );
        if(driver == null)
            return drivers.get( "unknown" );
        return driver;
    }

    private GenericElementTypeDriver getDriver(DataElement de)
    {
        for(GenericElementTypeDriver driver : drivers.values())
        {
            if(driver.isSupported( de.getClass() ))
                return driver;
        }
        throw new DataElementNotAcceptableException( DataElementPath.create(de), "no driver" );
    }

    private void updateChildInfo(Properties properties, DataElement de, boolean isNew, GenericElementTypeDriver driver)
    {
        if( de instanceof DataCollection )
        {
            DataCollectionInfo dcInfo = ((DataCollection<?>)de).getInfo();
            Properties dcProperties = dcInfo.getProperties();
            for( Map.Entry<Object, Object> entry : dcProperties.entrySet() )
            {
                Object propertyName = entry.getKey();
                if( propertyName.equals( DataCollectionConfigConstants.FILE_PATH_PROPERTY ) || propertyName.equals( DataCollectionConfigConstants.CONFIG_PATH_PROPERTY ) )
                    continue;
                Object value = entry.getValue();
                if(value != null)
                    properties.put(propertyName, value);
            }
            if(!dcInfo.getDisplayName().equals(de.getName()))
            {
                properties.setProperty( DataCollectionConfigConstants.DISPLAY_NAME_PROPERTY, dcInfo.getDisplayName() );
            } else
            {
                properties.remove( DataCollectionConfigConstants.DISPLAY_NAME_PROPERTY );
            }
        }
        properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, de.getName() );
        properties.setProperty( DataCollectionConfigConstants.CLASS_PROPERTY, de.getClass().getName() );
        properties.setProperty( DRIVER_PROPERTY, driver.getClass().getSimpleName() );
        String time = String.valueOf(System.currentTimeMillis());
        properties.setProperty("modifiedDate", time);
        if(isNew)
            properties.setProperty("createdDate", time);
    }

    @Override
    public boolean contains(String name)
    {
        File folder = new File(root, name);
        return folder.exists() && new File(folder, DataCollectionConfigConstants.DEFAULT_CONFIG_FILE).exists();
    }

    @Override
    public DataElement get(String name) throws Exception
    {
        if( closed || !isValid() )
            return null;
        DataElement de = getFromCache(name);
        if( de != null )
            return de;
        try(ElementLock lock = new ElementLock(name, Operation.GET))
        {
            if(closed || lock.isCancelled())
                return null;
            de = getFromCache( name );
            if(de != null) // Another thread put this element into the cache
                return de;
            Properties properties = getChildInfo( name );
            if(properties == null)
                return null;
            de = getDriver( properties ).doGet( this, new File(root, name), properties );
            if( de != null )
            {
                // basic validation
                if(!Objects.equals( de.getName(), name ))
                    throw new DataElementGetException(new InternalException("Name of created object is invalid: "+de.getName()), getCompletePath().getChildPath(name));
                cachePut(de);
            }
        }
        catch(DataElementGetException e)
        {
            de = new InvalidElement(name, this, e);
        }
        catch(Throwable e)
        {
            de = new InvalidElement(name, this, new DataElementGetException(e, getCompletePath().getChildPath(name)));
        }
        return de;
    }

    @Override
    public DataElement put(DataElement de) throws DataElementPutException
    {
        if( de == null || closed || !isValid() || !checkMutable() )
            return null;
        String name = de.getName();
        validateName(name);
        DataElement oldElement = null;
        File folder = new File(root, name);
        try(ElementLock lock = new ElementLock(name, Operation.PUT))
        {
            if(closed || lock.isCancelled())
                return null;
            Properties oldProperties = getChildInfo( name );
            boolean isNew = oldProperties == null;
            doAddPreNotify(name, isNew);
            de = DataCollectionUtils.fetchPrimaryElement(de, Permission.INFO);
            GenericElementTypeDriver driver = getDriver( de );
            Properties properties = null;
            if(!isNew)
            {
                GenericElementTypeDriver oldDriver = getDriver( oldProperties );
                try
                {
                    oldElement = oldDriver.doGet( this, folder, oldProperties );
                }
                catch( Throwable t )
                {
                    // Cannot retrieve old element: ok, let's log it and try to continue anyways
                    new DataElementGetException(t, getCompletePath().getChildPath(name)).log();
                }
                if(driver != oldDriver)
                {
                    if(oldDriver instanceof GenericCollectionTypeDriver)
                    {
                        throw new IllegalArgumentException("For safety replacing of folder with new item is disabled: delete folder first.");
                    }
                    try
                    {
                        oldDriver.doRemove( this, folder, oldProperties );
                    }
                    catch( LoggedException e )
                    {
                        log.log(Level.SEVERE,  "While removing " + getCompletePath().getChildPath( name ) + ": " + e.log() );
                    }
                    ApplicationUtils.removeDir( folder );
                    nameList = null;
                    isNew = true;
                } else
                    properties = (Properties)oldProperties.clone();
            }
            if(isNew)
            {
                folder.mkdirs();
                if(!folder.isDirectory())
                    throw new BiosoftFileCreateException( folder );
                properties = new ExProperties();
            }
            updateChildInfo( properties, de, isNew, driver );
            Properties storeProperties = (Properties)properties.clone();
            storeProperties.remove( DataCollectionConfigConstants.CONFIG_PATH_PROPERTY );
            storeProperties.remove( DataCollectionConfigConstants.CONFIG_FILE_PROPERTY );
            storeProperties.remove( DataCollectionConfigConstants.FILE_PATH_PROPERTY );
            try
            {
                File configFile = new File(folder, DataCollectionConfigConstants.DEFAULT_CONFIG_FILE + ".tmp");
                ExProperties.store( storeProperties, configFile );
                File realConfigFile = new File(folder, DataCollectionConfigConstants.DEFAULT_CONFIG_FILE);
                ApplicationUtils.linkOrCopyFile(realConfigFile, configFile, null);
                configFile.delete();
                driver.doPut( this, folder, de, properties );
                childInfos.put( de.getName(), new SoftReference<>(properties) );
                if(isNew)
                    nameList = null;
                cachePut(de);
                doAddPostNotify(name, oldElement == null, oldElement);
            }
            catch( Throwable t )
            {
                if(isNew)
                {
                    ApplicationUtils.removeDir( folder );
                }
                throw ExceptionRegistry.translateException( t );
            }
        }
        catch( DataElementPutException e )
        {
            if(e.getProperty("path").equals(getCompletePath().getChildPath(name)))
                throw e;
            throw new DataElementPutException(e, getCompletePath().getChildPath(name));
        }
        catch( DataCollectionVetoException ex )
        {
            log.info("Veto exception for <" + name + ">, is caught.");
        }
        catch( Throwable t )
        {
            throw new DataElementPutException(t, getCompletePath().getChildPath(name));
        }
        return oldElement;
    }

    @Override
    public void remove(String name) throws Exception
    {
        if(name == null || closed || !isValid() || !checkMutable())
            return;
        File folder = new File(root, name);
        try(ElementLock lock = new ElementLock(name, Operation.REMOVE))
        {
            if(closed || lock.isCancelled())
                return;
            Properties properties = getChildInfo( name );
            if(properties == null)
                return;
            doRemovePreNotify(name);
            GenericElementTypeDriver driver = getDriver( properties );
            DataElement oldElement = null;
            try
            {
                oldElement = driver.doGet(this, folder, properties);
            }
            catch( Throwable t )
            {
                // Cannot retrieve old element: ok, let's log it and try to continue anyways
                new DataElementGetException(t, getCompletePath().getChildPath(name)).log();
            }
            try
            {
                driver.doRemove( this, folder, properties );
            }
            catch( LoggedException e )
            {
                log.log(Level.SEVERE,  "While removing " + getCompletePath().getChildPath( name ) + ": " + e.log() );
            }
            ApplicationUtils.removeDir( folder );
            childInfos.remove( name );
            if( v_cache != null )
                v_cache.remove(name);
            nameList = null;
            doRemovePostNotify(name, oldElement);
        }
        catch( DataCollectionVetoException ex )
        {
            log.info("Veto exception <" + name + ">, is caught.");
        }
    }

    /**
     * Remove everything from the collection
     * Called only before removal of collection itself
     */
    protected void clear()
    {
        closed = true;
        // Try to wait till current operations finish
        int tries = 10;
        while(!activeOperations.isEmpty() && tries > 0)
        {
            try
            {
                Thread.sleep( 50 );
                tries--;
            }
            catch( InterruptedException e )
            {
            }
        }
        for(File file : root.listFiles())
        {
            if(file.isDirectory() && new File(file, DataCollectionConfigConstants.DEFAULT_CONFIG_FILE).exists())
            {
                try
                {
                    Properties properties = getChildInfo( file.getName() );
                    getDriver( properties ).doRemove( this, file, properties );
                }
                catch( LoggedException e )
                {
                    log.log(Level.SEVERE,  "While removing " + getCompletePath().getChildPath( file.getName() ) + ": " + e.log() );
                }
                // Unnecessary to remove the directory as it will be removed recursively by caller
            }
        }
        childInfos.clear();
        v_cache.clear();
    }

    @Override
    public DataElementDescriptor getDescriptor(final String name)
    {
        if(closed)
            return null;
        final Properties properties = getChildInfo( name );
        if(properties == null)
            return null;
        final GenericElementTypeDriver driver = getDriver( properties );
        Class<? extends DataElement> clazz = driver.getElementClass( properties );
        Map<String, String> map = EntryStream.of(properties).selectKeys(String.class).selectValues(String.class).toMap();
        return new DataElementDescriptor( clazz, driver.isLeaf( this, properties ), map )
        {
            @Override
            public String getValue(String key)
            {
                if(key.equals(DataCollectionConfigConstants.ELEMENT_SIZE_PROPERTY))
                {
                    long size = driver.estimateSize(GenericDataCollection2.this, new File(root, name), properties, false);
                    return size >= 0 ? String.valueOf(size) : null;
                }
                return super.getValue(key);
            }
        };
    }

    @Override
    public File getChildFile(String name)
    {
        File folder = new File(root, name);
        folder.mkdirs();
        return new File(folder, GenericElementTypeDriver.DATA_FILE_NAME);
    }

    @Override
    public boolean isAcceptable(Class<? extends DataElement> clazz)
    {
        if(TableDataCollection.class.isAssignableFrom( clazz ))
            return clazz == TableDataCollection.class || clazz == getPreferedTableImplementation().getTableClass();
        for(GenericElementTypeDriver driver : drivers.values())
        {
            if(driver.isSupported( clazz ))
                return true;
        }
        return false;
    }

    @Override
    public void release(String name)
    {
        super.release( name );
        childInfos.remove( name );
    }

    @Override
    public boolean isFileAccepted(File file)
    {
        return true;
    }

    @Override
    public void close() throws Exception
    {
        closed = true;
        super.close();
    }

    @Override
    public void reinitialize() throws LoggedException
    {
        nameList = null;
        childInfos.clear();
        v_cache.clear();
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

    @PropertyName("Database URL")
    @PropertyDescription("JDBC URL pointing to the database like jdbc:mysql://localhost:3306/my_database")
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
            log.log(Level.SEVERE, e.getMessage(),  e);
        }
        conn.set(null);
    }

    @PropertyName("Database user")
    @PropertyDescription("Name of database user to use when connecting")
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
            log.log(Level.SEVERE, e.getMessage(),  e);
        }
        conn.set(null);
    }

    @PropertyName("Database password")
    @PropertyDescription("Password to use when connecting to the database")
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
            log.log(Level.SEVERE, e.getMessage(),  e);
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
            log.log(Level.SEVERE, e.getMessage(),  e);
        }
    }

    @PropertyName("Preferred table implementation")
    @PropertyDescription("Select how tables should be represented (as files or as SQL tables)")
    public TableImplementationRecord getPreferedTableImplementation()
    {
        return TableImplementationRecord.getPreferedTableImplementation(getInfo().getProperties().getProperty(PREFERED_TABLE_IMPLEMENTATION_PROPERTY));
    }

    @Override
    public DataCollection createSubCollection(String name, Class<? extends FolderCollection> clazz)
    {
        if(!GenericDataCollection2.class.isAssignableFrom( clazz ))
            clazz = GenericDataCollection2.class;
        Properties properties = new ExProperties();
        properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, name );
        properties.setProperty( DataCollectionConfigConstants.CLASS_PROPERTY, clazz.getName() );
        properties.setProperty( PREFERED_TABLE_IMPLEMENTATION_PROPERTY, getPreferedTableImplementation().toString() );
        try
        {
            FolderCollection collection = clazz.getConstructor( ru.biosoft.access.core.DataCollection.class, Properties.class ).newInstance( this, properties );
            put(collection);
            return getCompletePath().getChildPath( name ).getDataCollection();
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException( e );
        }
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

    public Repository getRepositoryWrapper()
    {
        return repository;
    }

    private class RepositoryWrapper extends ReadOnlyVectorCollection<DataCollection<?>> implements Repository
    {
        public RepositoryWrapper()
        {
            super("", null, (Properties)null);
        }

        @Override
        public DataCollection createDataCollection(String name, Properties properties, String subDir, File[] files, CreateDataCollectionController controller) throws Exception
        {
            DataElementPath path = GenericDataCollection2.this.getCompletePath().getChildPath(name);
            if( closed || !isValid() || !checkMutable() )
                throw new RepositoryAccessDeniedException( path, SecurityManager.getSessionUser(), "create" );
            validateName(name);
            File folder = new File(root, name);
            try(ElementLock lock = new ElementLock(name, Operation.CREATE))
            {
                // Somebody is putting or removing element with the same name in another thread
                // Just fail
                if(lock.isCancelled())
                    throw new DataElementExistsException(getCompletePath().getChildPath(name));
                if(closed)
                    return null;
                Properties oldProperties = getChildInfo( name );
                if(oldProperties != null)
                {
                    GenericElementTypeDriver oldDriver = getDriver( oldProperties );
                    if(oldDriver instanceof GenericCollectionTypeDriver)
                    {
                        throw new IllegalArgumentException("For safety replacing of folder with new item is disabled: delete folder first.");
                    }
                    try
                    {
                        oldDriver.doRemove( GenericDataCollection2.this, folder, oldProperties );
                    }
                    catch( LoggedException e )
                    {
                        log.log(Level.SEVERE,  "While removing " + path + ": " + e.log() );
                    }
                    ApplicationUtils.removeDir( folder );
                    nameList = null;
                }
                GenericDataCollection2.this.doAddPreNotify(name, true);
                folder.mkdirs();
                if(!folder.isDirectory())
                    throw new BiosoftFileCreateException( folder );
                properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, name );
                properties.setProperty( DRIVER_PROPERTY, GenericRepositoryTypeDriver.class.getSimpleName() );
                String time = String.valueOf(System.currentTimeMillis());
                properties.setProperty("modifiedDate", time);
                properties.setProperty("createdDate", time);
                Properties storeProperties = (Properties)properties.clone();
                storeProperties.remove( DataCollectionConfigConstants.CONFIG_PATH_PROPERTY );
                storeProperties.remove( DataCollectionConfigConstants.CONFIG_FILE_PROPERTY );
                storeProperties.remove( DataCollectionConfigConstants.FILE_PATH_PROPERTY );
                // TODO: copy files
                try
                {
                    File configFile = new File(folder, DataCollectionConfigConstants.DEFAULT_CONFIG_FILE + ".tmp");
                    ExProperties.store( storeProperties, configFile );
                    File realConfigFile = new File(folder, DataCollectionConfigConstants.DEFAULT_CONFIG_FILE);
                    ApplicationUtils.linkOrCopyFile(realConfigFile, configFile, null);
                    configFile.delete();
                    childInfos.put( name, new SoftReference<>(properties) );
                    nameList = null;
                }
                catch( Throwable t )
                {
                    ApplicationUtils.removeDir( folder );
                    throw ExceptionRegistry.translateException( t );
                }
                GenericDataCollection2.this.doAddPostNotify(name, true, null);
            }
            catch( DataElementCreateException e )
            {
                if(e.getProperty("path").equals(path))
                    throw e;
                throw new DataElementCreateException(e, path, DataCollection.class);
            }
            catch( DataCollectionVetoException ex )
            {
                log.info("Veto exception for <" + name + ">, is caught.");
            }
            catch( Throwable t )
            {
                throw new DataElementCreateException(t, path, DataCollection.class);
            }
            return GenericDataCollection2.this.get(name).cast( ru.biosoft.access.core.DataCollection.class );
        }

        @Override
        public void updateRepository()
        {
        }

        @Override
        protected void doInit()
        {
        }
    }
}
