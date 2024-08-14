/* $Id$ */

package biouml.plugins.machinelearning.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.apache.commons.lang.ArrayUtils;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.bsa.Interval;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.util.ListUtil;
import biouml.plugins.machinelearning.distribution_mixture.NormalMixture;
import biouml.plugins.machinelearning.utils.MetaAnalysis.RankAggregation;
import biouml.plugins.machinelearning.utils.StatUtils.Distributions.ChiSquaredDistribution;
import biouml.plugins.machinelearning.utils.StatUtils.Distributions.NormalDistribution;
import biouml.plugins.machinelearning.utils.StatUtils.UnivariateSample.DensityEstimation;
import biouml.plugins.machinelearning.utils.StatUtils.UnivariateSample;
import biouml.plugins.machinelearning.utils.StatUtils.UnivariateSamples;
import biouml.plugins.machinelearning.utils.UtilsGeneral;

/**
 * @author yura
 *
 */

public class ExploratoryUtils
{
    /****************** StatisticalTests : start ******************************/
    // TODO: To test all methods in this class and re-write them (To change method Stat.chiDistribution() and save it into class DistributionFunctions)
    public static class StatisticalTests
    {
        public static double[] getPoissonTestBrownAndZhao(double[] sample)
        {
            double[] sampleTransformed = new double[sample.length];
            double constant = 3.0 / 8.0;
            for( int i = 0; i < sample.length; i++ )
                sampleTransformed[i] = Math.sqrt(sample[i] + constant);
            double statistic = 4.0 * PrimitiveOperations.getSumOfSquaresCentered(sampleTransformed)[1];
            return new double[]{statistic, 1.0 - ChiSquaredDistribution.getDistributionFunction(statistic, sample.length - 1, 100)};
        }
        
        public static double[] getPoissonTestLikelihoodRatio(double[] sample)
        {
            double statistic = 0.0, mean = PrimitiveOperations.getAverage(sample);
            for( int i = 0; i < sample.length; i++ )
                statistic += sample[i] * Math.log(sample[i] / mean); // TODO: to optimize run time
            statistic *= 2.0;
            return new double[]{statistic, 1.0 - ChiSquaredDistribution.getDistributionFunction(statistic, sample.length - 1, 100)};
        }
        
        public static double[] getPoissonTestConditionalChiSquared(double[] sample)
        {
            double[] meanAndSumOfSquaresCentered = PrimitiveOperations.getSumOfSquaresCentered(sample);
            double statistic = meanAndSumOfSquaresCentered[1] / meanAndSumOfSquaresCentered[0];
            return new double[]{statistic, 1.0 - ChiSquaredDistribution.getDistributionFunction(statistic, sample.length - 1, 100)};
        }
        
        public static double[] getPoissonTestNeymanScott(double[] sample)
        {
            double[] meanAndVariance = UnivariateSample.getMeanAndVariance(sample);
            double statistic = Math.sqrt(0.5 * (double)(sample.length - 1)) * (meanAndVariance[1] / meanAndVariance[1] - 1.0);
            double pvalue = 2.0 * (1.0 - NormalDistribution.getDistributionFunction(Math.abs(statistic)));
            return new double[]{statistic, pvalue};
        }
        
        public static DataMatrix getPoissonTests(double[] sample)
        {
            double[] statisticsAndPvalues = getPoissonTestBrownAndZhao(sample);
            statisticsAndPvalues = ArrayUtils.addAll(statisticsAndPvalues, getPoissonTestLikelihoodRatio(sample));
            statisticsAndPvalues = ArrayUtils.addAll(statisticsAndPvalues, getPoissonTestConditionalChiSquared(sample));
            statisticsAndPvalues = ArrayUtils.addAll(statisticsAndPvalues, getPoissonTestNeymanScott(sample));
            String[] columnNames = new String[]{"Brown_and_Zhao_statistic", "Brown_and_Zhao_p_value", "Likelihood_ratio_statistic", "Likelihood_ratio_p_value", "Conditional_chi_squared_statistic", "Conditional_chi_squared_p_value", "Neyman_Scott_statistic", "Neyman_Scott_p_value"};
            return new DataMatrix("values", columnNames, statisticsAndPvalues);
        }
    }
    /****************** StatisticalTests : end ********************************/

    /****************** For Fantom TSS's splitting : start ********************/
    // to create sums for closed TSSs
    public static void modifyDataMatrices(int distanceThreshold)
    {
        // 1.
        Object[] objects = getIndicesForSingleTsssAndIndicesForSums(distanceThreshold);
        int[] indicesForSingleTss = (int[])objects[0];
        int[][] indicesForSums = (int[][])objects[1];
        
        // 2.
        log.info("indicesForSingleTss.length = " + indicesForSingleTss.length + " indicesForSums.length = " + indicesForSums.length);
        for( int i = 0; i < 100; i++ )
            log.info("i = " + i + " indicesForSingleTss = " + indicesForSingleTss[i]);
        for( int i = 0; i < 10; i++ )
        {
            String s = "";
            for( int j = 0; j < indicesForSums[i].length; j++ )
                s += " " + Integer.toString(indicesForSums[i][j]);
            s = "indicesForSums[" + Integer.toString(i) + "] =" + s;
            log.info(s);
        }
        
        // 3.
        DataElementPath pathToFolderWithDataMatrix = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Answer02_indicator_matrices_in_cells_no_treatment/HepG2_with_QC_3.0_macs2_overlap");
        String dataMatrixName = "data_matrix_extended";
        String[] columnNamesForSums = new String[]{"Cell_1424_lg", "Cell_1425_lg", "Cell_1426_lg"};
        DataElementPath pathToOutputFolder = pathToFolderWithDataMatrix;
        modifyDataMatrix(pathToFolderWithDataMatrix, dataMatrixName, indicesForSingleTss, indicesForSums, columnNamesForSums, pathToOutputFolder);
        
        pathToFolderWithDataMatrix = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Answer02_indicator_matrices_in_cells_no_treatment/K562_with_QC_3.0_macs2_overlap");
        dataMatrixName = "data_matrix_extended";
        columnNamesForSums = new String[]{"Cell_1327", "Cell_1328", "Cell_1329", "Cell_1330"};
        pathToOutputFolder = pathToFolderWithDataMatrix;
        modifyDataMatrix(pathToFolderWithDataMatrix, dataMatrixName, indicesForSingleTss, indicesForSums, columnNamesForSums, pathToOutputFolder);
        
        pathToFolderWithDataMatrix = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Answer02_indicator_matrices_in_cells_no_treatment/HEK293_with_QC_3.0_macs2_overlap");
        dataMatrixName = "data_matrix_extended_2";
        columnNamesForSums = new String[]{"Cell_1356"};
        pathToOutputFolder = pathToFolderWithDataMatrix;
        modifyDataMatrix(pathToFolderWithDataMatrix, dataMatrixName, indicesForSingleTss, indicesForSums, columnNamesForSums, pathToOutputFolder);
    }

    private static void modifyDataMatrix(DataElementPath pathToFolderWithDataMatrix, String dataMatrixName, int[] indicesForSingleTss, int[][] indicesForSums, String[] columnNamesForSums, DataElementPath pathToOutputFolder)
    {
        // 1.
        DataMatrix dm = new DataMatrix(pathToFolderWithDataMatrix.getChildPath(dataMatrixName), null);
//        DataMatrix dmTruncated = (DataMatrix)DataMatrix.splitRowWise(dm, null, null, indicesForSingleTss)[0];
//        dmTruncated.writeDataMatrix(true, pathToOutputFolder, dataMatrixName + "_with_singles", log);
        for( int i = 0; i < columnNamesForSums.length; i++ )
        {
            double[] column = dm.getColumn(columnNamesForSums[i]);
            modifyColumn(column, indicesForSums);
            dm.fillColumn(column, columnNamesForSums[i]);
            dm.replaceColumnName(columnNamesForSums[i], columnNamesForSums[i] + "_with_sums_correct");
        }
        dm.writeDataMatrix(true, pathToOutputFolder, dataMatrixName + "_with_sums_correct", log);
        
        // 2.
        dm = new DataMatrix(pathToFolderWithDataMatrix.getChildPath(dataMatrixName), null);
        for( int i = 0; i < columnNamesForSums.length; i++ )
        {
            double[] column = dm.getColumn(columnNamesForSums[i]);
            modifyColumn2(column, indicesForSums);
            dm.fillColumn(column, columnNamesForSums[i]);
            dm.replaceColumnName(columnNamesForSums[i], columnNamesForSums[i] + "_with_averages");
        }
        dm.writeDataMatrix(true, pathToOutputFolder, dataMatrixName + "_with_averages", log);
    }
    
    private static void modifyColumn(double[] column, int[][] indicesForSums)
    {
        for( int i = 0; i < indicesForSums.length; i++ )
        {
            double sum = 0;
            for( int j = 0; j < indicesForSums[i].length; j++ )
            {
                double x = column[indicesForSums[i][j]] < 0.000001 ? 0 : Math.pow(10.0, column[indicesForSums[i][j]]);
                sum += x;
            }
            if( sum >= 0.999 )
                sum = Math.log10(sum);
            for( int j = 0; j < indicesForSums[i].length; j++ )
                column[indicesForSums[i][j]] = sum;
        }
    }
    
    private static void modifyColumn2(double[] column, int[][] indicesForSums)
    {
        for( int i = 0; i < indicesForSums.length; i++ )
        {
            //double[] array = new double[indicesForSums[i].length];
            double x = 0;
            for( int j = 0; j < indicesForSums[i].length; j++ )
                x += column[indicesForSums[i][j]];
            x /= (double)indicesForSums[i].length;
            for( int j = 0; j < indicesForSums[i].length; j++ )
                column[indicesForSums[i][j]] = x;
        }
    }

    private static Object[] getIndicesForSingleTsssAndIndicesForSums(int distanceThreshold)
    {
        // 1. Create fantomSites.
        DataElementPath pathToFileWithFantomSites = DataElementPath.create("data/Collaboration/yura_project/Data/Tables/Fantom5_hg38/Data/Converted_from_initial/TSSs_CAGE_peaks");
        DataMatrixString dms = new DataMatrixString(pathToFileWithFantomSites, new String[]{"chromosome", "strand", "TSS_start"});
        int n = dms.getRowNames().length;
        String[][] matrix = dms.getMatrix();
        FantomSite[] fantomSites = new FantomSite[n];
        for( int i = 0; i < n; i++ )
        {
            int position = Integer.parseInt(matrix[i][2]);
            fantomSites[i] = new FantomSite(matrix[i][0], new Interval(position, position), Integer.parseInt(matrix[i][1]), i);
        }
        log.info("O.K.1: fantomSites are created");
        
        // 2. Split fantomSites into distinct strands (i.e. into sitesWithPositiveStrand and sitesWithNegativeStrand).
        List<FantomSite> list1 = new ArrayList<>(), list2 = new ArrayList<>();
        for( FantomSite fs : fantomSites )
        {
            if( fs.getStrand() == 2 )
                list1.add(fs);
            else
                list2.add(fs);
        }
        Map<String, List<FantomSite>> sitesWithPositiveStrand = transformToMapAndSortThem(list1.toArray(new FantomSite[0])), sitesWithNegativeStrand = transformToMapAndSortThem(list2.toArray(new FantomSite[0]));
        
        // 3. Calculate final indices.
        Object[] objects = getIndicesForSingleTsssAndIndicesForSums(sitesWithPositiveStrand, distanceThreshold);
        List<Integer> indicesForSingleTss = (List<Integer>)objects[0];
        List<int[]> indicesForSums = (List<int[]>)objects[1];
        objects = getIndicesForSingleTsssAndIndicesForSums(sitesWithNegativeStrand, distanceThreshold);
        List<Integer> list11 = (List<Integer>)objects[0];
        List<int[]> list22 = (List<int[]>)objects[1];
        if( ! list11.isEmpty() )
            indicesForSingleTss.addAll(list11);
        if( ! list22.isEmpty() )
            indicesForSums.addAll(list22);
        int[][] mat = indicesForSums.isEmpty() ? new int[0][0] : indicesForSums.toArray(new int[indicesForSums.size()][]);
        return new Object[]{UtilsGeneral.fromListIntegerToArray(indicesForSingleTss), mat};
    }
    
    // Calculate indices for all chromosomes 
    private static Object[] getIndicesForSingleTsssAndIndicesForSums(Map<String, List<FantomSite>> fantomSites, int distanceThreshold)
    {
        List<Integer> indicesForSingleTss = new ArrayList<>();
        List<int[]> indicesForSums = new ArrayList<>();
        for( Entry<String, List<FantomSite>> entry : fantomSites.entrySet() )
        {
            List<FantomSite> list = entry.getValue();
            Object[] objects = getIndicesForSingleTsssAndIndicesForSums(list, distanceThreshold);
            List<Integer> indicesForSingleTssFromChromosome = (List<Integer>)objects[0];
            List<int[]> indicesForSumsFromChromosome = (List<int[]>)objects[1];
            if( ! indicesForSingleTssFromChromosome.isEmpty() )
                indicesForSingleTss.addAll(indicesForSingleTssFromChromosome);
            if( ! indicesForSumsFromChromosome.isEmpty() )
                indicesForSums.addAll(indicesForSumsFromChromosome);
            log.info("O.K.2: chromosome = " + entry.getKey());
        }
        return new Object[]{indicesForSingleTss, indicesForSums};
    }

    // Calculate indices for given chromosome 
    private static Object[] getIndicesForSingleTsssAndIndicesForSums(List<FantomSite> fantomSites, int distanceThreshold)
    {
        // 1. Treat degenerate List.
        List<Integer> indicesForSingleTss = new ArrayList<>();
        List<int[]> indicesForSums = new ArrayList<>();
        if( fantomSites.size() == 1 )
        {
            indicesForSingleTss.add(fantomSites.get(0).getIndex());
            return new Object[]{indicesForSingleTss, indicesForSums};
        }
        
        // 2. Calculate positions and initialIndices.
        int n = fantomSites.size();
        int[] positions = new int[n], initialIndices = new int[n];
        for( int i = 0; i < n; i++ )
        {
            FantomSite fs = fantomSites.get(i);
            positions[i] = fs.getStartPosition();
            initialIndices[i] = fs.getIndex();
        }
        
        // 3. Calculate indicesForSingleTss.
        if( positions[1] - positions[0] > distanceThreshold )
            indicesForSingleTss.add(initialIndices[0]);
        if( positions[n - 1] - positions[n - 2] > distanceThreshold )
            indicesForSingleTss.add(initialIndices[n - 1]);
        for( int i = 1; i < n - 1; i++ )
            if( positions[i] - positions[i - 1] > distanceThreshold && positions[i + 1] - positions[i] > distanceThreshold )
                indicesForSingleTss.add(initialIndices[i]);
        
        // 4. Calculate indicesForSums.
        List<Integer> list = new ArrayList<>();
        list.add(initialIndices[0]);
        for( int i = 1; i < n; i++ )
        {
            if( positions[i] - positions[i - 1] <= distanceThreshold )
            {
                list.add(initialIndices[i]);
                continue;
            }
            if( list.size() > 1 )
                indicesForSums.add(UtilsGeneral.fromListIntegerToArray(list));
            list.clear();
            list.add(initialIndices[i]);
        }
        if( list.size() > 1 )
            indicesForSums.add(UtilsGeneral.fromListIntegerToArray(list));
        return new Object[]{indicesForSingleTss, indicesForSums};
    }

    private static Map<String, List<FantomSite>> transformToMapAndSortThem(FantomSite[] fantomSites)
    {
        Map<String, List<FantomSite>> result = new HashMap<>();
        for( FantomSite fs : fantomSites )
            result.computeIfAbsent(fs.getChromosomeName(), key -> new ArrayList<>()).add(fs);
        ListUtil.sortAll(result);
        return result;
    }
    
    private static class FantomSite implements Comparable<FantomSite>
    {
        private String chromosomeName;
        private Interval coordinates;
        private int strand, index;
        
        public FantomSite(String chromosomeName, Interval coordinates, int strand, int index)
        {
            this.chromosomeName = chromosomeName;
            this.coordinates = coordinates;
            this.strand = strand;
            this.index = index;
        }

        @Override
        public int compareTo(FantomSite o)
        {
            return coordinates.compareTo(o.coordinates);
        }
        
        public String getChromosomeName()
        {
            return chromosomeName;
        }
        
        public int getStrand()
        {
            return strand;
        }
        
        public int getIndex()
        {
            return index;
        }
        
        public int getStartPosition()
        {
            return coordinates.getFrom();
        }
    }
    /****************** For Fantom TSS's splitting : end ***********************/

    /****************** For article on quality control : start ****************/
    public static DataMatrix changeDataMatrix(DataMatrix dataMatrix)
    {
        double[] column = dataMatrix.getColumn("FPCM");
        double[][] matrix = dataMatrix.getMatrix();
        String[] rowNames = dataMatrix.getRowNames(); 
        List<double[]> mat = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for( int i = 0; i < matrix.length; i++ )
            if( ! Double.isNaN(column[i]) && column[i] >= 0.9 )
            {
                mat.add(matrix[i]);
                names.add(rowNames[i]);
            }
        return new DataMatrix(names.toArray(new String[0]), dataMatrix.getColumnNames(), mat.toArray(new double[mat.size()][]));
    }
    
 // temp: for article about quality metrics
//  DataMatrix dmm = new DataMatrix(pathToDataMatrix, null);
//  double[] fpcm = dmm.getColumn("FPCM"), stat = dmm.getColumn("statistic2");
//  ExploratoryUtils.getGroupedValues(fpcm, stat);
////  dmm = ExploratoryUtils.changeDataMatrix(dmm);
////  dmm.writeDataMatrix(false, pathToOutputFolder, "quality_control_metrics_truncated", log);
    public static void getGroupedValues(double[] x, double[] y)
    {
        // 1.
        double[][] intervals = new double[][]{new double[]{0.0, 0.5}, new double[]{0.5, 0.8}, new double[]{0.5, 1.5}, new double[]{1.5, 3.0},
                new double[]{3.0, 10.0}, new double[]{10.0, 30.0}, new double[]{30.0, 100.0}, new double[]{100.0, 1.0e+20},
                new double[]{0.8, 1.5}, new double[]{0.9, 1.5}, new double[]{0.9, 3.0}, new double[]{0.8, 3.0}};
        
        // 2.
        double[][] result = new double[intervals.length][4];
        for( int i = 0; i < x.length; i++ )
            if( ! Double.isNaN(x[i]) && ! Double.isNaN(y[i]) )
                for( int j = 0; j < intervals.length; j++ )
                    if( intervals[j][0] <= x[i] && x[i] <= intervals[j][1] )
                    {
                        result[j][2] += y[i];
                        result[j][3] += 1.0;
                    }
        
        // 3.
        for( int j = 0; j < intervals.length; j++ )
        {
            result[j][0] = intervals[j][0];
            result[j][1] = intervals[j][1];
            if( result[j][3] > 0.0 )
                result[j][2] /= result[j][3];
        }

        // 4.
        for( int i = 0; i < result.length; i++ )
            log.info("i = " + i + " interval = " + result[i][0] + " " + result[i][1] + " y_mean_i = " + result[i][2] + " ni = " + result[i][3]);
    }
    /****************** For article on quality control : end *****************************/
    
    /****************** For cistrom construction: start **********************************/
//    public static void treatRankAggregationScoresByNormalMixture(DataElementPath pathToInputFolder, String[] fileNames, DataElementPath pathToOutputFolder)
//    {
//        for( String fileName : fileNames )
//        {
//            // 1. Normal mixture.
//            log.info("fileName = " + fileName);
//            DataMatrix dm = new DataMatrix(pathToInputFolder.getChildPath(fileName), new String[]{RankAggregation.RA_SCORE});
//            double[] scores = dm.getColumn(0);
//            NormalMixture normalMixture = new NormalMixture(scores, 2, null,null, 300);
//            dm = normalMixture.getParametersOfComponents();
//            dm.writeDataMatrix(false, pathToOutputFolder, fileName + "_mixture_parameters", log);
//            Chart chart = normalMixture.createChartWithDensities(RankAggregation.RA_SCORE);
//            TableAndFileUtils.addChartToTable("chart with RA-scores", chart, pathToOutputFolder.getChildPath(fileName + "_chart_mixture"));
//            
//            // 2. Univariate samples
//            DataMatrixString dms = new DataMatrixString(pathToInputFolder.getChildPath(fileName), new String[]{"Indicator"});
//            String[] samplesNames = dms.getColumn(0);
//            UnivariateSamples uss = new UnivariateSamples(samplesNames, scores);
//            dm = uss.getSimpleCharacteristicsOfSamples();
//            dm.writeDataMatrix(false, pathToOutputFolder, fileName + "_simple_characteristics", log);
//            chart = uss.createChartWithSmoothedDensities(RankAggregation.RA_SCORE, true, DensityEstimation.WINDOW_WIDTH_01, null);
//            TableAndFileUtils.addChartToTable("chart with RA-scores", chart, pathToOutputFolder.getChildPath(fileName + "_chart_densities"));
//        }
//    }
    
    private static Logger log = Logger.getLogger(ExploratoryUtils.class.getName());
}
