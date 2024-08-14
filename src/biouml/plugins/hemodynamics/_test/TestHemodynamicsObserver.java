package biouml.plugins.hemodynamics._test;

import java.io.Writer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.plugins.hemodynamics.ArterialBinaryTreeModel;
import biouml.plugins.hemodynamics.HemodynamicsObserver;
import biouml.plugins.hemodynamics.HemodynamicsObserver.VesselInfo;
import biouml.plugins.hemodynamics.HemodynamicsOptions;
import biouml.plugins.hemodynamics.HemodynamicsSimulationEngine;
import biouml.plugins.hemodynamics.SimpleVessel;
import biouml.plugins.hemodynamics.Vessel;
import biouml.plugins.hemodynamics._test.VelocityTest.ChainComparator;
import biouml.plugins.simulation.ResultPlotPane;
import biouml.standard.simulation.ResultListener;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementPath;

public class TestHemodynamicsObserver extends TestCase
{
    public TestHemodynamicsObserver(String name)
    {
        super(name);
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(TestHemodynamicsObserver.class.getName());
//                suite.addTest(new TestHemodynamicsObserver("test"));
        suite.addTest(new TestHemodynamicsObserver("testSingleEdge"));
        return suite;
    }

    public void test() throws Exception
    {
    	doTest("Arterial Brachial new", 0.001, 30, 30, 10, "PWV_30_hr10.txt", "PWV_30_hr10_full.txt");
//    	doTest("Arterial Brachial new", 0.001, 20, 20, 30, "PWV_20_hr30.txt", "PWV_10_hr30_full.txt");
//    	doTest("Arterial Brachial new", 0.001, 50, 50, 30, "PWV_50_hr30.txt", "PWV_50_hr30_full.txt");
//
//        doTest("Arterial Brachial 2", 0.001, 10, 10, 30, "PWV_10_hr30.txt", "PWV_10_hr30_full.txt");
//        doTest("Arterial Tree Equal RH", 0.01, 10, 10, 30, "PWV_RH_10_hr30.txt", "PWV_RH_10_hr30_full.txt");
//        doTest("Arterial Tree WP", 0.01, 10, 10, 30, "PWV_WP_10_hr30.txt", "PWV_WP_10_hr30_full.txt");
//
//        doTest("Arterial Brachial 2", 0.001, 20, 20, 30, "PWV_20_hr30.txt", "PWV_20_hr30_full");
//        doTest("Arterial Tree Equal RH", 0.01, 20, 20, 30, "PWV_RH_20_hr30.txt", "PWV_RH_20_hr30_full");
//        doTest("Arterial Tree WP", 0.01, 20, 20, 30, "PWV_WP_20_hr30.txt", "PWV_WP_20_hr30_full");
//
//        doTest("Arterial Brachial 2", 0.001, 40, 20, 30, "PWV_40_20_hr30.txt", "PWV_40_20_hr30_full");
//        doTest("Arterial Tree Equal RH", 0.01, 40, 20, 30, "PWV_RH_40_20_hr30.txt", "PWV_RH_40_20_hr30_full");
//        doTest("Arterial Tree WP", 0.01, 40, 20, 30, "PWV_WP_40_20_hr30.txt", "PWV_WP_40_20_hr30_full");
    }

    public void testSingleEdge() throws Exception
    {
//        doTestSingleVessel("Arterial One Edge", 0.001, 10, 10, 30, "singleEdgePWV", "singleEdgeME");
        
//        doTestSingleVessel("Arterial One Edge", 0.0005, 30, 30, 30, "singleEdgePWV_pressure30_0.0005", "singleEdgeME_pressure30_0.0005");
//        doTestSingleVessel("Arterial One Edge", 0.0005, 50, 50, 30, "singleEdgePWV_pressure50_0.0005", "singleEdgeME_pressure50_0.0005");
//        doTestSingleVessel("Arterial One Edge", 0.0002, 100, 100, 5, "singleEdgePWV_pressure100_0.0002_5hr", "singleEdgeME_pressure100_0.0002_5hr");
//        doTestSingleVessel("Arterial One Edge", 0.0001, 100, 100, 5, "singleEdgePWV_pressure100_0.0001_5hr", "singleEdgeME_pressure100_0.0001_5hr");
//        doTestSingleVessel("Arterial One Edge", 0.0005, 10, 10, 5, "singleEdgePWV_pressure10_0.0005_5hr", "singleEdgeME_pressure10_0.0005_5hr");
//        doTestSingleVessel("Arterial One Edge", 0.0001, 50, 50, 30, "singleEdgePWV_pressure50_0.0001", "singleEdgeME_pressure50_0.0001");
//        doTestSingleVessel("Arterial One Edge", 0.001, 10, 10, 30, "singleEdgePWV_pressure10", "singleEdgeME_pressure10");
//        doTestSingleVessel("Arterial One Edge", 0.001, 30, 30, 30, "singleEdgePWV_pressure30", "singleEdgeME_pressure30");
//        doTestSingleVessel("Arterial One Edge", 0.001, 100, 100, 30, "singleEdgePWV_pressure100", "singleEdgeME_pressure100");
//        doTestSingleVessel("Arterial One Edge", 0.0005, 10, 10, 30, "singleEdgePWV_pressure10_0.0005", "singleEdgeME_pressure10_0.0005");
        
//        doTestSingleVessel2("Arterial One Edge", new double[]{0.01}, new double[]{10}, new double[]{10}, 5, 5, 300000, "se_a5_beta300000");
        doTestSingleVessel2("Arterial One Edge", timeSteps, vesselSegments, vesselSegments, 10, 1, 1E6, "a1b1E6_new");
        doTestSingleVessel2("Arterial One Edge", timeSteps, vesselSegments, vesselSegments, 10, 1, 1E7, "a1b1E7_new");
//        doTestSingleVessel2("Arterial One Edge", timeSteps, vesselSegments, vesselSegments, 10, 0.5, 1E7, "a05b1E7");
//        doTestSingleVessel2("Arterial One Edge", timeSteps, vesselSegments, vesselSegments, 10, 5, 1E7, "a5b1E7");
//        doTestSingleVessel2("Arterial One Edge", timeSteps, vesselSegments, vesselSegments, 10, 5, 1E6, "a5b1E6");
        
//        doTestSingleVessel2("Arterial One Edge", timeSteps, vesselSegments, vesselSegments, 10, 0.2, 1E6, "a02b1E6");
//        doTestSingleVessel2("Arterial One Edge", timeSteps, vesselSegments, vesselSegments, 10, 0.1, 1E6, "a01b1E6");
    }

    
    static final double[] timeSteps = new double[]{0.01, 0.005, 0.001, 0.0005, 0.0002};
    static final double[] vesselSegments = new double[]{10, 20, 30, 50, 100};
            
    public void doTest(String diagramName, double timeStep, double vesselSegments, double integrationSegments, double heartrate,
            String fileName, String fileName2) throws Exception
    {
        Diagram d = getDiagram(diagramName);
        EModel eModel = d.getRole(EModel.class);
        eModel.getVariable("vesselSegments").setInitialValue(vesselSegments);
        eModel.getVariable("integrationSegments").setInitialValue(integrationSegments);
        eModel.getVariable("HR").setInitialValue(heartrate);
        HemodynamicsObserver observer = new HemodynamicsObserver();
        simulate(d, timeStep, observer, fileName, fileName2);
    }


    public void simulate(Diagram diagram, double timeStep, HemodynamicsObserver listener, String fileName, String fileName2)
            throws Exception
    {
        try (Writer writer = ApplicationUtils.utfWriter(AbstractBioUMLTest.getTestFile(fileName));
                Writer writer2 = ApplicationUtils.utfWriter(AbstractBioUMLTest.getTestFile(fileName2)))
        {

            listener.setWriter(writer2);

            HemodynamicsSimulationEngine engine = new HemodynamicsSimulationEngine();

            engine.setDiagram(diagram);
            engine.setInitialTime(0);
            engine.setTimeIncrement(timeStep);
            engine.setCompletionTime(16);
            ( (HemodynamicsOptions)engine.getSimulatorOptions() ).setInputCondition(HemodynamicsOptions.PRESSURE_INITIAL_CONDITION_STRING );
            ( (HemodynamicsOptions)engine.getSimulatorOptions() )
                    .setOutputCondition(HemodynamicsOptions.FILTRATION_INITIAL_CONDITION_STRING);//.PRESSURE_INITIAL_CONDITION_STRING );
            ( (HemodynamicsOptions)engine.getSimulatorOptions() ).setUseFullPressureConservation(false);
            ArterialBinaryTreeModel model = engine.createModel();
            engine.simulate(model, new ResultListener[] {listener});

            listener.complete();

            listener.writeTimeCourseData();
            writeReport(listener, model, writer);
        }
    }
    public Diagram getDiagram(String diagramName) throws Exception
    {
        CollectionFactory.createRepository("../data");
        return DataElementPath.create( "databases/Virtual Human/Diagrams" ).getChildPath( diagramName ).getDataElement( Diagram.class );
    }

    boolean vesselChains = true;
    public void writeReport(HemodynamicsObserver observer, ArterialBinaryTreeModel model, Writer writer) throws Exception
    {
        writer.write("Vessels\n");
        Map<String, VesselInfo> velocities = observer.getVesselInfos();
        String header = "name\tdepth\tlength" + /*"\tmidPStart\tmidPEnd\tmidTStart\tmidTend\tVmid"
                                                + */"\tminPStart\tminPEnd\tminTStart\tminTend\tVmin"
                + /*"\tmaxPStart\tmaxPEnd\tmaxTStart\tmaxTend\tVmax*/"\tPWV\n";
        writer.write(header);
        for( Entry<String, VesselInfo> entry : velocities.entrySet() )
        {
            String name = entry.getKey();
            VesselInfo vesselInfo = entry.getValue();
            
            
            String result = name + "\t" + vesselInfo.vessel.depth + "\t" + vesselInfo.vessel.length + "\t" /*+ vesselInfo.midPressureStart
                                                                                                           + "\t" + vesselInfo.midPressureEnd + "\t" + vesselInfo.midPressureStartTime + "\t" + vesselInfo.midPressureEndTime
                                                                                                           + "\t" + vesselInfo.velocityMid + "\t"*/
                    + vesselInfo.minPressureStart + "\t" + vesselInfo.minPressureEnd + "\t" + vesselInfo.minPressureStartTime + "\t"
                    + vesselInfo.minPressureEndTime + "\t" + vesselInfo.velocityMin + "\t" + vesselInfo.velocityForBranch + "\t"
                    + /*"\t"
                      + vesselInfo.maxPressureStart + "\t" + vesselInfo.maxPressureEnd + "\t" + vesselInfo.maxPressureStartTime + "\t"
                      + vesselInfo.maxPressureEndTime + "\t" + vesselInfo.velocityMax + "\t" +*/vesselInfo.vessel.pulseWave[0] + "\n";
            writer.write(result);
        }

        List<Deque<SimpleVessel>> chains = new ArrayList<>();
        if( vesselChains )
        {

            for( SimpleVessel vessel : model.vessels )
            {
                if( vessel.left != null || vessel.right != null )
                    continue;

                Deque<SimpleVessel> chain = new ArrayDeque<>();
                while( vessel != null )
                {
                    chain.push(vessel);
                    vessel = vessel.parent;
                }
                chains.add(chain);
            }

            Collections.sort(chains, new ChainComparator());

            for( Deque<SimpleVessel> chain : chains )
            {
                writer.write("\n");

                for( SimpleVessel v : chain )
                {
                    String result = v.getTitle() + "\t" + v.depth + "\t" + v.length + "\t";
                    VesselInfo info = velocities.get(v.name);
                    result += info.velocityMin + "\t" + v.pulseWave[1] + "\n";
                    writer.write(result);
                }
            }
        }
    }


//    private final double minA = 0.01;
//    private final double maxA = 6;
//    private final double minBeta = 300000;
//    private final double maxBeta = 50000000;
//    private final double stepsA = 10;
//    private final double stepsBeta = 10;

//    private double[] as = new double[]{0.01, 0.02, 0.04, 0.08, 0.1, 0.12, 0.2, 0.4, 0.8, 1, 1.2, 1.5, 2, 3, 5, 8};
//    private double[] betas = new double[]{300_000, 500_000, 800_000, 1E6, 1.2E6, 1.5E6, 2E6, 4E6, 8E6, 1E7, 2E7, 5E7, 7E7, 1E8, 5E8, 1E9, 1E10};
    
    private final double[] as = new double[]{0.01, 0.1, 1, 8};
    private final double[] betas = new double[]{300_000, 1E6, 1E7, 1E8};
    public void doTestSingleVessel(String diagramName, double timeStep, double vesselSegments, double integrationSegments,
            double heartrate, String fileName, String fileName2) throws Exception
    {
        try (Writer writer = ApplicationUtils.utfWriter(AbstractBioUMLTest.getTestFile(fileName));
                Writer writer2 = ApplicationUtils.utfWriter(AbstractBioUMLTest.getTestFile(fileName2)))
        {
            
//            double stepA = (maxA - minA) / stepsA;
//            double stepBeta = (maxBeta - minBeta) /stepsBeta;
            StringBuilder header = new StringBuilder("");
            //            for( double beta = minBeta; beta <= maxBeta; beta += stepBeta )
            for( double beta : betas )
                header.append(String.valueOf(beta) + "\t");
            writer.write(header.toString()+"\n");

//            for( double a = minA; a <= maxA; a += stepA )
            for( double a: as)
            {

                StringBuilder row1 = new StringBuilder(a+"\t");
                StringBuilder row2 = new StringBuilder(a+"\t");
//                for( double beta = minBeta; beta <= maxBeta; beta += stepBeta )
                for( double beta: betas)
                {
                    try
                    {
                        System.out.println("a = " + a + " beta = " + beta + " started");
                        Diagram d = getDiagram(diagramName);

                        EModel eModel = d.getRole(EModel.class);
                        eModel.getVariable("vesselSegments").setInitialValue(vesselSegments);
                        eModel.getVariable("integrationSegments").setInitialValue(integrationSegments);
                        eModel.getVariable("Heart_Rate").setInitialValue(heartrate);
                        HemodynamicsObserver observer = new HemodynamicsObserver();

                        observer.searchTime = 1;
                        observer.skipCycles = 0;
                        HemodynamicsSimulationEngine engine = new HemodynamicsSimulationEngine();
                        engine.setLogLevel( Level.SEVERE );
                        engine.setDiagram(d);
                        engine.setInitialTime(0);
                        engine.setTimeIncrement(timeStep);
                        engine.setCompletionTime(10);
                        ( (HemodynamicsOptions)engine.getSimulatorOptions() )
                                .setInputCondition(HemodynamicsOptions.PRESSURE_INITIAL_CONDITION_STRING);
                        ( (HemodynamicsOptions)engine.getSimulatorOptions() )
                                .setOutputCondition(HemodynamicsOptions.FILTRATION_INITIAL_CONDITION_STRING);//.PRESSURE_INITIAL_CONDITION_STRING );
                        ( (HemodynamicsOptions)engine.getSimulatorOptions() ).setUseFullPressureConservation(false);
                        ArterialBinaryTreeModel model = engine.createModel();

                        model.root.unweightedArea = a;
                        model.root.beta = beta;
                        
                        engine.simulate(model, new ResultListener[] {/*new ResultPlotPane(engine, null),*/ observer});

                        observer.complete();

                        row1.append(observer.getVesselInfos().get(model.root.name).velocityMin + "\t");
                        row2.append(model.root.pulseWave[1] + "\t");


                    }
                    catch( Exception ex )
                    {
                        row1.append("error" + "\t");
                        row2.append("error" + "\t");

                    }
                }
                writer.write(row1.toString() + "\n");
                writer2.write(row2.toString() + "\n");
            }
        }
    }
    
    public void doTestSingleVessel2(String diagramName, double[] timeSteps, double[] vesselSegments, double[] integrationSegments,
            double heartrate, double a0, double beta, String fileName) throws Exception
    {
        try (Writer writerMin = ApplicationUtils.utfWriter(AbstractBioUMLTest.getTestFile(fileName + "_MIN"));
                Writer writerMax = ApplicationUtils.utfWriter(AbstractBioUMLTest.getTestFile(fileName + "_MAX"));
                Writer writerMK = ApplicationUtils.utfWriter(AbstractBioUMLTest.getTestFile(fileName + "_MK"));
                Writer writerMin1 = ApplicationUtils.utfWriter(AbstractBioUMLTest.getTestFile(fileName + "_MIN1"));
                Writer writerMin2 = ApplicationUtils.utfWriter(AbstractBioUMLTest.getTestFile(fileName + "_MIN2"));
                
                Writer writerMax1 = ApplicationUtils.utfWriter(AbstractBioUMLTest.getTestFile(fileName + "_MAX1"));
                Writer writerMax2 = ApplicationUtils.utfWriter(AbstractBioUMLTest.getTestFile(fileName + "_MAX2"));
                )
        {

            StringBuilder header = new StringBuilder("");
            for( double vesselSegment : vesselSegments )
                header.append(String.valueOf(vesselSegment) + "\t");
            writerMin.write(header.toString()+"\n");
            writerMax.write(header.toString()+"\n");
            writerMK.write(header.toString()+"\n");
            writerMin1.write(header.toString()+"\n");
            writerMax1.write(header.toString()+"\n");
            writerMin2.write(header.toString()+"\n");
            writerMax2.write(header.toString()+"\n");
            
            for( double timeStep: timeSteps)
            {

                StringBuilder rowMin = new StringBuilder(timeStep+"\t");
                StringBuilder rowMax = new StringBuilder(timeStep+"\t");
                StringBuilder rowMK = new StringBuilder(timeStep+"\t");
                StringBuilder rowMin1 = new StringBuilder(timeStep+"\t");
                StringBuilder rowMax1 = new StringBuilder(timeStep+"\t");
                StringBuilder rowMin2 = new StringBuilder(timeStep+"\t");
                StringBuilder rowMax2 = new StringBuilder(timeStep+"\t");
                
                for( double vesselSegment: vesselSegments)
                {
                    try
                    {
                        System.out.println("timeStep = " + timeStep + " vesselSegment = " + vesselSegment + " started");
                        Diagram d = getDiagram(diagramName);

                        EModel eModel = d.getRole(EModel.class);
                        eModel.getVariable("vesselSegments").setInitialValue(vesselSegment);
                        eModel.getVariable("integrationSegments").setInitialValue(vesselSegment);
                        eModel.getVariable("Heart_Rate").setInitialValue(heartrate);
                        
                        Vessel vessel = (Vessel)d.get("Vessel").getAttributes().getValue("vessel");
                        
                        vessel.setPlotPressure(true);
                        vessel.setSegment((int)vesselSegment / 2);
                        
                        HemodynamicsObserver observer = new HemodynamicsObserver();

                        observer.searchTime = 0.3;
                        observer.skipCycles = 0;
                        HemodynamicsSimulationEngine engine = new HemodynamicsSimulationEngine();
                        engine.setLogLevel( Level.SEVERE );
                        engine.setDiagram(d);
                        engine.setInitialTime(0);
                        engine.setTimeIncrement(timeStep);
                        engine.setCompletionTime(10);
                        ( (HemodynamicsOptions)engine.getSimulatorOptions() )
                                .setInputCondition(HemodynamicsOptions.PRESSURE_INITIAL_CONDITION_STRING);
                        ( (HemodynamicsOptions)engine.getSimulatorOptions() )
                                .setOutputCondition(HemodynamicsOptions.FILTRATION_INITIAL_CONDITION_STRING);//.PRESSURE_INITIAL_CONDITION_STRING );
                        ( (HemodynamicsOptions)engine.getSimulatorOptions() ).setUseFullPressureConservation(false);
                        ArterialBinaryTreeModel model = engine.createModel();

                        model.root.unweightedArea = a0;
                        model.root.unweightedArea1 = a0;
                        model.root.beta = beta;
                        
                        engine.simulate(model, new ResultListener[] {new ResultPlotPane(engine, null), observer});

                        observer.complete();

                        double max = observer.getVesselInfos().get(model.root.name).velocityMax;
                        double min = observer.getVesselInfos().get(model.root.name).velocityMin;
                        double max1 = observer.getVesselInfos().get(model.root.name).velocityMax1;
                        double max2 = observer.getVesselInfos().get(model.root.name).velocityMax2;
                        double min1 = observer.getVesselInfos().get(model.root.name).velocityMin1;
                        double min2 = observer.getVesselInfos().get(model.root.name).velocityMin2;
                        double mk = model.root.pulseWave[1];
                        
                        System.out.println("Max: "+ max +" (" + max1+", "+max2+") MK: ["+mk+"]");
                        //System.out.println("Min: "+ min +" (" + min1+", "+min2+")");
                        rowMin.append(min + "\t");
                        rowMax.append(max + "\t");
                        rowMK.append(mk + "\t");
                        rowMin1.append(min1 + "\t");
                        rowMax1.append(max1 + "\t");
                        rowMin2.append(min2 + "\t");
                        rowMax2.append(max2 + "\t");


                    }
                    catch( Exception ex )
                    {
                        rowMin.append("error" + "\t");
                        rowMax.append("error" + "\t");
                        rowMK.append("error" + "\t");
                        rowMin1.append("error" + "\t");
                        rowMax1.append("error" + "\t");
                        rowMin2.append("error" + "\t");
                        rowMax2.append("error" + "\t");
                        ex.printStackTrace();

                    }
                }
                writerMin.write(rowMin.toString() + "\n");
                writerMax.write(rowMax.toString() + "\n");
                writerMin1.write(rowMin1.toString() + "\n");
                writerMax1.write(rowMax1.toString() + "\n");
                writerMin2.write(rowMin2.toString() + "\n");
                writerMax2.write(rowMax2.toString() + "\n");
                writerMK.write(rowMK.toString() + "\n");
            }
        }
    }
}
