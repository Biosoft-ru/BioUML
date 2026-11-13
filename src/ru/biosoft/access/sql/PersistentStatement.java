package ru.biosoft.access.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

public class PersistentStatement implements Statement
{
    protected PersistentConnection conn;
    protected Connection originalConnection;
    protected Statement parent;
    protected int type;
    protected int concurrency;
    protected int holdability;
    protected String lastQuery; // for debug purposes

    /**
     * Reconnects after ex, unless the connection was already replaced since this statement was bound to it (by
     * another statement or by idle validation): then this statement only moves to the new connection instead of
     * closing the fresh one again. Checking, reconnecting and recreating the statement are atomic
     * (PersistentConnection.reconnectIfCurrent).
     */
    void reconnect(SQLException ex) throws SQLException
    {
        parent = conn.reconnectIfCurrent(originalConnection, ex, c -> {
            originalConnection = c;
            return c.createStatement(type, concurrency, holdability);
        });
    }
    
    PersistentStatement()
    {
    }
    
    /** @param original the wrapped connection to create it on; called under the connection's replacement lock */
    PersistentStatement(PersistentConnection conn, Connection original, int type, int concurrency, int holdability) throws SQLException
    {
        this.conn = conn;
        this.type = type;
        this.concurrency = concurrency;
        this.holdability = holdability;
        this.originalConnection = original;
        parent = originalConnection.createStatement(type, concurrency, holdability);
        this.conn.addStatement(this);
    }
    
    @Override
    public void addBatch(String arg0) throws SQLException
    {
        conn.touch();
        parent.addBatch(arg0);
    }

    @Override
    public void cancel() throws SQLException
    {
        conn.touch();
        parent.cancel();
    }

    @Override
    public void clearBatch() throws SQLException
    {
        conn.touch();
        parent.clearBatch();
    }

    @Override
    public void clearWarnings() throws SQLException
    {
        conn.touch();
        parent.clearWarnings();
    }

    @Override
    public void close() throws SQLException
    {
        conn.touch();
        this.conn.releaseStatement(this);
        parent.close();
    }

    @Override
    public boolean execute(String arg0, int arg1) throws SQLException
    {
        conn.touch();
        lastQuery = arg0;
        try
        {
            return parent.execute(arg0, arg1);
        }
        catch (SQLException e)
        {
            reconnect(e);
            return parent.execute(arg0, arg1);
        }
    }

    @Override
    public boolean execute(String arg0, int[] arg1) throws SQLException
    {
        conn.touch();
        lastQuery = arg0;
        try
        {
            return parent.execute(arg0, arg1);
        }
        catch (SQLException e)
        {
            reconnect(e);
            return parent.execute(arg0, arg1);
        }
    }

    @Override
    public boolean execute(String arg0, String[] arg1) throws SQLException
    {
        conn.touch();
        lastQuery = arg0;
        try
        {
            return parent.execute(arg0, arg1);
        }
        catch (SQLException e)
        {
            reconnect(e);
            return parent.execute(arg0, arg1);
        }
    }

    @Override
    public boolean execute(String arg0) throws SQLException
    {
        conn.touch();
        lastQuery = arg0;
        try
        {
            return parent.execute(arg0);
        }
        catch (SQLException e)
        {
            reconnect(e);
            return parent.execute(arg0);
        }
    }

    @Override
    public int[] executeBatch() throws SQLException
    {
        conn.touch();
        return parent.executeBatch();
    }

    @Override
    public ResultSet executeQuery(String arg0) throws SQLException
    {
        conn.touch();
        lastQuery = arg0;
        try
        {
            return parent.executeQuery(arg0);
        }
        catch (SQLException e)
        {
            reconnect(e);
            return parent.executeQuery(arg0);
        }
    }

    @Override
    public int executeUpdate(String arg0, int arg1) throws SQLException
    {
        conn.touch();
        lastQuery = arg0;
        try
        {
            return parent.executeUpdate(arg0, arg1);
        }
        catch (SQLException e)
        {
            reconnect(e);
            return parent.executeUpdate(arg0, arg1);
        }
    }

    @Override
    public int executeUpdate(String arg0, int[] arg1) throws SQLException
    {
        conn.touch();
        lastQuery = arg0;
        try
        {
            return parent.executeUpdate(arg0, arg1);
        }
        catch (SQLException e)
        {
            reconnect(e);
            return parent.executeUpdate(arg0, arg1);
        }
    }

    @Override
    public int executeUpdate(String arg0, String[] arg1) throws SQLException
    {
        conn.touch();
        lastQuery = arg0;
        try
        {
            return parent.executeUpdate(arg0, arg1);
        }
        catch (SQLException e)
        {
            reconnect(e);
            return parent.executeUpdate(arg0, arg1);
        }
    }

    @Override
    public int executeUpdate(String arg0) throws SQLException
    {
        conn.touch();
        lastQuery = arg0;
        try
        {
            return parent.executeUpdate(arg0);
        }
        catch (SQLException e)
        {
            reconnect(e);
            return parent.executeUpdate(arg0);
        }
    }

    @Override
    public Connection getConnection() throws SQLException
    {
        return conn;
    }

    @Override
    public int getFetchDirection() throws SQLException
    {
        conn.touch();
        return parent.getFetchDirection();
    }

    @Override
    public int getFetchSize() throws SQLException
    {
        conn.touch();
        return parent.getFetchSize();
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException
    {
        conn.touch();
        return parent.getGeneratedKeys();
    }

    @Override
    public int getMaxFieldSize() throws SQLException
    {
        conn.touch();
        return parent.getMaxFieldSize();
    }

    @Override
    public int getMaxRows() throws SQLException
    {
        conn.touch();
        return parent.getMaxRows();
    }

    @Override
    public boolean getMoreResults() throws SQLException
    {
        conn.touch();
        return parent.getMoreResults();
    }

    @Override
    public boolean getMoreResults(int arg0) throws SQLException
    {
        conn.touch();
        return parent.getMoreResults(arg0);
    }

    @Override
    public int getQueryTimeout() throws SQLException
    {
        conn.touch();
        return parent.getQueryTimeout();
    }

    @Override
    public ResultSet getResultSet() throws SQLException
    {
        conn.touch();
        return parent.getResultSet();
    }

    @Override
    public int getResultSetConcurrency() throws SQLException
    {
        conn.touch();
        return parent.getResultSetConcurrency();
    }

    @Override
    public int getResultSetHoldability() throws SQLException
    {
        conn.touch();
        return parent.getResultSetHoldability();
    }

    @Override
    public int getResultSetType() throws SQLException
    {
        conn.touch();
        return parent.getResultSetType();
    }

    @Override
    public int getUpdateCount() throws SQLException
    {
        conn.touch();
        return parent.getUpdateCount();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException
    {
        conn.touch();
        return parent.getWarnings();
    }
    
    public boolean isValid()
    {
        conn.touch();
        return conn.getOriginalConnection() == originalConnection;
    }

    @Override
    public boolean isClosed() throws SQLException
    {
        conn.touch();
        return parent.isClosed();
    }

    @Override
    public boolean isPoolable() throws SQLException
    {
        conn.touch();
        return parent.isPoolable();
    }

    @Override
    public boolean isWrapperFor(Class<?> arg0) throws SQLException
    {
        conn.touch();
        return parent.isWrapperFor(arg0);
    }

    @Override
    public void setCursorName(String arg0) throws SQLException
    {
        conn.touch();
        parent.setCursorName(arg0);
    }

    @Override
    public void setEscapeProcessing(boolean arg0) throws SQLException
    {
        conn.touch();
        parent.setEscapeProcessing(arg0);
    }

    @Override
    public void setFetchDirection(int arg0) throws SQLException
    {
        conn.touch();
        parent.setFetchDirection(arg0);
    }

    @Override
    public void setFetchSize(int arg0) throws SQLException
    {
        conn.touch();
        parent.setFetchSize(arg0);
    }

    @Override
    public void setMaxFieldSize(int arg0) throws SQLException
    {
        conn.touch();
        parent.setMaxFieldSize(arg0);
    }

    @Override
    public void setMaxRows(int arg0) throws SQLException
    {
        conn.touch();
        parent.setMaxRows(arg0);
    }

    @Override
    public void setPoolable(boolean arg0) throws SQLException
    {
        conn.touch();
        parent.setPoolable(arg0);
    }

    @Override
    public void setQueryTimeout(int arg0) throws SQLException
    {
        conn.touch();
        parent.setQueryTimeout(arg0);
    }

    @Override
    public <T> T unwrap(Class<T> arg0) throws SQLException
    {
        conn.touch();
        return parent.unwrap(arg0);
    }

    @Override
    public String toString()
    {
        return parent.toString();
    }

    //compatible with java 1.7
    @Override
    public boolean isCloseOnCompletion()
    {
        return false;
    }
    @Override
    public void closeOnCompletion()
    {
    }
}
