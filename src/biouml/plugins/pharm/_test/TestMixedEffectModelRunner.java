package biouml.plugins.pharm._test;

import java.util.HashMap;

import one.util.streamex.DoubleStreamEx;

import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import biouml.plugins.pharm.nlme.MixedEffectModelRunner;

public class TestMixedEffectModelRunner extends AbstractBioUMLTest
{
    public TestMixedEffectModelRunner(String name)
    {
        super( name );
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite( TestMixedEffectModelRunner.class.getName() );
        suite.addTest( new TestMixedEffectModelRunner( "test2" ) );
        return suite;
    }

    /**
     * @throws Exception
     */
    public void test() throws Exception
    {

        TableDataCollection tdc =  TestUtil.getExampleTable(THEOPH_DATA_NAME);

        double[] times = TableDataCollectionUtils.getColumn( tdc, "Time" );
        double[] subject = TableDataCollectionUtils.getColumn( tdc, "Subject" );
        double[] doses = TableDataCollectionUtils.getColumn( tdc, "Dose" );
        int[] subj = DoubleStreamEx.of(subject).mapToInt(x -> (int)x).toArray();

        double[] ka = createArray( 0.5, 132 );
        double[] ke = createArray( -2.5, 132 );
        double[] cl = createArray( -3.2, 132 );

        MixedEffectModelRunner runner = new MixedEffectModelRunner();
        runner.setMixedEffectModel( new Theoph_nlme() );

        HashMap<String, double[]> parameters = new HashMap<>();
        parameters.put( "CL", cl );
        parameters.put( "ka", ka );
        parameters.put( "ke", ke );
        double[] result = runner.calculate( parameters, times, subj );


        for( double val : result )
        {
            //        assertTrue( Math.abs( result[i] - testResult[i] ) < ERROR );
            System.out.println( val + "\t" + Math.abs( val ) );

        }
    }

    public static double[] createArray(double value, int size)
    {
        double[] result = new double[size];
        for( int i = 0; i < size; i++ )
        {
            result[i] = value;
        }
        return result;
    }

    private static final String THEOPH_DATA_NAME = "theophdata";
    

    
    
    public void test2() throws Exception
    {

        TableDataCollection tdc = TestUtil.getExampleTable("Indometh");

        double[] times = TableDataCollectionUtils.getColumn( tdc, "time" );
        double[] subject = TableDataCollectionUtils.getColumn( tdc, "Subject" );

        int[] subj = DoubleStreamEx.of(subject).mapToInt(x -> (int)x).toArray();

        MixedEffectModelRunner runner = new MixedEffectModelRunner();
        runner.setMixedEffectModel( new Indometh_nlme() );

        HashMap<String, double[]> parameters = new HashMap<>();
        parameters.put( "k10", createArray( -0.1, 66 ) );
        parameters.put( "k12", createArray( -0.05, 66 ) );
        parameters.put( "k21", createArray( -0.15, 66 ) );
        parameters.put( "start", createArray( 0.7, 66 ) );
        double[] result = runner.calculate( parameters, times, subj );


        for( double val : result )
        {
            //        assertTrue( Math.abs( result[i] - testResult[i] ) < ERROR );
            System.out.println( val + "\t" + Math.abs( val ) );

        }
    }


}
