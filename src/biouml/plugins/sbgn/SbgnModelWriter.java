package biouml.plugins.sbgn;

import java.awt.Dimension;
import java.awt.Point;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementStyle;
import biouml.model.DiagramType;
import biouml.model.Edge;
import biouml.model.ModelDefinition;
import biouml.model.Node;
import biouml.model.SubDiagram;
import biouml.model.SubDiagram.PortOrientation;
import biouml.model.dynamics.DirectedConnection;
import biouml.model.dynamics.SimpleTableElement;
import biouml.model.dynamics.UndirectedConnection;
import biouml.model.util.DiagramWriter;
import biouml.model.util.DiagramXmlConstants;
import biouml.model.util.DiagramXmlWriter;
import biouml.model.util.XmlSerializationUtils;
import biouml.plugins.sbml.SbmlModelWriter;
import biouml.standard.diagram.Bus;
import biouml.standard.diagram.Util;
import biouml.standard.state.State;
import biouml.standard.state.StateXmlSerializer;
import biouml.standard.type.Specie;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import biouml.standard.type.Stub.ConnectionPort;
import biouml.standard.type.Type;
import ru.biosoft.graph.Path;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.font.ColorFont;

/**
 * Write SBGN diagrams to XML format
 */
public class SbgnModelWriter extends SBGNXmlConstants implements DiagramWriter
{
    protected Map<String, String> newPaths = new HashMap<>();
    private static final String COLUMN = "column";
    private static final String VARIABLE = "variable";
    private static final String TABLE_PATH = "tablePath";
    protected static final Logger log = Logger.getLogger(SbgnModelWriter.class.getName());
    protected Document doc;
    protected Diagram diagram;
    protected Diagram baseDiagram;
    protected String rootElementTag = SBGN_ELEMENT;
    protected SbmlModelWriter sbmlWriter;

    
    public SbgnModelWriter(Diagram diagram, Diagram baseDiagram)
    {
        this.diagram = diagram;
        this.baseDiagram = baseDiagram;
    }

    public void setNewPaths(Map<String, String> newPaths)
    {
        this.newPaths = newPaths;
    }  
    
    public void write(OutputStream stream) throws Exception
    {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        Document doc = createDocument();
        Element root = write(doc);
        doc.appendChild(root);
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(stream);
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.MEDIA_TYPE, "text/xml");
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
        transformer.transform(source, result);
    }

    public Element write(Document doc) throws DOMException
    {
        this.doc = doc;
        if( !hasCorrectDiagramType() )
            return null;
        Element element = createRootElement( doc );

        writeNodes(element, diagram);
        writeEdges(element, diagram);
        writeStates(element, diagram);

        Element viewOptions = DiagramXmlWriter.createDiagramViewOptions(doc, diagram, "viewOptions", false);
        if( viewOptions != null )
            element.appendChild(viewOptions);

        Element filters = DiagramXmlWriter.createFilters( doc, diagram );
        if( filters != null )
            element.appendChild( filters );

        writeLayouters( element, diagram );

        return element;
    }

    protected boolean hasCorrectDiagramType()
    {
        Class<? extends DiagramType> diagramTypeClass = diagram.getType().getClass();
        if( SbgnDiagramType.class.equals( diagramTypeClass ) || SbgnCompositeDiagramType.class.equals( diagramTypeClass ) )
            return true;

        log.log( Level.SEVERE, "Incorrect diagram type, should be SbgnDiagramType or SbgnCompositeDiagramType" );
        return false;
    }

    protected Element createRootElement(Document doc)
    {
        Element element = doc.createElement( rootElementTag );
        element.setAttribute( BIOUML_XMLNS_ATTR, BIOUML_XMLNS_VALUE );
        return element;
    }

    protected Document createDocument() throws Exception
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.newDocument();
    }

    protected void writeNodes(Element parent, Diagram diagram) throws DOMException
    {
        Element nodes = doc.createElement(NODES_ELEMENT);
        diagram.recursiveStream().select(Node.class).without(diagram).forEach(n -> writeNode(nodes, n));
        parent.appendChild(nodes);
    }

    protected void writeEdges(Element parent, Diagram diagram) throws DOMException
    {
        Element edges = doc.createElement(EDGES_ELEMENT);
        diagram.recursiveStream().select(Edge.class).forEach(e -> writeEdge(edges, e));
        parent.appendChild(edges);
    }

    protected void writeStates(Element parent, Diagram diagram) throws DOMException
    {
        Element states = doc.createElement(STATES_ELEMENT);
        diagram.states().forEach(s -> writeState(states, s));
        parent.appendChild(states);
    }

    @Override
    public void writeNode(Element parent, @Nonnull Node node)
    {
        try
        {
            Element nodeElement = doc.createElement( NODE_ELEMENT );
            nodeElement.setAttribute( ID_ATTR, node.getName() );
            String type = node.getKernel().getType();

            nodeElement.setAttribute( TYPE_ATTR, type );
            if( type.equals( Type.TYPE_REACTION ) )
                nodeElement.setAttribute( REACTION_TYPE_ATTR,
                        (String)node.getAttributes().getValue( SBGNPropertyConstants.SBGN_REACTION_TYPE ) );

            Compartment origin = (Compartment)node.getOrigin();
            nodeElement.setAttribute( PARENT_ATTR, origin.getCompleteNameInDiagram() );

            if( node.getTitle() != null && !node.getTitle().equals( node.getName() ) )
                nodeElement.setAttribute( TITLE_ATTR, node.getTitle() );

            DiagramElement de = node;
                    
            boolean isComplex = node.getKernel().getType().equals( "complex" );
            boolean isReaction = Util.isReaction( node );
            if( ( (isComplex || isReaction) && node.isShowTitle() ) || ( !isComplex && !isReaction && !node.isShowTitle() ) )
                nodeElement.setAttribute( SHOW_TITLE_ATTR, String.valueOf( node.isShowTitle() ) );

            if( de != null && de.getKernel() != null )
            {
                String refStr = null;
                if( ! ( de.getKernel() instanceof Stub ) )
                    refStr = node.getName(); //check

                if( de.getKernel() instanceof Specie )
                {
                    refStr = de.getRole() == null ? null : node.getRole().getDiagramElement().getCompleteNameInDiagram(); //additional species (e.g. inside complex) do not correspond to sbml object
                    initAttribute( node, nodeElement, SBGNPropertyConstants.SBGN_CLONE_MARKER, CLONE_ATTR );
                    initAttribute( node, nodeElement, SBGNPropertyConstants.SBGN_MULTIMER, MULTIMER_ATTR );
                    initAttribute( node, nodeElement, Util.COMPLEX_STRUCTURE, Util.COMPLEX_STRUCTURE );
                }
                else if( de.getKernel() instanceof biouml.standard.type.Compartment )
                {
                    refStr = node.getRole().getDiagramElement().getCompleteNameInDiagram();
                }
                else if( de.getKernel() instanceof ConnectionPort )
                {
                    if( de.getAttributes().getProperty( ConnectionPort.VARIABLE_NAME_ATTR ) != null )
                        refStr = de.getAttributes().getProperty( ConnectionPort.VARIABLE_NAME_ATTR ).getValue().toString();
                    initAttribute( node, nodeElement, PortOrientation.ORIENTATION_ATTR );
                    initAttribute( node, nodeElement, "accessType" );
                    if( node.getParent() instanceof SubDiagram )
                        initAttribute( node, nodeElement, SubDiagram.ORIGINAL_PORT_ATTR );
                }
                else if( de instanceof SubDiagram )
                {
                    refStr = ( (Compartment)de ).getName();
                    if( ( (SubDiagram)de ).getState() != null )
                        nodeElement.setAttribute( "state", ( (SubDiagram)de ).getState().getName() );
                }
                else if( de instanceof ModelDefinition )
                {
                    refStr = ( (Compartment)de ).getName();
                }
                else if( SbgnUtil.isLogical( node ) )
                {
                    initAttribute( node, nodeElement, SBGNPropertyConstants.ORIENTATION );
                    initAttribute( node, nodeElement, SBGNPropertyConstants.SBGN_LOGICAL_OPERATOR, OPERATOR_TYPE_ATTR );
                }
                else if( node.getRole() instanceof SimpleTableElement )
                {
                    SimpleTableElement table = node.getRole( SimpleTableElement.class );
                    String path = table.getTablePath().toString();
                    if( newPaths != null && newPaths.containsKey( path ) )
                        path = newPaths.get( path );
                    nodeElement.setAttribute( TABLE_PATH, path );
                    Element argColumnElement = doc.createElement( ARGCOLUMN_ELEMENT );
                    argColumnElement.setAttribute( VARIABLE, table.getArgColumn().getVariable() );
                    argColumnElement.setAttribute( COLUMN, table.getArgColumn().getColumn() );
                    for( int i = 0; i < table.getColumns().length; i++ )
                    {
                        Element varColumnElement = doc.createElement( VARCOLUMN_ELEMENT );
                        varColumnElement.setAttribute( VARIABLE, table.getColumns()[i].getVariable() );
                        varColumnElement.setAttribute( COLUMN, table.getColumns()[i].getColumn() );
                        nodeElement.appendChild( varColumnElement );
                    }
                    nodeElement.appendChild( argColumnElement );
                }
                else if( Util.isBus( node ) )
                {
                    Bus bus = node.getRole( Bus.class );
                    nodeElement.setAttribute( "busName", bus.getName() );
                    nodeElement.setAttribute( "brush", XmlSerializationUtils.getBrushString( new Brush(bus.getColor()) ) );
                    if( bus.isDirected() )
                        nodeElement.setAttribute( "directed", String.valueOf( bus.isDirected() ) );
                }

                if( refStr != null )
                    nodeElement.setAttribute( REF_ATTR, refStr );
            }

            if( !node.isVisible() )
                nodeElement.setAttribute( VISIBLE_ATTR, "false" );
            if( node.isFixed() )
                nodeElement.setAttribute( FIXED_ATTR, String.valueOf( node.isFixed() ) );

            writeNodeLayout( nodeElement, node );
            writeNodeTitle( nodeElement, node );
            writeNodePaint( nodeElement, node );

            //ports support
            parent.appendChild( nodeElement );
        }
        catch( Exception ex )
        {
            log.log( Level.SEVERE, "Can not write node " + node.getName(), ex );
        }
    }

    protected void initAttribute(DiagramElement de, Element nodeElement, String propertyName)
    {
        initAttribute(de, nodeElement, propertyName, propertyName);
    }

    protected void initAttribute(DiagramElement de, Element nodeElement, String propertyName, String attrName)
    {
        Object property = de.getAttributes().getValue(propertyName);
        if( property != null )
            nodeElement.setAttribute(attrName, property.toString());
    }

    protected void writeNodeLayout(Element parent, Node node)
    {
        Element nodeLayout = doc.createElement(NODE_LAYOUT_ELEMENT);
        nodeLayout.setAttribute(X_ATTR, String.valueOf(node.getLocation().x));
        nodeLayout.setAttribute(Y_ATTR, String.valueOf(node.getLocation().y));
        Dimension shapeSize = node.getShapeSize();
        if( shapeSize != null )
        {
            nodeLayout.setAttribute(WIDTH_ATTR, String.valueOf(shapeSize.width));
            nodeLayout.setAttribute(HEIGHT_ATTR, String.valueOf(shapeSize.height));
        }

        Object orientationObj = node.getAttributes().getValue(PortOrientation.ORIENTATION_ATTR);
        if( orientationObj != null )
            nodeLayout.setAttribute(PortOrientation.ORIENTATION_ATTR, orientationObj.toString());

        parent.appendChild(nodeLayout);
    }

    protected void writeNodePaint(Element parent, Node node)
    {
        if( node.getPredefinedStyle().equals(DiagramElementStyle.STYLE_DEFAULT) )
        {
            if( node.getKernel() instanceof Stub.Note && ! ( (Stub.Note)node.getKernel() ).isBackgroundVisible() )
            {
                Element nodePaint = doc.createElement(NODE_PAINT_ELEMENT);
                nodePaint.setAttribute(SBGNPropertyConstants.BACKGROUND_VISIBLE_ATTR, "false");
                parent.appendChild(nodePaint);
            }
            return;
        }

        Element nodePaint = doc.createElement(NODE_PAINT_ELEMENT);

        if( node.isStylePredefined() )
        {
            nodePaint.setAttribute(SBGNPropertyConstants.STYLE_ATTR, node.getPredefinedStyle());
        }
        else
        {
            DiagramElementStyle style = node.getCustomStyle();
            nodePaint.setAttribute(SBGNPropertyConstants.LINE_PEN_ATTR, XmlSerializationUtils.getPenString(style.getPen()));
            nodePaint.setAttribute(SBGNPropertyConstants.BRUSH_ATTR, XmlSerializationUtils.getBrushString(style.getBrush()));
        }

        if( node.getKernel() instanceof Stub.Note && ! ( (Stub.Note)node.getKernel() ).isBackgroundVisible() )
            nodePaint.setAttribute(SBGNPropertyConstants.BACKGROUND_VISIBLE_ATTR, "false");

        parent.appendChild(nodePaint);
    }

    protected void writeNodeTitle(Element parent, Node node)
    {
        Element nodeTitle = doc.createElement(NODE_TITLE_ELEMENT);

        Object titlePoint = node.getAttributes().getValue(SBGNPropertyConstants.NAME_POINT_ATTR);

        if( titlePoint instanceof Point )
        {
            nodeTitle.setAttribute(X_ATTR, Integer.toString( ( (Point)titlePoint ).x));
            nodeTitle.setAttribute(Y_ATTR, Integer.toString( ( (Point)titlePoint ).y));
        }

        if( !node.isStylePredefined() )
        {
            ColorFont font = node.getCustomStyle().getFont();
            nodeTitle.setAttribute(SBGNPropertyConstants.TITLE_FONT_ATTR, XmlSerializationUtils.getFontString(font));
        }
        if( nodeTitle.hasAttributes() )
            parent.appendChild(nodeTitle);
    }

    @Override
    public void writeEdge(Element parent, @Nonnull Edge edge)
    {
        try
        {
            Element edgeElement = doc.createElement( EDGE_ELEMENT );
            edgeElement.setAttribute( ID_ATTR, edge.getName() );
            edgeElement.setAttribute( TYPE_ATTR, edge.getKernel().getType() );
            edgeElement.setAttribute( FROM_ATTR, edge.getInput().getCompleteNameInDiagram() );
            edgeElement.setAttribute( TO_ATTR, edge.getOutput().getCompleteNameInDiagram() );

            if( edge.getRole() != null ) //connections between in-module port and propagated port does not contain role
            {
                if( Util.isUndirectedConnection( edge ) )
                {
                    edgeElement.setAttribute( MAIN_VAR_ATTR, edge.getRole( UndirectedConnection.class ).getMainVariableType().toString() );
                    edgeElement.setAttribute( CONVERSION_FACTOR_ATTR, edge.getRole( UndirectedConnection.class ).getConversionFactor() );
                }
                else if( Util.isDirectedConnection( edge ) )
                {
                    edgeElement.setAttribute( FUNCTION_ATTR, edge.getRole( DirectedConnection.class ).getFunction() );
                }
            }
            if( edge.getKernel() instanceof SpecieReference  )
            {
                String edgeType = edge.getAttributes().getValueAsString( SBGNPropertyConstants.SBGN_EDGE_TYPE );
                if( !biouml.plugins.sbgn.Type.TYPE_LOGIC_ARC.equals( edgeType ) )
                    edgeElement.setAttribute( EDGE_TYPE_ATTR, ( (SpecieReference)edge.getKernel() ).getModifierAction() );
            }
            initAttribute( edge, edgeElement, SBGNPropertyConstants.SBGN_EDGE_TYPE, EDGE_TYPE_ATTR );
            initAttribute( edge, edgeElement, TEXT_ATTR );

            writePath( edgeElement, edge );
            writeEdgePaint( edgeElement, edge );

            if( !biouml.plugins.sbgn.Type.TYPE_PORTLINK.equals( edge.getKernel().getType() ) )
                edgeElement.setAttribute( REF_ATTR, edge.getName() );

            if( edge.isFixed() )
                edgeElement.setAttribute( FIXED_ATTR, String.valueOf( edge.isFixed() ) );

            if( edge.isFixedInOut() )
                edgeElement.setAttribute( FIXED_IN_OUT_ATTR, String.valueOf( edge.isFixedInOut() ) );

            parent.appendChild( edgeElement );
        }
        catch( Exception ex )
        {
            log.log( Level.SEVERE, "Can not write edge " + edge.getName(), ex );
        }
    }

    protected void writeState(Element parent, State state)
    {
        parent.appendChild(StateXmlSerializer.getStateXmlElement(state, doc, this));
    }

    protected void writePath(Element parent, Edge edge)
    {
        Path path = edge.getPath();
        if( path == null || path.npoints <= 2 )
            return;
        Element pathElement = doc.createElement(PATH_ELEMENT);
        for( int i = 0; i < path.npoints; i++ )
            writeSegment(pathElement, path.xpoints[i], path.ypoints[i], path.pointTypes[i]);
        parent.appendChild(pathElement);
    }

    protected void writeSegment(Element parent, int x, int y, int type)
    {
        Element segmentElement = doc.createElement(SEGMENT_ELEMENT);
        String typeString = LINE_LINETO;
        if( type == 1 )
            typeString = LINE_QUADRIC;
        else if( type == 2 )
            typeString = LINE_CUBIC;
        segmentElement.setAttribute(SEGMENT_TYPE_ATTR, typeString);
        segmentElement.setAttribute(SEGMENT_X_ATTR, String.valueOf(x));
        segmentElement.setAttribute(SEGMENT_Y_ATTR, String.valueOf(y));
        parent.appendChild(segmentElement);
    }

    protected void writeEdgePaint(Element parent, Edge edge)
    {
        if( edge.getPredefinedStyle().equals(DiagramElementStyle.STYLE_DEFAULT) )
            return;

        Element edgePaint = doc.createElement(EDGE_PAINT_ELEMENT);
        if( edge.isStylePredefined() )
            edgePaint.setAttribute(SBGNPropertyConstants.STYLE_ATTR, edge.getPredefinedStyle());
        else
            edgePaint.setAttribute(SBGNPropertyConstants.LINE_PEN_ATTR, XmlSerializationUtils.getPenString(edge.getCustomStyle().getPen()));
        parent.appendChild(edgePaint);
    }

    protected void writeLayouters(Element element, Diagram diagram)
    {
        Element layouterInfo = DiagramXmlWriter.createLayouterInfo( doc, diagram.getPathLayouter(),
                DiagramXmlConstants.LAYOUTER_INFO_ELEMENT );
        if( layouterInfo != null )
            element.appendChild( layouterInfo );

        Element labelLayouterInfo = DiagramXmlWriter.createLayouterInfo( doc, diagram.getLabelLayouter(),
                DiagramXmlConstants.LABEL_LAYOUTER_INFO_ELEMENT );
        if( labelLayouterInfo != null )
            element.appendChild( labelLayouterInfo );
    }
}
