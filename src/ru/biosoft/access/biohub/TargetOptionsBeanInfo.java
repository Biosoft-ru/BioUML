package ru.biosoft.access.biohub;

import java.beans.PropertyDescriptor;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class TargetOptionsBeanInfo extends BeanInfoEx
{
    public TargetOptionsBeanInfo()
    {
        super(TargetOptions.class, MessageBundle.class.getName());
    }

    protected TargetOptionsBeanInfo(Class<?> beanClass, String messageBundle)
    {
        super(beanClass, messageBundle == null ? MessageBundle.class.getName() : messageBundle);
    }

    @Override
    public void initProperties() throws Exception
    {
        initResources(MessageBundle.class.getName());

        PropertyDescriptor pde = new PropertyDescriptorEx("collections", beanClass, "getCollections", null);
        add(pde, getResourceString("CN_COLLECTIONS"), getResourceString("CD_COLLECTIONS"));
    }
}
