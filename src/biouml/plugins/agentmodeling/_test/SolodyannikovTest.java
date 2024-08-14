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
import biouml.plugins.agentmodeling._test.models.Solodyannikov;
import biouml.plugins.simulation.Span;
import biouml.plugins.simulation.UniformSpan;
import biouml.plugins.simulation.java.EventLoopSimulator;

/**
 * Test for heart agent model consisting of 6 agents: ArterialSystem, VenousSystem, Ventricle, NeuroHumoralControlSystem, Capillary
 * @author axec
 *
 */
public class SolodyannikovTest extends TestCase
{
    static Span minuteSpan = new UniformSpan(0, 18280, 1);
    static Span hourSpan = new UniformSpan(0, 288, 1);
    static Span secondsSpan001 = new UniformSpan(0, 18280 * 60, 0.01);
    static Span secondsSpan01 = new UniformSpan(0, 18280 * 60, 0.1);
    
    static Pen solid = new Pen(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 10, null, 0), Color.black);
    static Pen dashed = new Pen(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 10, new float[] {9, 4}, 0), Color.black);

    static Pen solidRed = new Pen(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 10, null, 0), Color.red);
    static Pen solidBlue = new Pen(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 10, null, 0), Color.blue);

    public SolodyannikovTest(String name)
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

    public void test() throws Exception
    {
        new AgentModelSimulationEngine().simulate(generateModel());
    }



    private static AgentBasedModel generateModel() throws Exception
    {

        AgentBasedModel agentModel = new AgentBasedModel();


        ModelAgent solodyannikov01 = new ModelAgent(new Solodyannikov(), new EventLoopSimulator(), secondsSpan01, "Solodyannikov");

        ModelAgent solodyannikov001 = new ModelAgent(new Solodyannikov(), new EventLoopSimulator(), secondsSpan001, "Solodyannikov");

        
        AveragerAgent adapter1 = new AveragerAgent("adapter1", secondsSpan01, 6000);
        AveragerAgent adapter2 = new AveragerAgent("adapter2", secondsSpan001, 6000);

        PlotAgent plot_fast_1 = new PlotAgent("plot_F_1", new UniformSpan(60, 60 + 5, 0.001));
        PlotAgent plot_fast_2 = new PlotAgent("plot_F_2", new UniformSpan(100 * 60, 100 * 60 + 5, 0.001));
  
        agentModel.addAgent(solodyannikov01);
        agentModel.addAgent(solodyannikov001);
        
        agentModel.addAgent(plot_fast_1);
        agentModel.addAgent(plot_fast_2);

        agentModel.addAgent(adapter1);
        agentModel.addAgent(adapter2);

        agentModel.addDirectedLink(solodyannikov001, "CardiacOutput_Minute", adapter2, "Fi_co");
        agentModel.addDirectedLink(solodyannikov01, "CardiacOutput_Minute", adapter1, "Fi_co");
        
        agentModel.addDirectedLink(solodyannikov01, "Pressure_Arterial", plot_fast_1, "P_ma01");
        agentModel.addDirectedLink(adapter2, "Fi_co", plot_fast_1, "Fi_co01");
        agentModel.addDirectedLink(solodyannikov01, "Pulse", plot_fast_1, "Pulse01");
   
        agentModel.addDirectedLink(solodyannikov001, "Pressure_Arterial", plot_fast_1, "P_ma001");
        agentModel.addDirectedLink(adapter2, "Fi_co", plot_fast_1, "Fi_co001");
        agentModel.addDirectedLink(solodyannikov001, "Pulse", plot_fast_1, "Pulse001");
        
        
        agentModel.addDirectedLink(solodyannikov01, "Pressure_Arterial", plot_fast_2, "P_ma01");
        agentModel.addDirectedLink(adapter2, "Fi_co", plot_fast_2, "Fi_co01");
        agentModel.addDirectedLink(solodyannikov01, "Pulse", plot_fast_2, "Pulse01");
    
        agentModel.addDirectedLink(solodyannikov001, "Pressure_Arterial", plot_fast_2, "P_ma001");
        agentModel.addDirectedLink(adapter2, "Fi_co", plot_fast_2, "Fi_co001");
        agentModel.addDirectedLink(solodyannikov001, "Pulse", plot_fast_1, "Pulse001");
       

        return agentModel;
    }
}