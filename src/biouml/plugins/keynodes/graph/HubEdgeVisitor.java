package biouml.plugins.keynodes.graph;

@FunctionalInterface
public interface HubEdgeVisitor<N>
{
    public void accept(HubEdge edge, N otherEnd, float weight);
}
