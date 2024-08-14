package ru.biosoft.bsa._test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.LinearSequence;
import ru.biosoft.bsa.MapAsVector;
import ru.biosoft.bsa.MergedTrack;
import ru.biosoft.bsa.Nucleotide5LetterAlphabet;
import ru.biosoft.bsa.ResizedTrack;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.TrackImpl;
import ru.biosoft.bsa.WritableTrack;
import ru.biosoft.bsa.analysis.ProcessTrackParameters;
import ru.biosoft.bsa.track.WholeSequenceTrack;

/**
 * @author lan
 *
 */
public class TrackTest extends AbstractTrackTest
{
    private WritableTrack track;
    private String seqString;

    public void testTrackImpl() throws Exception
    {
        assertEquals(5, track.countSites("test/1", 0, 100));
        assertEquals(5, track.getAllSites().getSize());
        assertEquals(3, track.countSites("test/1", 1, 15));
        assertEquals(0, track.countSites("test/2", 0, 100));
        assertEquals(12, track.getSites("test/1", 1, 15).get("2").getTo());
        assertEquals(12, track.getSite("test/1", "2", 1, 15).getTo());
        checkViewBuilder(track, (AnnotatedSequence)CollectionFactory.getDataElement("test/1"), new Interval(0,30));
    }
    
    public void testWholeSequenceTrack() throws Exception
    {
        WholeSequenceTrack track = new WholeSequenceTrack("test", null);
        assertEquals(1, track.countSites("test/1", 1, 10));
        assertEquals(1, track.getSites("test/1", 0, 20).getSize());
        Site site = track.getSites("test/1", 0, 20).iterator().next();
        assertEquals(1, site.getFrom());
        assertEquals(seqString.length(), site.getLength());
        checkViewBuilder(track, (AnnotatedSequence)CollectionFactory.getDataElement("test/1"), new Interval(0,30));
    }
    
    public void testMergedTrack() throws Exception
    {
        MergedTrack track = new MergedTrack(this.track);
        assertEquals(2, track.countSites("test/1", 0, 50));
        DataCollection<Site> sites = track.getSites("test/1", 0, 50);
        Set<Interval> sitesSet = new HashSet<>();
        for(Site site: sites)
        {
            sitesSet.add(site.getInterval());
        }
        Set<Interval> expectedSet = new HashSet<>(Arrays.asList(new Interval(1,15), new Interval(18,30)));
        assertEquals(expectedSet, sitesSet);
        checkViewBuilder(track, (AnnotatedSequence)CollectionFactory.getDataElement("test/1"), new Interval(0,30));
    }
    
    public void testResizedTrack() throws Exception
    {
        ResizedTrack track = new ResizedTrack(this.track, ProcessTrackParameters.NO_SHRINK, 10, 10);
        assertEquals(5, track.countSites("test/1", 0, 100));
        assertEquals(5, track.getAllSites().getSize());
        assertEquals(3, track.countSites("test/1", 1, 15));
        assertEquals(0, track.countSites("test/2", 0, 100));
        assertEquals("Test resize of + site", 22, track.getSites("test/1", 1, 15).get("2").getTo());
        assertEquals("Test resize of - site", 25, track.getSites("test/1", 1, 15).get("3").getTo());
        checkViewBuilder(track, (AnnotatedSequence)CollectionFactory.getDataElement("test/1"), new Interval(0,30));
        
        track = new ResizedTrack(this.track, ProcessTrackParameters.SHRINK_TO_START, 10, 10);
        assertEquals(new Interval(-7, 12), track.getAllSites().get( "2" ).getInterval());

        track = new ResizedTrack(this.track, ProcessTrackParameters.SHRINK_TO_CENTER, 10, 10);
        assertEquals(new Interval(1, 20), track.getAllSites().get( "3" ).getInterval());

        track = new ResizedTrack(this.track, ProcessTrackParameters.SHRINK_TO_END, 10, 10);
        assertEquals(new Interval(-5, 14), track.getAllSites().get( "3" ).getInterval());
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        VectorDataCollection<AnnotatedSequence> vdc = new VectorDataCollection<>("test");
        CollectionFactory.registerRoot(vdc);
        seqString = "acgtacgtacgtacgatcgatcgatcgatgctagctagcatcgatcgatcgatcgatcgatc";
        Sequence seq = new LinearSequence("1", seqString, Nucleotide5LetterAlphabet.getInstance());
        vdc.put(new MapAsVector("1", vdc, seq, null));
        track = new TrackImpl("test", null);
        track.addSite(new SiteImpl(null, "1", 1, 11, StrandType.STRAND_PLUS, seq));
        track.addSite(new SiteImpl(null, "2", 3, 10, StrandType.STRAND_PLUS, seq));
        track.addSite(new SiteImpl(null, "3", 15, 11, StrandType.STRAND_MINUS, seq));
        track.addSite(new SiteImpl(null, "4", 18, 13, StrandType.STRAND_PLUS, seq));
        track.addSite(new SiteImpl(null, "5", 20, 5, StrandType.STRAND_PLUS, seq));
        track.finalizeAddition();
    }
}
