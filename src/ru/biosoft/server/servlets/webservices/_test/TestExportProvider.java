package ru.biosoft.server.servlets.webservices._test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

/**
 * @author lan
 *
 */
public class TestExportProvider extends AbstractProviderTest
{
    public void testExportProperties() throws Exception
    {
        VectorDataCollection<DataElement> vdc = new VectorDataCollection<>("test");
        CollectionFactory.registerRoot(vdc);
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(vdc, "table");
        String columnName = "column";
        table.getColumnModel().addColumn(columnName, String.class);
        vdc.put(table);
        TableDataCollection table2 = TableDataCollectionUtils.createTableDataCollection(vdc, "table2");
        String columnName2 = "column2";
        table2.getColumnModel().addColumn(columnName2, String.class);
        vdc.put(table2);
        
        Map<String, String> arguments = new HashMap<>();
        arguments.put("type", "deParams");
        arguments.put("detype", "Element");
        arguments.put("de", "test/table");
        arguments.put("exporter", "Tab-separated text (*.txt)");
        
        checkExport(arguments, columnName);

        arguments.put("de", "test/table2");
        checkExport(arguments, columnName2);
    }

    public void checkExport(Map<String, String> arguments, String... columnNames) throws Exception, JSONException
    {
        JsonObject response = getResponseJSON("export", arguments);
        assertOk(response);
        JsonArray values = response.get("values").asArray();
        boolean found = false;
        for(JsonValue child : values)
        {
            JsonObject value = child.asObject();
            if(value.get("name").asString().equals("columns"))
            {
                assertEquals(new JSONArray(Arrays.asList(columnNames)).toString(), value.get("value").toString());
                
                found = true;
            }
        }
        assertTrue(found);
    }
    
    public void testExportElement() throws Exception
    {
        VectorDataCollection<DataElement> vdc = new VectorDataCollection<>("test");
        CollectionFactory.registerRoot(vdc);

        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(vdc, "table");
        table.getColumnModel().addColumn("column", String.class);
        TableDataCollectionUtils.addRow(table, "1", new Object[] {"one"});
        TableDataCollectionUtils.addRow(table, "2", new Object[] {"two"});
        vdc.put(table);
        
        Map<String, String> arguments = new HashMap<>();
        arguments.put("type", "de");
        arguments.put("detype", "Element");
        arguments.put("de", "test/table");
        arguments.put("exporter", "Tab-separated text (*.txt)");
        
        assertEquals("ID\tcolumn\n1\tone\n2\ttwo", getResponseString("export", arguments).trim());
        
        JSONArray options = new JSONArray();
        JSONObject headers = new JSONObject();
        headers.put("name", "includeHeaders");
        headers.put("value", false);
        options.put(headers);
        arguments.put("parameters", options.toString());
        assertEquals("1\tone\n2\ttwo", getResponseString("export", arguments).trim());
    }
}
