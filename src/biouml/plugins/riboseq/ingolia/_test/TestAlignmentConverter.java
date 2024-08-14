package biouml.plugins.riboseq.ingolia._test;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import biouml.plugins.riboseq.ingolia.AlignmentConverter;
import biouml.plugins.riboseq.ingolia.AlignmentOnTranscript;
import biouml.plugins.riboseq.transcripts.Transcript;
import junit.framework.TestCase;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.BAMTrack;
import ru.biosoft.bsa.Interval;

public class TestAlignmentConverter extends TestCase
{
    private static final String TEST_REPOSITORY_PATH = "../data/test/biouml/plugins/riboseq/data/";

    @Override
    protected void setUp() throws Exception
    {
        CollectionFactory.createRepository( TEST_REPOSITORY_PATH );
    }

    public void test1() throws Exception
    {
        AlignmentConverter converter = new AlignmentConverter();
        BAMTrack bamTrack = DataElementPath.create( "databases/riboseq_data/uc008org.1.bam" ).getDataElement( BAMTrack.class );
        List<Interval> exonLocations = new ArrayList<>();
        exonLocations.add( new Interval( 16083249, 16083519 ) );
        exonLocations.add( new Interval( 16083921, 16083945 ) );
        exonLocations.add( new Interval( 16089477, 16089562 ) );
        exonLocations.add( new Interval( 16103186, 16103218 ) );
        exonLocations.add( new Interval( 16103825, 16105423 ) );
        exonLocations.add( new Interval( 16113954, 16116853 ) );
        Transcript transcript = new Transcript( "uc008org.1", "chr3", new Interval( 16083249, 16116854 ), true, exonLocations,
                Collections.singletonList( new Interval( 247, 2037 ) ) );
        List<AlignmentOnTranscript> transcriptAlignments = converter.getTranscriptAlignments( transcript, bamTrack );
        List<AlignmentOnTranscript> positiveStrand = new ArrayList<>();
        for( AlignmentOnTranscript a : transcriptAlignments )
            if( a.isPositiveStrand() )
                positiveStrand.add( a );
        assertEquals( 2847, positiveStrand.size() );
    }
}
