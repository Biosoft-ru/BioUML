package biouml.plugins.keynodes.graph;

import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;

import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.exception.Assert;
import biouml.model.DiagramType;
import biouml.model.Module;
import biouml.model.xml.XmlDiagramType;
import biouml.plugins.keynodes.biohub.KeyNodesHub;
import biouml.plugins.keynodes.graph.MemoryHubGraph.HubRelation;
import biouml.standard.type.Base;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;

public class CollectionBioHub extends KeyNodesHub<CollectionBioHub.HubElement>
{
    protected static final String REACTION_COLLECTION = "reaction";

    private final ElementConverter<HubElement> converter;

    private final MemoryHubCache<HubElement> hubCache;

    private final List<ru.biosoft.access.core.DataElementPath> collections;

    private HubElement convert(Element el)
    {
        String parent = el.getElementPath().getParentPath().getName();
        String name = el.getElementPath().getName();
        if(parent.isEmpty())
        {
            parent = StreamEx.of( collections ).findFirst( col -> col.getChildPath( name ).exists() ).map( DataElementPath::getName )
                    .orElse( "" );
        }
        return new HubElement( parent, name );
    }

    public CollectionBioHub(Properties properties)
    {
        super( properties );
        collections = getModulePath().getChildPath( Module.DATA ).getDataCollection().stream( ru.biosoft.access.core.DataCollection.class )
                .filter(dc -> dc.getDataElementType() == Reaction.class ||
                    ReferenceTypeRegistry.getElementReferenceType( dc ) != ReferenceTypeRegistry.getDefaultReferenceType() )
                .map( dc -> dc.getCompletePath() )
                .collect( Collectors.toList() );
        converter = ElementConverter.of( this::convert,
                n -> new Element( getModulePath().getChildPath( Module.DATA, n.collection, n.name ) ) );
        hubCache = new MemoryHubCache<>( spec -> readHub(), converter );
    }

    private static <T> BiPredicate<T, T> addReversed(BiPredicate<T, T> pred, boolean reversible)
    {
        return reversible ? pred.or( (a, b) -> pred.test( b, a ) ) : pred;
    }

    private static BiFunction<SpecieReference, SpecieReference, HubRelation<HubElement>> relationCreator(Reaction reaction, float weight)
    {
        return (reactant, product) -> new HubRelation<>( new HubElement( reactant ), new HubElement( product ), new CollectionRelation(
                reaction.getName(), reactant.getRole(), product.getRole() ), weight );
    }

    private static StreamEx<HubRelation<HubElement>> hubEdges(List<SpecieReference> srs)
    {
        if(srs.isEmpty())
            return StreamEx.empty();
        Reaction reaction = (Reaction)srs.get( 0 ).getOrigin();
        boolean reversible = reaction.isReversible();
        srs.stream().anyMatch( sr -> sr.getRole().equals( SpecieReference.MODIFIER ) );
        BiPredicate<SpecieReference, SpecieReference> isReactantProductEdge = (a, b) -> a.getRole().equals( SpecieReference.REACTANT ) && b.getRole().equals( SpecieReference.PRODUCT );
        BiPredicate<SpecieReference, SpecieReference> isModifierEdge = (a, b) ->
            a.getRole().equals( SpecieReference.REACTANT ) && b.getRole().equals( SpecieReference.MODIFIER ) ||
            a.getRole().equals( SpecieReference.MODIFIER ) && b.getRole().equals( SpecieReference.PRODUCT );

        StreamEx<HubRelation<HubElement>> rp = StreamEx.of( srs ).cross( srs )
                .filterKeyValue( addReversed( isReactantProductEdge, reversible ) ).mapKeyValue( relationCreator( reaction, 2 ) );
        StreamEx<HubRelation<HubElement>> rmp = StreamEx.of( srs ).cross( srs )
                .filterKeyValue( addReversed( isModifierEdge, reversible ) ).mapKeyValue( relationCreator( reaction, 1 ) );
        return rmp.append( rp );
    }

    private HubGraph<HubElement> readHub()
    {
        DataElementPath modulePath = getModulePath();
        DataCollection<Reaction> reactDC = modulePath.getChildPath( Module.DATA, REACTION_COLLECTION ).getDataCollection(
                Reaction.class );
        Set<String> stopList = modulePath.getChildPath( Module.DATA ).getDataCollection()
                .stream().filter( DataCollection.class::isInstance ).flatMap( dc -> ( (DataCollection<?>)dc ).stream() )
                .filter( Base.class::isInstance )
                .filter( base -> Boolean.TRUE.equals( ( (Base)base ).getAttributes().getValue( "StopMolecule" ) ) )
                .map( base -> ( (Base)base ).getCompletePath() )
                .map( path -> path.getPathDifference(modulePath) )
                .collect( Collectors.toSet() );

        return reactDC.stream()
                .filter( reaction -> ! ( Boolean.TRUE.equals( reaction.getAttributes().getValue( "StopReaction" ) ) ) )
                .map( reaction -> StreamEx.of( reaction.getSpecieReferences() ).remove( sr -> stopList.contains(sr.getSpecie( ))).toList() )
                .flatMap( CollectionBioHub::hubEdges )
                .collect( MemoryHubGraph.toMemoryHub( getHubCreator() ) );
    }

    protected Function<List<HubRelation<HubElement>>, ? extends MemoryHubGraph<HubElement>> getHubCreator()
    {
        return CollectionHubGraph::new;
    }

    protected static class CollectionHubGraph extends MemoryHubGraph<HubElement>
    {
        protected CollectionHubGraph(List<HubRelation<HubElement>> relations)
        {
            super( relations );
        }

        @Override
        public boolean isIntermediate(HubElement node)
        {
            // TODO: avoid hardcoding collection name
            return node.collection.equals( "substance" );
        }
    }

    public static class HubElement
    {
        final String collection, name;

        HubElement(SpecieReference sr)
        {
            this( DataElementPath.create( sr.getSpecie() ).getParentPath().getName(), DataElementPath.create( sr.getSpecie() ).getName() );
        }

        HubElement(String collection, String name)
        {
            this.collection = Assert.notNull( "collection", collection );
            this.name = Assert.notNull( "name", name );
        }

        @Override
        public boolean equals(Object obj)
        {
            if( this == obj )
                return true;
            if( obj == null || getClass() != obj.getClass() )
                return false;
            HubElement e = (HubElement)obj;
            return collection.equals( e.collection ) && name.equals( e.name );
        }

        @Override
        public int hashCode()
        {
            return collection.hashCode() * 31 + name.hashCode();
        }

        @Override
        public String toString()
        {
            return name;
        }

        public String getCollection()
        {
            return collection;
        }
    }

    public static class CollectionRelation extends HubElement implements HubEdge
    {
        private final String fromType;
        private final String toType;

        public CollectionRelation(String acc, String fromType, String toType)
        {
            super( REACTION_COLLECTION, acc );
            this.fromType = Assert.notNull( "type", fromType );
            this.toType = Assert.notNull( "type", toType );
        }

        @Override
        public int hashCode()
        {
            return Objects.hash( super.hashCode(), fromType, toType );
        }

        @Override
        public boolean equals(Object obj)
        {
            if( this == obj )
                return true;
            if( !super.equals( obj ) || getClass() != obj.getClass() )
                return false;
            return fromType.equals( ( (CollectionRelation)obj ).fromType ) && toType.equals( ( (CollectionRelation)obj ).toType );
        }

        @Override
        public Element createElement(KeyNodesHub<?> hub)
        {
            return new Element( hub.getModulePath().getChildPath( Module.DATA, collection, name ) );
        }

        @Override
        public String getRelationType(boolean upStream)
        {
            if( upStream )
            {
                return fromType;
            }
            else
            {
                return toType;
            }
        }
    }

    @Override
    protected HubGraph<HubElement> getHub(TargetOptions dbOptions, String[] relationTypes)
    {
        return hubCache.get( "hub", dbOptions );
    }

    @Override
    protected ElementConverter<HubElement> getElementConverter()
    {
        return converter;
    }

    @Override
    public String getElementTitle(Element element)
    {
        Base de = getCompleteElementPath( element.getAccession() ).optDataElement( Base.class );
        if( de != null )
            return de.getTitle();
        return element.getAccession();
    }

    @Override
    public DataElementPath getCompleteElementPath(String acc)
    {
        HubElement node = converter.toNode( new Element("stub/%//"+acc) );
        return getModulePath().getChildPath( Module.DATA, node.collection, node.name );
    }

    @Override
    public int getPriority(TargetOptions dbOptions)
    {
        DataElementPathSet dbList = dbOptions.getUsedCollectionPaths();
        if( dbList.size() > 0 )
        {
            for( DataElementPath cr : dbList )
            {
                if( !cr.isDescendantOf( KEY_NODES_HUB ) && !cr.isDescendantOf( getModulePath() ) )
                {
                    return 0;
                }
            }
            return 20;
        }
        return 0;
    }

    @Override
    public ReferenceType[] getSupportedInputTypes()
    {
        return getModulePath().getChildPath( Module.DATA ).getDataCollection().stream( ru.biosoft.access.core.DataCollection.class )
                .map( ReferenceTypeRegistry::getElementReferenceType )
                .filter( type -> ! ( type.equals( ReferenceTypeRegistry.getDefaultReferenceType() ) ) )
                .distinct().toArray( ReferenceType[]::new );
    }

    @Override
    public DiagramType getVisualizationDiagramType()
    {
        if(properties.getProperty( "diagramType" ) != null)
            return XmlDiagramType.getTypeObject( properties.getProperty( "diagramType" ) );
        return super.getVisualizationDiagramType();
    }
}
