package ru.biosoft.table.columnbeans;

import java.util.Properties;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.ReadOnlyVectorCollection;
import ru.biosoft.util.BeanUtil;
import ru.biosoft.util.PropertyInfo;

import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.model.Property;

/**
 * Collection with bean-based descriptors
 * @author lan
 */
public class BeanBasedDescriptorCollection extends ReadOnlyVectorCollection<Descriptor>
{
    private static final String SOURCE_PROPERTY = "source";
    private DataCollection<? extends DataElement> source;
    
    /**
     * @param parent
     * @param properties
     */
    public BeanBasedDescriptorCollection(DataCollection<?> parent, Properties properties)
    {
        super(parent, properties);
        String pathStr = properties.getProperty(SOURCE_PROPERTY);
        if(pathStr == null) return;
        DataElementPath path = (pathStr.startsWith("./") || pathStr.startsWith("../"))?DataElementPath.create(this).getRelativePath(pathStr):DataElementPath.create(pathStr);
        source = path.optDataCollection();
    }

    @Override
    protected void doInit()
    {
        DataElement dataElement = null;
        try
        {
            dataElement = source.get(source.getNameList().get(0));
        }
        catch( Exception e )
        {
        }
        if(dataElement == null) return;
        ComponentModel model = ComponentFactory.getModel(dataElement);
        for(PropertyInfo info: BeanUtil.getRecursivePropertiesList(dataElement))
        {
            Property property = model.findProperty(info.getName());
            if(property == null || DynamicPropertySet.class.isAssignableFrom(property.getValueClass())) continue;
            doPut(new BeanBasedDescriptor(info.getDisplayName().replace("/", ":"), this, source, info.getName()), true);
        }
    }

    @Override
    public @Nonnull Class<? extends DataElement> getDataElementType()
    {
        return Descriptor.class;
    }
}
