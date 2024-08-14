package biouml.plugins.sedml;

import java.util.Map;

import org.jlibsedml.AbstractTask;
import org.jlibsedml.RepeatedTask;
import org.jlibsedml.SedML;
import org.jlibsedml.Task;

import biouml.model.Compartment;
import biouml.model.Node;
import biouml.plugins.research.workflow.WorkflowSemanticController;

public class TaskBuilder extends WorkflowBuilder
{
    private SedML sedml;
    private Node modelNode;
    private String outputFolder;
    private AbstractTask task;
    
    private Node outputNode;
    private Map<String, Node> modelNodes;
    
    public TaskBuilder(Compartment parent, WorkflowSemanticController controller)
    {
        super( parent, controller );
    }
    
    public Node getOutputNode()
    {
        return outputNode;
    }

    public void setSedml(SedML sedml)
    {
        this.sedml = sedml;
    }
    
    public void setModelNode(Node modelNode)
    {
        this.modelNode = modelNode;
    }

    public void setTask(AbstractTask task)
    {
        this.task = task;
    }

    public void setOutputFolder(String outputFolder)
    {
        this.outputFolder = outputFolder;
    }
    
    public void setModelNodes(Map<String, Node> modelNodes)
    {
        this.modelNodes = modelNodes;
    }

    @Override
    public void build()
    {
        if(task instanceof RepeatedTask)
            buildRepeatedTask( (RepeatedTask)task );
        else if(task instanceof Task)
            buildSimulationTask( (Task)task );
        else
            throw new IllegalArgumentException("Unknown task type " + task.getClass().getName());
    }
    
    private void buildRepeatedTask(RepeatedTask task)
    {
        RepeatedTaskBuilder builder = new RepeatedTaskBuilder( parent, controller );
        builder.setSedml( sedml );
        builder.setTask( task );
        builder.setInputModelNode( modelNode );
        builder.setSimulationResultPath( outputFolder );
        builder.setIntermediateOutputPath( outputFolder + " intermediate" );
        builder.setModelNodes( modelNodes );
        builder.build();
        outputNode = builder.getSimulationResultNode();
    }
    
    private void buildSimulationTask(Task task)
    {
        SimulationTaskBuilder stBuilder = new SimulationTaskBuilder( parent, controller );
        stBuilder.setInputNode( modelNode );
        stBuilder.setTask( task );
        stBuilder.setSedml( sedml );
        stBuilder.setOutputFolder( outputFolder );
        stBuilder.build();
        outputNode = stBuilder.getOutputNode();
    }
}
