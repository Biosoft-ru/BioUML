package biouml.plugins.keynodes.graph;

import one.util.streamex.StreamEx;

/**
 * Abstract hub graph representation
 * @author lan
 *
 * @param <N> node representation: must be immutable object which has well-defined hashCode and equals
 * It's ok to use repeating objects or even {@link Void} if no specific representation is necessary
 */
public interface HubGraph<N>
{
    /**
     * Returns the stream of all nodes in this graph
     * 
     * @return the stream of all nodes in this graph
     */
    public StreamEx<N> nodes();
    
    public default StreamEx<N> startingNodes()
    {
        return nodes();
    }
    
    /**
     * Returns true if given node appears in the graph
     * 
     * @param node node to check
     * 
     * @return true if given node appears in the graph
     */
    public boolean hasNode(N node);
    
    /**
     * Returns true if given node can be intermediate (i.e. the graph search can go through it).
     * Check hasNode(node) first if you're not sure that the node is in graph
     * 
     * @param node node to check
     * 
     * @return true if given node is a intermediate node
     */
    public default boolean isIntermediate(N node)
    {
        return true;
    }
    
    /**
     * Calls visitor for each node directly adjacent to given start node in given direction
     * 
     * @param start the node to find the neighbors for
     * @param upstream if true, the upstream neighbors should be found, otherwise the downstream neighbors
     * @param visitor visitor which will be called for each node
     */
    public void visitEdges(N start, boolean upstream, HubEdgeVisitor<N> visitor);
}
