package ru.biosoft.access;

import ru.biosoft.access.core.MessageBundle;
import ru.biosoft.access.core.VectorDataCollectionBeanInfo;

public class FileImagesDataCollectionBeanInfo extends VectorDataCollectionBeanInfo
{
    public FileImagesDataCollectionBeanInfo()
    {
        super(FileImagesDataCollection.class, MessageBundle.class.getName() );
        
        initResources( "ru.biosoft.access.core.MessageBundle" );
        
        beanDescriptor.setDisplayName     ( getResourceString("CN_IMAGES_DC") );
        beanDescriptor.setShortDescription( getResourceString("CD_IMAGES_DC") );
    }

    public FileImagesDataCollectionBeanInfo(Class c, String messageBundle)
    {
        super(c, messageBundle);
    }
}
