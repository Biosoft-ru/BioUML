package ru.biosoft.access;

import ru.biosoft.access.core.MessageBundle;
import ru.biosoft.access.core.VectorDataCollectionBeanInfo;

/**
 * @pending Document
 */
public class SqlDataCollectionBeanInfo extends VectorDataCollectionBeanInfo
{
    protected SqlDataCollectionBeanInfo(Class c, String messageBundle)
    {
        super(c, messageBundle );
    }
    
    public SqlDataCollectionBeanInfo()
    {
        super(SqlDataCollection.class, MessageBundle.class.getName() );
        
        initResources( "ru.biosoft.access.core.MessageBundle" );
        
        beanDescriptor.setDisplayName     ( getResourceString("CN_SQL_DC") );
        beanDescriptor.setShortDescription( getResourceString("CD_SQL_DC") );
    }
}
