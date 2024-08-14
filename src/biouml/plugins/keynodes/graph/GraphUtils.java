package biouml.plugins.keynodes.graph;

import gnu.trove.iterator.TObjectFloatIterator;
import gnu.trove.map.TObjectFloatMap;
import gnu.trove.map.hash.TObjectFloatHashMap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import one.util.streamex.StreamEx;

import biouml.plugins.keynodes.biohub.KeyNodesHub;
import ru.biosoft.access.biohub.BioHub;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.RelationType;

public class GraphUtils
{
    public static final double WEIGHT_PRECISION = 0.00005;

    public static <N> String toString(HubGraph<N> graph)
    {
        List<String> edges = new ArrayList<>();
        graph.nodes().forEach( from ->
            graph.visitEdges( from, true, (edge, to, w) ->
                edges.add( to + " -" + edge + "-> " + from + " (" + w + ")\n" )
            )
        );
        return StreamEx.of(edges).sorted().distinct().joining();
    }

    public static <N> TObjectFloatMap<N> getReachableNodesWithDistance(HubGraph<N> graph, N start, float maxWeight, boolean upStream)
    {
        TObjectFloatMap<N> addedNodes = new TObjectFloatHashMap<>();
        Queue<N> curNodes = new ArrayDeque<>();
        addedNodes.put(start, 0);
        curNodes.add(start);
        while(!curNodes.isEmpty())
        {
            N node = curNodes.poll();
            if( node != start && !graph.isIntermediate( node ) )
                continue;
            float weight = addedNodes.get(node);
            graph.visitEdges( node, upStream, (edge, otherEnd, w) -> {
                float newWeight = weight+w;
                if(newWeight <= maxWeight + WEIGHT_PRECISION)
                {
                    float nextNodeWeight = addedNodes.containsKey(otherEnd)?addedNodes.get(otherEnd):Float.MAX_VALUE;
                    if(newWeight < nextNodeWeight)
                    {
                        curNodes.add(otherEnd);
                        addedNodes.put(otherEnd, newWeight);
                    }
                }
            });
        }
        return addedNodes;
    }

    public static class FoundNode<N>
    {
        public final N node;
        public final HubEdge edge;
        public final FoundNode<N> prev;
        public final float length;

        public FoundNode(N node, HubEdge edge, FoundNode<N> prev, float length)
        {
            this.node = node;
            this.edge = edge;
            this.prev = prev;
            this.length = length;
        }

        public FoundNode(N start)
        {
            this.node = start;
            this.edge = null;
            this.prev = null;
            this.length = 0;
        }

        @Override
        public String toString()
        {
            if( this.prev == null )
                return this.node.toString();
            return this.node + "-[" + this.edge + "]->" + this.prev;
        }
    }

    public static <N> Collection<FoundNode<N>> getMinimalPaths(HubGraph<N> graph, N start, Collection<N> targets, float maxLength,
            boolean upstream)
    {
        if(!graph.hasNode( start ))
            return Collections.emptyList();
        Map<N, FoundNode<N>> foundNodes = new HashMap<>();
        List<FoundNode<N>> currentStart = Collections.singletonList( new FoundNode<>( start ) );

        while( !currentStart.isEmpty() )
        {
            List<FoundNode<N>> nextStart = new ArrayList<>();
            for(FoundNode<N> curNode : currentStart)
            {
                if(curNode.prev != null && !graph.isIntermediate( curNode.node ))
                    continue;
                float elementLength = curNode.length;
                graph.visitEdges( curNode.node, upstream, (edge, otherEnd, weight) ->
                {
                    float curLength = weight + elementLength;
                    if(curLength > maxLength)
                        return;
                    FoundNode<N> nextNode = foundNodes.get( otherEnd );
                    if(nextNode == null || nextNode.length > curLength)
                    {
                        nextNode = new FoundNode<>( otherEnd, edge, curNode, curLength );
                        foundNodes.put( otherEnd, nextNode );
                        nextStart.add( nextNode );
                    }
                });
            }
            currentStart = nextStart;
        }
        return StreamEx.of( targets ).map( foundNodes::get ).nonNull().toList();
    }

    public static <N> Element[] getDirectedReferences(HubGraph<N> tpMemHub, Element startElement, int maxLength, int direction,
            ElementConverter<N> converter)
    {
        N start = converter.toNode( startElement );
        if(!tpMemHub.hasNode( start ))
            return new Element[] {};
        if(maxLength == 0)
            return new Element[] {converter.fromNode( start )};
        TObjectFloatMap<N> reachableNodes = getReachableNodesWithDistance( tpMemHub, start, maxLength, direction == BioHub.DIRECTION_UP );
        List<Element> result = new ArrayList<>();
        for( TObjectFloatIterator<N> iter = reachableNodes.iterator(); iter.hasNext();  )
        {
            iter.advance();
            if( iter.key().equals( start ) )
                continue;
            Element element = converter.fromNode( iter.key() );
            if(element == null)
                continue;
            element.setLinkedLength(iter.value());
            result.add( element );
        }
        return result.toArray( new Element[result.size()] );
    }

    public static <N> List<Element[]> getDirectedPaths(Element key, Element[] targets, int maxLength, int direction, HubGraph<N> graph,
            ElementConverter<N> converter, KeyNodesHub<?> hub)
    {
        boolean upStream = direction == BioHub.DIRECTION_UP;

        N start = converter.toNode( key );
        Set<N> id2find = StreamEx.of( targets ).map( converter::toNode ).toSet();

        Collection<FoundNode<N>> minimalPaths = getMinimalPaths( graph, start, id2find, maxLength, upStream );
        return createPathFromFoundNodes( start, minimalPaths, direction, converter, hub );
    }

    public static <N> List<Element[]> createPathFromFoundNodes(N start, Collection<FoundNode<N>> minimalPaths, int direction,
            ElementConverter<N> converter, KeyNodesHub<?> hub)
    {
        List<Element[]> resultmain = new ArrayList<>();
        for( FoundNode<N> foundNode : minimalPaths )
        {
            boolean upStream = direction == BioHub.DIRECTION_UP;
            List<Element> result = new ArrayList<>();
            int len = 0;
            Element elReact = null;
            String prevRel = RelationType.REACTANT;
            while( foundNode.node != start )
            {
                Element el = converter.fromNode( foundNode.node );
                el.setLinkedLength( len );
                el.setLinkedDirection( direction );

                if( upStream )
                {
                    el.setRelationType( prevRel );
                    prevRel = foundNode.edge.getRelationType( false );
                }
                else
                    el.setRelationType( foundNode.edge.getRelationType( false ) );
                result.add( el );

                elReact = foundNode.edge.createElement( hub );
                elReact.setLinkedLength( len + 1 );
                elReact.setLinkedDirection( direction );
                elReact.setRelationType( foundNode.edge.getRelationType( true ) );
                result.add( elReact );
                foundNode = foundNode.prev;
                len += 2;
            }
            Element el = converter.fromNode( start );
            el.setLinkedLength( len );
            el.setLinkedDirection( direction );
            el.setRelationType( upStream ? prevRel : RelationType.REACTANT );
            result.add( el );
            propagateLinks( result, upStream );
            resultmain.add( result.toArray( new Element[result.size()] ) );
        }
        return resultmain;
    }

    public static void propagateLinks(List<Element> elements, boolean upStream)
    {
        StreamEx.of( elements ).forPairs(
                upStream ? (prev, cur) -> cur.setLinkedFromPath( prev.getPath() )
                        : (cur, next) -> cur.setLinkedFromPath( next.getPath() ) );
    }

    public static class PrevContainer<N>
    {
        public final HubEdge edge;
        public final FoundNodeEx<N> prev;
        public final float edgeWeight;
        public PrevContainer(HubEdge edge, float weight, FoundNodeEx<N> prev)
        {
            this.edge = edge;
            this.edgeWeight = weight;
            this.prev = prev;
        }
    }
    public static class FoundNodeEx<N>
    {
        public final N node;
        public final Set<PrevContainer<N>> prevSet = new HashSet<>();
        public float length = 0;
        public FoundNodeEx(N node, HubEdge edge, float weight, FoundNodeEx<N> prev)
        {
            this( node );
            prevSet.add( new PrevContainer<>( edge, weight, prev ) );
        }
        public FoundNodeEx(N start)
        {
            this.node = start;
        }
        public void addPrev(HubEdge edge, float weight, FoundNodeEx<N> prev)
        {
            prevSet.add( new PrevContainer<>( edge, weight, prev ) );
        }
    }
    /**
     * returns reaction elements for all directed paths
     */
    public static <N> List<Element> getAllReactionInDirectedPaths(Element key, Element[] targets, int maxLength, int direction,
            HubGraph<N> graph, ElementConverter<N> converter, KeyNodesHub<?> hub)
    {
        N start = converter.toNode( key );
        Set<N> id2find = StreamEx.of( targets ).map( converter::toNode ).toSet();

        List<Element> resultmain = new ArrayList<>();
        boolean upStream = direction == BioHub.DIRECTION_UP;
        Collection<FoundNodeEx<N>> allPaths = getAllPaths( graph, start, id2find, maxLength, upStream );
        for( FoundNodeEx<N> foundNode : allPaths )
            resultmain.addAll( getReactionElements( foundNode, start, direction, hub, maxLength, new HashSet<N>() ) );
        return resultmain;
    }
    private static <N> List<FoundNodeEx<N>> getAllPaths(HubGraph<N> graph, N start, Collection<N> targets, float maxLength,
            boolean upstream)
    {
        if( !graph.hasNode( start ) )
            return Collections.emptyList();
        Map<N, FoundNodeEx<N>> foundNodes = new HashMap<>();
        List<FoundNodeEx<N>> currentStart = Collections.singletonList( new FoundNodeEx<>( start ) );

        while( !currentStart.isEmpty() )
        {
            List<FoundNodeEx<N>> nextStart = new ArrayList<>();
            for( FoundNodeEx<N> curNode : currentStart )
            {
                if( ( !curNode.prevSet.isEmpty() && !graph.isIntermediate( curNode.node ) ) )
                    continue;
                final float curLength = curNode.length;
                graph.visitEdges( curNode.node, upstream, (edge, otherEnd, weight) -> {
                    float nextLength = curLength + weight;
                    if( nextLength > maxLength )
                        return;
                    FoundNodeEx<N> next = foundNodes.get( otherEnd );
                    if( next == null )
                    {
                        next = new FoundNodeEx<>( otherEnd, edge, weight, curNode );
                        foundNodes.put( otherEnd, next );
                        nextStart.add( next );
                        next.length = nextLength;
                    }
                    else
                    {
                        if( next.length > nextLength )
                            next.length = nextLength;
                        next.addPrev( edge, weight, curNode );
                    }
                } );
            }
            currentStart = nextStart;
        }
        return StreamEx.of( targets ).map( foundNodes::get ).nonNull().toList();
    }
    private static <N> Set<Element> getReactionElements(FoundNodeEx<N> currentNode, N start, int direction, KeyNodesHub<?> hub,
            float maxLength, Set<N> ignore)
    {
        Set<Element> result = new HashSet<>();
        Set<PrevContainer<N>> nextStep = new HashSet<>();
        ignore.add( currentNode.node );
        for( PrevContainer<N> next : currentNode.prevSet )
        {
            if( next.prev.length + next.edgeWeight > maxLength )
                continue;
            Element elReact = next.edge.createElement( hub );
            elReact.setLinkedDirection( direction );
            elReact.setRelationType( next.edge.getRelationType( true ) );
            result.add( elReact );
            if( !next.prev.node.equals( start ) && !ignore.contains( next.prev.node ) )
                nextStep.add( next );
        }
        for( PrevContainer<N> next : nextStep )
            result.addAll( getReactionElements( next.prev, start, direction, hub, maxLength - next.edgeWeight, ignore ) );
        return result;
    }
}
