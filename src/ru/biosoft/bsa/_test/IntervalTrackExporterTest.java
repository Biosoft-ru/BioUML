package ru.biosoft.bsa._test;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetSupport;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.bsa.Precision;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SiteType;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.TrackImpl;
import ru.biosoft.bsa.TrackRegion;
import ru.biosoft.bsa.WritableTrack;
import ru.biosoft.bsa.exporter.IntervalTrackExporter;
import ru.biosoft.bsa.transformer.UnknownSequence;
import ru.biosoft.util.TempFile;
import ru.biosoft.util.TempFiles;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class IntervalTrackExporterTest extends AbstractBioUMLTest
{
    private static final String fileName = "result.interval";

    public void testWithoutHeader() throws Exception
    {
        final Site site = createSite( 10, 100, StrandType.STRAND_NOT_KNOWN, null );

        final WritableTrack track = createTrack();
        track.addSite( site );

        final String[][] expectedTokenArray = new String[][] {
                {"chr1", "9", "109"}};

        exportAndCheck( track, expectedTokenArray );
    }

    public void testHeaderOnlyStrand() throws Exception
    {
        final Site site = createSite( 10, 100, StrandType.STRAND_PLUS, null );

        final WritableTrack track = createTrack();
        track.addSite( site );

        final String[][] expectedTokenArray = new String[][] {
                {"#CHROM", "START", "END", "STRAND"},
                {"chr1", "9", "109", "+"}};

        exportAndCheck( track, expectedTokenArray );
    }

    public void testHeaderWithProperties() throws Exception
    {
        final DynamicPropertySet properties = new DynamicPropertySetSupport();
        properties.add( new DynamicProperty( "param", String.class, "testValue" ) );
        final Site site = createSite( 10, 100, StrandType.STRAND_PLUS, properties );

        final WritableTrack track = createTrack();
        track.addSite( site );

        final String[][] expectedTokenArray = new String[][] { 
                {"#CHROM", "START", "END", "STRAND", "param"},
                {"chr1", "9", "109", "+", "testValue"}};

        exportAndCheck( track, expectedTokenArray );
    }

    public void testHeaderPropertiesWithoutStrand() throws Exception
    {
        final DynamicPropertySet properties = new DynamicPropertySetSupport();
        properties.add( new DynamicProperty( "param", String.class, "testValue" ) );
        final Site site = createSite( 10, 100, StrandType.STRAND_NOT_KNOWN, properties );

        final WritableTrack track = createTrack();
        track.addSite( site );

        final String[][] expectedTokenArray = new String[][] { 
                {"#CHROM", "START", "END", "param"},
                {"chr1", "9", "109", "testValue"}};

        exportAndCheck( track, expectedTokenArray );
    }

    public void testSeveralSites() throws Exception
    {
        final Site site1 = createSite( 10, 100, StrandType.STRAND_NOT_KNOWN, null );
        final Site site2 = createSite( 20, 5, StrandType.STRAND_NOT_KNOWN, null );

        final WritableTrack track = createTrack();
        track.addSite( site1 );
        track.addSite( site2 );

        final String[][] expectedTokenArray = new String[][] {
                {"chr1", "9", "109"},
                {"chr1", "19", "24"}};

        exportAndCheck( track, expectedTokenArray );
    }

    public void testHeaderMissingValue() throws Exception
    {
        final DynamicPropertySet properties1 = new DynamicPropertySetSupport();
        properties1.add( new DynamicProperty( "param", String.class, "testValue" ) );
        final Site site1 = createSite( 10, 100, StrandType.STRAND_PLUS, properties1 );

        final DynamicPropertySet properties2 = new DynamicPropertySetSupport();
        properties2.add( new DynamicProperty( "param", String.class, "testValue2" ) );
        properties2.add( new DynamicProperty( "param2", String.class, "testValue3" ) );
        final Site site2 = createSite( 10, 100, StrandType.STRAND_NOT_KNOWN, properties2 );

        final WritableTrack track = createTrack();
        track.addSite( site1 );
        track.addSite( site2 );

        final String[][] expectedTokenArray = new String[][] {
        {"#CHROM", "START", "END", "STRAND", "param", "param2"},
        {"chr1", "9", "109", "+", "testValue", ""},
        {"chr1", "9", "109", "", "testValue2", "testValue3"}};

        exportAndCheck( track, expectedTokenArray );
    }

    private void exportAndCheck(final WritableTrack track, final String[][] expectedTokenArray) throws Exception
    {
        TrackRegion trackRegion = new TrackRegion( track );
        final IntervalTrackExporter exporter = new IntervalTrackExporter();
        
        try (TempFile intervalFile = TempFiles.file( fileName ))
        {
            exporter.doExport( trackRegion, intervalFile );
            assertEqualsDataSet( expectedTokenArray, intervalFile );
        }
    }

    private Site createSite(int start, int length, int strand, DynamicPropertySet properties)
    {
        final Sequence sequence = new UnknownSequence( "chr1", 0, 500 );

        final Site site = new SiteImpl( null, null, SiteType.TYPE_RBS, Site.BASIS_PREDICTED, start, length, Precision.PRECISION_EXACTLY,
                strand, sequence, properties );

        return site;
    }

    private WritableTrack createTrack() throws Exception
    {
        final Properties properties = new Properties();
        properties.setProperty( DataCollectionConfigConstants.NAME_PROPERTY, "" );

        final WritableTrack track = new TrackImpl( null, properties );

        return track;
    }

    private void assertEqualsDataSet(String[][] expectedTokenArray, File intervalFile) throws IOException
    {
        final String DELIMITER = "\t";
        final FileInputStream fileInputStream = new FileInputStream( intervalFile );
        try (final BufferedReader reader = new BufferedReader( new InputStreamReader( fileInputStream, StandardCharsets.UTF_8 ) ))
        {
            String line;
            int columnId;
            int lineId = 0;
            while( ( line = reader.readLine() ) != null )
            {
                columnId = 0;
                String[] fields = line.split( DELIMITER, -1 );
                for( String token : fields )
                {
                    final String expectedToken = expectedTokenArray[lineId][columnId];
                    assertEquals( expectedToken, token );
                    ++columnId;
                }
                ++lineId;
            }
        }
   }
}