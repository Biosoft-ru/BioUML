
package biouml.plugins.simulation;

import com.developmentontheedge.beans.annot.PropertyName;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.plugins.simulation.java.JavaSimulationEngine;

/**
 * @author axec
 *
 */
@PropertyName("Model analysis")
public class ModelAnalysisParameters extends AbstractAnalysisParameters
{
    private DataElementPath modelPath;   
    private DataElementPath reportPath;
    private SimulationEngine simulationEngine;

    public ModelAnalysisParameters()
    {
    }

    @PropertyName("Diagram path")
    public DataElementPath getModelPath()
    {
        return modelPath;
    }
    public void setModelPath(DataElementPath modelPath)
    {
        if( modelPath == null )
            return;
        DataElement de = modelPath.optDataElement();
        if( ! ( de instanceof Diagram ) || ! ( ( (Diagram)de ).getRole() instanceof EModel ) )
            return;


        SimulationEngine engine = SimulationEngineRegistry.getSimulationEngine( (Diagram)de );
        if( ! ( engine instanceof JavaSimulationEngine ) )
            return;

        engine.setDiagram( (Diagram)de );
        Object oldValue = this.modelPath;
        this.modelPath = modelPath;
        setSimulationEngine(engine);
        firePropertyChange( "modelPath", oldValue, modelPath );
    }

    public SimulationEngine getSimulationEngine()
    {
        return simulationEngine;
    }
    public void setSimulationEngine(SimulationEngine engine)
    {
        Object oldValue = this.simulationEngine;
        this.simulationEngine = engine;

        if( engine != null )
        {
            engine.setParent( this );            
            ComponentModel model = ComponentFactory.getModel( this );
            ComponentFactory.recreateChildProperties( model );
        }

        firePropertyChange( "simulationEngine", oldValue, engine );
    }

    @PropertyName("Report path")
    public DataElementPath getReportPath()
    {
        return reportPath;
    }
    public void setReportPath(DataElementPath reportPath)
    {
        Object oldValue = this.reportPath;
        this.reportPath = reportPath;
        firePropertyChange( "reportPath", oldValue, reportPath );
    }
}
