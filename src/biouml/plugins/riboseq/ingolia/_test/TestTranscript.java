package biouml.plugins.riboseq.ingolia._test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import biouml.plugins.riboseq.transcripts.Transcript;
import junit.framework.TestCase;
import ru.biosoft.bsa.Interval;

public class TestTranscript extends TestCase
{
    public void testGetStartInitializationsNonCodingTranscript() throws Exception
    {
        final List<Integer> expectedNonCodingList = Collections.emptyList();

        final Transcript nullCodingTranscript = createTranscript( null );
        assertEquals( expectedNonCodingList, nullCodingTranscript.getStartInitializations() );

        final Transcript emptyListCodingTranscript = createTranscript( Collections.<Interval>emptyList() );
        assertEquals( expectedNonCodingList, emptyListCodingTranscript.getStartInitializations() );
    }

    public void testGetStartInitializationsCodingTranscript() throws Exception
    {
        final Interval cdsLocation = new Interval( 513, 3155 );
        final Transcript transcript = createTranscript( Arrays.asList( cdsLocation ) );

        final List<Integer> expectedStartInitialization = Arrays.asList( cdsLocation.getFrom() );
        assertEquals( expectedStartInitialization, transcript.getStartInitializations() );
    }

    public void testIsCoding() throws Exception
    {
        final Interval cdsLocation = new Interval( 513, 3155 );
        final Transcript codingTranscript = createTranscript( Arrays.asList( cdsLocation ) );
        assertTrue( codingTranscript.isCoding() );

        final Transcript nonCodingTranscript = createTranscript( null );
        assertFalse( nonCodingTranscript.isCoding() );
    }

    private static Transcript createTranscript(List<Interval> cdsLocations)
    {
        final Interval location = new Interval( 57867031, 57909855 );
        final Interval[] exonArray = new Interval[]{
                new Interval( 57867031, 57869908 ),
                new Interval( 57895895, 57896073 ),
                new Interval( 57905740, 57909855 )};
        final List<Interval> exonLocations = Arrays.asList( exonArray );

        return new Transcript("noName","1", location, true, exonLocations, cdsLocations );
    }
}
