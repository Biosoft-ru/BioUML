package ru.biosoft.access.security;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.Repository;
import ru.biosoft.access.exception.DataElementExistsException;
import ru.biosoft.access.exception.ParameterNotAcceptableException;
import ru.biosoft.access.generic.GenericDataCollection;
import ru.biosoft.access.history.HistoryFacade;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.MissingParameterException;

/**
 * Data collection with session cache support
 */
public class NetworkDataCollection extends ProtectedDataCollection implements FolderCollection
{
    public static final String CLONE_FOR_EDIT_PROPERTY = "cloneForEdit";

    private final Map<String, Reference<DataElement>> clones = new ConcurrentHashMap<>();

    public NetworkDataCollection(DataCollection<?> parent, Properties properties) throws Exception
    {
        super(parent, properties);
        // Strong reference to this collection must be saved somewhere as long as at least one child exists
        // Children have origin which refers to internal GenericDataCollection (or something),
        // thus this NetworkDataCollection may become not referenced
        doGetPrimaryCollection().getInfo().setTransientValue("PrimaryCollection", this);
    }

    @Override
    public DataElement put(DataElement element)
    {
        SessionCache sessionCache = SessionCacheManager.getSessionCache();
        if(sessionCache != null)
        {
            DataElementPath path = DataElementPath.create(this, element.getName());
            Object cachedElement = sessionCache.getObject(path.toString());
            if(cachedElement == element)
            {
            	if(cachedElement instanceof DataCollection)
            		if( Boolean.parseBoolean( ( (DataCollection<?>)cachedElement ).getInfo().getProperty( CLONE_FOR_EDIT_PROPERTY ) ) )
            			element = cloneElement((DataElement)cachedElement);
                if(element == null) element = (DataElement)cachedElement;
            } else
            {
                Permission permissions = SecurityManager.getPermissions(getCompletePath());
                Object lock = locks.computeIfAbsent( element.getName(), k->new Object() );
                synchronized( lock )
                {
                    sessionCache.removeObject(path.toString());
                    if(permissions.isWriteAllowed()) clones.remove(element.getName());
                }
            }
        }
        return super.put(element);
    }

    /**
     * @param cachedElement
     * @return
     * @throws Exception
     */
    private DataElement cloneElement(DataElement element)
    {
        Class<? extends DataElement> resultClass = element.getClass();
        try
        {
            Method cloneMethod = resultClass.getMethod("clone", new Class[] {ru.biosoft.access.core.DataCollection.class, String.class});
            return (DataElement)cloneMethod.invoke(element, new Object[] {element.getOrigin(), element.getName()});
        }
        catch( NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex1 )
        {
            try
            {
                Method cloneMethod = resultClass.getMethod("clone", new Class[] {});
                return (DataElement)cloneMethod.invoke(element, new Object[] {});
            }
            catch( NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex2 )
            {
            }
        }
        return null;
    }
    
    private DataElement cloneElementOrCopy(DataElement element) throws Exception
    {
        DataElement clone = cloneElement( element );
        if( clone != null )
            return clone;
        release( element.getName() );
        return super.doGet( element.getName() );
    }

    @Override
    public void remove(String name) throws Exception, UnsupportedOperationException
    {
        super.remove(name);
        
        SessionCache sessionCache = SessionCacheManager.getSessionCache();
        Object lock = locks.computeIfAbsent( name, k->new Object() );
        synchronized( lock )
        {
            if(sessionCache != null)
                sessionCache.removeObject(DataElementPath.create(this, name).toString());
            clones.remove(name);
        }
    }

    private Map<String, Object> locks = new ConcurrentHashMap<>();
    @Override
    public DataElement get(String name) throws Exception
    {
        String completeName = DataElementPath.create(this, name).toString();
        
        DataElement result = getFromSession( completeName );
        if(result != null)
            return result;
        
        Object lock = locks.computeIfAbsent( name, k->new Object() );
        synchronized( lock )
        {
            result = getFromSession( completeName );
            if(result != null)
                return result;
            
            result = getSharedCopy(name);
            if(result != null)
            {
                putToSession(completeName, result);
                return result;
            }
            
            result = getFromPrimaryCollection( name );

            if( result != null )
            {
                if( ! ( result instanceof DataCollection )
                        || ! ( Boolean.parseBoolean( ( (DataCollection<?>)result ).getInfo().getProperty( CLONE_FOR_EDIT_PROPERTY ) ) ) )
                {
                    return result;
                }
                
                result = cloneElementOrCopy( result );

                if(result != null)
                {
                    putToSession( completeName, result );
                    putSharedCopy(result);
                }
            }
            return result;
        }
    }
    
    private DataElement getFromSession(String completeName)
    {
        SessionCache sessionCache = SessionCacheManager.getSessionCache();
        DataElement result = null;
        if( sessionCache != null )
        {
            try
            {
                result = (DataElement)sessionCache.getObject(completeName);
            }
            catch( Exception e )
            {
            }
        }
        return result;
    }
    
    private void putToSession(String completeName, DataElement result)
    {
        SessionCache sessionCache = SessionCacheManager.getSessionCache();
        if( sessionCache != null )
            sessionCache.addObject( completeName, result, false );
    }

    private DataElement getSharedCopy(String name)
    {
        Permission permissions = SecurityManager.getPermissions( getCompletePath() );
        if( !permissions.isWriteAllowed() )
            return null;
        Reference<DataElement> reference = clones.get( name );
        return reference == null ? null : reference.get();
    }
    
    private void putSharedCopy(DataElement element)
    {
        Permission permissions = SecurityManager.getPermissions( getCompletePath() );
        if( permissions.isWriteAllowed() )
        {
            clones.put( element.getName(), new WeakReference<>( element ) );
        }
    }
    
    private DataElement getFromPrimaryCollection(String name) throws Exception
    {
        DataElement result = null;
        try
        {
            result = getFromCache( name );
        }
        catch( Exception e )
        {
        }
        if( result == null )
            result = super.get( name );
        return result;
    }
    
    @Override
    public void release(String name)
    {
        Permission permissions = SecurityManager.getPermissions(getCompletePath());
        if( permissions.isWriteAllowed() )
        {
            Object lock = locks.computeIfAbsent( name, k->new Object() );
            synchronized( lock )
            {
                clones.remove( name );
            }
        }
        super.release(name);
    }

    @Override
    public DataCollection createSubCollection(String name, Class<? extends FolderCollection> clazz)
    {
        if( name == null || name.isEmpty() )
            throw new MissingParameterException( "Name" );
        if( contains(name) )
            throw new DataElementExistsException(getCompletePath().getChildPath(name));
        if( !name.trim().equals(name) )
            throw new ParameterNotAcceptableException(
                    new IllegalArgumentException("Name should not start or end with white-space characters."), "Name", name);
        if( !GenericDataCollection.class.isAssignableFrom(clazz) )
            clazz = GenericDataCollection.class;
        DataCollection repository = this;
        while( repository != null && ! ( repository instanceof Repository ) )
        {
            repository = repository.getOrigin();
        }
        if( repository == null )
            throw new IllegalArgumentException("Cannot find repository: failed to create sub-collection in " + getCompletePath());
        try
        {
            DataCollection folder = GenericDataCollection.createGenericCollection(this, (Repository)repository, name, null);//,//
                    //(Class<? extends GenericDataCollection>)clazz);
            String historyCollection = getInfo().getProperty(HistoryFacade.HISTORY_COLLECTION);
            if( historyCollection != null )
                folder.getInfo().getProperties().setProperty(HistoryFacade.HISTORY_COLLECTION, historyCollection);
            put(folder);
            return folder;
        }
        catch( Exception ex )
        {
            throw ExceptionRegistry.translateException(ex);
        }
    }
}
