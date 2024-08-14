package ru.biosoft.galaxy.filters;

import java.util.List;

import org.w3c.dom.Element;

import ru.biosoft.galaxy.parameters.FileParameter;
import ru.biosoft.galaxy.parameters.SelectParameter;

/**
 * @author lan
 *
 */
public class SourceFromDataSetFilter extends SourceFromFileFilter
{
    private String parameterName;
    private SelectParameter baseParameter;
    private FileParameter fileParameter;
    
    @Override
    public void init(Element element, SelectParameter parameter)
    {
        super.init(element, parameter);
        parameterName = element.getAttribute("from_dataset");
        baseParameter = parameter;
    }

    @Override
    public String[] getDependencies()
    {
        return new String[] {parameterName};
    }

    @Override
    public List<String[]> filter(List<String[]> input)
    {
        if(fileParameter == null)
            fileParameter = (FileParameter)baseParameter.getContainer().getParameter(parameterName);
        if(!fileParameter.exportFile()) return input;
        file = fileParameter.getFile();
        return super.filter(input);
    }

    @Override
    public Filter clone(SelectParameter parameter)
    {
        SourceFromDataSetFilter result = new SourceFromDataSetFilter();
        result.parameterName = parameterName;
        result.baseParameter = parameter;
        result.startsWith = startsWith;
        return result;
    }
}
