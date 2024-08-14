package ru.biosoft.bsa._test;

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
import ru.biosoft.bsa.importer.BreakDancerTrackImporter;
import ru.biosoft.bsa.importer.GFFTrackImporter;
import ru.biosoft.bsa.importer.GenotypeTrackImporter;
import ru.biosoft.bsa.importer.SissrsTrackImporter;
import ru.biosoft.bsa.importer.TrackImportProperties;
import ru.biosoft.bsa.importer.TrackImporter;
import ru.biosoft.bsa.importer.VCFTrackImporter;
import ru.biosoft.jobcontrol.FunctionJobControl;

/**
 * @author lan
 *
 */
public class ImportTest extends AbstractBioUMLTest
{
    private static final String RESOURCES_PATH = "ru/biosoft/bsa/_test/resources";
    private static String[] TEST_FILES = {"import_bed.bed", "import_breakdancer.ctx", "import_cnvnator.genotype", "import_ctcf.bsites",
            "import_gff.gff", "import_pindel.vcf", "import_sisrs.bsites"};

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        VectorDataCollection<DataElement> vdc = new VectorDataCollection<>("test", DataCollection.class, null);
        CollectionFactory.registerRoot(vdc);

        VectorDataCollection<WritableTrack> tracks = new VectorDataCollection<>("tracks", WritableTrack.class, vdc);
        vdc.put(tracks);

        VectorDataCollection<AnnotatedSequence> sequences = new VectorDataCollection<>("sequences", AnnotatedSequence.class, vdc);
        vdc.put(sequences);
        sequences.put(new MapAsVector("1", sequences, new LinearSequence("", Nucleotide5LetterAlphabet.getInstance()), null));
        sequences.put(new MapAsVector("2", sequences, new LinearSequence("", Nucleotide5LetterAlphabet.getInstance()), null));
        sequences.put(new MapAsVector("3", sequences, new LinearSequence("", Nucleotide5LetterAlphabet.getInstance()), null));
    }

    protected Track importFile(String name, TrackImporter importer) throws Exception
    {
        importer.init(new Properties());
        File file = new File(RESOURCES_PATH, name);
        assertNotNull(file);
        DataCollection<?> dc = CollectionFactory.getDataCollection( "test/tracks" );
        assertTrue(importer.accept(dc, null) > TrackImporter.ACCEPT_UNSUPPORTED);
        String suffix = name.substring(name.lastIndexOf('.'));
        for(String testFileName: TEST_FILES)
        {
            String testSuffix = testFileName.substring(testFileName.lastIndexOf('.'));
            if(!testSuffix.equals(suffix))
            {
                File testFile = new File(RESOURCES_PATH, testFileName);
                assertEquals(testFileName+" is unsupported by "+importer.getClass().getSimpleName(), TrackImporter.ACCEPT_UNSUPPORTED, importer.accept(dc, testFile));
            }
        }
        assertTrue(importer.accept(dc, file) > TrackImporter.ACCEPT_UNSUPPORTED);
        TrackImportProperties properties = importer.getProperties(dc, file, name);
        properties.setSequenceCollectionPath(DataElementPath.create("test/sequences"));
        FunctionJobControl fjc = new FunctionJobControl(null);
        importer.doImport(dc, file, name, fjc, null);
        assertEquals(100, fjc.getPreparedness());
        Track result = (Track)dc.get(name);
        assertNotNull(result);
        return result;
    }

    public void testImportBED() throws Exception
    {
        Track track = importFile("import_bed.bed", new BEDTrackImporter());
        DataCollection<Site> sites = track.getAllSites();
        assertEquals(7, sites.getSize());
        sites = track.getSites("1", 1000, 1100);
        assertEquals(1, sites.getSize());
        Site site = sites.iterator().next();
        assertEquals(new Interval(1016,1099), site.getInterval());
        assertEquals(StrandType.STRAND_NOT_KNOWN, site.getStrand());
        assertEquals(244.002, site.getScore(), 0.001);
        sites = track.getSites("1", 10000, 10100);
        assertEquals(1, sites.getSize());
        site = sites.iterator().next();
        assertEquals(new Interval(10045,10121), site.getInterval());
        assertEquals(StrandType.STRAND_MINUS, site.getStrand());
    }

    public void testImportGFF() throws Exception
    {
        Track track = importFile("import_gff.gff", new GFFTrackImporter());
        DataCollection<Site> sites = track.getAllSites();
        assertEquals(3, sites.getSize());
        sites = track.getSites("2", 10000000, 10001000);
        assertEquals(1, sites.getSize());
        Site site = sites.iterator().next();
        assertEquals("enhancer", site.getType());
        assertEquals(new Interval(10000000, 10001000), site.getInterval());
        assertEquals(StrandType.STRAND_PLUS, site.getStrand());
        sites = track.getSites("extraseq", 10000000, 20000000);
        assertEquals(1, sites.getSize());
    }

    public void testImportBeakdancer() throws Exception
    {
        Track track = importFile("import_breakdancer.ctx", new BreakDancerTrackImporter());
        DataCollection<Site> sites = track.getAllSites();
        assertEquals(22, sites.getSize());
        int found=0;
        for(Site site: sites)
        {
            assertTrue(site.getOriginalSequence().getName().equals("1") || site.getOriginalSequence().getName().equals("2"));
            if(site.getInterval().equals(new Interval(10016, 10532)))
            {
                found++;
                assertEquals("ITX", site.getType());
                assertEquals("338+7-", site.getProperties().getValue("Orientation1"));
                assertEquals("338+7-", site.getProperties().getValue("Orientation1"));
                assertEquals("94", site.getProperties().getValue("Score"));
                assertEquals("6", site.getProperties().getValue("NumReads"));
            }
            if(site.getInterval().equals(new Interval(46802857)))
            {
                found++;
                assertEquals("1", site.getOriginalSequence().getName());
                assertEquals("2", site.getProperties().getValue("Chromosome2"));
                assertEquals("CTX (from)", site.getType());
            }
            if(site.getInterval().equals(new Interval(2539702)))
            {
                found++;
                assertEquals("2", site.getOriginalSequence().getName());
                assertEquals("1", site.getProperties().getValue("Chromosome2"));
                assertEquals("CTX (to)", site.getType());
            }
        }
        assertEquals(3, found);
    }

    public void testImportCNVNator() throws Exception
    {
        Track track = importFile("import_cnvnator.genotype", new GenotypeTrackImporter());
        DataCollection<Site> sites = track.getAllSites();
        assertEquals(16, sites.getSize());
        int found=0;
        for(Site site: sites)
        {
            assertEquals("5173N.root", site.getProperties().getValue("Source"));
            if(site.getInterval().equals(new Interval(1, 16600)))
            {
                found++;
                assertEquals("587.294", site.getProperties().getValue("CNV"));
                assertEquals("570.388", site.getProperties().getValue("CNV_low"));
                assertEquals("MT", site.getOriginalSequence().getName());
            }
            if(site.getInterval().equals(new Interval(49601, 51400)))
            {
                found++;
                assertEquals("0.189224", site.getProperties().getValue("CNV"));
                assertEquals("0.183777", site.getProperties().getValue("CNV_low"));
                assertEquals("1", site.getOriginalSequence().getName());
            }
        }
        assertEquals(2, found);
    }

    public void testImportVCF() throws Exception
    {
        Track track = importFile("import_pindel.vcf", new VCFTrackImporter());
        DataCollection<Site> sites = track.getAllSites();
        assertEquals(21, sites.getSize());
        Properties properties = ((DataCollection<?>)track).getInfo().getProperties();
        assertEquals("Integer:\"End position of the variant described in this record\"", properties.getProperty("Info_END"));
        int found=0;
        for(Site site: sites)
        {
            assertEquals("PASS", site.getProperties().getValue("Filter"));
            if(site.getOriginalSequence().getName().equals("MT") && site.getFrom() == 12868)
            {
                found++;
                assertEquals(2111, site.getLength());
                assertEquals( 15, site.getProperties().getValue( "Format_AD_5173N" ) );
                assertEquals( "1/.", site.getProperties().getValue( "Format_GT_5173N" ) );
                assertEquals("INV", site.getProperties().getValue("Info_SVTYPE"));
            }
            if(site.getOriginalSequence().getName().equals("MT") && site.getFrom() == 65)
            {
                found++;
                assertEquals(1, site.getLength());
                assertEquals( 12, site.getProperties().getValue( "Format_AD_5173N" ) );
                assertEquals( "1/.", site.getProperties().getValue( "Format_GT_5173N" ) );
                assertEquals("TG", site.getProperties().getValue("AltAllele"));
                assertEquals("T", site.getProperties().getValue("RefAllele"));
                assertEquals("INS", site.getProperties().getValue("Info_SVTYPE"));
            }
        }
        assertEquals(2, found);
    }

    public void testImportSISSRs() throws Exception
    {
        Track track = importFile("import_ctcf.bsites", new SissrsTrackImporter());
        DataCollection<Site> sites = track.getAllSites();
        assertEquals(257, sites.getSize());
        int found=0;
        for(Site site: sites)
        {
            assertEquals("1", site.getOriginalSequence().getName());
            assertEquals(1, site.getLength());
            assertEquals(1, site.getFrom()%10);
            if(site.getFrom() == 227611)
            {
                found++;
                assertEquals("11", site.getProperties().getValue("tags"));
            }
            if(site.getFrom() == 6227891)
            {
                found++;
                assertEquals("56", site.getProperties().getValue("tags"));
            }
        }
        assertEquals(2, found);

        track = importFile("import_sisrs.bsites", new SissrsTrackImporter());
        sites = track.getAllSites();
        assertEquals(9, sites.getSize());
        found=0;
        for(Site site: sites)
        {
            assertEquals(0, site.getLength()%10);
            assertEquals(1, site.getFrom()%10);
            if(site.getFrom() == 35060071)
            {
                found++;
                assertEquals(40, site.getLength());
                assertEquals(14, Integer.parseInt(site.getProperties().getValue("tags").toString()));
                assertEquals(12.30, Double.parseDouble(site.getProperties().getValue("fold").toString()));
                assertEquals(4.0e-06, Double.parseDouble(site.getProperties().getValue("p-value").toString()));
            }
            if(site.getFrom() == 119721111)
            {
                found++;
                assertEquals(40, site.getLength());
                assertEquals(15, Integer.parseInt(site.getProperties().getValue("tags").toString()));
                assertEquals(6.59, Double.parseDouble(site.getProperties().getValue("fold").toString()));
                assertEquals(8.6e-04, Double.parseDouble(site.getProperties().getValue("p-value").toString()));
            }
        }
        assertEquals(2, found);
    }
}
