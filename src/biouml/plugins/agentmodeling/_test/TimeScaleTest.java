package biouml.plugins.agentmodeling._test;

import biouml.plugins.agentmodeling.AgentBasedModel;
import biouml.plugins.agentmodeling.AgentModelSimulationEngine;
import biouml.plugins.agentmodeling.ModelAgent;
import biouml.plugins.agentmodeling.PlotAgent;
import biouml.plugins.simulation.ArraySpan;
import biouml.plugins.simulation.UniformSpan;
import biouml.plugins.simulation.java.EventLoopSimulator;
import biouml.plugins.simulation.java.JavaBaseModel;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TimeScaleTest extends TestCase
{
    public TimeScaleTest(String name)
    {
        super(name);
    }

    UniformSpan span = new UniformSpan(0, 100, 0.1);


    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(TimeScaleTest.class.getName());
        suite.addTest(new TimeScaleTest("test"));
        return suite;
    }
    public void test() throws Exception
    {
        new AgentModelSimulationEngine().simulate(initializeModel());
    }

    public AgentBasedModel initializeModel() throws Exception
    {
        AgentBasedModel agentModel = new AgentBasedModel();

        ModelAgent odeAgent1 = new ModelAgent(new Model1(), new EventLoopSimulator(), span, "m1");
        ModelAgent odeAgent2 = new ModelAgent(new Model1(), new EventLoopSimulator(), new UniformSpan(0, 10, 0.1), "m2");
        odeAgent2.setTimeScale(10);
        PlotAgent plot = new PlotAgent("plot", new ArraySpan(0, 100, 1));

        agentModel.addAgent(odeAgent1);
        agentModel.addAgent(odeAgent2);
        agentModel.addAgent(plot);

        agentModel.addDirectedLink(odeAgent1, "0", plot, "pa1");
        agentModel.addDirectedLink(odeAgent2, "0", plot, "pa2");

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
    }
}
