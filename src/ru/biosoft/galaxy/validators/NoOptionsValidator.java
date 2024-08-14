package ru.biosoft.galaxy.validators;

import ru.biosoft.galaxy.parameters.SelectParameter;

/**
 * @author lan
 *
 */
public class NoOptionsValidator extends ValidatorSupport
{
    @Override
    public void validate() throws IllegalArgumentException
    {
        if(parameter.toString().isEmpty())
        {
            if(message != null) throw new IllegalArgumentException(message);
            if(parameter instanceof SelectParameter && ((SelectParameter)parameter).getOptions().isEmpty())
                throw new IllegalArgumentException("No options available for selection");
            else
                throw new IllegalArgumentException("Please select at least one value");
        }
    }
}
