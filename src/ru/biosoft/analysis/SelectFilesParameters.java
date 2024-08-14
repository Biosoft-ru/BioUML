package ru.biosoft.analysis;

import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

public class SelectFilesParameters extends AbstractAnalysisParameters
{
    private DataElementPath inputCollection;
    private DataElementPath outputCollection;
    private String mask;
    private String workDir;
    
    @PropertyName("Input folder")
    public DataElementPath getInputCollection()
    {
        return inputCollection;
    }
    public void setInputCollection(DataElementPath inputCollection)
    {
        Object oldValue = inputCollection;
        this.inputCollection =inputCollection;
        firePropertyChange("inputCollection", oldValue, inputCollection);
    }
    
    @PropertyName("Output folder")
    public DataElementPath getOutputCollection()
    {
        return outputCollection;
    }
    public void setOutputCollection(DataElementPath outputCollection)
    {
        Object oldValue = outputCollection;
        this.outputCollection = outputCollection;
        firePropertyChange("outputCollection", oldValue, outputCollection);
    }

    @PropertyName("Mask")
    public String getMask()
    {
        return mask;
    }
    public void setMask(String mask)
    {
        Object oldValue = mask;
        this.mask = mask;
        firePropertyChange("mask", oldValue, mask);
    }
    
    @PropertyName("Work dir")
    public String getWorkDir()
    {
        return workDir;
    }
    public void setWorkDir(String workDir)
    {
        this.workDir = workDir;
    }
}
