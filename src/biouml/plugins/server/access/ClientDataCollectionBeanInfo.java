package biouml.plugins.server.access;

import ru.biosoft.access.core.VectorDataCollectionBeanInfo;

/**
 * @pending Document
 */
public class ClientDataCollectionBeanInfo extends VectorDataCollectionBeanInfo
{
    
    public ClientDataCollectionBeanInfo ( )
    {
        this ( ClientDataCollection.class, "CLIENT_DC", MessageBundle.class.getName ( ) );
    }

    public ClientDataCollectionBeanInfo ( Class beanClass, String key, String messageBundle )
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
