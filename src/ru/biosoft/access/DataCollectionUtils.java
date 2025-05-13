package ru.biosoft.access;

import java.io.File;
import java.sql.Connection;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementCreateException;
import ru.biosoft.access.core.DataElementDescriptor;
import ru.biosoft.access.core.DataElementInvalidTypeException;
import ru.biosoft.access.core.DataElementNotFoundException;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementReadException;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.core.SymbolicLinkDataCollection;
import ru.biosoft.access.core.TransformedDataCollection;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.exception.DataElementExistsException;
import ru.biosoft.access.exception.QuotaException;
import ru.biosoft.access.file.FileBasedCollection;
import ru.biosoft.access.file.FileDataElement;
import ru.biosoft.access.file.GenericFileDataCollection;
import ru.biosoft.access.generic.GenericDataCollection;
import ru.biosoft.access.generic2.GenericDataCollection2;
import ru.biosoft.access.repository.DataCollectionTreeModelAdapter;
import ru.biosoft.access.security.Permission;
import ru.biosoft.access.security.ProtectedDataCollection;
import ru.biosoft.access.sql.SqlConnectionHolder;
import ru.biosoft.exception.MissingParameterException;
import ru.biosoft.util.Clazz;

public class DataCollectionUtils
{

    public static String SPECIES_PROPERTY = "species";
    public static String GRAPH_SEARCH = "graph-search";
    /**
     * Check whether ru.biosoft.access.core.DataElement specified by path fits the constraints for element class, child element class and element reference type
     * To omit constraint pass null.
     * Note that for the sake of speed it doesn't always check whether element actually exists
     */
    public static boolean isAcceptable(DataElementPath path, Class<? extends DataElement> childClass,
            Class<? extends DataElement> elementClass, Class<? extends ReferenceType> referenceType)
    {
        if( path.toString().equals(DataCollectionTreeModelAdapter.NONE_ELEMENT) )
            return true;
        if( childClass == null && elementClass == null && referenceType == null )
            return true;

        try
        {
            if( childClass != null && elementClass == null )
                elementClass = ru.biosoft.access.core.DataCollection.class;
            if( elementClass != null )
            {
                DataElementDescriptor descriptor = path.getDescriptor();
                if( descriptor == null )
                    return false;
                if( !elementClass.isAssignableFrom(descriptor.getType()) )
                    return false;
            }
            if( childClass != null )
            {
                DataCollection<?> element = path.optDataCollection();
                if( element == null )
                    return false;
                if( element instanceof GenericFileDataCollection )
                    return ((GenericFileDataCollection) element).isAcceptable( childClass, false );
                else
                    return element.isAcceptable( childClass );

            }
            if( referenceType != null )
            {
                if( !referenceType.isInstance(ReferenceTypeRegistry.getElementReferenceType(path)) )
                    return false;
            }
            return true;
        }
        catch( Exception e )
        {
            return false;
        }
    }

    public static boolean isAcceptable(DataElementPath path, Class<? extends DataElement> childClass,
            Class<? extends DataElement> elementClass)
    {
        return isAcceptable(path, childClass, elementClass, null);
    }

    public static boolean isAcceptable(DataCollection<?> collection, Class<? extends DataElement> childClass)
    {
        if( collection == null )
            return false;
        if( childClass == null )
            return true;
        try
        {
            return collection.isAcceptable(childClass);
        }
        catch( Exception e )
        {
            return false;
        }
    }

    /**
     * Checks whether specified path is leaf element
     * Note that for the sake of speed it doesn't check whether element actually exists, thus return value in this case is unspecified
     */
    public static boolean isLeaf(DataElementPath completeNodeName)
    {
        DataCollection<?> parent = completeNodeName.optParentCollection();
        if( parent == null )
            return false;
        DataElementDescriptor descriptor = parent.getDescriptor(completeNodeName.getName());
        if( descriptor == null )
            return false;
        return descriptor.isLeaf();
    }

    private static DataElement doFetchPrimaryElement(DataElement parent, int access)
    {
        if( ! ( parent instanceof ProtectedElement ) )
            return parent;
        return ( (ProtectedElement)parent ).getUnprotectedElement(access);
    }

    /**
     * @param parent possibly wrapped object
     * @param access wanted access
     * @return unwrapped parent if it was wrapped
     * @deprecated this method not safe and will be removed in future. The following alternatives are available:
     * - Try to avoid using this method if it's not really necessary
     * - Use methods from this class for common actions like (getFile, getSqlConnection, isFileAccepted, checkPrimaryElementType and so on)
     * - Use method fetchPrimaryCollectionPrivileged in privileged code section (see SecurityManager.runPrivilegedAction)
     */
    @Deprecated
    public static DataCollection fetchPrimaryCollection(DataCollection<?> parent, int access)
    {
        return (DataCollection)doFetchPrimaryElement(parent, access);
    }

    /**
     * @param parent possibly wrapped object
     * @param access wanted access
     * @return unwrapped parent if it was wrapped
     * @deprecated this method not safe and will be removed in future. The following alternatives are available:
     * - Try to avoid using this method if it's not really necessary
     * - Use methods from this class for common actions like (getFile, getSqlConnection, isFileAccepted, checkPrimaryElementType and so on)
     * - Use method fetchPrimaryElementPrivileged in privileged code section (see SecurityManager.runPrivilegedAction)
     */
    @Deprecated
    public static DataElement fetchPrimaryElement(DataElement parent, int access)
    {
        return doFetchPrimaryElement(parent, access);
    }

    public static DataCollection fetchPrimaryCollectionPrivileged(DataCollection<?> parent)
    {
        return (DataCollection)doFetchPrimaryElement(parent, Permission.ADMIN);
    }

    public static DataElement fetchPrimaryElementPrivileged(DataElement parent)
    {
        return doFetchPrimaryElement(parent, Permission.ADMIN);
    }

    /**
     * Checks whether primary element belongs to given type
     * @param parent either protected or unprotected element to check
     * @param type Class to check
     * @return true if primary element belongs to given type
     */
    public static boolean checkPrimaryElementType(DataElement parent, Class<? extends DataElement> type)
    {
        if( parent == null )
            return false;
        DataCollection<?> origin = DataElementPath.create(parent).optParentCollection();
        if( origin == null )
            return type.isInstance( parent );
        DataElementDescriptor descriptor = origin.getDescriptor(parent.getName());
        if( descriptor == null )
            return false;
        return type.isAssignableFrom(descriptor.getType());
    }

    public static Class<? extends DataElement> getPrimaryElementType(DataElement parent)
    {
        return doFetchPrimaryElement(parent, Permission.INFO).getClass();
    }

    public static @Nonnull DataCollection<DataElement> createSubCollection(DataElementPath path) throws Exception
    {
        return createSubCollection( path, CreateStrategy.REMOVE_WRONG_TYPE, FolderCollection.class );
    }

    public static @Nonnull DataCollection<DataElement> createSubCollection(DataElementPath path,
            boolean removeExisting) throws Exception
    {
        CreateStrategy strategy = removeExisting ? CreateStrategy.REMOVE_WRONG_TYPE : CreateStrategy.FAIL_IF_EXIST;
        return createSubCollection( path, strategy, FolderCollection.class );

    }

    public static enum CreateStrategy
    {
        FAIL_IF_EXIST, REMOVE_WRONG_TYPE, FORCE_REMOVE
    }
    /**
     * Creates new GenericDataCollection
     * @param path path for new collection. Parent path must exist and must be GenericDataCollection (possibly wrapped)
     * @param strategy strategy to use if element exists<br>
     *       - FAIL_IF_EXIST: exception will be thrown if element already exists<br>
     *       - REMOVE_WRONG_TYPE: existing element will be removed only if it has wrong type<br>
     *       - FORCE_REMOVE: existing element will be removed<br>
     * @param targetClass
     * @return
     * @throws Exception
     */
    public static @Nonnull DataCollection<DataElement> createSubCollection(DataElementPath path,
            @Nonnull CreateStrategy strategy,
            Class<? extends FolderCollection> targetClass) throws Exception
    {
        DataCollection<DataElement> parentCollection = path.getParentCollection();
        FolderCollection collection = doFetchPrimaryElement( parentCollection, Permission.WRITE ).cast( FolderCollection.class );
        String name = path.getName();
        DataElement de = doFetchPrimaryElement( path.optDataElement(), Permission.WRITE );
        if( de != null )
        {
            switch( strategy )
            {
                case FAIL_IF_EXIST:
                    throw new DataElementExistsException( path );
                case REMOVE_WRONG_TYPE:
                    if( targetClass.isInstance( de ) )
                        return (DataCollection<DataElement>)de;
                    break;
                case FORCE_REMOVE:
                    break;
                default:
                    break;
            }
            collection.remove( name );
        }
        if( de != null && CreateStrategy.FAIL_IF_EXIST == strategy )
            throw new DataElementExistsException( path );
        collection.createSubCollection( name, targetClass );
        return path.getDataElement( ru.biosoft.access.core.DataCollection.class );
    }

    /**
     * Returns type of specified element
     * This is fast check, so note that it might not be exact type, but best guess. Also element may not exist at all.
     */
    public static Class<? extends DataElement> getElementType(DataElementPath path)
    {
        DataCollection<?> parent = path.optParentCollection();
        if( parent == null )
            return null;
        DataElementDescriptor descriptor = parent.getDescriptor(path.getName());
        if( descriptor == null )
            return parent.getDataElementType();
        return descriptor.getType();
    }

    /**
     * Get protection status by {@link ru.biosoft.access.core.DataElementPath}
     */
    public static int getProtectionStatus(DataElementPath path)
    {
        DataCollection<?> parent = path.optParentCollection();
        if( parent == null )
            return ProtectedDataCollection.PROTECTION_PROTECTED_READ_ONLY;//the most strong protection for root collections
        if( ! ( parent instanceof Repository ) )
            return ProtectedDataCollection.PROTECTION_NOT_APPLICABLE;
        try
        {
            DataElement dc = parent.get(path.getName());
            if( dc instanceof ProtectedDataCollection )
            {
                return ProtectedDataCollection.PROTECTION_PUBLIC_READ_PROTECTED_WRITE;
            }
            else if( dc.getClass().getName().equals("biouml.model.ProtectedModule")
                    || dc.getClass().getName().equals("biouml.model.Module")
                    || dc.getClass().getName().equals("biouml.plugins.server.SqlModule") )
            {
                String protectionStatus = ( (DataCollection<?>)dc ).getInfo().getProperty(ProtectedDataCollection.PROTECTION_STATUS);
                return protectionStatus == null ? ProtectedDataCollection.PROTECTION_NOT_PROTECTED : Byte.parseByte(protectionStatus);
            }
            else
            {
                return ProtectedDataCollection.PROTECTION_NOT_APPLICABLE;
            }
        }
        catch( Exception e )
        {
            return ProtectedDataCollection.PROTECTION_NOT_APPLICABLE;
        }
    }

    private static DataCollection getTypeSpecificCollection(DataCollection<?> parent, Class<? extends DataElement> clazz, int access)
    {
        parent = (DataCollection<?>)doFetchPrimaryElement(parent, access);
        if(parent instanceof SymbolicLinkDataCollection)
        {
            parent = ( (SymbolicLinkDataCollection)parent ).getPrimaryCollection();
            parent = (DataCollection<?>)doFetchPrimaryElement( parent, access );
        }
        if( parent instanceof GenericDataCollection )
        {
            parent = ( (GenericDataCollection)parent ).getTypeSpecificCollection(clazz);
        }
        if( parent instanceof GenericDataCollection2 && DataCollection.class.isAssignableFrom( clazz ) )
        {
            parent = ( (GenericDataCollection2)parent).getRepositoryWrapper();
        }
        return parent;
    }

    public static DataCollection<?> getTypeSpecificCollection(DataCollection<?> parent, Class<? extends DataElement> clazz)
    {
        return getTypeSpecificCollection(parent, clazz, Permission.WRITE);
    }

    private static Map<Class<? extends DataElement>, String> classTitleCache = new ConcurrentHashMap<>();
    /**
     * Returns human-readable class name
     * @param clazz class to retrieve class name
     * @return
     */
    @SuppressWarnings ( "unchecked" )
    public static String getClassTitle(Class<? extends DataElement> clazz)
    {
        if( clazz == null )
            return ru.biosoft.access.core.DataElement.class.getAnnotation(PropertyName.class).value();
        String title = classTitleCache.get(clazz);
        if( title != null )
            return title;
        Deque<Class<? extends DataElement>> classes = new LinkedList<>();
        classes.add(clazz);
        while( !classes.isEmpty() )
        {
            Class<? extends DataElement> curClass = classes.pop();
            if( curClass == ru.biosoft.access.core.DataElement.class )
                continue;
            PropertyName annotation = curClass.getAnnotation(PropertyName.class);
            if( annotation != null )
            {
                title = annotation.value();
                classTitleCache.put(clazz, title);
                return title;
            }
            if( curClass.getSuperclass() != null && ru.biosoft.access.core.DataElement.class.isAssignableFrom(curClass.getSuperclass()) )
            {
                classes.add((Class<? extends DataElement>)curClass.getSuperclass());
            }
            Stream.of(curClass.getInterfaces()).flatMap( Clazz.of( ru.biosoft.access.core.DataElement.class )::selectClass ).forEach( classes::add );
        }
        title = ru.biosoft.access.core.DataElement.class.getAnnotation(PropertyName.class).value();
        classTitleCache.put(clazz, title);
        return title;
    }

    /**
     * Returns human-readable class name
     * @param clazz object to retrieve human readable class name of
     * @return
     */
    public static String getClassTitle(DataElement element)
    {
        return getClassTitle(getPrimaryElementType(element));
    }

    /**
     * Returns file for new FileDataElement
     * Consider refactoring
     */
    public static File getChildFile(DataCollection<?> collection, String name )
    {
        collection = getTypeSpecificCollection(collection, FileDataElement.class);
        if( collection instanceof TransformedDataCollection )
        {
            collection = ( (TransformedDataCollection<?, ?>)collection ).getPrimaryCollection();
        }
        return ( (FileBasedCollection<?>)collection ).getChildFile( name );
    }

    /**
     * Checks whether file is accepted by specified FileCollection
     * Consider refactoring
     */
    public static boolean isFileAccepted(DataCollection<?> collection, File file)
    {
        collection = getTypeSpecificCollection(collection, FileDataElement.class, Permission.INFO);
        if( collection instanceof TransformedDataCollection )
        {
            collection = ( (TransformedDataCollection<?, ?>)collection ).getPrimaryCollection();
        }
        if( collection instanceof FileBasedCollection )
        {
            return ( (FileBasedCollection<?>)collection ).isFileAccepted(file);
        }
        return collection.isAcceptable( FileDataElement.class );
    }

    /**
     * Returns SqlConnectionHolder connection
     * Consider refactoring
     */
    public static Connection getSqlConnection(@Nonnull ru.biosoft.access.core.DataElement collection) throws BiosoftSQLException
    {
        collection = doFetchPrimaryElement(collection, Permission.READ);
        if(collection instanceof SymbolicLinkDataCollection)
        {
            collection = ((SymbolicLinkDataCollection)collection).getPrimaryCollection();
            collection = doFetchPrimaryElement(collection, Permission.READ);
        }
        if( ! ( collection instanceof SqlConnectionHolder ) )
            throw new IllegalArgumentException("Collection "+collection.getCompletePath()+" must be associated with SQL database");
        return ( (SqlConnectionHolder)collection ).getConnection();
    }

    public static final String PERSISTENT_PROPERTY_PREFIX = "persistent.";
    /**
     * Copy all properties starting with 'persistent.' prefix from origin to collection
     */
    public static void copyPersistentInfo(DataCollection<?> collection, DataCollection<?> origin)
    {
        ReferenceTypeRegistry.copyCollectionReferenceType(collection, origin);

        Properties originProperties = origin.getInfo().getProperties();
        Properties properties = collection.getInfo().getProperties();
        Enumeration<?> e = originProperties.propertyNames();

        if( originProperties.containsKey( SPECIES_PROPERTY ) )
        {
            properties.put( SPECIES_PROPERTY, originProperties.get( SPECIES_PROPERTY ) );
        }
        if( originProperties.containsKey(DataCollectionConfigConstants.NODE_IMAGE) )
        {
            properties.put(DataCollectionConfigConstants.NODE_IMAGE, originProperties.get(DataCollectionConfigConstants.NODE_IMAGE));
        }

        while( e.hasMoreElements() )
        {
            String key = (String)e.nextElement();
            if( key.startsWith(PERSISTENT_PROPERTY_PREFIX) )
            {
                properties.put(key, originProperties.get(key));
            }
        }
    }

    /**
     * Copy all properties starting with 'analysis.' prefix from origin to collection
     * and change path from source to target if any.
     * @todo this method should be moved somewhere to reuse constants from AnalysisParametersFactory
     */
    public static void copyAnalysisParametersInfo(DataCollection<?> source, DataCollection<?> target)
    {
        Properties sourceProperties = source.getInfo().getProperties();
        Properties properties = target.getInfo().getProperties();
        Enumeration<?> e = sourceProperties.propertyNames();
        while( e.hasMoreElements() )
        {
            String key = (String)e.nextElement();
            boolean analysisName = "analysisName".equals( key );
            if( key.startsWith( "analysis." ) || analysisName )
            {
                String value = sourceProperties.getProperty( key );
                if( !analysisName && source.getCompletePath().toString().equals( value ) )
                    value = target.getCompletePath().toString();
                properties.put( key, value );
            }
        }
    }

    /**
     * @param dc ru.biosoft.access.core.DataCollection
     * @param clazz element class
     * @return java.util.Collection<T> view for given ru.biosoft.access.core.DataCollection
     */
    public static @Nonnull <T extends DataElement> Collection<T> asCollection(final DataCollection<T> dc, Class<T> clazz)
    {
        return new AbstractCollection<T>()
        {
            @Override
            public Iterator<T> iterator()
            {
                return dc.iterator();
            }

            @Override
            public int size()
            {
                return dc.getSize();
            }
        };
    }

    public static @Nonnull <T extends DataElement> Collection<T> asCollection(DataElementPath path, Class<T> clazz)
    {
        return asCollection(path.getDataCollection(clazz), clazz);
    }

    /**
     * Tries to create GenericDataCollection parent folders for given path
     * @param path
     */
    public static void createFoldersForPath(DataElementPath path)
    {
        if( path.optParentCollection() == null )
        {
            // Trying to create missing directories
            List<ru.biosoft.access.core.DataElementPath> parentPaths = new ArrayList<>();
            DataElementPath parentPath = path.getParentPath();
            while( !parentPath.exists() && !parentPath.isEmpty() )
            {
                parentPaths.add(0, parentPath);
                parentPath = parentPath.getParentPath();
            }
            if( parentPath.isEmpty() )
            {
                if( parentPaths.isEmpty() )
                    throw new DataElementCreateException( new MissingParameterException( "Path" ), path.getParentPath(),
                            FolderCollection.class);
                throw new DataElementCreateException(new DataElementNotFoundException(parentPaths.get(0)), path.getParentPath(),
                        FolderCollection.class);
            }
            if( ! ( checkPrimaryElementType(parentPath.optDataElement(), FolderCollection.class) ) )
                throw new DataElementCreateException(new DataElementInvalidTypeException(parentPath, FolderCollection.class),
                        path.getParentPath(), FolderCollection.class);
            for( DataElementPath curPath : parentPaths )
            {
                try
                {
                    createSubCollection(curPath);
                }
                catch( Exception e )
                {
                    if( !curPath.exists() )
                        throw new DataElementCreateException(e, curPath, FolderCollection.class);
                }
            }
        }
    }

    /**
     * Checks whether collection in given path has not exceeded its quota (if applicable)
     * @param path path to check
     * @throws SecurityException if quota is exceeded
     */
    public static void checkQuota(DataElementPath path) throws QuotaException
    {
        long quota = Long.MAX_VALUE;
        long diskSize = -1;
        DataElementPath projectPath = path;
        while( !path.isEmpty() )
        {
            DataCollection<? extends DataElement> dc = path.optDataCollection();
            if( dc != null )
            {
                String diskSizeProperty = dc.getInfo().getProperty(DataCollectionConfigConstants.ELEMENT_SIZE_PROPERTY);
                if( diskSizeProperty != null )
                {
                    try
                    {
                        diskSize = Math.max(diskSize, Long.parseLong(diskSizeProperty));
                    }
                    catch( NumberFormatException e )
                    {
                    }
                }
                String quotaProperty = dc.getInfo().getProperty(DataCollectionConfigConstants.DISK_QUOTA_PROPERTY);
                if( quotaProperty != null )
                {
                    try
                    {
                        quota = Math.min(quota, Long.parseLong(quotaProperty));
                        projectPath = path;
                    }
                    catch( NumberFormatException e )
                    {
                    }
                }
            }
            path = path.getParentPath();
        }
        if( diskSize > quota )
        {
            throw new QuotaException(projectPath, quota, diskSize);
        }
    }

    public static String getPropertyStrict(DataCollection<?> dc, String propertyName)
    {
        String property = dc.getInfo().getProperty(propertyName);
        if( property == null )
            throw new DataElementReadException(dc.getCompletePath(), propertyName);
        return property;
    }

    //Moved from RepositoryAccessDeniedException to avoid Permission dependency
    //TODO: find proper place
    public static String permissionToString(Permission permission)
    {
        if( permission == null )
            return "Access";
        if( !permission.isInfoAllowed() )
            return "Info";
        if( !permission.isReadAllowed() )
            return "Read";
        if( !permission.isWriteAllowed() )
            return "Write";
        if( !permission.isDeleteAllowed() )
            return "Delete";
        return "Admin";
    }

}
