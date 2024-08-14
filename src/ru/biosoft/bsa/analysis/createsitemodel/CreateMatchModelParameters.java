package ru.biosoft.bsa.analysis.createsitemodel;

import ru.biosoft.access.core.DataElementPath;

public class CreateMatchModelParameters extends CreateSiteModelParameters
{
    private DataElementPath matrixPath;
    private int coreStart = 0;
    private int coreLength = 1;
    private double coreCutoff = 0.75;
    private double cutoff = 0.75;
    private boolean defaultCore = true;
    
    public boolean isDefaultCore()
    {
        return defaultCore;
    }
    
    public void setDefaultCore(boolean defaultCore)
    {
        Object oldValue = this.defaultCore;
        this.defaultCore = defaultCore;
        firePropertyChange("*", null, null);
    }
    
    public int getCoreStart()
    {
        return coreStart;
    }
    
    public void setCoreStart(int coreStart)
    {
        Object oldValue = this.coreStart;
        this.coreStart = coreStart;
        firePropertyChange("coreStart", oldValue, coreStart);
    }
    
    public int getCoreLength()
    {
        return coreLength;
    }
    
    public void setCoreLength(int coreLength)
    {
        Object oldValue = this.coreLength;
        this.coreLength = coreLength;
        firePropertyChange("coreLength", oldValue, coreLength);
    }
    
    public double getCoreCutoff()
    {
        return coreCutoff;
    }
    
    public void setCoreCutoff(double coreCutoff)
    {
        Object oldValue = this.coreCutoff;
        this.coreCutoff = coreCutoff;
        firePropertyChange("coreCutoff", oldValue, coreCutoff);
    }
    
    public double getCutoff()
    {
        return cutoff;
    }
    
    public void setCutoff(double cutoff)
    {
        Object oldValue = this.cutoff;
        this.cutoff = cutoff;
        firePropertyChange("cutoff", oldValue, cutoff);
    }
    
    public DataElementPath getMatrixPath()
    {
        return matrixPath;
    }
    
    public void setMatrixPath(DataElementPath matrixPath)
    {
        Object oldValue = this.matrixPath;
        this.matrixPath = matrixPath;
        firePropertyChange("matrixPath", oldValue, matrixPath);
    }

}
