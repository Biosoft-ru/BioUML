package ru.biosoft.access.core;

import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.core.MessageBundle;
import ru.biosoft.access.repository.DataElementPathEditor;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class DataElementPathSetBeanInfo extends BeanInfoEx
{
    public DataElementPathSetBeanInfo()
    {
        super(DataElementPathSet.class, MessageBundle.class.getName());
        setBeanEditor(DataElementPathEditor.class);
        getBeanDescriptor().setValue(DataElementPathEditor.MULTI_SELECT, true);
        setHideChildren(true);
        setNoRecursionCheck(true);
    }

    @Override
    protected void initProperties() throws Exception
    {
        PropertyDescriptorEx pde;
        pde = new PropertyDescriptorEx("path", beanClass, "getPath", null);
        add(pde);
    }
}
