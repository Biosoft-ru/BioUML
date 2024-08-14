package biouml.plugins.agentmodeling;

import java.util.ArrayList;
import java.util.List;

import biouml.model.Diagram;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.SubDiagram;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.UndirectedConnection;
import biouml.model.dynamics.UndirectedConnection.MainVariableType;
import biouml.model.dynamics.Variable;
import biouml.plugins.simulation.Preprocessor;
import biouml.standard.diagram.Util;

/**
 * Preprocessor removes initial value calculation equations (which transforms initial concentration to amount)
 * Equations are removed for variables which are inputs for directed connections or has undirected connections and are not main.
 * @author Ilya
 *
 */
public class InitialValueSubmodelPreprocessor extends Preprocessor
{
    @Override
    public boolean accept(Diagram diagram)
    {
        SubDiagram subDiagram = SubDiagram.getParentSubDiagram(diagram);
        return ( subDiagram != null && subDiagram.getParent() instanceof Diagram );
    }

    @Override
    public Diagram preprocess(Diagram diagram) throws Exception
    {
        EModel emodel = diagram.getRole(EModel.class);
        SubDiagram subDiagram = SubDiagram.getParentSubDiagram(diagram);
        if( subDiagram == null )
            return diagram;

        List<String> variables = new ArrayList<>();
        subDiagram.stream(Node.class).filter(de -> Util.isInputPort(de) && de.edges().anyMatch(e -> Util.isConnection(e))).forEach(p -> {
            Variable var = emodel.getVariable(Util.getPortVariable(p));
            if( var != null )
                variables.add(var.getName());
        });

        List<Equation> toRemove = new ArrayList<>();
        for( Equation eq : emodel.getEquations().filter(eq -> eq.getType().equals(Equation.TYPE_INITIAL_VALUE)
                && variables.contains(eq.getVariable())) )
            toRemove.add(eq);

        for( Equation eq : toRemove )
            diagram.remove(eq.getDiagramElement().getName());

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
        if( Util.isUndirectedConnection(e) )
        {
            UndirectedConnection connection = e.getRole(UndirectedConnection.class);
            MainVariableType type = connection.getMainVariableType();
            return ( e.getInput().equals(node) && type == MainVariableType.INPUT )
                    || ( e.getOutput().equals(node) && type == MainVariableType.OUTPUT );
        }
        else
        {
            return e.getOutput().equals(node);
        }
    }
}