package ru.biosoft.templates;

import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.TemplateInitException;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.runtime.parser.ParseException;
import org.apache.velocity.runtime.parser.TemplateParseException;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.exception.BiosoftParseException;
import ru.biosoft.exception.ExceptionDescriptor;
import ru.biosoft.exception.ExceptionRegistry;

/**
 * @author lan
 *
 */
public class BiosoftVelocityException extends BiosoftParseException
{
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_ELEMENT = "element";

    public static final ExceptionDescriptor ED_VELOCITY = new ExceptionDescriptor( "Velocity", LoggingLevel.Summary,
            "Error while executing velocity template '$source$' for $element$.");
    public static final ExceptionDescriptor ED_VELOCITY_PARSE = new ExceptionDescriptor( "Velocity", LoggingLevel.Summary,
            "Error parsing velocity template '$source$'.");

    public BiosoftVelocityException(Throwable ex, String source, Object de)
    {
        super(getException(ex), getDescriptor(ex), source, getLine(ex), getColumn(ex));
        if(ex instanceof MethodInvocationException)
        {
            properties.put( KEY_MESSAGE, ex.getMessage() );
        }
        if(de != null)
        {
            if(de instanceof DataElement)
            {
                properties.put( KEY_ELEMENT, DataElementPath.create( (DataElement)de ) );
            } else
                properties.put( KEY_ELEMENT, de );
        }
    }

    private static ExceptionDescriptor getDescriptor(Throwable ex)
    {
        if(ex instanceof ParseException)
            return ED_VELOCITY_PARSE;
        return ED_VELOCITY;
    }

    private static int getColumn(Throwable ex)
    {
        if(ex instanceof MethodInvocationException)
            return ((MethodInvocationException)ex).getColumnNumber();
        if(ex instanceof ParseErrorException)
            return ((ParseErrorException)ex).getColumnNumber();
        if(ex instanceof TemplateInitException)
            return ((TemplateInitException)ex).getColumnNumber();
        if(ex instanceof TemplateParseException)
            return ((TemplateParseException)ex).getColumnNumber();
        return -1;
    }

    private static int getLine(Throwable ex)
    {
        if(ex instanceof MethodInvocationException)
            return ((MethodInvocationException)ex).getLineNumber();
        if(ex instanceof ParseErrorException)
            return ((ParseErrorException)ex).getLineNumber();
        if(ex instanceof TemplateInitException)
            return ((TemplateInitException)ex).getLineNumber();
        if(ex instanceof TemplateParseException)
            return ((TemplateParseException)ex).getLineNumber();
        return -1;
    }

    private static Throwable getException(Throwable ex)
    {
        if(ex instanceof MethodInvocationException)
            return ExceptionRegistry.translateException(((MethodInvocationException)ex).getWrappedThrowable());
        if(ex instanceof VelocityException || ex instanceof ParseException)
            return ex;
        return ExceptionRegistry.translateException(ex);
    }
}
