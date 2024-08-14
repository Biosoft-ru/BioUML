package biouml.plugins.keynodes.biohub;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.IntUnaryOperator;

import javax.annotation.Nonnull;

import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.BioHubSupport;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.biohub.RelationType;
import ru.biosoft.access.biohub.TargetOptions;
import biouml.model.Diagram;
import biouml.model.DiagramType;
import biouml.model.xml.XmlDiagramType;
import biouml.plugins.keynodes.KeyNodeConstants;
import biouml.plugins.keynodes.graph.ElementConverter;
import biouml.plugins.keynodes.graph.GraphUtils;
import biouml.plugins.keynodes.graph.HubGraph;
import biouml.standard.diagram.PathwayDiagramType;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Species;

import com.developmentontheedge.beans.DynamicProperty;

/**
 * BioHub extension for KeyNodes search
 * @author anna
 *
 */
public abstract class KeyNodesHub<N> extends BioHubSupport
{
    public static final DataElementPath KEY_NODES_HUB = DataElementPath.create("KeyNodesHub");
    public static final String SBGN_NOTATION_NAME = "sbml_sbgn.xml";

    public KeyNodesHub(Properties properties)
    {
        super(properties);
    }

    @Override
    public int getPriority(TargetOptions dbOptions)
    {
        return getPriority( dbOptions, KEY_NODES_HUB, () -> 5 );
    }

    abstract protected HubGraph<N> getHub(TargetOptions dbOptions, String[] relationTypes);
    abstract protected ElementConverter<N> getElementConverter();

    /**
     * Get random sample of elements from Hub search space, sample should contain different elements
     */
    public Element[] getRandomSample(int sampleSize, TargetOptions dbOptions, String[] relationTypes, IntUnaryOperator random)
    {
        List<String> names = new ArrayList<>(getNames(dbOptions, relationTypes));
        int namesSize = names.size();
        if( namesSize < sampleSize )
            sampleSize = namesSize;

        for( int i = names.size(); i > 1; i-- )
            Collections.swap( names, i - 1, random.applyAsInt( i ) );
        return names.stream().limit( sampleSize ).map( n -> "stub/KeyNodes//" + n ).map( Element::new ).toArray( Element[]::new );
    }

    /**
     * Get complete Element path by accession
     */
    public abstract String getElementTitle(Element element);

    public abstract DataElementPath getCompleteElementPath(String acc);
    
    public DataElementPath getCompleteElementPath(SpecieReference sr)
    {
        if(sr.getSpecie().startsWith( "Data/" ))
            return getModulePath().getRelativePath( sr.getSpecie() );
        return getCompleteElementPath( sr.getSpecieName() );
    }

    /**
     * @param reactionAcc reaction accession number
     * @param species species
     * @param preferredComponents reaction components which are preferred in case if several alternatives available
     * @return list of Elements which form this reaction
     */
    public List<Element> getReactionComponents(String reactionAcc, Species species, @Nonnull Map<String, Set<String>> preferredComponents)
    {
        List<Element> result = new ArrayList<>();
        DataElementPath reactionPath = getCompleteElementPath( reactionAcc );
        result.add( new Element( reactionPath ) );
        for( SpecieReference ref : reactionPath.getDataElement( Reaction.class ) )
        {
            Element e = new Element( getCompleteElementPath( ref ) );
            e.setRelationType( ref.getRole() );
            e.setLinkedDirection( ref.getRole().equals( RelationType.PRODUCT ) ? DIRECTION_DOWN : DIRECTION_UP );
            e.setLinkedFromPath( reactionPath.toString() );
            result.add( e );
        }
        return result;
    }

    /**
     * Get minimal paths from key molecule to every target molecule. Default implementation via getMinimalPath.
     * @param key
     * @param targets
     * @param dbOptions
     * @param relationTypes
     * @param maxLength
     * @param direction
     * @return list of paths, order of paths do not coincide with targets array, is not null
     */
    @Override
    public List<Element[]> getMinimalPaths(Element key, Element[] targets, TargetOptions dbOptions, String[] relationTypes, int maxLength,
            int direction)
    {
        if( targets.length == 0 )
            return new ArrayList<>();
        ElementConverter<N> converter = getElementConverter();
        HubGraph<N> graph = getHub( dbOptions, relationTypes );
        if( ( targets.length == 1 && key.equals(targets[0]) ) || maxLength == 0 ) //self path
        {
            return Collections.singletonList( new Element[] {converter.fromNode( converter.toNode( key ) )} );
        }
        switch(direction)
        {
            case DIRECTION_UP:
            case DIRECTION_DOWN:
                return GraphUtils.getDirectedPaths( key, targets, maxLength, direction, graph, converter, this );
            case DIRECTION_BOTH:
                List<Element[]> result = new ArrayList<>();
                result.addAll(GraphUtils.getDirectedPaths( key, targets, maxLength, DIRECTION_UP, graph, converter, this ));
                result.addAll(GraphUtils.getDirectedPaths( key, targets, maxLength, DIRECTION_DOWN, graph, converter, this ));
                return result;
            default:
                return Collections.emptyList();
        }
    }

    /**
     * Returns all reactions from all directed paths between key molecule and every target molecule
     * @param key
     * @param targets
     * @param dbOptions
     * @param relationTypes
     * @param maxLength
     * @param direction
     * @return
     */
    public List<Element> getAllReactions(Element key, Element[] targets, TargetOptions dbOptions, String[] relationTypes, int maxLength,
            int direction)
    {
        if( targets.length == 0 )
            return new ArrayList<>();
        ElementConverter<N> converter = getElementConverter();
        HubGraph<N> graph = getHub( dbOptions, relationTypes );
        if( ( targets.length == 1 && key.equals( targets[0] ) ) || maxLength == 0 )
            return Collections.emptyList();
        switch( direction )
        {
            case DIRECTION_UP:
            case DIRECTION_DOWN:
                return GraphUtils.getAllReactionInDirectedPaths( key, targets, maxLength, direction, graph, converter, this );
            case DIRECTION_BOTH:
                List<Element> result = new ArrayList<>();
                result.addAll( GraphUtils.getAllReactionInDirectedPaths( key, targets, maxLength, DIRECTION_UP, graph, converter, this ) );
                result.addAll( GraphUtils.getAllReactionInDirectedPaths( key, targets, maxLength, DIRECTION_DOWN, graph, converter, this ) );
                return result;
            default:
                return Collections.emptyList();
        }
    }

    @Override
    public Element[] getReference(Element startElement, TargetOptions dbOptions, String[] relationTypes, int maxLength, int direction)
    {
        HubGraph<N> hub = getHub( dbOptions, relationTypes );
        ElementConverter<N> converter = getElementConverter();
        switch(direction)
        {
            case DIRECTION_UP:
            case DIRECTION_DOWN:
                return GraphUtils.getDirectedReferences( hub, startElement, maxLength, direction, converter );
            case DIRECTION_BOTH:
                List<Element> result = new ArrayList<>();
                result.addAll(Arrays.asList(GraphUtils.getDirectedReferences( hub, startElement, maxLength, DIRECTION_UP, converter )));
                result.addAll(Arrays.asList(GraphUtils.getDirectedReferences( hub, startElement, maxLength, DIRECTION_DOWN, converter )));
                return result.toArray( new Element[0] );
            default:
                return new Element[0];
        }
    }

    @Override
    public Element[] getMinimalPath(Element key, Element target, TargetOptions dbOptions, String[] relationTypes, int maxLength,
            int direction)
    {
        List<Element[]> paths = getMinimalPaths(key, new Element[] {target}, dbOptions, relationTypes, maxLength, direction);
        return paths == null || paths.isEmpty() ? null : paths.get( 0 );
    }

    /**
     * @param basicForm to get the parent isoform for
     * @return parent isoform or null if non applicable
     */
    public String getParentIsoform(String basicForm)
    {
        return null;
    }

    public DiagramType getVisualizationDiagramType()
    {
        try
        {
            XmlDiagramType typeObject = XmlDiagramType.getTypeObject(SBGN_NOTATION_NAME);
            if( typeObject != null )
                return typeObject;
            return new PathwayDiagramType();
        }
        catch( Exception e )
        {
            return new PathwayDiagramType();
        }
    }

    protected void annotateDiagram(Diagram d)
    {
        d.getAttributes().add( new DynamicProperty( KeyNodeConstants.BIOHUB_PROPERTY, String.class, getName() ) );
        ReferenceType[] types = getSupportedInputTypes();
        if(types != null && types.length > 0)
        {
            d.getAttributes().add(new DynamicProperty(ReferenceTypeRegistry.REFERENCE_TYPE_PD, String.class, types[0].toString()));
        }
    }

    public Diagram convert(Diagram diagram)
    {
        annotateDiagram( diagram );
        return diagram;
    }

    public String getElementTypeName()
    {
        return "molecule";
    }

    /**
     * get input names which can be used with given hub and given options
     * @param dbOptions
     * @param relationTypes
     * @return
     */
    public Set<String> getNames(TargetOptions dbOptions, String[] relationTypes)
    {
        return getHub( dbOptions, relationTypes ).startingNodes().map( Object::toString ).toSet();
    }

    @Override
    public ReferenceType[] getSupportedMatching(ReferenceType inputType)
    {
        return StreamEx.of( getSupportedInputTypes() ).findFirst( type -> inputType.getStableName().equals( type.getStableName() ) )
                .map( type -> new ReferenceType[] {type} ).orElse( null );
    }

    protected static String getSpeciesName(String[] relationTypes)
    {
        Species species = getSpecies( relationTypes );
        return species == null ? null : getSpeciesName( species );
    }

    protected static Species getSpecies(String[] relationTypes)
    {
        if(relationTypes != null && relationTypes.length > 0)
            return Species.getSpecies(relationTypes[0]);
        else
            return Species.getDefaultSpecies(null);
    }

    protected static String getSpeciesName(Species species)
    {
        return species.getCommonName().toLowerCase( Locale.ENGLISH );
    }

    public boolean isRelationSignSupported()
    {
        return false;
    }
}
