package ru.biosoft.galaxy.filters;

import java.io.BufferedReader;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.galaxy.GalaxyDataCollection;
import ru.biosoft.galaxy.parameters.SelectParameter;

/**
 * @author lan
 */
public class SourceFromFileFilter implements Filter
{
    protected File file;
    protected String startsWith;

    @Override
    public void init(Element element, SelectParameter parameter)
    {
        String fileName = element.getAttribute("from_file");
        file = new File(GalaxyDataCollection.getGalaxyDistFiles().getToolDataFolder(), fileName);
        startsWith = element.getAttribute("startswith");
    }

    @Override
    public String[] getDependencies()
    {
        return null;
    }

    @Override
    public List<String[]> filter(List<String[]> input)
    {
        List<String[]> result = new ArrayList<>();
        try(BufferedReader reader = ApplicationUtils.asciiReader( file ))
        {
            String line;
            while( ( line = reader.readLine() ) != null )
            {
                if( line.isEmpty() || line.startsWith("#") )
                    continue;
                if( startsWith != null && !line.startsWith(startsWith) )
                    continue;
                result.add(line.split("\t"));
            }
        }
        catch( Exception e )
        {
            throw new RuntimeException(e);
        }
        return result;
    }

    @Override
    public Filter clone(SelectParameter parameter)
    {
        // Can omit cloning as this filter has no internal state and no dependence from parameter
        return this;
    }
}
