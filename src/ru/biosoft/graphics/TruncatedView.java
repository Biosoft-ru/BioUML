package ru.biosoft.graphics;

import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;

import org.json.JSONException;
import org.json.JSONObject;

import ru.biosoft.graphics.ShapeView;

public class TruncatedView extends ShapeView
{
    private ShapeView baseView;
    private float truncationPercent = 0.75f;

    public TruncatedView(JSONObject jsonObj)
    {
        super( null );
        initFromJSON( jsonObj );
    }

    public TruncatedView(ShapeView view)
    {
        super( null );
        init( view );
    }

    public TruncatedView(ShapeView view, float percent)
    {
        super( null );
        this.truncationPercent = percent;
        init( view );
    }

    @Override
    public JSONObject toJSON() throws JSONException
    {
        JSONObject result = super.toJSON();
        result.append( "base", baseView.toJSON() );
        result.append( "truncationPercent", truncationPercent );
        return result;
    }
    @Override
    protected void initFromJSON(JSONObject from)
    {
        super.initFromJSON( from );
        if( from.has( "truncationPercent" ) )
            truncationPercent = (float)from.getDouble( "truncationPercent" );
        try
        {
            init( (ShapeView)fromJSON( from.getJSONObject( "base" ) ) );
        }
        catch( JSONException e )
        {
        }
    }
    
    private void init(ShapeView view)
    {
        this.baseView = view;
        this.pen = view.getPen();
        this.brush = view.getBrush();
        this.shape = truncateShape( view.getShape() );
    }

    private Shape truncateShape(Shape shape)
    {
        Area a = new Area( shape );
        Rectangle2D rect = a.getBounds2D();
        Rectangle2D newRect = new Rectangle2D.Double( rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight() * truncationPercent );
        a.subtract( new Area( newRect ) );
        return a;
    }

    @Override
    public void move(int x, int y)
    {
        baseView.move( x, y );
        shape = truncateShape( baseView.getShape() );
    }
}