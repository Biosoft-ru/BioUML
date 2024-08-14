package biouml.plugins.agentmodeling._test;

import biouml.model.Diagram;
import biouml.plugins.agentmodeling.AgentBasedModel;
import biouml.plugins.agentmodeling.AgentModelSimulationEngine;
import biouml.plugins.agentmodeling.ModelAgent;
import biouml.plugins.agentmodeling.PlotAgent;
import biouml.plugins.hemodynamics.HemodynamicsModelSolver;
import biouml.plugins.hemodynamics.HemodynamicsOptions;
import biouml.plugins.simulation.UniformSpan;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementPath;

/**
 * Test for heart agent model consisting of 6 agents: ArterialSystem, VenousSystem, Ventricle, NeuroHumoralControlSystem, Capillary
 * @author axec
 *
 */
public class ArterialTreeTest extends TestCase
{
    public ArterialTreeTest(String name)
    {
        super(name);
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(ArterialTreeTest.class.getName());
        suite.addTest(new ArterialTreeTest("test"));
        return suite;
    }
    
    public void test() throws Exception
    {
        new AgentModelSimulationEngine().simulate(generateModel());
    }

    public static void main(String ... args) throws Exception
    {
        new AgentModelSimulationEngine().simulate(generateModel());
    }

    private static AgentBasedModel generateModel() throws Exception
    {
        AgentBasedModel agentModel = new AgentBasedModel();
        
        HemodynamicsModelSolver hemoSimulator = new HemodynamicsModelSolver();
        HemodynamicsOptions options = (HemodynamicsOptions)hemoSimulator.getOptions();
//        options.setNumberOfSegmentation(10);
//        options.setSegmentationForIntegration(5);
        options.setTestMode(true);
        options.setInputCondition(HemodynamicsOptions.PRESSURE_INITIAL_CONDITION_STRING);
        options.setOutputCondition(HemodynamicsOptions.PRESSURE_INITIAL_CONDITION_STRING);
        ModelAgent arterialTree = null;/*TODO: fix new ModelAgent(HemodynamicsDiagramGenerator.loadFromFile(new File(modelFileName)), hemoSimulator, new UniformSpan(0, 100, 0.01),
                "artreialTree");*/
        agentModel.addAgent(arterialTree);
        
        PlotAgent plot = new PlotAgent("plot", new UniformSpan(0, 10, 0.05));
        //Util.createGraphics(plot);
        agentModel.addAgent(plot);
        
        agentModel.addDirectedLink(arterialTree, "aortalOutputPressure", plot, "Pressure_Aortal");
        agentModel.addDirectedLink(arterialTree, "rRenalOutputPressure", plot, "Pressure_Renal");
        agentModel.addDirectedLink(arterialTree, "lastVesselPressure", plot, "Pressure_Las");
        agentModel.addDirectedLink(arterialTree, "outputPressure", plot, "output_pressure");
        agentModel.addDirectedLink(arterialTree, "inputFlow", plot, "input_flow");
        return agentModel;
    }
    
    public Diagram getDiagram() throws Exception
    {
        CollectionFactory.createRepository( "../data" );
        return DataElementPath.create("databases/Virtual Human/Diagrams/Arterial Tree test2" ).getDataElement( Diagram.class );
    }
}