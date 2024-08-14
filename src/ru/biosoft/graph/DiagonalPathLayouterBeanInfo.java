package ru.biosoft.graph;

import ru.biosoft.graph.DiagonalPathLayouter;

import com.developmentontheedge.beans.BeanInfoEx;

public class DiagonalPathLayouterBeanInfo extends BeanInfoEx
{
    public DiagonalPathLayouterBeanInfo()
    {
        super(DiagonalPathLayouter.class, MessageBundle.class.getName() );
        
        beanDescriptor.setDisplayName( getResourceString("CN_LAYOUTER"));
        beanDescriptor.setShortDescription( getResourceString("CD_LAYOUTER") );
    }
}
