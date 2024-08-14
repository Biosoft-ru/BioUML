package biouml.standard.filter;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.PathIterator;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.GradientBorderedBoxView;
import ru.biosoft.graphics.PolygonView;
import ru.biosoft.graphics.ShapeView;
import ru.biosoft.graphics.View;
import ru.biosoft.util.Pair;
import biouml.model.Compartment;
import biouml.model.DiagramElement;
import biouml.model.Edge;
import biouml.model.Node;

/**
 * @author lan
 *
 */
public class ShapedHighlightAction extends CompositeHighlightAction
{
    private int addX = 20, addY = 20;
    private boolean useGradientFill = false;

    public ShapedHighlightAction()
    {

    }

    public ShapedHighlightAction(int radius, boolean useGradientFill)
    {
        addX = radius;
        addY = radius;
        this.useGradientFill = useGradientFill;
    }

    /**
     * Cut polygon by vertical line using Sutherland-Hodgman algorithm
     * @param points - list of source polygon points
     * @param xPos - position of the line
     * @param left - if true, left part of polygon will be returned
     * @return cut polygon
     */
    private List<Point> cutBySutherlandHodgman(List<Point> points, int xPos, boolean left)
    {
        List<Point> result = new ArrayList<>();
        Point last = points.get(points.size()-1);
        for(Point cur: points)
        {
            if(last.x < xPos ^ cur.x < xPos) result.add(new Point(xPos, (cur.y-last.y)*(xPos-last.x)/(cur.x-last.x)+last.y));
            if(!left ^ cur.x < xPos) result.add(cur);
            last = cur;
        }
        return result;
    }

    @Override
    public void apply(DiagramElement de)
    {
        if( actions == null || !(de instanceof Node) || !(de.getOrigin() instanceof Compartment)) return;

        Compartment parent = (Compartment)de.getOrigin();
        CompositeView parentView = (CompositeView)parent.getView();
        View view = de.getView();
        ShapeView baseView = null;
        if( view instanceof ShapeView )
        {
            baseView = (ShapeView)view;
        }
        else if( view instanceof CompositeView )
        {
            //TODO: think about better solution
            InnerShapeView innerShape = InnerShapeView.findBiggestInnerShape( (CompositeView)view, -1 );
            if( innerShape != null )
                baseView = innerShape.getShapeView();
        }
        if(baseView == null) return;
        double cx = baseView.getBounds().getCenterX();
        double cy = baseView.getBounds().getCenterY();
        AffineTransform at = new AffineTransform();
        at.translate(cx, cy);
        if( !useGradientFill )
            at.scale( ( baseView.getBounds().getWidth() + addX * 2 ) / baseView.getBounds().getWidth(),
                    ( baseView.getBounds().getHeight() + addY * 2 ) / baseView.getBounds().getHeight() );
        at.translate(-cx, -cy);

        PathIterator pathIterator = baseView.getShape().getPathIterator(at, 1);
        double coords[] = new double[6];
        List<Point> points = new ArrayList<>();
        while(!pathIterator.isDone())
        {
            int type = pathIterator.currentSegment(coords);
            if(type != PathIterator.SEG_CLOSE)
                points.add(new Point((int)coords[0], (int)coords[1]));
            pathIterator.next();
        }

        int n = actions.length;
        CompositeView highlighter = new CompositeView();
        double leftX = cx - baseView.getBounds().getWidth() / 2 - addX - 2;
        double rightX = cx + baseView.getBounds().getWidth() / 2 + addX + 2;
        for(int i=0; i<n; i++)
        {
            List<Point> curPoints = i==0?points:cutBySutherlandHodgman(points, (int) ( (rightX-leftX)*i/n+leftX ), false);
            curPoints = i==n-1?curPoints:cutBySutherlandHodgman(curPoints, (int) ( (rightX-leftX)*(i+1)/n+leftX ), true);
            if( useGradientFill )
            {
                PolygonView poly = new PolygonView( actions[i].pen, actions[i].brush, curPoints );
                Rectangle rect = poly.getBounds();
                int side = i == 0 ? n == 1 ? GradientBorderedBoxView.SIDE_BOTH : GradientBorderedBoxView.SIDE_LEFT
                        : i == n - 1 ? GradientBorderedBoxView.SIDE_RIGHT : GradientBorderedBoxView.SIDE_NONE;
                Pair<Float, Float> radius = getRadiusForShape( baseView.getShape() );
                if( radius.getFirst() > rect.width )
                    radius.setFirst( (float)rect.width );
                if( radius.getSecond() > rect.width )
                    radius.setSecond( (float)rect.width );
                GradientBorderedBoxView bw = new GradientBorderedBoxView( actions[i].brush, rect.x, rect.y, rect.width, rect.height,
                        radius.getFirst(), radius.getSecond(), addX );
                bw.setSide( side );
                highlighter.add( bw );
            }
            else
            {
                highlighter.add( new PolygonView( actions[i].pen, actions[i].brush, curPoints ) );
            }
        }
        highlighter.setModel(this);
        parentView.insert( highlighter, findHighlightPosition( parentView ) );//lookupNodePosition(parentView, node));
    }

    private Pair<Float, Float> getRadiusForShape(Shape shape)
    {
        if( shape instanceof RoundRectangle2D )
        {
            return new Pair<>( (float) ( (RoundRectangle2D)shape ).getArcWidth(),
                    (float) ( (RoundRectangle2D)shape ).getArcHeight() );
        }
        else if( shape instanceof Ellipse2D )
        {
            double w = ( (Ellipse2D)shape ).getWidth();
            double h = ( (Ellipse2D)shape ).getHeight();
            float arcw, arch;
            if( w / h > 1.5 )
            {
                arcw = (float) ( h / 2 );
                arch = (float) ( h / 2 );
            }
            else if( h / w > 1.5 )
            {
                arcw = (float) ( w / 2 );
                arch = (float) ( w / 2 );
            }
            else
            {
                arcw = (float) ( w / 2 );
                arch = (float) ( h / 2 );
            }
            return new Pair<>( arcw, arch );
        }
        else
        {
            return new Pair<>( 5.0f, 5.0f );
        }
    }

    private int findHighlightPosition(CompositeView cv)
    {
        for( int i = 0; i < cv.size(); i++ )
        {
            View v = cv.elementAt( i );
            Object model = v.getModel();
            if( model == null || model.equals( cv.getModel() ) )
                continue;
            if( model instanceof Node || model instanceof Edge )
                return i;
        }
        return 0;

    }
}
