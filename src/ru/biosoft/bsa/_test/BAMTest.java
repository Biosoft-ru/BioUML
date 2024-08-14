package ru.biosoft.bsa._test;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.BAMTrack;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.LimitedSizeSitesCollection;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.bsa.access.SequencesDatabaseInfo;

/**
 * @author lan
 *
 */
public class BAMTest extends AbstractTrackTest
{
    public void testBAMTrack() throws Exception
    {
        BSATestUtils.createRepository();
        DataElementPath path = DataElementPath.create("databases/bam/small");
        BAMTrack track = path.getDataElement(BAMTrack.class);
        DataElementPath sequencesCollection = TrackUtils.getTrackSequencesPath(track);
        assertNotNull("Sequences collection exists", sequencesCollection);
        
        // Check sequences collection changing
        track.getGenomeSelector().setDbSelector(SequencesDatabaseInfo.NULL_SEQUENCES);
        assertNull(track.getInfo().getProperty(Track.SEQUENCES_COLLECTION_PROPERTY));
        track.getGenomeSelector().setDbSelector( SequencesDatabaseInfo.CUSTOM_SEQUENCES );
        track.getGenomeSelector().setSequenceCollectionPath(sequencesCollection);
        assertEquals(sequencesCollection.toString(), track.getInfo().getProperty(Track.SEQUENCES_COLLECTION_PROPERTY));
        
        DataElementPath seq = sequencesCollection.getChildPath("1");
        assertEquals(0, track.getSites(seq.toString(), 0, 50000).getSize());
        
        assertEquals(45, track.getUnalignedCount());
        assertEquals(1865, track.getAlignedCount());
        assertEquals(45+1865, track.getAllSites().getSize());
        Interval interval = new Interval(1000000, 1001000);
        assertEquals(60, track.countSites(seq.toString(), interval.getFrom(), interval.getTo()));
        LimitedSizeSitesCollection sites = (LimitedSizeSitesCollection)track.getSites(seq.toString(), interval.getFrom(), interval.getTo());
        assertTrue(sites.getSizeLimited(10)>=10);
        assertEquals(60, sites.getSizeLimited(70));
        checkSite(seq, sites.get("DBBK90L1:7:5:11599:141382#0_2"));
        checkSite(seq, track.getSite(seq.toString(), "DBBK90L1:7:5:11599:141382#0_2", interval.getFrom(), interval.getTo()));
        for(Site s: sites)
        {
            assertTrue(interval.intersects(s.getInterval()));
        }
        
        checkViewBuilder(track, seq.getDataElement(AnnotatedSequence.class), new Interval(1000000, 1000100));
    }

    /**
     * @param seq
     * @param site
     */
    private void checkSite(DataElementPath seq, Site site)
    {
        assertEquals(new Interval(999964, 1000033), site.getInterval());
        assertEquals(1000033, site.getStart());
        assertEquals(70, site.getLength());
        assertEquals(seq.getDataElement(AnnotatedSequence.class).getSequence(), site.getOriginalSequence());
        assertEquals("31S70M", site.getProperties().getValue("Cigar"));
        assertEquals(StrandType.STRAND_MINUS, site.getStrand());
        assertTrue((Boolean)site.getProperties().getValue("Mapped to negative strand"));
    }
}
