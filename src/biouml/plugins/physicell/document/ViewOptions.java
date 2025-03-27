package biouml.plugins.physicell.document;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataElementPath;

public class ViewOptions extends Option
{
    private boolean statistics = true;
    private int statisticsX = 10;
    private int statisticsY = 40;
    private int maxTime;
    private int time;
    private View2DOptions options2D = new View2DOptions();
    private View3DOptions options3D = new View3DOptions();
    private boolean is2D;
    private DataElementPath result;
    private boolean saveResult = false;
    private boolean drawNuclei = false;
    
    public ViewOptions()
    {
        options2D.setParent( this );
        options3D.setParent( this );
    }
    
    public void setSize(int x, int y, int z, int t)
    {
        this.options2D.setSize( x, y, z );
        this.options3D.setSize( x, y, z );
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
}