package ru.biosoft.galaxy.filters;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.w3c.dom.Element;

import ru.biosoft.galaxy.parameters.SelectParameter;

/**
 * @author lan
 *
 */
public class MultipleSplitterFilter implements Filter
{
    int column;
    String separator;

    @Override
    public void init(Element element, SelectParameter parameter)
    {
        column = parameter.getColumnIndex(element.getAttribute("column"));
        separator = element.getAttribute("separator");
        if(separator.isEmpty()) separator = ",";
        separator = Pattern.quote(separator);
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
            for( String columnValue : inputRow[column].split(separator, -1) )
            {
                String[] newRow = inputRow.clone();
                newRow[column] = columnValue;
                result.add(newRow);
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
