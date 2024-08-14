package ru.biosoft.galaxy.validators;

import java.io.BufferedReader;
import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Element;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.galaxy.GalaxyDataCollection;
import ru.biosoft.galaxy.GalaxyFactory;
import ru.biosoft.galaxy.parameters.FileParameter;
import ru.biosoft.galaxy.parameters.MetaParameter;
import ru.biosoft.galaxy.parameters.Parameter;

/**
 * @author lan
 *
 */
public class MetadataInFileColumnValidator extends ValidatorSupport
{
    private String metadataName;
    private Set<String> validValues = new HashSet<>();
    
    @Override
    public void init(Element element, Parameter parameter)
    {
        super.init(element, parameter);
        metadataName = element.getAttribute("metadata_name");

        String fileName = element.getAttribute("filename");
        String startsWith = element.getAttribute("line_startswith");
        int metadataColumn = 0;
        try
        {
            metadataColumn = Integer.parseInt(element.getAttribute("metadata_column"));
        }
        catch(Exception e)
        {
        }
        File file = new File(GalaxyDataCollection.getGalaxyDistFiles().getToolDataFolder(), fileName);
        try(BufferedReader reader = ApplicationUtils.asciiReader( file ))
        {
            String line;
            while( ( line = reader.readLine() ) != null )
            {
                if( !startsWith.isEmpty() && !line.startsWith(startsWith) )
                    continue;
                String[] fields = line.split("\t");
                if(fields.length > metadataColumn)
                    validValues.add(fields[metadataColumn]);
            }
        }
        catch( Exception e )
        {
        }
    }

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
            MetaParameter metaValue = metadata.get(metadataName);
            if(metaValue == null || !validValues.contains(metaValue.getValue()))
                throw new IllegalArgumentException(message == null?"Invalid metadata value "+(metaValue == null?"null":metaValue.getValue()):(metaValue == null?"null":metaValue.getValue())+": "+message);
        }
    }
}
