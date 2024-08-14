package ru.biosoft.fs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.Nonnull;

/**
 * Base interface for different file systems implementations
 * @author lan
 */
public interface FileSystem
{
    /**
     * @param name
     * @return true if supplied string is a valid file/directory name on the given filesystem
     */
    public boolean isValid(@Nonnull String name);
    
    /**
     * @param path path to the directory
     * @return list of entries contained in the directory specified by the given path
     * @throws IOException if path is not found or it's not a directory or something bad happens
     */
    public @Nonnull FileSystemEntry[] list(FileSystemPath path) throws IOException;
    
    /**
     * Creates a directory specified by the given path
     * @param path path to the new directory. Must not exist, but the parent path must exist.
     * @throws IOException
     */
    public void createDirectory(FileSystemPath path) throws IOException;
    
    /**
     * Deletes an object specified by the given path. If the path points to the directory, it will be deleted recursively.
     * @param path path to the object to delete.
     * @throws FileNotFoundException if path doesn't exist
     * @throws IOException if deletion fails. Warning: deletion may fail partially (e.g. only some of the files in the subdirectories are deleted)
     */
    public void delete(FileSystemPath path) throws FileNotFoundException, IOException;
    
    /**
     * Writes the content of given {@link InputStream} to the file specified by the given path.
     * If the path points to the existing file, it will be overwritten. 
     * @param path path to the file to write
     * @param numBytes the size of the stream to load
     * @param is InputStream to read data from
     * @throws IOException if path is a directory or parent path doesn't exist or something bad happens
     */
    public void writeFile(FileSystemPath path, long numBytes, InputStream is) throws IOException;
    
    /**
     * Reads the content of the file specified by the given path and writes it into given {@link OutputStream}
     * @param path path to the file to read
     * @param os OutputStream to write data into
     * @throws IOException if path doesn't exists or is a directory or something bad happens
     */
    public void readFile(FileSystemPath path, OutputStream os) throws IOException;
}
