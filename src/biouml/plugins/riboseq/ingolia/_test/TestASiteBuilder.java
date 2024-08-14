package biouml.plugins.riboseq.ingolia._test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import biouml.plugins.riboseq.ingolia.asite.ASiteOffsetBuilder;
import biouml.plugins.riboseq.ingolia.asite.ASiteOffsetBuilderParameters;
import biouml.plugins.riboseq.ingolia.asite.BuildASiteOffsetTable;
import biouml.plugins.riboseq.transcripts.Transcript;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.bsa.BAMTrack;
import ru.biosoft.bsa.Interval;

public class TestASiteBuilder extends AbstractRiboSeqTest
{
    private Transcript transcript;
    private BAMTrack bamTrack;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        transcript = getTestTranscript();
        bamTrack = getTestBamTrack();
    }

    public void testOneSiteOnTable() throws Exception
    {
        final ASiteOffsetBuilderParameters builderParameters = new ASiteOffsetBuilderParameters( 100, false );
        final ASiteOffsetBuilder builder = new ASiteOffsetBuilder( builderParameters );
        final AnalysisJobControl jobControl = new AnalysisJobControl(
                new BuildASiteOffsetTable( null, "BuildASiteOffsetTable" ) );
        builder.computeTableFromBamTracks( Collections.singletonList( bamTrack ),
                Collections.singletonList( transcript ), jobControl );

        assertEquals( 100, jobControl.getPreparedness() );

        final Map<Integer, Integer> aSiteOffsetTable = builder.getASiteOffsetTable();

        final Map<Integer, Integer> expectedTable = new HashMap<>();
        expectedTable.put( 32, 30 );
        assertEquals( expectedTable, aSiteOffsetTable );
    }

    public void testNonCodingTranscript() throws Exception
    {
        final Transcript nonCodingTranscript = new Transcript( transcript.getName(), transcript.getChromosome(),
                transcript.getLocation(), transcript.isOnPositiveStrand(), transcript.getExonLocations(),
                Collections.<Interval>emptyList() );

        final ASiteOffsetBuilderParameters builderParameters = new ASiteOffsetBuilderParameters( 100, false );
        final ASiteOffsetBuilder builder = new ASiteOffsetBuilder( builderParameters );
        final AnalysisJobControl jobControl = new AnalysisJobControl(
                new BuildASiteOffsetTable( null, "BuildASiteOffsetTable" ) );
        builder.computeTableFromBamTracks( Collections.singletonList( bamTrack ),
                Collections.singletonList( nonCodingTranscript ), jobControl );

        assertEquals( 100, jobControl.getPreparedness() );

        final Map<Integer, Integer> aSiteOffsetTable = builder.getASiteOffsetTable();

        final Map<Integer, Integer> expectedTable = new HashMap<>();
        assertEquals( expectedTable, aSiteOffsetTable );
    }

    public void testNegativeStrandTranscriptAndStrandSpecific() throws Exception
    {
        final Transcript nonCodingTranscript = new Transcript( transcript.getName(),
                transcript.getChromosome(), transcript.getLocation(), false,
                transcript.getExonLocations(), transcript.getCDSLocations() );

        final ASiteOffsetBuilderParameters builderParameters = new ASiteOffsetBuilderParameters( 100, true );
        final ASiteOffsetBuilder builder = new ASiteOffsetBuilder( builderParameters );
        final AnalysisJobControl jobControl = new AnalysisJobControl(
                new BuildASiteOffsetTable( null, "BuildASiteOffsetTable" ) );
        builder.computeTableFromBamTracks( Collections.singletonList( bamTrack ),
                Collections.singletonList( nonCodingTranscript ), jobControl );

        assertEquals( 100, jobControl.getPreparedness() );

        final Map<Integer, Integer> aSiteOffsetTable = builder.getASiteOffsetTable();

        final Map<Integer, Integer> expectedTable = new HashMap<>();
        assertEquals( expectedTable, aSiteOffsetTable );
    }

    private BAMTrack getTestBamTrack()
    {
        final DataElementPath bamFile = DataElementPath.create(
                "databases/riboseq_data/test_asite_transcript_sites.bam" );
        return bamFile.getDataElement( BAMTrack.class );
    }

    private Transcript getTestTranscript()
    {
        final Interval[] exonArray = new Interval[]{
                new Interval( 57867031, 57869908 ),
                new Interval( 57895895, 57896073 ),
                new Interval( 57905740, 57909855 )};
        final Interval[] cdsArray = new Interval[]{
                new Interval( 513, 3155 )};

        return new Transcript( "uc008sym.1", "4", new Interval( 57867031, 57909855 ), true,
                Arrays.asList( exonArray ), Arrays.asList( cdsArray ) );
    }
}
