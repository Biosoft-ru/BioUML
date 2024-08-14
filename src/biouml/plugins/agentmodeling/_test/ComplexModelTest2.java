package biouml.plugins.agentmodeling._test;

import biouml.plugins.agentmodeling.AveragerAgent;
import biouml.plugins.agentmodeling.AgentBasedModel;
import biouml.plugins.agentmodeling.AgentModelSimulationEngine;
import biouml.plugins.agentmodeling.ModelAgent;
import biouml.plugins.agentmodeling.PlotAgent;
import biouml.plugins.agentmodeling._test.models.KaraaslanSodiumIntake;
import biouml.plugins.agentmodeling._test.models.Karaaslan_corrected;
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
public class ComplexModelTest2
{
    public static void main(String ... args) throws Exception
    {
        new AgentModelSimulationEngine().simulate(generateModel());
    }

    private final static String modelFileName = ".//data//test//biouml//plugins//hemodynamics//t_correctAreas2.txt";

    private static AgentBasedModel generateModel() throws Exception
    {
        AgentBasedModel agentModel = new AgentBasedModel();

        EventLoopSimulator simulator = new EventLoopSimulator();
        JVodeOptions options = new JVodeOptions();
        options.setRtol(1E-7);
        simulator.setOptions(options);
        ModelAgent karaaslan = new ModelAgent(new Karaaslan_corrected(), simulator, new UniformSpan(0, 10000, 1), "Karaaslan");
        karaaslan.setTimeScale(60);

        ModelAgent sodiumIntake = new ModelAgent(new KaraaslanSodiumIntake(), new EventLoopSimulator(), new UniformSpan(0, 180, 1),
                "Sodium");
        sodiumIntake.setTimeScale(60);


        HemodynamicsModelSolver hemoSimulator = new HemodynamicsModelSolver();
        HemodynamicsOptions optionsH = (HemodynamicsOptions)hemoSimulator.getOptions();
        optionsH.setTestMode(false);
//        optionsH.setNumberOfSegmentation(5);
//        optionsH.setSegmentationForIntegration(5);
        optionsH.setInputCondition(HemodynamicsOptions.FLUX_INITIAL_CONDITION_STRING);//.F.PRESSURE_INITIAL_CONDITION);
        optionsH.setOutputCondition(HemodynamicsOptions.FILTRATION_INITIAL_CONDITION_STRING);//.PRESSURE_INITIAL_CONDITION);
        ModelAgent arterialTree = null;/*TODO: fix new ModelAgent(HemodynamicsDiagramGenerator.loadFromFile(new File(modelFileName)), hemoSimulator, new UniformSpan(0,
                10, 0.02), "artreialTree");*/
        ModelAgent solodyannikov = new ModelAgent(new Solodyannikov_without_arteries(), new EventLoopSimulator(), new UniformSpan(0, 10,
                0.02), "Solodyannikov");

        AveragerAgent adapter = new AveragerAgent("adapter", new UniformSpan(0, 10800, 0.01), 1000);

        PlotAgent plotLong = new PlotAgent("plot", new UniformSpan(0, 10800, 10));
        
//        PlotAgent plotLong2 = new PlotAgent("plotLong2", new UniformSpan(0, 600000,0.1));
        
        PlotAgent plotShort1 = new PlotAgent("plot1", new UniformSpan(0, 110, 0.02));
         
        PlotAgent plotShort2 = new PlotAgent("plot2", new UniformSpan(0, 110, 0.02));
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
//        agentModel.addDirectedLink(solodyannikov, "BloodFlow_VentricularToArterial", karaaslan, "10");
//        agentModel.addDirectedLink(solodyannikov, "Pressure_Arterial", adapter, "Pressure_Arterial");
//        agentModel.addDirectedLink(adapter, "Pressure_Arterial", karaaslan, "P_ma");

        
        
      agentModel.addDirectedLink(solodyannikov, "Cardiac_Output", karaaslan, "10");
      agentModel.addDirectedLink(solodyannikov, "Pressure_Arterial", adapter, "Pressure_Arterial");
      agentModel.addDirectedLink(adapter, "Pressure_Arterial", karaaslan, "P_ma");
        
        
        
//      agentModel.addDirectedLink(solodyannikov, "Cardiac_Output", plotLong2, "Fi_co");
//      agentModel.addDirectedLink(solodyannikov, "5", plotLong2, "min_out");
//      agentModel.addDirectedLink(solodyannikov, "BloodFlow_VentricularToArterial", plotLong2, "flow");
//      agentModel.addDirectedLink(adapter, "Pressure_Arterial", plotLong2, "P_ma");
      
        agentModel.addDirectedLink(arterialTree, "aortalOutputPressure", plotShort1, "Pressure_Aortal");
        agentModel.addDirectedLink(arterialTree, "rRenalOutputPressure", plotShort1, "Pressure_Renal");
        agentModel.addDirectedLink(arterialTree, "lastVesselPressure", plotShort1, "Pressure_Last");
//
        agentModel.addDirectedLink(arterialTree, "aortalOutputPressure", plotShort2, "Pressure_Aortal");
        agentModel.addDirectedLink(arterialTree, "inputPressure", plotShort2, "inputPressure");
        agentModel.addDirectedLink(arterialTree, "totalVolume", plotShort2, "totalVolume");
        
        
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
//        agentModel.addDirectedLink(karaaslan, "rsna", plotLong, "rsna");
//        agentModel.addDirectedLink(karaaslan, "4", plot, "C_r");
//        agentModel.addDirectedLink(karaaslan, "0", plot, "C_r");
//        agentModel.addDirectedLink(karaaslan, "V_large", plot, "Full_Volume");
        agentModel.addDirectedLink(karaaslan, "P_ma", plotLong, "Mean arterial pressure");



                
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