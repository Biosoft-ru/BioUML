package biouml.plugins.hemodynamics._test;


import java.io.BufferedWriter;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.plugins.hemodynamics.ArterialBinaryTreeModel;
import biouml.plugins.hemodynamics.HemodynamicsModelSolver;
import biouml.plugins.hemodynamics.HemodynamicsOptions;
import biouml.plugins.hemodynamics.HemodynamicsSimulationEngine;
import biouml.plugins.hemodynamics.SimpleVessel;
import biouml.standard.simulation.ResultListener;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.Stat;

public class VelocityTest extends TestCase implements ResultListener
{
    private final DataElementPath databasePath = DataElementPath.create("databases/Virtual Human/Diagrams");
    private static final String REPOSITORY_PATH = "../data";
    public VelocityTest(String name)
    {
        super(name);
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(VelocityTest.class.getName());
        suite.addTest(new VelocityTest("test"));
        return suite;
    }

    public void test() throws Exception
    {
        simulate(getDiagram("Arterial Brachial 2"), HemodynamicsModelSolver.BloodLoss.NO, 5, 5, "Tree_slow_5");
//        simulate(getDiagram("Arterial Tree WP"), HemodynamicsModelSolver.BloodLoss.NO, 5, 5, "WP_slow_5");
//        simulate(getDiagram("Arterial Tree WP"), HemodynamicsModelSolver.BloodLoss.SIMPLE, 5, 5, "WP_slow_5");
//
//        simulate(getDiagram("Arterial Tree Equal RH"), HemodynamicsModelSolver.BloodLoss.NO, 5, 5, "EqualRH_slow_5");
//        simulate(getDiagram("Arterial Tree Equal RH"), HemodynamicsModelSolver.BloodLoss.SIMPLE, 5, 5, "EqualRH_simple_5");

        //        simulate(getDiagram(), HemodynamicsModelSolver.BloodLoss.NO, 15, 15, "area_no_no_15.txt", "pressure_no_no_15.txt",
        //                "velocity_no_no_15.txt", "pwv_no_no_15.txt");
        //
        //        simulate(getDiagram(), HemodynamicsModelSolver.BloodLoss.NO, 20, 20, "area_no_no_20.txt", "pressure_no_no_20.txt",
        //                "velocity_no_no_20.txt", "pwv_no_no_20.txt");
        //
        //        simulate(getDiagram(), HemodynamicsModelSolver.BloodLoss.NO, 25, 25, "area_no_no_25.txt", "pressure_no_no_25.txt",
        //                "velocity_no_no_25.txt", "pwv_no_no_25.txt");
        //
        //        simulate(getDiagram(), HemodynamicsModelSolver.BloodLoss.NO, 40, 20, "area_no_no_40_20.txt", "pressure_no_no_40_20.txt",
        //                "velocity_no_no_40_20.txt", "pwv_no_no_40_20.txt");
        //
        //        simulate(getDiagram(), HemodynamicsModelSolver.BloodLoss.NO, 20, 40, "area_no_no_20_40.txt", "pressure_no_no_20_40.txt",
        //                "velocity_no_no_20_40.txt", "pwv_no_no_20_40.txt");
    }

    boolean vesselChains = true;
    boolean timeCourse = true;

    Map<String, Double> distances = new HashMap<>();
    public void simulate(Diagram diagram, HemodynamicsModelSolver.BloodLoss loss, double segments, double integration, String suffix)
            throws Exception
    {

        try (BufferedWriter bwArea = ApplicationUtils.utfWriter(AbstractBioUMLTest.getTestFile("area" + suffix + ".txt"));
                BufferedWriter bwPressure = ApplicationUtils.utfWriter(AbstractBioUMLTest.getTestFile("pressure" + suffix + ".txt"));
                BufferedWriter bwVelocity = ApplicationUtils.utfWriter(AbstractBioUMLTest.getTestFile("velocity" + suffix + ".txt"));
                BufferedWriter bwPWV = ApplicationUtils.utfWriter(AbstractBioUMLTest.getTestFile("pwv" + suffix + ".txt"));)
        {
            vesselToParent = new HashMap<>();
            looseVessels = new HashSet<>();
            HemodynamicsSimulationEngine engine = new HemodynamicsSimulationEngine();

            EModel emodel = diagram.getRole(EModel.class);
            emodel.getVariable("vesselSegments").setInitialValue(segments);
            emodel.getVariable("integrationSegments").setInitialValue(integration);
            emodel.getVariable("referencedPressure").setInitialValue(100);
            emodel.getVariable("Heart_Rate").setInitialValue(10);
            
            engine.setDiagram(diagram);
            engine.setInitialTime(0);
            engine.setTimeIncrement(0.01);
            engine.setCompletionTime(24);
            ( (HemodynamicsOptions)engine.getSimulatorOptions() ).setInputCondition(HemodynamicsOptions.FLUX_INITIAL_CONDITION_STRING);
            ( (HemodynamicsOptions)engine.getSimulatorOptions() )
                    .setOutputCondition(HemodynamicsOptions.FILTRATION_INITIAL_CONDITION_STRING);//.PRESSURE_INITIAL_CONDITION_STRING );
            ( (HemodynamicsOptions)engine.getSimulatorOptions() ).setUseFullPressureConservation(true);
//                        ( (HemodynamicsOptions)engine.getSimulatorOptions() ).setModelArteriols(true);

            HemodynamicsModelSolver solver = (HemodynamicsModelSolver)engine.getSolver();
            solver.bloodLossType = loss;
            ArterialBinaryTreeModel model = engine.createModel();


            if( timeCourse )
            {
                engine.setResultAllVessels(true);
                engine.simulate(model, new ResultListener[] {this});
            }
            else
            {
                engine.simulate(model);
            }

            fillParentMap(model.root);
            calcDistance(0.0, distances, model.root);

            if( timeCourse )
            {
                printTimeCourseHeader(bwArea);
                printTimeCourseHeader(bwPressure);
                printTimeCourseHeader(bwVelocity);

                printTimeCourseInfo(null, model.root, vesselArea, bwArea);
                printTimeCourseInfo(null, model.root, vesselPressure, bwPressure);
                printTimeCourseInfo(null, model.root, vesselVelocity, bwVelocity);
                printTimeCourseInfo(null, model.root, vesselPWV, bwPWV);

                bwArea.write("\n");
                bwPressure.write("\n");
                bwVelocity.write("\n");
                bwPWV.write("\n");

                List<Deque<SimpleVessel>> chains = new ArrayList<>();
                if( vesselChains )
                {

                    for( SimpleVessel vessel : looseVessels )
                    {
                        Deque<SimpleVessel> chain = new ArrayDeque<>();

                        chain.push(vessel);
                        while( vesselToParent.containsKey(vessel) )
                        {
                            vessel = vesselToParent.get(vessel);
                            chain.push(vessel);
                        }
                        chains.add(chain);
                    }

                    Collections.sort(chains, new ChainComparator());

                    for( Deque<SimpleVessel> chain : chains )
                    {
                        bwArea.write("\n");
                        bwPressure.write("\n");
                        bwVelocity.write("\n");
                        bwPWV.write("\n");

                        printTimeCourseMeanHeader(bwArea);
                        printTimeCourseMeanHeader(bwPressure);
                        printTimeCourseMeanHeader(bwVelocity);

                        for( SimpleVessel v : chain )
                        {
                            printTimeCourseMeanInfo(vesselToParent.containsKey(v) ? vesselToParent.get(v) : null, v, vesselArea, bwArea);
                            printTimeCourseMeanInfo(vesselToParent.containsKey(v) ? vesselToParent.get(v) : null, v, vesselPressure,
                                    bwPressure);
                            printTimeCourseMeanInfo(vesselToParent.containsKey(v) ? vesselToParent.get(v) : null, v, vesselVelocity,
                                    bwVelocity);
                            printTimeCourseMeanInfo(vesselToParent.containsKey(v) ? vesselToParent.get(v) : null, v, vesselPWV, bwPWV);
                        }
                    }
                }
            }
            else
            {
                printHeader(bwVelocity);
                printInfo(model.root, model.root.left, bwVelocity);
                printInfo(model.root, model.root.right, bwVelocity);
            }
        }
    }

    public static class ChainComparator implements Comparator<Collection<?>>
    {

        @Override
        public int compare(Collection<?> o1, Collection<?> o2)
        {
            return Integer.compare(o1.size(), o2.size());
        }

    }

    private Set<SimpleVessel> looseVessels;
    private Map<SimpleVessel, SimpleVessel> vesselToParent;

    private void calcDistance(Double currentDistance, Map<String, Double> distances, SimpleVessel currentVessel)
    {
        distances.put(currentVessel.getTitle(), currentDistance);
        if( currentVessel.left != null )
            calcDistance(currentDistance + currentVessel.length, distances, currentVessel.left);

        if( currentVessel.right != null )
            calcDistance(currentDistance + currentVessel.length, distances, currentVessel.right);
    }

    private void printTimeCourseHeader(Writer bw) throws Exception
    {

        bw.write("Title\t" + "Depth\t" + "Distance\t" + "Parent\t" + "isLoose\n");
    }

    private void printTimeCourseMeanHeader(Writer bw) throws Exception
    {

        bw.write("Title\tDepth\tDistance\tParent\tisLoose\tmin\tmax\taverage\tmean\n");
    }

    private void printTimeCourseMeanInfo(SimpleVessel parent, SimpleVessel child, Map<String, double[]> values, Writer bw) throws Exception
    {
        String loose = ( child.left == null && child.right == null ) ? "loose" : "no";
        String parentName = parent != null ? parent.getTitle() : "NONE";
        String result = child.getTitle() + "\t" + child.depth + "\t" + distances.get(child.title) + "\t" + parentName + "\t" + loose + "\t";

        double[] vals = values.get(child.name);
        Arrays.sort(vals);

        result += vals[0] + "\t" + vals[vals.length - 1] + "\t" + ( vals[0] + vals[vals.length - 1] ) / 2 + "\t" + Stat.mean(vals);
        result = result.replaceAll("\\.", ",");
        bw.write(result + "\n");

    }

    private void printTimeCourseInfo(SimpleVessel parent, SimpleVessel child, Map<String, double[]> values, Writer bw) throws Exception
    {
        String loose = ( child.left == null && child.right == null ) ? "loose" : "no";
        String parentName = parent != null ? parent.getTitle() : "NONE";
        StringBuilder resultBuilder = new StringBuilder( child.getTitle() );
        resultBuilder.append( "\t" ).append( child.depth );
        resultBuilder.append( "\t" ).append( distances.get( child.title ) );
        resultBuilder.append( "\t" ).append( parentName );
        resultBuilder.append( "\t" ).append( loose );

        double[] vals = values.get(child.name);
        for( double val : vals )
            resultBuilder.append( "\t" ).append( val );

        String result = resultBuilder.toString().replaceAll( "\\.", "," );
        bw.write(result + "\n");

        if( child.left != null )
            printTimeCourseInfo(child, child.left, values, bw);
        if( child.right != null )
            printTimeCourseInfo(child, child.right, values, bw);
    }

    private void printHeader(Writer bw) throws Exception
    {
        bw.write("Title\t" + "Depth\t" + "Distance\t" + "V\t" + "V fraction\t" + "V diff\t" + "A\t" + "A fraction\t" + "A diff\t"
                + "A summ\t" + "A summ fraction\t" + "A summ diff\t" + "P\t" + "Parent\t" + "isLoose\n");
    }

    private void printInfo(SimpleVessel parent, SimpleVessel child, Writer bw) throws Exception
    {
        double childrenArea = 0;
        if( parent.left != null )
            childrenArea += parent.left.area[0];
        if( parent.right != null )
            childrenArea += parent.right.area[0];

        double observedValue = getObservedParameter(child);
        double observedParentValue = getObservedParameter(parent);
        String loose = ( child.left == null && child.right == null ) ? "loose" : "no";
        bw.write(child.getTitle() + "\t" + child.depth + "\t" + distances.get(child.title) + "\t" + observedValue + "\t"
                + ( observedValue / observedParentValue ) + "\t" + ( observedValue - observedParentValue ) + "\t" + child.area[0] + "\t"
                + ( child.area[0] / parent.area[0] ) + "\t" + ( child.area[0] - parent.area[0] ) + "\t" + childrenArea + "\t"
                + ( childrenArea / parent.area[0] ) + "\t" + ( childrenArea - parent.area[0] ) + "\t" + ( childrenArea - child.pressure[0] )
                + "\t" + parent.getTitle() + "\t" + loose + "\n");


        if( child.left != null )
            printInfo(child, child.left, bw);
        if( child.right != null )
            printInfo(child, child.right, bw);
    }

    public void fillParentMap(SimpleVessel vessel)
    {
        if( vessel.left != null )
        {
            vesselToParent.put(vessel.left, vessel);
            fillParentMap(vessel.left);
        }
        if( vessel.right != null )
        {
            vesselToParent.put(vessel.right, vessel);
            fillParentMap(vessel.right);
        }

        if( vessel.left == null && vessel.right == null )
            this.looseVessels.add(vessel);
    }


    public Diagram getDiagram(String diagramName) throws Exception
    {
        CollectionFactory.createRepository(REPOSITORY_PATH);
        return databasePath.getChildPath( diagramName ).getDataElement( Diagram.class );
    }

    @Override
    public void start(Object model)
    {
        // TODO Auto-generated method stub
        vesselArea = new HashMap<>();
        vesselVelocity = new HashMap<>();
        vesselPressure = new HashMap<>();
        vesselPWV = new HashMap<>();
        this.model = (ArterialBinaryTreeModel)model;
        curIndex = 0;
    }

    int curIndex = 0;
    Map<String, double[]> vesselArea;
    Map<String, double[]> vesselVelocity;
    Map<String, double[]> vesselPressure;
    Map<String, double[]> vesselPWV;
    ArterialBinaryTreeModel model;
    @Override
    public void add(double t, double[] y) throws Exception
    {
        if( t > 12 && Math.abs(t / 0.01 - Math.round(t / 0.01)) < 0.0001 && curIndex < 600 )
        {
            for( String vesselName : model.vesselMap.keySet() )
            {
                SimpleVessel vessel = model.vesselMap.get(vesselName);
                if( !vesselArea.containsKey(vesselName) )
                    vesselArea.put(vesselName, new double[600]);
                vesselArea.get(vesselName)[curIndex] = vessel.area[1];

                if( !vesselVelocity.containsKey(vesselName) )
                    vesselVelocity.put(vesselName, new double[600]);
                vesselVelocity.get(vesselName)[curIndex] = vessel.velocity[1];

                if( !vesselPressure.containsKey(vesselName) )
                    vesselPressure.put(vesselName, new double[600]);
                vesselPressure.get(vesselName)[curIndex] = vessel.pressure[1];

                if( !vesselPWV.containsKey(vesselName) )
                    vesselPWV.put(vesselName, new double[600]);
                vesselPWV.get(vesselName)[curIndex] = vessel.pulseWave[1];
            }
            curIndex++;
        }
    }

    private double getObservedParameter(SimpleVessel vessel)
    {
        //        return vessel.velocity[1];
        //        return vessel.pressure[1];
        return vessel.area[1];
    }

}
