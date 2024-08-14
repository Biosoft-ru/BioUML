package ru.biosoft.access.security;

import java.sql.Connection;
import java.util.Properties;

import javax.annotation.Nonnull;

import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.access.exception.Assert;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.sql.SqlConnectionPool;
import ru.biosoft.access.sql.SqlUtil;

/**
 * Provides an access to bioumlsupport SQL database.
 */
public class GlobalDatabaseManager
{
    private static Properties properties;
    
    /**
     * Get SQL root privileges connection
     */
    public static @Nonnull Connection getDatabaseConnection() throws BiosoftSQLException
    {
        Connection connection = SqlConnectionPool.getPersistentConnection(properties);
        SqlUtil.checkConnection(connection);
        return connection;
    }

    /**
     * Initialize permissions information
     */
    public static void initDatabaseManager(@Nonnull Properties properties)
    {
        Assert.notNull("security.properties", properties);
        GlobalDatabaseManager.properties = properties;
    }
    
    /**
     * Get URL to current base database
     */
    public static String getCurrentDBUrl()
    {
        String dbUrl = properties.getProperty(SqlDataCollection.JDBC_URL_PROPERTY);
        return dbUrl.substring(0, dbUrl.lastIndexOf('/'));
    }
}
