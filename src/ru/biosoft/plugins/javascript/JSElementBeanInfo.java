package ru.biosoft.plugins.javascript;

import java.beans.PropertyDescriptor;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class JSElementBeanInfo extends BeanInfoEx
{
    public JSElementBeanInfo()
    {
        super(JSElement.class, "ru.biosoft.plugins.javascript.MessageBundle");
        beanDescriptor.setDisplayName(getResourceString("CN_JSELEMENT"));
        beanDescriptor.setShortDescription(getResourceString("CD_JSELEMENT"));
    }

    @Override
    public void initProperties() throws Exception
    {
        PropertyDescriptor pd = new PropertyDescriptorEx("name", beanClass, "getName", null);
        add(pd, getResourceString("PN_JSELEMENT_NAME"), getResourceString("PD_JSELEMENT_NAME"));

        pd = new PropertyDescriptorEx("content", beanClass);
        pd.setHidden(true);
        add(pd);
    }
}
