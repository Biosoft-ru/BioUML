package biouml.plugins.brain.model.cellular;

import com.developmentontheedge.beans.annot.PropertyName;
import biouml.plugins.brain.model.BrainModelProperties;

public class MinimalCellularModelProperties implements BrainModelProperties
{
	/*
	 * Minimal biophysical model at single neuron level based on time scale separation, 
	 * where the system is able reproduce the dynamics which have been identified in experiments, 2022.
	 * 
	 * cell dynamic:
	 * V_rate = (1/C_m)*(I_Cl+I_Na+I_K+I_pump)
	 * n_rate = (n_inf(V)-n)/tau_n
	 * DK_i_rate = (-gamma/omega_i)*(I_K-2.0*I_pump)
	 * K_g_rate = eps*(K_bath-K_o)
	 * 
	 * sources:
	 * Depannemaecker D., Ivanov A., Lillo D., Spek L.,  Bernard C., Jirsa V. A unified physiological framework of transitions between seizures, sustained ictal activity and depolarization block at the single neuron level.
	 */
	
	private double kBath = 12.5;
    private double cM = 1.0;
    private double tauN = 0.25;
    private double gCl = 7.5;
    private double gK = 22.0;
    private double gNa = 40.0;
    private double gKL = 0.12;
    private double gNaL = 0.02;
    private double omegaI = 2160.0;
    private double omegaO = 720.0;
    private double beta = 3.0;
    private double gamma = 0.04;
    private double eps = 0.01; // 0.001 in article, but 0.01 in git program.
    private double ro = 250.0;
    
	private boolean portsFlag = false; // if true than ports will be generated in equation diagram for further use in composite diagram

    public MinimalCellularModelProperties()
    {
    }
    
    @PropertyName("kBath")
    public double getKBath()
    {
        return kBath;
    }
    public void setKBath(double kBath)
    {
        this.kBath = kBath;
    }
    
    @PropertyName("cM")
    public double getCM()
    {
        return cM;
    }
    public void setCM(double cM)
    {
        this.cM = cM;
    }
    
    @PropertyName("tauN")
    public double getTauN()
    {
        return tauN;
    }
    public void setTauN(double tauN)
    {
        this.tauN = tauN;
    }
    
    @PropertyName("gCl")
    public double getGCl()
    {
        return gCl;
    }
    public void setGCl(double gCl)
    {
        this.gCl = gCl;
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
    
    @PropertyName("gNa")
    public double getGNa()
    {
        return gNa;
    }
    public void setGNa(double gNa)
    {
        this.gNa = gNa;
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
    
    @PropertyName("gNaL")
    public double getGNaL()
    {
        return gNaL;
    }
    public void setGNaL(double gNaL)
    {
        this.gNaL = gNaL;
    }
    
    @PropertyName("omegaI")
    public double getOmegaI()
    {
        return omegaI;
    }
    public void setOmegaI(double omegaI)
    {
        this.omegaI = omegaI;
    }
    
    @PropertyName("omegaO")
    public double getOmegaO()
    {
        return omegaO;
    }
    public void setOmegaO(double omegaO)
    {
        this.omegaO = omegaO;
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
    
    @PropertyName("gamma")
    public double getGamma()
    {
        return gamma;
    }
    public void setGamma(double gamma)
    {
        this.gamma = gamma;
    }
    
    @PropertyName("eps")
    public double getEps()
    {
        return eps;
    }
    public void setEps(double eps)
    {
        this.eps = eps;
    }
    
    @PropertyName("ro")
    public double getRo()
    {
        return ro;
    }
    public void setRo(double ro)
    {
        this.ro = ro;
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
