package biouml.plugins.modelreduction._test;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Map;

import biouml.model.Diagram;
import biouml.model.Module;
import biouml.model.dynamics.EModel;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.OdeSimulatorOptions;
import biouml.plugins.simulation.SimulatorSupport;
import biouml.plugins.simulation.Span;
import biouml.plugins.simulation.UniformSpan;
import biouml.plugins.simulation.java.EventLoopSimulator;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.standard.simulation.ResultListener;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;

public class SteadyStateTest extends TestCase implements ResultListener
{

    public SteadyStateTest(String name)
    {
        super(name);
    }

    public int validationSize = 10;

    private static final double accuracy = 1E-5;

    private static final double minimumTimeIncrement = 1E-5;
    private static final double maximumTimeIncrement = 100;
    private static final String inputName = "Karaaslan";
    private static final String resultName = inputName + "_Steady";

    static final String repositoryPath = "../data";
    static final String compositeModelsName = "databases/agentmodel_test";
    static final String out = "../out_test";

    private boolean running = true;

    public double initialTime = 0;
    private final double completionTime = 1E9;
    private double timeIncrement = maximumTimeIncrement;
    private final SimulatorSupport simulator = new EventLoopSimulator();

    private int round = 1;

    /** Make suite if tests. */
    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(SteadyStateTest.class.getName());
        suite.addTest(new SteadyStateTest("test"));
        return suite;
    }

    private double[] initialValues;
    

    private ArrayDeque<double[]> variableValues;
    private Map<String, Integer> variableIndices;
    private int timeIndex;
   
    public double time;

    public void test() throws Exception
    {
        //longSimulation();
//        solveAlgebraic();
    }

    private void longSimulation() throws Exception
    {
        CollectionFactory.createRepository(repositoryPath);
        Diagram input = getDiagram(inputName);
        DataCollection origin = input.getOrigin();
        Diagram result = input.clone(origin, resultName);

        Model model = init(result);
        initialValues = model.getInitialValues();

        ( (OdeSimulatorOptions)simulator.getOptions() ).setRtol(1E-7);
        while( timeIncrement > minimumTimeIncrement && running )
        {
            startSimulation(model);
            timeIncrement /= 10; //update timeIncrement
            initialTime = simulator.getProfile().getTime();
            initialValues = simulator.getProfile().getX();
            System.out.println("Round " + round++ + " finished");
        }

        setInitialValues(result);
        origin.put(result);
    }

//    private void solveAlgebraic() throws Exception
//    {
//        CollectionFactory.createRepository(repositoryPath);
//        Diagram diagram = (Diagram)getDiagram(inputName);
//        AeSimulationEngine engine = new AeSimulationEngine();
//        engine.setDiagram(diagram);
//        engine.setOutputDir("../out");
//        engine.generateModel();
//    }
    
    private Model init(Diagram diagram) throws Exception
    {
        JavaSimulationEngine engine = new JavaSimulationEngine();
        engine.setDiagram(diagram);
        engine.setOutputDir("../out");
        Model model = engine.createModel();
        variableIndices = engine.getVarIndexMapping();
        timeIndex = variableIndices.get("time");
        this.start(model);
        model.init();
        return model;
    }

    private void startSimulation(Model model) throws Exception
    {
        Span span = new UniformSpan(initialTime, completionTime, timeIncrement);
        simulator.start(model, initialValues, span, new ResultListener[] {this}, null);
    }

    /**
     * Set new initial values to all parameters of <b>diagram</b>
     * @param diagram
    //     */
    private void setInitialValues(Diagram diagram)
    {
        EModel emodel = diagram.getRole( EModel.class );
        for( Map.Entry<String, Integer> entry : variableIndices.entrySet() )
        {
            int variableIndex = entry.getValue();
            double variableValue = variableValues.getFirst()[variableIndex];
            emodel.getVariable(entry.getKey()).setInitialValue(variableValue);
        }
    }

    /**
     * Get diagram for testing
     */
    protected Diagram getDiagram(String name) throws Exception
    {
        CollectionFactory.createRepository(repositoryPath);
        DataCollection<?> db = CollectionFactory.getDataCollection(compositeModelsName);
        assertNotNull("Can not find collection " + compositeModelsName, db);
        assertTrue("Terget collection is not database: " + db.getCompletePath(), ( db instanceof Module ));
        DataCollection<Diagram> diagrams = ( (Module)db ).getDiagrams();
        assertNotNull("Can not find Diagrams in database " + db.getCompletePath(), diagrams);
        Diagram diagram = diagrams.get(name);
        assertNotNull("Can not find diagram: " + name, diagram);
        return diagram;
    }

    @Override
    public void add(double t, double[] y) throws Exception
    {
        if( variableValues.size() == validationSize )
            variableValues.pollFirst();
        variableValues.add(y);
        time = y[timeIndex];
        if( checkNaN(y) )
        {
            running = false;
            stopSimulation();
        }

        if( checkState() )
            stopSimulation();
    }

    private void stopSimulation()
    {
        if( simulator != null )
            simulator.stop();
    }


    private boolean checkNaN(double[] y)
    {
        boolean result = false;
        for( int i = 0; i < y.length; i++ )
        {
            if( Double.isNaN(y[i]) )
            {
                System.out.println("Value of variable " + findVariableName(i) + " turned NaN on time " + time);
                result = true;
            }
        }
        return result;
    }

    private String findVariableName(int index)
    {
        return StreamEx.ofKeys(variableIndices, val -> val == index).findAny().orElse("NOT_FOUND_VARIABLE");
    }

    /**
     * Check if we already reached steady state
     * @return
     */
    private boolean checkState()
    {
        if( variableValues.size() < validationSize )
            return false;
        Iterator<double[]> iter = variableValues.iterator();
        double[] first = iter.next();
        while( iter.hasNext() )
        {
            if( !check(first, iter.next(), timeIndex, accuracy) )
                return false;
        }
        return true;
    }

    /**
     * check equality of <b>a</b> and <b>b</b> arrays, but do not take into account index <b>exclude</b>
     */
    private boolean check(double[] a, double[] b, int exclude, double accuracy)
    {
        for( int i = 0; i < a.length; i++ )
        {
            if( Math.abs(a[i] - b[i]) > accuracy && i != exclude )//!= )
                return false;
        }
        return true;
    }


    @Override
    public void start(Object model)
    {
        variableValues = new ArrayDeque<>();
    }
}
