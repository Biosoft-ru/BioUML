package biouml.plugins.wdl.diagram;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.model.Compartment;
import biouml.model.Diagram;
import biouml.model.DiagramViewBuilder;
import biouml.model.Edge;
import biouml.model.Node;
import biouml.plugins.wdl.WorkflowUtil;

public class Util
{
    public static List<Node> findDirectPath(Node input)
    {
        List<Node> result = new ArrayList<Node>();

        while( true )
        {
            List<Edge> links = new ArrayList<Edge>();
            for( Edge edge : input.edges() )
            {
                if( edge.getKernel().getType().equals( WDLConstants.LINK_TYPE ) && edge.getOutput().equals( input ) )
                    links.add( edge );
            }

            if( links.size() == 1 )
            {

                Edge singleEdge = links.get( 0 );

                Node source = singleEdge.getInput();
                if( WorkflowUtil.isExternalParameter( source ) || WorkflowUtil.isExpression( source ) )
                {
                    result.add( source );
                    input = source;
                }
                else
                    break;
            }
            else
                break;

        }

        return result;
    }

    public static void hideDirectPathes(Diagram diagram)
    {
        for( Node input : diagram.recursiveStream().select( Node.class )
                .filter( n -> WorkflowUtil.isInput( n ) && WorkflowUtil.isCall( n.getCompartment() ) ) )
        {
            List<Node> path = findDirectPath( input );
            if( path.isEmpty() )
                continue;
            Node source = path.get( path.size() - 1 );
            if( WorkflowUtil.isExternalParameter( source ) )
            {
                for( Node node : path )
                {
                    node.setVisible( false );
                }
                input.getAttributes().add( new DynamicProperty( "ViewSubstitute", String.class, source.getName() ) );
            }
        }
    }

    public static void movePortsToEdge(Diagram diagram)
    {
        DiagramViewBuilder viewBuilder = diagram.getType().getDiagramViewBuilder();
        for( Node port : diagram.recursiveStream().select( Node.class ).without( diagram ) )
        {
            Compartment parent = port.getCompartment();
            if( WorkflowUtil.isCall( parent ) || WorkflowUtil.isTask( parent ) )
            {
                Rectangle parentRect = viewBuilder.getNodeBounds( parent );
                Rectangle rect = viewBuilder.getNodeBounds( port );
                Point location = port.getLocation();

                if( WorkflowUtil.isOutput( port ) )
                    location.x = parentRect.x + parentRect.width - rect.width;
                else if( WorkflowUtil.isInput( port ) )
                    location.x = parentRect.x;
                port.setLocation( location );
            }
            else if ( WDLConstants.CONDITIONAL_PORT_TYPE.equals( port.getKernel().getType()))
            {
                port.setLocation( port.getCompartment().getLocation() );
                port.setFixed( true );
            }
        }
    }
}
