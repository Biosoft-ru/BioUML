package biouml.model;

import java.beans.PropertyDescriptor;
import java.io.File;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.DataElementExporter;
import ru.biosoft.util.bean.StaticDescriptor;

public abstract class ScalableElementExporter implements DataElementExporter
{
    private static final String SCALE_PROPERTY = "scale";
    private static final PropertyDescriptor SCALE_PD = StaticDescriptor.create(SCALE_PROPERTY, "Scale");
    protected DynamicPropertySet properties = new DynamicPropertySetAsMap();

    public ScalableElementExporter()
    {
        this.properties.add(new DynamicProperty(SCALE_PD, Double.class, 1.0));
    }
    
    protected double getScale()
    {
        DynamicProperty scaleProperty = properties.getProperty(SCALE_PROPERTY);
        return (scaleProperty == null || scaleProperty.getValue() == null)?1:(Double)scaleProperty.getValue();
    }
    
    @Override
    public Object getProperties(DataElement de, File file)
    {
        return properties;
    }
}
