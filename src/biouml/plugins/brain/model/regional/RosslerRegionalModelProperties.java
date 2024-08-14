package biouml.plugins.brain.model.regional;

import com.developmentontheedge.beans.annot.PropertyName;
import biouml.plugins.brain.model.BrainModelProperties;

public class RosslerRegionalModelProperties implements BrainModelProperties
{	
	/*
	 * Rossler nonlinear oscillator regional model
	 * 
	 * region dynamic:
	 * x_rate = -omega*y - z
	 * y_rate = omega*x + alpha*y
	 * z_rate = b + z*(x - gamma)
	 * 
	 * synaptic strength dynamic:
	 * eta_rate = (1-eta)*(a1*(eta-eta_eq)*(eta-eta_th)+b1*u_exc)
	 * eta_eq_rate = -a2*eta_eq+b2*(eta-eta_eq)
	 * 
	 * Variables of interest to be used by monitors: sum(lambda_i*x_i)
	 * 
	 * sources:
	 * Mazen A., James S.W., Graham C.G. Synaptic plasticity-based model for epileptic seizures
	 * Zhang H.H., Su J.Z., Wang Q.Y., Liu Y.M., Good L., Pascual J.M. Predicting seizure by modeling synaptic plasticity based on EEG signals - a case study of inherited epilepsy 
	 */
	
	private double omega = 1.0;
    private double alpha = 0.15;
    private double b = 0.2;
    private double gamma = 8.5;
    
    private double a1 = 0.5;
    private double b1 = 0.005;
    private double a2 = 0.0013;
    private double b2 = 0.005;
    private double etaTh = 0.5;
    
    private boolean portsFlag = false; // if true than ports will be generated in equation diagram for further use in composite diagram
    
    public RosslerRegionalModelProperties()
    {
    }
    
    @PropertyName("omega")
    public double getOmega()
    {
        return omega;
    }
    public void setOmega(double omega)
    {
        this.omega = omega;
    }

    @PropertyName("alpha")
    public double getAlpha()
    {
        return alpha;
    }
    public void setAlpha(double alpha)
    {
        this.alpha = alpha;
    }

    @PropertyName("b")
    public double getB()
    {
        return b;
    }
    public void setB(double b)
    {
        this.b = b;
    }

    @PropertyName("gamma")
    public double getGamma()
    {
        return gamma;
    }
    public void setGamma(double gamma)
    {
        this.gamma = gamma;
    }
    
    @PropertyName("a1")
    public double getA1()
    {
        return a1;
    }
    public void setA1(double a1)
    {
        this.a1 = a1;
    }
    
    @PropertyName("b1")
    public double getB1()
    {
        return b1;
    }
    public void setB1(double b1)
    {
        this.b1 = b1;
    }
    
    @PropertyName("a2")
    public double getA2()
    {
        return a2;
    }
    public void setA2(double a2)
    {
        this.a2 = a2;
    }
    
    @PropertyName("b2")
    public double getB2()
    {
        return b2;
    }
    public void setB2(double b2)
    {
        this.b2 = b2;
    }
    
    @PropertyName("etaTh")
    public double getEtaTh()
    {
        return etaTh;
    }
    public void setEtaTh(double etaTh)
    {
        this.etaTh = etaTh;
    }
    
    @PropertyName("portsFlag")
    public boolean getPortsFlag()
    {
        return portsFlag;
    }
    public void setPortsFlag(boolean portsFlag)
    {
        this.portsFlag = portsFlag;
    }
}
