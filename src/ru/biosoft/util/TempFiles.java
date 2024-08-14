package ru.biosoft.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nonnull;

/**
 * @author lan
 *
 */
public class TempFiles
{
    private static final TempFileManager manager = TempFileManager.getDefaultManager();
    
    public static @Nonnull File getTempDirectory()
    {
        return manager.getTempDirectory();
    }
    
    protected static @Nonnull File getTempFile(String suffix)
    {
        return manager.getTempFile(suffix);
    }
    
    /**
     * Creates empty temporary file and returns it
     * @param suffix - suffix to add to temporary file
     * @return File
     * @throws IOException
     */
    public static @Nonnull TempFile file(String suffix) throws IOException
    {
        return manager.file(suffix);
    }
    
    /**
     * Generates path to temp file or directory but doesn't create it. However it's guaranteed that path will be unique.
     * @param suffix - suffix to add to temporary file
     * @return File path
     * @throws IOException
     */
    public static @Nonnull File path(String suffix)
    {
        return manager.path(suffix);
    }
    
    /**
     * Creates temporary file with given content and returns it
     * @param suffix - suffix to add to temporary file
     * @param content - file content
     * @return File
     * @throws IOException
     */
    public static @Nonnull TempFile file(String suffix, String content) throws IOException
    {
        return manager.file(suffix, content);
    }

    /**
     * Creates temporary file, fills it from the stream and returns it
     * @param suffix - suffix to add to temporary file
     * @param stream - stream to fill from (note: stream will be closed automatically)
     * @return File
     * @throws IOException
     */
    public static @Nonnull TempFile file(String suffix, InputStream stream) throws IOException
    {
        return manager.file(suffix, stream);
    }

    /**
     * Creates temporary directory and returns it
     * @param suffix - suffix to add to temporary directory
     * @return File
     * @throws IOException
     */
    public static @Nonnull File dir(String suffix) throws IOException
    {
        return manager.dir(suffix);
    }
}
