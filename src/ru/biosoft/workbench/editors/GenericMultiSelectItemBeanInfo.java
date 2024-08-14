package ru.biosoft.workbench.editors;

import com.developmentontheedge.beans.BeanInfoEx;

public class GenericMultiSelectItemBeanInfo extends BeanInfoEx
{
    public GenericMultiSelectItemBeanInfo()
    {
        super(GenericMultiSelectItem.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("CN_CLASS"));
        beanDescriptor.setShortDescription(getResourceString("CD_CLASS"));
        setBeanEditor(GenericMultiSelectEditor.class);
    }
}