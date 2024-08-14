package biouml.plugins.perfectosape._test;

import biouml.plugins.perfectosape.PerfectosapeAnalysis;
import biouml.plugins.perfectosape.PerfectosapeAnalysisParameters;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.bsa.Nucleotide15LetterAlphabet;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

/**
 * @author lan
 *
 */
public class TestPerfectosape extends AbstractBioUMLTest
{
    protected static DataCollection<FrequencyMatrix> createTestMatrixLib(VectorDataCollection<DataElement> vdc) throws Exception
    {
        VectorDataCollection<FrequencyMatrix> matrixLib = new VectorDataCollection<>("matrices", FrequencyMatrix.class, vdc);
        FrequencyMatrix testMatrix = new FrequencyMatrix(matrixLib, "AR1", Nucleotide15LetterAlphabet.getInstance(), null, new double[][] {
                {0.2368, 0.0969, 0.2687, 0.3976}, {0.1612, 0.2583, 0.3764, 0.2042}, {0.376, 0.226, 0.14, 0.258},
                {0.215, 0.258, 0.28, 0.247}, {0.3117, 0.4406, 0.1079, 0.1399}, {0.3656, 0.3976, 0.1079, 0.1289},
                {0.8811, 0.011, 0.043, 0.0649}, {0.043, 0.032, 0.903, 0.022}, {0.7842, 0.0969, 0.0649, 0.0539},
                {0.731, 0.183, 0.054, 0.032}, {0.0649, 0.8162, 0.0539, 0.0649}, {0.817, 0.043, 0.043, 0.097}, {0.226, 0.269, 0.43, 0.075},
                {0.204, 0.398, 0.194, 0.204}, {0.247, 0.183, 0.183, 0.387}, {0.043, 0.032, 0.011, 0.914}, {0.065, 0.118, 0.688, 0.129},
                {0.0649, 0.0969, 0.0649, 0.7732}, {0.0751, 0.0751, 0.043, 0.8068}, {0.022, 0.7952, 0.0649, 0.1179},
                {0.032, 0.108, 0.011, 0.849}, {0.1079, 0.2797, 0.3866, 0.2258}, {0.183, 0.591, 0.129, 0.097}, {0.29, 0.226, 0.215, 0.269},
                {0.2577, 0.2797, 0.1828, 0.2797}, {0.1828, 0.1508, 0.2797, 0.3866}, {0.032, 0.2797, 0.5375, 0.1508}}, false);
        matrixLib.put(testMatrix);
        vdc.put(matrixLib);
        return matrixLib;
    }

    public void testPerfectosape() throws Exception
    {
        VectorDataCollection<DataElement> vdc = new VectorDataCollection<>("test");
        CollectionFactory.registerRoot(vdc);
        DataCollection<FrequencyMatrix> matrixLib = createTestMatrixLib(vdc);
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(vdc, "input");
        table.getColumnModel().addColumn("Seq", String.class);
        TableDataCollectionUtils.addRow(table, "1", new Object[] {"AAGGTCAATACTCAACATCATAAAAACAGACAAAAGTATAAAACTTACAG[C/G]GTCTTACAAAAAGGATGATCCAGTAATATGCTGCTTACAAGAAACCCACC"});
        vdc.put(table);
        
        PerfectosapeAnalysis analysis = new PerfectosapeAnalysis(null, "");
        PerfectosapeAnalysisParameters parameters = analysis.getParameters();
        parameters.setMatrixLib(matrixLib.getCompletePath());
        parameters.setSeqTable(table.getCompletePath());
        analysis.justAnalyzeAndPut();
        assertEquals(100, analysis.getJobControl().getPreparedness());
        TableDataCollection output = parameters.getOutTable().getDataElement(TableDataCollection.class);
        assertEquals(1, output.getSize());
        RowDataElement row = output.getAt(0);
        assertNotNull(row);
        assertEquals("1", row.getName());
        assertEquals(matrixLib.getCompletePath().getChildPath("AR1"), row.getValue("Matrix"));
        assertEquals("1", row.getValue("SNP"));
        assertEquals(-22, row.getValue("Position 1"));
        assertEquals("+", row.getValue("Strand 1"));
        assertEquals("gacaaaagtataaaacttacagCgtct", row.getValue("Sequence 1"));
        assertEquals(-22, row.getValue("Position 2"));
        assertEquals("-", row.getValue("Strand 2"));
        assertEquals("agacCctgtaagttttatacttttgtc", row.getValue("Sequence 2"));
        assertEquals(0.01978219, (Double)row.getValue("P-value 1"), 0.00001);
        assertEquals(0.01550009, (Double)row.getValue("P-value 2"), 0.00001);
        assertEquals(1.276263, (Double)row.getValue("Fold change"), 0.00001);
        assertEquals(0.01550009, (Double)row.getValue("Min p-value"), 0.00001);
    }
    
}
