package biouml.plugins.sbgn;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Paint;
import java.awt.Point;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import ru.biosoft.access.core.AbstractDataCollection;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.graph.Path;
import ru.biosoft.graphics.Pen;
import ru.biosoft.util.XmlUtil;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.ModelDefinition;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.SubDiagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.VariableRole;
import biouml.model.util.DiagramXmlReader;
import biouml.model.xml.XmlDiagramSemanticController;
import biouml.model.xml.XmlDiagramType;
import biouml.model.xml.XmlDiagramTypeConstants;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.Util;
import biouml.standard.state.State;
import biouml.standard.type.Base;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Stub;
import biouml.standard.type.Stub.ConnectionPort;
import biouml.standard.type.Type;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.Option;

/**
 * Read SBGN diagrams from XML format
 */
@Deprecated
public class SBGNXmlReader extends SBGNXmlConstants
{
    protected static final Logger log = Logger.getLogger(SBGNXmlReader.class.getName());

    protected DataCollection origin;
    protected String name;
    protected Diagram baseDiagram;

    public SBGNXmlReader(DataCollection origin, String name, Diagram baseDiagram)
    {
        this.origin = origin;
        this.name = name;
        this.baseDiagram = baseDiagram;
    }

    /**
     * Read diagram from XML stream
     */
    public Diagram read(InputStream inputStream) throws Exception
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = null;

        try
        {
            doc = builder.parse(inputStream);
        }
        catch( SAXException e )
        {
            log.log(Level.SEVERE, "Parse SBGN error: " + e.getMessage());
            return null;
        }
        return read(doc.getDocumentElement());
    }

    /**
     * Read diagram from DOM element
     */
    public Diagram read(Element sbgnElement) throws Exception
    {
        nodeToClone.clear();

        if( !sbgnElement.getTagName().equals(SBGN_ELEMENT) || !sbgnElement.hasAttribute(NOTATION_REF) )
        {
            log.log(Level.SEVERE, "Incorrect root element");
            return null;
        }
        DataElementPath notationPath = DataElementPath.create(sbgnElement.getAttribute(NOTATION_REF));
        DataElement diagramTypeObj = notationPath.optDataElement();

        if( ! ( diagramTypeObj instanceof XmlDiagramType ) )
        {
            log.log(Level.SEVERE, "Incorrect graphic notation: " + notationPath);
            return null;
        }
        //remove from cache to get an unique copy of notation
        ( (AbstractDataCollection)diagramTypeObj.getOrigin() ).removeFromCache(diagramTypeObj.getName());

        XmlDiagramType xmlDiagramType = (XmlDiagramType)diagramTypeObj;
        ( (XmlDiagramSemanticController)xmlDiagramType.getSemanticController() )
                .setPrototype(baseDiagram.getType().getSemanticController());
        Diagram diagram = xmlDiagramType.createDiagram(origin, name, null);
        diagram.setTitle(baseDiagram.getTitle());
        ( (DiagramInfo)diagram.getKernel() ).setDescription( ( (DiagramInfo)baseDiagram.getKernel() ).getDescription());
        ( (DiagramInfo)diagram.getKernel() ).setDatabaseReferences( ( (DiagramInfo)baseDiagram.getKernel() ).getDatabaseReferences());
        ( (DiagramInfo)diagram.getKernel() ).setLiteratureReferences( ( (DiagramInfo)baseDiagram.getKernel() ).getLiteratureReferences());

        boolean notificationEnabled = diagram.isNotificationEnabled();
        diagram.setNotificationEnabled(false);

        readNodes(getElement(sbgnElement, NODES_ELEMENT), diagram);
        readEdges(getElement(sbgnElement, EDGES_ELEMENT), diagram);
        readStyles(getElement(sbgnElement, STYLES_ELEMENT), diagram);
 

        Element viewOptionsElement = getElement( sbgnElement, "viewOptions" );
        if( viewOptionsElement != null )
        {
            DiagramXmlReader.readViewOptions( viewOptionsElement, diagram );
        }
        
        adjustSubDiagrams( baseDiagram, diagram );
        diagram.setNotificationEnabled(notificationEnabled);

        
        Role diagramRole = baseDiagram.getRole();
        if( diagramRole instanceof EModel )
        {
            baseDiagram.removePropertyChangeListener((EModel)diagramRole);
            baseDiagram.removeDataCollectionListener((EModel)diagramRole);
            EModel newDiagramRole = (EModel)diagramRole.clone(diagram);
            diagram.setRole(newDiagramRole);
            eModelConvert(diagram);
            diagram.addPropertyChangeListener(newDiagramRole);
            diagram.addDataCollectionListener(newDiagramRole);
        }

        for( State state : baseDiagram.states() )
        {
            state.clone(diagram, state.getName());
        }
        diagram.setCurrentStateName(baseDiagram.getCurrentStateName());
        // set view builder for new diagram elements
        diagram.setNodeViewBuilders();
        return diagram;
    }

    protected void readNodes(Element element, Diagram diagram) throws Exception
    {
        for( Element child : XmlUtil.elements(element, NODE_ELEMENT) )
        {
            readNode(child, diagram);
        }
        DiagramUtility.compositeModelPostprocess(subDiagramSet, modelDefinitionSet);
        eModelConvert(diagram);
    }

    HashMap<String, String> nodeToClone = new HashMap<>();
    Set<SubDiagram> subDiagramSet = new HashSet<>();
    Set<ModelDefinition> modelDefinitionSet = new HashSet<>();
    
    protected void readNode(Element element, Diagram diagram) throws Exception
    {
        XmlDiagramType xmlDiagramType = (XmlDiagramType)diagram.getType();
        String id = element.getAttribute(ID_ATTR);
        String type = element.getAttribute(TYPE_ATTR);
        String parent = element.getAttribute(PARENT_ATTR);
        if( id.isEmpty() || type.isEmpty() )
        {
            log.log(Level.SEVERE, "Incorrect node attributes: id=\"" + id + "\" type=\"" + type + "\" parent=\"" + parent + "\"");
            return;
        }
        ComplexType complexType = getComplexType(type);
        Compartment parentCompartment = getParentCompartment(parent, diagram);
        if( parentCompartment == null )
        {
            return;
        }


        DiagramElement baseDiagramElement = null;

        String ref = element.getAttribute(REF_ATTR);
        if( !ref.isEmpty() && ( baseDiagram != null ) )
        {
            baseDiagramElement = baseDiagram.findDiagramElement(ref);
            if( baseDiagramElement == null )
            {
                //try oldstyle
                baseDiagramElement = baseDiagram.findDiagramElement(ref.replaceAll("_", "."));
            }
        }

        Base kernel = null;
        if( baseDiagramElement != null )
        {
            kernel = baseDiagramElement.getKernel();
        }
        else
        {
            String portType = element.getAttribute("portType");
            if( portType != null && portType.length() > 0 )
            {
                if( portType.equals("input") )
                {
                    portType = Type.TYPE_INPUT_CONNECTION_PORT;
                }
                else if( portType.equals("output") )
                {
                    portType = Type.TYPE_OUTPUT_CONNECTION_PORT;
                }
                else
                {
                    portType = Type.TYPE_CONTACT_CONNECTION_PORT;
                }
                kernel = Stub.ConnectionPort.createPortByType(null, id, portType);
            }
            else
            {
                kernel = new Stub(null, id, complexType.baseType);
                baseDiagramElement = baseDiagram.findDiagramElement(id);
            }
        }

        Node newNode;
        
        //dirty hack
        if (complexType.baseType.equals( "subDiagram" ) && baseDiagramElement instanceof SubDiagram)
        {
            Diagram innerDiagram = ( (SubDiagram)baseDiagramElement ).getDiagram();
            DynamicProperty p = innerDiagram.getAttributes().getProperty( SBGNPropertyConstants.SBGN_ATTRIBUTE_NAME );
            Diagram innerSBGNDiagram = null;
            if( p != null )
            {
                innerSBGNDiagram = (Diagram)p.getValue();//this diagram is the same as in model definition
                newNode = new SubDiagram( parentCompartment, innerSBGNDiagram!= null? innerSBGNDiagram: innerDiagram, id );
                innerSBGNDiagram = ((SubDiagram)newNode).getDiagram();
                
                String stateName = element.getAttribute( "state");
                //sbgn is read from the model earlier then states, therefore we need to copy states to the sbgn diagram
                //for non-composite diagrams it is made while doGet from transformedDataCollection, however subdiagrams are not in the TransformedDataCollection
                //probably this should be handled in one way for all diagrams
                innerSBGNDiagram.removeStates();
                for( State state : innerDiagram.states() )
                {
                    state.clone( innerSBGNDiagram, state.getName() );
                }
                innerSBGNDiagram.setCurrentStateName( stateName );
            }
            else
            newNode = new SubDiagram( parentCompartment, innerDiagram, id );
            subDiagramSet.add((SubDiagram)newNode);
        }
        else if( complexType.baseType.equals("modelDefinition") && baseDiagramElement instanceof ModelDefinition )
        {
            Diagram innerDiagram = ( (ModelDefinition)baseDiagramElement ).getDiagram();
            DynamicProperty p = innerDiagram.getAttributes().getProperty( SBGNPropertyConstants.SBGN_ATTRIBUTE_NAME );
            Diagram innerSBGNDiagram = null;
            if( p != null )
            {
                innerSBGNDiagram = (Diagram)p.getValue();
            }
            newNode = new ModelDefinition( parentCompartment, innerSBGNDiagram!= null? innerSBGNDiagram: innerDiagram, id );
            modelDefinitionSet.add((ModelDefinition)newNode);
        }
        else if( xmlDiagramType.checkCompartment(complexType.baseType) )
        {
            newNode = new Compartment(parentCompartment, id, kernel);
            newNode.setShapeSize(new Dimension(0, 0));
        }
        else
        {
            newNode = new Node(parentCompartment, id, kernel);
        }
        DynamicProperty dp = new DynamicProperty(XmlDiagramTypeConstants.XML_TYPE_PD, String.class, complexType.baseType);
        newNode.getAttributes().add(dp);

        DynamicPropertySet attributes = ( (XmlDiagramSemanticController)xmlDiagramType.getSemanticController() )
                .createAttributes(complexType.baseType);
        Iterator<String> iter = attributes.nameIterator();
        while( iter.hasNext() )
        {
            newNode.getAttributes().add(attributes.getProperty(iter.next()));
        }

        if( complexType.subTypePropertyName != null )
        {
            DynamicProperty property = newNode.getAttributes().getProperty(complexType.subTypePropertyName);
            if( property != null )
            {
                property.setValue(complexType.subTypePropertyValue);
            }
        }
        Element layout = getElement(element, NODE_LAYOUT_ELEMENT);
        Point location = getNodeLocation(layout);
        if( location != null )
        {
            newNode.setLocation(location);
            if (newNode instanceof SubDiagram)
            {
                ((SubDiagram)newNode).updatePorts(location);
            }
        }
        Dimension dimension = getNodeDimension(layout);
        if( dimension != null )
        {
            newNode.setShapeSize(dimension);
        }

        Element nodeTitle = getElement(element, NODE_TITLE_ELEMENT);
        if( nodeTitle != null )
        {
            int x = Integer.parseInt(nodeTitle.getAttribute(X_ATTR));
            int y = Integer.parseInt(nodeTitle.getAttribute(Y_ATTR));
            newNode.getAttributes().add(new DynamicProperty(SBGNPropertyConstants.NAME_POINT_ATTR, Point.class, new Point(x, y)));
        }

        Element paint = getElement(element, NODE_PAINT_ELEMENT);
        if( ( paint != null ) && ( newNode instanceof Compartment ) )
        {
            Paint color = DiagramXmlReader.stringToColor(paint.getAttribute(COLOR_ATTR), diagram.getName(), newNode.getName());
            String width = paint.getAttribute(WIDTH_ATTR);
            if( !width.isEmpty() && ( color instanceof Color ) )
            {
                newNode.getAttributes().add(
                        new DynamicProperty(SBGNPropertyConstants.LINE_PEN_ATTR, Pen.class, new Pen(Integer.parseInt(width), (Color)color)));

                Paint colorIn = DiagramXmlReader.stringToColor(paint.getAttribute(COLOR_ATTR + "In"), diagram.getName(), newNode.getName());
                String widthIn = paint.getAttribute(WIDTH_ATTR + "In");
                if( !widthIn.isEmpty() && ( colorIn instanceof Color ) )
                {
                    newNode.getAttributes().add(
                            new DynamicProperty(SBGNPropertyConstants.LINE_IN_PEN_ATTR, Pen.class, new Pen(Integer.parseInt(widthIn),
                                    (Color)colorIn)));
                }
                Paint colorOut = DiagramXmlReader.stringToColor(paint.getAttribute(COLOR_ATTR + "Out"), diagram.getName(), newNode
                        .getName());
                String widthOut = paint.getAttribute(WIDTH_ATTR + "Out");
                if( !widthOut.isEmpty() && ( colorOut instanceof Color ) )
                {
                    newNode.getAttributes().add(
                            new DynamicProperty(SBGNPropertyConstants.LINE_OUT_PEN_ATTR, Pen.class, new Pen(Integer.parseInt(widthOut),
                                    (Color)colorOut)));
                }
            }
        }

        //save clone value for further role changing
        String clone = element.getAttribute(CLONE_ATTR);
        if( ( clone != null ) && ( clone.length() > 0 ) && !clone.equals(newNode.getName()) )
        {
            nodeToClone.put(newNode.getName(), clone);
        }

        if( element.hasAttribute(TITLE_ATTR) )
        {
            newNode.setTitle(element.getAttribute(TITLE_ATTR));
        }

        DiagramXmlReader.fillProperties(element, newNode.getAttributes(), diagram.getType().getProperties());

        initProperty(newNode, element, SBGNPropertyConstants.SBGN_REACTION_TYPE, REACTION_TYPE_ATTR);
        initProperty(newNode, element, SBGNPropertyConstants.SBGN_CLONE_MARKER, CLONE_ATTR);
        initProperty(newNode, element, SBGNPropertyConstants.SBGN_MULTIMER, MULTIMER_ATTR);
        initProperty(newNode, element, "value", VALUE_ATTR, id);

        //compartment types support
        initProperty(newNode, element, SBGNPropertyConstants.CLOSEUP_ATTR, "closeup");
        initProperty(newNode, element, SBGNPropertyConstants.TYPE_ATTR, "compartmentType");

        //port support
        //TODO: handle this better
        if( Util.isPort( newNode ) )
        {
            initProperty( newNode, element, ConnectionPort.PORT_ORIENTATION );

            newNode.getAttributes().remove( ConnectionPort.PORT_TYPE );
            initProperty( newNode, element, ConnectionPort.ACCESS_TYPE, ConnectionPort.PUBLIC );
            DynamicProperty property = newNode.getAttributes().getProperty( ConnectionPort.ACCESS_TYPE );
            if( property != null )
                property.setReadOnly( true );
            newNode.getAttributes().remove( ConnectionPort.VARIABLE_NAME_ATTR );
            property = new DynamicProperty( ConnectionPort.VARIABLE_NAME_ATTR, String.class, ref );
            property.setReadOnly( true );
            newNode.getAttributes().add( property );
            initProperty( newNode, element, ConnectionPort.VARIABLE_NAME_ATTR, "ref" );
        }

        if( baseDiagramElement != null )
        {
            Role baseRole = baseDiagramElement.getRole();
            if( baseRole != null )
                newNode.setRole( baseRole.clone( newNode ) );

            newNode.setTitle( baseDiagramElement.getTitle() );
            copyAttributes( baseDiagramElement.getAttributes(), newNode.getAttributes() );
            if( baseDiagramElement instanceof Node )
                newNode.setVisible( ( (Node)baseDiagramElement ).isVisible());
        }
        parentCompartment.put(newNode);
    }

    protected void initProperty(Node node, Element element, String propertyName)
    {
        initProperty(node, element, propertyName, false);
    }

    protected void initProperty(Node node, Element element, String propertyName, boolean hidden)
    {
        initProperty(node, element, propertyName, propertyName, hidden);
    }

    protected void initProperty(Node node, Element element, String propertyName, String attributeName)
    {
        initProperty(node, element, propertyName, attributeName, false);
    }

    protected void initProperty(Node node, Element element, String propertyName, String attributeName, boolean hidden)
    {
        initProperty(node, element, propertyName, attributeName, null, hidden);
    }

    protected void initProperty(Node node, Element element, String propertyName, String attributeName, Object defaultValue)
    {
        initProperty(node, element, propertyName, attributeName, defaultValue, false);
    }

    protected void initProperty(Node node, Element element, String propertyName, String attributeName, Object defaultValue, boolean hidden)
    {
        DynamicProperty property = node.getAttributes().getProperty(propertyName);
        if( property != null )
        {
            String attr = element.getAttribute(attributeName);
            if( !attr.isEmpty() )
            {
                if( property.getType().equals(Integer.class) )
                {
                    try
                    {
                        property.setValue(Integer.parseInt(attr));
                    }
                    catch( NumberFormatException e )
                    {
                        log.log(Level.SEVERE, "Unexpected number format = " + attr);
                    }
                }
                else
                {
                    property.setValue(attr);
                }
            }
            else if( defaultValue != null )
            {
                property.setValue(defaultValue);
            }

            property.setHidden(hidden);
        }
    }

    protected void readEdges(Element element, Diagram diagram) throws Exception
    {
        for( Element child : XmlUtil.elements(element, EDGE_ELEMENT) )
        {
            readEdge(child, diagram);
        }
    }

    protected void readEdge(Element element, Diagram diagram) throws Exception
    {
        XmlDiagramType xmlDiagramType = (XmlDiagramType)diagram.getType();
        String id = element.getAttribute(ID_ATTR);
        String type = element.getAttribute(TYPE_ATTR);
        String kernelType = element.getAttribute(KERNEL_TYPE_ATTR);
        String from = element.getAttribute(FROM_ATTR);
        String to = element.getAttribute(TO_ATTR);
        if( id.isEmpty() || type.isEmpty() || from.isEmpty() || to.isEmpty() )
        {
            log.log(Level.SEVERE, "Incorrect node attributes: id=\"" + id + "\" type=\"" + type + "\" from=\"" + from + "\" to=\"" + to + "\"");
            return;
        }
        Node input = diagram.findNode(from);
        Node output = diagram.findNode(to);
        if( input == null || output == null )
        {
            log.log(Level.SEVERE, "Can not find input or output node: input=\"" + from + "\" output=\"" + to + "\"");
            return;
        }
        ComplexType complexType = getComplexType(type);
        ComplexType complexKernelType = getComplexType(kernelType);
        Edge newEdge = null;
        String ref = element.getAttribute(REF_ATTR);
        DiagramElement de = null;
        if( !ref.isEmpty() && ( baseDiagram != null ) )
        {
            de = baseDiagram.findDiagramElement(ref);
            if( de == null )
                de = baseDiagram.findDiagramElement(ref.replaceAll("_", ".")); //try oldStyle

            if( de == null && ref.contains(":"))
            {
                ref = ref.replaceFirst(": .+ to .+$", "");
                de = baseDiagram.findDiagramElement(ref);
            }
            if( de != null && de.getKernel() != null )
            {
                newEdge = new Edge(id, de.getKernel(), input, output);
            }
        }

        if( newEdge == null )
        {
            newEdge = new Edge(id, new Stub(null, id, complexKernelType.baseType), input, output);
        }

        Role role;
        if( de != null && ( role = de.getRole() ) != null )
        {
            newEdge.setRole( role.clone( newEdge ) );
            copyAttributes(de.getAttributes(), newEdge.getAttributes());
        }

        DynamicProperty dp = new DynamicProperty(XmlDiagramTypeConstants.XML_TYPE_PD, String.class, complexType.baseType);
        newEdge.getAttributes().add(dp);

        //we do not want to add all these reaction attributes (reaction type, orientation etc to the edge)
        if( !complexType.baseType.equals("reaction") )
        {
            DynamicPropertySet attributes = ( (XmlDiagramSemanticController)xmlDiagramType.getSemanticController() )
                    .createAttributes(complexType.baseType);
            Iterator<String> iter = attributes.nameIterator();
            while( iter.hasNext() )
            {
                newEdge.getAttributes().add(attributes.getProperty(iter.next()));
            }
        }
        
        String edgeType = element.getAttribute(EDGE_TYPE_ATTR);
        if( !edgeType.isEmpty() )
        {
            newEdge.getAttributes().add(new DynamicProperty(SBGNPropertyConstants.SBGN_EDGE_TYPE_PD, String.class, edgeType));
        }

        if( complexType.subTypePropertyName != null )
        {
            DynamicProperty property = newEdge.getAttributes().getProperty(complexType.subTypePropertyName);
            if( property != null )
            {
                property.setValue(complexType.subTypePropertyValue);
            }
        }
        Element path = getElement(element, PATH_ELEMENT);
        if( path != null )
        {
            readEdgePath(path, newEdge);
        }
        Element paint = getElement(element, EDGE_PAINT_ELEMENT);
        if( paint != null )
        {
            readEdgePaint(paint, newEdge, diagram);
        }

        //set special edge title
        String title = element.getAttribute(TEXT_ATTR);
        if( ( title != null ) && ( title.length() > 0 ) )
        {
            newEdge.getAttributes().add(new DynamicProperty("text", String.class, title));
        }

        newEdge.save();
    }

    protected void readEdgePath(Element element, Edge edge)
    {
        Path path = new Path();
        for( Element segment : XmlUtil.elements(element, SEGMENT_ELEMENT) )
        {
            String strType = segment.getAttribute(SEGMENT_TYPE_ATTR);
            int type = 0;
            if( strType.equals(LINE_LINETO) )
            {
                type = 0;
            }
            else if( strType.equals(LINE_QUADRIC) )
            {
                type = 1;
            }
            else if( strType.equals(LINE_CUBIC) )
            {
                type = 2;
            }
            try
            {
                int x = Integer.parseInt(segment.getAttribute(SEGMENT_X_ATTR));
                int y = Integer.parseInt(segment.getAttribute(SEGMENT_Y_ATTR));
                path.addPoint(x, y, type);
            }
            catch( NumberFormatException e )
            {
                log.log(Level.SEVERE, "X and Y value should be integer");
            }
        }
        edge.setPath(path);
    }

    protected void readEdgePaint(Element element, Edge edge, Diagram diagram) throws Exception
    {
        Color color = (Color)DiagramXmlReader.stringToColor(element.getAttribute(COLOR_ATTR), diagram.getName(), edge.getName());
        int width = Integer.parseInt(element.getAttribute(WIDTH_ATTR));
        Pen linePen = new Pen(width, color);
        edge.getAttributes().add(new DynamicProperty(SBGNPropertyConstants.LINE_PEN_ATTR, Pen.class, linePen));
    }

    protected void readStyles(Element element, Diagram diagram) throws Exception
    {
        //TODO: implement styles support
    }

    protected void copyAttributes(DynamicPropertySet from, DynamicPropertySet to)
    {
        Iterator<DynamicProperty> iter = from.iterator();
        while( iter.hasNext() )
        {
            to.add(iter.next());
        }
    }

    /**
     * Get parent compartment by complete name
     */
    protected Compartment getParentCompartment(String completeName, Diagram diagram) throws Exception
    {
        Compartment parentCompartment = diagram;
        if( completeName.trim().length() == 0 )
        {
            return parentCompartment;
        }
        String[] parentPath = completeName.split("\\.");
        for( String element : parentPath )
        {
            parentCompartment = (Compartment)parentCompartment.get(element);
            if( parentCompartment == null )
            {
                log.log(Level.SEVERE, "Can not find parent compartment " + completeName);
                return null;
            }
        }
        return parentCompartment;
    }

    /**
     * Read node location
     */
    protected Point getNodeLocation(Element element)
    {
        String x = element.getAttribute(X_ATTR);
        String y = element.getAttribute(Y_ATTR);
        if( !x.isEmpty() && !y.isEmpty() )
        {
            try
            {
                Point result = new Point();
                result.x = Integer.parseInt(x);
                result.y = Integer.parseInt(y);
                return result;
            }
            catch( NumberFormatException e )
            {
                log.log(Level.SEVERE, "Incorrect attribute value: x=" + x);
            }
        }

        return null;
    }

    /**
     * Read node dimension
     */
    protected Dimension getNodeDimension(Element element)
    {

        String width = element.getAttribute(WIDTH_ATTR);
        String height = element.getAttribute(HEIGHT_ATTR);
        if( !width.isEmpty() && !height.isEmpty() )
        {
            try
            {
                Dimension result = new Dimension();
                result.width = Integer.parseInt(width);
                result.height = Integer.parseInt(height);
                return result;
            }
            catch( NumberFormatException e )
            {
                log.log(Level.SEVERE, "Incorrect attribute value: width=" + width);
            }
        }
        return null;
    }

    /**
     * Get single child element
     */
    protected static Element getElement(Element element, String childName)
    {
        for( Element elem : XmlUtil.elements(element, childName) )
        {
            return elem;
        }
        return null;
    }

    protected ComplexType getComplexType(String type)
    {
        if( type.equals("unspecified") || type.equals("simple chemical") || type.equals("macromolecule")
                || type.equals("nucleic acid feature") || type.equals("perturbing agent") )
        {
            ComplexType complexType = new ComplexType("entity");
            complexType.subTypePropertyName = SBGNPropertyConstants.SBGN_ENTITY_TYPE;
            complexType.subTypePropertyValue = type;
            return complexType;
        }
        else if( type.equals("simple") || type.equals("omitted") || type.equals("uncertain") )
        {
            ComplexType complexType = new ComplexType("process");
            complexType.subTypePropertyName = SBGNPropertyConstants.SBGN_PROCESS_TYPE;
            complexType.subTypePropertyValue = type;
            return complexType;
        }
        else if( type.equals("AND") || type.equals("OR") || type.equals("NOT") )
        {
            ComplexType complexType = new ComplexType("logical operator");
            complexType.subTypePropertyName = SBGNPropertyConstants.SBGN_LOGICAL_OPERATOR;
            complexType.subTypePropertyValue = type;
            return complexType;
        }
        else if( type.equals("modulation") || type.equals("stimulation") || type.equals("catalysis") || type.equals("inhibition")
                || type.equals("necessary stimulation") )
        {
            ComplexType complexType = new ComplexType("regulation");
            complexType.subTypePropertyName = SBGNPropertyConstants.SBGN_EDGE_TYPE;
            complexType.subTypePropertyValue = type;
            return complexType;
        }
        return new ComplexType(type);
    }

    protected static class ComplexType
    {
        public String baseType;
        public String subTypePropertyName = null;
        public String subTypePropertyValue = null;

        public ComplexType(String baseType)
        {
            this.baseType = baseType;
        }
    }


    protected void eModelConvert(Diagram diagram)
    {
        for( Map.Entry<String, String> entry : nodeToClone.entrySet() )
        {
            String nodeName = entry.getKey();
            String sourceNodeName = entry.getValue();
            try
            {
                Node node = diagram.findNode(nodeName);
                Node sourceNode = diagram.findNode(sourceNodeName);
                String oldVariableName = node.getRole( VariableRole.class ).getName();
                String newVariableName = sourceNode.getRole( VariableRole.class ).getName();
                if( !newVariableName.equals(oldVariableName) )
                    diagram.getRole( EModel.class ).getVariableRoles().remove( oldVariableName );
                node.setRole(sourceNode.getRole());
                sourceNode.getRole( VariableRole.class ).addAssociatedElement( node );
            }
            catch( Exception ex )
            {

            }

        }
    }
    
    /**
     * If diagram referenced by subdiagram refers to modeldefinition
     * Then its converted version should refer to converted modelDefinition
     * @param diagram
     */
    protected void adjustSubDiagrams(Diagram diagramFrom, Diagram diagramTo)
    {
        for( SubDiagram subDiagram : diagramTo.stream( SubDiagram.class ) )
        {
            Diagram innerDiagram = subDiagram.getDiagram();
            try
            {
                Option parent = innerDiagram.getParent();
                if( parent != null && parent instanceof ModelDefinition
                        && Diagram.getDiagram( ( (ModelDefinition)parent ) ).equals( diagramFrom ) )
                {
                    Node newModelDefinition = diagramTo.findNode( ( (ModelDefinition)parent ).getName() );
                    if( newModelDefinition instanceof ModelDefinition )
                    {
                        innerDiagram.setParent( newModelDefinition );
                    }

                }
            }
            catch( Exception ex )
            {
                log.log(Level.SEVERE,  "Can not adjust parent for diagram referenced by subdiagram" + subDiagram.getName() );
            }
        }
    }
}
