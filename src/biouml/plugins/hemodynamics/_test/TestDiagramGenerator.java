package biouml.plugins.hemodynamics._test;

import java.util.ArrayList;
import java.util.List;

import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.plugins.hemodynamics.HemodynamicsDiagramGenerator;
import biouml.plugins.hemodynamics.HemodynamicsOptions;
import biouml.plugins.hemodynamics.HemodynamicsSimulationEngine;
import biouml.plugins.hemodynamics.Util;
import biouml.plugins.hemodynamics.Vessel;
import biouml.plugins.hemodynamics.VesselVisualizationListener;
import biouml.plugins.simulation.ResultPlotPane;
import biouml.standard.simulation.ResultListener;
import junit.framework.TestSuite;
import one.util.streamex.StreamEx;
import ru.biosoft.access._test.AbstractBioUMLTest;

public class TestDiagramGenerator extends AbstractBioUMLTest
{
    public TestDiagramGenerator(String name)
    {
        super(name);
    }

    static double AREA_FACTOR;

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(TestDiagramGenerator.class.getName());
        suite.addTest(new TestDiagramGenerator("test"));
        return suite;
    }



    public void test()
    {
        try
        {
            Diagram diagram = generateTree();

            EModel emodel = diagram.getRole(EModel.class);
            emodel.getVariable("outputFlow").setInitialValue(0);
            emodel.getVariable("HR").setInitialValue(30);
            emodel.getVariable("vesselSegments").setInitialValue(50);
            emodel.getVariable("integrationSegments").setInitialValue(10);
            emodel.getVariable("T_S").setInitialValue(0.3);
            emodel.getVariable("SV").setInitialValue(60);

            emodel.getVariable("venousPressure").setInitialValue(70);
            HemodynamicsSimulationEngine engine = new HemodynamicsSimulationEngine();
            ( (HemodynamicsOptions)engine.getSimulatorOptions() ).setInputCondition(HemodynamicsOptions.FLUX_INITIAL_CONDITION_STRING);
            ( (HemodynamicsOptions)engine.getSimulatorOptions() )
                    .setOutputCondition(HemodynamicsOptions.FILTRATION_INITIAL_CONDITION_STRING);

            engine.setDiagram(diagram);
            engine.setTimeIncrement(0.001);
            engine.setCompletionTime(100);
            List<ResultListener> listeners = new ArrayList<>();

            StreamEx.of("v1", "v11", "v111", "v1111").forEach(s -> Util.getVessel(s, diagram).setPlotPressure(true));
            //            Util.getVessels(diagram).forEach(v->v.setPlotPressure(true));
            //            emodel.getVariable("inputFlow").setShowInPlot(true);

            //            listeners.add(new VesselVisualizationListener(engine, null, "v1", "v11", "v111", "v1111"));
            listeners.add(new VesselVisualizationListener(engine, null, "v1", "v11", "v111", "v1111"));
            listeners.add(new VesselVisualizationListener(engine, null, "v1", "v12", "v121", "v1211"));
            listeners.add(new ResultPlotPane(engine, null));
            engine.simulate(listeners.toArray(new ResultListener[listeners.size()]));
        }
        catch( Exception ex )
        {
            ex.printStackTrace();
        }

    }
    
    public static Diagram generateTree() throws Exception
    {
        AREA_FACTOR = 1.7411011265;

        double startA = 6;
        double beta = 1E8;
        double length = 100;
        double a = startA;

        HemodynamicsDiagramGenerator generator = new HemodynamicsDiagramGenerator("test");
        generator.createDefaultEuations();

        Vessel v1 = new Vessel("v1", length, a, beta);
        a /= AREA_FACTOR;
        Vessel v11 = new Vessel("v11", length, a, beta);
        Vessel v12 = new Vessel("v12", length, a, beta);

        a /= AREA_FACTOR;
        Vessel v111 = new Vessel("v111", length, a, beta);
        Vessel v112 = new Vessel("v112", length, a, beta);
        Vessel v121 = new Vessel("v121", length, a, beta);
        Vessel v122 = new Vessel("v122", length, a, beta);

        a /= AREA_FACTOR;
        length *= 10;
        Vessel v1111 = new Vessel("v1111", length, a, beta);
        Vessel v1112 = new Vessel("v1112", length, a, beta);
        Vessel v1121 = new Vessel("v1121", length, a, beta);
        Vessel v1122 = new Vessel("v1122", length, a, beta);
        Vessel v1211 = new Vessel("v1211", length, a, beta);
        Vessel v1212 = new Vessel("v1212", length, a, beta);
        Vessel v1221 = new Vessel("v1221", length, a, beta);
        Vessel v1222 = new Vessel("v1222", length, a, beta);

        generator.addRootVessel(v1);
        generator.addVessel(v1, v11);
        generator.addVessel(v1, v12);
        generator.addVessel(v11, v111);
        generator.addVessel(v11, v112);
        generator.addVessel(v12, v121);
        generator.addVessel(v12, v122);
        generator.addVessel(v111, v1111);
        generator.addVessel(v111, v1112);
        generator.addVessel(v112, v1121);
        generator.addVessel(v112, v1122);
        generator.addVessel(v121, v1211);
        generator.addVessel(v121, v1212);
        generator.addVessel(v122, v1221);
        generator.addVessel(v122, v1222);

        return generator.getDiagram();
    }

}
