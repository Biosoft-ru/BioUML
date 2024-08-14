package ru.biosoft.galaxy.validators;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.galaxy.parameters.FileParameter;

/**
 * @author lan
 *
 */
public class DatasetOkValidator extends ValidatorSupport
{
    @Override
    public void validate() throws IllegalArgumentException
    {
        if(parameter instanceof FileParameter)
        {
            DataElementPath path = ((FileParameter)parameter).getDataElementPath();
            if(path == null || path.isEmpty() || !path.exists())
            {
                throw new IllegalArgumentException(message == null?"Invalid element selected":message);
            }
        }
    }
}
