package biouml.plugins.physicell;

import java.awt.Color;

import com.developmentontheedge.beans.annot.PropertyName;

public class ColorScheme
{
    private Color outerColor;
    private Color borderPen;
    private Color innerColor;
    private Color innerBorderPen;

    @PropertyName ( "Outer Color" )
    public Color getOuterColor()
    {
        return outerColor;
    }
    public void setOuterColor(Color outerColor)
    {
        this.outerColor = outerColor;
    }

    @PropertyName ( "Border color" )
    public Color getBorderPen()
    {
        return borderPen;
    }
    public void setBorderPen(Color borderPen)
    {
        this.borderPen = borderPen;
    }

    @PropertyName ( "Inner Color" )
    public Color getInnerColor()
    {
        return innerColor;
    }
    public void setInnerColor(Color innerColor)
    {
        this.innerColor = innerColor;
    }

    @PropertyName ( "Inner Border color" )
    public Color getInnerBorderPen()
    {
        return innerBorderPen;
    }
    public void setInnerBorderPen(Color innerBorderPen)
    {
        this.innerBorderPen = innerBorderPen;
    }

}