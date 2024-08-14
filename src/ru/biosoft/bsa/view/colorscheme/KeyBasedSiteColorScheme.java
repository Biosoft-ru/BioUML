package ru.biosoft.bsa.view.colorscheme;

import java.awt.Graphics;
import java.awt.Color;
import java.util.StringTokenizer;

import ru.biosoft.bsa.Site;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.CompositeView;


/**
 * Key based site color scheme - scheme based on keys.
 * Keys are generated for every site using SiteKeyGenerator.
 * Then key follows to ColorScheme, which determines color for
 * this key.
 */
public class KeyBasedSiteColorScheme extends AbstractSiteColorScheme
{
    public KeyBasedSiteColorScheme()
    {
        super("By key");
    }

    ////////////////////////////////////////
    // ColorScheme interface implementation
    //

    /**
     * Is site suitable for this scheme?
     * @return true if this scheme can assign sensible color to this site
     */
    @Override
    public boolean isSuitable(Site site)
    {
        return generator.isSuitable(site);
    }

    /**
     * Gets View Legend - graphically represented content of color scheme.
     * @param a graphics to writing Legend in.
     * @return view with scheme painted on it.
     */
    @Override
    public CompositeView getLegend(Graphics graphics)
    {
        CompositeView composite = new CompositeView();
        composite.add(createTitle(graphics));
        composite.add(colorGroup.getView(graphics), CompositeView.Y_BT);
        return composite;
    }

    @Override
    public Brush getDefaultBrush()
    {
        return colorGroup.getBrush();
    }

    @Override
    public void setDefaultBrush(Brush brush)
    {
        defaultBrush = brush;
        colorGroup.setBrush(brush);
        firePropertyChange("defaultBrush", null, null);
    }

    /**
     * This method returns Brush to paint this site.
     * If a site is not suitable for this scheme,
     * getBrush returns default brush
     * @return brush for this site
     */
    @Override
    public Brush getBrush(Site site)
    {
        Brush brush = colorGroup.getBrush();
        if(generator.isSuitable(site))
        {
            String key = generator.getKey(site);
            if (key != null)
            {
                StringTokenizer tokens = new StringTokenizer(key, keyDelimiters);
                KeyColorGroup group = colorGroup.getGroup(tokens);
                brush = group.getBrush();
            }
        }

        return brush;
    }


    ////////////////////////////////////////
    // Specific properties
    //

    protected SiteToKeyGenerator generator = new SiteTypeKeyGenerator();

    /**
     * Get Site to key generator
     * @return site to key generator
     */
    public SiteToKeyGenerator getKeyGenerator()
    {
        return generator;
    }

    /**
     * Set Site to key generator
     * @param keyGenerator a site to key generator
     */
    public void setKeyGenerator(SiteToKeyGenerator keyGenerator)
    {
        generator = keyGenerator;
    }

    private String keyDelimiters = ";";

    /**
     * Get Key delimiters - delimiters of simple keys in composite key string
     * @return key delimiters
     */
    public String getKeyDelimiters()
    {
        return keyDelimiters;
    }

    /**
     * Set Key delimiters - delimiters of simple keys in composite key string
     * @param key delimiters
     */
    public void setKeyDelimiters(String delimiters)
    {
        String old = keyDelimiters;
        keyDelimiters = delimiters;
        firePropertyChange("keyDelimiters", old, keyDelimiters);//siteColorScheme/
    }

    private KeyColorGroup colorGroup = new KeyColorGroup( this, "default", new Brush( Color.GREEN ) );

    /**
     * Get KeyColorGroup - first group in color groups tree hierarchy
     * @return Color group
     */
    public KeyColorGroup getColorGroup()
    {
        return colorGroup;
    }

    /**
     * Set KeyColorGroup
     * @param Color group
     */
    public void setColorGroup(KeyColorGroup group)
    {
        KeyColorGroup old = colorGroup;
        colorGroup = group;
        colorGroup.setParent(this);
        firePropertyChange("colorGroup", old, colorGroup);//siteColorScheme/
    }

    public void setBrush(String groupName, Brush brush)
    {
        KeyColorGroup group = colorGroup.provideGroup(new StringTokenizer(groupName, keyDelimiters));
        group.setBrush(brush);
    }

    public void setBrush(String groupName, Color color)
    {
        setBrush(groupName, new Brush(color));
    }
}
