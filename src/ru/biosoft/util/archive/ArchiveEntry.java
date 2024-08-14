package ru.biosoft.util.archive;

import java.io.BufferedInputStream;
import java.io.InputStream;

/**
 * Class describing entry of any archive
 * @author lan
 */
public class ArchiveEntry
{
    private String name;
    private boolean directory;
    private long size;
    private BufferedInputStream stream;

    /**
     * @param name
     * @param directory
     * @param size
     */
    public ArchiveEntry(String name, boolean directory, long size, InputStream stream)
    {
        super();
        this.name = name;
        this.directory = directory;
        this.size = size;
        this.stream = new BufferedInputStream(stream);
    }
    
    /**
     * @return the name
     */
    public String getName()
    {
        return name;
    }

    /**
     * @return the directory
     */
    public boolean isDirectory()
    {
        return directory;
    }

    /**
     * @return the size
     */
    public long getSize()
    {
        return size;
    }

    /**
     * @return the stream
     */
    public BufferedInputStream getInputStream()
    {
        return stream;
    }

    @Override
    public String toString()
    {
        return "Archive entry: ["+getName()+"]";
    }
    
    @Override
    public int hashCode()
    {
        return getName().hashCode();
    }
}
