package biouml.plugins.agentmodeling._test;

import java.util.HashMap;
import java.util.Map;

import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;
import biouml.plugins.agentmodeling.AgentBasedModel;
import biouml.plugins.agentmodeling.AgentModelSimulationEngine;
import biouml.plugins.agentmodeling.AgentSimulationEngineWrapper;
import biouml.plugins.agentmodeling.ModelAgent;
import biouml.plugins.agentmodeling.SimulationAgent;
import biouml.plugins.hemodynamics.HemodynamicsModelSolver;
import biouml.plugins.hemodynamics.HemodynamicsSimulationEngine;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.SimulationEngine;
import biouml.standard.simulation.ResultListener;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;

/**
 * Test for heart agent model consisting of 6 agents: ArterialSystem, VenousSystem, Ventricle, NeuroHumoralControlSystem, Capillary
 * @author axec
 *
 */
public class SteadyStateTest extends AbstractBioUMLTest implements ResultListener
{
    public SteadyStateTest(String name)
    {
        super( name );
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite( SteadyStateTest.class.getName() );
        suite.addTest( new SteadyStateTest( "test" ) );
        return suite;
    }

    public void test() throws Exception
    {
        Diagram diagram = getDiagram();
        engine = new AgentModelSimulationEngine();
        engine.setDiagram( diagram );
        engine.setOutputDir( "../out" );
        model = engine.createModel();

        engine.setCompletionTime( 205 );
        engine.setTimeIncrement( 1 );
        engine.setSolver( new HemodynamicsModelSolver() );
        engine.simulate( model, new ResultListener[] {this} );
    }

    DataCollection collection;
    public Diagram getDiagram() throws Exception
    {
        CollectionFactory.createRepository( "../data" );
        Diagram diagram = DataElementPath.create("databases/Virtual Human/Diagrams/Complex CVS Model with Kidney and AT" ).getDataElement( Diagram.class );
        collection = diagram.getOrigin();
        return diagram;
    }

    @Override
    public void start(Object model)
    {
        // TODO Auto-generated method stub

    }

    AgentBasedModel model;

    AgentModelSimulationEngine engine;
    @Override
    public void add(double t, double[] y) throws Exception
    {
        if( t > 50 )
        {
            for( SimulationAgent agent : model.getAgents() )
            {
                if( agent instanceof ModelAgent )
                {
                    SimulationEngine engine = ( (ModelAgent)agent ).getEngine();
                    if( engine instanceof AgentSimulationEngineWrapper )
                    {
                        engine = ( (AgentSimulationEngineWrapper)engine ).getPrototype();
                        if( engine instanceof HemodynamicsSimulationEngine )
                        {
                            //                        setParameters( (HemodynamicsSimulationEngine)engine );
                            continue;
                        }

                        Model model = ( (ModelAgent)agent ).getModel();
                        HashMap<String, Double> params = getParameters( engine, model );
                        setParameters( engine.getDiagram(), params );
                    }
                }
            }
            engine.stopSimulation();
        }
    }



    private HashMap<String, Double> getParameters(SimulationEngine engine, Model model) throws Exception
    {
        HashMap<String, Double> result = new HashMap<>();
        double[] simulationResult = model.getCurrentValues();

        String[] varNames = engine.getVariableNames();

        for( String varName : varNames )
        {
            int index = engine.getVarIndexMapping().get( varName );
            result.put( varName, simulationResult[index] );
        }
        return result;
    }

    private void setParameters(Diagram diagram, HashMap<String, Double> parameters)
    {
        try
        {
            Diagram origDiagram = getOriginalDiagram(diagram);
            EModel emodel = origDiagram.getRole( EModel.class );
            for( Map.Entry<String, Double> entry : parameters.entrySet() )
            {
                Variable var = emodel.getVariable(entry.getKey());
                if( var != null )
                    var.setInitialValue(entry.getValue());
            }
            origDiagram.save();
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }
    }


    private Diagram getOriginalDiagram(Diagram diagram) throws Exception
    {
        String name = diagram.getName();
        if( name.contains( "_main" ) )
        {

            name = name.replace( "_main", "" );
        }
        return (Diagram)collection.get( name );


    }
    //    HemodynamicsModelSolver6 arterialSolver;

    //    Matrix area[];
    //    Matrix pressure[];
    //    Matrix flow[];
    //    private void setParameters(HemodynamicsSimulationEngine engine)
    //    {
    //        area = arterialSolver.getArea();
    //        arterialSolver.getPresure();
    //        flow = arterialSolver.getFlux();
    //    }


}