package ru.biosoft.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ru.biosoft.access.FilePool;

/**
 * @author lan
 *
 */
public class ConcurrentFileBuffer extends FileBuffer
{
    private final File file;
    private final int chunkSize;
    private final Map<Integer, Reference<byte[]>> chunks = new ConcurrentHashMap<>();
    
    public ConcurrentFileBuffer(File file, int chunkSize)
    {
        this.file = file;
        this.chunkSize = chunkSize;
    }
    
    public ConcurrentFileBuffer(File file)
    {
        this(file, 65536);
    }

    @Override
    public byte read(int pos) throws IOException
    {
        return getChunk(pos/chunkSize)[pos%chunkSize];
    }

    private byte[] getChunk(int i) throws IOException
    {
        Reference<byte[]> ref = chunks.get(i);
        byte[] chunk = ref == null ? null : ref.get();
        if(chunk == null)
        {
            synchronized(chunks)
            {
                ref = chunks.get(i);
                chunk = ref == null ? null : ref.get();
                if(chunk == null)
                {
                    chunk = readChunk(i);
                    chunks.putIfAbsent( i, new WeakReference<>( chunk ) );
                }
            }
        }
        return chunk;
    }

    private byte[] readChunk(int i) throws IOException
    {
        String filePath = file.getAbsolutePath();
        RandomAccessFile randomFile = FilePool.acquire( filePath );
        try
        {
            randomFile.seek((long)i*chunkSize);
            byte[] result = new byte[chunkSize];
            int offset = 0;
            while( true )
            {
                int read = randomFile.read(result, offset, chunkSize-offset);
                if(read == -1)
                {
                    byte[] shortResult = new byte[offset];
                    System.arraycopy(result, 0, shortResult, 0, offset);
                    return shortResult;
                }
                offset+=read;
                if(offset >= chunkSize)
                    break;
            }
            return result;
        }
        finally
        {
            FilePool.release( filePath, randomFile );
        }
    }

    @Override
    public void write(int pos, byte bt) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException
    {
    }
}
