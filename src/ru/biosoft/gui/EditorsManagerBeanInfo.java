package ru.biosoft.gui;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class EditorsManagerBeanInfo extends BeanInfoEx
{
    public EditorsManagerBeanInfo()
    {
        super(EditorsManager.class, "ru.biosoft.gui.resources.MessageBundle" );
        beanDescriptor.setDisplayName     (getResourceString("CN_EDITORS_MANAGER"));
        beanDescriptor.setShortDescription(getResourceString("CD_EDITORS_MANAGER"));
        setSubstituteByChild(true);
    }

    @Override
    public void initProperties() throws Exception
    {
        add(new PropertyDescriptorEx("editors", beanClass),
            getResourceString("PN_EDITORS_MANAGER"),
            getResourceString("PD_EDITORS_MANAGER"));
    }
}
