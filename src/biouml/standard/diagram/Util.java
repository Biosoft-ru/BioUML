package biouml.standard.diagram;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramContainer;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.ModelDefinition;
import biouml.model.Node;
import biouml.model.SubDiagram;
import biouml.model.SubDiagram.PortOrientation;
import biouml.model.dynamics.Connection;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.standard.type.Base;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import biouml.standard.type.Stub.ConnectionPort;
import biouml.standard.type.Stub.DirectedConnection;
import biouml.standard.type.Stub.Note;
import biouml.standard.type.Stub.UndirectedConnection;
import biouml.standard.type.Type;
import one.util.streamex.StreamEx;
import ru.biosoft.util.DPSUtils;

public class Util
{
    public static final String INITIAL_VALUE = "Initial value";
    public static final String CONDITION = "Condition";

    public static final String INITIAL_TIME = "Inital time";
    public static final String COMPLETION_TIME = "Completion time";
    public static final String TIME_INCREMENT = "Time increment";
    public static final String TIME_SCALE = "Time scale";

    public static final String ORIGINAL_PATH = "originalPath";

    public static final String EXTENT_FACTOR = "Extent factor";

    public static final String REACTION_LABEL = "Reaction label";

    public static final String COMPLEX_STRUCTURE = "Structure";
    public static final String HIGHLIGHT_PROPERTY = "highlight";

    //utility methods
    public static boolean isBus(DiagramElement de)
    {
        return de.getKernel() != null && de.getKernel().getType().equals(Type.TYPE_CONNECTION_BUS);
    }
    public static boolean isContactPort(DiagramElement de)
    {
        return de.getKernel() != null && de.getKernel().getType().equals(Type.TYPE_CONTACT_CONNECTION_PORT);
    }
    public static boolean isInputPort(DiagramElement de)
    {
        return de.getKernel() != null && de.getKernel().getType().equals(Type.TYPE_INPUT_CONNECTION_PORT);
    }
    public static boolean isOutputPort(DiagramElement de)
    {
        return de.getKernel() != null && de.getKernel().getType().equals(Type.TYPE_OUTPUT_CONNECTION_PORT);
    }
    public static boolean isPort(DiagramElement de)
    {
        return de.getKernel() instanceof ConnectionPort || ( de.getKernel() instanceof Stub && de.getKernel().getType().equals("port") );
    }
    /**True if de is a port node on the top level (not inside somemodule)*/
    public static boolean isTopLevelPort(DiagramElement de)
    {
        return isPort(de) && ( de.getParent() instanceof Diagram || ( de.getParent() instanceof Compartment
                && ( (Compartment)de.getParent() ).getKernel() instanceof biouml.standard.type.Compartment ) );
    }
    public static boolean isPropagatedPort(DiagramElement de)
    {
        if( Util.isModulePort(de) )
            return false;
        return Util.isPort(de) && de.getAttributes().getProperty(ConnectionPort.ACCESS_TYPE) != null
                && de.getAttributes().getProperty(ConnectionPort.ACCESS_TYPE).getValue().equals(ConnectionPort.PROPAGATED);
    }

    public static boolean isPropagatedPort2(DiagramElement de)
    {
        return Util.isPort(de) && de.getAttributes().getProperty(ConnectionPort.ACCESS_TYPE) != null
                && de.getAttributes().getProperty(ConnectionPort.ACCESS_TYPE).getValue().equals(ConnectionPort.PROPAGATED);
    }

    public static void setPropagated(DiagramElement de, Node basePort) throws IllegalArgumentException
    {
        if( !isPort(de) )
            throw new IllegalArgumentException("Only ports can be propagated");
        de.getAttributes().add(new DynamicProperty(ConnectionPort.ACCESS_TYPE, String.class, ConnectionPort.PROPAGATED));
        de.getAttributes().add(new DynamicProperty(ConnectionPort.BASE_PORT_NAME_ATTR, String.class, basePort.getName()));
        de.getAttributes()
                .add(new DynamicProperty(ConnectionPort.BASE_MODULE_NAME_ATTR, String.class, basePort.getCompartment().getName()));
    }
    public static void setPublic(Node port)
    {
        port.getAttributes().getProperty(ConnectionPort.ACCESS_TYPE).setValue(ConnectionPort.PUBLIC);
    }
    public static boolean isPublicPort(DiagramElement de)
    {
        return Util.isPort(de) && ( de.getAttributes().getProperty(ConnectionPort.ACCESS_TYPE) == null
                || de.getAttributes().getProperty(ConnectionPort.ACCESS_TYPE).getValue().equals(ConnectionPort.PUBLIC) );
    }
    public static boolean isPrivatePort(DiagramElement de)
    {
        return Util.isPort(de) && de.getAttributes().getProperty(ConnectionPort.ACCESS_TYPE) != null
                && de.getAttributes().getProperty(ConnectionPort.ACCESS_TYPE).getValue().equals(ConnectionPort.PRIVATE);
    }
    public static boolean isSwitch(DiagramElement de)
    {
        return de.getKernel() != null && de.getKernel().getType().equals(Type.TYPE_SWITCH);
    }
    public static boolean isAverager(DiagramElement de)
    {
        return de.getKernel() != null && de.getKernel().getType().equals(Type.TYPE_AVERAGER);
    }
    public static boolean isPlot(DiagramElement de)
    {
        return de.getKernel() != null && de.getKernel().getType().equals(Type.TYPE_PLOT);
    }
    public static boolean isReaction(DiagramElement de)
    {
        return de.getKernel() instanceof Reaction;
    }
    public static boolean isFastReaction(DiagramElement de)
    {
        return de.getKernel() instanceof Reaction && ( (Reaction)de.getKernel() ).isFast();
    }
    public static boolean hasFastReactions(Diagram d)
    {
        return d.recursiveStream().anyMatch(de -> isFastReaction(de));
    }
    public static boolean isSpecieReference(DiagramElement de)
    {
        return de.getKernel() instanceof SpecieReference;
    }
    public static boolean isSourceSink(DiagramElement de)
    {
        return de.getKernel() != null && de.getKernel().getType().equals("source-sink");
    }
    public static boolean isModule(DiagramElement de)
    {
        return de instanceof DiagramContainer || isSwitch(de) || isAverager(de);
    }
    public static boolean isModulePort(DiagramElement de)
    {
        return Util.isPort(de) && isModule(de.getCompartment());
    }
    public static boolean isConstant(DiagramElement de)
    {
        return de.getKernel() != null && de.getKernel().getType().equals(Type.TYPE_CONSTANT);
    }
    public static boolean isBlock(DiagramElement de)
    {
        return de.getKernel() != null && de.getKernel().getType().equals(Type.TYPE_BLOCK);
    }
    public static boolean isVariable(DiagramElement de)
    {
        return de.getRole() instanceof VariableRole;
    }
    public static boolean isDiagram(DiagramElement de)
    {
        return de instanceof Diagram;
    }
    public static boolean isSubDiagram(DiagramElement de)
    {
        return de instanceof SubDiagram;
    }
    public static boolean isConnection(DiagramElement de)
    {
        return de.getKernel() instanceof DirectedConnection || de.getKernel() instanceof UndirectedConnection;
    }
    public static boolean isDirectedConnection(Edge edge)
    {
        return edge.getKernel() instanceof DirectedConnection;
    }
    public static boolean isUndirectedConnection(Edge edge)
    {
        return edge.getKernel() instanceof UndirectedConnection;
    }
    public static boolean isPortLink(Edge edge)
    {
        return edge.getKernel() != null && edge.getKernel().getType().equals("portlink");
    }

    public static List<Edge> getEdges(Node node)
    {
        if( node instanceof Compartment )
        {
            Compartment c = (Compartment)node;
            return c.recursiveStream().select(Node.class).flatMap(Node::edges)
                    .filter(e -> !contains(c, e.getInput()) || !contains(c, e.getOutput())).toList();
        }
        return node.edges().toList();
    }

    public static boolean contains(Compartment compartment, Node node)
    {
        // Faster compared to compartment.recursiveStream().has(node)
        return compartment.recursiveStream().select(Compartment.class).anyMatch(c -> node.equals(c.get(node.getName())));
    }

    public static String getPortVariable(DiagramElement de)
    {
        DynamicProperty dp = de.getAttributes().getProperty(ConnectionPort.VARIABLE_NAME_ATTR);
        if( dp != null )
            return dp.getValue().toString();
        return null;
    }

    public static String getPortModule(DiagramElement de)
    {
        DynamicProperty dp = de.getAttributes().getProperty(ConnectionPort.BASE_MODULE_NAME_ATTR);
        if( dp != null )
            return dp.getValue().toString();
        return null;
    }

    public static Node getBasePort(Node node) throws Exception
    {
        DynamicProperty dp = node.getAttributes().getProperty(ConnectionPort.BASE_MODULE_NAME_ATTR);
        if( dp == null )
            throw new Exception("Incorrect propagated port " + node.getName() + " attribute baseModuleName is missing");

        String moduleName = dp.getValue().toString();
        Node module = Diagram.getDiagram(node).findNode(moduleName);
        if( module == null || ! ( module instanceof Compartment ) )
        {
            moduleName = moduleName.replace(" ", "_"); //maybe names of modules themselves were normalized elsewhere
            module = Diagram.getDiagram(node).findNode(moduleName);
        }
        if( module == null || ! ( module instanceof Compartment ) )
            throw new Exception("Incorrect propagated port " + node.getName() + " can not find base module " + moduleName);

        dp = node.getAttributes().getProperty(ConnectionPort.BASE_PORT_NAME_ATTR);
        if( dp == null )
            throw new Exception("Incorrect propagated port " + node.getName() + " attribute basePortName is missing");

        Node basePort = ( (Compartment)module ).findNode(dp.getValue().toString());
        if( basePort == null )
            throw new Exception("Incorrect propagated port " + node.getName() + " base port" + dp.getValue().toString()
                    + "is missing in module " + moduleName);

        return basePort;
    }

    public static void setPortVariable(Node node, String variableName)
    {
        node.getAttributes().add(DPSUtils.createReadOnly(ConnectionPort.VARIABLE_NAME_ATTR, String.class, variableName));
    }

    public static boolean hasOrientation(Node node)
    {
        return node.getAttributes().hasProperty(PortOrientation.ORIENTATION_ATTR);

    }

    /**
     * Method for getting actual port orientation.<br>
     * in the case of composite diagram we would like to define orientation independently from case of simple diagram.
     * @param node
     * @return
     */
    public static PortOrientation getPortOrientation(Node node)
    {
        Object obj = node.getAttributes().getValue(PortOrientation.ORIENTATION_ATTR);
        if( obj instanceof PortOrientation )
            return (PortOrientation)obj;
        else if( obj instanceof String )
            return PortOrientation.getOrientation((String)obj);
        else
            node.getAttributes().add(new DynamicProperty(PortOrientation.ORIENTATION_ATTR, PortOrientation.class, PortOrientation.RIGHT));
        return PortOrientation.RIGHT;
    }

    public static void setPortOrientation(Node node, PortOrientation orientation)
    {
        node.getAttributes().add(new DynamicProperty(PortOrientation.ORIENTATION_ATTR, PortOrientation.class, orientation));
    }

    /**Redirects edge from node  node to  newNode If edge is not adjacent to node - does nothing.<br>
     * <b>Warning:</b> Removes edge from node
     */
    public static void redirect(Edge edge, Node node, Node newNode)
    {
        if( node.equals(edge.getInput()) )
        {
            edge.setInput(newNode);
            node.removeEdge(edge);
            newNode.addEdge(edge);
        }
        else if( node.equals(edge.getOutput()) )
        {
            edge.setOutput(newNode);
            node.removeEdge(edge);
            newNode.addEdge(edge);
        }
    }

    /** Returns all <b>top level</b> ports in diagram*/
    public static StreamEx<Node> getPorts(Compartment diagram)
    {
        return diagram.recursiveStream().select(Node.class).filter(Util::isTopLevelPort);
    }

    /**
     * Returns list of ports which belongs to subdiagrams of given diagram If diagram is not composite it will return empty list;
     * @param diagram
     * @return
     */
    public static Node[] getModulesPorts(Diagram diagram, Class<?> type)
    {
        if( !DiagramUtility.isComposite(diagram) )
            return new Node[0];
        return diagram.stream(SubDiagram.class).flatMap(d -> d.stream(Node.class))
                .filter(node -> Util.isPort(node) && ( type == null || node.getKernel().getClass().equals(type) )).toArray(Node[]::new);
    }

    /**
     *
     * @param diagram
     * @param type
     * @return
     */
    public static Node[] getModules(Diagram diagram)
    {
        if( !DiagramUtility.isComposite(diagram) )
            return new Node[0];
        return diagram.recursiveStream().select(Node.class)
                .filter(node -> node instanceof SubDiagram || Util.isAverager(node) || Util.isSwitch(node)).toArray(Node[]::new);
    }

    private static <T> StreamEx<T> ofTree(T root, Predicate<T> hasChildren, Function<T, Stream<T>> mapper)
    {
        return StreamEx.ofTree(root, t -> hasChildren.test(t) ? mapper.apply(t) : null);
    }

    public static Node findPort(Compartment compartment, String variableName)
    {
        return Util
                .<Node> ofTree(compartment, n -> n instanceof Compartment && ! ( n instanceof SubDiagram ),
                        n -> ( (Compartment)n ).stream(Node.class))
                .findAny(node -> Util.isPort(node) && variableName.equals(Util.getPortVariable(node))).orElse(null);
    }

    public static List<Node> findPrivatePorts(Compartment compartment, String variableName)
    {
        return Util
                .<Node> ofTree(compartment, n -> n instanceof Compartment && ! ( n instanceof SubDiagram ),
                        n -> ( (Compartment)n ).stream(Node.class))
                .filter(node -> Util.isPrivatePort(node) && variableName.equals(Util.getPortVariable(node))).toList();
    }

    public static Node findPort(Compartment compartment, PortProperties properties)
    {
        return Util.<Node> ofTree(compartment, n -> n instanceof Compartment && ! ( n instanceof SubDiagram ),
                n -> ( (Compartment)n ).stream(Node.class)).findAny(n -> Util.isPort(n) && matches(n, properties)).orElse(null);
    }

    public static boolean matches(Node n, PortProperties properties)
    {
        try
        {
            boolean propagated = properties.getAccessType().equals(ConnectionPort.PROPAGATED);
            boolean propagatedPort = isPropagatedPort(n);

            if( propagated != propagatedPort )
                return false;
            if( propagated && !Util.getBasePort(n).equals(properties.getModule().findNode(properties.getBasePortName())) )
                return false;
            else if( !propagated && !Util.getPortVariable(n).equals(properties.getVarName()) )
                return false;

            return properties.getPortClass().isAssignableFrom(n.getKernel().getClass())
                    && Util.getAccessType(n).equals(properties.getAccessType());
        }
        catch( Exception ex )
        {
            return false;
        }
    }

    public static List<SubDiagram> getSubDiagrams(Diagram diagram)
    {
        List<SubDiagram> result = new ArrayList<>();
        if( !DiagramUtility.isComposite(diagram) )
            return result;

        for( SubDiagram node : diagram.stream(SubDiagram.class) )
        {
            result.add(node);
            result.addAll(getSubDiagrams(node.getDiagram()));
        }
        return result;
    }

    /**
     * Returns diagram associated with given path
     * It may be either top level diagram or diagram in the hierarchy of diagrams inside given diagram
     * Path is given in the form "SubDiagram1/Subiagram2/Subdiagram3"
     */
    public static Diagram getInnerDiagram(Diagram diagram, String path)
    {
        if( path.isEmpty() )
            return diagram;

        SubDiagram subdiagram = getSubDiagram(diagram, path);

        return subdiagram == null ? diagram : subdiagram.getDiagram();
    }

    /**
     * Return two  main components: path to containing subdiagram and variable name TODO: maybe create separate class VariablePath 
     */
    public static String[] getMainPathComponents(String path)
    {
        if( !path.contains("/") )
            return new String[] {"", path};
        int index = path.lastIndexOf("/");
        return new String[] {path.substring(0, index), path.substring(index + 1)};
    }

    /**
     * Returns variable by its path in diagram hierarchy (e.g. SubDiagram1/SubDiagram2/SubDiagram3/Variable_Name or TopLevel_Variable_Name if variable is on the top level)
     * This path is the same as in simulation engine varPathIndexMapping
     */
    public static Variable getVariable(Diagram diagram, String path)
    {
        if( !path.contains("/") )
            return diagram.getRole(EModel.class).getVariable(path);

        int index = path.lastIndexOf("/");

        String subDiagramPath = path.substring(0, index);
        String name = path.substring(index + 1);
        return getInnerDiagram(diagram, subDiagramPath).getRole(EModel.class).getVariable(name);
    }

    public static Variable getVariable(Diagram diagram, String path, String delimiter)
    {
        if( !path.contains(delimiter) )
            return diagram.getRole(EModel.class).getVariable(path);

        int index = path.lastIndexOf(delimiter);
        String subDiagramPath = path.substring(0, index);
        String name = path.substring(index + 1);
        return getInnerDiagram(diagram, subDiagramPath).getRole(EModel.class).getVariable(name);
    }

    /**
     * Finds subdiagram inside given diagram with given path (i.e. it can be deep in hierarchy: sub1/sub2/...)
     */
    public static SubDiagram getSubDiagram(Diagram diagram, String path)
    {
        if( path.isEmpty() )
            return null;

        String[] pathElements = path.contains("/") ? path.split("/") : new String[] {path};

        SubDiagram subdiagram = null;
        for( String pathElement : pathElements ) //TODO: here we rely on the fact that subdiagrams in composite diagram have unique names (on the same level of hierarchy)
        {
            subdiagram = findDiagramElement(diagram, pathElement, SubDiagram.class).findFirst().orElse(null);
            if( subdiagram == null )
                return null;
            diagram = subdiagram.getDiagram();
        }

        return subdiagram;
    }

    /**
     * Find all diagram elements with specific class and name
     */
    public static <T extends DiagramElement> Stream<T> findDiagramElement(Diagram diagram, String name, Class<T> clazz)
    {
        return diagram.recursiveStream().select(clazz).filter(de -> de.getName().equals(name));
    }

    /**
     * Returns path of given subdiagram.
     * Path is given in the form "SubDiagram1\Subiagram2\Subdiagram3"
     */
    public static String getPath(SubDiagram subdiagram)
    {
        List<String> pathElements = new ArrayList<>();

        while( subdiagram != null )
        {
            pathElements.add(subdiagram.getName());
            subdiagram = SubDiagram.getParentSubDiagram(Diagram.getDiagram(subdiagram));
        }

        Collections.reverse(pathElements);
        return StreamEx.of(pathElements).joining("/");
    }

    public static StreamEx<String> getModelDefinitionNames(Diagram diagram)
    {
        if( !DiagramUtility.isComposite(diagram) )
            return StreamEx.of(Collections.emptyList());

        return diagram.recursiveStream().select(ModelDefinition.class).map(ModelDefinition::getName);
    }

    public static void removeProperties(DiagramElement de, Set<String> exceptions)
    {
        DynamicPropertySet dps = de.getAttributes();
        Set<String> attributeNames = new HashSet<>(dps.asMap().keySet());
        attributeNames.removeAll(exceptions);
        for( String attributeName : attributeNames )
            dps.remove(attributeName);
    }

    public static String getAccessType(Node port)
    {
        return isPublicPort(port) ? ConnectionPort.PUBLIC : isPropagatedPort(port) ? ConnectionPort.PROPAGATED : ConnectionPort.PRIVATE;
    }

    public static String getPortType(Node port)
    {
        return isOutputPort(port) ? "output" : isInputPort(port) ? "input" : "contact";
    }

    /**
     * Moves all element of diagram to positive domain
     * @param diagram
     */
    public static void moveToPositive(Diagram diagram)
    {
        int minX = 0;
        int minY = 0;
        for( Point p : diagram.recursiveStream().select(Node.class).map(n -> n.getLocation()) )
        {
            if( p.x < minX )
                minX = p.x;

            if( p.y < minY )
                minY = p.y;
        }

        if( minX == 0 && minY == 0 )
            return;

        minX = Math.max(0, -minX);
        minY = Math.max(0, -minY);

        for( DiagramElement de : diagram.recursiveStream() )
        {
            if( de instanceof Node )
            {
                Point p = ( (Node)de ).getLocation();
                p.translate(minX, minY);
                ( (Node)de ).setLocation(p);
            }

            if( de instanceof Edge )
            {
                Edge e = (Edge)de;
                if( e.getPath() != null )
                    e.getPath().translate(minX, minY);

                Point inPort = e.getInPort();
                Point outPort = e.getOutPort();
                inPort.translate(minX, minY);
                outPort.translate(minX, minY);
                e.setInPort(inPort);
                e.setOutPort(outPort);
            }
        }
    }


    /**
     * Changes orientation for all nodes who have this attribute<br>
     * New orientation is either "right" if !vertical or "bottom" if vertical<br>
     * Method is used for hierarchic layout
     */
    public static void setOrientation(Diagram diagram, boolean vertical)
    {
        diagram.recursiveStream().select(Node.class)
                .forEach(n -> setOrientation(n, vertical ? PortOrientation.BOTTOM : PortOrientation.RIGHT));
    }

    public static void setOrientation(Node node, PortOrientation orientation)
    {
        DynamicProperty dp = node.getAttributes().getProperty(PortOrientation.ORIENTATION_ATTR);
        if( dp == null )
        {
            dp = new DynamicProperty(PortOrientation.ORIENTATION_ATTR, PortOrientation.class, orientation);
            node.getAttributes().add(dp);
        }
        PortOrientation oldOrientation = (PortOrientation)dp.getValue();
        if( orientation.equals(oldOrientation) )
            return;

        if( oldOrientation.isVertical() != orientation.isVertical() )
        {
            int w = node.getShapeSize().width;
            int h = node.getShapeSize().height;
            int x = node.getLocation().x + ( w - h ) / 2;
            int y = node.getLocation().y + ( h - w ) / 2;
            node.setLocation(new Point(x, y));
            node.setShapeSize(new Dimension(h, w));
        }

        dp.setValue(orientation);
        node.setView(null);
    }

    /**
     * defines and sets appropriate orientation to reaction
     * @param node
     */
    public static void defineOrientation(Node node)
    {
        if( !isReaction(node) )
            return;

        List<Point> products = node.edges()
                .filter(e -> e.getKernel() instanceof SpecieReference && ( (SpecieReference)e.getKernel() ).isProduct())
                .map(e -> e.getOtherEnd(node).getLocation()).toList();
        List<Point> reactants = node.edges()
                .filter(e -> e.getKernel() instanceof SpecieReference && ( (SpecieReference)e.getKernel() ).isReactant())
                .map(e -> e.getOtherEnd(node).getLocation()).toList();

        int left = 0;
        int right = 0;
        int top = 0;
        int bottom = 0;

        for( Point product : products )
        {
            for( Point reactant : reactants )
            {
                int xDist = product.x - reactant.x;
                int yDist = product.y - reactant.y;

                if( Math.abs(xDist) > Math.abs(yDist) )
                {
                    if( xDist > 0 )
                        right++;
                    else
                        left++;
                }
                else
                {
                    if( yDist > 0 )
                        bottom++;
                    else
                        top++;
                }
            }
        }

        if( top >= right && top >= left && top >= bottom )
            Util.setOrientation(node, PortOrientation.TOP);
        else if( right >= left && right >= bottom )
            Util.setOrientation(node, PortOrientation.RIGHT);
        else
            Util.setOrientation(node, bottom >= left ? PortOrientation.BOTTOM : PortOrientation.LEFT);
    }

    public static boolean isProduct(Edge e)
    {
        return ( e.getKernel() instanceof SpecieReference && ( (SpecieReference)e.getKernel() ).isProduct() )
                || "production".equals(e.getKernel().getType());
    }

    public static boolean isReactant(Edge e)
    {
        return ( e.getKernel() instanceof SpecieReference && ( (SpecieReference)e.getKernel() ).isReactant() )
                || "consumption".equals(e.getKernel().getType());
    }

    public static boolean isModifier(Edge e)
    {
        return e.getKernel() instanceof SpecieReference && ! ( (SpecieReference)e.getKernel() ).isReactantOrProduct()
                || "regulation".equals(e.getKernel().getType());
    }

    public static void setConnectionPort(Connection connection, Node node, boolean input)
    {
        String varName = null;
        if( Util.isPort(node) )
            varName = Util.getPortVariable(node);
        else if( node.getRole() instanceof VariableRole )
            varName = node.getRole(VariableRole.class).getName();

        if( input )
            connection.setInputPort(new Connection.Port(varName));
        else
            connection.setOutputPort(new Connection.Port(varName));
    }

    /**Return all edges which connects to any instance of given bus node*/
    public static StreamEx<Edge> getBusEdges(biouml.model.Node bus)
    {
        return StreamEx.of(bus.getRole(VariableRole.class).getAssociatedElements()).select(biouml.model.Node.class)
                .flatMap(node -> node.edges());
    }

    /**
     * Check if compartment contains element with the given kernel.
     * Kernels are compared by name and class
     */
    public static boolean hasNodeWithKernel(Compartment cmp, Base kernel)
    {
        DiagramElement diagramElement = cmp.stream().filter(de -> de.getKernel() != null
                && de.getKernel().getName().equals(kernel.getName()) && de.getKernel().getClass().equals(kernel.getClass())).findAny()
                .orElse(null);
        return diagramElement != null;
    }

    /**
     * Find node in cmp by kernel
     */
    public static Node findNode(Compartment cmp, Base kernel)
    {
        return cmp.recursiveStream().select(Compartment.class).flatMap(c -> c.getKernelNodes(kernel)).findAny().orElse(null);
    }

    public static boolean isNote(Node node)
    {
        return node.getKernel() instanceof Note;
    }
    public static boolean isTable(Node node)
    {
        return Type.TYPE_TABLE.equals(node.getKernel().getType());
    }
}
