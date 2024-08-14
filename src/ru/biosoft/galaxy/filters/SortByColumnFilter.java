package ru.biosoft.galaxy.filters;

import java.util.Comparator;
import java.util.List;

import one.util.streamex.StreamEx;

import org.w3c.dom.Element;

import ru.biosoft.galaxy.parameters.SelectParameter;

/**
 * @author lan
 *
 */
public class SortByColumnFilter implements Filter
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
        return StreamEx.of( input ).sorted( Comparator.nullsLast( Comparator.comparing( o -> column < o.length ? o[column] : null ) ) )
                .toList();
    }

    @Override
    public Filter clone(SelectParameter parameter)
    {
        // Can omit cloning as this filter has no internal state and no dependence from parameter
        return this;
    }
}
