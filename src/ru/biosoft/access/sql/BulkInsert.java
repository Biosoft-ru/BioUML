package ru.biosoft.access.sql;

import java.sql.Connection;

import one.util.streamex.StreamEx;

import ru.biosoft.access.exception.BiosoftSQLException;

/**
 * Base class for different bulk inserters implementations
 * @author lan
 */
public abstract class BulkInsert implements SqlConnectionHolder
{
    protected Connection conn;
    protected String table;
    protected String[] fields;
    private SqlConnectionHolder holder;
    
    public BulkInsert(Connection conn, String table, String[] fields)
    {
        super();
        this.conn = conn;
        this.table = table;
        this.fields = fields;
    }
    
    public BulkInsert(SqlConnectionHolder holder, String table, String[] fields)
    {
        super();
        this.holder = holder;
        this.table = table;
        this.fields = fields;
    }
    
    @Override
    public Connection getConnection() throws BiosoftSQLException
    {
        return this.holder == null ? this.conn : this.holder.getConnection();
    }
    
    protected String[] getEscapedFields()
    {
        return StreamEx.of(fields).map( SqlUtil::quoteIdentifier ).toArray( String[]::new );
    }
    
    public abstract void insert(Object... fields) throws BiosoftSQLException;
    
    public abstract void flush() throws BiosoftSQLException;
}
