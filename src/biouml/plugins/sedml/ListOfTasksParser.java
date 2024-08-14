package biouml.plugins.sedml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import one.util.streamex.StreamEx;

import org.jlibsedml.Algorithm;
import org.jlibsedml.FunctionalRange;
import org.jlibsedml.Libsedml;
import org.jlibsedml.OneStep;
import org.jlibsedml.Range;
import org.jlibsedml.RepeatedTask;
import org.jlibsedml.SedML;
import org.jlibsedml.SetValue;
import org.jlibsedml.Simulation;
import org.jlibsedml.SteadyState;
import org.jlibsedml.SubTask;
import org.jlibsedml.Task;
import org.jlibsedml.UniformRange;
import org.jlibsedml.UniformTimeCourse;
import org.jlibsedml.VectorRange;
import org.jlibsedml.XPathTarget;
import org.jmathml.ASTNode;

import ru.biosoft.analysis.CopyDataElement;
import ru.biosoft.analysiscore.AnalysisDPSUtils;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.plugins.modelreduction.AlgebraicSteadyStateAnalysis;
import biouml.plugins.research.workflow.WorkflowSemanticController;
import biouml.plugins.research.workflow.engine.CycleElement;
import biouml.plugins.research.workflow.engine.ScriptElement;
import biouml.plugins.research.workflow.items.CycleType;
import biouml.plugins.research.workflow.items.EnumCycleType;
import biouml.plugins.research.workflow.items.RangeCycleType;
import biouml.plugins.research.workflow.items.WorkflowCycleVariable;
import biouml.plugins.sedml.analyses.MergeSimulationResults;
import biouml.plugins.sedml.analyses.SetInitialValuesFromSimulationResult;
import biouml.plugins.simulation.SimulationAnalysis;
import biouml.plugins.simulation.SimulationAnalysisParameters;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.state.analyses.ChangeDiagram;
import biouml.plugins.state.analyses.ChangeDiagramParameters;
import biouml.plugins.state.analyses.StateChange;
import biouml.plugins.stochastic.StochasticSimulationEngine;
import biouml.standard.type.Type;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;

public class ListOfTasksParser extends WorkflowParser
{
    private Map<String, Node> modelNodes;
    
    public ListOfTasksParser(Diagram workflow, SedML sedml)
    {
        super( workflow, sedml );
    }
    
    public void setModelNodes(Map<String, Node> modelNodes)
    {
        this.modelNodes = modelNodes;
    }

    @Override
    public void parse() throws Exception
    {
        for(Map.Entry<String, Node> entry : modelNodes.entrySet())
        {
            String modelId = entry.getKey();
            Node modelNode = entry.getValue();
            StreamEx.of( modelNode.getEdges() )
                .filter( e->e.getInput()==modelNode ).map( e->e.getOutput() )
                .filter( n->n.getKernel().getType().equals( Type.TYPE_DATA_ELEMENT_IN ) )
                .map( Node::getOrigin ).select( Compartment.class )
                .forEach( analysisNode->{
                    Compartment parent = (Compartment)analysisNode.getOrigin();
                    if(modelNode.getOrigin() == parent && parent == workflow)
                    {
                        if(isAnalysisNode( analysisNode, SimulationAnalysis.class ))
                            parseSimulationTask(analysisNode, modelId);
                        else if(isAnalysisNode(analysisNode, AlgebraicSteadyStateAnalysis.class))
                            parseSteadyStateTask(analysisNode, modelId);
                        else if(isAnalysisNode( analysisNode, CopyDataElement.class ))
                            parseRepeatedTaskResetFalse( analysisNode, modelId );
                    }
                    else
                    {
                        parseRepeatedTaskResetTrue(analysisNode, modelId);
                    }
                } );
        }
    }

    private String parseRepeatedTaskResetTrue(Compartment firstAnalysisNode, String modelId)
    {
        Compartment cycleNode = (Compartment)firstAnalysisNode.getOrigin();
        
        List<Compartment> mergeNodes = findAnalyses( MergeSimulationResults.class, cycleNode, false );
        if( mergeNodes.size() != 1 )
            return null;
        Compartment mergeNode = mergeNodes.get( 0 );
        Node outputPort = (Node)mergeNode.get( "outputPath" );
        if( outputPort.getEdges().length != 1 )
            return null;
        Edge edge = outputPort.getEdges()[0];
        if( edge.getInput() != outputPort )
            return null;
        String id, name;
        if( isWorkflowVariable( edge.getOutput() ) )
        {
            String title = edge.getOutput().getName();
            IdName res = parseTitle( title );
            id = res.id;
            name = res.name;
        }
        else
        {
            id = "task_" + ( ++lastTaskId );
            name = null;
        }

        List<Range> ranges = parseRanges( cycleNode );
        if(ranges.isEmpty())
            return null;
        
        RepeatedTask task = new RepeatedTask( id, name, true, ranges.get( 0 ).getId() );

        for(Range r : ranges)
            task.addRange( r );
        
        if( isAnalysisNode( firstAnalysisNode, ChangeDiagram.class ) )
        {
            for(SetValue change : parseChanges( firstAnalysisNode, modelId ))
                task.addChange( change );
            outputPort = (Node)firstAnalysisNode.get( "outputDiagram" );
            if(outputPort.getEdges().length != 1)
                return null;
            edge = outputPort.getEdges()[0];
            if(edge.getInput() != outputPort)
                return null;
            Node outputNode = isWorkflowVariable( edge.getOutput() ) ? edge.getOutput() : outputPort;
            firstAnalysisNode = StreamEx.of( outputNode.getEdges() )
                .filter( e->e.getInput() == outputNode ).map( Edge::getOutput )
                .filter( n->n.getKernel().getType().equals( Type.TYPE_DATA_ELEMENT_IN ) )
                .map( n->(Compartment)n.getOrigin() )
                .remove( n->isAnalysisNode( n, SetInitialValuesFromSimulationResult.class ) )
                .findAny().orElse( null );
            if(firstAnalysisNode == null)
                return null;
        }

        String taskId;
        if( isAnalysisNode( firstAnalysisNode, SimulationAnalysis.class ) )
        {
            taskId = parseSimulationTask( firstAnalysisNode, modelId );
        }
        else if( isAnalysisNode( firstAnalysisNode, AlgebraicSteadyStateAnalysis.class ) )
        {
            taskId = parseSteadyStateTask( firstAnalysisNode, modelId );
        }
        else if( isAnalysisNode( firstAnalysisNode, CopyDataElement.class ) )
        {
            taskId = parseRepeatedTaskResetFalse( firstAnalysisNode, modelId );
        }
        else if( isAnalysisNode( firstAnalysisNode, ChangeDiagram.class ) )
        {
            taskId = parseRepeatedTaskResetTrue(firstAnalysisNode, modelId);
        }
        else
            return null;
        if(taskId == null)
            return null;
        
        task.addSubtask( new SubTask( taskId ) );//TODO: multiple subtasks
        sedml.addTask( task );
        //TODO: parse parent cycle if model was passed directly to inner cycle
        return id;
    }

    private String parseRepeatedTaskResetFalse(Compartment copyAnalysis, String modelReference)
    {
        Node port = (Node)copyAnalysis.get( "dst" );
        if(port.getEdges().length != 1)
            return null;
        Edge edge = port.getEdges()[0];
        if(edge.getInput() != port)
            return null;
        Node modelBeforeIteration = edge.getOutput();
        if(!modelBeforeIteration.getKernel().getType().equals( Type.ANALYSIS_EXPRESSION ))
            return null;
        Optional<Node> firstAnalysisPort = StreamEx.of( modelBeforeIteration.getEdges() )
                .filter( e->e.getInput() == modelBeforeIteration ).map( e->e.getOutput() ).findAny();
        if(!firstAnalysisPort.isPresent())
            return null;
        Compartment firstAnalysis = (Compartment)firstAnalysisPort.get().getOrigin();
        String id = parseRepeatedTaskResetTrue( firstAnalysis, modelReference );
        if(id == null)
            return null;
        RepeatedTask task = (RepeatedTask)sedml.getTaskWithId( id );
        task.setResetModel( false );
        return id;
    }

    private String parseSteadyStateTask(Compartment analysisNode, String modelReference)
    {
        Node outputNode = (Node)analysisNode.get( "outputSimulationResult" );
        if(outputNode.getEdges().length == 1)
        {
            Edge edge = outputNode.getEdges()[0];
            if(edge.getInput() != outputNode)
                return null;
            if(isWorkflowVariable( edge.getOutput() ))
                outputNode = edge.getOutput();
        }
        String id,name;
        if(isWorkflowVariable( outputNode ))
        {
            IdName res = parseTitle( outputNode.getName() );
            id = res.id;
            name = res.name;
        }
        else
        {
            id = "task_" + (++lastTaskId);
            name = null;
        }
        String simulationId = parseSteadyStateSimulation(analysisNode);
        Task task = new Task( id, name, modelReference, simulationId );
        sedml.addTask( task );
        return id;
    }

    private String parseSteadyStateSimulation(Compartment analysisNode)
    {
        Algorithm algorithm = new Algorithm( "KISAO:0000282" );
        Optional<SteadyState> sameSimulation = StreamEx
                .of( sedml.getSimulations() )
                .select( SteadyState.class )
                .findAny( s -> s.getAlgorithm().equals( algorithm ) );
        if(sameSimulation.isPresent())
        {
            return sameSimulation.get().getId();
        }
        else
        {
            String id = "simulation_" + (++lastSimuilationId);
            String name = null;
            Simulation simulation = new SteadyState( id, name, algorithm );
            sedml.addSimulation( simulation  );
            return id;
        }
    }

    private int lastTaskId = 0;
    private String parseSimulationTask(Compartment analysisNode, String modelReference)
    {
        Node outputNode = (Node)analysisNode.get( "simulationResultPath" );
        if(outputNode.getEdges().length == 1)
        {
            Edge edge = outputNode.getEdges()[0];
            if(edge.getInput() != outputNode)
                return null;
            if(isWorkflowVariable( edge.getOutput() ))
                outputNode = edge.getOutput();
        }
        String id,name;
        if(isWorkflowVariable( outputNode ))
        {
            IdName res = parseTitle( outputNode.getName() );
            id = res.id;
            name = res.name;
        }
        else
        {
            id = "task_" + (++lastTaskId);
            name = null;
        }
        String simulationId = parseUniformSimulation(analysisNode);
        Task task = new Task( id, name, modelReference, simulationId );
        sedml.addTask( task );
        return id;
    }

    private int lastSimuilationId = 0;
    private String parseUniformSimulation(Compartment analysisNode)
    {
        SimulationAnalysisParameters parameters = (SimulationAnalysisParameters)AnalysisDPSUtils.readParametersFromAttributes( analysisNode.getAttributes() );
        SimulationEngine engine = parameters.getSimulationEngine();
        if(engine.getInitialTime() == 0 && engine.getCompletionTime() == engine.getTimeIncrement() && parameters.getSkipPoints() == 1)
        {
            return parseOneStep(parameters);
        }
        
        //TODO: support span
        double initialTime = engine.getInitialTime();
        double outputStartTime = parameters.getOutputStartTime();
        double outputEndTime = engine.getCompletionTime();
        int numberOfPoints = (int)Math.ceil((outputEndTime - outputStartTime) / engine.getTimeIncrement());
        //TODO: support KISAO
        Algorithm algorithm = engine instanceof StochasticSimulationEngine ? new Algorithm( "KISAO:0000241" ) : new Algorithm( "KISAO:0000019" );
        
        Optional<UniformTimeCourse> sameSimulation = StreamEx
                .of( sedml.getSimulations() )
                .select( UniformTimeCourse.class )
                .findAny( s -> s.getInitialTime() == initialTime && s.getOutputStartTime() == outputStartTime
                                && s.getOutputEndTime() == outputEndTime && s.getNumberOfPoints() == numberOfPoints
                                && s.getAlgorithm().equals( algorithm ) );
        if(sameSimulation.isPresent())
        {
            return sameSimulation.get().getId();
        }
        else
        {
            String id = "simulation_" + (++lastSimuilationId);
            String name = null;
            UniformTimeCourse simulation = new UniformTimeCourse( id, name, initialTime, outputStartTime, outputEndTime , numberOfPoints , algorithm );
            sedml.addSimulation( simulation );
            return id;
        }
    }
    
    private String parseOneStep(SimulationAnalysisParameters parameters)
    {
        //TODO: support KISAO
        Algorithm algorithm = new Algorithm( "KISAO:0000019" );
        double step = parameters.getSimulationEngine().getCompletionTime();
        Optional<OneStep> sameSimulation = StreamEx.of( sedml.getSimulations() ).select( OneStep.class ).findAny( s->s.getStep() == step );
        if(sameSimulation.isPresent())
            return sameSimulation.get().getId();
        String id = "simulation_" + (++lastSimuilationId);
        String name = null;
        OneStep oneStep = new OneStep( id, name, algorithm, step );
        sedml.addSimulation( oneStep );
        return id;
    }

    private List<SetValue> parseChanges(Compartment changeDiagramNode, String modelReference)
    {
        List<SetValue> sedmlChanges = new ArrayList<>();
        ChangeDiagramParameters changeDiagramParameters = (ChangeDiagramParameters)AnalysisDPSUtils
                .readParametersFromAttributes( changeDiagramNode.getAttributes() );
        StateChange[] changes = changeDiagramParameters.getChanges();
        for( int i = 0; i < changes.length; i++ )
        {
            StateChange stateChange = changes[i];
            String propertyName = "changes/[" + i + "]/propertyValue";
            Node rangeNode = changeDiagramNode.edges()
                .filter( e->e.getOutput() == changeDiagramNode )
                .filter( e->propertyName.equals( e.getAttributes().getValueAsString( WorkflowSemanticController.EDGE_ANALYSIS_PROPERTY ) ) )
                .map( Edge::getInput ).filter( n->isWorkflowExpression( n ) || isCycleVariable( n ) )
                .findAny().orElse( null );
            if(rangeNode == null)
                continue;
            
            Matcher matcher = ListOfModelsParser.INITIAL_VALUE_PATTERN.matcher( stateChange.getElementProperty() );
            if( matcher.matches() && stateChange.getElementId().isEmpty() )
            {
                String varName = matcher.group( 1 );
                XPathTarget xPath = new XPathTarget( "/sbml:sbml/sbml:model/sbml:listOfParameters/sbml:parameter[@id=\"" + varName+ "\"]" );
                String rangeId = rangeNode.getName();
                ASTNode math = Libsedml.parseFormulaString( rangeId );//more complex math is represented in functional ranges
                SetValue change = new SetValue( xPath, math , rangeId, modelReference );
                sedmlChanges.add( change );
            }
        }
        return sedmlChanges;
    }

    private List<Range> parseRanges(Compartment cycle)
    {
        List<Range> result = new ArrayList<>();
        WorkflowCycleVariable mainVariable = CycleElement.findCycleVariable( cycle );
        Range mainRange;
        CycleType cycleType = mainVariable.getCycleType();
        String expression = mainVariable.getExpression();
        if(cycleType instanceof EnumCycleType)
        {
            int n = cycleType.getCount( expression );
            List<Double> values = new ArrayList<>( n );
            for( int i  = 0; i < n; i++)
            {
                String valueStr = cycleType.getValue( expression, i );
                values.add( Double.valueOf( valueStr ) );
            }
            mainRange = new VectorRange( mainVariable.getName(), values );
        }
        else if(cycleType instanceof RangeCycleType)
        {
            biouml.plugins.research.workflow.items.Range r = new biouml.plugins.research.workflow.items.Range(expression);
            int numberOfPoints = r.getCount() - 1; //according to sedml spec, it will produce one more point
            mainRange = new UniformRange( mainVariable.getName(), r.getFirst().doubleValue(), r.getLast().doubleValue(), numberOfPoints );
        }
        else
            return result;
        result.add( mainRange );
        for( Node node : cycle.getNodes() )
            if( node.getKernel().getType().equals( Type.ANALYSIS_EXPRESSION ) )
            {
                Node scriptNode = null;
                for( Edge e : node.getEdges() )
                    if( e.getOutput() == node && e.getInput().getKernel().getType().equals( Type.ANALYSIS_SCRIPT ) )
                        scriptNode = e.getInput();
                if( scriptNode == null )
                    continue;
                String scriptType = scriptNode.getAttributes().getValueAsString( ScriptElement.SCRIPT_TYPE );
                if(scriptType == null)
                    continue;
                String scriptSource = scriptNode.getAttributes().getValueAsString( ScriptElement.SCRIPT_SOURCE );
                if( scriptType.equals( "math" ) )
                {
                    FunctionalRange fRange = new FunctionalRange( node.getName(), mainRange.getId() );
                    fRange.setMath( MathMLUtils.convertExpressionToMathML( scriptSource ) );
                    //TODO: functional range variables and parameters
                    result.add( fRange );
                }
                else if( scriptType.equalsIgnoreCase( "js" ))
                {
                    String pat = "[$] *\\[ *['\"]" + Pattern.quote( node.getName() ) + "['\"] *\\] *= *(\\[.*\\]) *\\[ *" + Pattern.quote( mainVariable.getName() ) + " *\\] *";
                    Matcher matcher = Pattern.compile( pat ).matcher( scriptSource );
                    if(!matcher.matches())
                        continue;
                    String jsonValues = matcher.group( 1 );
                    List<Double> values;
                    try
                    {
                        JsonArray json = JsonArray.readFrom( jsonValues );
                        values = StreamEx.of( json.values() ).map( JsonValue::asDouble ).toList();
                    }
                    catch(ParseException | UnsupportedOperationException e)
                    {
                        continue;
                    }
                    VectorRange vRange = new VectorRange( node.getName(), values );
                    result.add( vRange );
                }
                else
                    continue;
                    
            }
        return result;
    }
}
