package ru.biosoft.access.exception;

import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.Property;

import ru.biosoft.exception.ExceptionDescriptor;
import ru.biosoft.exception.ParameterException;

/**
 * @author lan
 *
 */
public class ParameterNotAcceptableException extends ParameterException
{
    public static final ExceptionDescriptor ED_NOT_ACCEPTABLE = new ExceptionDescriptor( "InvalidTarget", LoggingLevel.Summary,
            "$parameter$ is not acceptable: $value$");

    public ParameterNotAcceptableException(Throwable t, Property p)
    {
        super( t, ED_NOT_ACCEPTABLE, p.getDisplayName(), p.getValue() );
    }

    public ParameterNotAcceptableException(Property p)
    {
        this(null, p);
    }

    public ParameterNotAcceptableException(Throwable t, Object bean, String property)
    {
        this( t, ComponentFactory.getModel( bean ).findProperty( property ) );
    }

    public ParameterNotAcceptableException(String name, String value)
    {
        super(null, ED_NOT_ACCEPTABLE, name, value);
    }

    public ParameterNotAcceptableException(Throwable t, String name, String value)
    {
        super(t, ED_NOT_ACCEPTABLE, name, value);
    }
}
