package biouml.plugins.stochastic;

import java.util.Date;

import cern.jet.random.Exponential;
import cern.jet.random.Normal;
import cern.jet.random.Poisson;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;

public class Stochastic
{
    private Poisson poisson = null;
    private Uniform unif = null;
    private Exponential exponential = null;
    private Normal normal = null;

    public Stochastic(Date seed)
    {
        setSeed(seed);
    }

    int uniformCalls = 0;
    int exponentialCalls = 0;

    public void setSeed(Date seed)
    {
        MersenneTwister twister = new MersenneTwister(seed);
        poisson = new Poisson(1, twister);
        unif = new Uniform(twister);
        exponential = new Exponential(0, twister);
        normal = new Normal( 0, 1, twister );
    }
    public void setSeed(int seed)
    {
        MersenneTwister twister = new MersenneTwister( seed );
        poisson = new Poisson( 1, twister );
        unif = new Uniform( twister );
        exponential = new Exponential( 0, twister );
        normal = new Normal( 0, 1, twister );
    }

    public int getPoisson(double mean)
    {
        if( mean == 0 )
            return 0;
        return poisson.nextInt(mean);
    }

    public double getNormal(double mean, double var)
    {
        return normal.nextDouble( mean, var );
    }

    public double getUniform()
    {
        uniformCalls++;
        return unif.nextDouble();
    }

    public double getExponential(double mean)
    {
        exponentialCalls++;
        if( Double.isInfinite(mean) )
            return mean;

        return exponential.nextDouble(mean);
    }

}
