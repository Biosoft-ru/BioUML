package ru.biosoft.bsa.filter;

import java.beans.BeanDescriptor;
import java.beans.DefaultPersistenceDelegate;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.IndexedPropertyDescriptorEx;

public class SiteFilterBeanInfo extends BeanInfoEx
{
    public SiteFilterBeanInfo()
    {
        super(SiteFilter.class, "ru.biosoft.bsa.filter.MessageBundle");
        beanDescriptor.setDisplayName     (getResourceString("PN_SITE_FILTER"));
        beanDescriptor.setShortDescription(getResourceString("PD_SITE_FILTER"));
    }

    @Override
    protected void initProperties() throws Exception
    {
        add(new IndexedPropertyDescriptorEx("filter", beanClass),
            getResourceString("PN_SITE_FILTER"),
            getResourceString("PD_SITE_FILTER"));
        setSubstituteByChild(true);
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
                    "siteSetName",
                    "basis",
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

