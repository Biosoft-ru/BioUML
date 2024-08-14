package biouml.standard.simulation._test;

import ru.biosoft.access._test.AbstractBioUMLTest;
import biouml.standard.simulation.StochasticSimulationResult;
import junit.framework.Test;
import junit.framework.TestSuite;

public class TestStochasticResult extends AbstractBioUMLTest
{
    public TestStochasticResult(String name)
    {
        super( name );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( TestStochasticResult.class.getName() );
        suite.addTest( new TestStochasticResult( "test" ) );
        return suite;
    }
  
    //TODO: probably move all those methods to ru.biosoft.analysis.Stat
    public void test() throws Exception
    {
        double[] test = new double[] {1, 1, 2, 3, 5, 6, 7};
        assertEquals( StochasticSimulationResult.median(test), 3.0 );
        assertEquals( StochasticSimulationResult.quartile1(test), 1.0 );
        assertEquals( StochasticSimulationResult.quartile3(test), 6.0 );
        
        test = new double[] {1, 1, 2, 3, 5, 6, 7, 8};
        assertEquals( StochasticSimulationResult.median(test), 4.0 );
        assertEquals( StochasticSimulationResult.quartile1(test), 1.5 );
        assertEquals( StochasticSimulationResult.quartile3(test), 6.5 );
        
        test = new double[] {1, 1, 2, 3, 4, 5, 6, 7, 8};
        assertEquals( StochasticSimulationResult.median(test), 4.0 );
        assertEquals( StochasticSimulationResult.quartile1(test), 1.5 );
        assertEquals( StochasticSimulationResult.quartile3(test), 6.5 );        
    }
}