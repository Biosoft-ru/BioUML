
package ru.biosoft.bsa.view.colorscheme;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.util.Objects;

import com.developmentontheedge.beans.DynamicProperty;

import ru.biosoft.bsa.Site;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.util.ColorUtils;


/**
 * Site weigh color scheme. This scheme is designed for
 * brushing site in dependency of this weight.
 * Now this scheme simple creates brush with color between
 * <CODE>firstColor</CODE> and <CODE>secondColor</CODE> directly
 * depending on this weight.
 */
public class SiteWeightColorScheme extends AbstractSiteColorScheme
{
   
    public SiteWeightColorScheme()
    {
        super("By weight");
    }

    ////////////////////////////////////////
    // Properties
    //

    private Color firstColor = Color.white;
    public Color getFirstColor()
    {
        return firstColor;
    }
    public void setFirstColor(Color firstColor)
    {
        Color oldValue = this.firstColor;
        this.firstColor = firstColor;


        firePropertyChange("firstColor", oldValue, firstColor);//siteColorScheme/
    }

    private Color secondColor = Color.blue;
    public Color getSecondColor()
    {
        return secondColor;
    }
    public void setSecondColor(Color secondColor)
    {
        Color oldValue = this.secondColor;
        this.secondColor = secondColor;
        firePropertyChange("secondColor", oldValue, secondColor);//siteColorScheme/
    }

    private String weightProperty = Site.SCORE_PROPERTY;
    public String getWeightProperty()
    {
        return weightProperty;
    }
    public void setWeightProperty(String weightProperty)
    {
        Object oldValue = this.weightProperty;
        this.weightProperty = weightProperty;
        firePropertyChange( "weightProperty", oldValue, weightProperty );
    }
    
    private double minValue = 0;
    public double getMinValue()
    {
        return minValue;
    }
    public void setMinValue(double minValue)
    {
        Object oldValue = this.minValue;
        this.minValue = minValue;
        firePropertyChange( "minValue", oldValue, minValue );
    }

    private double maxValue = 1;
    public double getMaxValue()
    {
        return maxValue;
    }
    public void setMaxValue(double maxValue)
    {
        Object oldValue = this.maxValue;
        this.maxValue = maxValue;
        firePropertyChange( "maxValue", oldValue, maxValue );
    }

    ////////////////////////////////////////
    // SiteColorScheme interface implementation
    //

    /**
     * Is site suiable for this scheme?
     * @return true if this scheme can assign sensible color to this site
     */
    @Override
    public boolean isSuitable(Site site)
    {
        //if (site.getProperties() == null || site.getProperties().getMethodInfo() == null)
        //    return false;

        return true;
    }

    private Brush getBrush(double weight)
    {
        // create new brush
        if(getMinValue() == getMaxValue())
            return getDefaultBrush();
        weight = ( weight - getMinValue() ) / ( getMaxValue() - getMinValue() );
        
        if( weight < 0 )
            weight = 0;
        if( weight > 1 )
            weight = 1;

        Color color = ColorUtils.mix(firstColor, secondColor, weight);
        return new Brush(color);
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
        //MethodInfo info = null;
        //if (site.getProperties() != null) info = site.getProperties().getMethodInfo();
        //return getBrush(info == null ? -1 : info.getWeight());
        if(site.getProperties() != null)
        {
            DynamicProperty score = site.getProperties().getProperty(getWeightProperty());
            if(score != null && score.getValue() != null && score.getValue() instanceof Number)
                return getBrush( ( (Number)score.getValue() ).doubleValue() );
        }
        return getDefaultBrush();
    }

    /**
     * Gets View Legend - graphically represented content of color scheme.
     * @param a graphics to writing Legend in.
     * @return view with scheme painted on it.
     *
     * @pending constants should be declared in other place
     */
    @Override
    public CompositeView getLegend(Graphics graphics)
    {
        CompositeView cv = new CompositeView();
        cv.add(createTitle(graphics));

        Point insets1 = (Point)INSETS.clone();
        insets1.y = 0;

        final int NUMBER = 10;
        for(int i = 0; i <= NUMBER; i++)
        {
            Point insets = ( i == 0 ) ? INSETS : insets1;
            double weight = getMinValue() + i * (getMaxValue() - getMinValue()) / NUMBER;
            cv.add(new BoxText(getBrush( weight ), String.format( "%.2f", weight), graphics),
                    CompositeView.Y_BT, insets);
        }

        return cv;
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if( this == obj )
            return true;
        if( !super.equals( obj ) )
            return false;
        SiteWeightColorScheme other = (SiteWeightColorScheme)obj;
        
        if(!Objects.equals( firstColor, other.firstColor ))
            return false;
        
        if(!Objects.equals( secondColor, other.secondColor ))
            return false;

        if( maxValue != other.maxValue )
            return false;
        
        if( minValue != other.minValue )
            return false;
        
        if(!Objects.equals( weightProperty, other.weightProperty ))
            return false;
        
        return true;
    }
    
    
}
