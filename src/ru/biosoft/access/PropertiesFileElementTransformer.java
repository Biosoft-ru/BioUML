package ru.biosoft.access;

import java.io.File;
import java.util.Map.Entry;
import java.util.Properties;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.exception.BiosoftCustomException;
import ru.biosoft.util.BeanUtil;
import ru.biosoft.util.ExProperties;
import ru.biosoft.util.PropertyInfo;
import ru.biosoft.util.TextUtil;

import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.model.Property;

/**
 * This transformer creates ru.biosoft.access.core.DataElement by file in Properties format where properties names are bean properties of the element
 * This is handy way to create ru.biosoft.access.core.DataElement's which are not ru.biosoft.access.core.DataCollection
 * Required property is "class" which is a class of element to create
 * "plugins" might be specified also to simplify class look-up
 * Element must have a constructor(name, origin)
 * @author lan
 */
public class PropertiesFileElementTransformer extends AbstractFileTransformer<DataElement>
{
    @Override
    public Class<DataElement> getOutputType()
    {
        return ru.biosoft.access.core.DataElement.class;
    }

    @Override
    public DataElement load(File input, String name, DataCollection<DataElement> origin) throws Exception
    {
        Properties properties = new ExProperties(input);
        String className = properties.getProperty(DataCollectionConfigConstants.CLASS_PROPERTY);
        if(className == null)
            throw new BiosoftCustomException(null, "Class is missing in file");
        String plugins = properties.getProperty(DataCollectionConfigConstants.PLUGINS_PROPERTY);
        Class<? extends DataElement> clazz = ClassLoading.loadSubClass( className, plugins == null ? ClassLoading.getPluginForClass( className ) : plugins, DataElement.class );
        properties.remove(DataCollectionConfigConstants.CLASS_PROPERTY);
        properties.remove(DataCollectionConfigConstants.PLUGINS_PROPERTY);
        DataElement result = clazz.getConstructor(String.class, DataCollection.class).newInstance(name, origin);
        ComponentModel model = ComponentFactory.getModel(result);
        for(Entry<Object, Object> entry: properties.entrySet())
        {
            Property property = model.findProperty(entry.getKey().toString());
            if(property != null) property.setValue(TextUtil.fromString(property.getValueClass(), entry.getValue().toString()));
        }
        return result;
    }

    @Override
    public void save(File output, DataElement element) throws Exception
    {
        Properties properties = new ExProperties();
        ComponentModel model = ComponentFactory.getModel(element);
        for(PropertyInfo info: BeanUtil.getRecursivePropertiesList(model))
        {
            Property property = model.findProperty(info.getName());
            if(property != null)
                properties.setProperty(info.getName(), TextUtil.toString(property.getValue()));
        }
        properties.setProperty(DataCollectionConfigConstants.CLASS_PROPERTY, element.getClass().getName());
        ExProperties.store(properties, output);
    }
}
