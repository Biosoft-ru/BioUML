package ru.biosoft.access.sql;

import java.sql.Connection;

import ru.biosoft.access.exception.BiosoftSQLException;

/**
 * Bulk inserter which forms single long INSERT INTO ... VALUES(...),(...),...
 * @author lan
 */
public class RewriteBulkInsert extends BulkInsert
{
    private static final int maxAllowedPacked = 20000;
    private String queryPrefix;
    private StringBuffer currentQuery;
    private int queryRows;
    
    public RewriteBulkInsert(Connection conn, String table, String[] fields)
    {
        super(conn, table, fields);
        if(fields == null)
            queryPrefix = "INSERT INTO "+table+" VALUES";
        else
            queryPrefix = "INSERT INTO "+table+"("+String.join(",",getEscapedFields())+") VALUES";
        initCurrentQuery();
    }

    public RewriteBulkInsert(SqlConnectionHolder holder, String table, String[] fields)
    {
        super(holder, table, fields);
    }

    public RewriteBulkInsert(Connection conn, String table)
    {
        this(conn, table, null);
    }
    
    public RewriteBulkInsert(SqlConnectionHolder holder, String table)
    {
        this(holder, table, null);
    }

    private void initCurrentQuery()
    {
        currentQuery = new StringBuffer(queryPrefix);
        queryRows = 0;
    }

    @Override
    public void insert(Object... fields) throws BiosoftSQLException
    {
        StringBuilder destination = new StringBuilder();
        for(Object obj : fields)
        {
            if(destination.length() > 0)
                destination.append( ',' );
            if( obj == null )
            {
                destination.append("NULL");
            }
            else
            {
                String result = obj.toString();
                if( ( obj instanceof Double && ( (Double)obj ).isNaN() ) || ( obj instanceof Float && ( (Float)obj ).isNaN() ) )
                {
                    destination.append("NULL");
                }
                else if( ( obj instanceof Double && ( (Double)obj ).isInfinite() ) || ( obj instanceof Float && ( (Float)obj ).isInfinite() ) )
                {
                    destination.append("'").append( ( ( obj instanceof Double ) ? (Double)obj : (Float)obj ) > 0 ? String.valueOf(Double.MAX_VALUE) : String.valueOf(-Double.MAX_VALUE))
                            .append("'");
                }
                else if( ( obj instanceof Double ) || ( obj instanceof Float ) || ( obj instanceof Integer ) )
                {
                    destination.append(result);
                }
                else
                {
                    destination.append(SqlUtil.quoteString(result));
                }
            }
        }
        if(queryRows > 0 && destination.length()+currentQuery.length()+3 > maxAllowedPacked)
            flush();
        currentQuery.append(queryRows==0?"(":",(");
        currentQuery.append(destination);
        currentQuery.append(")");
        queryRows++;
    }

    @Override
    public void flush() throws BiosoftSQLException
    {
        if(queryRows == 0) return;
        SqlUtil.execute(getConnection(), currentQuery.toString());
        initCurrentQuery();
    }
}
