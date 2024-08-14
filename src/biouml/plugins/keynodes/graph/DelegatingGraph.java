package biouml.plugins.keynodes.graph;

import one.util.streamex.StreamEx;

abstract public class DelegatingGraph<N> implements HubGraph<N>
{
    protected final HubGraph<N> origin;
    
    public DelegatingGraph(HubGraph<N> origin)
    {
        this.origin = origin;
    }

    @Override
    public StreamEx<N> nodes()
    {
        return origin.nodes();
    }

    @Override
    public StreamEx<N> startingNodes()
    {
        return origin.startingNodes();
    }

    @Override
    public boolean hasNode(N node)
    {
        return origin.hasNode( node );
    }

    @Override
    public void visitEdges(N start, boolean upstream, HubEdgeVisitor<N> visitor)
    {
        origin.visitEdges( start, upstream, visitor );
    }
}
