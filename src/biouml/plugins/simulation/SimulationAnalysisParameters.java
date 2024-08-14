
package biouml.plugins.simulation;

import java.util.Arrays;
import java.util.Objects;

import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import biouml.model.Diagram;
import biouml.model.dynamics.EModel;

/**
 * @author anna
 *
 */
public class SimulationAnalysisParameters extends AbstractAnalysisParameters
{
    private DataElementPath modelPath;
    private Diagram diagram;
    private DataElementPath simulationResultPath;
    private int skipPoints = 0;
    private double outputStartTime = 0.0;
    private String simulationEngineName;
    private SimulationEngine simulationEngine;
    
    public SimulationAnalysisParameters()
    {
        String[] availableEngines = getAvailableEngines();
        if( availableEngines.length > 0 )
            setSimulationEngineName( availableEngines[0] );
    }

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

        this.diagram = (Diagram)de;

        String[] allowedEngines = SimulationEngineRegistry.getSimulationEngineNames( diagram.getRole( EModel.class ) );
        if(allowedEngines.length != 0 && !Arrays.asList( allowedEngines ).contains( simulationEngineName ) )
            setSimulationEngineName( allowedEngines[0] );
        setEngineDiagram();

        Object oldValue = this.modelPath;
        this.modelPath = modelPath;
        firePropertyChange("modelPath", oldValue, modelPath);
    }

    public SimulationEngine getSimulationEngine()
    {
        return simulationEngine;
    }
    public void setSimulationEngine(SimulationEngine engine)
    {
        setSimulationEngine( engine, false );
    }
    private void setSimulationEngine(SimulationEngine engine, boolean initFromModel)
    {
        Object oldValue = this.simulationEngine;
        this.simulationEngine = engine;
        
        if(engine != null)
        {
            engine.setParent( this );
            ComponentModel model = ComponentFactory.getModel(this);
            ComponentFactory.recreateChildProperties(model);
            
            setEngineDiagram();
        }
       
        firePropertyChange("simulationEngine", oldValue, engine);
    }
    private void setEngineDiagram()
    {
        if(diagram == null || simulationEngine == null)
            return;
        double initialTime = simulationEngine.getInitialTime();
        double completionTime = simulationEngine.getCompletionTime();
        double timeIncrement = simulationEngine.getTimeIncrement();
        simulationEngine.setDiagram(diagram);
        simulationEngine.setInitialTime( initialTime );
        simulationEngine.setCompletionTime( completionTime );
        simulationEngine.setTimeIncrement( timeIncrement );
    }

    public String getSimulationEngineName()
    {
        return simulationEngineName;
    }
    public void setSimulationEngineName(String engineName)
    {
        if(Objects.equals( engineName, this.simulationEngineName ))
            return;
        Object oldValue = this.simulationEngineName;
        this.simulationEngineName = engineName;
        SimulationEngine engine = SimulationEngineRegistry.getSimulationEngine(engineName);
        setSimulationEngine(engine, false);
        firePropertyChange("simulationEngineName", oldValue, engineName);
    }

    public DataElementPath getSimulationResultPath()
    {
        return simulationResultPath;
    }
    public void setSimulationResultPath(DataElementPath simulationResultPath)
    {
        Object oldValue = this.simulationResultPath;
        this.simulationResultPath = simulationResultPath;
        firePropertyChange("simulationResultPath", oldValue, simulationResultPath);
    }

    public int getSkipPoints()
    {
        return skipPoints;
    }
    public void setSkipPoints(int skipPoints)
    {
        Object oldValue = this.skipPoints;
        this.skipPoints = skipPoints;
        firePropertyChange("skipPoints", oldValue, skipPoints);
    }

    public double getOutputStartTime()
    {
        return outputStartTime;
    }

    public void setOutputStartTime(double outputStartTime)
    {
        Object oldValue = this.outputStartTime;
        this.outputStartTime = outputStartTime;
        firePropertyChange("outputStartTime", oldValue, outputStartTime);
    }

    public String[] getAvailableEngines()
    {
        if(diagram == null)
            return new String[]{};
//            return SimulationEngineRegistry.getAllSimulationEngineNames();
        EModel emodel = diagram.getRole( EModel.class );
        return SimulationEngineRegistry.getSimulationEngineNames(emodel);
    }
}
