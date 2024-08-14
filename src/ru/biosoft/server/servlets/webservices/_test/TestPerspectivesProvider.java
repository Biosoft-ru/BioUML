package ru.biosoft.server.servlets.webservices._test;

import java.util.Collections;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

/**
 * @author lan
 *
 */
public class TestPerspectivesProvider extends AbstractProviderTest
{
    public void testPerspectives() throws Exception
    {
        JsonObject response = getResponseJSON("perspective", Collections.singletonMap("name", "Default"));
        assertOk(response);
        JsonObject perspective = response.get("values").asObject().get("perspective").asObject();
        JsonArray repository = perspective.get("repository").asArray();
        assertEquals("databases", repository.get(0).asObject().get("path").asString());
        assertEquals(100, perspective.get("priority").asInt());
        assertEquals("Default", perspective.get("name").asString());
        JsonArray names = response.get("values").asObject().get("names").asArray();
        assertFalse(names.isEmpty());
        assertEquals("Default", names.get(0).asString());
    }
}
