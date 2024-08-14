package ru.biosoft.bsa._test;

import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.LinearSequence;
import ru.biosoft.bsa.MapAsVector;
import ru.biosoft.bsa.Nucleotide5LetterAlphabet;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceSupport;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.track.GCContentTrack;

/**
 * @author lan
 *
 */
public class TestGCContentTrack extends AbstractTrackTest
{
    private static final class StubSequence extends SequenceSupport
    {
        private final int length;
        public StubSequence(int length)
        {
            super(null, Nucleotide5LetterAlphabet.getInstance());
            this.length = length;
        }
        
        @Override
        public void setLetterCodeAt(int position, byte code)
        {
        }
        @Override
        public void setLetterAt(int position, byte letter) throws RuntimeException
        {
        }
        @Override
        public int getStart()
        {
            return 1;
        }
        @Override
        public byte getLetterAt(int position) throws RuntimeException
        {
            return (position%2 == 0)?(byte)'C':(byte)'A';
        }
        @Override
        public int getLength()
        {
            return length;
        }
    }

    public void testGCContentBasic() throws Exception
    {
        Track track = new GCContentTrack(null);
        VectorDataCollection<AnnotatedSequence> testCollection = new VectorDataCollection<>("test");
        CollectionFactory.registerRoot(testCollection);
        String string = "NNACGTACGTACGTACGATCGATCGTAGCTAGCTACGATCGATCGATCGTACGAT";
        LinearSequence sequence = new LinearSequence(string, Nucleotide5LetterAlphabet.getInstance());
        AnnotatedSequence seq = new MapAsVector("seq", testCollection, sequence, null);
        testCollection.put(seq);
        DataCollection<Site> sites = track.getSites("test/seq", sequence.getInterval().getFrom(), sequence.getInterval().getTo());
        assertNotNull(sites);
        assertEquals(sequence.getLength(), sites.getSize());
        for(Site site: sites)
        {
            Interval interval = new Interval(site.getName());
            assertEquals(interval.getFrom(), interval.getTo());
            int pos = interval.getFrom();
            switch(string.charAt(pos-1))
            {
                case 'N':
                    assertEquals(site.getType(), "N");
                    break;
                case 'A':
                case 'T':
                    assertEquals(site.getType(), "AT");
                    assertEquals(0.0, site.getProperties().getValue(GCContentTrack.SCORE));
                    break;
                case 'C':
                case 'G':
                    assertEquals(site.getType(), "GC");
                    assertEquals(100.0, site.getProperties().getValue(GCContentTrack.SCORE));
                    break;
                default:
                    fail("Invalid letter!");
            }
        }
        checkViewBuilder(track, seq, new Interval(1, string.length()));
    }

    public void testGCContentScaled() throws Exception
    {
        Track track = new GCContentTrack(null);
        VectorDataCollection<AnnotatedSequence> testCollection = new VectorDataCollection<>("test");
        CollectionFactory.registerRoot(testCollection);
        Sequence sequence = new StubSequence(50000);
        
        MapAsVector seq = new MapAsVector("seq", testCollection, sequence, null);
        testCollection.put(seq);
        DataCollection<Site> sites = track.getSites("test/seq", sequence.getInterval().getFrom(), sequence.getInterval().getTo());
        assertTrue(sites.getSize()>100);
        Site site = sites.iterator().next();
        assertEquals(50.0, site.getProperties().getValue(GCContentTrack.SCORE));
        checkViewBuilder(track, seq, sequence.getInterval());
    }

    public void testGCContentTooMany() throws Exception
    {
        Track track = new GCContentTrack(null);
        VectorDataCollection<AnnotatedSequence> testCollection = new VectorDataCollection<>("test");
        CollectionFactory.registerRoot(testCollection);
        Sequence sequence = new StubSequence(GCContentTrack.MAX_SEQUENCE_LENGTH*2);
        
        MapAsVector seq = new MapAsVector("seq", testCollection, sequence, null);
        testCollection.put(seq);
        DataCollection<Site> sites = track.getSites("test/seq", sequence.getInterval().getFrom(), sequence.getInterval().getTo());
        assertEquals(0, sites.getSize());
        checkViewBuilder(track, seq, sequence.getInterval());
    }
}
