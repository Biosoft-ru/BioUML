package ru.biosoft.analysis.diagram;

import java.util.logging.Level;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import biouml.model.Compartment;
import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.model.Role;
import biouml.model.SemanticController;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Variable;
import biouml.model.dynamics.VariableRole;
import biouml.standard.diagram.DiagramUtility;
import biouml.standard.type.Base;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;

public class RemoveUnobservableMolecules extends AnalysisMethodSupport<RemoveUnobservableMoleculesParameters>
{
    public RemoveUnobservableMolecules(DataCollection<?> origin, String name)
    {
        super(origin, name, new RemoveUnobservableMoleculesParameters());
    }

    private Container container;
    private Set<Node> observableNodes;

    private boolean wasStopped = false;

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        DataElementPath path = parameters.getInputPath();
        Diagram diagram = path.getDataElement(Diagram.class);
        path = parameters.getOutputPath();
        Diagram changedDiagram = diagram.clone(path.optParentCollection(), path.getName());
        changedDiagram.setType(diagram.getType().clone());

        if( parameters.isDeleteAllElements() )
        {
            changedDiagram = removeAllUnobservableNodes(changedDiagram);
        }
        else
        {
            changedDiagram = removeUnnecessaryUnobservableNodes(changedDiagram);
        }
        if( isStopped() )
            return null;
        path.save(changedDiagram);
        return changedDiagram;
    }

    private Diagram removeUnnecessaryUnobservableNodes(Diagram diagram) throws Exception
    {
        Diagram changedDiagram = diagram;
        EModel emodel = changedDiagram.getRole(EModel.class);

        observableNodes = getAllObservableNodes(emodel);
        container = new Container();
        for( Node node : observableNodes )
        {
            if( isStopped() )
                return null;
            getNecessaryConnectedNodes(node);
        }

        container.check();
        removeNodes(changedDiagram, false);
        for( Node reactionNode : container.toSourceSink )
        {
            if( isStopped() )
                return null;
            putOutputSourceSink(changedDiagram, reactionNode);
        }
        return changedDiagram;
    }

    private Diagram removeAllUnobservableNodes(Diagram diagram) throws Exception
    {
        Diagram changedDiagram = diagram;
        EModel emodel = changedDiagram.getRole(EModel.class);

        observableNodes = getAllObservableNodes(emodel);
        List<NecessaryNode> observablesList = new ArrayList<>();
        Set<Node> reactions = new HashSet<>();
        for( Node node : observableNodes )
        {
            if( isStopped() )
                return null;
            container = new Container();
            NecessaryNode currentObservableNode = new NecessaryNode(node);
            observablesList.add(currentObservableNode);

            getNecessaryConnectedNodes(node);
            container.necessary.remove(node);
            container.toSourceSink.removeAll(reactions);
            container.check();

            for( Node reactionNode : container.toSourceSink )
            {
                if( isStopped() )
                    return null;
                boolean hasDependent = false;
                for( Edge edge : reactionNode.getEdges() )
                {
                    if( reactionNode.equals(edge.getOutput()) && !observableNodes.contains(edge.getInput()) )
                        container.necessary.remove(edge.getInput());
                    if( reactionNode.equals(edge.getInput()) && observableNodes.contains(edge.getOutput()) )
                        hasDependent = true;
                }
                if( !hasDependent )
                    currentObservableNode.needOutSourceSink = true;
            }

            for( Node necessaryNode : container.necessary )
            {
                if( isStopped() )
                    return null;
                if( isReaction(necessaryNode) )
                {
                    reactions.add(necessaryNode);
                    if( currentObservableNode.needInSourceSink )
                        continue;
                    boolean isDependent = necessaryNode.edges()
                            .anyMatch(edge -> necessaryNode.equals( edge.getOutput() ) && observableNodes.contains( edge.getInput() ) );
                    if( !isDependent )
                        currentObservableNode.needInSourceSink = true;
                }
                else if( observableNodes.contains(necessaryNode) )
                    currentObservableNode.addDependency(necessaryNode);
            }
        }

        removeNodes(changedDiagram, true);
        int reactionCounter = 0;
        for( NecessaryNode obsNode : observablesList )
        {
            if( isStopped() )
                return null;
            if( obsNode.needInSourceSink )
                putReaction(changedDiagram, null, obsNode.node, reactionCounter++);
            if( obsNode.needOutSourceSink )
                putReaction(changedDiagram, obsNode.node, null, reactionCounter++);
            if( obsNode.isDependent() )
            {
                Set<Node> alreadyConnected = new HashSet<>();
                for( Edge edge : obsNode.getInputEdges() )
                    alreadyConnected.addAll(getInputNodes(edge.getInput()));
                for( Node node : obsNode.dependFrom )
                {
                    if( alreadyConnected.contains(node) )
                        continue;
                    putReaction(changedDiagram, node, obsNode.node, reactionCounter++);
                }
            }
        }
        return changedDiagram;
    }

    private boolean isStopped()
    {
        if( !wasStopped && jobControl != null )
            wasStopped = jobControl.isStopped();
        return wasStopped;
    }

    private void removeNodes(Compartment compartment, boolean deleteAllUnobservable) throws Exception
    {
        SemanticController semanticController = Diagram.getDiagram(compartment).getType().getSemanticController();
        for( Node node : compartment.getNodes() )
        {
            if( node.getKernel() instanceof biouml.standard.type.Compartment )
            {
                removeNodes((Compartment)node, deleteAllUnobservable);
                continue;
            }
            Role role = node.getRole();
            if( role instanceof Variable && shouldRemove(node, deleteAllUnobservable) )
                semanticController.remove(node);
        }
        if( compartment.getNodes().length == 0 && shouldRemove(compartment, deleteAllUnobservable) )
            semanticController.remove(compartment);
    }

    private boolean shouldRemove(Node node, boolean deleteAllUnobservable)
    {
        return deleteAllUnobservable ? !observableNodes.contains(node) : !container.necessary.contains(node);
    }

    private void putReaction(Diagram diagram, Node from, Node to, int reactionCounter) throws Exception
    {
        Reaction reaction = new Reaction(null, DefaultSemanticController.generateUniqueNodeName(diagram, "reaction"));
        Node reactionNode = new Node(diagram, reaction.getName(), reaction);

        if( from == null && to == null )
            return;

        StringBuilder formula = new StringBuilder("rate_");
        formula.append(reactionCounter);
        if( from != null )
        {
            SpecieReference ref = new SpecieReference(reaction, reaction.getName() + ": " + from.getName() + " as reactant",
                    SpecieReference.REACTANT);
            ref.setSpecie(from.getName());
            reaction.put(ref);
            Edge edge = new Edge(diagram, ref.getName(), ref, from, reactionNode);
            diagram.put(edge);
            formula.append("*").append( from.getRole(VariableRole.class).getName());
        }
        if( to != null )
        {
            SpecieReference ref = new SpecieReference(reaction, reaction.getName() + ": " + to.getName() + " as product",
                    SpecieReference.PRODUCT);
            ref.setSpecie(to.getName());
            reaction.put(ref);
            Edge edge = new Edge(diagram, ref.getName(), ref, reactionNode, to);
            diagram.put(edge);
        }

        diagram.put(reactionNode);
        DiagramUtility.generateRoles(diagram, reactionNode);
        diagram.setNotificationEnabled(false);
        reaction.setFormula(formula.toString());
        diagram.setNotificationEnabled(true);
    }

    private void putOutputSourceSink(Diagram diagram, Node source) throws Exception
    {
        Compartment compartment = source.getCompartment();
        Reaction oldReaction = (Reaction)source.getKernel();
        List<SpecieReference> components = new ArrayList<>();
        for( SpecieReference sr : oldReaction )
        {
            if( sr.isProduct() )
                continue;
            components.add(sr);
        }
        Reaction newReaction = new Reaction(oldReaction.getOrigin(), oldReaction.getName());
        Node reactionNode = new Node(compartment, newReaction.getName(), newReaction);
        for( SpecieReference sr : components )
        {
            newReaction.put(sr);
            Node specieNode = diagram.findNode(sr.getSpecieName());
            if( specieNode == null )
            {
                log.log(Level.SEVERE, "Could not find species: " + sr.getSpecieName());
                continue;
            }
            Edge edge = new Edge(compartment, sr.getName(), sr, specieNode, reactionNode);
            compartment.put(edge);
        }
        reactionNode.setLocation(source.getLocation());
        compartment.put(reactionNode);
        DiagramUtility.generateRoles(diagram, reactionNode);
        diagram.setNotificationEnabled(false);
        newReaction.setFormula(oldReaction.getFormula());
        diagram.setNotificationEnabled(true);
    }

    private void getNecessaryConnectedNodes(Node node)
    {
        if( isStopped() )
            return;
        if( container.necessary.contains(node) && !observableNodes.contains(node) )
            return;
        container.addNode(node);
        for( Edge edge : node.getEdges() )
        {
            Node output = edge.getOutput();
            Set<Node> inputNodes;
            if( node.equals(output) )
            {
                Node input = edge.getInput();
                container.addNode(input);
                inputNodes = getInputNodes(input);
                inputNodes.removeAll(container.necessary);
                for( Node inputNode : inputNodes )
                {
                    if( isStopped() )
                        return;
                    if( !observableNodes.contains(inputNode) )
                        getNecessaryConnectedNodes(inputNode);
                    else
                        container.addNode(inputNode);
                }
            }
            else if( isReaction(output) )
            {
                container.addToSourceSink(output);
                inputNodes = getInputNodes(output);
                inputNodes.removeAll(container.necessary);
                for( Node inputNode : inputNodes )
                {
                    if( isStopped() )
                        return;
                    if( !observableNodes.contains(inputNode) && !observableNodes.contains(node) )
                        getNecessaryConnectedNodes(inputNode);
                }
            }
        }
    }

    private Set<Node> getInputNodes(Node node)
    {
        return isReaction( node ) ? node.edges().filter( e -> node.equals( e.getOutput() ) ).map( Edge::getInput ).toSet()
                : new HashSet<>();
    }

    private static boolean isReaction(DiagramElement de)
    {
        Base kernel = de.getKernel();
        return kernel instanceof Reaction || ( kernel instanceof Stub && "reaction".equals(kernel.getType()) );
    }

    private Set<Node> getAllObservableNodes(EModel emodel)
    {
        Set<Node> result = new HashSet<>();
        Set<Equation> equations = getChosenEquations(emodel);
        for( Equation eq : equations )
            result.addAll(getObservableNodes(eq, emodel));
        return result;
    }

    private Set<Node> getObservableNodes(Equation equation, EModel emodel)
    {
        Set<Node> result = new HashSet<>();
        String formula = equation.getFormula();
        Set<String> variableNames = new HashSet<>();

        String delimiters = " ()+-/%*^";
        StringTokenizer tokens = new StringTokenizer(formula.trim(), delimiters, false);
        while( tokens.hasMoreTokens() )
        {
            String token = tokens.nextToken();
            if( token.equals(" ") )
                continue;
            if( token.startsWith("$") )
                variableNames.add(token);
        }
        for( String varName : variableNames )
        {
            Variable var = emodel.getVariable(varName);
            if( var instanceof VariableRole )
                result.add((Node) ( (VariableRole)var ).getDiagramElement());
        }

        return result;
    }

    private Set<Equation> getChosenEquations(EModel emodel)
    {
        Set<Equation> result = new HashSet<>();
        Set<String> names = new HashSet<>();
        String[] equationNames = parameters.getElementNames();
        for( String name : equationNames )
            names.add(name.substring(0, name.indexOf(" (variable:")));
        for( Equation eq : emodel.getEquations() )
        {
            DiagramElement de = eq.getDiagramElement();
            if( de instanceof Edge || isReaction(de) )
                continue;
            if( names.contains(de.getName()) )
                result.add(eq);
        }
        return result;
    }

    static class Container
    {
        Set<Node> necessary = new HashSet<>();
        Set<Node> toSourceSink = new HashSet<>();

        public void addNode(Node node)
        {
            this.necessary.add(node);
        }
        public void addToSourceSink(Node toSourceSinkNode)
        {
            this.toSourceSink.add(toSourceSinkNode);
        }
        public void check()
        {
            toSourceSink.removeAll(necessary);
        }
    }

    static class NecessaryNode
    {
        Node node;
        boolean needOutSourceSink = false;
        boolean needInSourceSink = false;
        Set<Node> dependFrom = new HashSet<>();

        public NecessaryNode(Node node)
        {
            this.node = node;
        }
        public void addDependency(Node node)
        {
            dependFrom.add(node);
        }
        public boolean isDependent()
        {
            return !dependFrom.isEmpty();
        }
        public Set<Edge> getInputEdges()
        {
            return node.edges().filter( e -> node.equals( e.getOutput() ) ).toSet();
        }
    }
}
