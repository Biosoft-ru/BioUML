package biouml.plugins.riboseq.ingolia._test;

import biouml.plugins.riboseq.ingolia.PredictedStartSite;
import biouml.plugins.riboseq.ingolia.StartSiteReadsCounter;
import junit.framework.TestCase;
import ru.biosoft.bsa.Interval;

public class TestStartSiteReadsCounter extends TestCase
{
    private static final int transcriptLength = 10;
    private static final PredictedStartSite startSite = createStartSite();

    public void testCountReadsEmptySampleProfiles() throws Exception
    {
        final int[][] sampleProfiles = createEmptySampleProfiles();
        final StartSiteReadsCounter readsCounter = new StartSiteReadsCounter( startSite, sampleProfiles );

        final int readsNumber = readsCounter.countReads();

        assertEquals( 0, readsNumber );
    }

    public void testCountReadsAllOneSampleProfiles() throws Exception
    {
        final int[][] sampleProfiles = createAllOneSampleProfiles();
        final StartSiteReadsCounter readsCounter = new StartSiteReadsCounter( startSite, sampleProfiles );

        final int readsNumber = readsCounter.countReads();

        assertEquals( 5, readsNumber );
    }

    public void testCountReadsTwoSampleProfiles() throws Exception
    {
        final int[][] sampleProfiles = createTwoSampleProfiles();
        final StartSiteReadsCounter readsCounter = new StartSiteReadsCounter( startSite, sampleProfiles );

        final int readsNumber = readsCounter.countReads();

        assertEquals( 25, readsNumber );
    }

    private static PredictedStartSite createStartSite()
    {
        final PredictedStartSite startSite = new PredictedStartSite();
        final Interval peak = new Interval( 3, 5 );
        startSite.setPeak( peak );

        return startSite;
    }

    private int[][] createEmptySampleProfiles()
    {
        return new int[1][transcriptLength];
    }

    private int[][] createAllOneSampleProfiles()
    {
        final int[][] sampleProfile = new int[1][];
        sampleProfile[0] = new int[transcriptLength];
        for( int i = 0; i < sampleProfile[0].length; i++ )
        {
            sampleProfile[0][i] = 1;
        }

        return sampleProfile;
    }

    private int[][] createTwoSampleProfiles()
    {
        final int[][] twoSampleProfiles = new int[2][];
        twoSampleProfiles[0] = new int[transcriptLength];
        twoSampleProfiles[1] = new int[transcriptLength];
        for( int i = 0; i < twoSampleProfiles[0].length; i++ )
        {
            twoSampleProfiles[0][i] = 1;
            twoSampleProfiles[1][i] = 4;
        }

        return twoSampleProfiles;
    }
}
