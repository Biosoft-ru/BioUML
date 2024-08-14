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
    private Connection parent;
    private volatile long lastActivity;
    private boolean closedByTimeout;
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

    public synchronized void reconnect(SQLException ex) throws SQLException
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
    
    /**
     * Update last activity time
     * @throws SQLException
     */
    private Object lock = new Object();
    public void touch()
    {
        if(closedByTimeout)
        {
            synchronized( lock )
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
            synchronized( lock )
            {

                if( !closedByTimeout && statements.isEmpty() && System.currentTimeMillis() - lastActivity > millis )
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
        try
        {
            return new PersistentStatement(this, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, getHoldability());
        }
        catch (SQLException e)
        {
            reconnect(e);
            return new PersistentStatement(this, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, getHoldability());
        }
    }

    @Override
    public Statement createStatement(int arg0, int arg1, int arg2) throws SQLException
    {
        touch();
        try
        {
            return new PersistentStatement(this, arg0, arg1, arg2);
        }
        catch (SQLException e)
        {
            reconnect(e);
            return new PersistentStatement(this, arg0, arg1, arg2);
        }
    }

    @Override
    public Statement createStatement(int arg0, int arg1) throws SQLException
    {
        touch();
        try
        {
            return new PersistentStatement(this, arg0, arg1, getHoldability());
        }
        catch (SQLException e)
        {
            reconnect(e);
            return new PersistentStatement(this, arg0, arg1, getHoldability());
        }
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
        try
        {
            return parent.prepareCall(arg0, arg1, arg2, arg3);
        }
        catch (SQLException e)
        {
            reconnect(e);
            return parent.prepareCall(arg0, arg1, arg2, arg3);
        }
    }

    @Override
    public CallableStatement prepareCall(String arg0, int arg1, int arg2) throws SQLException
    {
        touch();
        try
        {
            return parent.prepareCall(arg0, arg1, arg2);
        }
        catch (SQLException e)
        {
            reconnect(e);
            return parent.prepareCall(arg0, arg1, arg2);
        }
    }

    @Override
    public CallableStatement prepareCall(String arg0) throws SQLException
    {
        touch();
        try
        {
            return parent.prepareCall(arg0);
        }
        catch (SQLException e)
        {
            reconnect(e);
            return parent.prepareCall(arg0);
        }
    }

    @Override
    public PreparedStatement prepareStatement(String arg0, int arg1, int arg2, int arg3) throws SQLException
    {
        touch();
        try
        {
            return new PersistentPreparedStatement(this, arg0, arg1, arg2, arg3);
        }
        catch (SQLException e)
        {
            reconnect(e);
            return new PersistentPreparedStatement(this, arg0, arg1, arg2, arg3);
        }
    }

    @Override
    public PreparedStatement prepareStatement(String arg0, int arg1, int arg2) throws SQLException
    {
        touch();
        try
        {
            return new PersistentPreparedStatement(this, arg0, arg1, arg2, getHoldability());
        }
        catch (SQLException e)
        {
            reconnect(e);
            return new PersistentPreparedStatement(this, arg0, arg1, arg2, getHoldability());
        }
    }

    @Override
    public PreparedStatement prepareStatement(String arg0, int arg1) throws SQLException
    {
        touch();
        try
        {
            return parent.prepareStatement(arg0, arg1);
        }
        catch (SQLException e)
        {
            reconnect(e);
            return parent.prepareStatement(arg0, arg1);
        }
    }

    @Override
    public PreparedStatement prepareStatement(String arg0, int[] arg1) throws SQLException
    {
        touch();
        try
        {
            return parent.prepareStatement(arg0, arg1);
        }
        catch (SQLException e)
        {
            reconnect(e);
            return parent.prepareStatement(arg0, arg1);
        }
    }

    @Override
    public PreparedStatement prepareStatement(String arg0, String[] arg1) throws SQLException
    {
        touch();
        try
        {
            return parent.prepareStatement(arg0, arg1);
        }
        catch (SQLException e)
        {
            reconnect(e);
            return parent.prepareStatement(arg0, arg1);
        }
    }

    @Override
    public PreparedStatement prepareStatement(String arg0) throws SQLException
    {
        touch();
        try
        {
            return new PersistentPreparedStatement(this, arg0, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, getHoldability());
        }
        catch (SQLException e)
        {
            reconnect(e);
            return new PersistentPreparedStatement(this, arg0, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, getHoldability());
        }
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
        try
        {
            parent.rollback();
        }
        catch (SQLException e)
        {
            reconnect(e);
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
        else
            throw ex;
    }
}
