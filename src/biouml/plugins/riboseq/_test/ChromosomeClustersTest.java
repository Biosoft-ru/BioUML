package biouml.plugins.riboseq._test;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.LinearSequence;
import ru.biosoft.bsa.Nucleotide5LetterAlphabet;
import ru.biosoft.bsa.Precision;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SiteType;
import ru.biosoft.bsa.StrandType;
import biouml.plugins.riboseq.datastructure.ChromosomeClusters;
import biouml.plugins.riboseq.datastructure.SiteCluster;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetSupport;

public class ChromosomeClustersTest extends AbstractBioUMLTest
{
    private final String sequenceString = "acgtacgtacgtagcatcgatcgatcgtacacgtacgtac";
    private final int minNumberSites = 2;
    private final int maxLengthCluster = 100;

    private Site site1;
    private Site site2;
    private Site site3;
    private Site site4rev;

    private DataCollection<Site> simpleSiteCollection;
    private DataCollection<Site> complexSiteCollection;

    private Sequence sequence;

    private Site createSite(int start, int length, int strand, String name)
    {
        final String CIGAR = "Cigar";

        DynamicPropertySet properties = new DynamicPropertySetSupport();
        properties.add( new DynamicProperty( CIGAR, String.class, length + "M" ) );

        return new SiteImpl( null, name, SiteType.TYPE_RBS, Site.BASIS_PREDICTED, start, length, Precision.PRECISION_EXACTLY, strand,
                sequence, properties );
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        sequence = new LinearSequence( sequenceString, Nucleotide5LetterAlphabet.getInstance() );

        simpleSiteCollection = new VectorDataCollection<>( "siteTestCollection" );

        site1 = createSite( 5, 10, StrandType.STRAND_PLUS, "site1" );
        site2 = createSite( 6, 3, StrandType.STRAND_PLUS, "site2" );

        simpleSiteCollection.put( site1 );
        simpleSiteCollection.put( site2 );

        complexSiteCollection = new VectorDataCollection<>( "complexSiteCollection" );
        site3 = createSite( 1, 3, StrandType.STRAND_PLUS, "site3" );
        site4rev = createSite( 4, 3, StrandType.STRAND_MINUS, "site4rev" );

        complexSiteCollection.put( site1 );
        complexSiteCollection.put( site2 );
        complexSiteCollection.put( site3 );
        complexSiteCollection.put( site4rev );
    }

    public void testConstructor() throws Exception
    {
        ChromosomeClusters chrClusters = new ChromosomeClusters( simpleSiteCollection, sequence, minNumberSites, maxLengthCluster );

        assertTrue( chrClusters.empty() );

        final int expectedClusterSize = 0;
        assertEquals( expectedClusterSize, chrClusters.sizeClusters() );

        assertEquals( sequence, chrClusters.getChromosome() );
    }

    public void testRunClustering() throws Exception
    {
        ChromosomeClusters chrClusters = new ChromosomeClusters( simpleSiteCollection, sequence, minNumberSites, maxLengthCluster );
        chrClusters.runClustering();

        assertFalse( chrClusters.empty() );

        final int expectedClustersSize = 1;
        assertEquals( expectedClustersSize, chrClusters.sizeClusters() );

        SiteCluster cluster = chrClusters.getCluster( 0 );

        final int expectedSitesNumber = 2;
        assertEquals( expectedSitesNumber, cluster.getNumberOfSites() );

        final Interval site1Interval = site1.getInterval();
        final Interval site2Interval = site2.getInterval();
        final Interval expectedClusterInterval = site1Interval.union( site2Interval );
        assertEquals( expectedClusterInterval, cluster.getInterval() );
    }

    public void testSizeClusters() throws Exception
    {
        final ChromosomeClusters chrClusters = new ChromosomeClusters( simpleSiteCollection, sequence, minNumberSites, maxLengthCluster );

        final int expectedClusterSize = 0;
        assertEquals( expectedClusterSize, chrClusters.sizeClusters() );

        chrClusters.runClustering();

        final int expectedClusterSizeAfterRunning = 1;
        assertEquals( expectedClusterSizeAfterRunning, chrClusters.sizeClusters() );

        final DataCollection<Site> emptySiteCollection = new VectorDataCollection<>( "emptySiteCollection" );
        final ChromosomeClusters chrEmptySitesClusters = new ChromosomeClusters( emptySiteCollection, sequence, minNumberSites,
                maxLengthCluster );
        chrEmptySitesClusters.runClustering();

        final int expectedClusterSizeEmptyChr = 0;
        assertEquals( expectedClusterSizeEmptyChr, chrEmptySitesClusters.sizeClusters() );

        final ChromosomeClusters chrComplexSitesCollectionClusters = new ChromosomeClusters( complexSiteCollection, sequence,
                minNumberSites, maxLengthCluster );
        chrComplexSitesCollectionClusters.runClustering();

        final int expectedClusterSizeComplexSitesCollection = 3;
        assertEquals( expectedClusterSizeComplexSitesCollection, chrComplexSitesCollectionClusters.sizeClusters() );
    }

    public void testGetCluster() throws Exception
    {
        ChromosomeClusters chrClusters = new ChromosomeClusters( complexSiteCollection, sequence, minNumberSites, maxLengthCluster );
        chrClusters.runClustering();

        final int expectedClusterSize = 3;
        assertEquals( expectedClusterSize, chrClusters.sizeClusters() );

        final SiteCluster cluster1 = chrClusters.getCluster( 0 );
        final Interval site3Interval = site3.getInterval();
        assertEquals( site3Interval, cluster1.getInterval() );

        final SiteCluster cluster2 = chrClusters.getCluster( 1 );
        final Interval site1Interval = site1.getInterval();
        final Interval site2Interval = site2.getInterval();
        final Interval unionSites = site1Interval.union( site2Interval );
        assertEquals( unionSites, cluster2.getInterval() );

        final SiteCluster cluster3rev = chrClusters.getCluster( 2 );
        Interval site4Interval = site4rev.getInterval();
        assertEquals( site4Interval, cluster3rev.getInterval() );
    }

    public void testGetChromosome() throws Exception
    {
        ChromosomeClusters chrClusters = new ChromosomeClusters( simpleSiteCollection, sequence, minNumberSites, maxLengthCluster );

        assertEquals( sequence, chrClusters.getChromosome() );

        chrClusters.runClustering();

        assertEquals( sequence, chrClusters.getChromosome() );
    }

    public void testEmpty() throws Exception
    {
        ChromosomeClusters chrClusters = new ChromosomeClusters( simpleSiteCollection, sequence, minNumberSites, maxLengthCluster );

        assertTrue( chrClusters.empty() );

        chrClusters.runClustering();

        assertFalse( chrClusters.empty() );

        final DataCollection<Site> emptySiteCollection = new VectorDataCollection<>( "emptySiteCollection" );
        ChromosomeClusters chrEmptySitesClusters = new ChromosomeClusters( emptySiteCollection, sequence, minNumberSites, maxLengthCluster );
        chrEmptySitesClusters.runClustering();

        assertTrue( chrEmptySitesClusters.empty() );
    }

    public void testFilterClustersMinNumberSites() throws Exception
    {
        ChromosomeClusters chrClusters = new ChromosomeClusters( complexSiteCollection, sequence, minNumberSites, maxLengthCluster );
        chrClusters.runClustering();

        final int expectedClusterSize = 3;
        assertEquals( expectedClusterSize, chrClusters.sizeClusters() );

        chrClusters.filterClusters();

        final int expectedClusterSizeAfterFiltering = 1;
        assertEquals( expectedClusterSizeAfterFiltering, chrClusters.sizeClusters() );

        SiteCluster cluster = chrClusters.getCluster( 0 );
        assertTrue( cluster.getNumberOfSites() >= minNumberSites );
    }

    public void testFilterClustersMaxLengthCluster() throws Exception
    {
        final Interval site1Interval = site1.getInterval();
        final int lengthLessThanLongestCluster = site1Interval.getLength() - 1;



        final int unlimitedNumberSites = 0;
        ChromosomeClusters chrClusters = new ChromosomeClusters( complexSiteCollection, sequence, unlimitedNumberSites,
                lengthLessThanLongestCluster );
        chrClusters.runClustering();

        final int expectedClusterSize = 3;
        assertEquals( expectedClusterSize, chrClusters.sizeClusters() );

        chrClusters.filterClusters();

        final int expectedClusterSizeAfterFiltering = 2;
        assertEquals( expectedClusterSizeAfterFiltering, chrClusters.sizeClusters() );

        for( int i = 0; i < chrClusters.sizeClusters(); i++ )
        {
            final SiteCluster cluster = chrClusters.getCluster( i );
            final Interval clusterInterval = cluster.getInterval();

            assertFalse( clusterInterval.intersects( site1Interval ) );
        }
    }
}
