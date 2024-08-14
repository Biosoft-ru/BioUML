package biouml.plugins.bindingregions._test;

import java.beans.IntrospectionException;
import java.util.Properties;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.plugins.bindingregions.analysis.IPSInSNP;
import biouml.plugins.bindingregions.analysis.IPSInSNP.IPSInSNPParameters;
import biouml.plugins.bindingregions.analysis.SNPInBindingRegions;
import biouml.plugins.bindingregions.analysis.SNPInBindingRegions.SNPInBindingRegionsParameters;
import biouml.plugins.bindingregions.analysis.SNPRegionsInGenome;
import biouml.plugins.bindingregions.analysis.SNPRegionsInGenome.SNPRegionsInGenomeParameters;
import biouml.plugins.bindingregions.utils.BindingRegion;
import biouml.plugins.gtrd.TrackSqlTransformer;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Basis;
import ru.biosoft.bsa.Nucleotide5LetterAlphabet;
import ru.biosoft.bsa.Precision;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.SiteType;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.bsa.WritableTrack;
import ru.biosoft.bsa._test.BSATestUtils;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;

/**
 * @author lan
 *
 */
public class TestSNPAnalyses extends AbstractBioUMLTest
{
    public void testSNPRegionsInGenome() throws Exception
    {
        BSATestUtils.createRepository();
        DataElementPath sequences = DataElementPath.create("databases/Ensembl/Sequences").getChildren().first();

        VectorDataCollection<DataElement> vdc = new VectorDataCollection<>("test");
        CollectionFactory.registerRoot(vdc);
        createTestSNPTrack(vdc, sequences);

        SNPRegionsInGenome analysis = new SNPRegionsInGenome(null, "");
        SNPRegionsInGenomeParameters parameters = analysis.getParameters();
        parameters.getDbSelector().setSequenceCollectionPath(sequences);
        parameters.setSnpRegionLength(10);
        parameters.setSnpTrack(DataElementPath.create("test/input"));
        TableDataCollection result = analysis.justAnalyzeAndPut();
        assertEquals(100, analysis.getJobControl().getPreparedness());
        assertNotNull(result);
        
        assertEquals(1, result.getSize());
        RowDataElement row = result.get("rs3868004");
        assertNotNull(row);
        assertEquals("GCCCTTGACC", row.getValue("genomeRegionBeforeSnp"));
        assertEquals("GCCCTCGACC", row.getValue("genomeRegionAfterSnp"));
    }
    
    public void testSNPInBindingRegions() throws Exception
    {
        BSATestUtils.createRepository();
        DataElementPath sequences = DataElementPath.create("databases/Ensembl/Sequences").getChildren().first();

        VectorDataCollection<DataElement> vdc = new VectorDataCollection<>("test");
        CollectionFactory.registerRoot(vdc);
        createTestSNPTrack(vdc, sequences);
        createTestBindingRegionTrack(vdc, sequences);

        SNPInBindingRegions analysis = new SNPInBindingRegions(null, "");
        SNPInBindingRegionsParameters parameters = analysis.getParameters();
        parameters.setTrack(DataElementPath.create("test/br"));
        parameters.setSnpTrack(DataElementPath.create("test/input"));
        parameters.setTfNamesPath(null);
        TableDataCollection result = analysis.justAnalyzeAndPut();
        assertEquals(100, analysis.getJobControl().getPreparedness());
        assertNotNull(result);
        
        assertEquals(1, result.getSize());
        RowDataElement row = result.get("0");
        assertNotNull(row);
        assertEquals("rs3868004", row.getValues()[0]);
        assertEquals("1", row.getValues()[1]);
        assertEquals(310414, row.getValues()[2]);
        assertEquals(1, row.getValues()[3]);
        assertEquals("[\"2.3.3.50.1\"]", row.getValues()[4].toString());
        assertEquals("[\"2.3.3.50.1\"]", row.getValues()[5].toString());
    }
    
    public void testIPSinSNP() throws Exception
    {
        BSATestUtils.createRepository();
        DataElementPath sequences = DataElementPath.create("databases/Ensembl/Sequences").getChildren().first();

        VectorDataCollection<DataElement> vdc = new VectorDataCollection<>("test");
        CollectionFactory.registerRoot(vdc);
        createTestSNPTrack(vdc, sequences);
        createTestMatrixLib(vdc);
        
        IPSInSNP analysis = new IPSInSNP(null, "");
        IPSInSNPParameters parameters = analysis.getParameters();
        parameters.getDbSelector().setSequenceCollectionPath(sequences);
        parameters.setSnpTrack(DataElementPath.create("test/input"));
        parameters.setMatrixLibrary(DataElementPath.create("test/matrices"));
        TableDataCollection result = analysis.justAnalyzeAndPut();
        assertEquals(100, analysis.getJobControl().getPreparedness());
        assertNotNull(result);
        
        assertEquals(1, result.getSize());
        RowDataElement row = result.get("0");
        assertNotNull(row);
        Object[] values = row.getValues();
        assertNotNull(values);
        assertEquals("rs3868004", values[0]);
        assertEquals("1", values[1]);
        assertEquals(310414, values[2]);
        assertEquals("KLF4", values[3]);
        assertEquals(2.2397, (Double)values[4], 0.001);
        assertEquals(3.3805, (Double)values[5], 0.001);
        assertEquals((Double)values[5]-(Double)values[4], (Double)values[6], 0.00001);
    }
    
    public void createTestMatrixLib(VectorDataCollection<DataElement> vdc) throws Exception
    {
        VectorDataCollection<FrequencyMatrix> matrixLib = new VectorDataCollection<>("matrices", vdc, null);
        FrequencyMatrix testMatrix = new FrequencyMatrix(matrixLib, "KLF4", Nucleotide5LetterAlphabet.getInstance(), null, new double[][] {
                {0.053, 0.882, 0.032, 0.033}, {0.016, 0.955, 0.008, 0.021}, {0.761, 0.178, 0.021, 0.04}, {0.015, 0.946, 0.029, 0.01},
                {0.7207, 0.038, 0.1872, 0.0541}, {0.007, 0.949, 0.03, 0.014}, {0.013, 0.967, 0.009, 0.011}, {0.009, 0.959, 0.006, 0.026},
                {0.619, 0.076, 0.031, 0.274}, {0.1451, 0.2272, 0.4334, 0.1942}, {0.1758, 0.3636, 0.2348, 0.2258},
                {0.176, 0.312, 0.228, 0.284}, {0.171, 0.321, 0.23, 0.278}, {0.194, 0.297, 0.249, 0.26}, {0.2262, 0.2452, 0.2773, 0.2513},
                {0.204, 0.26, 0.317, 0.219}, {0.2268, 0.2308, 0.3117, 0.2308}, {0.215, 0.237, 0.303, 0.245}, {0.217, 0.213, 0.307, 0.263},
                {0.188, 0.248, 0.32, 0.244}}, false);
        matrixLib.put(testMatrix);
        vdc.put(matrixLib);
    }

    public void createTestSNPTrack(VectorDataCollection<DataElement> vdc, DataElementPath sequences) throws Exception, IntrospectionException
    {
        Properties properties = new Properties();
        properties.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, "input");
        properties.setProperty( Track.SEQUENCES_COLLECTION_PROPERTY, sequences.toString() );
        WritableTrack input = TrackUtils.createTrack(vdc, properties);
        Sequence sequence = sequences.getChildPath("1").getDataElement(AnnotatedSequence.class).getSequence();
        Site site = new SiteImpl(null, "1", SiteType.TYPE_VARIATION, Basis.BASIS_ANNOTATED, 310414, 1, Precision.PRECISION_EXACTLY, StrandType.STRAND_PLUS, sequence, null);
        site.getProperties().add(new DynamicProperty("RefAllele", String.class, "T"));
        site.getProperties().add(new DynamicProperty("AltAllele", String.class, "C"));
        site.getProperties().add(new DynamicProperty("name", String.class, "rs3868004"));
        input.addSite(site);
        input.finalizeAddition();
        vdc.put(input);
    }

    public void createTestBindingRegionTrack(VectorDataCollection<DataElement> vdc, DataElementPath sequences) throws Exception, IntrospectionException
    {
        Properties properties = new Properties();
        properties.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, "br");
        properties.setProperty( Track.SEQUENCES_COLLECTION_PROPERTY, sequences.toString() );
        WritableTrack input = TrackUtils.createTrack(vdc, properties);
        Sequence sequence = sequences.getChildPath("1").getDataElement(AnnotatedSequence.class).getSequence();
        Site site = new SiteImpl(null, "1", SiteType.TYPE_VARIATION, Basis.BASIS_ANNOTATED, 240326, 1650, Precision.PRECISION_EXACTLY, StrandType.STRAND_PLUS, sequence, null);
        site.getProperties().add(new DynamicProperty(TrackSqlTransformer.TF_CLASS_ID_PROPERTY, String.class, "2.3.3.50.1"));
        site.getProperties().add(new DynamicProperty(BindingRegion.NUMBER_OF_OVERLAPS, Integer.class, 0));
        input.addSite(site);
        site = new SiteImpl(null, "2", SiteType.TYPE_VARIATION, Basis.BASIS_ANNOTATED, 309926, 850, Precision.PRECISION_EXACTLY, StrandType.STRAND_PLUS, sequence, null);
        site.getProperties().add(new DynamicProperty(TrackSqlTransformer.TF_CLASS_ID_PROPERTY, String.class, "2.3.3.50.1"));
        site.getProperties().add(new DynamicProperty(BindingRegion.NUMBER_OF_OVERLAPS, Integer.class, 0));
        input.addSite(site);
        site = new SiteImpl(null, "3", SiteType.TYPE_VARIATION, Basis.BASIS_ANNOTATED, 311008, 126, Precision.PRECISION_EXACTLY, StrandType.STRAND_PLUS, sequence, null);
        site.getProperties().add(new DynamicProperty(TrackSqlTransformer.TF_CLASS_ID_PROPERTY, String.class, "3.5.1.1.1"));
        site.getProperties().add(new DynamicProperty(BindingRegion.NUMBER_OF_OVERLAPS, Integer.class, 0));
        input.addSite(site);
        input.finalizeAddition();
        vdc.put(input);
    }
}
