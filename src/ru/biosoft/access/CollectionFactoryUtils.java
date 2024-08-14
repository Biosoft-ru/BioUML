package ru.biosoft.access;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementCreateException;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPutException;
import ru.biosoft.access.core.DerivedDataCollection;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.SymbolicLinkDataCollection;
import ru.biosoft.access.core.TransformedDataCollection;
import ru.biosoft.access.core.Transformer;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.access.exception.DataElementExistsException;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.util.ExProperties;
import ru.biosoft.util.TempFiles;

public class CollectionFactoryUtils
{
    protected static final Logger log = Logger.getLogger( CollectionFactoryUtils.class.getName() );

    static
    {
        initVirtualCollections();
        initSecurityManager();
        ImageIO.setUseCache( false );
    }

    public static void init()
    {
        //Empty init to invoke static {} block
        //TODO: move inits to proper places
    }

    private static DataCollection<?> createVirtualCollection(DataCollection<?> parent, IConfigurationElement element)
    {
        VectorDataCollection<DataElement> result = new VectorDataCollection<>( element.getAttribute( "name" ), parent, null );
        for( IConfigurationElement child : element.getChildren( "folder" ) )
        {
            try
            {
                result.put( createVirtualCollection( result, child ) );
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE,  "While initializing virtual collection " + result.getCompletePath(), e );
            }
        }
        for( IConfigurationElement child : element.getChildren( "link" ) )
        {
            try
            {
                result.put( new SymbolicLinkDataCollection( result, child.getAttribute( "name" ),
                        DataElementPath.create( child
                        .getAttribute( "target" ) ) ) );
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE,  "While initializing virtual collection " + result.getCompletePath(), e );
            }
        }
        return result;
    }

    /**
     * Initializes virtual collections created via extension-points
     */
    private static void initVirtualCollections()
    {
        IExtensionRegistry registry = Application.getExtensionRegistry();
        if( registry == null )
            return;
        IConfigurationElement[] extensions = registry.getConfigurationElementsFor( "ru.biosoft.access.virtualCollection" );
        if( extensions == null )
            return;
        for( IConfigurationElement extension : extensions )
        {
            DataCollection<?> collection = createVirtualCollection( null, extension );
            CollectionFactory.registerRoot( collection );
        }
    }

    /**
     * Initialize JavaScript security manager if necessary
     */
    private static void initSecurityManager()
    {
        System.setProperty( "java.security.policy", "biouml.policy" );

        try
        {
            if( System.getProperty( "biouml.server.path" ) != null )
                System.setSecurityManager( new BiosoftSecurityManager() );
        }
        catch( Throwable e )
        {
            log.log(Level.SEVERE,  "Error: could not set security manager", e );
        }
    }

    /**
     * Creates {@link DataCollection} with the specified parent and properties.
     */
    static public @Nonnull DataCollection createCollection(DataCollection<?> parent, Properties properties)
    {
        String className = properties.getProperty( DataCollectionConfigConstants.CLASS_PROPERTY );
        DataElementPath childPath = DataElementPath.create( parent,
                properties.getProperty( DataCollectionConfigConstants.NAME_PROPERTY, "" ) );
        try
        {
            String pluginNames = properties.getProperty( DataCollectionConfigConstants.PLUGINS_PROPERTY );
            if( pluginNames == null )
                pluginNames = ClassLoading.getPluginForClass( className );
            Class<? extends DataCollection> c = ClassLoading.loadSubClass( className, pluginNames, DataCollection.class );
            Constructor<? extends DataCollection> constructor = c.getConstructor( DataCollection.class, Properties.class );
            return constructor.newInstance( parent, properties );
        }
        catch( DataElementCreateException e )
        {
            if( e.getProperty( "path" ).equals( childPath ) )
                throw e;
            throw new DataElementCreateException( e, DataElementPath.create( childPath.toString() ),
                    DataCollection.class );
        }
        catch( Throwable t )
        {
            throw new DataElementCreateException( t, DataElementPath.create( childPath.toString() ),
                    DataCollection.class );
        }
    }

    /**
     * Creates directory for the new database in "databases" repository (useful when installing new module)
     * @param dbName - name of new database
     * @return File - created directory
     * @throws Exception if error occurred
     */
    public static File createDatabaseDirectory(String dbName) throws Exception
    {
        if( !SecurityManager.isAdmin() )
            throw new SecurityException( "Access denied" );
        Repository repository = (Repository)getDatabases();
        if( repository.contains( dbName ) )
        {
            throw new DataElementExistsException( repository.getCompletePath().getChildPath( dbName ) );
        }
        LocalRepository localRepository = repository instanceof LocalRepository ? (LocalRepository)repository
                : (LocalRepository) ( (DerivedDataCollection<?, ?>)repository ).getPrimaryCollection();
        File root = localRepository.getRootDirectory();
        File directory = new File( root, dbName );
        if( !directory.mkdir() )
            throw new Exception( "Cannot create directory " + directory.getAbsolutePath() );
        return directory;
    }

    /**
     * @deprecated use DataElement.cast(clazz)
     */
    @Deprecated
    static public @Nonnull <T extends DataElement> T castDataElement(DataElement de, @Nonnull Class<T> clazz)
    {
        return de.cast( clazz );
    }

    /**
     * Returns list of DataCollections which start with given root path and can contain elements of specified type
     * @param root - String representing path from which search started. If null, then root of repository is assumed
     * @param wantedType - Class of wanted objects
     * @param limit - maximum number of collections to return (-1 for unlimited -- default)
     * @return array of DataCollection objects (empty array if nothing found)
     * @see findDataCollectionNames
     */
    static public <T extends DataElement> DataCollection<T>[] findDataCollections(DataElementPath root,
            Class<T> wantedType, int limit)
    {
        List<DataCollection<T>> result = new ArrayList<>();
        Set<DataElementPath> current = new HashSet<>();
        if( root != null && !root.isEmpty() )
            current.add( root );
        else
            CollectionFactory.getRootNames().stream().map( DataElementPath::create ).forEach( current::add );
        while( !current.isEmpty() )
        {
            Set<DataElementPath> newCurrent = new HashSet<>();
            for( DataElementPath dcName : current )
            {
                //System.out.println(dcName);
                DataCollection<?> dc = dcName.optDataCollection();
                if( dc == null )
                    continue;
                Class<?> elementType = dc.getDataElementType();
                if( wantedType.isAssignableFrom( elementType ) )
                {
                    result.add( (DataCollection<T>)dc );
                    if( limit >= 0 && result.size() >= limit )
                        return result.toArray( new DataCollection[result.size()] );
                }
                if( elementType == DataCollection.class || dc instanceof FolderCollection )
                {
                    dc.stream().filter( de -> de instanceof DataCollection )
                            .map( childDc -> DataElementPath
                                    .create( ( (DataCollection<?>)childDc ).getCompletePath().toString() ) )
                            .forEach( newCurrent::add );
                }
            }
            current = newCurrent;
        }
        return result.toArray( new DataCollection[0] );
    }

    /**
     * Returns list of DataCollections which start with given root path and can contain elements of specified type
     * @param root - String representing path from which search started. If null, then root of repository is assumed
     * @param wantedType - Class of wanted objects
     * @return array of DataCollection objects (empty array if nothing found)
     * @see findDataCollectionNames
     */
    static public <T extends DataElement> DataCollection<T>[] findDataCollections(DataElementPath root,
            Class<T> wantedType)
    {
        return findDataCollections( root, wantedType, -1 );
    }

    ////////////////////////////////////////////////////////////////////////////
    // TransformedCollection issues
    //

    static public DataCollection<?> createTransformedCollection(Repository parent, String name,
            Class<?> transformerClass, Class<? extends DataElement> dataElementType, String imgName,
            String childrenImage, String fileFilter, String startTag, String idTag, String endTag, String subDir) throws Exception
    {
        // Create primary collection config file and store it
        Properties primary = new ExProperties();
        primary.setProperty( DataCollectionConfigConstants.CLASS_PROPERTY, FileEntryCollection2.class.getName() );
        primary.setProperty( DataCollectionConfigConstants.FILE_PROPERTY, name + fileFilter );
        primary.setProperty( EntryCollection.ENTRY_START_PROPERTY, startTag );
        primary.setProperty( EntryCollection.ENTRY_ID_PROPERTY, idTag );
        primary.setProperty( EntryCollection.ENTRY_END_PROPERTY, endTag );
        primary.setProperty( EntryCollection.ENTRY_DELIMITERS_PROPERTY, "\"; \"" );
        primary.setProperty( EntryCollection.ENTRY_KEY_FULL, "true" );

        Properties transformed = new ExProperties();
        transformed.setProperty( DataCollectionConfigConstants.CLASS_PROPERTY, TransformedDataCollection.class.getName() );
        transformed.setProperty( DataCollectionConfigConstants.TRANSFORMER_CLASS, transformerClass.getName() );

        if( dataElementType != null )
            transformed.setProperty( DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY, dataElementType.getName() );
        if( imgName != null )
            transformed.setProperty( DataCollectionConfigConstants.NODE_IMAGE, imgName );
        if( childrenImage != null )
            transformed.setProperty( DataCollectionConfigConstants.CHILDREN_NODE_IMAGE, childrenImage );

        return createDerivedCollection( parent, name, primary, transformed, subDir );
    }

    public static Repository createLocalRepository(Repository parent, String name) throws Exception
    {
        Properties props = new Properties();
        props.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, name );
        props.setProperty( DataCollectionConfigConstants.CLASS_PROPERTY, LocalRepository.class.getName() );
        return (Repository)createSubDirCollection( parent, name, props );
    }

    public static DataCollection<?> createSubDirCollection(Repository parent, String name, Properties primary) throws Exception
    {
        return parent.createDataCollection( name, primary, name, null, null );
    }

    public static DataCollection<?> createDerivedCollection(Repository parent, String name, Properties primary, Properties derived,
            String subDir) throws Exception
    {
        if( !primary.containsKey( DataCollectionConfigConstants.NAME_PROPERTY ) )
            primary.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, name );

        if( !derived.containsKey( DataCollectionConfigConstants.NAME_PROPERTY ) )
            derived.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, name );
        if( !derived.containsKey( DataCollectionConfigConstants.NEXT_CONFIG ) )
            derived.setProperty( DataCollectionConfigConstants.NEXT_CONFIG, name + DataCollectionConfigConstants.DEFAULT_FORMAT_CONFIG_SUFFIX );

        // Create primary collection config file and store it
        File tmpDir = TempFiles.dir( "derivedCollection" );

        try
        {
            File tmp = new File( tmpDir, derived.getProperty( DataCollectionConfigConstants.NEXT_CONFIG ) );
            ExProperties.store( primary, tmp );
            return parent.createDataCollection( name, derived, subDir, new File[] {tmp}, null );
        }
        finally
        {
            ApplicationUtils.removeDir( tmpDir );
        }
    }

    static public DataCollection<?> createTransformedFileCollection(Repository parent, String name, String filter,
            Class<? extends Transformer<?, ?>> transformerClass) throws Exception
    {
        return createTransformedFileCollection( parent, name, filter, transformerClass, new Properties() );
    }

    static public DataCollection<?> createTransformedFileCollection(Repository parent, String name, String filter,
            Class<? extends Transformer<?, ?>> transformerClass, Properties additional) throws Exception
    {
        Properties primary = new Properties();
        primary.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, name );
        primary.setProperty( DataCollectionConfigConstants.CLASS_PROPERTY, FileCollection.class.getName() );
        primary.setProperty( FileCollection.FILE_FILTER, filter );

        Properties derived = new Properties();
        for( Object key : additional.keySet() )
        {
            derived.setProperty( (String)key, additional.getProperty( (String)key ) );
        }
        derived.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, name );
        derived.setProperty( DataCollectionConfigConstants.CLASS_PROPERTY, TransformedDataCollection.class.getName() );
        derived.setProperty( DataCollectionConfigConstants.TRANSFORMER_CLASS, transformerClass.getName() );

        return createDerivedCollection( parent, name, primary, derived, name );
    }

    static public <T extends DataElement> DataCollection<T> createTransformedSqlCollection(Repository parent,
            String name,
            Class<? extends SqlTransformer<T>> transformerClass, Class<T> dataElementType, Properties properties) throws Exception
    {
        properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, name );
        properties.setProperty( DataCollectionConfigConstants.CLASS_PROPERTY, SqlDataCollection.class.getName() );
        properties.setProperty( SqlDataCollection.SQL_TRANSFORMER_CLASS, transformerClass.getName() );
        if( dataElementType != null )
            properties.setProperty( DataCollectionConfigConstants.DATA_ELEMENT_CLASS_PROPERTY, dataElementType.getName() );

        @SuppressWarnings ( "unchecked" )
        DataCollection<T> result = parent.createDataCollection( name, properties, null, null, null );

        return result;
    }

    ////////////////////////////////////////////////////////////////////////////
    // GenericDataCollection issues
    //

    public static DataElementPath getUserProjectsPath()
    {
        DataElementPath path = DataElementPath.create( Application.getGlobalValue( "UserProjectsPath" ) );
        if( !path.exists() )
            path = DataElementPath.create( "data/Collaboration" );
        return path;
    }

    /**
     * @return databases collection
     */
    @SuppressWarnings ( {"unchecked", "rawtypes"} )
    public static @Nonnull DataCollection<DataCollection<?>> getDatabases()
    {
        return (DataCollection)DataElementPath.create( "databases" ).getDataCollection( DataCollection.class );
    }

    public static void save(@Nonnull DataElement de) throws DataElementPutException
    {
        DataElementPath.create( de ).save( de );
    }
}
