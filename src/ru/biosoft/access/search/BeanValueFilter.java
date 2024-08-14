package ru.biosoft.access.search;

import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

import ru.biosoft.access.core.filter.CompositeFilter;
import ru.biosoft.access.core.filter.Filter;

import com.developmentontheedge.beans.BeanInfoConstants;

public class BeanValueFilter extends CompositeFilter
{

    /** @pending high Think on working with array properties. */
    public BeanValueFilter(Class<?> beanClass) throws IntrospectionException
    {
        BeanInfo bi = Introspector.getBeanInfo(beanClass);
        PropertyDescriptor[] pd = bi.getPropertyDescriptors();
        for( PropertyDescriptor descriptor : pd )
        {
            if( Object[].class.isAssignableFrom(descriptor.getPropertyType()) )
            {
                //add( new ArrayPropertyValueFilter(descriptor) );
            }
            else
            {
                //System.err.println("PD=" + descriptor.getName());
                add(new PropertyValueFilter(descriptor));
            }
        }
    }

    public String getItemDisplayName(Integer index, Object o)
    {
        try
        {
            Filter filter = this.getFilter(index.intValue());
            BeanInfo bi = Introspector.getBeanInfo(filter.getClass());
            BeanDescriptor bd = bi.getBeanDescriptor();
            Method m = (Method)bd.getValue(BeanInfoConstants.BEAN_DISPLAY_NAME);
            String str = (String)m.invoke(filter);
            return str;
        }
        catch( Throwable t )
        {
        }
        return index.toString();
    }
}