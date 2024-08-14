package ru.biosoft.server.servlets.webservices._test;

import java.util.Collections;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

/**
 * @author lan
 *
 */
public class TestWebActionsProvider extends AbstractProviderTest
{
    public void testLoadActions() throws Exception
    {
        JsonObject json = getResponseJSON("action", Collections.singletonMap("type", "toolbar"));
        assertOk(json);
        JsonArray values = json.get("values").asArray();
        assertFalse(values.isEmpty());
        boolean found = false;
        for(int i=0; i<values.size(); i++)
        {
            JsonObject action = values.get(i).asObject();
            if(action.isEmpty()) continue;  // separator
            String message = i+": "+action;
            assertNotNull(message, action.get("id"));
            assertNotNull(message, action.get("visible"));
            assertNotNull(message, action.get("action"));
            assertNotNull(message, action.get("label"));
            if(action.get("id").asString().equals("logout"))
            {
                assertEquals("icons/logout.gif", action.get("icon").asString());
                assertEquals( "Log out", action.get( "label" ).asString() );
                found = true;
            }
        }
        assertTrue(found);
    }

    public void testLoadDynamicActions() throws Exception
    {
        JsonObject json = getResponseJSON("action/load", Collections.singletonMap("type", "dynamic"));
        assertOk(json);
        JsonArray values = json.get("values").asArray();
        assertFalse(values.isEmpty());
        boolean found = false;
        for(int i=0; i<values.size(); i++)
        {
            JsonObject action = values.get(i).asObject();
            String message = i+": "+action;
            assertNotNull(message, action.get("id"));
            assertNotNull(message, action.get("acceptReadOnly"));
            assertNotNull(message, action.get("numSelected"));
            int numSelected = action.get("numSelected").asInt();
            assertNotNull(message, numSelected >= -1 && numSelected <= 3);
            assertNotNull(message, action.get("label"));
            if(action.get("id").asString().equals("Remove selection"))
            {
                assertEquals( "Remove selected", action.get( "label" ).asString() );
                found = true;
            }
        }
        assertTrue(found);
    }
}
