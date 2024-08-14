package biouml.workbench.graph;

import java.awt.Point;
import java.awt.Rectangle;

import biouml.model.SubDiagram.PortOrientation;

import ru.biosoft.graph.Edge;
import ru.biosoft.graph.Node;
import ru.biosoft.graph.PortFinder;

/**
 * Port finder with only one appropriate point in the middle of left, right, bottom or top border
 * Point coordinates are stored in the relative to node coordinates
 * @author axec
 *
 */
public class OnePointFinder implements PortFinder
{
    Point p;

     public OnePointFinder(PortOrientation orientation, Rectangle rec)
    {
        if( orientation == PortOrientation.TOP )
        {
            p = new Point(rec.width / 2, 0);
        }
        else if( orientation == PortOrientation.BOTTOM )
        {
            p = new Point(rec.width / 2, rec.height);
        }
        else if( orientation == PortOrientation.LEFT )
        {
            p = new Point(0, rec.height / 2);
        }
        else
        {
            p = new Point(rec.width, rec.height / 2);
        }
    }

    @Override
    public Point findPort(Node node, Edge edge, int x, int y)
    {
        Point availablePoint = new Point(p);
        availablePoint.x += node.x;
        availablePoint.y += node.y;
        return availablePoint;
    }

}
