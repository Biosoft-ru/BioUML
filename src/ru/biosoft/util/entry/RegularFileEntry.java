package ru.biosoft.util.entry;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import one.util.streamex.StreamEx;

import ru.biosoft.access.core.PluginEntry;

public class RegularFileEntry extends PluginEntry
{
    private final File f;
    
    public RegularFileEntry(File file)
    {
        this.f = file;
    }
    
    @Override
    public PluginEntry child(String name)
    {
        return new RegularFileEntry(new File(f, name));
    }

    @Override
    public PluginEntry[] children() throws IOException
    {
        if(!f.exists())
        {
            throw new FileNotFoundException( toString() );
        }
        File[] files = f.listFiles();
        if(files == null)
        {
            throw new IOException( "Unable to read "+f );
        }
        return StreamEx.of(files).map( RegularFileEntry::new ).toArray( PluginEntry[]::new );
    }

    @Override
    public InputStream getInputStream() throws IOException
    {
        return new FileInputStream( f );
    }

    @Override
    public boolean is(File file)
    {
        return f.equals( file );
    }
    
    @Override
    public boolean isDirectory()
    {
        return f.isDirectory();
    }

    @Override
    public String getName()
    {
        return f.getName();
    }

    @Override
    public String toString()
    {
        return f.getAbsolutePath();
    }

    @Override
    public boolean exists()
    {
        return f.exists();
    }

    @Override
    public File getFile()
    {
        return f;
    }

    @Override
    public PluginEntry getParent()
    {
        return new RegularFileEntry( f.getAbsoluteFile().getParentFile() );
    }

    @Override
    public File extract()
    {
        return f;
    }
}