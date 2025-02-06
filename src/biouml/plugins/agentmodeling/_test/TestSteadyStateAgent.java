package biouml.plugins.agentmodeling._test;

import ru.biosoft.access._test.AbstractBioUMLTest;

import java.util.HashMap;
import java.util.Map;

import biouml.plugins.agentmodeling.AgentBasedModel;
import biouml.plugins.agentmodeling.ModelAgent;
import biouml.plugins.agentmodeling.Scheduler;
import biouml.plugins.agentmodeling.SteadyStateAgent;
import biouml.plugins.agentmodeling._test.models.LongtermModel;
import biouml.plugins.agentmodeling._test.models.ShorttermModel;
import biouml.plugins.simulation.ResultWriter;
import biouml.plugins.simulation.Span;
import biouml.plugins.simulation.UniformSpan;
import biouml.plugins.simulation.java.EventLoopSimulator;
import biouml.plugins.simulation.ode.jvode.JVodeSolver;
import biouml.standard.simulation.ResultListener;
import biouml.standard.simulation.SimulationResult;
import junit.framework.TestSuite;

public class TestSteadyStateAgent extends AbstractBioUMLTest
{
    public TestSteadyStateAgent(String name)
    {
        super( name );
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite( TestSteadyStateAgent.class.getName() );
        suite.addTest( new TestSteadyStateAgent( "testSingleAgent" ) );
        suite.addTest( new TestSteadyStateAgent( "testTwoAgentsModel" ) );
        return suite;
    }
    public void testSingleAgent() throws Exception
    {
        ShorttermModel model = new ShorttermModel();
        EventLoopSimulator simulator = new EventLoopSimulator();
        Span span = new UniformSpan( 0, 1, 0.1 );
        SteadyStateAgent agent = new SteadyStateAgent( model, simulator, span, "SteadyAgent" );
        AgentBasedModel agentModel = new AgentBasedModel();
        agentModel.addAgent( agent );
        Scheduler scheduler = new Scheduler();
        SimulationResult result = new SimulationResult( null, "result" );
        initResult( model.getVariables(), result );
        ResultWriter writer = new ResultWriter( result );
        scheduler.start( agentModel, span, new ResultListener[] {writer}, null );

        double[] resultX = result.getValues( "xMax" );
        for( double d : resultX )
            assertEquals( 4.62117157, d, 1E-8 );
    }

    public void testTwoAgentsModel() throws Exception
    {
        Span span = new UniformSpan( 0, 100, 10 );
        ShorttermModel shortModel = new ShorttermModel();
        LongtermModel longModel = new LongtermModel();
        SteadyStateAgent agent = new SteadyStateAgent( shortModel, new EventLoopSimulator(), span, "SteadyAgent" );
        ModelAgent agent2 = new ModelAgent( longModel, new JVodeSolver(), span, "LongtermAgent" );
        AgentBasedModel agentModel = new AgentBasedModel();
        agentModel.addAgent( agent );
        agentModel.addAgent( agent2 );

        agent2.addPort( "k", 0 );
        agent.addPort( "center", 0 );
        agentModel.addDirectedLink( agent2, "k", agent, "center" );
        Scheduler scheduler = new Scheduler();
        SimulationResult result = new SimulationResult( null, "result" );

        String[] names1 = shortModel.getVariables();
        String[] names2 = longModel.getVariables();
        String[] names = new String[names1.length + names2.length];
        System.arraycopy( names1, 0, names, 0, names1.length );
        System.arraycopy( names2, 0, names, names1.length, names2.length );
        initResult( names, result );
        ResultWriter writer = new ResultWriter( result );
        scheduler.start( agentModel, span, new ResultListener[] {writer}, null );

        double[] resultX = result.getValues( "xMax" );
        double[] expected = new double[11];
        for( int i = 0; i < expected.length; i++ )
            expected[i] = 4.62117157 + 10 * i;

         assertArrayEquals("Result is not correct", expected, resultX, 1E-8 );
    }

    private void initResult(String[] varNames, SimulationResult result)
    {
        Map<String, Integer> mapping = new HashMap<>();

        for( int i = 0; i < varNames.length; i++ )
            mapping.put( varNames[i], i );
        result.setVariableMap( mapping );
    }
}