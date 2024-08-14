package ru.biosoft.graph;
import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class SubgraphLayouterBeanInfo extends BeanInfoEx
{
    public SubgraphLayouterBeanInfo()
    {
        super(SubgraphLayouter.class, MessageBundle.class.getName());

        beanDescriptor.setDisplayName(getResourceString("CN_LAYOUTER"));
        beanDescriptor.setShortDescription(getResourceString("CD_LAYOUTER"));
    }

    @Override
    public void initProperties() throws Exception
    {
        initResources("ru.biosoft.graph.MessageBundle");

        add(new PropertyDescriptorEx("layerDeltaX", beanClass),
                getResourceString("PN_SUBGRAPH_DX"),
                getResourceString("PD_SUBGRAPH_DX"));
        
        add(new PropertyDescriptorEx("layerDeltaY", beanClass),
                getResourceString("PN_SUBGRAPH_DY"),
                getResourceString("PD_SUBGRAPH_DY"));
    }
}
