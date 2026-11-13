package ru.biosoft.access._test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLSyntaxErrorException;
import java.sql.SQLTransientConnectionException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
    static final List<FakeConnection> opened = Collections.synchronizedList( new ArrayList<>() );
    /** delay of every connect, to widen race windows in the concurrency tests */
    static volatile long connectDelayMillis;
    /** the server cannot be reached: connecting fails */
    static volatile boolean failConnect;
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
        connectDelayMillis = 0;
        failConnect = false;
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

    /** A connection failure the driver reports from a query (not via isValid), with SQLState class 08 */
    public void testConnectionStateErrorFromQueryReconnectsAndRetries() throws Exception
    {
        checkQueryFailureReconnects( new SQLTransientConnectionException( "Communications link failure", "08S01" ) );
    }

    /** Same, when the driver throws a plain SQLException that only the SQLState identifies */
    public void testPlainSqlExceptionWithConnectionStateReconnects() throws Exception
    {
        checkQueryFailureReconnects( new SQLException( "Communications link failure", "08S01" ) );
    }

    private void checkQueryFailureReconnects(SQLException failure) throws Exception
    {
        Connection conn = SqlConnectionPool.getPersistentConnection( newUrl(), "u", "p" );
        opened.get( 0 ).executeFailure = failure;

        SqlUtil.checkConnection( conn );

        assertEquals( 2, opened.size() );
        assertEquals( 0, opened.get( 0 ).executed );
        assertEquals( "the query is retried on the new connection", 1, opened.get( 1 ).executed );
    }

    /** isValid() throwing is logged and treated as a dead connection */
    public void testValidationErrorReconnects() throws Exception
    {
        PersistentConnection conn = (PersistentConnection)SqlConnectionPool.getPersistentConnection( newUrl(), "u", "p" );
        opened.get( 0 ).validationFailure = new SQLException( "ping failed", "08S01" );
        conn.validateIfIdle( -1, 5 );
        assertEquals( 2, opened.size() );
    }

    /** A driver without isValid() keeps its connection instead of reconnecting on every checkout */
    public void testValidationNotSupportedKeepsConnection() throws Exception
    {
        PersistentConnection conn = (PersistentConnection)SqlConnectionPool.getPersistentConnection( newUrl(), "u", "p" );
        opened.get( 0 ).validationFailure = new SQLFeatureNotSupportedException( "isValid" );
        conn.validateIfIdle( -1, 5 );
        assertEquals( 1, opened.size() );
    }

    /**
     * A statement created before the connection was replaced (by another statement's reconnect or by idle
     * validation on another thread) moves to the new connection instead of reconnecting a second time,
     * which would close the fresh connection under the other statements.
     */
    public void testStatementOnReplacedConnectionRebinds() throws Exception
    {
        PersistentConnection conn = (PersistentConnection)SqlConnectionPool.getPersistentConnection( newUrl(), "u", "p" );
        Statement st = conn.createStatement();
        conn.reconnect( null ); // closes connection 0, opens connection 1

        st.execute( "SELECT 1" );

        assertEquals( "no second reconnect", 2, opened.size() );
        assertFalse( opened.get( 1 ).closedByDriver );
        assertEquals( 1, opened.get( 1 ).executed );
        st.close();
    }

    /**
     * Several threads share one connection (callers keep Connection objects in fields) and their statements fail
     * together on the dropped connection: exactly one replacement connection is opened, and every statement
     * retries on it. Each thread waiting for the reconnect must see the connection was already replaced.
     */
    public void testConcurrentFailuresReconnectOnce() throws Exception
    {
        int threads = 4;
        Connection conn = SqlConnectionPool.getPersistentConnection( newUrl(), "u", "p" );
        List<Statement> statements = new ArrayList<>();
        for( int i = 0; i < threads; i++ )
            statements.add( conn.createStatement() );
        FakeConnection first = opened.get( 0 );
        first.closedByDriver = true;
        first.executeBarrier = new CyclicBarrier( threads ); // all fail at the same moment
        connectDelayMillis = 200; // the others wait while the first reconnect is in progress

        List<Throwable> errors = runConcurrently( threads, i -> statements.get( i ).execute( "SELECT 1" ) );

        assertEquals( "[]", errors.toString() );
        assertEquals( "one replacement connection", 2, opened.size() );
        assertFalse( opened.get( 1 ).closedByDriver );
        assertEquals( threads, opened.get( 1 ).executedCount.get() );
    }

    /**
     * While idle validation pings (and then replaces) the connection, another thread creating a statement on it
     * waits, and gets its statement on the new connection: nothing runs on the connection being replaced.
     */
    public void testStatementCreationWaitsForValidation() throws Exception
    {
        PersistentConnection conn = (PersistentConnection)SqlConnectionPool.getPersistentConnection( newUrl(), "u", "p" );
        FakeConnection first = opened.get( 0 );
        first.dead = true;
        first.pingStarted = new CountDownLatch( 1 );
        first.pingRelease = new CountDownLatch( 1 );
        CountDownLatch statementDone = new CountDownLatch( 1 );

        Thread validation = new Thread( () -> {
            try
            {
                conn.validateIfIdle( -1, 5 );
            }
            catch( SQLException e )
            {
                throw new RuntimeException( e );
            }
        } );
        validation.start();
        assertTrue( first.pingStarted.await( 10, TimeUnit.SECONDS ) );
        List<Throwable> errors = Collections.synchronizedList( new ArrayList<>() );
        Thread user = new Thread( () -> {
            try (Statement st = conn.createStatement())
            {
                st.execute( "SELECT 1" );
            }
            catch( Throwable t )
            {
                errors.add( t );
            }
            statementDone.countDown();
        } );
        user.start();
        assertFalse( "statement creation must wait for the validation", statementDone.await( 300, TimeUnit.MILLISECONDS ) );
        first.pingRelease.countDown();
        validation.join( 10_000 );
        user.join( 10_000 );

        assertEquals( "[]", errors.toString() );
        assertEquals( 2, opened.size() );
        assertEquals( "nothing created on the connection being replaced", 0, first.statementsCreated.get() );
        assertEquals( 0, first.executedCount.get() );
        assertEquals( 1, opened.get( 1 ).executedCount.get() );
    }

    /** A prepared statement on a replaced connection is prepared again on the latest connection */
    public void testPreparedStatementOnReplacedConnectionRebinds() throws Exception
    {
        PersistentConnection conn = (PersistentConnection)SqlConnectionPool.getPersistentConnection( newUrl(), "u", "p" );
        PreparedStatement ps = conn.prepareStatement( "SELECT 1" );
        conn.reconnect( null );

        ps.execute( "SELECT 1" );

        assertEquals( "no second reconnect", 2, opened.size() );
        assertEquals( 1, opened.get( 1 ).statementsCreated.get() );
        assertEquals( 1, opened.get( 1 ).executedCount.get() );
        ps.close();
    }

    @FunctionalInterface
    interface StatementCreator
    {
        Statement create(Connection connection) throws SQLException;
    }

    /**
     * Every statement the connection hands out takes part in the lifecycle, whichever overload created it
     * (generated keys / columns, callable): while it is open, idle validation does not ping or replace the
     * connection; once it is closed, validation runs again.
     */
    public void testOpenStatementOfEveryOverloadPreventsValidation() throws Exception
    {
        String sql = "SELECT 1";
        List<StatementCreator> creators = Arrays.asList( c -> c.createStatement(),
                c -> c.createStatement( ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY ),
                c -> c.createStatement( ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT ),
                c -> c.prepareStatement( sql ),
                c -> c.prepareStatement( sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY ),
                c -> c.prepareStatement( sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT ),
                c -> c.prepareStatement( sql, Statement.RETURN_GENERATED_KEYS ),
                c -> c.prepareStatement( sql, new int[] {1} ),
                c -> c.prepareStatement( sql, new String[] {"id"} ),
                c -> c.prepareCall( sql ),
                c -> c.prepareCall( sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY ),
                c -> c.prepareCall( sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT ) );
        for( int i = 0; i < creators.size(); i++ )
        {
            opened.clear();
            PersistentConnection conn = (PersistentConnection)SqlConnectionPool.getPersistentConnection( newUrl(), "u", "p" );
            Statement st = creators.get( i ).create( conn );
            String what = "overload #" + i + " (" + opened.get( 0 ).lastCreateSignature + ")";
            opened.get( 0 ).dead = true;

            conn.validateIfIdle( -1, 5 );
            assertEquals( what + ": not validated while open", 0, opened.get( 0 ).validations );
            assertEquals( what, 1, opened.size() );
            assertSame( what, conn, st.getConnection() );

            st.close();
            conn.validateIfIdle( -1, 5 );
            assertEquals( what + ": validated once closed", 2, opened.size() );
        }
    }

    /**
     * A generated-keys statement on a replaced connection is prepared again with the same overload.
     * Re-preparation is reached only through the Statement methods taking SQL text (execute(String) and the
     * like), which PersistentStatement retries; JDBC does not allow them on a PreparedStatement, and the real
     * prepared execution (execute(), executeQuery(), ...) never retries, see testPreparedExecutionIsNotRetried.
     */
    public void testGeneratedKeysStatementRebindsWithSameOverload() throws Exception
    {
        PersistentConnection conn = (PersistentConnection)SqlConnectionPool.getPersistentConnection( newUrl(), "u", "p" );
        PreparedStatement ps = conn.prepareStatement( "INSERT INTO t VALUES(1)", Statement.RETURN_GENERATED_KEYS );
        conn.reconnect( null );

        ps.execute( "SELECT 1" );

        assertEquals( 2, opened.size() );
        assertEquals( "prepareStatement[class java.lang.String, int]", opened.get( 1 ).lastCreateSignature );
        assertEquals( 1, opened.get( 1 ).executedCount.get() );
        ps.close();
    }

    /** The replacement connection itself fails while a statement is being created on it: reconnect again and retry */
    public void testStatementCreationFailsOnReplacementConnection() throws Exception
    {
        PersistentConnection conn = (PersistentConnection)SqlConnectionPool.getPersistentConnection( newUrl(), "u", "p" );
        conn.reconnect( null );
        opened.get( 1 ).closedByDriver = true;

        try (Statement st = conn.createStatement())
        {
            st.execute( "SELECT 1" );
        }

        assertEquals( 3, opened.size() );
        assertEquals( 1, opened.get( 2 ).executedCount.get() );
    }

    /**
     * Statement creation fails because the server cannot be reached at all: the failure propagates, and the
     * connection is not left in a broken state, the next call reconnects once the server is back.
     */
    public void testFailedReconnectPropagatesAndRecoversLater() throws Exception
    {
        Connection conn = SqlConnectionPool.getPersistentConnection( newUrl(), "u", "p" );
        opened.get( 0 ).closedByDriver = true;
        failConnect = true;
        try
        {
            conn.createStatement();
            fail( "connection failure expected" );
        }
        catch( SQLException e )
        {
            assertEquals( "08001", e.getSQLState() );
        }
        assertEquals( 1, opened.size() );

        failConnect = false;
        try (Statement st = conn.createStatement())
        {
            st.execute( "SELECT 1" );
        }
        assertEquals( 2, opened.size() );
        assertEquals( 1, opened.get( 1 ).executedCount.get() );
    }

    /**
     * Prepared execution is not retried after a connection failure (the statement's effect on the server is
     * unknown, and its parameters would have to be replayed): the error reaches the caller, nothing runs twice,
     * and the next statement from the connection works on the replacement connection.
     */
    public void testPreparedExecutionIsNotRetried() throws Exception
    {
        PersistentConnection conn = (PersistentConnection)SqlConnectionPool.getPersistentConnection( newUrl(), "u", "p" );
        PreparedStatement ps = conn.prepareStatement( "INSERT INTO t VALUES(?)" );
        ps.setInt( 1, 42 );
        conn.reconnect( null ); // e.g. another thread's statement failed and replaced the connection
        try
        {
            ps.executeUpdate();
            fail( "connection failure expected" );
        }
        catch( SQLException e )
        {
            assertEquals( "08003", e.getSQLState() );
        }
        assertEquals( 2, opened.size() );
        assertEquals( "not re-run on the new connection", 0, opened.get( 1 ).executedCount.get() );
        ps.close();

        try (PreparedStatement again = conn.prepareStatement( "INSERT INTO t VALUES(?)" ))
        {
            again.setInt( 1, 42 );
            assertEquals( 1, again.executeUpdate() );
        }
        assertEquals( 2, opened.size() );
        assertEquals( 1, opened.get( 1 ).executedCount.get() );
    }

    /**
     * A statement stays registered until its JDBC close() has returned: idle validation running meanwhile (here,
     * from inside that close) must still see it as open and leave the connection alone.
     */
    public void testStatementStaysRegisteredUntilClosed() throws Exception
    {
        String sql = "SELECT 1";
        List<StatementCreator> creators = Arrays.asList( c -> c.createStatement(), c -> c.prepareStatement( sql ),
                c -> c.prepareStatement( sql, Statement.RETURN_GENERATED_KEYS ), c -> c.prepareCall( sql ) );
        for( int i = 0; i < creators.size(); i++ )
        {
            opened.clear();
            PersistentConnection conn = (PersistentConnection)SqlConnectionPool.getPersistentConnection( newUrl(), "u", "p" );
            Statement st = creators.get( i ).create( conn );
            FakeConnection first = opened.get( 0 );
            String what = first.lastCreateSignature;
            first.dead = true;
            List<Throwable> errors = new ArrayList<>();
            first.onStatementClose = () -> {
                try
                {
                    conn.validateIfIdle( -1, 5 );
                }
                catch( Throwable t )
                {
                    errors.add( t );
                }
            };

            st.close();

            assertEquals( what, "[]", errors.toString() );
            assertEquals( what + ": validated while its JDBC close() was running", 0, first.validations );
            first.onStatementClose = null;
            conn.validateIfIdle( -1, 5 );
            assertEquals( what + ": validated once closed", 2, opened.size() );
        }
    }

    /** The column selection of a generated-columns statement is copied: later changes by the caller do not leak into re-preparation */
    public void testGeneratedColumnsAreCopied() throws Exception
    {
        PersistentConnection conn = (PersistentConnection)SqlConnectionPool.getPersistentConnection( newUrl(), "u", "p" );
        int[] indexes = {1};
        String[] names = {"id"};
        PreparedStatement byIndex = conn.prepareStatement( "INSERT INTO a VALUES(1)", indexes );
        PreparedStatement byName = conn.prepareStatement( "INSERT INTO b VALUES(1)", names );
        indexes[0] = 2;
        names[0] = "other";
        conn.reconnect( null );

        byIndex.execute( "SELECT 1" ); // re-prepares on the new connection (see testGeneratedKeysStatementRebindsWithSameOverload)
        assertEquals( "[INSERT INTO a VALUES(1), [1]]", opened.get( 1 ).lastCreateArgs );
        byName.execute( "SELECT 1" );
        assertEquals( "[INSERT INTO b VALUES(1), [id]]", opened.get( 1 ).lastCreateArgs );
        byIndex.close();
        byName.close();
    }

    @FunctionalInterface
    interface IndexedTask
    {
        void run(int i) throws Exception;
    }

    private static List<Throwable> runConcurrently(int threads, IndexedTask task) throws InterruptedException
    {
        List<Throwable> errors = Collections.synchronizedList( new ArrayList<>() );
        List<Thread> list = new ArrayList<>();
        for( int i = 0; i < threads; i++ )
        {
            int index = i;
            Thread t = new Thread( () -> {
                try
                {
                    task.run( index );
                }
                catch( Throwable e )
                {
                    errors.add( e );
                }
            } );
            list.add( t );
            t.start();
        }
        for( Thread t : list )
            t.join( 30_000 );
        return errors;
    }

    static class FakeConnection implements InvocationHandler
    {
        /** the driver marked this connection closed: every call fails with 08003 */
        volatile boolean closedByDriver;
        /** the server dropped the socket: isValid() is false */
        volatile boolean dead;
        volatile boolean syntaxError;
        /** thrown once by the next query */
        volatile SQLException executeFailure;
        /** thrown by isValid() */
        volatile SQLException validationFailure;
        /** if set, every query waits here first, so that concurrent queries fail together */
        volatile CyclicBarrier executeBarrier;
        /** if set, isValid() counts down pingStarted and waits for pingRelease */
        volatile CountDownLatch pingStarted, pingRelease;
        final AtomicInteger executedCount = new AtomicInteger(), statementsCreated = new AtomicInteger();
        volatile int validations;
        /** e.g. "prepareStatement[class java.lang.String, int]" */
        volatile String lastCreateSignature;
        /** e.g. "[INSERT ..., [1]]" */
        volatile String lastCreateArgs;
        /** runs inside the JDBC close() of every statement of this connection, before it returns */
        volatile Runnable onStatementClose;
        int executed;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
        {
            switch( method.getName() )
            {
                case "isValid":
                    validations++;
                    if( pingStarted != null )
                    {
                        pingStarted.countDown();
                        pingRelease.await( 10, TimeUnit.SECONDS );
                    }
                    if( validationFailure != null )
                        throw validationFailure;
                    return !dead && !closedByDriver;
                case "isClosed":
                    return closedByDriver;
                case "close":
                    closedByDriver = true;
                    return null;
                case "getHoldability":
                    return ResultSet.HOLD_CURSORS_OVER_COMMIT;
                case "createStatement":
                case "prepareStatement":
                case "prepareCall":
                    checkOpen();
                    statementsCreated.incrementAndGet();
                    lastCreateSignature = method.getName() + Arrays.toString( method.getParameterTypes() );
                    lastCreateArgs = Arrays.deepToString( args );
                    Class<?> type = method.getName().equals( "createStatement" ) ? Statement.class
                            : method.getName().equals( "prepareCall" ) ? CallableStatement.class : PreparedStatement.class;
                    return Proxy.newProxyInstance( getClass().getClassLoader(), new Class<?>[] {type}, (p, m, a) -> statement( m ) );
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

        private Object statement(Method m) throws Exception
        {
            switch( m.getName() )
            {
                case "close":
                    Runnable hook = onStatementClose;
                    if( hook != null )
                        hook.run();
                    return null;
                case "execute":
                case "executeQuery":
                case "executeUpdate":
                    CyclicBarrier barrier = executeBarrier;
                    if( barrier != null )
                        barrier.await( 10, TimeUnit.SECONDS );
                    checkOpen();
                    if( syntaxError )
                        throw new SQLSyntaxErrorException( "You have an error in your SQL syntax", "42000" );
                    if( executeFailure != null )
                    {
                        SQLException e = executeFailure;
                        executeFailure = null;
                        throw e;
                    }
                    executed = executedCount.incrementAndGet();
                    return m.getReturnType() == boolean.class ? (Object)true : m.getReturnType() == int.class ? (Object)1 : null;
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
        public Connection connect(String url, Properties info) throws SQLException
        {
            if( !acceptsURL( url ) )
                return null;
            if( failConnect )
                throw new SQLException( "Connection refused", "08001" );
            try
            {
                Thread.sleep( connectDelayMillis );
            }
            catch( InterruptedException e )
            {
                throw new SQLException( e );
            }
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
