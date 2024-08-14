package biouml.plugins.riboseq._test;

import java.util.ArrayList;
import java.util.List;

import biouml.plugins.riboseq.riboseqanalysis.RiboSeqAnalysis;
import biouml.plugins.riboseq.riboseqanalysis.RiboSeqParameters;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.bsa.BAMTrack;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.WritableTrack;

public class RiboSeqAnalysisTest extends AbstractBioUMLTest
{
    private static final String TEST_REPOSITORY_PATH = "../data/test/biouml/plugins/riboseq/data/";

    private static final DataElementPath ROOT = DataElementPath.create( "databases" );
    private static final DataElementPath BAM_FOLDER = ROOT.getChildPath( "riboseq_data" );

    private static final String OUTPUT_TRACKS_STR = "output_tracks";
    private static final DataElementPath OUTPUT_TRACKS_PATH = DataElementPath.create( OUTPUT_TRACKS_STR );

    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        CollectionFactory.createRepository( TEST_REPOSITORY_PATH );

        VectorDataCollection<WritableTrack> outputVdc = new VectorDataCollection<>( OUTPUT_TRACKS_STR, WritableTrack.class, null );
        CollectionFactory.registerRoot( outputVdc );
    }

    public void testEmptyInputTrack() throws Exception
    {
        final String inputTrackName = "emptyTest.bam";
        final DataElementPath inputTrack = BAM_FOLDER.getChildPath( inputTrackName );
        final DataElementPath resultTrack = OUTPUT_TRACKS_PATH.getChildPath( inputTrackName );

        final RiboSeqParameters parameters = createParameters( inputTrack, resultTrack, 0, 0 );

        final RiboSeqAnalysis analysis = new RiboSeqAnalysis( null, null );
        analysis.setParameters( parameters );
        analysis.validateParameters();

        final WritableTrack outputTrack = analysis.justAnalyzeAndPut();

        final List<Site> inputSiteList = getSiteListFromInputTrack( parameters.getInputPath() );

        final int expectedSize = inputSiteList.size();
        assertEquals( expectedSize, outputTrack.getAllSites().getSize() );
    }

    public void testOneSite() throws Exception
    {
        final String inputTrackName = "oneSiteTest.bam";
        final DataElementPath inputTrack = BAM_FOLDER.getChildPath( inputTrackName );
        final DataElementPath resultTrack = OUTPUT_TRACKS_PATH.getChildPath( inputTrackName );

        final RiboSeqParameters parameters = createParameters( inputTrack, resultTrack, 0, 0 );

        final RiboSeqAnalysis analysis = new RiboSeqAnalysis( null, null );
        analysis.setParameters( parameters );
        analysis.validateParameters();

        final WritableTrack outputTrack = analysis.justAnalyzeAndPut();

        final List<Site> inputSiteList = getSiteListFromInputTrack( parameters.getInputPath() );

        final List<Site> clusterList = getClusterList( outputTrack );
        final int expectedSize = inputSiteList.size();
        assertEquals( expectedSize, clusterList.size() );

        final Site firstCluster = clusterList.get( 0 );
        final Site expectedSite = inputSiteList.get( 0 );
        final int expectedStart = expectedSite.getStart();
        final int expectedLength = expectedSite.getLength();
        checkClusterSize( firstCluster, expectedStart, expectedLength );
    }

    public void testNeighboringSitesOneCluster() throws Exception
    {
        final String inputTrackName = "neighboringSitesTest.bam";
        final DataElementPath inputTrack = BAM_FOLDER.getChildPath( inputTrackName );
        final DataElementPath resultTrack = OUTPUT_TRACKS_PATH.getChildPath( inputTrackName );

        final RiboSeqParameters parameters = createParameters( inputTrack, resultTrack, 0, 0 );

        final RiboSeqAnalysis analysis = new RiboSeqAnalysis( null, null );
        analysis.setParameters( parameters );
        analysis.validateParameters();

        final WritableTrack outputTrack = analysis.justAnalyzeAndPut();

        final List<Site> inputSiteList = getSiteListFromInputTrack( parameters.getInputPath() );

        final List<Site> clusterList = getClusterList( outputTrack );
        final int expectedSize = inputSiteList.size() - 1;
        assertEquals( expectedSize, clusterList.size() );

        final Site firstCluster = clusterList.get( 0 );
        final Site firstSite = inputSiteList.get( 0 );
        final Site secondSite = inputSiteList.get( 1 );
        final int expectedStart = firstSite.getStart();
        final int expectedLength = firstSite.getLength() + secondSite.getLength() - 1;
        checkClusterSize( firstCluster, expectedStart, expectedLength );
    }

    public void testValidateParametersNumberSites() throws Exception
    {
        final String inputTrackName = "emptyTest.bam";
        final DataElementPath inputTrack = BAM_FOLDER.getChildPath( inputTrackName );
        final DataElementPath resultTrack = OUTPUT_TRACKS_PATH.getChildPath( inputTrackName );

        final int invalidMinNumberSites = -5;
        final RiboSeqParameters parameters = createParameters( inputTrack, resultTrack, invalidMinNumberSites, 0 );

        final RiboSeqAnalysis analysis = new RiboSeqAnalysis( null, null );
        analysis.setParameters( parameters );

        try
        {
            analysis.validateParameters();
            fail( "should've thrown an exception!" );
        }
        catch( IllegalArgumentException ignored )
        {
        }
    }

    public void testValidateParametersMissingSequence() throws Exception
    {
        final String inputTrackName = "emptyTestWithoutSeq.bam";
        final DataElementPath inputTrack = BAM_FOLDER.getChildPath( inputTrackName );
        final DataElementPath resultTrack = OUTPUT_TRACKS_PATH.getChildPath( inputTrackName );

        final RiboSeqParameters parameters = createParameters( inputTrack, resultTrack, 0, 0 );

        final RiboSeqAnalysis analysis = new RiboSeqAnalysis( null, null );
        analysis.setParameters( parameters );

        try
        {
            analysis.validateParameters();
            fail( "should've thrown an exception!" );
        }
        catch( IllegalArgumentException e )
        {
            assertEquals( "Invalid sequences collection specified", e.getMessage() );
        }
    }

    public void testSimpleExample() throws Exception
    {
        final String inputTrackName = "sample1.bam";
        final DataElementPath inputTrack = BAM_FOLDER.getChildPath( inputTrackName );
        final DataElementPath resultTrack = OUTPUT_TRACKS_PATH.getChildPath( inputTrackName );

        final RiboSeqParameters parameters = createParameters( inputTrack, resultTrack, 0, 0 );

        final RiboSeqAnalysis analysis = new RiboSeqAnalysis( null, null );
        analysis.setParameters( parameters );
        analysis.validateParameters();

        final WritableTrack outputTrack = analysis.justAnalyzeAndPut();

        final List<Site> clusterList = getClusterList( outputTrack );

        assertTrue( clusterList.size() > 0 );
    }

    private RiboSeqParameters createParameters(DataElementPath inputPath, DataElementPath outputPath, int minNumberSites,
            int maxLengthCluster)
    {
        final RiboSeqParameters parameters = new RiboSeqParameters();
        parameters.setMinNumberSites( minNumberSites );
        parameters.setMaxLengthCluster( maxLengthCluster );

        parameters.setInputPath( inputPath );

        parameters.setOutputPath( outputPath );

        return parameters;
    }

    private List<Site> getSiteListFromInputTrack(DataElementPath inputBamTrackPath)
    {
        final List<Site> siteList = new ArrayList<>();

        final BAMTrack bamTrack = inputBamTrackPath.getDataElement( BAMTrack.class );
        final DataCollection<Site> siteDataCollection = bamTrack.getAllSites();
        for( Site site : siteDataCollection )
        {
            siteList.add( site );
        }

        return siteList;
    }

    private List<Site> getClusterList(WritableTrack outputTrack)
    {
        final DataCollection<Site> clusterDataCollection = outputTrack.getAllSites();
        final List<Site> clusterList = new ArrayList<>();

        for( Site cluster : clusterDataCollection )
        {
            clusterList.add( cluster );
        }

        return clusterList;
    }

    private void checkClusterSize(Site cluster, int start, int length)
    {
        assertEquals( start, cluster.getStart() );
        assertEquals( length, cluster.getLength() );
    }
}
