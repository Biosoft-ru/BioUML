package ru.biosoft.util._test;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListResourceBundle;

import junit.framework.TestCase;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.util.BeanUtil;
import ru.biosoft.util.PropertyInfo;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;
import com.developmentontheedge.beans.annot.PropertyName;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentFactory.Policy;

public class BeanUtilTest extends TestCase
{
    public BeanUtilTest(String name)
    {
        super(name);
    }

    public static class MessageBundle extends ListResourceBundle
    {
        @Override
        protected Object[][] getContents()
        {
            return new Object[][] {};
        }
    }

    public static class TestNestedObject
    {
        boolean boolProperty;
        String stringProperty;
        /**
         * @return the boolProperty
         */
        public boolean isBoolProperty()
        {
            return boolProperty;
        }
        /**
         * @param boolProperty the boolProperty to set
         */
        public void setBoolProperty(boolean boolProperty)
        {
            this.boolProperty = boolProperty;
        }
        /**
         * @return the stringProperty
         */
        public String getStringProperty()
        {
            return stringProperty;
        }
        /**
         * @param stringProperty the stringProperty to set
         */
        public void setStringProperty(String stringProperty)
        {
            this.stringProperty = stringProperty;
        }
    }

    public static class TestNestedObjectBeanInfo extends BeanInfoEx
    {
        public TestNestedObjectBeanInfo()
        {
            super( TestNestedObject.class, MessageBundle.class.getName());
        }

        @Override
        protected void initProperties() throws Exception
        {
            add(new PropertyDescriptor("boolProperty", beanClass));
            add(new PropertyDescriptor("stringProperty", beanClass));
        }
    }

    public static class TestObject
    {
        int intProperty;
        TestNestedObject nested1 = new TestNestedObject();
        TestNestedObject nested2 = new TestNestedObject();
        /**
         * @return the intProperty
         */
        @PropertyName("intProperty")
        public int getIntProperty()
        {
            return intProperty;
        }
        /**
         * @param intProperty the intProperty to set
         */
        public void setIntProperty(int intProperty)
        {
            this.intProperty = intProperty;
        }
        /**
         * @return the nested1
         */
        @PropertyName("nested1")
        public TestNestedObject getNested1()
        {
            return nested1;
        }
        /**
         * @param nested1 the nested1 to set
         */
        public void setNested1(TestNestedObject nested1)
        {
            this.nested1 = nested1;
        }
        /**
         * @return the nested2
         */
        @PropertyName("nested2")
        public TestNestedObject getNested2()
        {
            return nested2;
        }
        /**
         * @param nested2 the nested2 to set
         */
        public void setNested2(TestNestedObject nested2)
        {
            this.nested2 = nested2;
        }
    }

    public static class TestObjectBeanInfo extends BeanInfoEx
    {
        public TestObjectBeanInfo()
        {
            super( TestObject.class, MessageBundle.class.getName());
        }

        @Override
        protected void initProperties() throws Exception
        {
            add(new PropertyDescriptor("intProperty", beanClass));
            add(new PropertyDescriptor("nested1", beanClass));
            add(new PropertyDescriptor("nested2", beanClass));
        }
    }

    public void testBeanPropertyAccessor() throws Exception
    {
        TestObject testObject = new TestObject();
        testObject.setIntProperty( 2 );
        TestNestedObject testNestedObject = new TestNestedObject();
        testNestedObject.setBoolProperty( true );
        testNestedObject.setStringProperty( "qq" );
        testObject.setNested1( testNestedObject );
        assertEquals(2, BeanUtil.getBeanPropertyValue( testObject, "intProperty" ));
        assertEquals(int.class, BeanUtil.getBeanPropertyType( testObject, "intProperty" ));
        assertEquals("qq", BeanUtil.getBeanPropertyValue( testObject, "nested1/stringProperty" ));
        BeanUtil.setBeanPropertyValue( testObject, "nested1/stringProperty", "ww" );
        BeanUtil.setBeanPropertyValue( testObject, "nested2", BeanUtil.getBeanPropertyValue( testObject, "nested1" ) );
        assertEquals("ww", BeanUtil.getBeanPropertyValue( testObject, "nested2/stringProperty" ));
        DynamicPropertySet dps = new DynamicPropertySetAsMap();
        dps.add( new DynamicProperty( "paths", DataElementPath[].class, new ru.biosoft.access.core.DataElementPath[] {DataElementPath.create( "a/b" ),
                DataElementPath.create( "c/d" )} ) );
        assertEquals("d", BeanUtil.getBeanPropertyValue( dps, "paths/[1]/name" ));
        BeanUtil.setBeanPropertyValue( dps, "paths/[0]", DataElementPath.create("e/f") );
        assertEquals("f", BeanUtil.getBeanPropertyValue( dps, "paths/[0]/name" ));
    }

    /**
     * @throws Exception
     */
    public void testSortProperties() throws Exception
    {
        ArrayList<String> list1 = new ArrayList<>(Arrays.asList("nested1/boolProperty", "intProperty", "nested1"));
        ArrayList<String> list1expected = new ArrayList<>(Arrays.asList("intProperty", "nested1", "nested1/boolProperty"));
        BeanUtil.sortProperties( ComponentFactory.getModel( TestObject.class, Policy.DEFAULT ), list1 );
        assertEquals("list1", list1expected, list1);

        ArrayList<String> list2 = new ArrayList<>(Arrays.asList("nested1/boolProperty", "intProperty", "nested2/stringProperty", "nested1/stringProperty", "nested2/boolProperty"));
        ArrayList<String> list2expected = new ArrayList<>(Arrays.asList("intProperty", "nested1/boolProperty", "nested1/stringProperty", "nested2/boolProperty", "nested2/stringProperty"));
        BeanUtil.sortProperties( ComponentFactory.getModel( TestObject.class, Policy.DEFAULT ), list2 );
        assertEquals("list2", list2expected, list2);
    }

    /**
     * @throws Exception
     */
    public void testCopyBean() throws Exception
    {
        TestObject test1 = new TestObject();
        test1.setIntProperty(5);
        test1.getNested1().setBoolProperty(true);
        test1.getNested1().setStringProperty("qq");
        test1.getNested2().setBoolProperty(false);
        test1.getNested2().setStringProperty("abyr");

        TestObject test2 = new TestObject();
        BeanUtil.copyBean(test1, test2);
        assertEquals("intProperty", 5, test2.getIntProperty());
        assertEquals("nested1/boolProperty", true, test2.getNested1().isBoolProperty());
        assertEquals("nested1/stringProperty", "qq", test2.getNested1().getStringProperty());
        assertEquals("nested2/boolProperty", false, test2.getNested2().isBoolProperty());
        assertEquals("nested2/stringProperty", "abyr", test2.getNested2().getStringProperty());
    }

    public void testPropertiesList() throws Exception
    {
        List<String> properties = new ArrayList<>(Arrays.asList(BeanUtil.getPropertiesList(new TestObject())));
        List<String> expected = new ArrayList<>(Arrays.asList("intProperty", "nested1", "nested2"));
        assertEquals(expected, properties);
        PropertyInfo[] propertyInfos = BeanUtil.getRecursivePropertiesList(new TestObject());
        assertEquals(7, propertyInfos.length);
        assertEquals("intProperty", propertyInfos[0].getName());
        assertEquals("nested1", propertyInfos[1].getName());
        assertEquals("nested1/boolProperty", propertyInfos[2].getName());
        assertEquals("nested1/stringProperty", propertyInfos[3].getName());
        assertEquals("nested2", propertyInfos[4].getName());
        assertEquals("nested2/boolProperty", propertyInfos[5].getName());
        assertEquals("nested2/stringProperty", propertyInfos[6].getName());
    }

    public void testBeanSortingValue() throws Exception
    {
        TestObject test1 = new TestObject();
        assertEquals("-----", BeanUtil.getBeanSortingValue(test1, "unknown"));
        assertEquals("00000", BeanUtil.getBeanSortingValue(test1, "intProperty"));
        assertEquals("00002/00001", BeanUtil.getBeanSortingValue(test1, "nested2/stringProperty"));
    }
}
