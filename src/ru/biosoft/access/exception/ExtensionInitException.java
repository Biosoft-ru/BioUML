package ru.biosoft.access.exception;

import ru.biosoft.exception.ExceptionDescriptor;
import ru.biosoft.exception.LoggedException;

/**
 * @author lan
 *
 */
public class ExtensionInitException extends LoggedException
{
    private static final String KEY_POINT = "point";
    private static final String KEY_PLUGIN = "plugin";
    private static final String KEY_EXTENSION = "extension";
    private static final String KEY_CLASS = "class";

    public static final ExceptionDescriptor ED_INIT_EXTENSION = new ExceptionDescriptor( "Extension", LoggingLevel.Summary,
            "Extension point $point$: cannot initialize $extension$");
    public static final ExceptionDescriptor ED_INIT_EXTENSION_POINT = new ExceptionDescriptor( "ExtensionPoint", LoggingLevel.Summary,
            "Extension point $point$ not found");

    public ExtensionInitException(Throwable t, String point, String extension, String clazz)
    {
        this(t, point, null, extension, clazz);
    }

    public ExtensionInitException(Throwable t, String point, String pluginId, String extension, String clazz)
    {
        super(t, ED_INIT_EXTENSION);
        properties.put( KEY_POINT, point );
        properties.put( KEY_EXTENSION, extension == null ? clazz == null ? "extension" : clazz : extension );
        if(clazz != null)
        {
            properties.put( KEY_CLASS, clazz );
        }
        if(pluginId != null)
        {
            properties.put( KEY_PLUGIN, pluginId );
        }
    }

    public ExtensionInitException(Throwable t, String point, String extension)
    {
        this(t, point, null, extension, (String)null);
    }

    public ExtensionInitException(String point)
    {
        super(ED_INIT_EXTENSION_POINT);
        properties.put( KEY_POINT, point );
    }
}
