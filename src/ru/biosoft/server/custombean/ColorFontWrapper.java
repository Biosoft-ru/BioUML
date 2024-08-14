package ru.biosoft.server.custombean;

import java.awt.Color;
import java.awt.Font;

import ru.biosoft.graphics.font.ColorFont;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

public class ColorFontWrapper
{
    private final ColorFont font;

    public ColorFontWrapper(ColorFont font)
    {
        this.font = font;
    }
    
    @PropertyName("Font")
    @PropertyDescription("Font type")
    public String getFamily()
    {
        return font.getFont().getFamily();
    }
    
    public void setFamily(String family)
    {
        font.setFont( new Font( family, font.getFont().getStyle(), font.getFont().getSize() ) );
    }
    
    @PropertyName("Size")
    @PropertyDescription("Font size")
    public int getSize()
    {
        return font.getFont().getSize();
    }
    
    public void setSize(int size)
    {
        font.setFont( new Font( font.getFont().getFamily(), font.getFont().getStyle(), size ) );
    }
    
    @PropertyName("Color")
    @PropertyDescription("Font color")
    public Color getColor()
    {
        return font.getColor();
    }
    
    public void setColor(Color color)
    {
        font.setColor( color );
    }
}
