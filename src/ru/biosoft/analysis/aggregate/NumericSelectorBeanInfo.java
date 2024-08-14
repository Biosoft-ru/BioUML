package ru.biosoft.analysis.aggregate;

import com.developmentontheedge.beans.BeanInfoEx;

public class NumericSelectorBeanInfo extends BeanInfoEx
{
    public NumericSelectorBeanInfo()
    {
        super(NumericSelector.class, MessageBundle.class.getName());
        setBeanEditor(NumericSelectorEditor.class);
        setSimple(true);
        setHideChildren(true);
    }
}
