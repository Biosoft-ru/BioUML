package ru.biosoft.bsa.access;

import ru.biosoft.bsa.gui.MessageBundle;

import com.developmentontheedge.beans.BeanInfoEx;

public class SequencesDatabaseInfoBeanInfo extends BeanInfoEx
{
    public SequencesDatabaseInfoBeanInfo()
    {
        super(SequencesDatabaseInfo.class, MessageBundle.class.getName());
        setBeanEditor(SequencesDatabaseInfoSelector.class);
        setSimple(true);
        setHideChildren(true);
    }
}