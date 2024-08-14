package biouml.plugins.brain.sde;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

import cern.jet.random.Exponential;
import cern.jet.random.Normal;
import cern.jet.random.Poisson;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;

import biouml.plugins.simulation.java.JavaLargeModel;

public abstract class SdeModel extends JavaLargeModel 
{
    private static final Logger log = Logger.getLogger(SdeModel.class.getName());
    
    private int seed = 0;
    
    private Normal normal = null;
    private Uniform unif = null;
    private Poisson poisson = null;
    private Exponential exponential = null;
    
    private final String SDE_TYPE_NORMAL = "normal";
    private final String SDE_TYPE_UNIFORM = "uniform";
    private final String SDE_TYPE_POISSON = "poisson";
    private final String SDE_TYPE_EXPONENTIAL = "exponential";
    
    protected double H_NOISE_INTERNAL = 0.001;
    protected double TIME_START_NOISE_INTERNAL = 0.0;
    protected double TIME_FINAL_NOISE_INTERNAL = 100.0;
    protected boolean PRECALCULATED_NOISE_FLAG_INTERNAL = false;
    
    protected List<Double> PRECALCULATED_NOISE_INTERNAL = new ArrayList<Double>();
    
    /**Mapping between stochastic variable index and its value*/
    protected Map<Integer, Double> stochasticValuesPreviousMapping = new HashMap<>();
    protected Map<Integer, Double> stochasticValuesCurrentMapping = new HashMap<>();
    
    protected double stochasticValuesPreviousTime = 0;
    protected double stochasticValuesCurrentTime = 0;
    
    public void setSeed(Date seed)
    {
        int i = (int)(new Date().getTime() / 1000);
        this.seed = i;
        MersenneTwister twister = new MersenneTwister(this.seed);
        poisson = new Poisson(1, twister);
        unif = new Uniform(twister);
        exponential = new Exponential(0, twister);
        normal = new Normal(0, 1, twister);
    }
    
    public void setSeed(int seed)
    {
        this.seed = seed;
        MersenneTwister twister = new MersenneTwister(this.seed);
        poisson = new Poisson(1, twister);
        unif = new Uniform(twister);
        exponential = new Exponential(0, twister);
        normal = new Normal(0, 1, twister);
    }
    
    public int getSeed()
    {
        return seed;
    }
    
    private double interpolate(double t1, double t2, double x1, double x2, double t)
    {
        return ((x2 - x1) / (t2 - t1)) * (t - t1) + x1;
    }
    
    public void setSolverStep(double h) 
    {
        this.H_NOISE_INTERNAL = h;
    }
    
    public void setStartTime(double tStart) 
    {
        this.TIME_START_NOISE_INTERNAL = tStart;
    }
    
    public void setFinalTime(double tFinal) 
    {
        this.TIME_FINAL_NOISE_INTERNAL = tFinal;
    }
    
    public boolean getPrecalculatedNoiseFlag()
    {
        return PRECALCULATED_NOISE_FLAG_INTERNAL;
    }
    
    public void setPrecalculatedNoiseFlag(boolean precalculatedNoiseFlag)
    {
        this.PRECALCULATED_NOISE_FLAG_INTERNAL = precalculatedNoiseFlag;
    }
    
    // precalculates normally distributed noise in grid nodes with 0 mean and 1 variance
    public void precalculateNoise()
    {
        PRECALCULATED_NOISE_INTERNAL = new ArrayList<Double>();
        
        int n = (int)((TIME_FINAL_NOISE_INTERNAL - TIME_START_NOISE_INTERNAL) / H_NOISE_INTERNAL) + 1;
            
        Random r = new Random();
            
        for (int i = 0; i < n; i++)
        {
            PRECALCULATED_NOISE_INTERNAL.add(r.nextGaussian());
        }
            
        setPrecalculatedNoiseFlag(true);
    }
    
    public double[] getCurrentStochastic()
    {
        return null;
    }
    
    public double[] dy_dt_stochastic(double time, double[] x) throws Exception
    {
        // default calculation should be overridden in template if model contains stochastic
        int rateEquationNumber = getY().length;
        double[] dydt_stochastic = new double[rateEquationNumber];
        for (int i = 0; i < rateEquationNumber; i++)
        {
            dydt_stochastic[i] = 0.0;
        }
        return dydt_stochastic;
    }
    
    public double[] dy_dt_deterministic(double time, double[] x) throws Exception
    {
        // default calculation should be overridden in template if model contains stochastic
        return super.dy_dt(time, x);
    }
    
     /**
     * 
     * Generates value of a random variable from the distribution 
     * specified by the given type and parameters.
     * 
     * @param distributionType Type of the distribution (normal/uniform/poisson/exponential).
     * @param loc Location of the distribution (mean for normal for example).
     * @param loc Scale of the distribution (std for normal for example).
     */
    public double stochastic(String distributionType, double loc, double scale)
    {
        switch (distributionType)
        {
            case SDE_TYPE_NORMAL:
                return normal(loc, scale);
            case SDE_TYPE_UNIFORM:
                return uniform(loc, scale);
            case SDE_TYPE_POISSON:
                return poisson(loc);
            case SDE_TYPE_EXPONENTIAL:
                return exponential(loc);
            default:
                log.warning("Incorrect distribution type: " + distributionType + ", returning 0 instead.");
                return 0;
        }
    }
    
    public double stochastic(int auxVarIndex, String distributionType, double loc, double scale, double time)
    {
    	if (Math.abs(time - stochasticValuesCurrentTime) < 1E-8)
    	{
    	    // visit a new grid node for the first time
    		if (!stochasticValuesCurrentMapping.containsKey(auxVarIndex))
    		{
    			stochasticValuesCurrentMapping.put(auxVarIndex, stochastic(distributionType, loc, scale));
    		}
    		return stochasticValuesCurrentMapping.get(auxVarIndex);
    	}
    	else if (Math.abs(time - stochasticValuesPreviousTime) < 1E-8)
    	{
    	    // integrationStep does not calculate parameters at current time step
    	    if (!stochasticValuesPreviousMapping.containsKey(auxVarIndex))
            {
    	        stochasticValuesPreviousMapping.put(auxVarIndex, stochastic(distributionType, loc, scale));
            }
    		return stochasticValuesPreviousMapping.get(auxVarIndex); // return value from previous step
    	}
    	else
    	{
    	    log.warning("Error in stochastic(" + distributionType + ", " + loc + ", " + scale + ") calculation, returning 0 instead.");
    	    return 0;
    	}
    }
    
    public double noise(double variance)
    {
        return getNormal(0, 1) * Math.sqrt(variance) / Math.sqrt(H_NOISE_INTERNAL); // EM solver step: xNew[i] = xOld[i] + (dydt[i] + noise) * h;
    }
    
    public double noise(double variance, double t)
    {
        if (!getPrecalculatedNoiseFlag())
        {
            return noise(variance);
        }
        
        int i = (int)Math.ceil((t - TIME_START_NOISE_INTERNAL) / H_NOISE_INTERNAL);
        
        // first grid node
        if (i == 0)
        {
            return noise(variance);
        }
        
        double t1 = TIME_START_NOISE_INTERNAL + (i - 1) * H_NOISE_INTERNAL;
        double x1 = PRECALCULATED_NOISE_INTERNAL.get(i - 1) * Math.sqrt(variance * H_NOISE_INTERNAL) / H_NOISE_INTERNAL;
            
        double t2 = TIME_START_NOISE_INTERNAL + (i) * H_NOISE_INTERNAL;
        double x2 = PRECALCULATED_NOISE_INTERNAL.get(i) * Math.sqrt(variance * H_NOISE_INTERNAL) / H_NOISE_INTERNAL;
            
        return interpolate(t1, t2, x1, x2, t);
    }
    
    /**
     * 
     * Generates random number from the Normal distribution 
     * specified by the parameters mean and std.
     * 
     * @param mean Mean of the Normal distribution.
     * @param std Standard deviation of the Normal distribution.
     */
    public double normal(double mean, double std) 
    {
        return getNormal(mean, std);
    }
    
    /**
     * 
     * Generates random number from the Uniform distribution 
     * specified by the parameters a and b.
     * 
     * @param a Lower boundary of the Uniform distribution.
     * @param b Lower boundary of the Uniform distribution.
     */
    public double uniform(double a, double b) 
    {
        return a + getUniform() * (b - a);
    }
    
    /**
     * 
     * Generates random number from the Poisson distribution 
     * specified by the rate parameter mean.
     * Poisson distribution replaced with Normal when mean is too large.
     * 
     * @param mean The rate parameter of the Poisson distribution.
     */
    public double poisson(double mean) 
    {
        double k = 0;
        try
        {
            if (mean > 1E8) 
            {
                k = getNormal(mean, Math.sqrt(mean));
            }
            else 
            {
                k = getPoisson(mean);
            }
        }
        catch (Exception ex) 
        {
            log.info("Can't calculate possion random variable, mean is too large: " + mean);
        }
        return k;
    }
    
    /**
     * 
     * Generates random number from the Exponential distribution 
     * specified by the rate parameter mean.
     * 
     * @param mean The rate parameter of the Poisson distribution.
     */
    public double exponential(double mean) 
    {
        return getExponential(mean);
    }

    public double getNormal(double mean, double std)
    {
        return normal.nextDouble(mean, std);
    }

    public double getUniform()
    {
        return unif.nextDouble();
    }
    
    public int getPoisson(double mean)
    {
        if (mean == 0) 
        {
            return 0;
        }
        return poisson.nextInt(mean);
    }

    public double getExponential(double mean)
    {
        if (Double.isInfinite(mean)) 
        {
            return mean;
        }
        return exponential.nextDouble(mean);
    }
    
//    public double noise(int index, String distributionType, double loc, double scale)
//    {
//        if( simulationResultTimes.size() == 0 || t < simulationResultTimes.get(0) )
//            return getPrehistory(t, index);
//
//        int i = firstPart(t);
//
//        if( i == 0 )
//            return simulationResultHistory.get(0)[index];
//
//
//        double x1;
//        double x2;
//        double t1;
//        double t2;
//
//        if( i == simulationResultHistory.size() )
//        {
//            //get current values
//            x1 = this.getCurrentHistory()[index];
//            t1 = this.time;
//        }
//        else
//        {
//            x1 = simulationResultHistory.get(i)[index];
//            t1 = simulationResultTimes.get(i).doubleValue();
//        }
//
//        x2 = simulationResultHistory.get(i - 1)[index];
//        t2 = simulationResultTimes.get(i - 1).doubleValue();
//        return interpolate(t1, t2, x1, x2, t);
//    }
    
    double[][] transitionMatrix;
    
    public void calculateTransitionMatrix(double ca, double kon)
    {
        double koff = 3.0; // Calcium dissociation rate from vesicle; unit: per ms; Ref: Bollman et al (2000)
        double gamma = 30.0, delta = 8.0; // Isomerization constants; unit: per ms; Ref: Bollman et al (2000)
        
        double a01 = 5.0 * kon * ca * H_NOISE_INTERNAL;
        double a10 = koff * H_NOISE_INTERNAL;
        double a12 = 4.0 * kon * ca * H_NOISE_INTERNAL;
        double a21 = 2.0 * koff * H_NOISE_INTERNAL;
        double a23 = 3.0 * kon * ca * H_NOISE_INTERNAL;
        double a32 = 3.0 * koff * H_NOISE_INTERNAL;
        double a34 = 2.0 * kon * ca * H_NOISE_INTERNAL;
        double a43 = 4.0 * koff * H_NOISE_INTERNAL;
        double a45 = kon * ca * H_NOISE_INTERNAL;
        double a54 = 5.0 * koff * H_NOISE_INTERNAL;
        double a56 = gamma * H_NOISE_INTERNAL;
        double a65 = delta * H_NOISE_INTERNAL;
        
        double D0 = 1.0 - a01;
        double D1 = 1.0 - a10 - a12;
        double D2 = 1.0 - a21 - a23;
        double D3 = 1.0 - a32 - a34;
        double D4 = 1.0 - a43 - a45;
        double D5 = 1.0 - a54 - a56;
        double D6 = 1.0 - a65;
        
        double newTransitionMatrix[][] = { 
                {D0, a01, 0.0, 0.0, 0.0, 0.0, 0.0},
                {a10, D1, a12, 0.0, 0.0, 0.0, 0.0},
                {0.0, a21, D2, a23, 0.0, 0.0, 0.0},
                {0.0, 0.0, a32, D3, a34, 0.0, 0.0},
                {0.0, 0.0, 0.0, a43, D4, a45, 0.0},
                {0.0, 0.0, 0.0, 0.0, a54, D5, a56},
                {0.0, 0.0, 0.0, 0.0, 0.0, a65, D6}
        };
        
        transitionMatrix = newTransitionMatrix;
    }
    
    public double markov(double state, double ca, double kon)
    {
        calculateTransitionMatrix(ca, kon);
        
        int curState = (int)state;
        double u = Math.random(); // URN
        int i  = 0; // Initial value of i
        double s = transitionMatrix[curState][0]; 
        
        while ((u > s) && (i < transitionMatrix.length - 1))
        {
            ++i;
            s += transitionMatrix[curState][i]; 
        }
                
        int newState = i;
        return newState;
    }
    
    /**
     * 
     * Calculates the Heaviside step function 
     * which is a discontinuous function 
     * that returns 0 for x < 0, 1/2 for x = 0, and 1 for x > 0.
     * 
     * @param x The number to calculate the Heaviside function value of.
     */
    public double heaviside(double x)
    {
        if (x < 0) 
        {
            return 0.0;
        }
        else if (x == 0)
        {
            return 0.5;
        }
        else 
        {
            return 1.0;
        }
    }
}
