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
        private int[] numbers = {7};
        private int limit = 5;
        public int getLimit()
        {
            return limit;
        }
        public void setLimit(int limit)
        {
            if( limit < 0 )
                throw new IllegalArgumentException( "limit must be >= 0" );
            Object oldValue = this.limit;
            this.limit = limit;
            firePropertyChange( "limit", oldValue, limit );
        }
        private String fixed = "fixed";
        private String tag = "t";
        public String getTag()
        {
            return tag;
        }
        /** Normalizes, and throws after changing the field: strict binding must report it all the same. */
        public void setTag(String tag)
        {
            this.tag = tag.trim();
            if( this.tag.isEmpty() )
                throw new IllegalArgumentException( "tag must not be blank" );
        }
        public String getFixed()
        {
            return fixed;
        }
        public void setFixed(String fixed)
        {
            this.fixed = fixed;
        }

        public int[] getNumbers()
        {
            return numbers;
        }
        public void setNumbers(int[] numbers)
        {
            Object oldValue = this.numbers;
            this.numbers = numbers;
            firePropertyChange( "numbers", oldValue, numbers );
        }
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
            property( "numbers" ).simple().hideChildren().add();
            add( "limit" );
            add( "tag" );
            property( "fixed" ).readOnly().add();
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

    /** Strict binding (MCP): a value that converts to nothing, or to an array with a missing item, is an error. */
    public void testUnknownChoiceIsRejected()
    {
        Object[][] cases = {
                {"count", "cannot convert count to Choice[]"},
                {"Count,Positions", "cannot convert Count,Positions to Choice[]"},
                {"[\"count\"]", "choices[0]: cannot convert \"count\" to Choice"},
                {java.util.Arrays.asList( "count" ), "choices[0]: cannot convert \"count\" to Choice"},
                {java.util.Arrays.asList( "Count", "nope", "Positions" ), "choices[1]: cannot convert \"nope\" to Choice"},
                {java.util.Arrays.asList( "Count", null ), "choices[1]: cannot convert null to Choice"},
                {java.util.Arrays.asList( (Object)null ), "choices[0]: cannot convert null to Choice"},
                {java.util.Arrays.asList( "Count", 123 ), "choices[1]: cannot convert \"123\" to Choice"},
                {java.util.Arrays.asList( java.util.Arrays.asList( "Count" ) ), "to Choice"}};
        for( Object[] c : cases )
        {
            try
            {
                String bound = choicesOf( c[0], true );
                fail( "unknown choice " + c[0] + " was bound as " + bound );
            }
            catch( IllegalArgumentException e )
            {
                assertTrue( c[0] + ": " + e.getMessage(), e.getMessage().contains( (String)c[1] ) );
            }
        }
    }

    /** An explicit null and an empty string clear the property in both modes; an empty list is an empty array. */
    public void testNullAndEmptyClear()
    {
        for( boolean strict : new boolean[] {true, false} )
        {
            assertEquals( "null", choicesOf( null, strict ) );
            assertEquals( "null", choicesOf( "", strict ) );
            assertEquals( "[]", choicesOf( new ArrayList<>(), strict ) );
        }
    }

    /** Lenient binding (workflows) is unchanged but for lists: an unknown name is bound as null, as it always was. */
    public void testLenientBinding()
    {
        assertEquals( "[Count]", choicesOf( java.util.Arrays.asList( "Count" ), false ) );
        assertEquals( "null", choicesOf( "count", false ) );
    }

    /** List items are passed as strings and parsed by the component type, so a list of numbers binds an int[]. */
    public void testListBindsToSimplePrimitiveArray()
    {
        ChoiceBean bean = new ChoiceBean();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put( "numbers", java.util.Arrays.asList( 1, 2 ) );
        BeanAsMapUtil.readBeanFromHierarchicalMap( map, bean, true );
        assertEquals( "[1, 2]", java.util.Arrays.toString( bean.getNumbers() ) );

        map.put( "numbers", java.util.Arrays.asList( "x" ) );
        try
        {
            BeanAsMapUtil.readBeanFromHierarchicalMap( map, bean, true );
            fail( "[\"x\"] was bound as " + java.util.Arrays.toString( bean.getNumbers() ) );
        }
        catch( IllegalArgumentException e )
        {
            assertTrue( e.getMessage(), e.getMessage().contains( "int[]" ) );
        }
    }

    private static ChoiceBean bind(String name, Object value, boolean strict)
    {
        ChoiceBean bean = new ChoiceBean();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put( name, value );
        BeanAsMapUtil.readBeanFromHierarchicalMap( map, bean, strict );
        return bean;
    }

    private static void assertStrictError(String name, Object value, String expected)
    {
        try
        {
            bind( name, value, true );
            fail( name + "=" + value + " was accepted" );
        }
        catch( IllegalArgumentException e )
        {
            assertTrue( name + "=" + value + ": " + e.getMessage(), e.getMessage().contains( expected ) );
        }
    }

    /** Strict binding reports what lenient binding drops: a primitive cleared, a setter refusing, a read-only property. */
    public void testStrictReportsWhatLenientDrops()
    {
        assertStrictError( "limit", "", "cannot convert" );
        // getValueClass() is Integer, so an int given null fails only in the setter call
        assertStrictError( "limit", null, "limit: cannot set null" );
        // Property.setValue logs and swallows what the setter throws: strict binding calls the setter itself
        assertStrictError( "limit", -1, "limit: -1 was refused: limit must be >= 0" );
        assertStrictError( "tag", "  ", "tag:    was refused: tag must not be blank" );
        assertEquals( "x", bind( "tag", " x ", true ).getTag() );
        assertStrictError( "fixed", "other", "fixed is read-only" );
        assertEquals( 3, bind( "limit", 3, true ).getLimit() );
        assertEquals( 5, bind( "limit", 5, true ).getLimit() );
        assertEquals( "fixed", bind( "fixed", "fixed", true ).getFixed() );

        assertEquals( 5, bind( "limit", -1, false ).getLimit() );
        assertEquals( "fixed", bind( "fixed", "other", false ).getFixed() );
    }
}
