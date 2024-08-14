package biouml.plugins.server;

import biouml.plugins.server.access.ClientDataCollectionBeanInfo;

public class DiagramClientCollectionBeanInfo extends
        ClientDataCollectionBeanInfo
{

    public DiagramClientCollectionBeanInfo ( )
    {
        this ( DiagramClientCollection.class, "DIAGRAM_DC", MessageBundle.class.getName ( ) );
    }

    public DiagramClientCollectionBeanInfo ( Class beanClass, String key, String messageBundle )
    {
        super ( beanClass, key, messageBundle );
    }
    
}
