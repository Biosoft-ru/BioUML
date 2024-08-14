package ru.biosoft.analysis.optimization._test;

import java.io.IOException;
import java.util.ArrayList;

import ru.biosoft.jobcontrol.JobControl;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.analysis.optimization.methods.ASAOptMethod;
import ru.biosoft.analysis.optimization.methods.GlbSolveOptMethod;
import ru.biosoft.analysis.optimization.methods.GoldfeldOptMethod;
import ru.biosoft.analysis.optimization.methods.MOCellOptMethod;
import ru.biosoft.analysis.optimization.methods.MOPSOOptMethod;
import ru.biosoft.analysis.optimization.OptimizationProblem;
import ru.biosoft.analysis.optimization.Parameter;
import ru.biosoft.analysis.optimization.methods.SRESOptMethod;

import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AutoTest extends TestCase
{
    public AutoTest(String name)
    {
        super(name);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(AutoTest.class.getName());

        suite.addTest(new AutoTest("testMocell_1"));
        suite.addTest(new AutoTest("testMocell_2"));
        suite.addTest(new AutoTest("testMocell_3"));
        suite.addTest(new AutoTest("testMocell_4"));
        suite.addTest(new AutoTest("testMocell_5"));

        suite.addTest(new AutoTest("testMopso_1"));
        suite.addTest(new AutoTest("testMopso_2"));
        suite.addTest(new AutoTest("testMopso_3"));
        suite.addTest(new AutoTest("testMopso_4"));
        suite.addTest(new AutoTest("testMopso_5"));

        suite.addTest(new AutoTest("testSres_1"));
        suite.addTest(new AutoTest("testSres_2"));
        suite.addTest(new AutoTest("testSres_3"));
        suite.addTest(new AutoTest("testSres_4"));
        suite.addTest(new AutoTest("testSres_5"));

        suite.addTest(new AutoTest("testGoldfeld_1"));
        suite.addTest(new AutoTest("testGoldfeld_2"));
        suite.addTest(new AutoTest("testGoldfeld_4"));
        suite.addTest(new AutoTest("testGoldfeld_5"));

        suite.addTest(new AutoTest("testGblSolve_4"));
        suite.addTest(new AutoTest("testGblSolve_5"));

        suite.addTest(new AutoTest("testAsa_1"));
        suite.addTest(new AutoTest("testAsa_2"));
        suite.addTest(new AutoTest("testAsa_3"));
        suite.addTest(new AutoTest("testAsa_4"));
        suite.addTest(new AutoTest("testAsa_5"));

        return suite;
    }

    public static void testMocell_1() throws IOException, Exception
    {
        runMocell(new Function_1(), 500);
    }
    public static void testMocell_2() throws IOException, Exception
    {
        runMocell(new Function_2(), 1000);
    }
    public static void testMocell_3() throws IOException, Exception
    {
        runMocell(new Function_3(), 500);
    }
    public static void testMocell_4() throws IOException, Exception
    {
        runMocell(new Function_4(), 500);
    }
    public static void testMocell_5() throws IOException, Exception
    {
        runMocell(new Function_5(), 1000);
    }
    private static void runMocell(Function function, int maxItrations) throws IOException, Exception
    {
        MOCellOptMethod method = new MOCellOptMethod(null, "mocellTest");
        method.getParameters().setMaxIterations(maxItrations);
        method.getParameters().setRandomSeedHidden(false);
        method.getParameters().setRandomSeed(1);
        method.setOptimizationProblem(function);
        assertEquals(true, function.isSolutionOK(method.getSolution()));
    }

    public static void testMopso_1() throws IOException, Exception
    {
        runMopso(new Function_1(), 1000);
    }
    public static void testMopso_2() throws IOException, Exception
    {
        runMopso(new Function_2(), 1000);
    }
    public static void testMopso_3() throws IOException, Exception
    {
        runMopso(new Function_3(), 1000);
    }
    public static void testMopso_4() throws IOException, Exception
    {
        runMopso(new Function_4(), 1000);
    }
    public static void testMopso_5() throws IOException, Exception
    {
        runMopso(new Function_5(), 1000);
    }
    private static void runMopso(Function function, int maxItrations) throws IOException, Exception
    {
        MOPSOOptMethod method = new MOPSOOptMethod(null, "mopsoTest_1");
        method.getParameters().setNumberOfIterations(maxItrations);
        method.getParameters().setRandomSeedHidden(false);
        method.getParameters().setRandomSeed(1);
        method.setOptimizationProblem(function);
        assertEquals(true, function.isSolutionOK(method.getSolution()));
    }

    public static void testSres_1() throws IOException, Exception
    {
        runSres(new Function_1());
    }
    public static void testSres_2() throws IOException, Exception
    {
        runSres(new Function_2());
    }
    public static void testSres_3() throws IOException, Exception
    {
        runSres(new Function_3());
    }
    public static void testSres_4() throws IOException, Exception
    {
        runSres(new Function_4());
    }
    public static void testSres_5() throws IOException, Exception
    {
        runSres(new Function_5());
    }
    private static void runSres(Function function) throws IOException, Exception
    {
        SRESOptMethod method = new SRESOptMethod(null, "sresTest");
        method.setOptimizationProblem(function);
        method.getParameters().setRandomSeedHidden(false);
        method.getParameters().setRandomSeed(1);
        assertEquals(true, function.isSolutionOK(method.getSolution()));
    }

    public static void testGoldfeld_1() throws IOException, Exception
    {
        runGoldfeld(new Function_1(), 1e-5);
    }
    public static void testGoldfeld_2() throws IOException, Exception
    {
        runGoldfeld(new Function_2(), 1e-6);
    }
    public static void testGoldfeld_4() throws IOException, Exception
    {
        runGoldfeld(new Function_4(), 1e-8);
    }
    public static void testGoldfeld_5() throws IOException, Exception
    {
        runGoldfeld(new Function_5(), 1e-4);
    }
    private static void runGoldfeld(Function function, double delta) throws IOException, Exception
    {
        GoldfeldOptMethod method = new GoldfeldOptMethod(null, "goldfeldTest");
        method.getParameters().setDeltaOutside(delta);
        method.setOptimizationProblem(function);
        assertEquals(true, function.isSolutionOK(method.getSolution()));
    }

    public static void testGblSolve_4() throws IOException, Exception
    {
        runGlbSolve(new Function_4());
    }
    public static void testGblSolve_5() throws IOException, Exception
    {
        runGlbSolve(new Function_5());
    }
    public static void runGlbSolve(Function function) throws IOException, Exception
    {
        GlbSolveOptMethod method = new GlbSolveOptMethod(null, "gblSolveTest");
        method.setOptimizationProblem(function);
        assertEquals(true, function.isSolutionOK(method.getSolution()));
    }

    public static void testAsa_1() throws IOException, Exception
    {
        runAsa(new Function_1(), 1e-10);
    }
    public static void testAsa_2() throws IOException, Exception
    {
        runAsa(new Function_2(), 1e-10);
    }
    public static void testAsa_3() throws IOException, Exception
    {
        runAsa(new Function_3(), 1e-9);
    }
    public static void testAsa_4() throws IOException, Exception
    {
        runAsa(new Function_4(), 1e-9);
    }
    public static void testAsa_5() throws IOException, Exception
    {
        runAsa(new Function_5(), 1e-9);
    }
    private static void runAsa(Function function, double delta) throws IOException, Exception
    {
        ASAOptMethod method = new ASAOptMethod(null, "asaTest");
        method.getParameters().setDelta(delta);
        method.getParameters().setRandomSeedHidden(false);
        method.getParameters().setRandomSeed(1);
        method.setOptimizationProblem(function);
        assertEquals(true, function.isSolutionOK(method.getSolution()));
    }

    public static abstract class Function implements OptimizationProblem
    {
        protected abstract double calculateDistance(double[] solution);
        protected abstract double calculatePenalty(double[] solution);

        protected abstract double[] getSolution();
        protected abstract double getSolutionDistance();

        protected ArrayList<Parameter> params = new ArrayList<>();

        @Override
        public ArrayList<Parameter> getParameters()
        {
            return params;
        }

        @Override
        public double[][] testGoodnessOfFit(double[][] values, JobControl jobControl) throws Exception
        {
            double[][] result = new double[values.length][];
            for( int i = 0; i < values.length; ++i )
                result[i] = new double[] {calculateDistance(values[i]), calculatePenalty(values[i])};
            return result;
        }

        @Override
        public double[] testGoodnessOfFit(double[] values, JobControl jobControl) throws Exception
        {
            return new double[] {calculateDistance(values), calculatePenalty(values)};
        }

        public boolean isSolutionOK(double[] calcSol) throws Exception
        {
            double epsilon = 1e-2;

            double trueDistance = getSolutionDistance();
            double[] trueSol = getSolution();

            if( trueSol != null )
            {
                // The case of the unique solution.

                double diffNorm = 0;
                double norm = 0;

                for( int i = 0; i < calcSol.length; ++i )
                {
                    diffNorm += Math.pow(calcSol[i] - trueSol[i], 2);
                    norm += Math.pow(trueSol[i], 2);
                }

                diffNorm = Math.sqrt(diffNorm);
                norm = Math.sqrt(norm);

                if( diffNorm / norm > epsilon )
                    return false;
            }

            if( Math.abs( ( calculateDistance(calcSol) - trueDistance ) / trueDistance) > epsilon || calculatePenalty(calcSol) > epsilon )
                return false;
            return true;
        }

        @Override
        public Object[] getResults(double[] values, DataCollection<?> origin) throws Exception
        {
            return null;
        }

        @Override
        public int getEvaluationsNumber()
        {
            return 0;
        }
    }

    /**
     * The problem was found in the article describing {@link SRESOptMethod}:
     *
     * Thomas P. Runarsson and Xin Yao
     * Stochastic Ranking for Constrained Evolutionary Optimization.
     * (IEEE Transactions on Evolutionary Computation (2000), vol. 4, #3, p 292)
     *
     * Note, that constraints g_2(x) and g_4(x) are automatically satisfied for the given search space.
     * Thus, we omitted these constraints.
     */
    public static class Function_1 extends Function
    {
        public Function_1()
        {
            params = new ArrayList<>();
            params.add(new Parameter("x0", 90, 78, 102));
            params.add(new Parameter("x1", 39, 33, 45));
            params.add(new Parameter("x2", 36, 27, 45));
            params.add(new Parameter("x3", 36, 27, 45));
            params.add(new Parameter("x4", 36, 27, 45));
        }

        @Override
        protected double calculateDistance(double[] values)
        {
            return ( 5.3578547 * values[2] * values[2] + 0.8356891 * values[0] * values[4] + 37.293239 * values[0] - 40792.141 );
        }

        @Override
        protected double calculatePenalty(double[] values)
        {
            double phi = Math.pow(Math.max(0, ( 85.334407 + 0.0056858 * values[1] * values[4] + 0.0006262 * values[0] * values[3]
                    - 0.0022053 * values[2] * values[4] - 92 )), 2); //g_1(x)

            phi += Math.pow(Math.max(0, ( 80.51249 + 0.0071317 * values[1] * values[4] + 0.0029955 * values[0] * values[1] + 0.0021813
                    * values[2] * values[2] - 110 )), 2); //g_3(x)

            phi += Math.pow(Math.max(0, ( 9.300961 + 0.0047026 * values[2] * values[4] + 0.0012547 * values[0] * values[2] + 0.0019085
                    * values[2] * values[3] - 25 )), 2); //g_5(x)

            phi += Math.pow(Math.max(0, ( -9.300961 - 0.0047026 * values[2] * values[4] - 0.0012547 * values[0] * values[2] - 0.0019085
                    * values[2] * values[3] + 20 )), 2); //g_6(x)
            return phi;
        }

        @Override
        protected double[] getSolution()
        {
            return new double[] {78, 33, 29.995256025682, 45, 36.775812905788};
        };

        @Override
        protected double getSolutionDistance()
        {
            return -30665.539;
        }

        @Override
        public void stop()
        {
        }
    }

    /**
     * The problem was found in the article describing {@link SRESOptMethod}:
     *
     * Thomas P. Runarsson and Xin Yao
     * Stochastic Ranking for Constrained Evolutionary Optimization.
     * (IEEE Transactions on Evolutionary Computation (2000), vol. 4, #3, p 293)
     */
    public static class Function_2 extends Function
    {
        public Function_2()
        {
            params = new ArrayList<>();
            params.add(new Parameter("x0", 56.5, 13, 100));
            params.add(new Parameter("x1", 50, 0, 100));
        }

        @Override
        protected double calculateDistance(double[] values)
        {
            return Math.pow(values[0] - 10, 3) + Math.pow(values[1] - 20, 3);
        }

        @Override
        protected double calculatePenalty(double[] values)
        {
            double phi = 0;

            phi += Math.pow(Math.max(0, -Math.pow(values[0] - 5, 2) - Math.pow(values[1] - 5, 2) + 100), 2);
            phi += Math.pow(Math.max(0, Math.pow(values[0] - 6, 2) + Math.pow(values[1] - 5, 2) - 82.81), 2);

            return phi;
        }

        @Override
        protected double[] getSolution()
        {
            return new double[] {14.095, 0.84296};
        }

        @Override
        protected double getSolutionDistance()
        {
            return -6961.81388;
        }

        @Override
        public void stop()
        {
        }
    }

    /**
     * The problem was found in the article describing {@link SRESOptMethod}:
     *
     * Thomas P. Runarsson and Xin Yao
     * Stochastic Ranking for Constrained Evolutionary Optimization.
     * (IEEE Transactions on Evolutionary Computation (2000), vol. 4, #3, p 293)
     */
    public static class Function_3 extends Function
    {
        public Function_3()
        {
            params = new ArrayList<>();
            params.add(new Parameter("x0", 5, 0, 10));
            params.add(new Parameter("x1", 5, 0, 10));
        }

        @Override
        protected double calculateDistance(double[] values)
        {
            return - ( Math.pow(Math.sin(2 * Math.PI * values[0]), 3) * Math.sin(2 * Math.PI * values[1]) )
                    / ( Math.pow(values[0], 3) * ( values[0] + values[1] ) );
        }

        @Override
        protected double calculatePenalty(double[] values)
        {
            double phi = Math.pow(Math.max(0, values[0] * values[0] - values[1] + 1), 2);
            phi += Math.pow(Math.max(0, 1 - values[0] + Math.pow(values[1] - 4, 2)), 2);
            return phi;
        }

        @Override
        protected double[] getSolution()
        {
            return new double[] {1.2279713, 4.2453733};
        }

        @Override
        protected double getSolutionDistance()
        {
            return -0.095825;
        }

        @Override
        public void stop()
        {
        }
    }

    /**
     * The problem (six-hump camel function) was found in the article describing {@link GlbSolveOptMethod}:
     *
     * Mattias Bjorkman and Kenneth Holmstrom
     * "Global Optimization Using the DIRECT Algorithm in Matlab"
     * (AMO - Advanced Modeling and Optimization, vol. 1, #2, 1999)
     */
    public static class Function_4 extends Function
    {
        public Function_4()
        {
            params = new ArrayList<>();
            params.add(new Parameter("x0", 0, -3, 3));
            params.add(new Parameter("x1", 0, -2, 2));
        }

        @Override
        protected double calculateDistance(double[] values)
        {
            return ( 4 - 2.1 * Math.pow(values[0], 2) + Math.pow(values[0], 4) / 3 ) * Math.pow(values[0], 2) + values[0] * values[1]
                    + ( -4 + 4 * Math.pow(values[1], 2) ) * Math.pow(values[1], 2);
        }

        @Override
        protected double calculatePenalty(double[] values)
        {
            return 0;
        }

        /**
         * The problem has two global minima: (-0.0898, 0.7126) and (0.0898, -0.7126)
         */
        @Override
        protected double[] getSolution()
        {
            return null;
        }

        @Override
        protected double getSolutionDistance()
        {
            return -1.0316;
        }

        @Override
        public void stop()
        {
        }
    }

    /**
     * The problem (two-dimensional Schubert) was found in the article describing {@link GlbSolveOptMethod}:
     *
     * Mattias Bjorkman and Kenneth Holmstrom
     * "Global Optimization Using the DIRECT Algorithm in Matlab"
     * (AMO - Advanced Modeling and Optimization, vol. 1, #2, 1999)
     */
    public static class Function_5 extends Function
    {
        public Function_5()
        {
            params = new ArrayList<>();
            params.add(new Parameter("x0", 5.5, -10, 10));
            params.add(new Parameter("x1", 5, -10, 10));
        }

        @Override
        protected double calculateDistance(double[] values)
        {
            double firstFactor = 0;
            double secondFactor = 0;

            for( int i = 1; i < 6; ++i )
            {
                firstFactor += i * Math.cos( ( i + 1 ) * values[0] + i);
                secondFactor += i * Math.cos( ( i + 1 ) * values[1] + i);
            }

            return firstFactor * secondFactor;
        }

        @Override
        protected double calculatePenalty(double[] values)
        {
            return 0;
        }

        /**
         * The problem has 18 global minima.
         */
        @Override
        protected double[] getSolution()
        {
            return null;
        }

        @Override
        protected double getSolutionDistance()
        {
            return -186.7309;
        }

        @Override
        public void stop()
        {
        }
    }
}