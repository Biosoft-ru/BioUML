package biouml.plugins.riboseq.ingolia._test;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;

import biouml.plugins.riboseq.ingolia.asite.BuildASiteOffsetTable;
import biouml.plugins.riboseq.ingolia.asite.BuildASiteOffsetTableParameters;
import biouml.plugins.riboseq.transcripts.TranscriptSet;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;

public class TestBuildASiteOffsetTable extends AbstractRiboSeqTest
{
    public void testOneTranscript() throws Exception
    {
        final BuildASiteOffsetTable analysis = new BuildASiteOffsetTable( null, "BuildASiteOffsetTable" );
        final BuildASiteOffsetTableParameters parameters = analysis.getParameters();

        final DataElementPathSet bamFiles = new DataElementPathSet();
        bamFiles.add( DataElementPath.create( "databases/riboseq_data/test_one_transcript_sites.bam" ) );
        parameters.setBamFiles( bamFiles );

        final DataElementPath transcriptsTrackPath = DataElementPath.create( "live/test_one_transcript.bed" );
        importBEDFile( getFile( "test_one_transcript.bed" ), transcriptsTrackPath );
        parameters.getTranscriptSet().setTranscriptsTrack( transcriptsTrackPath );

        parameters.getTranscriptSet().setAnnotationSource( TranscriptSet.ANNOTATION_SOURCE_BED_FILE );

        parameters.setStrandSpecific( false );
        parameters.setTranscriptOverhangs( 100 );

        final DataElementPath aSiteOffsetTablePath = DataElementPath.create( "live/summary" );
        parameters.setASiteOffsetTable( aSiteOffsetTablePath );

        analysis.justAnalyzeAndPut();

        final TableDataCollection aSiteOffsetTable = aSiteOffsetTablePath.getDataElement( TableDataCollection.class );
        assertEquals( 7, aSiteOffsetTable.getSize() );

        final Map<Integer, Integer> aSiteOffsetMap = tableDcToMap( aSiteOffsetTable );
        final Map<Integer, Integer> expectedASiteOffsetMap = getExpectedASiteOffsetMap();
        assertEquals( expectedASiteOffsetMap, aSiteOffsetMap );

        RowDataElement rowDataElement = getRow( aSiteOffsetTable );
        final Chart histogramChart = (Chart) rowDataElement.getValue( "Histogram" );
        assertEquals( 1, histogramChart.getSeriesCount() );

        final double[][] chartData = histogramChart.getSeries( 0 ).getData();
        assertEquals( 5, chartData.length );

        final double expectedXValueOffset = 16.0;
        final int modaXCoordinate = getCoordinate( chartData, expectedXValueOffset );

        assertEquals( expectedXValueOffset, chartData[modaXCoordinate][0] );
        final double expectedYValueCounter = 5.0;
        assertEquals( expectedYValueCounter, chartData[modaXCoordinate][1] );

        final boolean trust = rowDataElement.getValue( "Trust" ).equals( "true" );
        assertFalse( trust );
    }

    private int getCoordinate(double[][] chartData, double Offset)
    {
        for( int i = 0; i < chartData.length; i++ )
        {
            if( chartData[i][0] == Offset )
            {
                return i;
            }

        }

        fail( "should contain necessary offset" );
        return 0;
    }

    private RowDataElement getRow(TableDataCollection aSiteOffsetTable)
    {
        for( RowDataElement row : aSiteOffsetTable )
        {
            if( row.getValue( "Length" ).equals( 33 ) )
            {
                return row;
            }
        }

        fail( "should contain necessary row" );
        return null;
    }

    private Map<Integer, Integer> tableDcToMap(TableDataCollection table)
    {
        final Map<Integer, Integer> result = new HashMap<>();

        for( RowDataElement row : table )
        {
            final Integer length = (Integer) row.getValue( "Length" );
            final Integer offset = (Integer) row.getValue( "Offset" );

            result.put( length, offset );
        }

        return result;
    }

    private Map<Integer, Integer> getExpectedASiteOffsetMap()
    {
        final Integer[][] array = {
                {34, 28},
                {32, 16},
                {33, 16},
                {29, 15},
                {28, 6},
                {31, 6},
                {30, 14}};
        return ArrayUtils.toMap( array );
    }
}
