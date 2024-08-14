package biouml.plugins.brain.model.cellular;

import com.developmentontheedge.beans.annot.PropertyName;
import biouml.plugins.brain.model.BrainModelProperties;

public class OxygenCellularModelProperties implements BrainModelProperties
{
	/*
	 * Cellular model with oxygen dynamic
	 * 
	 * cell dynamic:
	 * V_rate = (1/C)*(I_ext-I_Na-I_K-I_Cl)
	 * K_o_rate = 0.001*(gamma*beta*I_K-2*beta*I_pump-I_diff-I_glia_2*I_gliapump) (0.001 because mM/msec)
	 * Na_i_rate = 0.001*(-gamma*I_Na-3*I_pump)
	 * O2_o_rate = 0.001*(-alpha*lambda*(I_pump+I_gliapump)+eps_O*(O2_bath-O2_o))
	 * 
	 * sources:
	 * Wei Y., Ullah G., Ingram J., Schiff SJ. Oxygen and seizure dynamics: II. Computational modeling.
	 */
	
    private double c = 1.0;
    private double iExt = 0.0;
    private double gNa = 30.0;
    private double gK = 25.0;
    private double gNaL = 0.0175;
    private double gKL = 0.05;
    private double gClL = 0.05;
    private double gamma = 0.0445;
    private double beta = 7.0;
    private double roMax = 1.25;
    private double gGlia = 8.0;
    private double epsK = 0.33;
    private double epsO = 0.17;
    private double alpha = 5.3;
    private double lambda = 1.0;
    
	private boolean portsFlag = false; // if true than ports will be generated in equation diagram for further use in composite diagram
    
    public OxygenCellularModelProperties()
    {
    }
    
    @PropertyName("c")
    public double getC()
    {
        return c;
    }
    public void setC(double c)
    {
        this.c = c;
    }
    
    @PropertyName("iExt")
    public double getIExt()
    {
        return iExt;
    }
    public void setIExt(double iExt)
    {
        this.iExt = iExt;
    }
    
    @PropertyName("gNa")
    public double getGNa()
    {
        return gNa;
    }
    public void setGNa(double gNa)
    {
        this.gNa = gNa;
    }
    
    @PropertyName("gK")
    public double getGK()
    {
        return gK;
    }
    public void setGK(double gK)
    {
        this.gK = gK;
    }
    
    @PropertyName("gNaL")
    public double getGNaL()
    {
        return gNaL;
    }
    public void setGNaL(double gNaL)
    {
        this.gNaL = gNaL;
    }
    
    @PropertyName("gKL")
    public double getGKL()
    {
        return gKL;
    }
    public void setGKL(double gKL)
    {
        this.gKL = gKL;
    }
    
    @PropertyName("gClL")
    public double getGClL()
    {
        return gClL;
    }
    public void setGClL(double gClL)
    {
        this.gClL = gClL;
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
    
    @PropertyName("beta")
    public double getBeta()
    {
        return beta;
    }
    public void setBeta(double beta)
    {
        this.beta = beta;
    }
    
    @PropertyName("roMax")
    public double getRoMax()
    {
        return roMax;
    }
    public void setRoMax(double roMax)
    {
        this.roMax = roMax;
    }
    
    @PropertyName("gGlia")
    public double getGGlia()
    {
        return gGlia;
    }
    public void setGGlia(double gGlia)
    {
        this.gGlia = gGlia;
    }
    
    @PropertyName("epsK")
    public double getEpsK()
    {
        return epsK;
    }
    public void setEpsK(double epsK)
    {
        this.epsK = epsK;
    }
    
    @PropertyName("epsO")
    public double getEpsO()
    {
        return epsO;
    }
    public void setEpsO(double epsO)
    {
        this.epsO = epsO;
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
    
    @PropertyName("lambda")
    public double getLambda()
    {
        return lambda;
    }
    public void setLambda(double lambda)
    {
        this.lambda = lambda;
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
