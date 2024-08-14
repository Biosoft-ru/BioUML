package ru.biosoft.plugins.javascript;

import com.developmentontheedge.beans.BeanInfoEx;

public class JSCommandBeanInfo extends BeanInfoEx
{
    public JSCommandBeanInfo()
    {
        super(JSCommand.class, "ru.biosoft.plugins.javascript.MessageBundle");
        beanDescriptor.setDisplayName(getResourceString("CN_JSCOMMAND"));
        beanDescriptor.setShortDescription(getResourceString("CD_JSCOMMAND"));
        setBeanEditor(JSCommandViewer.class);
    }
}
