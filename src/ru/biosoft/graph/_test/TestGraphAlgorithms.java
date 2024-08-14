package ru.biosoft.graph._test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import ru.biosoft.graph.GraphAlgorithms;

public class TestGraphAlgorithms extends TestCase
{
    public void testFindShortestPath()
    {
        Map<Integer, List<Integer>> graph = new HashMap<>();
        graph.put( 1, Arrays.asList( 2 ) );
        graph.put( 2, Arrays.asList( 3 ) );
        graph.put( 4, Arrays.asList( 2 ) );
        List<Integer> path = GraphAlgorithms.findShortestPath( 1, 3, graph  );
        assertEquals( "[1, 2, 3]", path.toString() );
        
        path = GraphAlgorithms.findShortestPath( 1, 1, graph  );
        assertEquals( "[1]", path.toString() );

        path = GraphAlgorithms.findShortestPath( 1, 4, graph  );
        assertEquals( "[]", path.toString() );
        
        path = GraphAlgorithms.findShortestPath( 9, 8, graph  );
        assertEquals( "[]", path.toString() );

    }
}
