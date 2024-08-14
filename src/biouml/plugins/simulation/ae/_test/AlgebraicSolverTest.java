package biouml.plugins.simulation.ae._test;

import junit.framework.TestCase;
import biouml.plugins.simulation.ae.AeModel;
import biouml.plugins.simulation.ae.NewtonSolver;

public class AlgebraicSolverTest extends TestCase
{
    double error = 1e-6;

    public void testAlgebraic() throws Exception
    {
        TestSystem1 aeModel1 = new TestSystem1();
        double[] result = aeModel1.initialGuess;
        NewtonSolver.solve(result, aeModel1);
        assertEquals( aeModel1.answer[0], result[0], error );
        assertEquals( aeModel1.answer[1], result[1], error );

        TestSystem2 aeModel2 = new TestSystem2();
        result = aeModel2.initialGuess;
        NewtonSolver.solve(result, aeModel2);
        assertEquals( aeModel2.answer[0], result[0], error );
        assertEquals( aeModel2.answer[1], result[1], error );

        //        TestSystem3 aeModel3 = new TestSystem3();
        //        result = aeModel3.initialGuess;
        //        NewtonSolver.solve( result, aeModel3 );
        //        assertEquals( aeModel3.answer[0], result[0], error );
        //        assertEquals( aeModel3.answer[1], result[1], error );
        //        assertEquals( aeModel3.answer[2], result[2], error );

        TestSystem4 aeModel4 = new TestSystem4();
        result = aeModel4.initialGuess;
        NewtonSolver.solve(result, aeModel4);
        assertEquals( aeModel4.answer[0], result[0], error );
        assertEquals( aeModel4.answer[1], result[1], error );
        assertEquals( aeModel4.answer[2], result[2], error );
    }

    public static class TestSystem1 implements AeModel
    {

        public double[] initialGuess = {1, 3};
        public double[] answer = {0, 4};

        @Override
        public double[] solveAlgebraic(double[] z)
        {
            double[] result = new double[initialGuess.length];
            result[0] = z[0];
            result[1] = z[0] + z[1] - 4;
            return result;
        }

        @Override
        public double[] getConstraints()
        {
            // TODO Auto-generated method stub
            return null;
        }

    }

    public static class TestSystem2 implements AeModel
    {

        public double[] initialGuess = {1, 3};
        public double[] answer = {2.666666666666666, 1.333333333333333};

        @Override
        public double[] solveAlgebraic(double[] z)
        {
            double[] result = new double[initialGuess.length];
            result[0] = z[0] - 2 * z[1];
            result[1] = z[0] + z[1] - 4;
            return result;
        }

        @Override
        public double[] getConstraints()
        {
            // TODO Auto-generated method stub
            return null;
        }

    }

    public static class TestSystem3 implements AeModel
    {

        public double[] initialGuess = {1, 1, 1};
        public double[] answer = {1, Math.E, Math.PI};

        @Override
        public double[] solveAlgebraic(double[] z)
        {
            double[] result = new double[initialGuess.length];
            result[0] = z[0] + Math.log(z[1] * z[1]) - Math.cos(4 * z[2]) - 2;
            result[1] = 3 * Math.sin(z[0] * z[2]) + 3 * Math.pow(z[0], 3) - 2 * Math.log(z[1]) - z[0];
            result[2] = Math.log(Math.pow(z[1], 5 * z[0])) - Math.sin(z[2] / 2) - 4;
            return result;
        }

        @Override
        public double[] getConstraints()
        {
            // TODO Auto-generated method stub
            return null;
        }

    }

    public static class TestSystem4 implements AeModel
    {

        public double[] initialGuess = {1, 1, 1};
        public double[] answer = {1, 2, 3};

        @Override
        public double[] solveAlgebraic(double[] z)
        {
            double[] result = new double[initialGuess.length];
            result[0] = z[0] * z[0] + z[1] - z[2];
            result[1] = z[0] * z[1] * z[2] - 6;
            result[2] = 3 * z[0] - 2 * z[0] * z[1] + z[2] * z[2] - 8;
            return result;
        }

        @Override
        public double[] getConstraints()
        {
            // TODO Auto-generated method stub
            return null;
        }

    }
}
