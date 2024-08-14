package ru.biosoft.server.servlets.webservices;

import java.awt.Rectangle;
import java.util.Map;

import ru.biosoft.util.TextUtil;

/**
 * 
 * @author puz
 *
 */
public class TileInfo
{
    private int x;
    private int y;
    private int width;
    private int height;
    private double scale;

    public int getX()
    {
        return x;
    }
    public void setX(int x)
    {
        this.x = x;
    }
    public int getY()
    {
        return y;
    }
    public void setY(int y)
    {
        this.y = y;
    }
    public int getWidth()
    {
        return width;
    }
    public void setWidth(int width)
    {
        this.width = width;
    }
    public int getHeight()
    {
        return height;
    }
    public void setHeight(int height)
    {
        this.height = height;
    }
    public double getScale()
    {
        return scale;
    }
    public void setScale(double scale)
    {
        this.scale = scale;
    }

    public Rectangle getRectangle()
    {
        return new Rectangle(x, y, width, height);
    }

    /**
     * Get tile from servlet arguments.
     * 
     * @param arguments
     * @return
     */
    public static TileInfo parse(Map arguments)
    {
        String xmin = (String)arguments.get("xmin");
        String ymin = (String)arguments.get("ymin");
        String width = (String)arguments.get("width");
        String height = (String)arguments.get("height");

        if( TextUtil.isEmpty(xmin) || TextUtil.isEmpty(ymin) || TextUtil.isEmpty(width) || TextUtil.isEmpty(height) )
        {
            return null;
        }

        final TileInfo tile = new TileInfo();

        try
        {
            tile.setX(Integer.parseInt(xmin));
            tile.setY(Integer.parseInt(ymin));
            tile.setWidth(Integer.parseInt(width));
            tile.setHeight(Integer.parseInt(height));
        }
        catch( Throwable t )
        {
            return null;
        }

        String scale = (String)arguments.get("scale");
        if( TextUtil.isEmpty(scale) )
        {
            tile.setScale(1.0);
        }
        else
        {
            try
            {
                tile.setScale(Double.parseDouble(scale));
            }
            catch( NumberFormatException e )
            {
            }
        }

        return tile;
    }

    /**
     * Create URL parameters for the given tile.
     * 
     * @return
     */
    public String getUrlParams()
    {
        return "xmin=" + x + "&width=" + width + "&ymin=" + y + "&height=" + height + "&scale=" + scale;
    }

    public String getTileId()
    {
        return x + "_" + y;
    }

}
