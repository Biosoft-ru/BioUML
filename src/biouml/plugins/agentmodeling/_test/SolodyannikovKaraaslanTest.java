package biouml.plugins.agentmodeling._test;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.logging.Logger;

import biouml.plugins.agentmodeling.AveragerAgent;
import biouml.plugins.agentmodeling.AgentBasedModel;
import biouml.plugins.agentmodeling.AgentModelSimulationEngine;
import biouml.plugins.agentmodeling.ModelAgent;
import biouml.plugins.agentmodeling.PlotAgent;
import biouml.plugins.agentmodeling.SimulationAgent;
import biouml.plugins.agentmodeling._test.models.KaraaslanSodiumIntake;
import biouml.plugins.agentmodeling._test.models.Karaaslan_part;
import biouml.plugins.agentmodeling._test.models.Solodyannikov;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.Simulator;
import ru.biosoft.jobcontrol.FunctionJobControl;
import biouml.plugins.simulation.UniformSpan;
import biouml.plugins.simulation.java.EventLoopSimulator;
import biouml.plugins.simulation.ode.jvode.JVodeSolver;
import biouml.plugins.simulation.ode.jvode.JVodeOptions;
import biouml.standard.simulation.ResultListener;

/**
 * Test for Solodyannikov Kaaraslan simulation
 * @author axec
 *
 */

public class SolodyannikovKaraaslanTest extends TestCase
{

    public static void main(String ... args) throws Exception
    {
        test();
    }

    public SolodyannikovKaraaslanTest(String name)
    {
        super(name);
    }


    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(SolodyannikovKaraaslanTest.class.getName());
        suite.addTest(new SolodyannikovKaraaslanTest("test"));
        return suite;
    }

    public static void test() throws Exception
    {
        new AgentModelSimulationEngine().simulate(generateModel());
    }

    private static AgentBasedModel generateModel() throws Exception
    {
        AgentBasedModel agentModel = new AgentBasedModel();

        EventLoopSimulator simulator = new EventLoopSimulator();
        JVodeOptions options = new JVodeOptions();
        options.setRtol(1E-7);
        simulator.setOptions(options);
        ModelAgent karaaslan = new ModelAgent(new Karaaslan_part(), simulator, new UniformSpan(0, 10000, 1), "Karaaslan");
        karaaslan.setTimeScale(60);

        ModelAgent sodiumIntake = new ModelAgent(new KaraaslanSodiumIntake(), new EventLoopSimulator(), new UniformSpan(0, 10000, 1),
                "Sodium");
        sodiumIntake.setTimeScale(60);

        ModelAgent solodyannikov = new ModelAgent(new Solodyannikov(), new EventLoopSimulator(), new UniformSpan(0, 600000, 0.1),
                "Solodyannikov");

        AveragerAgent adapter = new AveragerAgent("adapter", new UniformSpan(0, 600000, 0.1), 1000);

        PlotAgent plotFisodin = new PlotAgent("plotFisodin", new UniformSpan(0, 600000, 100));

        PlotAgent plotArterial = new PlotAgent("plotArterial", new UniformSpan(0, 600000, 100));
        //
        //        PlotAgent plotRsna = new PlotAgent("plotRsna", new UniformSpan(0, 600000, 100));
        //        Util.createGraphics(plotRsna);
        //
        //        PlotAgent plotFi_Co = new PlotAgent("plotFi_Co", new UniformSpan(0, 600000, 100));
        //        Util.createGraphics(plotFi_Co);
        //
        //        PlotAgent plotVolume = new PlotAgent("plotVolume", new UniformSpan(0, 600000, 100));
        //        Util.createGraphics(plotVolume);
        //
        //        PlotAgent plotRenin = new PlotAgent("plotRenin", new UniformSpan(0, 600000, 100));
        //        Util.createGraphics(plotRenin);
        //
        //        PlotAgent plotSod = new PlotAgent("plotSod", new UniformSpan(0, 600000, 100));
        //        Util.createGraphics(plotSod);

        PlotAgent plotArterialPressure0 = new PlotAgent("plotArterialPressure0", new UniformSpan(100000, 100020, 0.001));
        //        PlotAgent plotArterialPressure180000 = new PlotAgent("plotArterialPressure180000", new UniformSpan(12000, 12020, 0.1));
        //        PlotAgent plotArterialPressure300000 = new PlotAgent("plotArterialPressure300000", new UniformSpan(30000, 30020, 0.1));

        agentModel.addAgent(karaaslan);
        //        agentModel.addAgent(karaaslanFull);
        agentModel.addAgent(sodiumIntake);
        agentModel.addAgent(solodyannikov);

        agentModel.addAgent(plotArterial);
        agentModel.addAgent(plotFisodin);
        //        agentModel.addAgent(plotRsna);
        //        agentModel.addAgent(plotFi_Co);
        //        agentModel.addAgent(plotVolume);
        //        agentModel.addAgent(plotRenin);
        //        agentModel.addAgent(plotSod);
        //
        agentModel.addAgent(plotArterialPressure0);
        //        agentModel.addAgent(plotArterialPressure180000);
        //        agentModel.addAgent(plotArterialPressure300000);
        //        agentModel.addAgent(plotArterialPressure492000);
        //        agentModel.addAgent(plotArterialPressure592000);
        //
        agentModel.addAgent(adapter);

        agentModel.addDirectedLink(sodiumIntake, "Fi_sodin", karaaslan, "Fi_sodin");
        agentModel.addDirectedLink(solodyannikov, "Cardiac_Output", karaaslan, "Fi_co");
        agentModel.addDirectedLink(solodyannikov, "Pressure_Arterial", adapter, "Pressure_Arterial");
        agentModel.addDirectedLink(adapter, "Pressure_Arterial", karaaslan, "P_ma");
        agentModel.addDirectedLink(karaaslan, "V_large", solodyannikov, "Volume_Full");


        //        agentModel.addDirectedLink(karaaslanFull, "P_ma", plot, "P_ma_full");

        //        agentModel.addDirectedLink(solodyannikov, "Cardiac_Output", plotFi_Co, "Cardiac_Output");
        //        agentModel.addDirectedLink(sodiumIntake, "Fi_sodin", plot, "Fi_sodin");

        agentModel.addDirectedLink(solodyannikov, "Pressure_Arterial", plotArterialPressure0, "Pressure_Arterial");
        //        agentModel.addDirectedLink(solodyannikov, "Pressure_Arterial", plotArterialPressure180000, "Pressure_Arterial");
        //        agentModel.addDirectedLink(solodyannikov, "Pressure_Arterial", plotArterialPressure300000, "Pressure_Arterial");
        //        agentModel.addDirectedLink(solodyannikov, "Pressure_Arterial", plotArterialPressure492000, "Pressure_Arterial");
        //        agentModel.addDirectedLink(solodyannikov, "Pressure_Arterial", plotArterialPressure592000, "Pressure_Arterial");

        //        agentModel.addDirectedLink(karaaslan, "rsna", plotRsna, "rsna_agent");
        //        agentModel.addDirectedLink(karaaslanFull, "rsna", plotRsna, "rsna");
        //
        agentModel.addDirectedLink(karaaslan, "Fi_sodin", plotFisodin, "Fi_sodin_agent");
        agentModel.addDirectedLink(karaaslan, "0", plotFisodin, "0");
        //
        agentModel.addDirectedLink(karaaslan, "P_ma", plotArterial, "P_ma_agent");
        //        agentModel.addDirectedLink(karaaslanFull, "P_ma", plotArterial, "P_ma");
        //
        //        agentModel.addDirectedLink(karaaslan, "Fi_co", plotFi_Co, "Fi_co_agent");
        //        agentModel.addDirectedLink(karaaslanFull, "Fi_co", plotFi_Co, "Fi_co");
        //
        //        agentModel.addDirectedLink(karaaslan, "V_b", plotVolume, "V_b_agent");
        //        agentModel.addDirectedLink(karaaslanFull, "V_b", plotVolume, "V_b");
        //
        //        agentModel.addDirectedLink(karaaslan, "4", plotRenin, "C_r_agent");
        //        agentModel.addDirectedLink(karaaslanFull, "4", plotRenin, "C_r");
        //
        //        agentModel.addDirectedLink(karaaslan, "C_sod", plotSod, "C_sod_agent");
        //        agentModel.addDirectedLink(karaaslanFull, "C_sod", plotSod, "C_sod");
        //        agentModel.addDirectedLink(karaaslan, "0", plotSod, "M_sod_agent");
        //        agentModel.addDirectedLink(karaaslanFull, "0", plotSod, "M_sod");

        //        agentModel.addDirectedLink(karaaslan, "4", plot, "C_r");
        //        agentModel.addDirectedLink(karaaslan, "0", plot, "M_sod");
        //        agentModel.addDirectedLink(karaaslan, "V_large", plot, "Full_Volume");
        //        agentModel.addDirectedLink(karaaslan, "P_ma", plot, "Mean arterial pressure");
        //
        //
        //        agentModel.addDirectedLink(karaaslanFull, "Fi_sodin", plotKaraaslan, "Fi_sodin");
        //        agentModel.addDirectedLink(karaaslanFull, "rsna", plotKaraaslan, "rsna");
        //        agentModel.addDirectedLink(karaaslanFull, "4", plotKaraaslan, "C_r");
        //        agentModel.addDirectedLink(karaaslanFull, "0", plotKaraaslan, "M_sod");
        //        agentModel.addDirectedLink(karaaslanFull, "P_ma", plotKaraaslan, "Mean arterial pressure");
        return agentModel;
    }

    public static void testSimulation() throws Exception
    {
        Logger log = Logger.getLogger(SimulationAgent.class.getName());
        UniformSpan span = new UniformSpan(0, 6000, 0.01);
        Model model = new Solodyannikov();
        Simulator simulator = new JVodeSolver();
        model.init();
        simulator.init( model, model.getInitialValues(), span, new ResultListener[] {}, new FunctionJobControl( log ) );

        for( int i = 0; i < span.getLength(); i++ )
        {
            simulator.doStep();
        }
    }
}