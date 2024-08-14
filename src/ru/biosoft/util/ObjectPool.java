package ru.biosoft.util;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class ObjectPool<T>
{
    Queue<T> freeObjects = new ConcurrentLinkedQueue<>();

    public static class PooledObject<T> implements AutoCloseable
    {
        private final T obj;
        private final ObjectPool<T> pool;

        PooledObject(ObjectPool<T> pool, T obj)
        {
            this.obj = obj;
            this.pool = pool;
        }

        public T get()
        {
            return obj;
        }

        @Override
        public void close() throws Exception
        {
            pool.returnBack(obj);
        }
    }

    public PooledObject<T> get() throws Exception
    {
        T obj = freeObjects.poll();
        if( obj != null )
        {
            return new PooledObject<>(this, obj);
        }
        return new PooledObject<>(this, createObject());
    }

    void returnBack(T obj)
    {
        freeObjects.add(obj);
    }

    public void shutdown()
    {
        for( T obj : freeObjects )
        {
            shutdown(obj);
        }
    }

    protected void shutdown(T obj)
    {
    }

    protected abstract T createObject() throws Exception;
}