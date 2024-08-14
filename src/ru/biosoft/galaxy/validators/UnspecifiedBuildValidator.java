package ru.biosoft.galaxy.validators;

import java.util.Map;

import ru.biosoft.galaxy.GalaxyFactory;
import ru.biosoft.galaxy.parameters.FileParameter;
import ru.biosoft.galaxy.parameters.MetaParameter;


/**
 * @author lan
 *
 */
public class UnspecifiedBuildValidator extends ValidatorSupport
{
    @Override
    public void validate() throws IllegalArgumentException
    {
        if(parameter instanceof FileParameter)
        {
            if(!((FileParameter)parameter).exportFile()) return;
            Map<String, MetaParameter> metadata = null;
            try
            {
                metadata = GalaxyFactory.getMetadata((FileParameter)parameter);
            }
            catch( Exception e )
            {
                return;
            }
            MetaParameter dbKey = metadata.get("dbkey");
            if(dbKey == null || dbKey.getValue().equals("") || dbKey.getValue().equals("?"))
                throw new IllegalArgumentException(message == null?"Database build is unspecified":message);
        }
    }
}
