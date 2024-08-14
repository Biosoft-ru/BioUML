package ru.biosoft.table;

import com.developmentontheedge.beans.BeanInfoEx;

public class DescribedStringBeanInfo extends BeanInfoEx
{
    public DescribedStringBeanInfo()
    {
        super(DescribedString.class, MessageBundle.class.getName());
        setBeanEditor(DescribedStringViewer.class);
    }
}
