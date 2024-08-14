package ru.biosoft.bsa.view.colorscheme;

import java.awt.Graphics;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.bsa.Site;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.CompositeView;

import com.developmentontheedge.beans.Option;

/**
* Interface SiteColorScheme
* for every site we calculate it`s color
* using colorSchemes
*/
public interface SiteColorScheme extends DataElement
{
    /**
    * Is site suiable for this scheme?
    * @return true if this scheme can assign sensible color to this site
    */
    public boolean isSuitable(Site site);
    /**
    * Function, which all colorschemes are spinned around.
    * It returns Brush to paint this site.
    * If a site has big misery not to be suitable for this scheme,
    * getBrush returns default brush
    * @return brush for this site
    */
    public Brush getBrush(Site site);
    /**
    * Gets View Legend - graphically represented content of color scheme.
    * @param a graphics to writing Legend in.
    * @return view with scheme painted on it.
    */
    public CompositeView getLegend(Graphics graphics);
    /**
    * Gets default brush for this scheme.
    * Default brush is used for assigning it to new brushes
    * @return default brush
    */
    public Brush getDefaultBrush();
    /**
    * Sets default brush for this scheme.
    * Default brush is used for assigning it to new brushes
    * @param defaultBrush a default brush
    */
    public void   setDefaultBrush(Brush defaultBrush);
    /**
    * Set name of color scheme
    */
    public void   setName(String name);

    /**
    * Set parent property change listener
    */
    public void setParent( Option parent );
}
