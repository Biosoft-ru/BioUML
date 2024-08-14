package biouml.plugins.hemodynamics;

import java.awt.Color;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.standard.diagram.PathwaySimulationDiagramViewOptions;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.Pen;

@PropertyName("View options")
public class HemodynamicsDiagramViewOptions extends PathwaySimulationDiagramViewOptions
{
    private Pen vesselPen = new Pen(1.5f, Color.red);
    private int bifurcationRadius = 40;
    private Brush bifurcationColor = new Brush(Color.pink);
    
    public HemodynamicsDiagramViewOptions(Option parent)
    {
        super(parent); 
        autoLayout = true;
    }

    @PropertyName("Vessel pen")
    public Pen getVesselPen()
    {
        return vesselPen;
    }

    public void setVesselPen(Pen vesselPen)
    {
        this.vesselPen = vesselPen;
    }

    @PropertyName("Bifurcation point radius")
    public int getBifurcationRadius()
    {
        return bifurcationRadius;
    }

    public void setBifurcationRadius(int bifurcationRadius)
    {
        this.bifurcationRadius = bifurcationRadius;
    }

    @PropertyName("Bifurcation point color")
    public Brush getBifurcationColor()
    {
        return bifurcationColor;
    }

    public void setBifurcationColor(Brush bifurcationColor)
    {
        this.bifurcationColor = bifurcationColor;
    }
}
