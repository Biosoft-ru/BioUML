package biouml.plugins.sbgn.sbgnml;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramType;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.SubDiagram;
import biouml.model.SubDiagram.PortOrientation;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.UndirectedConnection;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.sbgn.SBGNPropertyConstants;
import biouml.plugins.sbgn.SBGNXmlConstants;
import biouml.plugins.sbgn.SbgnCompositeDiagramType;
import biouml.plugins.sbgn.SbgnDiagramType;
import biouml.plugins.sbgn.SbgnDiagramViewOptions;
import biouml.plugins.sbgn.SbgnSemanticController;
import biouml.plugins.sbgn.SbgnUtil;
import biouml.plugins.sbgn.Type;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.Util;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.KineticLaw;
import biouml.standard.type.Reaction;
import biouml.standard.type.Specie;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import biouml.standard.type.Stub.ConnectionPort;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.graph.Path;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.View;
import ru.biosoft.util.DPSUtils;
import ru.biosoft.util.XmlUtil;

public class SbgnMlReader extends SBGNPropertyConstants
{
    private Logger log = Logger.getLogger(SbgnMlReader.class.getName());
    private Map<Node, NodePortInfo> nodePorts;
    private HashMap<String, Node> sbgnToBioUMLNode;
    private HashMap<Node, String> sbgnOrientation;
    private HashMap<String, Node> cloneToMainNode;
    private EModel eModel;
    private Diagram diagram;
    private List<Edge> badEdges = new ArrayList();

    protected static final Set<String> reactionEdgeClasses = StreamEx.of( Type.TYPE_CONSUMPTION, Type.TYPE_PRODUCTION, Type.TYPE_CATALYSIS,
            Type.TYPE_INHIBITION, Type.TYPE_MODULATION, Type.TYPE_NECCESSARY_STIMULATION, Type.TYPE_STIMULATION, Type.TYPE_EQUIVALENCE_ARC,
            Type.TYPE_LOGIC_ARC ).toSet();
    
    public Diagram read(DataCollection origin, File file, String diagramName) throws Exception
    {
        init();
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
        Element model = getElement(document.getDocumentElement(), SbgnMlConstants.MAP_ELEMENT);
        DiagramType type = haveSubMaps(model) ? new SbgnCompositeDiagramType() : new SbgnDiagramType();
        diagram = type.createDiagram(origin, diagramName, new DiagramInfo(null, diagramName));
        eModel = diagram.getRole( EModel.class );    
        SbgnDiagramViewOptions options = (SbgnDiagramViewOptions)diagram.getViewOptions(); //set default options
        options.setAutoLayout( false ); //we assume that layout is established by SBGN ML document
        options.setAddSourceSink( false ); //sources and sinks should be explicitly created in SBGN ML document
        options.setOrientedReactions(true); //correct SBGN requires that reactions are oriented and have outlets
        
        diagram.setNotificationEnabled(false); //avoid unnecessary property change firing         
        readCompartmentList(diagram, model);
        readSubmapList(diagram, model);
        readSpeciesList(diagram, model);
        readStubList(diagram, model);
        List<Node> reactions = readReactions(diagram, model);

        //set correct parent to nodes
        diagram.getType().getDiagramViewBuilder().createDiagramView( diagram, ApplicationUtils.getGraphics());

        adjustNodeParents(diagram);

        readEdges(diagram, model);

        adjustNodeOrientations( diagram );

        for( Edge edge : badEdges )
            diagram.getType().getSemanticController().recalculateEdgePath( edge );

        for( Node reaction : reactions )
            adjustReaction(reaction);

        //diagram arrange
        diagram.setView(null);
        diagram.setNotificationEnabled(true);
        return diagram;
    }
    
    private void init()
    {
        cloneToMainNode = new HashMap<>();
        sbgnToBioUMLNode = new HashMap<>();
        nodePorts = new HashMap<>();
        sbgnOrientation = new HashMap<>();
    }
   
    /**
     * @return true if SBGN ML map have submaps which are references to other SBGN maps.
     * If this is the case - we need to create composite diagram 
     */
    private boolean haveSubMaps(Element model)
    {
        for( Element child : XmlUtil.elements(model, SbgnMlConstants.GLYPH_ELEMENT) )
        {
            if( SbgnMlConstants.SUBMAP_CLASS.equals(child.getAttribute(SbgnMlConstants.CLASS_ATTR)) )
                return true;
        }
        return false;
    }

    private static boolean maybeReactant(Edge e)
    {
        return e.getTitle().contains("reactant"); //hack for exported from BioUML models. However here we have no reliable way to choose which edge should be reactant
    }

    private void adjustReaction(Node reactionNode) throws Exception
    {
        if( reactionNode.edges().noneMatch(e -> Util.isReactant(e)) )
        {
            NodePortInfo info = nodePorts.get(reactionNode);

            List<String> reactantPoints = reactionNode.edges().filter(e -> maybeReactant(e)).map(e -> info.getPortName(e)).toList();

            if( !reactantPoints.isEmpty() )
            {
                String portName = reactantPoints.get(0);
                for( Edge e : info.getEdges(portName) )
                {
                    try
                    {
                        SpecieReference newReference = new SpecieReference( (Reaction)reactionNode.getKernel(), reactionNode.getName(),
                                e.getOtherEnd( reactionNode ).getName(), SpecieReference.REACTANT );
                        newReference.setSpecie( ( (SpecieReference)e.getKernel() ).getSpecie());
                        Edge reversedEdge = new Edge(newReference, e.getOutput(), e.getInput());
                        SbgnSemanticController.setNeccessaryAttributes(reversedEdge);
                        reversedEdge.setPath(reversePath(e.getPath()));
                        reversedEdge.setRole(e.getRole());
                        e.getOrigin().remove(e.getName());
                        reversedEdge.save();
                        info.addEdge(portName, reversedEdge);
                        info.removeEdge(e);
                    }
                    catch( Exception ex )
                    {
                        log.log(Level.SEVERE, "Error during arc " + e.getName() + " postprocessing " + ex.getMessage()
                                + ". Created arc can be incorrect.");
                    }
                }
                Reaction r = (Reaction)reactionNode.getKernel();
                r.setReversible(true);
            }
        }
        SbgnSemanticController.setNeccessaryAttributes(reactionNode);
        
        //TODO: reuse code from ReactionProperties
       SbgnUtil.generateSourceSink(reactionNode, true);
    }

    public static Path reversePath(Path path)
    {
        Path reversedPath = new Path();
        for( int i = path.npoints - 1; i >= 0; i-- )
            reversedPath.addPoint(path.xpoints[i], path.ypoints[i], path.pointTypes[i]);
        return reversedPath;
    }

    private void readCompartmentList(Diagram diagram, Element model) throws Exception
    {
        for( Element child : XmlUtil.elements(model, SbgnMlConstants.GLYPH_ELEMENT) )
        {
            String classAttr = child.getAttribute(SbgnMlConstants.CLASS_ATTR);
            if( SbgnMlConstants.COMPARTMENT_CLASS.equals(classAttr) )
            {
                String idAttr = child.getAttribute(SbgnMlConstants.ID_ATTR);
                Compartment compartment = new Compartment(diagram, new biouml.standard.type.Compartment(null, idAttr));
                VariableRole varRole = new VariableRole(compartment);
                compartment.setRole(varRole);
                SbgnSemanticController.setNeccessaryAttributes(compartment);
                readCompartmentTitle(child, compartment);
                readSize(child, compartment);
                eModel.getVariables().put( varRole );
                sbgnToBioUMLNode.put(idAttr, compartment);
                compartment.save();
            }
        }
    }

    private void readSpeciesList(Diagram diagram, Element model) throws Exception
    {
        for( Element child : XmlUtil.elements(model, SbgnMlConstants.GLYPH_ELEMENT) )
        {
            String classAttr = child.getAttribute(SbgnMlConstants.CLASS_ATTR);
            if( SBGNPropertyConstants.entityTypes.contains(classAttr) )
                readSpecies(diagram, child, classAttr);
        }
    }

    private void readSpecies(Compartment c, Element child, String classAttr) throws Exception
    {
        Compartment specie = readSpecie(c, child, classAttr);
        for( Element subElement : XmlUtil.elements(child, SbgnMlConstants.GLYPH_ELEMENT) )
        {
            String subClassAttr = subElement.getAttribute(SbgnMlConstants.CLASS_ATTR);
            if( SBGNPropertyConstants.entityTypes.contains(subClassAttr) )
                readSpecies(specie, subElement, subClassAttr);
            if( SbgnMlConstants.VARIABLE_CLASS.equals(subClassAttr) )
                readVariable(specie, subElement, subClassAttr);
            if( Type.TYPE_UNIT_OF_INFORMATION.equals(subClassAttr) )
                readUnitOfInformation(specie, subElement, subClassAttr);
        }
    }

    private Compartment readSpecie( Compartment c, Element child, String classAttr) throws Exception
    {
        String idAttr = child.getAttribute(SbgnMlConstants.ID_ATTR);
        Specie kernel = new Specie(null, idAttr);
        SbgnUtil.setSBGNTypes(kernel);
        kernel.setType(classAttr);

        Compartment compartment = c;
        if( child.hasAttribute(SbgnMlConstants.COMPARTMENT_REF_ATTR) )
            compartment = (Compartment)diagram.findDiagramElement(child.getAttribute(SbgnMlConstants.COMPARTMENT_REF_ATTR));

        Compartment specie = new Compartment(compartment, kernel);
        VariableRole varRole = new VariableRole(specie); 
        specie.setRole( varRole );
        SbgnSemanticController.setNeccessaryAttributes(specie);
        readTitle(child, specie);
        Element cloneElement = getElement(child, SbgnMlConstants.CLONE_ATTR);
        if( cloneElement != null )
        {
            Element cloneLabelElement = getElement(cloneElement, SbgnMlConstants.LABEL_ATTR);
            String cloneAttr = ( cloneLabelElement != null && cloneLabelElement.hasAttribute(SbgnMlConstants.TEXT_ATTR) )
                    ? cloneLabelElement.getAttribute(SbgnMlConstants.TEXT_ATTR) : specie.getTitle();

            SbgnSemanticController.setCloneMarker(specie, cloneAttr);
                    
            Node mainNode = cloneToMainNode.get(cloneAttr);
            if( mainNode == null ) //means that this IS the main node
                cloneToMainNode.put(cloneAttr, specie);
            else
            {
                VariableRole role = (VariableRole)mainNode.getRole();
                role.addAssociatedElement( specie );
                specie.setRole( role );
            }
        }
        readSize(child, specie);
        specie.save();
        eModel.getVariables().put( varRole );
        sbgnToBioUMLNode.put(idAttr, specie);
        return specie;
    }

    private void readUnitOfInformation(Compartment origin, Element child, String classAttr) throws Exception
    {
        String idAttr = child.getAttribute(SbgnMlConstants.ID_ATTR);
        Node n = new Node(origin, new Stub(null, idAttr, classAttr));
        SbgnSemanticController.setNeccessaryAttributes(n);
        readTitle(child, n);
        readSize(child, n);
        n.save();
    }

    private void readVariable(Compartment origin, Element child, String classAttr) throws Exception
    {
        classAttr = Type.TYPE_VARIABLE;
        String idAttr = child.getAttribute(SbgnMlConstants.ID_ATTR);

        Node n = new Node(origin, new Stub(null, idAttr, classAttr));
        SbgnSemanticController.setNeccessaryAttributes(n);

        Element stateElement = getElement(child, SbgnMlConstants.STATE_ATTR);
        if( stateElement != null )
        {
            String value = stateElement.getAttribute(SbgnMlConstants.VALUE_ATTR);
            String variable = stateElement.getAttribute(SbgnMlConstants.VARIABLE_ATTR).isEmpty() ? ""
                    : "@".concat(stateElement.getAttribute(SbgnMlConstants.VARIABLE_ATTR));
            n.setTitle(value.concat(variable));
        }
        else
            n.setTitle("");
        readSize(child, n);
        n.save();
    }

    private void readSubmapList(Diagram diagram, Element model)
    {
        for( Element child : XmlUtil.elements(model, SbgnMlConstants.GLYPH_ELEMENT) )
        {
            try
            {
                String classAttr = child.getAttribute(SbgnMlConstants.CLASS_ATTR);
                if( classAttr.equals(SbgnMlConstants.SUBMAP_CLASS) )
                {
                    String idAttr = child.getAttribute(SbgnMlConstants.ID_ATTR);
                    Diagram newDiagram = new SbgnDiagramType().createDiagram(diagram.getOrigin(), idAttr, new DiagramInfo(idAttr));

                    EModel emodel = newDiagram.getRole( EModel.class );
                    SubDiagram subdiagram = new SubDiagram(diagram, newDiagram, idAttr);
                    SbgnSemanticController.setNeccessaryAttributes(subdiagram);

                    readTitle(child, subdiagram);
                    readSize(child, subdiagram);

                    if( diagram.getOrigin() != null )
                        newDiagram.save();
                    sbgnToBioUMLNode.put(idAttr, subdiagram);
                    subdiagram.save();

                    newDiagram = subdiagram.getDiagram();

                    Set<String> terminalNames = new HashSet<>();
                    for( Element portElement : XmlUtil.elements(child, SbgnMlConstants.GLYPH_ELEMENT) )
                    {
                        classAttr = portElement.getAttribute(SbgnMlConstants.CLASS_ATTR);
                        if( classAttr.equals(SbgnMlConstants.TERMINAL_CLASS) )
                        {
                            idAttr = portElement.getAttribute(SbgnMlConstants.ID_ATTR);
                            Node portNode = new Node(newDiagram,
                                    new ConnectionPort(idAttr, null, ConnectionPort.TYPE_CONTACT_CONNECTION_PORT));

                            emodel.declareVariable(idAttr + "_var", 0.0);
                            Util.setPortVariable(portNode, idAttr + "_var");
                            SbgnSemanticController.setNeccessaryAttributes(portNode);
                            terminalNames.add(idAttr);
                            portNode.save();
                            subdiagram.updatePorts();
                            Node realNode = (Node)subdiagram.get(idAttr);
                            readTitle(portElement, realNode);
                            readSize(portElement, realNode);
                            readOrientation(portElement, realNode);
                            sbgnToBioUMLNode.put(idAttr, realNode);
                        }
                    }
                }
            }
            catch( Exception ex )
            {

            }
        }
    }

    private void readStubList(Diagram diagram, Element model) throws Exception
    {
        for( Element child : XmlUtil.elements(model, SbgnMlConstants.GLYPH_ELEMENT) )
        {
            String classAttr = child.getAttribute(SbgnMlConstants.CLASS_ATTR);
            if( SbgnMlConstants.SOURCE_SINK_CLASS.equals(classAttr) || SbgnMlConstants.TAG_CLASS.equals(classAttr)
                    || SbgnMlConstants.LOGICAL_CLASSES.contains(classAttr) || Type.TYPE_PHENOTYPE.equals(classAttr) )
            {
                String type;
                if( SbgnMlConstants.SOURCE_SINK_CLASS.equals(classAttr) )
                    type = Type.TYPE_SOURCE_SINK;
                else if( SbgnMlConstants.LOGICAL_CLASSES.contains(classAttr) )
                    type = Type.TYPE_LOGICAL;
                else
                    type = classAttr;

                String idAttr = child.getAttribute(SbgnMlConstants.ID_ATTR);

                Compartment parent = diagram;
                if( child.hasAttribute(SbgnMlConstants.COMPARTMENT_REF_ATTR) )
                    parent = (Compartment)diagram.findDiagramElement(child.getAttribute(SbgnMlConstants.COMPARTMENT_REF_ATTR));

                Node node = SbgnMlConstants.TAG_CLASS.equals(classAttr) ? new Node(parent, new Stub.ContactConnectionPort(null, idAttr))
                        : Type.TYPE_PHENOTYPE.equals(classAttr) ? new Compartment(parent, new Stub(null, idAttr, type))
                                : new Node(parent, new Stub(null, idAttr, type));

                if( SbgnMlConstants.LOGICAL_CLASSES.contains(classAttr) )
                {
                    String orientation = child.getAttribute(SbgnMlConstants.ORIENTATION_ATTR);
                    if( !orientation.isEmpty() )
                        this.sbgnOrientation.put(node, orientation);

                    readNodePorts(child, node);

                    node.getAttributes().add(new DynamicProperty(SBGNPropertyConstants.SBGN_LOGICAL_OPERATOR_PD, String.class,
                            classAttr.substring(0, 1).toUpperCase() + classAttr.substring(1)));
                }

                if( SbgnMlConstants.TAG_CLASS.equals(classAttr) )
                {
                    readOrientation(child, node);
                    readTitle(child, node);
                }

                if( Type.TYPE_PHENOTYPE.equals(classAttr) )
                    readTitle(child, node);

                readSize(child, node);
                SbgnSemanticController.setNeccessaryAttributes(node);
                sbgnToBioUMLNode.put(idAttr, node);
                node.save();
            }
        }
    }

    private void adjustNodeParents(Diagram d) throws Exception
    {
        for( Node node : d.recursiveStream().select(Node.class).filter(n -> n.getParent() instanceof Diagram) )
        {
            if( node instanceof Diagram )
                continue;

            Point center = node.getLocation();
            center.translate(node.getShapeSize().width / 2, node.getShapeSize().height / 2);
            View parentView = ( (CompositeView)d.getView() ).getDeepestActive(center, node.recursiveStream().toArray(), Compartment.class);
            DiagramElement newParent = (DiagramElement)parentView.getModel();

            if( newParent instanceof Compartment && ! ( newParent instanceof Diagram ) )
            {
                node.getCompartment().remove(node.getName());
                node.setOrigin((Compartment)newParent);
                node.save();
            }
        }
    }

    private Point[] locateInputOutput(NodePortInfo info, Node node)
    {
        boolean reaction = Util.isReaction(node); ///it can be reaction or logical operator

        boolean outputLocated = false;
        boolean inputLocated = false;

        Point outputPoint = null;
        Point inputPoint = null;

        for( Edge e : node.getEdges() )
        {
            if( ( reaction && Util.isProduct(e) ) || ( !reaction && "regulation".equals(e.getKernel().getType()) ) )
            {
                if( !outputLocated )
                {
                    outputPoint = info.getPort(e);
                    outputLocated = true;
                }
                else if( !inputLocated )
                    inputPoint = info.getPort(e);
            }
            else if( ( reaction && Util.isReactant(e) ) || ( !reaction && Util.isModifier(e) ) )
            {
                inputPoint = info.getPort(e);
                inputLocated = true;
            }

            if( inputLocated && outputLocated )
                break;
        }
        return new Point[] {inputPoint, outputPoint};
    }

    private void adjustNodeOrientations(Diagram d) throws Exception
    {
        for( Node node : d.recursiveStream().select(Node.class).filter(n -> Util.isReaction(n) || SbgnUtil.isLogical(n)) )
        {
            NodePortInfo info = nodePorts.get(node);

            if( info.isEmpty() )
                continue;

            Point center = node.getLocation();
            center.translate(node.getShapeSize().width / 2, node.getShapeSize().height / 2);

            Point[] points = locateInputOutput(info, node);

            Point inputPoint = points[0];
            Point outputPoint = points[1];
            boolean topRight = outputPoint.y - center.y < outputPoint.x - center.x;
            boolean topLeft = outputPoint.y - center.y < center.x - outputPoint.x;

            PortOrientation orientation;

            String preOrientation = sbgnOrientation.get(node);

            if( SBGNXmlConstants.VERTICAL.equals(preOrientation) )
            {
                orientation = topLeft || topRight ? PortOrientation.TOP : PortOrientation.BOTTOM;
            }
            else if( SBGNXmlConstants.HORIZONTAL.equals(preOrientation) )
            {
                orientation = topLeft ? PortOrientation.LEFT : PortOrientation.RIGHT;
            }
            else //sometimes there is no orientation for process in sbgn-ml document
            {
                if( topRight )
                    orientation = topLeft ? PortOrientation.TOP : PortOrientation.RIGHT;
                else
                    orientation = topLeft ? PortOrientation.LEFT : PortOrientation.BOTTOM;
            }

            Point location = node.getLocation();
            if( orientation == PortOrientation.LEFT || orientation == PortOrientation.RIGHT )
            {
                location.x = Math.min(inputPoint.x, outputPoint.x);
                node.setShapeSize(new Dimension(Math.abs(inputPoint.x - outputPoint.x), node.getShapeSize().height));
            }
            else
            {
                location.y = Math.min(inputPoint.y, outputPoint.y);
                node.setShapeSize( new Dimension( node.getShapeSize().width, Math.abs( inputPoint.y - outputPoint.y ) ) );
            }

            node.setLocation(location);
            node.getAttributes().add(new DynamicProperty(PortOrientation.ORIENTATION_ATTR, PortOrientation.class, orientation));
        }
    }

    private List<Node> readReactions(Diagram diagram, Element model) throws Exception
    {
        List<Node> reactions = new ArrayList<>();
        for( Element child : XmlUtil.elements(model, SbgnMlConstants.GLYPH_ELEMENT) )
        {
            String classAttr = child.getAttribute(SbgnMlConstants.CLASS_ATTR);
            if( reactionTypes.contains(classAttr) )
            {
                String idAttr = child.getAttribute(SbgnMlConstants.ID_ATTR);
                Reaction reaction = new Reaction(null, idAttr);
                reaction.setKineticLaw(new KineticLaw());
                reaction.setParent(diagram);
                Node process = new Node(diagram, reaction);
                process.getAttributes().add(new DynamicProperty(SBGNPropertyConstants.SBGN_REACTION_TYPE_PD, String.class, classAttr));
                DiagramUtility.generateReactionRole(diagram, process);
                readTitle(child, process);
                readSize(child, process);
                String orientation = child.getAttribute(SbgnMlConstants.ORIENTATION_ATTR);
                if( !orientation.isEmpty() )
                    this.sbgnOrientation.put(process, orientation);
                readNodePorts(child, process);
                process.save();
                sbgnToBioUMLNode.put(idAttr, process);
                reactions.add(process);
            }
        }
        return reactions;
    }

    private void readNodePorts(Element reactionElement, Node node)
    {
        NodePortInfo nodePortInfo = new NodePortInfo();
        nodePorts.put(node, nodePortInfo);

        for( Element portElement : XmlUtil.elements(reactionElement, SbgnMlConstants.PORT_ELEMENT) )
        {
            int x = Double.valueOf(portElement.getAttribute(SbgnMlConstants.X_ATTR)).intValue();
            int y = Double.valueOf(portElement.getAttribute(SbgnMlConstants.Y_ATTR)).intValue();
            String id = portElement.getAttribute(SbgnMlConstants.ID_ATTR);
            nodePortInfo.addPort(new Point(x, y), id);
            sbgnToBioUMLNode.put(id, node);
        }
    }

    private void readEdges(Diagram diagram, Element model) throws Exception
    {
        List<SpecieReferenceInfo> unresolvedEdges = new ArrayList<>();
        for( Element child : XmlUtil.elements(model, SbgnMlConstants.ARC_ELEMENT) )
        {
            String classAttr = child.getAttribute(SbgnMlConstants.CLASS_ATTR);

            if( !reactionEdgeClasses.contains(classAttr) )
                continue;

            String idAttr = child.getAttribute(SbgnMlConstants.ID_ATTR);
            String targetAttr = child.getAttribute(SbgnMlConstants.TARGET_ATTR);
            String sourceAttr = child.getAttribute(SbgnMlConstants.SOURCE_ATTR);
            Node targetNode = sbgnToBioUMLNode.get(targetAttr);
            Node sourceNode = sbgnToBioUMLNode.get(sourceAttr);
            Edge e = null;
            Node mainNode = null;
            Node otherNode = null;

            if( Util.isReaction(targetNode) || Util.isPort(targetNode) || targetNode.getKernel().getType().equals(Type.TYPE_PHENOTYPE) )
            {
                mainNode = targetNode;
                otherNode = sourceNode;
            }
            else if( Util.isReaction(sourceNode) || Util.isPort(sourceNode)
                    || sourceNode.getKernel().getType().equals(Type.TYPE_PHENOTYPE) )
            {
                mainNode = sourceNode;
                otherNode = targetNode;
            }
            else if( SbgnUtil.isLogical(targetNode) )
            {
                unresolvedEdges
                        .add(new SpecieReferenceInfo(targetNode, targetAttr, sourceNode, sourceAttr, classAttr, readPath(child), idAttr));
                continue;
            }
            else if( SbgnUtil.isLogical(sourceNode) )
            {
                unresolvedEdges
                        .add(new SpecieReferenceInfo(sourceNode, sourceAttr, targetNode, targetAttr, classAttr, readPath(child), idAttr));
                continue;
            }

            if( mainNode == null || otherNode == null )
                throw new Exception("Can not create edge from " + sourceAttr + " to " + targetAttr);

            if( Util.isPort(mainNode) )
            {

                if( mainNode.getParent() instanceof SubDiagram )
                {
                    String varName = Util.getPortVariable(mainNode);
                    VariableRole varRole = otherNode.getRole( VariableRole.class );
                    String otherVar = varRole.getName();
                    String unqueName = DefaultSemanticController.generateUniqueNodeName(diagram, "connection");
                    e = new Edge(new Stub.UndirectedConnection(diagram, unqueName), mainNode, otherNode);
                    UndirectedConnection connection = new UndirectedConnection(e);
                    connection.setInputPort(new UndirectedConnection.Port(varName, varName));
                    connection.setOutputPort(new UndirectedConnection.Port(otherVar, otherVar));
                    e.setRole(connection);
                }
                else
                {
                    VariableRole varRole = otherNode.getRole( VariableRole.class );
                    String otherVar = varRole.getName();
                    mainNode.getAttributes().add(DPSUtils.createReadOnly(ConnectionPort.VARIABLE_NAME_ATTR, String.class, otherVar));

                    String unqueName = DefaultSemanticController.generateUniqueNodeName(diagram, mainNode.getName() + "_link");
                    e = new Edge(new Stub(diagram, unqueName, Type.TYPE_PORTLINK), mainNode, otherNode);
                }
            }

            else if( mainNode.getKernel().getType().equals(Type.TYPE_PHENOTYPE) || SbgnUtil.isLogical(mainNode) )
            {
                String uniqueName = DefaultSemanticController.generateUniqueNodeName(diagram, "regulation");
                e = new Edge(new Stub(diagram, uniqueName, "regulation"), otherNode, mainNode);
                e.getAttributes().add(new DynamicProperty(SBGNPropertyConstants.SBGN_EDGE_TYPE_PD, String.class, classAttr));
            }

            else if( otherNode.getKernel() instanceof Specie )
            {
                Reaction reaction = (Reaction)mainNode.getKernel();

                String role = Type.TYPE_CONSUMPTION.equals(classAttr) ? SpecieReference.REACTANT
                        : Type.TYPE_PRODUCTION.equals(classAttr) ? SpecieReference.PRODUCT : SpecieReference.MODIFIER;

                Node mainSpecie = (Node)otherNode.getRole( VariableRole.class ).getDiagramElement();
                SpecieReference ref = new SpecieReference(reaction, reaction.getName(), mainSpecie.getKernel().getName(), role);
                ref.setSpecie(mainSpecie.getCompleteNameInDiagram());
                e = SpecieReference.PRODUCT.equals(role) ? new Edge(ref, mainNode, otherNode) : new Edge(ref, otherNode, mainNode);
                DiagramUtility.generateReactionRole(diagram, e);

                if( SpecieReference.MODIFIER.equals(role) )
                    e.getAttributes().add(new DynamicProperty(SBGNPropertyConstants.SBGN_EDGE_TYPE_PD, String.class, classAttr));
                reaction.put(ref);
            }

            else if( otherNode.getKernel() instanceof Stub )
            {
                Compartment origin = (Compartment)mainNode.getOrigin();

                if( SbgnUtil.isLogical(otherNode) )
                {
                    String uniqueName = DefaultSemanticController.generateUniqueNodeName(diagram, "regulation");
                    e = new Edge(origin, new Stub(null, uniqueName, "regulation"), otherNode, mainNode);
                    e.getAttributes().add(new DynamicProperty(SBGNPropertyConstants.SBGN_EDGE_TYPE_PD, String.class, classAttr));
                }
                if( Type.TYPE_CONSUMPTION.equals(classAttr) )
                {
                    e = new Edge(origin, new Stub(null, otherNode.getName() + "Edge", "consumption"), otherNode, mainNode);
                }
                else if( Type.TYPE_PRODUCTION.equals(classAttr) )
                {
                    e = new Edge(origin, new Stub(null, otherNode.getName() + "Edge", "production"), mainNode, otherNode);
                }
            }

            if( e == null )
            {
                log.log(Level.SEVERE, "Unknown error during reading arc " + idAttr);
                continue;
            }
            SbgnSemanticController.setNeccessaryAttributes(e);
            e.setTitle(idAttr);
            e.setPath(readPath(child));

            if( e.getInPort().equals( e.getOutPort() ) )
                badEdges.add( e );

            e.save();
            NodePortInfo info = nodePorts.get(targetNode);
            if( info != null )
                info.addEdge(targetAttr, e);

            info = nodePorts.get(sourceNode);
            if( info != null )
                info.addEdge(sourceAttr, e);
        }
        for( SpecieReferenceInfo info : unresolvedEdges )
            info.generateEdge(diagram);
    }





    private Path readPath(Element element)
    {
        Path path = new Path();
        addPoint(path, getElement(element, SbgnMlConstants.START_ATTR));
        for( Element elem : XmlUtil.elements(element, SbgnMlConstants.NEXT_ATTR) )
            addCurveSegment(path, elem);
        addCurveSegment(path, getElement(element, SbgnMlConstants.END_ATTR));
        return path;
    }

    private void addPoint(Path path, Element elem)
    {
        path.addPoint( XmlUtil.readDoubleAsInt( elem, SbgnMlConstants.X_ATTR ), XmlUtil.readDoubleAsInt( elem, SbgnMlConstants.Y_ATTR ) );
    }

    private void addCurveSegment(Path path, Element elem)
    {
        Path subPath = new Path();
        for( Element subElem : XmlUtil.elements(elem, SbgnMlConstants.POINT_ATTR) )
            addPoint(subPath, subElem);

        if( subPath.npoints > 0 )
            path.addPoint(subPath.xpoints[0], subPath.ypoints[0], subPath.npoints);

        if( subPath.npoints > 1 )
            path.addPoint(subPath.xpoints[1], subPath.ypoints[1], 0);

        addPoint(path, elem);
    }

    /**
     * Get single child element
     */
    protected static Element getElement(Element element, String childName)
    {
        for( Element elem : XmlUtil.elements(element, childName) )
            return elem;
        return null;
    }

    static final Set<String> reactionTypes = new HashSet<>(
            Arrays.asList("process", "omitted process", "dissociation", "association", "uncertain process"));

    private void readOrientation(Element child, Node node)
    {
        String orientAttr = child.hasAttribute(SbgnMlConstants.ORIENTATION_ATTR)
                ? SbgnMlWriter.orientationMapRead.get(child.getAttribute(SbgnMlConstants.ORIENTATION_ATTR)) : "right";
        node.getAttributes().add(
                new DynamicProperty(PortOrientation.ORIENTATION_ATTR, PortOrientation.class, PortOrientation.getOrientation(orientAttr)));
    }

    private void readTitle(Element child, Node node)
    {
        Element labelElement = getElement(child, SbgnMlConstants.LABEL_ATTR);
        if( labelElement != null && labelElement.hasAttribute(SbgnMlConstants.TEXT_ATTR) )
            node.setTitle(labelElement.getAttribute(SbgnMlConstants.TEXT_ATTR));
    }

    private void readCompartmentTitle(Element child, Compartment compartment)
    {
        Element labelElement = getElement(child, SbgnMlConstants.LABEL_ATTR);
        if( labelElement != null && labelElement.hasAttribute(SbgnMlConstants.TEXT_ATTR) )
        {
            String text = labelElement.getAttribute(SbgnMlConstants.TEXT_ATTR);
            Element bbox = getElement(labelElement, SbgnMlConstants.BBOX_ATTR);
            if( bbox != null )
            {
                compartment.setTitle("");
                Stub.Note kernel = new Stub.Note(null,
                        DefaultSemanticController.generateUniqueNodeName(compartment, compartment.getName() + "__title"));
                kernel.setBackgroundVisible(false);
                Node note = new Node(compartment, kernel);
                readSize(labelElement, note);
                note.setTitle(text);
                compartment.put(note);
            }
            else
                compartment.setTitle(text);
        }
    }

    private void readSize(Element child, Node node)
    {
        Element bboxElement = getElement(child, SbgnMlConstants.BBOX_ATTR);
        if( bboxElement != null )
        {
            try
            {
                int x = XmlUtil.readDoubleAsInt( bboxElement, SbgnMlConstants.X_ATTR );
                int y = XmlUtil.readDoubleAsInt( bboxElement, SbgnMlConstants.Y_ATTR );
                int w = XmlUtil.readDoubleAsInt( bboxElement, SbgnMlConstants.WIDTH_ATTR );
                int h = XmlUtil.readDoubleAsInt( bboxElement, SbgnMlConstants.HEIGHT_ATTR );
                node.setLocation( new Point( x, y ) );
                node.setShapeSize( new Dimension( w, h ) );
            }
            catch( Exception ex )
            {
                log.log(Level.SEVERE, "Can not read bbox for compartment " + node.getName() + ": " + ex.getMessage());
            }
        }
    }

    protected static class NodePortInfo
    {
        private final Map<String, Point> nameToPort;
        private final Map<Edge, String> edgeToPortName;

        public NodePortInfo()
        {
            nameToPort = new HashMap<>();
            edgeToPortName = new HashMap<>();
        }

        public boolean isEmpty()
        {
            return nameToPort.isEmpty();
        }

        public Point getPort(Edge e)
        {
            return nameToPort.get(edgeToPortName.get(e));
        }

        public String getPortName(Edge e)
        {
            return edgeToPortName.get(e);
        }

        public List<Edge> getEdges(String name)
        {
            return StreamEx.of(edgeToPortName.keySet()).filter(e -> name.equals(edgeToPortName.get(e))).toList();
        }

        public void addPort(Point p, String name)
        {
            nameToPort.put(name, p);
        }

        public void addEdge(String name, Edge e)
        {
            if( nameToPort.containsKey(name) )
                edgeToPortName.put(e, name);
        }

        public void removeEdge(Edge e)
        {
            edgeToPortName.remove(e);
        }
    }

    protected class SpecieReferenceInfo
    {
        Node logical;
        Node otherNode;
        String edgeType;
        Path path;
        String title;
        String logicalPort;
        String otherPort;

        public SpecieReferenceInfo(Node logical, String logicalPort, Node otherNode, String otherPort, String edgeType, Path path,
                String title)
        {
            this.logical = logical;
            this.otherNode = otherNode;
            this.edgeType = edgeType;
            this.path = path;
            this.title = title;
            this.logicalPort = logicalPort;
            this.otherPort = otherPort;
        }

        public void generateEdge(Diagram diagram) throws Exception
        {
            Node reactionNode = SbgnUtil.getLogicReaction(logical);
            if( reactionNode == null )
                throw new Exception("Can not find reaction for logical operator " + logical.getName());
            Reaction r = (Reaction)reactionNode.getKernel();
            String id = reactionNode.getName() + ": " + otherNode.getKernel().getName() + " as " + "modifier";
            SpecieReference reference = new SpecieReference(r, id, SpecieReference.MODIFIER);
            reference.setSpecie(otherNode.getCompleteNameInDiagram());
            Edge e = new Edge(reference, otherNode, logical);
            e.setTitle(title);
            DiagramUtility.generateReactionRole(diagram, e);
            e.getAttributes().add(new DynamicProperty(SBGNPropertyConstants.SBGN_EDGE_TYPE_PD, String.class, edgeType));
            e.setPath(path);
            SbgnSemanticController.setNeccessaryAttributes(e);
            r.put(reference);
            e.save();
            NodePortInfo info = nodePorts.get(logical);
            if( info != null )
                info.addEdge(logicalPort, e);

            info = nodePorts.get(otherNode);
            if( info != null )
                info.addEdge(otherPort, e);
        }
    }

    public static void setBlackAndWhite(SbgnDiagramViewOptions options)
    {
        options.setCloneBrush(new Brush(Color.black));
        options.setMacromoleculeBrush(new Brush(Color.white));
        options.setComplexBrush(new Brush(Color.white));
        options.setMacromoleculeBrush(new Brush(Color.white));
        options.setNucleicBrush(new Brush(Color.white));
        options.setPerturbingBrush(new Brush(Color.white));
        options.setPhenotypeBrush(new Brush(Color.white));
        options.setSourceSinkBrush(new Brush(Color.white));
        options.setContactPortBrush(new Brush(Color.white));
        options.setInputPortBrush(new Brush(Color.white));
        options.setModuleBrush(new Brush(Color.white));
        options.setUnspecifiedBrush(new Brush(Color.white));
        options.setSimpleChemicalBrush(new Brush(Color.white));
        options.getPortTitleFont().setColor(Color.black);
        options.setEdgePen(new Pen(1, Color.black));
    }
    

}
