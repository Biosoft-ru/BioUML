package biouml.plugins.bindingregions._test;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.FolderVectorCollection;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.access._test.AbstractBioUMLTest;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa._test.BSATestUtils;
import ru.biosoft.bsa.analysis.FrequencyMatrix;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.graphics.chart.ChartSeries;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import biouml.plugins.bindingregions.fiveSiteModels.AbstractFiveSiteModelsParameters;
import biouml.plugins.bindingregions.fiveSiteModels.BestSiteCorrelations;
import biouml.plugins.bindingregions.fiveSiteModels.BestSiteCorrelations.BestSiteCorrelationsParameters;
import biouml.plugins.bindingregions.fiveSiteModels.BestSiteLocations;
import biouml.plugins.bindingregions.fiveSiteModels.BestSiteLocations.BestSiteLocationsParameters;
import biouml.plugins.bindingregions.fiveSiteModels.BestSitesUnionROCCurves;
import biouml.plugins.bindingregions.fiveSiteModels.BestSitesUnionROCCurves.BestSitesUnionROCCurvesParameters;
import biouml.plugins.bindingregions.fiveSiteModels.IdenticalBestSiteROCCurves;
import biouml.plugins.bindingregions.fiveSiteModels.IdenticalBestSiteROCCurves.IdenticalBestSiteROCCurvesParameters;
import biouml.plugins.bindingregions.fiveSiteModels.ROCCurvesInGrouped;
import biouml.plugins.bindingregions.fiveSiteModels.ROCCurvesInGrouped.ROCCurvesInGroupedParameters;
import biouml.plugins.bindingregions.utils.SiteModelsComparison;

/**
 * @author lan
 *
 */
public class TestFiveSiteModels extends AbstractBioUMLTest
{
    private Track track;
    private DataCollection<FrequencyMatrix> matrixLib;
    private VectorDataCollection<DataElement> vdc;
    private DataElementPath sequences;
    private VectorDataCollection<FrequencyMatrix> outputMatrixLib;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        BSATestUtils.createRepository();
        sequences = DataElementPath.create("databases/Ensembl/Sequences").getChildren().first();

        vdc = new FolderVectorCollection("test", null);
        CollectionFactory.registerRoot(vdc);
        track = TestBindingRegionAnalyses.createTestTrack(vdc, sequences);
        matrixLib = TestBindingRegionAnalyses.createTestMatrixLib(vdc);

        outputMatrixLib = new VectorDataCollection<>("Matrix", FrequencyMatrix.class, vdc);
        vdc.put(outputMatrixLib);
    }

    protected void setParameters(AbstractFiveSiteModelsParameters parameters)
    {
        parameters.setMatrixPath(matrixLib.getCompletePath().getChildPath("AR1"));
        parameters.setTrackPath(DataElementPath.create(track));
        parameters.setAroundSummit(false);
        assertEquals(sequences, parameters.getDbSelector().getSequenceCollectionPath());
        parameters.setMinRegionLength(300);
        parameters.setSiteModelTypes(new String[] {SiteModelsComparison.WEIGHT_MATRIX_MODEL,
                SiteModelsComparison.LOG_WEIGHT_MATRIX_MODEL_WITH_MODERATE_PSEUDOCOUNTS, SiteModelsComparison.MATCH_SITE_MODEL,
                SiteModelsComparison.IPS_SITE_MODEL, SiteModelsComparison.LOG_IPS_SITE_MODEL});

    }

    public void testBestSitesUnionROCCurves() throws Exception
    {
        BestSitesUnionROCCurves analysis = new BestSitesUnionROCCurves(null, "");
        BestSitesUnionROCCurvesParameters parameters = analysis.getParameters();
        setParameters(parameters);
        parameters.setOutputPath(vdc.getCompletePath());
        parameters.setBestSitesPercentage(15);
        DataCollection<?> result = analysis.justAnalyzeAndPut();
        assertEquals(100, analysis.getJobControl().getPreparedness());
        assertNotNull(result);
        assertEquals(vdc, result);
        TableDataCollection tdc = (TableDataCollection)result.get("ROCcurve_for_AR1_in_2.1.1.1.4_br_percentage_15_size_26");
        assertNotNull(tdc);
        assertEquals(1, tdc.getSize());
        RowDataElement row = tdc.get("ROCcurve_for_AR1_in_2.1.1.1.4_br_percentage_15_size_26");
        assertNotNull(row);
        Chart chart = (Chart)row.getValues()[0];
        BSATestUtils.checkROCCurves(chart);
        assertEquals(5, chart.getSeriesCount());
        Map<String, Integer> seriesSizes = new HashMap<>();
        seriesSizes.put("Common additive model", 16);
        seriesSizes.put("Common multiplicative model", 21);
        seriesSizes.put("MATCH model", 17);
        seriesSizes.put("IPS model", 17);
        seriesSizes.put("Multiplicative IPS model", 18);
        for( ChartSeries series : chart )
        {
            String label = series.getLabel();
            assertNotNull(label);
            assertTrue(label, seriesSizes.containsKey(label));
            assertEquals(label, (int)seriesSizes.get(label), series.getData().length);
            assertEquals(label, 0.0, series.getData()[0][0], 0.0001);
        }

        tdc = (TableDataCollection)result.get("ROCcurve_for_AR1_revised_br_percentage_15_in_2.1.1.1.4_br_percentage_15_size_22");
        assertNotNull(tdc);
        assertEquals(1, tdc.getSize());
        row = tdc.get("ROCcurve_for_AR1_revised_br_percentage_15_in_2.1.1.1.4_br_percentage_15_size_22");
        assertNotNull(row);
        chart = (Chart)row.getValues()[0];
        BSATestUtils.checkROCCurves(chart);

        tdc = (TableDataCollection)result.get("AUCs");
        assertNotNull(tdc);
        assertEquals(1, tdc.getSize());
        row = tdc.get("0");
        assertEquals("2.1.1.1.4", row.getValue("TF-class"));
        assertEquals(15, row.getValue("Percentage of best sites"));
        assertEquals(0.9203772189349108, (Double)row.getValue("AUC for IPS model"), 0.0001);
        assertEquals(0.8910379684418142, (Double)row.getValue("AUC for MATCH model"), 0.0001);
        assertEquals(0.9146449704142006, (Double)row.getValue("AUC for Common multiplicative model"), 0.0001);
        assertEquals(0.9035502958579877, (Double)row.getValue("AUC for Multiplicative IPS model"), 0.0001);
        assertEquals(0.9191568047337274, (Double)row.getValue("AUC for Common additive model"), 0.0001);

        double[][] expected = {
                {0.17857142857142858, 0.03571428571428571, 0.2857142857142857, 0.5, 0.4642857142857143, 0.5357142857142857,
                        0.2142857142857143, 0.7857142857142857, 0.3214285714285714, 0.6785714285714286, 0.7142857142857143,
                        0.8214285714285714, 0.5, 0.9642857142857143, 1.0},
                {0.10714285714285714, 0.14285714285714285, 0.4642857142857143, 0.2857142857142857, 0.5714285714285714, 0.42857142857142855,
                        0.25, 0.75, 0.6071428571428572, 0.39285714285714285, 0.5357142857142857, 0.8928571428571429, 0.7142857142857143,
                        0.8571428571428571, 1.0},
                {0.17857142857142858, 0.35714285714285715, 0.21428571428571427, 0.25, 0.39285714285714285, 0.6071428571428572,
                        0.5357142857142857, 0.4642857142857143, 0.5714285714285714, 0.4285714285714286, 0.7857142857142857,
                        0.8214285714285714, 0.75, 0.6428571428571428, 1.0},
                {0.32142857142857145, 0.17857142857142858, 0.17857142857142858, 0.32142857142857145, 0.5, 0.5, 0.5, 0.5,
                        0.35714285714285715, 0.6428571428571429, 0.8214285714285714, 0.6785714285714286, 0.6785714285714286,
                        0.8214285714285714, 1.0},
                {0.17857142857142858, 0.5714285714285714, 0.14285714285714285, 0.10714285714285714, 0.3214285714285714, 0.6785714285714285,
                        0.75, 0.25, 0.7142857142857142, 0.2857142857142857, 0.8571428571428571, 0.8214285714285713, 0.8928571428571428,
                        0.42857142857142855, 0.9999999999999999},
                {0.32142857142857145, 0.5, 0.07142857142857142, 0.10714285714285714, 0.3928571428571429, 0.6071428571428571,
                        0.8214285714285714, 0.17857142857142855, 0.5714285714285714, 0.4285714285714286, 0.9285714285714285,
                        0.6785714285714285, 0.8928571428571428, 0.5, 0.9999999999999999},
                {0.8928571428571429, 0.03571428571428571, 0.03571428571428571, 0.03571428571428571, 0.9285714285714286,
                        0.07142857142857142, 0.9285714285714286, 0.07142857142857142, 0.07142857142857142, 0.9285714285714286,
                        0.9642857142857143, 0.10714285714285714, 0.9642857142857143, 0.9642857142857143, 1.0},
                {0.07142857142857142, 0.03571428571428571, 0.8928571428571429, 0.0, 0.9642857142857143, 0.03571428571428571,
                        0.10714285714285714, 0.8928571428571429, 0.9285714285714286, 0.07142857142857142, 0.10714285714285714,
                        0.9285714285714286, 1.0, 0.9642857142857143, 1.0},
                {0.6785714285714286, 0.14285714285714285, 0.10714285714285714, 0.07142857142857142, 0.7857142857142857,
                        0.21428571428571427, 0.8214285714285714, 0.17857142857142855, 0.25, 0.75, 0.8928571428571428, 0.3214285714285714,
                        0.9285714285714285, 0.8571428571428571, 0.9999999999999999},
                {0.5357142857142857, 0.2857142857142857, 0.14285714285714285, 0.03571428571428571, 0.6785714285714286, 0.3214285714285714,
                        0.8214285714285714, 0.17857142857142855, 0.42857142857142855, 0.5714285714285714, 0.8571428571428571,
                        0.46428571428571425, 0.9642857142857142, 0.7142857142857143, 0.9999999999999999},
                {0.17857142857142858, 0.75, 0.03571428571428571, 0.03571428571428571, 0.2142857142857143, 0.7857142857142857,
                        0.9285714285714286, 0.07142857142857142, 0.7857142857142857, 0.2142857142857143, 0.9642857142857143,
                        0.8214285714285714, 0.9642857142857143, 0.25, 1.0},
                {0.8571428571428571, 0.03571428571428571, 0.07142857142857142, 0.03571428571428571, 0.9285714285714285,
                        0.07142857142857142, 0.8928571428571428, 0.10714285714285714, 0.10714285714285714, 0.8928571428571428,
                        0.9285714285714285, 0.14285714285714285, 0.9642857142857142, 0.9642857142857142, 0.9999999999999999},
                {0.10714285714285714, 0.25, 0.2857142857142857, 0.35714285714285715, 0.39285714285714285, 0.6071428571428572,
                        0.35714285714285715, 0.6428571428571428, 0.5357142857142857, 0.4642857142857143, 0.7142857142857143,
                        0.8928571428571428, 0.6428571428571428, 0.75, 1.0},
                {0.21428571428571427, 0.42857142857142855, 0.21428571428571427, 0.14285714285714285, 0.42857142857142855,
                        0.5714285714285714, 0.6428571428571428, 0.3571428571428571, 0.6428571428571428, 0.3571428571428571,
                        0.7857142857142856, 0.7857142857142856, 0.8571428571428571, 0.5714285714285714, 1.0},
                {0.39285714285714285, 0.32142857142857145, 0.17857142857142858, 0.10714285714285714, 0.5714285714285714,
                        0.4285714285714286, 0.7142857142857143, 0.2857142857142857, 0.5, 0.5, 0.8214285714285714, 0.6071428571428571,
                        0.8928571428571429, 0.6785714285714285, 1.0},
                {0.07142857142857142, 0.07142857142857142, 0.17857142857142858, 0.6785714285714286, 0.25, 0.75, 0.14285714285714285,
                        0.8571428571428572, 0.25, 0.75, 0.8214285714285714, 0.9285714285714286, 0.3214285714285714, 0.9285714285714286, 1.0},
                {0.21428571428571427, 0.2857142857142857, 0.4642857142857143, 0.03571428571428571, 0.6785714285714286, 0.3214285714285714,
                        0.5, 0.5, 0.75, 0.25, 0.5357142857142857, 0.7857142857142857, 0.9642857142857143, 0.7142857142857143, 1.0},
                {0.03571428571428571, 0.03571428571428571, 0.10714285714285714, 0.8214285714285714, 0.14285714285714285,
                        0.8571428571428571, 0.07142857142857142, 0.9285714285714285, 0.14285714285714285, 0.8571428571428571,
                        0.8928571428571428, 0.9642857142857142, 0.17857142857142855, 0.9642857142857142, 1.0},
                {0.17857142857142858, 0.14285714285714285, 0.21428571428571427, 0.4642857142857143, 0.39285714285714285,
                        0.6071428571428572, 0.3214285714285714, 0.6785714285714286, 0.3571428571428571, 0.6428571428571429,
                        0.7857142857142857, 0.8214285714285714, 0.5357142857142857, 0.8571428571428572, 1.0},
                {0.0, 0.75, 0.10714285714285714, 0.14285714285714285, 0.10714285714285714, 0.8928571428571428, 0.75, 0.25,
                        0.8571428571428571, 0.14285714285714285, 0.8928571428571428, 1.0, 0.8571428571428571, 0.25, 1.0},
                {0.14285714285714285, 0.07142857142857142, 0.10714285714285714, 0.6785714285714286, 0.25, 0.75, 0.21428571428571427,
                        0.7857142857142857, 0.17857142857142855, 0.8214285714285714, 0.8928571428571429, 0.8571428571428572,
                        0.3214285714285714, 0.9285714285714286, 1.0},
                {0.14285714285714285, 0.5714285714285714, 0.21428571428571427, 0.07142857142857142, 0.3571428571428571, 0.6428571428571428,
                        0.7142857142857142, 0.2857142857142857, 0.7857142857142857, 0.21428571428571427, 0.7857142857142856,
                        0.8571428571428571, 0.9285714285714285, 0.4285714285714285, 0.9999999999999999},
                {0.39285714285714285, 0.32142857142857145, 0.07142857142857142, 0.21428571428571427, 0.4642857142857143,
                        0.5357142857142857, 0.7142857142857143, 0.2857142857142857, 0.3928571428571429, 0.6071428571428571,
                        0.9285714285714286, 0.6071428571428572, 0.7857142857142857, 0.6785714285714286, 1.0},
                {0.2857142857142857, 0.2857142857142857, 0.14285714285714285, 0.2857142857142857, 0.42857142857142855, 0.5714285714285714,
                        0.5714285714285714, 0.42857142857142855, 0.42857142857142855, 0.5714285714285714, 0.8571428571428571,
                        0.7142857142857142, 0.7142857142857142, 0.7142857142857142, 0.9999999999999999},
                {0.17857142857142858, 0.35714285714285715, 0.14285714285714285, 0.32142857142857145, 0.3214285714285714,
                        0.6785714285714286, 0.5357142857142857, 0.4642857142857143, 0.5, 0.5, 0.8571428571428572, 0.8214285714285714,
                        0.6785714285714286, 0.6428571428571428, 1.0},
                {0.17857142857142858, 0.07142857142857142, 0.25, 0.5, 0.4285714285714286, 0.5714285714285714, 0.25, 0.75,
                        0.3214285714285714, 0.6785714285714286, 0.75, 0.8214285714285714, 0.5, 0.9285714285714286, 1.0},
                {0.0, 0.2857142857142857, 0.5, 0.21428571428571427, 0.5, 0.5, 0.2857142857142857, 0.7142857142857143, 0.7857142857142857,
                        0.21428571428571427, 0.5, 1.0, 0.7857142857142857, 0.7142857142857143, 1.0}};
        TestBindingRegionAnalyses.compareMatrix(expected, outputMatrixLib.get("AR1_revised_br_percentage_15"));

        double[][] expected2 = {
                {0.0, 0.0, 0.2727272727272727, 0.7272727272727273, 0.2727272727272727, 0.7272727272727273, 0.0, 1.0, 0.2727272727272727,
                        0.7272727272727273, 0.7272727272727273, 1.0, 0.2727272727272727, 1.0, 1.0},
                {0.09090909090909091, 0.045454545454545456, 0.8181818181818182, 0.045454545454545456, 0.9090909090909092,
                        0.09090909090909091, 0.13636363636363635, 0.8636363636363636, 0.8636363636363636, 0.13636363636363635,
                        0.18181818181818182, 0.9090909090909091, 0.9545454545454546, 0.9545454545454546, 1.0},
                {0.13636363636363635, 0.5454545454545454, 0.045454545454545456, 0.2727272727272727, 0.18181818181818182,
                        0.8181818181818181, 0.6818181818181818, 0.3181818181818182, 0.5909090909090908, 0.40909090909090906,
                        0.9545454545454545, 0.8636363636363635, 0.7272727272727272, 0.45454545454545453, 0.9999999999999999},
                {0.22727272727272727, 0.045454545454545456, 0.045454545454545456, 0.6818181818181818, 0.2727272727272727,
                        0.7272727272727272, 0.2727272727272727, 0.7272727272727272, 0.09090909090909091, 0.9090909090909091,
                        0.9545454545454545, 0.7727272727272727, 0.3181818181818182, 0.9545454545454545, 1.0},
                {0.09090909090909091, 0.8636363636363636, 0.045454545454545456, 0.0, 0.13636363636363635, 0.8636363636363636,
                        0.9545454545454546, 0.045454545454545456, 0.9090909090909091, 0.09090909090909091, 0.9545454545454546,
                        0.9090909090909091, 1.0, 0.13636363636363635, 1.0},
                {0.18181818181818182, 0.8181818181818182, 0.0, 0.0, 0.18181818181818182, 0.8181818181818182, 1.0, 0.0, 0.8181818181818182,
                        0.18181818181818182, 1.0, 0.8181818181818182, 1.0, 0.18181818181818182, 1.0},
                {1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 1.0, 1.0, 1.0},
                {0.045454545454545456, 0.0, 0.9545454545454546, 0.0, 1.0, 0.0, 0.045454545454545456, 0.9545454545454546,
                        0.9545454545454546, 0.045454545454545456, 0.045454545454545456, 0.9545454545454546, 1.0, 1.0, 1.0},
                {0.45454545454545453, 0.09090909090909091, 0.45454545454545453, 0.0, 0.9090909090909091, 0.09090909090909091,
                        0.5454545454545454, 0.45454545454545453, 0.5454545454545454, 0.45454545454545453, 0.5454545454545454,
                        0.5454545454545454, 1.0, 0.9090909090909091, 1.0},
                {0.2727272727272727, 0.5909090909090909, 0.09090909090909091, 0.045454545454545456, 0.36363636363636365,
                        0.6363636363636364, 0.8636363636363636, 0.13636363636363635, 0.6818181818181819, 0.3181818181818182,
                        0.9090909090909091, 0.7272727272727273, 0.9545454545454546, 0.4090909090909091, 1.0},
                {0.13636363636363635, 0.6363636363636364, 0.0, 0.22727272727272727, 0.13636363636363635, 0.8636363636363636,
                        0.7727272727272727, 0.22727272727272727, 0.6363636363636364, 0.36363636363636365, 1.0, 0.8636363636363636,
                        0.7727272727272727, 0.36363636363636365, 1.0},
                {0.8181818181818182, 0.0, 0.18181818181818182, 0.0, 1.0, 0.0, 0.8181818181818182, 0.18181818181818182, 0.18181818181818182,
                        0.8181818181818182, 0.8181818181818182, 0.18181818181818182, 1.0, 1.0, 1.0},
                {0.0, 0.13636363636363635, 0.13636363636363635, 0.7272727272727273, 0.13636363636363635, 0.8636363636363636,
                        0.13636363636363635, 0.8636363636363636, 0.2727272727272727, 0.7272727272727273, 0.8636363636363636, 1.0,
                        0.2727272727272727, 0.8636363636363636, 1.0},
                {0.18181818181818182, 0.7272727272727273, 0.09090909090909091, 0.0, 0.2727272727272727, 0.7272727272727273,
                        0.9090909090909092, 0.09090909090909091, 0.8181818181818182, 0.18181818181818182, 0.9090909090909092,
                        0.8181818181818182, 1.0, 0.2727272727272727, 1.0},
                {0.5909090909090909, 0.18181818181818182, 0.18181818181818182, 0.045454545454545456, 0.7727272727272727,
                        0.2272727272727273, 0.7727272727272727, 0.2272727272727273, 0.36363636363636365, 0.6363636363636364,
                        0.8181818181818181, 0.4090909090909091, 0.9545454545454546, 0.8181818181818181, 1.0},
                {0.045454545454545456, 0.09090909090909091, 0.6363636363636364, 0.22727272727272727, 0.6818181818181818,
                        0.3181818181818182, 0.13636363636363635, 0.8636363636363636, 0.7272727272727273, 0.2727272727272727,
                        0.36363636363636365, 0.9545454545454546, 0.7727272727272727, 0.9090909090909091, 1.0},
                {0.6363636363636364, 0.13636363636363635, 0.22727272727272727, 0.0, 0.8636363636363636, 0.13636363636363635,
                        0.7727272727272727, 0.22727272727272727, 0.36363636363636365, 0.6363636363636364, 0.7727272727272727,
                        0.36363636363636365, 1.0, 0.8636363636363636, 1.0},
                {0.0, 0.0, 0.045454545454545456, 0.9545454545454546, 0.045454545454545456, 0.9545454545454546, 0.0, 1.0,
                        0.045454545454545456, 0.9545454545454546, 0.9545454545454546, 1.0, 0.045454545454545456, 1.0, 1.0},
                {0.13636363636363635, 0.045454545454545456, 0.6363636363636364, 0.18181818181818182, 0.7727272727272727,
                        0.2272727272727273, 0.18181818181818182, 0.8181818181818181, 0.6818181818181818, 0.3181818181818182,
                        0.36363636363636365, 0.8636363636363635, 0.8181818181818181, 0.9545454545454546, 1.0},
                {0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 1.0, 1.0, 1.0, 0.0, 1.0},
                {0.0, 0.045454545454545456, 0.13636363636363635, 0.8181818181818182, 0.13636363636363635, 0.8636363636363636,
                        0.045454545454545456, 0.9545454545454546, 0.18181818181818182, 0.8181818181818182, 0.8636363636363636, 1.0,
                        0.18181818181818182, 0.9545454545454546, 1.0},
                {0.045454545454545456, 0.9090909090909091, 0.045454545454545456, 0.0, 0.09090909090909091, 0.9090909090909091,
                        0.9545454545454545, 0.045454545454545456, 0.9545454545454545, 0.045454545454545456, 0.9545454545454545,
                        0.9545454545454545, 0.9999999999999999, 0.09090909090909091, 0.9999999999999999},
                {0.6818181818181818, 0.18181818181818182, 0.045454545454545456, 0.09090909090909091, 0.7272727272727272,
                        0.2727272727272727, 0.8636363636363635, 0.13636363636363635, 0.2272727272727273, 0.7727272727272727,
                        0.9545454545454545, 0.31818181818181823, 0.909090909090909, 0.8181818181818181, 0.9999999999999999},
                {0.13636363636363635, 0.6363636363636364, 0.045454545454545456, 0.18181818181818182, 0.18181818181818182,
                        0.8181818181818181, 0.7727272727272727, 0.2272727272727273, 0.6818181818181818, 0.3181818181818182,
                        0.9545454545454546, 0.8636363636363635, 0.8181818181818181, 0.36363636363636365, 1.0},
                {0.09090909090909091, 0.6363636363636364, 0.045454545454545456, 0.22727272727272727, 0.13636363636363635,
                        0.8636363636363636, 0.7272727272727273, 0.2727272727272727, 0.6818181818181818, 0.3181818181818182,
                        0.9545454545454546, 0.9090909090909091, 0.7727272727272727, 0.36363636363636365, 1.0},
                {0.0, 0.045454545454545456, 0.22727272727272727, 0.7272727272727273, 0.22727272727272727, 0.7727272727272727,
                        0.045454545454545456, 0.9545454545454546, 0.2727272727272727, 0.7272727272727273, 0.7727272727272727, 1.0,
                        0.2727272727272727, 0.9545454545454546, 1.0},
                {0.0, 0.18181818181818182, 0.8181818181818182, 0.0, 0.8181818181818182, 0.18181818181818182, 0.18181818181818182,
                        0.8181818181818182, 1.0, 0.0, 0.18181818181818182, 1.0, 1.0, 0.8181818181818182, 1.0}};
        TestBindingRegionAnalyses.compareMatrix(expected2, outputMatrixLib.get("AR1_revised_br_percentage_15_revised_br_percentage_15"));
    }

    public void testBestSiteLocations() throws Exception
    {
        BestSiteLocations analysis = new BestSiteLocations(null, "");
        BestSiteLocationsParameters parameters = analysis.getParameters();
        setParameters(parameters);
        parameters.setOutputPath(vdc.getCompletePath());
        DataCollection<?> result = analysis.justAnalyzeAndPut();
        assertEquals(100, analysis.getJobControl().getPreparedness());
        assertNotNull(result);
        assertEquals(vdc, result);
        TableDataCollection tdc = (TableDataCollection)result.get("bestSites_for_AR1_in_2.1.1.1.4_br_charts");
        assertEquals(100, analysis.getJobControl().getPreparedness());
        assertNotNull(tdc);
        assertEquals(1, tdc.getSize());
        RowDataElement row = tdc.get("Best site locations along normalized sequences");
        assertNotNull(row);
        Chart chart = (Chart)row.getValues()[0];
        assertNotNull(chart);
        assertEquals(5, chart.getSeriesCount());
        assertEquals(4.0, chart.getSeries(2).getData()[3][0], 0.0001);
        assertEquals(15.100671140939598, chart.getSeries(3).getData()[3][0], 0.0001);
        Map<String, Integer> seriesSizes = new HashMap<>();
        seriesSizes.put("Common additive model", 85);
        seriesSizes.put("Common multiplicative model", 85);
        seriesSizes.put("IPS model", 84);
        seriesSizes.put("MATCH model", 85);
        seriesSizes.put("Multiplicative IPS model", 84);
        for( ChartSeries series : chart )
        {
            String label = series.getLabel();
            assertNotNull(label);
            assertTrue(label, seriesSizes.containsKey(label));
            assertEquals(label, (int)seriesSizes.get(label), series.getData().length);
            for( int i = 0; i < series.getData().length; i++ )
            {
                double[] point = series.getData()[i];
                assertTrue(label + "/" + i, point[0] >= 0 && point[0] <= 100);
                assertTrue(label + "/" + i, point[1] >= 0 && point[1] <= 1);
            }
        }
    }

    public void testBestSiteCorrelations() throws Exception
    {
        BestSiteCorrelations analysis = new BestSiteCorrelations(null, "");
        BestSiteCorrelationsParameters parameters = analysis.getParameters();
        setParameters(parameters);
        parameters.setOutputPath(DataElementPath.create("test/bestSites_for_AR1_in_2.1.1.1.4_br_corr"));
        //        assertEquals(DataElementPath.create("test/bestSites_for_AR1_in_2.1.1.1.4_br_corr"), parameters.getOutputPath());
        DataCollection<?> dc = analysis.justAnalyzeAndPut();
        assertEquals(100, analysis.getJobControl().getPreparedness());
        assertNotNull(dc);
        assertEquals(2, dc.getSize());
        TableDataCollection tdc = (TableDataCollection)dc.get("correlationMatrix");
        assertNotNull(tdc);
        assertEquals(7, tdc.getSize());
        Map<String, Double[]> expected = new HashMap<>();
        expected.put("Peak length", new Double[] {0.10910843571176831, 0.035265950854770516, 0.12682138622648756, 0.04809973652546223,
                0.05309619144748937, 1.0, -0.06930514078025815});
        expected.put("Peak score", new Double[] {0.033334647565008146, -0.00764339439500917, 0.036396075744499816, 0.04975791488723025,
                0.017489345817309897, -0.06930514078025815, 1.0});
        expected.put("Common additive model", new Double[] {1.0, 0.9258548287887933, 0.9513213644354146, 0.8316543950202298,
                0.8888294726954062, 0.10910843571176831, 0.033334647565008146});
        expected.put("Common multiplicative model", new Double[] {0.9258548287887933, 1.0, 0.8769203923296889, 0.8410218463571377,
                0.9513575508068519, 0.035265950854770516, -0.00764339439500917});
        expected.put("MATCH model", new Double[] {0.8316543950202298, 0.8410218463571377, 0.7775253832823992, 1.0,
                0.8037362722035354, 0.04809973652546223, 0.04975791488723025});
        expected.put("IPS model", new Double[] {0.9513213644354146, 0.8769203923296889, 1.0, 0.7775253832823992, 0.9283774064957538,
                0.12682138622648756, 0.036396075744499816});
        expected.put("Multiplicative IPS model", new Double[] {0.8888294726954062, 0.9513575508068519, 0.9283774064957538,
                0.8037362722035354, 1.0, 0.05309619144748937, 0.017489345817309897});
        for( Entry<String, Double[]> entry : expected.entrySet() )
        {
            RowDataElement row = tdc.get(entry.getKey());
            assertNotNull(entry.getKey(), row);
            Object[] values = row.getValues();
            assertEquals(entry.getKey(), entry.getValue().length, values.length);
            for( int i = 0; i < values.length; i++ )
            {
                assertEquals(entry.getKey() + "/" + i, entry.getValue()[i], (Double)values[i], 0.00001);
            }
        }
    }

    public void testROCCurvesInGrouped() throws Exception
    {
        ROCCurvesInGrouped analysis = new ROCCurvesInGrouped(null, "");
        ROCCurvesInGroupedParameters parameters = analysis.getParameters();
        setParameters(parameters);
        parameters.setOutputPath(vdc.getCompletePath());
        parameters.setGroupsNumber(2);

        DataCollection<?> result = analysis.justAnalyzeAndPut();
        assertEquals(100, analysis.getJobControl().getPreparedness());
        assertNotNull(result);
        assertEquals(vdc, result);
        TableDataCollection tdc = (TableDataCollection)vdc.get("ROCcurve_for_AR1_in_2.1.1.1.4_br_groupedBy_Peak score");
        assertNotNull(tdc);
        assertEquals(2, tdc.getSize());
        RowDataElement row = tdc.get("group_0");
        assertNotNull(row);
        row = tdc.get("group_1");
        assertNotNull(row);
        String expectedChart = "[[{\"data\":[[0,0],[0.004,0.02],[0.01,0.05],[0.018,0.08],"
                + "[0.022,0.1],[0.034,0.12],[0.038,0.15],[0.046,0.18],[0.058,0.2],[0.076,0.22],"
                + "[0.096,0.24],[0.098,0.26],[0.11,0.28],[0.14,0.3],[0.172,0.32],[0.182,0.34],"
                + "[0.186,0.36],[0.204,0.38],[0.234,0.4],[0.25,0.42],[0.252,0.44],[0.254,0.47],"
                + "[0.266,0.5],[0.278,0.52],[0.29,0.54],[0.296,0.56],[0.328,0.58],[0.334,0.6],"
                + "[0.342,0.62],[0.348,0.64],[0.362,0.66],[0.378,0.68],[0.42,0.7],[0.454,0.72],"
                + "[0.47,0.74],[0.528,0.76],[0.564,0.78],[0.58,0.81],[0.62,0.84],[0.692,0.86],"
                + "[0.832,0.88],[0.846,0.9],[0.87,0.92],[0.878,0.94],[0.88,0.96],[0.996,0.98],"
                + "[0.998,1],[1,1]],\"color\":\"rgb(0,0,255)\",\"label\":\"Common additive model\"},"
                + "{\"data\":[[0,0.02],[0.006,0.06],[0.03,0.08],[0.032,0.1],[0.036,0.12],[0.05,0.14],"
                + "[0.08,0.16],[0.086,0.18],[0.088,0.2],[0.09,0.22],[0.102,0.24],[0.11,0.26],"
                + "[0.114,0.28],[0.156,0.3],[0.172,0.32],[0.208,0.34],[0.214,0.36],[0.226,0.38],"
                + "[0.234,0.4],[0.254,0.42],[0.256,0.44],[0.268,0.46],[0.312,0.48],[0.32,0.5],"
                + "[0.366,0.52],[0.368,0.54],[0.388,0.56],[0.432,0.58],[0.442,0.6],[0.542,0.62],"
                + "[0.546,0.64],[0.58,0.66],[0.586,0.69],[0.594,0.73],[0.662,0.77],[0.664,0.8],"
                + "[0.68,0.82],[0.682,0.84],[0.688,0.86],[0.776,0.88],[0.82,0.9],[0.824,0.92],"
                + "[0.964,0.94],[0.966,0.96],[0.998,0.98],[1,1]],\"color\":\"rgb(255,0,0)\","
                + "\"label\":\"Common multiplicative model\"},{\"data\":[[0,0.01],"
                + "[0.014,0.04],[0.03,0.06],[0.054,0.09],[0.064,0.12],[0.09,0.14],"
                + "[0.094,0.18],[0.11,0.2],[0.118,0.22],[0.13,0.24],[0.134,0.27],"
                + "[0.196,0.3],[0.2,0.32],[0.204,0.34],[0.224,0.36],[0.228,0.38],"
                + "[0.248,0.4],[0.256,0.42],[0.27,0.44],[0.272,0.46],[0.274,0.48],"
                + "[0.328,0.5],[0.348,0.52],[0.394,0.55],[0.408,0.58],[0.422,0.6],"
                + "[0.428,0.64],[0.444,0.68],[0.448,0.7],[0.458,0.72],[0.478,0.74],"
                + "[0.498,0.76],[0.592,0.78],[0.604,0.8],[0.614,0.82],[0.67,0.84],"
                + "[0.698,0.86],[0.746,0.88],[0.762,0.9],[0.852,0.92],[0.916,0.94],"
                + "[0.922,0.96],[0.958,0.98],[0.962,1],[1,1]],\"color\":\"rgb(0,255,255)\","
                + "\"label\":\"MATCH model\"},{\"data\":[[0,0.02],[0.012,0.06],[0.018,0.09],"
                + "[0.026,0.13],[0.032,0.17],[0.054,0.2],[0.128,0.22],[0.134,0.24],[0.142,0.26],"
                + "[0.164,0.29],[0.186,0.32],[0.194,0.34],[0.198,0.36],[0.208,0.38],[0.224,0.4],"
                + "[0.226,0.42],[0.234,0.44],[0.242,0.46],[0.25,0.48],[0.292,0.5],[0.326,0.52],"
                + "[0.354,0.54],[0.36,0.56],[0.372,0.58],[0.388,0.6],[0.398,0.62],[0.4,0.65],"
                + "[0.412,0.68],[0.45,0.7],[0.47,0.72],[0.56,0.74],[0.586,0.76],[0.594,0.78],"
                + "[0.638,0.8],[0.652,0.82],[0.682,0.84],[0.728,0.86],[0.748,0.88],[0.77,0.9],"
                + "[0.808,0.92],[0.834,0.94],[0.856,0.96],[0.938,0.98],[0.958,1],[1,1]],"
                + "\"color\":\"rgb(128,128,128)\",\"label\":\"IPS model\"},{\"data\":[[0,0],"
                + "[0.002,0.02],[0.004,0.04],[0.018,0.06],[0.02,0.1],[0.054,0.14],"
                + "[0.074,0.16],[0.086,0.18],[0.092,0.21],[0.104,0.24],[0.17,0.27],"
                + "[0.172,0.3],[0.186,0.32],[0.236,0.34],[0.238,0.36],[0.28,0.38],"
                + "[0.282,0.4],[0.316,0.42],[0.332,0.44],[0.34,0.46],[0.346,0.48],"
                + "[0.36,0.5],[0.368,0.52],[0.394,0.54],[0.422,0.56],[0.446,0.58],"
                + "[0.454,0.6],[0.474,0.62],[0.502,0.64],[0.532,0.66],[0.578,0.68],"
                + "[0.584,0.7],[0.596,0.72],[0.6,0.74],[0.618,0.77],[0.632,0.8],"
                + "[0.718,0.82],[0.728,0.84],[0.76,0.87],[0.788,0.9],[0.95,0.92]," + "[0.956,0.94],[0.976,0.96],[0.998,0.99],[1,1]],"
                + "\"color\":\"rgb(255,255,0)\",\"label\":\"Multiplicative IPS model\"}],"
                + "{\"xaxis\":{\"min\":0,\"max\":1,\"label\":\"False discovery rate\"},"
                + "\"yaxis\":{\"min\":0,\"max\":1,\"label\":\"Sensitivity\"}}]";
        Chart chart = (Chart)row.getValues()[0];
        BSATestUtils.checkROCCurves(chart);
        assertEquals(expectedChart, chart.toString());
    }

    public void testIdenticalBestSiteROCCurves() throws Exception
    {
        IdenticalBestSiteROCCurves analysis = new IdenticalBestSiteROCCurves(null, "");
        IdenticalBestSiteROCCurvesParameters parameters = analysis.getParameters();
        setParameters(parameters);
        parameters.setOutputPath(vdc.getCompletePath());
        DataCollection<?> result = analysis.justAnalyzeAndPut();
        assertEquals(100, analysis.getJobControl().getPreparedness());
        assertNotNull(result);
        assertEquals(vdc, result);

        TableDataCollection tdc = (TableDataCollection)vdc.get("ROCcurves_for_AR1_in_2.1.1.1.4_br_size_55");
        assertNotNull(tdc);
        assertEquals(1, tdc.getSize());
        RowDataElement row = tdc.get("ROCcurves_for_AR1_in_2.1.1.1.4_br_size_55");
        Chart chart = (Chart)row.getValues()[0];
        assertNotNull(chart);
        BSATestUtils.checkROCCurves(chart);
        assertEquals(5, chart.getSeriesCount());

        double[][] expected = { {0.09090909090909091, 0.18181818181818182, 0.3333333333333333, 0.3939393939393939},
                {0.15151515151515152, 0.24242424242424243, 0.5151515151515151, 0.09090909090909091},
                {0.24242424242424243, 0.30303030303030304, 0.18181818181818182, 0.2727272727272727},
                {0.18181818181818182, 0.3333333333333333, 0.18181818181818182, 0.30303030303030304},
                {0.18181818181818182, 0.5757575757575758, 0.09090909090909091, 0.15151515151515152},
                {0.18181818181818182, 0.45454545454545453, 0.21212121212121213, 0.15151515151515152},
                {0.8181818181818182, 0.030303030303030304, 0.06060606060606061, 0.09090909090909091},
                {0.06060606060606061, 0.030303030303030304, 0.9090909090909091, 0.0},
                {0.5454545454545454, 0.18181818181818182, 0.21212121212121213, 0.06060606060606061},
                {0.3939393939393939, 0.3333333333333333, 0.18181818181818182, 0.09090909090909091},
                {0.12121212121212122, 0.7575757575757576, 0.030303030303030304, 0.09090909090909091},
                {0.7878787878787878, 0.09090909090909091, 0.06060606060606061, 0.06060606060606061},
                {0.030303030303030304, 0.3333333333333333, 0.3333333333333333, 0.30303030303030304},
                {0.15151515151515152, 0.48484848484848486, 0.15151515151515152, 0.21212121212121213},
                {0.3939393939393939, 0.18181818181818182, 0.2727272727272727, 0.15151515151515152},
                {0.030303030303030304, 0.09090909090909091, 0.18181818181818182, 0.696969696969697},
                {0.24242424242424243, 0.2727272727272727, 0.36363636363636365, 0.12121212121212122},
                {0.15151515151515152, 0.12121212121212122, 0.12121212121212122, 0.6060606060606061},
                {0.09090909090909091, 0.06060606060606061, 0.2727272727272727, 0.5757575757575758},
                {0.0, 0.6666666666666666, 0.21212121212121213, 0.12121212121212122},
                {0.09090909090909091, 0.12121212121212122, 0.06060606060606061, 0.7272727272727273},
                {0.15151515151515152, 0.5757575757575758, 0.15151515151515152, 0.12121212121212122},
                {0.3333333333333333, 0.45454545454545453, 0.030303030303030304, 0.18181818181818182},
                {0.24242424242424243, 0.3939393939393939, 0.09090909090909091, 0.2727272727272727},
                {0.24242424242424243, 0.3939393939393939, 0.09090909090909091, 0.2727272727272727},
                {0.18181818181818182, 0.15151515151515152, 0.24242424242424243, 0.42424242424242425},
                {0.09090909090909091, 0.30303030303030304, 0.48484848484848486, 0.12121212121212122}};
        TestBindingRegionAnalyses.compareMatrix(expected, outputMatrixLib.get("AR1_revised_br"));
    }
}
