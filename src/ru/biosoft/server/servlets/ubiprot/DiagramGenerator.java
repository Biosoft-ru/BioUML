package ru.biosoft.server.servlets.ubiprot;

import java.awt.Dimension;
import java.awt.Point;
import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.DynamicProperty;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.util.BeanUtil;
import ru.biosoft.util.bean.StaticDescriptor;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramType;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.xml.XmlDiagramType;
import biouml.standard.diagram.PathwayDiagramType;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Protein;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;

public class DiagramGenerator
{
    public static final String NOTATION_NAME = "ubiprot.xml";
    public static final String DATA_SOURCE = "ru/biosoft/server/servlets/ubiprot/data/";
    
    private static final PropertyDescriptor COMPARTMENT_TYPE_DESCRIPTOR = StaticDescriptor.create("compartmentType");
    private static final PropertyDescriptor LABEL_DESCRIPTOR = StaticDescriptor.create("label");
    private static final PropertyDescriptor MODIFICATION_DESCRIPTOR = StaticDescriptor.create("modification");
    private static final PropertyDescriptor IS_UBI_DESCRIPTOR = StaticDescriptor.create("isUbi");

    private static DiagramGenerator generator = null;

    public static DiagramGenerator getInstance()
    {
        if( generator == null )
        {
            generator = new DiagramGenerator();
        }
        return generator;
    }

    protected Map<String, Diagram> generatedDiagrams = null;
    protected DataCollection<XmlDiagramType> gnCollection = null;
    protected DataLoader dataLoader;

    public DiagramGenerator()
    {
        generatedDiagrams = new HashMap<>();
        gnCollection = XmlDiagramType.getTypesCollection();
        dataLoader = new DataLoader(DATA_SOURCE);
    }

    public Diagram getDiagram(String line, String organism) throws Exception
    {
        String id = line + organism;
        if( generatedDiagrams.containsKey(id) )
        {
            return generatedDiagrams.get(id);
        }
        DiagramType diagramType = null;
        if( gnCollection.contains(NOTATION_NAME) )
        {
            diagramType = gnCollection.get(NOTATION_NAME);
        }
        else
        {
            diagramType = new PathwayDiagramType();
        }
        Diagram diagram = new Diagram(null, new DiagramInfo("diagram" + id), diagramType);

        UbiprotDiagramDescription udd = dataLoader.getDiagramDescription(line, organism);

        if( udd != null )
        {
            Node reactant = null;
            Node product = null;
            Node nodeE1 = null;
            Node nodeE2 = null;
            Node nodeE3 = null;
            Node dub = null;
            Node ubp = null;

            //create nodes
            if( udd.getSubstrate() != null )
            {
                Protein p = udd.getSubstrate();
                reactant = createProteinNode(diagram, p, new Point(280, 200), false);
                if( udd.getPreModification() != null )
                {
                    reactant.getAttributes().add(new DynamicProperty(MODIFICATION_DESCRIPTOR, String.class, udd.getPreModification()));
                }
                Protein ubiProtein = new Protein(null, p.getName() + " - Ubi");
                ubiProtein.setTitle(p.getTitle() + " - Ubi");
                ubiProtein.setComment(p.getComment());
                product = createProteinNode(diagram, ubiProtein, new Point(560, 198), true);
                if( udd.getPostModification() != null )
                {
                    product.getAttributes().add(new DynamicProperty(MODIFICATION_DESCRIPTOR, String.class, udd.getPostModification()));
                }
            }

            nodeE1 = createCompartment(diagram, udd.getE1(), new Point(10, 50), "E1");
            nodeE2 = createCompartment(diagram, udd.getE2(), new Point(200, 50), "E2");
            nodeE3 = createCompartment(diagram, udd.getE3(), new Point(410, 50), "E3");

            dub = createCompartment(diagram, udd.getDub(), new Point(410, 350), "DUB");
            ubp = createCompartment(diagram, udd.getUbp(), new Point(700, 177), "UBP");


            //create edges
            createSimpleEdge(diagram, nodeE1, nodeE2);
            createSimpleEdge(diagram, nodeE2, nodeE3);
            if( product != null )
            {
                createSimpleEdge(diagram, product, ubp);
            }

            //create reaction
            if( reactant != null && product != null )
            {
                Node reaction = new Node(diagram, "reaction", new Reaction(null, "reaction"));
                reaction.setLocation(new Point(444, 218));
                diagram.put(reaction);
                createReactionEdge(diagram, reaction, reactant, SpecieReference.REACTANT);
                createReactionEdge(diagram, reaction, product, SpecieReference.PRODUCT);
                createReactionEdge(diagram, reaction, nodeE3, SpecieReference.MODIFIER);
                createReactionEdge(diagram, reaction, dub, SpecieReference.MODIFIER);
            }

            generatedDiagrams.put(id, diagram);
        }
        return diagram;
    }

    protected @Nonnull Node createCompartment(Diagram parent, Protein protein, Point location, String type) throws Exception
    {
        if( protein == null )
        {
            return createCompartment(parent, new Protein[] {}, location, type);
        }
        return createCompartment(parent, new Protein[] {protein}, location, type);
    }
    protected @Nonnull Node createCompartment(Diagram parent, Protein[] proteins, Point location, String type) throws Exception
    {
        String compName;
        String title;
        int size = 0;
        if( proteins == null || proteins.length == 0 )
        {
            compName = type;
            title = "";
        }
        else
        {
            compName = BeanUtil.joinBeanProperties(proteins, "title", "+");
            title = compName;
            size = proteins.length;
        }

        Compartment compartment = new Compartment(parent, new Stub(null, compName, "Compartment"));
        compartment.setTitle(title);
        compartment.getAttributes().add(new DynamicProperty(COMPARTMENT_TYPE_DESCRIPTOR, String.class, type));
        compartment.setShapeSize(new Dimension( ( size == 0 ? 40 : ( size * 40 ) ) + 30, 70));
        compartment.setLocation(location);

        if( size > 0 )
        {
            for( int i = 0; i < size; i++ )
            {
                Node node = new Node(compartment, proteins[i]);
                Point nodeLocation = new Point(compartment.getLocation().x + i * 40 + 20, compartment.getLocation().y + 20);
                node.setLocation(nodeLocation);
                compartment.put(node);
            }
        }
        else
        {
            Node node = new Node(compartment, new Stub(null, "unknown", "Unknown"));
            Point nodeLocation = new Point(compartment.getLocation().x + 30, compartment.getLocation().y + 30);
            node.setLocation(nodeLocation);
            compartment.put(node);
        }

        parent.put(compartment);
        return compartment;
    }

    protected void createSimpleEdge(Diagram diagram, Node input, Node output) throws Exception
    {
        Edge edge = new Edge(diagram, new Stub(null, input.getName() + "->" + output.getName(), "simpleEdge"), input, output);
        diagram.put(edge);
    }

    protected void createReactionEdge(Diagram diagram, Node reaction, Node specie, String role) throws Exception
    {
        Edge edge = null;
        SpecieReference sr = new SpecieReference((Reaction)reaction.getKernel(), reaction.getName() + "->" + specie.getName(), role);
        if( role.equals(SpecieReference.PRODUCT) )
        {
            edge = new Edge(diagram, sr, reaction, specie);
        }
        else
        {
            edge = new Edge(diagram, sr, specie, reaction);
        }
        diagram.put(edge);
    }

    protected Node createProteinNode(Diagram parent, Protein protein, Point location, boolean isUbi) throws Exception
    {
        Node node = new Node(parent, protein);
        node.getAttributes().add(new DynamicProperty(LABEL_DESCRIPTOR, String.class, protein.getTitle()));
        if( isUbi )
        {
            node.getAttributes().add(new DynamicProperty(MODIFICATION_DESCRIPTOR, String.class, "U"));
            node.getAttributes().add(new DynamicProperty(IS_UBI_DESCRIPTOR, String.class, "true"));
        }
        node.setLocation(location);
        parent.put(node);
        return node;
    }
}
