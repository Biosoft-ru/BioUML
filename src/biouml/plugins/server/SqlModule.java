package biouml.plugins.server;

import java.sql.Connection;
import java.util.Properties;
import java.util.logging.Level;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.sql.SqlConnectionHolder;
import ru.biosoft.access.sql.SqlConnectionPool;
import biouml.model.Module;
import biouml.standard.SqlModuleType;

/**
 * Special wrapper over Module, which can
 * managed sql connection
 */
public class SqlModule extends Module implements SqlConnectionHolder
{
    public SqlModule ( DataCollection primaryCollection, Properties properties ) throws Exception
    {
        super ( primaryCollection, properties );
    }

    @Override
    protected void preInit ( Properties properties )
    {
        String driver = null;
        try
        {
            // Load the SQL JDBC driver
            driver = properties.getProperty ( SqlDataCollection.JDBC_DRIVER_PROPERTY );
            if ( driver != null )
            {
                Class.forName ( driver );
            }
        }
        catch ( ClassNotFoundException e )
        {
            log.log( Level.SEVERE, "Can not load JDBC driver: " + driver );
            throw new IllegalArgumentException ( e );
        }
    }

    @Override
    protected void applyType ( String className ) throws Exception
    {
        if ( className == null || className.trim ( ).length ( ) == 0 )
            type = new SqlModuleType ( );
        else
            super.applyType ( className );
    }

    @Override
    public synchronized Connection getConnection() throws BiosoftSQLException
    {
        return SqlConnectionPool.getConnection(this);
    }
}
