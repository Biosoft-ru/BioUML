package biouml.plugins.kegg;

import java.awt.Point;

import ru.biosoft.graph.Edge;
import ru.biosoft.graph.Graph;
import ru.biosoft.graph.GreedyLayouter;
import ru.biosoft.graph.LayoutJobControl;
import ru.biosoft.graph.Layouter;
import ru.biosoft.graph.LayouterInfo;
import ru.biosoft.graph.LayouterInfoSupport;
import ru.biosoft.graph.Node;
import ru.biosoft.graph.Path;

/**
 * Decorator for orthogonal layouter
 * 
 * @author tolstyh
 * 
 */
public class KeggPathwayLayouter implements Layouter
{
    /**
     * Diameter of smooth
     */
    public static final double DIAMETER = 20.0;

    /**
     * Minimal size of the node
     */
    public static final int MINIMAL_SIZE = 24;

    public static final String OLD_WIDTH_ATTR = "oldWidth";
    public static final String OLD_HEIGHT_ATTR = "oldHeight";

    /**
     * Base layouter
     */
    protected Layouter baseLayouter;

    public KeggPathwayLayouter()
    {
        baseLayouter = new GreedyLayouter(6, 6);
    }

    @Override
    public void doLayout(Graph graph, LayoutJobControl lJC)
    {
        //only edge layout
        layoutEdges(graph, lJC);
    }

    @Override
    public void layoutEdges(Graph graph, LayoutJobControl lJC)
    {
        preprocess(graph);
        baseLayouter.layoutEdges(graph, null);
        postproces(graph);
    }

    @Override
    public void layoutPath(Graph graph, Edge edge, LayoutJobControl lJC)
    {
        preprocess(graph);
        baseLayouter.layoutPath(graph, edge, null);
        postproces(graph);
    }

    /**
     * Actions before layout
     */
    protected void preprocess(Graph graph)
    {
        resizeNodes(graph);
    }

    /**
     * Actions after layout
     */
    protected void postproces(Graph graph)
    {
        restoreNodeSize(graph);
        smoothEdges(graph);
    }

    protected void resizeNodes(Graph graph)
    {
        for( Node node : graph.getNodes() )
        {
            if( node.width < MINIMAL_SIZE )
            {
                node.setAttribute(OLD_WIDTH_ATTR, Integer.toString(node.width));
                node.x -= ( MINIMAL_SIZE - node.width ) / 2;
                node.width = MINIMAL_SIZE;
            }
            if( node.height < MINIMAL_SIZE )
            {
                node.setAttribute(OLD_HEIGHT_ATTR, Integer.toString(node.height));
                node.y -= ( MINIMAL_SIZE - node.height ) / 2;
                node.height = MINIMAL_SIZE;
            }
        }
    }

    protected void restoreNodeSize(Graph graph)
    {
        for( Node node : graph.getNodes() )
        {
            String width = node.getAttribute(OLD_WIDTH_ATTR);
            if( width != null )
            {
                int oldWidth = Integer.parseInt(width);
                node.x += ( node.width - oldWidth ) / 2;
                node.width = oldWidth;
            }
            String height = node.getAttribute(OLD_HEIGHT_ATTR);
            if( height != null )
            {
                int oldHeight = Integer.parseInt(height);
                node.y += ( node.height - oldHeight ) / 2;
                node.height = oldHeight;
            }
        }
    }

    protected void smoothEdges(Graph graph)
    {
        for( Edge edge : graph.getEdges() )
        {
            Path oldPath = edge.getPath();
            if( oldPath != null && oldPath.npoints > 2 )
            {
                Path newPath = new Path();
                newPath.addPoint(oldPath.xpoints[0], oldPath.ypoints[0], Path.LINE_TYPE);
                Point previous = new Point(oldPath.xpoints[0], oldPath.ypoints[0]);
                Point current = new Point(oldPath.xpoints[1], oldPath.ypoints[1]);
                for( int i = 2; i < oldPath.npoints; i++ )
                {
                    Point next = new Point(oldPath.xpoints[i], oldPath.ypoints[i]);

                    double d = Point.distance(previous.x, previous.y, current.x, current.y);
                    if( d > DIAMETER )
                    {
                        int x = current.x + (int) ( ( previous.x - current.x ) * DIAMETER / d );
                        int y = current.y + (int) ( ( previous.y - current.y ) * DIAMETER / d );
                        newPath.addPoint(x, y, Path.LINE_TYPE);
                    }

                    newPath.addPoint(current.x, current.y, Path.QUAD_TYPE);

                    d = Point.distance(current.x, current.y, next.x, next.y);
                    if( d > DIAMETER )
                    {
                        int x = current.x + (int) ( ( next.x - current.x ) * DIAMETER / d );
                        int y = current.y + (int) ( ( next.y - current.y ) * DIAMETER / d );
                        newPath.addPoint(x, y, Path.LINE_TYPE);
                    }

                    previous = current;
                    current = next;
                }
                newPath.addPoint(current.x, current.y, Path.LINE_TYPE);
                edge.setPath(newPath);
            }
        }
    }

    //+++++++++++++++++++++++++++++++++++++++++++++++++++++

    @Override
    public LayouterInfo getInfo()
    {
       return new LayouterInfoSupport(false, false, false, false, false, false);
    }


    @Override
    public int estimate(Graph graph, int what)
    {
       return 0;
    }
}
