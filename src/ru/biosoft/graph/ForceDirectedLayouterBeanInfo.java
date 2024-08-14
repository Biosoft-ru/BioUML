package ru.biosoft.graph;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class ForceDirectedLayouterBeanInfo extends BeanInfoEx
{
    public ForceDirectedLayouterBeanInfo()
    {
        super(ForceDirectedLayouter.class, MessageBundle.class.getName() );

        beanDescriptor.setDisplayName( getResourceString("CN_LAYOUTER"));
        beanDescriptor.setShortDescription( getResourceString("CD_LAYOUTER") );
    }

    @Override
    public void initProperties() throws Exception
    {
        initResources("ru.biosoft.graph.MessageBundle");

        add(new PropertyDescriptorEx("initialPlacement", beanClass), InitialPlacementEditor.class,
                getResourceString("PN_FORCEDIRECTED_IP"),
                getResourceString("PD_FORCEDIRECTED_IP"));

        add(new PropertyDescriptorEx("edgeLength", beanClass),
                getResourceString("PN_FORCEDIRECTED_EL"),
                getResourceString("PD_FORCEDIRECTED_EL"));

        add(new PropertyDescriptorEx("repulsion", beanClass),
                getResourceString("PN_FORCEDIRECTED_RP"),
                getResourceString("PD_FORCEDIRECTED_RP"));

        add(new PropertyDescriptorEx("repulsionDistance", beanClass),
                getResourceString("PN_FORCEDIRECTED_RD"),
                getResourceString("PD_FORCEDIRECTED_RD"));

        add(new PropertyDescriptorEx("gravity", beanClass),
                getResourceString("PN_FORCEDIRECTED_GR"),
                getResourceString("PD_FORCEDIRECTED_GR"));

        add(new PropertyDescriptorEx("orientation", beanClass), OrientationEditor.class,
                getResourceString("PN_FORCEDIRECTED_OR"),
                getResourceString("PD_FORCEDIRECTED_OR"));

        add(new PropertyDescriptorEx("distanceMethod", beanClass), DistanceMethodEditor.class,
                getResourceString("PN_FORCEDIRECTED_DM"),
                getResourceString("PD_FORCEDIRECTED_DM"));

        add(new PropertyDescriptorEx("attraction", beanClass),
                getResourceString("PN_FORCEDIRECTED_AT"),
                getResourceString("PD_FORCEDIRECTED_AT"));

        add(new PropertyDescriptorEx("magneticIntencity", beanClass),
                getResourceString("PN_FORCEDIRECTED_MI"),
                getResourceString("PD_FORCEDIRECTED_MI"));

        add(new PropertyDescriptorEx("iterationNumber", beanClass),
                getResourceString("PN_FORCEDIRECTED_IN"),
                getResourceString("PD_FORCEDIRECTED_IN"));

        add(new PropertyDescriptorEx("minTemperature", beanClass),
                getResourceString("PN_FORCEDIRECTED_MIT"),
                getResourceString("PD_FORCEDIRECTED_MIT"));

        add(new PropertyDescriptorEx("maxTemperature", beanClass),
                getResourceString("PN_FORCEDIRECTED_MAT"),
                getResourceString("PD_FORCEDIRECTED_MAT"));

        add(new PropertyDescriptorEx("horisontalMovementAllowed", beanClass),
                getResourceString("PN_FORCEDIRECTED_HM"),
                getResourceString("PD_FORCEDIRECTED_HM"));

        add(new PropertyDescriptorEx("verticalMovementAllowed", beanClass),
                getResourceString("PN_FORCEDIRECTED_VM"),
                getResourceString("PD_FORCEDIRECTED_VM"));

        add( new PropertyDescriptorEx( "pathLayouterWrapper", beanClass ), getResourceString( "PN_PATH_LAYOUTER" ),
                getResourceString( "PD_PATH_LAYOUTER" ) );

        add(new PropertyDescriptorEx("subgraphLayouter", beanClass),
                getResourceString("PN_SUBGRAPH_LAYOUTER"),
                getResourceString("PD_SUBGRAPH_LAYOUTER"));
    }

    public static class InitialPlacementEditor extends com.developmentontheedge.beans.editors.TagEditorSupport
    {
        public InitialPlacementEditor()
        {
            super( new String[] {"Current placement", "Random placement"}, 1 );
        }
    }

    public static class OrientationEditor extends com.developmentontheedge.beans.editors.TagEditorSupport
    {
        public OrientationEditor()
        {
            super(new String[]{"Top to bottom", "Bottom to top", "Left to right", "Right to left"}, 0);
        }
    }

    public static class DistanceMethodEditor extends com.developmentontheedge.beans.editors.TagEditorSupport
    {
        public DistanceMethodEditor()
        {
            super(new String[]{"Minimal", "Center", "Diagonal"}, 0);
        }
    }
}
