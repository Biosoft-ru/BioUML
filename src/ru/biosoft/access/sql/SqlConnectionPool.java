package ru.biosoft.access.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Nonnull;
import one.util.streamex.StreamEx;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.SqlDataCollection;
import ru.biosoft.exception.LoggedException;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.security.SessionThread;
import ru.biosoft.exception.InternalException;
import ru.biosoft.util.TextUtil;


/**
 * Utility class for management connections for SQL databases
 * through JDBC.
 */
public class SqlConnectionPool
{
    static Logger cat = Logger.getLogger(SqlConnectionPool.class.getName());

    public static final long CONNECTION_TIMEOUT = 120000; // ms

    /**
     * Map of default JDBC driver classes
     */
    private static final Map<String, String> defaultDrivers = new HashMap<>();
    static
    {
        defaultDrivers.put("mysql", "com.mysql.jdbc.Driver");
    }

    /**
     * Connection hash map.
     * Key - user@url, value - the connection.
     */
    static ThreadLocal<Map<String, PersistentConnection>> persistentConnections = new ThreadLocal<>();

    static SqlConnectionCloser sqlConnectionCloser = new SqlConnectionCloser();

    /**
     * Get connection specific for selected {@link ru.biosoft.access.core.DataCollection}. If collection info has not JDBC properties try to search on parent.
     * @param dc - data collection
     * @return
     * @throws SQLException
     */
    public static Connection getConnection(@Nonnull DataCollection<?> dc) throws BiosoftSQLException
    {
        Properties properties = dc.getInfo().getProperties();
        String urlProperty = properties.getProperty(SqlDataCollection.JDBC_URL_PROPERTY);
        if( urlProperty != null && !urlProperty.isEmpty() )
        {
            return SqlConnectionPool.getPersistentConnection(properties);
        }
        for( DataCollection<?> parent = dc.getOrigin(); parent != null; parent = parent.getOrigin() )
        {
            try
            {
                return DataCollectionUtils.getSqlConnection(parent);
            }
            catch( IllegalArgumentException e )
            {
            }
        }
        return null;
    }

    public static void closeMyConnections()
    {
        Map<String, PersistentConnection> threadConnections;
        threadConnections = persistentConnections.get();
        persistentConnections.set(null);
        if(threadConnections != null)
        {
            for(PersistentConnection connection: threadConnections.values())
            {
                try
                {
                    connection.close();
                }
                catch( SQLException e )
                {
                }
            }
        }
    }

    /**
     * Returns persistent connection which will try to reopen itself if dropped by DB engine
     *
     * @param url - URL for database connection
     * @param user - user for database connection
     * @param password - password for database connection.
     * @see PersistentConnection
     */
    public static @Nonnull Connection getPersistentConnection(String url, String user, String password) throws BiosoftSQLException
    {
        try
        {
            Map<String, PersistentConnection> threadConnections = persistentConnections.get();
            if(threadConnections == null)
            {
                threadConnections = new HashMap<>();
                persistentConnections.set(threadConnections);
            }
            String key = user + "@" + url;
            // close current thread connections which are inactive for a long time
            for(PersistentConnection pc : StreamEx.ofValues(threadConnections, k -> !k.equals(key)))
            {
                pc.checkTimeout(CONNECTION_TIMEOUT);
            }
            PersistentConnection connection = threadConnections.get(key);
            if( connection == null )
            {
                connection = new PersistentConnection(getNewConnection(url, user, password), url, user, password);
                threadConnections.put(key, connection);
                if( ! ( Thread.currentThread() instanceof SessionThread ) )
                    sqlConnectionCloser.register( connection );
            }
            return connection;
        }
        catch( SQLException e )
        {
            throw new BiosoftSQLException(url, null, e);
        }
    }

    public static @Nonnull Connection getPersistentConnection(Properties properties) throws LoggedException
    {
        String url = properties.getProperty(SqlDataCollection.JDBC_URL_PROPERTY);
        if(url == null)
            throw new InternalException("Parameter "+SqlDataCollection.JDBC_URL_PROPERTY+" absent");
        if(!url.startsWith( "jdbc:" ))
            return Connectors.getConnection( url );
        String user = properties.getProperty(SqlDataCollection.JDBC_USER_PROPERTY);
        if(user == null)
            throw new InternalException("Parameter "+SqlDataCollection.JDBC_USER_PROPERTY+" absent");
        String password = properties.getProperty(SqlDataCollection.JDBC_PASSWORD_PROPERTY);
        if(password == null)
            throw new InternalException("Parameter "+SqlDataCollection.JDBC_PASSWORD_PROPERTY+" absent");
        return getPersistentConnection(url, user, password);
    }

    /**
     * Creates new connection, put it in hash and returns it.
     *
     * @param url - URL for database connection
     * @param user - user for database connection
     * @param password - password for database connection.
     */
    protected static Connection getNewConnection(String url, String user, String password) throws SQLException
    {
        String info = "\n  url=" + url + "\n  user=" + user + "\n  password=" + password;

        // try to create new connection
        if( cat.isLoggable( Level.FINE ) )
            cat.log(Level.FINE, "Sql connection creating:" + info);

        String[] urlParts = TextUtil.split( url, ':' );
        if( urlParts.length > 2 )
        {
            String driverName = defaultDrivers.get(urlParts[1]);
            if( driverName != null )
            {
                try
                {
                    Class.forName(driverName);
                }
                catch( ClassNotFoundException e )
                {
                    //nothing to do, maybe it will work with previous loaded drivers.
                }
            }
        }

        Properties properties = new Properties();
        properties.put( "user", user );
        properties.put( "password", password );
        properties.put( "useUnicode", "true" );
        properties.put( "characterEncoding", "utf8" );
        Connection conn = DriverManager.getConnection(url, properties);

        return conn;
    }
}
