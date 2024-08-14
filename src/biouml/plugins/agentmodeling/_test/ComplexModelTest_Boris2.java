package biouml.plugins.agentmodeling._test;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.File;

import ru.biosoft.graphics.Pen;
import biouml.plugins.agentmodeling.AgentBasedModel;
import biouml.plugins.agentmodeling.AveragerAgent;
import biouml.plugins.agentmodeling.AgentModelSimulationEngine;
import biouml.plugins.agentmodeling.ModelAgent;
import biouml.plugins.agentmodeling.PlotAgent;
import biouml.plugins.agentmodeling.SimulationResultAgent;
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
public class ComplexModelTest_Boris2
{

    static Pen solid = new Pen(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 10, null, 0), Color.black);
    static Pen dashed = new Pen(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 10, new float[] {9, 4}, 0), Color.black);
    static Pen dotted = new Pen(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 10, new float[] {1, 4}, 0), Color.black);

    static Span minuteSpan = new UniformSpan(0, 336 * 60, 1);
    static Span hourSpan = new UniformSpan(0, 336, 1);
    static Span secondsSpan = new UniformSpan(0, 336 * 60 * 60, 0.01);

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

        HemodynamicsModelSolver hemoSimulator = new HemodynamicsModelSolver();
        HemodynamicsOptions optionsH = (HemodynamicsOptions)hemoSimulator.getOptions();
        optionsH.setTestMode(false);
        optionsH.setInputCondition(HemodynamicsOptions.FLUX_INITIAL_CONDITION_STRING);//.F.PRESSURE_INITIAL_CONDITION);
        optionsH.setOutputCondition(HemodynamicsOptions.FILTRATION_INITIAL_CONDITION_STRING);//.PRESSURE_INITIAL_CONDITION);
        ModelAgent arterialTree = null;/*TODO: fix new ModelAgent(HemodynamicsDiagramGenerator.loadFromFile(new File(modelFileName)), hemoSimulator, secondsSpan,
                "artreialTree");*/
        AveragerAgent adapter = new AveragerAgent("adapter", secondsSpan, 600);
        AveragerAgent adapter2 = new AveragerAgent("adapter2", secondsSpan, 600);
        AveragerAgent adapter3 = new AveragerAgent("adapter2", secondsSpan, 600);


        agentModel.addAgent(karaaslan);
        agentModel.addAgent(sodiumIntake);
        agentModel.addAgent(solodyannikov);
        agentModel.addAgent(arterialTree);


        agentModel.addAgent(adapter);
        agentModel.addAgent(adapter2);
        agentModel.addAgent(adapter3);

        //        Sodium input  --> Kidney
        //        agentModel.addDirectedLink(sodiumIntake, "Fi_sodin", karaaslan, "Fi_sodin");
        //
        //        //        Heart         --> Kidney
        agentModel.addDirectedLink(solodyannikov, "CardiacOutput_Minute", adapter3, "CardiacOutput");
        agentModel.addDirectedLink(adapter3, "CardiacOutput", karaaslan, "Fi_co");


        //Heart         --> Arterial Tree
        agentModel.addDirectedLink(solodyannikov, "BloodFlow_VentricularToArterial", arterialTree, "inputFlow");
        agentModel.addDirectedLink(solodyannikov, "Conductivity_Capillary", arterialTree, "filtrationCoefficient");
        agentModel.addDirectedLink(solodyannikov, "Pressure_Venous", arterialTree, "venousPressure");

        //Kidney        --> Heart
        agentModel.addDirectedLink(karaaslan, "V_large", solodyannikov, "Volume_Full");

        // Arterial tree     --> Kidney
        agentModel.addDirectedLink(arterialTree, "averagePressure", adapter, "Pressure_Arterial");
        agentModel.addDirectedLink(adapter, "Pressure_Arterial", karaaslan, "P_ma");

        agentModel.addDirectedLink(arterialTree, "renalConductivity", karaaslan, "R_aass");

        agentModel.addDirectedLink(arterialTree, "kidneyInputFlow", adapter2, "Kidney_Flow");
        agentModel.addDirectedLink(adapter2, "Kidney_Flow", karaaslan, "Fi_rb");

        //Kidney    --> Arterial tree
        agentModel.addDirectedLink(karaaslan, "R_r", arterialTree, "kidneyResistance");

        //arterial Tree --> Heart
        agentModel.addDirectedLink(arterialTree, "averagePressure", solodyannikov, "Pressure_Arterial");
        agentModel.addDirectedLink(arterialTree, "totalVolume", solodyannikov, "Volume_Arterial");
        agentModel.addDirectedLink(arterialTree, "arterialConductivity", solodyannikov, "Conductivity_Arterial");
        agentModel.addDirectedLink(arterialTree, "outputFlow", solodyannikov, "BloodFlow_Capillary");

        SimulationResultAgent longResult = new SimulationResultAgent("hourSpan", hourSpan);
        karaaslan.addListener(longResult);

        //first control point 3-4 min
        double timePoint = 25;
        PlotAgent pressurePlotShort1 = new PlotAgent("pressure_plot1", new UniformSpan(timePoint - 5, timePoint + 5, 0.01));
        agentModel.addAgent(pressurePlotShort1);
        PlotAgent flowPlotShort1 = new PlotAgent("flow_plot1", new UniformSpan(timePoint - 5, timePoint + 5, 0.01));
        agentModel.addAgent(flowPlotShort1);

        timePoint = 60 * 60;
        PlotAgent pressurePlotShort2 = new PlotAgent("pressure_plot2", new UniformSpan(timePoint - 5, timePoint + 5, 0.01));
        agentModel.addAgent(pressurePlotShort2);
        PlotAgent flowPlotShort2 = new PlotAgent("flow_plot2", new UniformSpan(timePoint - 5, timePoint + 5, 0.01));
        agentModel.addAgent(flowPlotShort2);

        timePoint = 3 * 60 * 60;
        PlotAgent pressurePlotShort3 = new PlotAgent("pressure_plot3", new UniformSpan(timePoint - 5, timePoint + 5, 0.01));
        agentModel.addAgent(pressurePlotShort3);
        PlotAgent flowPlotShort3 = new PlotAgent("flow_plot3", new UniformSpan(timePoint - 5, timePoint + 5, 0.01));
        agentModel.addAgent(flowPlotShort3);

        timePoint = 6 * 60 * 60;
        PlotAgent pressurePlotShort4 = new PlotAgent("pressure_plot4", new UniformSpan(timePoint - 5, timePoint + 5, 0.01));
        agentModel.addAgent(pressurePlotShort4);
        PlotAgent flowPlotShort4 = new PlotAgent("flow_plot4", new UniformSpan(timePoint - 5, timePoint + 5, 0.01));
        agentModel.addAgent(flowPlotShort4);

        timePoint = 12 * 60 * 60;
        PlotAgent pressurePlotShort5 = new PlotAgent("pressure_plot5", new UniformSpan(timePoint - 5, timePoint + 5, 0.01));
        agentModel.addAgent(pressurePlotShort5);
        PlotAgent flowPlotShort5 = new PlotAgent("flow_plot5", new UniformSpan(timePoint - 5, timePoint + 5, 0.01));
        agentModel.addAgent(flowPlotShort5);

        timePoint = 24 * 60 * 60;
        PlotAgent pressurePlotShort6 = new PlotAgent("pressure_plot6", new UniformSpan(timePoint - 5, timePoint + 5, 0.01));
        agentModel.addAgent(pressurePlotShort6);
        PlotAgent flowPlotShort6 = new PlotAgent("flow_plot6", new UniformSpan(timePoint - 5, timePoint + 5, 0.01));
        agentModel.addAgent(flowPlotShort6);

        agentModel.addDirectedLink(arterialTree, "aortalOutputPressure", pressurePlotShort1, "aortalOutputPressure");
        pressurePlotShort1.setSpec("aortalOutputPressure", solid);
        agentModel.addDirectedLink(arterialTree, "rRenalOutputPressure", pressurePlotShort1, "rRenalOutputPressure");
        pressurePlotShort1.setSpec("rRenalOutputPressure", dashed);
        agentModel.addDirectedLink(arterialTree, "femoralRightOutputPressure", pressurePlotShort1, "femoralRightOutputPressure");
        pressurePlotShort1.setSpec("femoralRightOutputPressure", dotted);

        agentModel.addDirectedLink(arterialTree, "rRenalOutputFlow", flowPlotShort1, "rRenalOutputFlow");
        flowPlotShort1.setSpec("rRenalOutputFlow", dashed);
        agentModel.addDirectedLink(arterialTree, "inputFlow", flowPlotShort1, "inputFlow");
        flowPlotShort1.setSpec("inputFlow", solid);
        agentModel.addDirectedLink(arterialTree, "femoralRightOutputFlow", flowPlotShort1, "femoralRightOutputFlow");
        flowPlotShort1.setSpec("femoralRightOutputFlow", dotted);

        agentModel.addDirectedLink(arterialTree, "aortalOutputPressure", pressurePlotShort2, "aortalOutputPressure");
        pressurePlotShort2.setSpec("aortalOutputPressure", solid);
        agentModel.addDirectedLink(arterialTree, "rRenalOutputPressure", pressurePlotShort2, "rRenalOutputPressure");
        pressurePlotShort2.setSpec("rRenalOutputPressure", dashed);
        agentModel.addDirectedLink(arterialTree, "femoralRightOutputPressure", pressurePlotShort2, "femoralRightOutputPressure");
        pressurePlotShort2.setSpec("femoralRightOutputPressure", dotted);

        agentModel.addDirectedLink(arterialTree, "rRenalOutputFlow", flowPlotShort2, "rRenalOutputFlow");
        flowPlotShort2.setSpec("rRenalOutputFlow", dashed);
        agentModel.addDirectedLink(arterialTree, "inputFlow", flowPlotShort2, "inputFlow");
        flowPlotShort2.setSpec("inputFlow", solid);
        agentModel.addDirectedLink(arterialTree, "femoralRightOutputFlow", flowPlotShort2, "femoralRightOutputFlow");
        flowPlotShort2.setSpec("femoralRightOutputFlow", dotted);


        agentModel.addDirectedLink(arterialTree, "aortalOutputPressure", pressurePlotShort3, "aortalOutputPressure");
        pressurePlotShort3.setSpec("aortalOutputPressure", solid);
        agentModel.addDirectedLink(arterialTree, "rRenalOutputPressure", pressurePlotShort3, "rRenalOutputPressure");
        pressurePlotShort3.setSpec("rRenalOutputPressure", dashed);
        agentModel.addDirectedLink(arterialTree, "femoralRightOutputPressure", pressurePlotShort3, "femoralRightOutputPressure");
        pressurePlotShort3.setSpec("femoralRightOutputPressure", dotted);

        agentModel.addDirectedLink(arterialTree, "rRenalOutputFlow", flowPlotShort3, "rRenalOutputFlow");
        flowPlotShort3.setSpec("rRenalOutputFlow", dashed);
        agentModel.addDirectedLink(arterialTree, "inputFlow", flowPlotShort3, "inputFlow");
        flowPlotShort3.setSpec("inputFlow", solid);
        agentModel.addDirectedLink(arterialTree, "femoralRightOutputFlow", flowPlotShort3, "femoralRightOutputFlow");
        flowPlotShort3.setSpec("femoralRightOutputFlow", dotted);

        agentModel.addDirectedLink(arterialTree, "aortalOutputPressure", pressurePlotShort4, "aortalOutputPressure");
        pressurePlotShort4.setSpec("aortalOutputPressure", solid);
        agentModel.addDirectedLink(arterialTree, "rRenalOutputPressure", pressurePlotShort4, "rRenalOutputPressure");
        pressurePlotShort4.setSpec("rRenalOutputPressure", dashed);
        agentModel.addDirectedLink(arterialTree, "femoralRightOutputPressure", pressurePlotShort4, "femoralRightOutputPressure");
        pressurePlotShort4.setSpec("femoralRightOutputPressure", dotted);

        agentModel.addDirectedLink(arterialTree, "rRenalOutputFlow", flowPlotShort4, "rRenalOutputFlow");
        flowPlotShort4.setSpec("rRenalOutputFlow", dashed);
        agentModel.addDirectedLink(arterialTree, "inputFlow", flowPlotShort4, "inputFlow");
        flowPlotShort4.setSpec("inputFlow", solid);
        agentModel.addDirectedLink(arterialTree, "femoralRightOutputFlow", flowPlotShort4, "femoralRightOutputFlow");
        flowPlotShort4.setSpec("femoralRightOutputFlow", dotted);


        agentModel.addDirectedLink(arterialTree, "aortalOutputPressure", pressurePlotShort5, "aortalOutputPressure");
        pressurePlotShort5.setSpec("aortalOutputPressure", solid);
        agentModel.addDirectedLink(arterialTree, "rRenalOutputPressure", pressurePlotShort5, "rRenalOutputPressure");
        pressurePlotShort5.setSpec("rRenalOutputPressure", dashed);
        agentModel.addDirectedLink(arterialTree, "femoralRightOutputPressure", pressurePlotShort5, "femoralRightOutputPressure");
        pressurePlotShort5.setSpec("femoralRightOutputPressure", dotted);

        agentModel.addDirectedLink(arterialTree, "rRenalOutputFlow", flowPlotShort5, "rRenalOutputFlow");
        flowPlotShort5.setSpec("rRenalOutputFlow", dashed);
        agentModel.addDirectedLink(arterialTree, "inputFlow", flowPlotShort5, "inputFlow");
        flowPlotShort5.setSpec("inputFlow", solid);
        agentModel.addDirectedLink(arterialTree, "femoralRightOutputFlow", flowPlotShort5, "femoralRightOutputFlow");
        flowPlotShort5.setSpec("femoralRightOutputFlow", dotted);



        agentModel.addDirectedLink(arterialTree, "aortalOutputPressure", pressurePlotShort6, "aortalOutputPressure");
        pressurePlotShort6.setSpec("aortalOutputPressure", solid);
        agentModel.addDirectedLink(arterialTree, "rRenalOutputPressure", pressurePlotShort6, "rRenalOutputPressure");
        pressurePlotShort6.setSpec("rRenalOutputPressure", dashed);
        agentModel.addDirectedLink(arterialTree, "femoralRightOutputPressure", pressurePlotShort6, "femoralRightOutputPressure");
        pressurePlotShort6.setSpec("femoralRightOutputPressure", dotted);

        agentModel.addDirectedLink(arterialTree, "rRenalOutputFlow", flowPlotShort6, "rRenalOutputFlow");
        flowPlotShort6.setSpec("rRenalOutputFlow", dashed);
        agentModel.addDirectedLink(arterialTree, "inputFlow", flowPlotShort6, "inputFlow");
        flowPlotShort6.setSpec("inputFlow", solid);
        agentModel.addDirectedLink(arterialTree, "femoralRightOutputFlow", flowPlotShort6, "femoralRightOutputFlow");
        flowPlotShort6.setSpec("femoralRightOutputFlow", dotted);


        PlotAgent plotLong = new PlotAgent("longPlot", new UniformSpan(0, 336 * 60, 10));
        plotLong.setTimeScale(60);
        agentModel.addAgent(plotLong);
        agentModel.addDirectedLink(karaaslan, "P_ma", plotLong, "mean pressure");
        agentModel.addDirectedLink(karaaslan, "C_sod", plotLong, "C_sod");
        agentModel.addDirectedLink(karaaslan, "V_b", plotLong, "V_b");
        agentModel.addDirectedLink(karaaslan, "Fi_rb", plotLong, "Fi_rb");
        agentModel.addDirectedLink(karaaslan, "Fi_co", plotLong, "Fi_co");
        agentModel.addDirectedLink(karaaslan, "R_aass", plotLong, "R_aass");

        SimulationResultAgent simualtionResultAgent = new SimulationResultAgent("result", new UniformSpan(0, 336 * 60, 10));
        simualtionResultAgent.setTimeScale(60);
        simualtionResultAgent.setFile(new File("C:/Documents and Settings/axec/kidney result.txt"));
        karaaslan.addListener(simualtionResultAgent);
        //        agentModel.addDirectedLink(arterialTree, "averagePressure", pressurePlotShort1, "Pressure_Arterial");
        //        agentModel.addDirectedLink(arterialTree, "totalVolume", pressurePlotShort1, "Volume_Arterial");
        //        agentModel.addDirectedLink(arterialTree, "arterialConductivity", pressurePlotShort1, "Conductivity_Arterial");
        //        agentModel.addDirectedLink(arterialTree, "outputFlow", pressurePlotShort1, "BloodFlow_Capillary");
        //        agentModel.addDirectedLink(karaaslan, "V_large", pressurePlotShort1, "V_large");

        return agentModel;
    }
}