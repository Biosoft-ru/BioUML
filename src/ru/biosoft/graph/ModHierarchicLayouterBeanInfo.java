package ru.biosoft.graph;

import ru.biosoft.graph.ModHierarchicLayouter;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class ModHierarchicLayouterBeanInfo extends BeanInfoEx
{
    public ModHierarchicLayouterBeanInfo()
    {
        super(ModHierarchicLayouter.class, MessageBundle.class.getName() );
        
        beanDescriptor.setDisplayName( getResourceString("CN_LAYOUTER"));
        beanDescriptor.setShortDescription( getResourceString("CD_LAYOUTER") );
    }
    
    @Override
    public void initProperties() throws Exception
    {
        initResources("ru.biosoft.graph.MessageBundle");
        
        add(new PropertyDescriptorEx("verticalOrientation", beanClass),
                getResourceString("PN_HIERARCHIC_VO"),
                getResourceString("PD_HIERARCHIC_VO"));
        
        add(new PropertyDescriptorEx("hoistNodes", beanClass),
                getResourceString("PN_HIERARCHIC_HN"),
                getResourceString("PD_HIERARCHIC_HN"));
        
        add(new PropertyDescriptorEx("layerOrderIterationNum", beanClass),
                getResourceString("PN_HIERARCHIC_LO"),
                getResourceString("PD_HIERARCHIC_LO"));
        
        add(new PropertyDescriptorEx("straightenMethod", beanClass), StraightenMethodEditor.class,
                getResourceString("PN_HIERARCHIC_SM"),
                getResourceString("PD_HIERARCHIC_SM"));
        
        add(new PropertyDescriptorEx("forceDirectedLayouter", beanClass),
                getResourceString("PN_HIERARCHIC_FD"),
                getResourceString("PD_HIERARCHIC_FD"));
        
        add(new PropertyDescriptorEx("straightenIterationNum", beanClass),
                getResourceString("PN_HIERARCHIC_SI"),
                getResourceString("PD_HIERARCHIC_SI"));
        
        add(new PropertyDescriptorEx("processNeighbours", beanClass),
                getResourceString("PN_HIERARCHIC_PN"),
                getResourceString("PD_HIERARCHIC_PN"));
        
        add(new PropertyDescriptorEx("layerDeltaX", beanClass),
                getResourceString("PN_HIERARCHIC_DX"),
                getResourceString("PD_HIERARCHIC_DX"));
        
        add(new PropertyDescriptorEx("layerDeltaY", beanClass),
                getResourceString("PN_HIERARCHIC_DY"),
                getResourceString("PD_HIERARCHIC_DY"));
        
        add(new PropertyDescriptorEx("sameNameNodesWeight", beanClass, "getsameNameNodesWeight", "setsameNameNodesWeight"),
                getResourceString("PN_MODHIERARCHIC_SN"),
                getResourceString("PD_MODHIERARCHIC_SN"));
        
        add(new PropertyDescriptorEx("dummyEdgesCoeff", beanClass, "getdummyEdgesCoeff", "setdummyEdgesCoeff"),
                getResourceString("PN_MODHIERARCHIC_DE"),
                getResourceString("PD_MODHIERARCHIC_DE"));
        
        add(new PropertyDescriptorEx("scoreWeight", beanClass, "getscoreWeight", "setscoreWeight"),
                getResourceString("PN_MODHIERARCHIC_SW"),
                getResourceString("PD_MODHIERARCHIC_SW"));
        
        add(new PropertyDescriptorEx("edgesCrossCoeff", beanClass, "getedgesCrossCoeff", "setedgesCrossCoeff"),
                getResourceString("PN_MODHIERARCHIC_EC"),
                getResourceString("PD_MODHIERARCHIC_EC"));
    }
    
    public static class StraightenMethodEditor extends com.developmentontheedge.beans.editors.TagEditorSupport
    {
        public StraightenMethodEditor()
        {
            super(new String[]{"Default", "Force directed"}, 0);
        }
    }
}
