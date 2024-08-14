package biouml.plugins.agentmodeling._test;

import biouml.plugins.agentmodeling.AgentBasedModel;
import biouml.plugins.agentmodeling.AgentModelSimulationEngine;
import biouml.plugins.agentmodeling.AveragerAgent;
import biouml.plugins.agentmodeling.ModelAgent;
import biouml.plugins.agentmodeling.PlotAgent;
import biouml.plugins.agentmodeling._test.models.Karaaslan_part2;
import biouml.plugins.agentmodeling._test.models.Solodyannikov_without_arteries;
import biouml.plugins.hemodynamics.HemodynamicsModelSolver;
import biouml.plugins.hemodynamics.HemodynamicsOptions;
import biouml.plugins.simulation.UniformSpan;
import biouml.plugins.simulation.java.EventLoopSimulator;
import biouml.plugins.simulation.ode.jvode.JVodeOptions;

/**
 * Test for heart agent model consisting of 6 agents: ArterialSystem, VenousSystem, Ventricle, NeuroHumoralControlSystem, Capillary
 * @author vibis
 *
 */
public class ComplexModelTest_Boris1
{
    public static void main(String... args) throws Exception
    {
        new AgentModelSimulationEngine().simulate(generateModel());
    }

    private final static String modelFileName = ".//data//test//biouml//plugins//hemodynamics//t_correctAreasP40.txt";

    private static AgentBasedModel generateModel() throws Exception
    {
        AgentBasedModel agentModel = new AgentBasedModel();

        EventLoopSimulator simulator = new EventLoopSimulator();
        JVodeOptions options = new JVodeOptions();
        options.setRtol(1E-7);
        simulator.setOptions(options);
        ModelAgent karaaslan = new ModelAgent(new Karaaslan_part2(), simulator, new UniformSpan(0, 200, 1.0), "Karaaslan");
        karaaslan.setTimeScale(60);

//        ModelAgent sodiumIntake = new ModelAgent(new KaraaslanSodiumIntake(), new EventLoopSimulator(), new UniformSpan(0, 180, 0.02),
//                "Sodium");
//        sodiumIntake.setTimeScale(60);

        ModelAgent solodyannikov = new ModelAgent(new Solodyannikov_without_arteries(), new EventLoopSimulator(), new UniformSpan(0, 12000,
                0.01), "Solodyannikov");

        HemodynamicsOptions hemoOptions = new HemodynamicsOptions();
//        hemoOptions.setTestMode(true);
        HemodynamicsModelSolver hemoSimulator = new HemodynamicsModelSolver();
        hemoSimulator.setOptions(hemoOptions);
        ModelAgent arterialTree = null;/*TODO: fix new ModelAgent(HemodynamicsDiagramGenerator.loadFromFile(new File(modelFileName)), hemoSimulator, new UniformSpan(0, 12000, 0.01), "artreialTree");*/

        AveragerAgent adapter = new AveragerAgent("adapter", new UniformSpan(0, 12000, 0.01), 6000);
        AveragerAgent adapter2 = new AveragerAgent("adapter2", new UniformSpan(0, 12000, 0.01), 6000);
        AveragerAgent adapter3 = new AveragerAgent("adapter3", new UniformSpan(0, 12000, 0.01), 6000);

        PlotAgent plotLong_1 = new PlotAgent("plotLong_1", new UniformSpan(0, 12000, 60));
        PlotAgent plotLong_2 = new PlotAgent("plotLong_2", new UniformSpan(0, 12000, 60));
        PlotAgent plotLong_3 = new PlotAgent("plotLong_3", new UniformSpan(0, 12000, 60));
        PlotAgent plotLong_4 = new PlotAgent("plotLong_4", new UniformSpan(0, 12000, 60));
        PlotAgent plotLong_5 = new PlotAgent("plotLong_5", new UniformSpan(0, 12000, 60));
        
        //first control point 3-4 min
        PlotAgent plotShort1_1 = new PlotAgent("plot1_pres", new UniformSpan(0, 240, 0.01));
        PlotAgent plotShort1_2 = new PlotAgent("plot1_renpres", new UniformSpan(0, 240, 0.01));

        
        PlotAgent plotShort1_3 = new PlotAgent("plot1_renflow", new UniformSpan(0, 240, 0.01));

        
        PlotAgent plotShort1_4 = new PlotAgent("plot1_humoral", new UniformSpan(0, 240, 0.01));
        
        //second control point 25-26 min
        PlotAgent plotShort2_1 = new PlotAgent("plot2_pres", new UniformSpan(1500, 1560, 0.01));
        
        PlotAgent plotShort2_2 = new PlotAgent("plot2_renpres", new UniformSpan(1500, 1560, 0.01));

        PlotAgent plotShort2_3 = new PlotAgent("plot2_renflow", new UniformSpan(1500, 1560, 0.01));

        PlotAgent plotShort2_4 = new PlotAgent("plot2_humoral", new UniformSpan(1500, 1560, 0.01));
        
        
        //third control point 45-46 min
        PlotAgent plotShort3_1 = new PlotAgent("plot3_pres", new UniformSpan(2700, 2760, 0.01));
        
        PlotAgent plotShort3_2 = new PlotAgent("plot3_renpres", new UniformSpan(2700, 2760, 0.01));
        
        PlotAgent plotShort3_3 = new PlotAgent("plot3_renflow", new UniformSpan(2700, 2760, 0.01));
        
        PlotAgent plotShort3_4 = new PlotAgent("plot3_humoral", new UniformSpan(2700, 2760, 0.01));
        
        //third control point 66-67 min
        PlotAgent plotShort4_1 = new PlotAgent("plot4_pres", new UniformSpan(3960, 4020, 0.01));
        
        PlotAgent plotShort4_2 = new PlotAgent("plot4_renpres", new UniformSpan(3960, 4020, 0.01));
        
        PlotAgent plotShort4_3 = new PlotAgent("plot4_renflow", new UniformSpan(3960, 4020, 0.01));
        PlotAgent plotShort4_4 = new PlotAgent("plot4_humoral", new UniformSpan(3960, 4020, 0.01));

        //third control point 100-101 min
        PlotAgent plotShort5_1 = new PlotAgent("plot4_pres", new UniformSpan(6000, 6060, 0.01));

        PlotAgent plotShort5_2 = new PlotAgent("plot4_renpres", new UniformSpan(6000, 6060, 0.01));

        PlotAgent plotShort5_3 = new PlotAgent("plot4_renflow", new UniformSpan(6000, 6060, 0.01));

        
        PlotAgent plotShort5_4 = new PlotAgent("plot4_humoral", new UniformSpan(6000, 6060, 0.01));

        agentModel.addAgent(karaaslan);
//        agentModel.addAgent(sodiumIntake);
        agentModel.addAgent(solodyannikov);
        agentModel.addAgent(arterialTree);
        agentModel.addAgent(plotLong_1);
        agentModel.addAgent(plotLong_2);
        agentModel.addAgent(plotLong_3);
        agentModel.addAgent(plotLong_4);
        agentModel.addAgent(plotLong_5);
        agentModel.addAgent(plotShort1_1);
        agentModel.addAgent(plotShort1_2);
        agentModel.addAgent(plotShort1_3);
        agentModel.addAgent(plotShort1_4);
        agentModel.addAgent(plotShort2_1);
        agentModel.addAgent(plotShort2_2);
        agentModel.addAgent(plotShort2_3);
        agentModel.addAgent(plotShort2_4);
        agentModel.addAgent(plotShort3_1);
        agentModel.addAgent(plotShort3_2);
        agentModel.addAgent(plotShort3_3);
        agentModel.addAgent(plotShort3_4);
        agentModel.addAgent(plotShort4_1);
        agentModel.addAgent(plotShort4_2);
        agentModel.addAgent(plotShort4_3);
        agentModel.addAgent(plotShort4_4);
        agentModel.addAgent(plotShort5_1);
        agentModel.addAgent(plotShort5_2);
        agentModel.addAgent(plotShort5_3);
        agentModel.addAgent(plotShort5_4);
   
        
        
        
//        agentModel.addAgent(plotShort6);
//        agentModel.addAgent(plotShort7);
//        agentModel.addAgent(plotShort8);
//        agentModel.addAgent(plotShort9);
//        agentModel.addAgent(plotShort10);
//        agentModel.addAgent(plotShort11);
//        agentModel.addAgent(plotShort12);

//        agentModel.addAgent(plotLong2);
//        agentModel.addAgent(plotShort);
        agentModel.addAgent(adapter);
        agentModel.addAgent(adapter2);
        agentModel.addAgent(adapter3);

        //        Sodium input  --> Kidney
//        agentModel.addDirectedLink(sodiumIntake, "Fi_sodin", karaaslan, "Fi_sodin");

        //        Heart         --> Kidney
//      agentModel.addDirectedLink(solodyannikov, "Cardiac_Output", karaaslan, "Fi_co");
      agentModel.addDirectedLink(arterialTree, "averagePressure", adapter, "Pressure_Arterial");
      agentModel.addDirectedLink(adapter, "Pressure_Arterial", karaaslan, "P_ma");
        
//      agentModel.addDirectedLink(solodyannikov, "Cardiac_Output", plotLong2, "Fi_co");
//      agentModel.addDirectedLink(solodyannikov, "5", plotLong2, "min_out");
//      agentModel.addDirectedLink(solodyannikov, "BloodFlow_VentricularToArterial", plotLong2, "flow");
//      agentModel.addDirectedLink(adapter, "Pressure_Arterial", plotLong2, "P_ma");
      
      
        //Heart         --> Arterial Tree
        agentModel.addDirectedLink(solodyannikov, "BloodFlow_VentricularToArterial", arterialTree, "inputFlow");
        agentModel.addDirectedLink(solodyannikov, "Conductivity_Capillary", arterialTree, "filtrationCoefficient");
        agentModel.addDirectedLink(solodyannikov, "Pressure_Venous", arterialTree, "venousPressure");
        agentModel.addDirectedLink(solodyannikov, "Elasticity_Arterial", arterialTree, "arteriaElasticity");
        agentModel.addDirectedLink(solodyannikov, "4", arterialTree, "humoralFactor");
        agentModel.addDirectedLink(solodyannikov, "Stage_Sistole", arterialTree, "sistoleStage");
//        agentModel.addDirectedLink(solodyannikov, "BloodFlow_Capillary", arterialTree, "outputFlow")
//        agentModel.addDirectedLink(solodyannikov, "Pressure_Arterial", arterialTree, "inputPressure");

        //Kidney        --> Heart
        agentModel.addDirectedLink(karaaslan, "V_large", solodyannikov, "Volume_Full");
        agentModel.addDirectedLink(karaaslan, "V_b", arterialTree, "averageTotalVolume");
//        agentModel.addDirectedLink(karaaslan, "Fi_rb", arterialTree, "kidneyInputFlow");
//        agentModel.addDirectedLink(karaaslan, "Fi_co", arterialTree, "averageInputFlow");
        agentModel.addDirectedLink(karaaslan, "R_r", arterialTree, "kidneyResistance");
//        agentModel.addDirectedLink(karaaslan, "P_gh", arterialTree, "kidneyPressurePgh");
        agentModel.addDirectedLink(arterialTree, "renalConductivity", adapter3, "Renal_cond");
        agentModel.addDirectedLink(adapter3, "Renal_cond", karaaslan, "R_aass");
//        agentModel.addDirectedLink(arterialTree, "kidneyInputFlow", karaaslan, "Fi_rb");
        agentModel.addDirectedLink(arterialTree, "kidneyInputFlow", adapter2, "Kidney_Flow");
        agentModel.addDirectedLink(adapter2, "Kidney_Flow", karaaslan, "Fi_rb");
        agentModel.addDirectedLink(karaaslan, "6", arterialTree, "vascularity");
        agentModel.addDirectedLink(karaaslan, "eps_aum", arterialTree, "nueroReceptorsControl");


        //arterial Tree --> Heart
        agentModel.addDirectedLink(arterialTree, "averagePressure", solodyannikov, "Pressure_Arterial");
        agentModel.addDirectedLink(arterialTree, "totalVolume", solodyannikov, "Volume_Arterial");
        agentModel.addDirectedLink(arterialTree, "arterialConductivity", solodyannikov, "Conductivity_Arterial");
        agentModel.addDirectedLink(arterialTree, "outputFlow", solodyannikov, "BloodFlow_Capillary");

        //links to plot
//        agentModel.addDirectedLink(solodyannikov, "Pressure_Arterial", plotShort1, "Arterial pressure");
        agentModel.addDirectedLink(karaaslan, "R_r", plotLong_1, "R_r");
//        agentModel.addDirectedLink(karaaslan, "Fi_rb", plotLong_2, "Fi_rb");
//        agentModel.addDirectedLink(karaaslan, "4", plot, "C_r");
//        agentModel.addDirectedLink(karaaslan, "0", plot, "C_r");
//        agentModel.addDirectedLink(karaaslan, "V_large", plot, "Full_Volume");
        agentModel.addDirectedLink(karaaslan, "P_ma", plotLong_2, "Mean arterial pressure");
        agentModel.addDirectedLink(karaaslan, "V_b", plotLong_3, "V_b");
        
        agentModel.addDirectedLink(karaaslan, "P_gh", plotLong_4, "Pressure_in_kidney");
        agentModel.addDirectedLink(karaaslan, "Fi_rb", plotLong_5, "Renal_blood_flow");



                agentModel.addDirectedLink(arterialTree, "aortalOutputPressure", plotShort1_1, "Pressure_Aortal_1");
                agentModel.addDirectedLink(arterialTree, "lastVesselPressure", plotShort1_1, "lastVessel_1");
                agentModel.addDirectedLink(arterialTree, "carotidRightOutputPressure", plotShort1_1, "Carotid_1");
                
                agentModel.addDirectedLink(arterialTree, "rRenalOutputPressure", plotShort1_2, "Vel_1End");
                agentModel.addDirectedLink(arterialTree, "lRenalOutputPressure", plotShort1_2, "Vel_3Begin");
                agentModel.addDirectedLink(arterialTree, "lRenalInputFlow", plotShort1_2, "Vel_3End");
                
//                agentModel.addDirectedLink(arterialTree, "outputFlow", plotShort1_3, "outputFlow");
                agentModel.addDirectedLink(arterialTree, "averagePressure", plotShort1_3, "average_Pressure");
                
                agentModel.addDirectedLink(arterialTree, "betaCof", plotShort1_4, "beta_cof");
//                agentModel.addDirectedLink(solodyannikov, "Elasticity_Arterial", plotShort1_4, "El_Solodyan");
                
                
                agentModel.addDirectedLink(arterialTree, "aortalOutputPressure", plotShort2_1, "Pressure_Aortal_2");
                agentModel.addDirectedLink(arterialTree, "lastVesselPressure", plotShort2_1, "lastVessel_2");
                agentModel.addDirectedLink(arterialTree, "carotidRightOutputPressure", plotShort2_1, "Carotid_2");
                
                agentModel.addDirectedLink(arterialTree, "rRenalOutputPressure", plotShort2_2, "rRenalPressure_2");
                agentModel.addDirectedLink(arterialTree, "lRenalOutputPressure", plotShort2_2, "lRenalPressure_2");
                
                agentModel.addDirectedLink(arterialTree, "rRenalOutputFlow", plotShort2_3, "rRenalFlow_2");
                agentModel.addDirectedLink(arterialTree, "lRenalOutputFlow", plotShort2_3, "lRenalFlow_2");
                
                agentModel.addDirectedLink(solodyannikov, "4", plotShort2_4, "humoralFactor_2");
                
                
                
                agentModel.addDirectedLink(arterialTree, "aortalOutputPressure", plotShort3_1, "Pressure_Aortal_3");
                agentModel.addDirectedLink(arterialTree, "lastVesselPressure", plotShort3_1, "lastVessel_3");
                agentModel.addDirectedLink(arterialTree, "carotidRightOutputPressure", plotShort3_1, "Carotid_3");
                
                agentModel.addDirectedLink(arterialTree, "rRenalOutputPressure", plotShort3_2, "rRenalPressure_3");
                agentModel.addDirectedLink(arterialTree, "lRenalOutputPressure", plotShort3_2, "lRenalPressure_3");
                
                agentModel.addDirectedLink(arterialTree, "rRenalOutputFlow", plotShort3_3, "rRenalFlow_3");
                agentModel.addDirectedLink(arterialTree, "lRenalOutputFlow", plotShort3_3, "lRenalFlow_3");
                
                agentModel.addDirectedLink(solodyannikov, "4", plotShort3_4, "humoralFactor_3");
                
                
                
                agentModel.addDirectedLink(arterialTree, "aortalOutputPressure", plotShort4_1, "Pressure_Aortal_4");
                agentModel.addDirectedLink(arterialTree, "lastVesselPressure", plotShort4_1, "lastVessel_4");
                agentModel.addDirectedLink(arterialTree, "carotidRightOutputPressure", plotShort4_1, "Carotid_4");
                
                agentModel.addDirectedLink(arterialTree, "rRenalOutputPressure", plotShort4_2, "rRenalPressure_4");
                agentModel.addDirectedLink(arterialTree, "lRenalOutputPressure", plotShort4_2, "lRenalPressure_4");
                
                agentModel.addDirectedLink(arterialTree, "rRenalOutputFlow", plotShort4_3, "rRenalFlow_4");
                agentModel.addDirectedLink(arterialTree, "lRenalOutputFlow", plotShort4_3, "lRenalFlow_4");
                
                agentModel.addDirectedLink(solodyannikov, "4", plotShort4_4, "humoralFactor_4");
                
                
                
                agentModel.addDirectedLink(arterialTree, "aortalOutputPressure", plotShort5_1, "Pressure_Aortal_5");
                agentModel.addDirectedLink(arterialTree, "lastVesselPressure", plotShort5_1, "lastVessel_5");
                agentModel.addDirectedLink(arterialTree, "carotidRightOutputPressure", plotShort5_1, "Carotid_5");
                
                agentModel.addDirectedLink(arterialTree, "rRenalOutputPressure", plotShort5_2, "rRenalPressure_5");
                agentModel.addDirectedLink(arterialTree, "lRenalOutputPressure", plotShort5_2, "lRenalPressure_5");
                
                agentModel.addDirectedLink(arterialTree, "rRenalOutputFlow", plotShort5_3, "rRenalFlow_5");
                agentModel.addDirectedLink(arterialTree, "lRenalOutputFlow", plotShort5_3, "lRenalFlow_5");
                
                agentModel.addDirectedLink(solodyannikov, "4", plotShort5_4, "humoralFactor_5");
                
                      
        return agentModel;
    }
}