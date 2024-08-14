package biouml.plugins.keynodes.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import one.util.streamex.StreamEx;

public class MemoryHubGraph<N> implements HubGraph<N>
{
    private final Map<N, MHNode<N>> nodes = new HashMap<>();

    private static class MHNode<N>
    {
        public List<HubRelation<N>> inputs = Collections.emptyList();
        public List<HubRelation<N>> outputs = Collections.emptyList();

        public MHNode()
        {
        }

        public void addInput(HubRelation<N> relation)
        {
            if(inputs.isEmpty()) inputs = new ArrayList<>();
            inputs.add(relation);
        }

        public void addOutput(HubRelation<N> relation)
        {
            if(outputs.isEmpty()) outputs = new ArrayList<>();
            outputs.add(relation);
        }

        public void compactify()
        {
            if(inputs instanceof ArrayList)
                ((ArrayList<?>)inputs).trimToSize();
            if(outputs instanceof ArrayList)
                ((ArrayList<?>)outputs).trimToSize();
        }

        @Override
        public String toString()
        {
            return "Inputs: "+inputs+";\nOutputs: "+outputs;
        }
    }

    public static class HubRelation<N>
    {
        private final N start, end;
        private final HubEdge edge;
        private final float weight;

        public HubRelation(N start, N end, HubEdge edge, float weight)
        {
            this.start = start;
            this.end = end;
            this.edge = edge;
            this.weight = weight;
        }

        public N getStart()
        {
            return start;
        }

        public N getEnd()
        {
            return end;
        }

        public HubEdge getEdge()
        {
            return edge;
        }

        public float getWeight()
        {
            return weight;
        }

        public N getNode(boolean upstream)
        {
            return upstream ? start : end;
        }

        @Override
        public String toString()
        {
            return start + "->" + end;
        }
    }

    public static <N> Collector<HubRelation<N>, ?, MemoryHubGraph<N>> toMemoryHub()
    {
        return toMemoryHub( MemoryHubGraph<N>::new );
    }

    protected static <N, T extends MemoryHubGraph<N>> Collector<HubRelation<N>, ?, T> toMemoryHub(Function<List<HubRelation<N>>, T> creator)
    {
        return Collectors.collectingAndThen( Collectors.toList(), creator );
    }

    protected MemoryHubGraph(List<HubRelation<N>> relations)
    {
        relations.forEach( this::addRelation );
        nodes.values().forEach( MHNode::compactify );
    }

    private void addRelation(HubRelation<N> relation)
    {
        nodes.computeIfAbsent( relation.getStart(), k -> new MHNode<>() ).addOutput( relation );
        nodes.computeIfAbsent( relation.getEnd(), k -> new MHNode<>() ).addInput( relation );
    }

    @Override
    public StreamEx<N> nodes()
    {
        return StreamEx.ofKeys( nodes );
    }

    public StreamEx<HubRelation<N>> edges()
    {
        return StreamEx.ofValues( nodes ).flatCollection(m->m.inputs);
    }

    @Override
    public void visitEdges(N start, boolean upstream, HubEdgeVisitor<N> visitor)
    {
        MHNode<N> node = nodes.get( start );
        if(node == null)
            return;
        for(HubRelation<N> relation : upstream ? node.inputs : node.outputs)
        {
            visitor.accept( relation.getEdge(), relation.getNode(upstream), relation.getWeight() );
        }
    }

    @Override
    public boolean hasNode(N node)
    {
        return nodes.containsKey( node );
    }
}
