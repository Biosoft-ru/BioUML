package biouml.standard.type;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;

/**
 * Some constant that can be used in kinetic or other equations.
 * @pending refine definition
 */
@PropertyName("Constant")
@PropertyDescription("Equation or function constant.")
public class Constant extends Referrer
{
    protected double value;
    protected Unit unit;
    
    public Constant(DataCollection parent, String name)
    {
        super(parent, name, CONSTANT);
    }
    
    @PropertyName("Value")
    @PropertyDescription("Constant value.")
    public double getValue()
    {
        return value;
    }
    public void setValue(double value)
    {
        double oldValue = this.value;
        this.value = value;
        firePropertyChange("value", oldValue, value);
    }
    
    @PropertyName("Unit")
    @PropertyDescription("The unit expressed in base units. Constant, paremeter or varable unit.")
    public Unit getUnit()
    {
        return unit;
    }
    public void setUnit(Unit unit)
    {
        Unit oldValue = this.unit;
        this.unit= unit;
        firePropertyChange("unit", oldValue, unit);
    }
}
