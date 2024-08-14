package biouml.plugins.sbml.celldesigner;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.Pen;
import ru.biosoft.util.DPSUtils;
import ru.biosoft.util.XmlUtil;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramElementStyle;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.Role;
import biouml.plugins.sbgn.SBGNPropertyConstants;
import biouml.plugins.sbgn.Type;
import biouml.plugins.sbml.converters.SBGNConverter;
import biouml.plugins.sbml.extensions.SbmlExtensionSupport;
import biouml.standard.type.Base;
import biouml.standard.type.Reaction;
import biouml.standard.type.Specie;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;

import com.developmentontheedge.beans.DynamicProperty;

/**
 * SBML extension to work with CellDesigner specific SBML annotations
 */
public class CellDesignerExtension extends SbmlExtensionSupport implements CellDesignerConstants
{
    protected static final Logger log = Logger.getLogger(CellDesignerExtension.class.getName());

    protected Map<DiagramElement, ElementSet> unprocessedElements = new HashMap<>();
    protected ReactionSpeciesCache reactionSpecieCache = new ReactionSpeciesCache();
    protected Map<String, Node> complexes = new HashMap<>();

    protected Element proteinListElement = null;
    protected Map<String, List<Object>> proteinRefs = new HashMap<>(); //proteinReference to list of node id
    protected Map<String, String> stateValues = new HashMap<>(); //state values temporary map

    @Override
    public void readElement(Element element, DiagramElement specie, @Nonnull Diagram diagram)
    {
        if( specie == diagram )
        {
            //global annotation element
            readIncludedSpecies(element, diagram);
            readCompartmentAliases(element, diagram);
            readComplexAliases(element, diagram);
            readSpecieAliases(element, diagram);

            Element listOfProteins = getTopElement(element, CELLDESIGNER_PROTEIN_LIST);
            if( listOfProteins != null )
            {
                proteinListElement = listOfProteins;
            }
        }
        else
        {
            //put to unprocessed map
            ElementSet elementSet = unprocessedElements.get(specie);
            if( elementSet == null )
            {
                elementSet = new ElementSet();
                unprocessedElements.put(specie, elementSet);
            }
            elementSet.addElement(element);
            if( specie instanceof Edge )
                reactionSpecieCache.addEdge((Edge)specie);
        }
    }

    @Override
    public Element[] writeElement(DiagramElement specie, Document document)
    {
        // TODO Auto-generated method stub
        return null;
    }

    //
    // Protected methods
    //

    /**
     * Process 'celldesigner:listOfCompartmentAliases'
     */
    protected void readCompartmentAliases(Element element, @Nonnull Diagram diagram)
    {
        Element listElement = getTopElement(element, CELLDESIGNER_COMPARTMENT_ALIASES_LIST);
        if( listElement == null )
            return;
        for( Element node : XmlUtil.elements(listElement) )
        {
            if( node.getNodeName().equals(CELLDESIGNER_COMPARTMENT_ALIAS) )
            {
                String id = node.getAttribute(COMPARTMENT_ATTR);
                readAlias(node, diagram, id, node.getAttribute(ID_ATTR), null, null);
                readCompartmentType(node, diagram, id);
            }
        }
    }

    /**
     * Process 'celldesigner:listOfComplexSpeciesAliases'
     */
    protected void readComplexAliases(Element element, @Nonnull Diagram diagram)
    {
        Element listElement = getTopElement(element, CELLDESIGNER_COMPLEX_ALIASES_LIST);
        if( listElement == null )
            return;
        for( Element node : XmlUtil.elements(listElement, CELLDESIGNER_COMPLEX_ALIAS) )
        {
            String complexID = node.getAttribute(ID_ATTR);
            String id = node.getAttribute(SPECIES_ATTR);
            String compartmentID = node.getAttribute(COMPARTMENT_ALIAS_ATTR);
            if( compartmentID.isEmpty() )
                compartmentID = null;
            Node specieNode = readAlias(node, diagram, id, complexID, null, compartmentID);
            if( specieNode != null )
                complexes.put(complexID, specieNode);
        }
    }

    /**
     * Process 'celldesigner:listOfIncludedSpecies'
     */
    protected void readIncludedSpecies(Element element, @Nonnull Diagram diagram)
    {
        Element listElement = getTopElement(element, CELLDESIGNER_SPECIE_INCLUDED_LIST);
        if( listElement == null )
            return;
        NodeList list = listElement.getChildNodes();
        for( int i = 0; i < list.getLength(); i++ )
        {
            org.w3c.dom.Node node = list.item(i);
            if( ( node instanceof Element ) && ( node.getNodeName().equals(CELLDESIGNER_SPECIE) ) )
            {
                String name = ( (Element)node ).getAttribute(ID_ATTR);
                String title = ( (Element)node ).getAttribute(NAME_ATTR);
                Element annotation = getElement((Element)node, CELLDESIGNER_ANNOTATION);
                if( annotation != null )
                {
                    Element complex = getElement(annotation, CELLDESIGNER_COMPLEX_SPECIES);
                    if( complex != null )
                    {
                        try
                        {
                            String complexId = getTextContent(complex);
                            Node parent = CellDesignerUtils.findNode(diagram, complexId, null);

                            Compartment child = new Compartment(null, new Specie(null, name, "macromolecule"));
                            child.setTitle(title);
                            
                            Element identity = getElement(annotation, CELLDESIGNER_SPECIE_IDENTITY);
                            if (identity != null)
                                CellDesignerUtils.readClass(identity, child);
                            
                            addComplexElement(parent, child);
                            Element speciesIdentity = getElement(annotation, CELLDESIGNER_SPECIE_IDENTITY);
                            if( speciesIdentity != null )
                                addProteinRef(speciesIdentity, name);
                        }
                        catch( Exception e )
                        {
                            log.log(Level.SEVERE, "Cannot add complex subelement", e);
                        }
                    }
                }
            }
        }
    }

    /**
     * Process 'celldesigner:listOfSpeciesAliases'
     */
    protected void readSpecieAliases(Element element, @Nonnull Diagram diagram)
    {
        Element listElement = getTopElement(element, CELLDESIGNER_SPECIE_ALIASES_LIST);
        if( listElement != null )
        {
            NodeList list = listElement.getChildNodes();
            for( int i = 0; i < list.getLength(); i++ )
            {
                org.w3c.dom.Node node = list.item(i);
                if( ( node instanceof Element ) && ( node.getNodeName().equals(CELLDESIGNER_SPECIE_ALIAS) ) )
                {
                    String speciesAttr = ( (Element)node ).getAttribute(SPECIES_ATTR);
                    String alias = ( (Element)node ).getAttribute(ID_ATTR);
                    String complexID = ( (Element)node ).getAttribute(COMPLEX_ALIAS_ATTR);
                    if( !complexID.isEmpty() )
                    {
                        if( complexes.containsKey(complexID) )
                        {
                            try
                            {
                                Node parent = complexes.get(complexID);
                                if( !CellDesignerUtils.containsComplexElement(parent, speciesAttr) )
                                {
                                    Compartment child = new Compartment(null, new Specie(null, speciesAttr, "macromolecule"));
                                    CellDesignerUtils.addProperty(child,
                                            new DynamicProperty(SBGNPropertyConstants.SBGN_ENTITY_TYPE_PD, String.class, "macromolecule"),
                                            true);
                                    addComplexElement(parent, child);
                                }
                            }
                            catch( Exception e )
                            {
                                log.log(Level.SEVERE, "Cannot add complex subelement", e);
                            }
                        }
                        readAlias((Element)node, diagram, speciesAttr, alias, complexID, null);
                    }
                    else
                    {
                        String compartmentID = ( (Element)node ).getAttribute(COMPARTMENT_ALIAS_ATTR);
                        if( compartmentID.isEmpty() )
                            compartmentID = null;
                        readAlias((Element)node, diagram, speciesAttr, alias, null, compartmentID);
                    }
                }
            }
        }
    }

    /**
     * Read protein reference: 'celldesigner:proteinReference'
     */
    protected void addProteinRef(Element speciesIdentity, Object node)
    {
        //read protein reference
        Element prElement = getElement(speciesIdentity, CELLDESIGNER_PROTEIN_REFERENCE);
        if( prElement != null )
        {
            String pr = getTextContent(prElement);
            List<Object> list = proteinRefs.get(pr);
            if( list == null )
            {
                list = new ArrayList<>();
                proteinRefs.put(pr, list);
            }
            list.add(node);
        }
    }

    /**
     * Add complex sub element info to {@link Node} attributes
     */
    protected void addComplexElement(Node parent, Node subElement) throws Exception
    {
        CellDesignerUtils.addElementToNodeAttributes(parent, subElement, SBGNConverter.COMPLEX_ATTR);
        Object nodeAliases = parent.getAttributes().getValue(SBGNConverter.ALIASES_ATTR);
        if( nodeAliases instanceof Node[] )
        {
            for( Node n : (Node[])nodeAliases )
            {
                Node nodeCopy = cloneNode(subElement, null, subElement.getName());
                CellDesignerUtils.addElementToNodeAttributes(n, nodeCopy, SBGNConverter.COMPLEX_ATTR);
            }
        }
        subElement.getAttributes().add(DPSUtils.createTransient(PARENT_NODE_ATTR, Node.class, parent));
    }

    /**
     * Read attributes info for diagram element
     * NOTE: one SBML element may be linked to several aliases
     */
    protected Node readAlias(Element element, @Nonnull Diagram diagram, String id, String alias, String complexAlias,
            String compartmentAlias)
    {
        try
        {
            Node node = CellDesignerUtils.findNode(diagram, id, null);
            if( node != null )
            {
                //some complexes support
                if( complexAlias != null && complexes.containsKey(complexAlias) )
                {
                    Node complex = complexes.get(complexAlias);
                    Object complexElements = complex.getAttributes().getValue(SBGNConverter.COMPLEX_ATTR);
                    if( complexElements instanceof Node[] )
                    {
                        for( Node n : (Node[])complexElements )
                        {
                            if( n.getName().equals(id) )
                            {
                                node = n;
                                break;
                            }
                        }
                    }
                }

                //compartment alias support
                node = getNodeInCompartment(node, id, compartmentAlias);

                //several aliases support
                Object nodeAlias = node.getAttributes().getValue(SBGNConverter.ALIAS_ATTR);
                if( nodeAlias != null )
                {
                    Node parent = node;
                    String name = parent.getName();
                    Object aliases = parent.getAttributes().getValue(SBGNConverter.ALIASES_ATTR);
                    if( aliases instanceof Node[] )
                        name += "_" + ( ( (Node[])aliases ).length + 2 );
                    else
                        name += "_2";
                    Role role = node.getRole();
                    node = cloneNode(node, (Compartment)parent.getOrigin(), name);
                    node.setRole(role);
                    CellDesignerUtils.addElementToNodeAttributes(parent, node, SBGNConverter.ALIASES_ATTR);
                }
                CellDesignerUtils.addProperty(node, new DynamicProperty(SBGNConverter.ALIAS_ATTR, String.class, alias), false);

                //read bounds info
                Rectangle nodeBounds = readBoundsInfo(element, node);

                //read paint info
                readPaintInfo(element, node);

                //read title location
                Element namePoint = getElement(element, CELLDESIGNER_NAME_POINT);
                if( namePoint != null )
                {
                    try
                    {
                        double x = Double.parseDouble(namePoint.getAttribute(X_ATTR));
                        double y = Double.parseDouble(namePoint.getAttribute(Y_ATTR));
                        Point location = new Point((int)x - node.getLocation().x, (int)y - node.getLocation().y);
                        CellDesignerUtils.addProperty(node,
                                new DynamicProperty(SBGNPropertyConstants.NAME_POINT_ATTR, Point.class, location), false);
                    }
                    catch( NumberFormatException xe )
                    {
                        log.log(Level.SEVERE, "Invalid name location format");
                    }
                }

                //read activity info
                Element activityElement = getElement(element, CELLDESIGNER_ACTIVITY);
                if( activityElement != null )
                {
                    String activityStr = getTextContent(activityElement);
                    if( activityStr.equals("active") )
                    {
                        Node variable = new Node(null, new Stub(null, node.getName() + "_active", Type.TYPE_VARIABLE));
                        variable.setTitle("Active");
                        String angle = String.valueOf(Math.PI * 3.0 / 2.0);
                        variable.getAttributes().add(new DynamicProperty(SBGNConverter.ANGLE_ATTR, String.class, angle));
                        variable.getAttributes().add(new DynamicProperty("value", String.class, "Active"));
                        CellDesignerUtils.addElementToNodeAttributes(node, variable, SBGNConverter.MODIFICATION_ATTR);
                    }
                    else if( activityStr.equals("inactive") )
                    {
                        node.getAttributes().remove(SBGNConverter.MODIFICATION_ATTR);
                    }
                }

                //list of inner aliases support
                Element e = getElement(element, CELLDESIGNER_INNER_ALIAS_LIST);
                if( e != null )
                {
                    Rectangle complexBounds = null;
                    NodeList list = e.getChildNodes();
                    for( int i = 0; i < list.getLength(); i++ )
                    {
                        org.w3c.dom.Node n = list.item(i);
                        if( ( n instanceof Element ) && ( n.getNodeName().equals(CELLDESIGNER_INNER_ALIAS) ) )
                        {
                            Element innerNodeElement = (Element)n;
                            String childName = innerNodeElement.getAttribute(HETERODIMER_ATTR);
                            Compartment child = new Compartment(null, new Specie(null, childName, "entity"));
                            CellDesignerUtils.addProperty(child,
                                    new DynamicProperty(SBGNPropertyConstants.SBGN_ENTITY_TYPE_PD, String.class, "macromolecule"), true);
                            addComplexElement(node, child);
                            Rectangle rect = readBoundsInfo(innerNodeElement, child);
                            if( rect != null )
                            {
                                if( complexBounds == null )
                                    complexBounds = rect;
                                else
                                    complexBounds.add(rect);
                            }
                            readPaintInfo(innerNodeElement, child);
                        }
                    }
                    if( complexBounds != null && nodeBounds == null )
                    {
                        node.setLocation(complexBounds.x - 5, complexBounds.y - 5);
                        node.setShapeSize(new Dimension(complexBounds.width + 10, complexBounds.height + 10));
                        if( node instanceof Compartment )
                            node.getAttributes().add(new DynamicProperty(FIXED_SIZE_ATTR, Boolean.class, Boolean.TRUE));
                    }
                }
            }
            return node;
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Cannot find node: " + id, e);
        }
        return null;
    }
    /**
     * Read bound info from XML element
     */
    protected Rectangle readBoundsInfo(Element element, Node node) throws Exception
    {
        Element e = getElement(element, CELLDESIGNER_BOUNDS);
        if( e != null )
        {
            try
            {
                double x = Double.parseDouble(e.getAttribute(X_ATTR));
                double y = Double.parseDouble(e.getAttribute(Y_ATTR));
                double w = Double.parseDouble(e.getAttribute(W_ATTR));
                double h = Double.parseDouble(e.getAttribute(H_ATTR));
                if( node != null )
                {
                    addHiddenDoubleProperty(node, X_ATTR, x);
                    addHiddenDoubleProperty(node, Y_ATTR, y);
                    addHiddenDoubleProperty(node, W_ATTR, w);
                    addHiddenDoubleProperty(node, H_ATTR, h);
                    node.setLocation((int)x, (int)y);
                    node.setShapeSize(new Dimension((int)w, (int)h));

                    if( node instanceof Compartment )
                        node.getAttributes().add(new DynamicProperty(FIXED_SIZE_ATTR, Boolean.class, Boolean.TRUE));
                }
                return new Rectangle((int)x, (int)y, (int)w, (int)h);
            }
            catch( NumberFormatException xe )
            {
                log.log(Level.SEVERE, "Invalid bounds format");
            }
        }
        return null;
    }

    /**
     * Read paint info from XML element
     */
    protected void readPaintInfo(Element element, Node node) throws Exception
    {
        Element e = getElement(element, CELLDESIGNER_USUAL_VIEW);
        e = ( e == null ) ? getElement(element, CELLDESIGNER_PAINT) : getElement(e, CELLDESIGNER_PAINT);

        if( e != null )
        {
            String colorStr = e.getAttribute(COLOR_ATTR);
            String scheme = e.getAttribute(SCHEME_ATTR);
            try
            {
                colorStr = colorStr.toLowerCase();
                if( colorStr.length() >= 6 )
                {
                    int l = colorStr.length();
                    colorStr = colorStr.substring(l - 6, l);
                    Paint color = new Color(Integer.parseInt(colorStr, 16));

                    Element doubleLine = getElement(element, CELLDESIGNER_DOUBLE_LINE);
                    if( doubleLine != null )
                    {
                        Color inColor, outColor;
                        int r = ( (Color)color ).getRed();
                        int g = ( (Color)color ).getGreen();
                        int b = ( (Color)color ).getBlue();
                        if( r == 255 && g == 255 && b == 255 )
                        {
                            //use black in/out lines
                            inColor = Color.black;
                            outColor = Color.black;
                        }
                        else
                        {
                            inColor = (Color)color;
                            outColor = (Color)color;
                            color = new Color(r, g, b, 200);
                        }
                        int width = (int)Double.parseDouble(doubleLine.getAttribute(THICKNESS_ATTR));
                        Pen pen = new Pen(width, (Color)color);
                        node.setPredefinedStyle(DiagramElementStyle.STYLE_NOT_SELECTED);
                        node.getCustomStyle().setPen(pen.clone());

                        try
                        {
                            int widthOut = (int)Double.parseDouble(doubleLine.getAttribute(OUTER_WIDTH_ATTR));
                            CellDesignerUtils.addProperty(node,
                                    new DynamicProperty(SBGNPropertyConstants.LINE_OUT_PEN_ATTR, Pen.class, new Pen(widthOut, outColor)),
                                    false);
                            int widthIn = (int)Double.parseDouble(doubleLine.getAttribute(INNER_WIDTH_ATTR));
                            CellDesignerUtils.addProperty(node,
                                    new DynamicProperty(SBGNPropertyConstants.LINE_IN_PEN_ATTR, Pen.class, new Pen(widthIn, inColor)),
                                    false);
                        }
                        catch( Exception ex )
                        {
                        }
                    }
                    else
                    {
                        if( ( (Color)color ).getRed() == 255 && ( (Color)color ).getGreen() == 255 && ( (Color)color ).getBlue() == 255 )
                            color = new Color(254, 254, 254); //HACK: color (255,255,255) is the same as NULL in SBGN notation

                        if( scheme != null && scheme.toLowerCase().equals("gradation") )
                            color = new GradientPaint(0, 0, Color.white, 100, 100, (Color)color);

                        node.setPredefinedStyle(DiagramElementStyle.STYLE_NOT_SELECTED);
                        node.getCustomStyle().setBrush(new Brush(color));
                    }
                }
            }
            catch( Exception xe )
            {
                log.log(Level.SEVERE, "Invalid color format");
            }
        }
    }

    /**
     * Process specific compartment properties
     */
    protected void readCompartmentType(Element element, @Nonnull Diagram diagram, String id)
    {
        try
        {
            Node node = CellDesignerUtils.findNode(diagram, id, null);
            if( node instanceof Compartment )
            {
                Element classElement = getElement(element, CELLDESIGNER_CLASS);
                if( classElement != null )
                {
                    String type = getTextContent(classElement);
                    if( type.indexOf("_CLOSEUP_") != -1 )
                    {
                        String bioumlType = type.substring(type.lastIndexOf('_') + 1);
                        CellDesignerUtils.addProperty(node,
                                new DynamicProperty(SBGNPropertyConstants.CLOSEUP_ATTR, String.class, bioumlType), false);
                        Element point = getElement(element, CELLDESIGNER_POINT);
                        if( point != null )
                        {
                            double x = Double.parseDouble(point.getAttribute(X_ATTR));
                            double y = Double.parseDouble(point.getAttribute(Y_ATTR));
                            node.setLocation((int)x, (int)y);
                        }
                    }
                    else if( type.equals("OVAL") )
                    {
                        ( (Compartment)node ).setShapeType(Compartment.SHAPE_ELLIPSE);
                    }
                }
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Cannot find compartment: " + id, e);
        }
    }

    //
    // Utility functions
    //

    /**
     * Clone node with some additional actions
     */
    protected Node cloneNode(Node node, Compartment newParent, String newName) throws Exception
    {

        Base newKernel = node.getKernel();
        if( node.getKernel() instanceof Specie )
        {
            Specie oldKernel = (Specie)node.getKernel();
            newKernel = new Specie(oldKernel.getOrigin(), newName, oldKernel.getType());
        }
        Node result = node.clone(newParent, newName, newKernel);

        //aliases should be removed from clone
        result.getAttributes().remove(SBGNConverter.ALIASES_ATTR);
        //correct clone COMPLEX_ATTR attributes
        Object complexElements = node.getAttributes().getValue(SBGNConverter.COMPLEX_ATTR);
        if( complexElements instanceof Node[] )
        {
            int size = ( (Node[])complexElements ).length;
            Node[] newComplexElements = new Node[size];
            for( int i = 0; i < size; i++ )
            {
                Node e = ( (Node[])complexElements )[i];
                newComplexElements[i] = e.clone(null, e.getName());
                node.getAttributes().getProperty(SBGNConverter.COMPLEX_ATTR).setValue(newComplexElements);
            }
        }
        return result;
    }

    /**
     * Set double value as attribute with hidden flag
     */
    protected void addHiddenDoubleProperty(DiagramElement de, String name, double value)
    {
        try
        {
            DynamicProperty dp = new DynamicProperty(name, Double.class, value);
            dp.setHidden(true);
            de.getAttributes().add(dp);
        }
        catch( Exception e )
        {
        }
    }

    /**
     * Get node in selected compartment
     */
    protected Node getNodeInCompartment(Node node, String nodeId, String compartmentAlias) throws Exception
    {
        if( compartmentAlias != null )
        {
            Compartment comp = (Compartment)node.getOrigin();
            Object aliases = comp.getAttributes().getValue(SBGNConverter.ALIASES_ATTR);
            if( aliases instanceof Node[] )
            {
                for( Node compAlias : (Node[])aliases )
                {
                    Object compAliasName = compAlias.getAttributes().getValue(SBGNConverter.ALIAS_ATTR);
                    if( compAliasName != null && compAliasName.equals(compartmentAlias) )
                        return (Node) ( (Compartment)compAlias ).get(nodeId);
                }
            }
        }
        return node;
    }

    public static class DoublePoint
    {
        public double x;
        public double y;
        public DoublePoint(double x, double y)
        {
            this.x = x;
            this.y = y;
        }
        @Override
        public String toString()
        {
            return x + " : " + y;
        }
    }

    /**
     * Set of XML elements. Is useful with different versions of Celldesigner format
     */
    public static class ElementSet
    {
        protected Map<String, Element> elements = new HashMap<>();
        public void addElement(Element element)
        {
            if( element.getNodeName().equals(CELLDESIGNER_EXTENSION) )
            {
                //if there is common 'extension' tag (celldesigner 4.1)
                for( Element node : XmlUtil.elements(element) )
                    elements.put(node.getNodeName(), node);
            }
            else
            {
                //no common tags (old celldesigner versions)
                elements.put(element.getNodeName(), element);
            }
        }
        public Element getElement(String tagName)
        {
            return elements.get(tagName);
        }
    }

    /**
     * Access to species edges by reaction and species name
     */
    public static class ReactionSpeciesCache
    {
        protected Map<String, Map<String, List<Edge>>> edgesMap = new HashMap<>();
        public void addEdge(Edge edge)
        {
            String reaction = null;
            String specie = null;
            if( edge.getInput().getKernel() instanceof Reaction )
            {
                reaction = edge.getInput().getName();
                specie = edge.getOutput().getName();
            }
            else if( edge.getOutput().getKernel() instanceof Reaction )
            {
                reaction = edge.getOutput().getName();
                specie = edge.getInput().getName();
            }
            if( reaction != null && specie != null )
            {
                Map<String, List<Edge>> specieMap = edgesMap.get(reaction);
                if( specieMap == null )
                {
                    specieMap = new HashMap<>();
                    edgesMap.put(reaction, specieMap);
                }
                List<Edge> edges = specieMap.get(specie);
                if( edges == null )
                {
                    edges = new ArrayList<>();
                    specieMap.put(specie, edges);
                }
                edges.add(edge);
            }
        }

        public Edge getEdge(String reaction, String specie, String role)
        {
            Map<String, List<Edge>> specieMap = edgesMap.get(reaction);
            if( specieMap == null )
                return null;

            List<Edge> edges = specieMap.get(specie);
            if( edges != null )
            {
                for( Edge edge : edges )
                {
                    Base base = edge.getKernel();
                    if( ( base instanceof SpecieReference ) && ( ( (SpecieReference)base ) ).getRole().equals(role) )
                        return edge;
                }
            }
            return null;
        }
    }

    /**
     * CellDesigner extensions may contains celldesigner:extension element or not.
     * This method allows to combine this two formats.
     */
    public Element getTopElement(Element element, String name)
    {
        if( element.getNodeName().equals(name) )
            return element;
        return getElement(element, name);
    }
}
