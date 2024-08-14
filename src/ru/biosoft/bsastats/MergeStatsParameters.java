package ru.biosoft.bsastats;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * @author lan
 *
 */
public class MergeStatsParameters extends AbstractAnalysisParameters
{
    DataElementPathSet inputStatistics;
    DataElementPath output;
    
    @PropertyName("Statistics results")
    @PropertyDescription("Folders containing the results of 'Sequence statistics' analysis")
    public DataElementPathSet getInputStatistics()
    {
        return inputStatistics;
    }
    
    public void setInputStatistics(DataElementPathSet inputStatistics)
    {
        Object oldValue = this.inputStatistics;
        this.inputStatistics = inputStatistics;
        firePropertyChange("inputStatistics", oldValue, inputStatistics);
    }
    
    @PropertyName("Output path")
    @PropertyDescription("Path to output report")
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
}
