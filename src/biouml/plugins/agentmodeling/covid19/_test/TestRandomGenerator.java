package biouml.plugins.agentmodeling.covid19._test;

import biouml.plugins.agentmodeling.covid19.RandomGenerator;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.analysis.Stat;

public class TestRandomGenerator extends TestCase
{
    public TestRandomGenerator(String name)
    {
        super( name );
    }


    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite( TestRandomGenerator.class.getName() );
        suite.addTest( new TestRandomGenerator( "test" ) );
        return suite;
    }

    public void test() throws Exception
    {
        RandomGenerator randomGenerator = new RandomGenerator();

        int n = 10000000;
        double[] logNormal1 = new double[n];
        double time = System.nanoTime();
        for( int i = 0; i < n; i++ )
            logNormal1[i] = randomGenerator.sampleLogNormal();
        double time1 = ( System.nanoTime() - time ) / 1E9;

        double[] logNormal2 = new double[n];
        time = System.nanoTime();
        for( int i = 0; i < n; i++ )
            logNormal2[i] = randomGenerator.sampleLogNormal2();
        double time2 = ( System.nanoTime() - time ) / 1E9;

        System.out.println( n );
        System.out.println( "Log normal 1 " + time1 );
        System.out.println( "Log normal 2 " + time2 );
        //        System.out.println( "Results:" );
        //        System.out.println( DoubleStreamEx.of( logNormal1 ).joining( " , " ) );
        System.out.println( "Mean " + Stat.mean( logNormal1 ) );
        System.out.println( "Variance " + Stat.variance( logNormal1 ) );
        //        System.out.println( "" );
        //        System.out.println( DoubleStreamEx.of( ogNormal2 ).joining( " , " ) );
        System.out.println( "Mean " + Stat.mean( logNormal2 ) );
        System.out.println( "Variance " + Stat.variance( logNormal2 ) );
    }


}
