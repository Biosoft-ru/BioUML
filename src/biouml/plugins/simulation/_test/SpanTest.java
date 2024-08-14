package biouml.plugins.simulation._test;

import biouml.plugins.simulation.ArraySpan;
import biouml.plugins.simulation.Span;
import biouml.plugins.simulation.UniformSpan;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class SpanTest extends TestCase
{

    public SpanTest(String name)
    {
        super(name);
    }

    /** Make suite if tests. */
    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(SpanTest.class.getName());
        suite.addTest( new SpanTest( "testPrecision" ) );
        suite.addTest( new SpanTest( "testUniformSpan" ) );
        suite.addTest( new SpanTest( "testArraySpan" ) );
        return suite;
    }


    public void testUniformSpan() throws Exception
    {
        test(UNIFORM);

    }

    public void testArraySpan() throws Exception
    {
        test(ARRAY);
    }

    public void testPrecision() throws Exception
    {
        UniformSpan span = new UniformSpan( 5000, 6000, 1 );
        span.addPoints( new double[] {4999.998000000203} );
        UniformSpan sp = (UniformSpan)span.getRestrictedSpan( 4999.9990000002035, 6000 );
        assert ( sp.getTimeStart() == 4999.9990000002035 );
    }

    public void test(int type) throws Exception
    {
        Span span = createSpan(0, 13, 6, type);
        testSpan(span, new double[] {0, 6, 12, 13});
        testSpan(span.getRestrictedSpan(1.4, 6), new double[] {1.4, 6});
        testSpan(span.getRestrictedSpan(1.5, 12.6), new double[] {1.5, 6, 12, 12.6});
        testSpan(span.getRestrictedSpan(0.1, 1), new double[] {0.1, 1});
        testSpan(span.getRestrictedSpan(9, 9.8), new double[] {9, 9.8});
        testSpan(new UniformSpan(0, 1, 1).getRestrictedSpan(0, 1), new double[] {0, 1});
        testSpan(new UniformSpan(0, 10, 1).getRestrictedSpan(1.5, 6.7).getRestrictedSpan(1.5, 4.1), new double[] {1.5, 2.0, 3.0, 4.0, 4.1});

        span = createSpan(1, 7, 1.5, type);
        span.addPoints(new double[] {0, 1.1, 1.3, 2.5, 2.6, 8.3, 10, 11});
        testSpan(span, new double[] {0, 1, 1.1, 1.3, 2.5, 2.6, 4, 5.5, 7, 8.3, 10, 11});
        testSpan(span.getRestrictedSpan(8.8, 11), new double[] {8.8, 10, 11});
        testSpan(span.getRestrictedSpan(8.8, 10), new double[] {8.8, 10});

        span = createSpan(1, 5, 1, type);
        span.addPoints(new double[] {8, 9, 77});
        testSpan(span, new double[] {1, 2, 3, 4, 5, 8, 9, 77});
        testSpan(span.getRestrictedSpan(2.2, 9.6), new double[] {2.2, 3, 4, 5, 8, 9, 9.6});

        span = createSpan(5, 10, 2.5, type);
        span.addPoints(new double[] {0, 1.1, 1.2, 1.33, 2.5, 7.5, 7.6});
        testSpan(span, new double[] {0, 1.1, 1.2, 1.33, 2.5, 5, 7.5, 7.6, 10,});

    }

    public static final int UNIFORM = 0;
    public static final int ARRAY = 1;

    private Span createSpan(double t0, double tf, double inc, int type)
    {
        switch( type )
        {
            case UNIFORM:
                return new UniformSpan(t0, tf, inc);
            case ARRAY:
                return new ArraySpan(t0, tf, inc);
            default:
                return new UniformSpan(t0, tf, inc);
        }
    }

    private void testSpan(Span span, double[] array)
    {
        //check getTime method
        for( int i = 0; i < span.getLength(); i++ )
            assertEquals(span.getTime(i), array[i]);

        //check start and final times
        assertEquals(span.getTimeStart(), array[0]);
        assertEquals(span.getTimeFinal(), array[array.length - 1]);

        //check length
        assertEquals(span.getLength(), array.length);
    }

    private void assertArrayEquals(double[] array1, double[] array2)
    {
        assertEquals(array1.length, array2.length);
        for( int i = 0; i < array1.length; i++ )
            assertEquals(array1[i], array2[i]);
    }



}
