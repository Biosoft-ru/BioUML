package ru.biosoft.workbench.editors;

import java.beans.IntrospectionException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Comparator;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;

import com.developmentontheedge.beans.PropertyDescriptorEx;

public class DataElementComboBoxSelector extends GenericComboBoxEditor
{
    public static final String COLLECTION = "Collection";

    public static final String SORT_ORDER = "SortOrder";
    public static enum SortOrder
    {
        DEFAULT, // same order as in parent data collection
        NAME, // order by element name (using ru.biosoft.access.core.DataElement.getName)
        STRING // order by String representation (using Object.toString)
    }

    @Override
    protected Object[] getAvailableValues()
    {
        Object value = getDescriptor().getValue(COLLECTION);
        if(!(value instanceof DataElementPath)) return new ru.biosoft.access.core.DataElement[0];
        DataCollection<? extends DataElement> dc = ((DataElementPath)value).optDataCollection();
        if(dc == null) return new ru.biosoft.access.core.DataElement[0];
        ru.biosoft.access.core.DataElement[] result = dc.stream().toArray( size -> (DataElement[])Array.newInstance(dc.getDataElementType(), size) );
        SortOrder sortOrder = getSortOrder();
        if(sortOrder == SortOrder.NAME)
            Arrays.sort( result, Comparator.comparing( ru.biosoft.access.core.DataElement::getName ) );
        else if(sortOrder == SortOrder.STRING)
            Arrays.sort( result, Comparator.comparing( ru.biosoft.access.core.DataElement::toString ) );
        return result;
    }
    
    protected SortOrder getSortOrder()
    {
        Object sortOrder = getDescriptor().getValue( SORT_ORDER );
        if(!(sortOrder instanceof SortOrder))
            return SortOrder.NAME;
        return (SortOrder)sortOrder;
    }
    

    public static PropertyDescriptorEx registerSelector(PropertyDescriptorEx pde, DataElementPath collectionPath)
    {
        pde.setPropertyEditorClass(DataElementComboBoxSelector.class);
        pde.setSimple(true);
        pde.setValue(COLLECTION, collectionPath);
        return pde;
    }

    public static PropertyDescriptorEx registerSelector(String property, Class<?> beanClass, DataElementPath collectionPath) throws IntrospectionException
    {
        return registerSelector(new PropertyDescriptorEx(property, beanClass), collectionPath);
    }
    
    public static PropertyDescriptorEx registerSelector(String property, Class<?> beanClass, DataElementPath collectionPath, SortOrder sortOrder) throws IntrospectionException
    {
        PropertyDescriptorEx pde = new PropertyDescriptorEx(property, beanClass);
        pde.setValue( SORT_ORDER, sortOrder );
        return registerSelector(pde, collectionPath);
    }
}
