package ru.biosoft.server.servlets.webservices._test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.developmentontheedge.application.Application;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import biouml.workbench.perspective.PerspectiveRegistry;
import ru.biosoft.server.servlets.webservices.providers.PerspectivesProvider;

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

    public void testDescriptionAndEntryPoints() throws Exception
    {
        JsonObject perspective = getPerspective(Collections.singletonMap("name", "Microbiome"));
        assertTrue(perspective.getString("description", "").contains("SPAdes"));
        JsonObject entry = perspective.get("entryPoints").asArray().get(0).asObject();
        assertEquals("analyses/Workflows/Microbiome/Assembly basic", entry.get("path").asString());
        assertEquals("Basic assembly workflow", entry.get("title").asString());
        assertFalse(perspective.get("importers").asArray().isEmpty());
    }

    public void testSessionScope() throws Exception
    {
        try
        {
            Object preference = Application.getPreferences().getValue(PerspectiveRegistry.PERSPECTIVE_PREFERENCE);
            Map<String, String> arguments = new HashMap<>();
            arguments.put("name", "GTRD");
            arguments.put("scope", PerspectivesProvider.SCOPE_SESSION);
            assertEquals("GTRD", getPerspective(arguments).get("name").asString());
            assertEquals("Preference is not changed", preference,
                    Application.getPreferences().getValue(PerspectiveRegistry.PERSPECTIVE_PREFERENCE));
            assertEquals("Session perspective is current", "GTRD",
                    getPerspective(Collections.emptyMap()).get("name").asString());
        }
        finally
        {
            // selection without scope is stored in the preferences and replaces the session perspective
            assertEquals("Default", getPerspective(Collections.singletonMap("name", "Default")).get("name").asString());
        }
        assertEquals("Default", Application.getPreferences().getStringValue(PerspectiveRegistry.PERSPECTIVE_PREFERENCE, null));
        assertEquals("Default", getPerspective(Collections.emptyMap()).get("name").asString());
    }

    private JsonObject getPerspective(Map<String, String> arguments) throws Exception
    {
        JsonObject response = getResponseJSON("perspective", arguments);
        assertOk(response);
        return response.get("values").asObject().get("perspective").asObject();
    }
}
