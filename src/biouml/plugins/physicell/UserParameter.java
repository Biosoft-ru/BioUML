package biouml.plugins.physicell;

import com.developmentontheedge.beans.annot.PropertyName;

public class UserParameter implements Cloneable
{
    private String name;
    private String type;
    private String units;
    private String value;

    @PropertyName ( "Name" )
    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        this.name = name;
    }

    @PropertyName ( "Type" )
    public String getType()
    {
        return type;
    }
    public void setType(String type)
    {
        this.type = type;
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

    @PropertyName ( "Value" )
    public String getValue()
    {
        return value;
    }
    public void setValue(String value)
    {
        this.value = value;
    }

    @Override
    public UserParameter clone()
    {
        try
        {
            return (UserParameter)super.clone();
        }
        catch( CloneNotSupportedException e )
        {
            e.printStackTrace();
            return null;
        }
    }
}