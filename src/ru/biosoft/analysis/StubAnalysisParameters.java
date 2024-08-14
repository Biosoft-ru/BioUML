package ru.biosoft.analysis;

import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

public class StubAnalysisParameters extends AbstractAnalysisParameters
{
    public static final String LAUNCH_SLURM ="Slurm";
    public static final String LAUNCH_LOCAL ="Local";
    
    private DataElementPath input;
    private String launchType = LAUNCH_LOCAL;
    private DataElementPath outputFolder;
    
    @PropertyName("Input folder")
    public void setInput(DataElementPath input)
    {
        Object oldValue = input;
        this.input = input;
        firePropertyChange("input", oldValue, input);
    }
    public DataElementPath getInput()
    {
        return input;
    }

    @PropertyName("Launch type")
    public String getLaunchType()
    {
        return launchType;
    }
    public void setLaunchType(String launchType)
    {
        this.launchType = launchType;
    }
    
    @PropertyName("Result Folder")
    public DataElementPath getOutputFolder()
    {
        return outputFolder;
    }
    public void setOutputFolder(DataElementPath outputFolder)
    {
        this.outputFolder = outputFolder;
    }
}