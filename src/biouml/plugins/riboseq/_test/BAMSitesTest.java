package biouml.plugins.riboseq._test;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.BAMTrack;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.LimitedSizeSitesCollection;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.bsa._test.AbstractTrackTest;
import ru.biosoft.bsa._test.BSATestUtils;

public class BAMSitesTest extends AbstractTrackTest
{
    private BAMTrack trackForTesting;

    private DataElementPath seq;
    private String seqPath;

    private Interval interval;
    private DataCollection<Site> sitesFromInterval;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        BSATestUtils.createRepository();
        DataElementPath path = DataElementPath.create( "databases/bam/small" );

        trackForTesting = path.getDataElement( BAMTrack.class );

        DataElementPath sequencesCollection = TrackUtils.getTrackSequencesPath( trackForTesting );

        seq = sequencesCollection.getChildPath( "1" );
        seqPath = seq.toString();

        interval = new Interval( 1000000, 1001000 );

        sitesFromInterval = trackForTesting.getSites( seqPath, interval.getFrom(), interval.getTo() );
    }

    public void testTrackSitesCount() throws Exception
    {
        DataCollection<Site> sites = trackForTesting.getSites( seqPath, 0, 50000 );
        int actualTrackSitesSize = sites.getSize();

        assertEquals( 0, actualTrackSitesSize );

        assertEquals( 45, trackForTesting.getUnalignedCount() );
        assertEquals( 1865, trackForTesting.getAlignedCount() );
        assertEquals( 45 + 1865, trackForTesting.getAllSites().getSize() );
    }

    public void testSitesFromInterval() throws Exception
    {
        assertEquals( 60, trackForTesting.countSites( seqPath, interval.getFrom(), interval.getTo() ) );
    }

    public void testLimitedSitesCount() throws Exception
    {
        LimitedSizeSitesCollection limitedSizeSites = (LimitedSizeSitesCollection)sitesFromInterval;

        assertTrue( 10 <= limitedSizeSites.getSizeLimited( 10 ) );
        assertEquals( 60, limitedSizeSites.getSizeLimited( 70 ) );
    }

    public void testSiteDescription() throws Exception
    {
        final String siteName = "DBBK90L1:7:5:11599:141382#0_2";
        Site checkingSite = sitesFromInterval.get( siteName );
        checkSpecificSite( seq, checkingSite );

        checkingSite = trackForTesting.getSite( seqPath, siteName, interval.getFrom(), interval.getTo() );
        checkSpecificSite( seq, checkingSite );
    }

    public void testSitesIntersectsIntervalFrom()
    {
        for( Site s : sitesFromInterval )
        {
            assertTrue( interval.intersects( s.getInterval() ) );
        }
    }

    private void checkSpecificSite(DataElementPath seq, Site site)
    {
        assertEquals( new Interval( 999964, 1000033 ), site.getInterval() );
        assertEquals( 1000033, site.getStart() );
        assertEquals( 70, site.getLength() );
        assertEquals( seq.getDataElement( AnnotatedSequence.class ).getSequence(), site.getOriginalSequence() );
        assertEquals( "31S70M", site.getProperties().getValue( "Cigar" ) );
        assertEquals( StrandType.STRAND_MINUS, site.getStrand() );
        assertTrue( (Boolean)site.getProperties().getValue( "Mapped to negative strand" ) );
    }
}
