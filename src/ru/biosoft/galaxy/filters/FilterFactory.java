package ru.biosoft.galaxy.filters;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;

import ru.biosoft.galaxy.parameters.SelectParameter;

/**
 * @author lan
 *
 */
public class FilterFactory
{
    private static final Map<String, Class<? extends Filter>> filters = new HashMap<>();
    
    static
    {
        filters.put("unique_value", UniqueValueFilter.class);
        filters.put("sort_by", SortByColumnFilter.class);
        filters.put("static_value", StaticValueFilter.class);
        filters.put("multiple_splitter", MultipleSplitterFilter.class);
        filters.put("param_value", ParamValueFilter.class);
        filters.put("data_meta", DataMetaFilter.class);
        filters.put("remove_value", RemoveValueFilter.class);
    }
    
    public static Filter createFilter(Element element, SelectParameter parameter)
    {
        Filter filter = null;
        if(element.getTagName().equals("options"))
        {
            if(element.hasAttribute("from_file"))
                filter = new SourceFromFileFilter();
            else if(element.hasAttribute("from_dataset"))
                filter = new SourceFromDataSetFilter();
            else if(element.hasAttribute("from_data_table"))
                filter = new SourceFromDataTableFilter();
        } else
        {
            String type = element.getAttribute("type");
            if(filters.containsKey(type))
            {
                try
                {
                    filter = filters.get(type).newInstance();
                }
                catch( Exception e )
                {
                }
            }
        }
        if(filter != null) filter.init(element, parameter);
        return filter;
    }
}
