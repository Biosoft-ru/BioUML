package ru.biosoft.galaxy.filters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Element;

import ru.biosoft.galaxy.parameters.SelectParameter;

/**
 * @author lan
 *
 */
public class SourceStaticFilter implements Filter
{
    private List<String[]> options = new ArrayList<>();
    
    @Override
    public void init(Element element, SelectParameter parameter)
    {
    }
    
    public void addOption(String name, String value)
    {
        options.add(new String[] {name, value});
    }

    @Override
    public String[] getDependencies()
    {
        return null;
    }

    @Override
    public List<String[]> filter(List<String[]> input)
    {
        if(input == null || input.isEmpty()) return Collections.unmodifiableList(options);
        List<String[]> result = new ArrayList<>(input);
        result.addAll(options);
        return result;
    }

    @Override
    public Filter clone(SelectParameter parameter)
    {
        // Can omit cloning as this filter has no internal state and no dependence from parameter
        return this;
    }
}
