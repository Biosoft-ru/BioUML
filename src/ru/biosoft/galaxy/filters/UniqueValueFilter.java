package ru.biosoft.galaxy.filters;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;

import ru.biosoft.galaxy.parameters.SelectParameter;

/**
 * @author lan
 */
public class UniqueValueFilter implements Filter
{
    int column;
    
    @Override
    public void init(Element element, SelectParameter parameter)
    {
        column = parameter.getColumnIndex(element.getAttribute("column"));
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
        Set<String> values = new HashSet<>();
        for(String[] inputRow: input)
        {
            if(inputRow.length > column && !values.contains(inputRow[column]))
            {
                result.add(inputRow);
                values.add(inputRow[column]);
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
