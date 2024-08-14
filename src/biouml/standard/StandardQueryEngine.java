package biouml.standard;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import biouml.model.Module;
import biouml.standard.type.Base;
import biouml.standard.type.Reaction;
import biouml.standard.type.SemanticRelation;
import biouml.standard.type.SpecieReference;
import biouml.workbench.graphsearch.QueryEngineSupport;
import biouml.workbench.graphsearch.QueryOptions;
import biouml.workbench.graphsearch.SearchElement;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.RelationType;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.jobcontrol.JobControl;

/**
 * Dummy implementation, witch simple check all elements in data collections
 * and do many unnecessary operations - it is base implementation and can
 * be improved by optimization of obtaining of reactions and relations
 */
public class StandardQueryEngine extends QueryEngineSupport
{
    protected static final Logger log = Logger.getLogger(StandardQueryEngine.class.getName());

    @Override
    public String getName( TargetOptions dbOptions )
    {
        return "General collection search";
    }
    
    @Override
    public int canSearchLinked(TargetOptions dbOptions)
    {
        //Search linked is available only for 1 database with "standard" type
        DataElementPathSet collections = dbOptions.getUsedCollectionPaths();
        if( collections.size() == 1 )
        {
            Module module = collections.first().optDataElement(Module.class);
            if( module != null )
            {
                return 10;
            }
        }
        return 0;
    }

    @Override
    public SearchElement[] searchLinked(SearchElement[] startNodes, QueryOptions queryOptions, TargetOptions dbOptions,
            JobControl jobControl) throws Exception
    {
        Module module = dbOptions.getUsedCollectionPaths().first().getDataElement(Module.class);
        Set<SearchElement> elements = new HashSet<>();
        if( startNodes != null )
        {
            for( SearchElement startNode : startNodes )
            {
                internalSearchLinked(startNode, queryOptions.getDirection(), queryOptions.getDepth(), elements, module, jobControl);
            }
        }
        return elements.toArray(new SearchElement[elements.size()]);
    }

    protected void internalSearchLinked(SearchElement node, int direction, float depth, Set<SearchElement> elements, Module module,
            JobControl jobControl) throws Exception
    {
        if( depth == 0 )
            return;

        if( node == null )
            throw new IllegalArgumentException("'node' can't be null");

        Base kernel = node.getBase();
        if( kernel == null )
            throw new IllegalArgumentException("Kernel of 'node' can't be null node=" + node);

        if( jobControl != null )
        {
            jobControl.setPreparedness(0);
        }

        Set<SearchElement> addedElements = new HashSet<>();

        if( kernel instanceof Reaction )
        {
            // just add specie references
            for( SpecieReference sr : ( (Reaction)kernel ).getSpecieReferences() )
            {
                int linkDirection;
                String relationType = null;
                if( sr.getRole().equals(SpecieReference.PRODUCT) )
                {
                    linkDirection = BioHub.DIRECTION_DOWN;
                    relationType = RelationType.PRODUCT;
                }
                else if( sr.getRole().equals(SpecieReference.REACTANT) )
                {
                    linkDirection = BioHub.DIRECTION_UP;
                    relationType = RelationType.REACTANT;
                }
                else
                {
                    linkDirection = BioHub.DIRECTION_UP;
                    relationType = RelationType.MODIFIER;
                }
                if( ( direction == linkDirection ) || ( direction == BioHub.DIRECTION_BOTH ) )
                {
                    Base specie = getSpecieBase(module, sr);
                    if( specie != null )
                    {
                        SearchElement specieNodeInfo = new SearchElement(specie);
                        specieNodeInfo.setLinkedFromPath(getPath(node.getBase()));
                        specieNodeInfo.setLinkedLength(0.5f);
                        specieNodeInfo.setLinkedDirection(linkDirection);
                        specieNodeInfo.setRelationType(relationType);

                        if( !elements.contains(specieNodeInfo) )
                        {
                            elements.add(specieNodeInfo);
                            addedElements.add(specieNodeInfo);
                        }
                    }
                }
            }
        }
        else
        {
            // first iteration - get all reactions
            DataCollection<Reaction> reactions = getReactions(module, kernel, direction);
            if( reactions != null )
            {
                List<SearchElement> addedReactions = new ArrayList<>();
                Iterator<Reaction> reactionIter = reactions.iterator();
                while( reactionIter.hasNext() )
                {
                    Reaction reaction = reactionIter.next();
                    SearchElement reactionNodeInfo = new SearchElement(reaction);
                    reactionNodeInfo.setLinkedFromPath(getPath(node.getBase()));
                    for( SpecieReference sr : reaction.getSpecieReferences() )
                    {
                        Base base = getSpecieBase(module, sr);
                        if( ( base != null ) && ( base.getName().equals(node.getBase().getName()) ) )
                        {
                            reactionNodeInfo.setLinkedLength(0.5f);
                            if( sr.getRole().equals(SpecieReference.PRODUCT) )
                            {
                                reactionNodeInfo.setLinkedDirection(BioHub.DIRECTION_UP);
                                reactionNodeInfo.setRelationType(RelationType.PRODUCT);
                            }
                            else if( sr.getRole().equals(SpecieReference.REACTANT) )
                            {
                                reactionNodeInfo.setLinkedDirection(BioHub.DIRECTION_DOWN);
                                reactionNodeInfo.setRelationType(RelationType.REACTANT);
                            }
                            else
                            {
                                reactionNodeInfo.setLinkedDirection(BioHub.DIRECTION_DOWN);
                                reactionNodeInfo.setRelationType(RelationType.MODIFIER);
                            }
                            break;
                        }
                    }
                    if( !elements.contains(reactionNodeInfo) )
                    {
                        elements.add(reactionNodeInfo);
                        addedReactions.add(reactionNodeInfo);
                        addedElements.add(reactionNodeInfo);
                    }
                }
            }

            // second iteration - get all relations
            if( depth >= 1.0f )
            {
                DataCollection<SemanticRelation> relations = getSemanticRelations(module, kernel, direction);
                if( relations != null )
                {
                    Iterator<SemanticRelation> relationIter = relations.iterator();
                    while( relationIter.hasNext() )
                    {
                        SemanticRelation rel = relationIter.next();
                        String inName = rel.getInputElementName();
                        String outName = rel.getOutputElementName();

                        Base inputElement = (Base)module.getKernel(DataElementPath.create(Module.DATA).getRelativePath(inName).toString());
                        Base outputElement = (Base)module.getKernel(DataElementPath.create(Module.DATA).getRelativePath(outName).toString());

                        if( ( null != inputElement ) && ( kernel != inputElement ) )
                        {
                            SearchElement inputNodeInfo = new SearchElement(inputElement);
                            inputNodeInfo.setLinkedFromPath(getPath(node.getBase()));
                            inputNodeInfo.setLinkedLength(1);
                            inputNodeInfo.setLinkedDirection(BioHub.DIRECTION_UP);
                            inputNodeInfo.setRelationType(RelationType.SEMANTIC);
                            if( !elements.contains(inputNodeInfo) )
                            {
                                elements.add(inputNodeInfo);
                                addedElements.add(inputNodeInfo);
                            }
                        }

                        if( ( null != outputElement ) && ( kernel != outputElement ) )
                        {
                            SearchElement outputNodeInfo = new SearchElement(outputElement);
                            outputNodeInfo.setLinkedFromPath(getPath(node.getBase()));
                            outputNodeInfo.setLinkedLength(1);
                            outputNodeInfo.setLinkedDirection(BioHub.DIRECTION_DOWN);
                            outputNodeInfo.setRelationType(RelationType.SEMANTIC);
                            if( !elements.contains(outputNodeInfo) )
                            {
                                elements.add(outputNodeInfo);
                                addedElements.add(outputNodeInfo);
                            }
                        }
                    }
                }
            }
        }

        int addedSize = addedElements.size();
        int i = 0;
        Iterator<SearchElement> specieIterator = addedElements.iterator();
        while( specieIterator.hasNext() )
        {
            i++;
            if( jobControl != null )
            {
                jobControl.setPreparedness((int) ( ( i * 100.0 ) / addedSize ));
            }
            SearchElement specieNodeInfo = specieIterator.next();
            internalSearchLinked(specieNodeInfo, direction, depth - specieNodeInfo.getLinkedLength(), elements, module, null);
        }
        if( jobControl != null )
        {
            jobControl.setPreparedness(100);
        }
    }
    /**
     * Get Base element by SpacieRefertence
     */
    protected Base getSpecieBase(Module module, SpecieReference sr) throws Exception
    {
        String specieName = sr.getSpecie();
        Base base = (Base)module.getKernel(specieName);
        if( null == base )
        {
            log.log(Level.SEVERE, "Cannot find reaction species by name " + specieName);
        }
        return base;
    }

    /**
     * Get all reactions in specified direction
     */
    protected DataCollection<Reaction> getReactions(Module module, Base kernel, int direction) throws Exception
    {
        DataCollection<Reaction>[] dcList = CollectionFactoryUtils.findDataCollections(module.getCompletePath(), Reaction.class);
        if(dcList == null || dcList.length == 0)
            return null;

        VectorDataCollection<Reaction> reactions = new VectorDataCollection<>( module.getType().getCategory( Reaction.class ), null,
                new Properties());

        String kernelName = CollectionFactory.getRelativeName(kernel, module);
        if( kernelName != null )
        {
            for( DataCollection<Reaction> fullReactions: dcList )
            {
                for( Reaction r: fullReactions )
                {
                    for( SpecieReference node: r )
                    {
                        if( node.getSpecie().equals(kernelName) )
                        {
                            if( BioHub.DIRECTION_DOWN == direction )
                            {
                                if( SpecieReference.REACTANT.equalsIgnoreCase(node.getRole())
                                        || SpecieReference.MODIFIER.equalsIgnoreCase(node.getRole()) )
                                {
                                    reactions.put(r);
                                    break;
                                }
                            }
                            else if( BioHub.DIRECTION_UP == direction )
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
                                        || SpecieReference.PRODUCT.equalsIgnoreCase(node.getRole())
                                        || SpecieReference.MODIFIER.equalsIgnoreCase(node.getRole()) )
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
        }
        return reactions;
    }

    /**
     * Get all relations in speciefied direction
     */
    protected DataCollection<SemanticRelation> getSemanticRelations(Module module, Base kernel, int direction) throws Exception
    {
        DataCollection<SemanticRelation>[] relationCollections = CollectionFactoryUtils.findDataCollections(module.getCompletePath(), SemanticRelation.class);
        if(relationCollections == null || relationCollections.length == 0)
            return null;

        VectorDataCollection<SemanticRelation> relations = new VectorDataCollection<>(
                module.getType().getCategory(
                SemanticRelation.class), null, new Properties());

        DataCollection<?> category = module.getCategory(kernel.getClass());
        if( category != null )
        {
            String kernelName = DataElementPath.EMPTY_PATH.getChildPath(kernel.getOrigin().getName(), kernel.getName()).toString();
            for( DataCollection<SemanticRelation> fullRelations: relationCollections )
            {
                for( SemanticRelation r: fullRelations )
                {
                    if( BioHub.DIRECTION_DOWN == direction )
                    {
                        if( r.getInputElementName().equals(kernelName) )
                        {
                            relations.put(r);
                        }
                    }
                    else if( BioHub.DIRECTION_UP == direction )
                    {
                        if( r.getOutputElementName().equals(kernelName) )
                        {
                            relations.put(r);
                        }
                    }
                    else if( BioHub.DIRECTION_BOTH == direction || BioHub.DIRECTION_UNDEFINED == direction )
                    {
                        if( r.getInputElementName().equals(kernelName) || r.getOutputElementName().equals(kernelName) )
                        {
                            relations.put(r);
                        }
                    }
                }
            }
        }
        return relations;
    }
}
