package biouml.plugins.simulation.document;

import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;

/**
 * @author axec
 *
 */
public class InputParameter implements DataElement
{
    private String name;    
    private String title;
    private double value;
    private double defaultValue;
    private double valueStep;
    private String type;
    
    public InputParameter(String name, String title, String type, double value)
    {
        this.name = name;
        this.title = title;
        this.type = type;
        this.value =value;
        this.defaultValue = value;
        this.valueStep = value / 10;
    }

    @PropertyName("Name")
    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        this.name = name;
    }
    
    @PropertyName("Title")
    public String getTitle()
    {
        return title;
    }
    public void setTitle(String title)
    {
        this.title = title;
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
    
    @PropertyName("Step")
    public double getValueStep()
    {
        return valueStep;
    }
    public void setValueStep(double valueStep)
    {
        this.valueStep = valueStep;
    }

    @Override
    public DataCollection<?> getOrigin()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @PropertyName("Type")
    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }
    
    @PropertyName("DefaultValue")
    public double getDefaultValue()
    {
        return defaultValue;
    }
    public void setDefaultValue(double defaultValue)
    {
        this.defaultValue = defaultValue;
    }
}
