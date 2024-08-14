
/**
 * Author:  Igor V. Tyazhev  (champ@developmentontheedge.com)
 *
 * Created: 13.02.2001
 *
 * Description:
 *
 * Copyright (C) 2000, 2001 DevelopmentOnTheEdge.com. All rights reserved.
 */

package ru.biosoft.bsa.view;


import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.DefaultSiteViewTagger;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.view.colorscheme.AbstractSiteColorScheme;
import ru.biosoft.bsa.view.colorscheme.AutoTagColorScheme;
import ru.biosoft.bsa.view.colorscheme.SiteColorScheme;
import ru.biosoft.bsa.view.colorscheme.SitePropertyColorScheme;
import ru.biosoft.bsa.view.colorscheme.SiteWeightColorScheme;
import ru.biosoft.bsa.view.sitelayout.OptimizedListLayoutAlgorithm;
import ru.biosoft.bsa.view.sitelayout.SiteLayoutAlgorithm;
import ru.biosoft.graphics.GraphicProperties;
import ru.biosoft.graphics.font.ColorFont;
import ru.biosoft.util.OptionEx;
import ru.biosoft.util.bean.JSONBean;


/**
 * Class to store options for site painting.
 */
@SuppressWarnings ( "serial" )
@PropertyName("Track options")
@PropertyDescription("How track and sites will be displayed")
public class SiteViewOptions extends OptionEx implements JSONBean
{
    /**
     * Creates <CODE>SiteViewOptions</CODE> and initializes it.
     */
    public SiteViewOptions()
    {
        this(null, GraphicProperties.getInstance().getFont("Font_SiteColorScheme"));
    }

    /**
     * Creates <CODE>SiteViewOptions</CODE> and initializes it.
     *
     * @param parent   parent property
     */
    public SiteViewOptions(Option parent)
    {
        this(parent, GraphicProperties.getInstance().getFont("Font_SiteColorScheme"));
    }

    /**
     * Creates <CODE>SiteViewOptions</CODE> and initializes it to the specified font.
     *
     * @param parent   parent property
     * @param font   the specified font
     */
    public SiteViewOptions(Option parent, ColorFont font)
    {
        this(parent, font, 6);
    }

    /**
     * Creates <CODE>MapViewOptions</CODE> and initializes it to the specified options.
     *
     * @param parent   parent property
     * @param font     the specified font
     * @param penWidth width of the pen which used for site painting
     * @param penColor color of the pen which used for site painting
     */
    public SiteViewOptions(Option parent, ColorFont font, int penWidth)
    {
        this(parent, font, penWidth, 3);
    }

    /**
     * Creates <CODE>MapViewOptions</CODE> and initializes it to the specified options.
     *
     * @param parent   parent property
     * @param font     the specified font
     * @param penWidth width of the pen which used for site painting
     * @param penColor color of the pen which used for site painting
     * @param interval distance between site marker and site label.
     */
    public SiteViewOptions(Option parent, ColorFont font, int penWidth, int interval)
    {
        super(parent);
        this.font = font;
        this.boxHeight = penWidth;
        this.interval = interval;
        if( font != null )
            seqFont = new ColorFont(font.getFont(), Color.black);

        this.trackTitleFont = GraphicProperties.getInstance().getFont("Font_Title");

        // default values
        showTitle = true;
        showBox = false;
        showSequence = false;
        showStrand = true;
        showPositions = false;
        showStructure = true;
        layoutAlgorithm = new OptimizedListLayoutAlgorithm();
        viewTagger = new DefaultSiteViewTagger();
        setColorScheme( schemes.values().iterator().next() );
    }

    /**
     * <CODE>ColorFont</CODE> for labels painting.
     */
    private ColorFont font;

    @PropertyName("Site font")
    @PropertyDescription("Font used for painting site label, sequence and position")
    public ColorFont getFont()
    {
        return font;
    }

    public void setFont(ColorFont font)
    {
        ColorFont oldValue = this.font;
        this.font = font;
        if( font != null )
            seqFont = new ColorFont(font.getFont(), Color.black);
        firePropertyChange("font", oldValue, font);
    }

    private ColorFont trackTitleFont;

    @PropertyName("Track title font")
    @PropertyDescription("Font used for track title")
    public ColorFont getTrackTitleFont()
    {
        return trackTitleFont;
    }

    public void setTrackTitleFont(ColorFont trackTitleFont)
    {
        ColorFont oldValue = this.trackTitleFont;
        this.trackTitleFont = trackTitleFont;
        firePropertyChange("trackTitleFont", oldValue, trackTitleFont);
    }

    private ColorFont seqFont;

    public ColorFont getSequenceFont()
    {
        return seqFont;
    }

    /**
     * Layout algorithm used to optimeze locations of the sites
     */
    private SiteLayoutAlgorithm layoutAlgorithm;

    @PropertyName("Layout algorithm")
    @PropertyDescription("Layout algorithm used to optimeze locations of the sites")
    public SiteLayoutAlgorithm getLayoutAlgorithm()
    {
        return layoutAlgorithm;
    }

    public void setLayoutAlgorithm(SiteLayoutAlgorithm layoutAlgorithm)
    {
        SiteLayoutAlgorithm oldValue = this.layoutAlgorithm;
        this.layoutAlgorithm = layoutAlgorithm;
        firePropertyChange("layoutAlgorithm", oldValue, layoutAlgorithm);
    }
    
    private ViewTagger viewTagger;
    
    public ViewTagger getViewTagger()
    {
        return viewTagger;
    }

    /**
     * @param viewTagger the viewTagger to set
     */
    public void setViewTagger(ViewTagger viewTagger)
    {
        Object oldValue = this.viewTagger;
        this.viewTagger = viewTagger;
        firePropertyChange("viewTagger", oldValue, viewTagger);
    }

    /**
     * Distance between site marker and site label.
     */
    private int interval;

    @PropertyName("Interval")
    @PropertyDescription("Interval between site marker and label, sequence and positions")
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
     * Height of the box (if site painted as box).
     */
    private int boxHeight;

    @PropertyName("Size")
    @PropertyDescription("Height of the site, size of the arrow and so on")
    public int getBoxHeight()
    {
        return boxHeight;
    }

    public void setBoxHeight(int boxHeight)
    {
        int oldValue = this.boxHeight;
        this.boxHeight = boxHeight;
        firePropertyChange("boxHeight", oldValue, boxHeight);
    }
    
    private int maxProfileHeight = 200;

    /**
     * @return the maxProfileHeight
     */
    @PropertyName("Max profile height")
    @PropertyDescription("Max profile height in pixels")
    public int getMaxProfileHeight()
    {
        return maxProfileHeight;
    }

    /**
     * @param maxProfileHeight the maxProfileHeight to set
     */
    public void setMaxProfileHeight(int maxProfileHeight)
    {
        Object oldValue = this.maxProfileHeight;
        this.maxProfileHeight = maxProfileHeight;
        firePropertyChange("maxProfileHeight", oldValue, maxProfileHeight);
    }
    
    private int minProfileHeight = 15;
    
    public int getMinProfileHeight()
    {
        return minProfileHeight;
    }

    public void setMinProfileHeight(int minProfileHeight)
    {
        Object oldValue = this.minProfileHeight;
        this.minProfileHeight = minProfileHeight;
        firePropertyChange( "minProfileHeight", oldValue, minProfileHeight );
    }

    /**
     * Specifies if site title should be drawn
     */
    protected boolean showTitle;

    @PropertyName("Title visible")
    @PropertyDescription("Should be title of the site displayed?")
    public boolean isShowTitle()
    {
        return showTitle;
    }

    public void setShowTitle(boolean showTitle)
    {
        boolean oldValue = this.showTitle;
        this.showTitle = showTitle;
        firePropertyChange("showTitle", oldValue, showTitle);
    }

    /**
     * Specifies if site title should be drawn as box
     */
    protected boolean showBox;

    @PropertyName("As box")
    @PropertyDescription("Should be site displayed as box?")
    public boolean isShowBox()
    {
        return showBox;
    }

    public void setShowBox(boolean showBox)
    {
        boolean oldValue = this.showBox;
        this.showBox = showBox;
        firePropertyChange("showBox", oldValue, showBox);
    }
    
    /**
     * Specifies whether to render internal site structure (if it's available in site properties)
     */
    protected boolean showStructure;

    @PropertyName("Structure is visible")
    @PropertyDescription("Should internal structure of site be visible (if available)?")
    public boolean isShowStructure()
    {
        return showStructure;
    }

    public void setShowStructure(boolean showStructure)
    {
        Object oldValue = this.showStructure;
        this.showStructure = showStructure;
        firePropertyChange("showStructure", oldValue, this.showStructure);
    }

    /**
     * Specifies if site sequence should be drawn
     */
    protected boolean showSequence;

    @PropertyName("Sequence visible")
    @PropertyDescription("Should be sequence of the site displayed?")
    public boolean isShowSequence()
    {
        return showSequence;
    }

    public void setShowSequence(boolean showSequence)
    {
        boolean oldValue = this.showSequence;
        this.showSequence = showSequence;
        firePropertyChange("showSequence", oldValue, showSequence);
    }

    /**
     * Specifies if strand of site (arrow) should be drawn
     */
    protected boolean showStrand;

    @PropertyName("Strand visible")
    @PropertyDescription("Should be strand of the site displayed?")
    public boolean isShowStrand()
    {
        return showStrand;
    }

    public void setShowStrand(boolean showStrand)
    {
        boolean oldValue = this.showStrand;
        this.showStrand = showStrand;
        firePropertyChange("showStrand", oldValue, showStrand);
    }

    private int displayName = 2;

    public static final int DISPLAY_SITE_NAME = 0;
    public static final int DISPLAY_SITE_TYPE = 1;
    public static final int DISPLAY_SITE_TITLE = 2;
    public static final int DISPLAY_SITE_TF_NAME = 3;
    public static final int DISPLAY_SITE_STRAND = 4;

    @PropertyName("Label type")
    @PropertyDescription("Type of label for site")
    public int getDisplayName()
    {
        return displayName;
    }

    public void setDisplayName(int displayName)
    {
        int oldValue = this.displayName;
        this.displayName = displayName;
        firePropertyChange("displayName", oldValue, displayName);
    }

    protected boolean showPositions;

    @PropertyName("Positions visible")
    @PropertyDescription("Should be positions of the site displayed?")
    public boolean isShowPositions()
    {
        return showPositions;
    }

    public void setShowPositions(boolean showPositions)
    {
        boolean oldValue = this.showPositions;
        this.showPositions = showPositions;
        firePropertyChange("showPositions", oldValue, showPositions);
    }

    public static final String TRACK_MODE_FULL = "Full";
    public static final String TRACK_MODE_COMPACT = "Compact";
    private static final String[] TRACK_DISPLAY_MODES = {TRACK_MODE_FULL, TRACK_MODE_COMPACT};
    protected String trackDisplayMode = TRACK_MODE_FULL;
    
    public String getTrackDisplayMode()
    {
        return trackDisplayMode;
    }

    public void setTrackDisplayMode(String trackDisplayMode)
    {
        Object oldValue = this.trackDisplayMode;
        this.trackDisplayMode = trackDisplayMode;
        firePropertyChange("trackDisplayMode", oldValue, trackDisplayMode);
    }
    
    protected Map<String, AbstractSiteColorScheme> schemes = StreamEx.<AbstractSiteColorScheme>
            of( new AutoTagColorScheme(), new SiteWeightColorScheme(), new SitePropertyColorScheme() )
            .mapToEntry( SiteColorScheme::getName, a->a ).toCustomMap( LinkedHashMap::new );
    
    private AbstractSiteColorScheme scheme;
    
    @PropertyName("Color scheme")
    @PropertyDescription("How to color sites")
    public String getColorSchemeName()
    {
        return scheme.getName();
    }
    
    public void setColorSchemeName(String name)
    {
        String oldValue = getColorSchemeName();
        AbstractSiteColorScheme newScheme = schemes.get( name );
        if(newScheme != null)
        {
            setColorScheme(newScheme);
            firePropertyChange( "colorSchemeName", oldValue, name );
        }
    }
    
    public boolean isColorSchemeSelectorHidden()
    {
        return schemes.size() < 2;
    }

    @PropertyName("Coloring options")
    @PropertyDescription("Coloring options")
    public AbstractSiteColorScheme getColorScheme()
    {
        return scheme;
    }
    
    public void setColorScheme(AbstractSiteColorScheme scheme)
    {
        AbstractSiteColorScheme oldValue = scheme;
        this.scheme = withPropagation( oldValue, scheme );
        firePropertyChange( "colorScheme", oldValue, scheme );
    }

    public SiteViewOptions clone(Option parent)
    {
        SiteViewOptions siteViewOptions = new SiteViewOptions(parent);
        siteViewOptions.setFont(new ColorFont(font.getFont(), font.getColor()));
        siteViewOptions.setInterval(interval);
        siteViewOptions.setTrackTitleFont(new ColorFont(trackTitleFont.getFont(), trackTitleFont.getColor()));
        siteViewOptions.setBoxHeight(boxHeight);
        siteViewOptions.setShowTitle(showTitle);
        siteViewOptions.setShowBox(showBox);
        siteViewOptions.setShowSequence(showSequence);
        siteViewOptions.setShowStrand(showStrand);
        siteViewOptions.setShowPositions(showPositions);
        siteViewOptions.setTrackDisplayMode(trackDisplayMode);
        siteViewOptions.setViewTagger(viewTagger);
        return siteViewOptions;
    }
    
    public static String[] getTrackDisplayModes()
    {
        return TRACK_DISPLAY_MODES.clone();
    }

    public void initFromTrack(Track track)
    {
        schemes.values().stream().forEach( scheme -> scheme.initFromTrack( track ) );
        if( track instanceof DataCollection )
        {
            String schemeName = ( (DataCollection)track ).getInfo().getProperty( "colorScheme" );
            if(schemeName != null)
            {
                setColorSchemeName( schemeName );
            }
        }
    }
}
