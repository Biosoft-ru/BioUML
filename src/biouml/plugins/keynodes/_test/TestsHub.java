package biouml.plugins.keynodes._test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import biouml.plugins.keynodes.biohub.KeyNodesHub;
import biouml.plugins.keynodes.graph.ElementConverter;
import biouml.plugins.keynodes.graph.HubEdge;
import biouml.plugins.keynodes.graph.HubGraph;
import biouml.plugins.keynodes.graph.MemoryHubCache;
import biouml.plugins.keynodes.graph.MemoryHubGraph;
import biouml.plugins.keynodes.graph.MemoryHubGraph.HubRelation;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Substance;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.biohub.TargetOptions;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.access.exception.Assert;

public class TestsHub extends KeyNodesHub<String>
{
    private static final String SUBSTANCES_DC = "substances";
    private static final String REACTION_DC = "reactions";
    public static final String PRODUCT = SpecieReference.PRODUCT;
    public static final String REACTANT = SpecieReference.REACTANT;
    public static final String MODIFIER = SpecieReference.MODIFIER;
    private final ElementConverter<String> converter;
    private final MemoryHubCache<String> hubCache;
    public TestsHub(Properties properties)
    {
        super( properties );
        initRelations();

        converter = ElementConverter.of( Element::getAccession, s -> new Element( getCompleteElementPath( s ) ) );
        hubCache = new MemoryHubCache<>( spec -> createHub(), converter );
    }

    public void initCollections() throws Exception
    {
        VectorDataCollection<Substance> substances = new VectorDataCollection<>( SUBSTANCES_DC, Substance.class, null );
        createHub().nodes().map( name -> new Substance( substances, name ) ).forEach( substances::put );
        CollectionFactory.registerRoot( substances );

        VectorDataCollection<Reaction> reactions = new VectorDataCollection<>( REACTION_DC, Reaction.class, null );
        createReactions( reactions ).forEach( reactions::put );
        CollectionFactory.registerRoot( reactions );
    }
    private Collection<Reaction> createReactions(VectorDataCollection<Reaction> origin) throws Exception
    {
        Map<String, Reaction> rMap = new HashMap<>();
        for( HubRelation<String> hr : relations )
        {
            HubEdge hrEdge = hr.getEdge();
            String name = hrEdge.createElement( this ).getAccession();
            Reaction reaction = rMap.get( name );
            if( reaction == null )
            {
                reaction = new Reaction( origin, name );
                rMap.put( name, reaction );
            }
            //add SpecieReference for 'from' element
            String species = hr.getStart();
            String relation = hrEdge.getRelationType( true );
            String srName = name + ": " + species + " as " + relation;
            if( reaction.get( srName ) == null )
            {
                SpecieReference sr = new SpecieReference( reaction, srName, relation );
                sr.setSpecie( species );
                reaction.put( sr );
            }
            //add SpecieReference for 'to' element
            species = hr.getEnd();
            relation = hrEdge.getRelationType( false );
            srName = name + ": " + species + " as " + relation;
            if( reaction.get( srName ) == null )
            {
                SpecieReference sr = new SpecieReference( reaction, srName, relation );
                sr.setSpecie( species );
                reaction.put( sr );
            }
        }
        return rMap.values();
    }

    private final List<HubRelation<String>> relations = new ArrayList<>();
    /**
     * Creates relations for 2 separated graphs.
     */
    private void initRelations()
    {
        // 1) creates connected graph
        // irreversible reactions
        relations.add( new HubRelation<>( "E01", "E02", new TestRelation( "X01", REACTANT, PRODUCT ), 2 ) );
        relations.add( new HubRelation<>( "E01", "E03", new TestRelation( "X02", REACTANT, PRODUCT ), 2 ) );
        // reversible reaction
        relations.add( new HubRelation<>( "E01", "E06", new TestRelation( "X03", REACTANT, PRODUCT ), 2 ) );
        relations.add( new HubRelation<>( "E06", "E01", new TestRelation( "X03", PRODUCT, REACTANT ), 2 ) );
        // irreversible reactions
        relations.add( new HubRelation<>( "E02", "E03", new TestRelation( "X04", REACTANT, PRODUCT ), 2 ) );
        relations.add( new HubRelation<>( "E02", "E04", new TestRelation( "X05", REACTANT, PRODUCT ), 2 ) );
        // irreversible reactions
        relations.add( new HubRelation<>( "E03", "E04", new TestRelation( "X06", REACTANT, PRODUCT ), 2 ) );
        relations.add( new HubRelation<>( "E03", "E06", new TestRelation( "X07", REACTANT, PRODUCT ), 2 ) );
        // reversible reaction
        relations.add( new HubRelation<>( "E04", "E05", new TestRelation( "X08", REACTANT, PRODUCT ), 2 ) );
        relations.add( new HubRelation<>( "E05", "E04", new TestRelation( "X08", PRODUCT, REACTANT ), 2 ) );
        // reversible reaction
        relations.add( new HubRelation<>( "E05", "E06", new TestRelation( "X09", REACTANT, PRODUCT ), 2 ) );
        relations.add( new HubRelation<>( "E06", "E05", new TestRelation( "X09", PRODUCT, REACTANT ), 2 ) );

        // 2) creates three simple reactions with modifiers
        // irreversible reaction
        relations.add( new HubRelation<>( "E07", "E09", new TestRelation( "X10", REACTANT, MODIFIER ), 1 ) );
        relations.add( new HubRelation<>( "E07", "E10", new TestRelation( "X10", REACTANT, MODIFIER ), 1 ) );
        relations.add( new HubRelation<>( "E08", "E09", new TestRelation( "X10", REACTANT, MODIFIER ), 1 ) );
        relations.add( new HubRelation<>( "E08", "E10", new TestRelation( "X10", REACTANT, MODIFIER ), 1 ) );
        relations.add( new HubRelation<>( "E09", "E11", new TestRelation( "X10", MODIFIER, PRODUCT ), 1 ) );
        relations.add( new HubRelation<>( "E10", "E11", new TestRelation( "X10", MODIFIER, PRODUCT ), 1 ) );
        relations.add( new HubRelation<>( "E09", "E12", new TestRelation( "X10", MODIFIER, PRODUCT ), 1 ) );
        relations.add( new HubRelation<>( "E10", "E12", new TestRelation( "X10", MODIFIER, PRODUCT ), 1 ) );
        // reversible reaction
        relations.add( new HubRelation<>( "E11", "E13", new TestRelation( "X11", REACTANT, MODIFIER ), 1 ) );
        relations.add( new HubRelation<>( "E13", "E11", new TestRelation( "X11", MODIFIER, REACTANT ), 1 ) );
        relations.add( new HubRelation<>( "E12", "E13", new TestRelation( "X11", REACTANT, MODIFIER ), 1 ) );
        relations.add( new HubRelation<>( "E13", "E12", new TestRelation( "X11", MODIFIER, REACTANT ), 1 ) );
        relations.add( new HubRelation<>( "E13", "E14", new TestRelation( "X11", MODIFIER, PRODUCT ), 1 ) );
        relations.add( new HubRelation<>( "E14", "E13", new TestRelation( "X11", PRODUCT, MODIFIER ), 1 ) );
    }

    private HubGraph<String> createHub()
    {
        return relations.stream().collect( MemoryHubGraph.toMemoryHub() );
    }

    @Override
    protected HubGraph<String> getHub(TargetOptions dbOptions, String[] relationTypes)
    {
        return hubCache.get( "hub", dbOptions );
    }

    @Override
    protected ElementConverter<String> getElementConverter()
    {
        return converter;
    }

    @Override
    public String getElementTitle(Element element)
    {
        return element.getAccession();
    }

    @Override
    public DataElementPath getCompleteElementPath(String acc)
    {
        return acc.startsWith( "X" ) ? DataElementPath.create( REACTION_DC, acc ) : DataElementPath.create( SUBSTANCES_DC, acc );
    }

    @Override
    public String toString()
    {
        return "Special hub for tests";
    }

    @Override
    public ReferenceType[] getSupportedInputTypes()
    {
        return new ReferenceType[] {ReferenceTypeRegistry.getDefaultReferenceType()};
    }

    public static class TestRelation implements HubEdge
    {
        private final String fromType;
        private final String toType;
        private final String acc;

        public TestRelation(String acc, String fromType, String toType)
        {
            this.acc = Assert.notNull( "acc", acc );
            this.fromType = Assert.notNull( "type", fromType );
            this.toType = Assert.notNull( "type", toType );
        }

        @Override
        public int hashCode()
        {
            return Objects.hash( acc, fromType, toType );
        }

        @Override
        public boolean equals(Object obj)
        {
            if( obj == null )
                return false;
            if( this == obj )
                return true;
            else if( getClass() != obj.getClass() )
                return false;
            return acc.equals( ( (TestRelation)obj ).acc ) && fromType.equals( ( (TestRelation)obj ).fromType )
                    && toType.equals( ( (TestRelation)obj ).toType );
        }

        @Override
        public Element createElement(KeyNodesHub<?> hub)
        {
            return new Element( DataElementPath.create( REACTION_DC, acc ) );
        }

        @Override
        public String getRelationType(boolean upStream)
        {
            return upStream ? fromType : toType;
        }
    }

}
