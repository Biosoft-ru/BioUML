package biouml.plugins.keynodes.customhub;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Module;
import biouml.model.Node;
import biouml.plugins.lucene.LuceneUtils;
import biouml.standard.type.Concept;
import biouml.standard.type.DatabaseReference;
import biouml.standard.type.Protein;
import biouml.standard.type.Reaction;
import biouml.standard.type.Referrer;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Substance;
import biouml.standard.type.Type;
import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.ProtectedElement;
import ru.biosoft.access.Repository;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DerivedDataCollection;
import ru.biosoft.access.core.QuerySystem;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.util.bean.BeanInfoEx2;

public class DiagramToUserHubCollection extends AnalysisMethodSupport<DiagramToUserHubCollection.Parameters>
{
    public DiagramToUserHubCollection(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        super.validateParameters();
        checkPaths();

        Repository repo = getParentRepository( parameters.getOutputRepository() );
        if( repo == null )
            throw new IllegalArgumentException( "Incorrect parent repository selected" );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        log.info( "Collection initialization..." );
        jobControl.pushProgress( 0, 1 );
        Module module = createHubModule();
        jobControl.popProgress();

        Diagram d = parameters.getInputDiagram().getDataElement( Diagram.class );
        List<Node> nodes = d.recursiveStream().without( d ).select( Node.class )
                .remove( de -> de.getCompartment().getKernel().getType().equals( "complex" ) )
                .remove( DiagramToUserHubCollection::isReaction ).toList();

        Map<String, Referrer> referrers = new HashMap<>();

        log.info( "Processing nodes..." );
        jobControl.pushProgress( 1, 75 );
        jobControl.forCollection( nodes, de -> {
            String kernelType = de.getKernel().getType();
            Referrer element = null;
            String name = de.getKernel().getName();
            switch( kernelType )
            {
                case "compartment":
                    element = createElement( module, de, biouml.standard.type.Compartment.class, name );
                    break;
                case "molecule-substance":
                case "complex":
                case "simple chemical":
                case "unspecified":
                case "nucleic acid feature":
                case "perturbing agent":
                    element = createElement( module, de, Substance.class, name );
                    break;
                case "macromolecule":
                    element = createElement( module, de, Protein.class, name );
                    break;
                case "source-sink":
                    break;
                default:
                    log.warning( "Unknown element: " + de.getCompleteNameInDiagram() + " (type: " + kernelType + ")" );
            }
            if( element != null )
            {
                Referrer existing = element.getCompletePath().optDataElement( Referrer.class );
                if( existing != null )
                {
                    addArrayElement( existing.getAttributes(), "compartments", de.getCompartment().getTitle() );
                    existing.addDatabaseReferences( element.getDatabaseReferences() );
                    referrers.put( de.getCompleteNameInDiagram(), existing );
                }
                else
                {
                    addArrayElement( element.getAttributes(), "compartments", de.getCompartment().getTitle() );
                    referrers.put( de.getCompleteNameInDiagram(), element );
                    CollectionFactoryUtils.save( element );
                }
            }
            return true;
        } );
        jobControl.popProgress();

        log.info( "Processing reactions..." );
        jobControl.pushProgress( 75, 85 );
        List<Node> reactions = d.recursiveStream().select( Node.class )
                .filter( n -> n.getKernel() != null && n.getKernel().getType().equals( "reaction" ) ).toList();
        reactions.addAll( createRelationReactions( d ) );
        jobControl.popProgress();

        jobControl.pushProgress( 80, 85 );
        jobControl.forCollection( reactions, de -> {
            Reaction r = createElement( module, de, Reaction.class, de.getName() );
            Reaction src = de.getKernel().cast( Reaction.class );
            r.setReversible( src.isReversible() );
            for( SpecieReference sr : src.getSpecieReferences() )
            {
                SpecieReference newSr = new SpecieReference( r, sr.getName() );
                Referrer ref = referrers.get( sr.getSpecie() );
                if( ref == null )
                {
                    log.warning( "Unable to find specie " + sr.getSpecie() + " for reaction " + de.getCompleteNameInDiagram() );
                    continue;
                }
                newSr.setSpecie( ref.getCompletePath().getPathDifference( module.getCompletePath() ) );
                newSr.setRole( sr.getRole() );
                newSr.setStoichiometry( sr.getStoichiometry() );
                r.put( newSr );
            }
            CollectionFactoryUtils.save( r );
            return true;
        } );
        jobControl.popProgress();

        log.info( "Cleanup" );
        jobControl.pushProgress( 85, 90 );
        ( (DataCollection<?>)module.get( Module.DATA ) ).stream().collect( Collectors.toList() ).forEach( de -> cleanup( de, true ) );

        jobControl.popProgress();
        log.info( "Build lucene indexes" );
        jobControl.pushProgress( 90, 100 );
        LuceneUtils.buildIndexes( module, jobControl, log );
        jobControl.popProgress();

        return module;
    }

    private List<Node> createRelationReactions(Diagram d)
    {
        List<Node> reactions = new ArrayList<>();
        List<Edge> relationEdges = d.recursiveStream().select( Edge.class )
                .remove( e -> isReaction( e.getInput() ) || isReaction( e.getOutput() ) )
                .filter( e -> Type.TYPE_SEMANTIC_RELATION.equals( e.getKernel().getType() ) ).toList();
        int incr = 0;
        for( Edge relationEdge : relationEdges )
        {
            Node reactionNode = createRelationReaction( relationEdge, incr++ );
            reactions.add( reactionNode );
        }
        return reactions;
    }

    private Node createRelationReaction(Edge relationEdge, int number)
    {
        Node input = relationEdge.getInput();
        Node output = relationEdge.getOutput();
        String inTitle = input.getTitle() == null ? input.getName() : input.getTitle();
        String outTitle = output.getTitle() == null ? output.getName() : output.getTitle();
        String title = inTitle + " -> " + outTitle;
        Reaction relationReaction = new Reaction( null, "Relation_reaction_" + number );
        relationReaction.setTitle( title );

        SpecieReference sr = createSpecieReference( relationReaction, input, SpecieReference.MODIFIER );
        relationReaction.put( sr );
        sr = createSpecieReference( relationReaction, output, SpecieReference.PRODUCT );
        relationReaction.put( sr );

        Node reactionNode = new Node( null, relationReaction );
        reactionNode.setTitle( title );
        return reactionNode;
    }

    private SpecieReference createSpecieReference(Reaction parent, Node specie, String role)
    {
        SpecieReference sr = new SpecieReference( parent, parent.getName(), specie.getName(), role );
        sr.setSpecie( specie.getCompleteNameInDiagram() );
        return sr;
    }

    private static boolean isReaction(Node node)
    {
        return node.getKernel() != null && node.getKernel().getType().equals( "reaction" );
    }

    private void cleanup(DataElement de, boolean addLucene)
    {
        if( de instanceof DataCollection )
        {
            DataCollection<?> dc = (DataCollection<?>)de;
            if( dc.isEmpty() )
                dc.getCompletePath().remove();
            else
            {
                if( addLucene )
                    dc.getInfo().getProperties().setProperty( "lucene-indexes", "name;title;completeName;synonyms;description;comment" );
                CollectionFactoryUtils.save( de );
            }
        }
    }

    private void addArrayElement(DynamicPropertySet dps, String propertyName, String element)
    {
        String[] oldValue = (String[])dps.getValue( propertyName );
        if( oldValue == null )
        {
            dps.add( new DynamicProperty( propertyName, String[].class, new String[] {element} ) );
        }
        else if( !Arrays.asList( oldValue ).contains( element ) )
        {
            dps.setValue( propertyName, StreamEx.of( oldValue ).append( element ).toArray( String[]::new ) );
        }
    }

    private static <T extends Referrer> T createElement(Module module, DiagramElement de, Class<T> clazz, String title)
    {
        T base;
        try
        {
            base = clazz.getConstructor( ru.biosoft.access.core.DataCollection.class, String.class ).newInstance( module.getCategory( clazz ), title );
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException( e );
        }
        base.setTitle( de.getTitle() );
        base.setComment( de.getComment() );
        if( de.getKernel() instanceof Referrer )
        {
            Referrer ref = (Referrer)de.getKernel();
            DatabaseReference[] dr = ref.getDatabaseReferences();
            if( dr != null )
            {
                if( base instanceof Concept )
                    ( (Concept)base )
                            .setSynonyms( StreamEx.of( dr ).flatMap( r -> Stream.of( r.getAc(), r.getId() ) ).distinct().joining( ", " ) );
                base.setDatabaseReferences( dr.clone() );
            }
        }
        return base;
    }

    private Module createHubModule() throws Exception
    {
        Repository repository = getParentRepository( parameters.getOutputRepository() );
        String dbName = parameters.getHubCollectionName();
        if( repository.contains( dbName ) )
            repository.remove( dbName );

        UserHubModuleType moduleType = new UserHubModuleType();
        Module module = moduleType.createModule( repository, dbName );
        module.getInfo().getProperties().setProperty( QuerySystem.QUERY_SYSTEM_CLASS, "biouml.plugins.lucene.LuceneQuerySystemImpl" );
        module.getInfo().getProperties().setProperty( "data-collection-listener", "biouml.plugins.lucene.LuceneInitListener" );
        module.getInfo().getProperties().setProperty( "lucene-directory", "luceneIndex" );
        module.getInfo().getProperties().setProperty( "graph-search", "true" );

        module.getInfo().getProperties().setProperty( "bioHub.search",
                "biouml.plugins.keynodes.graph.CollectionBioHub;name=" + dbName + parameters.getDiagramTypeStr() );

        module.getCategory( Protein.class ).getInfo().getProperties().setProperty( ReferenceTypeRegistry.REFERENCE_TYPE_PROPERTY,
                "Proteins" );
        module.getCategory( Substance.class ).getInfo().getProperties().setProperty( ReferenceTypeRegistry.REFERENCE_TYPE_PROPERTY,
                "Substances" );

        CollectionFactoryUtils.save( module );

        return module;
    }

    private static Repository getParentRepository(DataElementPath startPath)
    {
        Repository outRepo;
        DataElement de = startPath.optDataElement();
        if( de instanceof Repository )
            outRepo = (Repository)de;
        else
        {
            while( de != null && ! ( de instanceof Repository ) )
            {
                if( de instanceof DerivedDataCollection<?, ?> )
                {
                    final DerivedDataCollection<?, ?> dc = (DerivedDataCollection<?, ?>)de;
                    if( dc instanceof ProtectedElement )
                    {
                        try
                        {
                            de = (DataCollection<?>)SecurityManager.runPrivileged( () -> {
                                return DataCollectionUtils.fetchPrimaryCollectionPrivileged( dc );
                            } );
                        }
                        catch( Exception e )
                        {
                            de = de.getOrigin();
                        }
                    }
                    else
                        de = dc.getPrimaryCollection();
                }
                else
                    de = de.getOrigin();
            }
            outRepo = (Repository)de;
        }
        return outRepo;
    }

    @SuppressWarnings ( "serial" )
    @PropertyName ( "Parameters" )
    @PropertyDescription ( "Parameters of diagram to hub collection" )
    public static class Parameters extends AbstractAnalysisParameters
    {
        public static final String METABOLICS_TYPE = "Metabolics (kegg-like)";
        public static final String SBGN_TYPE = "SBGN";

        public static final String[] DIAGRAM_TYPES = new String[] {SBGN_TYPE, METABOLICS_TYPE};

        DataElementPath inputDiagram;
        String diagramType = SBGN_TYPE;
        DataElementPath outputRepository;
        String hubCollectionName;
        //id field //TODO
        //stop molecules list //TODO

        @PropertyName ( "Input diagram" )
        @PropertyDescription ( "Path to the input diagram" )
        public DataElementPath getInputDiagram()
        {
            return inputDiagram;
        }
        public void setInputDiagram(DataElementPath inputDiagram)
        {
            Object oldValue = this.inputDiagram;
            this.inputDiagram = inputDiagram;
            firePropertyChange( "inputDiagram", oldValue, inputDiagram );
            if( outputRepository == null )
            {
                Repository repo = DiagramToUserHubCollection.getParentRepository( inputDiagram );
                if( repo != null )
                    setOutputRepository( repo.getCompletePath() );
            }
        }

        @PropertyName ( "Parent repository" )
        @PropertyDescription ( "Repository to store new collection" )
        public DataElementPath getOutputRepository()
        {
            return outputRepository;
        }
        public void setOutputRepository(DataElementPath outputRepository)
        {
            Object oldValue = this.outputRepository;
            this.outputRepository = outputRepository;
            firePropertyChange( "outputRepository", oldValue, outputRepository );
        }

        public String getDiagramTypeStr()
        {
            switch( diagramType )
            {
                case METABOLICS_TYPE:
                    return ";diagramType=kegg_recon.xml";
                case SBGN_TYPE:
                default:
                    return "";
            }
        }

        @PropertyName ( "Hub visualization type" )
        @PropertyDescription ( "Diagram visualization type of new hub" )
        public String getDiagramType()
        {
            return diagramType;
        }
        public void setDiagramType(String diagramType)
        {
            Object oldValue = this.diagramType;
            this.diagramType = diagramType;
            firePropertyChange( "diagramType", oldValue, diagramType );
        }

        @PropertyName ( "Collection name" )
        @PropertyDescription ( "Name of new collection" )
        public String getHubCollectionName()
        {
            return hubCollectionName;
        }
        public void setHubCollectionName(String hubCollectionName)
        {
            Object oldValue = this.hubCollectionName;
            this.hubCollectionName = hubCollectionName;
            firePropertyChange( "hubCollectionName", oldValue, hubCollectionName );
        }
    }

    public static class ParametersBeanInfo extends BeanInfoEx2<Parameters>
    {
        public ParametersBeanInfo()
        {
            super( Parameters.class );
        }
        @Override
        protected void initProperties() throws Exception
        {
            super.initProperties();

            property( "inputDiagram" ).inputElement( Diagram.class ).add();
            //            property( "outputRepository" ).outputElement( Module.class ).add();
            property( "outputRepository" ).value( DataElementPathEditor.ELEMENT_CLASS, Module.class ).simple().add();
            addWithTags( "diagramType", Parameters.DIAGRAM_TYPES );
            property( "hubCollectionName" ).auto( "$inputDiagram/name$ hub collection" ).add();
        }
    }
}
