package ru.biosoft.analysis;

import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

public class GenerateTableFromJSONParameters extends AbstractAnalysisParameters
{    
    private DataElementPath input;
    private DataElementPath output;
     
    @PropertyName("Output table")
    public DataElementPath getOutput()
    {
        return output;
    }
    public void setOutput(DataElementPath output)
    {
        this.output = output;
    }
    
    @PropertyName("Input file")
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
}