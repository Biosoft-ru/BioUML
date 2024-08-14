package ru.biosoft.galaxy.filters;

import java.util.ArrayList;
import java.util.List;

import one.util.streamex.EntryStream;

import org.w3c.dom.Element;

import ru.biosoft.galaxy.parameters.SelectParameter;

/**
 * @author lan
 *
 */
public class SourceFromDataTableFilter implements Filter
{
    private DataTable table;
    
    @Override
    public void init(Element element, SelectParameter parameter)
    {
        if(!element.hasAttribute("from_data_table"))
            throw new RuntimeException("Expecting from_data_table attribute");
        String tableName = element.getAttribute("from_data_table");
        table = DataTablesPool.getDataTable(tableName);
        if(table == null)
            throw new RuntimeException("Data table (" + tableName + ") not found");
        List<String> columns = table.getColumns();
        if(columns != null)
        {
            EntryStream.of( columns ).invert().forKeyValue( parameter::addColumnIndex );
        }
    }

    @Override
    public String[] getDependencies()
    {
        return null;
    }

    @Override
    public List<String[]> filter(List<String[]> input)
    {
        if(input == null || input.isEmpty()) return table.getContent();
        List<String[]> result = new ArrayList<>(input);
        result.addAll(table.getContent());
        return result;
    }

    @Override
    public Filter clone(SelectParameter parameter)
    {
        // Can omit cloning as this filter has no internal state and no dependence from parameter
        return this;
    }
}
