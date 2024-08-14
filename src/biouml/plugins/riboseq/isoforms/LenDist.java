package biouml.plugins.riboseq.isoforms;

public class LenDist
{
    int min,max;
    private double[] pdf;
    private double[] cdf;
    
    public LenDist(int min, int max)
    {
        if(max < min)
            throw new IllegalArgumentException();
        this.min = min;
        this.max = max;
        pdf = new double[max - min + 1];
        cdf = new double[max - min + 1];
    }
    
    public double getProb(int len)
    {
        checkBounds( len );
        return pdf[len - min];
    }
    
    //Actually this should depends on distance from read 5' end to transcript 3' end and not refLen?
    public double getAdjustedProb(int len, int refLen)
    {
        checkBounds( len );
        if(refLen < min)
            return 0;
        double denom = cdf[Math.min( refLen, max ) - min];
        return getProb( len ) / denom;
    }

    private void checkBounds(int len) throws IllegalArgumentException
    {
        if(len < min || len > max)
            throw new IllegalArgumentException();
    }
    
    public void update(int len)
    {
        checkBounds( len );
        pdf[len - min]++;
    }
    
    public void finish()
    {
        double sum = 0;
        for(int i = 0; i < pdf.length; i++)
            sum += pdf[i];
        for(int i = 0; i < pdf.length; i++)
            pdf[i] /= sum;

        sum = 0;
        for(int i = 0; i < pdf.length; i++)
        {
            sum += pdf[i];
            cdf[i] = sum;
        }
    }
    
    public double calcEffectiveLength(int refLen)
    {
        double res = Math.max( 0, refLen - max );
        for(int i = min; i <= max; i++)
            res += cdf[i - min];
        return res;
    }
}
