package ru.biosoft.graph;

import ru.biosoft.graph.HierarchicLayouter;
import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class HierarchicLayouterBeanInfo extends BeanInfoEx
{
    public HierarchicLayouterBeanInfo()
    {
        super(HierarchicLayouter.class, MessageBundle.class.getName() );

        beanDescriptor.setDisplayName( getResourceString("CN_LAYOUTER"));
        beanDescriptor.setShortDescription( getResourceString("CD_LAYOUTER") );
    }

    @Override
    public void initProperties() throws Exception
    {
        initResources("ru.biosoft.graph.MessageBundle");

        add( new PropertyDescriptorEx( "verticalOrientation", beanClass ),
                getResourceString("PN_HIERARCHIC_VO"),
                getResourceString("PD_HIERARCHIC_VO"));

        add(new PropertyDescriptorEx("hoistNodes", beanClass),
                getResourceString("PN_HIERARCHIC_HN"),
                getResourceString("PD_HIERARCHIC_HN"));

        add(new PropertyDescriptorEx("layerOrderIterationNum", beanClass),
                getResourceString("PN_HIERARCHIC_LO"),
                getResourceString("PD_HIERARCHIC_LO"));

        add(new PropertyDescriptorEx("processNeighbours", beanClass),
                getResourceString("PN_HIERARCHIC_PN"),
                getResourceString("PD_HIERARCHIC_PN"));

        add(new PropertyDescriptorEx("layerDeltaX", beanClass),
                getResourceString("PN_HIERARCHIC_DX"),
                getResourceString("PD_HIERARCHIC_DX"));

        add(new PropertyDescriptorEx("layerDeltaY", beanClass),
                getResourceString("PN_HIERARCHIC_DY"),
                getResourceString("PD_HIERARCHIC_DY"));

        add(new PropertyDescriptorEx("virtualNodesDistance", beanClass),
                getResourceString("PN_HIERARCHIC_VND"),
                getResourceString("PD_HIERARCHIC_VND"));

        add(new PropertyDescriptorEx("splineEdges", beanClass),
                "Spline edges",
                "Spline edges");

        add( new PropertyDescriptorEx( "pathLayouterWrapper", beanClass ), getResourceString( "PN_PATH_LAYOUTER" ),
                getResourceString( "PD_PATH_LAYOUTER" ) );

        add(new PropertyDescriptorEx("subgraphLayouter", beanClass),
                getResourceString("PN_SUBGRAPH_LAYOUTER"),
                getResourceString( "PD_SUBGRAPH_LAYOUTER" ) );
    }
}
