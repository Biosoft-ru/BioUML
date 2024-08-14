package biouml.plugins.agentmodeling._test;

import biouml.plugins.agentmodeling.AgentBasedModel;
import biouml.plugins.agentmodeling.AgentModelSimulationEngine;
import biouml.plugins.agentmodeling.ModelAgent;
import biouml.plugins.agentmodeling.PlotAgent;
import biouml.plugins.agentmodeling._test.models.ArterialSystem;
import biouml.plugins.agentmodeling._test.models.CapillarySystem;
import biouml.plugins.agentmodeling._test.models.NeuroHumoralControlSystem;
import biouml.plugins.agentmodeling._test.models.TissueMetabolismSystem;
import biouml.plugins.agentmodeling._test.models.VenousSystem;
import biouml.plugins.agentmodeling._test.models.Ventricular;
import biouml.plugins.simulation.Span;
import biouml.plugins.simulation.UniformSpan;
import biouml.plugins.simulation.java.EventLoopSimulator;

/**
 * Test for heart agent model consisting of 6 agents: ArterialSystem, VenousSystem, Ventricle, NeuroHumoralControlSystem, Capillary
 * @author axec
 *
 */
public class HeartAgentModelTest// extends TestCase
{
    static Span span = new UniformSpan(0, 10, 1E-2);

//    public HeartAgentModelTest(String name)
//    {
//        super(name);
//    }

//
//    public static TestSuite suite()
//    {
//        TestSuite suite = new TestSuite(HeartAgentModelTest.class.getName());
//        suite.addTest(new HeartAgentModelTest("test"));
//        return suite;
//    }

    public static void main(String... args) throws Exception
    {
        new AgentModelSimulationEngine().simulate(generateModel());
    }

    private static AgentBasedModel generateModel() throws Exception
    {

        AgentBasedModel agentModel = new AgentBasedModel();
        ModelAgent arterial = new ModelAgent(new ArterialSystem(), new EventLoopSimulator(), span, "arterial");
        ModelAgent venous = new ModelAgent(new VenousSystem(), new EventLoopSimulator(), span, "venous");
        ModelAgent capillary = new ModelAgent(new CapillarySystem(), new EventLoopSimulator(), span, "capillary");
        ModelAgent humoral = new ModelAgent(new NeuroHumoralControlSystem(), new EventLoopSimulator(), span, "humoral");
        ModelAgent tissue = new ModelAgent(new TissueMetabolismSystem(), new EventLoopSimulator(), span, "tissue");
        ModelAgent ventricle = new ModelAgent(new Ventricular(), new EventLoopSimulator(), span, "ventricle");
        
//        ModelAgent arterialTree = new ModelAgent(generateArterialTreeModel() , new HemodynamicsModelSolver2(), new UniformSpan(0, 10, 1E-2), "artreialTree");

        PlotAgent plot = new PlotAgent("plot", new UniformSpan(0, 100, 0.001));
        plot.setTimeIncrement(0.001);
        
        agentModel.addAgent(arterial);
        agentModel.addAgent(venous);
        agentModel.addAgent(capillary);
        agentModel.addAgent(humoral);
        agentModel.addAgent(tissue);
        agentModel.addAgent(ventricle);
        agentModel.addAgent(plot);
//        agentModel.addAgent(arterialTree);
        
        
//        agentModel.addDirectedLink(ventricle, "BloodFlow_VentricularToArterial", arterialTree, "inputFlow");
//        agentModel.addDirectedLink(capillary, "BloodFlow_Capillary", arterialTree, "outputFlow");
//
//        agentModel.addDirectedLink(arterialTree, "arteriaResistance", arterial, "Elasticity_Arterial_0");
//        agentModel.addDirectedLink(arterialTree, "totalVolume", arterial, "Volume_Arterial_N0");
        
        agentModel.addDirectedLink(arterial, "Pressure_Arterial", ventricle, "Pressure_Arterial");
        agentModel.addDirectedLink(arterial, "Pressure_Arterial", capillary, "Pressure_Arterial");
        agentModel.addDirectedLink(arterial, "Pressure_Arterial", humoral, "Pressure_Arterial");
        agentModel.addDirectedLink(arterial, "Pressure_Arterial", plot, "Pressure_Arterial");
        agentModel.addDirectedLink(arterial, "0", humoral, "Volume_Arterial");
        agentModel.addDirectedLink(arterial, "0", venous, "Volume_Arterial");
       
        agentModel.addDirectedLink(venous, "Pressure_Venous", ventricle, "Pressure_Venous");
        agentModel.addDirectedLink(venous, "Pressure_Venous", capillary, "Pressure_Venous");
        agentModel.addDirectedLink(venous, "Conductivity_Venous", ventricle, "Conductivity_Venous");
//        agentModel.addDirectedLink(venous, "Pressure_Venous", plot, "Pressure_Venous");
//        agentModel.addDirectedLink(venous, "Volume_Arterial", plot, "Volume_Arterial");
        agentModel.addDirectedLink(tissue, "1", capillary, "Oxygen_Debt");

        agentModel.addDirectedLink(capillary, "BloodFlow_Capillary", arterial, "BloodFlow_Capillary");
        agentModel.addDirectedLink(capillary, "BloodFlow_Capillary", tissue, "BloodFlow_Capillary");
        agentModel.addDirectedLink(capillary, "BloodFlow_Capillary", humoral, "BloodFlow_Capillary");
        agentModel.addDirectedLink(capillary, "BloodFlow_Capillary", plot, "BloodFlow_Capillary");

        agentModel.addDirectedLink(humoral, "0", ventricle, "Humoral");
        agentModel.addDirectedLink(humoral, "0", arterial, "Humoral");
        agentModel.addDirectedLink(humoral, "0", venous, "Humoral");
        agentModel.addDirectedLink(humoral, "0", capillary, "Humoral");

        agentModel.addDirectedLink(ventricle, "BloodFlow_VentricularToArterial", arterial, "BloodFlow_VentricularToArterial");
        agentModel.addDirectedLink(ventricle, "BloodFlow_VentricularToArterial", plot, "BloodFlow_VentricularToArterial");
        agentModel.addDirectedLink(ventricle, "1", venous, "Volume_Ventricular");
        return agentModel;
    }
}