package biouml.plugins.riboseq.isoforms;

import org.apache.commons.math3.distribution.GammaDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import gnu.trove.list.array.TDoubleArrayList;

/**
 * Gibbs sampling for RSEM model.
 * TODO: weighting by MW and EEL
 */
public class Gibbs
{
    private static final double PSEUDO_COUNT = 1;
    
    private final int transcriptCount;//Number of transcripts not including pseudo transcript
    private final int unalignedCount;//number of unaligned reads (output from aligner)
    private final HitContainer hits;
    private final double[] hitProbs;//estimated by EM
    private final double[] noiseProbs;
    
    public Gibbs(HitContainer hits, int transcriptCount, int unalignedCount, double[] hitProbs, double[] noiseProbs)
    {
        this.hits = hits;
        this.transcriptCount = transcriptCount;
        this.unalignedCount = unalignedCount;
        this.hitProbs = hitProbs;
        this.noiseProbs = noiseProbs;
        validateParams();
    }
    
    private final JDKRandomGenerator rng = new JDKRandomGenerator();
    public void setSeed(long seed) { rng.setSeed( seed ); }
    
    private int burnIn = 200;
    public void setBurnIn(int burnIn) { this.burnIn = burnIn; }
    
    private int sampleGap = 1;
    public void setSampleGap(int sampleGap) { this.sampleGap = sampleGap; }
    
    private int thetaSamplesPerOneCounts = 50;
    public void setThetaSamplesPerOneCounts(int value) { this.thetaSamplesPerOneCounts = value; }
    
    private void validateParams()
    {
        if(transcriptCount <= 0)
            throw new IllegalArgumentException();
        if(unalignedCount < 0)
            throw new IllegalArgumentException();
        if( hitProbs.length != hits.getHitCount() )
            throw new IllegalArgumentException();
        if( noiseProbs.length != hits.getReadCount())
            throw new IllegalArgumentException();
    }
    
    public double[][] sampleTheta(int nSamples)
    {
        int nCountSamples = nSamples / thetaSamplesPerOneCounts;
        if(nSamples % thetaSamplesPerOneCounts != 0)
            nCountSamples++;
        int[][] counts = sampleCounts( nCountSamples );
        
        double[][] theta = new double[nSamples][transcriptCount + 1];
        
        for(int i = 0; i < nCountSamples; i++)
        {
            double[][] curTheta = sampleThetaGivenCounts( counts[i] );
            int n = thetaSamplesPerOneCounts;
            if(i == nCountSamples - 1 && nSamples % thetaSamplesPerOneCounts != 0)
                n = nSamples % thetaSamplesPerOneCounts;
            System.arraycopy( curTheta, 0, theta, i*thetaSamplesPerOneCounts, n );
        }
        
        return theta;
    }
    
    private int[][] sampleCounts(int nSamples)
    {
        int[][] result = new int[nSamples][transcriptCount + 1];
        
        int[] z = new int[hits.getReadCount()];//assignment of read i to transcript z[i], zero for pseudo transcript
        int[] counts = new int[transcriptCount + 1];
        
        initState( z, counts );
        for(int step = 0; step < burnIn; step++) {
            oneStep( counts, z );
        }
        
        for(int sample = 0; sample < nSamples; sample++)
        {
            if(sample != 0)
            {
                for(int skip = 0; skip < sampleGap - 1; skip++)
                    oneStep( counts, z );
            }
            oneStep( counts, z );
            System.arraycopy( counts, 0, result[sample], 0, counts.length );
        }
        
        return result;
    }
    
    //Sample z from hitProbs and compute counts
    private void initState(int[] z, int[] counts)
    {
        counts[0] = unalignedCount;
        TDoubleArrayList p = new TDoubleArrayList();
        for(int read = 0; read < hits.getReadCount(); read++)
        {
            int from = hits.getReadBucketFrom( read );
            int to = hits.getReadBucketTo( read );
            p.reset();
            p.add( noiseProbs[read] );
            for(int hitId = from; hitId < to; hitId++)
                p.add( hitProbs[hitId] );
            int choosenIndex = sampleMultinom( p );
            if( choosenIndex == 0 )
                z[read] = 0;
            else {
                z[read] = 1 + hits.getTranscriptAt( choosenIndex - 1 + from );
            }
            ++counts[z[read]];
        }
    }

    private void oneStep(int[] counts, int[] z)
    {
        TDoubleArrayList p = new TDoubleArrayList();
        for(int read = 0; read < hits.getReadCount(); read++)
        {
            --counts[z[read]];
            int from = hits.getReadBucketFrom( read );
            int to = hits.getReadBucketTo( read );
            p.reset();
            p.add(  (counts[0] + PSEUDO_COUNT) * noiseProbs[read] );
            for(int hit = from; hit < to; hit++)
            {
                double prob = hitProbs[hit];
                int transcript = hits.getTranscriptAt(hit);
                p.add( (counts[1+transcript] + PSEUDO_COUNT) * prob );
            }
            int choosenIndex = sampleMultinom( p );
            if( choosenIndex == 0 )
                z[read] = 0;
            else
                z[read] = 1 + hits.getTranscriptAt( choosenIndex - 1 + from );
            ++counts[z[read]];
        }
    }
    
    //Given counts, theta has Dirichlet distribution
    private double[][] sampleThetaGivenCounts(int[] counts)
    {
        double[][] theta = new double[thetaSamplesPerOneCounts][transcriptCount + 1];
        GammaDistribution[] gammaDist = new GammaDistribution[counts.length];
        for(int i = 0; i < counts.length; i++)
            gammaDist[i] = new GammaDistribution( rng,  counts[i] + 1.0, 1.0 );
        for(int sample = 0; sample < thetaSamplesPerOneCounts; sample++)
        {
            double sum = 0;
            for(int i = 0; i < counts.length; i++)
            {
                double t = gammaDist[i].sample();
                theta[sample][i] = t;
                sum += t;
            }
            for(int i = 0; i < counts.length; i++)
                theta[sample][i] /= sum;
        }
        return theta;
    }

    private double[] calcTheta(int[] counts)
    {
        double[] theta = new double[transcriptCount + 1];
        double totc = (transcriptCount + 1)*PSEUDO_COUNT + hits.getReadCount() + unalignedCount;
        for(int i = 0; i < transcriptCount + 1; i++)
            theta[i] = (counts[i] < 0 ? 0 : (counts[i] + PSEUDO_COUNT) ) / totc;
        return theta;
    }
    
    private int sampleMultinom(TDoubleArrayList ps)
    {
        double t = rng.nextDouble() * ps.sum();
        double cumSum = 0;
        for(int i = 0; i < ps.size(); i++)
        {
            cumSum += ps.getQuick( i );
            if(cumSum >= t)
                return i;
        }
        throw new AssertionError();
    }
}
