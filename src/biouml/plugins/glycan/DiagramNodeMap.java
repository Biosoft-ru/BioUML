package biouml.plugins.glycan;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import biouml.model.Edge;
import biouml.model.Node;

public class DiagramNodeMap
{
    public static final String GLYCAN_ATTR = "glycanStructure";

    private Set<LinkedNode> notMapped1;
    private Set<LinkedNode> notMapped2;
    private LinkedNode startNode1;
    private LinkedNode startNode2;
    private int depthOfSearch = 0;

    public DiagramNodeMap(int depthOfSearch)
    {
        this.depthOfSearch = depthOfSearch;
    }

    public void initFirstChain(Node startNode)
    {
        startNode1 = new LinkedNode(startNode.getAttributes().getValueAsString(GLYCAN_ATTR), 0, null);
        if( startNode2.glycanStructure.isEmpty() )
            throw new IllegalArgumentException("Wrong start node in the first chain: null or empty glycan structure");
        getLinkedNodes(startNode1, startNode, depthOfSearch, 0);
    }
    public void initSecondChain(Node startNode)
    {
        startNode2 = new LinkedNode(startNode.getAttributes().getValueAsString(GLYCAN_ATTR), 0, null);
        if( startNode2.glycanStructure.isEmpty() )
            throw new IllegalArgumentException("Wrong start node in the second chain: null or empty glycan structure");
        getLinkedNodes(startNode2, startNode, depthOfSearch, 0);
    }
    public void compare()
    {
        notMapped1 = new HashSet<>();
        notMapped2 = new HashSet<>();
        compareLinked(startNode1, startNode2, notMapped1, notMapped2);
    }

    /**
     * Compares next linked nodes from given linked nodes and put equals nodes on given diagram.
     * Missed in comparison linked nodes will be saved in ignored1 and ignored2 lists.
     */
    private void compareLinked(LinkedNode startNode1, LinkedNode startNode2, Set<LinkedNode> ignored1, Set<LinkedNode> ignored2)
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
                if( LinkedNode.areSameLevel(linkedN1, linkedN2) && LinkedNode.areSameGlycan(linkedN1, linkedN2) )
                {
                    compareLinked(linkedN1, linkedN2, ignored1, ignored2);
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

    private void getLinkedNodes(LinkedNode startLNode, Node startNode, int remainingDepth, int currentDepth)
    {
        if( remainingDepth == 0 )
            return;

        for( Edge edge1 : startNode.getEdges() )
        {
            Node startOutput = edge1.getOutput();
            Node startInput = edge1.getInput();
            if( !startNode.equals(startInput) )
                continue;
            for( Edge edge2 : startOutput.getEdges() )
            {
                Node output = edge2.getOutput();
                Node input = edge2.getInput();
                if( startOutput.equals(input) )
                    addNextLinked(startLNode, remainingDepth, currentDepth + 1, startOutput, output);
            }
        }
    }

    private void addNextLinked(LinkedNode startLNode, int remainingDepth, int currentDepth, Node reactionNode, @Nonnull Node currentNode)
    {
        LinkedNode newLinkedNode = new LinkedNode(currentNode.getAttributes().getValueAsString(GLYCAN_ATTR), currentDepth, startLNode);
        if( newLinkedNode.glycanStructure.isEmpty() )
            return;
        newLinkedNode.setReactionNodeName(reactionNode.getName());
        startLNode.addNextLinked(newLinkedNode);
        getLinkedNodes(newLinkedNode, currentNode, remainingDepth - 1, currentDepth);
    }

    public Set<LinkedNode> getNotMapped1()
    {
        return notMapped1;
    }
    public Set<LinkedNode> getNotMapped2()
    {
        return notMapped2;
    }

    public static class LinkedNode
    {
        LinkedNode previousNode;
        String reactionNodeName = null;
        String glycanStructure = "";
        Set<LinkedNode> nextLinkedNodes = new HashSet<>();
        int currentDepth = 0;
        public LinkedNode(String glyStr, int currentDepth, @Nullable LinkedNode previousNode)
        {
            this.currentDepth = currentDepth;
            this.previousNode = previousNode;
            if( glyStr != null )
                glycanStructure = glyStr;
        }
        public String getGlycanStructure()
        {
            return glycanStructure;
        }
        public int getCurrentDepth()
        {
            return currentDepth;
        }
        public LinkedNode getPreviousNode()
        {
            return previousNode;
        }
        protected void addNextLinked(LinkedNode nextNode)
        {
            nextLinkedNodes.add(nextNode);
        }
        protected Set<LinkedNode> getNextNodes()
        {
            return nextLinkedNodes;
        }
        public String getReactionNodeName()
        {
            return reactionNodeName;
        }
        protected void setReactionNodeName(String reactionNodeName)
        {
            this.reactionNodeName = reactionNodeName;
        }

        public static boolean hasOneParent(LinkedNode lNode1, LinkedNode lNode2)
        {
            LinkedNode parent1 = lNode1.getPreviousNode();
            LinkedNode parent2 = lNode2.getPreviousNode();
            if( parent1 != null && parent2 != null )
                return areSameGlycan(parent1, parent2);
            return parent1 == null && parent2 == null;
        }
        public static boolean bySameReaction(LinkedNode lNode1, LinkedNode lNode2)
        {
            if( lNode1.reactionNodeName != null )
                return lNode1.reactionNodeName.equals(lNode2.reactionNodeName);
            return lNode2.reactionNodeName == null;
        }
        public static boolean areSameLevel(LinkedNode node1, LinkedNode node2)
        {
            return node1.currentDepth == node2.currentDepth;
        }
        public static boolean areSameGlycan(LinkedNode node1, LinkedNode node2)
        {
            return node1.glycanStructure.equals(node2.glycanStructure);
        }
        @Override
        public boolean equals(Object obj)
        {
            if( ! ( obj instanceof LinkedNode ) )
                return false;
            LinkedNode linkedNode = (LinkedNode)obj;
            return hasOneParent(linkedNode, this) && bySameReaction(linkedNode, this) && areSameLevel(linkedNode, this)
                    && areSameGlycan(linkedNode, this);
        }
        @Override
        public int hashCode()
        {
            int value = 0;
            if( reactionNodeName != null )
                value += reactionNodeName.hashCode();
            if( glycanStructure != null )
                value += glycanStructure.hashCode() * 31;
            return value + currentDepth;
        }
    }
}
