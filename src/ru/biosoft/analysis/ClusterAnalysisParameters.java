package ru.biosoft.analysis;

public class ClusterAnalysisParameters extends MicroarrayAnalysisParameters
{
    private Integer clusterCount = 2; //default parameter value
    private String method = CLUSTER_HARTIGAN_WONG;

    public static final String CLUSTER_HARTIGAN_WONG = "Hartigan-Wong";
    public static final String CLUSTER_LLOYD = "Lloyd";
    public static final String CLUSTER_FORGY = "Forgy";
    public static final String CLUSTER_MACQUEEN = "MacQueen";

    public ClusterAnalysisParameters()
    {
        getExperimentData().setNumerical(true);
    }

    public String getMethod()
    {
        return method;
    }
    public void setMethod(String method)
    {
        String oldValue = this.method;
        this.method = method;
        firePropertyChange("method", oldValue, method);
    }

    public Integer getClusterCount()
    {
        return clusterCount;
    }
    public void setClusterCount(Integer count)
    {
        Integer oldValue = clusterCount;
        clusterCount = count;
        firePropertyChange("clusterCount", oldValue, clusterCount);
    }
}
