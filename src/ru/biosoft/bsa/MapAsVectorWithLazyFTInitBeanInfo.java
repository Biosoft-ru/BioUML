package ru.biosoft.bsa;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class MapAsVectorWithLazyFTInitBeanInfo extends BeanInfoEx
{
    public MapAsVectorWithLazyFTInitBeanInfo()
    {
        super( MapAsVectorWithLazyFTInit.class, MessageBundle.class.getName() );

        beanDescriptor.setDisplayName     (getResourceString("CN_MAP_AS_VECTOR"));
        beanDescriptor.setShortDescription(getResourceString("CD_MAP_AS_VECTOR"));
    }

    @Override
    public void initProperties() throws Exception
    {
        add(new PropertyDescriptorEx("name", beanClass.getMethod("getName"), null),
            getResourceString("PN_MAP_AS_VECTOR_NAME"),
            getResourceString("PD_MAP_AS_VECTOR_NAME") );
    }
}
