package biouml.plugins.physicell;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

public class DomainOptions extends Option
{
    private boolean use2D = true;
    private double xFrom = -500;
    private double xTo = 500;
    private double xStep = 20;
    private double yFrom = -500;
    private double yTo = 500;
    private double yStep = 20;
    private double zTo = -10;
    private double zFrom = -10;
    private double zStep = 20;

    @PropertyName ( "Use 2D" )
    public boolean isUse2D()
    {
        return use2D;
    }
    public void setUse2D(boolean use2d)
    {
        boolean oldValue = this.use2D;
        this.use2D = use2d;
        firePropertyChange( "use2D", oldValue, use2d );
        firePropertyChange( "*", null, null );
    }

    @PropertyName ( "Xmin" )
    public double getXFrom()
    {
        return xFrom;
    }
    public void setXFrom(double xFrom)
    {
        this.xFrom = xFrom;
    }

    @PropertyName ( "Xmax" )
    public double getXTo()
    {
        return xTo;
    }
    public void setXTo(double xTo)
    {
        this.xTo = xTo;
    }

    @PropertyName ( "dx" )
    public double getXStep()
    {
        return xStep;
    }
    public void setXStep(double xStep)
    {
        this.xStep = xStep;
    }

    @PropertyName ( "Ymin" )
    public double getYFrom()
    {
        return yFrom;
    }
    public void setYFrom(double yFrom)
    {
        this.yFrom = yFrom;
    }

    @PropertyName ( "Ymax" )
    public double getYTo()
    {
        return yTo;
    }
    public void setYTo(double yTo)
    {
        this.yTo = yTo;
    }

    @PropertyName ( "dy" )
    public double getYStep()
    {
        return yStep;
    }
    public void setYStep(double yStep)
    {
        this.yStep = yStep;
    }

    @PropertyName ( "Zmin" )
    public double getZFrom()
    {
        return zFrom;
    }
    public void setZFrom(double zFrom)
    {
        this.zFrom = zFrom;
    }

    @PropertyName ( "Zmax" )
    public double getZTo()
    {
        return zTo;
    }
    public void setZTo(double zTo)
    {
        this.zTo = zTo;
    }

    @PropertyName ( "dz" )
    public double getZStep()
    {
        return zStep;
    }
    public void setZStep(double zStep)
    {
        this.zStep = zStep;
    }

    public DomainOptions clone()
    {
        DomainOptions result = new DomainOptions();
        result.setXFrom( xFrom );
        result.setXTo( xTo );
        result.setXStep( xStep );
        result.setYFrom( yFrom );
        result.setYTo( yTo );
        result.setYStep( yStep );
        result.setZFrom( zFrom );
        result.setZTo( zTo );
        result.setZStep( zStep );
        result.setUse2D( use2D );
        return result;
    }
}