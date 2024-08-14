package biouml.plugins.hemodynamics;

import ru.biosoft.graphics.Pen;

public class HemodynamicsPlotInfo
{
    public static final int PRESSURE = 0;
    public static final int FULL_PRESSURE = 5;
    public static final int AREA = 1;
    public static final int FLOW = 2;
    public static final int RESISTANCE = 3;
    public static final int VELOCITY = 4;
    public static final int PULSE_WAVE_VELOCITY = 6;
    
    public HemodynamicsPlotInfo(String name, String title, Vessel vessel, String typeStr, int segment, boolean doPlot, Pen spec)
    {
        this( name, title, vessel, getTypeByString( typeStr ),segment, doPlot, spec );
    }

    protected static int getTypeByString(String typeStr)
    {
        int type = PRESSURE;
        if( typeStr.equals( "Pressure" ) )
        {
            type = PRESSURE;
        }
        else if( typeStr.equals( "Flow" ) )
        {
            type = FLOW;
        }
        else if( typeStr.equals( "Area" ) )
        {
            type = AREA;
        }
        else if( typeStr.equals( "Resistance" ) )
        {
            type = RESISTANCE;
        }
        else if( typeStr.equals( "Velocity" ) )
        {
            type = VELOCITY;
        }
        else if( typeStr.equals( "Full pressure" ) )
        {
            type = FULL_PRESSURE;
        }
        return type;
    }

    public HemodynamicsPlotInfo(String name, String title, Vessel vessel, int type, int segment, boolean doPlot, Pen spec)
    {
        this.name = name;
        this.title = title;
        this.vessel = vessel;
        this.type = type;
        this.spec = spec;
        this.segment = segment;
        this.doPlot = doPlot;
    }

    private final boolean doPlot;
    int segment;
    String name;
    String title;
    Vessel vessel;
    int type;
    Pen spec = new Pen();
    
    public int getVesselIndex()
    {
        return vessel.index;
    }
    public int getType()
    {
        return type;
    }
    
    public int getSegment()
    {
        return segment;
    }
    
    public String getVariable()
    {
        return name;
    }
    
    public boolean doPlot()
    {
        return doPlot;
    }
}