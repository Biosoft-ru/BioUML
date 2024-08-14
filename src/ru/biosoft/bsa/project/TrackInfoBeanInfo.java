package ru.biosoft.bsa.project;

import ru.biosoft.bsa.MessageBundle;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class TrackInfoBeanInfo extends BeanInfoEx
{
    public TrackInfoBeanInfo(Class<? extends TrackInfo> clas, String string)
    {
        super(clas, string);
    }

    public TrackInfoBeanInfo()
    {
        super(TrackInfo.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("CN_TRACK_INFO"));
        beanDescriptor.setShortDescription(getResourceString("CD_TRACK_INFO"));
    }

    @Override
    public void initProperties() throws Exception
    {
        add(new PropertyDescriptorEx("dbName", beanClass, "getDbName", null),
                getResourceString("PN_TRACK_INFO_DB"),
                getResourceString("PD_TRACK_INFO_DB"));

        add(new PropertyDescriptorEx("title", beanClass),
                getResourceString("PN_TRACK_INFO_TITLE"),
                getResourceString("PD_TRACK_INFO_TITLE"));
        
        /*add(new PropertyDescriptorEx("track", beanClass),
                getResourceString("PN_TRACK_INFO_TITLE"),
                getResourceString("PD_TRACK_INFO_TITLE"));*/
        
        add(new PropertyDescriptorEx("group", beanClass),
                getResourceString("PN_TRACK_INFO_GROUP"),
                getResourceString("PD_TRACK_INFO_GROUP"));
        
        add(new PropertyDescriptorEx("order", beanClass),
                getResourceString("PN_TRACK_INFO_ORDER"),
                getResourceString("PD_TRACK_INFO_ORDER"));
        
        add(new PropertyDescriptorEx("visible", beanClass),
                getResourceString("PN_TRACK_INFO_VISIBLE"),
                getResourceString("PD_TRACK_INFO_VISIBLE"));
    }
}
