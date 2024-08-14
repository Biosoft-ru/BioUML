package biouml.plugins.agentmodeling;

import java.util.Map;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Option;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.SubDiagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;
import biouml.plugins.simulation.ArraySpan;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.Options;
import biouml.plugins.simulation.Preprocessor;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.SimulationEngineRegistry;
import biouml.plugins.simulation.Simulator;
import biouml.plugins.simulation.Span;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.standard.diagram.Util;
import biouml.standard.simulation.ResultListener;

@SuppressWarnings ( "serial" )
@PropertyName("Simulation engine")
@PropertyDescription("Simulation engine.")
public class AgentSimulationEngineWrapper extends SimulationEngine
{
    //subdiagram or diagram
    DiagramElement de;
    private SimulationEngine prototypeEngine = new JavaSimulationEngine();
    
    public static final String TYPE_STANDARD = "Standard";
    public static final String TYPE_STEADY_STATE = "Steady State";
    
    public AgentSimulationEngineWrapper()
    {
        prototypeEngine = new JavaSimulationEngine();
    }
    
    public AgentSimulationEngineWrapper(SimulationEngine engine)
    {
        this.prototypeEngine = engine;
        this.diagram = engine.getDiagram();

        if( diagram != null )
            de = ( diagram.getParent() instanceof SubDiagram ) ? (SubDiagram)diagram.getParent() : diagram;
    }
    
    @PropertyName("Simulation engine")
    public String getEngineName()
    {
        return getEngine() == null? null: SimulationEngineRegistry.getSimulationEngineName(getEngine());
    }

    public void setEngineName(String engineName)
    {
        setEngine(SimulationEngineRegistry.getSimulationEngine(engineName));
    }
    
    public SimulationEngine getEngine()
    {
        return prototypeEngine;
    }

    public void setEngine(SimulationEngine engine)
    {
        Object oldValue = this.prototypeEngine;
        this.prototypeEngine = engine;
        if( diagram != null )
            prototypeEngine.setDiagram(this.diagram);
        this.firePropertyChange("engine", oldValue, engine);
    }

    @Override
    public String getSimulatorType()
    {
        if( prototypeEngine != null )
            return prototypeEngine.getSimulatorType();
        return "Unknown";
    }
    
    public SimulationEngine getPrototype()
    {
        return prototypeEngine;
    }

    public void setPrototype(SimulationEngine engine)
    {
        setDiagram( engine.getDiagram() );
    }

    @Override
    public void setDiagram(Diagram diagram)
    {
        if( prototypeEngine != null )
            prototypeEngine.setDiagram( diagram );
        this.de = diagram;
        this.diagram = diagram;
        this.executableModel = diagram.getRole(EModel.class);
    }

    public void setSubDiagram(SubDiagram subDiagram)
    {
        Diagram innerDiagram = subDiagram.getDiagram();
        if( prototypeEngine != null )
            prototypeEngine.setDiagram( innerDiagram );
        this.diagram = innerDiagram;
        this.executableModel = innerDiagram.getRole(EModel.class);
        this.de = subDiagram;
    }


    @Override
    public Diagram getDiagram()
    {
        if( prototypeEngine != null )
        return prototypeEngine.getDiagram();
        return null;
    }

    @Override
    public String getEngineDescription()
    {
        if( prototypeEngine != null )
            return prototypeEngine.getEngineDescription();
        return "Unknown";
    }

    @Override
    public Model createModel() throws Exception
    {
        if( prototypeEngine != null )
            return prototypeEngine.createModel();
        return null;
    }

    @Override
    public Object getSolver()
    {
        if( prototypeEngine != null )
            return prototypeEngine.getSolver();
        return null;
    }

    @Override
    public void setSolver(Object solver)
    {
        if( prototypeEngine != null )
        prototypeEngine.setSolver( solver );
    }
    
    @Override
    public void setParent(Option option)
    {
        if(prototypeEngine != null)
          prototypeEngine.setParent(option);
    }

    @Override
    @PropertyName("Simulator")
    public String getSolverName()
    {
        if( prototypeEngine != null )
            return prototypeEngine.getSolverName();
        return "Unknown";
    }

    @Override
    public void setSolverName(String solverName)
    {
        if( prototypeEngine != null )
        prototypeEngine.setSolverName( solverName );
    }

    @Override
    @PropertyName("Simulator options")
    public Options getSimulatorOptions()
    {
        if( prototypeEngine == null )
            return super.getSimulatorOptions();
        return prototypeEngine.getSimulatorOptions();
    }

    @Override
    public Simulator getSimulator()
    {
        if( prototypeEngine == null )
            return super.getSimulator();
        return prototypeEngine.getSimulator();
    }

    @Override
    public void setSimulatorOptions(Options options)
    {
        prototypeEngine.setSimulatorOptions( options );
    }

    @Override
    public Map<String, Integer> getVarIndexMapping()
    {
        return prototypeEngine.getVarIndexMapping();
    }

    @Override
    public String getVariableCodeName(String varName)
    {
        return prototypeEngine.getVariableCodeName( varName );
    }

    @Override
    public String getVariableCodeName(String diagramName, String varName)
    {
        return prototypeEngine.getVariableCodeName( diagramName, varName );
    }

    @Override
    public String simulate(Model model, ResultListener[] resultListeners) throws Exception
    {
        return prototypeEngine.simulate( model, resultListeners );
    }

    public Span getSpan()
    {
        return new ArraySpan( getInitialTime(), getCompletionTime(), getTimeIncrement() );
    }
    
    @Override
    @PropertyName("Initial time")
    public double getInitialTime()
    {
        if( de == null )
            return prototypeEngine == null ? super.getInitialTime() : prototypeEngine.getInitialTime();
        try
        {
            DynamicProperty dp = getProperty(de, Util.INITIAL_TIME, Double.class, 0.0 );
            return Double.parseDouble( dp.getValue().toString() );
        }
        catch( NumberFormatException ex )
        {
            return 0;
        }
    }

    @Override
    public void setInitialTime(double initialTime)
    {
        if( de == null )
        {
            if( prototypeEngine != null )
                prototypeEngine.setInitialTime( initialTime );
            else
                super.setInitialTime( initialTime );
            return;
        }
        DynamicProperty dp = getProperty(de, Util.INITIAL_TIME, Double.class, initialTime );
        dp.setValue( initialTime );
    }

    @Override
    @PropertyName("Completion time")
    public double getCompletionTime()
    {
        if( de == null )
            return prototypeEngine == null ? super.getCompletionTime() : prototypeEngine.getCompletionTime();
        try
        {
            DynamicProperty dp = getProperty(de, Util.COMPLETION_TIME, Double.class, 100.0 );
            return Double.parseDouble( dp.getValue().toString() );
        }
        catch( NumberFormatException ex )
        {
            return 100;
        }
    }

    @Override
    public void setCompletionTime(double completionTime)
    {
        if( de == null )
        {
            if( prototypeEngine == null )
                super.setCompletionTime( completionTime );
            else
                prototypeEngine.setCompletionTime( completionTime );
            return;
        }
        DynamicProperty dp = getProperty(de, Util.COMPLETION_TIME, Double.class, completionTime );
        dp.setValue( completionTime );
    }

    @Override
    @PropertyName("Time increment")
    public double getTimeIncrement()
    {
        if( de == null )
            return prototypeEngine == null ? super.getTimeIncrement() : prototypeEngine.getTimeIncrement();
        try
        {
            DynamicProperty dp = getProperty(de, Util.TIME_INCREMENT, Double.class, 0.1 );
            return Double.parseDouble( dp.getValue().toString() );
        }
        catch( NumberFormatException ex )
        {
            return 1;
        }
    }

    @Override
    public void setTimeIncrement(double timeIncrement)
    {
        if( de == null )
        {
            if( prototypeEngine == null )
                super.setTimeIncrement( timeIncrement );
            else
                prototypeEngine.setTimeIncrement( timeIncrement );
            return;
        }
        DynamicProperty dp = getProperty(de, Util.TIME_INCREMENT, Double.class, timeIncrement );
        dp.setValue( timeIncrement );
    }

    @PropertyName("Time scale")
    @PropertyDescription("Time description.")
    public double getTimeScale()
    {
        if( de == null )
            return 1;
        try
        {
            DynamicProperty dp = getProperty(de, Util.TIME_SCALE, Integer.class, 1 );
            Variable var = Diagram.getDiagram(de).getRole(EModel.class).getVariable(dp.getValue().toString());
            if (var != null)
                return var.getInitialValue();
            return Double.parseDouble( dp.getValue().toString() );
        }
        catch( Exception ex )
        {
            return 1;
        }
    }

    public void setTimeScale(double timeScale)
    {
        if( de == null )
            return;
        DynamicProperty dp = getProperty(de,  Util.TIME_SCALE, Integer.class, timeScale );
        dp.setValue( timeScale );
    }

    public DynamicProperty getProperty(DiagramElement de, String propertyName, Class<?> propertyType, Object defaultValue)
    {
        DynamicProperty dp = de.getAttributes().getProperty( propertyName );
        if( dp == null )
        {
            dp = new DynamicProperty( propertyName, propertyType, defaultValue );
            dp.setHidden( true );
            de.getAttributes().add( dp );
        }
        return dp;
    }

    @Override
    public void setOutputDir(String dir)
    {
        if( prototypeEngine != null )
            this.prototypeEngine.setOutputDir( dir );
    }
    
    @Override
    public void restoreOriginalDiagram()
    {
        prototypeEngine.restoreOriginalDiagram();
    }
    
    @Override
    public void preprocess(Diagram diagram) throws Exception
    {
        prototypeEngine.preprocess(diagram);
    }
    
    @Override
    public void addPreprocessor(Preprocessor preprocessor)
    {
        prototypeEngine.addPreprocessor( preprocessor );
    }
    
    @Override
    public void addPreprocessor2(Preprocessor preprocessor)
    {
        prototypeEngine.addPreprocessor2( preprocessor );
    }
    
    
    @Override
    public EModel getExecutableModel()
    {
        return prototypeEngine.getExecutableModel();
    }
    
    public String[] getAvailableSimulationEngines()
    {
        return SimulationEngineRegistry.getAllSimulationEngineNames();
    }
    
    @Override
    public Map<String, Integer> getVarPathIndexMapping()
    {
        return prototypeEngine.getVarPathIndexMapping();
    }
    
    public String getSubDiagramPath()
    {
        if (de == null)
            return null;
        return de.getCompleteNameInDiagram();
    }
    
    public void setSubDiagramPath(String path)
    {
       //do nothing
    }
    
    //TODO: refactor
    @PropertyName("Simulation step type")
    @PropertyDescription("Simulation step type. Default is standard simulation until next time span. Experimental alternative is simulation until steady state reached.")
    public String getStepType()
    {
        if (de == null)
            return TYPE_STANDARD;
        DynamicProperty dp = de.getAttributes().getProperty( "stepType" );
        if (dp == null)
            return TYPE_STANDARD;
        return dp.getValue().toString();
    }
    public void setStepType(String type)
    {
        if (de == null)
            return;
        
        String oldValue = getStepType();
        
        DynamicProperty dp = de.getAttributes().getProperty( "stepType" );
        if (dp == null && TYPE_STEADY_STATE.equals( type ) )
            de.getAttributes().add( new DynamicProperty("stepType", String.class, type));
        else
            de.getAttributes().remove( "stepType" );
        
        firePropertyChange( "stepType",  oldValue, type);
    }
    
    @PropertyName("Inner time limit")
    public double getTimeBeforeSteadyState()
    {
        if( de == null )
            return 100;
        DynamicProperty dp = de.getAttributes().getProperty( "timeBeforeSteadyState" );
        if( dp == null )
            return 100;
        return (Double)dp.getValue();
    }
    public void setTimeBeforeSteadyState(double timeBeforeSteadyState)
    {
        if (de == null || de.getAttributes().getProperty( "stepType" ) == null)
            return;        
        
        Object oldValue = getTimeBeforeSteadyState();
        
        DynamicProperty dp = de.getAttributes().getProperty( "timeBeforeSteadyState" );
        if (dp == null )
            de.getAttributes().add( new DynamicProperty("timeBeforeSteadyState", Double.class, timeBeforeSteadyState));
        else
            dp.setValue( timeBeforeSteadyState );
        
        firePropertyChange( "timeBeforeSteadyState",  oldValue, timeBeforeSteadyState);
    }
    
    public boolean isNotSteadyState()
    {
        return !getStepType().equals( TYPE_STEADY_STATE );
    }

    @PropertyName("Inner time step")
    public double getTimeStepBeforeSteadyState()
    {
        if (de == null)
            return 1;
        DynamicProperty dp = de.getAttributes().getProperty( "timeStepBeforeSteadyState" );
        if (dp == null)
            return 1;
        return (Double)dp.getValue();
    }
    public void setTimeStepBeforeSteadyState(double timeStepBeforeSteadyState)
    {   
        if (de == null || de.getAttributes().getProperty( "stepType" ) == null)
            return;   
        Object oldValue = getTimeStepBeforeSteadyState();
        
        DynamicProperty dp = de.getAttributes().getProperty( "timeStepBeforeSteadyState" );
        if (dp == null )
            de.getAttributes().add( new DynamicProperty("timeStepBeforeSteadyState", Double.class, timeStepBeforeSteadyState));
        else
            dp.setValue( timeStepBeforeSteadyState );
        
        firePropertyChange( "timeStepBeforeSteadyState",  oldValue, timeStepBeforeSteadyState);
    }
    
    @PropertyName("Control time start")
    public double getControlTimeStart()
    {
        if (de == null)
            return 1;
        DynamicProperty dp = de.getAttributes().getProperty( "controlTimeStart" );
        if (dp == null)
            return 1;
        return (Double)dp.getValue();
    }
    public void setControlTimeStart(double controlTimeStart)
    {   
        if (de == null || de.getAttributes().getProperty( "stepType" ) == null)
            return;   
        Object oldValue = getControlTimeStart();
        
        DynamicProperty dp = de.getAttributes().getProperty( "controlTimeStart" );
        if (dp == null )
            de.getAttributes().add( new DynamicProperty("controlTimeStart", Double.class, controlTimeStart));
        else
            dp.setValue( controlTimeStart );
        
        firePropertyChange( "controlTimeStart",  oldValue, controlTimeStart);
    }
    
    @PropertyName("Control time step")
    public double getControlTimeStep()
    {
        if (de == null)
            return 0.1;
        DynamicProperty dp = de.getAttributes().getProperty( "controlTimeStep" );
        if (dp == null)
            return 1;
        return (Double)dp.getValue();
    }
    public void setControlTimeStep(double controlTimeStep)
    {   
        if (de == null || de.getAttributes().getProperty( "stepType" ) == null)
            return;   
        Object oldValue = getControlTimeStep();
        
        DynamicProperty dp = de.getAttributes().getProperty( "controlTimeStep" );
        if (dp == null )
            de.getAttributes().add( new DynamicProperty("controlTimeStep", Double.class, controlTimeStep));
        else
            dp.setValue( controlTimeStep );
        
        firePropertyChange( "controlTimeStep",  oldValue, controlTimeStep);
    }
}