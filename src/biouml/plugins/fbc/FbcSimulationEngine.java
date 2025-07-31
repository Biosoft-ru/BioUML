package biouml.plugins.fbc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import ru.biosoft.util.bean.BeanInfoEx2;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Variable;
import biouml.plugins.fbc.SbmlModelFBCReader.FluxBounds;
import biouml.plugins.simulation.ArraySpan;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.Options;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.SimulationEngineRegistry;
import biouml.plugins.simulation.Simulator;
import biouml.plugins.simulation.SimulatorRegistry;
import biouml.plugins.simulation.Span;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.simulation.ResultListener;
import biouml.standard.type.Reaction;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

@PropertyName ( "Flux Balance Analysis simulation engine" )
@PropertyDescription ( "Flux Balance Analysis simulation engine." )
public class FbcSimulationEngine extends SimulationEngine
{
    SimulationEngine engine = new JavaSimulationEngine();

    public FbcSimulationEngine()
    {
        simulatorType = "FBA";
        simulator = new FbcSolver(engine.getSimulator());
    }

    @Override
    public String[] getAvailableSolvers()
    {
        return engine.getAvailableSolvers();
    }

    @Override
    public String getEngineDescription()
    {
        return "FBA-" + simulator.getInfo().name;
    }

    @Override
    public String getVariableCodeName(String varName)
    {
        return engine.getVariableCodeName(varName);
    }

    @Override
    public String getVariableCodeName(String diagramName, String varName)
    {
        return engine.getVariableCodeName(diagramName, varName);
    }

    @Override
    public Map<String, Integer> getVarIndexMapping()
    {
        return engine.getVarIndexMapping();
    }

    public String getEngineName()
    {
        return SimulationEngineRegistry.getSimulationEngineName(getEngine());
    }

    public void setEngineName(String engineName)
    {
        setEngine(SimulationEngineRegistry.getSimulationEngine(engineName));
    }

    public SimulationEngine getEngine()
    {
        return engine;
    }

    public void setEngine(SimulationEngine engine)
    {
        Object oldValue = this.engine;
        this.engine = engine;
        this.engine.setDiagram(diagram);
        simulator = new FbcSolver(engine.getSimulator());
        this.firePropertyChange("engine", oldValue, engine);
    }

    @Override
    public Model createModel() throws Exception
    {
        for (Variable var: executableModel.getVariables())
        {
            if (var.getName().startsWith("$$"))
                var.setConstant(true);
        }
        Model model = engine.createModel();
        Map<String, Integer> indices = engine.getVarIndexMapping();
        FbcModel fbcModel = new ApacheModelCreator().createModel(diagram);

        Set<String> fbcReactions = new HashSet<>();
        for( String selectedReaction : fbcModel.getReactionNames() )
            fbcReactions.add(selectedReaction);

        List<Node> reactions = DiagramUtility.getReactionNodes(diagram);
        Map<String, Integer> constraintIndices = new HashMap<>();
        Map<String, Integer> parameterIndices = new HashMap<>();
        for( Node node : reactions )
        {
            Reaction r = (Reaction)node.getKernel();
            if( !fbcReactions.contains(r.getName()) )
                continue;
            String variableName = node.getRole( Equation.class ).getVariable();
            Variable variable = getExecutableModel().getVariable(variableName);
            if( variable == null )
                throw new Exception("");

            Integer index = indices.get(variableName);
            parameterIndices.put(r.getName(), index);

            DynamicProperty dp = node.getAttributes().getProperty(FbcConstant.FBC_BOUNDS);
            if( dp != null )
            {
                FluxBounds fluxBounds = (FluxBounds)dp.getValue();
                for( String val : fluxBounds.value )
                {
                    Variable var = executableModel.getVariable( val );
                    if( var != null )
                        constraintIndices.put( var.getName(), indices.get( var.getName() ) );
                }

            }
        }

        simulator = new FbcSolver(engine.getSimulator());
        ((FbcSolver)simulator).fbcModelCreator = new ApacheModelCreator();
        ((FbcSolver)simulator).fbcModelCreator.createModel(diagram);
        ((FbcSolver)simulator).reactionRateIndices = parameterIndices;
        ((FbcSolver)simulator).constraintIndices = constraintIndices;
        return model;
    }

    @Override
    public Object getSolver()
    {
        if (simulator instanceof FbcSolver)
            return ((FbcSolver)simulator).simulator;
        return simulator;
    }

    @Override
    public void setSolver(Object solver)
    {
        if (solver instanceof FbcSolver)
        simulator = (FbcSolver)solver;
    }

    @Override
    public String getSolverName()
    {
        return engine.getSolverName();
    }

    @Override
    public void setSolverName(String solverName)
    {
        engine.setSolverName( solverName );
    }

    @Override
    public String simulate(Model model, ResultListener[] resultListeners) throws Exception
    {
        log.info("Model " + diagram.getName() + ": simulation started.");

        Span tspan = new ArraySpan(engine.getInitialTime(), engine.getCompletionTime(), engine.getTimeIncrement());

        if( resultListeners != null )
        {
            for( ResultListener listener : resultListeners )
                listener.start(model);
        }

        try
        {
            simulator.start(model, tspan, resultListeners, jobControl);
        }
        catch( Throwable t )
        {
            return "Simulation error: " + t.getMessage();
        }

        log.info("Model " + diagram.getName() + ": simulation finished.");
        log.info("");
        return null;
    }

    @Override
    public void setDiagram(Diagram diagram)
    {
        super.setDiagram(diagram);
        engine.setDiagram(diagram);
    }

    @Override
    public void restoreOriginalDiagram()
    {
        this.diagram = originalDiagram;
        this.executableModel = (EModel)originalDiagram.getRole();
    }

    public String[] getAvailableSimulationEngines()
    {
        if (diagram == null || !(diagram.getRole() instanceof EModel))
            return new String[0];

        return SimulationEngineRegistry.getSimulationEngineNames( diagram.getRole( EModel.class ) );
    }

    @Override
    public Options getSimulatorOptions()
    {
        return new FbcOption();
    }

    public static class FbcOption extends Options
    {
    }
    
    public static class FbcOptionBeanInfo extends BeanInfoEx2<FbcOption>
    {
    }

    @Override
    public Map<String, Integer> getVarPathIndexMapping()
    {
        return getVarIndexMapping();
    }
}
