
package ru.biosoft.bsa.view.colorscheme;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.util.Objects;

import com.developmentontheedge.beans.Option;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.Const;
import ru.biosoft.bsa.Track;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.GraphicProperties;
import ru.biosoft.graphics.TextView;
import ru.biosoft.graphics.View;
import ru.biosoft.graphics.font.ColorFont;
import ru.biosoft.util.OptionEx;

/** AbstractSiteColorScheme class is abstract template class for all ColorSchemes. */
abstract public class AbstractSiteColorScheme extends OptionEx implements SiteColorScheme
{
    static final Point INSETS = new Point(20, 5);
    private static final ColorFont TITLE_FONT = GraphicProperties.getInstance().getFont( "Font_SiteColorScheme" );

    /**
     * @pending DataCollection for this ru.biosoft.access.core.DataElement in always localhost/colorschemes.
     * this class has no origin
     */
    protected AbstractSiteColorScheme(Option parentListener, String name)
    {
        this(Const.FULL_COLORSCHEMES.optDataCollection(), parentListener, name);
    }

    protected AbstractSiteColorScheme(DataCollection<?> parent, Option parentListener, String name)
    {
        super(parentListener);
        this.name = name;
        origin = parent;
    }

    /**
     *  Construct data element.
     *  @param name name of the data element.
     *  @param origin origin data collection.
     */
    protected AbstractSiteColorScheme(String name)
    {
        this(null, name);
    }

    ////////////////////////////////////////
    // ru.biosoft.access.core.DataElement interface implementation
    //

    /**
     * Data element name.
     * It is set up in constructor and is declared <code>private</code>
     * to warranty that it can not be changed.
     * @todo Getter for this member is not final, and so may be overrided.
     */
    private String name;

    // implements mgl3.access.ru.biosoft.access.core.DataElement
    @Override
    public String getName()
    {
        return name;
    }

    /**
     * Is site suitable for this scheme?
     * @return true if this scheme can assign sensible color to this site
     */
    @Override
    public void setName(String name)
    {
        this.name = name;
    }

    private final DataCollection<?> origin;

    // implements mgl3.access.DataElement
    @Override
    public DataCollection<?> getOrigin()
    {
        return origin;
    }

    ////////////////////////////////////////
    // Properties
    //

    protected Brush defaultBrush = new Brush(Color.green);

    /**
     * Gets default brush for this scheme.
     * Default brush is used for assigning it to new brushes
     * @return default brush
     */
    @Override
    public Brush getDefaultBrush()
    {
        return defaultBrush;
    }

    /**
     * Sets default brush for this scheme.
     * Default brush is used for assigning it to new brushes
     * @param defaultBrush a default brush
     */
    @Override
    public void setDefaultBrush(Brush defaultBrush)
    {
        Object oldValue = this.defaultBrush;
        this.defaultBrush = defaultBrush;


        firePropertyChange("defaultBrush", oldValue, defaultBrush); //siteColorScheme/
    }

    protected boolean showDefaultBrushInLegend;
    public boolean isShowDefaultBrushInLegend()
    {
        return showDefaultBrushInLegend;
    }

    public void setShowDefaultBrushInLegend(boolean showDefaultBrushInLegend)
    {
        boolean oldValue = this.showDefaultBrushInLegend;
        this.showDefaultBrushInLegend = showDefaultBrushInLegend;

        firePropertyChange("showDefaultBrushInLegend", oldValue, showDefaultBrushInLegend);
    }

    ////////////////////////////////////////
    // Utilites
    //

    protected View createTitle(Graphics g)
    {
        if(isShowDefaultBrushInLegend())
            return new BoxText(getDefaultBrush(), getName(), TITLE_FONT, g);

        return new TextView(getName(), TITLE_FONT, g);
    }

    @Override
    public boolean equals(Object obj)
    {
        if( this == obj )
            return true;
        if( obj == null )
            return false;
        if( getClass() != obj.getClass() )
            return false;
        AbstractSiteColorScheme other = (AbstractSiteColorScheme)obj;
        if(!Objects.equals( getDefaultBrush(), other.getDefaultBrush() ))
            return false;
        if(!Objects.equals( name,  other.name ))
        if( showDefaultBrushInLegend != other.showDefaultBrushInLegend )
            return false;
        return true;
    }

    public void initFromTrack(Track track)
    {
        //do nothing by default
    }
    
   
}

