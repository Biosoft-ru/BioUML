package ru.biosoft.graph;

import ru.biosoft.graph.GreedyLayouter;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class GreedyLayouterBeanInfo extends BeanInfoEx
{
    public GreedyLayouterBeanInfo()
    {
        super(GreedyLayouter.class, MessageBundle.class.getName());

        beanDescriptor.setDisplayName(getResourceString("CN_LAYOUTER"));
        beanDescriptor.setShortDescription(getResourceString("CD_LAYOUTER"));
    }

    @Override
    public void initProperties() throws Exception
    {
        initResources("ru.biosoft.graph.MessageBundle");

        add(new PropertyDescriptorEx("gridX", beanClass), getResourceString("PN_GRID_X"), getResourceString("PD_GRID_X"));

        add(new PropertyDescriptorEx("gridY", beanClass), getResourceString("PN_GRID_Y"), getResourceString("PD_GRID_Y"));

        add(new PropertyDescriptorEx("layerDeltaX", beanClass), getResourceString("PN_GREEDY_DX"), getResourceString("PN_GREEDY_DX"));

        add(new PropertyDescriptorEx("layerDeltaY", beanClass), getResourceString("PN_GREEDY_DY"), getResourceString("PN_GREEDY_DY"));

        add( new PropertyDescriptorEx( "pathLayouterWrapper", beanClass ), getResourceString( "PN_PATH_LAYOUTER" ),
                getResourceString( "PD_PATH_LAYOUTER" ) );

        add(new PropertyDescriptorEx("subgraphLayouter", beanClass),
                getResourceString("PN_SUBGRAPH_LAYOUTER"),
                getResourceString("PD_SUBGRAPH_LAYOUTER"));
    }
}
