package ru.biosoft.galaxy.validators;

/**
 * @author lan
 *
 */
public class EmptyTextfieldValidator extends ValidatorSupport
{
    @Override
    public void validate() throws IllegalArgumentException
    {
        if(parameter.toString().isEmpty())
            throw new IllegalArgumentException(message == null?"Field requires a value":message);
    }
}
