package ru.biosoft.plugins.javascript._test;

import java.util.Properties;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.plugins.javascript.FunctionInfo;
import ru.biosoft.plugins.javascript.HostObjectInfo;
import ru.biosoft.plugins.javascript.JScriptVisiblePlugin;
import ru.biosoft.util.ExProperties;

/**
 * @author lan
 *
 */
public class TestJSVisiblePlugin extends AbstractBioUMLTest
{
    public void testJSVisiblePlugin() throws Exception
    {
        Properties properties = new ExProperties();
        properties.put(DataCollectionConfigConstants.NAME_PROPERTY, "JS");
        JScriptVisiblePlugin plugin = new JScriptVisiblePlugin(null, properties);
        plugin.startup();
        assertEquals(2, plugin.getSize());
        assertTrue(plugin.contains("Functions"));
        assertTrue(plugin.contains("Host objects"));
        DataCollection fn = plugin.get("Functions");
        assertTrue(fn.getSize()>0);
        assertTrue(fn.contains("plot"));
        FunctionInfo fi = fn.get("plot").cast( FunctionInfo.class );
        assertEquals("void <b>plot</b>(Double[] x, Double[] y...)\n"
                + "<br>void <b>plot</b>(TableDataCollection table, String x, String y...)\n"
                + "<br>void <b>plot</b>(String xTitle, String xTitle, Double[] x, Object y...)\n" 
                + "<br>", fi.getFunctionDeclaration().trim());
        assertNull(fi.getExceptions());
        assertTrue(fi.isExceptionsHidden());
        assertFalse(fi.isExamplesHidden());
        assertTrue(fi.getExamples().length >= 3);
        
        DataCollection obj = plugin.get("Host objects");
        assertTrue(obj.getSize()>0);
        assertTrue(obj.contains("data"));
        HostObjectInfo hi = obj.get("data").cast( HostObjectInfo.class );
        assertTrue(hi.getSize()>0);
        assertTrue(hi.contains("get"));
        fi = hi.get("get").cast( FunctionInfo.class );
        assertEquals( "returns DataElement", fi.getDescription() );
        assertEquals("ru.biosoft.access.core.DataElement <b>get</b>(String path[, String className])\n<br>", fi.getFunctionDeclaration());
    }
}
