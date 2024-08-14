package ru.biosoft.util.serialization._test;
/**
 * Created by IntelliJ IDEA.
 * User: puz
 * Date: 04.05.2006
 * Time: 16:55:39
 */

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import ru.biosoft.util.SuppressHuntBugsWarning;
import junit.framework.TestCase;

public class SerializerTest extends TestCase
{
    public static class SuperSuperTestBean
    {
        int x = 1;
    }

    public static class SuperTestBean extends SuperSuperTestBean
    {
        int x = 1;
    }

    @SuppressHuntBugsWarning("*")
    public static class TestBean extends SuperTestBean
    {
        public static class TestHashBean1
        {
            public Hashtable<String, Object> h = new Hashtable<>();

            public TestHashBean1()
            {
                SerializerTest.TestBean.TestHashValBean to = new SerializerTest.TestBean.TestHashValBean();
                to.i1 = 5;
                to.s = "testValBeanField";

                h.put( "testKey", to );
                h.put( "testKey2", 3 );
                h.put( "testKey2", new String[][]{ { "1", "2" }, { "3", "4" } } );
            }
        }

        public static class TestHashBean2
        {
            public Hashtable<String, Object> h = new Hashtable<>();

            Hashtable[] hh = new Hashtable[2];

            List<Hashtable<String, Object>> lh = new ArrayList<>();

            public TestHashBean2()
            {
                SerializerTest.TestBean.TestHashValBean to = new SerializerTest.TestBean.TestHashValBean();
                to.i1 = 5;
                to.s = "testValBeanField";

                h.put( "testKey", to );
                h.put( "testKey2", 3 );
                h.put( "testKey2", new String[][]{ { "1", "2" }, { "3", "4" } } );

                hh[ 0 ] = h;
                hh[ 1 ] = h;

                lh.add( h );
                lh.add( h );
            }
        }

        public static class TestHashKeyBean
        {
            public int i1 = 1;
            public String s = "ss";
        }

        public static class TestHashValBean
        {
            public int i1 = 1;
            public String s = "ss";
        }

        public static class InnerTestBean
        {
            public int i1 = 1;
            public String s = "ss";
            public InnerTestBean2 bean;
            
            public InnerTestBean()
            {
                
            }
            
            public InnerTestBean(InnerTestBean2 beanArg)
            {
                this.bean = beanArg;
            }
        }
        
        public static class InnerTestBean3
        {
            public int i1 = 1;
            public String s = "ss";
        }

        public static class InnerTestBean2
        {
            public InnerTestBean2()
            {
            }

            public InnerTestBean2( int k )
            {
                i = k;
            }
            
            public InnerTestBean2(TestBean testBeanArg)
            {
                this.testBean = testBeanArg;
                this.innerTestBean2 = new InnerTestBean(this);
            }

            public InnerTestBean innerTestBean2;
            public TestBean testBean;
            public Integer i = 20;
            public Double d = 20.0;
            public String s = "ssssss";
        }

        public String p1 = "value1";
        public String p2 = "value2";

        public String _null = null;

        private final String[] y = { "n", "m" };
        private final double[] dd = { 1, 2, 3 };
        private final double[][] ddd = { { 1, 2, 3 }, { 2, 4 } };

        List a = new LinkedList();
        List<InnerTestBean2> aa = new LinkedList<>();

        public String[][] x = {
            new String[]{ "a", "b" },
            new String[]{ "c", "d" }
        };

        public InnerTestBean inner = new InnerTestBean();

        public InnerTestBean2[] qq = { new InnerTestBean2( 20 ), new InnerTestBean2( 40 ) };

        public TestBean()
        {
            a.add( new double[]{ 1, 2 } );
            a.add( new double[][]{ { 1 }, { 2, 3, 4, 5 } } );
            a.add( null );

            aa.add( new InnerTestBean2( 100 ) );
            //aa.add( new InnerTestBean2( 300 ) );
            aa.add( new InnerTestBean2( this ) );
        }

        int i = 10;
        boolean b = true;
        double d = 1.23456;
        float f = 35;

        java.util.Date date = new java.util.Date();
    }
    
    public static class DateTestBean
    {
        java.util.Date d = new java.util.Date();
        java.sql.Timestamp t = new java.sql.Timestamp( 1000 );
        java.sql.Date sqld = new java.sql.Date( System.currentTimeMillis() );
    }

    public void testFake()
    {
    }
}