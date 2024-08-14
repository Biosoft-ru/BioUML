package ru.biosoft.access.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;

import ru.biosoft.access.exception.BiosoftSQLException;

/**
 * Iterator on rows of sql table that loads rows by chunks(pages).
 * The table should has INTEGER UNSIGNED NOT NULL primary key.
 * This iterator return rows in the order of primary key.
 * Each row is converted to java object using provided {@code ResulSetMapper}.
 * The rows can be filtered given sql where clause.
 * The iterator is {@code AutoCloseable} but if you read it fully, you can skip call to close
 * since it will be closed automatically on the last call to the {@code SqlPagingIterator.next()}.
 * 
 * Implementation is more efficient then traditional "SELECT ... LIMIT OFFSET,ROWS"
 * and uses "SELECT ... WHERE id>prev_id LIMIT ROWS"
 * 
 * @author ivan
 */
public class SqlPagingIterator<E> implements Iterator<E>, AutoCloseable
{
    private SqlConnectionHolder connectionProvider;
    private ResultSetMapper<E> mapper;
    private int pageSize;
    private String tableName;
    private String idColumn;
    private String whereClause;
    
    private Statement statement;
    private ResultSet resultSet;
    private int nextId;
    private boolean hasNext;
    
    public SqlPagingIterator(SqlConnectionHolder connectionProvider, String tableName, String idColumn, String whereClause, int pageSize, ResultSetMapper<E> mapper)
    {
        this.connectionProvider = connectionProvider;
        this.tableName = tableName;
        this.idColumn = idColumn;
        this.whereClause = whereClause;
        this.pageSize = pageSize;
        this.mapper = mapper;
        nextResultSet();
    }

    @Override
    public boolean hasNext()
    {
        return hasNext;
    }

    @Override
    public E next()
    {
        E result;
        try
        {
            result = mapper.map( resultSet );
        }
        catch( SQLException e )
        {
            throw new BiosoftSQLException(connectionProvider, e);
        }
        advance();
        return result;
    }

    @Override
    public void close()
    {
        SqlUtil.close( statement, resultSet );
    }

    private void advance() throws BiosoftSQLException
    {
        try
        {
            if( resultSet.next() )
                updateNextId();    
            else
                nextResultSet();
        }
        catch( SQLException e )
        {
            throw new BiosoftSQLException(connectionProvider, e);
        }
    }

    private void nextResultSet() throws BiosoftSQLException
    {
        if(statement != null)
            SqlUtil.close( statement, resultSet );
        statement = SqlUtil.createStatement( connectionProvider.getConnection() );
        String where = "WHERE $id$ >= $from$";
        if(whereClause != null && !whereClause.trim().isEmpty())
            where += " AND " + whereClause;
        Query query = new Query("SELECT * FROM $table$ " + where + " ORDER BY $id$ LIMIT $max$")
                .name("table", tableName)
                .name("id", idColumn)
                .num("from", nextId)
                .num("max", pageSize);
        resultSet = SqlUtil.executeQuery(statement, query);
        try
        {
            hasNext = resultSet.next();
            if(hasNext)
                updateNextId();
            else
                close();
        }
        catch( SQLException e )
        {
            throw new BiosoftSQLException(connectionProvider, e);
        }
    }
    
    private void updateNextId() throws SQLException
    {
        nextId = 1 + resultSet.getInt( idColumn );
    }
}
