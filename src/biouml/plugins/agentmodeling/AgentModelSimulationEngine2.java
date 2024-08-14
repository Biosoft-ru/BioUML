package biouml.plugins.agentmodeling;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import one.util.streamex.StreamEx;

import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.developmentontheedge.beans.DynamicPropertySet;

import ru.biosoft.util.TempFiles;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.SubDiagram;
import biouml.model.dynamics.Connection;
import biouml.model.dynamics.DirectedConnection;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.MultipleConnection;
import biouml.model.dynamics.UndirectedConnection;
import biouml.model.dynamics.UndirectedConnection.MainVariableType;
import biouml.model.dynamics.Variable;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.ResultWriter;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.Span;
import biouml.plugins.simulation.UniformSpan;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.standard.simulation.ResultListener;
import biouml.standard.simulation.SimulationResult;
import biouml.standard.type.Specie;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.Util;

@PropertyName ( "Agent-based simulation engine" )
@PropertyDescription ( "Simulation engine for agent-based models." )
public class AgentModelSimulationEngine2 extends SimulationEngine implements PropertyChangeListener
{
    private static final String VARIABLE_NAME_DELIMITER = "__";
    private static final String ADDITIONAL = "additional";

    private ArrayList<AgentSimulationEngineWrapper> engines;

    private AgentBasedModel agentModel;
    private HashMap<String, SimulationAgent> nameToAgent;
    private HashMap<String, SimulationEngine> nameToEngine;

    private String outputDir = TempFiles.path( "simulation" ).getAbsolutePath();

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if( doNotAffectSimulation( evt ) )
            return;
        diagramModified = true;
        updateFromDiagram();
//        restoreOriginalDiagram();
    }

    /**
     * Mapping between variable name and its index in results array.
     */
    protected Map<String, Integer> varIndexMapping;

    protected Map<String, Integer> varPathIndexMapping;

    private Map<String, String> varPathToOriginalPath = new HashMap<>();

    protected Map<String, Map<String, Integer>> compositeVarIndexMapping;

    protected static final double SPAN_LENGTH_THRESHOLD = 1E9;

    public AgentModelSimulationEngine2()
    {
        engines = new ArrayList<>();
        diagram = null;
        simulatorType = "AGENT_SIMULATOR";
        simulator = new Scheduler();
    }

    public void updateFromDiagram()
    {
        ArrayList<AgentSimulationEngineWrapper> newEngines = new ArrayList<>();
        for (int i=0; i<engines.size(); i++)
        {
            AgentSimulationEngineWrapper oldEngine = engines.get(i);
            if( oldEngine.de instanceof SubDiagram )
            {
                if( diagram.get(oldEngine.de.getCompleteNameInDiagram()) != null )
                    newEngines.add(oldEngine);
            }
        }

        Map<String, AgentSimulationEngineWrapper> exEngines = StreamEx.of(engines).toMap(e->e.de.getCompleteNameInDiagram(), e->e);

        for( SubDiagram subDiagram : diagram.recursiveStream().select(SubDiagram.class) )
        {
            if( exEngines.containsKey(subDiagram.getCompleteNameInDiagram()) )
                continue;
            SimulationEngine engine = DiagramUtility.getEngine(subDiagram.getDiagram());            
            if( engine == null )
            {
                log.error("Can not load simulation engine for subdiagram " + subDiagram.getName());
                continue;
            }
            
            engine.setParent(this);
            AgentSimulationEngineWrapper engineWrapper = new AgentSimulationEngineWrapper(engine);
            engineWrapper.setSubDiagram(subDiagram);
            
            newEngines.add(engineWrapper);
        }
        setEngines( newEngines.toArray( new AgentSimulationEngineWrapper[newEngines.size()]) );
    }

    @Override
    public boolean hasVariablesToPlot()
    {
        return super.hasVariablesToPlot();
    }

    @Override
    public void setDiagram(Diagram diagram)
    {
        if( this.diagram != null && this.diagram == diagram )
            return;

        this.diagram = diagram;
        this.originalDiagram = diagram;
        this.executableModel = (EModel)diagram.getRole();

        engines.clear();

        for (SubDiagram subDiagram: diagram.stream(SubDiagram.class))
        {
            Diagram associatedDiagram = subDiagram.getDiagram();
            SimulationEngine engine = DiagramUtility.getEngine(associatedDiagram);
            
            if( engine == null )
            {
                log.error( "Can not load simulation engine for diagram " + associatedDiagram.getName());
                return;
            }
            
            engine.setParent(this);
            AgentSimulationEngineWrapper engineWrapper = new AgentSimulationEngineWrapper(engine);
            engineWrapper.setSubDiagram(subDiagram);
            engines.add(engineWrapper);
        }
    }

    private void initVarPathIndexMapping()
    {
        varPathIndexMapping = new HashMap<>();

//        for( String var : mainEngine.getVarIndexMapping().keySet() )
//            varPathIndexMapping.put(var, mainEngine.getVarIndexMapping().get(var));
//
        int size = 0;

        for( AgentSimulationEngineWrapper engineWrapper : engines )
        {
            SimulationEngine engine = engineWrapper.getEngine();
            for( String var : engine.getVarIndexMapping().keySet() )
            {
                int index = size + engine.getVarIndexMapping().get(var);
                String path = DiagramUtility.generatPath(engine.getDiagram()) + VAR_PATH_DELIMITER + var;

                String originalPath = varPathToOriginalPath.get(path);

                if (originalPath != null)
                    path = originalPath;

                varPathIndexMapping.put(path, index);
            }
            size += engine.getVarIndexMapping().size();
        }
    }

    private void initVarIndexMapping(AgentBasedModel model)
    {
        varIndexMapping = new HashMap<>();
        int size = 0;
        for( SimulationAgent agent : model.getAgents() )
        {
            if( agent instanceof ModelAgent )
            {
                SimulationEngine engine = ( (ModelAgent)agent ).getEngine();
                for( String var : engine.getVarIndexMapping().keySet() )
                {
                    int index =  size + engine.getVarIndexMapping().get(var);
                    String newName = generateNewVariableName(var, agent.getName());
                    varIndexMapping.put(newName, index);
                }
                size += engine.getVarIndexMapping().size();
            }
        }
    }

    //TODO: probably this and initVarIndexMapping should be somehow integrated
    @Override
    public void initSimulationResult(SimulationResult simuationResult)
    {
        super.initSimulationResult(simuationResult);

        if( agentModel == null )
            return;

        for( SimulationAgent agent : agentModel.getAgents() )
        {
            if( agent instanceof ModelAgent )
            {
                EModel emodel = ( (ModelAgent)agent ).getEngine().getExecutableModel();
                for( Variable var : emodel.getVariables() )
                {
                    String newName = generateNewVariableName(var.getName(), agent.getName());
                    Variable newVar = new Variable(newName, null, null);
                    newVar.setInitialValue(var.getInitialValue());
                    simuationResult.addInitialValue(newVar);
                }
            }
        }
    }

    public static String generateNewVariableName(String oldName, String parentName)
    {
        if (parentName.isEmpty())
            return oldName;

        String prefix = parentName + VARIABLE_NAME_DELIMITER;

        boolean hasBuck = oldName.startsWith("$");
        boolean has2Bucks = oldName.startsWith("$$");

        String newName = has2Bucks ? oldName.substring(2) : hasBuck ? oldName.substring(1) : oldName;
        newName = StreamEx.of(newName.split("\\.")).map(s -> prefix + s).joining(".");
        newName = has2Bucks ? "$$" + newName : hasBuck ? "$" + newName : newName;
        return newName;
    }

    public void addEngine(SimulationEngine e)
    {
        engines.add(new AgentSimulationEngineWrapper(e));
    }

    public PlotAgent[] getPlotAgents()
    {
        return agentModel != null ? StreamEx.of(agentModel.getAgents()).select(PlotAgent.class).toArray(PlotAgent[]::new) : new PlotAgent[0];
    }

    public String calcPlotName(Integer index, Object plotAgent)
    {
        return ( (PlotAgent)plotAgent ).getName();
    }

    /**
     * Generate integrated model for all engines
     */
    @Override
    public AgentBasedModel createModel() throws Exception
    {
        nameToAgent = new HashMap<>();
        nameToEngine = new HashMap<>();
        agentModel = new AgentBasedModel();

        //adding model agents to model
        for( AgentSimulationEngineWrapper engine : engines )
        {
            engine.resetPreprocessors();
            engine.setOutputDir(outputDir+"/"+diagram.getName());
            engine.getPrototype().setStandalone(false);
            engine.addPreprocessor(new SubModelPreprocessor());
            engine.addPreprocessor2(new InitialValueSubmodelPreprocessor());
            String agentName = engine.de.getName();
            SimulationAgent agent = new ModelAgent(engine, agentName);
            log.info(agentName+" is ready");
            agent.setTimeScale(engine.getTimeScale());
            nameToAgent.put(agentName, agent);
            nameToEngine.put(agentName, engine);
            agentModel.addAgent(agent);
        }

        initVarPathIndexMapping();
        initVarIndexMapping(agentModel);

        //adding auxiliary modules
        for( Node node : diagram.getNodes() )
        {
            SimulationAgent agent = generateAgent(node);
            if( agent != null )
            {
                nameToAgent.put(agent.getName(), agent);
                agentModel.addAgent(agent);
            }
        }

        //adding links to model (model should contain all agents at this moment)
        for( Connection c : diagram.stream(Edge.class).map(Edge::getRole).select(Connection.class) )
            addConnection(c);
        nameToAgent.clear();
        nameToEngine.clear();
        return agentModel;
    }

    private SimulationAgent generateAgent(Node node) throws Exception
    {
        try
        {
            if( Util.isPlot(node) )
            {
                return generatePlotAgent(node);
            }
            else if( Util.isAverager(node) )
            {
                return generateAveragerAgent(node);
            }
            else if( Util.isSubDiagram(node) && node.getAttributes().getProperty(ADDITIONAL) != null )
            {
                Diagram innerDiagram = ( (SubDiagram)node ).getDiagram();
                SimulationEngine engine = new JavaSimulationEngine();
                engine.setDiagram(innerDiagram);
                nameToEngine.put(node.getName(), engine);
                return new ModelAgent(engine, node.getName());
            }
            else if( node.getKernel() != null && node.getKernel().getType().equals("ScriptAgent") )
            {
                return generateScriptAgent(node);
            }
        }
        catch( Exception ex )
        {
            throw new Exception("Error during generation of agent from elemnt " + node.getCompleteNameInDiagram(), ex);
        }
        return null;
    }
  
    private SimulationAgent generatePlotAgent(Node node) throws Exception
    {
        SimulationAgent plotAgent = new PlotAgent(node.getName(), generateAgentSpan(node));
        if( node.getAttributes().getProperty(Util.TIME_SCALE) != null )
            plotAgent.setTimeScale(Double.parseDouble(node.getAttributes().getValueAsString(Util.TIME_SCALE)));
        return plotAgent;
    }

    private SimulationAgent generateScriptAgent(Node node) throws Exception
    {
        String scriptType = node.getAttributes().getValueAsString("ScriptType");
        String script = node.getAttributes().getValueAsString("Script");
        String scriptInit = node.getAttributes().getValueAsString("ScriptInitial");
        String scriptResult = node.getAttributes().getValueAsString("ScriptResult");
        ScriptAgent agent = ScriptAgent.createScriptAgent(node.getName(), generateAgentSpan(node), script, scriptInit, scriptType,
                scriptResult);
        Node[] ports = ( (Compartment)node ).getNodes();
        for( Node port : ports )
            agent.addVariable(Util.getPortVariable(port));
        return agent;
    }

    private SimulationAgent generateAveragerAgent(Node node) throws Exception
    {
        int stepsForAverage = Integer.parseInt(node.getAttributes().getValueAsString(AveragerAgent.STEPS_FOR_AVERAGE));
        SimulationAgent averagerAgent = new AveragerAgent(node.getName(), generateAgentSpan(node), stepsForAverage);
        averagerAgent.setTimeScale(Double.parseDouble(node.getAttributes().getValueAsString(Util.TIME_SCALE)));
        return averagerAgent;
    }

    private Span generateAgentSpan(Node node) throws Exception
    {
        DynamicPropertySet dps = node.getAttributes();
        double initialTime = Double.parseDouble(dps.getValueAsString(Util.INITIAL_TIME));
        double completionTime = Double.parseDouble(dps.getValueAsString(Util.COMPLETION_TIME));
        double timeIncrement = Double.parseDouble(dps.getValueAsString(Util.TIME_INCREMENT));
        return new UniformSpan(initialTime, completionTime, timeIncrement);
    }

//    private SimulationAgent generateConstantAgent(Node node)
//    {
//        double value;
//        if( node.getAttributes().getProperty(Util.INITIAL_VALUE) == null )
//        {
//            log.error( "Constant " + node.getName() + " is corrupted: no value, will be set to 0");
//            value = 0.0;
//        }
//        else
//            value = Double.parseDouble(node.getAttributes().getValueAsString(Util.INITIAL_VALUE));
//        SimulationAgent constantAgent = new ConstantAgent(node.getName(), value);
//        return constantAgent;
//    }

    private String getAgentName(Node node)
    {
        if( node.getKernel() instanceof biouml.standard.type.Compartment || node.getKernel() instanceof Specie
                || biouml.standard.diagram.Util.isPort(node) || biouml.standard.diagram.Util.isBus(node) )
        {
//            if( ( (Node)node.getParent() ).getName().contains(TOP_DIAGRAM_SUFFIX))
//                    return "";
            return ( (Node)node.getParent() ).getName();
        }

        return node.getName();
    }

    /**
     * Adds connection to agent model
     */
    private void addConnection(Connection connection) throws Exception
    {
        if( connection instanceof MultipleConnection )
        {
            for( Connection innerConnection : ( (MultipleConnection)connection ).getConnections() )
                addConnection(innerConnection);
            return;
        }
        Edge edge = (Edge)connection.getDiagramElement();
        String senderName = getAgentName(edge.getInput());
        SimulationAgent sender = nameToAgent.get(senderName);

        String receiverName = getAgentName(edge.getOutput());
        SimulationAgent receiver = nameToAgent.get(receiverName);

        String outputVarName = connection.getOutputPort().getVariableName();
        SimulationEngine receiverEngine = nameToEngine.get(receiverName);
        if( receiver instanceof ModelAgent && receiverEngine != null )
        {
            if( !receiverEngine.getVarIndexMapping().containsKey(outputVarName) )
            {
                log.error( "Can not find variable " + outputVarName + " in model " + receiverName);
                return;
            }
            int index = receiverEngine.getVarIndexMapping().get(outputVarName);
            ( (ModelAgent)receiver ).addPort(outputVarName, index);
        }

        String inputVarName = connection.getInputPort().getVariableName();
        SimulationEngine senderEngine = nameToEngine.get(senderName);
        if( sender instanceof ModelAgent && senderEngine != null )
        {
            if( !senderEngine.getVarIndexMapping().containsKey(inputVarName) )
            {
                log.error( "Can not find variable " + inputVarName + " in model " + senderName);
                return;
            }
            int index = senderEngine.getVarIndexMapping().get(inputVarName);
            ( (ModelAgent)sender ).addPort(inputVarName, index);
        }

        //TODO support formulas for directed connection
        if( connection instanceof DirectedConnection )
            agentModel.addDirectedLink(sender, inputVarName, receiver, outputVarName);

        //TODO support initial value for undirected connection
        else if( connection instanceof UndirectedConnection )
        {
            String conversionFactor = ( (UndirectedConnection)connection ).getConversionFactor();
            MainVariableType type = ( (UndirectedConnection)connection ).getMainVariableType();
            agentModel.addUndirectedLink(sender, inputVarName, receiver, outputVarName, type.equals(MainVariableType.INPUT), conversionFactor);
        }
    }
 
    @PropertyName ( "Simulation engines" )
    @PropertyDescription ( "Agent engines." )
    public AgentSimulationEngineWrapper[] getEngines()
    {
        return engines.toArray(new AgentSimulationEngineWrapper[engines.size()]);
    }

    public void setEngines(AgentSimulationEngineWrapper[] e)
    {
        ArrayList<AgentSimulationEngineWrapper> oldValue = engines;
        engines = new ArrayList<>(Arrays.asList(e));
        firePropertyChange( "engines", oldValue, e );
    }

    public String calcEngineName(Integer index, Object engine)
    {
        return engines.get(index).de.getName() + " engine";
    }

    @Override
    public String getEngineDescription()
    {
        return "Agent model simulation engine";
    }

    @Override
    public Object getSolver()
    {
        return simulator;
    }

    @Override
    public String simulate(Model model, SimulationResult result) throws Exception
    {
        initSimulationResult(result);
        return simulate(model, new ResultListener[] {new ResultWriter(result)});
    }

    @Override
    public String simulate(Model model, ResultListener[] resultListeners) throws Exception
    {
        if( ! ( model instanceof AgentBasedModel ) )
            throw new Exception("Only agent model is acceptable for agent model simulation engine");

        this.agentModel = (AgentBasedModel)model;

        try
        {
            for( ResultListener listener : resultListeners )
                this.listeners.add(listener);

            for( ResultListener listener : listeners )
                listener.start(model);

            UniformSpan span = new UniformSpan(getInitialTime(), getCompletionTime(), getTimeIncrement());
            if( span.getLength() > SPAN_LENGTH_THRESHOLD )
                log.error( "WARNING: span for simulation result is too wide. Result will not be saved.");

            log.info("Model " + diagram.getName() + ": simulation started.");
            System.out.println("Model " + diagram.getName() + ": simulation started.");
            
            simulator.start(model, span, getListeners(), jobControl);
        }
        finally
        {
            restoreOriginalDiagram();
            removeAllListeners();

            log.info("Model " + diagram.getName() + ": simulation finished.");
            System.out.println("Model " + diagram.getName() + ": simulation finished.");
            log.info("");
        }
        return simulator.getProfile().getErrorMessage();
    }

    public HashMap<String, XYDataset> getDataSets()
    {
        PlotAgent[] plotAgents = getPlotAgents();
        HashMap<String, XYDataset> nameToDataset = new HashMap<>();
        for( PlotAgent agent : plotAgents )
        {
            XYSeriesCollection collection = new XYSeriesCollection();
            HashMap<String, XYSeries> dataset = agent.getDataSet();
            for( XYSeries series : dataset.values() )
                collection.addSeries(series);
            nameToDataset.put(agent.getName(), collection);
        }
        return nameToDataset;
    }

    @Override
    public Map<String, Integer> getVarIndexMapping()
    {
        return varIndexMapping;
    }

    //TODO: implement methods
    @Override
    public String getVariableCodeName(String varName)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getVariableCodeName(String diagramName, String varName)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setSolver(Object solver)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void stopSimulation()
    {
        restoreOriginalDiagram();
        if( simulator != null )
            simulator.stop();
    }

    @Override
    public void setOutputDir(String outputDir)
    {
       this.outputDir = outputDir;
    }

    @Override
    public String getOutputDir()
    {
       return outputDir;
    }

    @Override
    public void restoreOriginalDiagram()
    {
        for( AgentSimulationEngineWrapper engine : engines )
            engine.setSubDiagram((SubDiagram)diagram.findNode(engine.de.getCompleteNameInDiagram()));
    }

    @Override
    public Map<String, Integer> getVarPathIndexMapping()
    {
        return varPathIndexMapping;
    }

    @Override
    public void setCompletionTime(double completionTime)
    {
        super.setCompletionTime(completionTime);
    }

    @Override
    public void setInitialTime(double initialTime)
    {
        super.setInitialTime(initialTime);
    }

    @Override
    public void setTimeIncrement(double timeIncrement)
    {
        super.setTimeIncrement(timeIncrement);
    }
}
