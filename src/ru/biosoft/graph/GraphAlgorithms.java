package ru.biosoft.graph;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GraphAlgorithms
{
    public static <T> List<T> findShortestPath(T from, T to, Map<T, List<T>> graph)
    {
        if(from.equals( to ))
            return Collections.singletonList( from );
        
        Map<T, T> backLinks = new HashMap<>();

        LinkedList<T> bfq = new LinkedList<>();//breadth first queue
        Set<T> enqued = new HashSet<>();
        
        bfq.add( from );
        enqued.add( from );
        
        while(!bfq.isEmpty())
        {
            T curNode = bfq.pop();
            List<T> childList = graph.get( curNode );
            if(childList == null)
                continue;
            for(T child : childList)
                if(!enqued.contains( child ))
                {
                    bfq.add( child );
                    enqued.add( child );
                    backLinks.put( child, curNode );
                    if(child.equals( to ))
                    {
                        bfq.clear();//we reached 'to' node, terminate early
                        break;
                    }
                }
        }
        if(!backLinks.containsKey( to ))
            return Collections.emptyList();//target node not reached

        LinkedList<T> path = new LinkedList<>();
        //recover shortest path from backLinks
        T node = to;
        while(node != null)
        {
            path.addFirst( node );
            node = backLinks.get( node );
        }
        
        return path;
    }
}
