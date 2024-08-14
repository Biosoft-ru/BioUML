package ru.biosoft.bsa;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class SiteImplBeanInfo extends BeanInfoEx
{
    public SiteImplBeanInfo()
    {
        super( SiteImpl.class, MessageBundle.class.getName() );

        beanDescriptor.setDisplayName     (getResourceString("CN_SITE"));
        beanDescriptor.setShortDescription(getResourceString("CD_SITE"));
    }

    @Override
    public void initProperties() throws Exception
    {
        add(new PropertyDescriptorEx("name", beanClass.getMethod("getName"), null),
            getResourceString("PN_SITE_NAME"),
            getResourceString("PD_SITE_NAME") );
        
        add(new PropertyDescriptorEx("sequenceName", beanClass.getMethod("getSequenceName"), null),
                getResourceString("PN_SEQUENCE_NAME"),
                getResourceString("PD_SEQUENCE_NAME") );
            
        add(new PropertyDescriptorEx("type", beanClass.getMethod("getType"), null),
                getResourceString("PN_SITE_TYPE"),
                getResourceString("PD_SITE_TYPE") );
        
        add(new PropertyDescriptorEx("from", beanClass.getMethod("getFrom"), null),
                getResourceString("PN_SITE_FROM"),
                getResourceString("PD_SITE_FROM") );
        
        add(new PropertyDescriptorEx("to", beanClass.getMethod("getTo"), null),
                getResourceString("PN_SITE_TO"),
                getResourceString("PD_SITE_TO") );
        
        add(new PropertyDescriptorEx("length", beanClass.getMethod("getLength"), null),
                getResourceString("PN_SITE_LENGTH"),
                getResourceString("PD_SITE_LENGTH") );
        
        add(new PropertyDescriptorEx("strand", beanClass.getMethod("getStrand"), null), StrandEditor.class,
                getResourceString("PN_SITE_STRAND"),
                getResourceString("PD_SITE_STRAND") );
        
        add(new PropertyDescriptorEx(Site.SCORE_PROPERTY, beanClass.getMethod("getScore"), null),
                getResourceString("PN_SITE_SCORE"),
                getResourceString("PD_SITE_SCORE") );
        
        add(new PropertyDescriptorEx("model", beanClass.getMethod("getModel"), null),
                getResourceString("PN_MODEL_NAME"),
                getResourceString("PD_MODEL_NAME") );
        
        add(new PropertyDescriptorEx("properties", beanClass.getMethod("getProperties"), null),
                getResourceString("PN_SITE_ATTR"),
                getResourceString("PD_SITE_ATTR") );
    }
}
