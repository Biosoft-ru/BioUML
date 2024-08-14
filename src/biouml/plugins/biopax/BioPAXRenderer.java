// $ Id: $
package biouml.plugins.biopax;

import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.coode.owl.rdf.model.RDFGraph;
import org.coode.owl.rdf.model.RDFLiteralNode;
import org.coode.owl.rdf.model.RDFNode;
import org.coode.owl.rdf.model.RDFResourceNode;
import org.coode.owl.rdf.model.RDFTranslator;
import org.coode.owl.rdf.model.RDFTriple;
import org.coode.xml.XMLWriter;
import org.coode.xml.XMLWriterFactory;
import org.coode.xml.XMLWriterNamespaceManager;
import org.coode.xml.XMLWriterPreferences;
import org.semanticweb.owl.model.OWLAxiom;
import org.semanticweb.owl.model.OWLClass;
import org.semanticweb.owl.model.OWLDataProperty;
import org.semanticweb.owl.model.OWLDataType;
import org.semanticweb.owl.model.OWLEntity;
import org.semanticweb.owl.model.OWLEntityVisitor;
import org.semanticweb.owl.model.OWLIndividual;
import org.semanticweb.owl.model.OWLObjectProperty;
import org.semanticweb.owl.model.OWLOntology;
import org.semanticweb.owl.model.OWLOntologyManager;
import org.semanticweb.owl.vocab.OWLRDFVocabulary;


public class BioPAXRenderer
{
    OWLOntologyManager manager;
    OWLOntology ontology;
    RDFGraph graph;
    BioPAXWriter writer;
    Set<RDFResourceNode> pending;
    Set<URI> annotationURIs;
    Set<URI> prettyPrintedTypes;

    public BioPAXRenderer(OWLOntologyManager manager, OWLOntology ontology, Writer w, String format)
    {
        this.ontology = ontology;
        this.manager = manager;
        pending = new HashSet<>();
        XMLWriterNamespaceManager xwnm = new BioPAXOntologyNamespaceManager(manager, ontology, format);
        XMLWriter xmlWriter = XMLWriterFactory.getInstance().createXMLWriter(w, xwnm, ontology.getURI().toString());
        writer = new BioPAXWriter(xmlWriter);

        annotationURIs = ontology.getAnnotationURIs();
        prettyPrintedTypes = new HashSet<>();
        prettyPrintedTypes.add(OWLRDFVocabulary.OWL_CLASS.getURI());
        prettyPrintedTypes.add(OWLRDFVocabulary.OWL_OBJECT_PROPERTY.getURI());
        prettyPrintedTypes.add(OWLRDFVocabulary.OWL_DATA_PROPERTY.getURI());
        prettyPrintedTypes.add(OWLRDFVocabulary.OWL_ANNOTATION_PROPERTY.getURI());
        prettyPrintedTypes.add(OWLRDFVocabulary.OWL_RESTRICTION.getURI());
        prettyPrintedTypes.add(OWLRDFVocabulary.OWL_THING.getURI());
        prettyPrintedTypes.add(OWLRDFVocabulary.OWL_NOTHING.getURI());
        prettyPrintedTypes.add(OWLRDFVocabulary.OWL_ONTOLOGY.getURI());
        prettyPrintedTypes.add(OWLRDFVocabulary.OWL_NEGATIVE_DATA_PROPERTY_ASSERTION.getURI());
        prettyPrintedTypes.add(OWLRDFVocabulary.OWL_NEGATIVE_OBJECT_PROPERTY_ASSERTION.getURI());
    }


    public void render()
    {
        XMLWriterPreferences preferences = XMLWriterPreferences.getInstance();
        boolean useNamespaceEntities = preferences.isUseNamespaceEntities();
        preferences.setUseNamespaceEntities(false);
        writer.startDocument();
        preferences.setUseNamespaceEntities(useNamespaceEntities);

        // Put imports at the top of the rendering
        Set<OWLAxiom> axioms = new HashSet<>();
        axioms.addAll(ontology.getImportsDeclarations());
        createGraph(axioms);
        graph.addTriple(new RDFTriple(new RDFResourceNode(ontology.getURI()), new RDFResourceNode(OWLRDFVocabulary.RDF_TYPE.getURI()),
                new RDFResourceNode(OWLRDFVocabulary.OWL_ONTOLOGY.getURI())));
        render(new RDFResourceNode(ontology.getURI()));


        Set<? extends OWLIndividual> individuals = ontology.getReferencedIndividuals();
        if( !individuals.isEmpty() )
        {
            for( OWLIndividual ind : toSortedSet(ontology.getReferencedIndividuals()) )
            {
                createGraph(ind);
                render(new RDFResourceNode(ind.getURI()));
                renderAnonRoots();
            }
        }

        writer.endDocument();
    }


    private void createGraph(OWLEntity entity)
    {
        final Set<OWLAxiom> axioms = new HashSet<>();
        axioms.addAll(entity.getAnnotationAxioms(ontology));
        entity.accept(new OWLEntityVisitor()
        {
            @Override
            public void visit(OWLClass cls)
            {
                axioms.addAll(ontology.getAxioms(cls));
                createGraph(axioms);
            }

            @Override
            public void visit(OWLDataType dataType)
            {
            }

            @Override
            public void visit(OWLIndividual individual)
            {
                axioms.addAll(ontology.getAxioms(individual));
                createGraph(axioms);
            }

            @Override
            public void visit(OWLDataProperty property)
            {
                axioms.addAll(ontology.getAxioms(property));
                createGraph(axioms);
            }

            @Override
            public void visit(OWLObjectProperty property)
            {
                axioms.addAll(ontology.getAxioms(property));
                axioms.addAll(ontology.getAxioms(manager.getOWLDataFactory().getOWLObjectPropertyInverse(property)));
                createGraph(axioms);
            }
        });
        addTypeTriple(entity);
    }


    private void createGraph(Set<? extends OWLAxiom> axioms)
    {
        RDFTranslator translator = new RDFTranslator(manager, ontology);
        for( OWLAxiom ax : axioms )
        {
            ax.accept(translator);
        }
        graph = translator.getGraph();
    }


    private void addTypeTriple(OWLEntity entity)
    {
        entity.accept(new OWLEntityVisitor()
        {
            @Override
            public void visit(OWLClass cls)
            {
                graph.addTriple(new RDFTriple(new RDFResourceNode(cls.getURI()), new RDFResourceNode(OWLRDFVocabulary.RDF_TYPE.getURI()),
                        new RDFResourceNode(OWLRDFVocabulary.OWL_CLASS.getURI())));
            }

            @Override
            public void visit(OWLDataType dataType)
            {
                graph.addTriple(new RDFTriple(new RDFResourceNode(dataType.getURI()), new RDFResourceNode(OWLRDFVocabulary.RDF_TYPE
                        .getURI()), new RDFResourceNode(OWLRDFVocabulary.RDFS_DATATYPE.getURI())));
            }

            @Override
            public void visit(OWLIndividual individual)
            {
            }

            @Override
            public void visit(OWLDataProperty property)
            {
                graph.addTriple(new RDFTriple(new RDFResourceNode(property.getURI()), new RDFResourceNode(OWLRDFVocabulary.RDF_TYPE
                        .getURI()), new RDFResourceNode(OWLRDFVocabulary.OWL_DATA_PROPERTY.getURI())));
                if( annotationURIs.contains(property.getURI()) )
                {
                    graph.addTriple(new RDFTriple(new RDFResourceNode(property.getURI()), new RDFResourceNode(OWLRDFVocabulary.RDF_TYPE
                            .getURI()), new RDFResourceNode(OWLRDFVocabulary.OWL_ANNOTATION_PROPERTY.getURI())));
                }
            }

            @Override
            public void visit(OWLObjectProperty property)
            {
                graph.addTriple(new RDFTriple(new RDFResourceNode(property.getURI()), new RDFResourceNode(OWLRDFVocabulary.RDF_TYPE
                        .getURI()), new RDFResourceNode(OWLRDFVocabulary.OWL_OBJECT_PROPERTY.getURI())));
                if( annotationURIs.contains(property.getURI()) )
                {
                    graph.addTriple(new RDFTriple(new RDFResourceNode(property.getURI()), new RDFResourceNode(OWLRDFVocabulary.RDF_TYPE
                            .getURI()), new RDFResourceNode(OWLRDFVocabulary.OWL_ANNOTATION_PROPERTY.getURI())));
                }
            }
        });
    }

    private static <N extends OWLEntity> Set<N> toSortedSet(Set<N> entities)
    {
        Set<N> results = new TreeSet<>( Comparator.comparing( OWLEntity::getURI ) );
        results.addAll(entities);
        return results;
    }

    public void renderAnonRoots()
    {
        for( RDFResourceNode node : graph.getRootAnonymousNodes() )
        {
            render(node);
        }
    }

    public void render(RDFResourceNode node)
    {
        if( pending.contains(node) )
        {
            // We essentially remove all structure sharing during parsing - any cycles therefore indicate a bug!
            throw new IllegalStateException("Rendering cycle!  This indicates structure sharing and should not happen!");
        }
        pending.add(node);
        Set<RDFTriple> triples = new TreeSet<>(new TripleComparator());
        triples.addAll(graph.getTriplesForSubject(node));
        RDFTriple candidatePrettyPrintTypeTriple = null;
        for( RDFTriple triple : graph.getTriplesForSubject(node) )
        {
            URI propertyURI = triple.getProperty().getURI();
            if( propertyURI.equals(OWLRDFVocabulary.RDF_TYPE.getURI()) && !triple.getObject().isAnonymous() )
            {
                if( OWLRDFVocabulary.BUILT_IN_VOCABULARY.contains(triple.getObject().getURI()) )
                {
                    if( prettyPrintedTypes.contains(triple.getObject().getURI()) )
                    {
                        candidatePrettyPrintTypeTriple = triple;
                    }
                }
                else
                {
                    candidatePrettyPrintTypeTriple = triple;
                }
            }
        }
        if( candidatePrettyPrintTypeTriple == null )
        {
            writer.writeStartElement(OWLRDFVocabulary.RDF_DESCRIPTION.getURI());
        }
        else
        {
            writer.writeStartElement(candidatePrettyPrintTypeTriple.getObject().getURI());
        }
        if( !node.isAnonymous() )
        {
            //writer.writeAboutAttribute(node.getURI());
            writer.writeIDAttribute(node.getURI());
        }
        for( RDFTriple triple : triples )
        {
            if( candidatePrettyPrintTypeTriple != null && candidatePrettyPrintTypeTriple.equals(triple) )
            {
                continue;
            }
            writer.writeStartElement(triple.getProperty().getURI());
            RDFNode objectNode = triple.getObject();
            if( !objectNode.isLiteral() )
            {
                RDFResourceNode objectRes = (RDFResourceNode)objectNode;
                if( objectRes.isAnonymous() )
                {
                    // Special rendering for lists
                    if( isObjectList(objectRes) )
                    {
                        writer.writeParseTypeAttribute();
                        List<RDFNode> list = new ArrayList<>();
                        toJavaList(objectRes, list);
                        for( RDFNode n : list )
                        {
                            if( n.isAnonymous() )
                            {
                                render((RDFResourceNode)n);
                            }
                            else
                            {
                                if( n.isLiteral() )
                                {
                                    RDFLiteralNode litNode = (RDFLiteralNode)n;
                                    writer.writeStartElement(OWLRDFVocabulary.RDFS_LITERAL.getURI());
                                    if( litNode.getDatatype() != null )
                                    {
                                        writer.writeDatatypeAttribute(litNode.getDatatype());
                                    }
                                    else if( litNode.getLang() != null )
                                    {
                                        writer.writeLangAttribute(litNode.getLang());
                                    }
                                    writer.writeTextContent( ( litNode.getLiteral() ));
                                    writer.writeEndElement();
                                }
                                else
                                {
                                    writer.writeStartElement(OWLRDFVocabulary.RDF_DESCRIPTION.getURI());
                                    writer.writeAboutAttribute(n.getURI());
                                    writer.writeEndElement();
                                }
                            }
                        }
                    }
                    else
                    {
                        render(objectRes);
                    }
                }
                else
                {
                    writer.writeResourceAttribute(objectRes.getURI());
                }
            }
            else
            {
                RDFLiteralNode rdfLiteralNode = ( (RDFLiteralNode)objectNode );
                if( rdfLiteralNode.getDatatype() != null )
                {
                    writer.writeDatatypeAttribute(rdfLiteralNode.getDatatype());
                }
                else if( rdfLiteralNode.getLang() != null )
                {
                    writer.writeLangAttribute(rdfLiteralNode.getLang());
                }
                writer.writeTextContent(rdfLiteralNode.getLiteral());
            }
            writer.writeEndElement();
        }
        writer.writeEndElement();
        pending.remove(node);
    }

    private boolean isObjectList(RDFResourceNode node)
    {
        for( RDFTriple triple : graph.getTriplesForSubject(node) )
        {
            if( triple.getProperty().getURI().equals(OWLRDFVocabulary.RDF_TYPE.getURI()) )
            {
                if( !triple.getObject().isAnonymous() )
                {
                    if( triple.getObject().getURI().equals(OWLRDFVocabulary.RDF_LIST.getURI()) )
                    {
                        List<RDFNode> items = new ArrayList<>();
                        toJavaList(node, items);
                        for( RDFNode n : items )
                        {
                            if( n.isLiteral() )
                            {
                                return false;
                            }
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void toJavaList(RDFNode node, List<RDFNode> list)
    {
        for( RDFTriple triple : graph.getTriplesForSubject(node) )
        {
            if( triple.getProperty().getURI().equals(OWLRDFVocabulary.RDF_FIRST.getURI()) )
            {
                list.add(triple.getObject());
            }
            else
            {
                if( triple.getProperty().getURI().equals(OWLRDFVocabulary.RDF_REST.getURI()) )
                {
                    if( !triple.getObject().isAnonymous() )
                    {
                        if( triple.getObject().getURI().equals(OWLRDFVocabulary.RDF_NIL.getURI()) )
                        {
                            // End of list
                        }
                    }
                    else
                    {
                        // Should be another list
                        toJavaList(triple.getObject(), list);
                    }
                }
            }
        }
    }

    private static class TripleComparator implements Comparator<RDFTriple>
    {
        private final List<URI> orderedURIs;

        public TripleComparator()
        {
            orderedURIs = new ArrayList<>();
            orderedURIs.add(OWLRDFVocabulary.RDF_TYPE.getURI());
            orderedURIs.add(OWLRDFVocabulary.RDFS_LABEL.getURI());
            orderedURIs.add(OWLRDFVocabulary.OWL_EQUIVALENT_CLASS.getURI());
            orderedURIs.add(OWLRDFVocabulary.RDFS_SUBCLASS_OF.getURI());
            orderedURIs.add(OWLRDFVocabulary.OWL_DISJOINT_WITH.getURI());

            orderedURIs.add(OWLRDFVocabulary.OWL_ON_PROPERTY.getURI());
            orderedURIs.add(OWLRDFVocabulary.OWL_DATA_RANGE.getURI());
            orderedURIs.add(OWLRDFVocabulary.OWL_ON_CLASS.getURI());

            orderedURIs.add(OWLRDFVocabulary.RDF_SUBJECT.getURI());
            orderedURIs.add(OWLRDFVocabulary.RDF_PREDICATE.getURI());
            orderedURIs.add(OWLRDFVocabulary.RDF_OBJECT.getURI());
        }

        private int getIndex(URI uri)
        {
            int index = orderedURIs.indexOf(uri);
            if( index == -1 )
            {
                index = orderedURIs.size();
            }
            return index;
        }

        @Override
        public int compare(RDFTriple o1, RDFTriple o2)
        {
            int diff = getIndex(o1.getProperty().getURI()) - getIndex(o2.getProperty().getURI());
            if( diff == 0 )
            {
                diff = 1;
            }
            return diff;
        }
    }
}
