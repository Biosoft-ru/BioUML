package ru.biosoft.access.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.util.ChunkedList;

/**
 * Returns unmodifiable list of Strings which result from big sql queries
 * Query will be split to parts using "LIMIT" clause, so the whole result will not appear in the memory
 * @author lan
 */
public class SqlList extends ChunkedList<String>
{
    private static final int CHUNK_SIZE = 10000;
    private final String query;
    private final SqlConnectionHolder connectionHolder;
    
    public SqlList(SqlConnectionHolder connectionHolder, Query query, int size, boolean sorted)
    {
        this(connectionHolder, query.get(), size, sorted);
    }
    
    public SqlList(SqlConnectionHolder connectionHolder, Query query, int size)
    {
        this(connectionHolder, query.get(), size);
    }
    
    public SqlList(SqlConnectionHolder connectionHolder, String query, int size, boolean sorted)
    {
        super(size, CHUNK_SIZE, sorted);
        this.connectionHolder = connectionHolder;
        this.query = query;
    }
    
    public SqlList(SqlConnectionHolder connectionHolder, String query, int size)
    {
        this(connectionHolder, query, size, false);
    }

    @Override
    protected String[] getChunk(int from, int to)
    {
        List<String> result = null;
        ResultSet resultSet = null;
        Statement st = null;
        String sql = query+" LIMIT "+from+","+(to-from);
        try
        {
            st = connectionHolder.getConnection().createStatement();
            resultSet = st.executeQuery(sql);
            result = new ArrayList<>(to-from);
            int i=0;
            while(resultSet.next() && i < to-from)
            {
                result.add(resultSet.getString(1));
            }
        }
        catch( SQLException e )
        {
            throw new BiosoftSQLException( connectionHolder, sql, e );
        }
        finally
        {
            SqlUtil.close(st, resultSet);
        }
        return result.toArray(new String[result.size()]);
    }
}
