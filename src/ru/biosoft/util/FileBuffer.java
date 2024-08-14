
package ru.biosoft.util;

import java.io.IOException;

/**
 * 
 * RandomAccessBuffer provides buffered reading/writing of random access file
 * Class provides fast access for extracting of sequential data.
 * 
 */
public abstract class FileBuffer
{
    /**
     * Reads one byte from requested position.
     * 
     * @param pos    position of requested byte
     * @return requested byte
     * @exception IOException    If any I/O errors
     */
    abstract public byte read(int pos)  throws IOException ;
    /**
     * Writes one byte to the requested position
     * in the buffer.
     * 
     * @param pos    position of requested byte
     * @param bt     stored byte
     * @exception IOException If any I/O errors
     */
    abstract public void  write(int pos,byte bt)  throws IOException;

    /**
     * Closes the buffer.
     *
     * @exception  IOException  if an I/O error occurs.
     */
    abstract public void close() throws IOException;
}
