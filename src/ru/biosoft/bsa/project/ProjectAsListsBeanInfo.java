package ru.biosoft.bsa.project;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

/**
 * @author lan
 *
 */
public class ProjectAsListsBeanInfo extends BeanInfoEx
{
    public ProjectAsListsBeanInfo()
    {
        super(ProjectAsLists.class, MessageBundle.class.getName());
    }

    @Override
    protected void initProperties() throws Exception
    {
        super.initProperties();
        add(new PropertyDescriptorEx("name", beanClass, "getName", null));
        add(new PropertyDescriptorEx("regions", beanClass, "getRegions", null));
        add(new PropertyDescriptorEx("tracks", beanClass, "getTracks", null));
        add(new PropertyDescriptorEx("trackNames", beanClass, "getTrackNames", null));
        add(new PropertyDescriptorEx("description", beanClass));
    }
}
