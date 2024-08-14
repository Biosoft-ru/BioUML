
/**
 * Author:  Igor V. Tyazhev  (champ@developmentontheedge.com)
 *
 * Created: 28.03.2001
 *
 * Description:
 *
 * Copyright (C) 2000, 2001 DevelopmentOnTheEdge.com. All rights reserved.
 */

package ru.biosoft.bsa.view;

import java.awt.Color;

import ru.biosoft.graphics.GraphicProperties;
import ru.biosoft.graphics.RulerOptions;
import ru.biosoft.graphics.font.ColorFont;

import com.developmentontheedge.beans.Option;

/**
 *  Class contains main properties of the map view.
 */
@SuppressWarnings ( "serial" )
public class MapViewOptions extends Option
{
    /** Stores the maximum width of sub-maps when map painted as a set of map one under another. */
    public static final int BIG_NUMBER = 900000000; // 900 000 000

    public MapViewOptions()
    {
        this(null, GraphicProperties.getInstance().getFont("Font_Bold_Title"));
    }

    /**
     * Creates default <code>ViewOptions</code>.
     */
    public MapViewOptions(Option parent)
    {
        this(parent, GraphicProperties.getInstance().getFont("Font_Bold_Title"));
    }


    /**
     * Creates <code>ViewOptions</code> and initializes it to the specified font.
     * @param font the specified font
     */
    public MapViewOptions(Option parent, ColorFont font)
    {
        super(parent);
        // Ruler options
        ColorFont cfont = GraphicProperties.getInstance().getFont("Font_RulerDefault");
        RulerOptions rulerOptions = new RulerOptions(cfont);

        // SequenceViewOptions
        cfont = GraphicProperties.getInstance().getFont("Font_SequenceViewDefault");
        SequenceViewOptions sequenceViewOptions = new SequenceViewOptions(this, cfont, rulerOptions, SequenceViewOptions.PT_RULER);

        init(font, sequenceViewOptions, BIG_NUMBER, 10, 38, new Color(206, 255, 206));
    }

    /**
     * Creates <code>ViewOptions</code> and initializes it to the specified options.
     * @param font the specified font
     * @param sequenceViewOptions the specified <code>SequenceViewOptions</code>
     * @param siteViewOptions the specified <code>SiteViewOptions</code>
     */
    public MapViewOptions(Option parent, ColorFont font, SequenceViewOptions sequenceViewOptions)
    {
        this(parent, font, sequenceViewOptions, BIG_NUMBER);
    }

    /**
     * Creates <code>ViewOptions</code> and initializes it to the specified options.
     * @param font the specified font
     * @param sequenceViewOptions the specified <code>SequenceViewOptions</code>
     * @param siteViewOptions the specified <code>SiteViewOptions</code>
     * @param maxWidth width of sub-maps
     */
    public MapViewOptions(Option parent, ColorFont font, SequenceViewOptions sequenceViewOptions, int maxWidth)
    {
        this(parent, font, sequenceViewOptions, maxWidth, 10);
    }

    /**
     * Creates <code>ViewOptions</code> and initializes it to the specified options.
     * @param font     the specified font
     * @param sequenceViewOptions the specified <code>SequenceViewOptions</code>
     * @param siteViewOptions the specified <code>SiteViewOptions</code>
     * @param maxWidth width of sub-maps
     * @param interval distance between sub-maps in vertical direction.
     */
    public MapViewOptions(Option parent, ColorFont font, SequenceViewOptions sequenceViewOptions, int maxWidth, int interval)
    {
        this(parent, font, sequenceViewOptions, maxWidth, interval, 38, new Color(206, 255, 206));
    }

    /**
     * Creates <code>ViewOptions</code> and initializes it to the specified options.
     * @param font     the specified font
     * @param sequenceViewOptions the specified <code>SequenceViewOptions</code>
     * @param siteViewOptions the specified <code>SiteViewOptions</code>
     * @param maxWidth width of sub-maps
     * @param interval distance between sub-maps in vertical direction.
     */
    public MapViewOptions(Option parent, ColorFont font, SequenceViewOptions sequenceViewOptions, int maxWidth, int interval, int region,
            Color color)
    {
        super(parent);
        init(font, sequenceViewOptions, maxWidth, interval, region, color);
    }

    /**
     * Creates <code>ViewOptions</code> and initializes it to the specified options.
     * @param font     the specified font
     * @param sequenceViewOptions the specified <code>SequenceViewOptions</code>
     * @param siteViewOptions the specified <code>SiteViewOptions</code>
     * @param maxWidth width of sub-maps
     * @param interval distance between sub-maps in vertical direction.
     */
    public MapViewOptions(Option parent, ColorFont font, SequenceViewOptions sequenceViewOptions, int maxWidth, int interval, int region)
    {
        super(parent);
        init(font, sequenceViewOptions, maxWidth, interval, region, new Color(206, 255, 206));
    }

    /**
     * @pending high color scheme initialization - how we should do it?
     */
    protected void init(ColorFont font, SequenceViewOptions sequenceViewOptions, int maxWidth, int interval, int region, Color color)
    {
        this.font = font;
        this.sequenceViewOptions = sequenceViewOptions;
        this.sequenceViewOptions.setParent(this);

        this.maxWidth = maxWidth;
        this.interval = interval;
        this.region = region;
        this.regionColor = color;
        sequenceViewOptions.setDensity(3.0);
    }

    ////////////////////////////////////////////////////////
    //
    // Properties
    //

    /** <code>ColorFont</code> for different graphical text outputs. */
    private ColorFont font;
    public ColorFont getFont()
    {
        return font;
    }
    public void setFont(ColorFont font)
    {
        ColorFont oldValue = this.font;
        this.font = font;
        firePropertyChange("font", oldValue, font);
    }

    /** <code>SequenceViewOptions</code> for sequence painting. */
    private SequenceViewOptions sequenceViewOptions;
    public SequenceViewOptions getSequenceViewOptions()
    {
        return sequenceViewOptions;
    }
    public void setSequenceViewOptions(SequenceViewOptions sequenceViewOptions)
    {
        SequenceViewOptions oldValue = this.sequenceViewOptions;
        this.sequenceViewOptions = sequenceViewOptions;
        this.sequenceViewOptions.setParent(this);
        firePropertyChange("sequenceViewOptions", oldValue, sequenceViewOptions);
    }

    /** Max width of the sub-maps when map painted as a set of map one under another. */
    private int maxWidth;
    public int getMaxWidth()
    {
        return maxWidth;
    }
    public void setMaxWidth(int maxWidth)
    {
        int oldValue = this.maxWidth;
        this.maxWidth = maxWidth > 0 ? maxWidth : 1;
        firePropertyChange("maxWidth", oldValue, maxWidth);
    }

    /** Distance between sub-maps in vertical direction. */
    private int interval;
    public int getInterval()
    {
        return interval;
    }
    public void setInterval(int interval)
    {
        int oldValue = this.interval;
        this.interval = interval;
        firePropertyChange("interval", oldValue, interval);
    }

    /**
     * Color of slection in LinkedMapMode
     */
    private Color regionColor;
    public Color getRegionColor()
    {
        return regionColor;
    }
    public void setRegionColor(Color regionColor)
    {
        Color oldValue = this.regionColor;
        this.regionColor = regionColor;
        firePropertyChange("regionColor", oldValue, regionColor);

    }

    /**
     * Width of selection in LinkedMapMode
     */
    int region;
    public int getRegion()
    {
        return region;
    }
    public void setRegion(int region)
    {
        int oldValue = this.region;
        this.region = region;
        firePropertyChange("region", oldValue, region);
    }

    private int maxSequenceLength;

    public void setMaxSequenceLength(int maxSequenceLength)
    {
        this.maxSequenceLength = maxSequenceLength;
    }

    public int getMaxSequenceLength()
    {
        return maxSequenceLength;
    }

    public MapViewOptions clone(Option parent)
    {
        MapViewOptions mapViewOptions = new MapViewOptions(parent);
        mapViewOptions.setFont(new ColorFont(font.getFont(), font.getColor()));
        mapViewOptions.setSequenceViewOptions(sequenceViewOptions.clone(mapViewOptions));
        mapViewOptions.setMaxWidth(maxWidth);
        mapViewOptions.setMaxSequenceLength(maxSequenceLength);
        mapViewOptions.setInterval(interval);
        mapViewOptions.setRegion(region);
        mapViewOptions.setRegionColor(regionColor);

        return mapViewOptions;
    }
}
