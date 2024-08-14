package biouml.plugins.riboseq.ingolia;

import one.util.streamex.IntStreamEx;

/**
 * Build single observation at given transcript position using precomputed riboseq profiles. 
 */
public class ObservationBuilder
{
    private int windowLeft = -40;
    private int windowRight = 40;
    
    private int windowOverhangs = -1;
    private int minWindowFootrpints = 50;
    private int minASiteFootprints = 10;
    
    public int getWindowLeft()
    {
        return windowLeft;
    }
    public int getWindowRight()
    {
        return windowRight;
    }
    public int getWindowOverhangs()
    {
        return windowOverhangs;
    }
    public void setWindowOverhangs(int windowOverhangs)
    {
        this.windowOverhangs = windowOverhangs;
    }

    public int getMinWindowFootrpints()
    {
        return minWindowFootrpints;
    }
    public void setMinWindowFootrpints(int minWindowFootrpints)
    {
        this.minWindowFootrpints = minWindowFootrpints;
    }
    
    public int getMinASiteFootprints()
    {
        return minASiteFootprints;
    }
    public void setMinASiteFootprints(int minASiteFootprints)
    {
        this.minASiteFootprints = minASiteFootprints;
    }
    
    public Observation buildObservation(Observation.Type type, int position, int[][] sampleProfiles)
    {
        if(position < 0 || position >= sampleProfiles[0].length)//Position not in transcript
            return null;
        
        int[] counts = computeCounts( position, sampleProfiles );
        if(counts == null)
            return null;
        
        int total = 0;
        for(int c : counts) total += c;
        if(total < minWindowFootrpints)
            return null;
        
        if( type == Observation.Type.YES || type == Observation.Type.UNKNOWN )
        {
            int aSiteFootprints = 0;
            for( int i = 0; i < sampleProfiles.length; i++ )
                for( int j = 0; j < 3; j++ )
                    aSiteFootprints += counts[i * (windowRight - windowLeft + 1) + 42 + j];
            if( aSiteFootprints < minASiteFootprints )
                return null;
        }
        
        double[] predictors = normalize(counts);
        
        Observation observation = new Observation( type, predictors );
        observation.setPosition( position );
        return observation;
    }

    private int[] computeCounts(int position, int[][] sampleProfiles)
    {
        int profileLength = sampleProfiles[0].length;
        int windowFrom = position + windowLeft;
        int windowTo = position + windowRight;
        if( windowOverhangs >= 0 && (windowFrom - windowOverhangs < 0 || windowTo + windowOverhangs >= profileLength) )
            return null;

        int[] predictors = new int[sampleProfiles.length * (windowTo - windowFrom + 1)];

        for( int sampleIdx = 0; sampleIdx < sampleProfiles.length; sampleIdx++ )
        {
            int[] profile = sampleProfiles[sampleIdx];
            for(int i = windowFrom; i <= windowTo; i++)
                if( i >= 0 && i < profile.length )
                    predictors[sampleIdx*(windowTo - windowFrom + 1) + i - windowFrom] = profile[i];
        }
        return predictors;
    }
    
    private double[] normalize(int[] values)
    {
        double sumSq = IntStreamEx.of( values ).mapToDouble( v -> v * (double)v ).sum();
        double coef = 1 / Math.sqrt( sumSq );
        return IntStreamEx.of( values ).mapToDouble( val -> coef * val ).toArray();
    }
}
