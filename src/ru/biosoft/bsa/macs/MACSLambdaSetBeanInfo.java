package ru.biosoft.bsa.macs;

import ru.biosoft.bsa.gui.MessageBundle;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class MACSLambdaSetBeanInfo extends BeanInfoEx
{
    public MACSLambdaSetBeanInfo()
    {
        super(MACSLambdaSet.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("CN_CLASS"));
        beanDescriptor.setShortDescription(getResourceString("CD_CLASS"));
    }
    
    @Override
    public void initProperties() throws Exception
    {
        add(new PropertyDescriptorEx("lambda1", beanClass), getResourceString("PN_MACS_LAMBDA1"), getResourceString("PD_MACS_LAMBDA1"));
        add(new PropertyDescriptorEx("lambda2", beanClass), getResourceString("PN_MACS_LAMBDA2"), getResourceString("PD_MACS_LAMBDA2"));
        add(new PropertyDescriptorEx("lambda3", beanClass), getResourceString("PN_MACS_LAMBDA3"), getResourceString("PD_MACS_LAMBDA3"));
    }
}
