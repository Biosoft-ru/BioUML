package biouml.plugins.physicell;

import java.awt.Color;

import com.developmentontheedge.beans.annot.PropertyName;

public class ColorScheme
{
    private String name = "Unknown";
    private Color color = Color.gray;
    private Color borderColor = Color.black;
    private Color coreColor = Color.darkGray;
    private Color coreBorderColor = Color.black;
    private boolean core = false;
    private boolean border = true;

    public ColorScheme(String name)
    {
        this.name = name;
    }

    public ColorScheme()
    {

    }

    public ColorScheme(String name, Color color, Color borderColor, Color coreColor, Color coreBorderColor)
    {
        this.name = name;
        this.color = color;
        this.borderColor = borderColor;
        this.coreColor = coreColor;
        this.coreBorderColor = coreBorderColor;
    }

    @PropertyName ( "Name" )
    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        this.name = name;
    }

    @PropertyName ( "Inner Color" )
    public Color getColor()
    {
        return color;
    }
    public void setColor(Color color)
    {
        this.color = color;
    }

    @PropertyName ( "Border color" )
    public Color getBorderColor()
    {
        return border ? borderColor : color;
    }
    public void setBorderColor(Color borderColor)
    {
        this.borderColor = borderColor;
    }

    @PropertyName ( "Core border Color" )
    public Color getCoreBorderColor()
    {
        return core ? coreBorderColor : color;
    }
    public void setCoreBorderColor(Color coreBorderColor)
    {
        this.coreBorderColor = coreBorderColor;
    }

    @PropertyName ( "Core color" )
    public Color getCoreColor()
    {
        return core ? coreColor : color;
    }
    public void setCoreColor(Color coreColor)
    {
        this.coreColor = coreColor;
    }

    public ColorScheme clone()
    {
        ColorScheme result = new ColorScheme( name, color, borderColor, coreColor, coreBorderColor );
        result.setBorder( border );
        result.setCore( core );
        return result;
    }

    public ColorScheme offset(ColorScheme scheme, double p)
    {
        Color color = offset( getColor(), scheme.getColor(), p );
        Color borderColor = offset( getBorderColor(), scheme.getBorderColor(), p );
        Color coreColor = offset( getCoreColor(), scheme.getCoreColor(), p );
        Color coreBorderColor = offset( getCoreBorderColor(), scheme.getCoreBorderColor(), p );
        return new ColorScheme( name, color, borderColor, coreColor, coreBorderColor );
    }

    private Color offset(Color from, Color to, double p)
    {
        int r = (int) ( from.getRed() + ( to.getRed() - from.getRed() ) * p );
        int g = (int) ( from.getGreen() + ( to.getGreen() - from.getGreen() ) * p );
        int b = (int) ( from.getBlue() + ( to.getBlue() - from.getBlue() ) * p );
        return new Color( r, g, b );
    }

    @PropertyName ( "Has core" )
    public boolean isCore()
    {
        return core;
    }
    public void setCore(boolean core)
    {
        this.core = core;
    }

    public boolean noCore()
    {
        return !isCore();
    }

    @PropertyName ( "Has border" )
    public boolean isBorder()
    {
        return border;
    }
    public void setBorder(boolean border)
    {
        this.border = border;
    }

    public boolean noBorder()
    {
        return !isBorder();
    }         
}