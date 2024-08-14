package ru.biosoft.analysis._test;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import ru.biosoft.analysis.Stat;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class GammaTest extends TestCase
{
    public GammaTest(String name)
    {
        super( name );
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite( GammaTest.class.getName() );
        suite.addTest( new GammaTest( "test" ) );
        return suite;
    }

    /**
     * @throws Exception
     */
    public void test() throws Exception
    {

        for( int i = 1; i < 100; i++ )
        {
            double gamma = Stat.gammaFunc( i );
            assertEquals( gamma, simpleFactorialDouble( i - 1 ) );
            assertEquals( Math.log( gamma ), Stat.logGamma( i ), 1E-10 );
        }

        //TODO: add more values & wth more precision
        Map<Double, Double> argToVal = new HashMap<Double, Double>()
        {
            {
                put( 1.01, 0.9943 );
                put( 1.02, 0.9888 );
                put( 1.03, 0.9835 );
                put( 1.04, 0.9784 );
                put( 0.5, Math.sqrt( Math.PI ) );
                put( 3d/2, Math.sqrt( Math.PI )/2 );
            }
        };

        double accuracy = 1E-4;
        for( Map.Entry<Double, Double> entry : argToVal.entrySet() )
        {
            Double val = entry.getKey();
            assertEquals( entry.getValue(), Stat.gammaFunc( val ), accuracy );
            assertEquals( entry.getValue() * val, Stat.gammaFunc( val + 1 ), accuracy );
            
            assertEquals( entry.getValue(), Math.exp( Stat.logGamma( val ) ), accuracy );
            assertEquals( entry.getValue() * val, Math.exp( Stat.logGamma( val + 1 ) ), accuracy );
        }

        assertEquals( Stat.gammaFunc( 56.567 ), Stat.gammaFunc( 55.567 ) * 55.567 );
        assertEquals( Stat.gammaFunc( 2d / 3 ), 2 * Math.PI / ( Math.sqrt( 3 ) * Stat.gammaFunc( 1d / 3 ) ), 1E-8 );

        assertEquals( Math.exp( Stat.logGamma( 56.567 ) ), Math.exp( Stat.logGamma( 55.567 ) ) * 55.567, Math.exp( Stat.logGamma( 56.567 ) )*1E-8 );
        assertEquals( Math.exp( Stat.logGamma( 2d / 3 ) ), 2 * Math.PI / ( Math.sqrt( 3 ) * Math.exp( Stat.logGamma(  1d / 3 )) ), 1E-8 );
    }

    private double simpleFactorialDouble(int val)
    {
        return IntStream.rangeClosed( 1, val ).asDoubleStream().reduce( 1.0, (a, b) -> a * b );
    }
}
