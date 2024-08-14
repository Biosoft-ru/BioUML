package biouml.plugins.simulation.ae;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.math3.analysis.MultivariateMatrixFunction;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer.Optimum;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.linear.DiagonalMatrix;

import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Variable;
import biouml.plugins.simulation.java.JavaSimulationEngine;

@SuppressWarnings ( "serial" )
public class AeLevenbergMarquardSolver extends AeApacheSolver
{
    private Map<Integer, Set<Integer>> influenceMap = null;

    @Override
    public double[] solve(double[] initialGuess, AeModel model) throws Exception
    {
        this.model = model;

        double[] currentFunctionValues = model.solveAlgebraic(initialGuess);
        if( normOf(currentFunctionValues) <= ftol )
            return initialGuess;

        LevenbergMarquardtOptimizer solver = new LevenbergMarquardtOptimizer();
        int equationNumber = currentFunctionValues.length;
        double[] observations = new double[equationNumber];

        LeastSquaresBuilder builder = new LeastSquaresBuilder();
        builder.model(new VectorFunction(), new JacobianFunction());
        builder.maxIterations(maxIter);
        builder.maxEvaluations(maxEval);
        builder.start(initialGuess);
        builder.target(observations);
        double[] weight = new double[equationNumber];
        for( int i = 0; i < weight.length; i++ )
            weight[i] = 1;
        builder.weight(new DiagonalMatrix(weight));
        Optimum result = solver.optimize(builder.build());

        lastResidualNorm = normOf(result.getResiduals().toArray());

        for( int i = 0; i < initialGuess.length; i++ )
            initialGuess[i] = result.getPoint().toArray()[i];
        return initialGuess;
    }

    public void createInfluenceMap(JavaSimulationEngine engine)
    {
        influenceMap = new HashMap<>();
        try
        {
            Map<Integer, Set<String>> equationToVarIndexMap = createEquationInfluenceMap(engine);
            Map<String, Integer> varIndexMap = createVarIndexMap(engine);
            for( Entry<Integer, Set<String>> entry : equationToVarIndexMap.entrySet() )
            {
                for( String varName : entry.getValue() )
                {
                    influenceMap.computeIfAbsent( varIndexMap.get(varName), k -> new HashSet<>() ).add( entry.getKey() );
                }
            }
        }
        catch( Throwable t )
        {
            influenceMap = null;
        }
    }

    public static Map<String, Integer> createVarIndexMap(JavaSimulationEngine engine)
    {
        Map<String, Integer> indexMap = new HashMap<>();
        int i = 0;
        for( Variable var : engine.getExecutableModel().getVariables() )
        {
            if( engine.isAlgebraic(var.getName()) )
            {
                indexMap.put(var.getName(), i);
                ++i;
            }
        }
        return indexMap;
    }

    public static Map<Integer, Set<String>> createEquationInfluenceMap(JavaSimulationEngine engine)
    {
        Map<Integer, Set<String>> equationToVarIndexMap = new HashMap<>();
        int i = 0;
        Map<String, Integer> varIndexMapping = engine.getVarIndexMapping();
        for( Equation eq : engine.getExecutableModel().getAlgebraic() )
        {
            String delimiters = " ()+-/%*^";
            StringTokenizer tokens = new StringTokenizer(eq.getFormula().trim(), delimiters, false);
            while( tokens.hasMoreTokens() )
            {
                String token = tokens.nextToken();
                if( varIndexMapping.get(token) != null )
                {
                    equationToVarIndexMap.computeIfAbsent( i, k -> new HashSet<>() ).add( token );
                }
            }
            i++;
        }
        return equationToVarIndexMap;
    }

    private class JacobianFunction implements MultivariateMatrixFunction
    {
        private final static double EPS = 1.0e-4;

        @Override
        public double[][] value(double[] x)
        {
            VectorFunction function = new VectorFunction();
            double[] f = function.value(x);
            double[][] jacobian = new double[f.length][x.length];

            for( int j = 0; j < x.length; j++ )
            {
                double temp = x[j];
                double h = EPS * Math.abs(temp);

                if( h < EPS )
                    h = EPS;

                x[j] = temp + h;
                double[] fResult = function.value(x);
                x[j] = temp;

                if( influenceMap != null && influenceMap.get(j) != null )
                {
                    for( Integer index : influenceMap.get(j) )
                        jacobian[index][j] = ( fResult[index] - f[index] ) / h;
                }
                else
                {
                    for( int i = 0; i < f.length; i++ )
                        jacobian[i][j] = ( fResult[i] - f[i] ) / h;
                }
            }
            return jacobian;
        }
    }

}
