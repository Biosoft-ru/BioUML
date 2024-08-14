package biouml.plugins.machinelearning.clustering_models;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataElementPath;
import biouml.plugins.machinelearning.utils.DataMatrix;
import biouml.plugins.machinelearning.utils.DataMatrixInteger;

public abstract class ClusteringModel
{
    public static final String CLUSTERIZATION_1_K_MEANS = "K-means";
    public static final String CLUSTERIZATION_2 = "Other methods : under construction";
    public static final String CLUSTER = "Cluster_";
    
    protected boolean doCalculateAccompaniedInformation;
    //protected boolean isModelFitted = true;		// ???
    protected String clusteringType;
    protected String[] variableNames, objectNames;
    //public String[] clusterNames;
    protected int numberOfClusters;
    protected int[] clusterIndices; // dim(clusterIndices) = dim(objectNames); clusterIndices[i] = number of cluster to which object[i] belongs to;
    //protected double[] probabilities;
    
    public ClusteringModel(String clusteringType, int numberOfClusters, DataMatrix dataMatrix, Object[] additionalInputParameters, boolean doCalculateAccompaniedInformation)
    {
        this.doCalculateAccompaniedInformation = doCalculateAccompaniedInformation;
        this.clusteringType = clusteringType;
        this.numberOfClusters = numberOfClusters;
        this.variableNames = dataMatrix.getColumnNames();
        this.objectNames = dataMatrix.getRowNames();
        
        //TODO
        // Take additional parameters from additionalInputParameters. 
        implementClusterization(dataMatrix, additionalInputParameters);
    }
    
    private void implementClusterization(DataMatrix dataMatrix, Object[] additionalInputParameters)
    {
    	implementClusterization(dataMatrix.getMatrix(), additionalInputParameters);
    }
    
    protected void implementClusterization(double[][] matrix, Object[] additionalInputParameters)
    {
    	implementClusterization(matrix);
        if( doCalculateAccompaniedInformation )
            calculateAccompaniedInformation(matrix);
    }

    protected void implementClusterization(double[][] matrix)
    {}
    
    protected void calculateAccompaniedInformation(double[][] matrix)
    {}
    
    public void saveResults(DataElementPath pathToOutputFolder)
    {
    	saveClusterIndices(pathToOutputFolder);
    	if( doCalculateAccompaniedInformation )
            saveAccompaniedResults(pathToOutputFolder);
    }
    
    protected void saveAccompaniedResults(DataElementPath pathToOutputFolder)
    {}
    
    private void saveClusterIndices(DataElementPath pathToOutputFolder)
    {
    	DataMatrixInteger dmi = new DataMatrixInteger(objectNames, "Cluster_indices", clusterIndices);
    	dmi.writeDataMatrix(pathToOutputFolder, "Cluster_indices");
    }
    
    protected static Logger log = Logger.getLogger(ClusteringModel.class.getName());
}
