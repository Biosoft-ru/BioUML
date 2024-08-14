package ru.biosoft.bsa.filter;

import java.beans.BeanDescriptor;
import java.beans.DefaultPersistenceDelegate;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class RegionFilterBeanInfo extends BeanInfoEx
{
    public RegionFilterBeanInfo()
    {
        super(RegionFilter.class, "ru.biosoft.bsa.filter.MessageBundle");
        beanDescriptor.setDisplayName(getResourceString("DISPLAY_NAME"));
        beanDescriptor.setShortDescription(getResourceString("SHORT_DESCRIPTION"));
        //setCompositeEditor("enabled;regionStart;regionLength", new java.awt.GridLayout(1, 3));
    }

    @Override
    public void initProperties() throws Exception
    {
        add(new PropertyDescriptorEx( "enabled", beanClass),
            getResourceString("ENABLED_NAME"),           //  display_name()
            getResourceString("ENABLED_DESCRIPTION"));   //  description

        add(new PropertyDescriptorEx( "totalLength", beanClass, "getTotalLength", null),
            getResourceString("TOTALLENGTH_NAME"),
            getResourceString("TOTALLENGTH_DESCRIPTION"));

        add(new PropertyDescriptorEx( "totalSiteNumber", beanClass, "getTotalSiteNumber", null),
            getResourceString("TOTALSITENUMBER_NAME"),
            getResourceString("TOTALSITENUMBER_DESCRIPTION"));

        add(new PropertyDescriptorEx( "regionFrom", beanClass),
            getResourceString("REGIONFROM_NAME"),
            getResourceString("REGIONFROM_DESCRIPTION"));

        add(new PropertyDescriptorEx( "regionTo", beanClass),
            getResourceString("REGIONTO_NAME"),
            getResourceString("REGIONTO_DESCRIPTION"));

        add(new PropertyDescriptorEx( "regionSiteNumber", beanClass, "getRegionSiteNumber", null),
            getResourceString("REGIONSITENUMBER_NAME"),
            getResourceString("REGIONSITENUMBER_DESCRIPTION"));
    }

    @Override
    public BeanDescriptor getBeanDescriptor()
    {
        BeanDescriptor descriptor = super.getBeanDescriptor();
        try
        {
            descriptor.setValue("persistenceDelegate",
                new DefaultPersistenceDelegate(
                new String[]
                {
                    "mapName",
                })
                {
                    @Override
                    protected boolean mutatesTo(Object oldInstance, Object newInstance)
                    {
                        return (newInstance != null &&
                        oldInstance.getClass() == newInstance.getClass());
                    }
                });
        }
        catch (Exception ex)
        {
            return null;
        }
        return descriptor;
    }

}