package biouml.plugins.physicell;

import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.physicell.core.Variable;

public class VariableProperties
{
    private String name;
    private double value;
    private String units;
    private boolean isConserved;

    public VariableProperties()
    {

    }

    public VariableProperties(Variable var)
    {
        this.name = var.getName();
        this.value = var.getValue();
        this.units = var.getUnits();
        this.isConserved = var.isConserved();
    }

    public VariableProperties clone()
    {
        VariableProperties result = new VariableProperties();
        result.name = name;
        result.value = value;
        result.units = units;
        result.isConserved = isConserved;
        return result;
    }

    @PropertyName ( "Name" )
    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        this.name = name;
    }

    @PropertyName ( "Value" )
    public double getValue()
    {
        return value;
    }
    public void setValue(double value)
    {
        this.value = value;
    }

    @PropertyName ( "Units" )
    public String getUnits()
    {
        return units;
    }
    public void setUnits(String units)
    {
        this.units = units;
    }

    @PropertyName ( "Conserved" )
    public boolean isConserved()
    {
        return isConserved;
    }
    public void setConserved(boolean isConserved)
    {
        this.isConserved = isConserved;
    }
}