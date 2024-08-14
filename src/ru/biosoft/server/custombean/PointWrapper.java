package ru.biosoft.server.custombean;

import java.awt.Point;

public class PointWrapper
{
    final private Point pt;
    
    public PointWrapper(Point pt)
    {
        this.pt = pt;
    }
    
    public int getX()
    {
        return pt.x;
    }
    
    public void setX(int x)
    {
        pt.x = x;
    }
    
    public int getY()
    {
        return pt.y;
    }
    
    public void setY(int y)
    {
        pt.y= y;
    }
}
