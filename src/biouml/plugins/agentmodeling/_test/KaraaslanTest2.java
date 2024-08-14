package biouml.plugins.agentmodeling._test;

import java.awt.BasicStroke;
import java.awt.Color;
import ru.biosoft.graphics.Pen;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import biouml.plugins.agentmodeling.AveragerAgent;
import biouml.plugins.agentmodeling.AgentBasedModel;
import biouml.plugins.agentmodeling.AgentModelSimulationEngine;
import biouml.plugins.agentmodeling.ModelAgent;
import biouml.plugins.agentmodeling.PlotAgent;
import biouml.plugins.agentmodeling._test.models.CVS1_Karaaslan;
import biouml.plugins.agentmodeling._test.models.CVS1_Solodyannikov;
import biouml.plugins.agentmodeling._test.models.KaraaslanSodiumIntake;
import biouml.plugins.agentmodeling._test.models.Karaaslan_corrected;
import biouml.plugins.simulation.Span;
import biouml.plugins.simulation.UniformSpan;
import biouml.plugins.simulation.java.EventLoopSimulator;

/**
 * Test for heart agent model consisting of 6 agents: ArterialSystem, VenousSystem, Ventricle, NeuroHumoralControlSystem, Capillary
 * @author axec
 *
 */
public class KaraaslanTest2 extends TestCase
{
    static Span minuteSpan = new UniformSpan(0, 15 * 24 * 60, 1);
    static Span hourSpan = new UniformSpan(0, 15 * 24, 1);
    static Span secondsSpan = new UniformSpan(0, 15 * 24 * 60 * 60, 0.1);

    static Pen solid = new Pen(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 10, null, 0), Color.black);
    static Pen dashed = new Pen(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 10, new float[] {9, 4}, 0), Color.black);

    static Pen solidRed = new Pen(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 10, null, 0), Color.red);
    static Pen solidBlue = new Pen(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 10, null, 0), Color.blue);

    public KaraaslanTest2(String name)
    {
        super(name);
    }


    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(KaraaslanTest2.class.getName());
        suite.addTest(new KaraaslanTest2("test"));
        return suite;
    }

    public static void main(String ... args) throws Exception
    {
        new AgentModelSimulationEngine().simulate(generateModel());

        Thread.sleep((long)1E9);
    }

    public void test() throws Exception
    {
        AgentModelSimulationEngine engine = new AgentModelSimulationEngine();
        AgentBasedModel model = generateModel();
        engine.simulate(model);

        System.out.println("done");
        Thread.sleep((long)1E9);
    }



    private static AgentBasedModel generateModel() throws Exception
    {

        AgentBasedModel agentModel = new AgentBasedModel();

        ModelAgent kidney = new ModelAgent(new Karaaslan_corrected(), new EventLoopSimulator(), minuteSpan, "kidney");
        kidney.setTimeScale(60);

        ModelAgent kidneyAgent = new ModelAgent(new CVS1_Karaaslan(), new EventLoopSimulator(), minuteSpan, "kidneyAgent");
        kidneyAgent.setTimeScale(60);

        ModelAgent sodiumIntake = new ModelAgent(new KaraaslanSodiumIntake(), new EventLoopSimulator(), minuteSpan, "Sodium");
        sodiumIntake.setTimeScale(60);

        ModelAgent solodyannikov = new ModelAgent(new CVS1_Solodyannikov(), new EventLoopSimulator(), secondsSpan, "Solodyannikov");

        AveragerAgent adapter1 = new AveragerAgent("adapter1", secondsSpan, 6000);
        AveragerAgent adapter2 = new AveragerAgent("adapter2", secondsSpan, 6000);

        PlotAgent plot_P_ma = new PlotAgent("plot_P_ma", hourSpan);
        plot_P_ma.setTimeScale(3600);

        PlotAgent plot_Sodin = new PlotAgent("plot_Sodin", hourSpan);
        plot_Sodin.setTimeScale(3600);

        PlotAgent plot_V_b = new PlotAgent("plot_V_b", hourSpan);
        plot_V_b.setTimeScale(3600);

        PlotAgent plot_rsna = new PlotAgent("plot_rsna", hourSpan);
        plot_rsna.setTimeScale(3600);

        PlotAgent plot_V_ecf = new PlotAgent("plot_V_ecf", hourSpan);
        plot_V_ecf.setTimeScale(3600);

        PlotAgent plot_C_adh = new PlotAgent("plot_C_adh", hourSpan);
        plot_C_adh.setTimeScale(3600);

        PlotAgent plot_vas = new PlotAgent("vas", hourSpan);
        plot_vas.setTimeScale(3600);

        PlotAgent plot_C_sod = new PlotAgent("C_sod", hourSpan);
        plot_C_sod.setTimeScale(3600);
        //
        PlotAgent plot_C_r = new PlotAgent("C_r", hourSpan);
        plot_C_r.setTimeScale(3600);
        //
        PlotAgent plot_C_al = new PlotAgent("C_al", hourSpan);
        plot_C_al.setTimeScale(3600);
        //
        PlotAgent plot_Fi_co = new PlotAgent("Fi_co", hourSpan);
        plot_Fi_co.setTimeScale(3600);

        PlotAgent plot_Pulse = new PlotAgent("Pulse", hourSpan);
        plot_Pulse.setTimeScale(3600);

        PlotAgent plot_fast_1 = new PlotAgent("plot_F_1", new UniformSpan(60, 60 + 5, 0.001));
        PlotAgent plot_fast_2 = new PlotAgent("plot_F_2", new UniformSpan(5000 * 60, 5000 * 60 + 5, 0.001));
        PlotAgent plot_fast_3 = new PlotAgent("plot_F_3", new UniformSpan(10000 * 60, 10000 * 60 + 5, 0.001));
        PlotAgent plot_fast_4 = new PlotAgent("plot_F_4", new UniformSpan(15000 * 60, 15000 * 60 + 5, 0.001));

        agentModel.addAgent(kidney);
        //                agentModel.addAgent(plot_);

        agentModel.addAgent(kidneyAgent);
        agentModel.addAgent(plot_P_ma);
        agentModel.addAgent(plot_Sodin);
        agentModel.addAgent(plot_V_b);
        agentModel.addAgent(plot_rsna);
        agentModel.addAgent(plot_V_ecf);
        agentModel.addAgent(plot_C_adh);
        agentModel.addAgent(plot_C_sod);
        agentModel.addAgent(plot_C_al);
        agentModel.addAgent(plot_C_r);
        agentModel.addAgent(plot_Fi_co);
        agentModel.addAgent(plot_Pulse);
        agentModel.addAgent(plot_vas);
        agentModel.addAgent(plot_fast_1);
        agentModel.addAgent(plot_fast_2);
        agentModel.addAgent(plot_fast_3);
        agentModel.addAgent(plot_fast_4);
        agentModel.addAgent(sodiumIntake);
        agentModel.addAgent(solodyannikov);
        agentModel.addAgent(adapter1);
        agentModel.addAgent(adapter2);

        agentModel.addDirectedLink(sodiumIntake, "Fi_sodin", kidneyAgent, "Fi_sodin");
        agentModel.addDirectedLink(sodiumIntake, "Fi_sodin", kidney, "Fi_sodin");

        agentModel.addDirectedLink(kidneyAgent, "9", solodyannikov, "vas");
        agentModel.addDirectedLink(kidneyAgent, "7", solodyannikov, "eps_aum");
        
        agentModel.addDirectedLink(kidneyAgent, "V_large", solodyannikov, "Volume_Full");

        agentModel.addDirectedLink(solodyannikov, "Pressure_Arterial", adapter1, "P_ma");
        agentModel.addDirectedLink(adapter1, "P_ma", kidneyAgent, "P_ma");

        agentModel.addDirectedLink(solodyannikov, "CardiacOutput_Minute", adapter2, "Fi_co");
        agentModel.addDirectedLink(adapter2, "Fi_co", kidneyAgent, "Fi_co");

        //        SimulationResultAgent result1 = new SimulationResultAgent("result", hourSpan);
        //        agentModel.addAgent(result1);
        //        result1.setFile(new File("C:/Documents and Settings/axec/results"));
        //        result1.setTimeScale(3600);
        //        kidney.addListener(result1);

        //to plots
        agentModel.addDirectedLink(kidneyAgent, "P_ma", plot_P_ma, "P_ma_agent");
        agentModel.addDirectedLink(kidney, "P_ma", plot_P_ma, "P_ma");
        plot_P_ma.setSpec("P_ma_agent", solid);
        plot_P_ma.setSpec("P_ma", dashed);
        //
        agentModel.addDirectedLink(kidneyAgent, "V_b", plot_V_b, "V_b_agent");
        agentModel.addDirectedLink(kidney, "V_b", plot_V_b, "V_b");
        plot_V_b.setSpec("V_b_agent", solid);
        plot_V_b.setSpec("V_b", dashed);
        //
        agentModel.addDirectedLink(kidneyAgent, "rsna", plot_rsna, "rsna_agent");
        agentModel.addDirectedLink(kidney, "rsna", plot_rsna, "rsna");
        plot_rsna.setSpec("rsna_agent", solid);
        plot_rsna.setSpec("rsna", dashed);
        //
        agentModel.addDirectedLink(kidneyAgent, "5", plot_V_ecf, "V_ecf_agent");
        agentModel.addDirectedLink(kidney, "5", plot_V_ecf, "V_ecf");
        plot_V_ecf.setSpec("V_ecf_agent", solid);
        plot_V_ecf.setSpec("V_ecf", dashed);

        agentModel.addDirectedLink(kidneyAgent, "9", plot_vas, "vas_agent");
        agentModel.addDirectedLink(kidney, "8", plot_vas, "vas");
        plot_vas.setSpec("vas_agent", solid);
        plot_vas.setSpec("vas", dashed);

        agentModel.addDirectedLink(kidneyAgent, "C_adh", plot_C_adh, "C_adh_agent");
        agentModel.addDirectedLink(kidney, "C_adh", plot_C_adh, "C_adh");
        plot_C_adh.setSpec("C_adh_agent", solid);
        plot_C_adh.setSpec("C_adh", dashed);

        agentModel.addDirectedLink(kidneyAgent, "C_sod", plot_C_sod, "C_sod_agent");
        agentModel.addDirectedLink(kidney, "C_sod", plot_C_sod, "C_sod");
        plot_C_sod.setSpec("C_sod_agent", solid);
        plot_C_sod.setSpec("C_sod", dashed);

        agentModel.addDirectedLink(kidneyAgent, "C_al", plot_C_al, "C_al_agent");
        agentModel.addDirectedLink(kidney, "C_al", plot_C_al, "C_al");
        plot_C_al.setSpec("C_al_agent", solid);
        plot_C_al.setSpec("C_al", dashed);

        agentModel.addDirectedLink(kidneyAgent, "6", plot_C_r, "C_r_agent");
        agentModel.addDirectedLink(kidney, "6", plot_C_r, "C_r");
        plot_C_r.setSpec("C_r_agent", solid);
        plot_C_r.setSpec("C_r", dashed);

        agentModel.addDirectedLink(kidney, "10", plot_Fi_co, "Fi_co");
        agentModel.addDirectedLink(kidneyAgent, "Fi_co", plot_Fi_co, "Fi_co_agent");
        plot_Fi_co.setSpec("Fi_co", dashed);
        plot_Fi_co.setSpec("Fi_co_agent", solid);

        agentModel.addDirectedLink(kidney, "Fi_sodin", plot_Sodin, "Fi_sodin");
        agentModel.addDirectedLink(kidneyAgent, "Fi_sodin", plot_Sodin, "Fi_sodin_agent");
        plot_Sodin.setSpec("Fi_sodin", dashed);
        plot_Sodin.setSpec("Fi_sodin_agent", solid);

        agentModel.addDirectedLink(kidney, "Fi_sodin", plot_Sodin, "Fi_sodin");
        agentModel.addDirectedLink(kidneyAgent, "Fi_sodin", plot_Sodin, "Fi_sodin_agent");
        plot_Sodin.setSpec("Fi_sodin", dashed);
        plot_Sodin.setSpec("Fi_sodin_agent", solid);


        //        agentModel.addDirectedLink(solodyannikov, "Pressure_Arterial", plot_fast_1, "Pressure_Arterial");

        //                agentModel.addDirectedLink(solodyannikov, "Pulse_Minute", plot_fast_1, "Pulse");
        //                agentModel.addDirectedLink(solodyannikov, "Pulse_Minute", plot_fast_2, "Pulse");
        //                agentModel.addDirectedLink(solodyannikov, "Pulse_Minute", plot_fast_3, "Pulse");
        //                agentModel.addDirectedLink(solodyannikov, "Pulse_Minute", plot_fast_4, "Pulse");

        agentModel.addDirectedLink(solodyannikov, "6", plot_fast_1, "Humoral");
        agentModel.addDirectedLink(solodyannikov, "6", plot_fast_2, "Humoral");
        agentModel.addDirectedLink(solodyannikov, "6", plot_fast_3, "Humoral");
        agentModel.addDirectedLink(solodyannikov, "6", plot_fast_4, "Humoral");

        agentModel.addDirectedLink(solodyannikov, "1", plot_fast_1, "Oxygen_Debt");
        agentModel.addDirectedLink(solodyannikov, "1", plot_fast_2, "Oxygen_Debt");
        agentModel.addDirectedLink(solodyannikov, "1", plot_fast_3, "Oxygen_Debt");
        agentModel.addDirectedLink(solodyannikov, "1", plot_fast_4, "Oxygen_Debt");

        //        plot_fast_1.setSpec("Pressure_Arterial", solid);
        //        plot_fast_2.setSpec("Pressure_Arterial", solid);
        //        plot_fast_3.setSpec("Pressure_Arterial", solid);
        //        plot_fast_4.setSpec("Pressure_Arterial", solid);

        plot_fast_1.setSpec("Humoral", solidRed);
        plot_fast_2.setSpec("Humoral", solidRed);
        plot_fast_3.setSpec("Humoral", solidRed);
        plot_fast_4.setSpec("Humoral", solidRed);

        plot_fast_1.setSpec("Oxygen_Debt", solidBlue);
        plot_fast_2.setSpec("Oxygen_Debt", solidBlue);
        plot_fast_3.setSpec("Oxygen_Debt", solidBlue);
        plot_fast_4.setSpec("Oxygen_Debt", solidBlue);

        return agentModel;
    }
}