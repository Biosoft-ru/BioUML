package biouml.plugins.sbgn;

import java.awt.Dimension;
import java.awt.Point;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementStyle;
import biouml.model.DiagramElementStyleDeclaration;
import biouml.model.DiagramType;
import biouml.model.Edge;
import biouml.model.ModelDefinition;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.SubDiagram;
import biouml.model.SubDiagram.PortOrientation;
import biouml.model.dynamics.Connection;
import biouml.model.dynamics.DirectedConnection;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.SimpleTableElement;
import biouml.model.dynamics.SimpleTableElement.VarColumn;
import biouml.model.dynamics.UndirectedConnection;
import biouml.model.dynamics.UndirectedConnection.MainVariableType;
import biouml.model.dynamics.plot.PlotsInfo;
import biouml.model.util.DiagramReader;
import biouml.model.util.DiagramXmlConstants;
import biouml.model.util.DiagramXmlReader;
import biouml.model.util.XmlSerializationUtils;
import biouml.plugins.sbml.SbmlDiagramType;
import biouml.plugins.sbml.SbmlSupport;
import biouml.plugins.sbml.composite.SbmlCompositeDiagramType;
import biouml.standard.diagram.Bus;
import biouml.standard.diagram.ConnectionEdgePane;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.Util;
import biouml.standard.state.State;
import biouml.standard.state.StateXmlSerializer;
import biouml.standard.type.Base;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Reaction;
import biouml.standard.type.Specie;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import biouml.standard.type.Stub.ConnectionPort;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.graph.Layouter;
import ru.biosoft.graph.Path;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.font.ColorFont;
import ru.biosoft.util.DPSUtils;
import ru.biosoft.util.XmlUtil;

/**
 * Read SBGN diagrams from XML format
 */
public class SbgnModelReader extends SBGNXmlConstants implements DiagramReader
{
    protected static final Logger log = Logger.getLogger(SbgnModelReader.class.getName());

    protected DataCollection<?> origin;
    protected String name;
    protected Diagram baseDiagram;

    //several sbgn nodes may be correspondent to one sbml
    private Map<Node, List<Node>> sbmlToSbgn = new HashMap<>();

    public SbgnModelReader(DataCollection<?> origin, String name, Diagram baseDiagram)
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
        try
        {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream);
            return read(doc.getDocumentElement());
        }
        catch( SAXException e )
        {
            log.log(Level.SEVERE, "Parse SBGN error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Read diagram from DOM element
     */
    public Diagram read(Element element) throws Exception
    {
        sbmlToSbgn.clear();

        if( !isValidRoot(element) )
        {
            log.log(Level.SEVERE, "Incorrect root element");
            return null;
        }
        Diagram diagram = getDiagramType().createDiagram(origin, name, ( (DiagramInfo)baseDiagram.getKernel() ).clone(name));
        diagram.setNotificationEnabled(false);
        readLayouters(element, diagram);
        readViewOptions(element, diagram);
        SbgnDiagramViewOptions viewOptions = (SbgnDiagramViewOptions)diagram.getViewOptions();
        DiagramElementStyleDeclaration[] oldStyles = baseDiagram.getViewOptions().getStyles();
        if( oldStyles != null && oldStyles.length != 0 )
            StreamEx.of(oldStyles).peek(s -> s.getStyle().setFont(viewOptions.getCustomTitleFont())).forEach(viewOptions::addStyleIfAbsent);
        readNodes(XmlUtil.findElementByTagName(element, NODES_ELEMENT), diagram);
        readEdges(XmlUtil.findElementByTagName(element, EDGES_ELEMENT), diagram);
        DiagramXmlReader.readFilters(XmlUtil.findElementByTagName(element, DiagramXmlReader.FILTERS_ELEMENT), diagram);
        DiagramUtility.compositeModelPostprocess(diagram);
        diagram.setNotificationEnabled(true);
        copyRole(diagram, baseDiagram);
        copyDiagramProperties(diagram, baseDiagram); //do after emodel set because some properties may need it
        readStates(XmlUtil.findElementByTagName(element, STATES_ELEMENT), diagram);
        diagram.setCurrentStateName(baseDiagram.getCurrentStateName());
        diagram.setNodeViewBuilders(); // set view builder for new diagram elements
        return diagram;
    }

    private void readViewOptions(Element element, Diagram diagram)
    {
        Element viewOptionsElement = XmlUtil.findElementByTagName(element, "viewOptions");
        if( viewOptionsElement != null )
        {
            Element optionsElement = XmlUtil.getChildElement(viewOptionsElement, "name", "options");
            if( optionsElement != null ) //compatibility with old diagrams
                DiagramXmlReader.readViewOptions(optionsElement, diagram);
            else
                DiagramXmlReader.readViewOptions(viewOptionsElement, diagram);
        }
    }

    private void copyRole(Diagram diagram, Diagram baseDiagram)
    {
        Role diagramRole = baseDiagram.getRole();
        if( diagramRole instanceof EModel )
            diagram.setRole(diagramRole.clone(diagram));
    }

    private void copyDiagramProperties(Diagram newDiagram, Diagram baseDiagram)
    {
        newDiagram.setTitle(baseDiagram.getTitle());
        if( baseDiagram.getAttributes().getProperty("Packages") != null )
            newDiagram.getAttributes().add(baseDiagram.getAttributes().getProperty("Packages"));
        if( baseDiagram.getAttributes().getProperty("fbc:activeObjective") != null )
            newDiagram.getAttributes().add(baseDiagram.getAttributes().getProperty("fbc:activeObjective"));
        if( baseDiagram.getAttributes().getProperty("listObjectives") != null )
            newDiagram.getAttributes().add(baseDiagram.getAttributes().getProperty("listObjectives"));

        if( baseDiagram.getAttributes().getProperty(DiagramXmlConstants.PLOTS_ATTR) != null )
        {
            PlotsInfo info = (PlotsInfo)baseDiagram.getAttributes().getValue(DiagramXmlConstants.PLOTS_ATTR);
            info.setEModel(newDiagram.getRole(EModel.class));
            newDiagram.getAttributes().add(DPSUtils.createHiddenReadOnlyTransient("Plots", PlotsInfo.class, info));
        }

        if( baseDiagram.getAttributes().getProperty(DiagramXmlConstants.SIMULATION_OPTIONS) != null )
            newDiagram.getAttributes().add(baseDiagram.getAttributes().getProperty(DiagramXmlConstants.SIMULATION_OPTIONS));
    }

    protected DiagramType getDiagramType()
    {
        return baseDiagram.getType() instanceof SbmlCompositeDiagramType ? new SbgnCompositeDiagramType() : new SbgnDiagramType();
    }
    protected boolean isValidRoot(Element root)
    {
        return root.getTagName().equals(SBGN_ELEMENT);
    }

    @Override
    public void readNodes(Element element, Compartment compartment) throws Exception
    {
        for( Element child : XmlUtil.elements(element, NODE_ELEMENT) )
        {
            boolean notificationEnabled = compartment.isNotificationEnabled();
            compartment.setNotificationEnabled(false);
            Node node = readNode(child, Diagram.getDiagram(compartment));
            compartment.setNotificationEnabled(notificationEnabled);
            if( node != null )
                node.save();
        }
    }

    Map<String, Bus> buses = new HashMap<>();

    protected Node readNode(Element element, Diagram diagram) throws Exception
    {
        try
        {
            String id = transformID(element.getAttribute(ID_ATTR));
            String type = element.getAttribute(TYPE_ATTR);
            String parent = SbmlSupport.castFullName(element.getAttribute(PARENT_ATTR));
            String title = ( element.hasAttribute(TITLE_ATTR) ) ? element.getAttribute(TITLE_ATTR) : id;

            if( id.isEmpty() || type.isEmpty() )
            {
                log.log(Level.SEVERE, "Incorrect node attributes: id=\"" + id + "\" type=\"" + type + "\" parent=\"" + parent + "\"");
                return null;
            }
            Compartment parentCompartment = getParentCompartment(parent, diagram);
            if( parentCompartment == null )
                return null;

            String ref = SbmlSupport.castFullName(element.getAttribute(REF_ATTR));

            //for backward compatibility
            if( mathTypes.containsKey(type) )
                type = mathTypes.get(type);
            else if( type.equals(Type.TYPE_PORT) )
                type = ConnectionPort.shortNameToFull.get(element.getAttribute(PORT_TYPE_ATTR));
            else if( type.equals(Type.TYPE_VARIABLE) )
            {
                String value = element.getAttribute(VALUE_ATTR);
                if( !value.isEmpty() )
                {
                    id = DefaultSemanticController.generateUniqueNodeName(diagram, "VARIABLE_" + value);
                    title = value;
                }
            }

            //for backward compatibility
            if( SBGNPropertyConstants.mathTypesFull.contains(type) )
                ref = id;

            Node baseNode = null;

            if( !ConnectionPort.portFullTypes.contains(type) )
            {
                baseNode = baseDiagram.findNode(ref);
                if( baseNode == null ) //try oldstyle
                    baseNode = baseDiagram.findNode(ref.replaceAll("_", "."));
            }

            Node newNode;

            if( baseNode != null )
            {
                newNode = createNode(element, parentCompartment, id, baseNode);
                sbmlToSbgn.computeIfAbsent(baseNode, k -> new ArrayList<>()).add(newNode);
            }
            else
            {
                newNode = (Node)SbgnSemanticController.createDiagramElement(type, id, parentCompartment);
            }

            if( SbgnUtil.isLogical(newNode) )
            {
                String operatorType = element.getAttribute(OPERATOR_TYPE_ATTR);
                if( operatorType.isEmpty() )
                    operatorType = "And"; //recover if damaged diagram
                newNode.getAttributes()
                        .add(new DynamicProperty(SBGNPropertyConstants.SBGN_LOGICAL_OPERATOR_PD, String.class, operatorType));
            }
            if( Util.isBus(newNode) )
            {
                String busName = element.getAttribute("busName");
                if( buses.containsKey(busName) )
                {
                    newNode.setRole(buses.get(busName));
                }
                else
                {
                    Bus bus;
                    if( element.hasAttribute("directed") )
                        bus = new Bus(busName, Boolean.parseBoolean(element.getAttribute("directed")));
                    else
                        bus = new Bus(busName, false);

                    bus.setColor(XmlSerializationUtils.readBrush(element.getAttribute("brush")).getColor());
                    newNode.setRole(bus);
                    bus.addNode(newNode);
                    buses.put(busName, bus);
                }
            }
            if( newNode.getKernel().getType().equals(Type.TYPE_TABLE) )
            {
                SimpleTableElement table = newNode.getRole(SimpleTableElement.class);
                table.setTablePath(DataElementPath.create(element.getAttribute("tablePath")));
                Element argElement = XmlUtil.getChildElement(element, ARGCOLUMN_ELEMENT);
                table.getArgColumn().setColumn(argElement.getAttribute("column"));
                table.getArgColumn().setVariable(argElement.getAttribute("variable"));
                List<VarColumn> columns = new ArrayList<>();
                for( Element child : XmlUtil.elements(element, VARCOLUMN_ELEMENT) )
                {
                    VarColumn column = new VarColumn();
                    column.setColumn(child.getAttribute("column"));
                    column.setVariable(child.getAttribute("variable"));
                    columns.add(column);
                }
                table.setColumns(StreamEx.of(columns).toArray(VarColumn[]::new));
            }

            if( ConnectionPort.portFullTypes.contains(type) )
                readPortAttributes(element, newNode);

            if( element.hasAttribute(Util.COMPLEX_STRUCTURE) )
            {
                newNode.getAttributes()
                        .add(DPSUtils.createTransient(Util.COMPLEX_STRUCTURE, String.class, element.getAttribute(Util.COMPLEX_STRUCTURE)));
            }
            readMultimer(element, newNode);

            if( element.hasAttribute(REACTION_TYPE_ATTR) )
                newNode.getAttributes().add(new DynamicProperty(SBGNPropertyConstants.SBGN_REACTION_TYPE_PD, String.class,
                        element.getAttribute(REACTION_TYPE_ATTR)));
            newNode.setTitle(title);
            if( element.hasAttribute(SHOW_TITLE_ATTR) )
                newNode.setShowTitle(Boolean.parseBoolean(element.getAttribute(SHOW_TITLE_ATTR)));
            else if( newNode.getKernel().getType().equals(Type.TYPE_COMPLEX) )
                newNode.setShowTitle(false); //for old diagrams without this property
            if( element.hasAttribute(FIXED_ATTR) )
                newNode.setFixed(true);
            readAdditionalInformation(element, newNode);
            SbgnSemanticController.setNeccessaryAttributes(newNode);
            return newNode;
        }
        catch( Exception ex )
        {
            log.log(java.util.logging.Level.SEVERE, "Error during node reading: " + ex.getMessage(), ex);
            return null;
        }
    }

    private void readPortAttributes(Element element, Node node)
    {
        if( element.hasAttribute(ConnectionPort.PORT_ORIENTATION) )
            node.getAttributes().add(new DynamicProperty(ConnectionPort.PORT_ORIENTATION, PortOrientation.class,
                    PortOrientation.getOrientation(element.getAttribute(ConnectionPort.PORT_ORIENTATION))));

        if( element.hasAttribute(ConnectionPort.ACCESS_TYPE) )
            node.getAttributes()
                    .add(new DynamicProperty(ConnectionPort.ACCESS_TYPE, String.class, element.getAttribute(ConnectionPort.ACCESS_TYPE)));
        if( element.hasAttribute(REF_ATTR) )
        {
            node.getAttributes()
                    .add(DPSUtils.createReadOnly(ConnectionPort.VARIABLE_NAME_ATTR, String.class, element.getAttribute(REF_ATTR)));
        }
        else
        {
            log.log(Level.SEVERE, "Missing reference attribute for port " + node.getName() + ". It won't be created.");
        }

        if( node.getParent() instanceof SubDiagram && element.hasAttribute(SubDiagram.ORIGINAL_PORT_ATTR) )
            node.getAttributes().add(DPSUtils.createHiddenReadOnly(SubDiagram.ORIGINAL_PORT_ATTR, String.class,
                    element.getAttribute(SubDiagram.ORIGINAL_PORT_ATTR)));
    }

    private Node createNode(Element element, Compartment parentCompartment, String id, Node baseNode) throws Exception
    {
        Base kernel = baseNode.getKernel();
        Node newNode = null;
        if( baseNode instanceof SubDiagram )
            newNode = readSubDiagram(element, parentCompartment, id, (SubDiagram)baseNode);
        else if( baseNode instanceof ModelDefinition )
            newNode = readModelDefinition(element, parentCompartment, id, (ModelDefinition)baseNode);
        else if( kernel instanceof Specie )
            newNode = createEntity(element, parentCompartment, id, baseNode);
        else if( kernel instanceof biouml.standard.type.Compartment )
            newNode = createCompartment(element, parentCompartment, id, (Compartment)baseNode);
        else if( kernel instanceof Reaction )
            newNode = createReaction(element, parentCompartment, id, baseNode);
        else
            newNode = new Node(parentCompartment, id, kernel);
        newNode.setUseCustomImage(baseNode.isUseCustomImage());
        if( baseNode.isUseCustomImage() )
            newNode.setImage(baseNode.getImage().clone());

        //in some cases role is assigned earlier (e.g. when we create clone of existing node)
        if( newNode.getRole() == null && baseNode.getRole() != null )
            newNode.setRole(baseNode.getRole().clone(newNode));

        newNode.setComment(baseNode.getComment());
        newNode.setTitle(baseNode.getTitle());
        newNode.setVisible(baseNode.isVisible());
        copyAttributes(baseNode.getAttributes(), newNode.getAttributes());
        return newNode;
    }

    private Node createReaction(Element element, Compartment parentCompartment, String id, Node sbmlReaction)
    {
        Reaction kernel = (Reaction)sbmlReaction.getKernel();
        Reaction newKernel = kernel.clone(null, kernel.getName());
        Node newNode = new Node(parentCompartment, id, newKernel);
        newNode.setShowTitle(false); //by default
        newKernel.setParent(newNode);
        return newNode;
    }

    private Compartment createCompartment(Element element, Compartment parentCompartment, String id, Compartment sbmlCompartment)
    {
        List<Node> elements = sbmlToSbgn.get(sbmlCompartment);
        boolean isClone = elements != null;
        if( isClone ) //this is a clone and we have already read another one
        {
            Node mainNode = elements.get(0);
            return (Compartment)Diagram.getDiagram(mainNode).getType().getSemanticController().cloneNode(mainNode, id, new Point());
        }

        Compartment newNode = new Compartment(parentCompartment, id, sbmlCompartment.getKernel());
        newNode.setShapeType(sbmlCompartment.getShapeType());
        return newNode;
    }

    private Compartment createEntity(Element element, Compartment parentCompartment, String id, Node sbmlNode)
    {
        Compartment newNode = null;
        List<Node> elements = sbmlToSbgn.get(sbmlNode);
        boolean isClone = elements != null;
        if( isClone ) //this is a clone and we have already read another one
        {
            Node mainNode = elements.get(0);
            newNode = (Compartment)Diagram.getDiagram(mainNode).getType().getSemanticController().cloneNode(mainNode, id, new Point());
        }
        else
        {
            Specie kernel = (Specie)sbmlNode.getKernel();
            SbgnUtil.setSBGNTypes(kernel);
            kernel.setType(element.getAttribute(TYPE_ATTR));
            newNode = new Compartment(parentCompartment, id, kernel);
            kernel.setParent(newNode);

            if( element.hasAttribute(CLONE_ATTR) )//means this is a clone but we have not read other ones, we designate this node as a main node
                SbgnSemanticController.setCloneMarker(newNode, element.getAttribute(CLONE_ATTR));
        }
        return newNode;
    }

    private void readMultimer(Element element, Node node)
    {
        if( element.hasAttribute(MULTIMER_ATTR) )
        {
            String value = element.getAttribute(MULTIMER_ATTR);
            try
            {
                node.getAttributes().add(new DynamicProperty(SBGNPropertyConstants.SBGN_MULTIMER, Integer.class, Integer.parseInt(value)));
            }
            catch( NumberFormatException ex )
            {
                log.log(Level.SEVERE, "Wrong multimer attribute " + value + " for node " + node.getName() + ". Should be a number.");
            }
        }
    }

    protected static void readAdditionalInformation(Element element, Node node) throws Exception
    {
        Element layout = XmlUtil.findElementByTagName(element, NODE_LAYOUT_ELEMENT);
        Point location = getNodeLocation(layout);
        if( location != null )
        {
            node.setLocation(location);
            if( node instanceof SubDiagram )
                ( (SubDiagram)node ).updatePorts(location);

            String orientation = layout.getAttribute(PortOrientation.ORIENTATION_ATTR);
            if( orientation != null && !orientation.isEmpty() )
            {
                node.getAttributes().add(new DynamicProperty(PortOrientation.ORIENTATION_ATTR, PortOrientation.class,
                        PortOrientation.getOrientation(orientation)));
            }
        }
        Dimension dimension = getNodeDimension(layout);
        if( dimension != null )
            node.setShapeSize(dimension);

        Element paint = XmlUtil.findElementByTagName(element, NODE_PAINT_ELEMENT);

        boolean customStyle = false;
        DiagramElementStyle style = new DiagramElementStyle(node);
        if( paint != null )
        {
            if( paint.hasAttribute(SBGNPropertyConstants.STYLE_ATTR) )
                node.setPredefinedStyle(paint.getAttribute(SBGNPropertyConstants.STYLE_ATTR));
            else
            {
                String penAttr = paint.getAttribute(SBGNPropertyConstants.LINE_PEN_ATTR);
                if( !penAttr.isEmpty() )
                {
                    Pen pen = XmlSerializationUtils.readPen(paint.getAttribute(SBGNPropertyConstants.LINE_PEN_ATTR));
                    customStyle = true;
                    style.setPen(pen);
                }

                String brushAttr = paint.getAttribute(SBGNPropertyConstants.BRUSH_ATTR);
                if( !brushAttr.isEmpty() )
                {
                    Brush brush = XmlSerializationUtils.readBrush(brushAttr);
                    if( brush != null )
                    {
                        customStyle = true;
                        style.setBrush(brush);
                    }
                }
            }

            if( node.getKernel() instanceof Stub.Note )
            {
                String visible = paint.getAttribute(SBGNPropertyConstants.BACKGROUND_VISIBLE_ATTR);
                if( !visible.isEmpty() )
                    ( (Stub.Note)node.getKernel() ).setBackgroundVisible(Boolean.valueOf(visible));
            }
        }

        Element nodeTitle = XmlUtil.findElementByTagName(element, NODE_TITLE_ELEMENT);
        if( nodeTitle != null )
        {
            if( nodeTitle.hasAttribute(X_ATTR) && nodeTitle.hasAttribute(Y_ATTR) )
            {
                int x = Integer.parseInt(nodeTitle.getAttribute(X_ATTR));
                int y = Integer.parseInt(nodeTitle.getAttribute(Y_ATTR));
                node.getAttributes().add(new DynamicProperty(SBGNPropertyConstants.NAME_POINT_ATTR, Point.class, new Point(x, y)));
            }

            ColorFont font = XmlSerializationUtils.readFont(nodeTitle.getAttribute(SBGNPropertyConstants.TITLE_FONT_ATTR));
            if( font != null )
            {
                customStyle = true;
                style.setFont(font);
            }
        }

        if( customStyle )
        {
            node.setPredefinedStyle(DiagramElementStyle.STYLE_NOT_SELECTED);
            node.setCustomStyle(style);
        }

        String attr = element.getAttribute(SBGNXmlConstants.VISIBLE_ATTR);
        if( !attr.isEmpty() && !Boolean.parseBoolean(attr) )
            node.setVisible(false);
    }

    @SuppressWarnings ( "serial" )
    private static Map<String, String> mathTypes = new HashMap<String, String>()
    {
        {
            put("event", Type.TYPE_EVENT);
            put("equation", Type.TYPE_EQUATION);
            put("function", Type.TYPE_FUNCTION);
            put("constraint", Type.TYPE_CONSTRAINT);
        }
    };

    @Override
    public void readEdges(Element element, Compartment compartment) throws Exception
    {
        for( Element child : XmlUtil.elements(element, EDGE_ELEMENT) )
        {
            boolean notificationEnabled = compartment.isNotificationEnabled();
            compartment.setNotificationEnabled(false);
            Edge edge = readEdge(child, Diagram.getDiagram(compartment));
            compartment.setNotificationEnabled(notificationEnabled);
            if( edge != null )
                edge.save();
        }
    }

    public static String transformID(String input)
    {
        String result = input.replaceAll("\\W", "_");
        if( result.matches("\\d\\w*") )
        {
            result = "_" + result;
        }
        return result;
    }

    protected Edge readEdge(Element element, Diagram diagram) throws Exception
    {
        String id = transformID(element.getAttribute(ID_ATTR));
        String type = element.getAttribute(TYPE_ATTR);
        String kernelType = element.getAttribute(KERNEL_TYPE_ATTR); //for backward compatibility
        if( !kernelType.isEmpty() )
            type = kernelType;

        String from = SbmlSupport.castFullName(element.getAttribute(FROM_ATTR));
        String to = SbmlSupport.castFullName(element.getAttribute(TO_ATTR));
        if( id.isEmpty() || type.isEmpty() || from.isEmpty() || to.isEmpty() )
        {
            log.log(Level.SEVERE,
                    "Incorrect node attributes: id=\"" + id + "\" type=\"" + type + "\" from=\"" + from + "\" to=\"" + to + "\"");
            return null;
        }
        Node input = diagram.findNode(from);
        Node output = diagram.findNode(to);
        if( input == null || output == null )
        {
            log.log(Level.SEVERE, "Can not find input or output node: input=\"" + from + "\" output=\"" + to + "\"");
            return null;
        }

        Edge newEdge = null;
        String ref = transformID(element.getAttribute(REF_ATTR));
        if( !ref.isEmpty() && ( baseDiagram != null ) )
        {
            DiagramElement de = baseDiagram.findDiagramElement(ref);
            if( de == null )
                de = baseDiagram.findDiagramElement(ref.replaceAll("_", ".")); //try oldStyle

            if( de == null && ref.contains(":") )
                de = baseDiagram.findDiagramElement(ref.replaceFirst(": .+ to .+$", ""));

            if( de != null && de.getKernel() != null )
            {
                Edge oldEdge = (Edge)de;

                Role oldRole = de.getRole();
                if( oldEdge.getKernel() instanceof Stub.DirectedConnection || oldEdge.getKernel() instanceof Stub.UndirectedConnection )
                {
                    newEdge = new Edge(id, oldEdge.getKernel(), input, output); //createConnectionEdge(oldEdge, input, output, id);                    
                    if( oldRole != null )
                    {
                        Connection con = (Connection)oldRole.clone(newEdge);
                        con.setInputPort(new Connection.Port(ConnectionEdgePane.getPortVariableName(input), input.getTitle()));
                        con.setOutputPort(new Connection.Port(ConnectionEdgePane.getPortVariableName(output), output.getTitle()));
                        newEdge.setRole(con);
                    }

                }
                else
                {
                    Base newKernel = de.getKernel();
                    if( newKernel instanceof SpecieReference )
                    {
                        Node baseReaction = SbgnUtil.findReaction( (Edge)de );
                        if( baseReaction != null )
                        {
                            Node newReaction = sbmlToSbgn.get( baseReaction ).iterator().next();
                            Reaction reaction = (Reaction)newReaction.getKernel();
                            newKernel = ( (SpecieReference)newKernel ).clone( reaction, de.getKernel().getName() );
                            reaction.put( (SpecieReference)newKernel );
                            newEdge = new Edge( id, newKernel, input, output );
                        }
                    }
                    else
                        newEdge = new Edge( id, de.getKernel(), input, output );
                    newEdge.setComment(de.getComment());
                    if( oldRole != null )
                        newEdge.setRole(oldRole.clone(newEdge));
                    copyAttributes(de.getAttributes(), newEdge.getAttributes());
                    if( newEdge.getKernel() instanceof SpecieReference )
                    {
                        Node baseReaction = SbgnUtil.findReaction((Edge)de);
                        if( baseReaction != null )
                        {
                            Node newReaction = sbmlToSbgn.get(baseReaction).iterator().next();
                            Reaction reaction = (Reaction)newReaction.getKernel();
                            reaction.put((SpecieReference)newEdge.getKernel());
                        }
                    }
                }
            }
        }

        if( newEdge == null )
        {
            if( type.equals(biouml.standard.type.Type.TYPE_DIRECTED_LINK) )
            {
                newEdge = new Edge(id, new Stub.DirectedConnection(null, id), input, output);
                DirectedConnection role = new DirectedConnection(newEdge);
                Util.setConnectionPort(role, newEdge.getInput(), true);
                Util.setConnectionPort(role, newEdge.getOutput(), false);
                newEdge.setRole(role);
            }
            else if( type.equals(biouml.standard.type.Type.TYPE_UNDIRECTED_LINK) )
            {
                newEdge = new Edge(id, new Stub.UndirectedConnection(null, id), input, output);
                UndirectedConnection role = new UndirectedConnection(newEdge);
                Util.setConnectionPort(role, newEdge.getInput(), true);
                Util.setConnectionPort(role, newEdge.getOutput(), false);
                newEdge.setRole(role);
            }
            else if( type.equals(biouml.standard.type.Type.TYPE_CHEMICAL_ROLE) )
            {
                Reaction reaction = input.getKernel() instanceof Reaction ? (Reaction)input.getKernel()
                        : output.getKernel() instanceof Reaction ? (Reaction)output.getKernel() : null;
                String srRole = id.contains("as reactant") ? SpecieReference.REACTANT
                        : id.contains("as product") ? SpecieReference.PRODUCT : SpecieReference.MODIFIER;
                String species = input.getKernel() instanceof Specie ? from : output.getKernel() instanceof Specie ? to : null;
                SpecieReference kernel = new SpecieReference(reaction, ref, srRole);
                kernel.setSpecie(species);
                newEdge = new Edge(id, kernel, input, output);
                if( reaction != null )
                {
                    reaction.put( kernel );

                }

                DiagramUtility.generateReactionRole(diagram, newEdge);

                String edgeType = element.getAttribute(EDGE_TYPE_ATTR);
                if( !edgeType.isEmpty() )
                    kernel.setModifierAction(edgeType);
                //                    newEdge.getAttributes().add(new DynamicProperty(SBGNPropertyConstants.SBGN_EDGE_TYPE_PD, String.class, edgeType));
            }
            else if( type.equals(biouml.standard.type.Type.TYPE_NOTE_LINK) )
            {
                newEdge = new Edge(id, new Stub.NoteLink(null, id), input, output);
            }
            else
                newEdge = new Edge(id, new Stub(null, id, type), input, output);
        }

        if( newEdge.getKernel() instanceof SpecieReference
                && SpecieReference.MODIFIER.equals( ( (SpecieReference)newEdge.getKernel() ).getRole()) )
        {
            String edgeType = element.getAttribute(EDGE_TYPE_ATTR);
            if( edgeType.isEmpty() )
                edgeType = Type.TYPE_CATALYSIS;
            if( edgeType.equals(Type.TYPE_LOGIC_ARC) )
                newEdge.getAttributes().add(DPSUtils.createReadOnly(SBGNPropertyConstants.SBGN_EDGE_TYPE, String.class, edgeType));
            else
            {
                ( (SpecieReference)newEdge.getKernel() ).setModifierAction(edgeType);
                newEdge.getAttributes().remove(SBGNPropertyConstants.SBGN_EDGE_TYPE);
            }
        }
        else if( Type.TYPE_REGULATION.equals(newEdge.getKernel().getType()) )
        {
            String edgeType = element.getAttribute(EDGE_TYPE_ATTR);
            if( edgeType.isEmpty() )
                edgeType = Type.TYPE_CATALYSIS;
            newEdge.getAttributes().add(new DynamicProperty(SBGNPropertyConstants.SBGN_EDGE_TYPE_PD, String.class, edgeType));
        }

        if( element.hasAttribute(FIXED_ATTR) )
            newEdge.setFixed(true);

        if( element.hasAttribute(FIXED_IN_OUT_ATTR) )
            newEdge.setFixedInOut(true);

        if( Util.isDirectedConnection(newEdge) && element.hasAttribute(FUNCTION_ATTR) )
        {
            newEdge.getRole(DirectedConnection.class).setFunction(element.getAttribute(FUNCTION_ATTR));
        }
        else if( Util.isUndirectedConnection(newEdge) )
        {
            UndirectedConnection uc = newEdge.getRole(UndirectedConnection.class);
            if( element.hasAttribute(MAIN_VAR_ATTR) )
                uc.setMainVariableType(MainVariableType.valueOf(element.getAttribute(MAIN_VAR_ATTR)));

            if( element.hasAttribute(CONVERSION_FACTOR_ATTR) )
                uc.setConversionFactor(element.getAttribute(CONVERSION_FACTOR_ATTR));
        }

        Element path = XmlUtil.findElementByTagName(element, PATH_ELEMENT);
        if( path != null )
            readEdgePath(path, newEdge);

        Element paint = XmlUtil.findElementByTagName(element, EDGE_PAINT_ELEMENT);
        if( paint != null )
            readEdgePaint(paint, newEdge, diagram);

        SbgnSemanticController.setNeccessaryAttributes(newEdge);
        return newEdge;
    }

    protected void readEdgePath(Element element, Edge edge)
    {
        Path path = new Path();
        for( Element segment : XmlUtil.elements(element, SEGMENT_ELEMENT) )
        {
            String strType = segment.getAttribute(SEGMENT_TYPE_ATTR);
            int type = strType.equals(LINE_QUADRIC) ? 1 : strType.equals(LINE_CUBIC) ? 2 : 0;
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
        if( element.hasAttribute(SBGNPropertyConstants.STYLE_ATTR) )
        {
            edge.setPredefinedStyle(element.getAttribute(SBGNPropertyConstants.STYLE_ATTR));
        }
        else
        {
            String penAttr = element.getAttribute(SBGNPropertyConstants.LINE_PEN_ATTR);
            if( !penAttr.isEmpty() )
            {
                Pen pen = XmlSerializationUtils.readPen(element.getAttribute(SBGNPropertyConstants.LINE_PEN_ATTR));
                edge.setPredefinedStyle(DiagramElementStyle.STYLE_NOT_SELECTED);
                edge.getCustomStyle().setPen(pen);
            }
        }
    }

    protected void readStates(Element element, Diagram diagram) throws Exception
    {
        if( element != null )
            for( Element child : XmlUtil.elements(element, StateXmlSerializer.STATE_ELEMENT) )
                diagram.addState(StateXmlSerializer.readXmlElement(child, diagram, this));
    }

    protected void copyAttributes(DynamicPropertySet from, DynamicPropertySet to)
    {
        from.forEach(dp -> to.add(dp));
    }

    /**
     * Get parent compartment by complete name
     */
    protected Compartment getParentCompartment(String completeName, Diagram diagram) throws Exception
    {
        Compartment parentCompartment = diagram;
        if( completeName.trim().length() == 0 )
            return parentCompartment;

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
    protected static Point getNodeLocation(Element element)
    {
        String x = element.getAttribute(X_ATTR);
        String y = element.getAttribute(Y_ATTR);
        if( !x.isEmpty() && !y.isEmpty() )
        {
            try
            {
                return new Point(Integer.parseInt(x), Integer.parseInt(y));
            }
            catch( NumberFormatException e )
            {
                log.log(Level.SEVERE, "Incorrect attribute value: x=" + x);
            }
        }
        return new Point();
    }

    /**
     * Read node dimension
     */
    protected static Dimension getNodeDimension(Element element)
    {

        String width = element.getAttribute(WIDTH_ATTR);
        String height = element.getAttribute(HEIGHT_ATTR);
        if( !width.isEmpty() && !height.isEmpty() )
        {
            try
            {
                return new Dimension(Integer.parseInt(width), Integer.parseInt(height));
            }
            catch( NumberFormatException e )
            {
                log.log(Level.SEVERE, "Incorrect node size attributes: width= " + width + " height= " + height);
            }
        }
        return new Dimension(20, 20);
    }

    private SubDiagram readSubDiagram(Element element, Compartment parent, String name, SubDiagram baseDiagramElement) throws Exception
    {
        Diagram innerDiagram = baseDiagramElement.getDiagram();
        if( ! ( innerDiagram.getType() instanceof SbmlDiagramType ) && ! ( innerDiagram.getType() instanceof SbgnDiagramType ) )
            return new SubDiagram(parent, innerDiagram, name);

        Diagram sbgnDiagram = null;
        if( ! ( innerDiagram.getType() instanceof SbgnDiagramType ) )
        {
            DynamicProperty p = innerDiagram.getAttributes().getProperty(SBGNPropertyConstants.SBGN_ATTRIBUTE_NAME);
            if( p != null && p.getValue() instanceof Diagram )
                sbgnDiagram = (Diagram)p.getValue();
        }
        else
            sbgnDiagram = innerDiagram;
        SubDiagram subDiagram = new SubDiagram(parent, sbgnDiagram, name);
        Diagram innerSBGNDiagram = subDiagram.getDiagram();
        String stateName = element.getAttribute("state");
        //sbgn is read from the model earlier then states, therefore we need to copy states to the sbgn diagram
        //for non-composite diagrams it is made while doGet from transformedDataCollection, however subdiagrams are not in the TransformedDataCollection
        //probably this should be handled in one way for all diagrams
        innerSBGNDiagram.removeStates();
        for( State state : innerDiagram.states() )
            state.clone(innerSBGNDiagram, state.getName());
        innerSBGNDiagram.setCurrentStateName(stateName);
        return subDiagram;
    }

    private ModelDefinition readModelDefinition(Element element, Compartment parent, String name, ModelDefinition baseDiagramElement)
    {
        Diagram innerDiagram = baseDiagramElement.getDiagram();
        DynamicProperty p = innerDiagram.getAttributes().getProperty(SBGNPropertyConstants.SBGN_ATTRIBUTE_NAME);
        if( p != null && p.getValue() instanceof Diagram )
            innerDiagram = (Diagram)p.getValue();
        return new ModelDefinition(parent, innerDiagram, name);
    }

    /**
     * In SBML there edge direction can be changed after writing and reading
     * because in SBML there is no actual "edge". Instead it deals with "replacements" which direction is not related to edge direction
     * @param oldEdge
     * @param suspectInput
     * @param suspectOutput
     * @param id
     * @return
     */
    private Edge createConnectionEdge(Edge oldEdge, Node suspectInput, Node suspectOutput, String id)
    {
        Node oldInput = oldEdge.getInput();
        Node newInput = suspectInput;
        Node newOutput = suspectOutput;
        if( sbmlToSbgn.containsKey(oldInput) )
        {
            for( Node sbgnNode : sbmlToSbgn.get(oldInput) )
            {
                if( sbgnNode.equals(suspectInput) )
                {
                    newInput = suspectInput;
                    newOutput = suspectOutput;
                    break;
                }
                else if( sbgnNode.equals(suspectOutput) )
                {
                    newInput = suspectOutput;
                    newOutput = suspectInput;
                    break;
                }
            }
        }
        return new Edge(id, oldEdge.getKernel(), newInput, newOutput);
    }

    protected void readLayouters(Element sbgnElement, Diagram diagram)
    {
        Element layouterInfoElement = XmlUtil.findElementByTagName(sbgnElement, DiagramXmlConstants.LAYOUTER_INFO_ELEMENT);
        if( layouterInfoElement != null )
        {
            Layouter layouter = DiagramXmlReader.readLayouterInfo(layouterInfoElement);
            if( layouter != null )
                diagram.setPathLayouter(layouter);
        }

        Element labelLayouterInfoElement = XmlUtil.findElementByTagName(sbgnElement, DiagramXmlConstants.LABEL_LAYOUTER_INFO_ELEMENT);
        if( labelLayouterInfoElement != null )
        {
            Layouter layouter = DiagramXmlReader.readLayouterInfo(labelLayouterInfoElement);
            if( layouter != null )
                diagram.setLabelLayouter(layouter);
        }
    }
}
