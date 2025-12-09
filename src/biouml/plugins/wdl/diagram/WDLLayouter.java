package biouml.plugins.wdl.diagram;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramElement;
import biouml.model.DiagramViewBuilder;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.plugins.wdl.WorkflowUtil;
import biouml.workbench.graph.DiagramToGraphTransformer;
import ru.biosoft.access.core.filter.Filter;
import ru.biosoft.graph.HierarchicLayouter;
import ru.biosoft.graph.OrthogonalPathLayouter;
import ru.biosoft.graph.PathLayouterWrapper;

public class WDLLayouter
{
    private static HierarchicLayouter layouter = new HierarchicLayouter();
    private static OrthogonalPathLayouter edgeLayouter = new OrthogonalPathLayouter();

    private static void redirectEdges(Diagram diagram)
    {
        for( Edge edge : diagram.recursiveStream().select( Edge.class ) )
        {
            Compartment parent = edge.getCompartment();
            Node oldInput = edge.getInput();
            Node oldOutput = edge.getOutput();
            edge.getAttributes().add( new DynamicProperty( "input", Node.class, oldInput ) );
            edge.getAttributes().add( new DynamicProperty( "output", Node.class, oldOutput ) );
            edge.setInput( findLargestParent( edge.getInput(), parent ) );
            edge.setOutput( findLargestParent( edge.getOutput(), parent ) );
        }
    }

    private static void restoreEdges(Diagram diagram)
    {
        for( Edge edge : diagram.recursiveStream().select( Edge.class ) )
        {
            Node input = (Node)edge.getAttributes().getValue( "input" );
            Node output = (Node)edge.getAttributes().getValue( "output" );
            edge.setInput( input );
            edge.setOutput( output );
            edge.getAttributes().remove( "input" );
            edge.getAttributes().remove( "output" );
        }
    }

    private static Node findLargestParent(Node node, Compartment c)
    {
        while( ! ( node.getCompartment().equals( c ) ) )
        {
            node = node.getCompartment();
        }
        return node;
    }

    public static Diagram layout(Diagram diagram)
    {
//        preLayout( diagram );
        redirectEdges( diagram );
        fixConditions( diagram );
        layoutNodes( diagram );
        restoreEdges( diagram );
        layoutEdges( diagram );
        diagram.setView( null );
        diagram.getViewOptions().setAutoLayout( true );
        return diagram;
    }

    private static void layoutEdges(Diagram diagram)
    {
        edgeLayouter.setSmoothEdges( true );
        for( Edge edge : diagram.recursiveStream().select( Edge.class ) )
        {
            DiagramToGraphTransformer.layoutSingleEdge( edge, edgeLayouter, null );
        }
    }


    private static void layoutNodes(Compartment c)
    {
        Diagram diagram = Diagram.getDiagram( c );
        DiagramViewBuilder builder = diagram.getType().getDiagramViewBuilder();
        layouter.setHoistNodes( true );
        layouter.setPathLayouterWrapper( new PathLayouterWrapper( edgeLayouter ) );

        for( Compartment inner : c.stream( Compartment.class ).filter( in -> needInnerLayout( in ) ) )
            layoutNodes( inner );

        ru.biosoft.graph.Graph graph = DiagramToGraphTransformer.generateGraph( c, new Filter<DiagramElement>()
        {

            @Override
            public boolean isAcceptable(DiagramElement de)
            {
                return de.getParent().equals( c );
            }

            @Override
            public boolean isEnabled()
            {
                return true;
            }
        } );

        layouter.doLayout( graph, null );

        Map<String, Point> oldCompartmentLocations = new HashMap<>();
        c.stream().select( Compartment.class ).forEach( inner -> oldCompartmentLocations.put( inner.getName(), inner.getLocation() ) );

        DiagramToGraphTransformer.applyLayout( graph, diagram, true );
        for( Compartment innerCompartment : c.stream().select( Compartment.class ) )
        {
            Point oldLocation = oldCompartmentLocations.get( innerCompartment.getName() );
            Point newLocation = innerCompartment.getLocation();
            Point shift = new Point( newLocation.x - oldLocation.x, newLocation.y - oldLocation.y );
            for( Node node : innerCompartment.recursiveStream().without( innerCompartment ).select( Node.class ) )
            {
                Point p = node.getLocation();
                p.translate( shift.x, shift.y );
                node.setLocation( p );
            }
        }

        if( ! ( c instanceof Diagram ) )
        {
            boolean isCycle = WorkflowUtil.isCycle( c );
            Rectangle wrapper = c.stream( Node.class ).map( n -> builder.getNodeBounds( n ) ).reduce( new Rectangle(), Rectangle::union );
            wrapper.height = wrapper.height + ( isCycle ? 75 : 50 );
            wrapper.width = wrapper.width + ( isCycle ? 75 : 50 );

            c.setShapeSize( wrapper.getSize() );


            for( Node n : c.recursiveStream().without( c ).select( Node.class ) )
            {
                Point p = n.getLocation();
                p.translate( 25, 25 );
                n.setLocation( p );
            }
        }
        c.setView( null );
    }

    private static boolean needInnerLayout(Compartment c)
    {
        return WorkflowUtil.isCycle( c ) || WorkflowUtil.isConditional( c );
    }

    private static void fixConditions(Diagram d)
    {
        for( Node node : d.recursiveStream().select( Node.class ).filter( n -> WorkflowUtil.isCondition( n ) ) )
            node.setFixed( true );
    }

}