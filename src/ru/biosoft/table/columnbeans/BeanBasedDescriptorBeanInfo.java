package ru.biosoft.table.columnbeans;

import com.developmentontheedge.beans.BeanInfoEx;

/**
 * @author lan
 *
 */
public class BeanBasedDescriptorBeanInfo extends BeanInfoEx
{
    public BeanBasedDescriptorBeanInfo()
    {
        super(BeanBasedDescriptor.class, MessageBundle.class.getName());
    }
}
