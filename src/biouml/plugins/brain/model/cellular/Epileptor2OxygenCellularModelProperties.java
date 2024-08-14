package biouml.plugins.brain.model.cellular;

import com.developmentontheedge.beans.annot.PropertyName;
import biouml.plugins.brain.model.BrainModelProperties;
import biouml.plugins.simulation.Options;

public class Epileptor2OxygenCellularModelProperties extends Options implements BrainModelProperties
{
	/*
	 * Epileptor-2 cellular model with oxygen dynamic
	 * 
	 * cell dynamic:
	 * K_o_rate = (K_bath-K_o)/tau_K-2*gamma*I_pump+dK_o*FR
	 * Na_i_rate = (Na_i_0-Na_i)/tau_Na-3*I_pump+dNa_i*FR
	 * K_o_rate = (K_bath-K_o)/tau_K-2*gamma*I_pump+dK_o*FR
	 * V_rate = (-V+u)/tau_M
	 * SR_rate = (1-SR)/tau_D-dSR*SR*FR
	 * 
	 * where:
	 * FR = FR_max*(2.0/(1.0+exp(-2.0*(V-V_th)/k_FR))-1.0), if V>V_th,
	 *      0, otherwise
	 * u = g_K_l*(V_K-V_K0) + G_syn*FR*(SR-0.5) + sigma*noise(1.0)
	 * V_K = 26.6*ln(K_o/130.0)
	 * I_pump = ro/((1.0+exp(3.5-K_o))*(1.0+exp((25.0-Na_i)/3.0)))
	 * 
	 * oxygen dynamic: 
	 * O2_o_rate = -alpha*lambda*(I_pump + I_gliapump) + eps_O*(O2_bath - O2_o)
	 * I_pump = ro/((1.0 + exp(3.5 - K_o))*(1.0 + exp((25.0 - Na_i)/3.0)))
	 * I_gliapump = 1.0/3.0*(ro/((1.0 + exp(3.5 - K_o))*(1.0 + exp((25.0 - Na_gi)/3.0))))
	 * ro = ro_max/(1.0 + exp((20.0 - O2_o)/3.0))
	 * 
	 * sources:
	 * Chizhov A.V., Zefirov A.V., Amakhin D.V., Smirnova E.Y., Zaitsev A.V. Minimal model of interictal and ictal discharges Epileptor-2. 
	 * Wei Y., Ullah G., Ingram J., Schiff SJ. Oxygen and seizure dynamics: II. Computational modeling.
	 */
	
	private double sf = 1.0; // synaptic factor associated with the probability of glutamate release.
	
	private double tauK = 100.0;
	private double tauNa = 20.0;
	private double tauM = 0.01; // s
	private double tauD = 2.0;
	private double dKo = 0.02;
	private double dNai = 0.03;
	private double dSR = 0.01;
	private double gamma = 10.0;
	private double roMax = 0.2;
	private double vTh = 25.0;
	private double FRMax = 100.0;
	private double kFR = 20.0;
	private double gKL = 0.5;
	private double gSyn = 5.0;
	private double sigma = 25.0;
	private double alpha = 5.3;
	//private double lambda = 1.0;
	private double lambda = 1.7;
	private double epsO = 0.17 * 0.1;
	
	/*
	 * A simple modeled neuron is used as an observer of the population activity.
	 * A representative neuron can be modeled with a quadratic integrate-and-fire model (QIF)
	 * or with an adaptive quadratic integrate-and-fire model (aQIF) where adaptation current is added (see supplementary).
	 */
	public static final String OBSERVER_QIF = "QIF";
	public static final String OBSERVER_AQIF = "aQIF";
	static final String[] availableObservers = new String[] {OBSERVER_QIF, OBSERVER_AQIF};
    private String neuronObserverType = OBSERVER_QIF;
    
    private double cU = 0.2; // nF
    private double gU = 0.4;
    //private double gL = 4.0;
    private double gL = 1.0;
    private double URest = -60.0;
    private double UTh = -40.0;
    private double UPeak = 25.0;
    private double UReset = -50.0;
	
	private boolean portsFlag = false; // if true than ports will be generated in equation diagram for further use in composite diagram

    public Epileptor2OxygenCellularModelProperties()
    {
    }
    
    @PropertyName("synaptic factor")
    public double getSf()
    {
        return sf;
    }
    public void setSf(double sf)
    {
        this.sf = sf;
    }
    
    @PropertyName("tauK")
    public double getTauK()
    {
        return tauK;
    }
    public void setTauK(double tauK)
    {
        this.tauK = tauK;
    }
    
    @PropertyName("tauNa")
    public double getTauNa()
    {
        return tauNa;
    }
    public void setTauNa(double tauNa)
    {
        this.tauNa = tauNa;
    }
    
    @PropertyName("tauM")
    public double getTauM()
    {
        return tauM;
    }
    public void setTauM(double tauM)
    {
        this.tauM = tauM;
    }
    
    @PropertyName("tauD")
    public double getTauD()
    {
        return tauD;
    }
    public void setTauD(double tauD)
    {
        this.tauD = tauD;
    }
    
    @PropertyName("dKo")
    public double getDKo()
    {
        return dKo;
    }
    public void setDKo(double dKo)
    {
        this.dKo = dKo;
    }
    
    @PropertyName("dNai")
    public double getDNai()
    {
        return dNai;
    }
    public void setDNai(double dNai)
    {
        this.dNai = dNai;
    }
    
    @PropertyName("dSR")
    public double getDSR()
    {
        return dSR;
    }
    public void setDSR(double dSR)
    {
        this.dSR = dSR;
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
    
    @PropertyName("roMax")
    public double getRoMax()
    {
        return roMax;
    }
    public void setRoMax(double roMax)
    {
        this.roMax = roMax;
    }
    
    @PropertyName("vTh")
    public double getVTh()
    {
        return vTh;
    }
    public void setVTh(double vTh)
    {
        this.vTh = vTh;
    }
    
    @PropertyName("FRMax")
    public double getFRMax()
    {
        return FRMax;
    }
    public void setFRMax(double FRMax)
    {
        this.FRMax = FRMax;
    }
    
    @PropertyName("kFR")
    public double getKFR()
    {
        return kFR;
    }
    public void setKFR(double kFR)
    {
        this.kFR = kFR;
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
    
    @PropertyName("gSyn")
    public double getGSyn()
    {
        return gSyn;
    }
    public void setGSyn(double gSyn)
    {
        this.gSyn = gSyn;
    }
    
    @PropertyName("sigma")
    public double getSigma()
    {
        return sigma;
    }
    public void setSigma(double sigma)
    {
        this.sigma = sigma;
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
    
    @PropertyName("epsO")
    public double getEpsO()
    {
        return epsO;
    }
    public void setEpsO(double epsO)
    {
        this.epsO = epsO;
    }
    
    @PropertyName("neuron observer type")
    public String getNeuronObserverType()
    {
        return neuronObserverType;
    }
    public void setNeuronObserverType(String neuronObserverType)
    {
    	switch (neuronObserverType)
    	{
    	    case OBSERVER_QIF:
    	    	setCU(0.2);
    	    	setGU(0.4);
    	    	//setGL(4.0);
    	    	setGL(1.0);
    	    	setURest(-60.0);
    	    	setUTh(-40.0);
    	    	setUPeak(25.0);
    	    	setUReset(-50.0);
    	    	break;
    	    case OBSERVER_AQIF:
    	    	setCU(1.0);
    	    	setGU(1.5);
    	    	//setGL(4.0);
    	    	setGL(1.0);
    	    	setURest(-60.0);
    	    	setUTh(-40.0);
    	    	setUPeak(25.0);
    	    	setUReset(-40.0);
    	    	break;
    	}
        Object oldValue = this.neuronObserverType;
        this.neuronObserverType = neuronObserverType;
        firePropertyChange("regimeType", oldValue, neuronObserverType);
        firePropertyChange("*", null, null);
    }
    
    @PropertyName("C_U")
    public double getCU()
    {
        return cU;
    }
    public void setCU(double cU)
    {
        this.cU = cU;
    }
    
    @PropertyName("g_U")
    public double getGU()
    {
        return gU;
    }
    public void setGU(double gU)
    {
        this.gU = gU;
    }
    
    @PropertyName("g_L")
    public double getGL()
    {
        return gL;
    }
    public void setGL(double gL)
    {
        this.gL = gL;
    }
    
    @PropertyName("U_rest")
    public double getURest()
    {
        return URest;
    }
    public void setURest(double URest)
    {
        this.URest = URest;
    }
    
    @PropertyName("U_th")
    public double getUTh()
    {
        return UTh;
    }
    public void setUTh(double UTh)
    {
        this.UTh = UTh;
    }
    
    @PropertyName("U_peak")
    public double getUPeak()
    {
        return UPeak;
    }
    public void setUPeak(double UPeak)
    {
        this.UPeak = UPeak;
    }
    
    @PropertyName("U_reset")
    public double getUReset()
    {
        return UReset;
    }
    public void setUReset(double UReset)
    {
        this.UReset = UReset;
    }
    
    @PropertyName("add ports")
    public boolean getPortsFlag()
    {
        return portsFlag;
    }
    public void setPortsFlag(boolean portsFlag)
    {
        this.portsFlag = portsFlag;
    }
}
