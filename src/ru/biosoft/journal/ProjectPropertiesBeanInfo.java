package ru.biosoft.journal;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class ProjectPropertiesBeanInfo extends BeanInfoEx
{
    public ProjectPropertiesBeanInfo()
    {
        super( ProjectProperties.class, true );
    }

    @Override
    protected void initProperties() throws Exception
    {
        add(new PropertyDescriptorEx("defaultDatabaseVersions", beanClass, "getDefaultDatabaseVersions", null));
    }
}
