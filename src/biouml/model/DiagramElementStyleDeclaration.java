package biouml.model;

import org.json.JSONException;
import org.json.JSONObject;

import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.font.ColorFont;

/**
 * Class is used in view options, its behavior is slightly different from DiagramElementStyle<br>
 * Namely: it can be changed always (see BeanInfo) and it can not be assigned with empty name
 * @author Ilya
 *
 */
public final class DiagramElementStyleDeclaration implements Cloneable
{
    String name = "Style_1";
    DiagramElementStyle style;

    public DiagramElementStyleDeclaration()
    {
        this.name = "Style_1";
        this.style = new DiagramElementStyle();
    }
    
    public DiagramElementStyleDeclaration(String name)
    {
        this.name = name;
        this.style = new DiagramElementStyle();
    }

    public DiagramElementStyleDeclaration(String name, Pen pen, Brush brush, ColorFont font)
    {
        this(name);
        if(pen != null) style.setPen( pen );
        if(brush != null) style.setBrush( brush );
        if(font != null) style.setFont( font );
    }
    
    @PropertyName ( "Style name" )
    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        if (name.equals(DiagramElementStyle.STYLE_DEFAULT) || name.equals(DiagramElementStyle.STYLE_NOT_SELECTED))
            return;
        this.name = name;
    }

    @PropertyName ( "Style properties" )
    public DiagramElementStyle getStyle()
    {
        return style;
    }
    public void setStyle(DiagramElementStyle style)
    {
        this.style = style;
    }

    public DiagramElementStyleDeclaration(JSONObject jsonObj)
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

    JSONObject json = null;
    public JSONObject toJSON() throws JSONException
    {
        if( json == null )
        {
            json = new JSONObject();
            json.put("name", new JSONObject(name));
            json.put("style", style.toJSON());
        }
        return json;
    }

    private void initFromJSON(JSONObject from)
    {
        try
        {
            name = from.getJSONObject("name").toString();
            style = new DiagramElementStyle(from.getJSONObject("style"));
        }
        catch( JSONException e )
        {
        }
    }

    @Override
    public DiagramElementStyleDeclaration clone()
    {
        DiagramElementStyleDeclaration style = new DiagramElementStyleDeclaration(name);
        style.setStyle(getStyle().clone());
        return style;
    }
}
