package ru.biosoft.galaxy.filters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;

import org.json.JSONArray;
import org.w3c.dom.Element;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.galaxy.GalaxyFactory;
import ru.biosoft.galaxy.parameters.FileParameter;
import ru.biosoft.galaxy.parameters.MetaParameter;
import ru.biosoft.galaxy.parameters.SelectParameter;

/**
 * @author lan
 *
 */
public class RemoveValueFilter implements Filter
{
    private int column = 1;
    private String ref;
    private String metaRef;
    private String key;
    private String value;
    private boolean multiple;
    private String separator = ",";
    private SelectParameter baseParameter;
    private FileParameter fileParameter;
    private DataElementPath lastPath = DataElementPath.EMPTY_PATH;
    private Object lastMetaParameterValue;

    @Override
    public void init(Element element, SelectParameter parameter)
    {
        if(element.hasAttribute("value"))
            value = element.getAttribute("value");
        if(element.hasAttribute("ref"))
            ref = element.getAttribute("ref");
        if(element.hasAttribute("meta_ref"))
        metaRef = element.getAttribute("meta_ref");
        key = element.getAttribute("key");
        multiple = element.getAttribute("multiple").equalsIgnoreCase("true");
        if(element.hasAttribute("separator"))
            separator = element.getAttribute("separator");
        baseParameter = parameter;
    }

    @Override
    public String[] getDependencies()
    {
        if(ref != null) return new String[] {ref};
        if(metaRef != null) return new String[] {metaRef};
        return null;
    }

    @Override
    public List<String[]> filter(List<String[]> input)
    {
        try
        {
            Object value = this.value;
            if(value == null)
            {
                if(ref != null)
                {
                    value = baseParameter.getContainer().getParameter(ref).toString();
                } else
                {
                    if(fileParameter == null)
                        fileParameter = (FileParameter)baseParameter.getContainer().getParameter(metaRef);
                    if(fileParameter.getDataElementPath().equals(lastPath))
                    {
                        value = lastMetaParameterValue;
                    } else
                    {
                        if(!fileParameter.exportFile()) return input;
                        Map<String, MetaParameter> metadata = GalaxyFactory.getMetadata(fileParameter);
                        MetaParameter metaParameter = metadata.get(key);
                        if(metaParameter == null) return input;
                        value = metaParameter.getValue();
                        lastPath = fileParameter.getDataElementPath();
                        lastMetaParameterValue = value;
                    }
                }
            }
            JSONArray valueArray;
            List<String[]> result = new ArrayList<>();
            if(value instanceof JSONArray) valueArray = (JSONArray)value;
            else
            {
                valueArray = new JSONArray();
                valueArray.put(value);
            }
            for(int i=0; i<input.size(); i++)
            {
                String columnValue = input.get(i)[column];
                boolean add;
                if(multiple)
                {
                    Set<String> columnValues = StreamEx.split(columnValue, separator).toSet();
                    add = IntStreamEx.range( valueArray.length() ).mapToObj( valueArray::optString ).allMatch( columnValues::contains );
                } else
                {
                    add = IntStreamEx.range( valueArray.length() ).mapToObj( valueArray::optString ).has( columnValue );
                }
                if(!add) result.add(input.get(i));
            }
            return result;
        }
        catch( Exception e )
        {
        }
        return input;
    }

    @Override
    public Filter clone(SelectParameter parameter)
    {
        RemoveValueFilter result = new RemoveValueFilter();
        result.column = column;
        result.ref = ref;
        result.metaRef = metaRef;
        result.key = key;
        result.value = value;
        result.multiple = multiple;
        result.separator = separator;
        result.baseParameter = parameter;
        return result;
    }

}
