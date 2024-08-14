package biouml.plugins.biopax;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Module;
import biouml.model.Node;
import biouml.model.SemanticController;
import biouml.standard.StandardQueryEngine;
import biouml.standard.type.Base;
import biouml.standard.type.Reaction;
import biouml.standard.type.SemanticRelation;
import biouml.standard.type.SpecieReference;
import biouml.workbench.graphsearch.SearchElement;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementNotFoundException;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.jobcontrol.JobControl;

public class BioPAXQueryEngine extends StandardQueryEngine
{
    @Override
    public String getName( TargetOptions dbOptions )
    {
        return "BioPAX-specific search in " + dbOptions.collections().findAny().get().getShortName();
    }

    @Override
    protected void internalSearchLinked(SearchElement node, int direction, float depth, Set<SearchElement> elements, Module module,
            JobControl jobControl) throws Exception
    {
        if( node == null )
            throw new IllegalArgumentException("'node' can't be null");

        Base kernel = node.getBase();
        if( kernel == null )
            throw new IllegalArgumentException("Kernel of 'node' can't be null node=" + node);

        Set<SearchElement> addedElements = new HashSet<>();

        //if we show Reaction we also show all specie references
        if( node.getBase() instanceof Reaction )
        {
            Base reaction = node.getBase();
            for( SpecieReference sr : ( (Reaction)reaction ).getSpecieReferences() )
            {
                Base base = getSpecieBase(module, sr);
                SearchElement specieNodeInfo = new SearchElement(base);
                boolean added = elements.add(specieNodeInfo);
                if( added )
                {
                    addedElements.add(specieNodeInfo);
                }
            }
        }

        // first iteration - get all reactions
        DataCollection<Reaction> reactions = getReactions(module, kernel, direction);
        for( Reaction reaction : reactions )
        {
            SearchElement reactionNodeInfo = new SearchElement(reaction);
            elements.add(reactionNodeInfo);
            internalSearchLinked(reactionNodeInfo, BioHub.DIRECTION_BOTH, 1, elements, module, jobControl);
        }

        for( Reaction reaction : reactions )
        {
            for( SpecieReference sr : reaction )
            {
                Base base = getSpecieBase(module, sr);
                SearchElement specieNodeInfo = new SearchElement(base);
                boolean added = elements.add(specieNodeInfo);
                if( added )
                {
                    addedElements.add(specieNodeInfo);
                }
            }
        }

        // second iteration - get all relations
        DataCollection<SemanticRelation> relations = getSemanticRelations(module, kernel, direction);
        for(SemanticRelation relation : relations)
        {
            SearchElement relationNodeInfo = new SearchElement(relation);
            elements.add(relationNodeInfo);
        }

        DataCollection<SpecieReference> references = module.getCompletePath().getChildPath(Module.DATA, BioPAXSupport.PARTICIPANT).getDataCollection(SpecieReference.class);
        for(SemanticRelation rel : relations)
        {
            String inName = rel.getInputElementName();
            if( inName.startsWith(Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + BioPAXSupport.PARTICIPANT) )
            {
                inName = references.get(DataElementPath.create(inName).getName()).getSpecie();
            }
            String outName = rel.getOutputElementName();
            if( outName.startsWith(Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + BioPAXSupport.PARTICIPANT) )
            {
                outName = references.get(DataElementPath.create(outName).getName()).getSpecie();
            }

            Base inputElement = (Base)module.getKernel(inName);
            Base outputElement = (Base)module.getKernel(outName);

            if( null != inputElement )
            {
                SearchElement inputNodeInfo = new SearchElement(inputElement);
                boolean added = elements.add(inputNodeInfo);
                if( added )
                {
                    if( inputElement instanceof Reaction )
                    {
                        internalSearchLinked(inputNodeInfo, BioHub.DIRECTION_BOTH, 1, elements, module, jobControl);
                    }
                    addedElements.add(inputNodeInfo);
                }
            }
            else
            {
                log.log(Level.SEVERE, "Input element '" + inName + "' for relation '" + rel.getName() + "' not found");
            }

            if( null != outputElement )
            {
                SearchElement outputNodeInfo = new SearchElement(outputElement);
                boolean added = elements.add(outputNodeInfo);
                if( added )
                {
                    if( outputElement instanceof Reaction )
                    {
                        internalSearchLinked(outputNodeInfo, BioHub.DIRECTION_BOTH, 1, elements, module, jobControl);
                    }
                    addedElements.add(outputNodeInfo);
                }
            }
            else
            {
                log.log(Level.SEVERE, "Output element '" + outName + "' for relation '" + rel.getName() + "' not found");
            }
        }

        --depth;
        if( depth > 0 )
        {
            for(SearchElement specieNodeInfo : addedElements)
            {
                internalSearchLinked(specieNodeInfo, direction, depth, elements, module, jobControl);
            }
        }
    }

    @Override
    protected @Nonnull DataCollection<Reaction> getReactions(Module module, Base kernel, int direction) throws Exception
    {
        VectorDataCollection<Reaction> reactions = new VectorDataCollection<>("reactions", Reaction.class, null);
        DataCollection<Reaction> fullReactions;
        try
        {
            fullReactions = module.getCompletePath().getChildPath(Module.DATA, BioPAXSupport.CONVERSION).getDataCollection(Reaction.class);
        }
        catch( DataElementNotFoundException e )
        {
            return reactions;
        }

        String kernelName = CollectionFactory.getRelativeName(kernel, module);
        if( kernelName != null )
        {
            for( Reaction r : fullReactions )
            {
                for( SpecieReference node : r )
                {
                    if( kernelName.endsWith(node.getSpecie()) )
                    {
                        if( BioHub.DIRECTION_UP == direction )
                        {
                            if( SpecieReference.REACTANT.equalsIgnoreCase(node.getRole()) )
                            {
                                reactions.put(r);
                                break;
                            }
                        }
                        else if( BioHub.DIRECTION_DOWN == direction )
                        {
                            if( SpecieReference.PRODUCT.equalsIgnoreCase(node.getRole()) )
                            {
                                reactions.put(r);
                                break;
                            }
                        }
                        else if( BioHub.DIRECTION_BOTH == direction )
                        {
                            if( SpecieReference.REACTANT.equalsIgnoreCase(node.getRole())
                                    || SpecieReference.PRODUCT.equalsIgnoreCase(node.getRole()) )
                            {
                                reactions.put(r);
                                break;
                            }
                        }
                        else if( BioHub.DIRECTION_UNDEFINED == direction )
                        {
                            reactions.put(r);
                            break;
                        }
                    }
                }
            }
        }
        return reactions;
    }

    @Override
    protected @Nonnull DataCollection<SemanticRelation> getSemanticRelations(Module module, Base kernel, int direction) throws Exception
    {
        VectorDataCollection<SemanticRelation> relations = new VectorDataCollection<>("relations", SemanticRelation.class, null);
        DataCollection<SemanticRelation> fullRelations;
        try
        {
            fullRelations = module.getCompletePath().getChildPath(Module.DATA, BioPAXSupport.CONTROL).getDataCollection(SemanticRelation.class);
        }
        catch( DataElementNotFoundException e )
        {
            return relations;
        }

        DataCollection<?> category = module.getCategory(kernel.getClass());
        if( category != null )
        {
            String kernelName = DataElementPath.create(kernel).toString();
            DataCollection<SpecieReference> references = module.getCompletePath().getChildPath(Module.DATA, BioPAXSupport.PARTICIPANT).getDataCollection(SpecieReference.class);
            for( SemanticRelation r : fullRelations )
            {
                String inputName = r.getInputElementName();
                if( inputName.startsWith(Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + BioPAXSupport.PARTICIPANT) )
                {
                    inputName = references.get(DataElementPath.create(inputName).getName()).getSpecie();
                }
                String outputName = r.getOutputElementName();
                if( outputName.startsWith(Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + BioPAXSupport.PARTICIPANT) )
                {
                    outputName = references.get(DataElementPath.create(outputName).getName()).getSpecie();
                }
                if( BioHub.DIRECTION_UP == direction )
                {
                    if( inputName.endsWith(kernelName) )
                    {
                        relations.put(r);
                    }
                }
                else if( BioHub.DIRECTION_DOWN == direction )
                {
                    if( outputName.endsWith(kernelName) )
                    {
                        relations.put(r);
                    }
                }
                else if( BioHub.DIRECTION_BOTH == direction || BioHub.DIRECTION_UNDEFINED == direction )
                {
                    if( inputName.endsWith(kernelName) || outputName.endsWith(kernelName) )
                    {
                        relations.put(r);
                    }
                }
            }
        }
        return relations;
    }

    protected void createEdgesForRelation(SemanticController controller, Module module, Compartment c, Base kernel)
            throws Exception
    {
        SemanticRelation relation = kernel.cast( SemanticRelation.class );
        String inName = relation.getInputElementName();
        String outName = relation.getOutputElementName();

        DataCollection<SpecieReference> references = module.getCompletePath().getChildPath(Module.DATA, BioPAXSupport.PARTICIPANT).getDataCollection(SpecieReference.class);
        if( inName.startsWith(Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + BioPAXSupport.PARTICIPANT) )
        {
            inName = references.get(DataElementPath.create(inName).getName()).getSpecie();
        }
        if( outName.startsWith(Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + BioPAXSupport.PARTICIPANT) )
        {
            outName = references.get(DataElementPath.create(outName).getName()).getSpecie();
        }

        Base inputElement = (Base)module.getKernel(inName);
        Base outputElement = (Base)module.getKernel(outName);

        if( null == inputElement )
        {
            log.log(Level.SEVERE, "Input element '" + inName + "' for relation '" + relation.getName() + "' not found");
            return;
        }

        if( null == outputElement )
        {
            log.log(Level.SEVERE, "Output element '" + outName + "' for relation '" + relation.getName() + "' not found");
            return;
        }

        List<Node> inputNodes = c.getNodesWithKernel(inputElement);
        if( inputNodes.isEmpty() )
        {
            log.log(Level.SEVERE, "Input element '" + inName + "' for relation '" + relation.getName() + "' not found in the compartment "
                    + c.getCompletePath());
            return;
        }

        List<Node> outNodes = c.getNodesWithKernel(outputElement);
        if( outNodes.isEmpty() )
        {
            log.log(Level.SEVERE, "Output element '" + outName + "' for relation '" + relation.getName() + "' not found in the compartment "
                    + c.getCompletePath());
            return;
        }

        for( Node inputNode : inputNodes )
        {
            for( Node outNode : outNodes )
            {
                Edge newEdge = controller.findEdge( inputNode, outNode, kernel );
                if( newEdge == null )
                {
                    newEdge = new Edge(c, relation, inputNode, outNode);
                    if( !controller.canAccept(c, newEdge) )
                        continue;
                }

                newEdge.setPath(null);
                inputNode.addEdge(newEdge);
                outNode.addEdge(newEdge);
                c.put(newEdge);
            }
        }
    }

    protected void createEdgesForReaction(SemanticController controller, Module module, Compartment c, Base kernel)
            throws Exception
    {
        Reaction reaction = kernel.cast( Reaction.class );
        SpecieReference[] refArr = reaction.getSpecieReferences();
        for( SpecieReference specRef : refArr )
        {
            if( specRef.getRole().equals(SpecieReference.OTHER) )
                continue;
            Base ref = CollectionFactory.getDataElement(specRef.getSpecie(), module, Base.class);
            List<Node> nodes = c.getNodesWithKernel(ref);
            String kernelRole = specRef.getRole();

            for( Node reactionNode : c.getNodesWithKernel(kernel) )
            {
                Edge newEdge = null;
                for( Node node : nodes )
                {
                    if( kernelRole.equals(SpecieReference.PRODUCT) )
                    {
                        newEdge = controller.findEdge( reactionNode, node, specRef );
                        if( newEdge == null )
                        {
                            newEdge = new Edge(c, specRef, reactionNode, node);
                            if( !controller.canAccept(c, newEdge) )
                                continue;
                        }
                    }
                    else
                    {
                        newEdge = controller.findEdge( node, reactionNode, specRef );
                        if( newEdge == null )
                        {
                            newEdge = new Edge(c, specRef, node, reactionNode);
                            if( !controller.canAccept(c, newEdge) )
                                continue;
                        }
                    }
                    newEdge.setPath(null);
                    node.addEdge(newEdge);
                    reactionNode.addEdge(newEdge);
                    c.put(newEdge);
                }
            }
        }
    }

    protected Edge findEdge(String edgeName, @Nonnull Node first, @Nonnull Node second) throws Exception
    {
        Edge edge = null;
        Compartment edgeOrigin = Node.findCommonOrigin(first, second);
        while( edgeOrigin != null )
        {
            if( edgeOrigin.contains(edgeName) )
            {
                edge = (Edge)edgeOrigin.get(edgeName);
                break;
            }
            if( edgeOrigin instanceof Diagram )
                break;
            edgeOrigin = (Compartment)edgeOrigin.getOrigin();
        }
        return edge;
    }
}
