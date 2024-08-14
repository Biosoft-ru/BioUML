package biouml.plugins.research.workflow.engine;

import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.model.Property;
import com.developmentontheedge.beans.model.ComponentFactory.Policy;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.plugins.research.MessageBundle;
import biouml.plugins.research.research.ResearchDiagramType;
import biouml.plugins.research.research.ResearchSemanticController;
import biouml.plugins.research.workflow.WorkflowDiagramViewOptions;
import biouml.plugins.research.workflow.WorkflowSemanticController;
import biouml.plugins.research.workflow.items.WorkflowCycleVariable;
import biouml.plugins.research.workflow.items.WorkflowExpression;
import biouml.plugins.research.workflow.items.WorkflowItem;
import biouml.plugins.research.workflow.items.WorkflowItemFactory;
import biouml.plugins.research.workflow.items.WorkflowParameter;
import biouml.plugins.research.workflow.items.WorkflowVariable;
import biouml.standard.simulation.ScriptDataGenerator;
import biouml.standard.type.Base;
import biouml.standard.type.Type;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.script.ScriptDataElement;
import ru.biosoft.access.script.ScriptTypeRegistry;
import ru.biosoft.access.task.TaskPool;
import ru.biosoft.analysiscore.AnalysisDPSUtils;
import ru.biosoft.analysiscore.AnalysisMethod;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.analysiscore.AnalysisParametersExceptionEvent;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;
import ru.biosoft.jobcontrol.JobControlEvent;
import ru.biosoft.jobcontrol.JobControlListener;
import ru.biosoft.journal.JournalRegistry;
import ru.biosoft.tasks.TaskInfo;
import ru.biosoft.tasks.TaskManager;
import ru.biosoft.util.BeanAsMapUtil;
import ru.biosoft.util.BeanUtil;
import ru.biosoft.util.DPSUtils;
import ru.biosoft.util.TextUtil;

/**
 * Engine for workflow execution.
 */
public class WorkflowEngine implements JobControlListener
{
    protected Logger log = Logger.getLogger(WorkflowEngine.class.getName());

    public static final String NODE_STATUS_PROPERTY = "statusProperty";
    public static final String EDGE_TMP_VALUE_PROPERTY = "tmpResultName";

    public static final String SKIP_COMPLETED_PROPERTY = "skipCompleted";
    public static final String IGNORE_FAIL_PROPERTY = "ignoreFail";

    protected Diagram workflow;
    protected ExecutionMap executionMap;
    protected boolean terminated;
    protected Set<ru.biosoft.access.core.DataElementPath> autoOpenPaths = new HashSet<>();
    protected Set<ru.biosoft.access.core.DataElementPath> temporaryPaths = new HashSet<>();
    protected Set<ru.biosoft.access.core.DataElementPath> outputs = new HashSet<>();
    protected FunctionJobControl jobControl;
    protected Set<WorkflowEngineListener> listeners = new HashSet<>();
    private final List<BufferedImage> images = new ArrayList<>();
    protected boolean skipCompleted;
    protected DynamicPropertySet parameters;
    protected boolean ignoreFail;
    protected boolean paused = false;

    /**
     * Set workflow model
     */
    public void setWorkflow(Diagram workflow)
    {
        this.workflow = workflow;
    }

    public void setParameters(DynamicPropertySet parameters)
    {
        this.parameters = parameters;
    }

    public void setJobControl(FunctionJobControl jobControl)
    {
        this.jobControl = jobControl;
    }

    public void setLogger(Logger logger)
    {
        this.log = logger;
    }

    /**
     * Add execution listener to workflow engine
     */
    public void addEngineListener(WorkflowEngineListener listener)
    {
        listeners.add(listener);
    }

    /**
     * Remove execution listener from workflow engine
     */
    public void removeEngineListener(WorkflowEngineListener listener)
    {
        listeners.remove(listener);
    }

    public void initWorkflow() throws Exception
    {
        try
        {
            checkModel();
            autoOpenPaths.clear();
            temporaryPaths.clear();
            executionMap = buildExecutionMap();
        }
        catch( Exception e )
        {
            for( WorkflowEngineListener listener : listeners )
            {
                listener.errorDetected(e.getMessage());
                log.log(Level.SEVERE, e.getMessage());
                listener.finished();
            }
            throw e;
        }
    }


    public void setSkipCompleted(boolean skipCompleted)
    {
        this.skipCompleted = skipCompleted;
    }

    public boolean isSkipCompleted()
    {
        return skipCompleted;
    }

    public void setIgnoreFail(boolean ignoreFail)
    {
        this.ignoreFail = ignoreFail;
    }

    public boolean isIgnoreFail()
    {
        return ignoreFail;
    }

    public boolean isPaused()
    {
        return paused;
    }

    public Diagram createResearchDiagram(DataElementPath targetPath) throws Exception
    {
        ResearchDiagramType rdt = new ResearchDiagramType();
        Diagram diagram = rdt.createDiagram(targetPath.optParentCollection(), targetPath.getName(), null);
        diagram.getInfo().setNodeImageLocation(MessageBundle.class, "resources/workflow.gif");
        targetPath.remove();
        targetPath.save(diagram);
        Map<Node, Node> nodeMap = new HashMap<>();
        ResearchSemanticController semanticController = (ResearchSemanticController)diagram.getType().getSemanticController();
        for(DiagramElement de : workflow)
        {
            if( ( de instanceof Node ) && ( ( (Node)de ).getKernel() != null ) )
            {
                Node node = (Node)de;
                WorkflowItem item = WorkflowItemFactory.getWorkflowItem(node);
                if(item instanceof WorkflowVariable)
                {
                    WorkflowVariable variable = (WorkflowVariable)item;
                    if(variable.getType().getTypeClass().isAssignableFrom(DataElementPath.class))
                    {
                        Node newNode = semanticController.createDataElementNode(diagram, (DataElementPath)variable.getValue());
                        newNode.setLocation(node.getLocation());
                        diagram.put(newNode);
                        nodeMap.put(node, newNode);
                    }
                }
                else if( node.getKernel().getType().equals(Type.ANALYSIS_METHOD) )
                {
                    Node newNode = semanticController.createAnalysisNode(diagram, AnalysisDPSUtils.getAnalysisMethodByNode(node.getAttributes()).getName(), node.getAttributes());
                    newNode.setLocation(node.getLocation());
                    diagram.put(newNode);
                    nodeMap.put(node, newNode);
                }
                else if( node.getKernel().getType().equals(Type.ANALYSIS_SCRIPT) )
                {
                    String path = (String) node.getAttributes().getValue(ScriptElement.SCRIPT_PATH);
                    String source = (String) node.getAttributes().getValue(ScriptElement.SCRIPT_SOURCE);
                    String type = (String) node.getAttributes().getValue(ScriptElement.SCRIPT_TYPE);
                    Node newNode = path == null?semanticController.createScriptNode(diagram, source, type):semanticController.createScriptNode(diagram, DataElementPath.create(path));
                    newNode.setLocation(node.getLocation());
                    diagram.put(newNode);
                    nodeMap.put(node, newNode);
                }
                else if( ( (Node)de ).getKernel().getType().equals(Type.ANALYSIS_QUERY) )
                {
                    Node newNode = semanticController.createSQLNode(diagram, (String)node.getAttributes().getValue(SQLElement.SQL_SOURCE), (String)node.getAttributes().getValue(SQLElement.SQL_HOST));
                    newNode.setLocation(node.getLocation());
                    diagram.put(newNode);
                    nodeMap.put(node, newNode);
                }
            }
        }
        for(DiagramElement de : workflow)
        {
            if( ( de instanceof Edge ) && ( ( (Edge)de ).getKernel().getType().equals(Base.TYPE_DIRECTED_LINK) ) )
            {
                Edge edge = (Edge)de;
                Node n1 = getWorkflowElement(edge.getInput());
                Node n2 = getWorkflowElement(edge.getOutput());
                if(nodeMap.get(n1) != null && nodeMap.get(n2) != null)
                {
                    diagram.put(new Edge(diagram, edge.getKernel(), nodeMap.get(n1), nodeMap.get(n2)));
                }
            }
        }
        return diagram;
    }

    /**
     * Start workflow execution
     */
    public void start()
    {
        if( jobControl != null )
        {
            jobControl.functionStarted();
        }
        terminated = false;
        paused = false;
        ((WorkflowDiagramViewOptions)workflow.getViewOptions()).setAutoLayout(false);
        runAvailableAnalysis();
    }

    protected void beforeStop()
    {
        terminated = true;
        if(executionMap != null)
        {
            for(WorkflowElement we: executionMap)
            {
                we.terminate();
            }
        }
        clearStatusProperty();
        completeWorkflow();
    }

    protected void stop(Exception e)
    {
        beforeStop();
        for( WorkflowEngineListener listener : listeners )
        {
            listener.finished();
        }
        if(jobControl != null)
            jobControl.functionTerminatedByError(e);
    }

    /**
     * Stop workflow execution
     */
    public void stop()
    {
        beforeStop();
        if(jobControl != null)
            jobControl.functionFinished();
    }

    /**
     * Pause workflow, stop running analyses
     */
    public void pause()
    {
        paused = true;
        if( executionMap != null )
        {
            for( WorkflowElement we : executionMap )
            {
                we.terminate();
                if( we.isStarted() && !we.isComplete() )
                    we.setStarted( false );
            }
        }
    }

    /**
     * Resume from pause, run with skipCompleted=true
     */
    public void resume()
    {
        setSkipCompleted( true );
        if( executionMap != null )
        {
            for( WorkflowElement we : executionMap )
            {
                if( we instanceof AnalysisElement )
                    ( (AnalysisElement)we ).setSkipCompleted( true );
            }
        }
        paused = false;
        start();
    }

    /**
     * Check if model correct
     */
    protected void checkModel() throws IllegalArgumentException
    {
        WorkflowChecker checker = new WorkflowChecker(workflow);
        if( checker.check() )
            return;
        throw new IllegalArgumentException( "Workflow cannot be executed, because it contains error(s):\n" + checker.getErrors() );
    }

    protected void buildCompartmentMap(ExecutionMap result, Compartment compartment)
    {
        Map<Node, WorkflowElement> nodeMap = new HashMap<>();
        for(DiagramElement de : compartment)
        {
            if( ( de instanceof Node ) && de.getKernel() != null )
            {
                String kernelType = de.getKernel().getType();
                WorkflowItem item = WorkflowItemFactory.getWorkflowItem((Node)de);
                if(item instanceof WorkflowVariable)
                {
                    SimpleNodeElement se;
                    DataElementPath path = null;
                    WorkflowVariable variable = (WorkflowVariable)item;
                    try
                    {
                        path = (DataElementPath)variable.getValue();
                    }
                    catch( Exception e )
                    {
                    }
                    if( path != null )
                    {
                        if(variable.isAutoOpen()) autoOpenPaths.add(path);
                        if(variable.isTemporary()) temporaryPaths.add(path);
                        if(variable instanceof WorkflowParameter && ((WorkflowParameter)variable).getRole().equals("Output"))
                            outputs.add(path);
                    }
                    se = new SimpleNodeElement(getStatusProperty(de));
                    result.addElement(se);
                    nodeMap.put((Node)de, se);
                }
                else if( kernelType.equals(Type.ANALYSIS_METHOD) )
                {
                    AnalysisMethod analysisMethod = AnalysisDPSUtils.getAnalysisMethodByNode( ( (Node)de ).getAttributes());
                    analysisMethod.setLogger(log);
                    AnalysisElement ae = new AnalysisElement(analysisMethod, (Node)de, getStatusProperty(de));
                    ae.setSkipCompleted(skipCompleted);
                    ae.setIgnoreFail( ignoreFail );
                    result.addElement(ae);
                    nodeMap.put((Node)de, ae);
                }
                else if( de instanceof Compartment && kernelType.equals(Type.ANALYSIS_CYCLE) )
                {
                    Compartment workflowCompartment = (Compartment)de;
                    DynamicProperty statusProperty = getStatusProperty(workflowCompartment);
                    WorkflowCycleVariable var = CycleElement.findCycleVariable(workflowCompartment);
                    WorkflowElement we = var != null && var.isParallel()
                            ? new ParallelCycleElement(this, workflowCompartment, statusProperty)
                            : new CycleElement(this, workflowCompartment, statusProperty);
                    we.setIgnoreFail( ignoreFail );
                    result.addElement(we);
                    nodeMap.put((Node)de, we);
                }
                else if( kernelType.equals(Type.TYPE_DATA_ELEMENT) )
                {
                    SimpleNodeElement ne = new SimpleNodeElement(getStatusProperty(de));
                    result.addElement(ne);
                    nodeMap.put((Node)de, ne);
                }
                else if( kernelType.equals(Type.ANALYSIS_SCRIPT) )
                {
                    Node scriptNode = (Node)de;
                    String path = (String)scriptNode.getAttributes().getValue(ScriptElement.SCRIPT_PATH);
                    Collection<WorkflowVariable> vars = WorkflowItemFactory.getVariables((Compartment)scriptNode.getOrigin()).values();

                    List<WorkflowExpression> outputVars = new ArrayList<>();
                    for( Edge edge : scriptNode.getEdges() )
                        if( edge.getInput() == scriptNode )
                        {
                            WorkflowItem outputItem = WorkflowItemFactory.getWorkflowItem(edge.getOutput());
                            if( outputItem instanceof WorkflowExpression )
                                outputVars.add((WorkflowExpression)outputItem);
                        }


                    ScriptDataElement script = null;
                    if( path != null )
                          script = DataElementPath.create( path ).getDataElement( ScriptDataElement.class );
                    else
                    {
                        String scriptType = ( (Node)de ).getAttributes().getValue(ScriptElement.SCRIPT_TYPE).toString();
                        String content = ( (Node)de ).getAttributes().getValue(ScriptElement.SCRIPT_SOURCE).toString();
                        script = ScriptTypeRegistry.createScript( scriptType, null, content );
                    }

                    ScriptElement se = new ScriptElement( script, getStatusProperty(de), log, vars, outputVars );
                    se.setIgnoreFail( ignoreFail );
                    result.addElement(se);
                    nodeMap.put((Node)de, se);
                }
                else if( kernelType.equals(Type.ANALYSIS_QUERY) )
                {
                    String outputPath = null;
                    for( Edge edge : ( (Node)de ).getEdges() )
                    {
                        if( edge.getInput() == de )
                        {
                            Node output = edge.getOutput();
                            Object value = output.getAttributes().getValue(DataCollectionConfigConstants.COMPLETE_NAME_PROPERTY);
                            if( value != null )
                            {
                                outputPath = value.toString();
                                break;
                            }
                        }
                    }

                    String source = (String) ( (Node)de ).getAttributes().getValue(SQLElement.SQL_SOURCE);
                    String host = (String) ( (Node)de ).getAttributes().getValue(SQLElement.SQL_HOST);
                    SQLElement se = new SQLElement(source, host, outputPath, getStatusProperty(de));
                    result.addElement(se);
                    nodeMap.put((Node)de, se);
                }
                else if( kernelType.equals(Type.TYPE_DATA_GENERATOR) )
                {
                    Node node = (Node)de;
                    String script = (String) ( (Node)de ).getAttributes().getValue(ScriptElement.SCRIPT_SOURCE);
                    if( script != null )
                        ( (Node)de ).getAttributes().setValue(ScriptDataGenerator.SCRIPT_PROPERTY, script);
                    DataGeneratorElement dge = new DataGeneratorElement(node.getName(), node.getAttributes(), getStatusProperty(node));
                    nodeMap.put((Node)de, dge);
                }
                else if( kernelType.equals(Type.TYPE_PLOT) )
                {
                    Node node = (Node)de;
                    Edge[] edges = node.getEdges();
                    List<String> xGen = new ArrayList<>();
                    List<String> yGen = new ArrayList<>();
                    for(Edge e: edges)
                    {
                        if(e.getKernel() != null && e.getKernel().getType().equals(Type.TYPE_LISTENER_LINK) && e.getOutput()==node)
                        {
                            if(e.getAttributes().getValue(PlotElement.X_GENERATOR_PROPERTY) != null)
                                xGen.addAll(Arrays.asList((String[])(e.getAttributes().getValue(PlotElement.X_GENERATOR_PROPERTY))));
                            if(e.getAttributes().getValue(PlotElement.Y_GENERATOR_PROPERTY) != null)
                                yGen.addAll(Arrays.asList((String[])(e.getAttributes().getValue(PlotElement.Y_GENERATOR_PROPERTY))));
                        }
                    }
                    boolean autoOpen = false;
                    if(node.getAttributes().getValue(PlotElement.AUTO_OPEN) != null)
                        autoOpen = (Boolean)node.getAttributes().getValue(PlotElement.AUTO_OPEN);
                    DataElementPath plotPath = DataElementPath.create((String)node.getAttributes().getValue(PlotElement.PLOT_PATH));
                    PlotElement plot = new PlotElement(node.getName(), plotPath, autoOpen, null, null, xGen, yGen, getStatusProperty(de));
                    if(autoOpen)
                    {
                        result.addElement(plot);
                        autoOpenPaths.add(plotPath);
                    }
                    nodeMap.put((Node)de, plot);
                }
            }
        }
        for(DiagramElement de : compartment)
        {
            if( ( de instanceof Edge ) && de.getKernel().getType().equals( Base.TYPE_DIRECTED_LINK ) )
            {
                Edge edge = (Edge)de;
                WorkflowElement element1 = getWorkflowElement(edge.getInput(), nodeMap);
                WorkflowElement element2 = getWorkflowElement(edge.getOutput(), nodeMap);
                if( ( element1 != null ) && ( element2 != null ) )
                {
                    element2.addDependence(element1);
                }
            }
            else if( ( de instanceof Edge ) && de.getKernel().getType().equals( Base.TYPE_LISTENER_LINK ) )
            {
                Edge edge = (Edge)de;
                WorkflowElement element1 = getWorkflowElement(edge.getInput(), nodeMap);
                WorkflowElement element2 = getWorkflowElement(edge.getOutput(), nodeMap);
                if( ( element1 != null ) && ( element2 != null ) )
                {
                    element1.addListener(element2);
                }
            }
        }
    }

    /**
     * Build execution map for workflow
     * @throws Exception
     * @todo initialize analysis parameters just before analysis (will be more flexible)
     */
    protected ExecutionMap buildExecutionMap() throws Exception
    {
        ExecutionMap result = new ExecutionMap();
        buildCompartmentMap(result, workflow);
        if(terminated) throw new Exception("Unable to start the workflow");
        return result;
    }
    protected static Node getWorkflowElement(Node node)
    {
        if( node.getKernel().getType().equals(Type.TYPE_DATA_ELEMENT_IN) || node.getKernel().getType().equals(Type.TYPE_DATA_ELEMENT_OUT) )
        {
            return (Node)node.getOrigin();
        }
        return node;
    }

    protected static WorkflowElement getWorkflowElement(Node node, Map<Node, WorkflowElement> nodeMap)
    {
        while(!(node instanceof Diagram))
        {
            WorkflowElement element = nodeMap.get(node);
            if(element != null) return element;
            node = (Node)node.getOrigin();
        }
        return null;
    }

    protected DynamicProperty getStatusProperty(DiagramElement de)
    {
        DynamicProperty result = de.getAttributes().getProperty(NODE_STATUS_PROPERTY);
        if( result == null )
        {
            try
            {
                result = new DynamicProperty(NODE_STATUS_PROPERTY, Integer.class, -1);
                DPSUtils.makeTransient(result);
                de.getAttributes().add(result);
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Cannot create status property for " + de.getName(), e);
            }
        }
        return result;
    }

    protected void scheduleElementExecution(final WorkflowElement element, final JobControlListener listener)
    {
        element.setStarted(true);
        TaskPool.getInstance().submit(new WorkflowElementTask(workflow.getName(), element, listener));
    }

    /**
     * Run available analysis from {@link ExecutionMap}
     */
    protected synchronized void runAvailableAnalysis()
    {
        if( terminated ) return;
        if( paused ) return;
        if( executionMap.isComplete() )
        {
            clearStatusProperty();
            removeTemporaryElements(executionMap);
            saveWorkflowProperties();
            completeWorkflow();
            //fire finished event
            for( WorkflowEngineListener listener : listeners )
            {
                listener.finished();
            }
            if( jobControl != null )
            {
                List<Object> results = new ArrayList<>();
                results.addAll(autoOpenPaths);
                results.addAll(images);
                jobControl.resultsAreReady(results.toArray());
                jobControl.functionFinished();
            }
        }
        else
        {
            WorkflowElement analysis = null;
            while( ( analysis = executionMap.getAvailableElement() ) != null )
            {
                scheduleElementExecution(analysis, this);
            }
            //fire diagram state changed
            for( WorkflowEngineListener listener : listeners )
            {
                listener.stateChanged();
            }
        }
    }

    protected void saveWorkflowProperties()
    {
        String serialized;
        try
        {
            serialized = TextUtil.writeDPSToJSON(parameters);
        }
        catch( Exception e1 )
        {
            log.log(Level.SEVERE, "Unable to serialize workflow parameters ("+workflow.getCompletePath()+")", e1);
            return;
        }
        for(DataElementPath path: outputs)
        {
            DataCollection<?> dc = path.optDataCollection();
            if(dc == null) continue;
            Properties properties = dc.getInfo().getProperties();
            properties.setProperty("workflow_path", workflow.getCompletePath().toString());
            properties.setProperty("workflow_properties", serialized);
            try
            {
                path.save(dc);
            }
            catch( Exception e )
            {
                log.log(Level.INFO, "path.save(dc)", e );
            }
        }
    }

    /**
     * Remove status properties from workflow
     */
    protected void clearStatusProperty()
    {
        clearStatusProperty(workflow, true);
    }

    //JobControl listener implementation
    @Override
    public void valueChanged(JobControlEvent event)
    {
        if(jobControl != null && jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST)
        {
            for( WorkflowEngineListener listener : listeners )
            {
                listener.errorDetected(event.getMessage());
            }
            stop();
            return;
        }
        if(jobControl != null)
            jobControl.setPreparedness(executionMap.getCompletePercent());
        for( WorkflowEngineListener listener : listeners )
        {
            listener.stateChanged();
        }
    }

    @Override
    public void jobStarted(JobControlEvent event)
    {
    }

    @Override
    public void jobTerminated(JobControlEvent event)
    {
        if(terminated) return;
        if( paused ) return;
        if(jobControl != null && jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST)
        {
            for( WorkflowEngineListener listener : listeners )
            {
                listener.errorDetected(event.getMessage());
            }
            stop();
        }
        else if( ( event == null ) || ( event.getStatus() == JobControl.COMPLETED ) )
        {
            //run available analysis if something complete
            runAvailableAnalysis();
            if( jobControl != null )
            {
                jobControl.setPreparedness(executionMap.getCompletePercent());
            }
        }
        else if( ignoreFail && event.getStatus() == JobControl.TERMINATED_BY_ERROR )
        {
            //run available analysis if failed by ignoreFail is set
            runAvailableAnalysis();
            if( jobControl != null )
            {
                jobControl.setPreparedness( executionMap.getCompletePercent() );
            }
        }
        else
        {
            boolean parameterError = (event instanceof AnalysisParametersExceptionEvent);
            for( WorkflowEngineListener listener : listeners )
            {
                if(parameterError)
                    listener.parameterErrorDetected(event.getMessage());
                else
                    listener.errorDetected(event.getMessage());
            }
            stop( parameterError ? new IllegalArgumentException(event.getMessage()) : new Exception(event.getMessage()) );
        }
    }

    @Override
    public void jobPaused(JobControlEvent event)
    {
    }

    @Override
    public void jobResumed(JobControlEvent event)
    {
    }

    @Override
    public void resultsReady(JobControlEvent event)
    {
        if(jobControl != null)
        {
            Object[] results = event.getResults();
            if(results != null && results.length > 0)
            {
                TaskManager taskManager = TaskManager.getInstance();
                TaskInfo taskInfo = taskManager.getTask(jobControl);
                if( taskInfo != null )
                {
                    for( Object result : results )
                    {
                        if( result instanceof BufferedImage )
                        {
                            images.add((BufferedImage)result);
                        }
                    }
                }
            }
        }
        Object[] results = event.getResults();
        for( WorkflowEngineListener listener : listeners )
        {
            listener.resultsReady(results);
        }
    }

    protected void completeWorkflow()
    {
        ((WorkflowDiagramViewOptions)workflow.getViewOptions()).setAutoLayout(true);
    }

    /**
     * Remove status property from given compartment
     * @param compartment
     */
    public static void clearStatusProperty(Compartment compartment, boolean clearTmpElements)
    {
        Deque<Compartment> compartments = new ArrayDeque<>();
        compartments.add(compartment);
        while(!compartments.isEmpty())
        {
            Compartment c = compartments.pollFirst();
            for(DiagramElement de : c)
            {
                DynamicProperty property = de.getAttributes().getProperty(NODE_STATUS_PROPERTY);
                if(property != null) property.setValue(-1);
                if(clearTmpElements)
                {
                    property = de.getAttributes().getProperty(EDGE_TMP_VALUE_PROPERTY);
                    if(property != null)
                    {
                        try
                        {
                            getTmpCollection().getChildPath(property.getValue().toString()).remove();
                        }
                        catch( Exception e )
                        {
                        }
                        de.getAttributes().remove(EDGE_TMP_VALUE_PROPERTY);
                    }
                }
                if(de instanceof Compartment && ((Compartment)de).getKernel().getType().equals(Type.ANALYSIS_CYCLE))
                {
                    compartments.add((Compartment)de);
                }
            }
        }
    }

    /**
     * Much faster than getAnalysisParametersByNode. Use if you don't need actual parameter values
     * @param n node
     * @return
     */
    public static AnalysisParameters getInitialParametersByNode(Node n)
    {
        if(!n.getKernel().getType().equals(Type.ANALYSIS_METHOD)) return null;
        AnalysisMethod method = AnalysisDPSUtils.getAnalysisMethodByNode(n.getAttributes());
        return method == null ? null : method.getParameters();
    }

    public static AnalysisParameters getAnalysisParametersByNode(Node n)
    {
        try
        {
            return getAnalysisParametersByNode(n, true);
        }
        catch( Exception e )
        {
            ExceptionRegistry.log( e );
            return null;
        }
    }

    public static AnalysisParameters getAnalysisParametersByNode(Node n, boolean ignoreErrors) throws Exception
    {
        if(!n.getKernel().getType().equals(Type.ANALYSIS_METHOD)) return null;
        AnalysisParameters parameters = AnalysisDPSUtils.readParametersFromAttributes(n.getAttributes());
        if(parameters == null)
        {
            if(!ignoreErrors)
            {
                throw new IllegalArgumentException( "Cannot get analysis parameters for "+n.getName() );
            }
            return null;
        }
        List<Edge> edges = new ArrayList<>();
        edges.addAll(Arrays.asList(n.getEdges()));
        if(n instanceof Compartment)
        {
            ((Compartment)n).stream( Node.class ).flatMap( Node::edges ).forEach( edges::add );
        }
        ComponentModel model = ComponentFactory.getModel(parameters, Policy.DEFAULT, true);
        Map<String, Object> flatValuesMap = BeanAsMapUtil.flattenMap( BeanAsMapUtil.convertBeanToMap( parameters ) );
        for(Edge edge: edges)
        {
            if( edge.getKernel() != null && edge.getKernel().getType().equals(Type.TYPE_LISTENER_LINK) )
                continue;
            try
            {
                Object edgeVariable = edge.getAttributes().getValue(WorkflowSemanticController.EDGE_VARIABLE);
                Object value = null;
                if(edgeVariable != null)
                {
                    Node varNode = edge.nodes().findFirst( node -> node.getName().equals( edgeVariable.toString() ) )
                            .orElseThrow( () -> new IllegalArgumentException("Invalid variable reference " + edgeVariable) );
                    WorkflowExpression item = (WorkflowExpression)WorkflowItemFactory.getWorkflowItem(varNode);
                    if( item == null )
                    {
                        //quick fix for variable nodes with same name as analysis parameter 
                        varNode = edge.getOtherEnd( varNode );
                        if( varNode.getName().equals( edgeVariable.toString() ) )
                            item = (WorkflowExpression)WorkflowItemFactory.getWorkflowItem( varNode );
                    }
                    try
                    {
                        value = item.getValue();
                    }
                    catch( NullPointerException npe )
                    {
                        throw new IllegalArgumentException(
                                "Error while calculating nodes for edge " + edge.getName() + " which points to variable "
                                        + edgeVariable.toString()
                                        + npe.getMessage() );
                    }
                    catch( Exception e )
                    {
                        if( item != null )
                            throw new IllegalArgumentException( "Error while calculating variable '" + item.getName() + "' (expression: '"
                                    + item.getExpression() + "'): " + e.getMessage() );
                    }
                } else  // Analysis-to-analysis node
                {
                    DynamicProperty property = edge.getAttributes().getProperty(WorkflowEngine.EDGE_TMP_VALUE_PROPERTY);
                    if(property == null)
                    {
                        property = new DynamicProperty(BeanUtil.createDescriptor(WorkflowEngine.EDGE_TMP_VALUE_PROPERTY), String.class,
                                UUID.randomUUID().toString());
                        DPSUtils.makeTransient(property);
                        edge.getAttributes().add(property);
                    }
                    value = getTmpCollection().getChildPath(property.getValue().toString());
                }
                Object edgeAnalysisPropertyObj = edge.getAttributes().getValue(WorkflowSemanticController.EDGE_ANALYSIS_PROPERTY);
                if(edgeAnalysisPropertyObj == null)
                {
                    if(getWorkflowElement(edge.getInput()).equals(n))
                        edgeAnalysisPropertyObj = edge.getAttributes().getValue(WorkflowSemanticController.EDGE_ANALYSIS_INPUT_PROPERTY);
                    else
                        edgeAnalysisPropertyObj = edge.getAttributes().getValue(WorkflowSemanticController.EDGE_ANALYSIS_OUTPUT_PROPERTY);
                }
                if(edgeAnalysisPropertyObj == null)
                    continue;
                String edgeAnalysisProperty = edgeAnalysisPropertyObj.toString();
                Property property = model.findProperty(edgeAnalysisProperty);

                if(property == null)
                    throw new Exception("Can not find property: " + edgeAnalysisProperty + " in " + parameters.getClass());
                if(property.getValueClass() == null)
                    throw new Exception("Can not get class for " + edgeAnalysisProperty);
                if(value == null)
                    throw new Exception("Can not fetch value for " + edgeAnalysisProperty);

                if( value instanceof DataElementPath && DataElementPathSet.class.isAssignableFrom( property.getValueClass() ) )
                {
                    if(!(flatValuesMap.get( edgeAnalysisProperty ) instanceof DataElementPathSet))
                        flatValuesMap.remove( edgeAnalysisProperty );

                    DataElementPathSet pathSet = (DataElementPathSet)flatValuesMap.get( edgeAnalysisProperty );
                    if( pathSet == null )
                        flatValuesMap.put( edgeAnalysisProperty, pathSet = new DataElementPathSet() );
                    pathSet.add( (DataElementPath)value );
                }
                else
                    flatValuesMap.put( edgeAnalysisProperty, value );
            }
            catch( Exception e )
            {
                if(!ignoreErrors) throw e;
            }
        }

        Map<String, Object> valuesMap = BeanAsMapUtil.expandMap( flatValuesMap );
        AnalysisParameters newParameters = AnalysisDPSUtils.getAnalysisMethodByNode( n.getAttributes() ).getParameters();
        BeanAsMapUtil.readBeanFromHierarchicalMap( valuesMap, newParameters );
        return newParameters;
    }

    private static DataElementPath getTmpCollection()
    {
        DataElementPath projectPath = JournalRegistry.getProjectPath();
        if(projectPath == null)
            return tmpCollectionPath;
        return projectPath.getRelativePath("tmp/");
    }

    private static DataElementPath tmpCollectionPath;
    /**
     * For tests only.
     */
    public static void setTmpCollection(DataElementPath path)
    {
        tmpCollectionPath = path;
    }

    protected void removeTemporaryElements(ExecutionMap executionMap)
    {
        for(DataElementPath path: temporaryPaths)
        {
            try
            {
                path.remove();
            }
            catch( Exception e )
            {
            }
        }
        temporaryPaths.clear();
    }

    public Collection<ru.biosoft.access.core.DataElementPath> getAutoOpenPaths()
    {
        return autoOpenPaths;
    }

    /**
     * Returns a bean to setup the script element
     * @param node representing the script element
     * @return either TextScriptParameters or LinkScriptParameters object
     */
    public static Object getScriptParameters(Node node)
    {
        String path = (String) ( node ).getAttributes().getValue(ScriptElement.SCRIPT_PATH);
        return path == null?new TextScriptParameters(node):new LinkScriptParameters(node);
    }

    /**
     * Returns a bean to setup the plot element
     * @param node representing the script element
     * @return PlotParameters object
     */
    public static Object getPlotParameters(Node node)
    {
        return new PlotElementParameters(node);
    }

    /**
     * Returns a bean to setup the data generator element
     * @param node representing the data generator node
     */
    public static Object getDataGeneratorParameters(Node node)
    {
        //TODO: more complicated parameters
        String script = (String)node.getAttributes().getValue(ScriptDataGenerator.SCRIPT_PROPERTY);
        if( node.getAttributes().getProperty(ScriptElement.SCRIPT_SOURCE) == null )
            node.getAttributes().add(new DynamicProperty(ScriptElement.SCRIPT_SOURCE, String.class, script));
        else
            node.getAttributes().setValue(ScriptElement.SCRIPT_SOURCE, script);

        return getScriptParameters(node);
    }

}
