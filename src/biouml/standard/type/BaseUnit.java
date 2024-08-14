package biouml.standard.type;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

@PropertyName("Base unit")
@PropertyDescription("Base unit (type | multiplier | scale | exponent).")
public class BaseUnit extends Option
{
    private String unitId;
    private String type = Unit.getBaseUnitsList().get(0);
    int scale = 0;
    private int exponent = 1;
    double multiplier = 1;
    
    public BaseUnit()
    {        
        type =  Unit.getBaseUnitsList().get(0);
    }
    
    public BaseUnit(String type, double multiplier, int scale, int exponent)
    {
        this.type = type;
        this.multiplier = multiplier;
        this.scale = scale;
        this.exponent = exponent;
    }
    
    public String getUnitId()
    {
        return this.unitId;
    }
    public void setUnitId(String unitId)
    {
        this.unitId = unitId;
    }

    @PropertyName ( "Type" )
    @PropertyDescription ( "Unit type." )
    public String getType()
    {
        return this.type;
    }
    public void setType(String type)
    {
        String oldValue = this.type;
        this.type = type;
        firePropertyChange("type", oldValue, type);
    }

    @PropertyName ( "Scale" )
    @PropertyDescription ( "Unit scale." )
    public int getScale()
    {
        return this.scale;
    }
    public void setScale(int scale)
    {
        int oldValue = this.scale;
        this.scale = scale;
        firePropertyChange( "scale", oldValue, scale );
        firePropertyChange( "*", null, null );
    }

    @PropertyName ( "Exponent" )
    @PropertyDescription ( "Unit exponent." )
    public int getExponent()
    {
        return this.exponent;
    }
    public void setExponent(int exponent)
    {
        int oldValue = this.exponent;
        this.exponent = exponent;
        this.firePropertyChange( "exponent", oldValue, exponent );
        firePropertyChange( "*", null, null );
    }

    @PropertyName ( "Multiplier" )
    @PropertyDescription ( "Unit multiplier." )
    public double getMultiplier()
    {
        return this.multiplier;
    }
    public void setMultiplier(double multiplier)
    {
        double oldValue = this.multiplier;
        this.multiplier = multiplier;
        this.firePropertyChange( "multiplier", oldValue, multiplier );
        firePropertyChange( "*", null, null );
    }

    public BaseUnit clone(Option newParent)
    {
        BaseUnit newBaseUnit = new BaseUnit(type, multiplier, scale, exponent);
        newBaseUnit.setParent(newParent);
        newBaseUnit.setUnitId(unitId);
        return newBaseUnit;
    }
}