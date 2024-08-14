package biouml.plugins.server.access;

import biouml.plugins.server.MessageBundle;

public class DataClientCollectionBeanInfo extends ClientDataCollectionBeanInfo
{

    public DataClientCollectionBeanInfo ( )
    {
        this ( DataClientCollection.class, "DATA_DC", MessageBundle.class.getName ( ) );
    }

    public DataClientCollectionBeanInfo ( Class<?> beanClass, String key, String messageBundle )
    {
        super ( beanClass, key, messageBundle );
    }
    
}
