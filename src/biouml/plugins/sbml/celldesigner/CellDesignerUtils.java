package biouml.plugins.sbml.celldesigner;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.w3c.dom.Element;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.xml.XmlDiagramTypeConstants;
import biouml.plugins.sbgn.SBGNPropertyConstants;
import biouml.plugins.sbml.celldesigner.CellDesignerExtension.DoublePoint;
import biouml.plugins.sbml.converters.SBGNConverter;
import biouml.plugins.sbml.extensions.SbmlExtensionSupport;
import biouml.standard.type.Reaction;
import biouml.standard.type.Specie;
import biouml.standard.type.SpecieReference;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.graph.Path;
import ru.biosoft.util.DPSUtils;
import ru.biosoft.util.TextUtil2;

public class CellDesignerUtils implements CellDesignerConstants
{
    protected static final Logger log = Logger.getLogger(CellDesignerUtils.class.getName());

    /**
     * Get point for orthogonal vector. Is used for calculation second base vector.
     */
    public static DoublePoint getOrthogonalPoint(DoublePoint p1, DoublePoint p2)
    {
        return new DoublePoint(p1.x - p2.y + p1.y, p1.y + p2.x - p1.x);
    }

    /**
     * Get point by base vectors (p1,p2) and (p1,p3).
     */
    public static DoublePoint getPointByBase(String pair, DoublePoint p1, DoublePoint p2, DoublePoint p3)
    {
        String[] vals = TextUtil2.split(pair, ',');
        if( vals.length == 2 )
        {
            try
            {
                double a = Double.parseDouble(vals[0]);
                double b = Double.parseDouble(vals[1]);
                return new DoublePoint( ( a * p2.x + b * p3.x + ( 1 - a - b ) * p1.x ), ( a * p2.y + b * p3.y + ( 1 - a - b ) * p1.y ));
            }
            catch( NumberFormatException e )
            {
                log.log(Level.SEVERE, "Cannot parse double value", e);
            }
        }
        return new DoublePoint(0.0, 0.0);
    }

    public static Point getProjection(DoublePoint p, DoublePoint p1, DoublePoint p2)
    {
        double fDenominator = ( p2.x - p1.x ) * ( p2.x - p1.x ) + ( p2.y - p1.y ) * ( p2.y - p1.y );
        if( fDenominator == 0 ) // p1 and p2 are the same
            return new Point((int)p1.x, (int)p1.y);
        double t = ( p.x * ( p2.x - p1.x ) - ( p2.x - p1.x ) * p1.x + p.y * ( p2.y - p1.y ) - ( p2.y - p1.y ) * p1.y ) / fDenominator;
        if( t < 0.0 || t > 1.0 )
            return new Point((int)p1.x, (int)p1.y);
        return new Point((int) ( p1.x + ( p2.x - p1.x ) * t ), (int) ( p1.y + ( p2.y - p1.y ) * t ));
    }

    /**
     * Get bounds for node
     */
    public static Rectangle getBounds(Node node)
    {
        Point l = node.getLocation();
        Dimension d = node.getShapeSize();
        return d == null ? new Rectangle(l.x, l.y, 5, 5) : new Rectangle(l.x, l.y, d.width, d.height);
    }

    /**
     * Check if node has the same name or contains sub element (for complexes) with name.
     */
    public static boolean isNodeInComplex(Node base, String name)
    {
        if( base.getName().equals(name) )
            return true;

        Object complexes = base.getAttributes().getValue(SBGNConverter.COMPLEX_ATTR);
        return complexes != null && StreamEx.of((Node[])complexes).anyMatch(n -> name.equals(n.getName()));
    }


    /**
     * Find edge between two nodes
     */
    public static Edge findEdge(Node reaction, Node species, String role)
    {
        for( Edge e : reaction.getEdges() )
        {
            if( ( e.getKernel() instanceof SpecieReference ) && ( ( (SpecieReference)e.getKernel() ).getRole().equals(role) ) )
            {
                if( ( e.getInput() == reaction && CellDesignerUtils.isNodeInComplex(e.getOutput(), species.getName()) )
                        || ( e.getOutput() == reaction && CellDesignerUtils.isNodeInComplex(e.getInput(), species.getName()) ) )
                {
                    return e;
                }
            }
        }

        //create Edge if not exists
        Edge result = null;
        try
        {
            Reaction reactionKernel = (Reaction)reaction.getKernel();
            SpecieReference ref = new SpecieReference(reactionKernel, reactionKernel.getName(), species.getName(), role);
            String fullName = getCompleteNameInDiagram(species);
            ref.setSpecie(fullName);
            ref.setTitle(reaction.getTitle());
            result = role.equals(SpecieReference.PRODUCT) ? new Edge(ref, reaction, species) : new Edge(ref, species, reaction);
            reactionKernel.put(ref);
            result.save();
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not create edge", e);
        }
        return result;
    }

    public static String getCompleteNameInDiagram(DiagramElement de)
    {
        String result = de.getName();
        if( ! ( de instanceof Diagram ) )
        {
            DataCollection dc = de.getOrigin();
            Diagram diagram = Diagram.optDiagram(de);
            while( ( dc != null ) && dc != diagram )
            {
                result = dc.getName() + "." + result;
                dc = dc.getOrigin();
            }
        }

        return result;
    }

    /**
     * Extension for getCompleteNameIndiagram() method for complex subelements support
     */
    public static String getCompleteName(Node node)
    {
        if( node.getOrigin() == null )
        {
            Object parent = node.getAttributes().getValue(PARENT_NODE_ATTR);
            if( parent instanceof Node )
                return getCompleteNameInDiagram( ( (Node)parent )) + "." + node.getName();
        }
        return getCompleteNameInDiagram(node);
    }

    /**
     * Get node port
     */
    public static DoublePoint getNodePort(Node node, float portX, float portY)
    {
        Object xObj = node.getAttributes().getValue(X_ATTR);
        Object yObj = node.getAttributes().getValue(Y_ATTR);
        Point location = node.getLocation();
        DoublePoint result = new DoublePoint(location.x, location.y);
        double x = ( xObj == null || ! ( xObj instanceof Double ) ) ? result.x : (Double)xObj;
        double y = ( yObj == null || ! ( yObj instanceof Double ) ) ? result.y : (Double)yObj;
        Dimension d = node.getShapeSize();
        if( d != null )
        {
            Object wObj = node.getAttributes().getValue(W_ATTR);
            Object hObj = node.getAttributes().getValue(H_ATTR);
            double w = ( wObj == null || ! ( wObj instanceof Double ) ) ? d.width : (Double)wObj;
            double h = ( hObj == null || ! ( hObj instanceof Double ) ) ? d.height : (Double)hObj;
            result.x = ( x + ( w / 2.0 ) + ( portX * w ) );
            result.y = ( y + ( h / 2.0 ) + ( portY * h ) );
        }
        return result;
    }

    /**
     * Get center point of node
     */
    public static DoublePoint getNodeCenter(Node node)
    {
        Object xObj = node.getAttributes().getValue(X_ATTR);
        Object yObj = node.getAttributes().getValue(Y_ATTR);
        Point location = node.getLocation();
        DoublePoint result = new DoublePoint(location.x, location.y);
        double x = ( xObj == null || ! ( xObj instanceof Double ) ) ? result.x : (Double)xObj;
        double y = ( yObj == null || ! ( yObj instanceof Double ) ) ? result.y : (Double)yObj;
        Dimension d = node.getShapeSize();
        if( d != null )
        {
            Object wObj = node.getAttributes().getValue(W_ATTR);
            Object hObj = node.getAttributes().getValue(H_ATTR);
            double w = ( wObj == null || ! ( wObj instanceof Double ) ) ? d.width : (Double)wObj;
            double h = ( hObj == null || ! ( hObj instanceof Double ) ) ? d.height : (Double)hObj;

            result.x = x + w / 2.0;
            result.y = y + h / 2.0;
        }
        else
        {
            //it should be reaction
            result.x += REACTION_DELTA;
            result.y += REACTION_DELTA;
        }
        return result;
    }

    /**
     * Curve edge from reaction to specieNode using baseSpecie
     */
    public static void curveLine(@Nonnull Diagram diagram, Edge edge, SpeciesInfo baseSpecie, Node specieNode, Node reaction)
            throws Exception
    {
        Node baseNode = findNode(diagram, baseSpecie.getName(), baseSpecie.getAlias());
        if( baseNode != null )
        {
            DoublePoint inport = getNodeCenter(edge.getInput());
            DoublePoint outport = getNodeCenter(edge.getOutput());

            DoublePoint p1 = getNodeCenter(reaction);
            DoublePoint p2 = getNodeCenter(baseNode);

            DoublePoint p = getNodeCenter(specieNode);
            Point controlPoint = getProjection(p, p1, p2);

            Path path = new Path();
            path.addPoint((int)inport.x, (int)inport.y);
            path.addPoint(controlPoint.x, controlPoint.y, Path.QUAD_TYPE);
            path.addPoint((int)outport.x, (int)outport.y);
            edge.setPath(path);
        }
    }

    /**
     * Find node in diagram or inner complex element
     */
    public static Node findNode(@Nonnull Diagram diagram, String name, String alias)
    {
        Node node = diagram.findNode(name);
        if( node == null )
            node = findInComplex(diagram, name, alias); //try to find in complexes
        //TODO: find in compartment aliases
        return node;
    }

    public static Node findInComplex(Node node, String name, String alias)
    {
        Object complexes = node.getAttributes().getValue(SBGNConverter.COMPLEX_ATTR);
        if( complexes instanceof Node[] )
        {
            for( Node n : (Node[])complexes )
            {
                if( n.getName().equals(name) )
                {
                    if( alias != null )
                    {
                        Object nAlias = n.getAttributes().getValue(SBGNConverter.ALIAS_ATTR);
                        if( nAlias != null && nAlias.toString().equals(alias) )
                            return n;
                    }
                    else
                    {
                        return n;
                    }
                }
                Node inner = findInComplex(n, name, alias);
                if( inner != null )
                    return inner;
            }
        }
        if( node instanceof Compartment )
        {
            for( DiagramElement child : (Compartment)node )
            {
                if( child instanceof Node )
                {
                    Node result = findInComplex((Node)child, name, alias);
                    if( result != null )
                        return result;
                }
            }
        }
        return null;
    }


    /**
     * Check if complex already contains this element
     */
    public static boolean containsComplexElement(Node parent, String subElement) throws Exception
    {
        DynamicProperty complexes = parent.getAttributes().getProperty(SBGNConverter.COMPLEX_ATTR);
        return complexes != null && StreamEx.of((Node[])complexes.getValue()).anyMatch(n -> subElement.equals(n.getName()));
    }

    /**
     * Set property to diagram element
     */
    public static void addProperty(DiagramElement de, DynamicProperty property, boolean processAliases)
    {
        try
        {
            de.getAttributes().remove(property.getName());
            de.getAttributes().add(property);
            if( processAliases && ( de.getAttributes().getValue(SBGNConverter.ALIASES_ATTR) instanceof Node[] ) )
            {
                for( Node n : (Node[])de.getAttributes().getValue(SBGNConverter.ALIASES_ATTR) )
                    n.getAttributes().add(property);
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Cannot set attribute: " + property.getName(), e);
        }
    }

    /**
     * Add node element to other node as attribute
     */
    public static void addElementToNodeAttributes(Node parent, Node subElement, String propertyName) throws Exception
    {
        DynamicProperty props = parent.getAttributes().getProperty(propertyName);
        if( props == null )
        {
            props = DPSUtils.createTransient(propertyName, Node[].class, new Node[] {subElement});
            parent.getAttributes().add(props);
        }
        else
        {
            Node[] oldNodes = (Node[])props.getValue();
            boolean addNew = true;
            for( int i = 0; i < oldNodes.length; i++ )
            {
                if( oldNodes[i].getName().equals(subElement.getName()) )
                {
                    //replace exist element
                    oldNodes[i] = subElement;
                    addNew = false;
                    break;
                }
            }
            if( addNew )
            {
                //add new element to the end
                Node[] newNodes = new Node[oldNodes.length + 1];
                System.arraycopy(oldNodes, 0, newNodes, 0, oldNodes.length);
                newNodes[oldNodes.length] = subElement;
                props.setValue(newNodes);
            }
        }
    }

    public static Set<Node> updateAliases(Node node)
    {
        Set<Node> result = new HashSet<>();
        Diagram diagram = Diagram.optDiagram(node);
        if( diagram != null )
        {
            Set<Node> nodes = getAllNodes(diagram, node.getName());
            result.addAll(nodes);
            for( Node otherNode : nodes )
                result.addAll(getAliases(otherNode).toList());
        }

        result.addAll(getAliases(node).toList());
        result.add(node);
        return result;

    }

    public static Set<Node> getAllNodes(Node node, String name)
    {
        Set<Node> result = new HashSet<>();
        if( node.getName().equals(name) )
            result.add(node);

        if( node instanceof Compartment )
            for( Node innerNode : ( (Compartment)node ).getNodes() )
                result.addAll(getAllNodes(innerNode, name));

        for( Node aliasNode : getAliases(node) )
            result.addAll(getAllNodes(aliasNode, name));
        return result;
    }

    public static StreamEx<Node> getAliases(Node node)
    {
        Object val = node.getAttributes().getValue(SBGNConverter.ALIASES_ATTR);
        if( val instanceof Node[] )
            return StreamEx.of((Node[])val);
        return StreamEx.empty();
    }

    /**
     * Read class element. Translate from Cell Designer to SBGN specie type
     */
    protected static void readClass(Element element, DiagramElement de)
    {
        Element classElement = SbmlExtensionSupport.getElement(element, CELLDESIGNER_CLASS);
        String result = "undefiend";
        if( classElement != null )
        {
            String type = SbmlExtensionSupport.getTextContent(classElement);
            try
            {
                if( type.equals("PROTEIN") )
                {
                    result = "macromolecule";
                }
                else if( type.equals("RNA") || type.equals("GENE") )
                {
                    result = "nucleic acid feature";
                }
                else if( type.equals("SIMPLE_MOLECULE") || type.equals("ION") )
                {
                    result = "simple chemical";
                }
                else if( type.equals("PHENOTYPE") )
                {
                    // phenotypes are used incorrectly in CellDesigner diagrams (as species, not as processes). TODO: rework
                    CellDesignerUtils.addProperty(de, new DynamicProperty(XmlDiagramTypeConstants.XML_TYPE, String.class, "phenotype"),
                            true);
                    return;
                }
                else if( type.equals("COMPLEX") )
                {
                    result = "complex";
                }
                else if( type.equals("DEGRADED") )
                {
                    CellDesignerUtils.addProperty(de, new DynamicProperty(XmlDiagramTypeConstants.XML_TYPE, String.class, "undefiend"),
                            true); //here we actually need to remove this element
                    return;
                }
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE, "Cannot create type attribute", e);
            }
        }

        if( de instanceof Node )
        {
            for( Node node : CellDesignerUtils.updateAliases((Node)de) )
            {
                if( node.getKernel() instanceof Specie )
                    ( (Specie)node.getKernel() ).setType(result);
                node.getAttributes().add(new DynamicProperty(SBGNPropertyConstants.SBGN_ENTITY_TYPE_PD, String.class, result));
            }
        }
    }
}
