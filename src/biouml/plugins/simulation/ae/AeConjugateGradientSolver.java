package biouml.plugins.simulation.ae;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.MaxIter;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleValueChecker;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.LeastSquaresConverter;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunctionGradient;
import org.apache.commons.math3.optim.nonlinear.scalar.gradient.NonLinearConjugateGradientOptimizer;
import org.apache.commons.math3.optim.nonlinear.scalar.gradient.NonLinearConjugateGradientOptimizer.Formula;

@SuppressWarnings ( "serial" )
public class AeConjugateGradientSolver extends AeApacheSolver
{

    @Override
    public double[] solve(double[] initialGuess, AeModel model) throws Exception
    {
        this.model = model;

        double[] currentFunctionValues = model.solveAlgebraic(initialGuess);
        if( normOf(currentFunctionValues) <= ftol )
            return initialGuess;

        int equationNumber = currentFunctionValues.length;
        double[] observations = new double[equationNumber];
        LeastSquaresConverter convert = new LeastSquaresConverter(new VectorFunction(), observations);
        NonLinearConjugateGradientOptimizer solver = new NonLinearConjugateGradientOptimizer(Formula.POLAK_RIBIERE, new SimpleValueChecker(
                1e-13, 1e-13));
        InitialGuess initialValue = new InitialGuess(initialGuess);
        ObjectiveFunctionGradient objectiveFunctionGradient = new ObjectiveFunctionGradient(new JacobianFunction(convert));
        PointValuePair result = solver.optimize(new ObjectiveFunction(convert), initialValue, objectiveFunctionGradient, new MaxIter(
                maxIter), new MaxEval(maxEval), GoalType.MINIMIZE);


        lastResidualNorm = result.getValue();

        for( int i = 0; i < initialGuess.length; i++ )
            initialGuess[i] = result.getPoint()[i];
        return initialGuess;
    }


    private static class JacobianFunction implements MultivariateVectorFunction
    {
        private final static double EPS = 1.0e-7;
        private final MultivariateFunction function;

        public JacobianFunction(MultivariateFunction function)
        {
            super();
            this.function = function;
        }
        @Override
        public double[] value(double[] x)
        {
            double f = function.value(x);
            double[] jacobian = new double[x.length];

            for( int j = 0; j < x.length; j++ )
            {
                double temp = x[j];
                double h = EPS * Math.abs(temp);

                if( h < EPS )
                    h = EPS;

                x[j] = temp + h;
                double fResult = function.value(x);
                x[j] = temp;

                jacobian[j] = ( fResult - f ) / h;
            }
            return jacobian;
        }
    }
}
