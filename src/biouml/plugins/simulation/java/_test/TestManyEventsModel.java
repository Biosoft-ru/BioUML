package biouml.plugins.simulation.java._test;

import biouml.model.Diagram;
import biouml.model.Node;
import biouml.model.dynamics.Assignment;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Event;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.ResultPlotPane;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.standard.diagram.DiagramGenerator;
import biouml.standard.simulation.ResultListener;
import biouml.standard.simulation.SimulationResult;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestManyEventsModel extends TestCase
{
    public TestManyEventsModel(String name)
    {
        super(name);
    }
    
    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestManyEventsModel.class.getName());
        suite.addTest ( new TestManyEventsModel("test"));
        return suite;
    }
    
    public static void test() throws Exception
    {
        DiagramGenerator generator = new DiagramGenerator( "diagram" );

        generator.createEquation( "t", "sin(time)", Equation.TYPE_SCALAR );
        
        for( int i = 0; i < 500; i++ )
        {
            Node evNode = generator.createEvent( "ev" + i, "t >= " + 0, new Assignment( "x" + i, "time", null ) );
            Event ev = evNode.getRole( Event.class );
            ev.setTriggerPersistent( false );
        }

        Diagram d = generator.getDiagram();
        EModel emodel = d.getRole( EModel.class );
//        emodel.getVariable( "x1" ).setShowInPlot( true );
//        emodel.getVariable( "t" ).setShowInPlot( true );
        JavaSimulationEngine engine = new JavaSimulationEngine();
        engine.setDiagram( d );
        engine.setTimeIncrement( 0.1 );
        engine.setCompletionTime( 500 );
        SimulationResult result = new SimulationResult( null, "result" );
        Model model = engine.createModel();
        
        double time = System.currentTimeMillis();
        engine.simulate( model, new ResultListener[] {new ResultPlotPane( engine, null )} );
        System.out.println( System.currentTimeMillis() - time );
        
    }
}