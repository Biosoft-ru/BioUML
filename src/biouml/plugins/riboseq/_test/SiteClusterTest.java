package biouml.plugins.riboseq._test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.bsa.BAMTrack;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.LinearSequence;
import ru.biosoft.bsa.Nucleotide5LetterAlphabet;
import ru.biosoft.bsa.Precision;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.StrandType;
import biouml.plugins.riboseq.datastructure.ClusterInfo;
import biouml.plugins.riboseq.datastructure.SiteCluster;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetSupport;

public class SiteClusterTest extends AbstractBioUMLTest
{
    public static final String sequenceString = "acgtatggacgtagcatcgatcgatcgtacacgtacgtac";
    Sequence sequence;

    List<Site> siteList = new ArrayList<>();

    Site defaultSite;
    SiteCluster defaultCluster;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        sequence = new LinearSequence( "seq1", sequenceString, Nucleotide5LetterAlphabet.getInstance() );

        DynamicPropertySet properties = new DynamicPropertySetSupport();
        final String readSequence = sequenceString.substring( ( 5 - 1 ), ( 5 - 1 ) + 3 );
        properties.add( new DynamicProperty( BAMTrack.READ_SEQUENCE, String.class, readSequence ) );
        properties.add( new DynamicProperty( BAMTrack.CIGAR_PROPERTY, String.class, "3M" ) );

        Site site = new SiteImpl( null, null, null, Site.BASIS_PREDICTED, 5, 3, Precision.PRECISION_EXACTLY, StrandType.STRAND_PLUS,
                sequence, properties );

        siteList.add( site );

        site = new SiteImpl( null, null, 7, 4, StrandType.STRAND_PLUS, sequence );
        siteList.add( site );

        site = new SiteImpl( null, null, 12, 5, StrandType.STRAND_MINUS, sequence );
        siteList.add( site );

        defaultSite = siteList.get( 0 );
        defaultCluster = new SiteCluster( defaultSite );
    }

    public void testSiteCluster() throws Exception
    {
        int expectedSitesNumber = 1;
        assertEquals( expectedSitesNumber, defaultCluster.getNumberOfSites() );

        Interval expectedInterval = defaultSite.getInterval();
        assertEquals( expectedInterval, defaultCluster.getInterval() );

        assertFalse( defaultCluster.isReversed() );
    }

    public void testAddSite() throws Exception
    {
        Site addingSite = siteList.get( 2 );
        defaultCluster.addSite( addingSite );

        final int expectedSitesNumber = 2;
        assertEquals( expectedSitesNumber, defaultCluster.getNumberOfSites() );

        Interval addingSiteInterval = addingSite.getInterval();
        Interval generatingSiteInterval = defaultSite.getInterval();

        Interval expectedUnitedInterval = generatingSiteInterval.union( addingSiteInterval );
        assertEquals( expectedUnitedInterval, defaultCluster.getInterval() );
    }

    public void testIsIntersects() throws Exception
    {
        assertTrue( defaultCluster.isIntersects( defaultSite ) );

        Site crossSite = siteList.get( 1 );

        assertTrue( defaultCluster.isIntersects( crossSite ) );

        final Site distantSite = siteList.get( 2 );

        assertFalse( defaultCluster.isIntersects( distantSite ) );
    }

    public void testGetInterval() throws Exception
    {
        Interval expectedInterval = defaultSite.getInterval();
        assertEquals( expectedInterval, defaultCluster.getInterval() );

        Site secondSite = siteList.get( 1 );
        defaultCluster.addSite( secondSite );

        Interval interval1 = defaultSite.getInterval();
        Interval interval2 = secondSite.getInterval();
        Interval expectedUnionInterval = interval1.union( interval2 );
        assertEquals( expectedUnionInterval, defaultCluster.getInterval() );
    }

    public void testIsReversed() throws Exception
    {
        Site minusSite = siteList.get( 2 );
        SiteCluster minusCluster = new SiteCluster( minusSite );

        assertFalse( defaultCluster.isReversed() );
        assertTrue( minusCluster.isReversed() );
    }

    public void testGetNumberOfSites() throws Exception
    {
        int expectedSitesNumberAfterInitialization = 1;
        assertEquals( expectedSitesNumberAfterInitialization, defaultCluster.getNumberOfSites() );

        Site secondSite = siteList.get( 1 );
        defaultCluster.addSite( secondSite );

        int expectedSitesNumberAfterAdding = 2;
        assertEquals( expectedSitesNumberAfterAdding, defaultCluster.getNumberOfSites() );

        Site thirdSite = siteList.get( 2 );
        defaultCluster.addSite( thirdSite );

        int expectedSitesNumber = 3;
        assertEquals( expectedSitesNumber, defaultCluster.getNumberOfSites() );
    }

    public void testGetSequenceAverageString1() throws Exception
    {
        final String readSequanceStr = ( (String)defaultSite.getProperties().getProperty( BAMTrack.READ_SEQUENCE ).getValue() )
                .toUpperCase();
        final String averageStr = defaultCluster.getSequenceAverageString( sequence );

        assertEquals( readSequanceStr, averageStr );
    }

    public void testCalculateInfoAndGetInfo() throws Exception
    {
        Site site = siteList.get( 0 );
        SiteCluster cluster = new SiteCluster( site );

        cluster.calculateInfo( sequence );

        assertNotNull( cluster.getInfo() );
    }

    public void testClusterInfo() throws Exception
    {
        defaultCluster.calculateInfo( sequence );

        final ClusterInfo info = defaultCluster.getInfo();
        final int fromCluster = defaultCluster.getInterval().getFrom();
        assertEquals( fromCluster, info.startModa );

        final int modasSize = 11;
        int[] expectedHistogram = new int[modasSize];
        expectedHistogram[modasSize / 2] = 1;
        final String expectedHistogramStr = Arrays.toString( expectedHistogram );
        final String actualHistogramStr = Arrays.toString( info.modasHistogram );
        assertEquals( expectedHistogramStr, actualHistogramStr );

        // because starting index is 1(not 0)
        final int beginIndex = defaultSite.getFrom() - 1;
        final int endIndex = defaultSite.getTo();
        final int startCodonOffset = sequenceString.substring( beginIndex, endIndex ).toUpperCase().indexOf( "ATG" );
        assertEquals( startCodonOffset, info.shiftStartCodon );
    }

    public void testClusterInfoInitCodonPosition() throws Exception
    {
        defaultCluster.calculateInfo( sequence );

        final ClusterInfo info = defaultCluster.getInfo();
        final Interval clusterInterval = defaultCluster.getInterval();
        final int expectedInitCodonPosition = clusterInterval.getFrom() + clusterInterval.getLength() / 2;
        assertEquals( expectedInitCodonPosition, info.initCodonPosition );

        final Site reversedSite = siteList.get( 2 );
        final SiteCluster reversedCluster = new SiteCluster( reversedSite );
        reversedCluster.calculateInfo( sequence );

        final ClusterInfo reversedInfo = reversedCluster.getInfo();
        final Interval reversedClusterInterval = reversedCluster.getInterval();
        final int expectedReversedInitCodonPosition = reversedClusterInterval.getTo() - reversedClusterInterval.getLength() / 2;
        assertEquals( expectedReversedInitCodonPosition, reversedInfo.initCodonPosition );
    }
}
