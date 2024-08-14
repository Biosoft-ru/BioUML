package ru.biosoft.access.sql;

import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.util.LazySubSequence;

/**
 * CharSequence which is not initialized until somebody actually reads it
 * @author lan
 */
public class SqlLazyCharSequence implements CharSequence
{
    volatile private String data;
    private String query;
    private SqlConnectionHolder holder;
    
    public SqlLazyCharSequence(SqlConnectionHolder holder, Query query)
    {
        this(holder, query.toString());
    }
    
    public SqlLazyCharSequence(SqlConnectionHolder holder, String query)
    {
        this.holder = holder;
        this.query = query;
    }
    
    private void init()
    {
        if(data == null)
        {
            synchronized(this)
            {
                if(data == null)
                {
                    try
                    {
                        data = SqlUtil.queryString(holder.getConnection(), query);
                    }
                    catch( BiosoftSQLException e )
                    {
                        data = "";
                    }
                    holder = null;
                    query = null;
                }
            }
        }
    }

    @Override
    public int length()
    {
        init();
        return data.length();
    }

    @Override
    public char charAt(int index)
    {
        init();
        return data.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end)
    {
        return new LazySubSequence(this, start, end);
    }

    @Override
    public String toString()
    {
        init();
        return data;
    }

    @Override
    public int hashCode()
    {
        init();
        return data.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if( this == obj )
            return true;
        if( obj == null )
            return false;
        if( getClass() != obj.getClass() )
            return false;
        init();
        SqlLazyCharSequence other = (SqlLazyCharSequence)obj;
        other.init();
        if( data == null )
        {
            if( other.data != null )
                return false;
        }
        else if( !data.equals(other.data) )
            return false;
        return true;
    }
}
