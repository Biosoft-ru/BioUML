package ru.biosoft.access.exception;

import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.exception.ExceptionDescriptor;
import ru.biosoft.exception.LoggedException;

/**
 * @author lan
 *
 */
public abstract class BiosoftFileException extends LoggedException
{
    private static final String KEY_FILE = "file";
    private static final String KEY_FILE_NAME = "fileName";

    public BiosoftFileException(ExceptionDescriptor descriptor)
    {
        super(descriptor);
    }

    public BiosoftFileException(Throwable t, ExceptionDescriptor descriptor)
    {
        super(t, descriptor);
    }

    protected void storeFileName(String fname)
    {
        if(SecurityManager.isAdmin())
        {
            properties.put( KEY_FILE, fname );
        } else
        {
            properties.put( KEY_FILE, "on the server" );
            properties.put( KEY_FILE_NAME, fname );
        }
    }
}
