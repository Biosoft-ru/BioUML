package biouml.plugins.ensembl._test;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementDescriptor;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.MapAsVector;
import ru.biosoft.bsa.SequenceRegion;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa._test.AbstractTrackTest;

/**
 * @author lan
 *
 */
public class TracksTest extends AbstractTrackTest
{
    public static final String repositoryPath = "../data";

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        CollectionFactory.createRepository(repositoryPath);
    }

    private DataCollection<AnnotatedSequence> getSequencesCollection(Track track)
    {
        DataElementPath path = DataElementPath.create(track);
        if(path.getPathComponents()[1].equals("EnsemblRemote"))
        {
            return path.getRelativePath("../../Sequences").getChildrenArray()[0].getDataCollection(AnnotatedSequence.class);
        }
        String sequencesCollection = path.getDescriptor().getValue(Track.SEQUENCES_COLLECTION_PROPERTY);
        assertNotNull("Sequences collection exists", sequencesCollection);
        return DataElementPath.create(sequencesCollection).getDataCollection(AnnotatedSequence.class);
    }

    private DataCollection<Track> getTracksCollection(String dbName)
    {
        return CollectionFactory.getDataCollection("databases/"+dbName+"/Tracks");
    }

    public void testCollection() throws Exception
    {
        DataCollection<Track> tracksDC = getTracksCollection("Ensembl");
        assertNotNull(tracksDC);
        assertTrue(Track.class.isAssignableFrom(tracksDC.getDataElementType()));
        for(String trackName: new String[] {"Genes", "Repeats", "GC content", "Variations"})
        {
            assertTrue("DC contains "+trackName, tracksDC.contains(trackName));
            Track track = tracksDC.get(trackName);
            assertNotNull(trackName+" exists", track);
            assertNotNull(trackName+" viewBuilder exists", track.getViewBuilder());
            DataElementDescriptor descriptor = tracksDC.getDescriptor(trackName);
            assertNotNull(trackName+" descriptor exists", descriptor);
            String sequenceCollection = descriptor.getValue(Track.SEQUENCES_COLLECTION_PROPERTY);
            DataElementPath.create(sequenceCollection).getDataCollection(AnnotatedSequence.class);
        }
    }

    public void testSubSequence() throws Exception
    {
        Track track = getTracksCollection("Ensembl").get("Genes");
        VectorDataCollection<AnnotatedSequence> vdc = new VectorDataCollection<>( "regions" );
        CollectionFactoryUtils.getDatabases().put(vdc);
        String chromosomePath = "databases/Ensembl/Sequences/chromosomes NCBI36/1";
        AnnotatedSequence seq = DataElementPath.create(chromosomePath).getDataElement(AnnotatedSequence.class);
        vdc.put(new MapAsVector("region", vdc, new SequenceRegion(seq.getSequence(), 20000000, 1000000, true, false), null));
        DataCollection<Site> sitesOrig = track.getSites(chromosomePath, 19100000, 19900000);
        DataCollection<Site> sitesRegion = track.getSites("databases/regions/region", 99999, 899999);
        assertEquals(sitesOrig.getSize(), sitesRegion.getSize());
        for(Site site: sitesOrig)
        {
            Site siteReg = sitesRegion.get(site.getName());
            assertNotNull(siteReg);
            assertTrue((site.getStrand() == StrandType.STRAND_MINUS && siteReg.getStrand() == StrandType.STRAND_PLUS)||
                    (site.getStrand() == StrandType.STRAND_PLUS && siteReg.getStrand() == StrandType.STRAND_MINUS));
            assertEquals(site.getLength(), siteReg.getLength());
            assertEquals(20000001, site.getStart()+siteReg.getStart());
        }
    }

    public void testVariationTrack() throws Exception
    {
        testTrackBasics("Ensembl", "Variations", "3", new Interval(10000000,10100000));
    }

    public void testRepeatTrack() throws Exception
    {
        testTrackBasics("Ensembl", "Repeats", "2", new Interval(1,1000000));
    }

    public void testGeneTrack() throws Exception
    {
        testTrackBasics("Ensembl", "Genes", "1", null);
    }

    public void testKaryotypeTrack() throws Exception
    {
        testTrackBasics("Ensembl", "Karyotype", "21", null);
    }

    public void testRemoteVariationTrack() throws Exception
    {
        testTrackBasics("EnsemblRemote", "Variations", "3", new Interval(10000000,10100000));
    }

    public void testRemoteRepeatTrack() throws Exception
    {
        testTrackBasics("EnsemblRemote", "Repeats", "2", new Interval(1,1000000));
    }

    public void testRemoteGeneTrack() throws Exception
    {
        testTrackBasics("EnsemblRemote", "Genes", "1", null);
    }

    public void testRemoteKaryotypeTrack() throws Exception
    {
        testTrackBasics("EnsemblRemote", "Karyotype", "21", null);
    }

    // Test basic track functionality
    private void testTrackBasics(String dbName, String trackName, String sequenceName, Interval interval) throws Exception
    {
        Track track = getTracksCollection(dbName).get(trackName);
        assertNotNull("Cannot get gene track", track);

        AnnotatedSequence seq = getSequencesCollection(track).get(sequenceName);
        assertNotNull("Cannot get sequence", seq);

        if(interval == null) interval = seq.getSequence().getInterval();

        // Test site counts and locations
        DataCollection<Site> trackSites = new VectorDataCollection<>( "Sites" );
        for(Interval subInterval: interval.split(100))
        {
            int expectedCount = track.countSites(seq.getCompletePath().toString(), subInterval.getFrom(), subInterval.getTo());
            DataCollection<Site> sites = track.getSites(seq.getCompletePath().toString(), subInterval.getFrom(), subInterval.getTo());
            int realCount = 0;
            for(Site site: sites)
            {
                assertTrue("Returned site lies in requested interval", subInterval.intersects(new Interval(site.getFrom(), site.getTo())));
                realCount++;
                trackSites.put(site);
            }
            assertEquals(expectedCount, realCount);
        }

        DataCollection<Site> totalSites = track.getSites(seq.getCompletePath().toString(), interval.getFrom(), interval.getTo());
        assertNotNull("Can not load all sites for track", totalSites);
        assertFalse( "No sites found", totalSites.isEmpty());

        // Test whether different queries returned the same sites
        assertEquals("Incorrect site number", totalSites.getSize(), trackSites.getSize());
        for(Site site: totalSites)
        {
            Site trackSite = trackSites.get(site.getName());
            assertNotNull(trackSite);
            assertEquals(trackSite.getFrom(), site.getFrom());
            assertEquals(trackSite.getTo(), site.getTo());
            assertEquals(trackSite.getStrand(), site.getStrand());
            assertEquals(trackSite.getType(), site.getType());
        }
        
        // test getSite query (not supported by ClientTrack)
        if( !dbName.equals( "EnsemblRemote" ) )
        {
            Site trackSite = totalSites.iterator().next();
            Site site = track.getSite( seq.getCompletePath().toString(), trackSite.getName(), interval.getFrom(), interval.getTo() );
            assertNotNull( site );
            assertEquals( trackSite.getFrom(), site.getFrom() );
            assertEquals( trackSite.getTo(), site.getTo() );
            assertEquals( trackSite.getStrand(), site.getStrand() );
            assertEquals( trackSite.getType(), site.getType() );
        }

        // test viewBuilder
        checkViewBuilder(track, seq, interval);
    }
}
