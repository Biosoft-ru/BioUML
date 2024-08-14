package ru.biosoft.bsa.view.colorscheme;

import java.awt.Color;
import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.view.SiteViewOptions;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.util.ColorUtils;
import com.developmentontheedge.beans.annot.PropertyName;

/**
 * @author lan
 *
 */
public class AutoTagColorScheme extends AbstractSiteColorScheme
{
    private static final String TAG_DEFAULT = "(default)";
    private DynamicPropertySetAsMap colors = new DynamicPropertySetAsMap();
    
    public AutoTagColorScheme()
    {
        super("By type");
        colors.addPropertyChangeListener(new PropertyChangeListener()
        {
            @Override
            public void propertyChange(PropertyChangeEvent evt)
            {
                firePropertyChange("colors/"+evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
            }
        });
    }
    
    @PropertyName("Colors")
    public DynamicPropertySetAsMap getColors()
    {
        return colors;
    }
    
    public void setColors(DynamicPropertySetAsMap dps)
    {
        this.colors = dps;
    }

    @Override
    public boolean isSuitable(Site site)
    {
        return true;
    }

    @Override
    public Brush getBrush(Site site)
    {
        String tag = site == null ? null : ( (SiteViewOptions)getParent() ).getViewTagger().getTag(site);
        if(tag == null || tag.isEmpty()) tag = "(default)";
        return getBrushByTag(tag);
    }

    /**
     * @param tag
     * @return
     */
    public Brush getBrushByTag(String tag)
    {
        DynamicProperty property = colors.getProperty(tag);
        if(property == null)
        {
            
            Color defaultColor = defaultBrush.getColor();
            Color color = tag.equals(TAG_DEFAULT) ? defaultColor : ColorUtils.getDefaultColor(colors.size());
            property = new DynamicProperty(tag, Color.class, color);
            colors.add(property);
        }
        Color color = (Color)colors.getValue(tag);
        return new Brush(color);
    }

    @Override
    public CompositeView getLegend(Graphics graphics)
    {
        CompositeView legend = new CompositeView();
        for(DynamicProperty property: colors)
        {
            legend.add(new BoxText(new Brush((Color)property.getValue()), property.getName(), graphics), CompositeView.Y_BT);
        }
        return legend;
    }

    @Override
    public Brush getDefaultBrush()
    {
        return getBrushByTag(TAG_DEFAULT);
    }
    
    @Override
    public void setDefaultBrush(Brush defaultBrush)
    {
        super.setDefaultBrush( defaultBrush );
        colors.add( new DynamicProperty( TAG_DEFAULT, Color.class, defaultBrush.getColor() ) );
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( colors == null ) ? 0 : colors.hashCode() );
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if( this == obj )
            return true;
        if( !super.equals( obj ) )
            return false;
        if( getClass() != obj.getClass() )
            return false;
        AutoTagColorScheme other = (AutoTagColorScheme)obj;
        if(!Objects.equals( colors.asMap(), other.colors.asMap() ))
            return false;
        return true;
    }

}
