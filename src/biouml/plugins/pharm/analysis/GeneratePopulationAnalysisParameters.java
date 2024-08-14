package biouml.plugins.pharm.analysis;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

public class GeneratePopulationAnalysisParameters extends AbstractAnalysisParameters
{
    private DataElementPath outputTablePath;
    private Distribution[] distributions; 
    
    @PropertyName("Output table")
    @PropertyDescription("Output table")
    public DataElementPath getOutputTablePath()
    {
        return outputTablePath;
    }
    public void setOutputTablePath(DataElementPath outputTablePath)
    {
        Object oldValue = outputTablePath;
        this.outputTablePath = outputTablePath;
        firePropertyChange("outputTablePath", oldValue, outputTablePath);
    } 
    
    @PropertyName("Variables")
    @PropertyDescription("Variables.")
    public Distribution[] getDistributions()
    {
        return distributions;
    }
    public void setDistributions(Distribution[] distributions)
    {
        Object oldValue = distributions;
        this.distributions = distributions;
        firePropertyChange("distributions", oldValue, distributions);
    } 
}
