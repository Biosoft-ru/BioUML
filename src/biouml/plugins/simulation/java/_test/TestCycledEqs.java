package biouml.plugins.simulation.java._test;

import biouml.model.Diagram;
import biouml.model.dynamics.Equation;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.standard.diagram.DiagramGenerator;
import biouml.standard.simulation.SimulationResult;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestCycledEqs extends TestCase
{
    public TestCycledEqs(String name)
    {
        super(name);
    }
    
    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestCycledEqs.class.getName());
        suite.addTest ( new TestCycledEqs("testClosedCycle"));
        suite.addTest ( new TestCycledEqs("testChain"));
        return suite;
    }
    
    public static void testClosedCycle() throws Exception
    {
        DiagramGenerator generator = new DiagramGenerator("diagram");
        generator.createEquation( "z", "y-1", Equation.TYPE_SCALAR );
        generator.createEquation( "x", "z+5", Equation.TYPE_SCALAR );
        generator.createEquation("y", "2*x", Equation.TYPE_SCALAR);
        
        Diagram d = generator.getDiagram();
        
        JavaSimulationEngine engine = new JavaSimulationEngine();
        engine.setDiagram( d );
        engine.setCompletionTime( 5 );
        SimulationResult result = new SimulationResult(null, "result");
        engine.simulate(result);
        
        double[][] values = result.getValues( new String[] {"x","y","z"} );
        assert ( values[0][4] == -4 );
        assert ( values[1][4] == -18 );
        assert ( values[0][4] == -9 );
    }
    
    public static void testChain() throws Exception
    {
        DiagramGenerator generator = new DiagramGenerator("diagram");
        generator.createEquation("y", "2*x", Equation.TYPE_SCALAR);
        generator.createEquation( "x", "z+5", Equation.TYPE_SCALAR );   
        generator.createEquation( "z", "time-1", Equation.TYPE_SCALAR );
        
        Diagram d = generator.getDiagram();
        
        JavaSimulationEngine engine = new JavaSimulationEngine();
        engine.setDiagram( d );
        engine.setCompletionTime( 5 );
        SimulationResult result = new SimulationResult(null, "result");
        engine.simulate(result);
        
        double[][] values = result.getValues( new String[] {"x","y","z"} );
        assert ( values[0][4] == 9 );
        assert ( values[1][4] == 18 );
        assert ( values[0][4] == 4 );
    }
}