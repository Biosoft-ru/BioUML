package biouml.plugins.riboseq.isoforms;

import java.util.Arrays;

public class NoiseProfile
{
    private int[] c;
    private double[] p;
    
    public NoiseProfile()
    {
        c = new int[Const.ALPHABET_SIZE];
        p = new double[Const.ALPHABET_SIZE];
    }

    public void updateC(byte[] seq)
    {
        for(byte s : seq)
            c[s]++;
    }

    public void calcInitParams()
    {
        double sum = 0;
        for(int i = 0; i < c.length; i++)
            sum += 1 + c[i];
        for(int i = 0; i < c.length; i++)
            p[i] = (c[i] + 1)/sum;
    }

    public void init()
    {
        Arrays.fill( p, 0 );
    }

    public void finish()
    {
        double sum = 0.0;
        for( int i = 0; i < p.length; i++ )
            sum += ( p[i] + c[i] );
        if( sum <= Const.EPSILON )
            return;
        for( int i = 0; i < p.length; i++ )
            p[i] = ( p[i] + c[i] ) / sum;
    }

    public void update(byte[] seq, double frac)
    {
        for(byte s : seq)
            p[s] += frac;
    }

    public void collect(NoiseProfile noiseProfile)
    {
        for(int i = 0; i < p.length; i++)
            p[i] += noiseProfile.p[i];
    }

    public double getProb(byte[] seq)
    {
        double prob = 1;
        for(byte s : seq)
            prob *= p[s];
        return prob;
    }
    
}
