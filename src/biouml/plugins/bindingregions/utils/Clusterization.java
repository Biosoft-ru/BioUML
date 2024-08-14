
package biouml.plugins.bindingregions.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;

import biouml.plugins.bindingregions.rscript.Rutils;
import biouml.plugins.bindingregions.utils.MatrixUtils.Distance;
import biouml.plugins.bindingregions.utils.StatUtil.DensityEstimation;
import one.util.streamex.EntryStream;
import one.util.streamex.IntCollector;
import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysis.Stat;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.Maps;

/**
 * @author yura
 * Implementation of clustering algorithms
 *
 */
public class Clusterization
{
    public static final String CLUSTER = "Cluster_";

    /***
     * i-th row of dataMatrix represents i-th object for clusterization
     * j-th column of dataMatrix represents j-th variable
     * dim(variableNames) = dim(dataMatrix[0])
     * (i-th object belongs to m-th cluster) <=> (indicesOfClusters[i] = m)
     */
    private double[][] dataMatrix; // dim = n x m; n objects and m variables
    private int[] indicesOfClusters; // dim = n; indicesOfClusters[i] = j <=> i-th object belong to j-th cluster
    
    public Clusterization(double[][] dataMatrix, int[] indicesOfClusters)
    {
        this.dataMatrix = dataMatrix;
        this.indicesOfClusters = indicesOfClusters;
    }

    public Map<Integer, double[][]> getNewClusterIndexAndDataMatrix()
    {
        return Maps.<Integer, int[], double[][]> transformValues( getClusterIndexAndObjectIndices(),
                objectIndices -> IntStreamEx.of( objectIndices ).elements( dataMatrix ).toArray( double[][]::new ) );
    }
    
    public static Set<Integer> getDistinctIndicesOfClusters(int[] indicesOfClusters)
    {
        return IntStreamEx.of(indicesOfClusters).boxed().toSet();
    }
    
    private static double getDistanceBetweenClusterCenters(String distanceType, double[] clusterCenter1, double[] clusterCenter2) throws Exception
    {
        return Distance.getDistance(distanceType, clusterCenter1, clusterCenter2);
    }
    
    private Map<Integer, int[]> getClusterIndexAndObjectIndices()
    {
        return IntStreamEx.ofIndices(indicesOfClusters).collect( IntCollector.groupingBy( i -> indicesOfClusters[i]) );
    }

    /***
     * 
     * @return lower triangular part of distance matrix
     * @throws Exception 
     ***/
    private static double[][] getDistanceMatrixBetweenClusterCenters(String distanceType, Map<?, double[]> keyAndClusterCenters) throws Exception
    {
        double[][] result = MatrixUtils.getLowerTriangularMatrix(keyAndClusterCenters.size());
        int index1 = 0;
        for( double[] center1 : keyAndClusterCenters.values() )
        {
            int index2 = 0;
            for( double[] center2 : keyAndClusterCenters.values() )
            {
                if( index2 > index1 ) break;
                else
                    result[index1][index2++] = getDistanceBetweenClusterCenters(distanceType, center1, center2);
            }
            index1++;
        }
        return result;
    }
    
    private static Map<String, double[][]> getClusterNameAndTransposedClusterDataMatrix(double[][] dataMatrix, int[] indicesOfClusters)
    {
        return IntStreamEx.of(indicesOfClusters).distinct()
                .<String, double[][]> mapToEntry(
                        index -> CLUSTER + index,
                        index -> IntStreamEx.ofIndices(indicesOfClusters, idx -> idx == index)
                                .elements(dataMatrix).toArray(double[][]::new))
//                .mapValues(MatrixUtils::getTransposedMatrix).toMap();
                .mapValues(matrix -> MatrixUtils.getTransposedMatrix(matrix)).toMap();
    }
    
    // O.K.
    public static void writeChartsWithVariableDensities(double[][] dataMatrix, String[] variableNames, int[] indicesOfClusters, Boolean doAddTwoZeroPoints, String windowSelector, Double givenWindow, DataElementPath pathToChartTable)
    {
        Map<String, double[][]> clusterNameAndTransposedClusterDataMatrix = getClusterNameAndTransposedClusterDataMatrix(dataMatrix, indicesOfClusters);
        Map<String, Double> nameAndMultipliers = new HashMap<>();
        for( Entry<String, double[][]> entry : clusterNameAndTransposedClusterDataMatrix.entrySet() )
            nameAndMultipliers.put(entry.getKey(), (double)entry.getValue()[0].length / dataMatrix.length);
        for( int j : IntStreamEx.ofIndices(variableNames).boxed() )
        {
            Map<String, double[]> nameAndSample = Maps.transformValues( clusterNameAndTransposedClusterDataMatrix, sample -> sample[j] );
            nameAndSample.put("Whole sample", MatrixUtils.getColumn(dataMatrix, j));
            Chart chart = DensityEstimation.chartWithSmoothedDensities(nameAndSample, variableNames[j], doAddTwoZeroPoints, nameAndMultipliers, windowSelector, givenWindow);
            TableUtils.addChartToTable(variableNames[j], chart, pathToChartTable);
        }
    }

    // old version: example of StreamEx.ofKeys()
    /***
    public static void writeTableWithDistancesBetweenClusterCenters(String distanceType, double[][] dataMatrix, int[] indicesOfClusters, DataElementPath pathToOutputs, String tableName) throws Exception
    {
        Map<Integer, double[][]> clusterIndexAndDataMatrix = createClusterIndexAndDataMatrix(dataMatrix, indicesOfClusters);
        Map<String, double[]> nameAndCenters = EntryStream.of(getClusterCenters(clusterIndexAndDataMatrix)).mapKeys(clusterIndex -> CLUSTER + clusterIndex).toSortedMap();
        double[][] distanceMatrix = getDistanceMatrixBetweenClusterCenters(distanceType, nameAndCenters);
        double[][] squareDistanceMatrix = MatrixUtils.transformSymmetricMatrixToSquareMatrix(distanceMatrix);
        String[] names = StreamEx.ofKeys(nameAndCenters).toArray(String[]::new);
        TableUtils.writeDoubleTable(squareDistanceMatrix, names, names, pathToOutputs, tableName);
    }
    ***/

    // new version
    public static void writeTableWithDistancesBetweenClusterCenters(String distanceType, double[][] dataMatrix, int[] indicesOfClusters, DataElementPath pathToOutputs, String tableName) throws Exception
    {
        Map<Integer, double[][]> clusterIndexAndDataMatrix = createClusterIndexAndDataMatrix(dataMatrix, indicesOfClusters);
        Map<String, double[]> nameAndCenters = EntryStream.of(getClusterCenters(clusterIndexAndDataMatrix)).mapKeys(clusterIndex -> CLUSTER + clusterIndex).toSortedMap();
        double[][] distanceMatrix = getDistanceMatrixBetweenClusterCenters(distanceType, nameAndCenters);
        String[] names = StreamEx.ofKeys(nameAndCenters).toArray(String[]::new);
        TableUtils.writeTableWithSquareSymmetricMatrix(names, distanceMatrix, pathToOutputs, tableName);
    }

    // O.K.
    // centers are determined as sample means
    public static Map<Integer, double[]> getClusterCenters(Map<Integer, double[][]> clusterIndexAndDataMatrix)
    {
        return Maps.transformValues(clusterIndexAndDataMatrix, MultivariateSample::getMeanVector);
    }

    // O.K.
    public static Map<Integer, double[][]> createClusterIndexAndDataMatrix(double[][] dataMatrix, int[] indicesOfClusters)
    {
        return Maps.<Integer, int[], double[][]> transformValues(getClusterIndexAndObjectIndices(indicesOfClusters), objectIndices -> IntStreamEx.of(objectIndices).elements(dataMatrix).toArray(double[][]::new));
    }

    // O.K.
    public static Map<Integer, int[]> getClusterIndexAndObjectIndices(int[] indicesOfClusters)
    {
        return IntStreamEx.ofIndices(indicesOfClusters).collect(IntCollector.groupingBy(i -> indicesOfClusters[i]));
    }

    // O.K.
    public static void writeTableWithMeansAndSigmas(double[][] dataMatrix, String[] variableNames, int[] indicesOfClusters, DataElementPath pathToOutputs, String tableName)
    {
        Map<String, double[][]> clusterNameAndTransposedClusterDataMatrix = getClusterNameAndTransposedClusterDataMatrix(dataMatrix, indicesOfClusters);
        clusterNameAndTransposedClusterDataMatrix.put("Whole sample", dataMatrix);
        double[][] data = new double[clusterNameAndTransposedClusterDataMatrix.size()][2 * variableNames.length + 1];
        String[] namesOfRows = new String[clusterNameAndTransposedClusterDataMatrix.size()];
        int i = 0;
        for( Entry<String, double[][]> entry : clusterNameAndTransposedClusterDataMatrix.entrySet() )
        {
            String clusterName = entry.getKey();
            double[][] sample = entry.getValue();
            data[i][2 * variableNames.length] = clusterName.equals("Whole sample") ? MatrixUtils.getColumn(sample, 0).length : sample[0].length;
            for( int j = 0; j < variableNames.length; j++ )
            {
                double[] values = clusterName.equals("Whole sample") ? MatrixUtils.getColumn(sample, j) : sample[j];
                double[] meanAndSigma = Stat.getMeanAndSigma(values);
                data[i][2 * j] = meanAndSigma[0];
                data[i][2 * j + 1] = meanAndSigma[1];
            }
            namesOfRows[i++] = clusterName;
        }
        String[] namesOfColumns = StreamEx.of( variableNames )
                .flatMap( name -> Stream.of( "Mean of (" + name + ")", "Sigma of (" + name + ")" ) ).append( "Size" )
                .toArray( String[]::new );
        TableUtils.writeDoubleTable(data, namesOfRows, namesOfColumns, pathToOutputs, tableName);
    }
    
    // O.K.
    public static int[] transformMembershipProbabilitiesToIndicesOfCusters(double[][] membershipProbabilities)
    {
        int[] indicesOfClusters = new int[membershipProbabilities.length];
        for( int i = 0; i < membershipProbabilities.length; i++ )
            indicesOfClusters[i] = (int)MatrixUtils.getMaximalValue(membershipProbabilities[i])[1];
        return indicesOfClusters;
    }

    // O.K.
    public static void writeMembershipProbabilitiesIntoTable(double[][] membershipProbabilities, String[] objectNames, DataElementPath pathToOutputs, String tableName)
    {
        String[] namesOfColumns = new String[membershipProbabilities[0].length];
        for( int i = 0; i < membershipProbabilities[0].length; i++ )
            namesOfColumns[i] = CLUSTER + Integer.toString(i);
        TableUtils.writeDoubleTable(membershipProbabilities, objectNames, namesOfColumns, pathToOutputs, tableName);
    }
    
    public static TableDataCollection writeDoubleDataMatrixAndClusterNamesIntoTable(double[][] dataMatrix, int[] indicesOfClusters, String[] namesOfRows, String[] namesOfDataColumns, String nameOfClusterNameColumn, DataElementPath pathToOutputs, String tableName)
    {
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(pathToOutputs.getChildPath(tableName));
        for( int j = 0; j < namesOfDataColumns.length; j++ )
            table.getColumnModel().addColumn(namesOfDataColumns[j].replace('/', '|'), Double.class);
        table.getColumnModel().addColumn(nameOfClusterNameColumn.replace('/', '|'), String.class);
        for( int i = 0; i < namesOfRows.length; i++ )
        {
            Object[] row = new Object[namesOfDataColumns.length + 1];
            for( int j = 0; j < namesOfDataColumns.length; j++ )
                row[j] = dataMatrix[i][j];
            row[namesOfDataColumns.length] = CLUSTER + indicesOfClusters[i]; 
            TableDataCollectionUtils.addRow(table, namesOfRows[i].replace('/', '|'), row, true);
        }
        table.finalizeAddition();
        CollectionFactoryUtils.save(table);
        return table;
    }
    
    // O.K.
    // initialize initial clusters (i.e. indices of clusters) randomly
    public static int[] initializeInitialIndicesOfClustersRandomly(int numberOfClusters, int numberOfObjects)
    {
        int[] result = new int[numberOfObjects];
        Random random = new Random(1);
        boolean success = false;
        while( ! success )
        {
            for( int i = 0; i < numberOfObjects; i++ )
                result[i] = random.nextInt(numberOfClusters);
            Set<Integer> indices = getDistinctIndicesOfClusters(result);
            success = IntStreamEx.range(numberOfClusters).allMatch(indices::contains);
        }
        return result;
    }

    // O.K.
    public static double[][] initializeInitialMembershipProbabilitiesRandomly(int numberOfClusters, int numberOfObjects)
    {
        double[][] result = new double[numberOfObjects][numberOfClusters];
        int[] indicesOfClusters = initializeInitialIndicesOfClustersRandomly(numberOfClusters, numberOfObjects);
        for( int i = 0; i < numberOfObjects; i++ )
            result[i][indicesOfClusters[i]] = 1.0;
        return result;
    }
    
    /************************* KMeansAlgorithm : start *************************/
    public static class KMeansAlgorithm
    {
        private double[][] dataMatrix; // dim = n x m; n objects and m variables
        private int[] indicesOfClusters; // dim = n; indicesOfClusters[i] = j <=> i-th object belong to j-th cluster
        private String distanceType;
        private Map<Integer, double[][]> clusterIndexAndDataMatrix;
        
        public KMeansAlgorithm(double[][] dataMatrix, int[] indicesOfClusters, int numberOfClusters, String distanceType)
        {
            this.dataMatrix = dataMatrix;
            this.indicesOfClusters = indicesOfClusters != null ? indicesOfClusters : initializeInitialIndicesOfClustersRandomly(numberOfClusters, dataMatrix.length);
            this.distanceType = distanceType;
            this.clusterIndexAndDataMatrix = createClusterIndexAndDataMatrix(dataMatrix, this.indicesOfClusters);
        }
        
        // centers are determined as sample means
        private Map<Integer, double[]> getClusterCenters()
        {
            return Clusterization.getClusterCenters(clusterIndexAndDataMatrix);
        }
        
        private double getDistanceBetweenObjectAndClusterCenter(int indexOfObject, double[] clusterCenter) throws Exception
        {
            return Distance.getDistance(distanceType, dataMatrix[indexOfObject], clusterCenter);
        }
        
        // produce new indicesOfClusters[];
        private int[] assignObjectsToNearestClusters(Map<Integer, double[]> clusterCenters) throws Exception
        {
            int n = dataMatrix.length;
            int[] indicesOfClusters = new int[n];
            for( int i = 0; i < n; i++ )
            {
                double minimalDistance = Double.MAX_VALUE;
                for( Entry<Integer, double[]> entry : clusterCenters.entrySet() )
                {
                    double distance = getDistanceBetweenObjectAndClusterCenter(i, entry.getValue());
                    if( distance <= minimalDistance )
                    {
                        minimalDistance = distance;
                        indicesOfClusters[i] = entry.getKey();
                    }
                }
            }
            return indicesOfClusters;
        }

        public int[] getIndicesOfClusters()
        {
            return indicesOfClusters;
        }

        public void implementKmeansAlgorithm() throws Exception
        {
            boolean isChanged = true;
            while( isChanged )
            {
                Map<Integer, double[]> clusterCenters = getClusterCenters();
                int[] newIndicesOfClusters = assignObjectsToNearestClusters(clusterCenters);
                isChanged = false;
                for( int i = 0; i < indicesOfClusters.length; i++ )
                    if( indicesOfClusters[i] != newIndicesOfClusters[i] )
                    {
                        isChanged = true;
                        indicesOfClusters = newIndicesOfClusters;
                        this.clusterIndexAndDataMatrix = createClusterIndexAndDataMatrix(dataMatrix, indicesOfClusters);
                        break;
                    }
            }
        }
    }
    /************************* KMeansAlgorithm : finish *************************/

    /************************* FunnyAlgorithm : start *************************/
    public static class FunnyAlgorithm
    {
        private final double[][] dataMatrix; // dim = n x m; n objects and m variables
        private double[][] membershipProbabilities; // dim = n x k, where k is number of clusters
        
        public FunnyAlgorithm(double[][] dataMatrix, int numberOfClusters, double[][] initialMembershipProbabilities)
        {
            this.dataMatrix = dataMatrix;
            this.membershipProbabilities = initialMembershipProbabilities != null ? initialMembershipProbabilities : initializeInitialMembershipProbabilitiesRandomly(numberOfClusters, dataMatrix.length);
        }
        
        public double[][] getMembershipProbabilities()
        {
            return membershipProbabilities;
        }
        
        public void implementFunnyAlgorithmUsingR(String scriptForFunnyAlgorithm, int numberOfClusters, String distanceType, Logger log, AnalysisJobControl jobControl, int from, int to) throws Exception
        {
            String distanceMeasure = null;
            switch( distanceType )
            {
                default                         : log.info("R-implementation of FUNNY does not support this distance type '" + distanceType + "'. Therefore '" + Distance.EUCLIDEAN + "' will be used for analysis");
                case Distance.EUCLIDEAN         : distanceMeasure = "euclidean"; break;
                case Distance.EUCLIDEAN_SQUARED : distanceMeasure = "SqEuclidean"; break;
                case Distance.MANHATTAN         : distanceMeasure = "manhattan"; break;
            }
            String[] inputObjectsNames = new String[]{"dataMatrix", "numberOfClusters", "initialMembershipProbabilities", "distanceMeasure"};
            Object[] inputObjects = new Object[]{dataMatrix, numberOfClusters, membershipProbabilities, distanceMeasure};
            String[] outputObjectsNames = new String[]{"membershipProbabilities"};
            this.membershipProbabilities = (double[][])Rutils.executeRscript(scriptForFunnyAlgorithm, inputObjectsNames, inputObjects, outputObjectsNames, null, null, null, null, log, jobControl, from, to)[0];
        }
    }
    /************************* FunnyAlgorithm : finish *************************/
    
    /************************* MclustAlgorithm : start *************************/
    public static class MclustAlgorithm
    {
        public static double[][] implementMclustAlgorithmUsingR(String scriptForMclustAlgorithm, int numberOfClusters, double[][] dataMatrix, Logger log, AnalysisJobControl jobControl, int from, int to) throws Exception
        {
            return (double[][])Rutils.executeRscript(scriptForMclustAlgorithm, new String[]{"dataMatrix", "numberOfClusters"}, new Object[]{dataMatrix, numberOfClusters}, new String[]{"membershipProbabilities"}, null, null, null, null, log, jobControl, from, to)[0];
        }
    }
    /************************* MclustAlgorithm : finish *************************/

    /************************* IndicesOfClusteringQuality : start *************************/
    public static class IndicesOfClusteringQuality
    {
        private static final String BALL_HALL = "Ball-Hall index";
        private static final String BANFIELD_RAFTERY = "Banfield-Raftery index";
        private static final String C_INDEX = "C index";
        private static final String CALINSKI_HARABASZ = "Calinski-Harabasz index";
        private static final String DAVIES_BOULDIN = "Davies-Bouldin index";
        private static final String T_W_RATIO = "|T|:|W| index";
        private static final String DUNN = "Dunn's index";
        private static final String GAMMA = "Gamma index";
        private static final String GAMMA_PLUS = "Gamma+ index";
        private static final String GDI11 = "Generalized Dunn's index(1,1)";
        private static final String GDI12 = "Generalized Dunn's index(1,2)";
        private static final String GDI13 = "Generalized Dunn's index(1,3)";
        private static final String GDI21 = "Generalized Dunn's index(2,1)";
        private static final String GDI22 = "Generalized Dunn's index(2,2)";
        private static final String GDI23 = "Generalized Dunn's index(2,3)";
        private static final String GDI31 = "Generalized Dunn's index(3,1)";
        private static final String GDI32 = "Generalized Dunn's index(3,2)";
        private static final String GDI33 = "Generalized Dunn's index(3,3)";
        private static final String GDI41 = "Generalized Dunn's index(4,1)";
        private static final String GDI42 = "Generalized Dunn's index(4,2)";
        private static final String GDI43 = "Generalized Dunn's index(4,3)";
        private static final String GDI51 = "Generalized Dunn's index(5,1)";
        private static final String GDI52 = "Generalized Dunn's index(5,2)";
        private static final String GDI53 = "Generalized Dunn's index(5,3)";
        private static final String K2_W = "k-squared |W| index";
        private static final String LOG_RATIO_T_W = "log(|T|:|W|) index";
        private static final String LOG_RATIO_BGSS_WGSS = "log(BGSS:WGSS) index";
        private static final String MCCLAIN_RAO = "McClain-Rao index";
        private static final String PBM = "Pakhira-Bandyopadhyay-Maulik index";
        private static final String POINT_BISERIAL = "Point-biserial index";
        private static final String RATKOWSKY_LANCE = "Ratkowsky-Lance index";
        private static final String RAY_TURI = "Ray-Turi index";
        private static final String SCOTT_SYMONS = "Scott-Symons index";
        private static final String SD_SCAT = "SD-Scat validity index";
        private static final String SD_DIS = "SD-Dis validity index";
        private static final String S_DBW = "S-Dbw validity index";
        private static final String SILHOUETTE = "Silhouette's index";
        private static final String TAU = "Tau index";
        private static final String TRACE_W = "Trace(W) index";
        private static final String TRACE_W_INVERSED_B = "Trace(inversed(W)*B) index";
        private static final String WEMMERT_GANCARSKI = "Wemmert-Gancarski index";
        private static final String XIE_BENI = "Xie-Beni index";

        public static String[] getQualityIndexNamesAvailableInClusterCrit()
        {
            return new String[]{BALL_HALL, BANFIELD_RAFTERY, C_INDEX, CALINSKI_HARABASZ, DAVIES_BOULDIN, T_W_RATIO, DUNN, GAMMA, GAMMA_PLUS, GDI11, GDI12, GDI13, GDI21, GDI22, GDI23, GDI31, GDI32, GDI33, GDI41, GDI42, GDI43, GDI51, GDI52, GDI53, K2_W, LOG_RATIO_T_W, LOG_RATIO_BGSS_WGSS, MCCLAIN_RAO, PBM, POINT_BISERIAL, RATKOWSKY_LANCE, RAY_TURI, SCOTT_SYMONS, SD_SCAT, SD_DIS, S_DBW, SILHOUETTE, TAU, TRACE_W, TRACE_W_INVERSED_B, WEMMERT_GANCARSKI, XIE_BENI};
        }
        
        private static String[] getQualityIndexNamesInClusterCrit(String[] standardQualityIndexNames)
        {
            String[] result = new String[standardQualityIndexNames.length];
            for( int i = 0; i < standardQualityIndexNames.length; i++ )
                result[i] = getQualityIndexNameInClusterCrit(standardQualityIndexNames[i]);
            return result;
        }

        private static String getQualityIndexNameInClusterCrit(String standardQualityIndexName)
        {
            switch( standardQualityIndexName )
            {
                case BALL_HALL           : return "ball_hall";
                case BANFIELD_RAFTERY    : return "banfeld_raftery";
                case C_INDEX             : return "c_index";
                case CALINSKI_HARABASZ   : return "calinski_harabasz";
                case DAVIES_BOULDIN      : return "davies_bouldin";
                case T_W_RATIO           : return "det_ratio";
                case DUNN                : return "dunn";
                case GAMMA               : return "gamma";
                case GAMMA_PLUS          : return "g_plus";
                case GDI11               : return "gdi11";
                case GDI12               : return "gdi12";
                case GDI13               : return "gdi13";
                case GDI21               : return "gdi21";
                case GDI22               : return "gdi22";
                case GDI23               : return "gdi23";
                case GDI31               : return "gdi31";
                case GDI32               : return "gdi32";
                case GDI33               : return "gdi33";
                case GDI41               : return "gdi41";
                case GDI42               : return "gdi42";
                case GDI43               : return "gdi43";
                case GDI51               : return "gdi51";
                case GDI52               : return "gdi52";
                case GDI53               : return "gdi53";
                case K2_W                : return "ksq_detw";
                case LOG_RATIO_T_W       : return "Log_Det_Ratio";
                case LOG_RATIO_BGSS_WGSS : return "log_ss_ratio";
                case MCCLAIN_RAO         : return "mcclain_rao";
                case PBM                 : return "pbm";
                case POINT_BISERIAL      : return "point_biserial";
                case RATKOWSKY_LANCE     : return "ratkowsky_lance";
                case RAY_TURI            : return "ray_turi";
                case SCOTT_SYMONS        : return "scott_symons";
                case SD_SCAT             : return "sd_scat";
                case SD_DIS              : return "sd_dis";
                case S_DBW               : return "s_dbw";
                case SILHOUETTE          : return "silhouette";
                case TAU                 : return "tau";
                case TRACE_W             : return "trace_w";
                case TRACE_W_INVERSED_B  : return "trace_wib";
                case WEMMERT_GANCARSKI   : return "wemmert_gancarski";
                case XIE_BENI            : return "xie_beni";
            }
            return null;
        }
        
        public static double[] getInternalQualityIndicesUsingR(String scriptForCalculationOfQualityIndices, double[][] dataMatrix, int[] indicesOfClusters, String[] qualityIndexNames, Logger log) throws Exception
        {
            int[] indicesOfClustersShiftedForR = MatrixUtils.getSumOfVectors(indicesOfClusters, 1);
            String[] qualityIndexNamesInClusterCrit = getQualityIndexNamesInClusterCrit(qualityIndexNames);
            return (double[])Rutils.executeRscript(scriptForCalculationOfQualityIndices, new String[]{"dataMatrix", "indicesOfClusters", "qualityIndexNamesInClusterCrit"}, new Object[]{dataMatrix, indicesOfClustersShiftedForR, qualityIndexNamesInClusterCrit}, new String[]{"qualityIndices"}, null, null, null, null, log, null, 0, 0)[0];
        }
        /************************* IndicesOfClusteringQuality : finish *************************/
    }
    // there exist also class 'ClusterAnalysis' in package ru.biosoft.analysis;
}
