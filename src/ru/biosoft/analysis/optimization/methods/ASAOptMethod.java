package ru.biosoft.analysis.optimization.methods;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.analysis.optimization.OptimizationMethod;
import ru.biosoft.analysis.optimization.OptimizationMethodParametersBeanInfo;
import ru.biosoft.analysis.optimization.OptimizationProblem;
import ru.biosoft.analysis.optimization.OptimizationMethodParameters;
import ru.biosoft.analysis.Util;

import java.io.IOException;
import java.util.Properties;
import java.util.Random;

import org.apache.commons.lang.ArrayUtils;

import com.developmentontheedge.beans.BeanInfoConstants;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import one.util.streamex.StreamEx;

/*
 * This algorithm was created based on the article
 * Lester Ingber
 * Adaptive simulated annealing (ASA): Lessons learned.
 * (Control and Cybernetics , Vol. 25 No. 1,pp. 33-54, 1996)
 */

public class ASAOptMethod extends OptimizationMethod<ASAOptMethod.ASAOptMethodParameters>
{
    /**
     * An accuracy for the gradients calculation.
     */
    private static final double ACCURACY = 0.00001;
    private static final int RESCALING_NUMBER = 60;

    public ASAOptMethod(DataCollection<?> origin, String name)
    {
        super(origin, name, null);
        parameters = new ASAOptMethodParameters();
    }

    @PropertyName ( "Method parameters" )
    @PropertyDescription ( "Method parameters." )
    public class ASAOptMethodParameters extends OptimizationMethodParameters
    {
        private double delta = 1e-9;
        private int maxEvaluationsNumber = 1000;

        public ASAOptMethodParameters()
        {
            delta = 1e-9;
            maxEvaluationsNumber = 1000;
            random = new Random();
        }

        @PropertyName ( "Calculation accuracy" )
        @PropertyDescription ( "The admissible absolute error between previous and current solutions calculated using two-norm." )
        public double getDelta()
        {
            return delta;
        }

        public void setDelta(double pDelta)
        {
            double oldValue = delta;
            delta = pDelta;
            firePropertyChange("delta", oldValue, pDelta);
        }

        @PropertyName ( "Maximum simulations number" )
        @PropertyDescription ( "The maximum number of simulations used to stop the optimization process if the required accuracy is unattainable." )
        public int getMaxEvaluationsNumber()
        {
            return maxEvaluationsNumber;
        }

        public void setMaxEvaluationsNumber(int maxEvaluationsNumber)
        {
            double oldValue = maxEvaluationsNumber;
            this.maxEvaluationsNumber = maxEvaluationsNumber;
            firePropertyChange("maxEvaluationsNumber", oldValue, maxEvaluationsNumber);
        }

        @Override
        public void read(Properties properties, String prefix)
        {
            super.read(properties, prefix);
            try
            {
                delta = Double.parseDouble(properties.getProperty(prefix + "delta"));
                maxEvaluationsNumber = Integer.parseInt(properties.getProperty(prefix + "maxEvaluationsNumber"));
            }
            catch( Exception e )
            {
            }
        }

        @Override
        public void write(Properties properties, String prefix)
        {
            super.write(properties, prefix);
            properties.put(prefix + "delta", Double.toString(delta));
            properties.put(prefix + "maxEvaluationsNumber", Integer.toString(maxEvaluationsNumber));
        }
    }

    public static class ASAOptMethodParametersBeanInfo extends OptimizationMethodParametersBeanInfo
    {
        public ASAOptMethodParametersBeanInfo()
        {
            super(ASAOptMethodParameters.class);
        }

        @Override
        public void initProperties() throws Exception
        {
            super.initProperties();
            property("delta").numberFormat(BeanInfoConstants.NUMBER_FORMAT_NONE).add();
            property("maxEvaluationsNumber").add();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Optimization problem
    //

    private double constant;
    private double[][] population;

    @Override
    public void setOptimizationProblem(OptimizationProblem problem)
    {
        super.setOptimizationProblem(problem);
        if( problem == null )
            return;
        individualsNumber = n;
        distances = new double[n];
        penalties = new double[n];
        solution = new double[n];
        population = new double[n][n];
        constant = 0.5 * Math.exp( -0.45 / n);
        stepsNumber = parameters.getMaxEvaluationsNumber();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Realization of the method
    //

    private double solutionDistance;
    private double solutionPenalty;
    private double[] solution;

    @Override
    public double getDeviation()
    {
        return solutionDistance;
    }

    @Override
    public double getPenalty()
    {
        return solutionPenalty;
    }

    @Override
    public double[] getIntermediateSolution()
    {
        return solution.clone();
    }

    @Override
    public double[] getSolution() throws IOException, Exception
    {
        Random random = getParameters().initRandom();
        solutionDistance = Double.POSITIVE_INFINITY;
        solutionPenalty = Double.POSITIVE_INFINITY;
        double[] currSolution = StreamEx.of( params ).mapToDouble( p -> p.getValue() ).toArray();
        double[] prevSolution = new double[n];
        double delta = Math.max(0.0001, parameters.getDelta());
        double err = Double.POSITIVE_INFINITY;
        double[] scale = Util.constArray(n, 1.0);
        int sequence = 1;
        while( go && err > parameters.getDelta() )
        {
            Util.copy(prevSolution, currSolution);
            currSolution = calcSolution(prevSolution, scale, sequence, delta, random);
            err = OptimizationMethodUtils.absoluteError(prevSolution, currSolution);
            sequence++;
        }
        return solution;
    }

    private double[] calcSolution(double[] prevX, double[] scale, int sequence, double delta, Random random) throws IOException, Exception
    {
        int iteration = 1;
        double[] temperat = new double[n];
        double[] nextX = null;
        double[] prevResult = problem.testGoodnessOfFit(prevX, jobControl);
        double prevFunctionValue = objFunc(prevResult, sequence);
        double err = Double.POSITIVE_INFINITY;

        while( go && err > delta )
        {
            double temperature = Math.exp( -constant * Math.pow(iteration, 1.0 / n)); //t = exp(-c*i^[1/n])
            
            if( iteration % RESCALING_NUMBER == 0 )
                rescale(scale, temperat, sequence);

            for( int i = 0; i < n; ++i )
                temperat[i] = scale[i] * temperature; // t_i = t0 * s_i * t
            
            nextX = nextPoint(prevX, temperat, random);
            double[] nextResult = problem.testGoodnessOfFit(nextX, jobControl);
            double nextFunctionValue = objFunc(nextResult, sequence);
            err = OptimizationMethodUtils.absoluteError(prevX, nextX);
            
            if( err > delta && random.nextDouble() < Math.exp( - ( nextFunctionValue - prevFunctionValue ) / temperature) ) //accept new point with probability exp(-(F_new - F_old)/t)
            {
                Util.copy(prevX, nextX);
                Util.copy(prevResult, nextResult);
                prevFunctionValue = nextFunctionValue;
                
                if( objFunc(solutionDistance, solutionPenalty, sequence) > prevFunctionValue ) //check if new solution is better than current
                {
                    solutionDistance = prevResult[0];
                    solutionPenalty = prevResult[1];
                    solution = ArrayUtils.clone(prevX);
                }
            }
            iteration++;
            displayInfo();
            incPreparedness(problem.getEvaluationsNumber());
            if(problem.getEvaluationsNumber() >= getParameters().getMaxEvaluationsNumber())
                go = false;
        }
        return nextX;
    }

    private double[] nextPoint(double[] pCurrentX, double[] temperat, Random random)
    {
        double[] result = new double[pCurrentX.length];
        for( int i = 0; i < n; ++i )
        {
            double lowerBound = params.get(i).getLowerBound();
            double upperBound = params.get(i).getUpperBound();
            double newCoord = Double.MAX_VALUE;
            int timer = 0;
            while( ( newCoord < lowerBound || newCoord > upperBound ) && timer < 10 ) //search for coordinate while we are not in desired interval [lowerBound, upperBound]
            {
                double alpha = random.nextDouble();
                double variate = Math.signum(alpha - 0.5) * temperat[i] * ( Math.pow(1 + 1 / temperat[i], Math.abs(2 * alpha - 1)) - 1 ); // sgn(a-0.5)*t_i*[(1+/t_i)^|2a-1|-1]
                newCoord = Double.isNaN(variate) ? lowerBound : pCurrentX[i] + variate * ( upperBound - lowerBound );
                timer++;
            }
            result[i] = ( timer == 10 )?  Util.restrict(lowerBound, upperBound, newCoord): newCoord;
        }
        return result;
    }

    private void rescale(double[] scale, double[] temperat, int sequence) throws Exception
    {
        for( int i = 0; i < n && go; ++i )
            for( int j = 0; j < n; ++j )
            {
                population[i][j] = solution[j];
                if( i == j )
                    population[i][j] += ACCURACY;
            }

        calculateDistances(population);

        double[] s = new double[n];
        double s_max = Double.MIN_VALUE;
        double solutionObj = objFunc(solutionDistance, solutionPenalty, sequence);

        for( int i = 0; i < n && go; ++i )
        {
            double lowerBound = params.get(i).getLowerBound();
            double upperBound = params.get(i).getUpperBound();
            double currObj = objFunc(distances[i], penalties[i], sequence);
            s[i] = Math.max(ACCURACY,( upperBound - lowerBound ) * Math.abs( ( currObj - solutionObj ) / ACCURACY));
            if( s_max < s[i] && s[i] != Double.POSITIVE_INFINITY )
                s_max = s[i];
        }

        for( int i = 0; i < n && go; ++i )
        {
            if( s[i] != Double.POSITIVE_INFINITY )
            {
                temperat[i] *= s[i] / s_max;
                scale[i] = temperat[i];
            }
        }
    }

    private double objFunc(double[] x, int sequence) throws Exception
    {
        return x[0] + x[1] * sequence;
    }

    private double objFunc(double distance, double penalty, int sequence) throws Exception
    {
        return distance + sequence * penalty;
    }
}