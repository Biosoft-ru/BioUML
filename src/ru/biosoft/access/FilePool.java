package ru.biosoft.access;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class FilePool
{
    private static FilePool instance = new FilePool( 100 );

    public static RandomAccessFile acquire(String path) throws IOException
    {
        return instance.doAcquire( path );
    }

    public static void release(String path, RandomAccessFile file) throws IOException
    {
        instance.doRelease( path, file );
    }
    
    public static void close(String path) throws IOException
    {
        instance.doClose( path );
    }

    private int cacheSize;
    private Map<String, RandomAccessFile> cache;

    @SuppressWarnings ( "serial" )
    public FilePool(int cacheSize)
    {
        this.cacheSize = cacheSize;
        cache = Collections.synchronizedMap( new LinkedHashMap<String, RandomAccessFile>( cacheSize, 0.75f, true )
        {
            @Override
            protected boolean removeEldestEntry(java.util.Map.Entry<String, RandomAccessFile> eldest)
            {
                if( size() > FilePool.this.cacheSize )
                {
                    try
                    {
                        eldest.getValue().close();
                    }
                    catch( IOException e )
                    {
                        throw new RuntimeIOException( e );
                    }
                    return true;
                }
                return false;
            };
        } );
    }

    public RandomAccessFile doAcquire(String path) throws IOException
    {
        RandomAccessFile result = cache.remove( path );
        if( result != null )
            return result;
        File file = new File( path );
        return new RandomAccessFile( path, ( file.canWrite() || !file.exists() ) ? "rw" : "r" );
    }

    public void doRelease(String path, RandomAccessFile file) throws IOException
    {
        try
        {
            RandomAccessFile old = cache.put( path, file );
            if( old != null )
                old.close();
        }
        catch( RuntimeIOException e )
        {
            throw e.getCause();
        }
    }

    public void doClose(String path) throws IOException
    {
        RandomAccessFile file = cache.remove( path );
        if(file != null)
            file.close();
    }
    
    @SuppressWarnings ( "serial" )
    private static class RuntimeIOException extends RuntimeException
    {
        public RuntimeIOException(IOException e)
        {
            super( e );
        }

        @Override
        public synchronized IOException getCause()
        {
            return (IOException)super.getCause();
        }
    }
}
