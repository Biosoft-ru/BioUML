package biouml.plugins.sedml.analyses;

import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * @author axec
 *
 */
public class GenerateReportParameters extends AbstractAnalysisParameters
{
    private DataElementPathSet simulationResultPath;
    
    @PropertyName("Simulation result")
    @PropertyDescription("Simulation result.")
    public DataElementPathSet getSimulationResultPath()
    {
        return simulationResultPath;
    }
    public void setSimulationResultPath(DataElementPathSet simulationResult)
    {
        if( simulationResult == null )
            return;
        Object oldValue = this.simulationResultPath;
        this.simulationResultPath = simulationResult;
        firePropertyChange("simulationResultPath", oldValue, simulationResult);
    }
}
