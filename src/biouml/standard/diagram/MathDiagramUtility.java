package biouml.standard.diagram;

import java.util.logging.Level;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.Nonnull;

import java.util.logging.Logger;

import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.model.Utils;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.MathCalculator;
import biouml.model.dynamics.MathContext;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.standard.type.Stub;
import biouml.standard.type.Type;
import one.util.streamex.StreamEx;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.Option;

public class MathDiagramUtility
{
    private static final String DEPENDENCY_TYPE = "dependencyType";
    protected static final Logger log = Logger.getLogger(DiagramUtility.class.getName());

    /**
     * Removes all dependency edges from diagram
     * @param diagram
     */
    public static void removeDependencies(Diagram diagram)
    {
        for( Edge e : diagram.recursiveStream().select(Edge.class).filter(e -> e.getKernel() instanceof Stub.Dependency).toList() )
        {
            try
            {
                e.getOrigin().remove(e.getName());
            }
            catch( Exception ex )
            {
                log.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
        return;
    }

    public static Map<String, Set<Node>> findEquationNodes(EModel emodel)
    {
        Map<String, Set<Node>> equationNodes = new HashMap<>();
        emodel.getEquations().filter(eq -> eq.getDiagramElement() instanceof Node && !eq.hasDelegate()).forEach(eq -> {
            Variable var = emodel.getVariable(eq.getVariable());
            if( var != null )
                equationNodes.computeIfAbsent( var.getName(), k -> new HashSet<>() ).add( getActualNode((Node)eq.getDiagramElement()) );
        });
        return equationNodes;
    }

    public static Map<String, Set<Node>> findVariableNodes(EModel emodel)
    {
        Map<String, Set<Node>> variableNodes = new HashMap<>();
        emodel.getVariableRoles().stream().filter(v -> Util.isVariable(v.getDiagramElement())).forEach(v->
        variableNodes.computeIfAbsent( v.getName(), k -> new HashSet<>() ).add( (Node)v.getDiagramElement() ));
        return variableNodes;
    }

    public static Set<Node> findPortNodes(Diagram diagram, String varName, boolean input)
    {
        Set<Node> ports = new HashSet<>();
        EModel emodel = diagram.getRole(EModel.class);
        diagram.recursiveStream().filter(
                de -> ( ( Util.isInputPort( de ) && input ) || ( Util.isOutputPort( de ) && !input ) || Util.isContactPort( de ) ) )
                .forEach( de -> {
                    Node n = (Node)de;
                    String variable = Util.getPortVariable( n );
                    if( variable == null || !emodel.containsVariable( variable ) ) //TODO: check if this is really necessary
                        log.log(Level.SEVERE,  "Error: port " + de.getName() + " does not have variable name or variable is incorrect" );
                    else if( varName.equals( emodel.getVariable( variable ).getName() ) )
                        ports.add( n );
                } );
        return ports;
    }

    public static Map<String, Node> findPortNodes(Diagram diagram, boolean input)
    {
        Map<String, Node> ports = new HashMap<>();
        EModel emodel = diagram.getRole(EModel.class);
        diagram.recursiveStream()
                .filter(de -> ( ( Util.isInputPort(de) && input ) || ( Util.isOutputPort(de) && !input ) || Util.isContactPort(de) ))
                .forEach(de ->
                {
                    Node n = (Node)de;
                    String variable = Util.getPortVariable(n);
                    if( variable == null || !emodel.containsVariable( variable ) ) //TODO: check if this is really necessary
                        log.log(Level.SEVERE, "Error: port " + de.getName() + " does not have variable name or variable is incorrect");
                    else
                        ports.put(emodel.getVariable(variable).getName(), n);
                });
        return ports;
    }


    /**
     * Generates all necessary dependency edges in MathDiagramType diagram
     * Will also remove all dependencies that are not needed anymore
     */
    public static void generateDependencies(Diagram diagram)
    {
        if( ! ( diagram.getRole() instanceof EModel ) )
            return;
        EModel emodel = diagram.getRole(EModel.class);

        //finding potential control and target elements
        Map<String, Set<Node>> varToEquationNodes = findEquationNodes(emodel);
        Map<String, Set<Node>> varToVariableNodes = findVariableNodes(emodel);
        Map<String, Node> varToInputPort = findPortNodes(diagram, true);
        Map<String, Node> varOutputPortMap = findPortNodes(diagram, false);

        MathCalculator calculator = new MathCalculator(emodel);

        List<Node> targets = StreamEx.of(varToEquationNodes.values()).flatMap(Set::stream).map(n -> getActualNode(n))
                .append(StreamEx.of(varToVariableNodes.values()).flatMap(Set::stream)).append(varOutputPortMap.values()).toList();

        for( Node targetNode : targets )
        {
            try
            {
                //Nodes which control target node
                HashMap<Node, Integer> controlNodesEffect = getControllerNodes(calculator, targetNode, varToEquationNodes,
                        varToVariableNodes, varToInputPort);

                //Remove old incoming dependency edges
                for( Edge e : targetNode.edges()
                        .filter(e -> e.getKernel().getType().equals(Type.TYPE_DEPENDENCY) && e.getOutput().equals(targetNode)).toSet() )
                    e.getOrigin().remove(e.getName());

                //Generate new dependency edges
                for( Map.Entry<Node, Integer> entry : controlNodesEffect.entrySet() )
                    generateDependencyEdge(entry.getKey(), targetNode, entry.getValue());
            }
            catch( Exception ex )
            {
                log.log(Level.SEVERE, "Error during dependency edges generation for node" + targetNode + ": " + ExceptionRegistry.log(ex));
            }
        }
    }

    /**
     * Returns node to which edge should be attached:
     * e.g.: if node lies inside block then we should attach edge to block.
     * @param n
     * @return
     */
    protected static Node getActualNode(Node node)
    {
        Option parent = node.getParent();
        return ( parent instanceof Node && Util.isBlock((Node)parent) )? (Node)parent: node;
    }

    /**
     * Generating edge with dependency kernel between controlNode and targetNode.
     * @param diagram
     * @param controlNode
     * @param targetNode
     * @param effect
     * @throws Exception
     */
    private static void generateDependencyEdge(@Nonnull Node controlNode, Node targetNode, int effect) throws Exception
    {
        if (controlNode.equals(targetNode) && !(controlNode.getRole() instanceof Equation)) //only equation controls itself
            return;

        String dependencyType = effect > 0 ? "increase" : effect < 0 ? "decrease" : "none";
        for( Edge oldEdge : targetNode.getEdges() )
        {
            if( oldEdge.getKernel().getType().equals(Type.TYPE_DEPENDENCY) && oldEdge.getInput().equals(controlNode) )
            {
                DynamicProperty dp = oldEdge.getAttributes().getProperty(DEPENDENCY_TYPE);
                if( dp == null || !dependencyType.equals(dp.getValue()) )
                    oldEdge.getAttributes().add(new DynamicProperty(DEPENDENCY_TYPE, String.class, dependencyType));
                return;
            }
        }
        Compartment origin = Diagram.findCommonOrigin(controlNode, targetNode);
        Stub.Dependency kernel = new Stub.Dependency(origin, controlNode.getName() + " -> " + targetNode.getName());
        Edge newEdge = new Edge(kernel, controlNode, targetNode);
        newEdge.getAttributes().add(new DynamicProperty(DEPENDENCY_TYPE, String.class, dependencyType));

        //path restoring
        newEdge.save();
    }

    public static Map<Node, Integer> getControlledNodes(Node node, MathCalculator calculator, Map<String, Set<Node>> varToEquations,
            Map<String, Set<Node>> varToNodes, Map<String, Node> varToPort)
    {

        if( Util.isBlock(node) )
        {
            return ( (Compartment)node ).stream(Node.class).filter(n -> n.getRole() instanceof Equation)
                    .flatMapToEntry(n -> getControlledNodes(n, calculator, varToEquations, varToNodes, varToPort)).toMap();
        }
        else if( node.getRole() instanceof Equation )
        {
            Map<Node, Integer> controlledNodes = new HashMap<>();

            String var = node.getRole( Equation.class ).getVariable();
            StreamEx.of(varToEquations.values()).flatMap(Set::stream).forEach(n -> {
                Map<String, Integer> controllerVariablesEffect = getControllerVariables(calculator, n);
                if( controllerVariablesEffect.containsKey(var) )
                    controlledNodes.put(n, controllerVariablesEffect.get(var));
            });

            if( varToNodes.containsKey(var) )
            {
                for( Node n : varToNodes.get(var) )
                    controlledNodes.put(n, 0);
            }

            if( varToPort.containsKey(var) )
            {
                Node n = varToPort.get(var);
                controlledNodes.put(n, 0);
            }
            return controlledNodes;
        }
        else
        {
            return Collections.emptyMap();
        }
    }

    /**
     * Get all equations from which current equation is dependent,
     * @return
     * @throws Exception
     */
    public static HashMap<Node, Integer> getControllerNodes(MathCalculator calculator, Node targetNode, Map<String, Set<Node>> varToEquations,
            Map<String, Set<Node>> varToNodes, Map<String, Node> varToPort)
    {
        HashMap<Node, Integer> result = new HashMap<>();

        //finding controller variables and calculating their effects
        Map<String, Integer> controllerVariablesEffect = getControllerVariables(calculator, targetNode);

        //finding nodes which produce controller variables, they are controller nodes
        for( Map.Entry<String, Integer> entry : controllerVariablesEffect.entrySet() )
        {
            String var = entry.getKey();
            Set<Node> controllerNodes;
            if( ( controllerNodes = varToEquations.get(var) ) != null )
            {
                for( Node controllerNode : controllerNodes )
                {
                    if( controllerNode.equals(targetNode) && Util.isBlock(targetNode) )
                        result.put(controllerNode, 0);
                    else
                        result.put(controllerNode, entry.getValue());
                }
            }

            if( ( controllerNodes = varToNodes.get(var) ) != null )
            {
                for( Node controllerNode : controllerNodes )
                {
                    if( !controllerNode.equals(targetNode) ) //variable node can not control itself
                        result.put(controllerNode, entry.getValue());
                }
            }

            if( varToPort.containsKey(var) )
                result.put(varToPort.get(var), entry.getValue());
        }
        return result;
    }

    public static Map<String, Integer> getControllerVariables(MathCalculator calculator, Node targetNode)
    {
        if( targetNode.getRole() instanceof Equation )
        {
            Equation eq = targetNode.getRole( Equation.class );
            AstStart start = eq.getMath();
            return Utils.variables( start ).mapToEntry( controllerVariable -> calcEffect( calculator, start, controllerVariable ) )
                    .toMap();
        }
        else if( Util.isOutputPort(targetNode) )
        {
            String controllerVariable = Util.getPortVariable(targetNode);
            return Collections.singletonMap( controllerVariable, 0 );
        }
        else if( targetNode.getRole() instanceof VariableRole )
        {
            String controllerVariable = targetNode.getRole( VariableRole.class ).getName();
            return Collections.singletonMap( controllerVariable, 0 );
        }
        else if( Util.isBlock(targetNode) )
        {
            Map<String, Integer> result = new HashMap<>();

            for( Node node : ( (Compartment)targetNode ).stream(Node.class).filter(node -> node.getRole() instanceof Equation).toList() )
            {
                Map<String, Integer> resultForEquation = getControllerVariables(calculator, node);
                for( Entry<String, Integer> entry : resultForEquation.entrySet() )
                {
                    result.compute(entry.getKey(), (k, v) -> v == null ? entry.getValue() : v + entry.getValue());
                }
            }
            return result;
        }
        return Collections.emptyMap();
    }

    /**
     * Calculates effect on equation result of variable <b>varName</b> value increasing by delta
     * @param start
     * @param emodel
     * @param varName
     * @param delta
     * @return
     */
    public static int calcEffect(MathCalculator calculator, AstStart formula, String varName)
    {
        int effect = calcEffect(calculator, formula, varName, 6, null);

//        if( effect == 0 )
//            effect = calcEffect(calculator, formula, varName, 8, Double.valueOf(1));

        return effect;
    }

    public static int calcEffect(MathCalculator calculator, AstStart formula, String varName, double delta, Double value)
    {
        try
        {
            MathContext values = new MathContext();

//            if( value != null )
//            {
//                for( Variable var : emodel.getVariables() )
//                    values.put(var.getName(), value != null ? value : var.getInitialValue());
//            }

            double val1 = calculator.calculateMath(formula, values)[0];

            values.put(varName, values.get(varName, 0) + delta);

            double val2 = calculator.calculateMath(formula, values)[0];

            return ( val2 > val1 ) ? 1 : ( val2 < val1 ) ? -1 : 0;

        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, e.getMessage());
            return 0;
        }
    }

    public static void generateDependenciesForEquation(Node node)
    {
        node = getActualNode(node);
        Diagram diagram = Diagram.getDiagram(node);
        if( ! ( diagram.getRole() instanceof EModel ) )
            return;
        EModel emodel = diagram.getRole(EModel.class);

        //finding potential control and target elements
        Map<String, Set<Node>> varToEquationNodes = findEquationNodes(emodel);
        Map<String, Set<Node>> varToVariableNodes = findVariableNodes(emodel);
        Map<String, Node> varToInputPort = findPortNodes(diagram, true);
        Map<String, Node> varOutputPortMap = findPortNodes(diagram, false);

        MathCalculator calculator = new MathCalculator(emodel);

        try
        {
            //Nodes which control this equation
            Map<Node, Integer> controllerNodes = getControllerNodes(calculator, node, varToEquationNodes, varToVariableNodes,
                    varToInputPort);

            //Nodes which are controlled by this equation
            Map<Node, Integer> controlledNodes = getControlledNodes(node, calculator, varToEquationNodes, varToVariableNodes,
                    varOutputPortMap);

            //remove old edges
            for( Edge e : node.edges().filter(e->e.getKernel().getType().equals(Type.TYPE_DEPENDENCY) ).toSet() )
                e.getOrigin().remove(e.getName());

            for( Map.Entry<Node, Integer> entry : controllerNodes.entrySet() )
                generateDependencyEdge(entry.getKey(), node, entry.getValue());

            for( Map.Entry<Node, Integer> entry : controlledNodes.entrySet() )
                generateDependencyEdge(node, entry.getKey(), entry.getValue());

        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, "Error during dependency edges generation: " + ex.getMessage());
        }

    }
}
