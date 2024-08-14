package ru.biosoft.bsa.view.colorscheme;

import java.awt.Color;
import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SitePropertyViewTagger;
import ru.biosoft.bsa.Track;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.util.ColorUtils;

/**
 * @author lan
 *
 */
public class SitePropertyColorScheme extends AbstractSiteColorScheme
{
    private static final String TAG_DEFAULT = "(default)";

    private String colorProperty;
    private DynamicPropertySetAsMap colors = null;
    private SitePropertyViewTagger viewTagger = new SitePropertyViewTagger( TAG_DEFAULT );
    private String[] siteProperties = new String[0];
    private boolean isInitialized = false;
    
    public SitePropertyColorScheme()
    {
        super( "By property" );
        initColors();
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
        if( !isInitialized && site != null )
            initSiteProperties( site, null );
        String tag = site == null ? null : viewTagger.getTag( site );
        if(tag == null || tag.isEmpty()) tag = "(default)";
        return getBrushByTag(tag);
    }


    private void initSiteProperties(Site site, String colorProperty)
    {
        if( isInitialized )
            return;
        List<String> result = new ArrayList<>();
        Iterator<String> nameIterator = site.getProperties().nameIterator();
        while( nameIterator.hasNext() )
        {
            result.add( nameIterator.next() );
        }
        isInitialized = true;
        setSiteProperties( result.toArray( new String[0] ) );
        if( colorProperty == null && !result.isEmpty() )
            colorProperty = result.iterator().next();
        setColorProperty( colorProperty );
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
            Color color = tag.equals( TAG_DEFAULT ) ? defaultColor : ColorUtils.getDefaultColor( colors.size() - 1 );
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
        SitePropertyColorScheme other = (SitePropertyColorScheme)obj;
        if(!Objects.equals( colors.asMap(), other.colors.asMap() ))
            return false;
        return true;
    }

    @PropertyName ( "Color by property" )
    @PropertyDescription ( "Select property to use for site colouring" )
    public String getColorProperty()
    {
        return colorProperty;
    }

    public void setColorProperty(String colorProperty)
    {
        if( colorProperty.equals( this.colorProperty ) )
            return;
        Object oldValue = this.colorProperty;
        this.colorProperty = colorProperty;
        viewTagger.setTagProperty( colorProperty );
        initColors();
        firePropertyChange( "colorProperty", oldValue, colorProperty );
    }

    private void initColors()
    {
        colors = new DynamicPropertySetAsMap();
        colors.add( new DynamicProperty( TAG_DEFAULT, Color.class, defaultBrush.getColor() ) );
        colors.addPropertyChangeListener( new PropertyChangeListener()
        {
            @Override
            public void propertyChange(PropertyChangeEvent evt)
            {
                firePropertyChange( "colors/" + evt.getPropertyName(), evt.getOldValue(), evt.getNewValue() );
            }
        } );
    }

    public String[] getSiteProperties()
    {
        return siteProperties;
    }

    public void setSiteProperties(String[] properties)
    {
        Object oldValue = siteProperties;
        siteProperties = properties;
        firePropertyChange( "siteProperties", oldValue, siteProperties );
    }

    @Override
    public void initFromTrack(Track track)
    {
        try
        {
            Site site = track.getAllSites().iterator().next();
            if( site != null )
            {
                String colorTag = null;
                if( track instanceof DataCollection )
                {
                    colorTag = ( (DataCollection)track ).getInfo().getProperty( "colorProperty" );
                    initSiteProperties( site, colorTag );
                }
            }
        }
        catch( Exception e )
        {

        }
        super.initFromTrack( track );
    }

}
