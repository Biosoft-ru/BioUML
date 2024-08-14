package ru.biosoft.util._test;

import java.util.Properties;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import com.developmentontheedge.beans.DPSProperties;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

public class DPSPropertiesTest extends TestCase
{
    public DPSPropertiesTest(String name)
    {
        super(name);
    }
    
    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(DPSPropertiesTest.class.getName());
        suite.addTest(new DPSPropertiesTest("test"));
        return suite;
    }

    /**
     * @throws Exception
     */
    public void test() throws Exception
    {
        DynamicPropertySet dps = new DynamicPropertySetAsMap();
        dps.add(new DynamicProperty("abyr", String.class, "ryba"));
        Properties properties = new DPSProperties(dps);
        assertTrue("Contains key from DPS", properties.containsKey("abyr"));
        assertFalse("Doesn't contain random key", properties.containsKey("foo"));
        assertFalse("Doesn't contain random key", properties.containsKey("ryba"));
        assertTrue("Contains value from DPS", properties.containsValue("ryba"));
        assertFalse("Dones't contain random value", properties.containsValue("abyr"));
        assertEquals(properties.get("abyr"), "ryba");
        assertEquals(properties.getProperty("abyr"), "ryba");
        assertEquals(properties.keySet().size(), 1);
        properties.setProperty("foo", "bar");
        assertTrue("Contains newly added key", properties.containsKey("foo"));
        assertEquals(properties.size(), 2);
        assertEquals(dps.size(), 2);
        assertEquals(properties.keySet().size(), 2);
        assertEquals(dps.getProperty("foo").getType(), String.class);
        assertEquals(dps.getProperty("foo").getValue(), "bar");
        dps.remove("abyr");
        assertEquals(properties.size(), 1);
        assertEquals(dps.size(), 1);
        assertEquals(dps.getProperty("foo").getType(), String.class);
        assertTrue("Contains added key", properties.containsKey("foo"));
        assertFalse("Doesn't contain removed key", properties.containsKey("abyr"));
        assertEquals(properties.keySet().size(), 1);
    }
}
