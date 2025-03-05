package biouml.plugins.physicell.document;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

public class ViewOptions extends Option
{
    private boolean axes = true;
    private boolean statistics = true;

    private int maxTime;
    private int time;
    
    private int maxX = 1500;
    private int maxY = 1500;
    private int maxZ = 1500;
    
    public void setSize(int x, int y, int z, int t)
    {
        maxX = x;
        maxY = y;
        maxZ = z;
        maxTime = t;
    }
    
    public int getMaxX()
    {
        return maxX;
    }

    public int getMaxY()
    {
        return maxY;
    }

    public int getMaxZ()
    {
        return maxZ;
    }
    
    public int getMaxTime()
    {
        return maxTime;
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

    @PropertyName("Time")
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
}