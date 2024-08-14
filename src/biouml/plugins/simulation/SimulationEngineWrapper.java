
package biouml.plugins.simulation;

import one.util.streamex.StreamEx;
import biouml.model.Diagram;
import biouml.model.dynamics.EModel;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.standard.diagram.DiagramUtility;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import com.developmentontheedge.beans.model.ComponentFactory;

/**
 * 
 * @author axec
 * Wrapper class for simulation engine - provides appropriate engines for currently set diagram
 */
public class SimulationEngineWrapper extends Option
{
    private SimulationEngine engine;
    private String engineName = noEngine;
    private Diagram diagram;
    private PropertyChangeListener engineListener;

    private static final String noEngine = "NO ENGINE SET";

    public SimulationEngineWrapper()
    {
        setEngine( new JavaSimulationEngine() );
        engine.setParent(this);
        engineListener = initEngineListener();
        engine.addPropertyChangeListener( engineListener );
    }

    public SimulationEngineWrapper(Diagram diagram)
    {
        engineListener = initEngineListener();
        if( diagram != null )
            setDiagram(diagram);
    }

    public boolean isEngineHidden()
    {
        return ( engineName.equals(noEngine) );
    }

    private PropertyChangeListener initEngineListener()
    {
        return new PropertyChangeListener()
        {
            @Override
            public void propertyChange(PropertyChangeEvent evt)
            {
                if( evt.getPropertyName().equals( "simulator" ) )
                {
                    ComponentFactory.recreateChildProperties(ComponentFactory.getModel(getParent()).findProperty(getNameInParent()));
                    firePropertyChange("*", null, null);
                }
            }
        };
    }

    public void setDiagram(Diagram diagram)
    {
        this.diagram = diagram;

        if( diagram != null )
        {
            SimulationEngine engine = DiagramUtility.getEngine( diagram );
            if( engine != null )
            {
                if( this.engine == null || !this.engine.getClass().equals( engine.getClass() ) )
                    setEngine( engine );
            }
            else if( diagram.getRole() instanceof EModel )
            {
                String[] names = SimulationEngineRegistry.getSimulationEngineNames( diagram.getRole( EModel.class ) );
                if( StreamEx.of( names ).has( engineName ) )
                    this.engine.setDiagram(diagram);
                else
                    setEngineName( ( names.length > 0 ) ? names[0] : noEngine);
            }
        }
    }

    public void setEngine(SimulationEngine engine)
    {
        if(this.engine == engine)
            return;

        engineName = SimulationEngineRegistry.getSimulationEngineName(engine);
        if( engineName != null )
        {
            Object oldValue = this.engine;

            if( this.engine != null )
                this.engine.removePropertyChangeListener( engineListener );
            this.engine = engine;
            this.engine.setParent(this);

            this.engine.addPropertyChangeListener( engineListener );

            if( diagram != null )
                engine.setDiagram(diagram);
            firePropertyChange("engine", oldValue, engine);
        }
    }

    public SimulationEngine getEngine()
    {
        return engine;
    }

    public Diagram getDiagram()
    {
        return diagram;
    }

    public void setEngineName(String name)
    {
        if(engine != null)
            engine.removePropertyChangeListener( engineListener );

        Object oldValue = this.engine;
        if( name.equals(noEngine) )
        {
            engine = null;
        }
        else
        {
            engine = SimulationEngineRegistry.getSimulationEngine(name);
            engine.addPropertyChangeListener( engineListener );
            engine.setDiagram(diagram);
            //diagram.getAttributes().add( DPSUtils.createHiddenReadOnlyTransient( "simulationOptions", SimulationEngine.class, engine ) );
            engine.setParent(this);
            engineName = ( engine != null ) ? name : noEngine;
        }

        firePropertyChange("engine", oldValue, engine);
        firePropertyChange("*", null, null);
    }

    @PropertyName("Selected engine")
    @PropertyDescription("Selected simulation engine.")
    public String getEngineName()
    {
        return engineName;
    }

    public String[] getAvailableEngines()
    {
        if( diagram != null && diagram.getRole() instanceof EModel )
            return SimulationEngineRegistry.getSimulationEngineNames( diagram.getRole( EModel.class ) );
        return new String[] {engineName};
    }
}
