package biouml.plugins.agentmodeling._test;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import biouml.plugins.agentmodeling.AgentBasedModel;
import biouml.plugins.agentmodeling.AgentModelSimulationEngine;
import biouml.plugins.agentmodeling.ModelAgent;
import biouml.plugins.agentmodeling.PlotAgent;
import biouml.plugins.agentmodeling._test.models.Solodyannikov;
import biouml.plugins.agentmodeling._test.models.Solodyannikov_without_arteries;
import biouml.plugins.hemodynamics.ArterialBinaryTreeModel;
import biouml.plugins.hemodynamics.HemodynamicsModelSolver;
import biouml.plugins.hemodynamics.HemodynamicsOptions;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.Simulator;
import biouml.plugins.simulation.UniformSpan;
import biouml.plugins.simulation.java.EventLoopSimulator;

/**
 * Test for heart agent model consisting of 6 agents: ArterialSystem, VenousSystem, Ventricle, NeuroHumoralControlSystem, Capillary
 * @author axec
 *
 */
public class ArterialTreeTest2 extends TestCase
{
    public ArterialTreeTest2(String name)
    {
        super(name);
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(ArterialTreeTest2.class.getName());
        suite.addTest(new ArterialTreeTest2("test"));
        return suite;
    }

    private final boolean showPlot = true;
    public static final UniformSpan span = new UniformSpan(0, 10, 0.01);
    
    public void test() throws Exception
    {
        simulate(generateTestModel(new HemodynamicsModelSolver()), showPlot);
        simulate(generateOneWayModel(new HemodynamicsModelSolver()), showPlot);
        simulate(generateModel(new HemodynamicsModelSolver()), showPlot);
        
    }

    public static void main(String ... args) throws Exception
    {
        AgentBasedModel model4 = generateModel(new HemodynamicsModelSolver());
        double t = System.currentTimeMillis();
        new AgentModelSimulationEngine().simulate(model4);
        System.out.println("4:" + ( System.currentTimeMillis() - t ));
        AgentBasedModel model6 = generateModel(new HemodynamicsModelSolver());
        t = System.currentTimeMillis();
        new AgentModelSimulationEngine().simulate(model6);
        System.out.println("6:" + ( System.currentTimeMillis() - t ));

    }
    
    private void simulate(AgentBasedModel model, boolean showOnPlot) throws Exception
    {
        SimulationEngine engine =new AgentModelSimulationEngine();
        engine.needToShowPlot = showOnPlot;
        engine.simulate(model);
    }

//    private final static String modelFileName = ".//data//test//biouml//plugins//hemodynamics//t_correctAreasP40.txt";
    //    private final static String modelFileName = "C://Documents and Settings//axec//workspace//BioUML//data//test//biouml//plugins//hemodynamics//t.txt";

    private static AgentBasedModel generateModel(Simulator simulator) throws Exception
    {
        AgentBasedModel agentModel = new AgentBasedModel();

//        HemodynamicsOptions options = (HemodynamicsOptions)simulator.getOptions();
//        options.setNumberOfSegmentation(10);
//        options.setSegmentationForIntegration(10);
        ArterialBinaryTreeModel atm = null;/*TODO: fix HemodynamicsDiagramGenerator.loadFromFile(new File(modelFileName));*/
        ModelAgent arterialTree = new ModelAgent(atm, simulator, span, "artreialTree");
        ModelAgent solodyannikov = new ModelAgent(new Solodyannikov_without_arteries(), new EventLoopSimulator(), span, "Solodyannikov");

        PlotAgent plot = new PlotAgent("plot", span);

        agentModel.addAgent(arterialTree);
        agentModel.addAgent(solodyannikov);
        agentModel.addAgent(plot);

        solodyannikov.addVariable( "BloodFlow_VentricularToArterial", 39 );
        solodyannikov.addVariable( "Pressure_Venous", 41 );
        solodyannikov.addVariable( "Conductivity_Capillary", 18 );
        
        solodyannikov.addVariable( "Pressure_Arterial", 35 );
        solodyannikov.addVariable( "BloodFlow_Capillary", 50 );
        solodyannikov.addVariable( "Volume_Arterial", 8 );
        
        arterialTree.addVariable( "inputFlow", 3 );
        arterialTree.addVariable( "venousPressure", 7 );
        arterialTree.addVariable( "averagePressure", 8 );
        arterialTree.addVariable( "outputFlow", 4 );
        arterialTree.addVariable( "totalVolume", 10 );
        
        arterialTree.addVariable( "inputArea", 5 );
        arterialTree.addVariable( "outputPressure", 2 );
        arterialTree.addVariable( "inputPressure", 1 );

        agentModel.addDirectedLink(solodyannikov, "BloodFlow_VentricularToArterial", arterialTree, "inputFlow");
        agentModel.addDirectedLink(solodyannikov, "Pressure_Venous", arterialTree, "venousPressure");
        agentModel.addDirectedLink(solodyannikov, "Conductivity_Capillary", arterialTree, "filtrationCoefficient");

        agentModel.addDirectedLink(arterialTree, "averagePressure", solodyannikov, "Pressure_Arterial");
        agentModel.addDirectedLink(arterialTree, "outputFlow", solodyannikov, "BloodFlow_Capillary");
        agentModel.addDirectedLink(arterialTree, "totalVolume", solodyannikov, "Volume_Arterial");

        agentModel.addDirectedLink(arterialTree, "inputArea", plot, "inputArea");
        agentModel.addDirectedLink(arterialTree, "averagePressure", plot, "averagePressure");
        agentModel.addDirectedLink(arterialTree, "outputPressure", plot, "outputPressure");
        agentModel.addDirectedLink(arterialTree, "inputPressure", plot, "inputPressure");
        agentModel.addDirectedLink(arterialTree, "totalVolume", plot, "totalVolume");

        return agentModel;
    }


    private static AgentBasedModel generateTestModel(Simulator simulator) throws Exception
    {
        AgentBasedModel agentModel = new AgentBasedModel();

        HemodynamicsOptions options = (HemodynamicsOptions)simulator.getOptions();
        options.setTestMode(true);
//        options.setNumberOfSegmentation(10);
//        options.setSegmentationForIntegration(10);
//
        ArterialBinaryTreeModel  atm = null;/*TODO: fix HemodynamicsDiagramGenerator.loadFromFile(new File(modelFileName));*/
        ModelAgent arterialTree = new ModelAgent(atm, simulator, span, "artreialTree");

        PlotAgent plot = new PlotAgent("plot", span);

        agentModel.addAgent(arterialTree);
        agentModel.addAgent(plot);
        
        arterialTree.addVariable( "inputFlow", 3 );
        arterialTree.addVariable( "outputFlow", 4 );
        arterialTree.addVariable( "averagePressure", 8 );
        
        agentModel.addDirectedLink(arterialTree, "inputFlow", plot, "inputFlow");
        agentModel.addDirectedLink(arterialTree, "outputFlow", plot, "outputFlow");
        agentModel.addDirectedLink(arterialTree, "averagePressure", plot, "averagePressure");

        return agentModel;
    }

    private static AgentBasedModel generateOneWayModel(Simulator simulator) throws Exception
    {
        AgentBasedModel agentModel = new AgentBasedModel();

//        HemodynamicsOptions options = (HemodynamicsOptions)simulator.getOptions();
//        options.setNumberOfSegmentation(5);
//        options.setSegmentationForIntegration(5);
        ArterialBinaryTreeModel atm = null;/*TODO: fix HemodynamicsDiagramGenerator.loadFromFile(new File(modelFileName));*/
        ModelAgent arterialTree = new ModelAgent(atm, simulator, span, "artreialTree");

        ModelAgent solodyannikov = new ModelAgent(new Solodyannikov(), new EventLoopSimulator(), span, "Solodyannikov");
        
        PlotAgent plot = new PlotAgent("plot", span);
        PlotAgent plot2 = new PlotAgent("plot2", span);
        
        agentModel.addAgent(arterialTree);
        agentModel.addAgent(solodyannikov);
        agentModel.addAgent(plot);
        agentModel.addAgent(plot2);
        
        agentModel.addDirectedLink(solodyannikov, "BloodFlow_VentricularToArterial", arterialTree, "inputFlow");
//      agentModel.addDirectedLink(solodyannikov, "BloodFlow_Capillary", arterialTree, "outputFlow");
//      agentModel.addDirectedLink(solodyannikov, "Pressure_Arterial", plot2, "inputPressure");
//        agentModel.addDirectedLink(solodyannikov, "BloodFlow_VentricularToArterial", arterialTree, "inputFlow");
        agentModel.addDirectedLink(solodyannikov, "Pressure_Venous", arterialTree, "venousPressure");
        agentModel.addDirectedLink(solodyannikov, "Conductivity_Capillary", arterialTree, "filtrationCoefficient");
        

//        agentModel.addDirectedLink(solodyannikov, "Pressure_Arterial", arterialTree, "inputPressure");
//        agentModel.addDirectedLink(solodyannikov, "outputPressure", arterialTree, "outputPressure");
        
    agentModel.addDirectedLink(arterialTree, "inputPressure", plot, "inputPressure");
    agentModel.addDirectedLink(arterialTree, "outputPressure", plot, "outputPressure");
      
        agentModel.addDirectedLink(arterialTree, "inputArea", plot, "inputArea");
//
        agentModel.addDirectedLink(arterialTree, "averagePressure", plot2, "averagePressure");
        agentModel.addDirectedLink(arterialTree, "totalVolume", plot2, "totalVolume");
        agentModel.addDirectedLink(arterialTree, "outputFlow", plot2, "outputFlow");
        agentModel.addDirectedLink(arterialTree, "arterialConductivity", plot2, "arterialConductivity");
//
//        agentModel.addDirectedLink(solodyannikov, "Pressure_Arterial", plot2, "averagePressure_sol");
//        agentModel.addDirectedLink(solodyannikov, "2", plot2, "totalVolume_sol");
//        agentModel.addDirectedLink(solodyannikov, "BloodFlow_Capillary", plot2, "outputFlow_sol");
//        agentModel.addDirectedLink(solodyannikov, "Conductivity_Arterial", plot2, "arterialConductivity_sol");

        return agentModel;
    }
}