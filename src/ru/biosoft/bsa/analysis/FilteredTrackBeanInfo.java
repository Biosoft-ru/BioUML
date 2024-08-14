package ru.biosoft.bsa.analysis;

import ru.biosoft.bsa.MessageBundle;
import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class FilteredTrackBeanInfo extends BeanInfoEx
{
    public FilteredTrackBeanInfo()
    {
        super(FilteredTrack.class, MessageBundle.class.getName());
    }

    @Override
    public void initProperties() throws Exception
    {
        add(new PropertyDescriptorEx("sourcePath", beanClass), getResourceString("PN_TRACK"),
                getResourceString("PD_TRACK"));
        add(new PropertyDescriptorEx("originPath", beanClass));
        add(new PropertyDescriptorEx("name", beanClass));
        add(new PropertyDescriptorEx("filter", beanClass), getResourceString("PN_FILTER"),
                getResourceString("PD_FILTER"));
    }
}
