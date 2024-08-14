package biouml.plugins.optimization._test;

import junit.framework.Test;
import junit.framework.TestSuite;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.analysis.optimization.OptimizationProblem;
import ru.biosoft.analysis.optimization.methods.ASAOptMethod;
import ru.biosoft.analysis.optimization.methods.MOCellOptMethod;
import ru.biosoft.analysis.optimization.methods.MOPSOOptMethod;
import ru.biosoft.analysis.optimization.methods.SRESOptMethod;

public class TestConstraintOptimization extends AbstractBioUMLTest
{
    public TestConstraintOptimization(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestConstraintOptimization.class);
        return suite;
    }

    public void testSres() throws Exception
    {
        SRESOptMethod method = new SRESOptMethod(null, "sresTest");
        method.getParameters().setNumOfIterations(50);

        OptimizationProblem problem = TestUtils.createConstraintProblem();
        method.setOptimizationProblem(problem);

        double[] solution = method.getSolution();

        assertEquals(true, TestUtils.isSolutionOk(solution));
        problem.stop();
    }

    public void testMocell() throws Exception
    {
        MOCellOptMethod method = new MOCellOptMethod(null, "mocellTest");

        OptimizationProblem problem = TestUtils.createConstraintProblem();
        method.setOptimizationProblem(problem);

        double[] solution = method.getSolution();

        assertEquals(true, TestUtils.isSolutionOk(solution));
        problem.stop();
    }

    public void testMopso() throws Exception
    {
        MOPSOOptMethod method = new MOPSOOptMethod(null, "mopsoTest");
        method.getParameters().setNumberOfIterations(150);

        OptimizationProblem problem = TestUtils.createConstraintProblem();
        method.setOptimizationProblem(problem);

        double[] solution = method.getSolution();

        assertEquals(true, TestUtils.isSolutionOk(solution));
        problem.stop();
    }

    public void testAsa() throws Exception
    {
        ASAOptMethod method = new ASAOptMethod(null, "asaTest");
        method.getParameters().setDelta(1E-7);
        method.getParameters().setRandomSeedHidden(false);
        method.getParameters().setRandomSeed(1);

        OptimizationProblem problem = TestUtils.createConstraintProblem();
        method.setOptimizationProblem(problem);

        double[] solution = method.getSolution();

        assertEquals(true, TestUtils.isSolutionOk(solution, 0.02));
        problem.stop();
    }
}