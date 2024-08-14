package biouml.standard.diagram;

import java.awt.Point;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramContainer;
import biouml.model.DiagramElement;
import biouml.model.DiagramFilter;
import biouml.model.DiagramType;
import biouml.model.Edge;
import biouml.model.ModelDefinition;
import biouml.model.Module;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.SemanticController;
import biouml.model.SubDiagram;
import biouml.model.SubDiagram.PortOrientation;
import biouml.model.dynamics.Connection;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.UndirectedConnection.MainVariableType;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.model.dynamics.plot.PlotsInfo;
import biouml.plugins.simulation.SimulationEngine;
import biouml.plugins.simulation.SimulationEngineRegistry;
import biouml.standard.diagram.ReactionPane.CreateReactionException;
import biouml.standard.type.Base;
import biouml.standard.type.KineticLaw;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import biouml.standard.type.Stub.ConnectionPort;
import biouml.standard.type.Stub.DirectedConnection;
import biouml.standard.type.Stub.UndirectedConnection;
import biouml.standard.type.Type;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.support.IdGenerator;
import ru.biosoft.graphics.editor.ViewEditorPane;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.DPSUtils;

public class DiagramUtility
{
    protected static final Logger log = Logger.getLogger(DiagramUtility.class.getName());

    public static @Nonnull Node createReactionNode(Diagram diagram, Compartment compartment, Reaction oldReaction,
            List<SpecieReference> components, String reactionRate, Point point, String type) throws Exception
    {
        return createReactionNode(diagram, compartment, oldReaction, components, reactionRate, point, type, null);
    }

    public static @Nonnull Node createReactionNode(Diagram diagram, Compartment compartment, Reaction oldReaction,
            List<SpecieReference> components, String reactionRate, Point point, String type, String nodeName) throws Exception
    {
        boolean notificationEnabled = diagram.isNotificationEnabled();
        diagram.setNotificationEnabled(false);
        DefaultSemanticController controller = (DefaultSemanticController)diagram.getType().getSemanticController();

        Reaction reaction = null;
        DataCollection<?> reactionDC = null;
        if( type == null )
        {
            // StandardModuleType case
            Module module = Module.getModule(diagram);
            if( module.getType().isCategorySupported() )
                reactionDC = module.getCategory(Reaction.class);

            // generate unique reaction name
            String name = nodeName != null ? nodeName : ( reactionDC != null ) ? DefaultSemanticController.generateReactionName(reactionDC)
                    : DefaultSemanticController.generateReactionName(compartment);
            reaction = new Reaction(reactionDC, name);
        }
        else
        {
            //XmlDiagramType case
            String name = ( nodeName != null ) ? nodeName : DefaultSemanticController.generateUniqueNodeName(diagram, type);
            reaction = new Reaction(null, name);
        }

        reaction.setParent(diagram);
        reaction.setTitle(generateReactionTitle(components));

        KineticLaw kineticLaw = new KineticLaw();
        if( reactionRate != null && !reactionRate.equals("") )
            kineticLaw.setFormula(reactionRate);
        reaction.setKineticLaw(kineticLaw);

        Node reactionNode = new Node(compartment, reaction);

        if( !controller.canAccept(compartment, reactionNode) )
            throw new CreateReactionException(
                    "Unacceptable compartment for reaction: " + ( compartment != null ? compartment.getName() : "null" ) + ".");

        reactionNode.setRelativeLocation(diagram, point);

        List<Edge> edges = new ArrayList<>();
        // add speicie roles and edges
        for( SpecieReference prototype : components )
        {
            //It's necessary to find node by prototype name (not by prototype specie) for the case when we have two nodes with identifiers like this:
            //CMP0225.CMP0034.PRT003455 and CMP0225.PRT003455
            Node de = diagram.findNode(DiagramUtility.toDiagramPath(prototype.getName()));
            if( de == null )
                de = diagram.findNode(DiagramUtility.toDiagramPath(prototype.getSpecie()));
            String id = SpecieReference.generateSpecieReferenceName(reaction.getName(), de.getKernel().getName(), prototype.getRole());
            SpecieReference real = prototype.clone(reaction, id);
            real.setTitle(de.getKernel().getName() + " as " + prototype.getRole());
            String specieLink = ( reactionDC != null ) ? CollectionFactory.getRelativeName(de.getKernel(), Module.getModule(diagram))
                    : de.getCompleteNameInDiagram();
            real.setSpecie(specieLink);
            reaction.put(real);

            Edge edge = ( real.getRole().equals(SpecieReference.PRODUCT) ) ? new Edge(real, reactionNode, de)
                    : new Edge(real, de, reactionNode);
            reactionNode.addEdge(edge); //needed for generateRoles
            edges.add(edge);
        }

        generateRoles(diagram, reactionNode);

        if( notificationEnabled )
            diagram.setNotificationEnabled(true);

        reaction.setParent(reactionNode);

        if( reaction.getOrigin() != null )
            reaction.getOrigin().put(reaction);

        if( !compartment.contains(reactionNode.getName()) )
            put(diagram, reactionNode);

        for( Edge edge : edges )
            put(diagram, edge);

        return reactionNode;
    }

    public static void put(Diagram diagram, DiagramElement de) throws Exception
    {
        boolean notificationEnabled = diagram.isNotificationEnabled();
        diagram.setNotificationEnabled(true);
        de.save();
        diagram.setNotificationEnabled(notificationEnabled);
    }

    public static String generateReactionTitle(List<SpecieReference> components)
    {
        StringBuffer reactants = new StringBuffer();
        StringBuffer products = new StringBuffer();
        StringBuffer modifiers = new StringBuffer();
        StringBuffer other = new StringBuffer();
        StringBuffer result = new StringBuffer();

        for( SpecieReference component : components )
        {
            String name = component.getSpecieName();

            if( component.getRole().equals(SpecieReference.REACTANT) )
            {
                if( reactants.length() > 0 )
                    reactants.append(" + ");
                reactants.append(name);
            }
            else if( component.getRole().equals(SpecieReference.PRODUCT) )
            {
                if( products.length() > 0 )
                    products.append(" + ");
                products.append(name);
            }
            else if( component.getRole().equals(SpecieReference.MODIFIER) )
            {
                if( modifiers.length() > 0 )
                    modifiers.append(", ");
                modifiers.append(name);
            }
            else if( component.getRole().equals(SpecieReference.OTHER) )
            {
                if( other.length() > 0 )
                    other.append(", ");
                other.append(name);
            }
        }

        if( reactants.length() > 0 )
        {
            result.append(reactants.toString());
            result.append(" ");
        }

        // @pending 1
        if( modifiers.length() == 0 && other.length() == 0 )
            result.append("->");
        else
        {
            result.append("-");
            result.append(modifiers.toString());
            result.append(other.toString());
            result.append("->");
        }

        if( products.length() > 0 )
        {
            result.append(" ");
            result.append(products.toString());
        }

        return result.toString();
    }

    /**
     * Generates roles for reaction node and edges for pathway simulation diagram.
     *
     * @throws Exception if can not create equation for some reaction edge.
     */
    public static void generateRoles(Diagram diagram, Node reactionNode) throws Exception
    {
        if( diagram.getRole() == null || ! ( diagram.getRole() instanceof EModel ) )
            return;

        generateReactionRole(diagram, reactionNode);
        for( Edge edge : reactionNode.edges()
                .filter(e -> e.getKernel() instanceof SpecieReference && ( (SpecieReference)e.getKernel() ).isReactantOrProduct()) )
        {
            try
            {
                generateReactionRole(diagram, edge);
            }
            catch( Throwable t )
            {
                throw new Exception("Can not create equation for edge " + edge.getName() + ", error: " + t.getMessage());
            }
        }
    }

    public static void generateReactionRole(Diagram diagram, DiagramElement de)
    {
        boolean isEnabled = diagram.isNotificationEnabled();
        diagram.setNotificationEnabled(false);

        if( de instanceof Node )
        {
            Equation rule = new Equation(de, Equation.TYPE_SCALAR, "$$rate_" + de.getName());
            EModel emodel = diagram.getRole(EModel.class);
            emodel.put(new Variable("$$rate_" + de.getName(), emodel, emodel.getVariables()));
            de.setRole(rule);
        }
        else if( de instanceof Edge && de.getKernel() instanceof SpecieReference )
        {
            Edge edge = (Edge)de;
            Node specie = ( edge.getInput().getKernel() instanceof Reaction ) ? edge.getOutput() : edge.getInput();
            VariableRole var = (VariableRole)specie.getRole();
            if( var == null )
            {
                var = new VariableRole(specie, 0.0);
                specie.setRole(var);
                log.warning("Generates variable for specie " + specie.getName());
            }
            de.setRole(new Equation(de, Equation.TYPE_RATE, var.getName()));
        }
        diagram.setNotificationEnabled(isEnabled);
    }


    /**
     * Check whether diagram can contain other diagrams, can be used to test if diagram is Composite Document
     * @param diagram
     * @return
     */
    public static boolean isComposite(Diagram diagram)
    {
        return isComposite(diagram.getType());
    }

    private static boolean isComposite(DiagramType type)
    {
        Object[] classes = type.getNodeTypes();

        if( classes == null )
            return false;

        for( Object obj : classes )
        {
            if( obj instanceof Class
                    && ( ( ( (Class<?>)obj ).isAssignableFrom(SubDiagram.class) || ( (Class<?>)obj ).isAssignableFrom(Diagram.class) )
                            || DiagramContainer.class.isAssignableFrom((Class<?>)obj) ) )
            {
                return true;
            }
        }
        return false;
    }

    public static boolean containModules(Compartment diagram)
    {
        return diagram.recursiveStream().select(Node.class)
                .anyMatch(n -> Util.isSubDiagram(n) || Util.isSwitch(n) || Util.isConstant(n) || Util.isAverager(n));
    }

    public static boolean containPorts(Compartment diagram)
    {
        return diagram.recursiveStream().select(Node.class).anyMatch(n -> Util.isPort(n));
    }

    /**
     * Converts path in repository style ("a/b/c") into diagram path ("a.b.c")
     * This method takes into account repository path escaping, thus "a/b/c\\sd" will be converted into "a.b.c/d"
     * @param pathStr path in repository style
     * @return
     */
    public static String toDiagramPath(String pathStr)
    {
        return StringUtils.join(DataElementPath.create(pathStr).getPathComponents(), ".");
    }

    /**
     * Converts path in diagram style ("a.b.c") into repository path ("a/b/c")
     * This method takes into account repository path escaping, thus "a.b.c/d" will be converted into "a/b/c\\sd"
     * @param pathStr path in diagram style
     * @return
     */
    public static DataElementPath toRepositoryPath(String pathStr)
    {
        return StreamEx.split(pathStr, '.').foldLeft(DataElementPath.EMPTY_PATH, DataElementPath::getChildPath);
    }

    public static void setBaseOrigin(Diagram diagram)
    {
        String baseOriginIDProperty = diagram.getAttributes().getValueAsString("baseOriginID");
        if( baseOriginIDProperty != null )
            diagram.setOrigin(CollectionFactory.getDataCollection(baseOriginIDProperty));
    }

    /**
     *
     * @param diagram
     * @return all nodes of reaction (including embedded in compartments)
     */
    public static List<Node> getReactionNodes(Compartment diagram)
    {
        return diagram.recursiveStream().select(Node.class).filter(node -> node.getKernel() instanceof Reaction).toList();
    }

    /**
     * get all reactions from selected compartment (including in nested compartments)
     */
    public static StreamEx<Reaction> getReactions(Compartment compartment)
    {
        return compartment.recursiveStream().select(Node.class).map(node -> node.getKernel()).select(Reaction.class);
    }

    /**
     * Returns stream of all ports which belong to the top level of the diagram (ant not to any subdiagram)
     */
    public static StreamEx<Node> getTopLevelPorts(Compartment compartment)
    {
        return compartment.recursiveStream().select(Node.class)
                .filter(n -> Util.isPort(n) && ! ( n.getCompartment() instanceof SubDiagram ));
    }

    public static StreamEx<Edge> getConnections(Compartment compartment)
    {
        return compartment.recursiveStream().select(Edge.class).filter(e -> Util.isConnection(e));
    }

    public static StreamEx<SubDiagram> getSubDiagrams(Compartment compartment)
    {
        return compartment.recursiveStream().select(SubDiagram.class);
    }

    public static List<DiagramElement> createPortNode(Compartment compartment, PortProperties portProperties, ViewEditorPane viewEditor,
            Point point) throws Exception
    {
        Diagram diagram = Diagram.getDiagram(compartment);
        SemanticController controller = diagram.getType().getSemanticController();

        EModel emodel = diagram.getRole(EModel.class);
        List<DynamicProperty> propertiesToAdd = new ArrayList<>();

        Class<?> type = portProperties.getPortClass();

        if( ! ( Stub.ConnectionPort.class.isAssignableFrom(type) ) )
            throw new IllegalArgumentException("Invalid class for port kernel: " + type.toString());

        DiagramElement basePort = null;
        if( portProperties.isPrivatePort() || portProperties.isPublicPort() )
        {
            String varName = portProperties.getVarName();
            if( varName == null || varName.isEmpty() )
                throw new IllegalArgumentException("Please specify variable for port");
            for( Node node : Util.getPorts(diagram) )
            {
                if( node.getKernel().getClass().equals(type) && varName.equals(Util.getPortVariable(node)) )
                {
                    if( !portProperties.isPrivatePort() && portProperties.getAccessType().equals(Util.getAccessType(node)) )
                        throw new IllegalArgumentException("Port for the same type and the same variable already exists");
                }
            }
            Variable var = emodel.getVariable(varName);
            propertiesToAdd.add(DPSUtils.createReadOnly(ConnectionPort.VARIABLE_NAME_ATTR, String.class, var.getName()));
            propertiesToAdd.add(DPSUtils.createReadOnly(ConnectionPort.ACCESS_TYPE, String.class, portProperties.getAccessType()));
        }
        if( portProperties.isPrivatePort() )
        {
            String basePortName = portProperties.getBasePortName();
            Compartment module = portProperties.getModule();
            if( module != null )
            {
                //                throw new IllegalArgumentException( "Please specify module for private port" );
                if( basePortName == null || basePortName.equals("") )
                    throw new IllegalArgumentException("Please specify base port for private");
                basePort = module.get(basePortName);
                if( basePort == null || ! ( basePort instanceof Node ) )
                    throw new IllegalArgumentException("Can not find port " + basePortName + " in module " + module.getName());
            }
        }
        else if( portProperties.isPropagatedPort() )
        {
            String basePortName = portProperties.getBasePortName();
            Compartment module = portProperties.getModule();
            if( module == null )
                throw new IllegalArgumentException("Please specify module for port propagation");
            if( basePortName == null || basePortName.equals("") )
                throw new IllegalArgumentException("Please specify base port for propagation");

            for( Node node : Util.getPorts(module) )
            {
                if( node.getKernel().getClass().equals(type) && Util.isPropagatedPort(node) )
                {
                    DynamicProperty basePortAttr = node.getAttributes().getProperty(ConnectionPort.BASE_PORT_NAME_ATTR);

                    if( basePortAttr != null && basePortAttr.getValue().equals(basePortName) )
                        throw new IllegalArgumentException("Propagated Port for " + basePortName + " already exists");
                }
            }

            basePort = module.get(basePortName);
            if( basePort == null || ! ( basePort instanceof Node ) )
                throw new IllegalArgumentException("Can not find port " + basePortName + " in module " + module.getName());

            propertiesToAdd.add(DPSUtils.createReadOnly(ConnectionPort.BASE_PORT_NAME_ATTR, String.class, basePortName));
            propertiesToAdd.add(DPSUtils.createReadOnly(ConnectionPort.BASE_MODULE_NAME_ATTR, String.class, module.getName()));
            propertiesToAdd.add(DPSUtils.createReadOnly(ConnectionPort.ACCESS_TYPE, String.class, ConnectionPort.PROPAGATED));
            propertiesToAdd.add(DPSUtils.createReadOnly(ConnectionPort.VARIABLE_NAME_ATTR, String.class, Util.getPortVariable(basePort)));
        }

        PortOrientation orientation = PortOrientation.RIGHT;
        String name = DefaultSemanticController.generateUniqueNodeName(diagram, portProperties.getName());
        Base kernel;
        if( type == Stub.OutputConnectionPort.class )
        {
            kernel = new Stub.OutputConnectionPort(null, name);
        }
        else if( type == Stub.InputConnectionPort.class )
        {
            kernel = new Stub.InputConnectionPort(null, name);
            orientation = PortOrientation.LEFT;
        }
        else
        {
            kernel = new Stub.ContactConnectionPort(null, name);
        }

        Node connectionPort = new Node(compartment, kernel);
        connectionPort.setTitle(portProperties.getTitle());

        propertiesToAdd.add(new DynamicProperty(PortOrientation.ORIENTATION_ATTR, PortOrientation.class, orientation));
        propertiesToAdd.forEach(dp -> connectionPort.getAttributes().add(dp));

        List<DiagramElement> result = new ArrayList<>();

        if( !controller.canAccept(compartment, connectionPort) )
            return result;

        result.add(connectionPort);

        Edge e = createPropagatedPortEdge(connectionPort);
        if( e != null )
            result.add(e);
        e = createPortEdge(connectionPort, null);
        if( e != null )
            result.add(e);

        if( portProperties.isPrivatePort() && basePort != null )
        {
            if( portProperties.getPortType().equals(Type.TYPE_CONTACT_CONNECTION_PORT) || portProperties.getPortType().equals("contact") )
                createConnection(diagram, connectionPort, (Node)basePort, false);
            else if( portProperties.getPortType().equals(Type.TYPE_INPUT_CONNECTION_PORT) || portProperties.getPortType().equals("input") )
                createConnection(diagram, (Node)basePort, connectionPort, true);
            else
                createConnection(diagram, connectionPort, (Node)basePort, true);
        }
        return result;
    }

    /**
     * Automatically creates edge from Node with variable to connection port associatiing with taht variable
     * @param portNode
     * @param viewEditor
     */
    public static Edge createPortEdge(Node portNode, ViewEditorPane viewEditor)
    {
        if( Util.isPropagatedPort(portNode) )
            return null;
        try
        {
            String varName = Util.getPortVariable(portNode);

            if( varName == null )
                return null;

            if( ! ( portNode.getParent() instanceof Compartment ) )
                return null;

            Diagram diagram = Diagram.getDiagram(portNode);
            Object nodeObj = diagram.getRole(EModel.class).getVariable(varName).getParent();

            if( ! ( nodeObj instanceof Node ) )
                return null;
            String unqueName = DefaultSemanticController.generateUniqueNodeName(diagram, portNode.getName() + "_link");

            Compartment compartment = Compartment.findCommonOrigin((Node)nodeObj, portNode);
            return new Edge(compartment, new Stub(diagram, unqueName, "portlink"), (Node)nodeObj, portNode);
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, "Can not add edge for port: " + portNode.getName() + " because of " + ex.getMessage());
        }
        return null;
    }

    public static Edge createPropagatedPortEdge(Node portNode) throws Exception
    {
        if( !Util.isPropagatedPort(portNode) )
            return null;

        Node basePort = Util.getBasePort(portNode);

        if( ! ( portNode.getParent() instanceof Compartment ) )
            return null;

        Diagram diagram = Diagram.getDiagram(portNode);
        String edgeName = DefaultSemanticController.generateUniqueNodeName(diagram, basePort.getName() + "_propagation");

        if( portNode.getKernel().getType().equals(Type.TYPE_CONTACT_CONNECTION_PORT) )
        {
            return new Edge(diagram, new UndirectedConnection(diagram, edgeName), basePort, portNode);
        }
        else
        {
            DirectedConnection kernel = new DirectedConnection(diagram, edgeName);
            return Util.isInputPort(portNode) ? new Edge(diagram, kernel, portNode, basePort)
                    : new Edge(diagram, kernel, basePort, portNode);
        }
    }

    public static void compositeModelPostprocess(Set<SubDiagram> subDiagramSet, Set<ModelDefinition> modelDefinitionSet)
    {
        Map<String, ModelDefinition> nameMap = new HashMap<>();

        for( ModelDefinition modDef : modelDefinitionSet )
            nameMap.put(modDef.getDiagram().getName(), modDef);

        for( SubDiagram subDiagram : subDiagramSet )
        {
            Diagram diagram = subDiagram.getDiagram();
            ModelDefinition modDef = nameMap.get(diagram.getName());
            if( modDef != null )
            {
                modDef.markRefModelDefinition(diagram);
                continue;
            }
        }
    }

    public static void compositeModelPostprocess(Diagram compositeDiagram)
    {
        Map<String, ModelDefinition> nameMap = new HashMap<>();

        for( ModelDefinition modDef : compositeDiagram.recursiveStream().select(ModelDefinition.class) )
            nameMap.put(modDef.getDiagram().getName(), modDef);

        for( SubDiagram subDiagram : compositeDiagram.recursiveStream().select(SubDiagram.class) )
        {
            Diagram innerDiagram = subDiagram.getDiagram();
            ModelDefinition modDef = nameMap.get(innerDiagram.getName());
            if( modDef != null )
                modDef.markRefModelDefinition(innerDiagram);

            subDiagram.updatePorts();
        }
    }

    public static String SEPARATOR = "/";
    public static String generatPath(String parentPath, String variableName)
    {
        return parentPath.isEmpty() ? variableName : parentPath + SEPARATOR + variableName;
    }

    public static String generatPath(Diagram diagram)
    {
        List<String> parents = new ArrayList<>();
        SubDiagram parentSubDiagram = SubDiagram.getParentSubDiagram(diagram);
        while( parentSubDiagram != null )
        {
            String[] subDiagramParents = parentSubDiagram.getCompleteNameInDiagram().split("\\.");
            for( int i = subDiagramParents.length - 1; i >= 0; i-- )
                parents.add(subDiagramParents[i]);

            Diagram parentDiagram = Diagram.getDiagram(parentSubDiagram);
            parentSubDiagram = SubDiagram.getParentSubDiagram(parentDiagram);
        }
        return StreamEx.of(parents).joining(SEPARATOR);
    }

    public static List<String> splitPath(String path)
    {
        return Arrays.asList(path.split(SEPARATOR));
    }

    /**Returns all buses from diagram*/
    public static StreamEx<Node> getBuses(Diagram diagram)
    {
        return diagram.recursiveStream().select(Node.class).filter(n -> Util.isBus(n));
    }

    /**Returns all notes from diagram*/
    public static StreamEx<Node> getNotes(Diagram diagram)
    {
        return diagram.recursiveStream().select(Node.class).filter(n -> Util.isNote(n));
    }

    /**Returns all public and propagated ports from diagram*/
    public static StreamEx<Node> getInterfacePorts(Diagram diagram)
    {
        return diagram.recursiveStream().select(Node.class).filter(n -> ( Util.isPublicPort(n) || Util.isPropagatedPort(n) ));
    }

    /**creates new connection ( move to semantic controller? ) */
    public static Connection createConnection(Diagram diagram, Node input, Node output, boolean directedConnection)// throws Exception
    {
        String stubName = IdGenerator.generateUniqueName(diagram, new DecimalFormat("CONNECTION0000"));
        Stub kernel = ( directedConnection ) ? new Stub.DirectedConnection(diagram, stubName)
                : new Stub.UndirectedConnection(diagram, stubName);
        if( input == null || output == null )
        {
            System.out.println("dsad");
        }
        Edge e = new Edge(diagram, kernel, input, output);
        Connection role = ( directedConnection ) ? new biouml.model.dynamics.DirectedConnection(e)
                : new biouml.model.dynamics.UndirectedConnection(e);
        role.setInputPort(new Connection.Port(ConnectionEdgePane.getPortVariableName(input), input.getTitle()));
        role.setOutputPort(new Connection.Port(ConnectionEdgePane.getPortVariableName(output), output.getTitle()));
        e.setRole(role);
        diagram.put(e);
        return role;
    }

    /**
     * Redirect all connections from buses to corresponding ports
     * TODO: testing
     */
    public static void processBuses(Diagram diagram) throws Exception
    {
        Map<Role, List<Node>> clusters = getBuses(diagram).groupingBy(n -> n.getRole());

        for( Entry<Role, List<Node>> cluster : clusters.entrySet() )
        {
            Bus role = (Bus)cluster.getKey();
            List<Edge> edges = StreamEx.of(cluster.getValue()).flatMap(n -> n.edges()).filter(e -> Util.isConnection(e)).toList();

            if( edges.size() == 0 )// <=1
                continue;

            Connection c = edges.get(0).getRole(Connection.class);
            boolean isDirected = c instanceof biouml.model.dynamics.DirectedConnection;

            List<Node> ports = StreamEx.of(edges).map(e -> Util.isBus(e.getInput()) ? e.getOutput() : e.getInput()).toList();

            for( Edge edge : edges )
            {
                edge.getInput().removeEdge(edge);
                edge.getOutput().removeEdge(edge);
                diagram.remove(edge.getName());
            }

            if( isDirected )
            {
                Node outputPort = StreamEx.of(ports).findAny(n -> Util.isOutputPort(n)).orElse(null);
                if( outputPort == null )
                {
                    throw new Exception("Bus " + role.getName() + " does not have input connection!");
                }
                for( Node port : ports )
                {
                    if( !port.equals(outputPort) )
                        createConnection(diagram, outputPort, port, isDirected);
                }
            }
            else
            {
                Node mainPort = null;

                for( Edge edge : edges )
                {
                    biouml.model.dynamics.UndirectedConnection con = edge.getRole(biouml.model.dynamics.UndirectedConnection.class);
                    MainVariableType type = con.getMainVariableType();
                    if( !type.equals(MainVariableType.NOT_SELECTED) )
                    {
                        if( type.equals(MainVariableType.INPUT) && !Util.isBus(edge.getInput()) )
                            mainPort = edge.getInput();
                        else if( !Util.isBus(edge.getOutput()) ) //bus can not be main - it will be removed
                            mainPort = edge.getOutput();
                    }
                }

                if( mainPort != null )
                {
                    for( Node port : ports )
                    {
                        if( !port.equals(mainPort) )
                        {
                            biouml.model.dynamics.UndirectedConnection con = (biouml.model.dynamics.UndirectedConnection)createConnection(
                                    diagram, port, mainPort, false);
                            con.setMainVariableType(MainVariableType.OUTPUT);
                        }
                    }
                }
                else
                {
                    for( int i = 1; i < ports.size(); i++ )
                    {
                        createConnection(diagram, ports.get(i), ports.get(0), false);
                    }
                }

            }
        }

        //last step - remove all buses
        for( Node node : getBuses(diagram) )
        {
            diagram.remove(node.getCompleteNameInDiagram());
        }
    }

    public static SimulationEngine getEngine(Diagram diagram)
    {
        SimulationEngine engine = getPreferredEngine(diagram);
        if( engine != null )
            return engine;
        return SimulationEngineRegistry.getSimulationEngine(diagram);
    }

    public static SimulationEngine getPreferredEngine(Diagram diagram)
    {
        DynamicProperty dp = diagram.getAttributes().getProperty("simulationOptions");
        if( dp != null && dp.getValue() instanceof SimulationEngine )
            return (SimulationEngine)dp.getValue();
        return null;
    }

    public static void setPreferredEngine(Diagram diagram, SimulationEngine engine)
    {
        diagram.getAttributes().add(DPSUtils.createHiddenReadOnlyTransient("simulationOptions", SimulationEngine.class, engine));
    }

    public static void setPlotsInfo(Diagram diagram, PlotsInfo plotsInfo)
    {
        diagram.getAttributes().add(DPSUtils.createHiddenReadOnlyTransient("Plots", PlotsInfo.class, plotsInfo));
    }

    public static PlotsInfo getPlotsInfo(Diagram diagram)
    {
        if( diagram.getAttributes().hasProperty("Plots") )
        {
            Object obj = diagram.getAttributes().getValue("Plots");

            if( obj instanceof PlotsInfo )
                return (PlotsInfo)obj;
        }
        return null;
    }

    public static void setInitialValues(Diagram diagram, TableDataCollection collection, String rowID)
    {
        String[] header = TableDataCollectionUtils.getColumnNames(collection);
        double[] row = TableDataCollectionUtils.getDoubleRow(collection, rowID);
        for( int j = 0; j < header.length; j++ )
        {
            String varPath = header[j].toString();
            Variable var = Util.getVariable(diagram, varPath, "\\");
            if( var == null )
            {
                log.info("Variable not found " + header[j]);
                continue;
            }
            if( var.getName().equals("time") )
                continue;

            var.setInitialValue(row[j]);
        }

        for( SubDiagram subDiagram : Util.getSubDiagrams(diagram) )
        {
            try
            {
                Diagram innerDiagram = subDiagram.getDiagram();
                innerDiagram.save();
            }
            catch( Exception ex )
            {
                log.info("Could not save subdiagram " + subDiagram.getName());
            }
        }
    }

    public static void highlight(Collection<DiagramElement> des)
    {
        for( DiagramElement de : des )
            de.getAttributes().add(DPSUtils.createTransient(Util.HIGHLIGHT_PROPERTY, Boolean.class, true));
    }

    public static void clearHighlight(Diagram diagram)
    {
        diagram.recursiveStream().forEach(de -> de.getAttributes().remove(Util.HIGHLIGHT_PROPERTY));
    }

    /**
     * Transforms diagram elemnt name to SBML-compatible name
     */
    public static String validateName(String name)
    {
        String result = name.replaceAll("\\W", "_");
        if( result.matches("\\d\\w*") )
        {
            result = "_" + result;
        }
        return result;
    }

    public static boolean hasFilter(Diagram diagram, DiagramFilter filter)
    {
        for( DiagramFilter f : diagram.getFilterList() )
        {
            if( f.equals(filter) )
                return true;
        }
        return false;
    }

    public static void addFilter(Diagram diagram, DiagramFilter filter)
    {
        diagram.setFilterList((DiagramFilter[])ArrayUtils.add(diagram.getFilterList(), filter));
    }

    /**Returns all tables from diagram*/
    public static StreamEx<Node> getTables(Diagram diagram)
    {
        return diagram.recursiveStream().select(Node.class).filter(n -> Util.isTable(n));
    }
}
