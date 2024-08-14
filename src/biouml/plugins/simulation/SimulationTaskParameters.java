

package biouml.plugins.simulation;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.SimulationEngineWrapper;

public class SimulationTaskParameters extends Option
{
    protected SimulationEngineWrapper engineWrapper;

    public SimulationTaskParameters()
    {
        engineWrapper = new SimulationEngineWrapper();
        engineWrapper.setParent(this);
    }

    public void setDiagram(Diagram diagram)
    {
        engineWrapper.setDiagram(diagram);
    }

    public Diagram getDiagram()
    {
        return engineWrapper.getDiagram();
    }

    public Object getParametersBean()
    {
        return engineWrapper;
    }

    public SimulationEngine getSimulationEngine()
    {
        return engineWrapper.getEngine();
    }

    public void setSimulationEngine(SimulationEngine engine)
    {
        engineWrapper.setEngine(engine);
    }

    @PropertyName("Simulation options")
    @PropertyDescription("A simulation engine suitable for the chosen diagram.")
    public SimulationEngineWrapper getEngineWrapper()
    {
        return engineWrapper;
    }
}
