package biouml.plugins.agentmodeling._test;

import java.text.DecimalFormat;

import ru.biosoft.access.support.IdGenerator;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.SubDiagram;
import biouml.model.dynamics.Connection;
import biouml.model.dynamics.DirectedConnection;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.MultipleConnection;
import biouml.model.dynamics.MultipleDirectedConnection;
import biouml.model.dynamics.MultipleUndirectedConnection;
import biouml.model.dynamics.UndirectedConnection;
import biouml.plugins.agentmodeling.AgentEModel;
import biouml.plugins.agentmodeling.AgentModelDiagramType;
import biouml.plugins.agentmodeling.AgentModelSimulationEngine;
import biouml.plugins.agentmodeling.AgentSimulationEngineWrapper;
import biouml.plugins.simulation.SimulationEngine;
import biouml.standard.diagram.ConnectionEdgePane;
import biouml.standard.diagram.DiagramGenerator;
import biouml.standard.diagram.PathwaySimulationDiagramType;
import biouml.standard.diagram.Util;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class SchedulerTest extends TestCase
{
    public SchedulerTest(String name)
    {
        super( name );
    }


    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite( SchedulerTest.class.getName() );
        suite.addTest( new SchedulerTest( "test" ) );
        return suite;
    }
    public void test() throws Exception
    {
        AgentModelSimulationEngine simulationEngine = new AgentModelSimulationEngine();
        simulationEngine.setDiagram( generateDiagram() );
        for( AgentSimulationEngineWrapper engineWrapper : simulationEngine.getEngines() )
        {
        	SimulationEngine engine = engineWrapper.getEngine();
        	engine.setInitialTime(0.0);
        	engine.setCompletionTime(10.0);
        	engine.setTimeIncrement(0.1);
        }
        simulationEngine.simulate();
        System.out.println( "" );
    }

    protected Diagram generateDiagram() throws Exception
    {
        Diagram d = new Diagram( null, new Stub( null, "diagram" ), new AgentModelDiagramType() );
        d.setRole( new AgentEModel( d ) );
        SubDiagram subDiagram1 = generateSubDiagram1( d );
        SubDiagram subDiagram2 = generateSubDiagram2( d );
        Node plotNode = createPlot( d, "plot", 0, 10, 0.1 );
        createConnection( d, subDiagram1, subDiagram2, "v", "v", false );
        createConnection( d, subDiagram2, plotNode, "v", "v", true );
        return d;
    }

    protected static SubDiagram generateSubDiagram1(Diagram diagram) throws Exception
    {
        Diagram d = new Diagram( null, new Stub( null, "subdiagram1" ), new PathwaySimulationDiagramType() );
        d.setRole( new EModel( d ) );
        DiagramGenerator generator = new DiagramGenerator( d );
        generator.createEquation( "v", "1", Equation.TYPE_RATE );
        generator.createPort( "v", Type.TYPE_INPUT_CONNECTION_PORT );
        SubDiagram subDiagram2 = new SubDiagram( diagram, d, "subdiagram1" );
        subDiagram2.save();
        return subDiagram2;
    }

    protected static SubDiagram generateSubDiagram2(Diagram diagram) throws Exception
    {
        Diagram d = new Diagram( null, new Stub( null, "subdiagram2" ), new PathwaySimulationDiagramType() );
        d.setRole( new EModel( d ) );
        DiagramGenerator generator = new DiagramGenerator( d );
        generator.createEquation( "v", "-1", Equation.TYPE_RATE );
        generator.createPort( "v", Type.TYPE_INPUT_CONNECTION_PORT );
        diagram.put( generator.getDiagram() );
        SubDiagram subDiagram2 = new SubDiagram( diagram, d, "subdiagram2" );
        subDiagram2.save();
        return subDiagram2;
    }

    protected static Node createPlot(Diagram diagram, String name, double initialTime, double completionTime, double timeIncrement)
            throws Exception
    {
        Node node = new Node( diagram, new Stub.PlotElement( null, name ) );
        node.getAttributes().add( new DynamicProperty( Util.INITIAL_TIME, Double.class, 0 ) );
        node.getAttributes().add( new DynamicProperty( Util.COMPLETION_TIME, Double.class, 100 ) );
        node.getAttributes().add( new DynamicProperty( Util.TIME_INCREMENT, Double.class, 1 ) );
        node.save();
        return node;
    }

    protected static void createConnection(Diagram diagram, Node input, Node output, boolean directedConnection) throws Exception
    {
        String stubName = IdGenerator.generateUniqueName( diagram, new DecimalFormat( "CONNECTION0000" ) );
        Edge e = new Edge( diagram, new Stub( diagram, stubName ), input, output );
        Connection role = ( directedConnection ) ? new DirectedConnection( e ) : new UndirectedConnection( e );
        role.setInputPort( new Connection.Port( ConnectionEdgePane.getPortVariableName( input ), input.getTitle() ) );
        role.setOutputPort( new Connection.Port( ConnectionEdgePane.getPortVariableName( output ), output.getTitle() ) );
        e.setRole( role );
        diagram.put( e );
    }

    protected static void createConnection(Diagram diagram, SubDiagram d1, SubDiagram d2, String v1, String v2, boolean directedConnection)
            throws Exception
    {
        String stubName = IdGenerator.generateUniqueName( diagram, new DecimalFormat( "CONNECTION0000" ) );
        Edge e = new Edge( diagram, new Stub( diagram, stubName ), d1, d2 );
        MultipleConnection role = ( directedConnection ) ? new MultipleDirectedConnection( e ) : new MultipleUndirectedConnection( e );

        Connection c = ( directedConnection ) ? new DirectedConnection( e ) : new UndirectedConnection( e );
        c.setInputPort( new Connection.Port( v1, v1 ) );
        c.setOutputPort( new Connection.Port( v2, v2 ) );

        role.addConnection( c );
        e.setRole( role );
        diagram.put( e );
    }

    protected static void createConnection(Diagram diagram, SubDiagram d1, Node plot, String v1, String v2, boolean directedConnection)
            throws Exception
    {
        String stubName = IdGenerator.generateUniqueName( diagram, new DecimalFormat( "CONNECTION0000" ) );
        Edge e = new Edge( diagram, new Stub( diagram, stubName ), d1, plot );

        Connection c = ( directedConnection ) ? new DirectedConnection( e ) : new UndirectedConnection( e );
        c.setInputPort( new Connection.Port( v1, v1 ) );
        c.setOutputPort( new Connection.Port( v2, v2 ) );

        e.setRole( c );
        diagram.put( e );
    }
}
