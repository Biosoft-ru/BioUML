package biouml.plugins.hemodynamics._test;

import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.dynamics.EModel;
import biouml.plugins.hemodynamics.ArterialBinaryTreeModel;
import biouml.plugins.hemodynamics.EndResistancesListener;
import biouml.plugins.hemodynamics.HemodynamicsModelSolver;
import biouml.plugins.hemodynamics.HemodynamicsObserver;
import biouml.plugins.hemodynamics.HemodynamicsOptions;
import biouml.plugins.hemodynamics.HemodynamicsSimulationEngine;
import biouml.plugins.hemodynamics.Util;
import biouml.plugins.hemodynamics.Vessel;
import biouml.plugins.hemodynamics.VesselVisualizationListener;
import biouml.plugins.simulation.ResultPlotPane;
import biouml.standard.simulation.ResultListener;
import junit.framework.TestSuite;
import one.util.streamex.DoubleStreamEx;
import one.util.streamex.StreamEx;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.util.TempFiles;

public class TestArterialTree extends AbstractBioUMLTest implements ResultListener
{
    private static final double START_SEARCH_TIME = 0;
    private static final double COMPLETION_TIME = 3;

    private static final Object R_RADIAL = "R. Radial";;

    private static final Object AORTAL = "Ascending Aorta";
    private static final Object L_RADIAL = "L. Radial";

    public TestArterialTree(String name)
    {
        super(name);
    }

    public double AREA_FACTOR;
    
    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(TestArterialTree.class.getName());
        suite.addTest(new TestArterialTree("test"));
        return suite;
    }

    public void test() throws Exception
    {

        AREA_FACTOR = 1.7411011265;
        
        //        test("d0_0.01_10_1", 0.01, 1, 0, 10);
        //        test("d0_0.01_100_1", 0.01, 1, 0, 100);
        //        test("d0_0.001_10_1", 0.001, 1, 0, 10);
        //        test("d0_0.001_100_1", 0.001, 1, 0, 100);

//                test("d02_0.01_10_01", 0.01, 1, 0, 10);
//        test1(2276000.0, 0.43); //as in matlab
        testTree("Arterial Tree new", true, new String[]{"Brachiocefalic_3"});
//        test1(444000, 1.2*1.2*Math.PI, 1.2*1.2*Math.PI);
//        testV3(2E6, 2E6, 2E6, 3.6, 3.6, 3.6);
//        testTree("Arterial Tree new WP", true, "Ascending Aorta", "Aortic Arch I", "Aortic Arch II", "Thoracic Aorta I", "Thoracic Aorta II" , "Abdominal I", "Abdominal II",
//              "Abdominal III","Abdominal IV", "Abdominal V", "L. common Iliac", "L. external Iliac", "L. Femoral",
//              "L. posterior Tibial");
        
//        testTree("Arterial Tree new WP Matched Abdominal", true, "Ascending Aorta", "Aortic Arch I", "Aortic Arch II", "Thoracic Aorta I", "Thoracic Aorta II" , "Abdominal I", "Abdominal II",
//                "Abdominal III","Abdominal IV", "Abdominal V", "L. common Iliac", "L. external Iliac", "L. Femoral",
//                "L. posterior Tibial");
         
//        testTree("Arterial Tree new WP Matched Abdominal", true, new String[]{"Ascending Aorta", "Aortic Arch II", "Thoracic Aorta I", "Thoracic Aorta II" ,
//              "Abdominal III", "Abdominal V", "L. Femoral", "L. posterior Tibial", "L. Radial", "L. Brachial","L. Carotid"});//, "L. external Iliac", "L. Femoral",
              //"L. posterior Tibial"});
        
//        testTree("Arterial Tree new WP", true, new String[]{"Thoracic Aorta I", "Thoracic Aorta II", "Abdominal I"});
                
//        testTree("Arterial Tree new WP", true,  "Thoracic Aorta II", "Abdominal I", "Abdominal II", "Abdominal III");
//        testTree("Arterial Tree new WP", true, "L. Femoral","L. posterior Tibial");
        
//        testTree("Arterial Tree new WP", true, "Thoracic Aorta I", "Thoracic Aorta II", "Celiac I");
          
//        testTree("Arterial Tree new WP", true, "Abdominal I", "Superior Mesenteric");
        
//        testTree("Arterial Tree new WP", true, new String[]{"Ascending Aorta", "Aortic Arch I" , "Aortic Arch II", "Thoracic Aorta I","Thoracic Aorta II", "Abdominal I", "Superior Mesenteric"});
        
//        testTree("Arterial Tree new WP notmatched", true, "Ascending Aorta", "Aortic Arch I" , "Aortic Arch II", "Thoracic Aorta I", "Thoracic Aorta II", "Abdominal I", "Abdominal II",
//                "Abdominal III", "Abdominal IV", "Abdominal V", "L. common Iliac", "L. external Iliac", "L. posterior Tibial");
        
//        testTree("Arterial Tree new WP", true, "Ascending Aorta", "Aortic Arch I" , "Aortic Arch II", "Thoracic Aorta I", "Thoracic Aorta II", "Abdominal I", "Abdominal II",
//              "Abdominal III", "Abdominal IV", "Abdominal V", "L. common Iliac", "L. external Iliac", "L. deep Femoral");
//                "L. posterior Tibial");
//        testTree("Arterial Tree new WP Matched Abdominal", true, new String[]{"Abdominal I",
//              "Abdominal II", "Abdominal III", "Abdominal IV", "Abdominal V"});
//                new String[] {"Ascending Aorta", "Brachiocephalic", "R. Subclavian I", "R. Subclavian II", "R. Brachial", "R. Radial"},
//                new String[] {"Ascending Aorta", "Aortic Arch I", "Aortic Arch II", "Thoracic Aorta I", "Thoracic Aorta II", "Abdominal I",
//                        "Abdominal II", "Abdominal III", "Abdominal IV", "Abdominal V"});
//
//        testTree("Arterial Tree new WP", true, "Ascending Aorta", "Aortic Arch I", "Aortic Arch II", "L. Subclavian I", "L. Subclavian II", "L. Brachial", "L. Ulnar I",
//                "L. Ulnar II");

//        testTree(true, "Ascending Aorta", "Aortic Arch I", "Aortic Arch II");
//        testTree("Arterial Tree new WP", true, "Ascending Aorta", "Aortic Arch I", "L. Carotid", "L. external Carotid");
//        testTree("Abdominal V", "Ascending Aorta");
        
//        testTree("Ascending Aorta", "R. Carotid", "R. internal Carotid");
        //testTree("Ascending Aorta", "Brachiocephalic", "Aortic Arch I");
        
//        testTree("Aortic Arch I", "Aortic Arch II", "L. Carotid");
        //testV3_parallel(789036, 13458109, 1348109, 2.58, 0.1577, 0.1577);
//        testV2(1E6, 1E6, 1, 1);
        //testV3_parallel(47501763.204, 60972412.471, 65226301.713, 0.833, 0.423, 0.648);
//        testV3_parallel(1E6, 1E6, 1E6, 6, 6/AREA_FACTOR, 6/AREA_FACTOR);
        //testV3_parallel(47501763.204, 60972412.471, 65226301.713, 0.513, 0.095, 0.145);
        
//        testV3_parallel(47501763.204, 60972412.471, 6522630.1713, 0.833, 0.423, 0.283);

//        testV3_parallel(2E6, 10E7, 10E7, 0.4, 0.4, 0.4); //different
        
//        testV3_parallel(1E5, 1E6, 1E6, 0.5, 1, 1);
        
//        testV3_parallel(388000, 348000, 932000, 5.983, 5.147, 1.219); //1st
        
//        testV3_parallel(348000, 2076000, 520000, 5.147, 0.43, 3.142);
//                test("d02_0.001_10_01", 0.001, 0.1, 0.9, 10);
        //        test("d02_0.001_100_05", 0.001, 0.5, 0.5, 100);

        //        test("d04_0.01_10_05", 0.01, 0.5, 0.5, 10);
        //        test("d04_0.01_100_05", 0.01, 0.5, 0.5, 100);
        //        test("d04_0.001_10_05", 0.001, 0.5, 0.5, 10);
        //        test("d04_0.001_100_05", 0.001, 0.5, 0.5, 100);

        //        test("0.01_1_0_0", 0.01, 1, 0, 0);
        //        test("0.01_0.5_0_0.5", 0.01, 0.5, 0, 0.5);
        //        test("0.01_0.5_0.5_0", 0.01, 0.5, 0.5, 0);
        //        test("0.01_0.3_0.3_0.3", 0.01, 0.3, 0.3, 0.3);
        //        test("d0.2_0.01_10_0.6_0.4", 0.01, 0.6, 0.4, 10);
        //        test("d0.2_0.001_10_0.6_0.4", 0.001, 0.6, 0.4, 10);
        //        test("d0.2_0.01_100_0.6_0.4", 0.01, 0.6, 0.4, 100);
        //        test("d0.2_0.001_100_0.6_0.4", 0.001, 0.6, 0.4, 100);
        //        test("0.001_1_0_0", 0.001, 1, 0, 0);
        //        test("0.001_0.5_0_0.5", 0.001, 0.5, 0, 0.5);
        //        test("0.001_0.5_0.5_0", 0.001, 0.5, 0.5, 0);
        //        test("0.001_0.3_0.3_0.3", 0.001, 0.3, 0.3, 0.3);
    }

    public void testTree(String... vesselNames) throws Exception
    {
        testTree(false, vesselNames);
    }

    public void testTree(boolean samePlot, String... vesselNames) throws Exception
    {
        testTree("Arterial Tree new mm", samePlot, vesselNames);
    }
    
    public void testTree(String diagramName, boolean samePlot, String[]... vesselNames) throws Exception
    {
        Diagram diagram = getDiagram(diagramName);

        HemodynamicsSimulationEngine engine = new HemodynamicsSimulationEngine();
        engine.setDiagram(diagram);
        EModel emodel = diagram.getRole(EModel.class);
//        emodel.getVariable("outputFlow").setInitialValue(0);
//        emodel.getVariable("vesselSegments").setInitialValue(10);
//        emodel.getVariable("integrationSegments").setInitialValue(10);
//        emodel.getVariable("venousPressure").setInitialValue(0);
//
//        emodel.getVariable("totalVolume").setShowInPlot(true);
//        emodel.getVariable("capillaryResistance").setInitialValue(1.17);
//        emodel.getVariable("referencedPressure").setInitialValue(70);

        //small spike of input blood flow
//              emodel.getVariable("T_S").setInitialValue(0.01);
//              emodel.getVariable("SV").setInitialValue(6);

        ( (HemodynamicsOptions)engine.getSimulatorOptions() ).setInputCondition(HemodynamicsOptions.PRESSURE_INITIAL_CONDITION_STRING);
//        ( (HemodynamicsOptions)engine.getSimulatorOptions() ).setOutputCondition(HemodynamicsOptions.FILTRATION_INITIAL_CONDITION_STRING);
        engine.setCompletionTime(100);
 
//        
        //experiment with doubleing of vessels cross-sectional areas
//        Util.getVessels(diagram).forEach(v->{
//            v.setInitialArea(v.getInitialArea()*2); v.setInitialArea1(v.getInitialArea1()*2);
//        });
//      {
        
//        Util.getVessels(diagram).forEach(v->
//        {
//           if (v.getTitle().equals("Ascending Aorta") || v.getTitle().equals("Aortic Arch I") || v.getTitle().equals("Aortic Arch II") || v.getTitle().equals("Thoracic Aorta I")  || v.getTitle().equals("Thoracic Aorta II")
//                   || v.getTitle().equals("Abdominal I") || v.getTitle().equals("Abdominal II") || v.getTitle().equals("Abdominal III") || v.getTitle().equals("Abdominal IV") || v.getTitle().equals("Abdominal V"))
//           {
//               System.out.println(v.getTitle());
//               v.setBeta(v.getBeta()/2);
//           }
//        });
        
        Util.getVessels(diagram).forEach(v->v.setPlotPressure(false));
//        Util.getVessel(vesselNames[0], diagram).setPlotPressure(true);
        StreamEx.of(vesselNames).forEach(arr->StreamEx.of(arr).forEach(s->Util.getVessel(s, diagram).setPlotPressure(true)));
        StreamEx.of(vesselNames).forEach(arr->StreamEx.of(arr).forEach(s->Util.getVessel(s, diagram).setSegment(5)));

        List<ResultListener> listeners = new ArrayList<>();

        for (String[] vNames: vesselNames)
            listeners.add(new VesselVisualizationListener(engine, null, vNames));
        
//        if( samePlot )
//            listeners.add(new VesselVisualizationListener(engine, null, vesselNames));
//        else
//            for( String vname : vesselNames )
//                listeners.add(new VesselVisualizationListener(vname, engine, null));
        
        listeners.add(new ResultPlotPane(engine, null));
        listeners.add( new EndResistancesListener());
        
        engine.simulate(listeners.toArray(new ResultListener[listeners.size()]));
    }
    
    
    //TODO: more sophisticated test
    public void test1(double beta, double a) throws Exception
    {
        Diagram diagram = getDiagram("Arterial Tree new 1");

        HemodynamicsSimulationEngine engine = new HemodynamicsSimulationEngine();
        engine.setDiagram(diagram);
        HemodynamicsModelSolver solver = (HemodynamicsModelSolver)engine.getSimulator();
        EModel emodel = diagram.getRole(EModel.class);
//        emodel.getVariable("outputFlow").setInitialValue(0);
//        emodel.getVariable("HR").setInitialValue(60);
        emodel.getVariable("vesselSegments").setInitialValue(100);
//        emodel.getVariable("bloodViscosity").setInitialValue(1E-9);//0.00035);
//        emodel.getVariable("referencedPressure").setInitialValue(70);
//        emodel.getVariable("venousPressure").setInitialValue(10);
        emodel.getVariable("T_S").setInitialValue(0.3);
        emodel.getVariable("SV").setInitialValue(60);
//        emodel.getVariable("capillaryResistance").setInitialValue(0.584);//58);
//        ( (HemodynamicsOptions)engine.getSimulatorOptions() ).setInputCondition(HemodynamicsOptions.PRESSURE_INITIAL_CONDITION_STRING);
//        ( (HemodynamicsOptions)engine.getSimulatorOptions() ).setOutputCondition(HemodynamicsOptions.FILTRATION_INITIAL_CONDITION_STRING);
        Vessel v1 = Util.getVessel((Edge)diagram.get("Vessel"));
        
        v1.setSegment(50);
        v1.setBeta(beta);
        v1.setInitialArea(a);
        v1.setInitialArea1(a);
        v1.setLength(100);
        engine.setCompletionTime(200);
        HemodynamicsObserver observer = new HemodynamicsObserver();
        observer.skipCycles = 0;
        observer.searchTime = 1;

        EndResistancesListener resistanceListener = new EndResistancesListener();
        
        engine.simulate(new ResultListener[] {new VesselVisualizationListener("Ascending Aorta", engine, null),
                new ResultPlotPane(engine, null), resistanceListener, observer});
    }
    
    public void test1(double beta, double a, double aEnd) throws Exception
    {
        Diagram diagram = getDiagram("Arterial Tree new 1");

        HemodynamicsSimulationEngine engine = new HemodynamicsSimulationEngine();
        engine.setDiagram(diagram);
        HemodynamicsModelSolver solver = (HemodynamicsModelSolver)engine.getSimulator();
        EModel emodel = diagram.getRole(EModel.class);
//        emodel.getVariable("outputFlow").setInitialValue(0);
//        emodel.getVariable("HR").setInitialValue(60);
        emodel.getVariable("integrationSegments").setInitialValue(100);
        emodel.getVariable("vesselSegments").setInitialValue(100);
//        emodel.getVariable("bloodViscosity").setInitialValue(1E-9);//0.00035);
        emodel.getVariable("referencedPressure").setInitialValue(0);
        emodel.getVariable("T_S").setInitialValue(0.3);
        emodel.getVariable("SV").setInitialValue(60);
//        emodel.getVariable("capillaryResistance").setInitialValue(1.1);//58);
        emodel.getVariable("venousPressure").setInitialValue(0);//58);
        
        emodel.getVariable("capillaryResistance").setInitialValue(Math.sqrt(beta/(2*Math.sqrt(aEnd)))/1333);//58);
        
        ( (HemodynamicsOptions)engine.getSimulatorOptions() ).setInputCondition(HemodynamicsOptions.PRESSURE_INITIAL_CONDITION_STRING);
        ( (HemodynamicsOptions)engine.getSimulatorOptions() ).setOutputCondition(HemodynamicsOptions.FILTRATION_INITIAL_CONDITION_STRING);
        Vessel v1 = Util.getVessel((Edge)diagram.get("Vessel"));
        v1.setBeta(beta);
        v1.setInitialArea(a);
        v1.setInitialArea1(aEnd);
        v1.setLength(100);
        engine.setCompletionTime(100);
        HemodynamicsObserver observer = new HemodynamicsObserver();
        observer.skipCycles = 0;
        observer.searchTime = 1;

        engine.simulate(new ResultListener[] {new VesselVisualizationListener("Ascending Aorta", engine, null),
                new ResultPlotPane(engine, null), observer});
    }

    public void testV3(String resultName, double timeStep, double kp, double kp1, int steps) throws Exception
    {
        Diagram diagram = getDiagram("Arterial 3 sequence");

        HemodynamicsSimulationEngine engine = new HemodynamicsSimulationEngine();
        engine.setDiagram(diagram);
        HemodynamicsModelSolver solver = (HemodynamicsModelSolver)engine.getSimulator();
        EModel emodel = diagram.getRole(EModel.class);
        emodel.getVariable("HR").setInitialValue(30);
//        emodel.getVariable("vesselSegments").setInitialValue(100);
        emodel.getVariable("integrationSegments").setInitialValue(steps);
        ( (HemodynamicsOptions)engine.getSimulatorOptions() ).setInputCondition(HemodynamicsOptions.PRESSURE_INITIAL_CONDITION_STRING);
//        ( (HemodynamicsOptions)engine.getSimulatorOptions() ).setOutputCondition(HemodynamicsOptions.PRESSURE_INITIAL_CONDITION_STRING);
        
        //        engine.setTimeIncrement( timeStep );
        engine.setTimeIncrement(timeStep);
        engine.setCompletionTime(20);

        HemodynamicsObserver observer = new HemodynamicsObserver();
        observer.skipCycles = 0;
        observer.searchTime = 0.3;
        VesselVisualizationListener l1 = new VesselVisualizationListener("Ascending Aorta", engine, null);
        VesselVisualizationListener l2 = new VesselVisualizationListener("v2", engine, null);
        VesselVisualizationListener l3 = new VesselVisualizationListener("v3", engine, null);
        engine.simulate(new ResultListener[] {l1, l2, l3, new ResultPlotPane(engine, null), observer});

    }
    
    public void testV2(double beta1, double beta2, double a1, double a2) throws Exception
    {
        Diagram diagram = getDiagram("Arterial 2 sequence");

        HemodynamicsSimulationEngine engine = new HemodynamicsSimulationEngine();
        engine.setDiagram(diagram);
        HemodynamicsModelSolver solver = (HemodynamicsModelSolver)engine.getSimulator();
        EModel emodel = diagram.getRole(EModel.class);
        emodel.getVariable("HR").setInitialValue(60);
        emodel.getVariable("T_S").setInitialValue(0.05);
        emodel.getVariable("SV").setInitialValue(0.2);
        emodel.getVariable("outputFlow").setInitialValue(0);
        emodel.getVariable("integrationSegments").setInitialValue(100);
        emodel.getVariable("vesselSegments").setInitialValue(100);
        emodel.getVariable("referencedPressure").setInitialValue(70);
        //emodel.getVariable("bloodViscosity").setInitialValue(0.00000035);
        ( (HemodynamicsOptions)engine.getSimulatorOptions() ).setInputCondition(HemodynamicsOptions.FLUX_INITIAL_CONDITION_STRING);
        ( (HemodynamicsOptions)engine.getSimulatorOptions() ).setOutputCondition(HemodynamicsOptions.FILTRATION_INITIAL_CONDITION_STRING);

        engine.setCompletionTime(20);

        Vessel v1 = (Vessel)diagram.get("Vessel").getAttributes().getValue("vessel");
        v1.setBeta(beta1);
        v1.setInitialArea(a1);
        v1.setInitialArea1(a1);
        v1.setLength(150);
        Vessel v2 = (Vessel)diagram.get("v2").getAttributes().getValue("vessel");
        v2.setBeta(beta2);
        v2.setLength(150);
        v2.setInitialArea(a2);
        v2.setInitialArea1(a2);
        
        HemodynamicsObserver observer = new HemodynamicsObserver();
        observer.skipCycles = 0;
        observer.searchTime = 0.3;
        VesselVisualizationListener l1 = new VesselVisualizationListener(engine, null, "Ascending Aorta", "v2");
//        VesselVisualizationListener l2 = new VesselVisualizationListener("v2", engine, null);
        engine.simulate(new ResultListener[] {l1, new ResultPlotPane(engine, null), observer});
    }
    
    public void testV3(double beta1, double beta2, double beta3,  double a1, double a2, double a3) throws Exception
    {
        Diagram diagram = getDiagram("Arterial 3 sequence");

        HemodynamicsSimulationEngine engine = new HemodynamicsSimulationEngine();
        engine.setDiagram(diagram);
        EModel emodel = diagram.getRole(EModel.class);
        emodel.getVariable("HR").setInitialValue(60);
        emodel.getVariable("T_S").setInitialValue(0.1);
        emodel.getVariable("SV").setInitialValue(60);
        emodel.getVariable("outputFlow").setInitialValue(0);
        emodel.getVariable("integrationSegments").setInitialValue(100);
        emodel.getVariable("vesselSegments").setInitialValue(100);
        emodel.getVariable("referencedPressure").setInitialValue(70);
        ( (HemodynamicsOptions)engine.getSimulatorOptions() ).setInputCondition(HemodynamicsOptions.FLUX_INITIAL_CONDITION_STRING);
        ( (HemodynamicsOptions)engine.getSimulatorOptions() ).setOutputCondition(HemodynamicsOptions.FILTRATION_INITIAL_CONDITION_STRING);

        engine.setCompletionTime(20);

        Vessel v1 = (Vessel)diagram.get("Vessel").getAttributes().getValue("vessel");
        v1.setBeta(beta1);
        v1.setInitialArea(a1);
        v1.setInitialArea1(a1);
        v1.setLength(50);
        Vessel v2 = (Vessel)diagram.get("v2").getAttributes().getValue("vessel");
        v2.setBeta(beta2);
        v2.setLength(50);
        v2.setInitialArea(a2);
        v2.setInitialArea1(a2);
        Vessel v3 = (Vessel)diagram.get("v3").getAttributes().getValue("vessel");
        v3.setBeta(beta3);
        v3.setLength(50);
        v3.setInitialArea(a3);
        v3.setInitialArea1(a3);
        
        HemodynamicsObserver observer = new HemodynamicsObserver();
        observer.skipCycles = 0;
        observer.searchTime = 0.3;
        VesselVisualizationListener l1 = new VesselVisualizationListener(engine, null, "Ascending Aorta", "v2", "v3");
        engine.simulate(new ResultListener[] {l1, new ResultPlotPane(engine, null), observer});
    }
    
    public void testV3_parallel(double beta1, double beta2, double beta3, double a1, double a2, double a3) throws Exception
    {
        Diagram diagram = getDiagram("Arterial 3 parallel");

        HemodynamicsSimulationEngine engine = new HemodynamicsSimulationEngine();
        engine.setDiagram(diagram);
        HemodynamicsModelSolver solver = (HemodynamicsModelSolver)engine.getSimulator();
        EModel emodel = diagram.getRole(EModel.class);
        emodel.getVariable("T_S").setInitialValue(0.3);
        emodel.getVariable("SV").setInitialValue(60);
        emodel.getVariable("HR").setInitialValue(60);
        emodel.getVariable("outputFlow").setInitialValue(0);
        emodel.getVariable("referencedPressure").setInitialValue(70);
//        emodel.getVariable("vesselSegments").setInitialValue(100);
        emodel.getVariable("integrationSegments").setInitialValue(300);
//        ( (HemodynamicsOptions)engine.getSimulatorOptions() ).setInputCondition(HemodynamicsOptions.PRESSURE_INITIAL_CONDITION_STRING);
                engine.setTimeIncrement( 0.001 );
//        engine.setTimeIncrement(timeStep);
        engine.setCompletionTime(20);

        Vessel v1 = (Vessel)diagram.get("Vessel").getAttributes().getValue("vessel");
        v1.setBeta(beta1);
        v1.setInitialArea(a1);
        v1.setInitialArea1(a1);
        v1.setLength(100);
        
        Vessel v2 = (Vessel)diagram.get("v2").getAttributes().getValue("vessel");
        v2.setBeta(beta2);
        v2.setInitialArea(a2);
        v2.setInitialArea1(a2);
        v2.setLength(300);
        
        Vessel v3 = (Vessel)diagram.get("v3").getAttributes().getValue("vessel");
        v3.setBeta(beta3);
        v3.setInitialArea(a3);
        v3.setInitialArea1(a3);
        v3.setLength(300);
        
//        ( (HemodynamicsOptions)engine.getSimulatorOptions() ).setInputCondition(HemodynamicsOptions.PRESSURE_INITIAL_CONDITION_STRING);
//        ( (HemodynamicsOptions)engine.getSimulatorOptions() ).setOutputCondition(HemodynamicsOptions.FLUX_INITIAL_CONDITION_STRING);
        
        HemodynamicsObserver observer = new HemodynamicsObserver();
        observer.skipCycles = 0;
        observer.searchTime = 0.3;
        //        solver.beta1 = 3.5;//5;//0.1;
        //        solver.beta3 = 3.5;//0.1;
        //        solver.beta2 = 3.5;//0.1;
        //        solver.kp = kp;
        //        solver.kp1 = kp1;
        //        solver.pressureDelay = 0.2;
        //        solver.gamma = 0.2;
        //        solver.kp2 = kp2;
        double time = System.currentTimeMillis();
        VesselVisualizationListener l1 = new VesselVisualizationListener("Ascending Aorta", engine, null);
        VesselVisualizationListener l2 = new VesselVisualizationListener("v2", engine, null);
        VesselVisualizationListener l3 = new VesselVisualizationListener("v3", engine, null);
        engine.simulate(new ResultListener[] {l1, l2, l3, new ResultPlotPane(engine, null), observer});
        //        engine.simulate( new ResultListener[] {this, new ResultPlotPane( engine, null )} );

        //System.out.println( ( System.currentTimeMillis() - time ) / 1000 );

        //        write("kappa_result", resultName);
    }

    protected String outputDir = TempFiles.path("simulation").getAbsolutePath();

    public Diagram getDiagram(String name) throws Exception
    {
        CollectionFactory.createRepository("../data");
        return DataElementPath.create("databases/Virtual Human/Diagrams/"+name).getDataElement(Diagram.class);
        //        return DataElementPath.create("databases/Virtual Human/Diagrams/Arterial Brachial new").getDataElement( Diagram.class );
    }

    protected void write(String fileName, String resultName) throws Exception
    {
        try (Writer bw = ApplicationUtils.utfAppender(AbstractBioUMLTest.getTestFile(fileName)))
        {
            bw.write("\nn" + resultName + "\n");
            bw.write("\n" + "times" + "\t");
            bw.write(DoubleStreamEx.of(timeValues).joining("\t"));
            bw.write("\n" + "aorta_" + resultName + "\t");
            bw.write(DoubleStreamEx.of(aortaValues).joining("\t"));
            bw.write("\n" + "rradial_" + resultName + "\t");
            bw.write(DoubleStreamEx.of(rradialValues).joining("\t"));
            bw.write("\n" + "lradial_" + resultName + "\t");
            bw.write(DoubleStreamEx.of(lradialValues).joining("\t"));
            bw.write("\n" + "rradial_End" + resultName + "\t");
            bw.write(DoubleStreamEx.of(rradialValuesEnd).joining("\t"));
            bw.write("\n" + "lradial_End" + resultName + "\t");
            bw.write(DoubleStreamEx.of(lradialValuesEnd).joining("\t"));
        }
    }

    @Override
    public void start(Object model)
    {
        // TODO Auto-generated method stub
        this.atm = (ArterialBinaryTreeModel)model;
        aortaValues = new ArrayList<>();
        rradialValues = new ArrayList<>();
        lradialValues = new ArrayList<>();
        rradialValuesEnd = new ArrayList<>();
        lradialValuesEnd = new ArrayList<>();
        timeValues = new ArrayList<>();
    }

    ArterialBinaryTreeModel atm;
    @Override
    public void add(double t, double[] y) throws Exception
    {
        if( t > START_SEARCH_TIME && Math.abs(t / 0.01 - Math.round(t / 0.01)) < 0.0001 && t < COMPLETION_TIME )
        {
            aortaValues.add(atm.vesselMap.get(AORTAL).pressure[1]);
            lradialValues.add(atm.vesselMap.get(L_RADIAL).pressure[1]);
            rradialValues.add(atm.vesselMap.get(R_RADIAL).pressure[1]);

            lradialValuesEnd.add(atm.vesselMap.get(L_RADIAL).pressure[(int)atm.vesselSegments - 1]);
            rradialValuesEnd.add(atm.vesselMap.get(R_RADIAL).pressure[(int)atm.vesselSegments - 1]);
            timeValues.add(t);
        }
        // TODO Auto-generated method stub

    }

    List<Double> aortaValues;
    List<Double> lradialValues;
    List<Double> rradialValues;
    List<Double> lradialValuesEnd;
    List<Double> rradialValuesEnd;
    List<Double> timeValues;
}
