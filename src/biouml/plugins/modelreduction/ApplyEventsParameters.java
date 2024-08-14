package biouml.plugins.modelreduction;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.plugins.simulation.SimulationEngineWrapper;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

@SuppressWarnings ( "serial" )
@PropertyName ( "Parameters" )
@PropertyDescription ( "Analysis parameters." )
public class ApplyEventsParameters extends AbstractAnalysisParameters
{
    private DataElementPath diagramPath;
    private DataElementPath tablePath;
    private DataElementPath resultPath;
    private SimulationEngineWrapper engineWrapper;

    public ApplyEventsParameters()
    {
        setEngineWrapper(new SimulationEngineWrapper());
    }

    
    @PropertyName ( "Diagram path" )
    @PropertyDescription ( "Path to the diagram for analysis." )
    public DataElementPath getDiagramPath()
    {
        return diagramPath;
    }
    public void setDiagramPath(DataElementPath diagramPath)
    {
        try
        {
            setDiagram(diagramPath.getDataElement(Diagram.class));
        }
        catch( Exception ex )
        {
            return;
        }

        Object oldValue = this.diagramPath;
        this.diagramPath = diagramPath;
        firePropertyChange("diagramPath", oldValue, diagramPath);
    }

    protected void setDiagram(Diagram diagram)
    {
        engineWrapper.setDiagram(diagram);
    }

    @PropertyName ( "Table path" )
    @PropertyDescription ( "Path to the table for analysis." )
    public DataElementPath getTablePath()
    {
        return tablePath;
    }
    public void setTablePath(DataElementPath tablePath)
    {
        this.tablePath = tablePath;
    }

    @PropertyName ( "Result path" )
    @PropertyDescription ( "Path to the diagram for analysis." )
    public DataElementPath getResultPath()
    {
        return resultPath;
    }
    public void setResultPath(DataElementPath diagramPath)
    {
        this.resultPath = diagramPath;
    }

    @PropertyName ( "Simulation parameters" )
    @PropertyDescription ( "Simulation parameters." )
    public SimulationEngineWrapper getEngineWrapper()
    {
        return engineWrapper;
    }
    public void setEngineWrapper(SimulationEngineWrapper engineWrapper)
    {
        Object oldValue = this.engineWrapper;
        this.engineWrapper = engineWrapper;
        this.engineWrapper.setParent(this, "engineWrapper");
        firePropertyChange("engineWrapper", oldValue, this.engineWrapper);
    }
}