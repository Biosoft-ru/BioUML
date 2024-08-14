package biouml.plugins.riboseq.ingolia._test;

import java.util.ArrayList;
import java.util.List;

import biouml.plugins.riboseq.transcripts.EnsemblTranscriptsProvider;
import biouml.plugins.riboseq.transcripts.Transcript;
import biouml.plugins.server.SqlModule;
import junit.framework.TestCase;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Sequence;

public class TestEnsemblTranscriptsProvider extends TestCase
{
    private static final String TEST_REPOSITORY_PATH = "../data/test/biouml/plugins/riboseq/data/";

    private List<Transcript> transcripts;

    @Override
    protected void setUp() throws Exception
    {
        CollectionFactory.createRepository( TEST_REPOSITORY_PATH );
        DataElementPath ensemblPath = DataElementPath.create( "databases/EnsemblMouse37" );
        EnsemblTranscriptsProvider provider = new EnsemblTranscriptsProvider( ensemblPath.getDataElement( SqlModule.class ) );
        transcripts = provider.getTranscripts();
    }

    public void testPositiveStrand() throws Exception
    {
        Transcript transcript = findTranscript( "ENSMUST00000070968", transcripts );
        assertEquals( "1", transcript.getChromosome() );
        assertEquals( new Interval( 52176281, 52218703 ), transcript.getLocation() );
        assertEquals( 5181, transcript.getLength() );

        List<Interval> exons = new ArrayList<>();
        exons.add( new Interval( 52176281, 52176466 ) );
        exons.add( new Interval( 52176755, 52176915 ) );
        exons.add( new Interval( 52179434, 52179564 ) );
        exons.add( new Interval( 52179957, 52180101 ) );
        exons.add( new Interval( 52183363, 52183461 ) );
        exons.add( new Interval( 52189387, 52189476 ) );
        exons.add( new Interval( 52192446, 52192524 ) );
        exons.add( new Interval( 52193722, 52193813 ) );
        exons.add( new Interval( 52194122, 52194273 ) );
        exons.add( new Interval( 52196037, 52196195 ) );
        exons.add( new Interval( 52197420, 52197512 ) );
        exons.add( new Interval( 52199744, 52199803 ) );
        exons.add( new Interval( 52200929, 52200958 ) );
        exons.add( new Interval( 52201047, 52201140 ) );
        exons.add( new Interval( 52201993, 52202034 ) );
        exons.add( new Interval( 52204137, 52204220 ) );
        exons.add( new Interval( 52204726, 52204842 ) );
        exons.add( new Interval( 52205746, 52205881 ) );
        exons.add( new Interval( 52207462, 52207511 ) );
        exons.add( new Interval( 52208081, 52208175 ) );
        exons.add( new Interval( 52209076, 52209221 ) );
        exons.add( new Interval( 52210687, 52210872 ) );
        exons.add( new Interval( 52211829, 52211904 ) );
        exons.add( new Interval( 52212826, 52212925 ) );
        exons.add( new Interval( 52216126, 52218703 ) );
        assertEquals( exons, transcript.getExonLocations() );

        Interval cds = new Interval( 350, 2617 );
        assertEquals( 1, transcript.getCDSLocations().size() );
        assertEquals( transcript.getCDSLocations().get( 0 ), cds );
        
        DataCollection<AnnotatedSequence> chromosomes = DataElementPath.create( "databases/EnsemblMouse37/Sequences/chromosomes NCBIM37" ).getDataCollection( AnnotatedSequence.class );
        Sequence sequence = transcript.getSequence( chromosomes.get( transcript.getChromosome() ).getSequence() );
        String seqStr = new String( sequence.getBytes() ).toUpperCase();
        assertEquals( 5181, seqStr.length());
        assertTrue( seqStr.startsWith( "AGGAGGCGGGA" ) );
        assertTrue( seqStr.endsWith( "CACTAATAAAAGAAATGCCTC" ) );
        
    }

    public void testNegativeStrand()
    {
        Transcript transcript = findTranscript( "ENSMUST00000056974", transcripts );
        assertEquals( "10", transcript.getChromosome() );
        assertEquals( new Interval( 41905591, 41996547 ), transcript.getLocation() );
        assertEquals( 2889, transcript.getLength() );

        List<Interval> exons = new ArrayList<>();
        exons.add( new Interval(41905591, 41906100) );
        exons.add( new Interval(41916271, 41917706) );
        exons.add( new Interval(41994621, 41995346) );
        exons.add( new Interval(41996331, 41996547) );
        assertEquals( exons, transcript.getExonLocations() );

        Interval cds = new Interval( 325, 2343 );
        assertEquals( 1, transcript.getCDSLocations().size() );
        assertEquals( transcript.getCDSLocations().get( 0 ), cds );
    }



    private Transcript findTranscript(String name, List<Transcript> transcripts)
    {
        for( Transcript t : transcripts )
            if( t.getName().equals( name ) )
                return t;
        return null;
    }
}
