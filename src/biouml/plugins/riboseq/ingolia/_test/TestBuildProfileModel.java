package biouml.plugins.riboseq.ingolia._test;

import biouml.plugins.riboseq.ingolia.BuildProfileModel;
import biouml.plugins.riboseq.ingolia.BuildProfileModelParameters;
import biouml.plugins.riboseq.transcripts.TranscriptSet;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.table.TableDataCollection;

public class TestBuildProfileModel extends AbstractRiboSeqTest
{
    public void testOnIngoliaData() throws Exception
    {
        BuildProfileModel analysis = new BuildProfileModel( null, "BuildSVMModel" );
        BuildProfileModelParameters parameters = analysis.getParameters();

        DataElementPathSet bamFiles = new DataElementPathSet();
        bamFiles.add( DataElementPath.create( "databases/riboseq_data/harr120s_sorted.bam" ) );
        bamFiles.add( DataElementPath.create( "databases/riboseq_data/harr150s_sorted.bam" ) );
        bamFiles.add( DataElementPath.create( "databases/riboseq_data/harr180s_sorted.bam" ) );
        bamFiles.add( DataElementPath.create( "databases/riboseq_data/harr90s_sorted.bam" ) );
        parameters.setBamFiles( bamFiles );

        parameters.getTranscriptSet().setAnnotationSource( TranscriptSet.ANNOTATION_SOURCE_BED_FILE );

        DataElementPath transcriptsTrackPath = DataElementPath.create( "live/knownGeneOld4.bed" );
        importBEDFile( getFile( "knownGeneOld4.bed" ), transcriptsTrackPath );
        parameters.getTranscriptSet().setTranscriptsTrack( transcriptsTrackPath );

        DataElementPath wellExpressedPath = DataElementPath.create( "live/wellExpressed.txt" );
        importTable( getFile( "wellExpressed.txt" ), wellExpressedPath );
        parameters.getTranscriptSet().setTranscriptSubset( wellExpressedPath );

        parameters.setLearningFraction( 2.0 / 3 );
        parameters.setMinWindowFootprints( 50 );
        parameters.setWindowOverhangs( 0 );
        parameters.setStrandSpecific( true );
        parameters.setTranscriptOverhangs( 100 );
        parameters.setRandomSeed( 0 );

        parameters.setModelFile( DataElementPath.create( "databases/riboseq_data/files/model" ) );
        parameters.setConfusionMatrix( DataElementPath.create( "live/confusionMatrix" ) );

        analysis.getJobControl().run();

        TableDataCollection confusionMatrix = parameters.getConfusionMatrix().getDataElement( TableDataCollection.class );
        int tp = (Integer)confusionMatrix.get( "Predicted positive" ).getValue( "Real positive" );
        int fp = (Integer)confusionMatrix.get( "Predicted positive" ).getValue( "Real negative" );
        int fn = (Integer)confusionMatrix.get( "Predicted negative" ).getValue( "Real positive" );
        int tn = (Integer)confusionMatrix.get( "Predicted negative" ).getValue( "Real negative" );
        assertEquals( 1432, tp );
        assertEquals( 198, fp );
        assertEquals( 132, fn );
        assertEquals( 11553, tn );

        assertFileEquals( getFile( "model_expected" ), getFile( "model" ) );
    }

    public void testOnIngoliaData2() throws Exception
    {
        BuildProfileModel analysis = new BuildProfileModel( null, "BuildSVMModel" );
        BuildProfileModelParameters parameters = analysis.getParameters();

        DataElementPathSet bamFiles = new DataElementPathSet();
        bamFiles.add( DataElementPath.create( "databases/riboseq_data/harr120s_sorted.bam" ) );
        parameters.setBamFiles( bamFiles );

        TranscriptSet transcriptSet = parameters.getTranscriptSet();
        transcriptSet.setAnnotationSource( TranscriptSet.ANNOTATION_SOURCE_BED_FILE );

        DataElementPath transcriptsTrackPath = DataElementPath.create( "live/knownGeneOld4.bed" );
        importBEDFile( getFile( "knownGeneOld4.bed" ), transcriptsTrackPath );
        transcriptSet.setTranscriptsTrack( transcriptsTrackPath );

        DataElementPath wellExpressedPath = DataElementPath.create( "live/wellExpressedSmall.txt" );
        importTable( getFile( "wellExpressedSmall.txt" ), wellExpressedPath );
        transcriptSet.setTranscriptSubset( wellExpressedPath );

        parameters.setLearningFraction( 2.0 / 3 );
        parameters.setMinWindowFootprints( 50 );
        parameters.setWindowOverhangs( 0 );
        parameters.setStrandSpecific( true );
        parameters.setTranscriptOverhangs( 100 );
        parameters.setRandomSeed( 0 );

        parameters.setModelFile( DataElementPath.create( "databases/riboseq_data/files/model" ) );
        parameters.setConfusionMatrix( DataElementPath.create( "live/confusionMatrix" ) );

        analysis.getJobControl().run();

        TableDataCollection confusionMatrix = parameters.getConfusionMatrix().getDataElement( TableDataCollection.class );
        int tp = (Integer)confusionMatrix.get( "Predicted positive" ).getValue( "Real positive" );
        int fp = (Integer)confusionMatrix.get( "Predicted positive" ).getValue( "Real negative" );
        int fn = (Integer)confusionMatrix.get( "Predicted negative" ).getValue( "Real positive" );
        int tn = (Integer)confusionMatrix.get( "Predicted negative" ).getValue( "Real negative" );
        assertEquals( 1, tp );
        assertEquals( 1, fp );
        assertEquals( 2, fn );
        assertEquals( 16, tn );
    }
}
