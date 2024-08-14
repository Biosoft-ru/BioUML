package biouml.plugins.simulation.ae;

import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.MaxIter;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleValueChecker;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.LeastSquaresConverter;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.NelderMeadSimplex;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.SimplexOptimizer;

@SuppressWarnings ( "serial" )
public class AeNelderMeadSolver extends AeApacheSolver
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
        NelderMeadSimplex simplex = new NelderMeadSimplex(initialGuess.length);
        SimplexOptimizer solver = new SimplexOptimizer(new SimpleValueChecker(1e-13, 1e-13));
        InitialGuess initialValue = new InitialGuess(initialGuess);
        PointValuePair result = solver.optimize(simplex, new ObjectiveFunction(convert), initialValue, new MaxIter(maxIter), new MaxEval(
                maxEval), GoalType.MINIMIZE);


        lastResidualNorm = result.getValue();

        for( int i = 0; i < initialGuess.length; i++ )
            initialGuess[i] = result.getPoint()[i];
        return initialGuess;
    }
}
