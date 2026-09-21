package ru.biosoft.analysis._test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestSuite;
import ru.biosoft.access.FolderVectorCollection;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.CRClusterAnalysis;
import ru.biosoft.analysis.CRClusterAnalysisParameters;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.columnbeans.Column;
import ru.biosoft.table.columnbeans.ColumnGroup;

/**
 * Tests the CRC (Chinese Restaurant Clustering) algorithm implementation.
 *
 * The algorithm is a stochastic Gibbs-like sampler, so exact cluster numbering is not reproducible.
 * The tests therefore check the properties the algorithm must guarantee: genes with (nearly) identical
 * expression profiles must be grouped together, genes with well separated profiles must be split apart,
 * posterior probabilities must be well formed and must drive the output cutoff.
 * Input data below are well separated so that the posterior is concentrated on the true partition;
 * this makes the assertions stable (verified over hundreds of runs).
 *
 * @author axec
 */
public class CRClusterAnalysisTest extends AbstractBioUMLTest
{
    private static final String ROOT = "test";
    private static final int COLUMN_COUNT = 8;
    private static final int GROUP_SIZE = 6;

    /** Deviation of a single measurement from its group level: much smaller than the distance between groups. */
    private static final double NOISE = 0.1;

    private FolderVectorCollection root;

    public CRClusterAnalysisTest(String name)
    {
        super(name);
    }

    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite(CRClusterAnalysisTest.class.getName());
        suite.addTest(new CRClusterAnalysisTest("testTwoDistinctProfileGroups"));
        suite.addTest(new CRClusterAnalysisTest("testThreeDistinctProfileGroups"));
        suite.addTest(new CRClusterAnalysisTest("testMissingValuesAreTolerated"));
        suite.addTest(new CRClusterAnalysisTest("testDefaultCutoffKeepsWellSeparatedGenes"));
        suite.addTest(new CRClusterAnalysisTest("testCutoffAboveOneRejectsEverything"));
        suite.addTest(new CRClusterAnalysisTest("testOutputTableStructure"));
        suite.addTest(new CRClusterAnalysisTest("testInversionMergesMirroredProfiles"));
        suite.addTest(new CRClusterAnalysisTest("testNoGeneIsSilentlyDropped"));
        suite.addTest(new CRClusterAnalysisTest("testSingleLogLikelihood"));
        suite.addTest(new CRClusterAnalysisTest("testValidateParameters"));
        return suite;
    }

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        root = new FolderVectorCollection(ROOT, null);
        CollectionFactory.registerRoot(root);
    }

    /**
     * Two groups of six genes, group levels 10 and -10. The algorithm must recover exactly these two groups.
     */
    public void testTwoDistinctProfileGroups() throws Exception
    {
        double[] levels = {10, -10};
        TableDataCollection result = analyze("two", profiles(levels), false, -1.0);

        assertEquals("All genes must be assigned to a cluster", levels.length * GROUP_SIZE, result.getSize());
        assertPartition(result, levels.length);
    }

    /**
     * Three groups of six genes, group levels -20, 0 and 20.
     */
    public void testThreeDistinctProfileGroups() throws Exception
    {
        double[] levels = { -20, 0, 20};
        TableDataCollection result = analyze("three", profiles(levels), false, -1.0);

        assertEquals("All genes must be assigned to a cluster", levels.length * GROUP_SIZE, result.getSize());
        assertPartition(result, levels.length);
    }

    /**
     * Single missing measurements must be skipped by the likelihood instead of breaking the clustering,
     * and must be passed through to the output table.
     */
    public void testMissingValuesAreTolerated() throws Exception
    {
        double[] levels = {10, -10};
        double[][] data = profiles(levels);
        data[1][2] = Double.NaN;
        data[4][0] = Double.NaN;
        data[7][5] = Double.NaN;
        data[10][3] = Double.NaN;

        TableDataCollection result = analyze("missing", data, false, -1.0);

        assertEquals("All genes must be assigned to a cluster", levels.length * GROUP_SIZE, result.getSize());
        assertPartition(result, levels.length);

        Object[] row = TableDataCollectionUtils.getRowValues(result, geneName(1));
        assertEquals("Missing value must be kept in the output", Double.NaN, ((Number)row[2 + 2]).doubleValue(), 0.0);
    }

    /**
     * For well separated groups every gene belongs to its cluster with posterior probability close to 1,
     * so the default cutoff (0.9) must not discard anything.
     */
    public void testDefaultCutoffKeepsWellSeparatedGenes() throws Exception
    {
        double[] levels = {10, -10};
        TableDataCollection result = analyze("cutoffDefault", profiles(levels), false, null);

        assertEquals("Default cutoff must not discard well separated genes", levels.length * GROUP_SIZE, result.getSize());
        for( String name : result.getNameList() )
        {
            double probability = (Double)TableDataCollectionUtils.getRowValues(result, name)[1];
            assertTrue(name + ": probability " + probability + " is out of [0, 1]", probability >= 0 && probability <= 1);
            assertTrue(name + ": probability " + probability + " is too low for a well separated gene", probability > 0.99);
        }
    }

    /**
     * A posterior probability can never exceed 1, so a cutoff above 1 must reject every gene
     * while still producing a properly structured (empty) table.
     */
    public void testCutoffAboveOneRejectsEverything() throws Exception
    {
        TableDataCollection result = analyze("cutoffAll", profiles(new double[] {10, -10}), false, 1.5);

        assertEquals("No gene may pass a cutoff above 1", 0, result.getSize());
        assertTrue("Empty result must still have the Cluster column", result.getColumnModel().hasColumn("Cluster"));
    }

    /**
     * Output table layout: cluster and probability first, then the analyzed columns of the input table.
     * The Sign column is present only when inversion is allowed.
     */
    public void testOutputTableStructure() throws Exception
    {
        ColumnModel model = analyze("structure", profiles(new double[] {10, -10}), false, -1.0).getColumnModel();
        assertEquals("Column count", 2 + COLUMN_COUNT, model.getColumnCount());
        assertColumn(model, 0, "Cluster", Integer.class);
        assertColumn(model, 1, "Probability", Double.class);
        for( int i = 0; i < COLUMN_COUNT; i++ )
            assertColumn(model, 2 + i, columnName(i), Double.class);

        ColumnModel invertedModel = analyze("structureInverted", profiles(new double[] {10, -10}), true, -1.0).getColumnModel();
        assertEquals("Column count with inversion", 3 + COLUMN_COUNT, invertedModel.getColumnCount());
        assertColumn(invertedModel, 0, "Cluster", Integer.class);
        assertColumn(invertedModel, 1, "Probability", Double.class);
        assertColumn(invertedModel, 2, "Sign", String.class);
    }

    /**
     * With inversion allowed a profile and its mirror image are equivalent, so mirrored profiles that
     * are split in two clusters without inversion must be merged into a single one - with every gene
     * still reported, and with the Sign column marking exactly one of the two mirrored halves.
     */
    public void testInversionMergesMirroredProfiles() throws Exception
    {
        double[][] data = new double[2 * GROUP_SIZE][COLUMN_COUNT];
        for( int i = 0; i < data.length; i++ )
        {
            for( int j = 0; j < COLUMN_COUNT; j++ )
            {
                double value = ( j % 2 == 0 ? 10 : -10 ) + noise(i, j);
                // second half of the genes is the mirror image of the first half
                data[i][j] = i < GROUP_SIZE ? value : -value;
            }
        }

        TableDataCollection withoutInversion = analyze("mirror", data, false, -1.0);
        assertEquals("All genes must be assigned to a cluster", 2 * GROUP_SIZE, withoutInversion.getSize());
        assertPartition(withoutInversion, 2);

        TableDataCollection withInversion = analyze("mirrorInverted", data, true, -1.0);
        assertEquals("All genes must be assigned to a cluster", 2 * GROUP_SIZE, withInversion.getSize());
        assertEquals("With inversion mirrored profiles must be merged", 1, countClusters(withInversion));

        String directSign = signOf(withInversion, 0);
        String mirroredSign = signOf(withInversion, GROUP_SIZE);
        assertFalse("Mirrored halves must get opposite signs, both are '" + directSign + "'", directSign.equals(mirroredSign));
        for( int i = 0; i < 2 * GROUP_SIZE; i++ )
        {
            assertEquals("Sign of " + geneName(i), i < GROUP_SIZE ? directSign : mirroredSign, signOf(withInversion, i));
            // a NaN probability passes no cutoff, so it would silently drop the gene from the output
            double probability = (Double)TableDataCollectionUtils.getRowValues(withInversion, geneName(i))[1];
            assertFalse(geneName(i) + ": probability must not be NaN", Double.isNaN(probability));
            assertTrue(geneName(i) + ": probability " + probability + " is out of [0, 1]", probability >= 0 && probability <= 1);
        }
    }

    /**
     * Overlapping groups keep the sampler moving genes between clusters instead of settling down. However
     * it wanders, every gene must end up in exactly one reported cluster: a gene that is neither clustered
     * nor rejected by the cutoff is lost without any diagnostics. The true partition is not asserted here -
     * with overlapping groups it is not identifiable - only that nothing disappears.
     *
     * Repeated, because the loss used to happen in under 10% of the runs.
     */
    public void testNoGeneIsSilentlyDropped() throws Exception
    {
        int geneCount = 2 * GROUP_SIZE;
        double[][] data = new double[geneCount][COLUMN_COUNT];
        for( int i = 0; i < geneCount; i++ )
            for( int j = 0; j < COLUMN_COUNT; j++ )
                // the group levels differ by 2, single measurements deviate by up to 2: the groups overlap
                data[i][j] = ( i < GROUP_SIZE ? 0 : 2 ) + 0.4 * ( ( i * 7 + j * 3 ) % 11 - 5 );

        for( int attempt = 0; attempt < 40; attempt++ )
        {
            TableDataCollection result = analyze("overlap" + attempt, data, false, -1.0);
            for( int i = 0; i < geneCount; i++ )
                assertTrue("Attempt " + attempt + ": " + geneName(i) + " is neither clustered nor filtered out", result.getNameList()
                        .contains(geneName(i)));
            assertEquals("Attempt " + attempt + ": number of reported genes", geneCount, result.getSize());
        }
    }

    private String signOf(TableDataCollection result, int gene) throws Exception
    {
        return (String)TableDataCollectionUtils.getRowValues(result, geneName(gene))[2];
    }

    /**
     * The core of the model: the log of the marginal likelihood of one cluster in one column,
     * with a normal-inverse-gamma prior (a, b) on the column level.
     */
    public void testSingleLogLikelihood() throws Exception
    {
        // (3/2 + 1/2) * ln(1 + 1/2 * (14 + 4 - 64/4)) = 2 * ln(2)
        assertEquals(2 * Math.log(2), CRClusterAnalysis.singleloglikelihood(0.5, 1.0, 14, 6, 2, 3), 1e-12);
        // (4/2 + 1/2) * ln(2 + 1/2 * (30 + 0 - 100/5)) = 2.5 * ln(7)
        assertEquals(2.5 * Math.log(7), CRClusterAnalysis.singleloglikelihood(0.5, 2.0, 30, 10, 0, 4), 1e-12);

        // a single observation equal to the prior mean leaves no residual variance: (a + 1/2) * ln(b)
        for( double x : new double[] { -3.5, 0, 1, 42} )
            for( double a : new double[] {0.5, 1.0, 2.5} )
                for( double b : new double[] {0.25, 1.0, 7.0} )
                    assertEquals("x=" + x + " a=" + a + " b=" + b, ( a + 0.5 ) * Math.log(b),
                            CRClusterAnalysis.singleloglikelihood(a, b, x * x, x, x, 1), 1e-12);

        // the value grows with the scatter of the observations, all other things being equal
        double previous = Double.NEGATIVE_INFINITY;
        for( double x2sum = 4; x2sum < 40; x2sum += 4 )
        {
            double value = CRClusterAnalysis.singleloglikelihood(0.5, 1.0, x2sum, 4, 0, 4);
            assertTrue("Must increase with the sum of squares", value > previous);
            previous = value;
        }
    }

    public void testValidateParameters() throws Exception
    {
        CRClusterAnalysis analysis = new CRClusterAnalysis(null, "crc");
        CRClusterAnalysisParameters parameters = analysis.getParameters();

        parameters.getExperimentData().setColumns(new Column[0]);
        assertRejected(analysis, "Please specify experiment columns");

        TableDataCollection input = createInput("validate", profiles(new double[] {10, -10}));
        parameters.setExperimentData(new ColumnGroup(parameters, columnNames(), input.getCompletePath()));
        assertRejected(analysis, "Please specify output collection");

        parameters.setOutputTablePath(input.getCompletePath());
        assertRejected(analysis, "Output is the same as the input. Please specify different output name.");

        parameters.setOutputTablePath(DataElementPath.create(ROOT, "validateOut"));
        analysis.validateParameters();
    }

    private void assertRejected(CRClusterAnalysis analysis, String expectedMessage)
    {
        try
        {
            analysis.validateParameters();
            fail("Invalid parameters were accepted, expected: " + expectedMessage);
        }
        catch( IllegalArgumentException e )
        {
            assertEquals(expectedMessage, e.getMessage());
        }
    }

    /**
     * Asserts that the resulting clustering is exactly the partition of the genes into groups of
     * {@link #GROUP_SIZE} consecutive genes used to generate the input data.
     */
    private void assertPartition(TableDataCollection result, int groupCount) throws Exception
    {
        int geneCount = groupCount * GROUP_SIZE;
        int[] clusters = new int[geneCount];
        for( int i = 0; i < geneCount; i++ )
        {
            String name = geneName(i);
            assertTrue("Gene " + name + " is missing in the result", result.getNameList().contains(name));
            clusters[i] = (Integer)TableDataCollectionUtils.getRowValues(result, name)[0];
            assertTrue("Gene " + name + " has invalid cluster number " + clusters[i], clusters[i] >= 1);
        }
        assertEquals("Number of clusters", groupCount, countClusters(result));
        for( int i = 0; i < geneCount; i++ )
        {
            for( int j = i + 1; j < geneCount; j++ )
            {
                boolean sameGroup = i / GROUP_SIZE == j / GROUP_SIZE;
                String message = geneName(i) + " (cluster " + clusters[i] + ") and " + geneName(j) + " (cluster " + clusters[j] + ")";
                if( sameGroup )
                    assertEquals("Same profiles must share a cluster: " + message, clusters[i], clusters[j]);
                else
                    assertFalse("Different profiles must not share a cluster: " + message, clusters[i] == clusters[j]);
            }
        }
    }

    private int countClusters(TableDataCollection result) throws Exception
    {
        Set<Object> clusters = new HashSet<>();
        for( String name : result.getNameList() )
            clusters.add(TableDataCollectionUtils.getRowValues(result, name)[0]);
        return clusters.size();
    }

    private void assertColumn(ColumnModel model, int index, String name, Class<?> type)
    {
        assertEquals("Column " + index + " name", name, model.getColumn(index).getName());
        assertEquals("Column " + name + " type", type, model.getColumn(index).getType().getType());
    }

    /**
     * Generates one group of {@link #GROUP_SIZE} genes per given level. Genes of the same group differ
     * from the group level (and thus from each other) by at most {@link #NOISE}.
     */
    private static double[][] profiles(double[] levels)
    {
        double[][] data = new double[levels.length * GROUP_SIZE][COLUMN_COUNT];
        for( int i = 0; i < data.length; i++ )
            for( int j = 0; j < COLUMN_COUNT; j++ )
                data[i][j] = levels[i / GROUP_SIZE] + noise(i, j);
        return data;
    }

    /** Fixed pseudo-random-looking deviation in [-2 * NOISE, 2 * NOISE], so that input data are reproducible. */
    private static double noise(int gene, int column)
    {
        return NOISE * ( ( gene * 7 + column * 3 ) % 5 - 2 );
    }

    private TableDataCollection analyze(String name, double[][] data, boolean invert, Double cutoff) throws Exception
    {
        TableDataCollection input = createInput(name, data);

        CRClusterAnalysis analysis = new CRClusterAnalysis(null, "crc");
        CRClusterAnalysisParameters parameters = analysis.getParameters();
        parameters.setExperimentData(new ColumnGroup(parameters, columnNames(), input.getCompletePath()));
        parameters.setOutputTablePath(DataElementPath.create(ROOT, name + "Clusters"));
        parameters.setInvert(invert);
        if( cutoff != null )
            parameters.setCutoff(cutoff);
        parameters.setChainsCount(5);
        parameters.setCycleCount(10);

        TableDataCollection result = analysis.justAnalyze();
        assertNotNull("Analysis returned no result", result);
        return result;
    }

    private TableDataCollection createInput(String name, double[][] data) throws Exception
    {
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(root, name);
        for( String columnName : columnNames() )
            table.getColumnModel().addColumn(columnName, Double.class);
        for( int i = 0; i < data.length; i++ )
        {
            Object[] row = new Object[data[i].length];
            for( int j = 0; j < row.length; j++ )
                row[j] = data[i][j];
            TableDataCollectionUtils.addRow(table, geneName(i), row);
        }
        root.put(table);
        return table;
    }

    private static String[] columnNames()
    {
        String[] names = new String[COLUMN_COUNT];
        for( int i = 0; i < COLUMN_COUNT; i++ )
            names[i] = columnName(i);
        return names;
    }

    private static String columnName(int index)
    {
        return "point" + ( index + 1 );
    }

    private static String geneName(int index)
    {
        return "gene" + ( index + 1 );
    }
}
