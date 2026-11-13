package ru.biosoft.access.sql;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLRecoverableException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Wrapper for SQL Connection which tries to keep connection alive
 * @author lan
 */
public class PersistentConnection implements Connection
{
    private static Logger cat = Logger.getLogger(PersistentConnection.class.getName());
    private volatile Connection parent;
    private volatile long lastActivity;
    private volatile boolean closedByTimeout;
    /**
     * Guards every replacement of {@link #parent} (reconnect, idle close and reopen, idle validation) and every
     * creation of a statement on it, so that a statement is always created on the current connection and
     * concurrent failures of statements on the same connection replace it once.
     * Lock order: replacementLock before {@link #statements}, never the reverse. Statements register only from
     * their constructors, which run under replacementLock (see {@link #withRetry}).
     */
    private final Object replacementLock = new Object();
    private String url, user, password;
    private Collection<Statement> statements = new ArrayList<>();
    private static String CONNECTION_RESET = "Connection reset";

    public Connection cloneConnection() throws SQLException
    {
        return DriverManager.getConnection(url, user, password);
    }
    
    protected void addStatement(Statement st)
    {
        synchronized(statements)
        {
            statements.add(st);
        }
    }
    
    protected void releaseStatement(Statement st)
    {
        synchronized(statements)
        {
            statements.remove(st);
        }
    }

    public void reconnect(SQLException ex) throws SQLException
    {
        synchronized( replacementLock )
        {
            doReconnect( ex );
        }
    }

    private void doReconnect(SQLException ex) throws SQLException
    {
        if( ex != null )
        {
            checkReconnect(ex);

            cat.log(Level.WARNING, "Reconnecting " + url + " (user:" + user + ")...");
            cat.log(Level.FINE, ex.getMessage(), ex);
        }
        else
            cat.log(Level.WARNING, "Reconnecting " + url + " (user:" + user + ")...");
        try
        {
            parent.close();
        }
        catch( SQLException e )
        {
        }
        try
        {
            parent = SqlConnectionPool.getNewConnection(url, user, password);
        }
        catch( SQLException e )
        {
            cat.log(Level.SEVERE, "Retrying SQL query failed", e);
            throw e;
        }
        cat.info("Reconnected successfully!");
    }

    @FunctionalInterface
    interface ConnectionFunction<T>
    {
        T apply(Connection connection) throws SQLException;
    }

    /**
     * Runs function on the current wrapped connection; if it fails with a connection error, reconnects and runs it
     * once more. Only for creating statements: never pass the execution of SQL, which may already have taken effect
     * on the server when the connection failed (a retried INSERT could run twice). All of it happens under {@link #replacementLock}: another thread failing at the same time waits and
     * then simply uses the new connection instead of replacing it a second time.
     */
    <T> T withRetry(ConnectionFunction<T> function) throws SQLException
    {
        synchronized( replacementLock )
        {
            try
            {
                return function.apply( parent );
            }
            catch( SQLException e )
            {
                doReconnect( e );
                return function.apply( parent );
            }
        }
    }

    /**
     * For a statement bound to expected that failed with ex: reconnects unless the connection was already replaced
     * since then (by another statement or by validation), then runs function on the current connection to create the
     * statement's replacement. The check, the reconnect and the creation are one critical section, so the new
     * statement cannot end up on a connection that is being replaced.
     */
    <T> T reconnectIfCurrent(Connection expected, SQLException ex, ConnectionFunction<T> function) throws SQLException
    {
        synchronized( replacementLock )
        {
            if( parent == expected )
                doReconnect( ex );
            return function.apply( parent );
        }
    }

    void reconnectIfCurrent(Connection expected, SQLException ex) throws SQLException
    {
        reconnectIfCurrent( expected, ex, c -> null );
    }
    
    /**
     * Update last activity time
     * @throws SQLException
     */
    public void touch()
    {
        if(closedByTimeout)
        {
            synchronized( replacementLock )
            {
                if( closedByTimeout )
                {
                    closedByTimeout = false;
                    try
                    {
                        parent = SqlConnectionPool.getNewConnection( url, user, password );
                    }
                    catch( Exception e )
                    {
                        cat.log( Level.SEVERE, "Unable to reinitialize the connection (url=" + url + "): " + e.getMessage() );
                    }
                }
            }
        }
        lastActivity = System.currentTimeMillis();
    }
    
    public void checkTimeout(long millis)
    {
        if( !closedByTimeout )
        {
            synchronized( replacementLock )
            {
                if( !closedByTimeout && noOpenStatements() && System.currentTimeMillis() - lastActivity > millis )
                {
                    try
                    {
                        parent.close();
                    }
                    catch( SQLException e )
                    {
                    }
                    closedByTimeout = true;
                }
            }
        }
    }

    private boolean noOpenStatements()
    {
        synchronized( statements )
        {
            return statements.isEmpty();
        }
    }

    PersistentConnection(Connection parent, String url, String user, String password)
    {
        this.parent = parent;
        this.url = url;
        this.user = user;
        this.password = password;
        touch();
    }

    protected Connection getOriginalConnection()
    {
        return this.parent;
    }

    @Override
    public void clearWarnings() throws SQLException
    {
        touch();
        parent.clearWarnings();
    }

    @Override
    public void close() throws SQLException
    {
        //cat.log( Level.SEVERE, "Persistent connection close", new Exception() );
        touch();
        synchronized(statements)
        {
            statements.clear();
        }
        parent.close();
    }

    @Override
    public void commit() throws SQLException
    {
        touch();
        parent.commit();
    }

    @Override
    public Array createArrayOf(String arg0, Object[] arg1) throws SQLException
    {
        touch();
        return parent.createArrayOf(arg0, arg1);
    }

    @Override
    public Blob createBlob() throws SQLException
    {
        touch();
        return parent.createBlob();
    }

    @Override
    public Clob createClob() throws SQLException
    {
        touch();
        return parent.createClob();
    }

    @Override
    public NClob createNClob() throws SQLException
    {
        touch();
        return parent.createNClob();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException
    {
        touch();
        return parent.createSQLXML();
    }

    @Override
    public Statement createStatement() throws SQLException
    {
        touch();
        return withRetry( c -> new PersistentStatement(this, c, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, getHoldability()) );
    }

    @Override
    public Statement createStatement(int arg0, int arg1, int arg2) throws SQLException
    {
        touch();
        return withRetry( c -> new PersistentStatement(this, c, arg0, arg1, arg2) );
    }

    @Override
    public Statement createStatement(int arg0, int arg1) throws SQLException
    {
        touch();
        return withRetry( c -> new PersistentStatement(this, c, arg0, arg1, getHoldability()) );
    }

    @Override
    public Struct createStruct(String arg0, Object[] arg1) throws SQLException
    {
        touch();
        return parent.createStruct(arg0, arg1);
    }

    @Override
    public boolean getAutoCommit() throws SQLException
    {
        touch();
        return parent.getAutoCommit();
    }

    @Override
    public String getCatalog() throws SQLException
    {
        touch();
        return parent.getCatalog();
    }

    @Override
    public Properties getClientInfo() throws SQLException
    {
        touch();
        return parent.getClientInfo();
    }

    @Override
    public String getClientInfo(String arg0) throws SQLException
    {
        touch();
        return parent.getClientInfo(arg0);
    }

    @Override
    public int getHoldability() throws SQLException
    {
        touch();
        return parent.getHoldability();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException
    {
        touch();
        return parent.getMetaData();
    }

    @Override
    public int getTransactionIsolation() throws SQLException
    {
        touch();
        return parent.getTransactionIsolation();
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException
    {
        touch();
        return parent.getTypeMap();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException
    {
        touch();
        return parent.getWarnings();
    }

    @Override
    public boolean isClosed() throws SQLException
    {
        touch();
        return parent.isClosed();
    }

    @Override
    public boolean isReadOnly() throws SQLException
    {
        touch();
        return parent.isReadOnly();
    }

    @Override
    public boolean isValid(int arg0) throws SQLException
    {
        touch();
        return parent.isValid(arg0);
    }

    @Override
    public boolean isWrapperFor(Class<?> arg0) throws SQLException
    {
        touch();
        return parent.isWrapperFor(arg0);
    }

    @Override
    public String nativeSQL(String arg0) throws SQLException
    {
        touch();
        return parent.nativeSQL(arg0);
    }

    @Override
    public CallableStatement prepareCall(String arg0, int arg1, int arg2, int arg3) throws SQLException
    {
        touch();
        return withRetry( c -> TrackedCallableStatement.wrap(this, c.prepareCall(arg0, arg1, arg2, arg3)) );
    }

    @Override
    public CallableStatement prepareCall(String arg0, int arg1, int arg2) throws SQLException
    {
        touch();
        return withRetry( c -> TrackedCallableStatement.wrap(this, c.prepareCall(arg0, arg1, arg2)) );
    }

    @Override
    public CallableStatement prepareCall(String arg0) throws SQLException
    {
        touch();
        return withRetry( c -> TrackedCallableStatement.wrap(this, c.prepareCall(arg0)) );
    }

    @Override
    public PreparedStatement prepareStatement(String arg0, int arg1, int arg2, int arg3) throws SQLException
    {
        touch();
        return withRetry( c -> new PersistentPreparedStatement(this, c, arg0, arg1, arg2, arg3) );
    }

    @Override
    public PreparedStatement prepareStatement(String arg0, int arg1, int arg2) throws SQLException
    {
        touch();
        return withRetry( c -> new PersistentPreparedStatement(this, c, arg0, arg1, arg2, getHoldability()) );
    }

    @Override
    public PreparedStatement prepareStatement(String arg0, int arg1) throws SQLException
    {
        touch();
        return withRetry( c -> new PersistentPreparedStatement(this, c, arg0, c2 -> c2.prepareStatement(arg0, arg1)) );
    }

    @Override
    public PreparedStatement prepareStatement(String arg0, int[] arg1) throws SQLException
    {
        touch();
        return withRetry( c -> new PersistentPreparedStatement(this, c, arg0, c2 -> c2.prepareStatement(arg0, arg1)) );
    }

    @Override
    public PreparedStatement prepareStatement(String arg0, String[] arg1) throws SQLException
    {
        touch();
        return withRetry( c -> new PersistentPreparedStatement(this, c, arg0, c2 -> c2.prepareStatement(arg0, arg1)) );
    }

    @Override
    public PreparedStatement prepareStatement(String arg0) throws SQLException
    {
        touch();
        return withRetry( c -> new PersistentPreparedStatement(this, c, arg0, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, getHoldability()) );
    }

    @Override
    public void releaseSavepoint(Savepoint arg0) throws SQLException
    {
        touch();
        parent.releaseSavepoint(arg0);
    }

    @Override
    public void rollback() throws SQLException
    {
        touch();
        Connection current = parent;
        try
        {
            current.rollback();
        }
        catch (SQLException e)
        {
            reconnectIfCurrent(current, e);
        }
    }

    @Override
    public void rollback(Savepoint arg0) throws SQLException
    {
        touch();
        parent.rollback(arg0);
    }

    @Override
    public void setAutoCommit(boolean arg0) throws SQLException
    {
        touch();
        parent.setAutoCommit(arg0);
    }

    @Override
    public void setCatalog(String arg0) throws SQLException
    {
        touch();
        parent.setCatalog(arg0);
    }

    @Override
    public void setClientInfo(Properties arg0) throws SQLClientInfoException
    {
        parent.setClientInfo(arg0);
    }

    @Override
    public void setClientInfo(String arg0, String arg1) throws SQLClientInfoException
    {
        parent.setClientInfo(arg0, arg1);
    }

    @Override
    public void setHoldability(int arg0) throws SQLException
    {
        touch();
        parent.setHoldability(arg0);
    }

    @Override
    public void setReadOnly(boolean arg0) throws SQLException
    {
        touch();
        parent.setReadOnly(arg0);
    }

    @Override
    public Savepoint setSavepoint() throws SQLException
    {
        touch();
        return parent.setSavepoint();
    }

    @Override
    public Savepoint setSavepoint(String arg0) throws SQLException
    {
        touch();
        return parent.setSavepoint(arg0);
    }

    @Override
    public void setTransactionIsolation(int arg0) throws SQLException
    {
        touch();
        parent.setTransactionIsolation(arg0);
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> arg0) throws SQLException
    {
        touch();
        parent.setTypeMap(arg0);
    }

    @Override
    public <T> T unwrap(Class<T> arg0) throws SQLException
    {
        touch();
        return parent.unwrap(arg0);
    }

    //
    //Compatible with java 1.7
    @Override
    public int getNetworkTimeout() throws SQLException
    {
        return 0;
    }
    
    @Override
    public void setNetworkTimeout(java.util.concurrent.Executor executor,int milliseconds)
    {
    }
    
    public boolean isCloseOnCompletion()
    {
        return false;
    }
    
    public void closeOnCompletion()
    {
    }
    
    @Override
    public void abort(java.util.concurrent.Executor executor)
    {
    }
    
    @Override
    public String getSchema()
    {
        return null;
    }
    
    @Override
    public void setSchema(String schema)
    {
    }

    @Override
    public String toString()
    {
        return "PersistentConnection: "+url;
    }
    
    public String getURL()
    {
        return url;
    }

    private void checkReconnect(SQLException ex) throws SQLException
    {
        if ( ex instanceof SQLRecoverableException )
            return;
        else if ( ex instanceof SQLException && ex.getMessage() != null && ex.getMessage().startsWith(CONNECTION_RESET) )
        {
            cat.log(Level.WARNING, "Trying to reconnect on 'Connection reset'", ex);
            return;
        }
        // The driver has already given up on the wrapped connection, e.g. MySQL's "No operations allowed after
        // connection closed." (SQLNonTransientConnectionException, SQLState 08003) once an earlier call, often a
        // prepared statement, which does not reconnect, hit a socket the server dropped after wait_timeout.
        // Without reconnecting here every later call on this thread's connection fails the same way for good.
        else if ( ex instanceof SQLNonTransientConnectionException || isConnectionState( ex.getSQLState() ) || isParentClosed() )
            return;
        else
            throw ex;
    }

    /** SQLState class 08 = connection exception */
    private static boolean isConnectionState(String sqlState)
    {
        return sqlState != null && sqlState.startsWith( "08" );
    }

    private boolean isParentClosed()
    {
        try
        {
            return parent.isClosed();
        }
        catch( SQLException e )
        {
            return true;
        }
    }

    /**
     * Replaces the wrapped connection if it has been idle for longer than idleMillis and no longer answers.
     * A connection kept by a long-lived worker thread can sit unused for hours (e.g. through a CMA run) while
     * the server drops it after wait_timeout; checking it once on checkout is cheaper than failing the analysis.
     * Skipped while statements are open: a result set may still be streaming over this connection
     * (reading rows does not update the activity time), and pinging or replacing it would break that.
     */
    public void validateIfIdle(long idleMillis, int timeoutSeconds) throws SQLException
    {
        if( !isIdle( idleMillis ) )
            return; // cheap check without the lock; repeated under it
        // Held through the ping and the reconnect: statements are only created and registered under this lock,
        // so with no open statement none can start (or still be streaming) while the connection is pinged or
        // replaced, even when the connection object is shared between threads (callers keep it in fields)
        synchronized( replacementLock )
        {
            // rechecked under the lock: another thread may have used the connection in the meantime
            if( !isIdle( idleMillis ) || !noOpenStatements() )
                return;
            if( !isAlive( timeoutSeconds ) )
                doReconnect( null );
        }
        touch();
    }

    private boolean isIdle(long idleMillis)
    {
        return !closedByTimeout && System.currentTimeMillis() - lastActivity > idleMillis;
    }

    private boolean isAlive(int timeoutSeconds)
    {
        try
        {
            if( parent.isValid( timeoutSeconds ) )
                return true;
            cat.log( Level.WARNING, "Idle connection " + url + " (user:" + user + ") did not answer within " + timeoutSeconds + " s" );
            return false;
        }
        catch( SQLFeatureNotSupportedException | AbstractMethodError e )
        {
            return true; // the driver cannot tell: keep the connection, as before validation existed
        }
        catch( SQLException e )
        {
            cat.log( Level.WARNING, "Validation of idle connection " + url + " (user:" + user + ") failed, reconnecting", e );
            return false;
        }
    }
}
