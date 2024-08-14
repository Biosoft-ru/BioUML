package ru.biosoft.util.serialization.xml._test;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import ru.biosoft.util.serialization.Utils;
import ru.biosoft.util.serialization._test.SerializerTest;
import ru.biosoft.util.serialization.xml.Parser;
import ru.biosoft.util.serialization.xml.XMLSerializer;


import junit.framework.TestCase;

/**
 * Created by IntelliJ IDEA.
 * User: puz
 * Date: 06.05.2006
 * Time: 16:26:17
 */

public class XMLSerializerTest extends TestCase
{
    public void testXMLSerializerForObject() throws Exception
    {
        XMLSerializer xmlSerializer = new XMLSerializer();
        xmlSerializer.alias( SerializerTest.TestBean.class, "bean" );
        //xmlSerializer.alias( SerializerTest.TestBean.InnerTestBean.class, "inner_bean" );
        xmlSerializer.alias( SerializerTest.TestBean.InnerTestBean2.class, "inner_bean2" );
        xmlSerializer.alias( SerializerTest.DateTestBean.class, "dbean" );
        xmlSerializer.alias( String[].class, "string_array" );

        String s;
        
        SerializerTest.TestBean testBean = new SerializerTest.TestBean();
        s = xmlSerializer.serialize( testBean );
        
        assertNotNull( s );
        
        SerializerTest.TestBean parsedBean = ( SerializerTest.TestBean )Parser.fromXML( s );
        assertTrue( Utils.areEqual( parsedBean, testBean ) );

        // Test date bean personally
        SerializerTest.DateTestBean dateBean = new SerializerTest.DateTestBean();
        String s1 = xmlSerializer.serialize( dateBean );
        assertNotNull( s1 );
        SerializerTest.DateTestBean dBean = ( SerializerTest.DateTestBean )Parser.fromXML( s1 );
        assertTrue( Utils.areEqual( dBean, dateBean ) );

        // test exclude
        xmlSerializer.exclude( SerializerTest.TestBean.class.getDeclaredField( "p1" ) );
        xmlSerializer.exclude( SerializerTest.TestBean.class.getDeclaredField( "y" ) );
        xmlSerializer.exclude( LinkedList.class );
        s = xmlSerializer.serialize( testBean );
        assertNotNull( s );
        assertEquals( s.indexOf( "p1=\"" ), -1 );
        assertEquals( s.indexOf( "LinkedList" ), -1 );

        xmlSerializer.resetExcluded();

        // test array
        s = xmlSerializer.serialize( new SerializerTest.TestBean[]{ new SerializerTest.TestBean() } );
        assertNotNull( s );

        List<Object> list = new ArrayList<>();
        list.add( new SerializerTest.TestBean() );
        s = xmlSerializer.serialize( list );
        assertNotNull( s );

        list = new ArrayList<>();
        list.add( new SerializerTest.TestBean[]{ new SerializerTest.TestBean() } );
        s = xmlSerializer.serialize( list );
        assertNotNull( s );
    }
}