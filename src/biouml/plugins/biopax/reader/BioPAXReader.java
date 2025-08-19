
package biouml.plugins.biopax.reader;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.semanticweb.owl.apibinding.OWLManager;
import org.semanticweb.owl.model.OWLConstant;
import org.semanticweb.owl.model.OWLDataFactory;
import org.semanticweb.owl.model.OWLDescription;
import org.semanticweb.owl.model.OWLIndividual;
import org.semanticweb.owl.model.OWLObjectPropertyExpression;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyManager;
import org.semanticweb.owl.model.OWLOntologyURIMapper;
import org.semanticweb.owl.model.OWLTypedConstant;
import org.semanticweb.owl.util.SimpleURIMapper;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramType;
import biouml.model.Module;
import biouml.model.Node;
import biouml.model.util.DiagramImageGenerator;
import biouml.plugins.biopax.BioPAXQuerySystem;
import biouml.plugins.biopax.BioPAXSupport;
import biouml.plugins.biopax.access.BioPaxOwlDataCollection.BioPAXCollectionJobControl;
import biouml.plugins.biopax.biohub.BioHubBuilder;
import biouml.plugins.biopax.model.BioSource;
import biouml.plugins.biopax.model.EntityFeature;
import biouml.plugins.biopax.model.OpenControlledVocabulary;
import biouml.plugins.sbgn.SbgnDiagramType;
import biouml.standard.type.Complex;
import biouml.standard.type.Concept;
import biouml.standard.type.DNA;
import biouml.standard.type.DatabaseInfo;
import biouml.standard.type.DatabaseReference;
import biouml.standard.type.Protein;
import biouml.standard.type.Publication;
import biouml.standard.type.RNA;
import biouml.standard.type.Reaction;
import biouml.standard.type.SemanticRelation;
import biouml.standard.type.Specie;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Substance;
import biouml.workbench.graph.DiagramToGraphTransformer;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.InternalException;
import ru.biosoft.graph.Graph;
import ru.biosoft.graph.HierarchicLayouter;
import ru.biosoft.graph.PathwayLayouter;
import ru.biosoft.graphics.ComplexTextView;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.View;
import ru.biosoft.util.ApplicationUtils;
import uk.ac.manchester.cs.owl.OWLObjectPropertyImpl;

/**
 * @author anna
 */
public abstract class BioPAXReader extends BioPAXSupport
{
    private static final Logger log = Logger.getLogger(BioPAXReader.class.getName());
    protected DataCollection module;

    protected DataCollection<DataCollection> data;
    protected DataCollection<Diagram> diagrams;
    protected DataCollection<DataCollection> dictionaries;

    //internal collections
    protected DataCollection<Concept> physicalEntities;
    protected DataCollection<Complex> complexes;
    protected DataCollection<Protein> proteins;
    protected DataCollection<RNA> rnas;
    protected DataCollection<DNA> dnas;
    protected DataCollection<Substance> smallMolecules;
    protected DataCollection<Reaction> conversions;
    protected DataCollection<SemanticRelation> controls;
    protected DataCollection<SpecieReference> participants;
    protected DataCollection<Publication> publications;
    protected DataCollection<OpenControlledVocabulary> vocabulary;
    protected DataCollection<DatabaseInfo> dataSources;
    protected DataCollection<BioSource> organisms;
    protected DataCollection<EntityFeature> entityFeature;
    
    protected OWLDataFactory factory;
    protected OWLOntology ontology;
    protected String biopaxPrefix;

    protected BioHubBuilder bioHubBuilder = null;

    private static final String BIOPAX_URL_PREFIX = "http://www.biopax.org/release/";
    private static String[] biopaxLevelNames = new String[] {"biopax-level1.owl", "biopax-level2.owl", "biopax-level3.owl"};

    public BioPAXReader(OWLOntology ontology)
    {
        this.ontology = ontology;
        OWLOntologyManager manager = getOWLOntologyManager();
        factory = manager.getOWLDataFactory();
        biopaxPrefix = BioPAXReaderFactory.getBioPAXVersion(ontology);
    }

    public abstract boolean read(String namePrefix, BioPAXCollectionJobControl jobControl) throws Exception;

    public static OWLOntologyManager getOWLOntologyManager()
    {

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

        try
        {
            for( String levelName : biopaxLevelNames )
            {
                OWLOntologyURIMapper mapper = new SimpleURIMapper(URI.create(BIOPAX_URL_PREFIX + levelName),
                        ApplicationUtils.getResourceURL("biouml.plugins.biopax", levelName).toURI());
                manager.addURIMapper(mapper);
            }
        }
        catch( Exception e )
        {
        }

        return manager;
    }

    public void setCollections(DataCollection<DataCollection> data, DataCollection<Diagram> diagrams, DataCollection<DataCollection> dictionaries)
    {
        this.data = data;
        this.diagrams = diagrams;
        this.dictionaries = dictionaries;
        setInternalCollections();
        module = data.getOrigin();
    }

    protected void setInternalCollections()
    {
        try
        {
            if( data.contains(PHYSICAL_ENTITY) )
                physicalEntities = data.get(PHYSICAL_ENTITY);
            else
            {
                physicalEntities = new VectorDataCollection<>(PHYSICAL_ENTITY, data, null);
                physicalEntities.getInfo().setQuerySystem(new BioPAXQuerySystem(physicalEntities));
                data.put(physicalEntities);
            }
            if( data.contains(COMPLEX) )
                complexes = data.get(COMPLEX);
            else
            {
                complexes = new VectorDataCollection<>(COMPLEX, data, null);
                complexes.getInfo().setQuerySystem(new BioPAXQuerySystem(complexes));
                data.put(complexes);
            }
            if( data.contains(PROTEIN) )
                proteins = data.get(PROTEIN);
            else
            {
                proteins = new VectorDataCollection<>(PROTEIN, data, null);
                proteins.getInfo().setQuerySystem(new BioPAXQuerySystem(proteins));
                data.put(proteins);
            }
            if( data.contains(RNA) )
                rnas = data.get(RNA);
            else
            {
                rnas = new VectorDataCollection<>(RNA, data, null);
                rnas.getInfo().setQuerySystem(new BioPAXQuerySystem(rnas));
                data.put(rnas);
            }
            if( data.contains(DNA) )
                dnas = data.get(DNA);
            else
            {
                dnas = new VectorDataCollection<>(DNA, data, null);
                dnas.getInfo().setQuerySystem(new BioPAXQuerySystem(dnas));
                data.put(dnas);
            }
            if( data.contains(SMALL_MOLECULE) )
                smallMolecules = data.get(SMALL_MOLECULE);
            else
            {
                smallMolecules = new VectorDataCollection<>(SMALL_MOLECULE, data, null);
                smallMolecules.getInfo().setQuerySystem(new BioPAXQuerySystem(smallMolecules));
                data.put(smallMolecules);
            }
            if( data.contains(CONVERSION) )
                conversions = data.get(CONVERSION);
            else
            {
                conversions = new VectorDataCollection<>(CONVERSION, data, null);
                conversions.getInfo().setQuerySystem(new BioPAXQuerySystem(conversions));
                data.put(conversions);
            }
            if( data.contains(CONTROL) )
                controls = data.get(CONTROL);
            else
            {
                controls = new VectorDataCollection<>(CONTROL, data, null);
                controls.getInfo().setQuerySystem(new BioPAXQuerySystem(controls));
                data.put(controls);
            }
            if( data.contains(PARTICIPANT) )
                participants = data.get(PARTICIPANT);
            else
            {
                participants = new VectorDataCollection<>(PARTICIPANT, data, null);
                participants.getInfo().setQuerySystem(new BioPAXQuerySystem(participants));
                data.put(participants);
            }
            if( data.contains(PUBLICATION) )
                publications = data.get(PUBLICATION);
            else
            {
                publications = new VectorDataCollection<>(PUBLICATION, data, null);
                publications.getInfo().setQuerySystem(new BioPAXQuerySystem(publications));
                data.put(publications);
            }
            if( dictionaries.contains(VOCABULARY) )
                vocabulary = dictionaries.get(VOCABULARY);
            else
            {
                vocabulary = new VectorDataCollection<>(VOCABULARY, dictionaries, null);
                vocabulary.getInfo().setQuerySystem(new BioPAXQuerySystem(vocabulary));
                dictionaries.put(vocabulary);
            }
            if( dictionaries.contains(DATA_SOURCE) )
                dataSources = dictionaries.get(DATA_SOURCE);
            else
            {
                dataSources = new VectorDataCollection<>(DATA_SOURCE, dictionaries, null);
                dataSources.getInfo().setQuerySystem(new BioPAXQuerySystem(dataSources));
                dictionaries.put(dataSources);
            }
            if( dictionaries.contains(ORGANISM) )
                organisms = dictionaries.get(ORGANISM);
            else
            {
                organisms = new VectorDataCollection<>(ORGANISM, dictionaries, null);
                organisms.getInfo().setQuerySystem(new BioPAXQuerySystem(organisms));
                dictionaries.put(organisms);
            }
            if( data.contains(ENTITY_FEATURE) )
                entityFeature = data.get(ENTITY_FEATURE);
            else
            {
                entityFeature = new VectorDataCollection<>(ENTITY_FEATURE, data, null);
                entityFeature.getInfo().setQuerySystem(new BioPAXQuerySystem(entityFeature));
                data.put(entityFeature);
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not initialize collections fro BioPAX reader", e);
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // get elements by full name
    //
    protected DataElement getDataElementByName(String name)
    {
        DataElement de = module != null ? DataElementPath.create(module).getRelativePath(name).optDataElement() : null;
        //inner collections might be non-repository VectorDataCollections
        if( de == null )
        {
            DataCollection parentDC = null;
            String relativeName = null;
            if( name.startsWith(Module.DATA) )
            {
                parentDC = data;
                relativeName = name.substring( ( Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR ).length());
            }
            else if( name.startsWith(Module.DIAGRAM) )
            {
                parentDC = diagrams;
                relativeName = name.substring( ( Module.DIAGRAM + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR ).length());
            }
            else if( name.startsWith(Module.METADATA) )
            {
                parentDC = dictionaries;
                relativeName = name.substring( ( Module.METADATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR ).length());
            }
            if( parentDC != null && relativeName != null )
                de = CollectionFactory.getDataElement(relativeName, parentDC);
        }
        return de;
    }

    //Diagram type
    protected DiagramType getDiagramType()
    {
        return new SbgnDiagramType();
    }

    /**
     * Arrange all diagram elements
     */
    public static void arrangeDiagram(Diagram diagram)
    {
        Graphics g = com.developmentontheedge.application.ApplicationUtils.getGraphics();
        diagram.setView(null);
        diagram.getType().getDiagramViewBuilder().createDiagramView(diagram, g);
        arrangeElements(diagram.iterator());
        diagram.setView(null);
        diagram.getType().getDiagramViewBuilder().createDiagramView(diagram, g);

        diagram.setView(null);
        DiagramImageGenerator.generateDiagramView( diagram, g );
        diagram.setView(null);
        HierarchicLayouter layouter = new HierarchicLayouter();
        layouter.setVerticalOrientation(true);
        Graph graph = DiagramToGraphTransformer.generateGraph(diagram, null);
        PathwayLayouter pathwayLayouter = new PathwayLayouter(layouter);
        pathwayLayouter.doLayout(graph, null);
        DiagramToGraphTransformer.applyLayout(graph, diagram);
    }

    /**
     * Arrange elements in compartment
     */
    protected static void arrangeElements(Iterator<DiagramElement> iter)
    {
        while( iter.hasNext() )
        {
            Object element = iter.next();
            if( element instanceof Compartment )
            {
                Compartment node = (Compartment)element;
                
                if (node.getKernel().getType().equals( "complex" ))
                {
                    arrangeComplex(node);
                }
                else if (node.getKernel() instanceof Specie)
                {
                    arrangeEntity(node);
                }
                else
                {
                    arrangeElements(node.iterator());
                } 
            }
            else if( element instanceof Node )
            {
                Node node = (Node)element;
                if (node.getKernel() instanceof Reaction)
                {
                    arrangeReaction(node);
                }

            }
        }
    }

    /**
     * Arrange entity node
     */
    protected static void arrangeEntity(Node node)
    {
        CompositeView view = (CompositeView)node.getView();
        Rectangle labelBounds = new Rectangle(0, 0, 30, 20);
        int maxLabelWidth = 0;
        int stateCount = 0;
        for( View childView : view )
        {
            if( childView instanceof ComplexTextView )
            {
                labelBounds = childView.getBounds();
            }
            else if( childView instanceof CompositeView )
            {
                Rectangle bounds = childView.getBounds();
                if( bounds.width > maxLabelWidth )
                {
                    maxLabelWidth = bounds.width;
                }
                stateCount++;
            }
        }
        int height = labelBounds.height;
        if( height < ( stateCount * 20 ) )
        {
            height = stateCount * 20;
        }
        int labelBonus = 0;
        if( maxLabelWidth > 10 )
        {
            labelBonus = maxLabelWidth - 10;
        }
        node.setShapeSize(new Dimension(Math.max(60, labelBounds.width + labelBonus + 20), Math.max(30, height + 20)));
    }

    /**
     * Arrange complex node
     */
    protected static void arrangeComplex(Compartment node)
    {
        int height = 15;
        int maxWidth = 60;
        for(DiagramElement childObj: node)
        {
            if( childObj instanceof Compartment && childObj.getKernel().getType().equals( "complex" ) )
            {
                Compartment child = (Compartment)childObj;
                child.setLocation(node.getLocation().x + 15, node.getLocation().y + height);
                arrangeComplex(child);
                Dimension size = child.getShapeSize();
                height += size.height;
                if( size.width > maxWidth )
                {
                    maxWidth = size.width;
                }
            }
            else if( childObj instanceof Node )
            {
                Node child = (Node)childObj;
                arrangeEntity(child);
                child.setLocation(node.getLocation().x + 15, node.getLocation().y + height);
                Dimension size = child.getShapeSize();
                height += size.height;
                if( size.width > maxWidth )
                {
                    maxWidth = size.width;
                }
            }
        }
        for(DiagramElement childObj: node)
        {
            if( childObj instanceof Node )
            {
                Node child = (Node)childObj;
                Dimension size = child.getShapeSize();
                if( size.width < maxWidth )
                {
                    child.setShapeSize(new Dimension(maxWidth, size.height));
                }
            }
        }
        CompositeView view = (CompositeView)node.getView();
        Rectangle labelBounds = new Rectangle(0, 0, 0, 0);
        for( View childView : view )
        {
            if( childView instanceof ComplexTextView )
            {
                labelBounds = childView.getBounds();
                break;
            }
        }
        node.setShapeSize(new Dimension(maxWidth + 30, height + labelBounds.height + 15));
    }

    /**
     * Arrange reaction nodes for layout issues
     */
    protected static void arrangeReaction(Node node)
    {
        Rectangle bounds = node.getView().getBounds();
        node.setShapeSize(new Dimension(bounds.width, bounds.height));
    }

    protected static String toString(OWLConstant obj)
    {
        if( obj instanceof OWLTypedConstant )
        {
            return ( (OWLTypedConstant)obj ).getLiteral();
        }
        return obj.toString();
    }

    protected static String join(String[] input)
    {
        return input.length == 1? input[0]: EntryStream.of( input ).mapKeyValue( (i, str) -> " ("+(i+1)+") "+str ).joining();
    }

    public BioHubBuilder getBioHubBuilder()
    {
        return bioHubBuilder;
    }

    public void setBioHubBuilder(BioHubBuilder bioHubBuilder)
    {
        this.bioHubBuilder = bioHubBuilder;
    }

    /**
     * A flat one-to-one link between Reactants and Products will be created for the reaction.
     * Reaction modifier is linked downstream to all products and reactants
     * In reversible reactions links reactant-product are also reverted
     */
    protected void addReactionLinks(Reaction reaction)
    {
        if( bioHubBuilder == null )
            return;
        SpecieReference[] par = reaction.getSpecieReferences();
        List<Element> reactants = new ArrayList<>();
        List<Element> products = new ArrayList<>();
        List<Element> modifiers = new ArrayList<>();
        List<Element> others = new ArrayList<>();
        for( SpecieReference sp : par )
        {
            String role = sp.getRole();
            Element elem = new Element(sp.getSpecie().isEmpty()?getCompleteName(sp):module.getCompletePath().getRelativePath(sp.getSpecie()).toString());
            if( role == null || role.equals(SpecieReference.OTHER) )
                others.add(elem);
            else if( role.endsWith(SpecieReference.REACTANT) )
                reactants.add(elem);
            else if( role.endsWith(SpecieReference.PRODUCT) )
                products.add(elem);
            else if( role.endsWith(SpecieReference.MODIFIER) )
                modifiers.add(elem);
        }
        String reactionName = getCompleteName(reaction);
        for( Element from : reactants )
        {
            for( Element to : products )
            {
                bioHubBuilder.addReference(from, to, null, reactionName, 1, BioHub.DIRECTION_DOWN);
                if( reaction.isReversible() )
                    bioHubBuilder.addReference(to, from, null, reactionName, 1, BioHub.DIRECTION_DOWN);
            }
        }
        for( Element from : modifiers )
        {
            for( Element to : reactants )
            {
                bioHubBuilder.addReference(from, to, null, reactionName, 1, BioHub.DIRECTION_DOWN);
            }
            for( Element to : products )
            {
                bioHubBuilder.addReference(from, to, null, reactionName, 1, BioHub.DIRECTION_DOWN);
            }
        }
        if( others.size() > 0 )
        {
            if( reactants.size() > 0 && products.size() > 0 )
            {
                log.log(Level.SEVERE, "Reaction " + reactionName + " contains undefined praticipant types, BioHub can be incorrect");
            }
            for( Element from : others )
            {
                for( Element to : others )
                {
                    if( from.equals(to) )
                        continue;
                    bioHubBuilder.addReference(from, to, null, reactionName, 1, BioHub.DIRECTION_DOWN);
                }
            }
        }
    }

    /**
     * Semantic relation is transformed to link modifier-reaction, i.e. links between modifier and participants will be added
     */
    protected void addRelationLinks(SemanticRelation relation)
    {
        if( bioHubBuilder == null )
            return;
        try
        {
            String fromName = relation.getInputElementName();
            try
            {
                String name = CollectionFactory.getDataElement(fromName, module, SpecieReference.class).getSpecie();
                if(name != null && !name.isEmpty()) fromName = name;
            }
            catch( RepositoryException e )
            {
            }
            Element fromElem = new Element(module.getCompletePath().getRelativePath(fromName));

            DataElement outDE = getDataElementByName(relation.getOutputElementName());

            if( outDE instanceof Reaction )
            {
                String reactionName = getCompleteName(outDE);
                for( SpecieReference sp : ( (Reaction)outDE ).getSpecieReferences() )
                {
                    Element to = new Element(module.getCompletePath().getRelativePath(sp.getSpecie()));
                    bioHubBuilder.addReference(fromElem, to, null, reactionName, 1, BioHub.DIRECTION_DOWN);
                }
            }
            else
            {
                String relationName = getCompleteName(relation);
                Element toElem = new Element(getCompleteName(outDE));
                bioHubBuilder.addReference(fromElem, toElem, null, relationName, 1, BioHub.DIRECTION_DOWN);
            }
        }
        catch( Exception e )
        {
        }
    }

    /**
     * Add matching info to biohub
     */
    protected void addMatchingLink(String name, DatabaseReference[] dbRefs, boolean isMain)
    {
        if( bioHubBuilder == null )
            return;
        try
        {
            Element from = new Element(name);
            for( DatabaseReference dbRef : dbRefs )
            {
                ReferenceType type = defineReferenceType(dbRef);
                if( type != null )
                {
                    Element to = new Element(dbRef.getId());
                    bioHubBuilder.addReference(from, null, to, type, isMain);
                }
            }
        }
        catch( Exception e )
        {
        }
    }

    private ReferenceType defineReferenceType(DatabaseReference ref)
    {
        ReferenceType detectedType = ReferenceTypeRegistry.detectReferenceType(ref.getId());
        if(detectedType.getIdScore(ref.getId()) >= ReferenceType.SCORE_HIGH_SPECIFIC) return detectedType;
        return ReferenceTypeRegistry.types()
                .findAny( type -> type.getSource().equalsIgnoreCase( ref.getDatabaseName() )
                                && type.getIdScore( ref.getId() ) > ReferenceType.SCORE_NOT_THIS_TYPE ).orElse( null );
    }

    protected String getCompleteName(DataElement de)
    {
        return DataElementPath.create(de).toString();
    }

    protected String getRelativeToModuleName(DataElement de)
    {
        return module != null ? CollectionFactory.getRelativeName(de, module) : Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + de.getOrigin().getName() + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + de.getName();
    }

    protected void writeToDPS(DynamicPropertySet dps, String name, Class<?> type, Object value)
    {
        if(value == null || (value instanceof String[] && ((String[])value).length == 0))
            return;
        dps.add(new DynamicProperty(name, type));
        dps.setValue(name, value);
    }

    protected String getType(OWLIndividual ind)
    {
        Set<OWLDescription> types = ind.getTypes(ontology);
        return (types == null || types.isEmpty()) ? null : types.iterator().next().toString();
    }

    protected <T extends DataElement> T get(DataCollection<T> dc, String name)
    {
        try
        {
            return dc.get( name );
        }
        catch( Exception e )
        {
            throw ExceptionRegistry.translateException( e );
        }
    }

    protected boolean isType(String type, String... types)
    {
        if(type == null) return false;
        for(String wantedType: types)
        {
            if(type.equals(wantedType))
                return true;
        }
        return false;
    }

    protected URI createURI(String propertyName)
    {
        URI uri;
        try
        {
            uri = new URI(biopaxPrefix + "#" + propertyName);
        }
        catch( URISyntaxException e )
        {
            try
            {
                uri = new URI(biopaxPrefix + propertyName); //BioPAX files now has these # in them
            }
            catch( URISyntaxException e2 )
            {
                throw new InternalException(e2);
            }
        }
        return uri;
    }

    protected Set<OWLIndividual> getProperties(OWLIndividual individual, String propertyName)
    {
        OWLObjectPropertyExpression ope = new OWLObjectPropertyImpl(factory, createURI(propertyName));
        Set<OWLIndividual> param = individual.getObjectPropertyValues(ontology).get(ope);
        return param == null ? Collections.<OWLIndividual>emptySet() : param;
    }

    protected OWLIndividual getProperty(OWLIndividual individual, String propertyName)
    {
        Set<OWLIndividual> properties = getProperties(individual, propertyName);
        if( properties.isEmpty() )
            return null;
        return properties.iterator().next();
    }

    protected StreamEx<OWLIndividual> getPropertiesByTypes(OWLIndividual individual, String propertyName, String... types)
    {
        return getPropertiesByTypes(getProperties(individual, propertyName), types);
    }

    protected StreamEx<OWLIndividual> getPropertiesByTypes(Set<OWLIndividual> param, String... types)
    {
        if(param == null) return StreamEx.of();
        return StreamEx.of( param ).filter( ind -> isType( getType( ind ), types ) );
    }
}
