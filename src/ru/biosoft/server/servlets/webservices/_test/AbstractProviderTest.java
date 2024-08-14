package ru.biosoft.server.servlets.webservices._test;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.server.servlets.webservices.JSONResponse;
import ru.biosoft.server.servlets.webservices.SystemSession;
import ru.biosoft.server.servlets.webservices.WebServicesServlet;
import ru.biosoft.server.servlets.webservices.WebSession;
import ru.biosoft.util.Maps;

import com.developmentontheedge.beans.Preferences;
import com.developmentontheedge.application.Application;
import ru.biosoft.jobcontrol.JobControl;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

/**
 * @author lan
 *
 */
public class AbstractProviderTest extends AbstractBioUMLTest
{
    private SystemSession session;
    private WebServicesServlet servlet;
    private HttpResponseStub lastHttpResponse;

    /**
     * asserts that JSON result doesn't contain error
     * @param response
     */
    protected static void assertOk(JsonObject response)
    {
        assertNotNull("Null response received", response);
        if(JSONResponse.TYPE_OK != response.get(JSONResponse.ATTR_TYPE).asInt())
        {
            fail(response.getString(JSONResponse.ATTR_MESSAGE, "Provider request failed"));
        }
    }
    
    protected static void assertValueEquals(String expected, JsonObject result)
    {
        assertOk(result);
        assertEquals(expected, result.get(JSONResponse.ATTR_VALUES).asString());
    }
    
    protected static void assertErrorCodeEquals(String expected, JsonObject result)
    {
        assertNotNull("Null response received", result);
        assertEquals("Provider didn't return error message as expected", JSONResponse.TYPE_ERROR, result.get(JSONResponse.ATTR_TYPE).asInt());
        assertEquals("Wrong error reported by provider", expected, result.get(JSONResponse.ATTR_ERROR_CODE).asString());
    }
    
    protected void assertLastContentType(String expected) throws Exception
    {
        assertEquals("Wrong content-type returned", expected, lastHttpResponse.getContentType());
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        Application.setPreferences(new Preferences());
        session = new SystemSession();
        WebSession.getSession(session);
        servlet = new WebServicesServlet();
    }

    protected void waitForJob(String jobID) throws Exception
    {
        Map<String, String> arguments = new HashMap<>();
        arguments.put("jobID", jobID);
        while(true)
        {
            Thread.sleep(50);
            JsonObject response = getResponseJSON("jobcontrol", arguments);
            assertOk(response);
            switch(response.get(JSONResponse.ATTR_STATUS).asInt())
            {
                case JobControl.COMPLETED:
                    return;
                case JobControl.TERMINATED_BY_ERROR:
                    String[] messages = response.get( JSONResponse.ATTR_VALUES ).asArray().values().stream().map( JsonValue::asString )
                            .toArray( String[]::new );
                    throw new Exception( "Job exception: " + String.join( "\n", messages ) );
                case JobControl.TERMINATED_BY_REQUEST:
                    throw new Exception("Job was terminated by request");
                default:
                    break;
            }
        }
    }

    protected JsonObject getResponseJSON(String providerName, Map<String, String> arguments) throws Exception
    {
        String response = getResponseString(providerName, arguments);
        assertLastContentType("application/json");
        return JsonObject.readFrom( response );
    }

    protected String getResponseString(String providerName, Map<String, String> arguments) throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Map<String, String[]> params = Maps.transformValues(arguments, val -> new String[] {val});
        lastHttpResponse = new HttpResponseStub();
        servlet.service("biouml/web/"+providerName, session, params, out, lastHttpResponse);
        //provider.process(arguments, out, new HttpResponseWrapperStub());
        String response = out.toString("UTF-8");
        return response;
    }

    protected void setBean(String beanPath, JsonArray json) throws Exception
    {
        Map<String, String> arguments = new HashMap<>();
        arguments.put("de", beanPath);
        arguments.put("json", json.toString());
        assertOk(getResponseJSON("bean/set", arguments));
    }

    protected void setBean(String beanPath, String... parameters) throws Exception
    {
        setBean(beanPath, createParametersJSON(parameters));
    }

    protected JsonArray createParametersJSON(String... parameters)
    {
        JsonArray result = new JsonArray();
        for(int i=0; i<parameters.length; i+=2)
        {
            result.add( new JsonObject().add( "name", parameters[i] ).add( "value", parameters[i + 1] ) );
        }
        return result;
    }
}
