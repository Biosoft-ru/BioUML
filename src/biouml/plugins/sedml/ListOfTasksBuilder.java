package biouml.plugins.sedml;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import one.util.streamex.StreamEx;

import org.jlibsedml.AbstractTask;
import org.jlibsedml.RepeatedTask;
import org.jlibsedml.SedML;
import biouml.model.Compartment;
import biouml.model.Node;
import biouml.plugins.research.workflow.WorkflowSemanticController;

public class ListOfTasksBuilder extends WorkflowBuilder
{
    private SedML sedml;
    private Map<String, Node> modelNodes;
    private String outputFolder;

    private Map<String, Node> simulationResultNodes = new HashMap<>();

    public ListOfTasksBuilder(Compartment parent, WorkflowSemanticController controller)
    {
        super( parent, controller );
    }

    public Map<String, Node> getSimulationResultNodes()
    {
        return simulationResultNodes;
    }

    public void setSedml(SedML sedml)
    {
        this.sedml = sedml;
    }

    public void setModelNodes(Map<String, Node> modelNodes)
    {
        this.modelNodes = modelNodes;
    }
    
    public void setOutputFolder(String outputFolder)
    {
        this.outputFolder = outputFolder;
    }

    @Override
    public void build()
    {
        Set<String> subTasks = StreamEx.of(sedml.getTasks())
            .select( RepeatedTask.class )
            .flatMap( x -> x.getSubTasks().keySet().stream() )
            .toSet();

        for( AbstractTask task : sedml.getTasks() )
            if( !subTasks.contains( task.getId() ) )
                buildTask(task);
    }

    private void buildTask(AbstractTask task)
    {
        TaskBuilder builder = new TaskBuilder( parent, controller );
        Node modelNode;
        if(task instanceof RepeatedTask)
            modelNode = modelNodes.get( SedmlUtils.getModelReference( (RepeatedTask)task, sedml ) );
        else
            modelNode = modelNodes.get( task.getModelReference() );
        builder.setModelNode( modelNode );
        builder.setOutputFolder( outputFolder );
        builder.setSedml( sedml );
        builder.setTask( task );
        builder.setModelNodes( modelNodes );
        builder.build();
        simulationResultNodes.put( task.getId(), builder.getOutputNode());
    }

    
}
