package biouml.plugins.agentmodeling;

import java.util.HashSet;
import java.util.Set;

import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.SubDiagram;
import biouml.model.dynamics.Connection;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.MultipleDirectedConnection;
import biouml.model.dynamics.UndirectedConnection;
import biouml.model.dynamics.UndirectedConnection.MainVariableType;
import biouml.model.dynamics.Variable;
import biouml.plugins.simulation.Preprocessor;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.diagram.Util;
import biouml.standard.type.Reaction;
import ru.biosoft.math.model.AstStart;
import ru.biosoft.math.model.AstVarNode;
import ru.biosoft.math.model.DefaultParserContext;
import ru.biosoft.math.model.Function;
import ru.biosoft.math.model.LinearFormatter;
import ru.biosoft.math.model.PredefinedFunction;
import ru.biosoft.math.model.Utils;

public class SubModelPreprocessor extends Preprocessor
{
    @Override
    public boolean accept(Diagram diagram)
    {
        SubDiagram subDiagram = SubDiagram.getParentSubDiagram( diagram );
        return ( subDiagram != null && subDiagram.getParent() instanceof Diagram );
    }

    @Override
    public Diagram preprocess(Diagram diagram)
    {
        EModel emodel = diagram.getRole( EModel.class );
        SubDiagram subDiagram = SubDiagram.getParentSubDiagram( diagram );
        if( subDiagram == null )
            return diagram;

        subDiagram.stream(Node.class).filter(de -> Util.isInputPort(de) && de.edges().anyMatch(e -> Util.isConnection(e))).forEach(p ->
        {
            Variable var = emodel.getVariable(Util.getPortVariable(p));
            if( var != null )
                var.setConstant(true);
        });

        Set<String> replacedReactions = new HashSet<>();

        //hack for reactions replaced by undirected connection
        for (Node p: subDiagram.getNodes())
        {
            if( Util.isContactPort(p) && hasConnection(p) )
            {
                Variable var = emodel.getVariable(Util.getPortVariable(p));
                if( var == null )
                    continue;
                if( var.getName().startsWith( "$$" ) )
                    var.setConstant(true);
                replacedReactions.add(var.getName());
            }
        }

        for( Edge e : subDiagram.getEdges() )
        {
            Role role = e.getRole();
            if( role instanceof MultipleDirectedConnection && e.getOutput().equals( subDiagram ) )
            {
                for( Connection connection : ( (MultipleDirectedConnection)role ).getConnections() )
                {
                    String varName = connection.getOutputPort().getVariableName();
                    Variable var = emodel.getVariable( varName );
                    if( var != null )
                        var.setConstant( true );
                }

            }
        }

        //if reaction is replaced by reaction from another submodel then we do not need to apply conversion factor to it
        applyExtentFactor(diagram, getExtentFactor(diagram), replacedReactions);

        return diagram;
    }

    /**
     * Returns true if port node has either incoming directed connection or undirected and it is not main port
     * Also checks if node is propagated outside and propagated port has incoming connections
     * @param node
     * @return
     */
    public boolean hasConnection(Node node)
    {
        if( node.edges().anyMatch(e -> Util.isConnection(e) && e.getRole() != null && !isMain(e, node)) )
            return true;

        Node propagatedPort = node.edges().map(e -> e.getOtherEnd(node)).findAny(n -> Util.isPropagatedPort(n)).orElse(null);
        if( propagatedPort == null )
            return false;
        Diagram parentDiagram = Diagram.getDiagram(node.getCompartment());
        SubDiagram parentSubDiagram = SubDiagram.getParentSubDiagram(parentDiagram);
        if( parentSubDiagram != null )
        {
            Node subDiagramPort = parentSubDiagram.findNode(propagatedPort.getName());
            return hasConnection(subDiagramPort);
        }

        return false;
    }

    public static boolean isMain(Edge e, Node node)
    {
        if (Util.isUndirectedConnection(e))
        {
            UndirectedConnection connection = e.getRole( UndirectedConnection.class );
            MainVariableType type = connection.getMainVariableType();
            return ( e.getInput().equals(node) && type == MainVariableType.INPUT )
                    || ( e.getOutput().equals(node) && type == MainVariableType.OUTPUT );
        }
        else
        {
            return e.getOutput().equals( node );
        }
    }

    /**
     * Returns cumulative extent factor:<br>
     * Say diagram is inside subdiagram which is inside other subdiagram and so on<br>
     * result is multiplication of all subdiagrams extent factors
     */
    protected double getExtentFactor(Diagram diagram)
    {
        SubDiagram subDiagram = SubDiagram.getParentSubDiagram( diagram );
        if( subDiagram == null )
            return 1;

        Diagram parentDiagram = Diagram.getDiagram(subDiagram);
        double factor = getExtentFactor(parentDiagram);

        String extentFactor = subDiagram.getAttributes().getValueAsString(Util.EXTENT_FACTOR);
        if( extentFactor != null && !extentFactor.isEmpty() )
            factor *= parentDiagram.getRole(EModel.class).getVariable(extentFactor).getInitialValue();

        return factor;
    }

    protected void applyExtentFactor(Diagram diagram, double factor, Set<String> exceptions)
    {
        if (factor == 1)
            return;
        for( Node reactioNode : DiagramUtility.getReactionNodes(diagram) )
        {
            Reaction reaction = (Reaction)reactioNode.getKernel();
            if( !exceptions.contains(reactioNode.getRole(Equation.class).getVariable()) )
                reaction.setFormula("(" + reaction.getFormula() + ")*" + factor);
        }

        EModel emodel = diagram.getRole(EModel.class);
        for( Equation eq : emodel.getEquations().filter(eq->!eq.hasDelegate()) )
        {
            String expression = eq.getExpressions()[1];
            AstStart start = emodel.readMath(expression, eq);
            applyExtentFactor(start, factor, exceptions);
            eq.setFormula(new LinearFormatter().format(start)[1]);
        }
    }

    protected void applyExtentFactor(AstStart start, double factor, Set<String> exceptions)
    {
        for ( ru.biosoft.math.model.Node node: Utils.deepChildren(start).select(AstVarNode.class))
        {
            String name = ((AstVarNode)node).getName() ;
            if( name.startsWith("$$") && !exceptions.contains(name))
            {
                ru.biosoft.math.model.Node parent = node.jjtGetParent();
                ru.biosoft.math.model.Node newNode = Utils.applyFunction(node, Utils.createConstant(factor),
                        new PredefinedFunction(DefaultParserContext.DIVIDE, Function.TIMES_PRIORITY, -1));
                parent.jjtReplaceChild(node, newNode);
            }
        }
    }
}