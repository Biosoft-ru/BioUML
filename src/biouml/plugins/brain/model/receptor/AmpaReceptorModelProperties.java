package biouml.plugins.brain.model.receptor;

import com.developmentontheedge.beans.annot.PropertyName;
import biouml.plugins.brain.model.BrainModelProperties;
import biouml.plugins.simulation.Options;

public class AmpaReceptorModelProperties extends Options implements BrainModelProperties
{
	/*
	 * Biophysical model of AMPA-receptor trafficking in dendritic spine.
	 * 
	 * sources:
	 * Earnshaw B.A., Bressloff P.C. Biophysical model of AMPA receptor trafficking and its regulation during long-term potentiation/long-term depression.
	 */
	
	public static final String REGIME_NORMAL = "normal"; // basal conditions
	public static final String REGIME_LTP = "ltp"; // long-term potentiation
	public static final String REGIME_LTD = "ltd"; // long-term depression
	public static final String REGIME_LTD_FOLLOWED_BY_LTP = "ltdFollowedByLtp";
	static final String[] availableRegimes = new String[] {REGIME_NORMAL, REGIME_LTP, REGIME_LTD, REGIME_LTD_FOLLOWED_BY_LTP};
    private String regimeType = REGIME_LTD_FOLLOWED_BY_LTP;
    
    public static final String MODEL_SBML = "sbml"; // sbml model in sbgn notation
    public static final String MODEL_MATH = "math"; // model with math equations only
    static final String[] availableModelTypes = new String[] {MODEL_SBML, MODEL_MATH};
    private String modelType = MODEL_SBML;
	
	double lTotal = 159.15;
    double alpha1 = 1.0E-6;
    double alpha2 = 1.0E-4;
    double beta1 = 1.0E-5;
    double beta2a = 1.0E-5;
    double beta2b = 0.1;
    double c = 0.65;
    double delta1 = 0.2778;
    double gamma = 0.001;
    double h1 = 0.001257;
    double h2a = 0.001257;
    double h2b = 0.001257;
    double k1 = 0.01667;
    double k2 = 0.01667;
    double kappa1 = 5.556E-4;
    double kappa2 = 0.001667;
    double mu = 0.01;
    double nu = 0.01;
    double omega1 = 0.001257;
    double omega2 = 0.001257;
    double sigma1 = 0.2778;
    double sigma2 = 0.1667;
	
	private boolean portsFlag = false; // if true than ports will be generated in equation diagram for further use in composite diagram
	
    @PropertyName("Regime type")
    public String getRegimeType()
    {
        return regimeType;
    }
    public void setRegimeType(String regimeType)
    {
    	switch (regimeType)
    	{
    	    case REGIME_NORMAL:
    	    	setMu(0.0);
    	    	setGamma(0.0);
    	    	setAlpha1(1.0E-6);
    	    	setKappa1(5.556E-4);
    	    	setH1(1.257E-3);
    	    	setC(0.0);
    	    	break;
    	    case REGIME_LTP:
    	    	setMu(0.0);
    	    	setGamma(0.0);
    	    	setAlpha1(0.001);
    	    	setKappa1(0.0556);
    	    	setH1(0.01);
    	    	setC(0.65);
    	    	break;
    	    case REGIME_LTD:
    	    	setMu(0.01);
    	    	setGamma(0.001);
    	    	setAlpha1(1.0E-6);
    	    	setKappa1(5.556E-4);
    	    	setH1(0.001257);
    	    	setC(0.0);
    	    	break;
    	    case REGIME_LTD_FOLLOWED_BY_LTP:
    	    	setMu(0.01);
    	    	setGamma(0.001);
    	    	setAlpha1(1.0E-6);
    	    	setKappa1(5.556E-4);
    	    	setH1(0.001257);
    	    	setC(0.325);
    	    	break;
    	}
        Object oldValue = this.regimeType;
        this.regimeType = regimeType;
        firePropertyChange("regimeType", oldValue, regimeType);
        firePropertyChange("*", null, null);
    }
	
    @PropertyName("lTotal")
    public double getLTotal()
    {
        return lTotal;
    }
    public void setLTotal(double lTotal)
    {
        this.lTotal = lTotal;
    }
    
    @PropertyName("alpha1")
    public double getAlpha1()
    {
        return alpha1;
    }
    public void setAlpha1(double alpha1)
    {
        this.alpha1 = alpha1;
    }
    
    @PropertyName("alpha2")
    public double getAlpha2()
    {
        return alpha2;
    }
    public void setAlpha2(double alpha2)
    {
        this.alpha2 = alpha2;
    }
    
    @PropertyName("beta1")
    public double getBeta1()
    {
        return beta1;
    }
    public void setBeta1(double beta1)
    {
        this.beta1 = beta1;
    }
    
    @PropertyName("beta2a")
    public double getBeta2a()
    {
        return beta2a;
    }
    public void setBeta2a(double beta2a)
    {
        this.beta2a = beta2a;
    }
    
    @PropertyName("beta2b")
    public double getBeta2b()
    {
        return beta2b;
    }
    public void setBeta2b(double beta2b)
    {
        this.beta2b = beta2b;
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
    
    @PropertyName("delta1")
    public double getDelta1()
    {
        return delta1;
    }
    public void setDelta1(double delta1)
    {
        this.delta1 = delta1;
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
    
    @PropertyName("h1")
    public double getH1()
    {
        return h1;
    }
    public void setH1(double h1)
    {
        this.h1 = h1;
    }
    
    @PropertyName("h2a")
    public double getH2a()
    {
        return h2a;
    }
    public void setH2a(double h2a)
    {
        this.h2a = h2a;
    }
    
    @PropertyName("h2b")
    public double getH2b()
    {
        return h2b;
    }
    public void setH2b(double h2b)
    {
        this.h2b = h2b;
    }
    
    @PropertyName("k1")
    public double getK1()
    {
        return k1;
    }
    public void setK1(double k1)
    {
        this.k1 = k1;
    }
    
    @PropertyName("k2")
    public double getK2()
    {
        return k2;
    }
    public void setK2(double k2)
    {
        this.k2 = k2;
    }
    
    @PropertyName("kappa1")
    public double getKappa1()
    {
        return kappa1;
    }
    public void setKappa1(double kappa1)
    {
        this.kappa1 = kappa1;
    }
    
    @PropertyName("kappa2")
    public double getKappa2()
    {
        return kappa2;
    }
    public void setKappa2(double kappa2)
    {
        this.kappa2 = kappa2;
    }
    
    @PropertyName("mu")
    public double getMu()
    {
        return mu;
    }
    public void setMu(double mu)
    {
        this.mu = mu;
    }
    
    @PropertyName("nu")
    public double getNu()
    {
        return nu;
    }
    public void setNu(double nu)
    {
        this.nu = nu;
    }
    
    @PropertyName("omega1")
    public double getOmega1()
    {
        return omega1;
    }
    public void setOmega1(double omega1)
    {
        this.omega1 = omega1;
    }
    
    @PropertyName("omega2")
    public double getOmega2()
    {
        return omega2;
    }
    public void setOmega2(double omega2)
    {
        this.omega2 = omega2;
    }
    
    @PropertyName("sigma1")
    public double getSigma1()
    {
        return sigma1;
    }
    public void setSigma1(double sigma1)
    {
        this.sigma1 = sigma1;
    }
    
    @PropertyName("sigma2")
    public double getSigma2()
    {
        return sigma2;
    }
    public void setSigma2(double sigma2)
    {
        this.sigma2 = sigma2;
    }
    
    @PropertyName("model type")
    public String getModelType()
    {
        return modelType;
    }
    public void setModelType(String modelType)
    {
        String oldValue = this.modelType;
        this.modelType = modelType;
        firePropertyChange("modelType", oldValue, modelType);
        firePropertyChange("*", null, null);
    }
	
    @PropertyName("add ports")
    public boolean getPortsFlag()
    {
        return portsFlag;
    }
    public void setPortsFlag(boolean portsFlag)
    {
    	boolean oldValue = this.portsFlag;
    	if (oldValue == false && portsFlag == true)
    	{
    		setRegimeType(REGIME_NORMAL);
    	}
        this.portsFlag = portsFlag;
        firePropertyChange("portsFlag", oldValue, portsFlag);
        firePropertyChange("*", null, null);
    }
}
