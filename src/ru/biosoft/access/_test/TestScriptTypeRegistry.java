package ru.biosoft.access._test;

import ru.biosoft.access.script.ScriptTypeRegistry;

/**
 * @author lan
 *
 */
public class TestScriptTypeRegistry extends AbstractBioUMLTest
{
    public void testScriptTypeRegistry()
    {
        assertEquals("JavaScript", ScriptTypeRegistry.getScriptTypes().get("js").getTitle());
        assertNotNull(ScriptTypeRegistry.createScript("js", null, ""));
    }
}
