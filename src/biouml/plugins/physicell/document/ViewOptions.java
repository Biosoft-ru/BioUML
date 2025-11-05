package biouml.plugins.physicell.document;

import java.awt.Color;
import java.awt.Font;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.graphics.font.ColorFont;
import ru.biosoft.physicell.ui.ModelData;

public class ViewOptions extends Option
{
    private String[] substrates = new String[0];
    
    private boolean statistics = true;
    private int statisticsX = 10;
    private int statisticsY = 40;
    private int legendX;
    private int legendY = 40;


    private ColorFont statisticsFont =  new ColorFont( "TimesRoman", Font.PLAIN, 20 );
    private boolean statisticsBackground = true;
    private int maxTime;
    private int time;
    private View2DOptions options2D = new View2DOptions();
    private View3DOptions options3D = new View3DOptions();
    private boolean is2D;
    private DataElementPath result;
    private boolean saveResult = false;
    private int fps = 10;
    private int timeStep;
    private boolean cells = true;
    private boolean drawDensity = false;
    private String substrate;
    private boolean axes = true;
    private boolean drawNuclei = false;
    private double maxDensity = 38;
    private Color densityColor = Color.red;
    private boolean showLegend = true;

    public ViewOptions()
    {
        options2D.setParent( this );
        options3D.setParent( this );
    }
    
    public void setSize(ModelData data, int t)
    {
        this.options2D.setSize( data );
        this.options3D.setSize(data );
        maxTime = t;
    }

    public int getMaxTime()
    {
        return maxTime;
    }

    @PropertyName ( "Statistics" )
    public boolean isStatistics()
    {
        return statistics;
    }
    public void setStatistics(boolean statistics)
    {
        boolean oldValue = this.statistics;
        this.statistics = statistics;
        this.firePropertyChange( "statistics", oldValue, statistics );
    }

    @PropertyName ( "Time" )
    public int getTime()
    {
        return time;
    }

    public void setTime(int time)
    {
        int oldValue = this.time;
        this.time = time;
        this.firePropertyChange( "time", oldValue, time );
    }

    @PropertyName ( "2D" )
    public boolean is2D()
    {
        return is2D;
    }

    public void set2D(boolean is2D)
    {
        boolean oldValue = this.is2D;
        this.is2D = is2D;
        this.firePropertyChange( "is2D", oldValue, is2D );
        this.firePropertyChange( "*", null, null );
    }

    public boolean is3D()
    {
        return !is2D();
    }

    @PropertyName ( "Options 2D" )
    public View2DOptions getOptions2D()
    {
        return options2D;
    }
    public void setOptions2D(View2DOptions options2D)
    {
        Object oldValue = this.options2D;
        this.options2D = options2D;
        options2D.setParent( this );
        this.firePropertyChange( "options2D", oldValue, options2D );
    }

    @PropertyName ( "Options 3D" )
    public View3DOptions getOptions3D()
    {
        return options3D;
    }
    public void setOptions3D(View3DOptions options3D)
    {
        Object oldValue = this.options3D;
        this.options3D = options3D;
        options3D.setParent( this );
        this.firePropertyChange( "options3D", oldValue, options3D );
    }
    
    @PropertyName ( "Statistics background" )
    public boolean isStatisticsBackground()
    {
        return statisticsBackground;
    }
    
    public void setStatisticsBackground(boolean statisticsBackground)
    {
        Object oldValue = this.statisticsBackground;
        this.statisticsBackground = statisticsBackground;
        this.firePropertyChange( "statisticsBackground", oldValue, statisticsBackground );
    }
    
    @PropertyName ( "Statistics X" )
    public int getStatisticsX()
    {
        return statisticsX;
    }

    public void setStatisticsX(int statisticsX)
    {
        int oldValue = this.statisticsX;
        this.statisticsX = statisticsX;
        this.firePropertyChange( "statisticsX", oldValue, statisticsX );
    }

    @PropertyName ( "Statistics Y" )
    public int getStatisticsY()
    {
        return statisticsY;
    }

    public void setStatisticsY(int statisticsY)
    {
        int oldValue = this.statisticsY;
        this.statisticsY = statisticsY;
        this.firePropertyChange( "statisticsY", oldValue, statisticsY );
    }
    
    @PropertyName("Statistics Font")
    public ColorFont getStatisticsFont()
    {
        return statisticsFont;
    }

    public void setStatisticsFont(ColorFont statisticsFont)
    {
        ColorFont oldValue = this.statisticsFont;
        this.statisticsFont = statisticsFont;
        this.firePropertyChange( "statisticsFont", oldValue, statisticsFont );
    }
    
    @PropertyName("Result video path")
    public DataElementPath getResult()
    {
        return result;
    }

    public void setResult(DataElementPath result)
    {
        DataElementPath oldValue = this.result;
        this.result = result;
        this.firePropertyChange( "result", oldValue, result );
    }

    @PropertyName("Save video")
    public boolean isSaveResult()
    {
        return saveResult;
    }

    public void setSaveResult(boolean saveResult)
    {
        boolean oldValue = this.saveResult;
        this.saveResult = saveResult;
        this.firePropertyChange( "saveResult", oldValue, saveResult );
    }
    
    
    @PropertyName("Draw nuclei")
    public boolean isDrawNuclei()
    {
        return drawNuclei;
    }

    public void setDrawNuclei(boolean drawNuclei)
    {
        boolean oldValue = this.drawNuclei;
        this.drawNuclei = drawNuclei;
        this.firePropertyChange( "drawNuclei", oldValue, drawNuclei );
    }
    
    @PropertyName("Frame per second")
    public int getFps()
    {
        return fps;
    }

    public void setFps(int fps)
    {
        this.fps = fps;
    }
    
    @PropertyName("Time step")
    public int getTimeStep()
    {
        return timeStep;
    }

    public void setTimeStep(int timeStep)
    {
        this.timeStep = timeStep;
    }
    
    @PropertyName ( "Axes" )
    public boolean isAxes()
    {
        return axes;
    }
    public void setAxes(boolean axes)
    {
        boolean oldValue = this.axes;
        this.axes = axes;
        this.firePropertyChange( "axes", oldValue, axes );
    }

    @PropertyName ( "Cells" )
    public boolean isCells()
    {
        return cells;
    }

    public void setCells(boolean cells)
    {
        boolean oldValue = this.cells;
        this.cells = cells;
        this.firePropertyChange( "cells", oldValue, cells );
    }
    
    public String[] getSubstrates()
    {
        return substrates;
    }

    public void setSubstrates(String[] substrates)
    {
        this.substrates = substrates;
        if( substrates != null && substrates.length > 0 )
            this.substrate = substrates[0];
    }

    @PropertyName ( "Substrate" )
    public boolean isDrawDensity()
    {
        return drawDensity;
    }

    public void setDrawDensity(boolean drawDensity)
    {
        boolean oldValue = this.drawDensity;
        this.drawDensity = drawDensity;
        firePropertyChange( "drawDensity", oldValue, drawDensity );
    }
    

    @PropertyName ( "Substrate name" )
    public String getSubstrate()
    {
        return substrate;
    }

    public void setSubstrate(String substrate)
    {
        String oldValue = this.substrate;
        this.substrate = substrate;
        firePropertyChange( "substrate", oldValue, substrate );
    }
    
    @PropertyName("Maximum density")
    public double getMaxDensity()
    {
        return maxDensity;
    }
    public void setMaxDensity(double maxDensity)
    {
        this.maxDensity = maxDensity;
    }

    @PropertyName("Density Color")
    public Color getDensityColor()
    {
        return densityColor;
    }
    public void setDensityColor(Color densityColor)
    {
        Color oldValue = this.densityColor;
        this.densityColor = densityColor;
        firePropertyChange( "densityColor", oldValue, densityColor );
    }
    
    @PropertyName("Show Legend")
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

    @PropertyName("Legend X")
    public int getLegendX()
    {
        return legendX;
    }

    public void setLegendX(int legendX)
    {
        int oldValue = this.legendX;
        this.legendX = legendX;
        firePropertyChange( "legendX", oldValue, legendX );
    }

    @PropertyName("Legend Y")
    public int getLegendY()
    {
        return legendY;
    }

    public void setLegendY(int legendY)
    {
        int oldValue = this.legendY;
        this.legendY = legendY;    
        firePropertyChange( "legendY", oldValue, legendY );
    }
}