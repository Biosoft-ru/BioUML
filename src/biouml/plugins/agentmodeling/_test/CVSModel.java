package biouml.plugins.agentmodeling._test;

import biouml.plugins.agentmodeling.AveragerAgent;
import biouml.plugins.agentmodeling.AgentBasedModel;
import biouml.plugins.agentmodeling.AgentModelSimulationEngine;
import biouml.plugins.agentmodeling.ModelAgent;
import biouml.plugins.agentmodeling.PlotAgent;
import biouml.plugins.agentmodeling._test.models.KaraaslanSodiumIntake;
import biouml.plugins.agentmodeling._test.models.Karaaslan_corrected_part;
import biouml.plugins.agentmodeling._test.models.Solodyannikov_without_arteries;
import biouml.plugins.hemodynamics.HemodynamicsModelSolver;
import biouml.plugins.hemodynamics.HemodynamicsOptions;
import biouml.plugins.simulation.Span;
import biouml.plugins.simulation.UniformSpan;
import biouml.plugins.simulation.java.EventLoopSimulator;

/**
 * Test for heart agent model consisting of 6 agents: ArterialSystem, VenousSystem, Ventricle, NeuroHumoralControlSystem, Capillary
 * @author vibis
 *
 */
public class CVSModel
{

    static Span minuteSpan = new UniformSpan(0, 18280, 1);
    static Span hourSpan = new UniformSpan(0, 288, 1);
    static Span secondsSpan = new UniformSpan(0, 18280 * 60, 0.01);


    public static void main(String ... args) throws Exception
    {
        new AgentModelSimulationEngine().simulate(generateModel());
    }

    private final static String modelFileName = ".//data//test//biouml//plugins//hemodynamics//t_correctAreasP40.txt";

    private static AgentBasedModel generateModel() throws Exception
    {
        AgentBasedModel agentModel = new AgentBasedModel();

        ModelAgent karaaslan = new ModelAgent(new Karaaslan_corrected_part(), new EventLoopSimulator(), minuteSpan, "Karaaslan");
        karaaslan.setTimeScale(60);

        ModelAgent sodiumIntake = new ModelAgent(new KaraaslanSodiumIntake(), new EventLoopSimulator(), minuteSpan, "Sodium");
        sodiumIntake.setTimeScale(60);

        ModelAgent solodyannikov = new ModelAgent(new Solodyannikov_without_arteries(), new EventLoopSimulator(), secondsSpan,
                "Solodyannikov");

        //        HemodynamicsOptions hemoOptions = new HemodynamicsOptions();
        //        hemoOptions.setTestMode(true);
        HemodynamicsModelSolver hemoSimulator = new HemodynamicsModelSolver();
        HemodynamicsOptions optionsH = (HemodynamicsOptions)hemoSimulator.getOptions();
        optionsH.setTestMode(false);
        optionsH.setInputCondition(HemodynamicsOptions.FLUX_INITIAL_CONDITION_STRING);//.F.PRESSURE_INITIAL_CONDITION);
        optionsH.setOutputCondition(HemodynamicsOptions.FILTRATION_INITIAL_CONDITION_STRING);//.PRESSURE_INITIAL_CONDITION);
        ModelAgent arterialTree = null;/*TODO: fix new ModelAgent(HemodynamicsDiagramGenerator.loadFromFile(new File(modelFileName)), hemoSimulator, secondsSpan,
                "artreialTree");*/
        AveragerAgent adapter = new AveragerAgent("adapter", secondsSpan, 6000);
        AveragerAgent adapter2 = new AveragerAgent("adapter2", secondsSpan, 6000);



        //first control point 3-4 min
        PlotAgent plotShort1_1 = new PlotAgent("plot1_f", new UniformSpan(0, 120, 0.01));


        PlotAgent plotShort1_2 = new PlotAgent("plot2_f", new UniformSpan(500, 550, 0.01));


        PlotAgent plotShort1_3 = new PlotAgent("plot3_f", new UniformSpan(6000, 6050, 0.01));


        PlotAgent plotShort1_4 = new PlotAgent("plot4_f", new UniformSpan(10000, 10050, 0.01));

        PlotAgent plotLong = new PlotAgent("plotLong", new UniformSpan(0, 18280, 100));
        plotLong.setTimeScale(60);


        agentModel.addAgent(karaaslan);
        agentModel.addAgent(sodiumIntake);
        agentModel.addAgent(solodyannikov);
        agentModel.addAgent(arterialTree);
        agentModel.addAgent(plotShort1_1);
        agentModel.addAgent(plotShort1_2);
        agentModel.addAgent(plotShort1_3);
        agentModel.addAgent(plotShort1_4);



        //        agentModel.addAgent(plotShort6);
        //        agentModel.addAgent(plotShort7);
        //        agentModel.addAgent(plotShort8);
        //        agentModel.addAgent(plotShort9);
        //        agentModel.addAgent(plotShort10);
        //        agentModel.addAgent(plotShort11);
        //        agentModel.addAgent(plotShort12);

        agentModel.addAgent(plotLong);
        //        agentModel.addAgent(plotShort);
        agentModel.addAgent(adapter);
        agentModel.addAgent(adapter2);

        //        Sodium input  --> Kidney
        agentModel.addDirectedLink(sodiumIntake, "Fi_sodin", karaaslan, "Fi_sodin");

        //        Heart         --> Kidney
        agentModel.addDirectedLink(solodyannikov, "CardiacOutput_Minute", adapter2, "Fi_co");
        agentModel.addDirectedLink(adapter2, "Fi_co", karaaslan, "Fi_co");

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

        //Kidney        --> Heart
        agentModel.addDirectedLink(karaaslan, "V_large", solodyannikov, "Volume_Full");

        agentModel.addDirectedLink(karaaslan, "R_r", arterialTree, "kidneyResistance");

        agentModel.addDirectedLink(arterialTree, "renalConductivity", karaaslan, "R_aass");

        agentModel.addDirectedLink(arterialTree, "kidneyInputFlow", adapter2, "Kidney_Flow");
        agentModel.addDirectedLink(adapter2, "Kidney_Flow", karaaslan, "Fi_rb");


        //arterial Tree --> Heart
        agentModel.addDirectedLink(arterialTree, "averagePressure", solodyannikov, "Pressure_Arterial");
        agentModel.addDirectedLink(arterialTree, "totalVolume", solodyannikov, "Volume_Arterial");
        agentModel.addDirectedLink(arterialTree, "arterialConductivity", solodyannikov, "Conductivity_Arterial");
        agentModel.addDirectedLink(arterialTree, "outputFlow", solodyannikov, "BloodFlow_Capillary");



        agentModel.addDirectedLink(arterialTree, "aortalOutputPressure", plotShort1_1, "Pressure_Aortal_1");
        agentModel.addDirectedLink(arterialTree, "lastVesselPressure", plotShort1_1, "lastVessel_1");
        agentModel.addDirectedLink(arterialTree, "inputArea", plotShort1_1, "inputArea");

        agentModel.addDirectedLink(arterialTree, "aortalOutputPressure", plotShort1_2, "Pressure_Aortal_1");
        agentModel.addDirectedLink(arterialTree, "lastVesselPressure", plotShort1_2, "lastVessel_1");
        agentModel.addDirectedLink(arterialTree, "inputArea", plotShort1_2, "inputArea");

        agentModel.addDirectedLink(arterialTree, "aortalOutputPressure", plotShort1_3, "Pressure_Aortal_1");
        agentModel.addDirectedLink(arterialTree, "lastVesselPressure", plotShort1_3, "lastVessel_1");
        agentModel.addDirectedLink(arterialTree, "inputArea", plotShort1_3, "inputArea");

        agentModel.addDirectedLink(arterialTree, "aortalOutputPressure", plotShort1_4, "Pressure_Aortal_1");
        agentModel.addDirectedLink(arterialTree, "lastVesselPressure", plotShort1_4, "lastVessel_1");
        agentModel.addDirectedLink(arterialTree, "inputArea", plotShort1_4, "inputArea");


        agentModel.addDirectedLink(karaaslan, "P_ma", plotLong, "P_ma");
        agentModel.addDirectedLink(karaaslan, "V_b", plotLong, "V_b");
        agentModel.addDirectedLink(karaaslan, "Fi_co", plotLong, "Fi_co");
        agentModel.addDirectedLink(karaaslan, "C_sod", plotLong, "C_sod");

        return agentModel;
    }

}