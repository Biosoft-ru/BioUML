package biouml.plugins.sbgn.sbgnml;

import java.awt.Dimension;
import java.awt.Point;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import one.util.streamex.EntryStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import ru.biosoft.graph.Path;
import com.developmentontheedge.application.ApplicationUtils;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.SubDiagram;
import biouml.model.SubDiagram.PortOrientation;
import biouml.plugins.sbgn.SBGNPropertyConstants;
import biouml.plugins.sbgn.SbgnDiagramViewOptions;
import biouml.plugins.sbgn.SbgnUtil;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.Util;
import biouml.standard.type.Reaction;
import biouml.standard.type.Specie;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import biouml.plugins.sbgn.Type;

public class SbgnMlWriter
{
    private Document document;
    private Diagram diagram;
    private Map<String, String> bioumlToSbgnId;

    Map<Edge, String> sourcePorts = new HashMap<>();
    Map<Edge, String> targetPorts = new HashMap<>();
    
    public Map<String, String> getBioumlToSbgnId()
    {
        return bioumlToSbgnId;
    }

    /**
     * Correspondence between biouml and sbgn-ml orientations
    */
    private static final Map<String, String> orientationMapWrite =
            EntryStream.of("top", "up", "bottom", "down", "left", "right", "right", "left").toMap();

    private static final Map<String, String> reactionOrientation =
            EntryStream.of("top", "vertical", "bottom", "vertical", "left", "horizontal", "right", "horizontal").toMap();

    /**
     * Correspondence between sbgn-ml and biouml orientations
    */
    static final Map<String, String> orientationMapRead = EntryStream.of( orientationMapWrite ).invert().toMap();

    public void write(Diagram diagram, File file) throws Exception
    {
        bioumlToSbgnId = new HashMap<>();
        Document document = createDOM(diagram);
        writeDocument(file, document);
    }

    public String getNameSpace()
    {
        return SbgnMlConstants.SBGN_ML_NAMESPACE;
    }

    protected Element createSBGNElement()
    {
        Element result = document.createElement(SbgnMlConstants.SBGN_ELEMENT);
        result.setAttribute(SbgnMlConstants.XMLNS_ATTR, getNameSpace());
        return result;
    }

    protected Element createGlyphElement(Node node, String type)
    {
        Element result = document.createElement(SbgnMlConstants.GLYPH_ELEMENT);
        result.setAttribute(SbgnMlConstants.ID_ATTR, getSbgnId(node));
        result.setAttribute(SbgnMlConstants.CLASS_ATTR, type);
        return result;
    }
    
    protected Element createBboxGlyphElement(Node node, String type)
    {
        Element element = createGlyphElement(node, type);
        writeLabelElement(node.getTitle(), element);
        writeBboxElement(node, element);
        return element;
    }

    protected Element createArcElement(Edge edge)
    {
        Element result = document.createElement(SbgnMlConstants.ARC_ELEMENT);

        String sourceAttr = sourcePorts.get(edge);
        String targetAttr = targetPorts.get(edge);

        result.setAttribute(SbgnMlConstants.SOURCE_ATTR, sourceAttr != null ? sourceAttr : getSbgnId(edge.getInput()));
        result.setAttribute(SbgnMlConstants.TARGET_ATTR, targetAttr != null ? targetAttr : getSbgnId(edge.getOutput()));
        result.setAttribute(SbgnMlConstants.ID_ATTR, getSbgnId(edge));
        return result;
    }

    public Document createDOM(Diagram sourceDiagram) throws Exception
    {
        if( sourceDiagram == null )
            throw new NullPointerException("Diagram to export not found.");

        diagram = sourceDiagram.clone(null, sourceDiagram.getName());
        diagram.getType().getDiagramViewBuilder().createDiagramView(diagram, ApplicationUtils.getGraphics());
        Util.moveToPositive(diagram);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        document = builder.newDocument();

        Element element = createSBGNElement();
        document.appendChild(element);

        Element mapElement = document.createElement(SbgnMlConstants.MAP_ELEMENT);
        mapElement.setAttribute(SbgnMlConstants.LANGUAGE_ATTR, SbgnMlConstants.PROCESS_DESCRIPTION);
        element.appendChild(mapElement);
        bioumlToSbgnId = generateSBGNIds(diagram);
        writeDiagram(mapElement);
        return document;
    }

    protected void writeDiagram(Element map) throws Exception
    {
        writeCompartmentList(map);
        writeSpeciesList(map);
        writeStubList(map);
        writeSubmapList(map);
        writeReactionList(map);
        writeEdges(map);
    }

    public static void writeDocument(File file, Document document) throws Exception
    {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2"); // set because default indent amount is zero
        try (OutputStream os = new FileOutputStream(file))
        {
            transformer.transform(new DOMSource(document), new StreamResult(os));
        }
    }

    protected void writeCompartmentList(Element map)
    {
        diagram.recursiveStream().select(Compartment.class).filter(c -> c.getKernel() instanceof biouml.standard.type.Compartment)
                .forEach(compartment ->
                {
                    Element element = createGlyphElement(compartment, SbgnMlConstants.COMPARTMENT_CLASS);
                    boolean lyoutedTitle = false;
                    if( compartment.getTitle().isEmpty() )
                    {
                        Node node = (Node)compartment.stream().findAny(de -> Type.TYPE_NOTE.equals(de.getKernel().getType())).orElse(null);
                        if( node != null )
                        {
                            lyoutedTitle = true;
                            writeBboxElement(node, writeLabelElement(node.getTitle(), element));
                        }
                    }

                    if( !lyoutedTitle )
                        writeLabelElement(compartment.getTitle(), element);

                    writeBboxElement(compartment, element);
                    map.appendChild(element);
                });
    }


    protected void writeSpeciesList(Element map)
    {
        diagram.recursiveStream().select(Node.class)
                .filter(node -> node.getKernel() instanceof Specie && ! ( node.getCompartment().getKernel() instanceof Specie ))
                .forEach(node -> {
                    writeSpecies((Compartment)node, map);
                });
    }

    protected void writeSpecies(Compartment species, Element map)
    {
        Element element = writeSpecie(species);
        map.appendChild(element);
        for( Node node : species.stream().select(Node.class) )
        {
            if( node.getKernel() instanceof Specie )
                writeSpecies((Compartment)node, element);
            else if( node.getKernel().getType().equals(Type.TYPE_UNIT_OF_INFORMATION) )
                writeUnitOfInformation(node, element);
            else if( node.getKernel().getType().equals(Type.TYPE_VARIABLE) )
                writeVariable(node, element);
        }
    }

    private Element writeSpecie(Node de)
    {
        Element element = createGlyphElement(de, de.getKernel().getType());

        if( de.getCompartment().getKernel() instanceof biouml.standard.type.Compartment )
            element.setAttribute(SbgnMlConstants.COMPARTMENT_REF_ATTR, getSbgnId(de.getCompartment()));

        writeLabelElement(de.getTitle(), element);

        if( SbgnUtil.isClone(de) )
        {
            Element cloneElement = document.createElement(SbgnMlConstants.CLONE_ATTR);
            String cloneMarker = de.getAttributes().getValueAsString(SBGNPropertyConstants.SBGN_CLONE_MARKER);
            element.appendChild(cloneElement);
            if( cloneMarker != null )
            {
                Element cloneLabelElement = document.createElement(SbgnMlConstants.LABEL_ATTR);
                cloneLabelElement.setAttribute(SbgnMlConstants.TEXT_ATTR, cloneMarker);
                cloneElement.appendChild(cloneLabelElement);
            }
        }
        writeBboxElement(de, element);//for some reason some other tools (e.g.VANTED) wants clone before bbox
        return element;
    }

    private void writeVariable(Node de, Element element)
    {
        Element subElement = createGlyphElement(de, SbgnMlConstants.VARIABLE_CLASS);
        Element stateElement = document.createElement(SbgnMlConstants.STATE_ATTR);
        String[] state = de.getTitle().split("@");
        if( !state[0].equals("") )
            stateElement.setAttribute(SbgnMlConstants.VALUE_ATTR, state[0]);
        if( state.length > 1 )
            stateElement.setAttribute(SbgnMlConstants.VARIABLE_ATTR, state[1]);
        subElement.appendChild(stateElement);
        writeBboxElement(de, subElement);
        element.appendChild(subElement);
    }

    private void writeUnitOfInformation(Node de, Element element)
    {
        element.appendChild(createBboxGlyphElement(de, de.getKernel().getType()));
    }

    protected void writeSubmapList(Element map)
    {
        diagram.recursiveStream().select(SubDiagram.class).forEach(subDiagram -> {
            Element element = createBboxGlyphElement(subDiagram, SbgnMlConstants.SUBMAP_CLASS);

            for( DiagramElement node : subDiagram )
            {
                Element child = createBboxGlyphElement((Node)node, SbgnMlConstants.TERMINAL_CLASS);
                String orientation = orientationMapWrite.get(node.getAttributes().getValueAsString(SBGNPropertyConstants.ORIENTATION));
                child.setAttribute(SbgnMlConstants.ORIENTATION_ATTR, orientation != null ? orientation : "left");
                element.appendChild(child);
            }
            map.appendChild(element);
        });
    }

    protected void writeStubList(Element map)
    {
        diagram.recursiveStream().select(Node.class)
                .filter( node -> node.getKernel() instanceof Stub && ! ( node.getKernel().getType().equals( Type.TYPE_NOTE )
                        || node.getKernel().getType().equals( Type.TYPE_UNIT_OF_INFORMATION )
                        || node.getKernel().getType().equals( Type.TYPE_VARIABLE ) || Util.isSubDiagram( node )
                        || Util.isSubDiagram( node.getCompartment() ) ) )
                .forEach(node -> {
                    Element element = createGlyphElement(node, getStubClass(node));
                    
                    if( node.getCompartment().getKernel() instanceof biouml.standard.type.Compartment )
                        element.setAttribute(SbgnMlConstants.COMPARTMENT_REF_ATTR, getSbgnId(node.getCompartment()));

                    if( Util.isPort(node) )
                    {
                        writeLabelElement(node.getTitle(), element);
                        String orientation = orientationMapWrite
                                .get(node.getAttributes().getValueAsString(SBGNPropertyConstants.ORIENTATION));
                        element.setAttribute(SbgnMlConstants.ORIENTATION_ATTR, orientation != null ? orientation : "left");
                    }

                    if( SbgnUtil.isLogical(node) )
                    {
                        //preprocessing
                        int w = node.getShapeSize().width;
                        int h = node.getShapeSize().height;
                        int r = Math.min(w, h);
                        node.setShapeSize(new Dimension(r, r));

                        Point location = node.getLocation();
                        location.translate( ( w - r ) / 2, ( h - r ) / 2);
                        node.setLocation(location);

                        PortOrientation orientation = (PortOrientation)node.getAttributes().getValue(SBGNPropertyConstants.ORIENTATION);
                        element.setAttribute(SbgnMlConstants.ORIENTATION_ATTR,
                                orientation != null ? reactionOrientation.get(orientation.toString()) : "horizontal");

                        boolean inputPort = false;
                        boolean outputPort = false;
                        for( Edge e : node.getEdges() )
                        {
                            if( Util.isModifier(e) )
                            {
                                if( e.getInput().equals(node) )
                                {
                                    sourcePorts.put(e, node.getName() + ".0");
                                    if( !inputPort )
                                    {
                                        addPortElement(node.getName() + ".0", e.getInPort(), element);
                                        inputPort = true;
                                    }
                                }
                                else
                                {
                                    targetPorts.put(e, node.getName() + ".1");
                                    if( !outputPort )
                                    {
                                        addPortElement(node.getName() + ".1", e.getOutPort(), element);
                                        outputPort = true;
                                    }
                                }
                            }
                        }

                    }

                    if( Type.TYPE_PHENOTYPE.equals(node.getKernel().getType()) )
                    {
                        writeLabelElement(node.getTitle(), element);
                    }
                    writeBboxElement(node, element);
                    map.appendChild(element);
                });
    }

    private void writeReactionList(Element map)
    {
        for( Node node : DiagramUtility.getReactionNodes(diagram) )
        {
            Element element = createGlyphElement(node, node.getAttributes().getValueAsString(SBGNPropertyConstants.SBGN_REACTION_TYPE));
            if( ( (SbgnDiagramViewOptions)diagram.getViewOptions() ).isOrientedReactions() )
            {
                Object orientationObject = node.getAttributes().getValue(SBGNPropertyConstants.ORIENTATION);
                if( orientationObject != null )
                {
                    PortOrientation orientation = (PortOrientation)orientationObject;
                    String sbgnOrientation = reactionOrientation.get(orientation.toString());
                    element.setAttribute(SbgnMlConstants.ORIENTATION_ATTR, sbgnOrientation != null ? sbgnOrientation : "left");
                }

                //preprocessing
                int w = node.getShapeSize().width;
                int h = node.getShapeSize().height;
                int r = Math.min(w, h);
                node.setShapeSize(new Dimension(r, r));

                Point location = node.getLocation();
                location.translate( ( w - r ) / 2, ( h - r ) / 2);
                node.setLocation(location);
            }
            writeBboxElement(node, element);
            boolean reversible = ( (Reaction)node.getKernel() ).isReversible();
            boolean inputPort = false;
            boolean outputPort = false;
            for( Edge e : node.getEdges() )
            {
                if( e.getKernel() instanceof SpecieReference && ( (SpecieReference)e.getKernel() ).isReactantOrProduct()
                        || Type.TYPE_PRODUCTION.equals(e.getKernel().getType()) || Type.TYPE_CONSUMPTION.equals(e.getKernel().getType()) )
                {
                    if( e.getInput().equals(node) )
                    {
                        sourcePorts.put(e, node.getName() + ".0");

                        if( !inputPort )
                        {
                            addPortElement(node.getName() + ".0", e.getInPort(), element);
                            inputPort = true;
                        }
                    }
                    else
                    {
                        if( reversible )
                            sourcePorts.put(e, node.getName() + ".1");
                        else
                            targetPorts.put(e, node.getName() + ".1");

                        if( !outputPort )
                        {
                            addPortElement(node.getName() + ".1", e.getOutPort(), element);
                            outputPort = true;
                        }
                    }

                    if( reversible && Util.isReactant(e) )
                    {
                        Node input = e.getInput();
                        e.setInput(e.getOutput());
                        e.setOutput(input);
                        e.setPath(SbgnMlReader.reversePath(e.getPath()));
                    }
                }
            }
            map.appendChild(element);
        }
    }

    private void writeEdges(Element map)
    {
        diagram.recursiveStream().select( Edge.class ).filter( e -> acceptEdge( e ) ).forEach( edge -> {
            Element element = createArcElement(edge);
            setArcClass(edge, element);
            writePath(edge, element);
            map.appendChild(element);
        });
    }

    private boolean acceptEdge(Edge e)
    {
        return e.getKernel() != null && !e.getKernel().getType().equals( Type.TYPE_NOTELINK );
    }

    public String castBioumlToSbgnId(String input)
    {
        String result = input.replaceAll("\\W", "_");
        if( result.matches("\\d\\w*") )
            result = "_" + result;
        return result;
    }

    private Map<String, String> generateSBGNIds(Diagram diagram)
    {
        Map<String, String> result = new HashMap<>();
        Set<String> sbgnIds = new HashSet<>();
        diagram.recursiveStream().forEach(de -> {
            String sbgnId = castBioumlToSbgnId(de.getName());
            String resultId = sbgnId;

            int i = 2;
            while( sbgnIds.contains(resultId) )
                resultId += "_" + i++;

            sbgnIds.add(resultId);
            result.put(de.getCompleteNameInDiagram(), resultId);
        });
        return result;
    }

    protected String getSbgnId(DiagramElement de)
    {
        return bioumlToSbgnId.get(de.getCompleteNameInDiagram());
    }

    private String getStubClass(Node node)
    {
        String type = node.getKernel().getType();
        if( Util.isContactPort(node) )
            return SbgnMlConstants.TAG_CLASS;
        else if( type.equals(Type.TYPE_SOURCE_SINK) )
            return SbgnMlConstants.SOURCE_SINK_CLASS;
        else if( type.equals(Type.TYPE_LOGICAL) )
            return node.getAttributes().getValueAsString(SBGNPropertyConstants.SBGN_LOGICAL_OPERATOR).toLowerCase(); //in BioUML: "And", "Or", "Not", in SBGN-ML: "and", "or", "not"
        return type;
    }

    private void setArcClass(Edge edge, Element element)
    {
        Node reaction = Util.isReaction(edge.getInput()) ? edge.getInput() : Util.isReaction(edge.getOutput()) ? edge.getOutput() : null;
        boolean reversible = reaction != null && ( (Reaction)reaction.getKernel() ).isReversible();
        String role = ( edge.getKernel() instanceof SpecieReference ) ? ( (SpecieReference)edge.getKernel() ).getRole()
                : edge.getKernel().getType();
        if( role.equals(SpecieReference.PRODUCT) || ( reversible && role.equals(SpecieReference.REACTANT) ) )
            role = Type.TYPE_PRODUCTION;
        else if( role.equals(SpecieReference.REACTANT) )
            role = Type.TYPE_CONSUMPTION;
        else if( role.equals( SpecieReference.MODIFIER ) || role.equals( Type.TYPE_REGULATION ) )
        {
            if( edge.getKernel() instanceof SpecieReference )
                role = ( (SpecieReference)edge.getKernel() ).getModifierAction();
            else
                role = edge.getAttributes().getValueAsString( SBGNPropertyConstants.SBGN_EDGE_TYPE );
        }
        else if( role.equals( Type.TYPE_PORTLINK ) || edge.getKernel() instanceof Stub.UndirectedConnection
                || edge.getKernel() instanceof Stub.DirectedConnection )
            role = Type.TYPE_EQUIVALENCE_ARC;
        element.setAttribute(SbgnMlConstants.CLASS_ATTR, role);
    }

    private Element writeLabelElement(String label, Element element)
    {
        Element labelElement = document.createElement(SbgnMlConstants.LABEL_ATTR);
        labelElement.setAttribute(SbgnMlConstants.TEXT_ATTR, label);
        element.appendChild(labelElement);
        return labelElement;
    }

    private void writeBboxElement(Node node, Element element)
    {
        Element bboxElement = document.createElement(SbgnMlConstants.BBOX_ATTR);
        setLocation(node, bboxElement);
        setShapeSize(node, bboxElement);
        element.appendChild(bboxElement);
    }

    private void addPortElement(String name, Point p, Element element)
    {
        Element portElement = document.createElement(SbgnMlConstants.PORT_ELEMENT);
        portElement.setAttribute(SbgnMlConstants.ID_ATTR, name);
        portElement.setAttribute(SbgnMlConstants.X_ATTR, Integer.toString(p.x));
        portElement.setAttribute(SbgnMlConstants.Y_ATTR, Integer.toString(p.y));
        element.appendChild(portElement);
    }

    private void setLocation(Node node, Element element)
    {
        element.setAttribute(SbgnMlConstants.X_ATTR, Integer.toString(node.getLocation().x));
        element.setAttribute(SbgnMlConstants.Y_ATTR, Integer.toString(node.getLocation().y));
    }

    private void setShapeSize(Node node, Element element)
    {
        element.setAttribute(SbgnMlConstants.HEIGHT_ATTR, Integer.toString(node.getShapeSize().height));
        element.setAttribute(SbgnMlConstants.WIDTH_ATTR, Integer.toString(node.getShapeSize().width));
    }

    private void writePath(Edge edge, Element element)
    {
        Path path = edge.getPath();
        element.appendChild(createPathElement(SbgnMlConstants.START_ATTR, path.xpoints[0], path.ypoints[0]));
        List<Point> pendingPoints = new ArrayList<>();
        for( int i = 1; i < path.npoints; i++ )
        {
            int type = path.pointTypes[i];

            if( type > 0 ) //this is control point, should be added later
                pendingPoints.add(new Point(path.xpoints[i], path.ypoints[i]));

            else if( i > 1 && path.pointTypes[i - 1] == 2 ) //this is control point because previous point is cubic control point
                pendingPoints.add(new Point(path.xpoints[i], path.ypoints[i]));

            else
            {
                String tag = ( i == path.npoints - 1 ) ? SbgnMlConstants.END_ATTR : SbgnMlConstants.NEXT_ATTR;
                Element nextElement = createPathElement(tag, path.xpoints[i], path.ypoints[i]);
                for( Point pendingPoint : pendingPoints )
                    nextElement.appendChild(createPathElement(SbgnMlConstants.POINT_ATTR, pendingPoint.x, pendingPoint.y));
                pendingPoints.clear();
                element.appendChild(nextElement);
            }
        }
    }

    private Element createPathElement(String attr, double x, double y)
    {
        Element pathElement = document.createElement(attr);
        pathElement.setAttribute(SbgnMlConstants.X_ATTR, Double.toString(x));
        pathElement.setAttribute(SbgnMlConstants.Y_ATTR, Double.toString(y));
        return pathElement;
    }
}