package biouml.plugins.server;

import ru.biosoft.access.SqlDataCollectionBeanInfo;

public class DataSqlCollectionBeanInfo extends SqlDataCollectionBeanInfo
{

    protected DataSqlCollectionBeanInfo ( Class c, String messageBundle )
    {
        super ( c, messageBundle );
    }

    public DataSqlCollectionBeanInfo ( )
    {
        super ( DataSqlCollection.class, MessageBundle.class.getName ( ) );

        initResources ( MessageBundle.class.getName ( ) );

        beanDescriptor.setDisplayName ( getResourceString ( "CN_SQL_DC" ) );
        beanDescriptor.setShortDescription ( getResourceString ( "CD_SQL_DC" ) );
    }

}
