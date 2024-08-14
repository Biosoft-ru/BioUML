package ru.biosoft.access.security;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.VectorDataCollectionBeanInfo;

public class ProtectedDataCollectionBeanInfo extends VectorDataCollectionBeanInfo
{
    
    public ProtectedDataCollectionBeanInfo ( )
    {
        this( ProtectedDataCollection.class, "CLIENT_DC", MessageBundle.class.getName ( ) );
    }

    public ProtectedDataCollectionBeanInfo(Class<? extends DataCollection> beanClass, String key, String messageBundle)
    {
        super ( beanClass, null );

        if ( key != null && messageBundle != null )
        {
            initResources ( messageBundle );
    
            beanDescriptor.setDisplayName ( getResourceString ( "CN_" + key ) );
            beanDescriptor.setShortDescription ( getResourceString ( "CD_" + key ) );
        }
    }
    
}