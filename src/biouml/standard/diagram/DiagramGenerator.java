package biouml.standard.diagram;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.DefaultSemanticController;
import biouml.model.Diagram;
import biouml.model.DiagramElementGroup;
import biouml.model.Edge;
import biouml.model.InitialElementProperties;
import biouml.model.Node;
import biouml.model.SubDiagram.PortOrientation;
import biouml.model.dynamics.Assignment;
import biouml.model.dynamics.Constraint;
import biouml.model.dynamics.EModel;
import biouml.model.dynamics.Equation;
import biouml.model.dynamics.Event;
import biouml.model.dynamics.SimpleTableElement;
import biouml.model.dynamics.State;
import biouml.model.dynamics.Transition;
import biouml.model.dynamics.VariableRole;
import biouml.plugins.sbgn.EquivalenceOperatorProperties;
import biouml.plugins.sbgn.LogicalOperatorProperties;
import biouml.plugins.sbgn.PhenotypeProperties;
import biouml.plugins.sbgn.SBGNPropertyConstants;
import biouml.standard.type.Reaction;
import biouml.standard.type.SpecieReference;
import biouml.standard.type.Stub;
import biouml.standard.type.Stub.Note;
import biouml.standard.type.Substance;
import biouml.standard.type.Type;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;

/**
 * Class for models creation for test purposes
 */
public class DiagramGenerator
{
    private final Diagram diagram;

    public DiagramGenerator(String name) throws Exception
    {
        this(name, null);
    }

    public DiagramGenerator(String name, DataCollection collection) throws Exception
    {
        diagram = new Diagram(null, new Stub(null, name), new PathwaySimulationDiagramType());
        diagram.setRole(new EModel(diagram));
    }

    public DiagramGenerator(Diagram diagram)
    {
        if( ! ( diagram.getType() instanceof PathwaySimulationDiagramType ) )
            throw new IllegalArgumentException("Only PathwaySimulationDiagramType diagrams allowed");

        if( diagram.getRole() == null || ! ( diagram.getRole() instanceof EModel ) )
            throw new IllegalArgumentException("Missing or incorrect role in diagram");
        this.diagram = diagram;
    }

    public Diagram getDiagram()
    {
        return diagram;
    }

    public EModel getEModel()
    {
        return (EModel)diagram.getRole();
    }

    public Node createSpecies(String name, double value) throws Exception
    {
        Node node = new Node(diagram, new Substance(null, name));
        node.setRole(new VariableRole(null, node, value));
        diagram.put(node);
        return node;
    }

    public Node createEquation(String variable, String formula, String type) throws Exception
    {
        Node node = new Node(diagram, new Stub(null, "equation_" + variable, Type.MATH_EQUATION));
        Equation eq = new Equation(node, type, variable, formula);
        node.setRole(eq);
        diagram.put(node);
        return node;
    }

    public Node createConstraint(String name, String formula, String message) throws Exception
    {
        Node node = new Node(diagram, new Stub(null, name, Type.MATH_CONSTRAINT));
        Constraint constraint = new Constraint(node);
        constraint.setFormula(formula);
        constraint.setMessage(message);
        node.setRole(constraint);
        diagram.put(node);
        return node;
    }

    public Node createEvent(String name, String trigger, Assignment assignment) throws Exception
    {
        Node node = new Node(diagram, new Stub(diagram, name));
        Event event = new Event(node);
        event.clearAssignments(false);
        event.setTrigger(trigger);
        event.addEventAssignment(assignment, true);
        node.setRole(event);
        diagram.put(node);
        return node;
    }

    public Node createState(String name, Assignment entryAssignment, Assignment exitAssignment) throws Exception
    {
        return createState(name, entryAssignment, exitAssignment, false);
    }

    public Node createState(String name, Assignment entryAssignment, Assignment exitAssignment, boolean isStart) throws Exception
    {
        List<Assignment> entryAssignmentList = new ArrayList<>();
        if( entryAssignment != null )
            entryAssignmentList.add(entryAssignment);

        List<Assignment> exitAssignmentList = new ArrayList<>();
        if( exitAssignment != null )
            exitAssignmentList.add(exitAssignment);

        return createState(name, entryAssignmentList, exitAssignmentList, isStart);
    }

    public Node createState(String name, List<Assignment> entryAssignment, List<Assignment> exitAssignment, boolean isStart)
            throws Exception
    {
        Node node = new Node(diagram, new Stub(diagram, name, Type.MATH_STATE));
        State state = new State(node);
        state.setStart(isStart);
        if( entryAssignment != null )
        {
            for( Assignment assignment : entryAssignment )
                state.addOnEntryAssignment(assignment, false);
        }
        if( exitAssignment != null )
        {
            for( Assignment assignment : exitAssignment )
                state.addOnExitAssignment(assignment, false);
        }
        node.setRole(state);
        diagram.put(node);
        return node;
    }

    public Node createState(String name, List<Assignment> entryAssignment, List<Assignment> exitAssignment) throws Exception
    {
        return createState(name, entryAssignment, exitAssignment, false);
    }

    public Edge createTransition(String name, Node from, Node to, String when, String after) throws Exception
    {
        Edge edge = new Edge(diagram, new Stub(diagram, name, Type.MATH_TRANSITION), from, to);
        Transition transition = new Transition(edge);
        if( when != null )
            transition.setWhen(when);
        if( after != null )
            transition.setAfter(after);
        edge.setRole(transition);
        diagram.put(edge);
        return edge;
    }

    public DiagramElementGroup createReaction(String formula, List<SpecieReference> components) throws Exception
    {
        return createReaction(diagram, DefaultSemanticController.generateReactionName(diagram), formula, components);
    }

    public DiagramElementGroup createReaction(Diagram diagram, String name, String formula, List<SpecieReference> components)
            throws Exception
    {
        Reaction prototype = new Reaction(null, name);
        prototype.setFormula(formula);
        prototype.setSpecieReferences(StreamEx.of(components).toArray(SpecieReference[]::new));
        DiagramElementGroup reactionElements = diagram.getType().getSemanticController().createInstance(diagram, Reaction.class,
                new Point(0, 0), prototype);
        reactionElements.putToCompartment();
        return reactionElements;
    }

    public DiagramElementGroup createLogicalOperator(Diagram diagram, String name, String type, String reactionName,
            List<String> components) throws Exception
    {
        LogicalOperatorProperties lop = new LogicalOperatorProperties(diagram, name + "_" + type);
        lop.setReactionName(reactionName);
        lop.getProperties().add(new DynamicProperty(SBGNPropertyConstants.SBGN_LOGICAL_OPERATOR_PD, String.class, type));
        lop.setNodeNames(components.toArray(new String[0]));
        DiagramElementGroup elements = lop.doCreateElements(diagram, new Point(0, 0), null);
        elements.putToCompartment();
        return elements;
    }

    public DiagramElementGroup createPhenotype(Diagram diagram, String name, List<String> components) throws Exception
    {
        PhenotypeProperties phenProps = new PhenotypeProperties(diagram, name);
        phenProps.setNodeNames(components.toArray(new String[0]));
        DiagramElementGroup elements = phenProps.doCreateElements(diagram, new Point(0, 0), null);
        elements.putToCompartment();
        return elements;
    }

    public DiagramElementGroup createEquivalence(Diagram diagram, String name, String superTypeNode, List<String> subTypeNodes)
            throws Exception
    {
        EquivalenceOperatorProperties eop = new EquivalenceOperatorProperties(diagram, name);
        eop.setMainNodeName(superTypeNode);
        eop.setNodeNames(subTypeNodes.toArray(new String[0]));
        DiagramElementGroup elements = eop.doCreateElements(diagram, new Point(0, 0), null);
        elements.putToCompartment();
        return elements;
    }

    public DiagramElementGroup createReaction(String formula, SpecieReference ... components) throws Exception
    {
        return createReaction(formula, StreamEx.of(components).toList());
    }

    public SpecieReference createSpeciesReference(Node node, String role) throws Exception
    {
        if( ! ( node.getRole() instanceof VariableRole ) )
            throw new Exception("Wrong node");

        String specieName = node.getName();
        SpecieReference specieReference = new SpecieReference(null, specieName, role);
        specieReference.setSpecie(specieName);
        return specieReference;
    }

    public void createPort(String variableName, String type) throws Exception
    {
        String name = DefaultSemanticController.generateUniqueNodeName(diagram, "port");
        Node node = new Node(diagram, Stub.ConnectionPort.createPortByType(diagram, name, type));
        node.getAttributes().add(new DynamicProperty(PortOrientation.ORIENTATION_ATTR, PortOrientation.class, PortOrientation.RIGHT));
        node.getAttributes().add(new DynamicProperty(Stub.ConnectionPort.VARIABLE_NAME_ATTR, String.class, variableName));
        diagram.put(node);
    }

    public Node createBus(Diagram diagram, String name, boolean directed) throws Exception
    {
        SimpleBusProperties busProps = new SimpleBusProperties(diagram);
        busProps.setName(name);
        busProps.setDirected(directed);
        DiagramElementGroup elements = busProps.createElements(diagram, new Point(0, 0), null);
        elements.putToCompartment();
        return (Node)elements.get(0);
    }

    public void createNote(Diagram diagram, String name, List<String> nodeList) throws Exception
    {
        CreatorElementWithName controller = (CreatorElementWithName)diagram.getType().getSemanticController();
        InitialElementProperties iep = (InitialElementProperties)controller.getPropertiesByType(diagram, Type.TYPE_NOTE, null);
        DiagramElementGroup noteElements = controller.createInstance(diagram, Note.class, name, new Point(0, 0), iep);
        Node note = (Node)noteElements.getElement();
        for( String nodeName : nodeList )
        {
            Node node = (Node)diagram.findNode(nodeName);
            if( node == null )
                continue;

            Edge edge = new Edge(new Stub.NoteLink(null, nodeName), note, node);
            noteElements.add(edge);
        }

        noteElements.putToCompartment();
    }

    public void createTable(Diagram diagram, DataElementPath path, String name)
    {
        SimpleTableElementProperties steb = new SimpleTableElementProperties(name);
        SimpleTableElement elem = steb.getElement();

        CreatorElementWithName controller = (CreatorElementWithName)diagram.getType().getSemanticController();
        elem.setTablePath(path);

        DiagramElementGroup elements = controller.createInstance(diagram, SimpleTableElementProperties.class, name, null, steb);

        elements.putToCompartment();

    }

}
