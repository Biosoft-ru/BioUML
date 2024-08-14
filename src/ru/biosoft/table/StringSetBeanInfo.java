package ru.biosoft.table;

import com.developmentontheedge.beans.BeanInfoEx;

public class StringSetBeanInfo extends BeanInfoEx
{
    public StringSetBeanInfo()
    {
        super(StringSet.class, StringSetMessageBundle.class.getName());
        setBeanEditor(StringSetViewer.class);
    }
}
