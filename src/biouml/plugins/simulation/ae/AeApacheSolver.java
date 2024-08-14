package biouml.plugins.simulation.ae;

import org.apache.commons.math3.analysis.MultivariateVectorFunction;

import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.simulation.Options;

@SuppressWarnings ( "serial" )
public abstract class AeApacheSolver extends Options implements AeSolver
{
    protected double ftol = 1e-6;
    protected int maxIter = 1000;
    protected int maxEval = 1000;
    protected double lastResidualNorm = 0;

    protected AeModel model;

    @Override
    public boolean isSuccess()
    {
        return lastResidualNorm <= ftol;
    }

    @Override
    public String getMessage()
    {
        if( isSuccess() )
            return "";
        return "Problem is unsolvable or function value (" + lastResidualNorm + ") is bigger than permissible function tolerance (" + ftol
                + ").";
    }

    protected double normOf(double[] residual)
    {
        double norm = 0.0;
        for( double r : residual )
            norm += r * r;
        norm = Math.sqrt(norm);
        return norm;
    }

    @PropertyName ( "Function tolerance" )
    public double getFtol()
    {
        return ftol;
    }
    public void setFtol(double ftol)
    {
        Object oldValue = this.ftol;
        this.ftol = ftol;
        firePropertyChange("ftol", oldValue, ftol);
    }

    @PropertyName ( "Max iterations" )
    public int getMaxIter()
    {
        return maxIter;
    }
    public void setMaxIter(int maxIter)
    {
        Object oldValue = this.maxIter;
        this.maxIter = maxIter;
        firePropertyChange("maxIter", oldValue, maxIter);
    }

    @PropertyName ( "Max evaluations" )
    public int getMaxEval()
    {
        return maxEval;
    }
    public void setMaxEval(int maxIter)
    {
        Object oldValue = this.maxEval;
        this.maxEval = maxIter;
        firePropertyChange("maxEval", oldValue, maxIter);
    }

    protected class VectorFunction implements MultivariateVectorFunction
    {
        @Override
        public double[] value(double[] x)
        {
            return model.solveAlgebraic(x);
        }
    }
}
