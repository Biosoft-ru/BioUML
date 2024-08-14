package biouml.plugins.riboseq._test;

import java.io.File;
import java.util.Properties;

import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.LinearSequence;
import ru.biosoft.bsa.MapAsVector;
import ru.biosoft.bsa.Nucleotide5LetterAlphabet;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.WritableTrack;
import ru.biosoft.bsa.importer.BEDTrackImporter;
import ru.biosoft.bsa.importer.TrackImportProperties;
import ru.biosoft.bsa.importer.TrackImporter;
import ru.biosoft.jobcontrol.FunctionJobControl;

public class BEDSitesTest extends AbstractBioUMLTest
{
    private static final String RESOURCES_PATH = "biouml/plugins/riboseq/_test/resources";
    private static final String TEST_FILE_NAME = "simpleBEDFile.bed";

    private Track BEDTrackFromFile;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        VectorDataCollection<DataElement> vdc = new VectorDataCollection<>( "test", DataCollection.class, null );
        CollectionFactory.registerRoot( vdc );

        VectorDataCollection<WritableTrack> tracks = new VectorDataCollection<>( "tracks", WritableTrack.class, vdc );
        vdc.put( tracks );

        VectorDataCollection<AnnotatedSequence> sequences = new VectorDataCollection<>( "sequences",
                AnnotatedSequence.class, vdc );
        vdc.put( sequences );
        sequences.put( new MapAsVector( "1", sequences, new LinearSequence( "", Nucleotide5LetterAlphabet.getInstance() ), null ) );

        BEDTrackFromFile = importFile( TEST_FILE_NAME, new BEDTrackImporter() );
    }

    protected Track importFile(String name, TrackImporter importer) throws Exception
    {
        File file = new File( RESOURCES_PATH, name );

        DataCollection<?> dc = DataElementPath.create( "test/tracks" ).getDataCollection();

        importer.init( new Properties() );
        TrackImportProperties properties = importer.getProperties( dc, file, name );
        properties.setSequenceCollectionPath( DataElementPath.create( "test/sequences" ) );

        FunctionJobControl fjc = new FunctionJobControl( null );
        importer.doImport( dc, file, name, fjc, null );

        assertEquals( 100, fjc.getPreparedness() );

        Track result = (Track)dc.get( name );

        assertNotNull( result );

        return result;
    }

    public void testGetAllSitesSize() throws Exception
    {
        DataCollection<Site> sites = BEDTrackFromFile.getAllSites();
        assertEquals( 7, sites.getSize() );
    }

    public void testGetSiteBySequenceSize() throws Exception
    {
        DataCollection<Site> sites = BEDTrackFromFile.getSites( "1", 1000, 1100 );
        assertEquals( 1, sites.getSize() );
    }

    public void testSiteDefinition() throws Exception
    {
        DataCollection<Site> sites = BEDTrackFromFile.getSites( "1", 1000, 1100 );
        Site site = getFirstSite( sites );

        Interval expectedInterval = new Interval( 1016, 1099 );
        assertEquals( expectedInterval, site.getInterval() );

        assertEquals( StrandType.STRAND_NOT_KNOWN, site.getStrand() );

        final double expectedScore = 244.002;
        final double delta = 0.001;
        assertEquals( expectedScore, site.getScore(), delta );
    }

    private Site getFirstSite(DataCollection<Site> siteDataCollection)
    {
        return siteDataCollection.iterator().next();
    }

    public void testSiteDefinition2() throws Exception
    {
        DataCollection<Site> sites = BEDTrackFromFile.getSites( "1", 10000, 10100 );
        Site site = getFirstSite( sites );

        Interval expectedInterval = new Interval( 10045, 10121 );
        assertEquals( expectedInterval, site.getInterval() );

        assertEquals( StrandType.STRAND_MINUS, site.getStrand() );
    }
}
