package ru.biosoft.plugins.javascript;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.web.HtmlPropertyInspector;

public class PropertyInfoBeanInfo extends BeanInfoEx
{
    public PropertyInfoBeanInfo()
    {
        super(PropertyInfo.class, "ru.biosoft.plugins.javascript.MessageBundle");
        beanDescriptor.setDisplayName     (getResourceString("CN_PROPERTY_INFO"));
        beanDescriptor.setShortDescription(getResourceString("CD_PROPERTY_INFO"));
    }

    @Override
    public void initProperties() throws Exception
    {
        HtmlPropertyInspector.setHtmlGeneratorMethod(beanDescriptor, beanClass.getMethod("toString"));

        add(new PropertyDescriptorEx("name", beanClass, "getName", null),
                getResourceString("PN_PROPERTY_NAME"),
                getResourceString("PD_PROPERTY_NAME"));

        PropertyDescriptorEx pde = new PropertyDescriptorEx("type", beanClass);
        pde.setReadOnly(true);
        add(pde,
            getResourceString("PN_PROPERTY_TYPE"),
            getResourceString("PD_PROPERTY_TYPE"));

        pde = new PropertyDescriptorEx("readOnly", beanClass);
        pde.setReadOnly(true);
        add(pde,
            getResourceString("PN_PROPERTY_READ_ONLY"),
            getResourceString("PD_PROPERTY_READ_ONLY"));

        pde = new PropertyDescriptorEx("description", beanClass);
        pde.setReadOnly(true);
        add(pde,
            getResourceString("PN_PROPERTY_DESCRIPTION"),
            getResourceString("PD_PROPERTY_DESCRIPTION"));

    }
}

