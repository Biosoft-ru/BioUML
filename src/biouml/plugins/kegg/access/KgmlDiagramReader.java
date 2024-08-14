package biouml.plugins.kegg.access;

import java.awt.Dimension;
import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;

import one.util.streamex.StreamEx;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import java.util.logging.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.graph.LabelLayouter;
import ru.biosoft.util.TextUtil;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramType;
import biouml.model.Edge;
import biouml.model.Module;
import biouml.model.Node;
import biouml.model.xml.XmlDiagramType;
import biouml.plugins.kegg.KeggPathwayDiagramType;
import biouml.plugins.kegg.KeggPathwayLayouter;
import biouml.plugins.kegg.KeggPathwaySemanticController;
import biouml.standard.type.Base;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Protein;
import biouml.standard.type.Reaction;
import biouml.standard.type.Relation;
import biouml.standard.type.SemanticRelation;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import biouml.standard.type.Substance;
import biouml.standard.type.Type;
import biouml.workbench.graph.DiagramToGraphTransformer;

/**
 * This class reads diagrams in KEGG pathways diagrams in KGML v.0.4 format.
 *
 * See KEGG module description for further details.
 *
 * @pending - remove unused constants
 */
public class KgmlDiagramReader extends DefaultHandler
{


    ///////////////////////////////////////////////////////////////////
    // KGML elements and attributes
    //

    public static final String PATHWAY_ELEMENT = "pathway";
    public static final String ENTRY_ELEMENT = "entry";
    public static final String REACTION_ELEMENT = "reaction";
    public static final String RELATION_ELEMENT = "relation";
    public static final String SUBTYPE_ELEMENT = "subtype";
    public static final String COMPONENT_ELEMENT = "component";
    public static final String SUBSTRATE_ELEMENT = "substrate";
    public static final String PRODUCT_ELEMENT = "product";

    public static final String ID_ATTRIBUTE = "id";
    public static final String NAME_ATTRIBUTE = "name";
    public static final String REACTION_ATTRIBUTE = "reaction";
    public static final String MAP_ATTRIBUTE = "map";

    public static final String TYPE_ATTRIBUTE = "type";
    public static final String ORTHOLOG_TYPE = "ortholog";
    public static final String ENZYME_TYPE = "enzyme";
    public static final String GENE_TYPE = "gene";
    public static final String GENES_TYPE = "genes";
    public static final String COMPOUND_TYPE = "compound";
    public static final String MAP_TYPE = "map";
    public static final String REACTION_TYPE = "reaction"; // our internal type
    public static final String UNDEFINED_TYPE = "undefined";
    public static final String GROUP_TYPE = "group";
    public static final String OTHER_TYPE = "other";

    public static final String ENTRY1_ATTRIBUTE = "entry1";
    public static final String ENTRY2_ATTRIBUTE = "entry2";
    public static final String VALUE_ATTRIBUTE = "value";

    public static final String REACTION_TYPE_ATTR = "rType";

    public static final String GRAPHICS_ELEMENT = "graphics";
    static final String HEIGHT_ATTRIBUTE = "height";
    static final String WIDTH_ATTRIBUTE = "width";
    static final String X_ATTRIBUTE = "x";
    static final String Y_ATTRIBUTE = "y";

    ///////////////////////////////////////////
    static final String COMPONENT = "component";

    static final String LINK = "link";
    static final String NUMBER = "number";
    static final String IMAGE = "image";
    static final String TITLE = "title";
    static final String ORG = "org";
    static final String COMPOUND = "compound";
    static final String BGCOLOR = "bgcolor";
    static final String FGCOLOR = "fgcolor";

    private static Logger log = Logger.getLogger(KgmlDiagramReader.class.getName());
    private final File file;
    private Module module;
    private Diagram diagram;

    private Node currentNode;
    private KgmlRelation currentRelation;

    protected HashMap<String, Node> idMap = new HashMap<>();
    protected Map<String, List<String>> proteinLinks = new HashMap<>();
    protected Map<String, List<Node>> reactionLinks = new HashMap<>();
    protected Map<Node, List<String>> reactionElements = new HashMap<>();
    protected Map<String, List<Node>> mapLinks = new HashMap<>();
    protected List<KgmlRelation> relations = new ArrayList<>( 0 );

    ///////////////////////////////////////////////////////////////////
    // Constructor and initialisation
    //

    public KgmlDiagramReader(File file)
    {
        this.file = file;
    }

    public Diagram read(DataCollection<?> origin, String name, Module module) throws Exception
    {
        this.module = module;
        DiagramType diagramType = null;
        try
        {
            diagramType = XmlDiagramType.getTypeObject("kegg.xml");
            ( (XmlDiagramType)diagramType ).setSemanticController(new KeggPathwaySemanticController());
        }
        catch( Exception e )
        {
            log.warning("Can not find diagramt type: kegg.xml");
        }
        if( diagramType == null )
            diagramType = new KeggPathwayDiagramType();

        diagram = new Diagram(origin, new DiagramInfo(null, name), diagramType);
        diagram.setNotificationEnabled(false);

        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setValidating(false);
        SAXParser parser = factory.newSAXParser();

        long time = System.currentTimeMillis();

        String str = ApplicationUtils.readAsString(file);

        // dirty trick to escape DTD loading
        // TODO: check whether encoding is actually UTF-8
        parser.parse( new ByteArrayInputStream( str.replaceAll( "<!DOCTYPE.*>", "" ).getBytes( StandardCharsets.UTF_8 ) ), this );
        log.info("KEGG diagram is parsed, time=" + ( System.currentTimeMillis() - time ) + " ms.");

        diagram.setPathLayouter(new KeggPathwayLayouter());
        diagram.setLabelLayouter(new LabelLayouter());
        DiagramToGraphTransformer.layoutEdges(diagram);
        diagram.setView(null);

        diagram.setNotificationEnabled(true);
        return diagram;
    }

    ///////////////////////////////////////////////////////////////////
    // SAX parser events dispatcher
    //

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
    {
        if( qName.equals(PATHWAY_ELEMENT) )
        {
            String title = attributes.getValue(TITLE);
            if( title != null )
                diagram.setTitle(title);
        }

        // Node issues
        else if( qName.equals(ENTRY_ELEMENT) )
        {
            createNode(attributes);
        }
        else if( qName.equals(GRAPHICS_ELEMENT) )
        {
            initNodeGraphics(attributes);
        }

        else if( qName.equals(REACTION_ELEMENT) )
        {
            createReaction(attributes);
        }
        else if( qName.equals(RELATION_ELEMENT) )
        {
            createRelation(attributes);
        }
        else if( qName.equals(SUBTYPE_ELEMENT) )
        {
            createSubtype(attributes);
        }
        else if( qName.equals(COMPONENT_ELEMENT) )
        {
            createComponent(attributes);
        }
        else if( qName.equals(SUBSTRATE_ELEMENT) )
        {
            createReactionElement(attributes, SUBSTRATE_ELEMENT);
        }
        else if( qName.equals(PRODUCT_ELEMENT) )
        {
            createReactionElement(attributes, PRODUCT_ELEMENT);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException
    {
        if( qName.equals(PATHWAY_ELEMENT) )
        {
            createEdges();
            createRelationEdges();
            createMapLinks();
        }
        else if( qName.equals(RELATION_ELEMENT) )
        {
            storeRelation();
        }
    }

    ///////////////////////////////////////////////////////////////////
    // Node issues - entry element
    //

    public void createNode(Attributes attributes)
    {
        String id = attributes.getValue(ID_ATTRIBUTE);
        String name = attributes.getValue(NAME_ATTRIBUTE);
        if(TextUtil.isEmpty(name))
            name = id;
        String type = attributes.getValue(TYPE_ATTRIBUTE);
        String reactionLink = attributes.getValue(REACTION_ATTRIBUTE);
        String mapLink = attributes.getValue(MAP_ATTRIBUTE);

        try
        {
            if( GROUP_TYPE.equals(type) )
            {
                // create compartment
                int i = 0;
                String nodeId = name;
                while( diagram.contains(nodeId) )
                {
                    i++;
                    nodeId = name + "(" + i + ")";
                }
                currentNode = new Compartment(diagram, new biouml.standard.type.Compartment(null, nodeId));
                diagram.put(currentNode);
                idMap.put(id, currentNode);
            }
            else
            {
                // create node
                Base kernel = getKernel(name, type);

                String nodeId = name;
                if( diagram.containsKernel(kernel) )
                {
                    nodeId = nodeId + "(" + ( diagram.getKernelNodes(kernel).count() + 1 ) + ")";
                }

                currentNode = new Node(diagram, nodeId, kernel);
                if( mapLink == null )
                {
                    diagram.put(currentNode);
                }

                idMap.put(id, currentNode);

                // create reaction links
                if( reactionLink != null )
                {
                    StringTokenizer tokens = new StringTokenizer(reactionLink, " ;");
                    while( tokens.hasMoreTokens() )
                    {
                        addLink(reactionLinks, cutPrefix(tokens.nextToken(), ":"), currentNode);
                    }

                    if( kernel instanceof Protein )
                    {
                        List<String> reactions = new ArrayList<>();
                        proteinLinks.put(currentNode.getName(), reactions);
                        tokens = new StringTokenizer(reactionLink, " ;");
                        while( tokens.hasMoreTokens() )
                        {
                            reactions.add(cutPrefix(tokens.nextToken(), ":"));
                        }
                    }
                }
            }
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Can not create node, name=" + name + ", type=" + type + ", diagram=" + diagram.getCompletePath(), t);
            return;
        }
    }

    protected void addLink(Map<String, List<Node>> map, String link, Node node)
    {
        List<Node> values;
        if( map.containsKey(link) )
            values = map.get(link);
        else
        {
            values = new ArrayList<>();
            map.put(link, values);
        }

        values.add(node);
    }

    /**
     * Returns kernels with of specified type with the specified name.
     *
     * @pending GENES_TYPE, group
     * @pending MAP_TYPE - stub version, we should check, whether this map is available
     * and provides the map showing on click.
     */
    public Base getKernel(String name, String type)
    {
        Base kernel = null;

        try
        {
            if( ORTHOLOG_TYPE.equals(type) )
            {
                // if name contains several ids we should split it
                String[] names = TextUtil.split( name, ' ' );
                // trim "ko:" prefix
                kernel = (Base)module.getKernel(Module.DATA + "/ortholog/" + names[0].substring(3));
                type = Type.TYPE_PROTEIN;
            }

            else if( ENZYME_TYPE.equals(type) )
            {
                // if name contains several ids we should split it
                String[] names = TextUtil.split( name, ' ' );
                // trim "ec:" prefix
                kernel = (Base)module.getKernel(Module.DATA + "/enzyme/EC " + names[0].substring(3));
                type = Type.TYPE_PROTEIN;
            }

            else if( GENE_TYPE.equals(type) || GENES_TYPE.equals(type) )
            {
                kernel = new Stub(null, name, Type.TYPE_PROTEIN);
            }

            else if( COMPOUND_TYPE.equals(type) )
            {
                // trim "cpd:" prefix
                kernel = (Base)module.getKernel(Module.DATA + "/compound/" + name.substring(4));
                type = Type.TYPE_SUBSTANCE;
            }

            else if( MAP_TYPE.equals(type) )
            {
                // trim "path:" prefix
                kernel = (Base)module.getKernel(Module.DIAGRAM + "/" + name.substring(5));
                if( kernel != null )
                    return kernel;
                kernel = new Stub(null, name, Type.TYPE_DIAGRAM_REFERENCE);
            }

            else if( REACTION_TYPE.equals(type) )
            {
                kernel = (Base)module.getKernel(Module.DATA + "/reaction/" + name);
            }

            else if( UNDEFINED_TYPE.equals(type) )
            {
                kernel = new Stub(null, name, type);
            }
            else if( OTHER_TYPE.equals(type) )
            {
                int ind = name.indexOf(':');
                if( ind != -1 )
                {
                    name = name.substring(ind + 1);
                }
                kernel = new Stub(null, name, Type.TYPE_PROTEIN);
            }

            if( kernel == null )
                throw new java.util.NoSuchElementException();
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Can not find kernel, name=" + name + ", type=" + type + ".");
            kernel = new Stub(null, name, type);
        }

        return kernel;
    }

    private void initNodeGraphics(Attributes attributes)
    {
        Base kernel = currentNode.getKernel();
        String kernelType = kernel.getType();

        int x = Integer.parseInt(attributes.getValue(X_ATTRIBUTE));
        int y = Integer.parseInt(attributes.getValue(Y_ATTRIBUTE));
        int width = Integer.parseInt(attributes.getValue(WIDTH_ATTRIBUTE));
        int height = Integer.parseInt(attributes.getValue(HEIGHT_ATTRIBUTE));

        x -= width / 2;
        y -= height / 2;

        // scale
        float k = 2.0f;
        x = Math.round(x * k);
        y = Math.round(y * k);
        float p = 2.0f;
        width = Math.round(width * p);
        height = Math.round(height * p);

        currentNode.setLocation(x, y);
        currentNode.setShapeSize(new Dimension(width, height));

        String name = attributes.getValue(NAME_ATTRIBUTE);
        if( kernel instanceof Stub && name != null )
        {
            if( name.startsWith("TITLE:") )
            {
                //cut "TITLE:" string from title
                name = name.substring(6);
            }
            if( kernelType.equals(Base.TYPE_PROTEIN) )
                ( (Stub)kernel ).setTitle(name);
            else if( kernelType.equals(Base.TYPE_DIAGRAM_REFERENCE) )
                currentNode.setTitle(name);
        }
    }

    ///////////////////////////////////////////////////////////////////
    // Map links
    //

    /**
     * In KGML v. 0.4. map links are stored in following form:
     *
     * <pre>
     * <entry id="184" name="ec:4.1.3.4" type="enzyme" map="20"
     link="http://www.genome.ad.jp/dbget-bin/show_pathway?map00072+4.1.3.4"/>
     * </pre>
     *
     * During nodes parsing we are collecting all such links in mapLinks hasMap.
     * Then, if such mapLink node has not graphics element (detected as shapeSize is null),
     * then we are trying to find normal (real) node that should be connected with the map
     * reference. Generally entry is enzyme and real node is compound,
     * and we are finding this compund by finding node with the corresponding kernel title.
     */
    protected void createMapLinks()
    {
        for(Entry<String, List<Node>> entry : mapLinks.entrySet())
        {
            String mapNodeId = entry.getKey();

            Node mapNode = idMap.get(mapNodeId);
            if( mapNode == null )
            {
                log.log(Level.SEVERE, "Can not find map node, mapLinkId=" + mapNodeId);
                continue;
            }

            List<Node> list = entry.getValue();
            for( Node node : list )
            {
                boolean isSubstrate = true;

                if( node.getShapeSize() == null )
                {
                    Node realNode = null;

                    if( node.getKernel() instanceof Protein )
                    {
                        Protein enzyme = (Protein)node.getKernel();
                        String substrateName = (String)enzyme.getAttributes().getValue("substrate");
                        if( substrateName != null )
                        {
                            realNode = findRealNode(substrateName);
                        }
                        if( realNode == null )
                        {
                            String productName = (String)enzyme.getAttributes().getValue("product");
                            if( productName != null )
                            {
                                realNode = findRealNode(productName);
                            }
                            if( realNode != null )
                                isSubstrate = false;
                        }

                        if( realNode != null )
                        {
                            removeNode(node);
                            node = realNode;
                        }
                    }

                    // we can not find real node
                    if( node.getShapeSize() == null )
                    {
                        log.warning("Can not reslve map link, map=" + mapNodeId + ", node=" + node.getName());
                        removeNode(node);
                        continue;
                    }
                }

                String relId = isSubstrate ? mapNode.getName() + " -> " + node.getName() : node.getName() + " -> " + mapNode.getName();
                try
                {
                    SemanticRelation rel = new SemanticRelation(null, relId);
                    rel.setParticipation(Relation.PARTICIPATION_INDIRECT);
                    rel.setInputElementName(isSubstrate ? mapNode.getName() : node.getName());
                    rel.setOutputElementName(isSubstrate ? node.getName() : mapNode.getName());

                    Edge edge = new Edge(diagram, //rel.getName(),
                            rel, isSubstrate ? mapNode : node, isSubstrate ? node : mapNode);
                    diagram.put(edge);
                }
                catch( Throwable t )
                {
                    log.log(Level.SEVERE, "Can not create map link, node=" + node.getName() + ", map=" + mapNode.getName(), t);
                }
            }
        }
    }

    protected Node findRealNode(String titleList)
    {
        if( titleList == null )
            return null;

        Set<String> titles = StreamEx.split( titleList, "\r\n" ).map( String::toLowerCase ).toSet();
        return diagram.stream( Node.class ).filter( n -> n.getKernel() != null )
                .findFirst( n -> titles.contains( n.getKernel().getTitle().toLowerCase() ) ).orElse( null );
    }

    protected void removeNode(Node node)
    {
        try
        {
            diagram.remove(node.getName());
            for( Edge edge : node.getEdges() )
                diagram.remove(edge.getName());

            for(Entry<String, Node> entry : idMap.entrySet())
            {
                if( entry.getValue() == node )
                {
                    idMap.remove(entry.getKey());
                    return;
                }
            }
        }
        catch( Throwable t )
        {
            log.log(Level.WARNING, "Can not remove unused node, node=" + node.getName(), t);
        }
    }

    ///////////////////////////////////////////////////
    protected void createReaction(Attributes attributes)
    {
        String reactionName = cutPrefix(attributes.getValue("name"), ":");
        String reactionType = attributes.getValue("type");
        createReaction(reactionName, reactionType);
    }

    private void createReaction(String reactionName, String reactionType)
    {
        try
        {
            Base kernel = getKernel(reactionName, REACTION_TYPE);
            if( kernel instanceof Reaction )
            {
                Reaction reaction = (Reaction)kernel;
                currentNode = new Node(diagram, reaction.getName(), reaction);
                currentNode.getAttributes().add(new DynamicProperty(REACTION_TYPE_ATTR, String.class, reactionType));
                diagram.put(currentNode);
            }
        }
        catch( Throwable th )
        {
            log.log(Level.SEVERE, "Can not create reaction '" + reactionName + "', error=" + th, th);
        }
    }

    private void createReactionElement(Attributes attributes, String type)
    {
        if( currentNode != null )
        {
            List<String> rElements = reactionElements.get(currentNode);
            if( rElements == null )
            {
                rElements = new ArrayList<>();
                reactionElements.put(currentNode, rElements);
            }
            String name = attributes.getValue("name");
            rElements.add(name + " as " + type);
        }
    }

    private String cutPrefix(String str, String delim)
    {
        if( str == null || delim == null )
            return null;

        int i = str.indexOf(delim);
        if( i > -1 )
            str = str.substring(i + delim.length());
        return str;
    }

    /**
     * Builds diagram edges which links reaction nodes with corresponding
     * compound nodes (reactants and products), and enzyme nodes.
     *
     * */
    private void createEdges()
    {
        List<Node> reactionNodes = new ArrayList<>();
        Map<String, Base> nameToKernel = new HashMap<>();
        // Iterate through diagram and put reaction nodes to reactionNodes list,
        // build kernel name to kernel map for nodes with kernels of compound type.
        //
        for(Node node : diagram.stream( Node.class ))
        {
            Base kernel = node.getKernel();
            if( kernel instanceof Reaction )
            {
                reactionNodes.add(node);
            }
            else if( kernel instanceof Substance )
            {
                if( !nameToKernel.containsValue(kernel) )
                    nameToKernel.put(kernel.getName(), kernel);
            }
        }

        // iterate through reactionNodes list and build edges.
        for(Node reactionNode : reactionNodes)
        {
            Reaction reaction;
            try
            {
                reaction = (Reaction)reactionNode.getKernel();
            }
            catch( Throwable th )
            {
                continue;
            }

            // Create modifier edges (typically it is a enzyme-to-reaction edges)
            createModifierEdges(reactionNode, reactionLinks.get(reaction.getName()));

            // Create product and reactant edges.
            List<String> elements = reactionElements.get(reactionNode);
            if( elements != null )
            {
                for( int i = 0; i < elements.size(); i++ )
                {
                    Node nodeIn = null;
                    Node nodeOut = null;
                    try
                    {
                        String name = null;
                        String type = null;
                        String[] element = elements.get(i).split(" as ");
                        if( element.length != 2 )
                            throw new Exception("Invalid element name: "+elements.get(i));
                        name = element[0];
                        type = element[1];

                        int idx = name.indexOf(':');
                        if( idx != -1 )
                        {
                            name = name.substring(idx + 1);
                        }

                        String role = null;
                        if( type.equals(SUBSTRATE_ELEMENT) )
                        {
                            nodeIn = findNearestNode(reactionNode, nameToKernel.get(name));
                            nodeOut = reactionNode;
                            role = SpecieReference.REACTANT;
                        }
                        else if( type.equals(PRODUCT_ELEMENT) )
                        {
                            nodeOut = findNearestNode(reactionNode, nameToKernel.get(name));
                            nodeIn = reactionNode;
                            role = SpecieReference.PRODUCT;
                        }

                        if( nodeIn != null && nodeOut != null )
                        {
                            SpecieReference sr = new SpecieReference(reaction, name + " as " + role);
                            sr.setSpecie(Module.DATA + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR + "compound" + ru.biosoft.access.core.DataElementPath.PATH_SEPARATOR
                                    + ru.biosoft.access.core.DataElementPath.escapeName(name));
                            sr.setRole(role);
                            Edge edge = new Edge(diagram, sr, nodeIn, nodeOut);
                            String rType = (String)reactionNode.getAttributes().getValue(REACTION_TYPE_ATTR);
                            if( rType != null )
                            {
                                edge.getAttributes().add(new DynamicProperty(REACTION_TYPE_ATTR, String.class, rType));
                            }
                            diagram.put(edge);
                        }
                    }
                    catch( Throwable th )
                    {
                        log.log(Level.SEVERE, "Cannot create diagram edge: " + th, th);
                    }
                }
            }
        }
    }

    /**
     * Bulds modifier edges (typically its a enzyme to reaction edges).
     * */
    private void createModifierEdges(Node reactionNode, Collection<Node> modifierNodes)
    {
        if( modifierNodes == null )
            return;

        boolean setReactionLocation = true;
        Reaction reaction = (Reaction)reactionNode.getKernel();

        for(Node modifierNode : modifierNodes)
        {
            if( setReactionLocation )
            {
                reactionNode.setLocation(modifierNode.getLocation());
                setReactionLocation = false;
            }

            try
            {
                SpecieReference ref = reaction.get(modifierNode.getName().substring(2));
                if( ref == null )
                {
                    ref = new SpecieReference(reaction, modifierNode.getName() + " as " + SpecieReference.MODIFIER,
                            SpecieReference.MODIFIER);
                    DataCollection<?> specieParent = modifierNode.getKernel().getOrigin();
                    if( specieParent != null )
                    {
                        ref.setSpecie(DataElementPath.create(specieParent, modifierNode.getKernel().getName()).toString());

                        SpecieReference[] oldSpecieReferences = reaction.getSpecieReferences();
                        SpecieReference[] newSpecieReferences = new SpecieReference[oldSpecieReferences.length + 1];
                        System.arraycopy(oldSpecieReferences, 0, newSpecieReferences, 0, oldSpecieReferences.length);
                        newSpecieReferences[oldSpecieReferences.length] = ref;
                        reaction.setSpecieReferences(newSpecieReferences);
                    }
                }

                //put reaction size
                reactionNode.setLocation(modifierNode.getLocation());
                reactionNode.setShapeSize(modifierNode.getShapeSize());

                //modifier edges are not visible for KEGG, for correctly work of orthogonal layout we don't add this edges to graph
                //diagram.put(new Edge(diagram, ref, modifierNode, reactionNode));
            }
            catch( Exception ex )
            {
                log.log(Level.SEVERE, "cannot create modifier->reaction edge: " + reaction.getName() + "->" + modifierNode.getName(), ex);
            }
        }
    }
    protected Node findNearestNode(Node focus, Base kernel)
    {
        if( kernel == null )
            return null;

        Point loc = focus.getLocation();
        return diagram.getKernelNodes(kernel).minBy( node -> node.getLocation().distance( loc ) ).orElse( null );
    }

    protected void createRelation(Attributes attributes)
    {
        String entry1 = attributes.getValue(ENTRY1_ATTRIBUTE);
        String entry2 = attributes.getValue(ENTRY2_ATTRIBUTE);
        String type = attributes.getValue(TYPE_ATTRIBUTE);
        if( entry1 == null || entry2 == null || type == null )
            return;

        currentRelation = new KgmlRelation(entry1, entry2);
        if( type.equals("ECrel") )
        {
            currentRelation.setType(KgmlRelation.TYPE_ENZYME_ENZYME);
        }
        else if( type.equals("PPrel") )
        {
            currentRelation.setType(KgmlRelation.TYPE_PROTEIN_PROTEIN);
        }
        else if( type.equals("PCrel") )
        {
            currentRelation.setType(KgmlRelation.TYPE_PROTEIN_COMPOUND);
        }
        else if( type.equals("GErel") )
        {
            currentRelation.setType(KgmlRelation.TYPE_GE);
        }
        else if( type.equals("GCrel") )
        {
            currentRelation.setType(KgmlRelation.TYPE_GENE_EXPRESSION);
        }
        else if( type.equals("maplink") )
        {
            currentRelation.setType(KgmlRelation.TYPE_MAPLINK);
        }
    }
    protected void createSubtype(Attributes attributes)
    {
        if( currentRelation == null )
            return;

        String name = attributes.getValue(NAME_ATTRIBUTE);
        String value = attributes.getValue(VALUE_ATTRIBUTE);
        if( name == null || value == null )
            return;

        Subtype subtype = new Subtype(name, value);
        currentRelation.addSubtype(subtype);
    }

    protected void storeRelation()
    {
        if( currentRelation != null )
        {
            relations.add(currentRelation);
        }
    }

    protected Edge createRelationEdge(Node from, Node to)
    {
        Edge edge = null;
        SemanticRelation rel = new SemanticRelation(null, from.getName() + "rel" + to.getName());
        rel.setParticipation(Relation.PARTICIPATION_INDIRECT);
        rel.setInputElementName(from.getName());
        rel.setOutputElementName(to.getName());

        try
        {
            edge = new Edge(diagram, rel, from, to);
            diagram.put(edge);
        }
        catch( Throwable th )
        {
            log.log(Level.SEVERE, "cannot create relation edge: " + th.toString(), th);
        }
        return edge;
    }

    protected boolean isNodesTied(Node from, Node to)
    {
        Set<Edge> fromEdges = from.edges().toSet();
        return to.edges().anyMatch( fromEdges::contains );
    }

    protected void createRelationEdges()
    {
        for(KgmlRelation relation : relations)
        {
            if( relation.getType() == KgmlRelation.TYPE_PROTEIN_PROTEIN || relation.getType() == KgmlRelation.TYPE_PROTEIN_COMPOUND
                    || relation.getType() == KgmlRelation.TYPE_GE )
            {
                Node entry1 = idMap.get(relation.getEntry1());
                Node entry2 = idMap.get(relation.getEntry2());

                Edge edge = createRelationEdge(entry1, entry2);
                try
                {
                    for( Subtype subtype : relation.getSubtypes() )
                    {
                        edge.getAttributes().add(new DynamicProperty(subtype.getName(), String.class, subtype.getValue()));
                    }
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE, "Can not set property for edge: from " + entry1.getName() + " to" + entry2.getName(), e);
                }
            }
            else if( relation.getType() == KgmlRelation.TYPE_MAPLINK )
            {
                if( relation.getSubtypes().size() == 0 )
                    continue;

                Subtype subtype = relation.getSubtypes().get(0);

                Node entry1 = idMap.get(relation.getEntry1());
                Node entry2 = idMap.get(relation.getEntry2());
                Node compound = idMap.get(subtype.getValue());

                if( entry1.getKernel() instanceof Protein )
                {
                    Node from = compound;
                    Node to = entry2;
                    /*List reactions = proteinLinks.get(entry1.getName());
                    if( reactions != null )
                    {
                        try
                        {
                            Reaction reaction = (Reaction)diagram.findNode((String)reactions.get(0)).getKernel();
                            String compountName = cutPrefix(compound.getName(), ":");
                            Iterator iter = reaction.iterator();
                            while( iter.hasNext() )
                            {
                                SpecieReference sr = (SpecieReference)iter.next();
                                if( sr.getName().startsWith(compountName) )
                                {
                                    if( sr.getRole().equals(SpecieReference.REACTANT) )
                                    {
                                        from = entry2;
                                        to = compound;
                                    }
                                    break;
                                }
                            }
                        }
                        catch( Exception e )
                        {

                        }
                    }*/
                    createRelationEdge(from, to);
                }
            }
        }
    }

    protected void createComponent(Attributes attributes)
    {
        if( ! ( currentNode instanceof Compartment ) )
            return;

        String id = attributes.getValue(ID_ATTRIBUTE);
        Node component = idMap.get(id);
        if(component == null)
        {
            log.warning("KEGG diagram "+diagram.getCompletePath()+": element#"+id+" not found; skipping");
            return;
        }
        try
        {
            diagram.remove(component.getName());
            ( (Compartment)currentNode ).put(component);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not move node to group: node = " + component.getName(), e);
        }
    }

    static class KgmlRelation
    {
        public static final int TYPE_ENZYME_ENZYME = 0;
        public static final int TYPE_PROTEIN_PROTEIN = 1;
        public static final int TYPE_GENE_EXPRESSION = 2;
        public static final int TYPE_MAPLINK = 3;
        public static final int TYPE_PROTEIN_COMPOUND = 4;
        public static final int TYPE_GE = 5;
        public static final int TYPE_UNDEFINED = -1;

        public KgmlRelation(String entry1, String entry2, int type)
        {
            this.entry1 = entry1;
            this.entry2 = entry2;
            this.type = type;
            this.subtypes = new ArrayList<>();
        }

        public KgmlRelation(String entry1, String entry2)
        {
            this(entry1, entry2, TYPE_UNDEFINED);
        }

        public String getEntry1()
        {
            return entry1;
        }

        public void setEntry1(String entry1)
        {
            this.entry1 = entry1;
        }

        public String getEntry2()
        {
            return entry2;
        }

        public void setEntry2(String entry2)
        {
            this.entry2 = entry2;
        }

        public int getType()
        {
            return type;
        }

        public void setType(int type)
        {
            this.type = type;
        }

        public List<Subtype> getSubtypes()
        {
            return subtypes;
        }

        public void addSubtype(Subtype subtype)
        {
            subtypes.add(subtype);
        }

        private String entry1;
        private String entry2;
        private int type;
        private final List<Subtype> subtypes;
    }

    static class Subtype
    {
        public Subtype(String name, String value)
        {
            this.name = name;
            this.value = value;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public String getValue()
        {
            return value;
        }

        public void setValue(String value)
        {
            this.value = value;
        }

        private String name;
        private String value;
    }
}
