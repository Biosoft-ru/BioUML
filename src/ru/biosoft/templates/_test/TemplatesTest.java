package ru.biosoft.templates._test;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.velocity.Template;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.ParseException;
import org.apache.velocity.runtime.parser.node.SimpleNode;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.templates.TemplateRegistry;
import ru.biosoft.util.DPSUtils;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

/**
 * @author lan
 *
 */
public class TemplatesTest extends AbstractBioUMLTest
{
    /** Standart JUnit constructor */
    public TemplatesTest(String name)
    {
        super(name);
    }

    /** Make suite if tests. */
    public static Test suite()
    {
        TestSuite suite = new TestSuite(TemplatesTest.class.getName());

        suite.addTest(new TemplatesTest("testTemplates"));
        return suite;
    }
    
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        // Initialize velocity
        TemplateRegistry.getSuitableTemplates(new Object());
    }

    private Template createTemplate(String text, String name) throws ParseException
    {
        RuntimeServices services = RuntimeSingleton.getRuntimeServices();
        SimpleNode node = services.parse(text, name);
        Template template = new Template();
        template.setRuntimeServices(services);
        template.setData(node);
        template.initDocument();
        return template;
    }
    
    private void testTemplate(String expected, Object de, String text, String name) throws Exception
    {
        String result = TemplateRegistry.mergeTemplate(de, createTemplate(text, name)).toString();
        result = result.replaceAll("\\s", "");
        expected = expected.replaceAll("\\s", "");
        assertEquals(name, expected, result);
    }

    public void testTemplates() throws Exception
    {
        VectorDataCollection<DataElement> dc = new VectorDataCollection<>("test");
        testTemplate("test", dc, "$de.Name", "testName");
        testTemplate("hello", dc, "#if($de)\nhello\n#else\nbye\n#end", "testIf");
        testTemplate("<b>Field</b>: val<br>", dc, "#strField(\"val\", \"Field\")", "testStrField");
        testTemplate("<b>Bool</b>: true<br>", dc, "#boolField(true, \"Bool\")", "testBoolField");
        
        DynamicPropertySet dps = new DynamicPropertySetAsMap();
        dps.add(new DynamicProperty("str", String.class, "testString"));
        dps.add(new DynamicProperty("int", Integer.class, 5));
        dps.add(new DynamicProperty("null", String.class, null));
        DynamicProperty property = new DynamicProperty("transient", String.class, "transient");
        DPSUtils.makeTransient(property);
        dps.add(property);
        dps.add(new DynamicProperty("empty", String.class, ""));
        dps.add(new DynamicProperty("array", String[].class, new String[] {"1", "2", "3"}));
        testTemplate("<ul>" +
                "<li><b>array</b>:" +
                "  <ul>" +
                "    <li>1</li>" +
                "    <li>2</li>" +
                "    <li>3</li>" +
                "  </ul></li>" +
                "<li><b>empty</b>:</li>" +
                "<li><b>int</b>:5</li>" +
                "<li><b>str</b>:testString</li>" +
                "</ul>", dps, "#displayAttributes($de)", "testDisplayAttributes");
    }
}
