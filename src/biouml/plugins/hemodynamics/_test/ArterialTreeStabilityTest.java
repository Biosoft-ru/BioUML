package biouml.plugins.hemodynamics._test;

import java.io.BufferedWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.plugins.hemodynamics.ArterialBinaryTreeModel;
import biouml.plugins.hemodynamics.HemodynamicsModelSolver;
import biouml.plugins.hemodynamics.HemodynamicsSimulationEngine;
import biouml.standard.simulation.ResultListener;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementPath;

public class ArterialTreeStabilityTest extends TestCase implements
        ResultListener {
    private static final String REPOSITORY_PATH = "../data";

    private static final Object AORTAL = "Ascending Aorta";
    private static final Object L_RADIAL = "L. Radial";

    private static final DataElementPath DIAGRAM_PATH = ru.biosoft.access.core.DataElementPath
            .create("databases/Virtual Human/Diagrams/Arterial Brachial new");
    private static final File TEST_DIR = new File("C:/VP/");

    private static final double START_SEARCH_TIME = 1.6;

    private static final double COMPLETION_TIME = 2.6;

    public Diagram getDiagram() throws Exception {
        CollectionFactory.createRepository(REPOSITORY_PATH);
        return DIAGRAM_PATH.getDataElement(Diagram.class);
    }

    public ArterialTreeStabilityTest() {
        super("test");
    }

    public static TestSuite suite() {
        TestSuite suite = new TestSuite(
                ArterialTreeStabilityTest.class.getName());
        suite.addTest(new ArterialTreeStabilityTest());
        return suite;
    }
    
    public void test()
    {
        File result = new File(TEST_DIR+"/Stability.txt");

        simulate("0.01_10_10", 0.01, 10, 10, result);
        simulate("0.01_20_20", 0.01, 20, 20, result);
        simulate("0.01_60_60", 0.01, 50, 50, result);
        simulate("0.01_100_100", 0.01, 100, 100, result);
        simulate("0.001_10_10", 0.001, 10, 10, result);
        simulate("0.001_20_20", 0.001, 20, 20, result);
        simulate("0.001_60_60", 0.001, 50, 50, result);
        simulate("0.001_100_100", 0.001, 100, 100, result);
        simulate("0.0005_10_10", 0.0005, 10, 10, result);
        simulate("0.0005_20_20", 0.0005, 20, 20, result);
        simulate("0.0005_60_60", 0.0005, 50, 50, result);
        simulate("0.0005_100_100", 0.0005, 100, 100, result);
        simulate("0.0002_60_60", 0.0002, 50, 50, result);
        simulate("0.0002_100_100", 0.0002, 100, 100, result);
        simulate("0.0001_60_60", 0.0001, 60, 60, result);
        simulate("0.0001_100_100", 0.0001, 100, 100, result);
    }

    public void simulate(String title, double step, int integrationSteps, int sements,
            File resultFile) {
        try
        {
            HemodynamicsModelSolver solver = new HemodynamicsModelSolver();
            HemodynamicsSimulationEngine engine = new HemodynamicsSimulationEngine();
            Diagram diagram = getDiagram();

            diagram.getRole( EModel.class ).getVariable( "integrationSegments" ).setInitialValue( integrationSteps );
            diagram.getRole( EModel.class ).getVariable( "vesselSegments" ).setInitialValue( sements );

            engine.setDiagram( diagram );
            engine.setSolver( solver );
            engine.setTimeIncrement( step );
            engine.setCompletionTime( COMPLETION_TIME );
            engine.simulate( new ResultListener[] {this} );
            writeSeries( title, resultFile );
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        System.out.println(title+" done");
    }

    public void writeSeries(String title, File file) throws Exception {
        try (BufferedWriter bw = ApplicationUtils.utfAppender(file)) {
            bw.write(title +"_time\t"+StreamEx.of(times).joining("\t")+"\n");
            bw.write(title +"\t"+StreamEx.of(radialArtery).joining("\t")+"\n");
        }
    }

    @Override
    public void start(Object model) {
        // TODO Auto-generated method stub
        radialArtery = new ArrayList<>();
        times = new ArrayList<>();
        this.model = (ArterialBinaryTreeModel) model;
    }

    ArterialBinaryTreeModel model;
    List<Double> radialArtery;
    List<Double> times;

    @Override
    public void add(double t, double[] y) throws Exception {
        if (t > START_SEARCH_TIME
                && Math.abs(t / 0.01 - Math.round(t / 0.01)) < 0.0001
                && t < COMPLETION_TIME) {
            radialArtery.add(model.vesselMap.get(L_RADIAL).getPressure()[1]);
            times.add(t);
        }
    }
}
