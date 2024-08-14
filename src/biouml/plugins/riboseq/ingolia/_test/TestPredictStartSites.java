package biouml.plugins.riboseq.ingolia._test;

import java.util.Collections;
import java.util.List;

import biouml.plugins.riboseq.ingolia.PredictStartSites;
import biouml.plugins.riboseq.ingolia.PredictStartSitesParameters;
import biouml.plugins.riboseq.transcripts.TranscriptSet;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

public class TestPredictStartSites extends AbstractRiboSeqTest
{
    PredictStartSites analysis;
    DataElementPath summaryPath;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        analysis = new PredictStartSites( null, "PredictStartSites" );
        PredictStartSitesParameters parameters = analysis.getParameters();
        DataElementPathSet bamFiles = new DataElementPathSet();
        bamFiles.add( DataElementPath.create( "databases/riboseq_data/harr120s_sorted.bam" ) );
        bamFiles.add( DataElementPath.create( "databases/riboseq_data/harr150s_sorted.bam" ) );
        bamFiles.add( DataElementPath.create( "databases/riboseq_data/harr180s_sorted.bam" ) );
        bamFiles.add( DataElementPath.create( "databases/riboseq_data/harr90s_sorted.bam" ) );
        parameters.setBamFiles( bamFiles );

        parameters.setModelFile( DataElementPath.create( "databases/riboseq_data/files/model_for_prediction" ) );

        parameters.getTranscriptSet().setAnnotationSource( TranscriptSet.ANNOTATION_SOURCE_BED_FILE );

        DataElementPath transcriptsTrackPath = DataElementPath.create( "live/knownGeneOld4.bed" );
        importBEDFile( getFile( "knownGeneOld4.bed" ), transcriptsTrackPath );
        parameters.getTranscriptSet().setTranscriptsTrack( transcriptsTrackPath );
        parameters.getTranscriptSet().setSequencesCollection( DataElementPath.create("databases/EnsemblMouse37/Sequences/chromosomes NCBIM37") );

        parameters.setMinWindowFootprints( 50 );
        parameters.setWindowOverhangs( 0 );
        parameters.setStrandSpecific( true );
        parameters.setTranscriptOverhangs( 100 );

        summaryPath = DataElementPath.create( "live/summary" );
        parameters.setSummaryTable( summaryPath );
    }

    private DataElementPath createTranscriptSubset(List<String> transcriptNames) throws Exception
    {
        DataElementPath transcriptsPath = DataElementPath.create( "live/transcripts" );
        TableDataCollection transcripts = TableDataCollectionUtils.createTableDataCollection( transcriptsPath );

        for( String transcriptName : transcriptNames )
        {
            final RowDataElement row = new RowDataElement( transcriptName, transcripts );
            transcripts.addRow( row );
        }

        transcriptsPath.save( transcripts );
        return transcriptsPath;
    }

    public void testOnIngoliaData() throws Exception
    {
        final PredictStartSitesParameters parameters = analysis.getParameters();
        final List<String> transcriptNames = Collections.singletonList( "uc007afi.1" );
        DataElementPath transcriptsPath = createTranscriptSubset( transcriptNames );
        parameters.getTranscriptSet().setTranscriptSubset( transcriptsPath );

        analysis.getJobControl().run();
        
        TableDataCollection summaryTable = summaryPath.getDataElement( TableDataCollection.class );
        assertEquals( 3, summaryTable.getSize() );
        for(RowDataElement row : summaryTable)
        {
            assertEquals( "uc007afi.1", row.getValue( "Transcript name" ) );
            Integer peakFrom = (Integer)row.getValue( "Peak from" );
            Integer peakTo = (Integer)row.getValue( "Peak to" );
            String peakSequence = (String)row.getValue( "Peak sequence" );
            Integer initCodonOffset = (Integer)row.getValue( "Init codon offset" );
            String codon = (String)row.getValue( "Init codon" );
            Integer cdsLength = (Integer)row.getValue( "CDS length" );
            Integer knownCDSStartOffset = (Integer)row.getValue( "Offset from known CDS start" );
            Integer knownCDSEndOffset = (Integer)row.getValue( "Offset from known CDS end" );
            String type = (String)row.getValue( "Type" );
            String protein = (String)row.getValue( "Protein" );
            Double score = (Double)row.getValue( "Score" );
            Integer readNumber = (Integer)row.getValue( "Reads number" );
            if(peakFrom == 27)
            {
                assertEquals( (Integer)29, peakTo );
                assertEquals( "CTT", peakSequence );
                assertEquals( (Integer)28, initCodonOffset );
                assertEquals( "TTG", codon );
                assertEquals( (Integer)978, cdsLength );
                assertEquals( (Integer)(-72), knownCDSStartOffset);
                assertEquals( (Integer)0, knownCDSEndOffset);
                assertEquals( "EXTENSION", type );
                assertEquals( "LSNVRAASLASRPLWPGEGRPSGAM" +
                        "EDEVVRIAKKMDKMVQKKNAAGALDLLKELKNIPM" +
                        "TLELLQSTRIGMSVNALRKQSTDEEVTSLAKSLIK" +
                        "SWKKLLDGPSTDKDPEEKKKEPAISSQNSPEAREE" +
                        "SSSSSNVSSRKDETNARDTYVSSFPRAPSTSDSVR" +
                        "LKCREMLAAALRTGDDYVAIGADEEELGSQIEEAI" +
                        "YQEIRNTDMKYKNRVRSRISNLKDAKNPNLRKNVL" +
                        "CGNIPPDLFARMTAEEMASDELKEMRKNLTKEAIR" +
                        "EHQMAKTGGTQTDLFTCGKCKKKNCTYTQVQTRSA" +
                        "DEPMTTFVVCNECGNRWKFC", protein );
                assertEquals( 2.153, score, 0.001 );
                assertEquals( (Integer)2, readNumber );
            }else if(peakFrom == 99)
            {
                assertEquals( (Integer)101, peakTo );
            }else if(peakFrom == 298)
            {
                assertEquals( (Integer)300, peakTo );
            }else
            {
                assertTrue( "Unknown peak from", false );
            }
        }
    }

    public void testOnIngoliaDataReadNumber() throws Exception
    {
        final PredictStartSitesParameters parameters = analysis.getParameters();
        final List<String> transcriptNames = Collections.singletonList( "uc007afj.1" );
        DataElementPath transcriptsPath = createTranscriptSubset( transcriptNames );
        parameters.getTranscriptSet().setTranscriptSubset( transcriptsPath );

        analysis.getJobControl().run();

        TableDataCollection summaryTable = summaryPath.getDataElement( TableDataCollection.class );
        assertEquals( 1, summaryTable.getSize() );

        final RowDataElement row = summaryTable.getAt( 0 );
        final int readNumber = (Integer)row.getValue( "Reads number" );
        assertEquals( 1, readNumber );
    }
}
