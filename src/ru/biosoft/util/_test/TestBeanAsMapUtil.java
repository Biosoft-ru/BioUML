package ru.biosoft.util._test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.developmentontheedge.beans.Option;

import junit.framework.TestCase;
import ru.biosoft.util.BeanAsMapUtil;
import ru.biosoft.util._test.BeanUtilTest.TestNestedObject;
import ru.biosoft.util._test.BeanUtilTest.TestObject;
import ru.biosoft.util.bean.BeanInfoEx2;

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

    /** An item chosen by name, like SiteAggregator: {@code createInstance} answers an unknown name with null. */
    public static class Choice
    {
        private static final Choice[] ALL = {new Choice( "Count" ), new Choice( "Count in exons" ), new Choice( "Positions" )};
        private final String name;
        private Choice(String name)
        {
            this.name = name;
        }
        public static Choice createInstance(String name)
        {
            for( Choice c : ALL )
                if( c.name.equals( name ) )
                    return c;
            return null;
        }
        @Override
        public String toString()
        {
            return name;
        }
    }

    /** A bean with an array of choices shown as one value, as MutationWeightGECalculator and TrackToGeneSet declare resultTypes. */
    @SuppressWarnings ( "serial" )
    public static class ChoiceBean extends Option
    {
        private Choice[] choices = {Choice.createInstance( "Count" )};
        public Choice[] getChoices()
        {
            return choices;
        }
        public void setChoices(Choice[] choices)
        {
            Object oldValue = this.choices;
            this.choices = choices;
            firePropertyChange( "choices", oldValue, choices );
        }
    }

    public static class ChoiceBeanBeanInfo extends BeanInfoEx2<ChoiceBean>
    {
        public ChoiceBeanBeanInfo()
        {
            super( ChoiceBean.class );
        }
        @Override
        protected void initProperties() throws Exception
        {
            property( "choices" ).simple().hideChildren().add();
        }
    }

    private static String choicesOf(Object value, boolean strict)
    {
        ChoiceBean bean = new ChoiceBean();
        bean.setChoices( new Choice[] {Choice.createInstance( "Positions" )} );
        Map<String, Object> map = new LinkedHashMap<>();
        map.put( "choices", value );
        BeanAsMapUtil.readBeanFromHierarchicalMap( map, bean, strict );
        Choice[] choices = bean.getChoices();
        return choices == null ? "null" : java.util.Arrays.toString( choices );
    }

    private static String choicesOf(Object value)
    {
        return choicesOf( value, true );
    }

    /** A JSON list (what an MCP client or a YAML workflow passes) binds to the array, not to null. */
    public void testListBindsToSimpleArray()
    {
        assertEquals( "[Count]", choicesOf( java.util.Arrays.asList( "Count" ) ) );
        assertEquals( "[Count in exons, Positions]", choicesOf( java.util.Arrays.asList( "Count in exons", "Positions" ) ) );
        assertEquals( "[]", choicesOf( new ArrayList<>() ) );
    }

    public void testTextBindsToSimpleArray()
    {
        assertEquals( "[Count in exons]", choicesOf( "Count in exons" ) );
        assertEquals( "[Count, Positions]", choicesOf( "[\"Count\",\"Positions\"]" ) );
    }

    private static final Object[] UNKNOWN_CHOICES = {"count", "Count,Positions", "[\"count\"]", java.util.Arrays.asList( "count" ),
            java.util.Arrays.asList( "Count", "nope" )};

    /** Strict binding (MCP): an unknown name is an error, not a silent null (or a null item) the analysis later fails on. */
    public void testUnknownChoiceIsRejected()
    {
        for( Object value : UNKNOWN_CHOICES )
        {
            try
            {
                String bound = choicesOf( value, true );
                fail( "unknown choice " + value + " was bound as " + bound );
            }
            catch( IllegalArgumentException e )
            {
                assertTrue( e.getMessage(), e.getMessage().contains( "Choice[]" ) );
            }
        }
    }

    /** Lenient binding (workflows): an unknown name is skipped, the property keeps its value and is never set to null. */
    public void testUnknownChoiceIsSkippedWhenLenient()
    {
        for( Object value : UNKNOWN_CHOICES )
            assertEquals( String.valueOf( value ), "[Positions]", choicesOf( value, false ) );
    }
}
