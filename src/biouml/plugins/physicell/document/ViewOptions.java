package biouml.plugins.physicell.document;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

public class ViewOptions extends Option
{
    private boolean statistics = true;
    private int maxTime;
    private int time;
    private View2DOptions options2D = new View2DOptions();
    private View3DOptions options3D = new View3DOptions();
    private boolean is3D;

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

    @PropertyName ( "3D" )
    public boolean is3D()
    {
        return is3D;
    }

    public void set3D(boolean is3d)
    {
        boolean oldValue = this.is3D;
        this.is3D = is3d;
        this.firePropertyChange( "is3D", oldValue, is3d );
        this.firePropertyChange( "*", null, null );
    }

    public boolean is2D()
    {
        return !is3D();
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
}