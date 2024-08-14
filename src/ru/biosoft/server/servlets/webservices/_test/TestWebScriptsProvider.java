package ru.biosoft.server.servlets.webservices._test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.plugins.Plugins;
import ru.biosoft.plugins.javascript.JSElement;

/**
 * @author lan
 *
 */
public class TestWebScriptsProvider extends AbstractProviderTest
{
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        Plugins.getPlugins();
    }

    private JsonObject getScriptResults(String jobID) throws Exception
    {
        waitForJob(jobID);
        JsonObject json = getResponseJSON("web/script/environment", Collections.singletonMap("jobID", jobID));
        assertOk(json);
        return json;
    }

    public void testScriptTypes() throws Exception
    {
        JsonObject json = getResponseJSON("web/script/types", Collections.<String,String>emptyMap());
        assertOk(json);
        JsonObject values = json.get("values").asObject();
        assertEquals( "JavaScript", values.get( "js" ).asString() );
    }

    public void testCreateScript() throws Exception
    {
        VectorDataCollection<DataElement> vdc = new VectorDataCollection<>("test");
        CollectionFactory.registerRoot(vdc);
        Map<String, String> arguments = new HashMap<>();
        arguments.put("de", "test/script.js");
        arguments.put("type", "invalid");
        JsonObject json = getResponseJSON("web/script/create", arguments );
        assertErrorCodeEquals( "CannotCreate", json );
        arguments.put("type", "js");
        json = getResponseJSON("web/script/create", arguments );
        assertOk(json);
        assertTrue(vdc.get("script.js") instanceof JSElement);
    }

    public void testExecuteContextScript() throws Exception
    {
        Map<String, String> arguments = new HashMap<>();
        arguments.put("script", "print(a);var a = 123");
        arguments.put("jobID", "scriptJOB_1");
        arguments.put("type", "js");
        assertOk(getResponseJSON("web/script/runInline", arguments));
        assertEquals("undefined\n", getScriptResults("scriptJOB_1").get("values").asString());
        arguments.put("script", "print(a);");
        arguments.put("jobID", "scriptJOB_2");
        assertOk(getResponseJSON("web/script/runInline", arguments));
        assertEquals("123\n", getScriptResults("scriptJOB_2").get("values").asString());
    }

    public void testExecuteScript() throws Exception
    {
        VectorDataCollection<DataElement> vdc = new VectorDataCollection<>("test");
        CollectionFactory.registerRoot(vdc);
        JSElement script = new JSElement(vdc, "myscript.js", "print('Hello!')");
        vdc.put(script);
        Map<String, String> arguments = new HashMap<>();
        arguments.put("de", "test/myscript.js");
        arguments.put("script", script.getContent());
        arguments.put("jobID", "scriptJOB");
        assertOk(getResponseJSON("web/script/run", arguments));
        assertEquals("Hello!\n", getScriptResults("scriptJOB").get("values").asString());
    }

    public void testJSContext() throws Exception
    {
        JsonObject json = getResponseJSON("web/script/context", Collections.<String, String>emptyMap());
        assertOk(json);
        JsonObject values = json.get("values").asObject();
        JsonValue dataValue = values.get("data");
        assertNotNull(values.toString(), dataValue);
        assertNotNull(values.toString(), dataValue.asObject());
        assertNotNull(values.toString(), dataValue.asObject().get("get").asObject());
    }
}
