package ru.biosoft.access.exception;

import java.io.File;

import ru.biosoft.exception.ExceptionDescriptor;

public class BiosoftFileCreateException extends BiosoftFileException
{
    public static final ExceptionDescriptor ED_FILE_CREATE = new ExceptionDescriptor( "FileCreate",
            LoggingLevel.TraceIfNoCause, "Cannot create $file$.");

    public BiosoftFileCreateException(String fileName)
    {
        super(ED_FILE_CREATE);
        storeFileName(fileName);
    }

    public BiosoftFileCreateException(File file)
    {
        this(file.getAbsolutePath());
    }
}
