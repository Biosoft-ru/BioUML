package ru.biosoft.util._test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import ru.biosoft.util.BeanAsMapUtil;
import ru.biosoft.util._test.BeanUtilTest.TestNestedObject;
import ru.biosoft.util._test.BeanUtilTest.TestObject;

public class TestBeanAsMapUtil extends TestCase
{
    public void testConvertBeanToMap()
    {
        TestObject testObject = new TestObject();
        testObject.setIntProperty( 2 );
        TestNestedObject testNestedObject = new TestNestedObject();
        testNestedObject.setBoolProperty( true );
        testNestedObject.setStringProperty( "qq" );
        testObject.setNested1( testNestedObject );
        testObject.setNested2( null );
        
        Map<String, Object> map = BeanAsMapUtil.convertBeanToMap( testObject );
        assertEquals( 3, map.size() );
        assertEquals(Integer.valueOf( 2 ), map.get( "intProperty" ));
        
        Object nestedMap = map.get( "nested1" );
        assertTrue( nestedMap instanceof Map );
        assertEquals( 2, ((Map<?,?>)nestedMap).size() );
        assertEquals( Boolean.TRUE, ((Map<?,?>)nestedMap).get( "boolProperty" ) );
        assertEquals( "qq", ((Map<?,?>)nestedMap).get( "stringProperty" ) );

        assertTrue( map.containsKey( "nested2" ) );
        assertNull( map.get( "nested2" ) );
    }
    
    public void testFlattenMap()
    {
        Map<String, Object> hMap = new LinkedHashMap<>();
        hMap.put( "int1", 1 );
        
        Map<String, Object> nestedMap = new LinkedHashMap<>();
        nestedMap.put( "stringABC", "ABC" );
        hMap.put( "nestedMap", nestedMap  );
        
        List<Object> complexList = new ArrayList<>();
        complexList.add( null );
        Map<String, Object> mapInList = new LinkedHashMap<>();
        mapInList.put( "a", 2 );
        complexList.add( mapInList );
        hMap.put( "complexList", complexList );
        
        Map<String, Object> fMap = BeanAsMapUtil.flattenMap( hMap );
        
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put( "int1", 1 );
        expected.put( "nestedMap/stringABC", "ABC" );
        expected.put( "complexList/[0]", null );
        expected.put( "complexList/[1]/a", 2 );

        assertEquals( expected.toString(), fMap.toString() );
    }
    
    public void testFlattenEmptyCollections()
    {
        Map<String, Object> hMap = new LinkedHashMap<>();
        Map<String, Object> fMap = BeanAsMapUtil.flattenMap( hMap );
        assertEquals("{}", fMap.toString());
        
        hMap.put( "key", Collections.emptyMap() );
        fMap = BeanAsMapUtil.flattenMap( hMap );
        assertEquals("{key={}}", fMap.toString());
        
        hMap.put( "key", Collections.emptyList() );
        fMap = BeanAsMapUtil.flattenMap( hMap );
        assertEquals("{key=[]}", fMap.toString());
    }
    
    public void testExpandMap()
    {
        Map<String, Object> flatMap = new LinkedHashMap<>();
        flatMap.put( "int1", 1 );
        flatMap.put( "nestedMap/stringABC", "ABC" );
        flatMap.put( "complexList/[0]", null );
        flatMap.put( "complexList/[1]/a", 2 );
        
        Map<String, Object> expandedMap = BeanAsMapUtil.expandMap( flatMap );
        
        Map<String, Object> expectedMap = new LinkedHashMap<>();
        expectedMap.put( "int1", 1 );
        
        Map<String, Object> nestedMap = new LinkedHashMap<>();
        nestedMap.put( "stringABC", "ABC" );
        expectedMap.put( "nestedMap", nestedMap  );
        
        List<Object> complexList = new ArrayList<>();
        complexList.add( null );
        Map<String, Object> mapInList = new LinkedHashMap<>();
        mapInList.put( "a", 2 );
        complexList.add( mapInList );
        expectedMap.put( "complexList", complexList );
        
        assertEquals( expectedMap.toString(), expandedMap.toString() );
    }
    
    public void testReadBeanFromHierarchicalMap()
    {
        TestObject bean = new TestObject();
        Map<String, Object> hMap = new LinkedHashMap<>();
        hMap.put( "intProperty", 2 );
        Map<String, Object> nested1 = new LinkedHashMap<>();
        nested1.put( "boolProperty", true );
        nested1.put( "stringProperty", "qq" );
        hMap.put( "nested1", nested1 );
        hMap.put( "nested2", null );
        BeanAsMapUtil.readBeanFromHierarchicalMap( hMap, bean );
        
        assertEquals(2, bean.getIntProperty());
        assertTrue( bean.getNested1().isBoolProperty() );
        assertEquals( "qq", bean.getNested1().getStringProperty() );
        assertNull( bean.getNested2() );
    }
}
