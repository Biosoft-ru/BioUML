package ru.biosoft.galaxy.filters;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Element;

import ru.biosoft.galaxy.GalaxyFactory;
import ru.biosoft.galaxy.parameters.FileParameter;
import ru.biosoft.galaxy.parameters.Parameter;
import ru.biosoft.galaxy.parameters.SelectParameter;

/**
 * @author lan
 *
 */
public class ParamValueFilter extends StaticValueFilter
{
    protected static final Logger log = Logger.getLogger(ParamValueFilter.class.getName());

    private String ref;
    private boolean extMode = false;
    private SelectParameter baseParameter;
    private Parameter parameter;

    @Override
    public void init(Element element, SelectParameter parameter)
    {
        super.init(element, parameter);
        String refAttribute = element.getAttribute("ref_attribute");
        if(refAttribute.equals("extension") || refAttribute.equals("ext")) extMode = true;
        ref = element.getAttribute("ref");
        baseParameter = parameter;
    }

    @Override
    public String[] getDependencies()
    {
        return new String[] {ref};
    }

    @Override
    public List<String[]> filter(List<String[]> input)
    {
        if( parameter == null )
            parameter = baseParameter.getContainer().getParameter(ref);
        if( extMode )
        {
            FileParameter fp = (FileParameter)parameter;
            if( fp.isExtensionSet() )//extension set explicitly only in tests
                value = fp.getExtension();
            else
            // get extension from metadata
            {
                try
                {
                    value = GalaxyFactory.getMetadata(fp).get("extension").toString();
                }
                catch( Exception e )
                {
                    log.log( Level.WARNING, "Can not get extension for " + ref, e );
                    value = fp.getExtension();//use real file extension
                }
            }
        }
        else
        {
            value = parameter.toString();
        }
        return super.filter(input);
    }

    @Override
    public Filter clone(SelectParameter parameter)
    {
        ParamValueFilter result = new ParamValueFilter();
        result.column = column;
        result.keep = keep;
        result.value = value;
        result.ref = ref;
        result.extMode = extMode;
        result.baseParameter = parameter;
        return result;
    }
}
