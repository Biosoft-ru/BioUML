package biouml.plugins.sedml;

import java.util.Map;
import java.util.Set;

import org.jlibsedml.RepeatedTask;
import org.jlibsedml.SedML;

import ru.biosoft.analysis.CopyDataElement;
import ru.biosoft.analysiscore.AnalysisMethodRegistry;
import biouml.model.Compartment;
import biouml.model.Node;
import biouml.plugins.research.workflow.WorkflowSemanticController;

public class RepeatedTaskBuilder extends WorkflowBuilder
{
    private SedML sedml;
    private RepeatedTask task;
    private Node inputModelNode;
    
    private String simulationResultPath;
    private String intermediateOutputPath;
    
    private Node simulationResultNode;
    
    private Map<String, Node> modelNodes;
    
    public RepeatedTaskBuilder(Compartment parent, WorkflowSemanticController controller)
    {
        super( parent, controller );
    }
    
    public void setSedml(SedML sedml)
    {
        this.sedml = sedml;
    }

    public void setTask(RepeatedTask task)
    {
        this.task = task;
    }
    
    public void setInputModelNode(Node node)
    {
        this.inputModelNode = node;
    }
    
    public void setSimulationResultPath(String simulationResultPath)
    {
        this.simulationResultPath = simulationResultPath;
    }

    public void setIntermediateOutputPath(String intermediateOutputPath)
    {
        this.intermediateOutputPath = intermediateOutputPath;
    }
    
    public void setModelNodes(Map<String, Node> modelNodes)
    {
        this.modelNodes = modelNodes;
    }

    public Node getSimulationResultNode()
    {
        return simulationResultNode;
    }

    @Override
    public void build()
    {
        String title = getTitleForSedmlElement( task );
        simulationResultNode = addDataElementNode( title, simulationResultPath + "/" + title );
        
        if(task.getRange() == null || task.getRange().isEmpty())
        {
            Set<String> names = task.getRanges().keySet();
            if(!names.isEmpty())
                task.setRange( names.iterator().next() );
        }
        
        Compartment cycle = controller.createCycleNode( parent );
        parent.put( cycle );
        CycleBuilder cycleBuilder = new CycleBuilder( cycle, controller );
        cycleBuilder.setTask( task );
        cycleBuilder.setSedml( sedml );
        cycleBuilder.setModelNodes( modelNodes );
        cycleBuilder.setSimulationResultNode( simulationResultNode );
        String iterationNameTemplate = task.getRange() + "=$" + task.getRange() + "$";
        cycleBuilder.setOutputPath( intermediateOutputPath + "/iterations/" + iterationNameTemplate );
        if(task.getResetModel())
        {
            cycleBuilder.setInputModelNode(inputModelNode);
        }
        else
        {
            String currentModelPath = intermediateOutputPath + "/Current model";
            Node modelBeforeIteration = copyModel(currentModelPath);
            Node modelAfterIteration = addDataElementNode( "Model after iteration", currentModelPath );
            cycleBuilder.setInputModelNode( modelBeforeIteration );
            cycleBuilder.setModelAfterIterationNode(modelAfterIteration);
        }
        
        cycleBuilder.build();
        parent.put( cycle );
    }

    private Node copyModel(String path)
    {
        CopyDataElement analysis = AnalysisMethodRegistry.getAnalysisMethod( CopyDataElement.class );
        Compartment analysisNode = addAnalysis( analysis );
        Node dstNode = addDataElementNode( "Model before iteration", path );
        addDirectedEdge( parent, inputModelNode, (Node)analysisNode.get( "src" ) );
        addDirectedEdge( parent, (Node)analysisNode.get( "dst" ), dstNode );
        return dstNode;
    }
}
