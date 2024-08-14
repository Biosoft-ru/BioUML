package ru.biosoft.graph;

import ru.biosoft.graph.HierarchicPathLayouter;

import com.developmentontheedge.beans.BeanInfoEx;

public class HierarchicPathLayouterBeanInfo extends BeanInfoEx
{
    public HierarchicPathLayouterBeanInfo()
    {
        super(HierarchicPathLayouter.class, MessageBundle.class.getName() );
        
        beanDescriptor.setDisplayName( getResourceString("CN_LAYOUTER"));
        beanDescriptor.setShortDescription( getResourceString("CD_LAYOUTER") );
    }
}
