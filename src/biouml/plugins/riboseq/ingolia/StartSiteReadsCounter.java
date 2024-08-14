package biouml.plugins.riboseq.ingolia;

import ru.biosoft.bsa.Interval;

public class StartSiteReadsCounter
{
    private final PredictedStartSite predictedStartSite;
    private final int[][] sampleProfiles;

    public StartSiteReadsCounter(PredictedStartSite startSite, int[][] sampleProfiles)
    {
        this.predictedStartSite = startSite;
        this.sampleProfiles = sampleProfiles;
    }

    public int countReads(){
        int counter = 0;

        final Interval peak = predictedStartSite.getPeak();
        for( int[] profile : sampleProfiles )
        {
            for( int pos = peak.getFrom() + 2; pos <= peak.getTo() + 4; pos++ )
            {
                if(pos >= 0 && pos < profile.length)
                    counter += profile[pos];
            }
        }

        return counter;
    }
}
