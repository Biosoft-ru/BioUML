package ru.biosoft.access.exception;

import java.sql.Connection;
import java.sql.SQLException;

import ru.biosoft.access.sql.PersistentConnection;
import ru.biosoft.access.sql.Query;
import ru.biosoft.access.sql.SqlConnectionHolder;
import ru.biosoft.exception.ExceptionDescriptor;
import ru.biosoft.exception.LoggedException;

/**
 * @author lan
 *
 */
public class BiosoftSQLException extends LoggedException
{
    private static String KEY_URL = "url";
    private static String KEY_QUERY = "query";

    public static final ExceptionDescriptor ED_SQL_COMMON = new ExceptionDescriptor( "Common", LoggingLevel.Summary,
            "Error communicating with SQL server.");
    public static final ExceptionDescriptor ED_SQL_ACCESS = new ExceptionDescriptor( "Access", LoggingLevel.Summary,
            "Access to SQL server is denied.");

    public BiosoftSQLException(SQLException ex)
    {
        this((Connection)null, null, ex);
    }

    public BiosoftSQLException(String url, String query, SQLException ex)
    {
        super( ex, defineDescriptor( ex, url, query ) );
        if(url != null)
        {
            properties.put( KEY_URL, url );
        }
        if(query != null)
        {
            properties.put( KEY_QUERY, query );
        }
    }

    public BiosoftSQLException(Connection c, String query, SQLException ex)
    {
        this(c instanceof PersistentConnection?( (PersistentConnection)c ).getURL():null, query, ex);
    }

    public BiosoftSQLException(SqlConnectionHolder holder, SQLException ex)
    {
        this(getConnection(holder), (String)null, ex);
    }

    public BiosoftSQLException(SqlConnectionHolder holder, String query, SQLException ex)
    {
        this(getConnection(holder), query, ex);
    }

    public BiosoftSQLException(SqlConnectionHolder holder, Query sql, SQLException ex)
    {
        this(holder, sql.toString(), ex);
    }

    private static Connection getConnection(SqlConnectionHolder holder)
    {
        try
        {
            return holder.getConnection();
        }
        catch( BiosoftSQLException e )
        {
            return null;
        }
    }

    private static ExceptionDescriptor defineDescriptor( SQLException ex, String url, String query )
    {
        if( ex.getSQLState().startsWith( "08" ) )
        { 
            return new ExceptionDescriptor( "Connection", LoggingLevel.Summary,
                "Connection to SQL server cannot be established. URL: " + url );
        }  
        if( ex.getSQLState().equals("28000") || ex.getMessage().startsWith("Access denied") )
        {
            return ED_SQL_ACCESS;
        }
        if( query != null )
        {
            return new ExceptionDescriptor( "Common", LoggingLevel.Summary,
                "Error \"" + ex.getMessage() + "\" in SQL statement:\n" + query );
        } 
        return ED_SQL_COMMON;
    }
}
