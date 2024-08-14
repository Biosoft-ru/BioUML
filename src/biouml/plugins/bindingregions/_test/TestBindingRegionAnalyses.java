package biouml.plugins.bindingregions._test;

import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

import biouml.plugins.bindingregions.analysis.BindingRegionsSummary;
import biouml.plugins.bindingregions.analysis.BindingRegionsSummary.BindingRegionsSummaryParameters;
import biouml.plugins.bindingregions.analysis.CountOligoFrequencies;
import biouml.plugins.bindingregions.analysis.CountOligoFrequencies.CountOligoFrequenciesParameters;
import biouml.plugins.bindingregions.analysis.CreateMatrixByMixture;
import biouml.plugins.bindingregions.analysis.CreateMatrixByMixture.CreateMatrixByMixtureParameters;
import biouml.plugins.bindingregions.analysis.IPSROCCurve;
import biouml.plugins.bindingregions.analysis.IPSROCCurve.IPSROCCurveParameters;
import biouml.plugins.bindingregions.analysis.NormalComponentsMixture;
import biouml.plugins.bindingregions.analysis.NormalComponentsMixture.NormalComponentsMixtureParameters;
import biouml.plugins.bindingregions.utils.BindingRegion;
import biouml.plugins.bindingregions.utils.CisModule;
import biouml.plugins.gtrd.TrackSqlTransformer;
import ru.biosoft.access.FolderVectorCollection;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Basis;
import ru.biosoft.bsa.Nucleotide15LetterAlphabet;
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
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.graphics.chart.ChartSeries;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;

/**
 * @author lan
 *
 */
public class TestBindingRegionAnalyses extends AbstractBioUMLTest
{
    private static final int[][] TRACK_DATA = { {127001, 225}, {441476, 250}, {713876, 350}, {715276, 175}, {941276, 425}, {1213526, 200},
            {1232101, 100}, {1443101, 100}, {2573551, 175}, {2573976, 900}, {2575126, 575}, {2575901, 475}, {2576401, 225}, {2577101, 150},
            {2577451, 500}, {2578051, 250}, {2578351, 200}, {2578726, 350}, {2579301, 175}, {2579926, 300}, {2580551, 200}, {2582401, 425},
            {2605226, 325}, {2605801, 325}, {2606151, 200}, {2606851, 150}, {2607976, 350}, {2612601, 150}, {2623726, 175}, {2679226, 175},
            {4064651, 175}, {4262851, 400}, {4607351, 125}, {6054351, 100}, {6271776, 175}, {6287701, 150}, {6569001, 150}, {6575951, 100},
            {7552151, 125}, {9418951, 325}, {9480201, 125}, {10600701, 125}, {10928051, 100}, {11374551, 125}, {11763926, 150},
            {11994751, 75}, {12778426, 125}, {12814251, 125}, {12887201, 175}, {15168376, 50}, {15655776, 125}, {16548526, 100},
            {16713001, 525}, {16715076, 275}, {16733776, 150}, {16738726, 150}, {16744451, 100}, {16745076, 175}, {16747676, 75},
            {16751326, 175}, {16762426, 225}, {16762951, 125}, {16764376, 325}, {16765376, 150}, {16767426, 300}, {16767901, 325},
            {16771951, 225}, {16772851, 200}, {16773076, 175}, {16774326, 425}, {16775151, 325}, {16775776, 175}, {16776151, 225},
            {16776551, 175}, {16776876, 175}, {16780076, 350}, {16780601, 450}, {16790201, 150}, {16791176, 225}, {16806951, 150},
            {16810426, 150}, {16818376, 125}, {16820901, 125}, {16856451, 200}, {16865851, 150}, {16866026, 175}, {16868551, 150},
            {16872376, 100}, {16984301, 125}, {16989001, 150}, {17105426, 125}, {18486601, 125}, {18645676, 100}, {19150576, 125},
            {19540126, 100}, {21449126, 150}, {21622976, 125}, {22136876, 150}, {23894851, 50}, {25450151, 125}};

    protected static Track createTestTrack(VectorDataCollection<DataElement> vdc, DataElementPath sequences) throws Exception
    {
        Sequence sequence = sequences.getChildPath("1").getDataElement(AnnotatedSequence.class).getSequence();
        DynamicPropertySet dps = new DynamicPropertySetAsMap();
        dps.add(new DynamicProperty(TrackSqlTransformer.TF_CLASS_ID_PROPERTY, String.class, "2.1.1.1.4"));
        dps.add(new DynamicProperty(BindingRegion.NUMBER_OF_OVERLAPS, Integer.class, 0));
        dps.add(new DynamicProperty(BindingRegion.NUMBER_OF_MERGED_TRACKS, Integer.class, 1));
        Properties properties = new Properties();
        properties.setProperty(DataCollectionConfigConstants.NAME_PROPERTY, "br");
        properties.setProperty(Track.SEQUENCES_COLLECTION_PROPERTY, sequences.toString());
        properties.setProperty(TrackSqlTransformer.TF_CLASS_ID_PROPERTY, "2.1.1.1.4");
        properties.setProperty(CisModule.DISTINCT_TFCLASSES_AND_NAMES, "{\"2.1.1.1.4\":\"Androgen receptor (AR) (NR3C4)\"}");
        WritableTrack input = TrackUtils.createTrack(vdc, properties);
        int score = 0;
        for( int[] siteData : TRACK_DATA )
        {
            Site site = new SiteImpl(null, null, SiteType.TYPE_VARIATION, Basis.BASIS_ANNOTATED, siteData[0], siteData[1],
                    Precision.PRECISION_EXACTLY, StrandType.STRAND_PLUS, sequence, (DynamicPropertySet)dps.clone());
            site.getProperties().add(new DynamicProperty(Site.SCORE_PROPERTY, Double.class, (double) ( score++ % 10 )));
            input.addSite(site);
        }
        input.finalizeAddition();
        vdc.put(input);
        return input;
    }

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

    protected static void compareMatrix(double[][] expected, FrequencyMatrix actual)
    {
        assertNotNull(actual);
        assertEquals("Matrix: " + actual.getName(), expected.length, actual.getLength());
        for( int i = 0; i < expected.length; i++ )
        {
            double sum = 0;
            for( int j = 0; j < 4; j++ )
            {
                assertEquals("Matrix: " + actual.getName() + "#" + i + "," + j, expected[i][j], actual.getFrequency(i, (byte)j), 0.0001);
                sum += actual.getFrequency(i, (byte)j);
            }
            assertEquals("Matrix: " + actual.getName() + "#" + i, 1.0, sum, 0.0001);
        }
    }

    //    public void testCommonAndIPSCorrelation() throws Exception
    //    {
    //        BSATestUtils.createRepository();
    //        DataElementPath sequences = DataElementPath.create("databases/Ensembl/Sequences").getChildren().first();
    //
    //        VectorDataCollection<DataElement> vdc = new VectorDataCollection<>("test");
    //        CollectionFactory.registerRoot(vdc);
    //        Track track = createTestTrack(vdc, sequences);
    //        DataCollection<FrequencyMatrix> matrixLib = createTestMatrixLib(vdc);
    //
    //        CommonAndIPSCorrelation analysis = new CommonAndIPSCorrelation(null, "");
    //        CommonAndIPSCorrelationParameters parameters = analysis.getParameters();
    //        parameters.getDbSelector().setSequenceCollectionPath(sequences);
    //        parameters.setTrackPath(DataElementPath.create(track));
    //        parameters.setMinRegionLength(300);
    //        parameters.setMatrixPath(matrixLib.getCompletePath().getChildPath("AR1"));
    //        parameters.setOutputPath(vdc.getCompletePath());
    //        analysis.justAnalyzeAndPut();
    //        assertEquals(100, analysis.getJobControl().getPreparedness());
    //
    //        RowDataElement row;
    //        Chart chart;
    //
    //
    //        // Now this analysis (after refactoring in previous summer) does not create this table "scoresAndIpsScores_for_AR1_in_br"
    //        /***
    //        TableDataCollection dataTable = (TableDataCollection)vdc.get("scoresAndIpsScores_for_AR1_in_br");
    //        ***/
    //
    //        /***
    //        Now this analysis (after refactoring in previous summer) does not create table
    //        ('scoresAndIpsScores_for_AR1_in_br_charts') with 2 charts. Currently this analysis
    //        create a single chart
    //         ***/
    //        // TableDataCollection chartTable = (TableDataCollection)vdc.get("scoresAndIpsScores_for_AR1_in_br_charts");
    //
    //        TableDataCollection chartTable = (TableDataCollection)vdc.get("ipsAndCommonScores_chart");
    //        assertNotNull("Chart table not found", chartTable);
    //        assertEquals(1, chartTable.getSize());
    //        row = chartTable.get("chart");
    //        assertNotNull(row);
    //        chart = (Chart)row.getValues()[0];
    //        assertNotNull(chart);
    //        assertEquals(2, chart.getSeriesCount());
    //        assertEquals("IPSs", chart.getOptions().getXAxis().getLabel());
    //        assertEquals("Common scores", chart.getOptions().getYAxis().getLabel());
    //        assertEquals(100, chart.getSeries(0).getData().length);
    //        assertEquals(3.89087, chart.getSeries(0).getData()[0][0], 0.0001);
    //        assertEquals(10.81588, chart.getSeries(0).getData()[0][1], 0.0001);
    //        assertEquals(11.61882, chart.getSeries(0).getData()[3][1], 0.0001);
    //
    //        TableDataCollection correlationTable = (TableDataCollection)vdc.get("correlationCoefficients");
    //        assertNotNull("Correlation table not found", correlationTable);
    //        assertEquals(1, correlationTable.getSize());
    //        row = correlationTable.get("Correlation coefficient");
    //        assertNotNull(row);
    //        assertEquals(0.948556, (Double)row.getValues()[0], 0.00001);
    //        assertEquals(0.956194, (Double)row.getValues()[1], 0.00001);
    //    }

    public void testCreateMatrixByMixture() throws Exception
    {
        BSATestUtils.createRepository();
        DataElementPath sequences = DataElementPath.create("databases/Ensembl/Sequences").getChildren().first();

        VectorDataCollection<DataElement> vdc = new VectorDataCollection<>("test");
        CollectionFactory.registerRoot(vdc);
        Track track = createTestTrack(vdc, sequences);
        DataCollection<FrequencyMatrix> matrixLib = createTestMatrixLib(vdc);

        CreateMatrixByMixture analysis = new CreateMatrixByMixture(null, "");
        CreateMatrixByMixtureParameters parameters = analysis.getParameters();
        parameters.setTrackPath(DataElementPath.create(track));
        parameters.setTfClass("2.1.1.1.4");
        parameters.setNumberOfMatrices(1);
        parameters.setMaxIterations(1);
        parameters.setMinRegionLength(300);
        parameters.setPValue(0.05);
        parameters.setUseConsensus(false);
        parameters.setMatrixPath(matrixLib.getCompletePath().getChildPath("AR1"));
        parameters.setOutputPath(matrixLib.getCompletePath());
        parameters.setMatrixBaseName("test");
        analysis.justAnalyzeAndPut();
        assertEquals(100, analysis.getJobControl().getPreparedness());
        double[][] expected = new double[][] { {0.10526315789473684, 0.05263157894736842, 0.3157894736842105, 0.5263157894736842},
                {0.15789473684210528, 0.26315789473684215, 0.4736842105263158, 0.10526315789473685},
                {0.15789473684210528, 0.5263157894736843, 0.05263157894736843, 0.26315789473684215},
                {0.2631578947368421, 0.2631578947368421, 0.05263157894736842, 0.42105263157894735},
                {0.0, 0.8947368421052632, 0.05263157894736842, 0.05263157894736842},
                {0.4210526315789474, 0.4736842105263158, 0.10526315789473685, 0.0}, {1.0, 0.0, 0.0, 0.0},
                {0.15789473684210525, 0.05263157894736842, 0.7894736842105263, 0.0},
                {0.7894736842105263, 0.10526315789473684, 0.05263157894736842, 0.05263157894736842},
                {0.47368421052631576, 0.3684210526315789, 0.10526315789473684, 0.05263157894736842},
                {0.10526315789473684, 0.8421052631578947, 0.05263157894736842, 0.0},
                {0.8947368421052632, 0.05263157894736842, 0.05263157894736842, 0.0},
                {0.0, 0.26315789473684215, 0.4210526315789474, 0.31578947368421056},
                {0.15789473684210525, 0.6842105263157895, 0.10526315789473684, 0.05263157894736842},
                {0.42105263157894735, 0.3157894736842105, 0.10526315789473684, 0.15789473684210525},
                {0.10526315789473684, 0.10526315789473684, 0.3157894736842105, 0.47368421052631576},
                {0.3157894736842105, 0.05263157894736842, 0.631578947368421, 0.0},
                {0.0, 0.10526315789473684, 0.15789473684210525, 0.7368421052631579},
                {0.15789473684210525, 0.21052631578947367, 0.2631578947368421, 0.3684210526315789},
                {0.0, 0.7894736842105263, 0.15789473684210525, 0.05263157894736842},
                {0.15789473684210525, 0.0, 0.15789473684210525, 0.6842105263157895},
                {0.10526315789473684, 0.47368421052631576, 0.3684210526315789, 0.05263157894736842},
                {0.42105263157894746, 0.4736842105263159, 0.05263157894736843, 0.05263157894736843},
                {0.21052631578947367, 0.42105263157894735, 0.21052631578947367, 0.15789473684210525},
                {0.15789473684210528, 0.31578947368421056, 0.2105263157894737, 0.31578947368421056},
                {0.05263157894736842, 0.10526315789473684, 0.2631578947368421, 0.5789473684210527},
                {0.05263157894736842, 0.10526315789473684, 0.6842105263157895, 0.15789473684210525}};
        compareMatrix(expected, matrixLib.get("test_1_iteration_1"));
    }

    public void testCountOligs() throws Exception
    {
        BSATestUtils.createRepository();
        DataElementPath sequences = DataElementPath.create("databases/Ensembl/Sequences").getChildren().first();

        VectorDataCollection<DataElement> vdc = new VectorDataCollection<>("test");
        CollectionFactory.registerRoot(vdc);
        Track track = createTestTrack(vdc, sequences);
        CountOligoFrequencies analysis = new CountOligoFrequencies(null, "");
        CountOligoFrequenciesParameters parameters = analysis.getParameters();
        parameters.setTrackPath(DataElementPath.create(track));
        parameters.setOligLength(5);
        parameters.setOutputPath(vdc.getCompletePath());
        analysis.justAnalyzeAndPut();
        assertEquals(100, analysis.getJobControl().getPreparedness());
        TableDataCollection result = (TableDataCollection)vdc.get("2.1.1.1.4");
        assertNotNull(result);
        assertEquals(512, result.getSize());
        Object[][] expected = new Object[][] { {"TTTTT", "AAAAA", 33}, {"TTTTA", "TAAAA", 28}, {"TTTTG", "CAAAA", 46},
                {"TTTTC", "GAAAA", 41},};
        for( int i = 0; i < expected.length; i++ )
        {
            RowDataElement row = result.get(String.valueOf(i));
            assertNotNull(row);
            Object[] values = row.getValues();
            assertEquals("Row#" + i, 4, values.length);
            assertEquals("Row#" + i, expected[i][0], values[0]);
            assertEquals("Row#" + i, expected[i][1], values[1]);
            assertEquals("Row#" + i, expected[i][2], values[2]);
            assertEquals("Row#" + i, ( (Integer)expected[i][2] ) / 100.0, (Double)values[3], 0.0001);
        }
    }

    public void testIPSROCCurve() throws Exception
    {
        BSATestUtils.createRepository();
        DataElementPath sequences = DataElementPath.create("databases/Ensembl/Sequences").getChildren().first();

        FolderVectorCollection vdc = new FolderVectorCollection("test", null);
        CollectionFactory.registerRoot(vdc);
        Track track = createTestTrack(vdc, sequences);
        DataCollection<FrequencyMatrix> matrixLib = createTestMatrixLib(vdc);

        IPSROCCurve analysis = new IPSROCCurve(null, "");
        IPSROCCurveParameters parameters = analysis.getParameters();
        parameters.setMatrixPath(matrixLib.getCompletePath().getChildPath("AR1"));
        parameters.setTrackPath(DataElementPath.create(track));
        parameters.setOutputPath(vdc.getCompletePath().getChildPath("output"));
        DataCollection<?> outputFolder = analysis.justAnalyzeAndPut();
        assertEquals(100, analysis.getJobControl().getPreparedness());
        assertNotNull(outputFolder);
        assertEquals("[thresholds_ips_AUC, thresholds_ips_chart, windows_ips_AUC, windows_ips_chart]", outputFolder.getNameList()
                .toString());
        checkMode(outputFolder, "thresholds", "multiplier = 0.%d", new int[] {4, 5, 6, 7, 8, 9}, 0.6555125000000008);
        checkMode(outputFolder, "windows", "window = %d", new int[] {30, 50, 75, 100, 150, 200}, 0.5890275000000007);
    }

    private void checkMode(DataCollection<?> outputFolder, String mode, String format, int[] vals, double expectedFirstAUC)
            throws Exception
    {
        TableDataCollection result = (TableDataCollection)outputFolder.get(mode + "_ips_chart");
        assertEquals(1, result.getSize());
        RowDataElement row = result.get(mode + "_ips");
        assertNotNull(row);
        Chart chart = (Chart)row.getValues()[0];
        BSATestUtils.checkROCCurves(chart);
        assertEquals(6, chart.getSeriesCount());
        for( int i = 0; i < chart.getSeriesCount(); i++ )
        {
            ChartSeries series = chart.getSeries(i);
            String name = String.format(Locale.ENGLISH, format, vals[i]);
            assertEquals("Series " + i, name, series.getLabel());
            double[][] data = series.getData();
            assertTrue("Series " + i + ": " + data.length, data.length > 0);
            assertEquals("Series " + i, 0.0, data[0][0], 0.0001);
            assertEquals("Series " + i, 0.0, data[0][1], 0.0001);
            assertEquals("Series " + i, 1.0, data[data.length - 1][0], 0.0001);
            assertEquals("Series " + i, 1.0, data[data.length - 1][1], 0.0001);
        }
        TableDataCollection auc = (TableDataCollection)outputFolder.get(mode + "_ips_AUC");
        assertEquals(chart.getSeriesCount(), auc.getSize());
        for( int i = 0; i < chart.getSeriesCount(); i++ )
        {
            row = auc.get(chart.getSeries(i).getLabel());
            assertNotNull(chart.getSeries(i).getLabel(), row);
            assertEquals(1, row.getValues().length);
            double aucValue = (double)row.getValues()[0];
            assertTrue(aucValue >= 0.0 && aucValue <= 1.0);
            if( i == 0 )
            {
                assertEquals(expectedFirstAUC, aucValue, 0.00001);
            }
        }
    }

    public void testNormalComponentsMixture() throws Exception
    {
        BSATestUtils.createRepository();
        DataElementPath sequences = DataElementPath.create("databases/Ensembl/Sequences").getChildren().first();

        VectorDataCollection<DataElement> vdc = new VectorDataCollection<>("test");
        CollectionFactory.registerRoot(vdc);
        Track track = createTestTrack(vdc, sequences);
        DataCollection<FrequencyMatrix> matrixLib = createTestMatrixLib(vdc);

        NormalComponentsMixture analysis = new NormalComponentsMixture(null, "");
        NormalComponentsMixtureParameters parameters = analysis.getParameters();
        parameters.setMatrixPath(matrixLib.getCompletePath().getChildPath("AR1"));
        parameters.setTrackPath(DataElementPath.create(track));
        parameters.setTfClass("2.1.1.1.4");
        parameters.setMixtureComponentsNumber(2);
        parameters.setPValue(0.05);
        TableDataCollection result = analysis.justAnalyzeAndPut();
        assertEquals(100, analysis.getJobControl().getPreparedness());
        assertNotNull(result);

        assertEquals(4, result.getSize());
        RowDataElement row = result.get("normalComponentDensities");
        assertNotNull(row);
        Chart chart = (Chart)row.getValues()[0];
        assertNotNull(chart);
        assertEquals(3, chart.getSeriesCount());
        assertEquals("Mixture component 1", chart.getSeries(0).getLabel());
        assertEquals(81, chart.getSeries(0).getData().length);
        assertEquals("Mixture component 2", chart.getSeries(1).getLabel());
        assertEquals(22, chart.getSeries(1).getData().length);
        assertEquals("Total density", chart.getSeries(2).getLabel());
        assertEquals(101, chart.getSeries(2).getData().length);

        row = result.get("totalDensity");
        assertNotNull(row);
        chart = (Chart)row.getValues()[0];
        assertNotNull(chart);
        assertEquals(1, chart.getSeriesCount());
        assertEquals("Best score", chart.getOptions().getXAxis().getLabel());
        assertEquals("Probability", chart.getOptions().getYAxis().getLabel());
        assertEquals(101, chart.getSeries(0).getData().length);
        assertEquals( 2.122946, chart.getSeries( 0 ).getData()[0][0], 0.0001 );
        assertEquals( 0.0, chart.getSeries( 0 ).getData()[0][1], 0.0001 );
        assertEquals( 0.166075, chart.getSeries( 0 ).getData()[3][1], 0.0001 );
    }

    public void testBindingRegionsSummary() throws Exception
    {
        BSATestUtils.createRepository();
        DataElementPath sequences = DataElementPath.create("databases/Ensembl/Sequences").getChildren().first();

        VectorDataCollection<DataElement> vdc = new VectorDataCollection<>("test");
        CollectionFactory.registerRoot(vdc);
        Track track = createTestTrack(vdc, sequences);

        BindingRegionsSummary analysis = new BindingRegionsSummary(null, "");
        BindingRegionsSummaryParameters parameters = analysis.getParameters();
        parameters.setTrackPath(DataElementPath.create(track));
        parameters.setOutputPath(vdc.getCompletePath());

        analysis.justAnalyzeAndPut();
        assertEquals(100, analysis.getJobControl().getPreparedness());

        TableDataCollection summaryOfBindingRegions = (TableDataCollection)vdc.get("summaryOnBindingRegions");
        assertNotNull(summaryOfBindingRegions);
        assertEquals(2, summaryOfBindingRegions.getSize());
        RowDataElement row = summaryOfBindingRegions.get("2.1.1.1.4_Androgen receptor (AR) (NR3C4)");
        assertNotNull(row);
        assertEquals(1.0, row.getValue("Number of merged tracks"));
        assertEquals(100.0, row.getValue("Number of binding regions"));
        assertEquals(209.25, row.getValue("Mean length of binding regions"));
        assertEquals(0.0, row.getValue("Mean number of overlaps"));
        RowDataElement row2 = summaryOfBindingRegions.get("all TF-classes");
        assertNotNull(row2);
        assertEquals(Arrays.asList(row.getValues()), Arrays.asList(row2.getValues()));

        TableDataCollection chartsDensities = (TableDataCollection)vdc.get("_charts_densities");
        assertNotNull(chartsDensities);
        assertEquals(2, chartsDensities.getSize());
        row = chartsDensities.get("Mean number of overlaps");
        assertNotNull(row);
        assertTrue(row.getValues()[0] instanceof Chart);
        row = chartsDensities.get("Mean length of binding regions");
        assertNotNull(row);
        assertTrue(row.getValues()[0] instanceof Chart);
        Chart chart = (Chart)row.getValues()[0];
        assertEquals(1, chart.getSeriesCount());
        ChartSeries series = chart.getSeries(0);
        assertEquals(3, series.getData().length);
        assertEquals(209.25, series.getData()[0][0], 0.000001);
        assertEquals(0.035842293906810034, series.getData()[0][1], 0.000001);
    }
}
