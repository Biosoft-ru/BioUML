package ru.biosoft.bsa.project;

import ru.biosoft.bsa.MessageBundle;
import ru.biosoft.bsa.StrandEditor;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.ChoicePropertyDescriptorEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class RegionBeanInfo extends BeanInfoEx
{
    public RegionBeanInfo(Class<? extends Region> clas, String string)
    {
        super(clas, string);
    }

    public RegionBeanInfo()
    {
        super(Region.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("CN_REGION"));
        beanDescriptor.setShortDescription(getResourceString("CD_REGION"));
    }

    @Override
    public void initProperties() throws Exception
    {
        add(new PropertyDescriptorEx("sequenceName", beanClass, "getSequenceName", null),
                getResourceString("PN_REGION_DB"),
                getResourceString("PD_REGION_DB"));

        add(new PropertyDescriptorEx("title", beanClass),
                getResourceString("PN_REGION_TITLE"),
                getResourceString("PD_REGION_TITLE"));
        
        add(new ChoicePropertyDescriptorEx("strand", beanClass, StrandEditor.class),
                getResourceString("PN_REGION_STRAND"),
                getResourceString("PD_REGION_STRAND"));
        
        add(new PropertyDescriptorEx("from", beanClass),
                getResourceString("PN_REGION_FROM"),
                getResourceString("PD_REGION_FROM"));
        
        add(new PropertyDescriptorEx("to", beanClass),
                getResourceString("PN_REGION_TO"),
                getResourceString("PD_REGION_TO"));
        
        add(new PropertyDescriptorEx("order", beanClass),
                getResourceString("PN_REGION_ORDER"),
                getResourceString("PD_REGION_ORDER"));
        
        add(new PropertyDescriptorEx("visible", beanClass),
                getResourceString("PN_REGION_VISIBLE"),
                getResourceString("PD_REGION_VISIBLE"));
    }
}
