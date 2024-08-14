package ru.biosoft.util.serialization._test;
/**
 * Created by IntelliJ IDEA.
 * User: puz
 * Date: 10.05.2006
 * Time: 15:01:51
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.biosoft.util.serialization.Utils;


import junit.framework.TestCase;

public class UtilsTest extends TestCase
{
    public static class TestBean1
    {
        public String s;
        public int i;
        public double[] d = { 1, 2 };

        public static class NestedBean
        {
            public String[][] x = new String[][]{
                new String[]{ "1", "2" },
                new String[]{ "3", "2" }
            };
        }

        List<NestedBean> a;

        public TestBean1( String s )
        {
            nb = new NestedBean();
            nb.x[ 0 ][ 1 ] = s;

            a = new ArrayList<>();
            a.add( new NestedBean() );
        }

        public NestedBean nb;
    }

    public void testAreEqual() throws Exception
    {
        assertTrue( Utils.areEqual( new TestBean1( "a" ), new TestBean1( "a" ) ) );
        assertFalse( Utils.areEqual( new TestBean1( "a" ), new TestBean1( "b" ) ) );

        assertFalse( Utils.areEqual( new double[]{ 1, 2 }, new double[]{ 2, 3 } ) );
        assertFalse( Utils.areEqual(
            new double[][]{ new double[]{ 1, 2 }, new double[]{ 3, 4 } },
            new double[][]{ new double[]{ 1, 2 }, new double[]{ 3, 5 } } ) );

        assertTrue( Utils.areEqual(
            new SerializerTest.TestBean[]{ new SerializerTest.TestBean() },
            new SerializerTest.TestBean[]{ new SerializerTest.TestBean() } ) );
    }

    public void testMapAreEqual() throws Exception
    {
        Map m1 = new HashMap();
        Map m2 = new HashMap();

        m1.put( "1", 2 );
        m2.put( "1", 2 );
        
        assertTrue( Utils.areEqual( m1, m2 ) );

        m2.put( "2", 4 );
        assertFalse( Utils.areEqual( m1, m2 ) );
        assertFalse( Utils.areEqual( m2, m1 ) );
    }
}