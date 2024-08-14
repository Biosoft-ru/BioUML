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
public class DataMetaFilter implements Filter
{
    private int column = -1;
    private String ref;
    private String key;
    private boolean multiple;
    private String separator = ",";
    private SelectParameter baseParameter;
    private FileParameter fileParameter;
    private DataElementPath lastPath = DataElementPath.EMPTY_PATH;
    private Object lastMetaParameterValue;

    @Override
    public void init(Element element, SelectParameter parameter)
    {
        if(element.hasAttribute("column"))
            column = parameter.getColumnIndex(element.getAttribute("column"));
        ref = element.getAttribute("ref");
        key = element.getAttribute("key");
        multiple = element.getAttribute("multiple").equalsIgnoreCase("true");
        if(element.hasAttribute("separator"))
            separator = element.getAttribute("separator");
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
        if(fileParameter == null)
            fileParameter = (FileParameter)baseParameter.getContainer().getParameter(ref);
        try
        {
            Object value;
            if(fileParameter.getDataElementPath() != null && fileParameter.getDataElementPath().equals(lastPath))
            {
                value = lastMetaParameterValue;
            } else
            {
                Map<String, MetaParameter> metadata;
                if( fileParameter.getMetadata().containsKey(key) )
                    metadata = fileParameter.getMetadata();
                else
                {
                    if( !fileParameter.exportFile() )
                        return column > -1 ? new ArrayList<>() : input;
                    metadata = GalaxyFactory.getMetadata(fileParameter);
                }
                MetaParameter metaParameter = metadata.get(key);
                if( metaParameter == null )
                    return column > -1 ? new ArrayList<>() : input;
                value = metaParameter.getValue();
                lastPath = fileParameter.getDataElementPath();
                lastMetaParameterValue = value;
            }
            JSONArray valueArray;
            List<String[]> result = new ArrayList<>();
            if(value instanceof JSONArray) valueArray = (JSONArray)value;
            else
            {
                valueArray = new JSONArray();
                valueArray.put(value);
            }
            if(column > -1)
            {
                for(String[] row : input)
                {
                    String columnValue = row[column];
                    boolean add;
                    if(multiple)
                    {
                        Set<String> columnValues = StreamEx.split(columnValue, separator).toSet();
                        add = IntStreamEx.range( valueArray.length() ).mapToObj( valueArray::optString ).allMatch( columnValues::contains );
                    } else
                    {
                        add = IntStreamEx.range( valueArray.length() ).mapToObj( valueArray::optString ).has( columnValue );
                    }
                    if(add) result.add(row);
                }
            } else
            {
                result.addAll(input);
                for(int i=0; i<valueArray.length(); i++)
                {
                    result.add(new String[] {valueArray.getString(i), valueArray.getString(i)});
                }
            }
            return result;
        }
        catch( Exception e )
        {
        }
        return column > -1 ? new ArrayList<>():input;
    }

    @Override
    public Filter clone(SelectParameter parameter)
    {
        DataMetaFilter result = new DataMetaFilter();
        result.column = column;
        result.ref = ref;
        result.key = key;
        result.multiple = multiple;
        result.separator = separator;
        result.baseParameter = parameter;
        return result;
    }
}
