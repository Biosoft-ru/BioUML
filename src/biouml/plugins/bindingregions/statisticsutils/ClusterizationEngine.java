
package biouml.plugins.bindingregions.statisticsutils;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisJobControl;
import biouml.plugins.bindingregions.rscript.RHelper;
import biouml.plugins.bindingregions.utils.Classification;
import biouml.plugins.bindingregions.utils.Clusterization;
import biouml.plugins.bindingregions.utils.Clusterization.FunnyAlgorithm;
import biouml.plugins.bindingregions.utils.Clusterization.IndicesOfClusteringQuality;
import biouml.plugins.bindingregions.utils.Clusterization.KMeansAlgorithm;
import biouml.plugins.bindingregions.utils.Clusterization.MclustAlgorithm;
import biouml.plugins.bindingregions.utils.MultivariateSample.Transformation;
import biouml.plugins.bindingregions.utils.StatUtil.DensityEstimation;
import biouml.plugins.bindingregions.utils.TableUtils;

/**
 * @author yura
 * Important note : it is assumed that the input data matrix is located in table (TableDataCollection) !!!
 *
 */
public class ClusterizationEngine
{
    public static final String CLUSTERIZATION_1_K_MEANS = "k-means";
    public static final String CLUSTERIZATION_2_FUNNY = "FUNNY : k-means fuzzy clustering";
    public static final String CLUSTERIZATION_3_MCLUST = "Mclust : normal mixture model-based clustering";

    public static final String OUTPUT_1_WRITE_INDICES_OF_CLUSTERS_AND_DATA_MATRIX = "Write data matrix and indices of clusters into table";
    public static final String OUTPUT_2_WRITE_MEMBERSHIP_PROBABILITIES = "Write membership probabilities into table";
    public static final String OUTPUT_3_WRITE_MEANS_AND_SIGMAS = "Write means and sigmas for clusters into table";
    public static final String OUTPUT_4_WRITE_MEANS_AND_SIGMAS_TRANSFORMED = "Write transformed means and sigmas for clusters into table";
    public static final String OUTPUT_5_WRITE_QUALITY_INDICES = "Write indices of quality of clustering into table";
    public static final String OUTPUT_6_WRITE_QUALITY_INDICES_TRANSFORMED = "Write indices of quality of clustering for transformed data into table";
    public static final String OUTPUT_7_WRITE_DISTANCES_BETWEEN_CENTERS = "Write distances between cluster centers into table";
    public static final String OUTPUT_8_WRITE_DISTANCES_BETWEEN_CENTERS_TRANSFORMED = "Write distances between cluster centers for transformed data into table";
    public static final String OUTPUT_9_WRITE_DENSITIES = "Write charts with variable densities";
    public static final String OUTPUT_10_WRITE_DENSITIES_TRANSFORMED = "Write charts with transformed variable densities";
    
    private final String clusterizationType;
    private final String dataTransformationType;
    private final String distanceType;
    private final String[] objectNames;   // dim(objectNames) = n;
    private final String[] variableNames; // dim(variableNames) = m;
    private final double[][] dataMatrix;  // dim(dataMatrix) = n x m; n objects and m variables
    private final int numberOfClusters;
    private final String[] outputOptions;
    private final String[] clusteringQualityIndexNames;
    
    public ClusterizationEngine(String clusterizationType, String dataTransformationType, String distanceType, String[] objectNames, String[] variableNames, double[][] dataMatrix, int numberOfClusters, String[] outputOptions, String[] clusteringQualityIndexNames)
    {
        this.clusterizationType = clusterizationType;
        this.dataTransformationType = dataTransformationType;
        this.distanceType = distanceType;
        this.objectNames = objectNames;
        this.variableNames = variableNames;
        this.dataMatrix = dataMatrix;
        this.numberOfClusters = numberOfClusters;
        this.outputOptions = outputOptions;
        this.clusteringQualityIndexNames = clusteringQualityIndexNames;
    }
    
    public void implementClusterization(DataElementPath pathToOutputs, Logger log, AnalysisJobControl jobControl, int from, int to) throws Exception
    {
        int[] indicesOfClusters = null;
        double[][] membershipProbabilities = null;
        double[][] dataMatrixTransformed = Transformation.transformData(dataMatrix, dataTransformationType);
        jobControl.setPreparedness(from + 5 * (to - from) / 100);
        switch( clusterizationType )
        {
            case CLUSTERIZATION_1_K_MEANS : KMeansAlgorithm kmAlgorithm = new KMeansAlgorithm(dataMatrixTransformed, null, numberOfClusters, distanceType);
                                            kmAlgorithm.implementKmeansAlgorithm();
                                            indicesOfClusters = kmAlgorithm.getIndicesOfClusters();
                                            indicesOfClusters = Classification.removeEmptyClasses(indicesOfClusters);
                                            jobControl.setPreparedness(from + 7 * (to - from) / 10);
                                            break;
            case CLUSTERIZATION_2_FUNNY   : FunnyAlgorithm funny = new FunnyAlgorithm(dataMatrixTransformed, numberOfClusters, null);
                                            String scriptForFunnyAlgorithm = RHelper.getScript("ClusterAnalysis", "FunnyAlgorithm");
                                            funny.implementFunnyAlgorithmUsingR(scriptForFunnyAlgorithm, numberOfClusters, distanceType, log, jobControl, from, from + 7 * (to - from) / 10);
                                            membershipProbabilities = funny.getMembershipProbabilities();
                                            indicesOfClusters = Clusterization.transformMembershipProbabilitiesToIndicesOfCusters(membershipProbabilities);
                                            // TODO: create method FunnyAlgorithm.removeEmptyClasses(indicesOfClusters, membershipProbabilities);
                                            break;
            case CLUSTERIZATION_3_MCLUST  : String scriptForMclustAlgorithm = RHelper.getScript("ClusterAnalysis", "MclustAlgorithm");;
                                            membershipProbabilities = MclustAlgorithm.implementMclustAlgorithmUsingR(scriptForMclustAlgorithm, numberOfClusters, dataMatrix, log, jobControl, from, from + 7 * (to - from) / 10);
                                            indicesOfClusters = Clusterization.transformMembershipProbabilitiesToIndicesOfCusters(membershipProbabilities);
                                            // TODO: create method MclustAlgorithm.removeEmptyClasses(indicesOfClusters, membershipProbabilities);
                                            break;
            default                       : throw new Exception("This type of clusterization (namely, '" + clusterizationType + "') is not supported in our cluster analysis currently");
        }
        outputResults(indicesOfClusters, membershipProbabilities, dataMatrixTransformed, pathToOutputs, log, jobControl, from + 7 * (to - from) / 10, to);
    }
    
    private void outputResults(int[] indicesOfClusters, double[][] membershipProbabilities, double[][] dataMatrixTransformed, DataElementPath pathToOutputs,  Logger log, AnalysisJobControl jobControl, int from, int to) throws Exception
    {
        String scriptForCalculationOfQualityIndices;
        int difference = to - from;
        for( int i = 0; i < outputOptions.length; i++ )
        {
            switch( outputOptions[i] )
            {
                case OUTPUT_1_WRITE_INDICES_OF_CLUSTERS_AND_DATA_MATRIX   : Clusterization.writeDoubleDataMatrixAndClusterNamesIntoTable(dataMatrix, indicesOfClusters, objectNames, variableNames, "Cluster", pathToOutputs, "indicesOfClustersAndDataMatrix"); break;
                case OUTPUT_2_WRITE_MEMBERSHIP_PROBABILITIES              : if( membershipProbabilities != null )
                                                                                Clusterization.writeMembershipProbabilitiesIntoTable(membershipProbabilities, objectNames, pathToOutputs, "membershipProbabilities");
                                                                            break;
                case OUTPUT_3_WRITE_MEANS_AND_SIGMAS                      : Clusterization.writeTableWithMeansAndSigmas(dataMatrix, variableNames, indicesOfClusters, pathToOutputs, "meansAndSigmas"); break;
                case OUTPUT_4_WRITE_MEANS_AND_SIGMAS_TRANSFORMED          : Clusterization.writeTableWithMeansAndSigmas(dataMatrixTransformed, variableNames, indicesOfClusters, pathToOutputs, "meansAndSigmasTransformed"); break;
                case OUTPUT_5_WRITE_QUALITY_INDICES                       : scriptForCalculationOfQualityIndices = RHelper.getScript("ClusterAnalysis", "getInternalQualityIndex");
                                                                            double[] qualityIndices = IndicesOfClusteringQuality.getInternalQualityIndicesUsingR(scriptForCalculationOfQualityIndices, dataMatrix, indicesOfClusters, clusteringQualityIndexNames, log);
                                                                            for( int j = 0; j < clusteringQualityIndexNames.length; j++ )
                                                                                log.info("quality index name = " + clusteringQualityIndexNames[j] + " quality index value = " + qualityIndices[j]);
                                                                            TableUtils.writeDoubleTable(qualityIndices, clusteringQualityIndexNames, "Value of index", pathToOutputs, "clusteringQualityIndices"); break;
                case OUTPUT_6_WRITE_QUALITY_INDICES_TRANSFORMED           : scriptForCalculationOfQualityIndices = RHelper.getScript("ClusterAnalysis", "getInternalQualityIndex");
                                                                            double[] qualityIndicesOnTransformedData = IndicesOfClusteringQuality.getInternalQualityIndicesUsingR(scriptForCalculationOfQualityIndices, dataMatrixTransformed, indicesOfClusters, clusteringQualityIndexNames, log);
                                                                            TableUtils.writeDoubleTable(qualityIndicesOnTransformedData, clusteringQualityIndexNames, "Value of index", pathToOutputs, "clusteringQualityIndicesTransformed"); break;
                case OUTPUT_7_WRITE_DISTANCES_BETWEEN_CENTERS             : Clusterization.writeTableWithDistancesBetweenClusterCenters(distanceType, dataMatrix, indicesOfClusters, pathToOutputs, "distancesBetweenClusterCenters"); break;
                case OUTPUT_8_WRITE_DISTANCES_BETWEEN_CENTERS_TRANSFORMED : Clusterization.writeTableWithDistancesBetweenClusterCenters(distanceType, dataMatrixTransformed, indicesOfClusters, pathToOutputs, "distancesBetweenClusterCentersTransformed"); break;
                case OUTPUT_9_WRITE_DENSITIES                             : Clusterization.writeChartsWithVariableDensities(dataMatrix, variableNames, indicesOfClusters, true, DensityEstimation.WINDOW_WIDTH_01, null, pathToOutputs.getChildPath("chart_variableDensities")); break;
                case OUTPUT_10_WRITE_DENSITIES_TRANSFORMED                : Clusterization.writeChartsWithVariableDensities(dataMatrixTransformed, variableNames, indicesOfClusters, true, DensityEstimation.WINDOW_WIDTH_01, null, pathToOutputs.getChildPath("chart_variableDensitiesTransformed")); break;
            }
            if( jobControl != null )
                jobControl.setPreparedness(from + (i + 1) * difference / outputOptions.length);
        }
    }
    
    public static String[] getAvailableClusterizationTypes()
    {
        return new String[]{CLUSTERIZATION_1_K_MEANS, CLUSTERIZATION_2_FUNNY, CLUSTERIZATION_3_MCLUST};
    }
}
