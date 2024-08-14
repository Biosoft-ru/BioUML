package biouml.model._test;

import junit.framework.TestCase;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;

public class DiagramChecker extends TestCase
{
    public DiagramChecker( String name )
    {
        super(name);
    }

    @Override
    protected void tearDown()
    {
        diagram = null;
    }

    protected void checkDiagram()
    {
        checkCompartment( diagram );
    }

    protected void checkCompartment( Compartment compartment )
    {
        for( DiagramElement de : compartment )
        {
            assertEquals( "Parent of '"+de.getName()+"' is wrong.",compartment,de.getOrigin() );
            if( de instanceof Edge )
            {
                checkEdge( (Edge)de );
            }
            if( de instanceof Compartment )
            {
                checkCompartment( (Compartment)de );
            }
        }
    }

    protected void checkEdge( Edge edge )
    {
        Node inNode  = edge.getInput();
        Node outNode = edge.getOutput();
        assertNotNull( "Input of edge '"+edge.getName()+"' shouldn't be null",inNode );
        assertNotNull( "Output of edge '"+edge.getName()+"' shouldn't be null",outNode );
        assertNotNull( "Input '"+inNode.getName()+"' of edge '"+edge.getName()+"' not found in any part of diagram.",findNode(diagram,inNode.getName()) );
        assertNotNull( "Output '"+outNode.getName()+"' of edge '"+edge.getName()+"' not found in any part of diagram.",findNode(diagram,outNode.getName()) );
        assertTrue( "In Node '"+inNode.getCompletePath()+"' not found in '"+inNode.getOrigin().getCompletePath()+"'",inNode.getOrigin().contains(inNode.getName()) );
        assertTrue( "Out Node '"+outNode.getCompletePath()+"' not found in '"+outNode.getOrigin().getCompletePath()+"'",outNode.getOrigin().contains(outNode.getName()) );
        DataElementPath inNodeOriginPath  = inNode.getOrigin().getCompletePath();
        DataElementPath outNodeOriginPath = outNode.getOrigin().getCompletePath();
        DataElementPath edgeOriginPath    = edge.getOrigin().getCompletePath();
        assertTrue(
                "Wrong location '" + edgeOriginPath + "' of edge '" + edge.getName() + "' for input node '"
                        + inNode.getCompletePath() + "'", inNodeOriginPath.isDescendantOf(edgeOriginPath));
        assertTrue(
                "Wrong location '" + edgeOriginPath + "' of edge '" + edge.getName() + "' for output node '"
                        + outNode.getCompletePath() + "'", outNodeOriginPath.isDescendantOf(edgeOriginPath));
    }

    protected void countEdges( int expectedCount )
    {
        assertEquals( "Wrong count of edges in '"+diagram.getName()+"'",expectedCount,countEdges(diagram) );
    }

    protected int countEdges( Compartment compartment )
    {
        int count = 0;
        for( DiagramElement item: compartment )
        {
            if( item instanceof Edge )
            {
                count++;
            }
            else if( item instanceof DataCollection )
            {
                count += countEdges( (Compartment)item );
            }
        }
        return count;
    }

    protected Node findNode( Compartment compartment,String name )
    {
        for( DiagramElement de: compartment )
        {
            assertSame( "Wrong origin of '"+de.getName()+"'",compartment,de.getOrigin() );
            if( de instanceof Compartment )
            {
                Node node = findNode( (Compartment)de,name );
                if(node != null) return node;
            }
            else if( de.getName().equals(name) )
            {
                return (Node)de;
            }
        }
        return null;
    }

    protected Diagram diagram = null;
}