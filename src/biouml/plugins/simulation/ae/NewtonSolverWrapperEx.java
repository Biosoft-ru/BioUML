package biouml.plugins.simulation.ae;

import ru.biosoft.util.bean.JSONBean;

import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.simulation.Options;

@SuppressWarnings ( "serial" )
public class NewtonSolverWrapperEx extends Options implements AeSolver, JSONBean
{
    private boolean isSuccess = false;

    private double maxIts = 20000;
    private double tolF = 1.0e-10;
    private double tolMin = 1.0e-12;
    private double tolX = 1.0e-13;

    public NewtonSolverWrapperEx()
    {
    }

    @Override
    public double[] solve(double[] initialGuess, AeModel model) throws Exception
    {
        isSuccess = true;
        try
        {
            NewtonSolver.solve( initialGuess, model, maxIts, tolF, tolMin, tolX );
        }
        catch( Exception e )
        {
            isSuccess = false;
        }

        return initialGuess;
    }

    @Override
    public boolean isSuccess()
    {
        return isSuccess;
    }

    @Override
    public String getMessage()
    {
        return isSuccess ? "" : "Newton solver can't find solution.";
    }

    @PropertyName ( "Maximum iterations" )
    public double getMaxIts()
    {
        return maxIts;
    }

    public void setMaxIts(double maxIts)
    {
        double oldValue = this.maxIts;
        this.maxIts = maxIts;
        firePropertyChange("maxIts", oldValue, maxIts);
    }

    @PropertyName ( "Function tolerance" )
    public double getTolF()
    {
        return tolF;
    }

    public void setTolF(double tolF)
    {
        double oldValue = this.tolF;
        this.tolF = tolF;
        firePropertyChange("tolF", oldValue, tolF);
    }

    @PropertyName ( "Minimal gradient tolerance" )
    public double getTolMin()
    {
        return tolMin;
    }

    public void setTolMin(double tolMin)
    {
        double oldValue = this.tolMin;
        this.tolMin = tolMin;
        firePropertyChange("tolMin", oldValue, tolMin);
    }

    @PropertyName ( "Points tolerance" )
    public double getTolX()
    {
        return tolX;
    }

    public void setTolX(double tolX)
    {
        double oldValue = this.tolX;
        this.tolX = tolX;
        firePropertyChange("tolX", oldValue, tolX);
    }
}
