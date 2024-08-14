package ru.biosoft.util;

import java.awt.Dimension;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * @author Ilya
 * Class is used instead of java.awt.Dimension. It is editable via property inspector and supports bounds for values.
 */
@SuppressWarnings ( "serial" )
public class DimensionEx extends Option
{
    private int width = 0;
    private int height = 0;

    private int minWidth = Integer.MIN_VALUE;
    private int maxWidth = Integer.MAX_VALUE;

    private int minHeight = Integer.MIN_VALUE;
    private int maxHeight = Integer.MAX_VALUE;

    public DimensionEx(Option parent, int minWidth, int maxWidth, int minHeight, int maxHeight, int width, int height)
    {
        super(parent);
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.minWidth = minWidth;
        this.maxWidth = maxWidth;
        if( width <= maxWidth && width >= minWidth )
            this.width = width;
        if( height <= maxHeight && height >= minHeight )
            this.height = height;
    }

    public DimensionEx(Option parent)
    {
        super(parent);
    }

    @PropertyName ( "Width" )
    public int getWidth()
    {
        return width;
    }
    public void setWidth(int width)
    {
        if( width < minWidth || width > maxWidth )
            return;
        int oldValue = this.width;
        this.width = width;
        this.firePropertyChange("width", oldValue, width);
    }

    @PropertyName ( "Height" )
    public int getHeight()
    {
        return height;
    }
    public void setHeight(int height)
    {
        if( height < minHeight || height > maxHeight )
            return;
        int oldValue = this.height;
        this.height = height;
        this.firePropertyChange("height", oldValue, height);
    }

    public @Nonnull Dimension getDimension()
    {
        return new Dimension(width, height);
    }
}
