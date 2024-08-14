package ru.biosoft.server._test;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

import com.developmentontheedge.beans.editors.ColorEditor;
import com.developmentontheedge.beans.editors.StringTagEditor;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.model.Property;

import biouml.model.Diagram;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.Interval;
import ru.biosoft.server.JSONUtils;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.FieldMap;
import ru.biosoft.util.bean.BeanInfoEx2;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import ru.biosoft.workbench.editors.GenericComboBoxEditor;
import junit.framework.TestCase;

/**
 * @author lan
 *
 */
public class TestJSONUtils extends TestCase
{
    public static class TestBean
    {
        private String str;
        private DataElementPath input, output;
        private Color color = Color.BLUE;
        private String select;
        private Interval interval;

        @PropertyName("String")
        @PropertyDescription("Test string property")
        public String getStr()
        {
            return str;
        }
        public void setStr(String str)
        {
            this.str = str;
        }
        public DataElementPath getInput()
        {
            return input;
        }
        public void setInput(DataElementPath input)
        {
            this.input = input;
        }
        public DataElementPath getOutput()
        {
            return output;
        }
        public void setOutput(DataElementPath output)
        {
            this.output = output;
        }

        public Color getColor()
        {
            return color;
        }

        public void setColor(Color color)
        {
            this.color = color;
        }
        public String getSelect()
        {
            return select;
        }
        public void setSelect(String select)
        {
            this.select = select;
        }
        public Interval getInterval()
        {
            return interval;
        }
        public void setInterval(Interval interval)
        {
            this.interval = interval;
        }
    }

    public static class TestSelector extends StringTagEditor
    {

        @Override
        public String[] getTags()
        {
            return new String[] {"one", "two"};
        }
    }

    public static class TestIntervalSelector extends GenericComboBoxEditor
    {
        private static Interval[] values = new Interval[] {
            new Interval(0,100),
            new Interval(100,200),
            new Interval(200,300),
        };

        @Override
        protected Object[] getAvailableValues()
        {
            return values;
        }
    }

    public static class TestBeanBeanInfo extends BeanInfoEx2<TestBean>
    {
        public TestBeanBeanInfo()
        {
            super(TestBean.class);
        }

        @Override
        public void initProperties() throws Exception
        {
            add("str");
            findPropertyDescriptor("str").setExpert(true);
            property( "input" ).inputElement( TableDataCollection.class ).add();
            property( "output" ).outputElement( Diagram.class ).add();
            add("color", ColorEditor.class);
            add("select", TestSelector.class);
            property( "interval" ).simple().editor( TestIntervalSelector.class ).add();
        }
    }

    public void testGetModelAsJSON() throws Exception
    {
        TestBean bean = new TestBean();
        bean.setStr("string value");
        bean.setInput(DataElementPath.create("test/path"));
        bean.setOutput(DataElementPath.create("test/path output"));
        bean.setColor(Color.BLACK);
        bean.setSelect("one");
        bean.setInterval(new Interval(0,100));

        ComponentModel model = ComponentFactory.getModel(bean);
        JSONArray json = JSONUtils.getModelAsJSON(model);
        assertNotNull(json);
        assertEquals(model.getPropertyCount()-1, json.length());

        json = JSONUtils.getModelAsJSON(model, FieldMap.ALL, Property.SHOW_EXPERT);
        assertNotNull(json);
        assertEquals(model.getPropertyCount(), json.length());

        JSONObject property = json.getJSONObject(0);
        assertEquals("str", property.getString("name"));
        assertEquals("String", property.getString("displayName"));
        assertEquals("Test string property", property.getString("description"));
        assertEquals("string value", property.getString("value"));
        assertFalse(property.getBoolean("readOnly"));
        assertEquals("code-string", property.getString("type"));

        property = json.getJSONObject(1);
        assertEquals("input", property.getString("name"));
        assertEquals(TableDataCollection.class.getName(), property.getString("elementClass"));
        assertTrue(property.getBoolean("elementMustExist"));
        assertFalse(property.getBoolean("canBeNull"));
        assertFalse(property.getBoolean("multiSelect"));
        assertEquals("test/path", property.getString("value"));
        assertEquals("data-element-path", property.getString("type"));

        property = json.getJSONObject(2);
        assertEquals("output", property.getString("name"));
        assertEquals(Diagram.class.getName(), property.getString("elementClass"));
        assertFalse(property.getBoolean("elementMustExist"));
        assertFalse(property.getBoolean("canBeNull"));
        assertFalse(property.getBoolean("multiSelect"));
        assertEquals("test/path output", property.getString("value"));
        assertEquals("data-element-path", property.getString("type"));

        property = json.getJSONObject(3);
        assertEquals("color", property.getString("name"));
        // TODO: something wrong with color; need to investigate
        /*assertEquals("color-selector", property.getString("type"));
        assertEquals("[0,0,0]", property.getJSONArray("value").get(0));*/

        property = json.getJSONObject(4);
        assertEquals("select", property.getString("name"));
        assertEquals("code-string", property.getString("type"));
        assertEquals("one", property.getString("value"));
        assertEquals("[[\"one\",\"one\"],[\"two\",\"two\"]]", property.getJSONArray("dictionary").toString());

        property = json.getJSONObject(5);
        assertEquals("interval", property.getString("name"));
        assertEquals("code-string", property.getString("type"));
        assertEquals(new Interval(0,100).toString(), property.getString("value"));
        JSONArray expectedDictionary = new JSONArray();
        expectedDictionary.put(Arrays.asList(new Interval(0,100).toString(),new Interval(0,100).toString()));
        expectedDictionary.put(Arrays.asList(new Interval(100,200).toString(),new Interval(100,200).toString()));
        expectedDictionary.put(Arrays.asList(new Interval(200,300).toString(),new Interval(200,300).toString()));
        assertEquals(expectedDictionary.toString(), property.getJSONArray("dictionary").toString());
        // TODO: test more types
    }

    public static class CompositeObject
    {
        public CompositeObject(int val, String str)
        {
            this.val = val;
            this.str = str;
        }

        private int val;
        private String str;
        public int getVal()
        {
            return val;
        }
        public void setVal(int val)
        {
            this.val = val;
        }
        public String getStr()
        {
            return str;
        }
        public void setStr(String str)
        {
            this.str = str;
        }

        @Override
        public String toString()
        {
            return str + ":" + val;
        }
    }
    public void testJSONArray() throws Exception
    {
        CompositeObject obj1 = new CompositeObject( 1, "str1" );
        CompositeObject obj2 = new CompositeObject( 2, "str2" );
        List<CompositeObject> objects = Arrays.asList( obj1, obj2 );
        JSONArray arrSimpleObj = JSONUtils.toSimpleJSONArray( objects );
        assertEquals( obj1.toString(), arrSimpleObj.getString( 0 ) );
        assertEquals( obj2.toString(), arrSimpleObj.getString( 1 ) );
    }
}
