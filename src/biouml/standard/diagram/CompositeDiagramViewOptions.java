package biouml.standard.diagram;

import java.awt.Color;
import java.awt.Font;

import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.font.ColorFont;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

@PropertyName("View options")
public class CompositeDiagramViewOptions extends PathwaySimulationDiagramViewOptions
{
    protected ColorFont connectionTitleFont = new ColorFont("Arial", Font.BOLD, 12, Color.black);
    protected boolean isCollapsed = true;
    protected Pen modulePen = new Pen();
    protected Brush moduleBrush = new Brush(new Color(135, 206, 250));
    protected Brush busBrush = new Brush(new Color(249, 250, 135));
    protected Brush constantBrush = new Brush(new Color(249, 236, 135));
    protected Brush switchBrush = new Brush(new Color(249,183,125));
    protected boolean arrows = true; 
    
    public CompositeDiagramViewOptions(Option parent)
    {
        super(parent);
        showReactionName = false;
        autoLayout = true;
        setStateTitleFont(new ColorFont("Arial", Font.BOLD, 13, Color.red));
    }

    public CompositeDiagramViewOptions()
    {
        this(null);
    }

    @PropertyName("Subdiagrams collapsed")
    @PropertyDescription("Specifies whether subdiagrams views are collapsed or not.")
    public boolean isCollapsed()
    {
        return isCollapsed;
    }
    public void setCollapsed(boolean isCollapsed)
    {
        this.isCollapsed = isCollapsed;
    }

    @PropertyName("Connection title font")
    @PropertyDescription("Title font for connections between subdiagrams.")
    public ColorFont getConnectionTitleFont()
    {
        return connectionTitleFont;
    }
    public void setConnectionTitleFont(ColorFont connectionTitleFont)
    {
        ColorFont oldValue = this.connectionTitleFont;
        this.connectionTitleFont = connectionTitleFont;
        firePropertyChange("connectionTitleFont", oldValue, connectionTitleFont);
    }

    @PropertyName("Subdiagram pen")
    public Pen getModulePen()
    {
        return modulePen;
    }
    public void setModulePen(Pen modulePen)
    {
        Pen oldValue = this.modulePen;
        this.modulePen = modulePen;
        firePropertyChange("modulePen", oldValue, modulePen);
    }

    @PropertyName("Subdiagram brush")
    public Brush getModuleBrush()
    {
        return moduleBrush;
    }
    public void setModuleBrush(Brush analysisBrush)
    {
        Brush oldValue = this.moduleBrush;
        this.moduleBrush = analysisBrush;
        firePropertyChange("moduleBrush", oldValue, analysisBrush);
    }

    public Brush getBusBrush()
    {
        return busBrush;
    }
    public void setBusBrush(Brush busBrush)
    {
        Brush oldValue = this.busBrush;
        this.busBrush = busBrush;
        firePropertyChange("busBrush", oldValue, busBrush);
    }

    public Brush getConstantBrush()
    {
        return constantBrush;
    }
    public void setConstantBrush(Brush brush)
    {
        Brush oldValue = constantBrush;
        constantBrush = brush;
        firePropertyChange("constantBrush", oldValue, constantBrush);
    }

    public Brush getSwitchBrush()
    {
        return switchBrush;
    }
    public void setSwitchBrush(Brush brush)
    {
        Brush oldValue = this.switchBrush;
        switchBrush = brush;
        firePropertyChange("switchBrush", oldValue, switchBrush);
    }

    @PropertyName("Show arrows")
    @PropertyDescription("Show arrows for connections")
    public boolean isArrows()
    {
        return arrows;
    }
    public void setArrows(boolean arrows)
    {
        boolean oldValue = this.arrows;
        this.arrows = arrows;
        firePropertyChange("arrows", oldValue, arrows);
    }
}
