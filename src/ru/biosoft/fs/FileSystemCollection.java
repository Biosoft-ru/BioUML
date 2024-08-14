package ru.biosoft.fs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nonnull;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;

import org.yaml.snakeyaml.Yaml;

import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementDescriptor;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.InvalidElement;
import ru.biosoft.access.exception.CollectionLoginException;
import ru.biosoft.access.core.DataElementGetException;
import ru.biosoft.access.core.DataElementNotFoundException;
import ru.biosoft.access.core.DataElementPutException;
import ru.biosoft.access.core.DataElementReadException;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.exception.ParameterNotAcceptableException;
import ru.biosoft.access.security.CredentialsCollection;
import ru.biosoft.util.ClassExtensionRegistry;
import ru.biosoft.util.TempFile;
import ru.biosoft.util.TempFiles;

public class FileSystemCollection extends AbstractDataCollection<DataElement> implements FolderCollection, CredentialsCollection
{
    private static final String DEFAULT_FILE_SYSTEM_NAME = "local";
    public static final String METAFILE_NAME = ".biouml_metadata";
    public static final String FILE_SYSTEM_PROPERTY = "fileSystem";
    public static final String FILE_SYSTEM_PROPERTIES_PREFIX = "fileSystem.";
    private static final ClassExtensionRegistry<FileSystem> registry = new ClassExtensionRegistry<>( "ru.biosoft.fs.fileSystem", "name",
            FileSystem.class );
    private static final ScheduledExecutorService daemonPool = Executors.newScheduledThreadPool( 0, r -> new Thread( r, "FileSystemCollection properties serializer" ) );
    private static final FileSystemElementDriver[] drivers = {new FolderDriver(), new FileDriver()};

    private final FileSystem fileSystem;
    private final FileSystemPath fileSystemPath;
    private volatile ElementData elementData;
    private volatile boolean closed = false;

    public static FileSystemElementDriver getDriverByClass(Class<? extends DataElement> clazz) throws ParameterNotAcceptableException
    {
        for(FileSystemElementDriver driver: drivers)
        {
            if(driver.isSupported( clazz ))
                return driver;
        }
        throw new ParameterNotAcceptableException( "class", clazz.getName() );
    }

    private class ElementData
    {
        final ConcurrentMap<String, ElementInfo> elementInfos;
        final Lock updateLock = new ReentrantLock();
        final Callable<Void> updater = new Callable<Void>()
        {
            @Override
            public Void call() throws Exception
            {
                updateLock.lock();
                try
                {
                    writeMeta( elementInfos.values() );
                }
                catch(IOException ex)
                {
                    ExceptionRegistry.log( new DataElementPutException( ex, getCompletePath() ) );
                }
                finally
                {
                    updateLock.unlock();
                }
                return null;
            }
        };
        volatile ScheduledFuture<Void> future;

        public ElementData() throws IOException
        {
            boolean hasMetaFile = false;
            Map<String, ElementInfo> elementInfos = new HashMap<>();
            for( FileSystemEntry entry : fileSystem.list( fileSystemPath ) )
            {
                if( entry.getName().equals( METAFILE_NAME ) )
                {
                    hasMetaFile = true;
                }
                if( entry.getName().startsWith( METAFILE_NAME ) )
                {
                    continue;
                }
                elementInfos.put( entry.getName(), new ElementInfo( entry.getName(), entry.isDirectory() ) );
            }
            if( hasMetaFile )
            {
                try(ByteArrayOutputStream baos = new ByteArrayOutputStream())
                {
                    fileSystem.readFile( fileSystemPath.child( METAFILE_NAME ), baos );
                    readMeta( elementInfos, baos.toString( "UTF-8" ) );
                }
            }
            this.elementInfos = new ConcurrentHashMap<>( elementInfos );
        }

        private void readMeta(Map<String, ElementInfo> elementInfos, String meta)
        {
            Object metadataObj = new Yaml().load( meta );
            if( metadataObj instanceof Map )
            {
                @SuppressWarnings ( "unchecked" )
                Map<String, Object> metadata = (Map<String, Object>)metadataObj;
                EntryStream.of(metadata).selectValues( Map.class ).forKeyValue(
                    (name, props) -> elementInfos.computeIfPresent( name, (k, v) -> new ElementInfo(v.getName(), v.getClazz(), props) )
                );
            }
        }

        private void writeMeta(Collection<ElementInfo> elementInfos) throws IOException
        {
            Map<String, Object> rootMap = new TreeMap<>();
            for(ElementInfo info : elementInfos)
            {
                Map<String, Object> elementMap = info.write();
                if(elementMap != null)
                {
                    rootMap.put( info.getName(), elementMap );
                }
            }
            if(!rootMap.isEmpty())
            {
                byte[] bytes = new Yaml().dump( rootMap ).getBytes( StandardCharsets.UTF_8 );
                fileSystem.writeFile( fileSystemPath.child( METAFILE_NAME ), bytes.length, new ByteArrayInputStream( bytes ) );
            } else
            {
                fileSystem.delete( fileSystemPath.child( METAFILE_NAME ) );
            }
        }

        public DataElementDescriptor getDescriptor(String name)
        {
            ElementInfo elementInfo = elementInfos.get( name );
            return elementInfo == null ? null : elementInfo.getDescriptor();
        }

        public void remove(String name)
        {
            updateLock.lock();
            try
            {
                if(elementInfos.remove( name ) != null)
                {
                    updateMeta();
                }
            }
            finally
            {
                updateLock.unlock();
            }
        }

        public void put(String name, ElementInfo newInfo)
        {
            updateLock.lock();
            try
            {
                ElementInfo oldInfo = elementInfos.put( name, newInfo );
                if(!newInfo.equals( oldInfo ))
                {
                    updateMeta();
                }
            }
            finally
            {
                updateLock.unlock();
            }
        }

        private void updateMeta()
        {
            if(future != null)
            {
                future.cancel( false );
            }
            future = daemonPool.schedule( updater, 1, TimeUnit.SECONDS );
        }

        public void flush()
        {
            if(future != null && !future.isDone())
            {
                future.cancel( false );
                try
                {
                    updater.call();
                }
                catch( Exception e )
                {
                    ExceptionRegistry.log(e);
                }
            }
        }
    }

    public FileSystemCollection(DataCollection<?> parent, Properties properties) throws Exception
    {
        super( parent, properties );
        Class<? extends FileSystem> fsClass = registry.getExtension( properties
                .getProperty( FILE_SYSTEM_PROPERTY, DEFAULT_FILE_SYSTEM_NAME ) );
        if( fsClass == null )
        {
            throw new DataElementReadException( this, FILE_SYSTEM_PROPERTY );
        }
        Properties fsProperties = new Properties();
        for( Object keyObj : properties.keySet() )
        {
            String key = keyObj.toString();
            if( key.startsWith( FILE_SYSTEM_PROPERTIES_PREFIX ) )
            {
                fsProperties.setProperty( key.substring( FILE_SYSTEM_PROPERTIES_PREFIX.length() ), properties.getProperty( key ) );
            }
        }
        if( properties.getProperty( DataCollectionConfigConstants.FILE_PATH_PROPERTY ) != null && fsProperties.getProperty( DataCollectionConfigConstants.FILE_PATH_PROPERTY ) == null )
        {
            fsProperties.put( DataCollectionConfigConstants.FILE_PATH_PROPERTY, new File( properties.getProperty( DataCollectionConfigConstants.FILE_PATH_PROPERTY ), "Data" ).toString() );
        }
        fileSystem = fsClass.getConstructor( Properties.class ).newInstance( fsProperties );
        fileSystemPath = FileSystemPath.of();
    }

    protected FileSystemCollection(FileSystemCollection parent, String name)
    {
        super( name, parent, null );
        this.fileSystem = parent.fileSystem;
        this.fileSystemPath = parent.fileSystemPath.child( name );
    }

    @Override
    public void close() throws Exception
    {
        closed = true;
        if(elementData != null)
        {
            elementData.flush();
        }
        super.close();
    }

    @Override
    public void removeFromCache(String dataElementName)
    {
        if( v_cache != null )
        {
            DataElement element = v_cache.remove(dataElementName);
            if(element instanceof FileSystemCollection)
            {
                ( (FileSystemCollection)element ).elementData.flush();
            }
        }
    }

    @Override
    @Nonnull
    public List<String> getNameList()
    {
        if( !isValid() )
            return Collections.emptyList();
        initNames();
        return StreamEx.ofValues( elementData.elementInfos ).sorted().map( ElementInfo::getName ).toList();
    }

    private void initNames()
    {
        if( elementData == null )
        {
            synchronized( this )
            {
                if( elementData == null )
                {
                    try
                    {
                        elementData = new ElementData();
                    }
                    catch( IOException e )
                    {
                        valid = false;
                        throw new DataElementReadException( e, this, "names" );
                    }
                }
            }
        }
    }

    @Override
    public DataElementDescriptor getDescriptor(String name)
    {
        if( !isValid() )
            return null;
        initNames();
        return elementData.getDescriptor( name );
    }

    @Override
    protected DataElement doGet(String name) throws Exception
    {
        initNames();
        ElementInfo elementInfo = elementData.elementInfos.get( name );
        if(elementInfo == null)
        {
            return null;
        }
        if(FolderCollection.class.isAssignableFrom( elementInfo.getClazz() ))
        {
            return elementInfo.getDriver().create( this, elementInfo, null );
        }
        try(TempFile file = TempFiles.file( "fsc" ))
        {
            try(FileOutputStream out = new FileOutputStream( file ))
            {
                fileSystem.readFile( fileSystemPath.child( name ), out );
            }
            try
            {
                return elementInfo.getDriver().create( this, elementInfo, file );
            }
            catch( Throwable t )
            {
                DataElementGetException dege = new DataElementGetException( t, getCompletePath().getChildPath( name ) );
                return new InvalidElement( name, this, dege );
            }
        }
    }

    @Override
    protected void doPut(DataElement dataElement, boolean isNew) throws Exception
    {
        FileSystemElementDriver driver = getDriverByClass( dataElement.getClass() );
        ElementInfo oldInfo = elementData.elementInfos.get( dataElement.getName() );
        ElementInfo newInfo;
        FileSystemPath childPath = fileSystemPath.child( dataElement.getName() );
        if( dataElement instanceof FolderCollection )
        {
            if(oldInfo != null && !FolderCollection.class.isAssignableFrom( oldInfo.getClazz() ))
            {
                try
                {
                    fileSystem.delete( childPath );
                }
                catch( IOException e )
                {
                    // Ignore
                }
            }
            if(oldInfo == null || !FolderCollection.class.isAssignableFrom( oldInfo.getClazz() ))
            {
                fileSystem.createDirectory( childPath );
            }
            newInfo = driver.save( dataElement, null );
        } else
        {
            if(oldInfo != null && FolderCollection.class.isAssignableFrom( oldInfo.getClass() ))
            {
                throw new IllegalArgumentException( "For safety replacing of folder with new item is disabled: delete folder first." );
            }
            try(TempFile file = TempFiles.file( "fsc" ))
            {
                newInfo = driver.save( dataElement, file );
                try(FileInputStream is = new FileInputStream(file))
                {
                    fileSystem.writeFile( childPath, file.length(), is );
                }
            }
        }
        elementData.put( dataElement.getName(), newInfo );
    }

    public static StreamEx<String> getAvailableTypes(DataElement de)
    {
        return de.getOrigin().cast( FileSystemCollection.class ).getAvailableTypes( de.getName() );
    }

    private StreamEx<String> getAvailableTypes(String name)
    {
        ElementInfo elementInfo = elementData.elementInfos.get( name );
        if(elementInfo == null)
        {
            throw new DataElementNotFoundException( getCompletePath().getChildPath( name ) );
        }
        return elementInfo.getDriver().getAvailableTypes(elementInfo);
    }

    public String getElementType(String name) throws Exception
    {
        ElementInfo elementInfo = elementData.elementInfos.get( name );
        if(elementInfo == null)
        {
            throw new DataElementNotFoundException( getCompletePath().getChildPath( name ) );
        }
        return elementInfo.getDriver().getCurrentType( elementInfo );
    }

    public void setElementType(String name, String type) throws Exception
    {
        ElementInfo elementInfo = elementData.elementInfos.get( name );
        if(elementInfo == null)
        {
            throw new DataElementNotFoundException( getCompletePath().getChildPath( name ) );
        }
        ElementInfo newInfo = elementInfo.getDriver().updateInfoForType(elementInfo, type);
        if(!newInfo.equals( elementInfo ))
        {
            removeFromCache( name );
            elementData.put( name, newInfo );
            fireElementChanged( this, this, name, null, null );
        }
    }

    @Override
    public boolean isAcceptable(Class<? extends DataElement> clazz)
    {
        try
        {
            getDriverByClass( clazz );
            return true;
        }
        catch( ParameterNotAcceptableException e )
        {
            return false;
        }
    }

    @Override
    protected void doRemove(String name) throws IOException
    {
        try
        {
            fileSystem.delete( fileSystemPath.child( name ) );
        }
        catch( FileNotFoundException e )
        {
            // Ignore
        }
        elementData.remove( name );
    }

    @Override
    protected void validateName(String dataElementName)
    {
        super.validateName( dataElementName );
        if( dataElementName.startsWith( METAFILE_NAME ) || !fileSystem.isValid( dataElementName ) )
            throw new IllegalArgumentException( "Inacceptable element name: " + dataElementName );
    }

    @Override
    public DataCollection createSubCollection(String name, Class<? extends FolderCollection> clazz)
    {
        try
        {
            FileSystemCollection collection = new FileSystemCollection( this, name );
            put( collection );
            return getCompletePath().getChildPath( name ).getDataCollection();
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException( e );
        }
    }

    @Override
    public boolean needCredentials()
    {
        return fileSystem instanceof CredentialsCollection && ( (CredentialsCollection)fileSystem ).needCredentials();
    }

    @Override
    public Object getCredentialsBean()
    {
        if( ! ( fileSystem instanceof CredentialsCollection ) )
            return null;
        return ( (CredentialsCollection)fileSystem ).getCredentialsBean();
    }

    @Override
    public void processCredentialsBean(Object bean) throws CollectionLoginException
    {
        if( ! ( fileSystem instanceof CredentialsCollection ) )
            return;
        try
        {
            ( (CredentialsCollection)fileSystem ).processCredentialsBean( bean );
        }
        catch( CollectionLoginException e )
        {
            throw new CollectionLoginException( e.getCause(), this, e.getProperty( "user" ).toString() );
        }
        reinitialize();
    }
}
