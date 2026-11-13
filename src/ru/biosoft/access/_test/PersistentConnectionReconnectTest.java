package ru.biosoft.access._test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLSyntaxErrorException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import junit.framework.TestCase;
import ru.biosoft.access.sql.PersistentConnection;
import ru.biosoft.access.sql.SqlConnectionPool;
import ru.biosoft.access.sql.SqlUtil;

/**
 * Reconnection of {@link PersistentConnection} when the server dropped the connection (MySQL wait_timeout),
 * using a fake JDBC driver: each opened connection is a {@link FakeConnection} that can be killed.
 */
public class PersistentConnectionReconnectTest extends TestCase
{
    private static final String URL_PREFIX = "jdbc:fakereconnect:";

    /** Connections opened by the fake driver, in order */
    static final List<FakeConnection> opened = new ArrayList<>();
    private static Driver driver;
    private static int urlCounter;

    @Override
    protected void setUp() throws Exception
    {
        if( driver == null )
        {
            driver = new FakeDriver();
            DriverManager.registerDriver( driver );
        }
        opened.clear();
    }

    /** Distinct URL per test: pooled connections are thread-local and keyed by URL */
    private static String newUrl()
    {
        return URL_PREFIX + "db" + ( ++urlCounter );
    }

    /**
     * The failure seen on strange.genexplain.com: the driver had already closed the connection and throws
     * "No operations allowed after connection closed." (SQLNonTransientConnectionException, 08003),
     * which was not considered reconnectable, so the thread's connection stayed broken.
     */
    public void testReconnectsWhenDriverAlreadyClosedTheConnection() throws Exception
    {
        Connection conn = SqlConnectionPool.getPersistentConnection( newUrl(), "u", "p" );
        assertEquals( 1, opened.size() );
        opened.get( 0 ).closedByDriver = true;

        SqlUtil.checkConnection( conn );

        assertEquals( "a new connection must be opened", 2, opened.size() );
        assertEquals( 1, opened.get( 1 ).executed );
    }

    /** An idle connection the server dropped is replaced when the pool hands it out again */
    public void testIdleDeadConnectionIsReplacedOnCheckout() throws Exception
    {
        String url = newUrl();
        PersistentConnection conn = (PersistentConnection)SqlConnectionPool.getPersistentConnection( url, "u", "p" );
        opened.get( 0 ).dead = true;

        conn.validateIfIdle( -1, 5 ); // treat as idle

        assertEquals( 2, opened.size() );
        assertTrue( "the dead connection is closed", opened.get( 0 ).closedByDriver );
        SqlUtil.checkConnection( conn );
        assertEquals( 0, opened.get( 0 ).executed );
        assertEquals( 1, opened.get( 1 ).executed );
    }

    public void testLiveIdleConnectionIsKept() throws Exception
    {
        PersistentConnection conn = (PersistentConnection)SqlConnectionPool.getPersistentConnection( newUrl(), "u", "p" );
        conn.validateIfIdle( -1, 5 );
        assertEquals( 1, opened.size() );
    }

    /** A statement still open (e.g. a streaming result set) must not be broken by a validation ping */
    public void testConnectionWithOpenStatementIsNotValidated() throws Exception
    {
        PersistentConnection conn = (PersistentConnection)SqlConnectionPool.getPersistentConnection( newUrl(), "u", "p" );
        Statement st = conn.createStatement();
        opened.get( 0 ).dead = true;
        conn.validateIfIdle( -1, 5 );
        assertEquals( 1, opened.size() );
        assertEquals( 0, opened.get( 0 ).validations );
        st.close();
    }

    public void testRecentlyUsedConnectionIsNotValidated() throws Exception
    {
        PersistentConnection conn = (PersistentConnection)SqlConnectionPool.getPersistentConnection( newUrl(), "u", "p" );
        conn.validateIfIdle( 60_000, 5 );
        assertEquals( 0, opened.get( 0 ).validations );
    }

    /** Errors unrelated to the connection are still thrown as before, without reconnecting */
    public void testQueryErrorDoesNotReconnect() throws Exception
    {
        Connection conn = SqlConnectionPool.getPersistentConnection( newUrl(), "u", "p" );
        opened.get( 0 ).syntaxError = true;
        try
        {
            SqlUtil.checkConnection( conn );
            fail( "syntax error expected" );
        }
        catch( Exception e )
        {
            // expected
        }
        assertEquals( 1, opened.size() );
    }

    static class FakeConnection implements InvocationHandler
    {
        /** the driver marked this connection closed: every call fails with 08003 */
        boolean closedByDriver;
        /** the server dropped the socket: isValid() is false */
        boolean dead;
        boolean syntaxError;
        int executed, validations;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
        {
            switch( method.getName() )
            {
                case "isValid":
                    validations++;
                    return !dead && !closedByDriver;
                case "isClosed":
                    return closedByDriver;
                case "close":
                    closedByDriver = true;
                    return null;
                case "getHoldability":
                    return ResultSet.HOLD_CURSORS_OVER_COMMIT;
                case "createStatement":
                    checkOpen();
                    return Proxy.newProxyInstance( getClass().getClassLoader(), new Class<?>[] {Statement.class},
                            (p, m, a) -> statement( m ) );
                case "toString":
                    return "FakeConnection";
                case "hashCode":
                    return System.identityHashCode( proxy );
                case "equals":
                    return proxy == args[0];
                default:
                    return defaultValue( method.getReturnType() );
            }
        }

        private Object statement(Method m) throws SQLException
        {
            switch( m.getName() )
            {
                case "execute":
                    checkOpen();
                    if( syntaxError )
                        throw new SQLSyntaxErrorException( "You have an error in your SQL syntax", "42000" );
                    executed++;
                    return true;
                case "toString":
                    return "FakeStatement";
                default:
                    return defaultValue( m.getReturnType() );
            }
        }

        private void checkOpen() throws SQLException
        {
            if( closedByDriver )
                throw new SQLNonTransientConnectionException( "No operations allowed after connection closed.", "08003" );
        }

        private static Object defaultValue(Class<?> type)
        {
            if( type == boolean.class )
                return false;
            if( type == int.class || type == long.class || type == short.class || type == byte.class )
                return 0;
            return null;
        }
    }

    static class FakeDriver implements Driver
    {
        @Override
        public Connection connect(String url, Properties info)
        {
            if( !acceptsURL( url ) )
                return null;
            FakeConnection handler = new FakeConnection();
            opened.add( handler );
            return (Connection)Proxy.newProxyInstance( FakeDriver.class.getClassLoader(), new Class<?>[] {Connection.class}, handler );
        }

        @Override
        public boolean acceptsURL(String url)
        {
            return url != null && url.startsWith( URL_PREFIX );
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info)
        {
            return new DriverPropertyInfo[0];
        }

        @Override
        public int getMajorVersion()
        {
            return 1;
        }

        @Override
        public int getMinorVersion()
        {
            return 0;
        }

        @Override
        public boolean jdbcCompliant()
        {
            return false;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException
        {
            throw new SQLFeatureNotSupportedException();
        }
    }
}
