package ru.biosoft.analysis.optimization.methods;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.analysis.optimization.OptimizationMethod;
import ru.biosoft.analysis.optimization.OptimizationMethodParameters;
import ru.biosoft.analysis.optimization.OptimizationMethodParametersBeanInfo;
import ru.biosoft.analysis.optimization.OptimizationProblem;

import Jama.Matrix;
import Jama.EigenvalueDecomposition;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;

import com.developmentontheedge.beans.BeanInfoConstants;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;


/*
 * This algorithm was created based on the article
 * Stephen M. Goldfeld et al
 * Maximization by Quadratic Hill-climbing.
 * (Econometric Research Program, Research Memorandum #72, January 19, 1965)
 */
public class GoldfeldOptMethod extends OptimizationMethod<GoldfeldOptMethod.GoldfeldOptMethodParameters>
{
    private static final double DELTAINSIDE = 0.00001; // An accuracy of a gradient and a hessian calculating.

    private boolean fail = false;

    public GoldfeldOptMethod(DataCollection<?> origin, String name)
    {
        super(origin, name, null);
        parameters = new GoldfeldOptMethodParameters();
    }

    @PropertyName("Method parameters")
    @PropertyDescription("Method parameters.")
    public class GoldfeldOptMethodParameters extends OptimizationMethodParameters
    {
        private double deltaOutside;

        public GoldfeldOptMethodParameters()
        {
            this.deltaOutside = 0.0001;
            random = null;
        }

        @PropertyName("Calculation accuracy")
        @PropertyDescription("The admissible absolute error between previous and current solutions calculated using two-norm.")
        public double getDeltaOutside()
        {
            return this.deltaOutside;
        }

        public void setDeltaOutside(double deltaOutside)
        {
            double oldValue = this.deltaOutside;
            this.deltaOutside = deltaOutside;
            firePropertyChange("deltaOutside", oldValue, deltaOutside);
        }

        @Override
        public void read(Properties properties, String prefix)
        {
            super.read(properties, prefix);
            try
            {
                deltaOutside = Double.parseDouble(properties.getProperty(prefix + "deltaOutside"));
            }
            catch( Exception e )
            {
            }
        }

        @Override
        public void write(Properties properties, String prefix)
        {
            super.write(properties, prefix);
            properties.put(prefix + "deltaOutside", Double.toString(deltaOutside));
        }
    }

    public static class GoldfeldOptMethodParametersBeanInfo extends OptimizationMethodParametersBeanInfo
    {
        public GoldfeldOptMethodParametersBeanInfo()
        {
            super(GoldfeldOptMethodParameters.class);
        }

        @Override
        public void initProperties() throws Exception
        {
            super.initProperties();
            property("deltaOutside").numberFormat(BeanInfoConstants.NUMBER_FORMAT_NONE).add();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Optimization problem
    //

    @Override
    public void setOptimizationProblem(OptimizationProblem problem)
    {
        this.problem = problem;
        if( problem != null )
        {
            params = problem.getParameters();
            n = params.size();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Realization of the method
    //

    private double deviation;
    private double penalty;
    private double[] solution;

    @Override
    public double getDeviation()
    {
        return deviation;
    }

    @Override
    public double getPenalty()
    {
        return penalty;
    }

    @Override
    public double[] getIntermediateSolution()
    {
        return solution.clone();
    }

    @Override
    public double[] getSolution() throws IOException, Exception
    {
        solution = new double[n];
        double[] prevX = new double[n];

        for( int i = 0; i < n; ++i )
            solution[i] = params.get(i).getValue();

        int sequence = 1;
        double err = Double.POSITIVE_INFINITY;

        deviation = Double.POSITIVE_INFINITY;
        penalty = Double.POSITIVE_INFINITY;

        while( go && err > parameters.getDeltaOutside() )
        {
            for( int i = 0; i < n; ++i )
            {
                prevX[i] = solution[i];
            }

            solution = calculateSolution(prevX, sequence);
            if( fail )
            {
                log.info("Initial values of the parameters are failed.");
                break;
            }
            double[] goodness = problem.testGoodnessOfFit(solution, jobControl);

            deviation = goodness[0];
            penalty = goodness[1];

            sequence++;

            err = OptimizationMethodUtils.absoluteError(prevX, solution);
        }

        return solution;
    }

    private double[] calculateSolution(double[] pCurrX, int pSequence) throws Exception
    {
        Matrix result = new Matrix(pCurrX.length, 1);

        for( int i = 0; i < n; ++i )
            result.set(i, 0, pCurrX[i]);

        while( go )
        {
            result = calculateNextPointWithNullNorm(result, pSequence);

            if( fail )
                break;

            Matrix mHessian = calculateHessian(calculateGradient(result, pSequence), result, pSequence);

            double[] eigValAndVect = getLargestEigenvalue(mHessian);
            double largestEigenvalue = eigValAndVect[0];

            Matrix eigenvector = new Matrix(n, 1);
            for( int i = 0; i < n; ++i )
                eigenvector.set(i, 0, eigValAndVect[i + 1]);

            if( largestEigenvalue < 0 )
                break;
            result.plusEquals(eigenvector);
        }

        double[] resultArray = new double[n];
        for( int i = 0; i < n; ++i )
            resultArray[i] = result.get(i, 0);

        return resultArray;
    }

    /*
     * The following method calculates the gradient in a given point
     * by the divided differences method.
     */

    private Matrix calculateGradient(Matrix x, int pSequence) throws Exception
    {
        Matrix result = new Matrix(n, 1);
        double currentValue = objectiveFunction(x, pSequence);

        for( int i = 0; i < n && go; ++i )
        {
            Matrix increment = new Matrix(n, 1);
            increment.set(i, 0, DELTAINSIDE);
            Matrix xPlusDelta = x.plus(increment);
            result.set(i, 0, ( objectiveFunction(xPlusDelta, pSequence) - currentValue ) / DELTAINSIDE);
        }
        return result;
    }

    /*
     * The following method calculates the Hessian matrix in a given point
     * by the divided differences method.
     */
    private Matrix calculateHessian(Matrix mGradient, Matrix x, int pSequence) throws Exception
    {
        Matrix result = new Matrix(n, n);
        for( int i = 0; i < n; ++i )
        {
            Matrix increment = new Matrix(n, 1);
            increment.set(i, 0, DELTAINSIDE);

            Matrix xPlusDelta = x.plus(increment);
            double obj2 = objectiveFunction(xPlusDelta, pSequence);

            for( int j = 0; j < n && go; ++j )
            {
                increment = new Matrix(n, 1);
                increment.set(i, 0, DELTAINSIDE);
                increment.set(j, 0, increment.get(j, 0) + DELTAINSIDE);

                xPlusDelta = x.plus(increment);
                double obj1 = objectiveFunction(xPlusDelta, pSequence);

                result.set(j, i, ( ( obj1 - obj2 ) / DELTAINSIDE - mGradient.get(j, 0) ) / DELTAINSIDE);
            }
        }
        return result;
    }

    private Matrix calculateNextPointWithNullNorm(Matrix pCurrX, int pSequence) throws Exception
    {
        Matrix gradient = calculateGradient(pCurrX, pSequence);

        double r = 0;
        boolean rNotChange = true;
        double currValOfObjFunc, currValOfTaylorExp = 1, prevValOfObjFunc = 0, prevValOfTaylorExp = 0;

        currValOfObjFunc = objectiveFunction(pCurrX, pSequence);

        while( go && gradient.normF() > DELTAINSIDE )
        {
            Matrix hessian = calculateHessian(gradient, pCurrX, pSequence);

            double largestEigenvalue = getLargestEigenvalue(hessian)[0];

            double z = ( currValOfObjFunc - prevValOfObjFunc ) / ( currValOfTaylorExp - prevValOfTaylorExp );

            if( rNotChange )
            {
                r = 0.1;
                rNotChange = false;
            }
            else
                r = calcR(r, z);

            double alpha = largestEigenvalue + r * gradient.normF();

            Matrix nextX = calculateNextPoint(pCurrX, gradient, hessian, alpha, pSequence);

            if( fail )
                break;

            prevValOfObjFunc = currValOfObjFunc;
            prevValOfTaylorExp = currValOfTaylorExp;

            currValOfObjFunc = objectiveFunction(nextX, pSequence);
            currValOfTaylorExp = taylorExp(pCurrX, nextX, gradient, hessian, prevValOfTaylorExp, pSequence);

            pCurrX = nextX.copy();
            gradient = calculateGradient(pCurrX, pSequence);
        }

        return pCurrX;
    }

    private double[] getLargestEigenvalue(Matrix pMatrix)
    {
        double[] result = new double[pMatrix.getRowDimension() + 1];

        double eigVal = -Double.MAX_VALUE;
        boolean notChange = true;
        int resultNum = 0;

        int mSize = pMatrix.getColumnDimension();

        EigenvalueDecomposition eigDec = new EigenvalueDecomposition(pMatrix);
        double[] eigenvaluesRe = eigDec.getRealEigenvalues();

        for( int i = 0; i < mSize - 1; ++i )
            if( eigenvaluesRe[i] != eigenvaluesRe[i + 1] && eigenvaluesRe[i] > eigVal )
            {
                eigVal = eigenvaluesRe[i];
                resultNum = i;
                notChange = false;
            }

        if( eigenvaluesRe[mSize - 1] != eigenvaluesRe[mSize - 2] && eigenvaluesRe[mSize - 1] > eigVal )
        {
            eigVal = eigenvaluesRe[mSize - 1];
            resultNum = mSize - 1;
            notChange = false;
        }

        if( notChange )
        {
            double[] eigenvaluesIm = eigDec.getImagEigenvalues();

            for( int i = 0; i < mSize; i += 2 )
            {
                double newValue = Math.sqrt(Math.pow(eigenvaluesRe[i], 2) + Math.pow(eigenvaluesIm[i], 2));
                if( newValue > eigVal )
                {
                    eigVal = newValue;
                    resultNum = i;
                }
            }
        }

        Matrix eigVectors = eigDec.getV();

        result[0] = eigVal;
        for( int i = 0; i < mSize; ++i )
            result[i + 1] = eigVectors.get(i, resultNum);

        return result;
    }

    private double calcR(double pR, double pZ)
    {
        if( pZ <= 0 || pZ > 2 )
            pR *= 4;
        if( 0 < pZ && pZ <= 0.7 )
            pR *= pZ * 3.6 / 0.7;
        if( 0.7 < pZ && pZ < 1.3 )
            pR *= 0.4;
        if( 1.3 <= pZ && pZ <= 2 )
            pR *= ( pZ - 1.3 ) * 3.6 / 0.7;
        return pR;
    }

    private Matrix calculateNextPoint(Matrix pCurrX, Matrix pGradient, Matrix pHessian, double pAlpha, int pSequence) throws Exception
    {
        Matrix pNextX = new Matrix(n, 1);
        if( pAlpha < 0 )
        {
            try
            {
                pNextX = pCurrX.minus(pHessian.inverse().times(pGradient));
            }

            catch( Exception e )
            {
                log.log(Level.SEVERE, "The null matrix is appear to invert. A calculation accuracy must be worse!");
                jobControl.terminate();
            }
        }
        else
        {
            //workMatrix = (hessian - pAlpha * IdentityMatrix)^(-1)
            Matrix workMatrix = pHessian.minus(Matrix.identity(n, n).times(pAlpha)).inverse();

            pNextX = pCurrX.minus(workMatrix.times(pGradient));

            double h = calculateAngle(pNextX, pCurrX);

            Matrix lastGoodX = new Matrix(n, 1);

            boolean inCycle = false;

            int timer = 0;
            while( go && objectiveFunction(pNextX, pSequence) < objectiveFunction(pCurrX, pSequence) && timer < 50 )
            {
                inCycle = true;
                lastGoodX = pNextX.copy();
                pNextX = pCurrX.minus(workMatrix.times(pGradient).times(h));
                h *= calculateAngle(pNextX, pCurrX);
                timer++;
            }

            if( timer == 50 )
                fail = true;


            if( inCycle )
                pNextX = lastGoodX;
        }
        return pNextX;
    }

    /*
     * The following method calculates the angle between given vectors.
     */
    private double calculateAngle(Matrix firstVect, Matrix secondVect)
    {
        double result = 0;
        double cosOfAngle = firstVect.transpose().times(secondVect).get(0, 0) / ( firstVect.normF() * secondVect.normF() );
        result = Math.acos(cosOfAngle);
        return result;
    }

    private double objectiveFunction(Matrix x, int pSequence) throws Exception
    {
        double phi = 0;
        for( int i = 0; i < n; ++i )
        {
            phi += pSequence * Math.pow(Math.max(0, params.get(i).getLowerBound() - x.get(i, 0)), 2);
            phi += pSequence * Math.pow(Math.max(0, x.get(i, 0) - params.get(i).getUpperBound()), 2);
        }

        double[] xArray = new double[n];
        for( int i = 0; i < n; ++i )
        {
            xArray[i] = x.get(i, 0);
        }

        double[] goodness = problem.testGoodnessOfFit(xArray, jobControl);

        displayInfo();
        incPreparedness(problem.getEvaluationsNumber());

        return -goodness[0] - ( pSequence * goodness[1] + phi );
    }

    /*
     * The following method calculates a second-order Taylor series expansion
     * of the objective function around a point fixPoint in a point x.
     * Q(x) = Q(a) + (x - a)' * gradient(a) + 0.5 * (x - a)' * hessian(a) * (x - a),
     * where a is the fixPoint, Q is the objective function.
     */
    private double taylorExp(Matrix fixPoint, Matrix x, Matrix pGradient, Matrix pHessian, double objFuncValue, int pSequence)
            throws Exception
    {
        Matrix difference = x.minus(fixPoint);
        double firstSum = difference.transpose().times(pGradient).get(0, 0);
        double secondSum = 0.5 * difference.transpose().times(pHessian).times(difference).get(0, 0);
        return objFuncValue + firstSum + secondSum;
    }
}