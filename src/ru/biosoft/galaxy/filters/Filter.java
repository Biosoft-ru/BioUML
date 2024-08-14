package ru.biosoft.galaxy.filters;

import java.util.List;

import org.w3c.dom.Element;

import ru.biosoft.galaxy.parameters.SelectParameter;

/**
 * Represents filter for dynamic options
 * @author lan
 */
public interface Filter
{
    /**
     * Init filter from DOM-element
     * @param element containing filter info
     * @param parameter analysis parameter to which filter is applied
     */
    public void init(Element element, SelectParameter parameter);
    
    /**
     * Returns array of parameter names this filter depends from
     * Empty array or null if this filter is independent
     */
    public String[] getDependencies();

    /**
     * Filters input list
     */
    public List<String[]> filter(List<String[]> input);
    
    /**
     * Clone filter with new parent parameter
     */
    public Filter clone(SelectParameter parameter);
}
