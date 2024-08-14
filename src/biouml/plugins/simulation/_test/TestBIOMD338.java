package biouml.plugins.simulation._test;

import biouml.plugins.simulation.UniformSpan;
import biouml.plugins.simulation.java.EventLoopSimulator;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.plugins.simulation.ode.jvode.JVodeSolver;
import biouml.standard.simulation.ResultListener;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import one.util.streamex.DoubleStreamEx;

public class TestBIOMD338 extends TestCase implements ResultListener
{
    public TestBIOMD338(String name)
    {
        super( name );
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite( TestBIOMD338.class.getName() );
        suite.addTest( new TestBIOMD338( "test" ) );
        return suite;
    }

    public void test() throws Exception
    {
        
//        TestModel model = new TestModel();
        
//        JavaSimulationEngine engine = new JavaSimulationEngine();
        
//        engine.simulate( model );
//        engine.setSpan(  );
        
//        engine.createModel();
        
//        engine.simulate( model );
        
//        model.init();
//        
//        double[] vals = model.getCurrentValues();
//        model.setCurrentValues( vals );
//        EventLoopSimulator solver = new EventLoopSimulator();
        
//        solver.start( model, new UniformSpan(0, 100, 7), new ResultListener[] {this}, null );
        
    }

    @Override
    public void start(Object model)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void add(double t, double[] y) throws Exception
    {
        // TODO Auto-generated method stub
        System.out.println( t+"\t"+DoubleStreamEx.of( y ).joining( "\t" ) );
    }
}
