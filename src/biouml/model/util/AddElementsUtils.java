package biouml.model.util;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementGroup;
import biouml.model.DiagramType;
import biouml.model.DiagramTypeConverter;
import biouml.model.Edge;
import biouml.model.Module;
import biouml.model.Node;
import biouml.model.SemanticController;
import biouml.model.xml.XmlDiagramSemanticController;
import biouml.model.xml.XmlDiagramType;
import biouml.model.xml.XmlDiagramTypeConstants;
import biouml.standard.diagram.Util;
import biouml.standard.type.Base;
import biouml.standard.type.BaseSupport;
import biouml.standard.type.DatabaseReference;
import biouml.standard.type.Reaction;
import biouml.standard.type.SemanticRelation;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;
import biouml.workbench.diagram.DiagramEditorHelper;
import biouml.workbench.diagram.DiagramTypeConverterRegistry;
import biouml.workbench.diagram.DiagramTypeConverterRegistry.Conversion;
import biouml.workbench.diagram.ViewEditorPaneStub;
import biouml.workbench.graph.DiagramToGraphTransformer;
import one.util.streamex.StreamEx;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.RelationType;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.graph.FastGridLayouter;
import ru.biosoft.graph.Graph;
import ru.biosoft.graph.Layouter;
import ru.biosoft.graph.PathwayLayouter;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.util.Clazz;

/**
 * Utility class for adding elements to diagram operations.
 */
public class AddElementsUtils
{
    protected static final Logger log = Logger.getLogger( AddElementsUtils.class.getName() );

    public static final String INVISIBLE_ELEMENT_PROPERTY = "invisible";

    /**
     * Add search elements to diagram and layout it. If element elready exist in compartment, it will be skipped.
     * Add all components of new reactions, add edges from new elements to existing reaction nodes.
     * @param location TODO
     * @throws Exception
     */
    public static void addElements(Compartment compartment, Element[] elements, Point location) throws Exception
    {
        addElements( compartment, elements, location, true, true );
    }

    public static void addElements(Compartment compartment, Element[] elements, Point location, boolean correctRelations, boolean addMissed)
            throws Exception
    {
        Diagram diagram = Diagram.getDiagram( compartment );
        List<DiagramElement> previouslyFixed = fixCurrentNodes( diagram, true );
        DiagramTypeConverter[] converters = getAvailableConverters( diagram );
        addNodesToCompartment( elements, compartment, converters, location );
        addEdgesToCompartment( elements, compartment, correctRelations, converters );
        if( addMissed )
        {
            extendReactionNodes( elements, compartment, converters );
            addMissedEdges( getNewlyCreatedNodes( elements, compartment ), compartment, converters );
        }
        if( location == null )
            layoutDiagram( diagram, getLayouter( compartment ) );
        fixCurrentNodes( diagram, false, previouslyFixed );
    }

    /**
     * Get data element converters available for diagram to process nodes and edges
     */
    public static DiagramTypeConverter[] getAvailableConverters(Diagram diagram)
    {
        Conversion[] conversions = DiagramTypeConverterRegistry.getDiagramElementConverter( diagram );
        return StreamEx.of( conversions ).map( Conversion::getConverter ).map( Clazz.of( DiagramTypeConverter.class )::create )
                .toArray( DiagramTypeConverter[]::new );
    }

    /**
     * Set fixed status for all diagram nodes.
     * Returns list of diagram elements that were "fixed" before.
     */
    public static List<DiagramElement> fixCurrentNodes(Compartment compartment, boolean fixed)
    {
        return fixCurrentNodes( compartment, fixed, null );
    }

    /**
     * Set fixed status for all diagram nodes and set !fixed for DiagramElements in restoreList.
     * Returns list of diagram elements that were "fixed" before.
     */
    public static List<DiagramElement> fixCurrentNodes(Compartment compartment, boolean fixed, List<DiagramElement> restoreList)
    {
        List<DiagramElement> previouslyFixed = compartment.recursiveStream().filter( de -> de.isFixed() == fixed ).toList();
        compartment.recursiveStream().forEach( node -> node.setFixed( fixed ) );
        if( restoreList != null )
        {
            restoreList.stream().forEach( de -> de.setFixed( !fixed ) );
        }
        return previouslyFixed;
    }

    /**
     * Add selected element to diagram
     * @throws Exception
     */
    public static void addNodesToCompartment(Element[] elements, Compartment compartment, DiagramTypeConverter[] converters, Point location)
            throws Exception
    {
        for( Element element : elements )
        {
            addNode( compartment, getKernel( element ), converters, isElementVisible( element ), location );
        }
    }

    private static boolean isElementVisible(Element element)
    {
        return ! ( element.getValue( INVISIBLE_ELEMENT_PROPERTY ) instanceof Boolean && (Boolean)element
                .getValue( INVISIBLE_ELEMENT_PROPERTY ) );
    }

    public static DataElement getKernel(Element element)
    {
        DataElement kernel = getKernel( element.getElementPath() );
        if( kernel instanceof Base )
            fillUserElementProperties( (Base)kernel, element );
        return kernel;
    }

    private static void fillUserElementProperties(Base kernel, Element element)
    {
        if( element.getValue( Element.USER_TITLE_PROPERTY ) != null && element.getValue( Element.USER_TITLE_PROPERTY ) instanceof String )
            ( (BaseSupport)kernel ).setTitle( (String)element.getValue( Element.USER_TITLE_PROPERTY ) );

        for( String propertyName : new String[] {Element.USER_REACTANTS_PROPERTY, Element.USER_PRODUCTS_PROPERTY} )
        {
            if( element.getValue( propertyName ) != null )
                kernel.getAttributes().add( new DynamicProperty( propertyName, String.class, element.getValue( propertyName ) ) );
        }
    }

    @CheckForNull
    public static DataElement optKernel(Element element)
    {
        try
        {
            return getKernel( element );
        }
        catch( Exception ex )
        {
            return null;
        }
    }


    /**
     * Return kernel for diagram element
     * If path specifies DiagramElement, returns it's original kernel.
     * If path specifies other ru.biosoft.access.core.DataElement, returns this element.
     * If path specifies no valid element and contains "_", tries to get Reaction with name up to "_" (Workaround for Transpath)
     */

    private static DataElement getKernel(DataElementPath path)
    {
        if(path.isDescendantOf( DatabaseReference.STUB_PATH.getChildPath( "reaction" ) ))
            return new Reaction( null, path.getName() );

        DataElement de = path.optDataElement();
        if(de == null)
        {
            //Workaround for Transpath reactions like XN000000007_1 coming from BioHub
            if( path.getName().contains("_") ) //try to get element without suffix _1
            {
                String originalName = path.getName().substring(0, path.getName().lastIndexOf("_"));
                DataElementPath originalPath = path.getSiblingPath(originalName);
                DataElement originalDe = originalPath.optDataElement();
                if(originalDe != null && originalDe instanceof Reaction)
                {
                    Reaction originalReaction = (Reaction)originalDe;
                    Reaction result = new Reaction( originalReaction.getOrigin(), path.getName() );
                    result.getAttributes().add(new DynamicProperty("Original reaction", String.class, originalPath.toString()));
                    return result;
                }
            }
            return null;
        }
        if( de instanceof DiagramElement )
        {
            Base kernel = ( (DiagramElement)de ).getKernel();
            return kernel;
        }
        else
            return de;
    }

    /**
     * Add node on diagram
     * @param visible whether the node is visible
     * @throws Exception
     */
    public static DiagramElement[] addNode(Compartment cmp, @Nonnull ru.biosoft.access.core.DataElement kernel, DiagramTypeConverter[] converters, boolean visible,
            Point location) throws Exception
    {
        DiagramElement[] nodes = null;
        boolean hasConverter = false;
        if( converters != null && kernel instanceof Base )
        {
            Node testNode = new Node( cmp, (Base)kernel );
            DiagramTypeConverter[] available = StreamEx.of( converters ).filter( c -> c.canConvert( testNode ) )
                    .toArray( DiagramTypeConverter[]::new );
            if( available.length > 0 )
            {
                hasConverter = true;
                nodes = createDiagramElements( cmp, (Base)kernel, available );
                if( nodes == null )
                    throw new Exception( "Can not add element " + kernel.getName() + " with available converters" );
                Base kernelToFind = nodes[0].getKernel();
                DiagramElement existingDE = findNodeWithKernel( cmp, kernelToFind );
                if(existingDE != null )
                    return new DiagramElement[] {existingDE};
                addDiagramElements( cmp, nodes, visible, location );
                return nodes;
            }
        }

        if( !hasConverter )
        {
            Diagram diagram = Diagram.getDiagram( cmp );
            DiagramEditorHelper helper = new DiagramEditorHelper( diagram );
            SemanticController semanticController = diagram.getType().getSemanticController();
            ViewEditorPane viewEditor = new ViewEditorPaneStub( helper, diagram );
            DiagramElementGroup elements = semanticController.addInstanceFromElement( cmp, kernel,
                    location != null ? location : cmp.getLocation(), viewEditor );
            return elements.getElements().toArray( new DiagramElement[elements.size()] );
        }
        return null;
    }

    public static void addDiagramElements(Compartment cmp, DiagramElement[] nodes, boolean visible, Point location)
    {
        Diagram diagram = Diagram.getDiagram( cmp );
        DiagramEditorHelper helper = new DiagramEditorHelper( diagram );
        ViewEditorPane viewEditor = new ViewEditorPaneStub( helper, diagram );
        if( location == null )
            location = cmp.getLocation();
        if( location == null )
            location = new Point( 0, 0 );
        for( DiagramElement de : nodes )
        {
            viewEditor.add( de, location );
            if( !visible && de instanceof Node )
            {
                ( (Node)de ).setVisible( false );
            }
        }
    }

    public static DiagramElement[] createDiagramElements(Compartment cmp, Base base, DiagramTypeConverter[] converters)
            throws Exception
    {
        Diagram diagram = Diagram.getDiagram( cmp );
        DiagramElement[] nodes = null;
        if( converters != null )
        {
            nodes = convertDiagramElement( new Node( cmp, base ), diagram, converters );
            if( nodes == null )
            {
                nodes = new DiagramElement[1];
                if( diagram.getType() instanceof XmlDiagramType )
                {
                    nodes[0] = getXmlDiagramNode( base, cmp, diagram );
                }
                else
                {
                    nodes[0] = new Node( cmp, base );
                }
            }
        }
        else
        {

        }
        return nodes;
    }

    /**
     * Add edges for selected elements to diagram
     */
    public static void addEdgesToCompartment(Element[] elements, Compartment compartment, boolean correctRelations,
            DiagramTypeConverter[] converters)
    {
        for( Element element : elements )
        {
            addSingleEdgeToCompartment( compartment, element, correctRelations, converters );
        }
    }

    /**
     * Add edge on diagram
     */
    protected static void addSingleEdgeToCompartment(Compartment compartment, Element element, boolean correctDirection,
            DiagramTypeConverter[] converters)
    {
        if( element.getLinkedFromPath() != null && !element.getLinkedFromPath().isEmpty())
        {
            Base toBase = (Base)getKernel( element );
            Base fromBase = (Base)getKernel( DataElementPath.create( element.getLinkedFromPath() ) );
            if( fromBase != null )
            {
                //KeyNodes hubs return correct relation directions,
                //so we need to correct only in case of other BioHub searches
                if( correctDirection && element.getLinkedDirection() == BioHub.DIRECTION_UP )
                {
                    Base tmp = toBase;
                    toBase = fromBase;
                    fromBase = tmp;
                }
                try
                {
                    Node toNode = compartment.findNode( toBase.getName() );
                    Node fromNode = compartment.findNode( fromBase.getName() );
                    if( toNode == null )
                    {
                        DiagramElement de = findNodeWithKernel( compartment, toBase );
                        if( de != null && de instanceof Node )
                            toNode = (Node)de;
                        else
                            toNode = Diagram.getDiagram( compartment ).findNode( toBase.getName() );
                    }
                    if( fromNode == null )
                    {
                        DiagramElement de = findNodeWithKernel( compartment, fromBase );
                        if( de != null && de instanceof Node )
                            fromNode = (Node)de;
                        else
                            fromNode = Diagram.getDiagram( compartment ).findNode( fromBase.getName() );
                    }
                    if( ( toNode != null ) && ( fromNode != null ) )
                    {
                        addEdge( fromNode, toNode, element.getRelationType(), compartment, converters );
                    }
                }
                catch( Exception e )
                {
                    log.log(Level.SEVERE, "Can not add edge to diagram", e);
                }
            }
        }
    }

    public static void addEdge(@Nonnull Node fromNode, @Nonnull Node toNode, String edgeType, Compartment compartment,
            DiagramTypeConverter[] converters) throws Exception
    {
        DiagramElement[] des = createEdge( fromNode, toNode, edgeType, compartment, converters );
        if( des == null )
            return;
        Compartment origin = Node.findCommonOrigin(toNode, fromNode);
        for(DiagramElement de: des)
        {
            origin.put( de );
        }
    }

    public static DiagramElement[] createEdge(@Nonnull Node fromNode, @Nonnull Node toNode, String edgeType, Compartment compartment,
            DiagramTypeConverter[] converters) throws Exception
    {
        Diagram diagram = Diagram.getDiagram( compartment );
        SemanticController sc = diagram.getType().getSemanticController();
        Compartment origin = Node.findCommonOrigin(toNode, fromNode);
        Base edgeKernel = null;
        Edge edge = null;
        Node specieNode = toNode;
        Reaction reaction = getReactionByNode(fromNode);
        if( reaction == null )
        {
            reaction = getReactionByNode(toNode);
            specieNode = fromNode;
        }

        Base kernel = specieNode.getKernel();
        DataElementPath modulePath = Module.optModulePath( kernel );
        if( reaction != null )
        {
            String speciePath = modulePath != null ? kernel.getCompletePath().getPathDifference( modulePath ) : kernel.getName();
            String searchType = edgeType;
            List<SpecieReference> species = reaction.stream()
                    .filter( sp -> sp.getSpecie().equals( speciePath ) && sp.getRole().equals( searchType ) )
                    .collect( Collectors.toList() );
            if( species.size() > 0 )
            {
                edgeKernel = species.get( 0 );
            }
            if( edgeKernel != null )
            {
                edge = sc.findEdge( fromNode, toNode, edgeKernel );
                if( edge != null )
                    return null;
                else
                {
                    //in some cases edge can be rotated by converter
                    edge = sc.findEdge( toNode, fromNode, edgeKernel );
                    if( edge != null )
                        return null;
                }
                edge = new Edge( origin, edgeKernel, fromNode, toNode );
            }
        }
        if( edge == null )
            edge = sc.createEdge( fromNode, toNode, edgeType, origin );

        if( edge == null )
            return null;

        DiagramElement[] converted = convertDiagramElement( edge, diagram, converters );
        if( converted != null )
        {
            return converted;
        }
        else
        {
            return new DiagramElement[] {edge};
        }
    }

    public static boolean edgeExists(Node from, Node to, Base kernel)
    {
        Edge[] edges = from.getEdges();
        for( Edge e : edges )
        {
            if(e.getInput().getName().equals(from.getName()) && e.getOutput().getName().equals(to.getName()))
            {
                if(e.getKernel() != null && e.getKernel() instanceof SpecieReference && kernel instanceof SpecieReference)
                {
                    if(e.getKernel().getOrigin() != null && kernel.getOrigin() != null && e.getKernel().getOrigin().getCompletePath().equals(kernel.getOrigin().getCompletePath()))
                        return true;
                }
            }
        }
        return false;
    }

    /**
     * Add nodes linked to Reaction element
     * @param elements - array of SearchElements added to diagram
     * @param diagram - parent diagram
     * @param converters - DiagramTypeConverter, used for node processing
     */
    protected static void extendReactionNodes(Element[] elements, Compartment compartment, DiagramTypeConverter[] converters)
    {
        for( Element element : elements )
        {
            try
            {
                Base base = (Base)getKernel( element );
                if( base instanceof Reaction )
                {
                    Module module = Module.getModule(base);
                    Node reactionNode = compartment.findNode( base.getName() );
                    if( reactionNode != null )
                    {
                        Reaction reaction = (Reaction)base;
                        for( SpecieReference sr : reaction.getSpecieReferences() )
                        {
                            String specieName = sr.getSpecie();
                            Base specieBase = (Base)module.getKernel(specieName);
                            if( specieBase != null )
                            {
                                boolean needEdge = true;
                                //ru.biosoft.access.core.DataElement specieDe = compartment.get( specieBase.getName() );
                                Node specieNode = null;
                                DiagramElement[] added = addNode( compartment, specieBase, converters, true, reactionNode.getLocation() );
                                if( added != null && added.length > 0 && added[0] instanceof Node )
                                {
                                    specieNode = (Node)added[0];
                                }
                                if( specieNode != null )
                                {
                                    Edge[] edges = reactionNode.getEdges();
                                    for( Edge e : edges )
                                    {
                                        Node in = e.getInput();
                                        Node out = e.getOutput();
                                        if( ( in.getName().equals(reaction.getName()) && out.getName().equals(specieBase.getName()) )
                                                || ( out.getName().equals(reaction.getName()) && in.getName().equals(specieBase.getName()) ) )
                                        {
                                            needEdge = false;
                                            break;
                                        }
                                    }
                                }
                                if( needEdge )
                                {
                                    if( sr.getRole().equals(SpecieReference.PRODUCT) )
                                    {
                                        addEdge( reactionNode, specieNode, sr.getRole(), compartment, converters );
                                    }
                                    else
                                    {
                                        addEdge( specieNode, reactionNode, sr.getRole(), compartment, converters );
                                    }
                                }
                            }
                        }
                    }
                }
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Can not add reaction components to diagram", e);
            }
        }
    }

    /**
     * Now we suppose that nodes should not be duplicated when adding elements 
     * This method tries to find node with same kernel on diagram
     * It's better to create node first because converters may change kernel to specific for current diagram
     */
    private static DiagramElement findNodeWithKernel(Compartment compartment, Base kernel)
    {
        //First we try to find node in the same compartment
        if( Util.hasNodeWithKernel( compartment, kernel ) )
            return compartment.get( kernel.getName() );
        //Then in full diagram
        Diagram diagram = Diagram.getDiagram( compartment );
        if( Util.hasNodeWithKernel( diagram, kernel ) )
            return diagram.get( kernel.getName() );
        //Then in other compartments
        Compartment cmpWithNode = (Compartment)diagram.recursiveStream()
                .filter( node -> node.getKernel() != null && node instanceof Compartment
                        && node.getKernel().getType().equals( Type.TYPE_COMPARTMENT ) )
                .findAny( cmp -> Util.hasNodeWithKernel( (Compartment)cmp, kernel ) ).orElse( null );
        if( cmpWithNode != null )
            return cmpWithNode.get( kernel.getName() );
        return null;
    }

    private static List<Node> getNewlyCreatedNodes(Element[] elements, Compartment compartment)
    {
        List<Node> nodes = new ArrayList<>();
        for( Element element : elements )
        {
            DataElement de = getKernel( element );
            if( ! ( de instanceof Reaction ) )
            {
                Node node = compartment.findNode( de.getName() );
                if( node != null )
                    nodes.add( node );
            }
        }
        return nodes;
    }

    public static void addMissedEdges(Element[] elements, Compartment compartment, DiagramTypeConverter[] converters)
    {
        addMissedEdges( getNewlyCreatedNodes( elements, compartment ), compartment, converters );
    }
    /**
     * Add missed edges from newly added elements to reaction nodes, existing on diagram.
     */
    protected static void addMissedEdges(List<Node> nodes, Compartment compartment, DiagramTypeConverter[] converters)
    {
        List<Node> reactionNodes = compartment.stream( Node.class )
                .filter( node -> node.getKernel() != null && node.getKernel() instanceof Reaction ).toList();
        for( Node node : nodes )
        {
            for( Node reactionNode : reactionNodes )
            {
                Reaction reaction = (Reaction)reactionNode.getKernel();
                Object reactionNameObj = reactionNode.getAttributes().getValue( DataCollectionConfigConstants.COMPLETE_NAME_PROPERTY );
                if( reactionNameObj != null )
                {
                    DataElement reactionDe = DataElementPath.create( reactionNameObj.toString() ).optDataElement();
                    if( reactionDe != null && reactionDe instanceof Reaction )
                        reaction = (Reaction)reactionDe;
                }
                for( SpecieReference sr : reaction.getSpecieReferences() )
                {
                    String specieName = sr.getSpecieName();
                    if( specieName.equals( node.getName() ) )
                    {
                        boolean needEdge = true;
                        Edge[] edges = reactionNode.getEdges();
                        for( Edge e : edges )
                        {
                            Node in = e.getInput();
                            Node out = e.getOutput();
                            if( ( in.getName().equals( reactionNode.getName() ) && out.getName().equals( specieName ) )
                                    || ( out.getName().equals( reactionNode.getName() ) && in.getName().equals( specieName ) ) )
                            {
                                needEdge = false;
                                break;
                            }
                        }
                        if( needEdge )
                        {
                            try
                            {
                                if( sr.getRole().equals( SpecieReference.PRODUCT ) )
                                {
                                    addEdge( reactionNode, node, sr.getRole(), compartment, converters );
                                }
                                else
                                {
                                    addEdge( node, reactionNode, sr.getRole(), compartment, converters );
                                }
                            }
                            catch( Exception e )
                            {
                                log.log( Level.SEVERE, "Can not add edge to diagram", e );
                            }
                        }
                        break;
                    }
                }
            }
        }
    }

    private static DiagramElement[] convertDiagramElement(DiagramElement de, Diagram diagram, DiagramTypeConverter[] converters)
            throws Exception
    {
        if( converters != null )
        {
            for( DiagramTypeConverter converter : converters )
            {
                DiagramElement[] nodes = converter.convertDiagramElement(de, diagram);
                if( nodes != null )
                {
                    if( diagram.getType() instanceof XmlDiagramType )
                    {
                        XmlDiagramType xmlDiagramType = (XmlDiagramType)diagram.getType();
                        if( xmlDiagramType.getSemanticController() instanceof XmlDiagramSemanticController )
                        {
                            XmlDiagramSemanticController sc = (XmlDiagramSemanticController)xmlDiagramType.getSemanticController();
                            for( int j = 0 ; j < nodes.length; j++ )
                            {
                                nodes[j] = sc.getPrototype().validate(diagram, nodes[j]);
                            }
                        }
                    }
                    return nodes;
                }
            }
        }
        return null;
    }

    private static Node getXmlDiagramNode(@Nonnull Base base, Compartment origin, Diagram diagram) throws Exception
    {
        Node node;
        XmlDiagramType xmlDiagramType = (XmlDiagramType)diagram.getType();
        String typeStr = xmlDiagramType.getKernelTypeName(base.getClass());
        if( typeStr == null )
            typeStr = xmlDiagramType.getDefaultTypeName();
        if( typeStr == null )
            typeStr = "";
        boolean isCompartment = xmlDiagramType.checkCompartment(typeStr);
        if( isCompartment )
        {
            node = new Compartment(origin, base);
            node.setShapeSize(new Dimension(0, 0));//reset dimension
        }
        else
        {
            node = new Node(origin, base);
        }
        SemanticController sc = xmlDiagramType.getSemanticController();
        if( sc instanceof XmlDiagramSemanticController )
        {
            DynamicPropertySet attributes = ( (XmlDiagramSemanticController)sc ).createAttributes(typeStr);
            for(DynamicProperty attribute : attributes)
            {
                node.getAttributes().add(attribute);
            }
            node = (Node) ( (XmlDiagramSemanticController)sc ).getPrototype().validate(origin, node);
        }
        if( typeStr.length() > 0 )
        {
            DynamicProperty dp = new DynamicProperty(XmlDiagramTypeConstants.XML_TYPE_PD, String.class, typeStr);
            node.getAttributes().add(dp);
        }
        return node;
    }

    private static void layoutDiagram(Diagram diagram, Layouter layouter)
    {
        // Generate diagram view as it may update sizes and visibility
        ImageGenerator.generateDiagramView( diagram, ApplicationUtils.getGraphics() );
        PathwayLayouter pathwayLayouter = new PathwayLayouter( layouter );
        Graph graph = DiagramToGraphTransformer.generateGraph( diagram, null );
        pathwayLayouter.doLayout( graph, null );
        DiagramToGraphTransformer.applyLayout( graph, diagram );
    }

    private static Layouter getLayouter(Compartment compartment)
    {
        FastGridLayouter layouter = new FastGridLayouter();
        //TODO: set layouter options depending on compartment where elements are added
        // if compartment has fixed size, lower grid values should be set, than if compartment is diagram
        if( compartment.isFixed() )
        {
            layouter.setGridX( 40 );
            layouter.setGridY( 30 );
        }
        else
        {
            layouter.setGridX( 60 );
            layouter.setGridY( 40 );
        }
        layouter.setIterations( 2 );
        layouter.setThreadCount( 1 );
        layouter.setCool( 0.7 );
        return layouter;
    }

    private static Reaction getReactionByNode(Node node)
    {
        Base kernel = node.getKernel();
        if( kernel != null )
        {
            if( kernel instanceof Reaction )
                return (Reaction)kernel;
            else if( kernel instanceof Stub && kernel.getType().equals(Type.TYPE_REACTION) )
            {
                Object reactionNameObj = node.getAttributes().getValue( DataCollectionConfigConstants.COMPLETE_NAME_PROPERTY );
                if( reactionNameObj != null )
                {
                    DataElement reactionDe = CollectionFactory.getDataElement(reactionNameObj.toString());
                    if( reactionDe instanceof Reaction )
                    {
                        return (Reaction)reactionDe;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Add elements from one diagram to another. Preserve original node layout. Edges will be relayouted.
     * @throws Exception
     */
    //TODO: move this to Semantic controller
    public static void addDiagram(Compartment compartment, Diagram sourceDiagram, Point addPoint) throws Exception
    {
        Diagram diagram = Diagram.getDiagram( compartment );
        DiagramType targetType = diagram.getType();
        DiagramType sourceType = sourceDiagram.getType();
        if( ! ( targetType.getClass().isInstance( sourceType ) ) && ! ( sourceType.getClass().isInstance( targetType ) ) )
        {
            log.log( Level.WARNING, "Source diagram type " + sourceType.getTitle() + " is not compartible with target diagram type "
                    + targetType.getTitle() );
            DiagramEditorHelper helper = new DiagramEditorHelper( diagram );
            SemanticController semanticController = diagram.getType().getSemanticController();
            ViewEditorPane viewEditor = new ViewEditorPaneStub( helper, diagram );
            semanticController.addInstanceFromElement( compartment, sourceDiagram, addPoint != null ? addPoint : compartment.getLocation(),
                    viewEditor );
        }
        else
        {
            copyDiagramByElements( compartment, sourceDiagram );
        }
    }

    private static void copyDiagramByElements(Compartment compartment, Diagram sourceDiagram)
            throws Exception
    {

        Diagram diagram = Diagram.getDiagram( compartment );
        SemanticController sc = diagram.getType().getSemanticController();

        ImageGenerator.generateDiagramView( diagram, ApplicationUtils.getGraphics() );
        Rectangle rect = diagram.getView().getBounds();
        List<DiagramElement> previouslyFixed = fixCurrentNodes( diagram, true );
        DiagramTypeConverter[] converters = getAvailableConverters( diagram );
        int shiftX = rect.x + rect.width + 10;
        int shiftY = rect.y + rect.height + 10;
        if( shiftX > shiftY ) //add below original diagram
            shiftX = 0;
        else //add to the right from original diagram
            shiftY = 0;

        List<Node> newNodes = new ArrayList<>();
        //Add nodes, skip existing ones
        for( DiagramElement de : sourceDiagram )
        {
            if( de instanceof Node )
            {
                if( compartment.findNode( de.getName() ) == null )
                {
                    Base kernel = de.getKernel();
                    if( kernel instanceof Reaction ) //first add molecules and than reactions
                        continue;
                    addNode( compartment, de.getKernel(), converters, ( (Node)de ).isVisible(), null );
                    Node newNode = compartment.findNode( de.getName() );
                    Point location = ( (Node)de ).getLocation();
                    Dimension offset = new Dimension( location.x + shiftX, location.y + shiftY );
                    try
                    {
                        sc.move( newNode, compartment, offset, null );
                    }
                    catch( Exception e )
                    {
                        log.log( Level.SEVERE, "Can not move element", e );
                    }
                    newNode.setFixed( true );
                    newNodes.add( newNode );
                }
            }
        }
        for( DiagramElement de : sourceDiagram )
        {
            if( de instanceof Node && de.getKernel() instanceof Reaction )
            {
                if( compartment.findNode( de.getName() ) == null )
                {
                    addNode( compartment, de.getKernel(), converters, ( (Node)de ).isVisible(), null );
                    Node newNode = compartment.findNode( de.getName() );
                    Point location = ( (Node)de ).getLocation();
                    Dimension offset = new Dimension( location.x + shiftX, location.y + shiftY );
                    try
                    {
                        sc.move( newNode, compartment, offset, null );
                    }
                    catch( Exception e )
                    {
                        log.log( Level.SEVERE, "Can not move element", e );
                    }
                    newNode.setFixed( true );
                    newNodes.add( newNode );
                }
            }
        }

        for( DiagramElement de : sourceDiagram )
        {
            if( de instanceof Edge )
            {
                Edge edge = (Edge)de;
                Node newInput = compartment.findNode( edge.getInput().getName() );
                Node newOutput = compartment.findNode( edge.getOutput().getName() );
                if( newInput != null && newOutput != null )
                    try
                    {
                        String edgeType;
                        if( edge.getKernel() instanceof SpecieReference )
                            edgeType = ( (SpecieReference)edge.getKernel() ).getRole();
                        else if( edge.getKernel() instanceof SemanticRelation )
                            edgeType = RelationType.SEMANTIC;
                        else
                            edgeType = RelationType.NOTE_LINK;

                        addEdge( newInput, newOutput, edgeType, compartment, converters );
                    }
                    catch( Exception e )
                    {
                        log.log( Level.SEVERE, "Can not add edge to diagram", e );
                    }
            }
        }

        addMissedEdges( newNodes, compartment, converters );
        layoutDiagram( diagram, getLayouter( diagram ) );
        fixCurrentNodes( diagram, false, previouslyFixed );
    }
}
