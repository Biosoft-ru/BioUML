package biouml.plugins.agentmodeling;

import java.awt.Point;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.Set;
import one.util.streamex.StreamEx;

import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import ru.biosoft.util.TempFiles;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramContainer;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.SubDiagram;
import biouml.model.dynamics.Connection;
import biouml.model.dynamics.DirectedConnection;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.MultipleConnection;
import biouml.model.dynamics.UndirectedConnection;
import biouml.model.dynamics.UndirectedConnection.MainVariableType;
import biouml.plugins.simulation.ArraySpan;
import biouml.plugins.simulation.Model;
import biouml.plugins.simulation.ResultWriter;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.Span;
import biouml.plugins.simulation.UniformSpan;
import biouml.plugins.simulation.java.JavaSimulationEngine;
import biouml.standard.simulation.ResultListener;
import biouml.standard.simulation.SimulationResult;
import biouml.standard.type.Specie;
import biouml.standard.type.Stub.ConnectionPort;
import biouml.standard.diagram.CompositeModelPreprocessor;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.PortProperties;
import biouml.standard.diagram.Util;

@PropertyName ( "Agent population simulation engine" )
@PropertyDescription ( "Simulation ngine for agent-based models with multiple similar agents." )
public class PopulationAgentSimulationEngine extends SimulationEngine implements PropertyChangeListener
{
    private static final String VARIABLE_NAME_DELIMITER = "__";
    private static final String TOP_DIAGRAM_SUFFIX = "__main";
    private static final String ADDITIONAL = "additional";

    private AgentSimulationEngineWrapper mainEngine = new AgentSimulationEngineWrapper(new JavaSimulationEngine());
    private ArrayList<AgentSimulationEngineWrapper> engines;

    private AgentBasedModel agentModel;
    private HashMap<String, SimulationAgent> nameToAgent;
    private HashMap<String, SimulationEngine> nameToEngine;
    private String outputDir = TempFiles.path( "simulation" ).getAbsolutePath();
    private int agentsNumber = 0;
    public boolean oneSimulator = true;
    private int threads = 1;

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if( doNotAffectSimulation( evt ) )
            return;
        diagramModified = true;
        updateFromDiagram();
    }

    /**
     * Mapping between variable name and its index in results array.
     */
    protected Map<String, Integer> varIndexMapping;

    protected Map<String, Integer> varPathIndexMapping;

    protected static final double SPAN_LENGTH_THRESHOLD = 1E9;

    public PopulationAgentSimulationEngine()
    {
        engines = new ArrayList<>();
        diagram = null;
        simulatorType = "AGENT_SIMULATOR";
        simulator = new Scheduler();
        mainEngine.setParent(this);
    }

    List<ClassificationStatCollector> collectors = new ArrayList<>();
    public void addStatCollector(ClassificationStatCollector collector)
    {
        collectors.add( collector );
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
        mainEngine.setDiagram(diagram);
        engines.clear();

        for (SubDiagram subDiagram: diagram.stream(SubDiagram.class))
        {
            Diagram associatedDiagram = subDiagram.getDiagram();
            SimulationEngine engine = DiagramUtility.getEngine(associatedDiagram).clone();
            
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
        List<AgentSimulationEngineWrapper> newEngines = new ArrayList<>();
        newEngines.addAll(engines);
        
        //preprocess input to completely agent diagrams: all elements are only in subDiagrams
        Diagram processedDiagram = generateMainAgent(diagram, false);//!flatSubModels);
        processSwitches(processedDiagram);

        nameToAgent = new HashMap<>();
        nameToEngine = new HashMap<>();
        agentModel = new AgentBasedModel();

        //main engine uses main subDiagram
        mainEngine.setDiagram( ( (SubDiagram)processedDiagram.get(processedDiagram.getName() + TOP_DIAGRAM_SUFFIX) ).getDiagram());
        mainEngine.addPreprocessor(new SubModelPreprocessor());
        mainEngine.addPreprocessor2(new InitialValueSubmodelPreprocessor());
        mainEngine.setOutputDir(outputDir+"/"+diagram.getName());
        SimulationAgent mainAgent;
        if( oneSimulator )
            mainAgent = new LightModelAgent( mainEngine, "" );
        else
            mainAgent = new ModelAgent( mainEngine, "" );
        nameToAgent.put(mainAgent.getName(), mainAgent);
        nameToEngine.put(mainAgent.getName(), mainEngine);
        agentModel.addAgent(mainAgent);

        if( agentsNumber > 0 )
        {
            for( int i = 0; i < agentsNumber - 1; i++ )
            {
                agentModel.addAgent( mainAgent.clone( "agent_" + i ) );
            }
        }

        //adding model agents to model
        for( AgentSimulationEngineWrapper engine : newEngines )
        {
            engine.resetPreprocessors();
            engine.setOutputDir(outputDir+"/"+diagram.getName());
            engine.setSubDiagram((SubDiagram)processedDiagram.findNode(engine.de.getCompleteNameInDiagram()));
            engine.getPrototype().setStandalone(false);
            engine.addPreprocessor(new SubModelPreprocessor());
            engine.addPreprocessor2(new InitialValueSubmodelPreprocessor());
            String agentName = engine.de.getName();

            SimulationAgent agent;
            if( !engine.isNotSteadyState() )
            {
                agent = new SteadyStateAgent( engine, agentName );
                ( (SteadyStateAgent)agent ).setTimeBeforeSteadyState( engine.getTimeBeforeSteadyState() );
                ( (SteadyStateAgent)agent ).setTimeStep( engine.getTimeStepBeforeSteadyState() );
                ( (SteadyStateAgent)agent ).setTimeControlStart( engine.getControlTimeStart() );
                ( (SteadyStateAgent)agent ).setTimeControlStep( engine.getControlTimeStep() );        
                mainAgent.span.addPoints( calcAdditionalPointsSpan(((SteadyStateAgent) agent)));
            }
            else
                agent = new LightModelAgent( engine, agentName );

            log.info( agentName + " is ready" );
            agent.setTimeScale( engine.getTimeScale());
            nameToAgent.put(agentName, agent);
            nameToEngine.put(agentName, engine);
            agentModel.addAgent(agent);
        }

        //adding auxiliary modules
        for( Node node : processedDiagram.getNodes() )
        {
            SimulationAgent agent = generateAgent(mainAgent, node);
            if( agent != null )
            {
                nameToAgent.put(agent.getName(), agent);
                agentModel.addAgent(agent);
            }
        }

        //adding links to model (model should contain all agents at this moment)
        for( Connection c : processedDiagram.stream(Edge.class).map(Edge::getRole).select(Connection.class) )
            addConnection(c);
        nameToAgent.clear();
        nameToEngine.clear();
        return agentModel;
    }
    
    private double[] calcAdditionalPointsSpan(SteadyStateAgent agent)
    {
        double start = agent.getTimeControlStart();
        double step = agent.getTimeControlStep();
        double finish = agent.completionTime;
        if (start >= finish)
        {
            log.info( "No additional time points from agent "+agent.getName()+".");
            return new double[0];
        }
        if ((finish - start)/step > 10000)
        {
            log.error( "Too many additional time points from agent "+agent.getName()+". they will be ignored" );
            return new double[0];
        }
        ArraySpan subSpan = new ArraySpan(start, finish, step);            
        return subSpan.getTimes();
    }

    private SimulationAgent generateAgent(SimulationAgent mainAgent, Node node) throws Exception
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
            else if( Util.isConstant(node) )
            {
                return generateConstantAgent(mainAgent, node);
            }
            else if( Util.isSubDiagram(node) && node.getAttributes().getProperty(ADDITIONAL) != null )
            {
                Diagram innerDiagram = ( (SubDiagram)node ).getDiagram();
                SimulationEngine engine = new JavaSimulationEngine();
                engine.setDiagram(innerDiagram);
                nameToEngine.put(node.getName(), engine);
                
                if (node.getTitle().contains( "steady" ))
                    return new SteadyStateAgent( engine, node.getName());
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

    private SimulationAgent generateConstantAgent(SimulationAgent parent, Node node)
    {
        double value;
        if( node.getAttributes().getProperty(Util.INITIAL_VALUE) == null )
        {
            log.error( "Constant " + node.getName() + " is corrupted: no value, will be set to 0");
            value = 0.0;
        }
        else
            value = Double.parseDouble(node.getAttributes().getValueAsString(Util.INITIAL_VALUE));
        SimulationAgent constantAgent = new ConstantAgent(parent, node.getName(), value);
        return constantAgent;
    }

    private String getAgentName(Node node)
    {
        if( node.getKernel() instanceof biouml.standard.type.Compartment || node.getKernel() instanceof Specie
                || biouml.standard.diagram.Util.isPort(node) || biouml.standard.diagram.Util.isBus(node) )
        {
            if( ( (Node)node.getParent() ).getName().contains(TOP_DIAGRAM_SUFFIX))
                    return "";
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

    private void processSwitches(Diagram compositeDiagram) throws Exception
    {
        List<Node> toRemove = new ArrayList<>();
        List<Node> toAdd = new ArrayList<>();
        for( Node node : compositeDiagram.getNodes() )
        {
            if( !Util.isSwitch(node) || ! ( node instanceof Compartment ) )
                continue;

            SubDiagram subDiagram = CompositeModelPreprocessor.processSwitch((Compartment)node);
            subDiagram.getAttributes().add(node.getAttributes().getProperty(Util.INITIAL_TIME));
            subDiagram.getAttributes().add(node.getAttributes().getProperty(Util.TIME_INCREMENT));
            subDiagram.getAttributes().add(node.getAttributes().getProperty(Util.COMPLETION_TIME));
            subDiagram.getAttributes().add(new DynamicProperty(ADDITIONAL, Boolean.class, true));
            toRemove.add(node);
            toAdd.add(subDiagram);
        }

        for( Node node : toRemove )
            compositeDiagram.remove(node.getName());

        for( Node node : toAdd )
            compositeDiagram.put(node);
    }

    /**
     * Method creates agent diagram where all elements which are at the top of input <b>diagram</b> are placed in additional subDiagram
     */
    private Diagram generateMainAgent(Diagram diagram, boolean processBuses) throws Exception
    {
        Diagram result = diagram.clone(null, diagram.getName());
        DiagramUtility.processBuses( result );
      
        //create new subDiagram
        Diagram innerDiagram = diagram.clone(null, diagram.getName() + TOP_DIAGRAM_SUFFIX);
        SubDiagram subdiagram = new SubDiagram(result, innerDiagram, innerDiagram.getName());
        result.put(subdiagram);
        innerDiagram = subdiagram.getDiagram();

        if( processBuses )
        {
            //redirect all connections to bus on the top level to new buses inside subdiagram
            for( Node bus : result.recursiveStream().select(Node.class).filter(node -> Util.isBus(node)).toList() )
            {
                String fullName = bus.getCompleteNameInDiagram();
                Node newBus = (Node)innerDiagram.get(fullName);
                for( Edge e : bus.getEdges() )
                    Util.redirect(e, bus, newBus);
                result.remove(fullName);
            }
        }

        Util.getPorts(innerDiagram).filter(n -> Util.isPrivatePort(n)).forEach(n -> Util.setPublic(n));
        subdiagram.updatePorts();
        for( Node port : Util.getPorts(result) )
        {
            Node newPort;

            if( Util.isPublicPort(port) )
            {
                result.remove(port.getName());
                PortProperties properties = new PortProperties(result, port.getKernel().getClass());
                properties.setAccessType(ConnectionPort.PROPAGATED);
                properties.setModuleName(subdiagram.getName());
                properties.setBasePortName(port.getName());
                properties.setName(port.getName());
                newPort = (Node)properties.createElements( result, new Point(), null ).getElement( Util::isPort );
            }
            else
                newPort =  (Node)subdiagram.get(port.getName());

            for( Edge e : port.getEdges() )
                Util.redirect(e, port, newPort);
            result.remove(port.getName());
        }

        for (AgentSimulationEngineWrapper engine: engines)
        {
            if (!(engine.getEngine() instanceof AgentModelSimulationEngine) && DiagramUtility.containModules(engine.getDiagram()))
            {
                SubDiagram subDiagram = (SubDiagram)result.findDiagramElement(engine.de.getCompleteNameInDiagram());
                new CompositeModelPreprocessor().processCompositeSubDiagram(result, subDiagram);
            }
        }

        for( DiagramElement de : innerDiagram.recursiveStream().toSet() )
        {
            if( de instanceof DiagramContainer || Util.isPlot(de) || Util.isSwitch(de) || Util.isAverager(de) || Util.isConstant(de)
                    || de.getKernel() != null && de.getKernel().getType().equals("ScriptAgent") || Util.isConnection(de) )
                innerDiagram.remove(de.getCompleteNameInDiagram());
            else
                result.remove(de.getCompleteNameInDiagram());
        }
        return result;
    }

    public static boolean contains(Set<Node> nodes, Node node)
    {
        return nodes.contains(node) || nodes.contains(node.getParent());
    }

    public static void redirectEdge(Edge edge, Node node, Node newNode)
    {
        boolean input = node.equals(edge.getInput());
        boolean output = node.equals(edge.getOutput());
        if( input )
            edge.setInput(newNode);
        if( output )
            edge.setOutput(newNode);

        if( input || output )
            newNode.addEdge(edge);
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

    @PropertyName ( "Main engine" )
    @PropertyDescription ( "Main simulation engine." )
    public SimulationEngine getMainEngine()
    {
        return mainEngine;
    }

    public void setMainEngine(SimulationEngine e)
    {
        if( e instanceof AgentSimulationEngineWrapper )
            mainEngine = (AgentSimulationEngineWrapper)e;
        else
            mainEngine = new AgentSimulationEngineWrapper( e );
        mainEngine.setParent( this );
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
            
            for (SimulationAgent agent: agentModel.agents)
            {
                if (agent instanceof SteadyStateAgent)
             span.addPoints( calcAdditionalPointsSpan(((SteadyStateAgent) agent)));
            }
            log.info("Model " + diagram.getName() + ": simulation started.");
            System.out.println( "Model " + diagram.getName() + ": simulation started." );
            if( oneSimulator )
            {
                simulator = new PopulationScheduler( this.mainEngine.getSimulator(), threads );
                for( ClassificationStatCollector collector : this.collectors )
                    ( (PopulationScheduler)simulator ).addStatisticsCollector( collector );
            }
            else
                simulator = new Scheduler();
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
        mainEngine.setDiagram(diagram);

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
        for (AgentSimulationEngineWrapper engine: engines)
            engine.setCompletionTime(completionTime / engine.getTimeScale());
        mainEngine.setCompletionTime(completionTime);
    }

    @Override
    public void setInitialTime(double initialTime)
    {
        super.setInitialTime(initialTime);
        for (AgentSimulationEngineWrapper engine: engines)
            engine.setInitialTime(initialTime);
        mainEngine.setInitialTime(initialTime);
    }

    @Override
    public void setTimeIncrement(double timeIncrement)
    {
        super.setTimeIncrement(timeIncrement);
        for( AgentSimulationEngineWrapper engine : engines )
            engine.setTimeIncrement( timeIncrement );
        mainEngine.setTimeIncrement(timeIncrement);
    }

    @Override
    public void setLogLevel(Level level)
    {
        super.setLogLevel(level);
        for( AgentSimulationEngineWrapper innerEngine : engines )
            innerEngine.setLogLevel( level );
    }

    @PropertyName ( "Number of agents" )
    public int getAgentsNumber()
    {
        return agentsNumber;
    }

    public void setAgentsNumber(int agentsNumber)
    {
        this.agentsNumber = agentsNumber;
    }

    public void setThreads(int threads)
    {
        this.threads = threads;
    }

    @PropertyName ( "Threads number" )
    public int getThreads()
    {
        return threads;
    }

    private void initVarPathIndexMapping(List<AgentSimulationEngineWrapper> engines)
    {
        varPathIndexMapping = new HashMap<>();

        for( String var : mainEngine.getVarIndexMapping().keySet() )
            varPathIndexMapping.put( var, mainEngine.getVarIndexMapping().get( var ) );

        int size = mainEngine.getVarIndexMapping().size();

        for( AgentSimulationEngineWrapper engineWrapper : engines )
        {
            SimulationEngine engine = engineWrapper.getEngine();
            for( String var : engine.getVarIndexMapping().keySet() )
            {
                int index = size + engine.getVarIndexMapping().get( var );
                String path = DiagramUtility.generatPath( engine.getDiagram() ) + VAR_PATH_DELIMITER + var;
                varPathIndexMapping.put( path, index );
            }
            size += engine.getVarIndexMapping().size();
        }
    }
}
