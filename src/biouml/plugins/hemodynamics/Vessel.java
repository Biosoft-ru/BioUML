package biouml.plugins.hemodynamics;


import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.MutableDataElement;
import ru.biosoft.util.TextUtil;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import biouml.model.DiagramElement;
import biouml.model.dynamics.EModelRoleSupport;

// TODO: Separate from BinaryTreeNode implementation
public class Vessel extends EModelRoleSupport implements MutableDataElement, Comparable<Vessel>
{
    public int depth = 0; // depth of the vessel's UP junction

    public Vessel left = null; // vessel's child connected to the DOWN junction

    public Vessel right = null; // vessel's child connected to the DOWN junction

    public int index = 0;

    public double length;

    public double unweightedArea; // area

    public double unweightedArea1;// area at the end
    
    public double referencedPressure;

    private double beta;

    public String title;

    public Vessel(String str)
    {
        String[] strs = TextUtil.split( str, ';' );
        this.title = strs[0];
    }

    public Vessel(String title, DiagramElement de, double length, double area, double area1, double beta)
    {
        super( de );
        this.length = length;
        this.unweightedArea = area;
        this.unweightedArea1 = area1;
        this.title = title;
        this.beta = beta;
    }
    
    public Vessel(String title, double length, double area, double beta)
    {
        this(title, null, length, area, area, beta);
    }


    @PropertyName ( "Title" )
    @PropertyDescription ( "Vessel title." )
    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    @PropertyName ( "Beta" )
    @PropertyDescription ( "Beta." )
    public double getBeta()
    {
        return beta;
    }
    public void setBeta(double beta)
    {
        this.beta = beta;
    }

    @Override
    public int compareTo(Vessel o)
    {
        return Integer.compare( o.index, index );
    }

    @Override
    public boolean equals(Object obj)
    {
        if( this == obj )
            return true;
        if( obj == null )
            return false;
        if( getClass() != obj.getClass() )
            return false;
        final Vessel other = (Vessel)obj;
        if( Double.doubleToLongBits( unweightedArea ) != Double.doubleToLongBits( other.unweightedArea ) )
            return false;
        if( Double.doubleToLongBits( beta ) != Double.doubleToLongBits( other.beta ) )
            return false;
        if( depth != other.depth )
            return false;
        if( index != other.index )
            return false;
        if( left == null )
        {
            if( other.left != null )
                return false;
        }
        else if( !left.equals( other.left ) )
            return false;
        if( Double.doubleToLongBits( length ) != Double.doubleToLongBits( other.length ) )
            return false;

        if( right == null )
        {
            if( other.right != null )
                return false;
        }
        else if( !right.equals( other.right ) )
            return false;

        return true;
    }

    protected boolean plotPressure = false;
    protected boolean plotArea = false;
    protected boolean plotFlow = false;
    protected boolean plotVelocity = false;
    protected boolean plotPulseWaveVelocity = false;
    
    @PropertyName ( "Plot pressure" )
    @PropertyDescription ( "If true then pressure at the start of vessel will be outputted to chart." )
    public boolean isPlotPressure()
    {
        return plotPressure;
    }

    public void setPlotPressure(boolean plotPressure)
    {
        this.plotPressure = plotPressure;
    }

    @PropertyName ( "Plot area" )
    @PropertyDescription ( "If true then area at the start of vessel will be outputted to chart." )
    public boolean isPlotArea()
    {
        return plotArea;
    }

    public void setPlotArea(boolean plotArea)
    {
        this.plotArea = plotArea;
    }

    @PropertyName ( "Plot flow" )
    @PropertyDescription ( "If true then flow at the start of vessel will be outputted to chart." )
    public boolean isPlotFlow()
    {
        return plotFlow;
    }

    public void setPlotFlow(boolean plotFlow)
    {
        this.plotFlow = plotFlow;
    }
    
    @PropertyName ( "Plot velocity" )
    @PropertyDescription ( "If true then velocity at the start of vessel will be outputted to chart." )
    public boolean isPlotVelocity()
    {
        return plotVelocity;
    }

    public void setPlotVelocity(boolean plotVelocity)
    {
        this.plotVelocity = plotVelocity;
    }
    
    @PropertyName ( "Plot PWV" )
    @PropertyDescription ( "Plot pulse wave velocity." )
    public boolean isPlotPulseWaveVelocity()
    {
        return plotPulseWaveVelocity;
    }

    public void setPlotPulseWaveVelocity(boolean plotPulseWaveVelocity)
    {
        this.plotPulseWaveVelocity = plotPulseWaveVelocity;
    }
    
    private int segment;
    @PropertyName ( "Segment" )
    @PropertyDescription ( "Vessel segment to be plotted." )
    public int getSegment()
    {
        return segment;
    }

    public void setSegment(int segment)
    {
        this.segment = segment;
    }

    @PropertyName ( "Length" )
    @PropertyDescription ( "Vessel length." )
    public double getLength()
    {
        return length;
    }

    public void setLength(double length)
    {
        this.length = length;
    }

    @PropertyName ( "Pressure" )
    @PropertyDescription ( "Referenced pressure." )
    public double getReferencedPressure()
    {
        return referencedPressure;
    }

    public void setReferencedPressure(double pressure)
    {
        this.referencedPressure = pressure;
    }

    @PropertyName ( "Area" )
    @PropertyDescription ( "Cross-sectional area at referenced pressure." )
    public double getInitialArea()
    {
        return unweightedArea;
    }

    public void setInitialArea(double area)
    {
        this.unweightedArea = area;
    }
    
    @PropertyName ( "Area end" )
    @PropertyDescription ( "Cross-sectional area at the end at referenced pressure." )
    public double getInitialArea1()
    {
        return unweightedArea1;
    }

    public void setInitialArea1(double area)
    {
        this.unweightedArea1 = area;
    }

    @Override
    public String getName()
    {
        return title;
    }

    @Override
    public String toString()
    {
        return getTitle();
    }

    @Override
    public DataCollection getOrigin()
    {
        // TODO Auto-generated method stub
        return null;
    }

}
