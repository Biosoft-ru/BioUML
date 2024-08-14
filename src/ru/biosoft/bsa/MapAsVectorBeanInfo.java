package ru.biosoft.bsa;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class MapAsVectorBeanInfo extends BeanInfoEx
{
    public MapAsVectorBeanInfo()
    {
        super( MapAsVector.class, MessageBundle.class.getName() );

        beanDescriptor.setDisplayName     (getResourceString("CN_MAP_AS_VECTOR"));
        beanDescriptor.setShortDescription(getResourceString("CD_MAP_AS_VECTOR"));
    }

    @Override
    public void initProperties() throws Exception
    {
        add(new PropertyDescriptorEx("name", beanClass.getMethod("getName"), null),
            getResourceString("PN_MAP_AS_VECTOR_NAME"),
            getResourceString("PD_MAP_AS_VECTOR_NAME") );
        
        add(new PropertyDescriptorEx("sequence", beanClass.getMethod("getSequence"), null),
                getResourceString("PN_MAP_AS_VECTOR_SEQUENCE"),
                getResourceString("PD_MAP_AS_VECTOR_SEQUENCE") );
        
        add(new PropertyDescriptorEx("properties", beanClass.getMethod("getProperties"), null),
                getResourceString("PN_MAP_AS_VECTOR_ATTR"),
                getResourceString("PD_MAP_AS_VECTOR_ATTR") );
    }
}
