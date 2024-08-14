package ru.biosoft.access;

import java.beans.PropertyDescriptor;

import com.developmentontheedge.beans.BeanInfoEx;

/**
 * @author lan
 *
 */
public class HtmlDataElementBeanInfo extends BeanInfoEx
{
    public HtmlDataElementBeanInfo()
    {
        super( HtmlDataElement.class, true );
    }

    @Override
    protected void initProperties() throws Exception
    {
        add(new PropertyDescriptor("name", beanClass, "getName", null));
    }
}
