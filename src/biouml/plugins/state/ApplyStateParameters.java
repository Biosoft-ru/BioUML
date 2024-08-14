
package biouml.plugins.state;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

import com.developmentontheedge.beans.annot.PropertyName;

/**
 * @author anna
 *
 */
public class ApplyStateParameters extends AbstractAnalysisParameters
{
    private DataElementPath inputDiagramPath;
    private DataElementPath outputDiagramPath;
    private DataElementPath statePath;
    private String stateName;
    private boolean newDiagram;
    private boolean writeStateToDiagram;
    
    public DataElementPath getInputDiagramPath()
    {
        return inputDiagramPath;
    }
    public void setInputDiagramPath(DataElementPath inputDiagramPath)
    {
        Object oldValue = this.inputDiagramPath;
        this.inputDiagramPath = inputDiagramPath;
        firePropertyChange("inputDiagramPath", oldValue, inputDiagramPath);
    }

    public DataElementPath getOutputDiagramPath()
    {
        return outputDiagramPath;
    }
    public void setOutputDiagramPath(DataElementPath outputDiagramPath)
    {
        Object oldValue = this.outputDiagramPath;
        this.outputDiagramPath = outputDiagramPath;
        firePropertyChange("outputDiagramPath", oldValue, outputDiagramPath);
    }

    public DataElementPath getStatePath()
    {
        return statePath;
    }
    public void setStatePath(DataElementPath statePath)
    {
        Object oldValue = this.statePath;
        this.statePath = statePath;
        setStateName(statePath.getName());
        firePropertyChange("statePath", oldValue, statePath);
    }

    @PropertyName ( "State name" )   
    public String getStateName()
    {
        return stateName;
    }
    public void setStateName(String stateName)
    {
        this.stateName = stateName;
    }
    
    
    @PropertyName ("Save to new diagram")
    public boolean isNewDiagram()
    {
        return newDiagram;
    }
    public void setNewDiagram(boolean newDiagram)
    {
        this.newDiagram = newDiagram;
        firePropertyChange("*", null, null);
    }

    public boolean applyToSameDiagram()
    {
        return !isNewDiagram();
    }
    
    @PropertyName("Save as diagram without state")
    public boolean isWriteStateToDiagram()
    {
        return writeStateToDiagram;
    }
    public void setWriteStateToDiagram(boolean writeStateToDiagram)
    {
        this.writeStateToDiagram = writeStateToDiagram;
    }
}
