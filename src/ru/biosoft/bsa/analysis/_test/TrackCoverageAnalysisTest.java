package ru.biosoft.bsa.analysis._test;

import junit.framework.TestCase;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa._test.BSATestUtils;
import ru.biosoft.bsa.analysis.TrackCoverageAnalysis;
import ru.biosoft.bsa.analysis.TrackCoverageAnalysisParameters;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.TableDataCollection;

/**
 * @author lan
 *
 */
public class TrackCoverageAnalysisTest extends TestCase
{
    public void testTrackCoverageAnalysis() throws Exception
    {
        DataCollection<DataCollection<?>> repository = BSATestUtils.createRepository();
        VectorDataCollection<TableDataCollection> outputDC = new VectorDataCollection<>("output", StandardTableDataCollection.class, repository);
        repository.put(outputDC);
        
        TrackCoverageAnalysis analysis = new TrackCoverageAnalysis(null, "");
        TrackCoverageAnalysisParameters parameters = analysis.getParameters();
        parameters.setTrack(DataElementPath.create("databases/bam/small"));
        DataElementPath sequencesPath = DataElementPath.create("databases/Ensembl/Sequences/chromosomes NCBI36");
        assertEquals(sequencesPath, parameters.getSequences().getSequenceCollectionPath());
        assertEquals(DataElementPath.create("databases/bam/small coverage"), parameters.getOutput());
        parameters.setWindow(10000);
        parameters.setStep(1000);
        DataElementPath output = DataElementPath.create("databases/output/coverage");
        parameters.setOutput(output);
        analysis.setParameters(parameters);
        analysis.validateParameters();
        analysis.getJobControl().run();
        Object[] results = analysis.getAnalysisResults();
        assertEquals(1, results.length);
        TableDataCollection table = output.getDataElement(TableDataCollection.class);
        assertNotNull(table);
        assertEquals(table, results[0]);
        double[] coverage = new double[] {0.0609,0.5093,2.1371,3.6518,5.8595,6.0119,8.7315,10.5039,11.3655,13.7216,16.5783,16.1953,14.5675,13.0528,10.8451,10.6927,7.9731,6.2007,5.3391,2.983,0.0654};
        for(RowDataElement row: table)
        {
            Object[] values = row.getValues();
            assertEquals(5, values.length);
            assertEquals("1", values[0]);
            assertEquals(10000, values[3]);
            int pos = Integer.parseInt(row.getName());
            assertEquals(pos*1000+989001, values[1]);
            assertEquals((Integer)values[1]+10000-1, values[2]);
            assertEquals(coverage[pos-1], ((Number)values[4]).doubleValue(), 0.001);
        }
        
        DataCollection<AnnotatedSequence> sequencesCollection = sequencesPath.getDataCollection(AnnotatedSequence.class);
        analysis = new TrackCoverageAnalysis(null, "");
        parameters.setWindow(1000000000);
        parameters.setStep(1000000000);
        parameters.setOutputEmptyIntervals(true);
        analysis.setParameters(parameters);
        analysis.validateParameters();
        analysis.getJobControl().run();
        results = analysis.getAnalysisResults();
        assertEquals(1, results.length);
        table = output.getDataElement(TableDataCollection.class);
        for(RowDataElement row: table)
        {
            Object[] values = row.getValues();
            AnnotatedSequence sequence = sequencesCollection.get(values[0].toString());
            assertNotNull(sequence);
            assertEquals(1, values[1]);
            assertEquals(sequence.getSequence().getLength(), values[2]);
            assertEquals(sequence.getSequence().getLength(), values[3]);
            assertEquals(values[0].equals("1")?6.756165413478184E-4:0.0, ((Number)values[4]).doubleValue(), 0.0001);
        }
    }
}
