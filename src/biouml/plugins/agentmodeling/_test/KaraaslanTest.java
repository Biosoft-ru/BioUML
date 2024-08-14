package biouml.plugins.agentmodeling._test;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import biouml.plugins.agentmodeling.AgentBasedModel;
import biouml.plugins.agentmodeling.AgentModelSimulationEngine;
import biouml.plugins.agentmodeling.ModelAgent;
import biouml.plugins.agentmodeling.PlotAgent;
import biouml.plugins.agentmodeling._test.models.Karaaslan_new;
import biouml.plugins.simulation.Span;
import biouml.plugins.simulation.UniformSpan;
import biouml.plugins.simulation.ode.jvode.JVodeSolver;

/**
 * Test for heart agent model consisting of 6 agents: ArterialSystem, VenousSystem, Ventricle, NeuroHumoralControlSystem, Capillary
 * @author axec
 *
 */
public class KaraaslanTest extends TestCase
{
    static Span span = new UniformSpan(0, 100000, 1);

    public KaraaslanTest(String name)
    {
        super(name);
    }


    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(KaraaslanTest.class.getName());
        suite.addTest(new KaraaslanTest("test"));
        return suite;
    }

    public static void main(String ... args) throws Exception
    {
        new AgentModelSimulationEngine().simulate(generateModel());
        Thread.sleep((long)1E9);
    }

    private static AgentBasedModel generateModel() throws Exception
    {

        AgentBasedModel agentModel = new AgentBasedModel();
        JVodeSolver solver = new JVodeSolver();
        solver.getOptions().setRtol(1E-7);
        ModelAgent kidney = new ModelAgent(new Karaaslan_new(), solver, span, "kidney");


//        Karaaslan_new model = new Karaaslan_new();
//        model.init();
//        NewtonSolver.solve(algebraicResult, model);

//        JVodeSolver solverNew = new JVodeSolver();
//        ( (JVodeOptions)solverNew.getOptions() ).setRtol(1E-7);
//        ModelAgent kidneyNew = new ModelAgent(new KidneyModel(), solverNew, span, "kidneyNew");


        PlotAgent plot2 = new PlotAgent("plot", new UniformSpan(0, 20000, 100));

        agentModel.addAgent(kidney);
//        agentModel.addAgent(kidneyNew);
//        agentModel.addAgent(plot);
        //        agentModel.addAgent(plot1);
                agentModel.addAgent(plot2);


        //        agentModel.addDirectedLink(kidneyNew, "P_ma", plot1, "P_ma_new");
        //        agentModel.addDirectedLink(kidney, "P_ma", plot1, "P_ma");
        //        agentModel.addDirectedLink(kidneyNew, "Fi_md_sod", plot1, "Fi_md_sod_new");
        //        agentModel.addDirectedLink(kidney, "Fi_md_sod", plot1, "Fi_md_sod");

        //        agentModel.addDirectedLink(kidneyNew, "Fi_dt_sodreab", plot1, "Fi_dt_sodreab_new21");
        //        agentModel.addDirectedLink(kidney, "Fi_dt_sodreab", plot1, "Fi_dt_sodreab");
        //        agentModel.addDirectedLink(kidneyNew, "Fi_md_sod", plot1, "Fi_md_sod_new");
        //        agentModel.addDirectedLink(kidney, "Fi_md_sod", plot1, "Fi_md_sod");
        //        agentModel.addDirectedLink(kidneyNew, "Fi_u_sod", plot1, "Fi_u_sod_new");
        //        agentModel.addDirectedLink(kidney, "Fi_u_sod", plot1, "Fi_u_sod");
        //        agentModel.addDirectedLink(kidneyNew, "Fi_dt_sod", plot1, "Fi_dt_sod_new");
        //        agentModel.addDirectedLink(kidney, "Fi_dt_sod", plot1, "Fi_dt_sod");
        //        agentModel.addDirectedLink(kidneyNew, "Fi_cd_sodreab", plot1, "Fi_cd_sodreab_new");
        //        agentModel.addDirectedLink(kidney, "Fi_cd_sodreab", plot1, "Fi_cd_sodreab");

//                agentModel.addDirectedLink(kidneyNew, "P_ma", plot2, "P_ma_new");
                agentModel.addDirectedLink(kidney, "P_ma", plot2, "P_ma");
        //        agentModel.addDirectedLink(kidneyNew, "P_ra", plot2, "P_ra_new");
        //        agentModel.addDirectedLink(kidney, "P_ra", plot2, "P_ra");
        //        agentModel.addDirectedLink(kidneyNew, "Fi_win", plot2, "Fi_win_new");
        //        agentModel.addDirectedLink(kidney, "Fi_win", plot2, "Fi_win");
        //        agentModel.addDirectedLink(kidneyNew, "Fi_u", plot2, "Fi_u_new");
        //        agentModel.addDirectedLink(kidney, "Fi_u", plot2, "Fi_u");
        //        agentModel.addDirectedLink(kidneyNew, "rsna", plot2, "rsna_new");
        //        agentModel.addDirectedLink(kidney, "rsna", plot2, "rsna");

        //        agentModel.addDirectedLink(/kidneyNew, "Fi_gfilt", plot1, "Fi_gfilt_new");
        //        agentModel.addDirectedLink(kidneyNew, "Fi_filsod", plot1, "Fi_filsod_new");
        //        agentModel.addDirectedLink(kidneyNew, "Fi_md_sod", plot1, "Fi_md_sod_new");
        //        agentModel.addDirectedLink(kidneyNew, "C_adh", plot, "C_adh_new");
//        agentModel.addDirectedLink(kidney, "Fi_sodin", kidneyNew, "Fi_sodin");
        //        agentModel.addDirectedLink(kidney, "Fi_sodin", plot, "Fi_sodin");
        //        agentModel.addDirectedLink(kidneyNew, "Fi_sodin", plot, "Fi_sodinNew");
//        agentModel.addDirectedLink(kidney, "V_b", plot, "V_b");
//        plot.setSpec("V_b", 2.0f, Color.black, new float[] {6, 3});
        
//        agentModel.addDirectedLink(kidney, "C_anp", plot, "C_anp");
//        agentModel.addDirectedLink(kidney, "C_al", plot, "C_al");
//        agentModel.addDirectedLink(kidney, "rsna", plot, "rsna");
//
//        agentModel.addDirectedLink(kidneyNew, "V_b", plot, "V_b_");
//        agentModel.addDirectedLink(kidneyNew, "C_anp", plot, "C_anp_");
//        agentModel.addDirectedLink(kidneyNew, "C_al", plot, "C_al_");
//        agentModel.addDirectedLink(kidneyNew, "rsna", plot, "rsna_");
//        agentModel.addDirectedLink(kidneyNew, "Fi_co", plot, "Fi_co_new");

        //        agentModel.addDirectedLink(kidneyNew, "0", plot, "0_new");
        //        agentModel.addDirectedLink(kidneyNew, "1", plot, "1_new");
        //        agentModel.addDirectedLink(kidneyNew, "2", plot, "2_new");
        //        agentModel.addDirectedLink(kidneyNew, "3", plot, "3_new");
        //        agentModel.addDirectedLink(kidneyNew, "4", plot, "4_new");
        //        agentModel.addDirectedLink(kidneyNew, "5", plot, "5_new");
        //        agentModel.addDirectedLink(kidneyNew, "6", plot, "6_new");
        //        agentModel.addDirectedLink(kidneyNew, "7", plot, "7_new");

        //        agentModel.addDirectedLink(kidney, "P_ma", plot, "P_ma");
        //        agentModel.addDirectedLink(kidney, "Fi_gfilt", plot1, "Fi_gfilt");
        //        agentModel.addDirectedLink(kidney, "Fi_filsod", plot1, "Fi_filsod");
        //        agentModel.addDirectedLink(kidney, "Fi_md_sod", plot1, "Fi_md_sod");
        //        agentModel.addDirectedLink(kidney, "C_adh", plot, "C_adh");
        //        agentModel.addDirectedLink(kidney, "V_b", plot, "V_b");
        //        agentModel.addDirectedLink(kidney, "Fi_co", plot, "Fi_co_new");
        //        agentModel.addDirectedLink(kidney, "0", plot, "0");
        //        agentModel.addDirectedLink(kidney, "1", plot, "1");
        //        agentModel.addDirectedLink(kidney, "2", plot, "2");
//        agentModel.addDirectedLink(kidneyNew, "3", plot, "3");
//        agentModel.addDirectedLink(kidney, "4", plot, "4");
        //        agentModel.addDirectedLink(kidneyNew, "4", plot, "4_");
        //        agentModel.addDirectedLink(kidney, "5", plot, "5");
        //        agentModel.addDirectedLink(kidney, "6", plot, "6");
        //        agentModel.addDirectedLink(kidney, "7", plot, "7");


        return agentModel;
    }
    
}