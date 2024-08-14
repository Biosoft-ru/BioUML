package ru.biosoft.galaxy.validators;

import java.util.Map;

import org.w3c.dom.Element;

import ru.biosoft.galaxy.GalaxyFactory;
import ru.biosoft.galaxy.parameters.FileParameter;
import ru.biosoft.galaxy.parameters.MetaParameter;
import ru.biosoft.galaxy.parameters.Parameter;

public class MetadataValidator extends ValidatorSupport
{
    private String metadataName;

    @Override
    public void init(Element element, Parameter parameter)
    {
        super.init(element, parameter);
        metadataName = element.getAttribute("check");
    }

    @Override
    public void validate() throws IllegalArgumentException
    {
        if( parameter instanceof FileParameter )
        {
            if( ! ( (FileParameter)parameter ).exportFile() )
                return;
            Map<String, MetaParameter> metadata = null;
            try
            {
                metadata = GalaxyFactory.getMetadata((FileParameter)parameter);
            }
            catch( Exception e )
            {
                return;
            }
            MetaParameter metaValue = metadata.get(metadataName);
            if( metaValue == null )
                throw new IllegalArgumentException(message == null ? "Invalid metadata value for " + metadataName : message);
        }
    }

}
