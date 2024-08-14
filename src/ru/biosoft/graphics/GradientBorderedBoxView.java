package ru.biosoft.graphics;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.json.JSONException;
import org.json.JSONObject;

public class GradientBorderedBoxView extends BoxView
{
    public static final int SIDE_NONE = 0;
    public static final int SIDE_RIGHT = 1;
    public static final int SIDE_LEFT = 2;
    public static final int SIDE_BOTH = 3;

    float gradientRadius;
    int side;

    public GradientBorderedBoxView(JSONObject jsonObj)
    {
        super( null );
        initFromJSON( jsonObj );
    }

    public GradientBorderedBoxView(Brush brush, float x, float y, float width, float height, float arcWidth, float arcHeight,
            float gradientRadius)
    {
        super( new Pen(), brush, x, y, width, height, arcWidth, arcHeight );
        this.gradientRadius = gradientRadius;
        side = SIDE_BOTH;
    }

    ////////////////////////////////////////////////////////////////

    public void setSide(int side)
    {
        this.side = side;
    }

    @Override
    public Rectangle getBounds()
    {
        Rectangle rect = (Rectangle)super.getBounds().clone();
        if( gradientRadius > 0.0 )
            rect.grow( (int)gradientRadius, (int)gradientRadius );//TODO: take sides into account
        return rect;
    }

    @Override
    public void paint(Graphics2D g2)
    {
        if( isVisible() )
        {
            if( brush != null )
            {
                try
                {
                    Rectangle2D r = shape.getBounds2D();
                    Color c0 = brush.getColor();
                    Color c1 = new Color( c0.getRed(), c0.getGreen(), c0.getBlue(), 0 );

                    double x0 = r.getMinX();
                    double y0 = r.getMinY();
                    double x1 = r.getMaxX();
                    double y1 = r.getMaxY();
                    double w = r.getWidth();
                    double h = r.getHeight();

                    //Center
                    g2.setPaint( c0 );
                    g2.fill( new Rectangle2D.Double( x0, y0, w, h ) );

                    // Top
                    g2.setPaint( new GradientPaint( new Point2D.Double( x0, y0 ), c0, new Point2D.Double( x0, y0 - gradientRadius ), c1 ) );
                    g2.fill( new Rectangle2D.Double( x0, y0 - gradientRadius, w, gradientRadius ) );

                    // Bottom
                    g2.setPaint( new GradientPaint( new Point2D.Double( x0, y1 ), c0, new Point2D.Double( x0, y1 + gradientRadius ), c1 ) );
                    g2.fill( new Rectangle2D.Double( x0, y1, w, gradientRadius ) );

                    if( ( side & SIDE_LEFT ) == SIDE_LEFT )
                    {
                        float fractions[] = new float[] {0.0f, 1.0f};
                        Color colors[] = new Color[] {c0, c1};
                        //Left
                        g2.setPaint(
                                new GradientPaint( new Point2D.Double( x0, y0 ), c0, new Point2D.Double( x0 - gradientRadius, y0 ), c1 ) );
                        g2.fill( new Rectangle2D.Double( x0 - gradientRadius, y0, gradientRadius, h ) );

                        // Top Left
                        g2.setPaint( new RadialGradientPaint( new Rectangle2D.Double( x0 - gradientRadius, y0 - gradientRadius,
                                gradientRadius + gradientRadius, gradientRadius + gradientRadius ), fractions, colors,
                                CycleMethod.NO_CYCLE ) );
                        g2.fill( new Rectangle2D.Double( x0 - gradientRadius, y0 - gradientRadius, gradientRadius, gradientRadius ) );

                        // Bottom Left
                        g2.setPaint( new RadialGradientPaint( new Rectangle2D.Double( x0 - gradientRadius, y1 - gradientRadius,
                                gradientRadius + gradientRadius, gradientRadius + gradientRadius ), fractions, colors,
                                CycleMethod.NO_CYCLE ) );
                        g2.fill( new Rectangle2D.Double( x0 - gradientRadius, y1, gradientRadius, gradientRadius ) );
                    }

                    if( ( side & SIDE_RIGHT ) == SIDE_RIGHT )
                    {
                        // Right
                        g2.setPaint(
                                new GradientPaint( new Point2D.Double( x1, y0 ), c0, new Point2D.Double( x1 + gradientRadius, y0 ), c1 ) );
                        g2.fill( new Rectangle2D.Double( x1, y0, gradientRadius, h ) );


                        float fractions[] = new float[] {0.0f, 1.0f};
                        Color colors[] = new Color[] {c0, c1};



                        // Top Right
                        g2.setPaint( new RadialGradientPaint( new Rectangle2D.Double( x1 - gradientRadius, y0 - gradientRadius,
                                gradientRadius + gradientRadius, gradientRadius + gradientRadius ), fractions, colors,
                                CycleMethod.NO_CYCLE ) );
                        g2.fill( new Rectangle2D.Double( x1, y0 - gradientRadius, gradientRadius, gradientRadius ) );



                        // Bottom Right
                        g2.setPaint( new RadialGradientPaint( new Rectangle2D.Double( x1 - gradientRadius, y1 - gradientRadius,
                                gradientRadius + gradientRadius, gradientRadius + gradientRadius ), fractions, colors,
                                CycleMethod.NO_CYCLE ) );
                        g2.fill( new Rectangle2D.Double( x1, y1, gradientRadius, gradientRadius ) );
                    }
                }
                catch( Throwable t )
                {
                    g2.setColor( Color.RED );
                    g2.setStroke( new BasicStroke( 3 ) );
                    g2.draw( shape );
                }
            }
        }
    }

    @Override
    public JSONObject toJSON() throws JSONException
    {
        JSONObject result = super.toJSON();
        result.put( "gradientRadius", gradientRadius );
        result.put( "side", side );
        return result;
    }

    @Override
    protected void initFromJSON(JSONObject from)
    {
        super.initFromJSON( from );
        if( from.has( "gradientRadius" ) )
        {
            gradientRadius = (float)from.getDouble( "gradientRadius" );
        }
        if( from.has( "side" ) )
            side = from.getInt( "side" );
        else
            side = SIDE_BOTH;
    }

}
