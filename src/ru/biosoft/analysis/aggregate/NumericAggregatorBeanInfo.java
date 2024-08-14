package ru.biosoft.analysis.aggregate;

import com.developmentontheedge.beans.BeanInfoEx;

public class NumericAggregatorBeanInfo extends BeanInfoEx
{
    public NumericAggregatorBeanInfo()
    {
        super(NumericAggregator.class, MessageBundle.class.getName());
        setBeanEditor(NumericAggregatorEditor.class);
        setSimple(true);
        setHideChildren(true);
    }
}
