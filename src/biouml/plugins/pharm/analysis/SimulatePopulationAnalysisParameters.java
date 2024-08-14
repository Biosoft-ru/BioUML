package biouml.plugins.pharm.analysis;

import java.util.stream.Stream;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

public class SimulatePopulationAnalysisParameters extends AbstractAnalysisParameters
{
    private DataElementPath inputDiagramPath;
    private DataElementPath inputTablePath;
    private DataElementPath outputTablePath;
    private String[] estimatedVariables;
    private String[] observedVariables;
    private Diagram diagram;
    
    @PropertyName("Input diagram")
    @PropertyDescription("Input diagram")
    public DataElementPath getInputDiagramPath()
    {
        return inputDiagramPath;
    }
    public void setInputDiagramPath(DataElementPath diagramPath)
    {
        try
        {
            diagram = diagramPath.getDataElement(Diagram.class);
        }
        catch( Exception ex )
        {
            return;
        }
        if( diagram == null )
            return;
        Object oldValue = inputDiagramPath;
        inputDiagramPath = diagramPath;
        setEstimatedVariables(new String[]{});
        setObservedVariables(new String[]{});
        firePropertyChange("inputDiagramPath", oldValue, inputDiagramPath);
        
    }
    
    @PropertyName("Input table")
    @PropertyDescription("Input table")
    public DataElementPath getInputTablePath()
    {
        return inputTablePath;
    }
    public void setInputTablePath(DataElementPath tablePath)
    {
        Object oldValue = inputTablePath;
        this.inputTablePath = tablePath;
        firePropertyChange("inputTablePath", oldValue, inputTablePath);
    }
    
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

    @PropertyName("Observed variables")
    public String[] getObservedVariables()
    {
        return observedVariables;
    }
    public void setObservedVariables(String[] observedVariables)
    {
        Object oldValue = observedVariables;
        this.observedVariables = observedVariables;
        firePropertyChange("observedVariables", oldValue, observedVariables);
    }

    @PropertyName("Variables to calculate")
    public String[] getEstimatedVariables()
    {
        return estimatedVariables;
    }
    public void setEstimatedVariables(String[] estimatedVariables)
    {
        Object oldValue = estimatedVariables;
        this.estimatedVariables = estimatedVariables;
        firePropertyChange("estimatedVariables", oldValue, estimatedVariables);
    }
    
    public Stream<String> getAvailableVariables()
    {
        return diagram == null ? StreamEx.of( new String[0] )
                : diagram.getRole( EModel.class ).getVariables().stream().map( v -> v.getName() ).filter( n -> ! ( n.equals( "time" ) ) );
    }
    
    public boolean isDiagramNotSet()
    {
        return diagram == null;
    }
}
