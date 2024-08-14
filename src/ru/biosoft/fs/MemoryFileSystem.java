package ru.biosoft.fs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import one.util.streamex.EntryStream;

import com.developmentontheedge.application.ApplicationUtils;

public class MemoryFileSystem implements FileSystem
{
    private interface DataHolder
    {
        
    }
    private static class FileDataHolder implements DataHolder
    {
        private final byte[] data;
        
        public FileDataHolder(byte[] data)
        {
            this.data = data;
        }
        
        public byte[] getData()
        {
            return data;
        }
    }
    private static class FolderDataHolder implements DataHolder
    {
        Map<String, DataHolder> map = new ConcurrentHashMap<>();
        
        public DataHolder get(String name)
        {
            return map.get( name );
        }
        
        public DataHolder put(String name, DataHolder value)
        {
            return map.put( name, value );
        }
        
        public DataHolder remove(String name)
        {
            return map.remove( name );
        }
        
        public EntryStream<String, DataHolder> stream()
        {
            return EntryStream.of(map);
        }
    }
    private final FolderDataHolder root = new FolderDataHolder();
    
    public MemoryFileSystem(Properties properties)
    {
        // properties is unused
    }

    @Override
    public boolean isValid(@Nonnull String name)
    {
        return true;
    }

    @Override
    public @Nonnull
    FileSystemEntry[] list(FileSystemPath path) throws IOException
    {
        return traverseToFolder( path ).stream()
                .mapValues( dh -> dh instanceof FolderDataHolder )
                .mapKeyValue( FileSystemEntry::new )
                .toArray( FileSystemEntry[]::new );
    }

    private FolderDataHolder traverseToFolder(FileSystemPath path) throws FileNotFoundException
    {
        if(path == null)
            throw new IllegalArgumentException();
        FolderDataHolder current = root;
        for(String component : path.components())
        {
            DataHolder dataHolder = current.get( component );
            if(!(dataHolder instanceof FolderDataHolder))
                throw new FileNotFoundException( path.toString() );
            current = (FolderDataHolder)dataHolder;
        }
        return current;
    }

    @Override
    public void createDirectory(FileSystemPath path) throws IOException
    {
        FolderDataHolder folder = traverseToFolder( path.parent() );
        if(folder.get( path.name() ) != null)
            throw new IOException( "Element already exists: "+path );
        folder.put( path.name(), new FolderDataHolder() );
    }

    @Override
    public void delete(FileSystemPath path) throws FileNotFoundException, IOException
    {
        FolderDataHolder folder = traverseToFolder( path.parent() );
        if(folder.get( path.name() ) == null)
            throw new FileNotFoundException(path.toString());
        folder.remove( path.name() );
    }

    @Override
    public void writeFile(FileSystemPath path, long numBytes, InputStream is) throws IOException
    {
        FolderDataHolder folder = traverseToFolder( path.parent() );
        if(folder.get( path.name() ) instanceof FolderDataHolder)
            throw new IOException( "It's a folder: "+path );
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ApplicationUtils.copyStreamNoClose( baos, is );
        folder.put( path.name(), new FileDataHolder( baos.toByteArray() ) );
    }

    @Override
    public void readFile(FileSystemPath path, OutputStream os) throws IOException
    {
        FolderDataHolder folder = traverseToFolder( path.parent() );
        DataHolder dataHolder = folder.get( path.name() );
        if(!(dataHolder instanceof FileDataHolder))
            throw new FileNotFoundException( path.toString() );
        ByteArrayInputStream bais = new ByteArrayInputStream( ( (FileDataHolder)dataHolder ).getData() );
        ApplicationUtils.copyStreamNoClose( os, bais );
    }

}
