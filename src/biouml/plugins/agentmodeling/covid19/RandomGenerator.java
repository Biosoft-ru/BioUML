package biouml.plugins.agentmodeling.covid19;

import java.util.Random;

import org.apache.commons.math3.distribution.LogNormalDistribution;

public class RandomGenerator
{
    private double meanContacts = 1.0;
    private double varianceContacts = 0.7;
    
//    cern.jet.random.sampling.RandomSampler sampler = new RandomSampler();
    private LogNormalDistribution logNormal;
    private Random r; //TODO: replace with generator from library
    
    public RandomGenerator()
    {
        logNormal = new LogNormalDistribution(calculateMu(meanContacts, varianceContacts), calculateSigma(meanContacts, varianceContacts));;
//        logNormal.reseedRandomGenerator( 1 );
        r = new Random();
    }
    
    public RandomGenerator(long seed)
    {        
        logNormal = new LogNormalDistribution(calculateMu(meanContacts, varianceContacts), calculateSigma(meanContacts, varianceContacts));
        logNormal.reseedRandomGenerator( seed );
        r = new Random(seed);        
    }
    
    public double sampleLogNormal()
    {
        return logNormal.sample();
    }
    
    public double sampleLogNormal2()
    {
        return Math.exp( ( mean + ( sigma * r.nextGaussian() ))); 
    }
    
    public double sampleUniform()
    {
        return r.nextDouble();
    }
    
    public int sampleInteger(int max)
    {
        return r.nextInt(max);
    }
    
    public double calculateMu(double mean, double variance)
    {
        return Math.log( mean*mean / Math.sqrt(mean*mean + variance*variance) );     
    }
    
    public double calculateSigma(double mean, double variance)
    {
        return Math.sqrt( Math.log( variance / (mean*mean) + 1 ));     
    }    
    
    private double mean = Math.log( 1.0 ) - ( 0.5 * Math.pow( 0.7, 2 ) );;
    private double sigma = Math.log( 1.0 + ( 0.7 / Math.pow( 1.0, 2 ) ) );
}
