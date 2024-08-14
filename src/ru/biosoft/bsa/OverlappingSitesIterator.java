package ru.biosoft.bsa;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;

import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.sql.Query;
import ru.biosoft.access.sql.SqlUtil;

//SELECT * FROM test WHERE chrom='7' AND start >= 0 AND start <= 159345974 AND id >= 0 and end >=0 ORDER BY chrom,start,id LIMIT 60975;
public class OverlappingSitesIterator implements Iterator<Site>, AutoCloseable
{
    private boolean hasNext;
    private ResultSet resultSet;
    private Statement statement;
    private SqlTrack parent;
    private SubSequence seq;
    
    private int lastStart = -1, lastId = -1;
    
    public OverlappingSitesIterator(SqlTrack parent, SubSequence seq)
    {
        this.parent = parent;
        this.seq = seq;
        nextResultSet();
    }

    @Override
    public boolean hasNext()
    {
        return hasNext;
    }

    @Override
    public Site next()
    {
        Site result = parent.createSite( seq, resultSet );
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
            throw new BiosoftSQLException(parent, e);
        }
    }

    private void nextResultSet() throws BiosoftSQLException
    {
        if(statement != null)
            SqlUtil.close( statement, resultSet );
        statement = SqlUtil.createStatement( parent.getConnection() );
        
        int leftBoundary = seq.getFrom() - parent.getMaxSiteLength() + 1;
        if( leftBoundary < 0 )
            leftBoundary = 0;
        leftBoundary = Math.max( leftBoundary, lastStart );
        
        Query query = new Query("SELECT * FROM $table$ WHERE chrom=$chr$ AND ((start=$left$ AND id>$lastId$) OR start>$left$) AND start <= $to$ AND end >=$from$ ORDER BY chrom,start,id LIMIT $max$")
                .name("table", parent.getTableId())
                .str( "chr", seq.getSequenceName() )
                .num("from", seq.getFrom())
                .num("to", seq.getTo() )
                .num("left", leftBoundary)
                .num( "lastId", lastId )
                .num("max", parent.initTableStatus());
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
            throw new BiosoftSQLException(parent, e);
        }
    }
    
    private void updateNextId() throws SQLException
    {
        lastStart = resultSet.getInt( 3 );
        lastId = resultSet.getInt( 1 );
    }
}
