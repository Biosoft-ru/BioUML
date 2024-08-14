package ru.biosoft.plugins.javascript;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.web.HtmlPropertyInspector;

public class HostObjectInfoBeanInfo extends BeanInfoEx
{
    public HostObjectInfoBeanInfo()
    {
        super(HostObjectInfo.class, "ru.biosoft.plugins.javascript.MessageBundle");
        beanDescriptor.setDisplayName     (getResourceString("CN_HOST_OBJECT_INFO"));
        beanDescriptor.setShortDescription(getResourceString("CD_HOST_OBJECT_INFO"));
    }

    @Override
    public void initProperties() throws Exception
    {
        HtmlPropertyInspector.setHtmlGeneratorMethod(beanDescriptor, beanClass.getMethod("toString"));

        add(new PropertyDescriptorEx("name", beanClass, "getName", null),
                getResourceString("PN_HOST_OBJECT_NAME"),
                getResourceString("PD_HOST_OBJECT_NAME"));

        PropertyDescriptorEx pde = new PropertyDescriptorEx("type", beanClass);
        pde.setReadOnly(true);
        add(pde,
            getResourceString("PN_HOST_OBJECT_TYPE"),
            getResourceString("PD_HOST_OBJECT_TYPE"));

        pde = new PropertyDescriptorEx("description", beanClass);
        pde.setReadOnly(true);
        add(pde,
            getResourceString("PN_HOST_OBJECT_DESCRIPTION"),
            getResourceString("PD_HOST_OBJECT_DESCRIPTION"));
    }
}

