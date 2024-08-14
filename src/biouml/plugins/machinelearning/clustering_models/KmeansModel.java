package biouml.plugins.machinelearning.clustering_models;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import ru.biosoft.access.core.DataElementPath;
import biouml.plugins.machinelearning.utils.DataMatrix;
import biouml.plugins.machinelearning.utils.PrimitiveOperations;
import biouml.plugins.machinelearning.utils.StatUtils.MultivariateSamples;
import biouml.plugins.machinelearning.utils.StatUtils.RandomUtils;
import biouml.plugins.machinelearning.utils.UtilsGeneral.UtilsForArray;
import biouml.plugins.machinelearning.utils.VectorUtils.Distance;

/* Object[] additionalInputParameters :
*           additionalInputParameters[0] = String distanceType
*/

public class KmeansModel extends ClusteringModel
{
	private String distanceType; /*** type of distance between object and cluster center; for example String s = Distance.DISTANSCE_1_MANHATTAN; ***/
	private DataMatrix dataMatrixWithClusterCenters;
	
	public KmeansModel(int numberOfClusters, DataMatrix dataMatrix, Object[] additionalInputParameters, boolean doCalculateAccompaniedInformation)
	{
		super(ClusteringModel.CLUSTERIZATION_1_K_MEANS, numberOfClusters, dataMatrix, additionalInputParameters, doCalculateAccompaniedInformation);
	}
	
	@Override
	protected void calculateAccompaniedInformation(double[][] matrix)
	{
//		double[][][] maticesForClusters = createMatricesForClusters(matrix);
//        double[][] clusterCenters = MultivariateSamples.getMeanVectorsInSamples(maticesForClusters);
//        String[] rowNames = new String[clusterCenters.length];
//        for( int i = 0; i < clusterCenters.length; i++ )
//        	rowNames[i] = CLUSTER + "_" + Integer.toString(i);
//        dataMatrixWithClusterCenters = new DataMatrix(rowNames, variableNames, clusterCenters);
		dataMatrixWithClusterCenters = getDataMatrixWithClusterCenters(matrix);
	}
	
	// TODO: to move getDataMatrixWithClusterCenters() to appropriate Class, for example, to class MultivariateSamples.
	private DataMatrix getDataMatrixWithClusterCenters(double[][] matrix)
	{
		double[][][] maticesForClusters = createMatricesForClusters(matrix);
        double[][] clusterCenters = MultivariateSamples.getMeanVectorsInSamples(maticesForClusters);
        String[] rowNames = new String[clusterCenters.length];
        for( int i = 0; i < clusterCenters.length; i++ )
        {
        	rowNames[i] = CLUSTER + Integer.toString(i);
        	clusterCenters[i] = ArrayUtils.add(clusterCenters[i], (double)maticesForClusters[i].length);
        }
        return new DataMatrix(rowNames, (String[])ArrayUtils.add(variableNames, "size"), clusterCenters);
	}
	
	@Override
	protected void saveAccompaniedResults(DataElementPath pathToOutputFolder)
    {
		dataMatrixWithClusterCenters.writeDataMatrix(false, pathToOutputFolder, "Cluster_centers", log);
    }
	
	@Override
    protected void implementClusterization(double[][] matrix, Object[] additionalInputParameters)
    {
		distanceType = (String)additionalInputParameters[0];
    	implementClusterization(matrix);
        if( doCalculateAccompaniedInformation )
            calculateAccompaniedInformation(matrix);
    }

	@Override
	protected void implementClusterization(double[][] matrix)
    {
		int maxIterationsNumber = 1000; ///*** TODO: to add maxIterationsNumber to abstract class ClusteringModel ??? 
		determineInitialClusterIndices(); 
        for( int actualNumberOfIterations = 1; actualNumberOfIterations <= maxIterationsNumber; actualNumberOfIterations++ )
        {
            log.info("actualNumberOfIterations = " + actualNumberOfIterations + " maxIterationsNumber = " + maxIterationsNumber);
        	double[][][] maticesForClusters = createMatricesForClusters(matrix);
        	maticesForClusters = removeMatricesForEmptyClusters(maticesForClusters);
            if( maticesForClusters.length != numberOfClusters )
            {
            	numberOfClusters = maticesForClusters.length;
                if( numberOfClusters == 1 ) break;
            }
            double[][] clusterCenters = MultivariateSamples.getMeanVectorsInSamples(maticesForClusters);
            int[] clusterIndicesNew = assignObjectsToNearestClusters(clusterCenters, matrix);
            
            /********************************************/
//            log.info("clusterCenters = ");
//            for( int i = 0; i < clusterCenters[0].length; i++ )
//                log.info("i = " + i + " clusterCenters[0][i] = " + clusterCenters[0][i]);
            /*******************************************/
            
            if( UtilsForArray.equal(clusterIndices, clusterIndicesNew) ) break;
            clusterIndices = clusterIndicesNew;
        }
        log.info("maxIterationsNumber = " + maxIterationsNumber);
    }
	
    private int[] assignObjectsToNearestClusters(double[][] clusterCenters, double[][] matrix)
    {
        int[] clusterIndices = new int[matrix.length];
        for( int i = 0; i < matrix.length; i++ )
        {
        	double[] distances = new double[clusterCenters.length];
            for( int j = 0; j < clusterCenters.length; j++ )
            	distances[j] = getDistanceBetweenObjectAndClusterCenter(matrix[i], clusterCenters[j]);
            clusterIndices[i] = (int) PrimitiveOperations.getMin(distances)[0];
        }
        return clusterIndices;
    }

    private double getDistanceBetweenObjectAndClusterCenter(double[] dataForObject, double[] clusterCenter)
    {
        return Distance.getDistance(distanceType, dataForObject, clusterCenter);
    }
	
	private void determineInitialClusterIndices()
	{
		int index = 0;
		clusterIndices = new int[objectNames.length];
        for( int i = 0; i < objectNames.length; i++ )
        {
        	clusterIndices[i] = index;
        	if( ++index >= numberOfClusters )
        		index = 0;
        }
        RandomUtils.permuteVector(clusterIndices, 0);
	}
	
	// matrices[i][][] = submatrix for cluster i.
	private double[][][] createMatricesForClusters(double[][] matrix)
	{
		double[][][] matrices = new double[numberOfClusters][][];
        for( int i = 0; i < numberOfClusters; i++ )
        {
        	List<Integer> listOfIndices = new ArrayList<>();
            for( int j = 0; j < clusterIndices.length; j++ )
            	if( i == clusterIndices[j] )
            		listOfIndices.add(j);
            double[][] subMatrix = null;
            if( ! listOfIndices.isEmpty() )
            {
            	subMatrix = new double[listOfIndices.size()][];
                for( int j = 0; j < listOfIndices.size(); j++ )
                	subMatrix[j] = matrix[listOfIndices.get(j)];
            }
            matrices[i] = subMatrix;
        }
        return matrices;
	}
	
	private double[][][] removeMatricesForEmptyClusters(double[][][] matrices)
	{
		// 1. Check for non-empty clusters.
		int count = 0;
        for( int i = 0; i < matrices.length; i++ )
        	if( matrices[i] != null )
        		count++;
        if( count == numberOfClusters ) return matrices;
        
        // 2. Remove matrices for empty clusters.
		double[][][] result = new double[count][][];
		int index = 0;
        for( int i = 0; i < matrices.length; i++ )
        	if( matrices[i] != null )
        		result[index++] = matrices[i];
		return result;
	}
}