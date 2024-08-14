package biouml.plugins.sedml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jlibsedml.AbstractTask;
import org.jlibsedml.RepeatedTask;
import org.jlibsedml.SedML;
import org.jlibsedml.SetValue;
import org.jlibsedml.Simulation;
import org.jlibsedml.SteadyState;
import org.jlibsedml.SubTask;
import org.jmathml.FormulaFormatter;

import biouml.model.Compartment;
import biouml.model.Node;
import biouml.plugins.research.workflow.WorkflowSemanticController;
import biouml.plugins.research.workflow.items.VariableType;
import biouml.plugins.sedml.analyses.MergeSimulationResults;
import biouml.plugins.sedml.analyses.MergeSimulationResults.Parameters;
import biouml.plugins.sedml.analyses.MergeSimulationResults.SimulationResultReference;
import biouml.plugins.state.analyses.ChangeDiagram;
import biouml.plugins.state.analyses.ChangeDiagramParameters;
import biouml.plugins.state.analyses.StateChange;
import biouml.plugins.sedml.analyses.SetInitialValuesFromSimulationResult;
import ru.biosoft.analysiscore.AnalysisMethodRegistry;

public class CycleBuilder extends WorkflowBuilder
{
    private RepeatedTask task;
    private Node inputModelNode;
    private Node modelAfterIterationNode;
    private Node simulationResultNode;
    private String outputPath;
    private SedML sedml;
    private Map<String, Node> modelNodes;

    public CycleBuilder(Compartment parent, WorkflowSemanticController controller)
    {
        super( parent, controller );
    }

    public void setTask(RepeatedTask task)
    {
        this.task = task;
    }

    public void setInputModelNode(Node inputModelNode)
    {
        this.inputModelNode = inputModelNode;
    }

    public void setModelAfterIterationNode(Node modelAfterIteration)
    {
        this.modelAfterIterationNode = modelAfterIteration;
    }

    public void setSimulationResultNode(Node simulationResultNode)
    {
        this.simulationResultNode = simulationResultNode;
    }

    public void setOutputPath(String outputPath)
    {
        this.outputPath = outputPath;
    }

    public void setSedml(SedML sedml)
    {
        this.sedml = sedml;
    }

    public void setModelNodes(Map<String, Node> modelNodes)
    {
        this.modelNodes = modelNodes;
    }

    @Override
    public void build()
    {
        buildRanges();

        Node modelNode = inputModelNode;
        if(!task.getChanges().isEmpty())
            modelNode = changeDiagram(modelNode, task.getChanges());

        List<SubTask> subTasks = getSubTasks();
        if(subTasks.isEmpty())
            return;

        List<Node> resultsToMerge = new ArrayList<>();

        boolean modelNodeIsExpression = task.getChanges().isEmpty();
        AbstractTask firstSubTask = sedml.getTaskWithId( subTasks.get( 0 ).getTaskId() );
        boolean needModelNodeAsExpression = subTasks.size() > 1 || !task.getResetModel() || firstSubTask instanceof RepeatedTask;
        if( !modelNodeIsExpression && needModelNodeAsExpression )
        {
            String newModelTitle = "Model before " + subTasks.get( 0 ).getTaskId();
            Node newModelNode = addDataElementNode( newModelTitle, outputPath + "/" + newModelTitle );
            addDirectedEdge( parent, modelNode, newModelNode );
            modelNode = newModelNode;
        }

        Node node = buildSubTask( modelNode, subTasks.get( 0 ) );
        resultsToMerge.add( node );

        for( int i = 1; i < subTasks.size(); i++ )
        {
            modelNode = setInitialValuesFromSimulationResult( node, modelNode );

            String newModelTitle = "Model before " + subTasks.get( i ).getTaskId();
            Node newModelNode = addDataElementNode( newModelTitle, outputPath + "/" + newModelTitle );
            addDirectedEdge( parent, modelNode, newModelNode );
            modelNode = newModelNode;

            node = buildSubTask( modelNode, subTasks.get( i ) );
            resultsToMerge.add( node );
        }

        if(!task.getResetModel())
        {
            modelNode = setInitialValuesFromSimulationResult(node, modelNode);
            addDirectedEdge( parent, modelNode, modelAfterIterationNode );
        }

        if(resultsToMerge.size() > 1)
            node = mergeSimulationResults(resultsToMerge, Parameters.MERGE_BY_TIME, false);
        else
            node = resultsToMerge.get( 0 );

        String mergeType = getMergeType( subTasks );

        node = mergeSimulationResults(Collections.singletonList( node ), mergeType, true);
        addDirectedEdge( parent, node, simulationResultNode );
    }

    private String getMergeType(List<SubTask> subTasks)
    {
        return isMergeByTime( task, subTasks, sedml ) ? Parameters.MERGE_BY_TIME : Parameters.MERGE_BY_VARS;
    }

    public static boolean isMergeByTime(RepeatedTask task, List<SubTask> subTasks, SedML sedml)
    {
        String lastTaskId = subTasks.get( subTasks.size() - 1 ).getTaskId();
        String simulationIdOfLastTask = sedml.getTaskWithId( lastTaskId ).getSimulationReference();
        Simulation lastSimulation = sedml.getSimulation( simulationIdOfLastTask );
        return lastSimulation instanceof SteadyState || !task.getResetModel();
    }

    public void buildRanges()
    {
        RangesBuilder builder = new RangesBuilder( parent, controller );
        builder.setRanges( task.getRanges() );
        builder.setMainRange( task.getRange(task.getRange()) );
        builder.setModelNodes( modelNodes );
        builder.build();
    }

    private List<SubTask> getSubTasks()
    {
        return SedmlUtils.getSubTasks( task );
    }

    private Node changeDiagram(Node modelNode, List<SetValue> changes)
    {
        ChangeDiagram analysis = AnalysisMethodRegistry.getAnalysisMethod( ChangeDiagram.class );
        ChangeDiagramParameters parameters = analysis.getParameters();

        StateChange[] stateChanges = new StateChange[changes.size()];
        for( int i = 0; i < changes.size(); i++ )
        {
            SetValue change = changes.get( i );
            StateChange stateChange = new StateChange();
            String varName = SedmlUtils.getIdFromXPath( change.getTargetXPath().getTargetAsString() );
            stateChange.setElementId( "" );//change diagram itself
            stateChange.setElementProperty( "role/vars/" + varName + "/initialValue" );
            stateChanges[i] = stateChange;
        }
        parameters.setChanges( stateChanges );

        Compartment analysisNode = addAnalysis( analysis );

        for( int i = 0; i < changes.size(); i++ )
        {
            SetValue change = changes.get( i );
            //change.getModelReference()  should always reference modelNode (model of this repeatedTask)

            Node nodeToBind;
            FormulaFormatter formulaFormatter = new FormulaFormatter();
            String expression = formulaFormatter.formulaToString( change.getMath() );
            if( expression.equals( change.getRangeReference() ) || expression.equals( "(" + change.getRangeReference() + ")" ) )
            {
                nodeToBind = (Node)parent.get( change.getRangeReference() );
            }
            else
            {
                //TODO: create node that will compute expression from change.getListOfParameters(), change.getListOfVariables(), change.getRangeReference()
                nodeToBind = addDataElementNode( "change" + ( i + 1 ), expression );
            }

            controller.bindParameter( nodeToBind, analysisNode, "changes/[" + i + "]/propertyValue", true );
        }

        addDirectedEdge( parent, modelNode, (Node)analysisNode.get( "diagramPath" ) );
        return (Node)analysisNode.get( "outputDiagram" );
    }

    private Node setInitialValuesFromSimulationResult(Node simulationResultNode, Node modelNode)
    {
        SetInitialValuesFromSimulationResult analysis = AnalysisMethodRegistry.getAnalysisMethod( SetInitialValuesFromSimulationResult.class );
        Compartment node = addAnalysis( analysis );
        addDirectedEdge( parent, modelNode, (Node)node.get("inputDiagram") );
        addDirectedEdge( parent, simulationResultNode, (Node)node.get("simulationResult") );
        return (Node)node.get("outputDiagram");
    }

    private Node mergeSimulationResults(List<Node> resultsToMerge, String mergeType, boolean toExistinOutput)
    {
        MergeSimulationResults analysis = AnalysisMethodRegistry.getAnalysisMethod( MergeSimulationResults.class );

        Parameters parameters = analysis.getParameters();
        parameters.setMergeType( mergeType );
        parameters.setMergeToExistingOutput( toExistinOutput );
        SimulationResultReference[] inputs = new SimulationResultReference[resultsToMerge.size()];
        for(int i = 0; i < inputs.length; i++)
            inputs[i] = new SimulationResultReference();
        parameters.setInputs( inputs );

        Compartment analysisNode = addAnalysis( analysis );
        for(int i = 0; i < resultsToMerge.size(); i++)
        {
            Node input = resultsToMerge.get( i );
            Node output = (Node)analysisNode.get( "inputs:[" + i + "]:path" );
            addDirectedEdge( parent, input, output );
        }

        if( Parameters.MERGE_BY_VARS.equals( mergeType ) )
        {
            Node prefixNode = addWorkflowExpression( "prefix", task.getRange() + "=$" + task.getRange() + "$/", VariableType.getType( String.class ) );
            for(int i = 0; i < resultsToMerge.size(); i++)
                controller.bindParameter( prefixNode, analysisNode, "inputs/[" + i + "]/namePrefix", true );
        }

        return (Node)analysisNode.get( "outputPath" );
    }

    private Node buildSubTask(Node modelNode, SubTask subTask)
    {
        TaskBuilder builder = new TaskBuilder( parent, controller );
        builder.setModelNode( modelNode );
        builder.setOutputFolder( outputPath );
        builder.setSedml( sedml );
        builder.setTask( sedml.getTaskWithId( subTask.getTaskId() ) );
        builder.setModelNodes( modelNodes );
        builder.build();
        return builder.getOutputNode();
    }

}
