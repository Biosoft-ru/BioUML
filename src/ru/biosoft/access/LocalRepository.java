package ru.biosoft.access;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataCollectionInfo;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementCreateException;
import ru.biosoft.access.core.DataElementDescriptor;
import ru.biosoft.access.core.DataElementGetException;
import ru.biosoft.access.core.DataElementPutException;
import ru.biosoft.access.core.DataElementReadException;
import ru.biosoft.access.security.NetworkDataCollection;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.InternalException;
import ru.biosoft.exception.LoggedClassNotFoundException;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.util.ApplicationUtils;
import ru.biosoft.util.ExProperties;
import ru.biosoft.util.TextUtil2;
import ru.biosoft.access.core.PluginEntry;
import ru.biosoft.access.file.GenericFileDataCollection;
import ru.biosoft.util.entry.RegularFileEntry;

/**
 *   LocalRepository creates hierarchical ru.biosoft.access.core.DataCollection. The information about used
 * in a tree nodes is extracted from files, which are organized in a tree.
 * In other words each subdirectory contains one ru.biosoft.access.core.DataCollection.
 * DataCollection is described by the special configuration files ("default.config" and *.node.config)
 * It is the standard file of a java.util.Properties . This file contains key-value
 * lines, which describes concrete ru.biosoft.access.core.DataCollection. Key values constants are defined in
 * {@link ru.biosoft.access.core.DataCollection}
 * @see  java.util.Properties#load(InputStream )
 *
 * @todo sorting order
 */
public class LocalRepository extends AbstractDataCollection<DataCollection<?>> implements Repository, HtmlDescribedElement
{
    public final static String PARENT_COLLECTION = "parent-collecion";
    public final static String PUT_TO_REPOSITORY = "put-to-repository";
    public static final String UNPROTECTED_PROPERTY = "unprotected";    // if true, children will not be protected
    public static final String CONFIG_ALT_PATH_PROPERTY = "configAltRoot";

    private final Map<String, LoggedException> initErrors = new HashMap<>();

    protected Map<String, PluginEntry> elementsConfigs = new TreeMap<>();
    protected Map<String, PluginEntry> elementsNoConfigs = new TreeMap<>();
    protected List<String> nameList;

    /** Repository root subdirectory. */
    protected File root;
    public File getRootDirectory()
    {
        return root;
    }

    public String getAbsolutePath()
    {
        return root.getAbsolutePath();
    }

    protected PluginEntry configRoot;

    ////////////////////////////////////////
    // constructors
    //
    /**
     * Constructs LocalRepository. Root subdirectory is defined by {@link ru.biosoft.access.core.DataCollection#PATH_PROPERTY}
     *
     *
     * @param parent parent ru.biosoft.access.core.DataCollection
     * @param properties DataCollection properties
     * @exception Exception If any error
     */
    public LocalRepository(DataCollection<?> parent, Properties properties) throws Exception
    {
        super(parent, properties);
        v_cache = new ConcurrentHashMap<>();
        root = new File(properties.getProperty(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY));
        String altPath = properties.getProperty( CONFIG_ALT_PATH_PROPERTY );
        if( altPath == null )
            configRoot = ApplicationUtils.resolvePluginPath( properties.getProperty( DataCollectionConfigConstants.CONFIG_PATH_PROPERTY ) );
        else
            configRoot = ApplicationUtils.resolvePluginPath( altPath, root.getPath() );
        if(!configRoot.is( root ))
        {
            PluginEntry addConfig = configRoot.child( DataCollectionConfigConstants.DEFAULT_CONFIG_FILE );
            try
            {
                EntryStream.of(new ExProperties( addConfig )).forKeyValue( getInfo().getProperties()::putIfAbsent );
            }
            catch(IOException ex)
            {
                // no file or something else: ignore
            }
        }
    }

    ///////////////////////////////////////////////////////////////////
    // Initilisation issues
    //

    //TODO: move property to DataCollectionConfigConstants
    public static final String EXCLUDE_NAMES = "exclude-names";
    private volatile boolean isInit = false;
    private Map<String, String> remapping;

    /**
     *   Scans files in {@link #root} subdirectory.
     *  If the subdirectory contains other subdirectories, they are considered as the challengers
     *  for creation nested ru.biosoft.access.core.DataCollection-s.
     *  DataCollection is created, if in the nested subdirectory is found "default.config" file.
     *  Also DataCollection is created, if a root subdirectory contains a file with ".node.config" suffix
     *  All DataCollections are added to the <b>this</b> ru.biosoft.access.core.DataCollection
     *
     * @see #createCollection(File file,boolean isNotify)
     *
     */
    protected void init()
    {
        if( isInit )
            return;
        synchronized(this)
        {
            if( isInit )
                return;
            log.log(Level.FINE, "load repository " + getName());
            long start = System.currentTimeMillis();

            Set<String> excludeNames = new HashSet<>();
            if( getInfo().getProperties().containsKey( EXCLUDE_NAMES ) )
                StreamEx.split( getInfo().getProperties().getProperty( EXCLUDE_NAMES ), ';' ).map( String::trim ).filter( TextUtil2::nonEmpty ).forEach( excludeNames::add );

            // Initialize primary collection in privileged mode
            try
            {
                SecurityManager.runPrivileged( () -> {
                    PluginEntry[] files;
                    try
                    {
                        files = configRoot.children();
                    }
                    catch( IOException e1 )
                    {
                        new DataElementReadException( e1, LocalRepository.this, "files root" ).log();
                        return null;
                    }
                    Arrays.sort( files );

                    for( PluginEntry file : files )
                    {

                        PluginEntry propertiesFile = null;

                        if( file.isDirectory() )
                        {
                            propertiesFile = file.child( DataCollectionConfigConstants.DEFAULT_CONFIG_FILE );
                        }
                        else
                        {
                            if( file.getName().endsWith(DataCollectionConfigConstants.DEFAULT_NODE_CONFIG_SUFFIX) && !file.getName().endsWith(DataCollectionConfigConstants.DEFAULT_CONFIG_FILE) )
                                propertiesFile = file;
                        }

                        if( propertiesFile != null && propertiesFile.exists() )
                        {
                            try
                            {
                                String name = readCollectionName( propertiesFile );
                                elementsConfigs.put( name, propertiesFile );
                                log.log( Level.FINE, "Initializing DC: " + propertiesFile );
                                //createCollection(propertiesFile, NO_NOTIFICATION, null, filePath);
                            }
                            catch( Throwable t )
                            {
                                LoggedException ex = ExceptionRegistry.translateException( t );
                                ex.log();
                                log.log(Level.SEVERE, ex.getMessage());
                                try
                                {
                                    initErrors.put( new ExProperties( propertiesFile ).getProperty( DataCollectionConfigConstants.NAME_PROPERTY ), ex );
                                }
                                catch( IOException e )
                                {
                                }
                            }
                        }
                        else if( file.isDirectory() && !excludeNames.contains( file.getName() ) )
                        {
                            //Directory without inner config file is treated as GenericFileDataCollection
                            elementsNoConfigs.put( file.getName(), file );
                        }
                    }
                    return null;
                } );
            }
            catch( Exception e )
            {
                throw ExceptionRegistry.translateException( e );
            }
            System.out.println( "Load " + getCompletePath() + ", " + elementsConfigs.size() + " items initialized, time="
                    + ( System.currentTimeMillis() - start ) );

            String remap = getInfo().getProperty( "remap" );
            if(remap != null)
            {
                remapping = new HashMap<>();
                for(String keyValue : StreamEx.split( remap, ';')) {
                    int pos = keyValue.indexOf( '=' );
                    if(pos >= 0) {
                        String from = keyValue.substring( 0, pos ).trim();
                        String to = keyValue.substring( pos+1 ).trim();
                        PluginEntry source = elementsConfigs.get( from );
                        PluginEntry target = elementsConfigs.get( to );
                        if(source == null && target != null) {
                            remapping.put( from, to );
                        }
                    }
                }
            }

            isInit = true;
        }
    }

    private String readCollectionName(PluginEntry propertiesFile) throws Exception
    {
        Properties properties = new ExProperties( propertiesFile );
        return properties.getProperty( DataCollectionConfigConstants.NAME_PROPERTY );
    }


    /**
     * Creates DataCollection using specified config file and adds it to <B>this</B> ru.biosoft.access.core.DataCollection
     *
     * @param propertiesFile     config file
     * @param filePath path which will be used for DataCollectionConfigConstants.FILE_PATH_PROPERTY if necessary
     * @exception Exception If any error
     * @todo log message
     * @todo whether it should check that there is no collection with such name?
     */
    protected DataCollection createCollection(PluginEntry propertiesFile, FunctionJobControl fjc, File filePath) throws Exception
    {
        Properties properties = new ExProperties( propertiesFile );
        String path = properties.getProperty( DataCollectionConfigConstants.CONFIG_PATH_PROPERTY );
        String filePathStr = (filePath == null || !filePath.isDirectory()) ? path : filePath.getAbsolutePath();
        properties.setProperty(DataCollectionConfigConstants.CONFIG_FILE_PROPERTY, propertiesFile.getName());
        
        if(!properties.containsKey( DataCollectionConfigConstants.FILE_PATH_PROPERTY ))
            properties.setProperty( DataCollectionConfigConstants.FILE_PATH_PROPERTY, filePathStr );

        if( fjc != null && properties.get(DataCollectionConfigConstants.JOB_CONTROL_PROPERTY) == null )
        {
            properties.put(DataCollectionConfigConstants.JOB_CONTROL_PROPERTY, fjc);
        }

        DataCollection<DataCollection<?>> parent = null;
        String pc = properties.getProperty(PARENT_COLLECTION);
        try
        {
            if( pc != null )
                parent = CollectionFactory.getDataCollection(pc);
            else
                parent = (DataCollection)getCompletePath().optDataCollection(DataCollection.class);
        }
        catch( Exception e )
        {
            //Can not get parent data collection by name
        }

        if( parent == null )
            parent = this;

        //add parent plugin dependences
        Properties parentProperties = parent.getInfo().getProperties();
        ExProperties.addPlugins(properties, parentProperties.getProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY));

        DataCollection<?> dc = null;
        Class<?> collectionClass;
        String plugins = properties.getProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY);
        try
        {
            if(plugins == null)
                collectionClass = ClassLoading.loadClass( properties.getProperty(DataCollectionConfigConstants.CLASS_PROPERTY) );
            else
                collectionClass = ClassLoading.loadClass( properties.getProperty(DataCollectionConfigConstants.CLASS_PROPERTY), plugins );
        }
        catch( LoggedClassNotFoundException e )
        {
            throw new DataElementCreateException(e, getCompletePath().getChildPath(properties.getProperty(DataCollectionConfigConstants.NAME_PROPERTY, "")), DataCollection.class);
        }
        if( !Boolean.valueOf(getInfo().getProperty(UNPROTECTED_PROPERTY))
                && ( parent instanceof ProtectedElement )
                && ( !ProtectedElement.class.isAssignableFrom(collectionClass) ) )
        {
            //if parent DC is protected and child is not protected we should protect it
            Properties protectedProperties = new Properties();
            protectedProperties.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, properties.getProperty(DataCollectionConfigConstants.NAME_PROPERTY));
            protectedProperties.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, NetworkDataCollection.class.getName());
            protectedProperties.setProperty(DataCollectionConfigConstants.NEXT_CONFIG, propertiesFile.getName());
            if( path != null )
            {
                protectedProperties.put(DataCollectionConfigConstants.CONFIG_PATH_PROPERTY, path);
                protectedProperties.put(DataCollectionConfigConstants.FILE_PATH_PROPERTY, filePathStr);
            }
            if( plugins != null )
            {
                protectedProperties.setProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY, plugins);
            }
            dc = CollectionFactory.createCollection(parent, protectedProperties);
        }
        else
        {
            dc = CollectionFactory.createCollection(parent, properties);
            DataCollection<?> primaryDC = DataCollectionUtils.fetchPrimaryCollection(dc, 0);
            if( primaryDC != null && primaryDC.getInfo() != null && propertiesFile.getFile() != null)
            {
                primaryDC.getInfo().addUsedFile(propertiesFile.getFile());
            }
        }
        return dc;
    }

    /**
     * Remove ru.biosoft.access.core.DataElement from data collection.
     * If ru.biosoft.access.core.DataElement that should be removed is ru.biosoft.access.core.DataCollection,
     * then all {@link mgl3.access.DataCollectionInfo#getUsedFiles() files}
     * used by this data collection permanently deleted.
     *
     * @param de     DataElement which should be removed (cannot be null).
     * @exception Exception
     *                      If base version of doRemove throws Exception.
     * @see mgl3.access.DataCollectionInfo#getUsedFiles()
     * @see mgl3.access.ru.biosoft.access.core.DataCollection
     */
    @Override
    protected void doRemove(String name) throws Exception
    {
        init();

        DataCollection<?> dc = get(name);

        List<File> otherFiles = new ArrayList<>();

        DataCollectionInfo dci = dc.getInfo();
        List<File> files = dci.getUsedFiles();
        if( files != null )
        {
            for(File file : files)
            {
                if(file.isDirectory())
                {
                    otherFiles.addAll( Arrays.asList( file.listFiles() ) );
                }
                otherFiles.add( file );
            }
        }

        // recursively delete any local repositories

        String removeChildren = dci.getProperties().getProperty(DataCollectionConfigConstants.REMOVE_CHILDREN, "false");
        log.info("Remove children: " + removeChildren);
        if( "true".equalsIgnoreCase(removeChildren) )
        {
            String[] nameList = dc.getNameList().toArray(new String[dc.getSize()]);
            for( String iname : nameList )
            {
                try
                {
                    DataElement de = dc.get(iname);
                    if( de instanceof DataCollection )
                    {
                        List<File> ifiles = ( (DataCollection)de ).getInfo().getUsedFiles();
                        if( ifiles != null )
                            otherFiles.addAll(ifiles);
                    }
                    log.info("   Removing \"" + iname + "\"...");
                    dc.remove(iname);
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE, "While removing "+getCompletePath().getChildPath(iname), e);
                }
            }
        }

        // Destroy collection
        dc.close();

        // Remove from internal storage
        synchronized( elementsConfigs )
        {
            if( elementsConfigs.containsKey( name ) )
            {
                elementsConfigs.remove( name );
                nameList = null;
            }
        }

        // sort in order to remove file first than subdirs
        Collections.sort(otherFiles, (o1, o2) -> {
            try
            {
                File file1 = o1.getCanonicalFile();
                File file2 = o2.getCanonicalFile();
                return file2.compareTo(file1); // reverse order
            }
            catch( IOException ignore )
            {
                return 0;
            }
        });

        // Delete used files
        for( File file: otherFiles )
        {
            if( !file.exists() )
                log.info("File <" + file + "> doesn't exist.");
            else if( !file.delete() )
                log.log(Level.SEVERE, "Removing collection <" + name + "> : File <" + file + "> not deleted.");
            else
                log.info("File <" + file + "> deleted.");
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // redefine DataCollection methods due to lazy initialisation
    //

    @Override
    public int getSize()
    {
        init();
        return super.getSize();
    }
    
    private Map<String, Object> locks = new ConcurrentHashMap<>();
    
    @Override
    public DataCollection<?> get(String name) throws Exception
    {
        if( !isValid() )
            return null;
        DataCollection<?> de = v_cache.get(name);
        if( de != null )
            return de;
        Object lock = locks.computeIfAbsent( name, x -> new Object() );
        synchronized( lock )
        {
            de = v_cache.get( name );
            if( de != null )
                return de;
            try
            {
                de = doGet( name );
            }
            catch( DataElementGetException e )
            {
                throw e;
            }
            catch( Throwable e )
            {
                throw new DataElementGetException( e, getCompletePath().getChildPath( name ) );
            }
            if( de != null )
            {
                // basic validation
                if( !Objects.equals( de.getName(), name ) )
                    throw new DataElementGetException(
                            new InternalException( "Name of created object is invalid: " + de.getName() + "', should be: '" + name + "'" ),
                            getCompletePath().getChildPath( name ) );
            }
            if( de != null )
                cachePut( de );
        }
        return de;
    }

    @Override
    protected DataCollection<?> doGet(String name)
    {
        init();
        DataCollection<?> result = null;
        PluginEntry file = elementsConfigs.get( name );
        if( file != null )
        {
            try
            {
                result = (DataCollection<?>)SecurityManager.runPrivileged( () -> {
                    return initFromConfig( file );
                } );
            }
            catch( Exception e )
            {
                log.log( Level.SEVERE, "Can not init DC " + name + " from config file", e );
            }
            if( result == null )
            {
                //TODO: In previous version of LocalRepository elements did not get to collection on init step
                //Should element be removed from elementsConfigs if it could not be initialized properly (for example, if config file is incorrect)?
            }
        }
        else
        {
            if( elementsNoConfigs.containsKey( name ) )
            {
                try
                {
                    result = GenericFileDataCollection.initGenericFileDataCollection( this, elementsNoConfigs.get( name ).getFile() );
                }
                catch (Exception e)
                {
                    log.log( Level.SEVERE, "Can not init GenericFileDataCollection " + name + " from folder", e );
                }
            }
        }
        if( result == null && remapping != null && remapping.containsKey( name ) )
        {
            String realName = remapping.get( name );
            try
            {
                result = super.get( realName );
            }
            catch( Exception e )
            {
                log.log( Level.SEVERE, "While getting remapped " + name + " ->" + realName, e );
            }
        }
        if( result == null )
        {
            LoggedException ex = initErrors.get( name );
            if( ex != null )
                throw ex;
        }
        return result;
    }

    DataCollection<?> initFromConfig(PluginEntry propertiesFile)
    {
        File defaultFilePath = new File( getInfo().getProperties().getProperty( DataCollectionConfigConstants.FILE_PATH_PROPERTY, root.getAbsolutePath() ) );
        File filePath = defaultFilePath;

        DataCollection<?> result = null;

        if( propertiesFile.getName().equals( DataCollectionConfigConstants.DEFAULT_CONFIG_FILE ) )
            filePath = new File( defaultFilePath, propertiesFile.getParent().getName() );

        try
        {
            log.log( Level.FINE, "Open DC: " + propertiesFile );
            result = createCollection( propertiesFile, null, filePath );
        }
        catch( Throwable t )
        {
            LoggedException ex = ExceptionRegistry.translateException( t );
            ex.log();
            log.log( Level.SEVERE, ex.getMessage() );
            try
            {
                initErrors.put( new ExProperties( propertiesFile ).getProperty( DataCollectionConfigConstants.NAME_PROPERTY ), ex );
            }
            catch( IOException e )
            {
            }
        }
        return result;
    }

    @Override
    public @Nonnull Iterator<DataCollection<?>> iterator()
    {
        init();
        return super.iterator();
    }

    /**
     * Returns an unmodifiable list of the data element names contained in this data collection.
     * Query operations on the returned list "read through" to the internal name list,
     * and attempts to modify the returned list, whether direct or via its iterator,
     * result in an <code>UnsupportedOperationException</code>.
     *
     * The returned list is backed by the data collection,
     * so changes to the data collection are reflected in the returned list.
     *
     * @return Names of all elements in this data collection in alphabetically sorted order.
     */
    @Override
    public @Nonnull List<String> getNameList()
    {
        init();
        if( nameList == null )
        {
            nameList = new ArrayList<>( elementsConfigs.keySet() );
            nameList.addAll( elementsNoConfigs.keySet() );
        }
        if( getInfo().getQuerySystem() != null )
        {
            List<String> sortedList = new ArrayList<>( nameList );
            sortNameList( sortedList );
            return sortedList;
        }
        return Collections.unmodifiableList( nameList );
    }

    @Override
    public boolean contains(String name)
    {
        init();
        return elementsConfigs.containsKey( name );
    }

    ////////////////////////////////////////
    // Info methods
    //

    /**
     * Returns ru.biosoft.access.core.DataCollection.class
     *
     * @return ru.biosoft.access.core.DataCollection.class.
     */
    @Override
    public @Nonnull Class<DataCollection> getDataElementType()
    {
        return ru.biosoft.access.core.DataCollection.class;
    }

    /**
     *  @todo Document
     */
    @Override
    public void close() throws Exception
    {
        if( isInit )
        {
            for(DataCollection<?> dc: this)
            {
                dc.close();
            }
        }

        super.close();
    }

    ////////////////////////////////////////
    // implimenting of Repository interface
    //
    /**
     *   Creates new DataCollection with specified subdir (if not null) and config file name.
     *    If config file already exists, specified controller is used. If controller
     *    permits to override existing config file, then DataCollection is created.
     *
     * @todo high Change processing of file overwriting!!!
     * @param name       DataCollection name
     * @param properties DataCollection properties
     * @param subDir     Sub directory for new collection.
     * @param files      Files to be moved into repository
     * @param controller Object for special control functions (dialogs for example).
     * @return Created data collection, or <b>null</b>.
     * @exception Exception If error occurred.
     */
    @Override
    public DataCollection createDataCollection(String name, Properties properties, String subDir, File[] files, CreateDataCollectionController controller) throws Exception
    {
        File newRoot = root;
        String configName;
        if( subDir == null )
        {
            configName = name + ".node" + DataCollectionConfigConstants.DEFAULT_CONFIG_SUFFIX;
        }
        else
        {
            configName = DataCollectionConfigConstants.DEFAULT_CONFIG_FILE;
            newRoot = new File(root, subDir);
            newRoot.mkdirs();
        }

        // write config file
        File config = new File(newRoot, configName);

        // Ask controller for file overwriting
        if( controller != null )
        {
            int answer = CreateDataCollectionController.CANCEL;
            if( config.exists() && ( answer = controller.fileAlreadyExists(config) ) == CreateDataCollectionController.CANCEL )
            {
                return null;
            }
            if( files != null && answer != CreateDataCollectionController.OVERWRITE_ALL )
            {
                for( File file : files )
                {
                    File f = new File(newRoot, file.getName());
                    if( f.exists() )
                    {
                        answer = controller.fileAlreadyExists(f);
                        if( answer == CreateDataCollectionController.CANCEL )
                            return null;
                        if( answer == CreateDataCollectionController.OVERWRITE_ALL )
                            break;
                    }
                }
            }
        }
        ExProperties.store(properties, config);

        FunctionJobControl jc = controller == null ? null : controller.getJobControl();

        if(files != null)
        {
            for( File file : files )
            {
                com.developmentontheedge.application.ApplicationUtils.linkOrCopyFile(new File(newRoot, file.getName()), file, jc);
                file.delete();
            }
        }

        DataCollection<?> newDC = createCollection( new RegularFileEntry( config ), jc, newRoot );
        try
        {
            String putNecessary = properties.getProperty( PUT_TO_REPOSITORY );
            if( putNecessary == null || !putNecessary.equals( "false" ) )
            {
                super.put( newDC );
            }
        }
        catch( Throwable t )
        {
            log.log( Level.SEVERE, ExceptionRegistry.log( t ) );
        }

        // @pending in fact we mark as used all subdirs between root and newRoot
        if( subDir != null )
        {
            newDC.getInfo().addUsedFile(newRoot);
        }

        return newDC;
    }


    /**
     * Constants for {@link createCollection(File file,boolean isNotify)} method
     */
    private final static boolean NO_NOTIFICATION = false;
    private final static boolean NOTIFICATION = true;
    @Override
    protected void doPut(DataCollection<?> dc, boolean isNew)
    {
        if( dc == null )
            throw new IllegalArgumentException( "DataCollection cannot be null." );

        String name = dc.getName();
        File configFile = null;

        Properties newProperties = (Properties)dc.getInfo().getProperties().clone();

        String configFileName = newProperties.getProperty( DataCollectionConfigConstants.CONFIG_FILE_PROPERTY,
                DataCollectionConfigConstants.DEFAULT_CONFIG_FILE );
        String configFilePath = newProperties.getProperty( DataCollectionConfigConstants.CONFIG_PATH_PROPERTY );
        File configPath;
        if( configFilePath == null )
        {
            if( configFileName.equals( DataCollectionConfigConstants.DEFAULT_CONFIG_FILE ) )
            {
                configPath = new File( getRootDirectory(), dc.getName() );
                configPath.mkdirs();
            }
            else
            {
                configPath = getRootDirectory();
            }
        }
        else
        {
            configPath = new File( configFilePath );
        }
        newProperties.remove( DataCollectionConfigConstants.CONFIG_FILE_PROPERTY );
        newProperties.remove( DataCollectionConfigConstants.CONFIG_PATH_PROPERTY );
        newProperties.remove( DataCollectionConfigConstants.FILE_PATH_PROPERTY );
        newProperties.remove( DataCollectionConfigConstants.PRIMARY_COLLECTION );

        configFile = new File( configPath, configFileName );
        PluginEntry entry = new RegularFileEntry( configFile );

        synchronized( elementsConfigs )
        {
            if( !elementsConfigs.containsKey( name ) )
                nameList = null;
            elementsConfigs.put( name, entry );
        }
        //resave config if changed
        try
        {
            if( configFile != null && newProperties != null )
            {
                if( newProperties.containsKey( DataCollectionConfigConstants.CLASS_PROPERTY ) )
                {
                    if( !configFile.exists() )
                        ExProperties.store( newProperties, configFile );
                    else
                    {
                        Properties fromConfig = new Properties();
                        try (InputStream is = entry.getInputStream();
                                InputStreamReader reader = new InputStreamReader( is, StandardCharsets.UTF_8 ))
                        {
                            fromConfig.load( reader );
                            if( !fromConfig.equals( newProperties ) )
                                ExProperties.store( newProperties, configFile );
                        }
                    }
                }
                else
                {
                    log.log( Level.FINE, DataCollectionConfigConstants.CLASS_PROPERTY
                            + " is absent in properties but required, config file was not saved." );
                }
            }
        }
        catch( IOException e )
        {
            throw new DataElementPutException( e, getCompletePath().getChildPath( dc.getName() ) );
        }
    }

    @Override
    public DataElementDescriptor getDescriptor(String name)
    {
        DataCollection<?> element = null;
        try
        {
            element = get(name);
        }
        catch( Exception e )
        {
        }
        if(element == null) return null;
        Properties elementProperties = element.getInfo().getProperties();
        Map<String, String> properties = EntryStream.of(elementProperties).selectKeys(String.class).selectValues(String.class).toMap();
        boolean leaf = Boolean.valueOf(element.getInfo().getProperties().getProperty(DataCollectionConfigConstants.IS_LEAF));
        return new DataElementDescriptor(DataCollectionUtils.getPrimaryElementType(element), leaf, properties);
    }

    @Override
    public URL getBase()
    {
        try
        {
            return root.toURI().toURL();
        }
        catch( MalformedURLException e )
        {
            return null;
        }
    }

    @Override
    public String getBaseId()
    {
        return root.toString();
    }

    @Override
    public String getDescriptionHTML()
    {
        String description = getInfo().getDescription();
        if( description == null )
            return "";
        URL url = null;
        try
        {
            url = ( new File(root, description) ).toURI().toURL();
        }
        catch( MalformedURLException e1 )
        {
        }
        if( url == null )
        {
            return description;
        }
        try
        {
            return com.developmentontheedge.application.ApplicationUtils.readAsString(url.openStream());
        }
        catch( Exception e )
        {
            return "";
        }
    }

    @Override
    public void updateRepository()
    {
        isInit = false;
        v_cache.clear();
        nameList = null;
        elementsConfigs.clear();
        
    }

}
