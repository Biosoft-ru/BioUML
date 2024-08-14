package biouml.plugins.simulation.ode.jvode;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.simulation.OdeSimulatorOptions;
import biouml.plugins.simulation.ode.jvode.JVodeSupport.IterationType;
import biouml.plugins.simulation.ode.jvode.JVodeSupport.JacobianType;
import biouml.plugins.simulation.ode.jvode.JVodeSupport.Method;

public class JVodeOptions extends OdeSimulatorOptions
{
    public static final double ATOL_DEFAULT = 1E-20;
    public static final double RTOL_DEFAULT = 1E-12;

    private Method method;
    private IterationType iterationsMethod;
    private JacobianType jacobianApproximation;
    private int mu;
    private int ml;
    private int stepsLimit;
    private double hMin;
    private double hMaxInv;   

    public JVodeOptions(Method method, IterationType iterationType, JacobianType jacobianType)
    {
        this.method = method;
        this.iterationsMethod = iterationType;
        this.jacobianApproximation = jacobianType;
        mu = 0;
        ml = 0;
        stepsLimit = JVode.MXSTEP_DEFAULT;
        hMin = JVode.HMIN_DEFAULT;
        hMaxInv = JVode.HMAX_INV_DEFAULT;
        absTolerance = ATOL_DEFAULT;
        relTolerance = RTOL_DEFAULT;
    }

    //default options
    public JVodeOptions()
    {
        this( Method.BDF, IterationType.NEWTON, JacobianType.DENSE );
    }

    @PropertyName("Steps limit")
    @PropertyDescription("Limitation for solver steps count")
    public int getStepsLimit()
    {
        return stepsLimit;
    }
    public void setStepsLimit(int limit)
    {
        int oldValue = this.stepsLimit;
        this.stepsLimit = limit;
        firePropertyChange( "stepsLimit", oldValue, limit );
    }
    
    @PropertyName("Minimum time step")
    @PropertyDescription("Minimum time step.")
    public double getHMin()
    {
        return hMin;
    }
    public void setHMin(double min)
    {
        double oldValue = this.hMin;
        this.hMin = min;
        firePropertyChange( "hMin", oldValue, min );
    }

    @PropertyName("Maximum time step inverted")
    @PropertyDescription("1 / maximum time step.")
    public double getHMaxInv()
    {
        return hMaxInv;
    }
    public void setHMaxInv(double max)
    {
        double oldValue = this.hMaxInv;
        this.hMaxInv = max;
        firePropertyChange( "hMaxInv", oldValue, max );
    }
    public void setMu(int mu)
    {
        int oldValue = this.mu;
        this.mu = mu;
        firePropertyChange( "mu", oldValue, mu );
    }
    @PropertyName("Mu")
    @PropertyDescription("Upper bandwidth of Banded Jacobian")
    public int getMu()
    {
        return mu;
    }
    
    @PropertyName("Ml")
    @PropertyDescription("Lower bandwidth of Banded Jacobian")
    public int getMl()
    {
        return ml;
    }
    public void setMl(int ml)
    {
        int oldValue = this.ml;
        this.ml = ml;
        firePropertyChange( "ml", oldValue, ml );
    }

    @PropertyName("Integration method")
    @PropertyDescription("Integration method (ADAMS or BDF)")
    public int getMethod()
    {
        return method == null? 0: method.ordinal();
    }
    public void setMethod(int m)
    {
        int oldValue = getMethod();
        try
        {
            method = Method.values()[m];
        }
        catch( IndexOutOfBoundsException ex )
        {
            throw new IllegalArgumentException("Can not set method type = " + m + " only values from range (0-"
                    + ( Method.values().length - 1 ) + ") can be set");
        }
        firePropertyChange( "method", oldValue, m );
    }

   
    public Method getMethodType()
    {
        return method;
    }

    @PropertyName("Inner linear solver type")
    @PropertyDescription("Inner linear solver type")
    public int getIterations()
    {
        return iterationsMethod == null? 0: iterationsMethod.ordinal();
    }
    public void setIterations(IterationType iter)
    {
            iterationsMethod = iter;
    }
    public void setIterations(int iter)
    {
        int oldValue = getIterations();
        try
        {
            iterationsMethod = IterationType.values()[iter];
        }
        catch( IndexOutOfBoundsException ex )
        {
            throw new IllegalArgumentException("Can not set iteration type = " + iter + " only values from range (0-"
                    + ( IterationType.values().length - 1 ) + ") can be set");
        }
        firePropertyChange( "iterations", oldValue, iter );
        firePropertyChange( "*", null, null );
    }

    public IterationType getIterationType()
    {
        return iterationsMethod;
    }

    @PropertyName("Jacobian approximation type")
    @PropertyDescription("Jacobian approximation type")
    public int getJacobianApproximation()
    {
        return jacobianApproximation == null? 0:jacobianApproximation.ordinal();
    }
    public void setJacobianType(JacobianType jac)
    {
        jacobianApproximation = jac;
    }
    public void setJacobianApproximation(int jac)
    {
        int oldValue = getJacobianApproximation();
        try
        {
            jacobianApproximation = JacobianType.values()[jac];
        }
        catch( IndexOutOfBoundsException ex )
        {
            throw new IllegalArgumentException("Can not set jacobian type = " + jac + " only values from range (0-"
                    + ( JacobianType.values().length - 1 ) + ") can be set");
        }
        firePropertyChange( "jacobianApproximation", oldValue, jac );
        firePropertyChange( "*", null, null );
    }

    public JacobianType getJacobianType()
    {
        return jacobianApproximation;
    }


    public boolean isNotBandJacobian()
    {
        return isFunctional() || jacobianApproximation != JacobianType.BAND;
    }

    public boolean isFunctional()
    {
        return iterationsMethod == IterationType.FUNCTIONAL;
    }
}
