package ru.biosoft.access.exception;

import com.developmentontheedge.beans.model.Property;

import ru.biosoft.exception.ExceptionDescriptor;
import ru.biosoft.exception.ParameterException;

/**
 * @author lan
 *
 */
public class ParameterInvalidTargetException extends ParameterException
{
    public static final ExceptionDescriptor ED_INVALID_TARGET = new ExceptionDescriptor( "InvalidTarget", LoggingLevel.Summary,
            "$parameter$: Unable to save result as $value$");

    public ParameterInvalidTargetException(Throwable t, Property property)
    {
        super( t, ED_INVALID_TARGET, property.getDisplayName(), property.getValue() );
    }
}
