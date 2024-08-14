package ru.biosoft.bsa.analysis;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

@SuppressWarnings ( "serial" )
public class IPSMotifDiscoveryAnalysisParameters extends AbstractAnalysisParameters
{
    /** Set of paths to initial matrices */
    private DataElementPathSet initialMatrices;
    
    /** Path to sequences (Track) */
    private DataElementPath sequencesPath;
    
    /** Path to DataCollection in which final FrequencyMatrices will be stored */
    private DataElementPath outputPath;
    
    private int windowSize = 100;
    private double critIPS = 5;
    private int maxIterations = 200;
    private int minClusterSize = 3;
    private boolean extendInitialMatrices;
    
    
    public DataElementPathSet getInitialMatrices()
    {
        return initialMatrices;
    }

    public void setInitialMatrices(DataElementPathSet initialMatrices)
    {
        Object oldValue = this.initialMatrices;
        this.initialMatrices = initialMatrices;
        firePropertyChange("profilePath", oldValue, initialMatrices);
    }
    
    public DataElementPath getSequencesPath()
    {
        return sequencesPath;
    }

    public void setSequencesPath(DataElementPath sequencePath)
    {
        Object oldValue = this.sequencesPath;
        this.sequencesPath = sequencePath;
        firePropertyChange("sequencesPath", oldValue, sequencePath);
    }
    
    public DataElementPath getOutputPath()
    {
        return outputPath;
    }
    
    public void setOutputPath(DataElementPath outputPath)
    {
        Object oldValue = this.outputPath;
        this.outputPath = outputPath;
        firePropertyChange("ouputPath", oldValue, outputPath);
    }
    
    public int getMaxIterations()
    {
         return maxIterations;
    }
    
    public void setMaxIterations(int maxIterations)
    {
        Object oldValue = this.maxIterations;
        this.maxIterations = maxIterations;
        firePropertyChange("maxIterations", oldValue, maxIterations);
    }
    
    public int getMinClusterSize()
    {
        return minClusterSize;
    }
    
    public void setMinClusterSize(int minClusterSize)
    {
        Object oldValue = this.minClusterSize;
        this.minClusterSize = minClusterSize;
        firePropertyChange("minClusterSize", oldValue, minClusterSize);
    }
    
    public int getWindowSize()
    {
        return windowSize;
    }

    public void setWindowSize(int windowSize)
    {
        Object oldValue = this.windowSize;
        this.windowSize = windowSize;
        firePropertyChange("windowSize", oldValue, windowSize);
    }

    public double getCritIPS()
    {
        return critIPS;
    }

    public void setCritIPS(double critIPS)
    {
        Object oldValue = this.critIPS;
        this.critIPS = critIPS;
        firePropertyChange("critIPS", oldValue, critIPS);
    }

    public boolean isExtendInitialMatrices()
    {
        return extendInitialMatrices;
    }

    public void setExtendInitialMatrices(boolean extendInitialMatrices)
    {
        Object oldValue = this.extendInitialMatrices;
        this.extendInitialMatrices = extendInitialMatrices;
        firePropertyChange("extendInitialMatrices", oldValue, extendInitialMatrices);
    }
}
