package biouml.standard.filter;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Objects;

import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.GradientBorderedBoxView;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.ShapeView;
import ru.biosoft.graphics.View;
import biouml.model.Compartment;
import biouml.model.DiagramElement;
import biouml.model.Node;

/**
 * If diagram element satisfies to the filter condition it will be highlighted.
 */
public class HighlightAction implements Action
{
    private int addX = 20, addY = 20;

    public HighlightAction()
    {
        this.brush = new Brush(new Color(255,255,255));
        this.pen = null;
    }

    public HighlightAction(Brush brush)
    {
        this.brush = brush;
        this.pen = null;
    }

    public HighlightAction(Brush brush, Pen pen)
    {
        this.brush = brush;
        this.pen = pen;
    }

    public HighlightAction(Brush brush, Pen pen, String description, int radius)
    {
        this.brush = brush;
        this.pen = pen;
        this.description = description;
        addX = radius;
        addY = radius;
    }

    /** Description for highlight view */
    protected String description;
    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    /** Brush that should be used to highlight the diagram element (node). */
    protected Brush brush;
    public Brush getBrush()
    {
        return brush;
    }
    public void setBrush(Brush brush)
    {
        this.brush = brush;
    }

    /** Pen that should be used to highlight the diagram element (node). */
    protected Pen pen;
    public Pen getPen()
    {
        return pen;
    }
    public void setPen(Pen pen)
    {
        this.pen = pen;
    }

    @Override
    public void apply(DiagramElement de)
    {
        if( de instanceof Node && de.getOrigin() instanceof Compartment)
        {
            Node node = (Node)de;
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
                double maxArea = 0;
                for( View childView : (CompositeView)view )
                {
                    double area = childView.getBounds().getWidth() * childView.getBounds().getHeight();
                    if( childView instanceof ShapeView && area > maxArea )
                    {
                        maxArea = area;
                        baseView = (ShapeView)childView;
                    }
                    else if( childView instanceof CompositeView )
                    {
                        for( View ccView : (CompositeView)childView )
                        {
                            if( ccView instanceof ShapeView && area > maxArea )
                            {
                                maxArea = area;
                                baseView = (ShapeView)ccView;
                            }
                        }
                    }
                }
            }
            if( baseView == null )
                return;

            Rectangle bounds = (Rectangle)baseView.getBounds().clone();
            //bounds.grow(20, 20);

            //BoxView highlighter = new BoxView(pen, brush, bounds.x, bounds.y, bounds.width, bounds.height);
            Shape shape = baseView.getShape();
            GradientBorderedBoxView highlighter = null;
            if( shape instanceof RoundRectangle2D )
            {
                highlighter = new GradientBorderedBoxView( brush, bounds.x, bounds.y, bounds.width, bounds.height,
                        (float) ( (RoundRectangle2D)shape ).getArcWidth(), (float) ( (RoundRectangle2D)shape ).getArcHeight(), addX );
            }
            else if( shape instanceof Ellipse2D )
            {
                highlighter = new GradientBorderedBoxView( brush, bounds.x, bounds.y, bounds.width, bounds.height,
                        (float) ( (Ellipse2D)shape ).getWidth() / 2, (float) ( (Ellipse2D)shape ).getHeight() / 2, addX );
            }
            else
            {
                highlighter = new GradientBorderedBoxView( brush, bounds.x, bounds.y, bounds.width, bounds.height,
                        5, 5, addX );
            }
            if( highlighter != null )
            {
                highlighter.setModel( this );
                if( description != null )
                    highlighter.setDescription( description );
                int i = 0;
                for( View view1 : parentView )
                {
                    if( view1.getModel() == node )
                        break;
                    i++;
                }
                parentView.insert( highlighter, i );
            }
        }
    }

    @Override
    public int hashCode()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object obj)
    {
        if( obj instanceof HighlightAction )
        {
            Brush brush2 = ( (HighlightAction)obj ).getBrush();
            Pen pen2 = ( (HighlightAction)obj ).getPen();
            return ( brush.getPaint().equals(brush2.getPaint()) && Objects.equals( pen, pen2 ) );
        }

        return false;
    }
}
