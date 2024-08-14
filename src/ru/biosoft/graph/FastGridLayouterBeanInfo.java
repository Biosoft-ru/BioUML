package ru.biosoft.graph;


import ru.biosoft.graph.FastGridLayouter;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class FastGridLayouterBeanInfo extends BeanInfoEx
{
    public FastGridLayouterBeanInfo()
    {
        super(FastGridLayouter.class, MessageBundle.class.getName());

        beanDescriptor.setDisplayName(getResourceString("CN_LAYOUTER"));
        beanDescriptor.setShortDescription(getResourceString("CD_LAYOUTER"));
    }

    @Override
    public void initProperties() throws Exception
    {
        initResources("ru.biosoft.graph.MessageBundle");

        add(new PropertyDescriptorEx("gridX", beanClass), getResourceString("PN_GRID_X"), getResourceString("PD_GRID_X"));

        add(new PropertyDescriptorEx("gridY", beanClass), getResourceString("PN_GRID_Y"), getResourceString("PD_GRID_Y"));

        add(new PropertyDescriptorEx("iterations", beanClass), getResourceString("PN_FASTGRID_IT"), getResourceString("PD_FASTGRID_IT"));

        add(new PropertyDescriptorEx("cool", beanClass), getResourceString("PN_FASTGRID_CO"), getResourceString("PD_FASTGRID_CO"));

        add(new PropertyDescriptorEx("threadCount", beanClass), getResourceString("PN_FASTGRID_TC"), getResourceString("PD_FASTGRID_TC"));

//        add(new PropertyDescriptorEx("startingFromThisLayout", beanClass), getResourceString("PN_FASTGRID_SFTL"),
//                getResourceString("PD_FASTGRID_SFTL"));

        add( new PropertyDescriptorEx( "keepCompartmentSize", beanClass ), getResourceString( "PN_FASTGRID_COMPSIZE" ),
                getResourceString( "PD_FASTGRID_COMPSIZE" ) );
        
        add( new PropertyDescriptorEx( "adjustReactions", beanClass ), getResourceString( "PN_ADJUST_REACTIONS" ),
                getResourceString( "PD_ADJUST_REACTIONS" ) );

        PropertyDescriptorEx pde = new PropertyDescriptorEx("edgeEdgeCrossCost", beanClass);
        pde.setExpert(true);
        add(pde, getResourceString("PN_FASTGRID_EECOST"), getResourceString("PD_FASTGRID_EECOST"));

        pde = new PropertyDescriptorEx("edgeNodeCrossCost", beanClass);
        pde.setExpert(true);
        add(pde, getResourceString("PN_FASTGRID_ENCOST"), getResourceString("PD_FASTGRID_ENCOST"));

        pde = new PropertyDescriptorEx("nodeNodeCrossCost", beanClass);
        pde.setExpert(true);
        add(pde, getResourceString("PN_FASTGRID_NNCOST"), getResourceString("PD_FASTGRID_NNCOST"));

        pde = new PropertyDescriptorEx("strongAttraction", beanClass);
        pde.setExpert(true);
        add(pde, getResourceString("PN_FASTGRID_SATTR"), getResourceString("PD_FASTGRID_SATTR"));

        pde = new PropertyDescriptorEx("averageAttraction", beanClass);
        pde.setExpert(true);
        add(pde, getResourceString("PN_FASTGRID_AATTR"), getResourceString("PD_FASTGRID_AATTR"));

        pde = new PropertyDescriptorEx("weakAttraction", beanClass);
        pde.setExpert(true);
        add(pde, getResourceString("PN_FASTGRID_WATTR"), getResourceString("PD_FASTGRID_WATTR"));

        pde = new PropertyDescriptorEx("weakRepulsion", beanClass);
        pde.setExpert(true);
        add(pde, getResourceString("PN_FASTGRID_WREP"), getResourceString("PD_FASTGRID_WREP"));

        pde = new PropertyDescriptorEx("averageRepulsion", beanClass);
        pde.setExpert(true);
        add(pde, getResourceString("PN_FASTGRID_AREP"), getResourceString("PD_FASTGRID_AREP"));

        pde = new PropertyDescriptorEx("strongRepulsion", beanClass);
        pde.setExpert(true);
        add(pde, getResourceString("PN_FASTGRID_SREP"), getResourceString("PD_FASTGRID_SREP"));

        add( new PropertyDescriptorEx( "pathLayouterWrapper", beanClass ), getResourceString( "PN_PATH_LAYOUTER" ),
                getResourceString( "PD_PATH_LAYOUTER" ) );
    }
}
