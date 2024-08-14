package ru.biosoft.server.custombean;

import java.awt.Dimension;

public class DimensionWrapper
{
    final private Dimension dim;
    
    public DimensionWrapper(Dimension dim)
    {
        this.dim = dim;
    }
    
    public int getWidth()
    {
        return dim.width;
    }
    
    public void setWidth(int width)
    {
        dim.width = width;
    }
    
    public int getHeight()
    {
        return dim.height;
    }
    
    public void setHeight(int height)
    {
        dim.height = height;
    }
}
