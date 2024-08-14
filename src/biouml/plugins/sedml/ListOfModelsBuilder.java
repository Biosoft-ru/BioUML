package biouml.plugins.sedml;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import one.util.streamex.StreamEx;

import org.jlibsedml.ComputeChange;
import org.jlibsedml.Model;
import org.jlibsedml.SedML;
import org.jlibsedml.Variable;

import ru.biosoft.access.core.DataElementPath;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Node;
import biouml.plugins.research.workflow.WorkflowSemanticController;

public class ListOfModelsBuilder extends WorkflowBuilder
{
    private SedML sedml;
    private DataElementPath modelCollectionPath;
    
    private Map<String, Node> resultingNodes;
    private Map<String, Diagram> resultingDiagrams;

    public ListOfModelsBuilder(Compartment parent, WorkflowSemanticController controller)
    {
        super( parent, controller );
    }
    
    public void setSedml(SedML sedml)
    {
        this.sedml = sedml;
    }
    
    public void setModelCollectionPath(DataElementPath modelCollectionPath)
    {
        this.modelCollectionPath = modelCollectionPath;
    }
    
    public Map<String, Node> getResultingNodes()
    {
        return resultingNodes;
    }

    public Map<String, Diagram> getResultingDiagrams()
    {
        return resultingDiagrams;
    }

    @Override
    public void build()
    {
        resultingNodes = new HashMap<>();
        resultingDiagrams = new HashMap<>();
        Set<String> modelIds = sedml.getModels().stream().map( Model::getId ).collect( Collectors.toSet() );
        while(!modelIds.isEmpty())
        {
            Iterator<String> it = modelIds.iterator();
            boolean changed = false;
            while(it.hasNext())
            {
                String modelId = it.next();
                Model model = sedml.getModelWithId( modelId );
                Set<String> referencedModels = StreamEx.of( model.getListOfChanges() ).select( ComputeChange.class )
                    .flatMap( c-> c.getListOfVariables().stream() ).map( Variable::getReference ).nonNull().toSet();
                Model sourceModel = sedml.getModelWithId( model.getSource() );
                Node sourceModelNode = resultingNodes.get( model.getSource() );
                if((sourceModel == null || sourceModelNode != null) && resultingNodes.keySet().containsAll( referencedModels ))
                {
                    ModelBuilder builder = new ModelBuilder( parent, controller );
                    
                    builder.setInputModelNode( sourceModelNode );
                    builder.setInputDiagram( resultingDiagrams.get( model.getSource() ) );
                    builder.setModelCollectionPath( modelCollectionPath );
                    builder.setSedmlModel( model );
                    builder.setAvailableModels( resultingDiagrams );
                    builder.setAvailableModelNodes( resultingNodes );
                    builder.build();
                    
                    resultingNodes.put( modelId, builder.getOutputModelNode() );
                    resultingDiagrams.put( modelId, builder.getOutputDiagram() );
                    
                    it.remove();
                    changed = true;
                }
            }
            if(!changed)
                throw new IllegalArgumentException("Cyclic model reference");
        }
    }

}
