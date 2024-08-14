package ru.biosoft.bsa;

public class PValueCutoff
{
    private double[] scores;
    private double[] pvalues;
    
    public PValueCutoff(double[] scores, double[] pvalues)
    {
        if(scores.length != pvalues.length)
            throw new IllegalArgumentException();
        if(scores.length < 1)
            throw new IllegalArgumentException();
        
        this.scores = scores;
        this.pvalues = pvalues;
        
        checkOrder();
    }
    
    private void checkOrder()
    {
        for(int i = 1; i < scores.length; i++)
        {
            if(scores[i] > scores[i-1])
                throw new IllegalArgumentException();
            if(pvalues[i] < pvalues[i-1])
                throw new IllegalArgumentException();
        }
    }

    public double getCutoff(double pvalue) {
        for(int i = 0; i < pvalues.length; i++)
            if(pvalues[i] >= pvalue)
                return scores[i];
        return -Double.MAX_VALUE;
    }
    
    //the actual p-value for this cutoff is <= then returned value
    public double getPvalue(double cutoff) {
        for(int i = 0; i < scores.length; i++)
            if(scores[i] <= cutoff)
                return pvalues[i];
        return 1;
    }
    
    public double[] getCutoffs()
    {
        return scores;
    }
    
    public double[] getPvalues()
    {
        return pvalues;
    }
}
