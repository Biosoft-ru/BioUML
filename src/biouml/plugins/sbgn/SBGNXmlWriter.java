package biouml.plugins.sbgn;

import java.awt.Point;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.graph.Path;
import ru.biosoft.graphics.Pen;
import ru.biosoft.util.ColorUtils;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramType;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.SubDiagram;
import biouml.model.util.DiagramXmlWriter;
import biouml.model.xml.XmlDiagramType;
import biouml.model.xml.XmlDiagramTypeConstants;
import biouml.standard.type.Stub;
import biouml.standard.type.Stub.ConnectionPort;
import biouml.standard.type.Type;

/**
 * Write SBGN diagrams to XML format
 */
@Deprecated
public class SBGNXmlWriter extends SBGNXmlConstants
{
    protected static final Logger log = Logger.getLogger(SBGNXmlWriter.class.getName());

    protected Document doc;
    protected Diagram diagram;
    protected Diagram baseDiagram;

    public SBGNXmlWriter(Diagram diagram, Diagram baseDiagram)
    {
        this.diagram = diagram;
        this.baseDiagram = baseDiagram;
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

    public Element write(Document doc) throws Exception
    {
        this.doc = doc;
        Element element = doc.createElement(SBGN_ELEMENT);

        DiagramType diagramType = diagram.getType();
        if( ! ( diagramType instanceof XmlDiagramType ) )
        {
            log.log(Level.SEVERE, "Incorrect diagram type, should be XmlDiagramType");
            return null;
        }
        XmlDiagramType xmlDiagramType = (XmlDiagramType)diagramType;
        element.setAttribute(NOTATION_REF, DataElementPath.create(xmlDiagramType).toString());
        element.setAttribute(BIOUML_XMLNS_ATTR, BIOUML_XMLNS_VALUE);
        List<DiagramElement> diagramElements = new ArrayList<>();
        fillDiagramElements(diagramElements, diagram);
        writeNodes(element, diagramElements);
        writeEdges(element, diagramElements);

       Element viewOptions = DiagramXmlWriter.createDiagramViewOptions(doc, diagram, "viewOptions");
        if( viewOptions != null )
            element.appendChild( viewOptions );
        
        return element;
    }

    protected Document createDocument() throws Exception
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.newDocument();
    }

    protected void writeNodes(Element parent, List<DiagramElement> diagramElements) throws Exception
    {
        Element nodes = doc.createElement(NODES_ELEMENT);
        for( DiagramElement de : diagramElements )
        {
            if( de instanceof Node )
            {
                writeNode(nodes, (Node)de);
            }
        }
        parent.appendChild(nodes);
    }

    protected void writeNode(Element parent, Node node)
    {
        Element nodeElement = doc.createElement(NODE_ELEMENT);
        nodeElement.setAttribute(ID_ATTR, node.getName());
        String type = (String)node.getAttributes().getValue(XmlDiagramTypeConstants.XML_TYPE);
        if( type == null )
        {
            type = node.getKernel().getType();
        }
        type = correctSBGNElementType(type, node.getAttributes());
        nodeElement.setAttribute(TYPE_ATTR, type);
        if( type.equals(Type.TYPE_REACTION) )
        {
            String reactionType = (String)node.getAttributes().getValue(SBGNPropertyConstants.SBGN_REACTION_TYPE);
            nodeElement.setAttribute(REACTION_TYPE_ATTR, reactionType);
        }

        Compartment origin = (Compartment)node.getOrigin();
        nodeElement.setAttribute(PARENT_ATTR, getElementPath(origin));
        if( node.getTitle() != null && !node.getTitle().equals(node.getName()) )
        {
            nodeElement.setAttribute(TITLE_ATTR, node.getTitle());
        }

        DiagramElement de = null;
        Role role = node.getRole();
        if( role != null )
        {
            de = role.getDiagramElement();
        }
        else
        {
            de = node;
        }
        if( de != null && de.getKernel() != null )
        {
            String refStr = null;
            if( ! ( de.getKernel() instanceof Stub ) )
            {
                refStr = de.getName();
            }
            if( ( de instanceof Compartment )
                    && ( ( de.getKernel().getType().equals(Type.TYPE_SUBSTANCE) ) || ( de.getKernel().getType()
                            .equals(Type.TYPE_COMPARTMENT) ) ) )
            {
                refStr = ((Compartment)de ).getCompleteNameInDiagram();
            }
            else if( de.getKernel() instanceof ConnectionPort )
            {
                if( de.getAttributes().getProperty(ConnectionPort.VARIABLE_NAME_ATTR) != null )
                {
                    refStr = de.getAttributes().getProperty(ConnectionPort.VARIABLE_NAME_ATTR).getValue().toString();
                }
                //initAttribute(node, nodeElement, ConnectionPort.PORT_ORIENTATION, ConnectionPort.PORT_ORIENTATION);
            }
            else if( de instanceof SubDiagram )
            {
                refStr = ( (Compartment)de ).getCompleteNameInDiagram();
                if (((SubDiagram)de).getState() != null)
                nodeElement.setAttribute("state", ((SubDiagram)de).getState().getName());
            }
            if( refStr != null )
            {
                nodeElement.setAttribute(REF_ATTR, refStr);
            }
        }

        initAttribute(node, nodeElement, SBGNPropertyConstants.SBGN_CLONE_MARKER, CLONE_ATTR);
        initAttribute(node, nodeElement, SBGNPropertyConstants.SBGN_MULTIMER, MULTIMER_ATTR);
        initAttribute(node, nodeElement, "value", VALUE_ATTR);

        writeNodeLayout(nodeElement, node);
        writeNodeTitle(nodeElement, node);
        writeNodePaint(nodeElement, node);

        //compartment type support
        initAttribute(node, nodeElement, SBGNPropertyConstants.CLOSEUP_ATTR, "closeup");
        initAttribute(node, nodeElement, SBGNPropertyConstants.TYPE_ATTR, "compartmentType");

        //ports support
        initAttribute(node, nodeElement, "orientation");
        initAttribute(node, nodeElement, "accessType");
        
        DynamicProperty property = node.getAttributes().getProperty("portBrush");
        if( property != null )
        {
            DiagramXmlWriter.serializeDynamicProperty(doc, nodeElement, property, null);
        }

        if( node.getKernel() instanceof Stub.ConnectionPort )
        {
            String portType = node.getKernel().getType();
            if( portType.equals(Type.TYPE_INPUT_CONNECTION_PORT) )
            {
                portType = "input";
            }
            else if( portType.equals(Type.TYPE_OUTPUT_CONNECTION_PORT) )
            {
                portType = "output";
            }
            else
            {
                portType = "contact";
            }
            nodeElement.setAttribute("portType", portType);
            
        }

        parent.appendChild(nodeElement);
    }

    protected void initAttribute(Node node, Element nodeElement, String propertyName)
    {
        initAttribute(node, nodeElement, propertyName, propertyName);
    }

    protected void initAttribute(Node node, Element nodeElement, String propertyName, String attrName)
    {
        Object property = node.getAttributes().getValue(propertyName);
        if( property != null )
        {
            nodeElement.setAttribute(attrName, property.toString());
        }
    }

    protected void writeNodeLayout(Element parent, Node node)
    {
        Element nodeLayout = doc.createElement(NODE_LAYOUT_ELEMENT);
        nodeLayout.setAttribute(X_ATTR, String.valueOf(node.getLocation().x));
        nodeLayout.setAttribute(Y_ATTR, String.valueOf(node.getLocation().y));
        if( node.getShapeSize() != null )
        {
            nodeLayout.setAttribute(WIDTH_ATTR, String.valueOf(node.getShapeSize().width));
            nodeLayout.setAttribute(HEIGHT_ATTR, String.valueOf(node.getShapeSize().height));
        }
        parent.appendChild(nodeLayout);
    }

    protected void writeNodePaint(Element parent, Node node)
    {
        if( ! ( node instanceof Compartment ) )
        {
            return;
        }

        Element nodePaint = doc.createElement(NODE_PAINT_ELEMENT);

        Object pen = node.getAttributes().getValue(SBGNPropertyConstants.LINE_PEN_ATTR);
        if( pen instanceof Pen )
        {
            String color = ColorUtils.paintToString( ( (Pen)pen ).getColor());
            nodePaint.setAttribute(COLOR_ATTR, color);
            nodePaint.setAttribute(WIDTH_ATTR, Integer.toString((int) ( (Pen)pen ).getWidth()));

            Object penIn = node.getAttributes().getValue(SBGNPropertyConstants.LINE_IN_PEN_ATTR);
            if( penIn != null )
            {
                nodePaint.setAttribute(COLOR_ATTR + "In", ColorUtils.paintToString( ( (Pen)penIn ).getColor()));
                nodePaint.setAttribute(WIDTH_ATTR + "In", Integer.toString((int) ( (Pen)penIn ).getWidth()));
            }
            Object penOut = node.getAttributes().getValue(SBGNPropertyConstants.LINE_OUT_PEN_ATTR);
            if( penOut != null )
            {
                nodePaint.setAttribute(COLOR_ATTR + "Out", ColorUtils.paintToString( ( (Pen)penOut ).getColor()));
                nodePaint.setAttribute(WIDTH_ATTR + "Out", Integer.toString((int) ( (Pen)penOut ).getWidth()));
            }
        }
        else
        {
//            String color = ColorUtils.paintToString( ( (Compartment)node ).getShapeColor().getPaint());
//            nodePaint.setAttribute(COLOR_ATTR, color);
        }
        parent.appendChild(nodePaint);
    }

    protected void writeNodeTitle(Element parent, Node node)
    {
        Object titlePoint = node.getAttributes().getValue(SBGNPropertyConstants.NAME_POINT_ATTR);
        if( titlePoint instanceof Point )
        {
            Element nodeTitle = doc.createElement(NODE_TITLE_ELEMENT);
            nodeTitle.setAttribute(X_ATTR, Integer.toString( ( (Point)titlePoint ).x));
            nodeTitle.setAttribute(Y_ATTR, Integer.toString( ( (Point)titlePoint ).y));
            parent.appendChild(nodeTitle);
        }
    }

    protected void writeEdges(Element parent, List<DiagramElement> diagramElements) throws Exception
    {
        Element edges = doc.createElement(EDGES_ELEMENT);
        for( DiagramElement de : diagramElements )
        {
            if( de instanceof Edge )
            {
                writeEdge(edges, (Edge)de);
            }
        }
        parent.appendChild(edges);
    }

    protected void writeEdge(Element parent, Edge edge) throws Exception
    {
        Element edgeElement = doc.createElement(EDGE_ELEMENT);
        edgeElement.setAttribute(ID_ATTR, edge.getName());
        String type = (String)edge.getAttributes().getValue(XmlDiagramTypeConstants.XML_TYPE);
        String kernelType = edge.getKernel().getType();
        if( type == null )
        {
            type = kernelType;
        }
        type = correctSBGNElementType(type, edge.getAttributes());
        kernelType = correctSBGNElementType(kernelType, edge.getAttributes());
        edgeElement.setAttribute(TYPE_ATTR, type);
        edgeElement.setAttribute(KERNEL_TYPE_ATTR, kernelType);
        edgeElement.setAttribute(FROM_ATTR, getElementPath(edge.getInput()));
        edgeElement.setAttribute(TO_ATTR, getElementPath(edge.getOutput()));

        if( type.equals(Type.TYPE_REACTION) && edge.getAttributes().getValue(SBGNPropertyConstants.SBGN_EDGE_TYPE) != null
                && !edge.getAttributes().getValueAsString(SBGNPropertyConstants.SBGN_EDGE_TYPE).isEmpty() )
        {
            String edgeType = (String)edge.getAttributes().getValue(SBGNPropertyConstants.SBGN_EDGE_TYPE);
            edgeElement.setAttribute(EDGE_TYPE_ATTR, edgeType);
        }

        DiagramElement de = null;
        Role role = edge.getRole();
        if( role != null )
        {
            de = role.getDiagramElement();
        }
        else
        {
            de = edge;
        }
        if( de != null && !kernelType.equals("portlink") )
        {
            edgeElement.setAttribute(REF_ATTR, de.getName());
        }
        Path path = edge.getPath();
        if( path != null && path.npoints > 2 )
        {
            writePath(edgeElement, path);
        }

        writeEdgePaint(edgeElement, edge);
        Object title = edge.getAttributes().getValue("text");
        if( ( title != null ) && ( title.toString().length() > 0 ) )
        {
            edgeElement.setAttribute(TEXT_ATTR, title.toString());
        }

        parent.appendChild(edgeElement);
    }

    protected void writePath(Element parent, Path path)
    {
        Element pathElement = doc.createElement(PATH_ELEMENT);
        for( int i = 0; i < path.npoints; i++ )
        {
            writeSegment(pathElement, path.xpoints[i], path.ypoints[i], path.pointTypes[i]);
        }
        parent.appendChild(pathElement);
    }

    protected void writeSegment(Element parent, int x, int y, int type)
    {
        Element segmentElement = doc.createElement(SEGMENT_ELEMENT);
        String typeString = LINE_LINETO;
        if( type == 1 )
        {
            typeString = LINE_QUADRIC;
        }
        else if( type == 2 )
        {
            typeString = LINE_CUBIC;
        }
        segmentElement.setAttribute(SEGMENT_TYPE_ATTR, typeString);
        segmentElement.setAttribute(SEGMENT_X_ATTR, String.valueOf(x));
        segmentElement.setAttribute(SEGMENT_Y_ATTR, String.valueOf(y));
        parent.appendChild(segmentElement);
    }

    protected void writeEdgePaint(Element parent, Edge edge)
    {
        Object pen = edge.getAttributes().getValue(SBGNPropertyConstants.LINE_PEN_ATTR);
        if( pen instanceof Pen )
        {
            Element nodePaint = doc.createElement(EDGE_PAINT_ELEMENT);
            String color = ColorUtils.paintToString( ( (Pen)pen ).getColor());
            nodePaint.setAttribute(COLOR_ATTR, color);
            nodePaint.setAttribute(WIDTH_ATTR, String.valueOf((int) ( (Pen)pen ).getWidth()));
            parent.appendChild(nodePaint);
        }
    }

    protected void fillDiagramElements(List<DiagramElement> elements, Compartment compartment)
    {
        Iterator<DiagramElement> iter = compartment.iterator();
        while( iter.hasNext() )
        {
            DiagramElement de = iter.next();
            elements.add(de);
            if( de instanceof Compartment )
            {
                fillDiagramElements(elements, (Compartment)de);
            }
        }
    }

    protected String getElementPath(DiagramElement de)
    {
        if( de instanceof Diagram )
        {
            return "";
        }
        Compartment origin = (Compartment)de.getOrigin();
        if( origin instanceof Diagram )
        {
            return de.getName();
        }
        else
        {
            return origin.getCompleteNameInDiagram() + "." + de.getName();
        }
    }

    protected String correctSBGNElementType(String oldType, DynamicPropertySet attributes)
    {
        if( oldType.equals("entity") )
        {
            return (String)attributes.getValue(SBGNPropertyConstants.SBGN_ENTITY_TYPE);
        }
        else if( oldType.equals("process") )
        {
            return (String)attributes.getValue(SBGNPropertyConstants.SBGN_PROCESS_TYPE);
        }
        else if( oldType.equals("logical operator") )
        {
            return (String)attributes.getValue(SBGNPropertyConstants.SBGN_LOGICAL_OPERATOR);
        }
        else if( oldType.equals("regulation") )
        {
            return (String)attributes.getValue(SBGNPropertyConstants.SBGN_EDGE_TYPE);
        }
        return oldType;
    }

    public String castStringToSId(String input)
    {
        String result = input.replaceAll("\\W", "_");
        if( result.matches("\\d\\w*") )
        {
            result = "_" + result.substring(1);
        }
        return result;
    }
}
