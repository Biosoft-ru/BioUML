package ru.biosoft.bsa.track.combined;

import java.awt.Color;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.util.bean.JSONBean;

@SuppressWarnings ( "serial" )
public class CombinedItem extends Option implements JSONBean
{
    private DataElementPath path;
    private Color color;

    public CombinedItem()
    {

    }

    public CombinedItem(DataElementPath path)
    {
        this.path = path;
    }

    public CombinedItem(DataElementPath path, Color color)
    {
        this.path = path;
        this.color = color;
    }

    @PropertyName ( "Path" )
    public DataElementPath getPath()
    {
        return path;
    }
    public void setPath(DataElementPath path)
    {
        Object oldValue = this.path;
        this.path = path;
        firePropertyChange( "path", oldValue, path );
    }

    @PropertyName ( "Color" )
    public Color getColor()
    {
        return color;
    }
    public void setColor(Color color)
    {
        Object oldValue = this.color;
        this.color = color;
        firePropertyChange( "color", oldValue, color );
    }

}
