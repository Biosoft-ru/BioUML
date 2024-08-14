package ru.biosoft.table.columnbeans;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class ColumnBeanInfo extends BeanInfoEx
{
    public ColumnBeanInfo()
    {
        super(Column.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("PN_COLUMNS"));
        beanDescriptor.setShortDescription(getResourceString("PD_COLUMNS"));
        setHideChildren(true);
        setCompositeEditor("newName;timePoint", new java.awt.GridLayout(1, 2));
    }
    @Override
    public void initProperties() throws Exception
    {
        add(new PropertyDescriptorEx("name", beanClass), getResourceString("PN_COLUMN_NAME"), getResourceString("PD_COLUMN_NAME"));
        add(new PropertyDescriptorEx("newName", beanClass), getResourceString("PN_COLUMN_NEW_NAME"),
                getResourceString("PD_COLUMN_NEW_NAME"));
        add(new PropertyDescriptorEx("timePoint", beanClass), getResourceString("PN_COLUMN_TIME_POINT"),
                getResourceString("PD_COLUMN_TIME_POINT"));
    }
}
