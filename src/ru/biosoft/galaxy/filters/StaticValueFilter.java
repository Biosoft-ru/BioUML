package ru.biosoft.galaxy.filters;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import ru.biosoft.galaxy.parameters.SelectParameter;

/**
 * @author lan
 *
 */
public class StaticValueFilter implements Filter
{
    protected int column;
    protected boolean keep = true;
    protected String value;

    @Override
    public void init(Element element, SelectParameter parameter)
    {
        column = parameter.getColumnIndex(element.getAttribute("column"));
        if(element.hasAttribute("keep"))
            keep = element.getAttribute("keep").equalsIgnoreCase("true");
        value = element.getAttribute("value");
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
        for(String[] inputRow: input)
        {
            if( column >= inputRow.length )
                continue;
            if(!inputRow[column].equals(value) ^ keep)
            {
                result.add(inputRow);
            }
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
