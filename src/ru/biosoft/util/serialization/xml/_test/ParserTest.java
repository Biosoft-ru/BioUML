package ru.biosoft.util.serialization.xml._test;

import java.util.ArrayList;
import java.util.List;
import ru.biosoft.util.serialization._test.SerializerTest;
import ru.biosoft.util.serialization.xml.TestUtils;


import junit.framework.TestCase;

/**
 * Created by IntelliJ IDEA.
 * User: puz
 * Date: 10.05.2006
 * Time: 18:38:45
 */

public class ParserTest extends TestCase
{
    public void testFromXMLNull() throws Exception
    {
        assertTrue( TestUtils.verifyXMLSerialization( null ) );
    }

    public void testFromXMLArray() throws Exception
    {
        assertTrue( TestUtils.verifyXMLSerialization( new int[]{ 10, 5 } ) );
    }

    public void testFromXMLStringArray() throws Exception
    {
        assertTrue( TestUtils.verifyXMLSerialization( new String[]{ } ) );
        assertTrue( TestUtils.verifyXMLSerialization( new String[]{ "10", "5" } ) );
        assertTrue( TestUtils.verifyXMLSerialization( new String[]{ null } ) );
        assertTrue( TestUtils.verifyXMLSerialization( new String[]{ "10", "5", null } ) );
    }

    public void testFromXMLInteger() throws Exception
    {
        assertTrue( TestUtils.verifyXMLSerialization( 10 ) );
    }

    public void testFromXMLList() throws Exception
    {
        List<Integer> list = new ArrayList<>();
        list.add( 15 );
        list.add( 25 );
        assertTrue( TestUtils.verifyXMLSerialization( list ) );
    }

    public void testFromXMLArray2() throws Exception
    {
        assertTrue( TestUtils.verifyXMLSerialization( new Object[]
            {
                10,
                20.0
            } ) );
    }

    public void testFromXMLInnerBean() throws Exception
    {
//        assertTrue( TestUtils.verifyXMLSerialization( new SerializerTest.TestBean.InnerTestBean() ) );
    }

    public void testFromXMLInnerBean2() throws Exception
    {
        assertTrue( TestUtils.verifyXMLSerialization( new SerializerTest.TestBean.InnerTestBean2() ) );
    }

    public void testFromXMLBean() throws Exception
    {
        assertTrue( TestUtils.verifyXMLSerialization( new SerializerTest.TestBean() ) );
    }

    public void testFromXMLDateBean() throws Exception
    {
        assertTrue( TestUtils.verifyXMLSerialization( new SerializerTest.DateTestBean() ) );
    }

    public void testFromXMLBeanArray() throws Exception
    {
        assertTrue( TestUtils.verifyXMLSerialization( new SerializerTest.TestBean[]{ new SerializerTest.TestBean() } ) );
    }

    public void testFromXMLBeanList() throws Exception
    {
        List<SerializerTest.TestBean> list = new ArrayList<>();
        list.add( new SerializerTest.TestBean() );
        assertTrue( TestUtils.verifyXMLSerialization( list ) );
    }

    public void testFromXMLBeanArrayList() throws Exception
    {
        List<SerializerTest.TestBean[]> list = new ArrayList<>();
        list.add( new SerializerTest.TestBean[]{ new SerializerTest.TestBean() } );
        assertTrue( TestUtils.verifyXMLSerialization( list ) );
    }

    public void testFromXMLBeanMap() throws Exception
    {
//        Map m = new Hashtable();
//        SerializerTest.TestBean.TestHashValBean v = new SerializerTest.TestBean.TestHashValBean();
//        v.i1 = 100;
//        v.s = "xx";
//        m.put( "1", v );
//
//        assertTrue( TestUtils.verifyXMLSerialization( m ) );
//        assertTrue( TestUtils.verifyXMLSerialization( new SerializerTest.TestBean.TestHashBean1() ) );
//        assertTrue( TestUtils.verifyXMLSerialization( new SerializerTest.TestBean.TestHashBean2() ) );
    }
}