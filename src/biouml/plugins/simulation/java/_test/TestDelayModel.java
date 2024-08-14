package biouml.plugins.simulation.java._test;

import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import biouml.plugins.simulation.Span;
import biouml.plugins.simulation.UniformSpan;
import biouml.plugins.simulation.java.EventLoopSimulator;

public class TestDelayModel extends AbstractBioUMLTest
{
    public TestDelayModel(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestDelayModel.class.getName());
        suite.addTest(new TestDelayModel("test"));
        return suite;
    }

    public void test() throws Exception
    {
        DelayModel model = new DelayModel();
        
        EventLoopSimulator solver = new EventLoopSimulator();
        Span span = new UniformSpan(0,100,1);
        solver.start( model, span, null, null );       
        
    }
    
    
    
}
