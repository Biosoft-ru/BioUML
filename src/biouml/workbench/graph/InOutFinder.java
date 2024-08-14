package biouml.workbench.graph;

import java.awt.Point;
import java.awt.Rectangle;

import ru.biosoft.graph.Edge;
import ru.biosoft.graph.Node;
import ru.biosoft.graph.PortFinder;

/**
 * Port finder which connects inputs on the right and outputs on the left
 */
public class InOutFinder implements PortFinder
{
    Point in, out;

    public InOutFinder(boolean vertical, Rectangle rec)
    {
        if(vertical)
        {
            in = new Point( rec.width / 2, 0 );
            out = new Point( rec.width / 2, rec.height );
        } else
        {
            in = new Point( rec.width, rec.height / 2 );
            out = new Point( 0, rec.height / 2 );
        }
    }

    @Override
    public Point findPort(Node node, Edge edge, int x, int y)
    {
        Point p = new Point(edge.getFrom() == node ? in : out);
        p.x += node.x;
        p.y += node.y;
        return p;
    }

}
