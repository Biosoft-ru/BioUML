package biouml.model.dynamics.plot;

import java.awt.Font;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.standard.simulation.plot.Plot.AxisType;
import ru.biosoft.graphics.font.ColorFont;

public class AxisInfo extends Option
{
    private String title = "";
    private String axisType = AxisType.toString( AxisType.NUMBER );
    private double to;
    private double from;
    private boolean autoRange = true;
    private ColorFont tickLabelFont = new ColorFont( "SansSerif", Font.PLAIN, 12 );
    private ColorFont titleFont = new ColorFont( "SansSerif", Font.PLAIN, 12 );

    public AxisInfo(String title)
    {
        this.title = title;
    }

    @PropertyName ( "Axis title" )
    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        Object oldValue = this.title;
        this.title = title;
        firePropertyChange( "title", oldValue, title );
    }

    @PropertyName ( "Title font" )
    @PropertyDescription ( "Font for title." )
    public ColorFont getTitleFont()
    {
        return titleFont;
    }
    public void setTitleFont(ColorFont titleFont)
    {
        ColorFont oldValue = this.titleFont;
        this.titleFont = titleFont;
        firePropertyChange( "titleFont", oldValue, titleFont );
    }

    @PropertyName ( "Axis type" )
    @PropertyDescription ( "Type of axis." )
    public String getAxisType()
    {
        return axisType;
    }

    public boolean isAxisTypeLogarithmic()
    {
        return axisType.equals( AxisType.toString( AxisType.LOGARITHMIC ) );
    }

    public void setAxisType(String axisType)
    {
        this.axisType = axisType;
        if( isAxisTypeLogarithmic() )
            setAutoRange( false );
        else
            firePropertyChange( "*", null, null );
    }

    @PropertyName ( "Axis auto range" )
    @PropertyDescription ( "Axis auto range." )
    public boolean isAutoRange()
    {
        return autoRange;
    }
    public void setAutoRange(boolean autoRange)
    {
        this.autoRange = autoRange;
        firePropertyChange( "*", null, null );
    }

    @PropertyName ( "To " )
    @PropertyDescription ( "Largest value of coordinate." )
    public double getTo()
    {
        return to;
    }
    public void setTo(double to)
    {
        double oldValue = this.to;
        this.to = to;
        firePropertyChange( "to", oldValue, to );
    }

    @PropertyName ( "From" )
    @PropertyDescription ( "Smallest value of coordinate." )
    public double getFrom()
    {
        return from;
    }
    public void setFrom(double from)
    {
        double oldValue = this.from;
        this.from = from;
        firePropertyChange( "from", oldValue, from );
    }

    @PropertyName ( "Axis tick font" )
    @PropertyDescription ( "Axis tick label font." )
    public ColorFont getTickLabelFont()
    {
        return tickLabelFont;
    }
    public void setTickLabelFont(ColorFont tickLabelFont)
    {
        ColorFont oldValue = this.tickLabelFont;
        this.tickLabelFont = tickLabelFont;
        firePropertyChange( "tickLabelFont", oldValue, tickLabelFont );
    }
}