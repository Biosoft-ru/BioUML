package ru.biosoft.bsa._test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.WritableTrack;
import ru.biosoft.bsa.importer.IntervalTrackImporter;

public class IntervalTrackImporterTest extends AbstractBioUMLTest
{
    private static final String REPOSITORY_PATH = "../data/test/ru/biosoft/bsa/interval/";
    private static final String OUTPUT_TRACK_PATH = "output_tracks";

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        createRepository();
    }

    private void createRepository() throws Exception
    {
        VectorDataCollection<WritableTrack> vdc = new VectorDataCollection<>( OUTPUT_TRACK_PATH, WritableTrack.class, null );
        CollectionFactory.registerRoot( vdc );
    }

    public void testFromExample() throws Exception
    {
        final String fileNameStr = "galaxyExample.interval";
        final List<Site> siteList = getSiteListFromIntervalFile( fileNameStr );

        final int expectedSiteNumber = 2;
        assertEquals( expectedSiteNumber, siteList.size() );

        final Site firstSite = siteList.get( 0 );
        assertSiteMainValues( firstSite, "chr1", 10, 100 );
        assertEquals( StrandType.STRAND_PLUS, firstSite.getStrand() );
        final String[] attributeNames = {"NAME", "COMMENT"};
        final String[] attributeValues = {"exon", "myExon"};
        assertSiteAdditionValues( firstSite, attributeNames, attributeValues );
    }

    public void testWithoutHeader() throws Exception
    {
        final String fileNameStr = "withoutHeader.interval";
        final List<Site> siteList = getSiteListFromIntervalFile( fileNameStr );

        final int expectedSiteNumber = 2;
        assertEquals( expectedSiteNumber, siteList.size() );

        final Site firstSite = siteList.get( 0 );
        assertSiteMainValues( firstSite, "chrX", 10, 100 );

        final Site secondSite = siteList.get( 1 );
        assertSiteMainValues( secondSite, "chr1", 1000, 10050 );
    }

    public void testOnlyStrand() throws Exception
    {
        final String fileNameStr = "onlyStrand.interval";
        final List<Site> siteList = getSiteListFromIntervalFile( fileNameStr );

        final int expectedSiteNumber = 3;
        assertEquals( expectedSiteNumber, siteList.size() );

        final Site firstSite = siteList.get( 0 );
        assertSiteMainValues( firstSite, "chr1", 10, 100 );
        assertEquals( StrandType.STRAND_PLUS, firstSite.getStrand() );

        final Site secondSite = siteList.get( 1 );
        assertSiteMainValues( secondSite, "chr1", 10, 100 );
        assertEquals( StrandType.STRAND_MINUS, secondSite.getStrand() );

        final Site thirdSite = siteList.get( 2 );
        assertSiteMainValues( thirdSite, "chr1", 10, 100 );
        assertEquals( StrandType.STRAND_NOT_KNOWN, thirdSite.getStrand() );
    }

    public void testWithoutStrand() throws Exception
    {
        final String fileNameStr = "withoutStrand.interval";
        final List<Site> siteList = getSiteListFromIntervalFile( fileNameStr );

        final int expectedSiteNumber = 1;
        assertEquals( expectedSiteNumber, siteList.size() );

        final Site firstSite = siteList.get( 0 );
        assertSiteMainValues( firstSite, "chr1", 10, 100 );
        assertEquals( StrandType.STRAND_NOT_KNOWN, firstSite.getStrand() );
        final String[] attributeNames = {"NAME"};
        final String[] attributeValues = {"exon"};
        assertSiteAdditionValues( firstSite, attributeNames, attributeValues );
    }

    public void testSkipValues() throws Exception
    {
        final String fileNameStr = "skipValues.interval";
        final List<Site> siteList = getSiteListFromIntervalFile( fileNameStr );

        final int expectedSiteNumber = 2;
        assertEquals( expectedSiteNumber, siteList.size() );

        final Site firstSite = siteList.get( 0 );
        assertSiteMainValues( firstSite, "chr1", 10, 100 );
        assertEquals( StrandType.STRAND_NOT_KNOWN, firstSite.getStrand() );
        final String[] attributeNames1 = {"NAME"};
        final String[] attributeValues1 = {"exon"};
        assertSiteAdditionValues( firstSite, attributeNames1, attributeValues1 );

        final Site secondSite = siteList.get( 1 );
        assertSiteMainValues( secondSite, "chrX", 10, 100 );
        assertEquals( StrandType.STRAND_MINUS, secondSite.getStrand() );
        final String[] attributeNames2 = {"NAME"};
        final String[] attributeValues2 = {""};
        assertSiteAdditionValues( secondSite, attributeNames2, attributeValues2 );
    }

    public void testNumberField() throws Exception
    {
        final String fileNameStr = "numberValues.interval";
        final List<Site> siteList = getSiteListFromIntervalFile( fileNameStr );

        final Site site = siteList.get( 0 );
        assertSiteMainValues( site, "chr1", 10, 100 );
        assertEquals( StrandType.STRAND_PLUS, site.getStrand() );
        final String[] attributeNames = {"numberInteger", "numberDouble"};
        final Object[] attributeValues = {2, 2.02};
        assertSiteAdditionValues( site, attributeNames, attributeValues );
    }

    public void testCastStringValue() throws Exception
    {
        Object actualObject = IntervalTrackImporter.detectType( "5" );
        assertEquals( Integer.class, actualObject.getClass() );

        actualObject = IntervalTrackImporter.detectType( "5.02" );
        assertEquals( Double.class, actualObject.getClass() );

        actualObject = IntervalTrackImporter.detectType( "text" );
        assertEquals( String.class, actualObject.getClass() );
    }

    private List<Site> getSiteListFromIntervalFile(String fileNameStr) throws Exception
    {
        final Track track = importTrackFromIntervalFile( fileNameStr );
        if( track == null )
        {
            throw new Exception( "Track is null" );
        }

        return getSiteList( track );
    }

    private Track importTrackFromIntervalFile(String importingFileName) throws Exception
    {
        final File importingFile = new File( REPOSITORY_PATH, importingFileName );

        final IntervalTrackImporter importer = new IntervalTrackImporter();
        importer.init( new Properties() );

        final DataCollection<?> dataCollection = CollectionFactory.getDataCollection( OUTPUT_TRACK_PATH );
        assertNotNull( dataCollection );

        importer.doImport( dataCollection, importingFile, importingFileName, null, null );

        final Track track = (Track)dataCollection.get( importingFileName );
        assertNotNull( track );

        return track;
    }

    private List<Site> getSiteList(Track track)
    {
        final DataCollection<Site> siteDataCollection = track.getAllSites();
        final List<Site> siteList = new ArrayList<>();
        for( Site site : siteDataCollection )
        {
            siteList.add( site );
        }

        return siteList;
    }

    private void assertSiteMainValues(Site site, String chrom, int intervalFrom, int intervalEnd)
    {
        String chrName = getNormalizedChrName( chrom );

        assertEquals( chrName, site.getSequence().getName() );
        assertEquals( intervalFrom + 1, site.getFrom() );
        assertEquals( intervalEnd, site.getTo() );

        final boolean isReversed = site.getStrand() == StrandType.STRAND_MINUS;
        final int expectedStart = isReversed ? intervalEnd : intervalFrom + 1;
        assertEquals( expectedStart, site.getStart() );
        assertEquals( intervalEnd - intervalFrom, site.getLength() );
    }

    private String getNormalizedChrName(String chrom)
    {
        String chrName = chrom;
        if( chrName.startsWith( "chr" ) )
            chrName = chrName.substring( "chr".length() );
        chrName = chrName.equals( "M" ) ? "MT" : chrName;
        return chrName;
    }

    private void assertSiteAdditionValues(Site site, String[] attributeNameArray, Object[] valueArray)
    {
        for( int i = 0; i < attributeNameArray.length; i++ )
        {
            final Object expectedValue = valueArray[i];
            final Object actualValue = site.getProperties().getValue( attributeNameArray[i] );

            assertEquals( expectedValue.getClass(), actualValue.getClass() );
            assertEquals( expectedValue, actualValue );
        }
    }
}
