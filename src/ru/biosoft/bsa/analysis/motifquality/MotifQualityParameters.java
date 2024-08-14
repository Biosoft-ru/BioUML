package ru.biosoft.bsa.analysis.motifquality;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

public class MotifQualityParameters extends AbstractAnalysisParameters
{
    private DataElementPath sequences;
    private DataElementPath siteModel;
    private DataElementPath output;
    private long seed;
    private int numberOfPoints = 11;
    private int shufflesCount = 10;
    
    public int getNumberOfPoints()
    {
        return numberOfPoints;
    }
    
    public void setNumberOfPoints(int numberOfPoints)
    {
        Object oldValue = this.numberOfPoints;
        this.numberOfPoints = numberOfPoints;
        firePropertyChange("numberOfPoints", oldValue, numberOfPoints);
    }
    
    public int getShufflesCount()
    {
        return shufflesCount;
    }
    
    public void setShufflesCount(int shufflesCount)
    {
        Object oldValue = this.shufflesCount;
        this.shufflesCount = shufflesCount;
        firePropertyChange("shufflesCount", oldValue, shufflesCount);
    }

    public DataElementPath getSequences()
    {
        return sequences;
    }
    
    public void setSequences(DataElementPath sequences)
    {
        Object oldValue = this.sequences;
        this.sequences = sequences;
        firePropertyChange("sequences", oldValue, sequences);
    }

    public DataElementPath getSiteModel()
    {
        return siteModel;
    }

    public void setSiteModel(DataElementPath siteModel)
    {
        Object oldValue = this.siteModel;
        this.siteModel = siteModel;
        firePropertyChange("siteModel", oldValue, siteModel);
    }

    public DataElementPath getOutput()
    {
        return output;
    }

    public void setOutput(DataElementPath output)
    {
        Object oldValue = this.output;
        this.output = output;
        firePropertyChange("output", oldValue, output);
    }
    
    public long getSeed()
    {
        return seed;
    }
    
    public void setSeed(long seed)
    {
        Object oldValue = this.seed;
        this.seed = seed;
        firePropertyChange("seed", oldValue, seed);
    }


}
