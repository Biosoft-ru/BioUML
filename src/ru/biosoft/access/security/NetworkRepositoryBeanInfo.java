package ru.biosoft.access.security;

import ru.biosoft.access.core.MessageBundle;
import ru.biosoft.access.core.VectorDataCollectionBeanInfo;

/**
 * @pending Document
 */
public class NetworkRepositoryBeanInfo extends VectorDataCollectionBeanInfo
{
    public NetworkRepositoryBeanInfo()
    {
        super( NetworkRepository.class, MessageBundle.class.getName() );
        
        initResources( "ru.biosoft.access.core.MessageBundle" );
        
        beanDescriptor.setDisplayName     ( getResourceString("CN_LOCAL_REPOSITORY") );
        beanDescriptor.setShortDescription( getResourceString("CD_LOCAL_REPOSITORY") );
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        
        initResources( "ru.biosoft.access.core.MessageBundle" );
    }
}
