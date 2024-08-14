package ru.biosoft.table.columnbeans;

import java.beans.IntrospectionException;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.BeanUtil;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.model.ComponentFactory;

// TODO: unify code with ColumnNameSelector
public class ColumnNamesSelector extends GenericMultiSelectEditor
{
    public static final String COLLECTION_PROPERTY = "CollectionProperty";
    public static final String NUMERIC_ONLY_PROPERTY = "NumericOnly";
    
    @Override
    public Object[] getAvailableValues()
    {
        Object propertyName = getDescriptor().getValue(COLLECTION_PROPERTY);
        DataElementPath path = (DataElementPath)ComponentFactory.getModel(getBean()).findProperty(propertyName.toString()).getValue();
        if(path == null)
            return new String[] {};
        TableDataCollection collection = path.optDataElement(TableDataCollection.class);
        if( collection == null )
            return new String[] {};
        boolean numericOnly = BeanUtil.getBooleanValue(this, NUMERIC_ONLY_PROPERTY);
        return collection.columns().filter( numericOnly ? c -> c.getType().isNumeric() : c -> true )
                .map( TableColumn::getName ).toArray( String[]::new );
    }
    
    public static PropertyDescriptorEx registerSelector(PropertyDescriptorEx pde, String collectionProperty)
    {
        pde.setPropertyEditorClass(ColumnNamesSelector.class);
        pde.setSimple(true);
        pde.setHideChildren( true );
        pde.setValue(COLLECTION_PROPERTY, collectionProperty);
        return pde;
    }

    public static PropertyDescriptorEx registerSelector(String property, Class<?> beanClass, String collectionProperty) throws IntrospectionException
    {
        return registerSelector(new PropertyDescriptorEx(property, beanClass), collectionProperty);
    }

    public static PropertyDescriptorEx registerNumericSelector(PropertyDescriptorEx pde, String collectionProperty)
    {
        pde.setPropertyEditorClass(ColumnNamesSelector.class);
        pde.setSimple(true);
        pde.setHideChildren( true );
        pde.setValue(COLLECTION_PROPERTY, collectionProperty);
        pde.setValue(NUMERIC_ONLY_PROPERTY, true);
        return pde;
    }

    public static PropertyDescriptorEx registerNumericSelector(String property, Class<?> beanClass, String collectionProperty) throws IntrospectionException
    {
        return registerNumericSelector(new PropertyDescriptorEx(property, beanClass), collectionProperty);
    }
}