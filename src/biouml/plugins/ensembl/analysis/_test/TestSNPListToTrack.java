package biouml.plugins.ensembl.analysis._test;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.developmentontheedge.log.WriterHandler;

import biouml.plugins.ensembl.analysis.SNPListToTrack;
import biouml.plugins.ensembl.analysis.SNPListToTrackParameters;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa._test.BSATestUtils;
import ru.biosoft.bsa.snp.SNPTableType;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

/**
 * @author lan
 *
 */
public class TestSNPListToTrack extends AbstractBioUMLTest
{
    public void testSNPTableType()
    {
        SNPTableType snpTableType = new SNPTableType();
        assertEquals(ReferenceType.SCORE_HIGH_SPECIFIC, snpTableType.getIdScore("rs12345"));
        assertEquals(ReferenceType.SCORE_NOT_THIS_TYPE, snpTableType.getIdScore("rs12345d"));
    }

    public void testSNPListToTrack() throws Exception
    {
        BSATestUtils.createRepository();
        VectorDataCollection<DataElement> vdc = new VectorDataCollection<>("test");
        CollectionFactory.registerRoot(vdc);

        TableDataCollection inputTable = TableDataCollectionUtils.createTableDataCollection(vdc, "input");
        inputTable.getColumnModel().addColumn("num", Integer.class);
        Map<String, Object[]> expected = new HashMap<>();
        expected.put("rs10010325", new Object[] {"ENSG00000168769", "TET2", "Intron", "4", 106325802, "C/A", "+"});
        expected.put("rs10037512", new Object[] {null, null, null, "5", 88390431, "T/C", "+"});
        expected.put("rs1468758", new Object[] {"ENSG00000198121", "LPAR1", "5' region (promoter)", "9", 112846903, "C/T", "+"});
        expected.put("rs1047014", new Object[] {"ENSG00000172201", "ID4", "3' region", "6", 19949472, "T/C", "+"});
        expected.put("rs1043515", new Object[] {"ENSG00000141720", "PIP5K2B", "Exon", "17", 34175722, "A/G", "+"});
        expected.put("rs1046934", new Object[] {"ENSG00000198860", "TSEN15", "Exon", "1", 182290152, "A/C", "+"});
        expected.put("rs1046943", new Object[] {"ENSG00000112365", "ZBTB24", "Exon", "6", 109890634, "A/G", "+"});
        expected.put("dummy", new Object[] {null, null, null, null, null, null, null});

        int rowNum=0;
        for(String row: expected.keySet())
        {
            TableDataCollectionUtils.addRow(inputTable, row, new Object[] {++rowNum});
        }
        inputTable.setReferenceType(ReferenceTypeRegistry.getReferenceType(SNPTableType.class).toString());
        vdc.put(inputTable);

        SNPListToTrack analysis = new SNPListToTrack(null, "");
        SNPListToTrackParameters parameters = analysis.getParameters();
        parameters.setSourcePath(inputTable.getCompletePath());
        parameters.setOutputNonMatched(true);
        parameters.setFivePrimeSize(10000);
        parameters.setThreePrimeSize(10000);
        parameters.setColumn("num");
        Logger logger = Logger.getLogger("analysis-test-logger");
        Handler[] handlers = logger.getHandlers();
        for( Handler h : handlers )
        {
            logger.removeHandler( h );
        }
        Writer writer = new StringWriter();
        logger.addHandler( new WriterHandler( writer ) );
        logger.setLevel(Level.INFO);
        analysis.setLogger(logger);
        analysis.justAnalyzeAndPut();
        assertEquals(100, analysis.getJobControl().getPreparedness());

        Track track = parameters.getDestPath().getDataElement(Track.class);
        DataCollection<Site> sites = track.getAllSites();
        assertEquals(expected.size()-1, sites.getSize());
        boolean found = false;
        for(Site site: sites)
        {
            String snpName = site.getProperties().getValueAsString("name");
            assertNotNull(snpName);
            assertTrue(snpName.startsWith("rs"));
            assertTrue(expected.containsKey(snpName));
            assertEquals(1, site.getLength());
            if(snpName.equals("rs10010325"))
            {
                found = true;
                assertEquals("4", site.getSequence().getName());
            }
        }
        assertTrue(found);

        TableDataCollection annotatedTable = parameters.getAnnotatedPath().getDataElement(TableDataCollection.class);
        assertEquals(expected.size()-1, annotatedTable.getSize());
        assertEquals(ReferenceTypeRegistry.getReferenceType(SNPTableType.class).toString(), annotatedTable.getReferenceType());
        TableColumn columnInfo = annotatedTable.getColumnModel().getColumn("num");
        assertNotNull(columnInfo);
        assertEquals(Integer.class, columnInfo.getValueClass());
        for(RowDataElement row: annotatedTable)
        {
            Object[] values = expected.get(row.getName());
            assertNotNull(values);
            assertEquals(values[0], row.getValue("Ensembl Id"));
            assertEquals(values[1], row.getValue("Gene symbol"));
            assertEquals(values[2], row.getValue("Location"));
            assertEquals(values[3], row.getValue("SNP_matching-Chromosome"));
            assertEquals(values[4], row.getValue("SNP_matching-Position"));
            assertEquals(values[5], row.getValue("SNP_matching-Allele"));
            assertEquals(values[6], row.getValue("SNP_matching-Strand"));
        }

        TableDataCollection genesTable = parameters.getOutputGenes().getDataElement(TableDataCollection.class);
        assertNotNull(genesTable);
        Set<String> expectedGenes = new HashSet<>(Arrays.asList("ENSG00000108294", "ENSG00000112365", "ENSG00000135596",
                "ENSG00000141720", "ENSG00000168769", "ENSG00000172201", "ENSG00000198121", "ENSG00000198860"));
        assertEquals(expectedGenes.size(), genesTable.getSize());
        for(RowDataElement row: genesTable)
        {
            assertTrue(expectedGenes.contains(row.getName()));
        }
    }
}
