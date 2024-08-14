package ru.biosoft.util;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class FileItemBeanInfo extends BeanInfoEx
{
    public FileItemBeanInfo()
    {
        super(FileItem.class);
        beanDescriptor.setDisplayName(getResourceString("CN_FILE"));
        beanDescriptor.setShortDescription(getResourceString("CD_FILE"));
    }

    @Override
    protected void initProperties() throws Exception
    {
        add(new PropertyDescriptorEx("name", beanClass, "getDisplayName", null),
                getResourceString("PN_NAME"), getResourceString("PD_NAME"));
        add(new PropertyDescriptorEx("suffix", beanClass, "getSuffix", null),
                getResourceString("PN_SUFFIX"), getResourceString("PD_SUFFIX"));
        add(new PropertyDescriptorEx("nameNoSuffix", beanClass, "getNameWithoutSuffix", null),
                getResourceString("PN_NAME_NO_SUFFIX"), getResourceString("PD_NAME_NO_SUFFIX"));
    }
}
