package biouml.plugins.physicell.document;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.physicell.ui.ModelData;

public class View2DOptions extends Option
{
    private static final String XZ_PLANE = "XZ Plane";
    private static final String YZ_PLANE = "YZ Plane";
    private static final String XY_PLANE = "XY Plane";
    public static final String[] SECTION_VALUES = new String[] {XY_PLANE, YZ_PLANE, XZ_PLANE};

    private String substrate;
    private boolean drawGrid = false;
    private boolean drawDensity = true;
    private Section sec = Section.Z;
    private String sectionString = XY_PLANE;
    private int slice = 0;
    private ModelData data;
    private String[] substrates = new String[0];

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

    public enum Section
    {
        X, Y, Z
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

    @PropertyName ( "Grid" )
    public boolean isDrawGrid()
    {
        return drawGrid;
    }

    public void setDrawGrid(boolean drawGrid)
    {
        boolean oldValue = this.drawGrid;
        this.drawGrid = drawGrid;
        firePropertyChange( "drawGrid", drawGrid, oldValue );
    }

    @PropertyName ( "Density" )
    public boolean isDrawDensity()
    {
        return drawDensity;
    }

    public void setDrawDensity(boolean drawDensity)
    {
        boolean oldValue = this.drawDensity;
        this.drawDensity = drawDensity;
        firePropertyChange( "drawDensity", drawDensity, oldValue );
    }

    public int getMaxSlice()
    {
        switch( sectionString )
        {
            case XY_PLANE:
                return getMaxZ();
            case YZ_PLANE:
                return getMaxX();
            default:
                return getMaxY();
        }
    }
    
    public int getMinSlice()
    {
        switch( sectionString )
        {
            case XY_PLANE:
                return getMinZ();
            case YZ_PLANE:
                return getMinX();
            default:
                return getMinY();
        }
    }

    @PropertyName ( "Slice" )
    public int getSlice()
    {
        return slice;
    }

    public void setSlice(int slice)
    {
        int oldValue = this.slice;
        this.slice = slice;
        firePropertyChange( "slice", slice, oldValue );
    }

    @PropertyName ( "Section" )
    public String getSectionString()
    {
        return sectionString;
    }

    public void setSectionString(String sectionString)
    {
        String oldValue = this.sectionString;
        this.sectionString = sectionString;
        switch( sectionString )
        {
            case XY_PLANE:
                this.sec = Section.Z;
                break;
            case YZ_PLANE:
                this.sec = Section.X;
                break;
            default:
                this.sec = Section.Y;
        }
        firePropertyChange( "sectionString", sectionString, oldValue );
    }

    public Section getSection()
    {
        return sec;
    }

    @PropertyName ( "Substrate" )
    public String getSubstrate()
    {
        return substrate;
    }

    public void setSubstrate(String substrate)
    {
        String oldValue = this.substrate;
        this.substrate = substrate;
        firePropertyChange( "substrate", substrate, oldValue );
    }
}
