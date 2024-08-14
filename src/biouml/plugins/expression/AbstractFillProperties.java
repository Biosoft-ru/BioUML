package biouml.plugins.expression;

import java.awt.Color;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.util.ColorUtils;
import ru.biosoft.util.Pair;
import ru.biosoft.util.bean.JSONBean;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import com.developmentontheedge.beans.Option;

/**
 * @author lan
 *
 */
@SuppressWarnings ( "serial" )
public abstract class AbstractFillProperties extends Option implements JSONBean
{
    protected Pair<Double, Double> tableMinMax = new Pair<>( -1.0, 1.0 );
    private Color color1 = Color.BLUE;
    private Color color2 = Color.RED;
    private Color colorZero = Color.WHITE;
    private Color colorNan = Color.BLACK;
    private boolean useZeroColor = true;
    private double zeroLevel = 0;
    private boolean loading = true;

    @PropertyName ( "Use color for zero" )
    @PropertyDescription ( "If checked, zero-color will be used to represent zero value" )
    public boolean isUseZeroColor()
    {
        return useZeroColor;
    }

    public void setUseZeroColor(boolean useZeroColor)
    {
        Object oldValue = this.useZeroColor;
        this.useZeroColor = useZeroColor;
        firePropertyChange("useZeroColor", oldValue, useZeroColor);
    }

    @PropertyName ( "Zero color" )
    @PropertyDescription ( "Color to represent zero value. Used only if minimum value < 0, maximum value > 0 and 'Use color for zero' is checked." )
    public Color getColorZero()
    {
        return colorZero;
    }

    public void setColorZero(Color colorZero)
    {
        Object oldValue = this.colorZero;
        this.colorZero = colorZero;
        firePropertyChange("colorZero", oldValue, colorZero);
    }

    @PropertyName ( "Color for not presented" )
    @PropertyDescription ( "Color to represent missed value (when row exists in the table, but value is not a number or empty)." )
    public Color getColorNan()
    {
        return colorNan;
    }

    public void setColorNan(Color colorNan)
    {
        Object oldValue = this.colorNan;
        this.colorNan = colorNan;
        firePropertyChange( "colorNan", oldValue, colorNan );
    }

    @PropertyName ( "Start-color" )
    @PropertyDescription ( "Color for minimum value" )
    public Color getColor1()
    {
        return color1;
    }

    public void setColor1(Color color1)
    {
        Object oldValue = this.color1;
        this.color1 = color1;
        firePropertyChange("color1", oldValue, color1);
    }

    @PropertyName ( "End-color" )
    @PropertyDescription ( "Color for maximum value" )
    public Color getColor2()
    {
        return color2;
    }

    public void setColor2(Color color2)
    {
        Object oldValue = this.color2;
        this.color2 = color2;
        firePropertyChange("color2", oldValue, color2);
    }

    private Pair<Double, Double> getMinMax()
    {
        return tableMinMax;
    }

    @PropertyName ( "Minimum value" )
    @PropertyDescription ( "Lowest value in the selected columns" )
    public double getMin()
    {
        return getMinMax()==null?-1:getMinMax().getFirst();
    }

    public void setMin(double min)
    {
        double oldValue = getMinMax().getFirst();
        getMinMax().setFirst(min);
        firePropertyChange("min", oldValue, min);
    }

    @PropertyName ( "Maximum value" )
    @PropertyDescription ( "Highest value in the selected columns" )
    public double getMax()
    {
        return getMinMax()==null?1:getMinMax().getSecond();
    }

    public void setMax(double max)
    {
        double oldValue = getMinMax().getSecond();
        getMinMax().setSecond(max);
        firePropertyChange("max", oldValue, max);
    }

    public Color getColor(double value)
    {
        double ratio;
        Color col1, col2;
    
        if( Double.isNaN( value ) )
        {
            return getColorNan();
        }
        else if( isUseZeroColor() && getMin() < zeroLevel && getMax() > zeroLevel )
        {
            if( value < zeroLevel )
            {
                ratio = ( value - zeroLevel ) / ( getMin() - zeroLevel );
                col1 = getColorZero();
                col2 = getColor1();
            }
            else
            {
                ratio = ( value - zeroLevel ) / ( getMax() - zeroLevel );
                col1 = getColorZero();
                col2 = getColor2();
            }
        } else
        {
            ratio = ( value - getMin() ) / ( getMax() - getMin() );
            col1 = getColor1();
            col2 = getColor2();
        }
        if( ratio < 0 )
            ratio = 0;
        if( ratio > 1 )
            ratio = 1;
        return ColorUtils.mix(col1, col2, ratio);
    }

    public DataElementPath getTable()
    {
        return ((ExpressionFilterProperties)getParent()).getTable();
    }

    @PropertyName("Zero level")
    @PropertyDescription("Value at which zero color will be used")
    public double getZeroLevel()
    {
        return zeroLevel;
    }

    public void setZeroLevel(double zeroLevel)
    {
        Object oldValue = this.zeroLevel;
        this.zeroLevel = zeroLevel;
        firePropertyChange("zeroLevel", oldValue, zeroLevel);
    }

    public boolean isZeroHidden()
    {
        return !useZeroColor;
    }

    public boolean getLoading()
    {
        return loading;
    }
    public void setLoading(boolean loading)
    {
        this.loading = loading;
    }
}
