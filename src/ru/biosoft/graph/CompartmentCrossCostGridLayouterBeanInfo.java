package ru.biosoft.graph;

import ru.biosoft.graph.CompartmentCrossCostGridLayouter;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class CompartmentCrossCostGridLayouterBeanInfo extends BeanInfoEx
{
    public CompartmentCrossCostGridLayouterBeanInfo()
    {
        super(CompartmentCrossCostGridLayouter.class, MessageBundle.class.getName() );

        beanDescriptor.setDisplayName( getResourceString("CN_LAYOUTER"));
        beanDescriptor.setShortDescription( getResourceString("CD_LAYOUTER") );
    }

    @Override
    public void initProperties() throws Exception
    {
        initResources("ru.biosoft.graph.MessageBundle");

        add(new PropertyDescriptorEx("gridX", beanClass),
                getResourceString("PN_GRID_X"),
                getResourceString("PD_GRID_X"));

        add(new PropertyDescriptorEx("gridY", beanClass),
                getResourceString("PN_GRID_Y"),
                getResourceString("PD_GRID_Y"));

        add(new PropertyDescriptorEx("ne", beanClass),
                getResourceString("PN_CROSSCOST_NE"),
                getResourceString("PD_CROSSCOST_NE"));

        add(new PropertyDescriptorEx("rc", beanClass),
                getResourceString("PN_CROSSCOST_RC"),
                getResourceString("PD_CROSSCOST_RC"));

        add(new PropertyDescriptorEx("probabilityThreshold", beanClass),
                getResourceString("PN_CROSSCOST_PT"),
                getResourceString("PD_CROSSCOST_PT"));

        add(new PropertyDescriptorEx("saturationDist", beanClass),
                getResourceString("PN_CROSSCOST_ST"),
                getResourceString("PD_CROSSCOST_ST"));

        add( new PropertyDescriptorEx( "pathLayouterWrapper", beanClass ), getResourceString( "PN_PATH_LAYOUTER" ),
                getResourceString( "PD_PATH_LAYOUTER" ) );
    }
}
