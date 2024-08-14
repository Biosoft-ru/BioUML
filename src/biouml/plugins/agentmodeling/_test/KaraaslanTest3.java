package biouml.plugins.agentmodeling._test;

import java.awt.BasicStroke;
import java.awt.Color;

import ru.biosoft.graphics.Pen;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import biouml.plugins.agentmodeling.AgentBasedModel;
import biouml.plugins.agentmodeling.AgentModelSimulationEngine;
import biouml.plugins.agentmodeling.ModelAgent;
import biouml.plugins.agentmodeling.PlotAgent;
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
public class KaraaslanTest3 extends TestCase
{
    static Span minuteSpan = new UniformSpan(0, 18280, 100);
    static Span hourSpan = new UniformSpan(0, 288, 1);
    static Span secondsSpan = new UniformSpan(0, 18280 * 60, 0.1);

    static Pen solid = new Pen(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 10, null, 0), Color.black);
    static Pen dashed = new Pen(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 10, new float[] {9, 4}, 0), Color.black);

    public KaraaslanTest3(String name)
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
    }
    
    public void test(String ... args) throws Exception
    {
        new AgentModelSimulationEngine().simulate(generateModel());
    }

    private static AgentBasedModel generateModel() throws Exception
    {

        AgentBasedModel agentModel = new AgentBasedModel();

        ModelAgent kidney = new ModelAgent(new Karaaslan_corrected(), new EventLoopSimulator(), minuteSpan, "kidney");

        ModelAgent sodiumIntake = new ModelAgent(new KaraaslanSodiumIntake(), new EventLoopSimulator(), minuteSpan, "Sodium");

        PlotAgent plot_P_ma = new PlotAgent("plot_P_ma", minuteSpan);

        PlotAgent plot_Sodin = new PlotAgent("plot_Sodin", minuteSpan);

        PlotAgent plot_V_b = new PlotAgent("plot_V_b", minuteSpan);
        
        PlotAgent plot_rsna = new PlotAgent("plot_rsna", minuteSpan);

        PlotAgent plot_V_ecf = new PlotAgent("plot_V_ecf", minuteSpan);
        
        PlotAgent plot_C_adh = new PlotAgent("plot_C_adh", minuteSpan);
        
        PlotAgent plot_vas = new PlotAgent("vas", minuteSpan);

        PlotAgent plot_C_sod = new PlotAgent("C_sod", minuteSpan);

        PlotAgent plot_C_r = new PlotAgent("C_r", minuteSpan);
        
        PlotAgent plot_C_al = new PlotAgent("C_al", minuteSpan);

        agentModel.addAgent(kidney);
        agentModel.addAgent(plot_P_ma);
        agentModel.addAgent(plot_Sodin);
        agentModel.addAgent(plot_V_b);
        agentModel.addAgent(plot_rsna);
        agentModel.addAgent(plot_V_ecf);
        agentModel.addAgent(plot_C_adh);
        agentModel.addAgent(plot_C_sod);
        agentModel.addAgent(plot_C_al);
        agentModel.addAgent(plot_C_r);
        agentModel.addAgent(plot_vas);
        agentModel.addAgent(sodiumIntake);

    
        agentModel.addDirectedLink(sodiumIntake, "Fi_sodin", kidney, "Fi_sodin");

        agentModel.addDirectedLink(kidney, "P_ma", plot_P_ma, "P_ma");
        plot_P_ma.setSpec("P_ma", solid);

        agentModel.addDirectedLink(kidney, "V_b", plot_V_b, "V_b");
        plot_V_b.setSpec("V_b", solid);

        agentModel.addDirectedLink(sodiumIntake, "Fi_sodin", plot_Sodin, "Sodium intake");
        plot_Sodin.setSpec("Sodium intake", solid);

        agentModel.addDirectedLink(kidney, "rsna", plot_rsna, "rsna");
        plot_rsna.setSpec("rsna", solid);

        agentModel.addDirectedLink(kidney, "5", plot_V_ecf, "V_ecf");
        plot_V_ecf.setSpec("V_ecf", solid);

        agentModel.addDirectedLink(kidney, "8", plot_vas, "vas");
        plot_vas.setSpec("vas", solid);

        agentModel.addDirectedLink(kidney, "C_adh", plot_C_adh, "C_adh");
        plot_C_adh.setSpec("C_adh", solid);
        
        agentModel.addDirectedLink(kidney, "C_sod", plot_C_sod, "C_sod");
        plot_C_sod.setSpec("C_sod", solid);
        
        agentModel.addDirectedLink(kidney, "C_al", plot_C_al, "C_al");
        plot_C_al.setSpec("C_al", solid);
        
        agentModel.addDirectedLink(kidney, "6", plot_C_r, "C_r");
        plot_C_r.setSpec("C_r", solid);
       
        return agentModel;
    }
}