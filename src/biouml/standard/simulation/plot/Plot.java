package biouml.standard.simulation.plot;

import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import com.developmentontheedge.beans.swing.table.AbstractRowModel;
import com.developmentontheedge.beans.swing.table.RowModel;

import biouml.standard.type.BaseSupport;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.graphics.font.ColorFont;

@SuppressWarnings ( "serial" )
@ClassIcon ( "resources/plot.gif" )
public class Plot extends BaseSupport
{
    protected List<Series> series;
    protected String description;
    protected String xTitle;
    protected String yTitle;
    protected String yAxisType = AxisType.toString(AxisType.NUMBER);
    protected String xAxisType = AxisType.toString(AxisType.NUMBER);
    protected double xTo;
    protected double yTo;
    protected double xFrom;
    protected double yFrom;
    protected boolean xAutoRange;
    protected boolean yAutoRange;
    protected ColorFont xTickLabelFont = new ColorFont("SansSerif", Font.PLAIN, 12);
    protected ColorFont yTickLabelFont = new ColorFont("SansSerif", Font.PLAIN, 12);
    protected ColorFont xTitleFont = new ColorFont("SansSerif", Font.PLAIN, 12);    
    protected ColorFont yTitleFont = new ColorFont("SansSerif", Font.PLAIN, 12);
    private DataElementPath defaultSource;

    protected SeriesRowModel rowModel = new SeriesRowModel();
    private boolean needUpdate = false;  

    public Plot(DataCollection<?> origin, String name, List<Series> series)
    {
        super(origin, name, TYPE_PLOT);
        prepareSeries(series);
        this.series = series;
    }

    public Plot(DataCollection<?> origin, String name)
    {
        super(origin, name);
        series = new ArrayList<>();
    }

    private void prepareSeries(List<Series> series)
    {
        for( Series s : series )
            s.setParent(this);
    }

    public List<Series> getSeries()
    {
        return series;
    }

    public void removeAllSeries()
    {
        if( series.size() > 0 )
        {
            int to = series.size() - 1;
            for( int i = 0; i <= to; i++ )
            {
                Series s = series.get(i);
                ( (Option)s ).setParent(null);
            }
            series.clear();
            rowModel.fireTableRowsDeleted(0, to);
        }
    }

    public void addSeries(Series s)
    {
        int row = series.size();
        series.add(s);
        s.setParent(this);
        rowModel.fireTableRowsInserted(row, row);
    }

    public void removeSeries(int index)
    {
        Series s = series.remove(index);
        if( s != null )
            ( (Option)s ).setParent(null);        
        rowModel.fireTableRowsDeleted(index, index);
    }

    @Override
    @PropertyName("Title")
    @PropertyDescription("Title of the plot, how it will be named in the picture.")
    public String getTitle()
    {
        return title;
    }
    @Override
    public void setTitle(String title)
    {
        String oltTitle = this.title;
        this.title = title;
        firePropertyChange("title", oltTitle, title);
    }

    @PropertyName("Description")
    @PropertyDescription("Description of the current plot.")
    public String getDescription()
    {
        return description;
    }
    public void setDescription(String description)
    {
        String oldDescription = description;
        this.description = description;
        firePropertyChange("description", oldDescription, description);
    }

    @PropertyName("X axis title")
    @PropertyDescription("Title of X axis of the plot.")
    public String getXTitle()
    {
        return xTitle;
    }
    public void setXTitle(String xTitle)
    {
        String oldXTitle = this.xTitle;
        this.xTitle = xTitle;
        firePropertyChange("xTitle", oldXTitle, xTitle);
    }
    
    @PropertyName("X axis title font")
    @PropertyDescription("Font for title of X axis of the plot.")
    public ColorFont getXTitleFont()
    {
        return xTitleFont;
    }
    public void setXTitleFont(ColorFont xTitleFont)
    {
        ColorFont oldXTitleFont = this.xTitleFont;
        this.xTitleFont = xTitleFont;
        firePropertyChange("xTitleFont", oldXTitleFont, xTitleFont);
    }

    @PropertyName("X axis type")
    @PropertyDescription("Type of X axis of the plot.")
    public String getXAxisType()
    {
        return xAxisType;
    }

    public boolean isXAxisTypeLogarithmic()
    {
        return xAxisType.equals(AxisType.toString(AxisType.LOGARITHMIC));
    }

    public void setXAxisType(String xAxisType)
    {
        this.xAxisType = xAxisType;
        if( isXAxisTypeLogarithmic() )
            setXAutoRange(false);
        else
            firePropertyChange( "*", null, null );
    }

    @PropertyName("Y axis type")
    @PropertyDescription("Type of Y axis of the plot.")
    public String getYAxisType()
    {
        return yAxisType;
    }

    public void setYAxisType(String yAxisType)
    {
        this.yAxisType = yAxisType;
        if( isYAxisTypeLogarithmic() )
            setYAutoRange(false);
        else
            firePropertyChange( "*", null, null );
    }

    public boolean isYAxisTypeLogarithmic()
    {
        return yAxisType.equals(AxisType.toString(AxisType.LOGARITHMIC));
    }

    @PropertyName("X axis auto range")
    @PropertyDescription("X axis auto range.")
    public boolean isXAutoRange()
    {
        return xAutoRange;
    }
    public void setXAutoRange(boolean xAutoRange)
    {
        this.xAutoRange = xAutoRange;
        firePropertyChange("*", null, null);
    }

    @PropertyName("   X: to ")
    @PropertyDescription("Largest value of X coordinate.")
    public double getXTo()
    {
        return xTo;
    }
    public void setXTo(double xTo)
    {
        double oldXTo = this.xTo;
        this.xTo = xTo;
        firePropertyChange("xTo", oldXTo, xTo);
    }

    @PropertyName("   X: from")
    @PropertyDescription("Smallest value of X coordinate.")
    public double getXFrom()
    {
        return xFrom;
    }
    public void setXFrom(double xFrom)
    {
        double oldXFrom = this.xFrom;
        this.xFrom = xFrom;
        firePropertyChange("xFrom", oldXFrom, xFrom);
    }

    @PropertyName("X axis tick font")
    @PropertyDescription("X axis tick label font.")
    public ColorFont getXTickLabelFont()
    {
        return xTickLabelFont;
    }
    public void setXTickLabelFont(ColorFont xTickLabelFont)
    {
        ColorFont oldValue = this.xTickLabelFont;
        this.xTickLabelFont = xTickLabelFont;
        firePropertyChange("xTickLabelFont", oldValue, xTickLabelFont);
    }

    @PropertyName("Y axis title")
    @PropertyDescription("Title of Y axis of the plot.")
    public String getYTitle()
    {
        return yTitle;
    }
    public void setYTitle(String yTitle)
    {
        String oldYTitle = this.yTitle;
        this.yTitle = yTitle;
        firePropertyChange("yTitle", oldYTitle, yTitle);
    }

    @PropertyName("Y axis title font")
    @PropertyDescription("Font for title of YX axis of the plot.")
    public ColorFont getYTitleFont()
    {
        return yTitleFont;
    }
    public void setYTitleFont(ColorFont yTitleFont)
    {
        ColorFont oldYTitleFont = this.yTitleFont;
        this.yTitleFont = yTitleFont;
        firePropertyChange("yTitleFont", oldYTitleFont, yTitleFont);
    }

    @PropertyName("Y axis auto range")
    @PropertyDescription("Y axis auto range.")
    public boolean isYAutoRange()
    {
        return yAutoRange;
    }
    public void setYAutoRange(boolean yAutoRange)
    {
        this.yAutoRange = yAutoRange;
        firePropertyChange("*", null, null);
    }

    @PropertyName("   Y: to ")
    @PropertyDescription("Largest value of Y coordinate.")
    public double getYTo()
    {
        return yTo;
    }
    public void setYTo(double yTo)
    {
        double oldYTo = this.yTo;
        this.yTo = yTo;
        firePropertyChange("yTo", oldYTo, yTo);
    }

    @PropertyName("   Y: from ")
    @PropertyDescription("Smallest value of Y coordinate.")
    public double getYFrom()
    {
        return yFrom;
    }
    public void setYFrom(double yFrom)
    {
        double oldYFrom = this.yFrom;
        this.yFrom = yFrom;
        firePropertyChange("yFrom", oldYFrom, yFrom);
    }
  
    @PropertyName("Y axis tick font")
    @PropertyDescription("Y axis tick label font.")
    public ColorFont getYTickLabelFont()
    {
        return yTickLabelFont;
    }
    public void setYTickLabelFont(ColorFont yTickLabelFont)
    {
        ColorFont oldValue = this.yTickLabelFont;
        this.yTickLabelFont = yTickLabelFont;
        firePropertyChange("yTickLabelFont", oldValue, yTickLabelFont);
    }

    ////////////////////////////////////////////////////////////////////////////
    // RowModel implementation
    //

    class SeriesRowModel extends AbstractRowModel
    {
        // row model methods
        @Override
        public int size()
        {
            if( series != null )
            {
                return series.size();
            }
            return 0;
        }

        @Override
        public Object getBean(int index)
        {
            if( series.size() == 0 )
            {
                return Plot.getDefaultSeries();
            }
            return series.get(index);
        }

        @Override
        public Class<?> getBeanClass()
        {
            return Plot.class;
        }
    };

    public RowModel getRowModel()
    {
        return rowModel;
    }

    ////////////////////////////////////////////////////////////////////////////
    // utilities
    //

    public static Series getDefaultSeries(String xName, String yName, String selectedResultName, Series.SourceNature sourceNature)
    {
        Series s = new Series();
        s.setSource(selectedResultName);
        s.setSourceNature(sourceNature);
        s.setXVar(xName);
        s.setYVar(yName);
        return s;
    }
    
    public static Series getDefaultSeries(String xPath, String xName, String yPath, String yName, String selectedResultName, Series.SourceNature sourceNature)
    {
        Series s = new Series();
        s.setSource(selectedResultName);
        s.setSourceNature(sourceNature);
        s.setXPath( xPath );
        s.setXVar(xName);
        s.setYPath( yPath );
        s.setYVar(yName);
        return s;
    }

    public static Series getDefaultSeries()
    {
        Series s = new Series();
        s.setSource("");
        s.setXVar("");
        s.setYVar("");
        return s;
    }

    public static Plot createDefaultPlot(DataCollection<?> plotCollection)
    {
        Plot pl = new Plot(plotCollection, "new_plot");
        pl.setTitle("");
        pl.setDescription("");
        pl.setXTitle("");
        pl.setXTitleFont(new ColorFont("SansSerif", Font.PLAIN, 12));
        pl.setYTitle("");
        pl.setXAutoRange(true);
        pl.setYAutoRange(true);
        pl.setXFrom(0);
        pl.setYFrom(0);
        pl.setXTo(10);
        pl.setYTo(10);
        return pl;
    }

    public boolean needUpdate()
    {
        return needUpdate;
    }
    public void setNeedUpdate(boolean needUpdate)
    {
        this.needUpdate = needUpdate;
    }

    public static enum AxisType
    {
        NUMBER, LOGARITHMIC, LOG10;

        public static String toString(AxisType axisType)
        {
            switch( axisType )
            {
                case NUMBER:
                    return "Number";

                case LOGARITHMIC:
                    return "Logarithmic";
                    
                case LOG10:
                    return "Log 10";

                default:
                    return "";
            }
        }

        public static @Nonnull List<String> getAxisTypes()
        {
            List<String> list = new ArrayList<>();
            list.add(toString(NUMBER));
            list.add(toString(LOGARITHMIC));
            list.add(toString(LOG10));
            return list;
        }

        public static AxisType getAxisType(String axisType)
        {
            int index = getAxisTypes().indexOf(axisType);
            return AxisType.values()[index];
        }
    }

    @Override
    public Plot clone(DataCollection<?> origin, String newName)
    {
        Plot plot = new Plot(origin, newName, getSeries());
        plot.setTitle(getTitle());
        plot.setDescription(getDescription());
        plot.setXTitle(getXTitle());
        plot.setXTitleFont(getXTitleFont());
        plot.setYTitle(getYTitle());
        plot.setXTo(getXTo());
        plot.setYTo(getYTo());
        plot.setXFrom(getXFrom());
        plot.setYFrom(getYFrom());
        plot.setXAutoRange(isXAutoRange());
        plot.setYAutoRange(isYAutoRange());
        plot.setDefaultSource( getDefaultSource() );
        return plot;
    }

    @PropertyName ( "Default source path" )
    @PropertyDescription ( "Default path to plot series" )
    public DataElementPath getDefaultSource()
    {
        return defaultSource;
    }

    public void setDefaultSource(DataElementPath source)
    {
        Object oldValue = this.defaultSource;
        this.defaultSource = source;
        firePropertyChange( "defaultSource", oldValue, source );
    }
}