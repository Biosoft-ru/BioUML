package ru.biosoft.access.exception;

import ru.biosoft.exception.ExceptionDescriptor;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.LoggedException;

/**
 * @author lan
 *
 */
public class BiosoftParseException extends LoggedException
{
    private static String KEY_SOURCE = "source";
    private static String KEY_LINE = "line";
    private static String KEY_COLUMN = "column";

    public static final ExceptionDescriptor ED_PARSE_COMMON = new ExceptionDescriptor( "Common", LoggingLevel.Summary,
            "Error parsing $source$");

    public BiosoftParseException(Throwable t, String source, int line, int column)
    {
        this(ExceptionRegistry.translateException( t ), ED_PARSE_COMMON, source, line, column);
    }

    public BiosoftParseException(Throwable t, String source)
    {
        this(ExceptionRegistry.translateException( t ), source, -1, -1);
    }

    protected BiosoftParseException(Throwable t, ExceptionDescriptor descriptor, String source, int line, int column)
    {
        super(ExceptionRegistry.translateException( t ), descriptor);
        properties.put( KEY_SOURCE, source );
        if(line >= 0)
            properties.put( KEY_LINE, line );
        if(column >= 0)
            properties.put( KEY_COLUMN, column );
    }
}
