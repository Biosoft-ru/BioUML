package biouml.plugins.simulation.java._test;

import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.standard.diagram.DiagramGenerator;
import biouml.standard.simulation.SimulationResult;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestInitialAssignments extends TestCase
{
    public TestInitialAssignments(String name)
    {
        super(name);
    }
    
    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestInitialAssignments.class.getName());
        suite.addTest ( new TestInitialAssignments("test"));
        return suite;
    }
    
    public static void test() throws Exception
    {
        DiagramGenerator generator = new DiagramGenerator("diagram");
        generator.createEquation( "z", "1", Equation.TYPE_RATE );
        generator.createEquation( "y", "x*x", Equation.TYPE_INITIAL_ASSIGNMENT );
        generator.createEquation( "x", "time+3", Equation.TYPE_SCALAR );
        generator.createEquation( "z", "y-4", Equation.TYPE_INITIAL_ASSIGNMENT );
        
        Diagram d = generator.getDiagram();
        
        d.getRole( EModel.class ).getVariable( "z" ).setConstant( true );
        
        JavaSimulationEngine engine = new JavaSimulationEngine();
        engine.setDiagram( d );
        SimulationResult result = new SimulationResult( null, "result" );
        Model model = engine.createModel();
        engine.simulate( model, result );
        double[][] values = result.getValues( new String[] {"z"} );
        assert ( values[0][99] == 5 );
    }
}