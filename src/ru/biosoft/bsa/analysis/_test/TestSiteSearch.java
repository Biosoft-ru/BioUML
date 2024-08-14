package ru.biosoft.bsa.analysis._test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;
import ru.biosoft.bsa.Alphabet;
import ru.biosoft.bsa.BindingElement;
import ru.biosoft.bsa.LinearSequence;
import ru.biosoft.bsa.Nucleotide5LetterAlphabet;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteModel;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.TrackImpl;
import ru.biosoft.bsa.WritableTrack;
import ru.biosoft.bsa.analysis.SequenceAccessor;
import ru.biosoft.bsa.analysis.SiteSearchWorker;

public class TestSiteSearch extends TestCase
{
    public void testGetWorkers() throws Exception
    {
        byte[] bytes = new byte[5014];
        for( int i = 0; i < bytes.length; i++ )
            bytes[i] = 'A';
        int[] positions = new int[] {1, 10, 998, 1999, 3000, 4001, 5012};
        for( int pos : positions )
            for( int i = 0; i < 3; i++ )
                bytes[pos + i - 1] = 'C';
        Sequence sequence = new LinearSequence("MySequence", bytes, Nucleotide5LetterAlphabet.getInstance() );
        SequenceAccessor accessor = new SequenceAccessor( sequence );

        SiteModel siteModel = new SiteModel( "CCC", null, 1 )
        {
            @Override
            public double getScore(Sequence sequence, int position)
            {
                if( sequence.getLetterAt( position ) == 'C' && sequence.getLetterAt( position + 1 ) == 'C'
                        && sequence.getLetterAt( position + 2 ) == 'C' )
                    return 1;
                return 0;
            }

            @Override
            public int getLength()
            {
                return 3;
            }

            @Override
            public BindingElement getBindingElement()
            {
                return null;
            }

            @Override
            public Alphabet getAlphabet()
            {
                return Nucleotide5LetterAlphabet.getInstance();
            }
        };


        final List<Site> result = new ArrayList<>();
        WritableTrack track = new TrackImpl( "", null ){
            @Override
            public void addSite(Site site)
            {
                result.add( site );
            }
        };
        List<SiteSearchWorker> workers = SiteSearchWorker.getWorkers( accessor, sequence.getStart(), sequence.getLength(),
                Collections.singletonList( siteModel ), track, 1000 );
        for( SiteSearchWorker worker : workers )
            worker.call();

        assertEquals( 6, workers.size() );
        assertEquals( positions.length, result.size() );
        boolean[] found = new boolean[positions.length];
        for( Site s : result )
        {
            assertEquals( "MySequence", s.getOriginalSequence().getName() );
            assertEquals( 3, s.getLength() );
            assertEquals( StrandType.STRAND_PLUS, s.getStrand() );
            int start = s.getStart();
            boolean siteFound = false;
            for( int i = 0; i < positions.length; i++ )
                if( !found[i] && start == positions[i] )
                {
                    found[i] = true;
                    siteFound = true;
                    break;
                }
            assertTrue( siteFound );
        }
    }
}
