package ru.biosoft.bsa.view.colorscheme;

import java.awt.Graphics;

import ru.biosoft.bsa.Site;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.CompositeView;

/**
 * Composite color scheme - a container for color schemes.
 * It consists of array of color schemes which are applying
 * One by one beginning from first. If first scheme is not
 * suitable - then second is applied and so on.
 */
public class CompositeColorScheme extends AbstractSiteColorScheme
{
    
    public CompositeColorScheme()
    {
        super("CompositeColorScheme");
    }

    ////////////////////////////////////////
    // Properties
    //
    private SiteColorScheme[] schemes = new SiteColorScheme[0];

    public SiteColorScheme getColorSchemes(int i)
    {
        return schemes[0];
    }

    /** @pending extended property change notification: array item changed */
    public void setColorSchemes(int i, SiteColorScheme siteColorScheme)
    {
        siteColorScheme.setParent(this);
        schemes[i] = siteColorScheme;
        firePropertyChange("colorSchemes", null, null);
    }

    public SiteColorScheme[] getColorSchemes()
    {
        return schemes;
    }

    public void setColorSchemes(SiteColorScheme[] schemes)
    {
        SiteColorScheme[] oldValue = this.schemes;
        this.schemes = schemes;
        for (int i=0; i<schemes.length; i++)
            schemes[i].setParent(this);

        firePropertyChange("colorSchemes", oldValue, schemes);
    }

    /** @pending extended property change notification: array item added */
    public void addColorScheme(SiteColorScheme scheme)
    {
        SiteColorScheme[] newSchemes = new SiteColorScheme[schemes.length+1];
        System.arraycopy(schemes, 0, newSchemes, 0, schemes.length);

        scheme.setParent(this);
        newSchemes[schemes.length] = scheme;

        SiteColorScheme[] oldValue = schemes;
        schemes = newSchemes;
        firePropertyChange("colorSchemes", oldValue, schemes);
    }

    ////////////////////////////////////////
    // SiteColorScheme interface implementation
    //

    /**
     * Is site suitable for this scheme?
     * @return true if this scheme can assign sensible color to this site
     */
    @Override
    public boolean isSuitable(Site site)
    {
        for(int i=0; i<schemes.length; i++)
        {
            if(schemes[i].isSuitable(site))
                return true;
        }

        return false;
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
        for(int i=0; i<schemes.length; i++)
        {
            if (schemes[i].isSuitable(site))
                return schemes[i].getBrush(site);
        }

        return getDefaultBrush();
    }

    /**
     * Gets View Legend - graphically represented content of color scheme.
     * @param a graphics to writing Legend in.
     * @return view with scheme painted on it.
     */
    @Override
    public CompositeView getLegend(Graphics graphics)
    {
        CompositeView cv = new CompositeView();

        for (int i = 0; i < schemes.length; i++)
            cv.add(schemes[i].getLegend(graphics), CompositeView.X_RL, INSETS);

        cv.add(createTitle(graphics), CompositeView.X_LL|CompositeView.Y_TB, INSETS);
        return cv;
    }
}



