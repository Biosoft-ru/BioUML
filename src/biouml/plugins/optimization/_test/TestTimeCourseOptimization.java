package biouml.plugins.optimization._test;

import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.analysis.optimization.OptimizationProblem;
import ru.biosoft.analysis.optimization.methods.ASAOptMethod;
import ru.biosoft.analysis.optimization.methods.GlbSolveOptMethod;
import ru.biosoft.analysis.optimization.methods.GoldfeldOptMethod;
import ru.biosoft.analysis.optimization.methods.MOCellOptMethod;
import ru.biosoft.analysis.optimization.methods.MOPSOOptMethod;
import ru.biosoft.analysis.optimization.methods.SRESOptMethod;
import junit.framework.Test;
import junit.framework.TestSuite;

public class TestTimeCourseOptimization extends AbstractBioUMLTest
{
    public TestTimeCourseOptimization(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TestTimeCourseOptimization.class);
        return suite;
    }

    public void testSres() throws Exception
    {
        SRESOptMethod method = new SRESOptMethod(null, "sresTest");
        method.getParameters().setNumOfIterations(50);

        OptimizationProblem problem = TestUtils.createTimeCourseProblem();
        method.setOptimizationProblem(problem);

        double[] solution = method.getSolution();

        assertEquals(true, TestUtils.isSolutionOk(solution));
        problem.stop();
    }

    public void testMocell() throws Exception
    {
        MOCellOptMethod method = new MOCellOptMethod(null, "mocellTest");

        OptimizationProblem problem = TestUtils.createTimeCourseProblem();
        method.setOptimizationProblem(problem);

        double[] solution = method.getSolution();

        assertEquals(true, TestUtils.isSolutionOk(solution));
        problem.stop();
    }

    public void testMopso() throws Exception
    {
        MOPSOOptMethod method = new MOPSOOptMethod(null, "mopsoTest");
        method.getParameters().setNumberOfIterations(50);

        OptimizationProblem problem = TestUtils.createTimeCourseProblem();
        method.setOptimizationProblem(problem);

        double[] solution = method.getSolution();

        assertEquals(true, TestUtils.isSolutionOk(solution));
        problem.stop();
    }

    public void testGoldfeld() throws Exception
    {
        GoldfeldOptMethod method = new GoldfeldOptMethod(null, "goldfeldTest");

        OptimizationProblem problem = TestUtils.createTimeCourseProblem(TestUtils.OptimizationType.LOCAL);
        method.setOptimizationProblem(problem);

        double[] solution = method.getSolution();

        assertEquals(true, TestUtils.isSolutionOk(solution));
        problem.stop();
    }

    public void testGblSolve() throws Exception
    {
        GlbSolveOptMethod method = new GlbSolveOptMethod(null, "glbsolveTest");
        method.getParameters().setNumOfIterations(50);
        OptimizationProblem problem = TestUtils.createTimeCourseProblem();
        method.setOptimizationProblem(problem);

        double[] solution = method.getSolution();

        assertEquals(true, TestUtils.isSolutionOk(solution));
        problem.stop();
    }

    public void testAsa() throws Exception
    {
        ASAOptMethod method = new ASAOptMethod(null, "asaTest");
        method.getParameters().setDelta(1E-8);
        method.getParameters().setRandomSeedHidden(false);
        method.getParameters().setRandomSeed(10);

        OptimizationProblem problem = TestUtils.createTimeCourseProblem();
        method.setOptimizationProblem(problem);

        double[] solution = method.getSolution();

        assertEquals(true, TestUtils.isSolutionOk(solution));
        problem.stop();
    }
}