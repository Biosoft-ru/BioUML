package biouml.plugins.agentmodeling._test;

import biouml.plugins.agentmodeling.AveragerAgent;
import biouml.plugins.agentmodeling.AgentBasedModel;
import biouml.plugins.agentmodeling.AgentModelSimulationEngine;
import biouml.plugins.agentmodeling.ModelAgent;
import biouml.plugins.agentmodeling.PlotAgent;
import biouml.plugins.agentmodeling._test.models.Solodyannikov;
import biouml.plugins.simulation.ArraySpan;
import biouml.plugins.simulation.java.EventLoopSimulator;
import biouml.plugins.simulation.java.JavaBaseModel;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AdapterAgentTest extends TestCase
{
    public AdapterAgentTest(String name)
    {
        super(name);
    }


    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(AdapterAgentTest.class.getName());
        suite.addTest(new AdapterAgentTest("test"));
        return suite;
    }
    public void test() throws Exception
    {
        new AgentModelSimulationEngine().simulate(initializeModel());
    }

    public static AgentBasedModel initializeModel() throws Exception
    {
        AgentBasedModel agentModel = new AgentBasedModel();
        ArraySpan span = new ArraySpan(0, 21000, 0.5);
        ModelAgent odeAgent = new ModelAgent(new Solodyannikov(), new EventLoopSimulator(), span, "heart");
        PlotAgent plot = new PlotAgent("plot", span);
        PlotAgent plot1 = new PlotAgent("plot", new ArraySpan(0, 21000, 100));

//        AdapterAgent adapter = new AdapterAgent("adapter", new ArraySpan(0, 21000, 100), 10000);
//        AdapterAgent adapter1 = new AdapterAgent("adapter", new ArraySpan(0, 21000, 40), 4000);
//
        AveragerAgent adapter10 = new AveragerAgent("adapter", new ArraySpan(0, 21000, 0.5), 8000);
//        AdapterAgent adapter100 = new AdapterAgent("adapter", new ArraySpan(0, 21000, 100), 4000);
        
        agentModel.addAgent(odeAgent);
        agentModel.addAgent(plot);
        agentModel.addAgent(plot1);
//        agentModel.addAgent(adapter);
//
//        agentModel.addAgent(adapter1);
        agentModel.addAgent(adapter10);
//        agentModel.addAgent(adapter100);
        
//        agentModel.addDirectedLink(odeAgent, "Pressure_Arterial", plot, "pa");
//        agentModel.addDirectedLink(odeAgent, "Pressure_Arterial", adapter, "pa");
//        agentModel.addDirectedLink(adapter, "pa", plot1, "pa_mean05");
        
//        agentModel.addDirectedLink(odeAgent, "Pressure_Arterial", adapter1, "pa");
//        agentModel.addDirectedLink(adapter1, "pa", plot1, "pa_mean1");
//
        agentModel.addDirectedLink(odeAgent, "Pressure_Arterial", adapter10, "pa");
        agentModel.addDirectedLink(adapter10, "pa", plot1, "pa_mean10");
//
//        agentModel.addDirectedLink(odeAgent, "Pressure_Arterial", adapter100, "pa");
//        agentModel.addDirectedLink(adapter100, "pa", plot1, "pa_mean");
        return agentModel;
    }

    public static class Model1 extends JavaBaseModel
    {
        double k1 = 1;
        @Override
        public double[] dy_dt(double time, double[] y)
        {
            double[] dy = new double[1];
            dy[0] = k1; //B
            return dy;
        }

        @Override
        public void init()
        {
        }

        @Override
        public double[] getInitialValues()
        {
            return new double[] {0};
        }

        @Override
        public double[] checkEvent(double time, double[] x)
        {
            this.time = time;
            double[] result = new double[2];
            result[0] = x[0] >= 10 ? 1 : 0;
            result[1] = x[0] <= -10 ? 1 : 0;
            return result;
        }

        public void processEvent(int v9, double time, double[] x)
        {
            this.time = time;
            if( v9 == 0 )
            { // event0
               k1 = -1;
            }
            else if( v9 == 1 )
            { // event1
                k1 = 1;
            }
        }
    }
}
