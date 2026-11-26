package ru.biosoft.access.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import ru.biosoft.util.HashMapWeakValues;

/**
 * Cache for saving copies of objects for session
 */
public class SessionCache
{
    private final Map<ConsoleKey<?>, ConsoleState<?>> consoles = new ConcurrentHashMap<>();

    /** Access to private console state requires the key retained by its Java caller. */
    public static final class ConsoleKey<T>
    {
        @SuppressWarnings( "unchecked" )
        public ConsoleState<T> forSession(SessionCache cache)
        {
            return (ConsoleState<T>)cache.consoles.computeIfAbsent( this, key -> new ConsoleState<>() );
        }
    }

    public static final class ConsoleState<T>
    {
        private final ReentrantLock execution = new ReentrantLock();
        private final AtomicReference<Snapshot<T>> administrator = new AtomicReference<>( new Snapshot<>( null ) );

        public ReentrantLock executionLock()
        {
            return execution;
        }

        public Snapshot<T> administrator()
        {
            return administrator.get();
        }

        public boolean publish(Snapshot<T> expected, T scope)
        {
            return administrator.compareAndSet( expected, new Snapshot<>( scope ) );
        }

        private void publishInvalidation()
        {
            administrator.set( new Snapshot<>( null ) );
        }

        public record Snapshot<T>(T scope)
        {
        }
    }

    void invalidateAdministratorConsoles()
    {
        consoles.values().forEach( console -> console.publishInvalidation() );
    }

    /**
     * Map for asked(not changed) objects.
     */
    protected Map<String, Object> readObjects = new HashMapWeakValues();
    /**
     * Map for changed objects
     */
    protected Map<String, Object> changedObjects = new ConcurrentHashMap<>();

    /**
     * Add object to cache
     */
    public void addObject(String key, Object object, boolean isChanged)
    {
        if( isChanged )
        {
            changedObjects.put(key, object);
        }
        else
        {
            readObjects.put(key, object);
        }
    }

    /**
     * Move object to changed objects map
     */
    public void setObjectChanged(String key, Object object)
    {
        readObjects.remove(key);
        changedObjects.put(key, object);
    }

    /**
     * Get object from cache. Returns null if object not found in cache.
     */
    public Object getObject(String key)
    {
        Object result = changedObjects.get(key);
        if( result == null )
        {
            result = readObjects.get(key);
        }
        return result;
    }

    /**
     * Remove object from cache.
     */
    public void removeObject(String key)
    {
        changedObjects.remove(key);
        readObjects.remove(key);
    }
}
