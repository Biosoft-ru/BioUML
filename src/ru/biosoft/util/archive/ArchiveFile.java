package ru.biosoft.util.archive;

import java.io.IOException;

/**
 * Interface to read an archive
 * @author lan
 */
public interface ArchiveFile
{
    /**
     * @return true if object in the valid state
     */
    public boolean isValid();
    
    /**
     * Returns next entry or null if no more entries in the archive
     * Calling this method implies that previously returned entries cannot be read anymore
     * @throws IOException
     */
    public ArchiveEntry getNextEntry() throws IOException;

    /**
     * @return current offset in the input stream
     */
    public long offset();
    
    /**
     * Close file and release all associated resources
     */
    public void close();
}
