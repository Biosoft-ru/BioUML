package biouml.plugins.simulation._test;

import biouml.model.dynamics.Assignment;
import biouml.model.dynamics.Equation;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.SimulatorSupport;
import biouml.plugins.simulation.Span;
import biouml.plugins.simulation.UniformSpan;
import biouml.plugins.simulation.java.EventLoopSimulator;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.plugins.simulation.ode.DormandPrince;
import biouml.plugins.simulation.ode.EulerSimple;
import biouml.plugins.simulation.ode.ImexSD;
import biouml.standard.diagram.DiagramGenerator;
import biouml.standard.simulation.ResultListener;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;

public class TestSolverResult extends AbstractBioUMLTest
{

    public TestSolverResult(String name)
    {
        super(name);
    }
    private final static double ERROR = 1E-10;

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(TestSolverResult.class.getName());
        suite.addTest(new TestSolverResult("test"));
        return suite;
    }

    /**
     * Simple test checks if solver output requested amount of solution points
     * Even when model contain events which fire at span points and between them and thus making simulation process more complicated by involving EventLoopSimulator
     * @throws Exception
     */
    public void test() throws Exception
    {
        Span span = new UniformSpan(0, 10, 0.01);

        //test solver on the model without events
        System.out.println("test solver on the model without events");
        test(new EulerSimple(), span, false);
        test(new DormandPrince(), span, false);
        test(new ImexSD(), span, false);

        //test event loop simulator on the model without events
        System.out.println("test event loop simulator  on the model without events");
        test(new EventLoopSimulator(new EulerSimple()), span, false);
        test(new EventLoopSimulator(new DormandPrince()), span, false);
        test(new EventLoopSimulator(new ImexSD()), span, false);

        //test event loop simulator on the model with events
        System.out.println("test event loop simulator on the model with events");
        test(new EventLoopSimulator(new EulerSimple()), span, true);
        test(new EventLoopSimulator(new DormandPrince()), span, true);
        test(new EventLoopSimulator(new ImexSD()), span, true);

        //some special checks
        System.out.println("some special checks");
        EulerSimple solver = new EulerSimple();
        solver.getOptions().setInitialStep(0.01);
        test(new EventLoopSimulator(solver), span, true);

        solver = new EulerSimple();
        solver.getOptions().setInitialStep(0.0099);
        test(new EventLoopSimulator(solver), span, true);

        solver = new EulerSimple();
        solver.getOptions().setInitialStep(0.008);
        test(new EventLoopSimulator(solver), span, true);

        solver = new EulerSimple();
        solver.getOptions().setEventLocation(false);
        test(solver, span, true);
    }

    public void test(SimulatorSupport solver, Span span, boolean events) throws Exception
    {
        double[] expected = new double[span.getLength()];
        for( int i = 0; i < expected.length; i++ )
            expected[i] = span.getTime(i);

        DiagramGenerator generator = new DiagramGenerator("test");
        generator.createEquation("x", "1", Equation.TYPE_RATE);

        if( events )
        {
            generator.createEvent("ec1", "time > 0", new Assignment("x", "-1"));
            generator.createEvent("ec2", "time > 0.02", new Assignment("x", "-1"));
            generator.createEvent("ec3", "time > 10", new Assignment("x", "-1"));
            generator.createEvent("ec4", "time > 0.06789", new Assignment("x", "-0.1"));
        }
        SimulationEngine engine = new JavaSimulationEngine();
        engine.setDiagram(generator.getDiagram());
        Model model = engine.createModel();
        TestListener listener = new TestListener(span.getLength());
        solver.start(model, span, new ResultListener[] {listener}, null);
        assertArrayEquals("", expected, listener.result, ERROR);
    }

    private static class TestListener implements ResultListener
    {
        double[] result;
        int step = 0;
        public TestListener(int length)
        {
            result = new double[length];
            step = 0;
        }

        @Override
        public void start(Object model)
        {
        }

        @Override
        public void add(double t, double[] y) throws Exception
        {
            result[step++] = t;
        }
    }

}
