package biouml.plugins.server;

import java.sql.Connection;
import java.util.Properties;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DefaultQuerySystem;
import ru.biosoft.access.core.QuerySystem;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.support.BeanInfoEntryTransformer;
import biouml.model.Module;
import biouml.plugins.server.access.AccessProtocol;
import biouml.standard.type.access.TitleIndex;
import biouml.standard.type.access.TitleSqlIndex;

/**
 * Wrapper over SqlDataCollection class which obtain
 * all connection properties from Module
 */
public class DataSqlCollection extends SqlDataCollection
{

    private Connection connection;
    
    public DataSqlCollection ( DataCollection<?> parent, Properties properties )
    {
        super ( parent, properties );
    }

    @Override
    protected void preInit ( Properties properties )
    {
        Module module = Module.getModule ( getOrigin ( ) );
        
        // Init server connection properties
        String driver = properties.getProperty ( JDBC_DRIVER_PROPERTY );
        if ( driver == null )
        {
            driver = module.getInfo ( ).getProperty ( JDBC_DRIVER_PROPERTY );
            properties.put ( JDBC_DRIVER_PROPERTY, driver );
        }
        
        // Init qeury system property
        if ( properties.get ( TitleSqlIndex.INDEX_TITLE_TABLE ) != null ||
                properties.get ( TitleSqlIndex.INDEX_TITLE_QUERY ) != null )
        {
            properties.put (  QuerySystem.QUERY_SYSTEM_CLASS, DefaultQuerySystem.class.getName ( ) );
            properties.put (  QuerySystem.INDEX_LIST, "title" );
            properties.put (  TitleIndex.INDEX_TITLE, TitleSqlIndex.class.getName ( ) );
        }
        
        String text = properties.getProperty ( AccessProtocol.TEXT_TRANSFORMER_NAME );
        if ( text == null )
        {
            properties.put ( AccessProtocol.TEXT_TRANSFORMER_NAME, BeanInfoEntryTransformer.class.getName ( ) );
        }
    }
    
    @Override
    public Connection getConnection ( ) throws BiosoftSQLException
    {
        Properties properties = getInfo ( ).getProperties ( );
        if ( properties.getProperty ( JDBC_URL_PROPERTY ) == null ||
                properties.getProperty ( JDBC_USER_PROPERTY ) == null ||
                properties.getProperty ( JDBC_PASSWORD_PROPERTY ) == null )
        {
            if ( connection == null )
            {
                Module module = Module.optModule ( getOrigin ( ) );
                if ( module instanceof SqlModule )
                {
                    connection = ( ( SqlModule ) module ).getConnection ( );
                    return connection;
                }
            }
        }
        return super.getConnection ( );
    }
    
}
