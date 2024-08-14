package biouml.plugins.brain.model.regional;

import com.developmentontheedge.beans.annot.PropertyName;
import biouml.plugins.brain.model.BrainModelProperties;

public class EpileptorRegionalModelProperties implements BrainModelProperties
{	
	/*
	 * Epileptor regional model
	 * 
	 * region dynamic:
	 * x1_rate = (1 / tau1) * (y1 - f1(x1, x2, z) - z + I1)
	 * y1_rate = y0 - 5 * x1^2 - y1
	 * z_rate = (1 / tau0) * (h(x1) - z)
	 * x2_rate = -y2 + x2 - x2^3 + I2 + 2 * g - 0.3 * (z - 3.5)
	 * y2_rate = (1 / tau2) * (-y2 + f2(x1, x2))
	 * g_rate = -gamma*(g - 0.1*x1) 
	 * 
	 * Variables of interest to be used by monitors: -x1_i+x2_i or +x1_i+x2_i for channel i
	 * 
	 * sources:
	 * Jirsa V.K., Stacey W.C., Quilichini P.P., Ivanov A.I., Bernard C. On the nature of seizure dynamics 
	 * Proix T., Bartolomei F., Chauvel P., Bernard C., & Jirsa V.K. Permittivity coupling across brain regions determines seizure recruitment in partial epilepsy
	 * Proix T., Bartolomei F., Guye M., Jirsa V.K. Individual brain structure and modelling predict seizure propagation 
	 */
	
	private boolean modification = false; // the slow permittivity variable z can be modify to account for the time difference between interictal and ictal states
	private double w = 1.0; // scaling factor
	private double x0 = -1.6;
	private double y0 = 1.0;
	private double tau0 = 2857.0;
	private double tau1 = 1.0;
	private double tau2 = 10;
	private double I1 = 3.1;
	private double I2 = 0.45;
	private double gamma = 0.01;
	
	private boolean portsFlag = false; // if true than ports will be generated in equation diagram for further use in composite diagram
 
    public EpileptorRegionalModelProperties()
    {
    }
    
    @PropertyName("modification")
    public boolean getModification()
    {
        return modification;
    }
    public void setModification(boolean modification)
    {
        this.modification = modification;
        if (modification)
        {
        	setX0(2.5);
        }
    }

    @PropertyName("w")
    public double getW()
    {
        return w;
    }
    public void setW(double w)
    {
        this.w = w;
    }
    
    @PropertyName("x0")
    public double getX0()
    {
        return x0;
    }
    public void setX0(double x0)
    {
        this.x0 = x0;
    }

    @PropertyName("y0")
    public double getY0()
    {
        return y0;
    }
    public void setY0(double y0)
    {
        this.y0 = y0;
    }
    
    @PropertyName("tau0")
    public double getTau0()
    {
        return tau0;
    }
    public void setTau0(double tau0)
    {
        this.tau0 = tau0;
    }
    
    @PropertyName("tau1")
    public double getTau1()
    {
        return tau1;
    }
    public void setTau1(double tau1)
    {
        this.tau1 = tau1;
    }
    
    @PropertyName("tau2")
    public double getTau2()
    {
        return tau2;
    }
    public void setTau2(double tau2)
    {
        this.tau2 = tau2;
    }
    
    @PropertyName("I1")
    public double getI1()
    {
        return I1;
    }
    public void setI1(double I1)
    {
        this.I1 = I1;
    }
    
    @PropertyName("I2")
    public double getI2()
    {
        return I2;
    }
    public void setI2(double I2)
    {
        this.I2 = I2;
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
