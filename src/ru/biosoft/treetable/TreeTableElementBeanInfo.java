package ru.biosoft.treetable;

import java.beans.PropertyDescriptor;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class TreeTableElementBeanInfo extends BeanInfoEx
{
    public TreeTableElementBeanInfo()
    {
        super(TreeTableElement.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("CN_VIEWMODEL"));
        beanDescriptor.setShortDescription(getResourceString("CD_VIEWMODEL"));
    }

    @Override
    public void initProperties() throws Exception
    {
        PropertyDescriptor pd = new PropertyDescriptorEx("treePath", beanClass);
        add(pd, getResourceString("PN_VIEWMODEL_TREE"), getResourceString("PD_VIEWMODEL_TREE"));

        pd = new PropertyDescriptorEx("tableScript", beanClass);
        add(pd, getResourceString("PN_VIEWMODEL_TABLE"), getResourceString("PD_VIEWMODEL_TABLE"));
    }
}
