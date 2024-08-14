package biouml.plugins.server;

public class DiagramSqlCollectionBeanInfo extends DataSqlCollectionBeanInfo
{

    protected DiagramSqlCollectionBeanInfo ( Class c, String messageBundle )
    {
        super ( c, messageBundle );
    }

    public DiagramSqlCollectionBeanInfo ( )
    {
        super ( DiagramSqlCollection.class, MessageBundle.class.getName ( ) );

        initResources ( MessageBundle.class.getName ( ) );

        beanDescriptor.setDisplayName ( getResourceString ( "CN_SQLDIAGRAM_DC" ) );
        beanDescriptor.setShortDescription ( getResourceString ( "CD_SQLDIAGRAM_DC" ) );
    }
    
}
