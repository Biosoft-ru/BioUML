package biouml.standard.type;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataCollection;

@PropertyName("Complex")
@PropertyDescription("Complex.")
public class Complex extends Concept
{
    private String[] components;
    
    public Complex(DataCollection origin, String name)
    {
        super(origin, name);
    }

    @Override
    public String getType()
    {
        return TYPE_SUBSTANCE;
    }
    
    @PropertyName("Components")
    @PropertyDescription("Complex components.")
    public String[] getComponents()
    {
        return components;
    }
    public void setComponents(String[] components)
    {
        String[] oldValue = this.components;
        this.components = components;
        firePropertyChange("components", oldValue, components);
    }
}
