package ru.biosoft.workbench.editors;

import java.beans.IntrospectionException;
import ru.biosoft.access.core.DataElementPath;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.model.ComponentFactory;

public class DataElementMultiSelector extends GenericMultiSelectEditor
{
    public static final String COLLECTION_PROPERTY = "CollectionProperty";

    @Override
    protected Object[] getAvailableValues()
    {
        try
        {
            Object propertyName = getDescriptor().getValue(COLLECTION_PROPERTY);
            DataElementPath path = (DataElementPath)ComponentFactory.getModel(getBean()).findProperty(propertyName.toString()).getValue();
            return path.getDataCollection().names().sorted().toArray( String[]::new );
        }
        catch( Exception e )
        {
            return new String[0];
        }
    }

    public static PropertyDescriptorEx registerSelector(PropertyDescriptorEx pde, String collectionProperty)
    {
        pde.setPropertyEditorClass(DataElementMultiSelector.class);
        pde.setSimple(true);
        pde.setValue(COLLECTION_PROPERTY, collectionProperty);
        return pde;
    }

    public static PropertyDescriptorEx registerSelector(String property, Class<?> beanClass, String collectionProperty) throws IntrospectionException
    {
        return registerSelector(new PropertyDescriptorEx(property, beanClass), collectionProperty);
    }
}
