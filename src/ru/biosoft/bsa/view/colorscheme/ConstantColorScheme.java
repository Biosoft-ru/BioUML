package ru.biosoft.bsa.view.colorscheme;

import java.awt.Color;
import java.awt.Graphics;

import ru.biosoft.bsa.Site;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.CompositeView;

public class ConstantColorScheme extends AbstractSiteColorScheme
{
    public ConstantColorScheme()
    {
        super( "Constant" );
    }
    
    public ConstantColorScheme(Color color)
    {
        this();
        setColor( color );
    }

    @Override
    public boolean isSuitable(Site site)
    {
        return true;
    }

    @Override
    public Brush getBrush(Site site)
    {
        return defaultBrush;
    }
    
    public Color getColor()
    {
        return defaultBrush.getColor();
    }
    
    public void setColor(Color color)
    {
        defaultBrush.setColor( color );
    }

    @Override
    public CompositeView getLegend(Graphics graphics)
    {
        return new BoxText( defaultBrush, "", graphics );
    }

}
