package biouml.plugins.agentmodeling._test;

import biouml.plugins.agentmodeling.AveragerAgent;
import biouml.plugins.agentmodeling.AgentBasedModel;
import biouml.plugins.agentmodeling.AgentModelSimulationEngine;
import biouml.plugins.agentmodeling.ModelAgent;
import biouml.plugins.agentmodeling.PlotAgent;
import biouml.plugins.agentmodeling._test.models.KaraaslanSodiumIntake;
import biouml.plugins.agentmodeling._test.models.Karaaslan_part;
import biouml.plugins.agentmodeling._test.models.Solodyannikov_without_arteries;
import biouml.plugins.hemodynamics.HemodynamicsModelSolver;
import biouml.plugins.hemodynamics.HemodynamicsOptions;
import biouml.plugins.simulation.UniformSpan;
import biouml.plugins.simulation.java.EventLoopSimulator;
import biouml.plugins.simulation.ode.jvode.JVodeOptions;

/**
 * Test for heart agent model consisting of 6 agents: ArterialSystem, VenousSystem, Ventricle, NeuroHumoralControlSystem, Capillary
 * @author axec
 *
 */
public class ComplexModelTest
{
    public static void main(String ... args) throws Exception
    {
        new AgentModelSimulationEngine().simulate(generateModel());
    }

    private final static String modelFileName = ".//data//test//biouml//plugins//hemodynamics//t.txt";

    private static AgentBasedModel generateModel() throws Exception
    {
        AgentBasedModel agentModel = new AgentBasedModel();

        EventLoopSimulator simulator = new EventLoopSimulator();
        JVodeOptions options = new JVodeOptions();
        options.setRtol(1E-7);
        simulator.setOptions(options);
        ModelAgent karaaslan = new ModelAgent(new Karaaslan_part(), simulator, new UniformSpan(0, 10000, 1), "Karaaslan");
        karaaslan.setTimeScale(60);

        ModelAgent sodiumIntake = new ModelAgent(new KaraaslanSodiumIntake(), new EventLoopSimulator(), new UniformSpan(0, 180, 1),
                "Sodium");
        sodiumIntake.setTimeScale(60);

        ModelAgent solodyannikov = new ModelAgent(new Solodyannikov_without_arteries(), new EventLoopSimulator(), new UniformSpan(0, 180,
                0.01), "Solodyannikov");

        HemodynamicsOptions hemoOptions = new HemodynamicsOptions();
//        hemoOptions.setNumberOfSegmentation(10);
//        hemoOptions.setSegmentationForIntegration(5);
//        hemoOptions.setTestMode(true);
        HemodynamicsModelSolver hemoSimulator = new HemodynamicsModelSolver();
        hemoSimulator.setOptions(hemoOptions);
        ModelAgent arterialTree = null;/*TODO: fix new ModelAgent(HemodynamicsDiagramGenerator.loadFromFile(new File(modelFileName)), hemoSimulator, new UniformSpan(0, 14400, 0.05), "artreialTree");*/

        AveragerAgent adapter = new AveragerAgent("adapter", new UniformSpan(0, 10800, 0.05), 1000);

        PlotAgent plotLong = new PlotAgent("plot", new UniformSpan(0, 10800, 10));
        
//        PlotAgent plotLong2 = new PlotAgent("plotLong2", new UniformSpan(0, 600000,0.1));
        
        PlotAgent plotShort1 = new PlotAgent("plot1", new UniformSpan(0, 110, 0.05));
        
        PlotAgent plotShort2 = new PlotAgent("plot2", new UniformSpan(0, 10, 0.05));
//
//        PlotAgent plotShort3 = new PlotAgent("plot3", new UniformSpan(7200, 10, 0.01));

        agentModel.addAgent(karaaslan);
        agentModel.addAgent(sodiumIntake);
        agentModel.addAgent(solodyannikov);
        agentModel.addAgent(arterialTree);
        agentModel.addAgent(plotLong);
        agentModel.addAgent(plotShort1);
        agentModel.addAgent(plotShort2);
//        agentModel.addAgent(plotShort3);
//        agentModel.addAgent(plotLong2);
//        agentModel.addAgent(plotShort);
        agentModel.addAgent(adapter);

        //        Sodium input  --> Kidney
        agentModel.addDirectedLink(sodiumIntake, "Fi_sodin", karaaslan, "Fi_sodin");

        //        Heart         --> Kidney
        agentModel.addDirectedLink(solodyannikov, "BloodFlow_VentricularToArterial", karaaslan, "Fi_co");
        agentModel.addDirectedLink(solodyannikov, "Pressure_Arterial", adapter, "Pressure_Arterial");
        agentModel.addDirectedLink(adapter, "Pressure_Arterial", karaaslan, "P_ma");

      agentModel.addDirectedLink(solodyannikov, "Cardiac_Output", karaaslan, "Fi_co");
      agentModel.addDirectedLink(solodyannikov, "Pressure_Arterial", adapter, "Pressure_Arterial");
      agentModel.addDirectedLink(adapter, "Pressure_Arterial", karaaslan, "P_ma");
        
//      agentModel.addDirectedLink(solodyannikov, "Cardiac_Output", plotLong2, "Fi_co");
//      agentModel.addDirectedLink(solodyannikov, "5", plotLong2, "min_out");
//      agentModel.addDirectedLink(solodyannikov, "BloodFlow_VentricularToArterial", plotLong2, "flow");
//      agentModel.addDirectedLink(adapter, "Pressure_Arterial", plotLong2, "P_ma");
      
      
        //Heart         --> Arterial Tree
        agentModel.addDirectedLink(solodyannikov, "BloodFlow_VentricularToArterial", arterialTree, "inputFlow");
        agentModel.addDirectedLink(solodyannikov, "Conductivity_Capillary", arterialTree, "filtrationCoefficient");
        agentModel.addDirectedLink(solodyannikov, "Pressure_Venous", arterialTree, "venousPressure");

        //Kidney        --> Heart
        agentModel.addDirectedLink(karaaslan, "V_large", solodyannikov, "Volume_Full");


        //arterial Tree --> Heart
        agentModel.addDirectedLink(arterialTree, "averagePressure", solodyannikov, "Pressure_Arterial");
        agentModel.addDirectedLink(arterialTree, "totalVolume", solodyannikov, "Volume_Arterial");
        agentModel.addDirectedLink(arterialTree, "arterialConductivity", solodyannikov, "Conductivity_Arterial");
        agentModel.addDirectedLink(arterialTree, "outputFlow", solodyannikov, "BloodFlow_Capillary");

        //links to plot
//        agentModel.addDirectedLink(solodyannikov, "Pressure_Arterial", plot, "Arterial pressure");
        agentModel.addDirectedLink(sodiumIntake, "Fi_sodin", plotLong, "Fi_sodin");
        agentModel.addDirectedLink(karaaslan, "rsna", plotLong, "rsna");
//        agentModel.addDirectedLink(karaaslan, "4", plot, "C_r");
//        agentModel.addDirectedLink(karaaslan, "0", plot, "C_r");
//        agentModel.addDirectedLink(karaaslan, "V_large", plot, "Full_Volume");
        agentModel.addDirectedLink(karaaslan, "P_ma", plotLong, "Mean arterial pressure");


                agentModel.addDirectedLink(arterialTree, "aortalOutputPressure", plotShort1, "Pressure_Aortal");
//                agentModel.addDirectedLink(arterialTree, "rRenalOutputPressure", plotShort1, "Pressure_Renal");
//                agentModel.addDirectedLink(arterialTree, "lastVesselPressure", plotShort1, "Pressure_Last");
                
//                agentModel.addDirectedLink(arterialTree, "outputFlow", plotShort2, "input_flow");
//                agentModel.addDirectedLink(arterialTree, "inputFlow", plotShort2, "output_flow");
                
//                agentModel.addDirectedLink(arterialTree, "aortalOutputPressure", plotShort2, "Pressure_Aortal");
//                agentModel.addDirectedLink(arterialTree, "rRenalOutputPressure", plotShort2, "Pressure_Renal");
                
//                agentModel.addDirectedLink(arterialTree, "aortalOutputPressure", plotShort3, "Pressure_Aortal");
//                agentModel.addDirectedLink(arterialTree, "rRenalOutputPressure", plotShort3, "Pressure_Renal");
        //        agentModel.addDirectedLink(arterialTree, "rRenalInputFlow", lot, "jh");
//              agentModel.addDirectedLink(arterialTree, "totalVolume", plotShort, "Volume_Arterial");
        //      agentModel.addDirectedLink(arterialTree, "arterialConductivity", solodyannikov, "Conductivity_Arterial");
        //              agentModel.addDirectedLink(arterialTree, "outputFlow", plot, "output");
        //              agentModel.addDirectedLink(arterialTree, "inputFlow", plot, "input");
        return agentModel;
    }
}