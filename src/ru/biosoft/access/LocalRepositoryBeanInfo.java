package ru.biosoft.access;

import com.developmentontheedge.beans.PropertyDescriptorEx;

import ru.biosoft.access.core.MessageBundle;
import ru.biosoft.access.core.VectorDataCollectionBeanInfo;

/**
 * @pending Document
 */
public class LocalRepositoryBeanInfo extends VectorDataCollectionBeanInfo
{
    public LocalRepositoryBeanInfo()
    {
        super(LocalRepository.class, MessageBundle.class.getName() );
        
        initResources( "ru.biosoft.access.core.MessageBundle" );
        
        beanDescriptor.setDisplayName     ( getResourceString("CN_LOCAL_REPOSITORY") );
        beanDescriptor.setShortDescription( getResourceString("CD_LOCAL_REPOSITORY") );
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        
        initResources( "ru.biosoft.access.core.MessageBundle" );
        
        add(2, new PropertyDescriptorEx( "absolutePath", beanClass, "getAbsolutePath", null),
            getResourceString("PN_LOCAL_REPOSITORY_ABSOLUTE_PATH"),
            getResourceString("PD_LOCAL_REPOSITORY_ABSOLUTE_PATH"));
    }
}
