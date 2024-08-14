package ru.biosoft.analysis.diagram;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramType;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.standard.state.State;
import biouml.standard.type.DiagramInfo;
import biouml.standard.type.Stub;
import biouml.workbench.graph.DiagramToGraphTransformer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.graph.ForceDirectedLayouter;
import ru.biosoft.graph.Graph;
import ru.biosoft.graph.HierarchicLayouter;
import ru.biosoft.graph.Layouter;
import ru.biosoft.graph.PathwayLayouter;
import ru.biosoft.util.DPSUtils;

@ClassIcon ( "resources/CompareDiagrams.png" )
public class DiagramGraphCompareAnalysis extends AnalysisMethodSupport<DiagramGraphCompareParameters>
{
    private static final String PROTOTYPES_INFO = "PrototypesInfo";

    public DiagramGraphCompareAnalysis(DataCollection<?> origin, String name)
    {
        super(origin, name, new DiagramGraphCompareParameters());
    }

    public static final int DOWN = -1;
    public static final int UP = 1;
    public static final int BOTH = 0;

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        super.validateParameters();

        Diagram diagram1 = (Diagram)parameters.getInputPath1().optDataElement();
        Diagram diagram2 = (Diagram)parameters.getInputPath2().optDataElement();
        if( diagram1 == null )
            throw new IllegalArgumentException( "Path to the first diagram must be specified" );
        if( diagram2 == null )
            throw new IllegalArgumentException( "Path to the second diagram must be specified" );
        if( !diagram1.getType().getClass().equals( diagram2.getType().getClass() ) )
            throw new IllegalArgumentException( "Diagrams must have the same type" );

        String nodeName1 = parameters.getStartNodeId1();
        String nodeName2 = parameters.getStartNodeId2();
        if( nodeName1 == null || nodeName1.isEmpty() || nodeName2 == null || nodeName2.isEmpty() )
            throw new IllegalArgumentException( "Start node must be selected" );
        Node node1 = null;
        Node node2 = null;
        try
        {
            node1 = diagram1.findNode(nodeName1);
            node2 = diagram2.findNode(nodeName2);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
        if( node1 == null )
            throw new IllegalArgumentException("Can't find node: " + parameters.getStartNodeName1());
        if( node2 == null )
            throw new IllegalArgumentException("Can't find node: " + parameters.getStartNodeName2());
    }

    @Override
    public Diagram justAnalyzeAndPut() throws Exception
    {
        Diagram d1 = parameters.getInputPath1().getDataElement(Diagram.class);
        Diagram d2 = parameters.getInputPath2().getDataElement(Diagram.class);
        int depthOfSearch = parameters.getDepth();
        int direction = parameters.getDirection();
        boolean needLayout = parameters.isNeedLayout();
        Graph defaultGraph = null;

        DataElementPath path = parameters.getOutputPath();
        String outputName = path.getName();
        String diagramName = outputName.substring(Math.max(0, outputName.lastIndexOf('\\')));
        DiagramType type = d1.getType();
        Diagram outputDiagram = type.createDiagram( path.optParentCollection(), diagramName, new DiagramInfo( null, diagramName ) );

        //actually we have already check for null in validateParameters
        @SuppressWarnings ( "null" )
        LinkedNode startNode1 = new LinkedNode( d1.findNode( parameters.getStartNodeId1() ), 0, direction, null );
        @SuppressWarnings ( "null" )
        LinkedNode startNode2 = new LinkedNode( d2.findNode( parameters.getStartNodeId2() ), 0, direction, null );
        Set<String> ignore = new HashSet<>();
        getLinkedNodes( startNode1, ignore, depthOfSearch, direction, 0 );
        ignore.clear();
        getLinkedNodes( startNode2, ignore, depthOfSearch, direction, 0 );

        Set<LinkedNode> ignored1 = new HashSet<>();
        Set<LinkedNode> ignored2 = new HashSet<>();

        boolean notificationEnabled = outputDiagram.isNotificationEnabled();
        outputDiagram.setNotificationEnabled(true);
        Node node = createNodeWithTwoPrototypes(outputDiagram, startNode1, startNode2, null);
        compareLinked(outputDiagram, node, startNode1, startNode2, ignored1, ignored2);
        if( needLayout )
            defaultGraph = layoutDiagram(outputDiagram, null);

        State state = new State( outputDiagram.getOrigin(), outputDiagram, "Missed from diagram#1: " + d1.getName() );
        outputDiagram.addState(state);
        outputDiagram.setStateEditingMode(state);
        processMissedLinked(outputDiagram, ignored1, null, '1');
        if( needLayout )
            layoutDiagram(outputDiagram, defaultGraph);
        outputDiagram.setCurrentStateName(Diagram.NON_STATE);

        state = new State( outputDiagram.getOrigin(), outputDiagram, "Missed from diagram#2: " + d2.getName() );
        outputDiagram.addState(state);
        outputDiagram.setStateEditingMode(state);
        processMissedLinked(outputDiagram, ignored2, null, '2');
        if( needLayout )
            layoutDiagram(outputDiagram, defaultGraph);
        outputDiagram.setCurrentStateName(Diagram.NON_STATE);

        outputDiagram.setNotificationEnabled(notificationEnabled);

        path.save(outputDiagram);
        return outputDiagram;
    }

    private void processMissedLinked(@Nonnull Diagram diagram, Set<LinkedNode> missed, Node currentNode, char diagramNumber) throws Exception
    {
        for( LinkedNode linkedN : missed )
        {
            Node prevNode = currentNode == null ? diagram.findNode( linkedN.getPreviousNode().getCompleteName() ) : currentNode;
            Node node;
            if( ( node = diagram.findNode( linkedN.getCompleteName() ) ) == null )
            {
                node = createNodeWithSinglePrototype(diagram, diagramNumber, linkedN);
                processMissedLinked(diagram, linkedN.getNextNodes(), node, diagramNumber);
            }
            if( prevNode != null )
            {
                String id = createEdgeName( prevNode.getName(), node.getName(), linkedN.getCurrentDepth(), linkedN.getDirection() );
                Edge edge = new Edge(diagram, id, new Stub(null, id, "edge"), prevNode, node);
                edge.getAttributes().add( new DynamicProperty( PROTOTYPES_INFO, String.class,
                        "From diagram#" + diagramNumber + ": " + linkedN.reactionNode.getName() ) );
                diagram.put(edge);
            }
        }
    }

    /**
     * Compares next linked nodes from given linked nodes and put equals nodes on given diagram.
     * Missed in comparison linked nodes will be saved in ignored1 and ignored2 lists.
     */
    private void compareLinked(@Nonnull Diagram diagram, Node currentNode, LinkedNode startNode1, LinkedNode startNode2,
            Set<LinkedNode> ignored1, Set<LinkedNode> ignored2) throws Exception
    {
        Set<LinkedNode> linkedList1 = startNode1.getNextNodes();
        Set<LinkedNode> linkedList2 = startNode2.getNextNodes();

        if( linkedList1.size() == 0 )
        {
            if( linkedList2.size() != 0 )
                ignored2.addAll(linkedList2);
            else
                return;
        }
        else if( linkedList2.size() == 0 )
            ignored1.addAll(linkedList1);

        Set<LinkedNode> ignore = new HashSet<>();
        L1: for( LinkedNode linkedN1 : linkedList1 )
        {
            for( LinkedNode linkedN2 : linkedList2 )
            {
                if( ignore.contains(linkedN2) )
                    continue;
                if( LinkedNode.areSameLevel(linkedN1, linkedN2) && areAnalogues(linkedN1.getNode(), linkedN2.getNode()) )
                {
                    Node node = createNodeWithTwoPrototypes(diagram, linkedN1, linkedN2, currentNode);
                    compareLinked(diagram, node, linkedN1, linkedN2, ignored1, ignored2);
                    ignore.add(linkedN2);
                    continue L1;
                }
            }
            ignored1.add(linkedN1);
        }
        for( LinkedNode linkedN2 : linkedList2 )
        {
            if( !ignore.contains(linkedN2) )
                ignored2.add(linkedN2);
        }
    }

    private boolean areAnalogues(Node node1, Node node2)
    {
        return parameters.getComparator().areAnalogues(node1, node2);
    }

    private Node createNodeWithTwoPrototypes(@Nonnull Diagram diagram, LinkedNode linkedN1, LinkedNode linkedN2, Node currentNode)
            throws Exception
    {
        Node originalNode1 = linkedN1.getNode();
        Node node = originalNode1.clone( diagram, originalNode1.getName() );
        copyAttributes( originalNode1, node );
        copyAttributes(linkedN2.getNode(), node);
        node.getAttributes().add( new DynamicProperty( PROTOTYPES_INFO, String.class,
                "From diagram1: " + originalNode1.getName() + ". " + "From diagram2: " + linkedN2.getNode().getName() ) );
        node.setShapeSize( originalNode1.getShapeSize() );
        diagram.put(node);

        if( currentNode != null )
        {
            String id = createEdgeName( currentNode.getName(), node.getName(), linkedN1.getCurrentDepth(), linkedN1.getDirection() );
            Edge edge = new Edge(diagram, id, new Stub(null, id, "edge"), currentNode, node);
            edge.getAttributes().add( new DynamicProperty( PROTOTYPES_INFO, String.class,
                    "From diagram#1: " + linkedN1.reactionNode.getName() + ". " + "From diagram#2: " + linkedN2.reactionNode.getName() ) );
            diagram.put(edge);
        }
        return node;
    }

    private Node createNodeWithSinglePrototype(@Nonnull Diagram diagram, char diagramNumber, LinkedNode linkedN) throws Exception
    {
        Node originalNode = linkedN.getNode();
        Node node = originalNode.clone( diagram, originalNode.getName() );
        copyAttributes(originalNode, node);
        node.getAttributes().add( new DynamicProperty( PROTOTYPES_INFO, String.class,
                "From diagram#" + diagramNumber + ": " + originalNode.getName() ) );
        node.setShapeSize( originalNode.getShapeSize() );
        diagram.put( node );
        return node;
    }

    private void copyAttributes(Node from, Node to)
    {
        DynamicPropertySet dps = to.getAttributes();
        for( DynamicProperty dp : from.getAttributes() )
        {
            if( dps.getProperty(dp.getName()) != null )
                continue;
            DynamicProperty newDP = new DynamicProperty(dp.getName(), dp.getClass(), dp.getValue());
            newDP.setHidden(dp.isHidden());
            if( DPSUtils.isTransient(dp) )
                DPSUtils.makeTransient(newDP);
            dps.add(newDP);
        }
    }

    private void getLinkedNodes(LinkedNode startLNode, Set<String> ignoreSet, int remainingDepth, int direction, int currentDepth)
    {
        if( remainingDepth == 0 )
            return;

        Node startNode = startLNode.getNode();
        String completeName = startLNode.getCompleteName();

        //skip nodes from ignore list
        if( ignoreSet.contains( completeName ) )
            return;
        if( !completeName.isEmpty() )
            ignoreSet.add( startNode.getCompleteNameInDiagram() );

        currentDepth++;
        for( Edge edge1 : startNode.getEdges() )
        {
            Node startOutput = edge1.getOutput();
            Node startInput = edge1.getInput();
            if( direction == UP )
            {
                if( !startNode.equals( startOutput ) )
                    continue;
                for( Edge edge2 : startInput.getEdges() )
                {
                    Node output = edge2.getOutput();
                    Node input = edge2.getInput();
                    if( startInput.equals( output ) )
                        addNextLinked( startLNode, ignoreSet, remainingDepth, currentDepth, direction, UP, startInput, input );
                }
            }
            else if( direction == DOWN )
            {
                if( !startNode.equals( startInput ) )
                    continue;
                for( Edge edge2 : startOutput.getEdges() )
                {
                    Node output = edge2.getOutput();
                    Node input = edge2.getInput();
                    if( startOutput.equals( input ) )
                        addNextLinked( startLNode, ignoreSet, remainingDepth, currentDepth, direction, DOWN, startOutput, output );
                }
            }
            else
            {
                if( startNode.equals( startInput ) )
                {
                    for( Edge edge2 : startOutput.getEdges() )
                    {
                        if( edge2.equals( edge1 ) )
                            continue;
                        Node output = edge2.getOutput();
                        Node input = edge2.getInput();
                        if( startOutput.equals( input ) )
                            addNextLinked( startLNode, ignoreSet, remainingDepth, currentDepth, direction, BOTH, startOutput, output );
                        else
                            addNextLinked( startLNode, ignoreSet, remainingDepth, currentDepth, direction, BOTH, startOutput, input );
                    }
                }
                else if( startNode.equals( startOutput ) )
                {
                    for( Edge edge2 : startInput.getEdges() )
                    {
                        if( edge2.equals( edge1 ) )
                            continue;
                        Node output = edge2.getOutput();
                        Node input = edge2.getInput();
                        if( startInput.equals( output ) )
                            addNextLinked( startLNode, ignoreSet, remainingDepth, currentDepth, direction, BOTH, startInput, input );
                        else
                            addNextLinked( startLNode, ignoreSet, remainingDepth, currentDepth, direction, BOTH, startInput, output );
                    }
                }
            }
        }
        currentDepth--;
    }

    private void addNextLinked(LinkedNode startLNode, Set<String> ignoreSet, int remainingDepth, int currentDepth, int searchDirection,
            int currentDirection, Node reactionNode, @Nonnull Node currentNode)
    {
        LinkedNode newLinkedNode = new LinkedNode( currentNode, currentDepth, currentDirection, startLNode );
        newLinkedNode.setReactionNode( reactionNode );
        startLNode.addNextLinked( newLinkedNode );
        getLinkedNodes( newLinkedNode, ignoreSet, remainingDepth - 1, searchDirection, currentDepth );
    }

    private String createEdgeName(String inNodeName, String outNodeName, int depth, String direction)
    {
        return "Link " + inNodeName + " with " + outNodeName + " at depth " + depth + " and direction " + direction;
    }

    private static Graph layoutDiagram(Diagram diagram, Graph defaultGraph)
    {
        Layouter layouter;
        if( diagram.getSize() < 1000 )
        {
            layouter = new HierarchicLayouter();
            ( (HierarchicLayouter)layouter ).setVerticalOrientation(true);
        }
        else
        {
            layouter = new ForceDirectedLayouter();
        }
        Graph graph = DiagramToGraphTransformer.generateGraph(diagram, null);
        PathwayLayouter pathwayLayouter = new PathwayLayouter(layouter);
        pathwayLayouter.doLayout(graph, null);
        if( defaultGraph != null )
        {
            DiagramToGraphTransformer.reApplyLayout(defaultGraph, graph);
            for( DiagramElement de : diagram )
            {
                if( de instanceof Edge && !de.isFixed() )
                    diagram.getType().getSemanticController().recalculateEdgePath((Edge)de);
                de.setFixed(false);
            }
        }
        DiagramToGraphTransformer.applyLayout(graph, diagram);
        return graph;
    }

    public static class LinkedNode
    {
        private @Nonnull Node node;
        LinkedNode previousNode;
        Node reactionNode;
        Set<LinkedNode> nextLinkedNodes = new HashSet<>();
        int currentDepth = 1;
        int direction;

        public LinkedNode(@Nonnull Node node, int currentDepth, int direction, LinkedNode previousNode)
        {
            this.node = node;
            this.currentDepth = currentDepth;
            this.direction = direction;
            this.previousNode = previousNode;
        }

        public static boolean areSameLevel(LinkedNode node1, LinkedNode node2)
        {
            return node1.currentDepth == node2.currentDepth && node1.direction == node2.direction;
        }
        public @Nonnull Node getNode()
        {
            return node;
        }
        public @Nonnull String getCompleteName()
        {
            String result = node.getCompleteNameInDiagram();
            return result == null ? "" : result;
        }
        public int getCurrentDepth()
        {
            return currentDepth;
        }
        public LinkedNode getPreviousNode()
        {
            return previousNode;
        }
        public void addNextLinked(LinkedNode nextNode)
        {
            nextLinkedNodes.add(nextNode);
        }
        public Set<LinkedNode> getNextNodes()
        {
            return nextLinkedNodes;
        }
        public void setReactionNode(Node reactionNode)
        {
            this.reactionNode = reactionNode;
        }
        public String getDirection()
        {
            switch( direction )
            {
                case UP:
                    return "UP";
                case DOWN:
                    return "DOWN";
                default:
                    return "BOTH";
            }
        }
        @Override
        public boolean equals(Object obj)
        {
            if( ! ( obj instanceof LinkedNode ) )
                return false;
            LinkedNode linkedNode = (LinkedNode)obj;
            LinkedNode previousNode2 = linkedNode.getPreviousNode();
            if( ( previousNode2 == null && previousNode != null ) || ( previousNode2 != null && previousNode == null ) )
                return false;
            return previousNode != null ? linkedNode.getNode().equals(node) && areSameLevel(linkedNode, this)
                    && previousNode.equals(previousNode2) : true;
        }
        @Override
        public int hashCode()
        {
            int value = previousNode != null ? previousNode.hashCode() : 0;
            if( reactionNode != null )
                value += reactionNode.hashCode();
            return node.hashCode() + value + currentDepth + direction;
        }
    }
}
