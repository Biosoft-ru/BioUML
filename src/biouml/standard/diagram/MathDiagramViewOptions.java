package biouml.standard.diagram;

import java.awt.Color;

import ru.biosoft.graphics.Brush;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

@PropertyName("View options")
public class MathDiagramViewOptions extends PathwaySimulationDiagramViewOptions
{
    protected Brush tableEntityBrush = new Brush(Color.white);
    private EquationStyle eqStyle = EquationStyle.FULL;
    
    public MathDiagramViewOptions(Option parent)
    {
        super(parent);
        autoLayout = true;
        showVariableValue = false;
        createPortEdges = false;
    }

    public MathDiagramViewOptions()
    {
        this(null);
    }

    @PropertyName("Equation style")
    @PropertyDescription("Equation style.")
    public String getEquationStyle()
    {
        return eqStyle.toString();
    }

    public void setEquationStyle(String style)
    {
        Object oldValue = eqStyle;
        eqStyle = EquationStyle.fromString(style);
        this.firePropertyChange("equationStyle", oldValue, eqStyle);
    }
}
