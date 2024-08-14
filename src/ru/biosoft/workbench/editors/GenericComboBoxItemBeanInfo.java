package ru.biosoft.workbench.editors;

import com.developmentontheedge.beans.BeanInfoEx;

public class GenericComboBoxItemBeanInfo extends BeanInfoEx
{
    public GenericComboBoxItemBeanInfo()
    {
        super(GenericComboBoxItem.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("CN_CLASS"));
        beanDescriptor.setShortDescription(getResourceString("CD_CLASS"));
        setBeanEditor(GenericComboBoxEditor.class);
    }
}