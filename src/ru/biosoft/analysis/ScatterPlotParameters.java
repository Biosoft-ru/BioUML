package ru.biosoft.analysis;

import com.developmentontheedge.beans.annot.PropertyName;

@SuppressWarnings ( "serial" )
public class ScatterPlotParameters extends ChartAnalysisParameters
{
    private String xColumn;
    private String yColumn;
    private String xColumnLabel = null;
    private String yColumnLabel = null;
    private boolean showLegend = true;
    private boolean skipNaN = true;
    @PropertyName("X Column")
    public String getXColumn()
    {
        return xColumn;
    }
    public void setXColumn(String xColumn)
    {
        Object oldValue = this.xColumn;
        this.xColumn = xColumn;
        firePropertyChange("xColumn", oldValue, xColumn );
    }
    @PropertyName("Y Column")
    public String getYColumn()
    {
        return yColumn;
    }
    public void setYColumn(String yColumn)
    {
        Object oldValue = this.yColumn;
        this.yColumn = yColumn;
        firePropertyChange( "yColumn", oldValue, yColumn);
    }
    @PropertyName ( "X axis label" )
    public String getXColumnLabel()
    {
        return xColumnLabel;
    }
    public void setXColumnLabel(String xColumnLabel)
    {
        Object oldValue = this.xColumnLabel;
        this.xColumnLabel = xColumnLabel;
        firePropertyChange( "xColumnLabel", oldValue, xColumnLabel );
    }
    @PropertyName ( "Y axis label" )
    public String getYColumnLabel()
    {
        return yColumnLabel;
    }
    public void setYColumnLabel(String yColumnLabel)
    {
        Object oldValue = this.yColumnLabel;
        this.yColumnLabel = yColumnLabel;
        firePropertyChange( "yColumnLabel", oldValue, yColumnLabel );
    }
    @PropertyName ( "Show legend" )
    public boolean isShowLegend()
    {
        return showLegend;
    }
    public void setShowLegend(boolean showLegend)
    {
        boolean oldValue = this.showLegend;
        this.showLegend = showLegend;
        firePropertyChange( "showLegend", oldValue, showLegend );
    }
    @PropertyName ( "Skip NaN" )
    public boolean isSkipNaN()
    {
        return skipNaN;
    }
    public void setSkipNaN(boolean skipNaN)
    {
        boolean oldValue = this.skipNaN;
        this.skipNaN = skipNaN;
        firePropertyChange( "skipNaN", oldValue, skipNaN );
    }
}
