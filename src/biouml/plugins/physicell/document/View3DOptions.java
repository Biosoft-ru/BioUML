package biouml.plugins.physicell.document;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.physicell.ui.ModelData;
import ru.biosoft.physicell.ui.render.Vertex;

public class View3DOptions extends Option
{
    private static final String QUALITY_GOOD = "Good";
    private static final String QUALITY_POOR = "Poor";
    public static final String[] QUALITIES = new String[] {QUALITY_GOOD, QUALITY_POOR};
    private String quality = QUALITY_POOR;

    private int xCutOff = 0;
    private int yCutOff = 0;
    private int zCutOff = 0;
    private int head = -40;
    private int pitch = -40;
    private ModelData data;

    public void setSize(ModelData data)
    {
        this.data = data;
    }

    public int getMaxX()
    {
        return (int)data.getXDim().getTo();
    }

    public int getMaxY()
    {
        return (int)data.getYDim().getTo();
    }

    public int getMaxZ()
    {
        return (int)data.getZDim().getTo();
    }

    public int getMinX()
    {
        return (int)data.getXDim().getFrom();
    }

    public int getMinY()
    {
        return (int)data.getYDim().getFrom();
    }

    public int getMinZ()
    {
        return (int)data.getZDim().getFrom();
    }

    public void setAngle(int head, int pitch)
    {
        head = Util.restrict( head, 180 );
        pitch = Util.restrict( pitch, 180 );
        this.head = head;
        this.pitch = pitch;
        this.firePropertyChange( "*", null, null );
    }

    @PropertyName ( "Heading" )
    public int getHead()
    {
        return head;
    }
    public void setHead(int head)
    {
        head = Util.restrict( head, 180 );
        int oldValue = this.head;
        this.head = head;
        this.firePropertyChange( "head", oldValue, head );
    }

    @PropertyName ( "Pitch" )
    public int getPitch()
    {
        return pitch;
    }
    public void setPitch(int pitch)
    {
        pitch = Util.restrict( pitch, 180 );
        int oldValue = this.pitch;
        this.pitch = pitch;
        this.firePropertyChange( "pitch", oldValue, pitch );
    }

    @PropertyName ( "Spheres quality" )
    public String getQuality()
    {
        return quality;
    }
    public void setQuality(String quality)
    {
        String oldValue = this.quality;
        this.quality = quality;
        this.firePropertyChange( "quality", oldValue, quality );
    }

    public int getQualityInt()
    {
        switch( quality )
        {
            case QUALITY_GOOD:
                return 3;
            case QUALITY_POOR:
                return 2;
        }
        return 3;
    }

    public String[] getQualities()
    {
        return QUALITIES;
    }

    @PropertyName ( "XY slice, Z =" )
    public int getXCutOff()
    {
        return xCutOff;
    }
    public void setXCutOff(int xCutOff)
    {
        int oldValue = this.xCutOff;
        this.xCutOff = xCutOff;
        this.firePropertyChange( "xCutOff", oldValue, xCutOff );
    }

    @PropertyName ( "YZ slice, X =" )
    public int getYCutOff()
    {
        return yCutOff;
    }
    public void setYCutOff(int yCutOff)
    {
        int oldValue = this.yCutOff;
        this.yCutOff = yCutOff;
        this.firePropertyChange( "yCutOff", oldValue, yCutOff );
    }

    @PropertyName ( "XZ slice, Y =" )
    public int getZCutOff()
    {
        return zCutOff;
    }
    public void setZCutOff(int zCutOff)
    {
        int oldValue = this.zCutOff;
        this.zCutOff = zCutOff;
        this.firePropertyChange( "zCutOff", oldValue, zCutOff );
    }

    public Vertex getCutOff()
    {
        return new Vertex( getXCutOff(), getYCutOff(), getZCutOff() );
    }
}