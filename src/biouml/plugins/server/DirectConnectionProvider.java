package biouml.plugins.server;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import biouml.plugins.server.access.SQLRegistry.SQLInfo;
import ru.biosoft.access.sql.SqlConnectionPool;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.Maps;

public class DirectConnectionProvider implements SqlEditorConnectionProvider
{
    protected SQLInfo info;
    protected Connection connection = null;
    protected Logger log;

    public DirectConnectionProvider(SQLInfo info, Logger log)
    {
        this.info = info;
        this.log = log;
    }

    @Override
    public void fillResultTable(String query, TableDataCollection tableDataCollection) throws Exception
    {
        initConnection();
        List<List<String>> data = SqlEditorUtils.getQueryResult(connection, query, 0, Integer.MAX_VALUE);
        List<String> columns = data.get(0);
        int numberOfColumns = columns.size();
        String columnName;
        for( int i = 0; i < numberOfColumns; i++ )
        {
            columnName = columns.get(i);
            if( "".equals(columnName) )
                columnName = "Column" + i;
            tableDataCollection.getColumnModel().addColumn(columnName, String.class);
        }

        int rowCnt = 0;
        for( int j = 1; j < data.size(); j++ )
        {
            List<String> values = data.get(j);
            Object rowValues[] = new Object[numberOfColumns];
            for( int i = 0; i < numberOfColumns; i++ )
            {
                rowValues[i] = values.get(i);
            }
            TableDataCollectionUtils.addRow(tableDataCollection, Integer.toString(rowCnt), rowValues);

            rowCnt++;
        }
    }

    @Override
    public Map<String, ColumnModel> getTablesStructure() throws Exception
    {
        initConnection();
        Map<String, List<String[]>> tables = SqlEditorUtils.getTablesStructure(connection);
        return Maps.transformValues(tables, columns -> {
            ColumnModel cm = new ColumnModel(null);
            for( String[] col : columns )
            {
                cm.addColumn(col[0], col[0], col[1], null, null);
            }
            return cm;
        });
    }

    @Override
    public String getServerHost()
    {
        return info.getHost() + ":" + info.getPort() + "/" + info.getDatabase();
    }

    @Override
    public void close()
    {
        if( connection != null )
        {
            try
            {
                connection.close();
            }
            catch( SQLException e )
            {
                log.log(Level.SEVERE, "Connection error", e);
            }
        }
    }

    @Override
    public String getInfo()
    {
        return info.toString();
    }

    private void initConnection()
    {
        try
        {
            if( connection == null )
            {
                connection = SqlConnectionPool.getPersistentConnection(info.getJdbcUrl(), info.getUsername(), info.getPassword());
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Connection error", e);
        }
    }

}
