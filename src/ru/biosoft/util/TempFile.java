package ru.biosoft.util;

import java.io.File;

/**
 * File implementation which is deleted upon calling close()
 * Useful for try-with-resources blocks
 * @author lan
 */
public class TempFile extends File implements AutoCloseable
{
    TempFile(File dir, String child)
    {
        super(dir, child);
    }

    @Override
    public void close() throws Exception
    {
        delete();
    }
}
