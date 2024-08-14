package ru.biosoft.access.exception;

import java.io.File;
import java.io.FileNotFoundException;

import ru.biosoft.exception.ExceptionDescriptor;

/**
 * @author lan
 *
 */
public class BiosoftFileNotFoundException extends BiosoftFileException
{
    public static final ExceptionDescriptor ED_FILE_NOT_FOUND = new ExceptionDescriptor( "FileNotFound",
            LoggingLevel.TraceIfNoCause, "Cannot find the requested file $file$.");

    public BiosoftFileNotFoundException(FileNotFoundException ex)
    {
        super(ex, ED_FILE_NOT_FOUND);
        String fname = ex.getMessage();
        int pos = fname.lastIndexOf(" (");
        if(pos > 0)
            fname = fname.substring(0, pos);
        storeFileName(fname);
    }

    public BiosoftFileNotFoundException(String fileName)
    {
        super(ED_FILE_NOT_FOUND);
        storeFileName(fileName);
    }

    public BiosoftFileNotFoundException(File file)
    {
        this(file.getAbsolutePath());
    }
}
