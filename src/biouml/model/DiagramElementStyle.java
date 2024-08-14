package biouml.model;

import java.awt.Color;
import java.util.Objects;

import org.json.JSONException;
import org.json.JSONObject;

import com.developmentontheedge.beans.Option;

import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.font.ColorFont;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

public final class DiagramElementStyle extends Option implements Cloneable
{
    public static final String STYLE_NOT_SELECTED = "Not selected";
    public static final String STYLE_DEFAULT = "Default";
    
    private Pen pen;
    private Brush brush;
    private ColorFont font;

    public DiagramElementStyle(DiagramElement de)
    {
        super(de);
        pen = new Pen();
        brush = new Brush(Color.white);
        font = new ColorFont("Arial", 0, 12, Color.black);
    }
    
    public DiagramElementStyle()
    {
        pen = new Pen();
        brush = new Brush(Color.white);
        font = new ColorFont("Arial", 0, 12, Color.black);
    }

    public DiagramElementStyle(JSONObject jsonObj)
    {
        initFromJSON(jsonObj);
    }

    @Override
    public String toString()
    {
        try
        {
            return toJSON().toString();
        }
        catch( JSONException e )
        {
            return "";
        }
    }

    @PropertyName ( "Pen" )
    @PropertyDescription ( "Border pen." )
    public Pen getPen()
    {
        return pen;
    }
    public void setPen(Pen pen)
    {
        Object oldValue = this.pen;
        this.pen = pen;
        firePropertyChange("pen", oldValue, pen);

    }

    @PropertyName ( "Brush" )
    @PropertyDescription ( "Brush." )
    public Brush getBrush()
    {
        return brush;
    }
    public void setBrush(Brush brush)
    {
        Object oldValue = this.brush;
        this.brush = brush;
        firePropertyChange("brush", oldValue, brush);
    }

    @PropertyName ( "Title font" )
    @PropertyDescription ( "Title font." )
    public ColorFont getFont()
    {
        return font;
    }
    public void setFont(ColorFont font)
    {
        Object oldValue = this.font;
        this.font = font;
        firePropertyChange("font", oldValue, font);
    }

    transient JSONObject json = null;
    public JSONObject toJSON() throws JSONException
    {
        if( json == null )
        {
            json = new JSONObject();
            json.put("font", font.toJSON());
            json.put("pen", pen.toJSON());
            json.put("brush", brush.toJSON());
        }
        return json;
    }

    private void initFromJSON(JSONObject from)
    {
        try
        {
            brush = new Brush(from.getJSONObject("brush"));
            pen = new Pen(from.getJSONObject("pen"));
            font = new ColorFont(from.getJSONObject("font"));
        }
        catch( JSONException e )
        {
        }
    }
    
    @Override
    public DiagramElementStyle clone()
    {
        DiagramElementStyle style = new DiagramElementStyle();
        style.setBrush(new Brush(brush.getPaint()));
        style.setPen(pen.clone());
        style.setFont(new ColorFont(font.getFont(), font.getColor()));
        return style;
    }

    @Override
    public boolean equals(Object obj)
    {
        if( this == obj )
            return true;
        if( obj == null || getClass() != obj.getClass() )
            return false;
        DiagramElementStyle other = (DiagramElementStyle)obj;
        return Objects.equals( brush, other.brush ) &&
                Objects.equals( font, other.font ) &&
                Objects.equals( pen, other.pen );
    }
}
