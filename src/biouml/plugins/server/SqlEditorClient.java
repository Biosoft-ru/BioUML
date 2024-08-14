package biouml.plugins.server;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.server.Request;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;

/**
 * Client class for SQL service
 */
public class SqlEditorClient extends SqlEditorProtocol implements SqlEditorConnectionProvider
{
    protected Request connection;
    protected Logger log;
    protected String sessionId;
    protected String host;
    protected String username;

    public SqlEditorClient(String host, Request conn, Logger log, String sessionId, String username)
    {
        this.connection = conn;
        this.log = log;
        this.sessionId = sessionId;
        this.host = host;
        this.username = username;
    }

    public SqlEditorClient(String host, Request conn, Logger log, String sessionId)
    {
        this(host, conn, log, sessionId, null);
    }

    @Override
    public void close()
    {
        if( connection != null )
            connection.close();
    }

    @Override
    public String getServerHost()
    {
        return host;
    }

    /**
     * Get table structure
     */
    @Override
    public Map<String, ColumnModel> getTablesStructure() throws Exception
    {
        Map<String, String> map = new HashMap<>();
        map.put(SecurityManager.SESSION_ID, sessionId);
        byte[] data = connection.request(SqlEditorProtocol.SQL_EDITOR_SERVICE, DB_GET_TABLES, map, true);

        if( data != null )
        {
            JSONObject json = new JSONObject(new String(data, "UTF-16BE"));
            Map<String, ColumnModel> result = new HashMap<>();
            Iterator<String> iter = json.keys();
            while( iter.hasNext() )
            {
                String key = iter.next();
                ColumnModel cm = new ColumnModel(null);
                JSONArray columns = json.getJSONArray(key);
                for( int i = 0; i < columns.length(); i++ )
                {
                    JSONObject cInfo = columns.getJSONObject(i);
                    cm.addColumn(cInfo.getString("name"), cInfo.getString("name"), cInfo.getString("type"), null, null);
                }
                result.put(key, cm);
            }
            return result;
        }
        return null;
    }

    /**
     * Fill table with SQL query results
     */
    @Override
    public void fillResultTable(String query, TableDataCollection tableDataCollection) throws Exception
    {
        Map<String, String> map = new HashMap<>();
        map.put(SecurityManager.SESSION_ID, sessionId);
        map.put(SqlEditorProtocol.KEY_QUERY, query);
        byte[] data;
        try
        {
            data = connection.request(SqlEditorProtocol.SQL_EDITOR_SERVICE, DB_EXECUTE, map, true);
        }
        catch( Exception e )
        {
            throw new SqlEditorException(ExceptionRegistry.log(e));
        }
        if( data == null )
        {
            throw new SqlEditorException("Empty result");
        }
        JSONObject json = new JSONObject(new String(data, "UTF-16BE"));
        JSONArray jsonColumns = json.getJSONArray("columns");
        JSONObject jsonData = json.getJSONObject("data");

        for( int i = 0; i < jsonColumns.length(); i++ )
        {
            tableDataCollection.getColumnModel().addColumn(jsonColumns.getString(i), String.class);
        }
        Iterator<String> iter = jsonData.keys();
        while( iter.hasNext() )
        {
            String key = iter.next();
            JSONArray jsonRow = jsonData.getJSONArray(key);
            Object rowValues[] = new Object[jsonRow.length()];
            for( int i = 0; i < jsonRow.length(); i++ )
            {
                rowValues[i] = jsonRow.getString(i);
            }
            TableDataCollectionUtils.addRow(tableDataCollection, key, rowValues);
        }
    }

    static class SqlEditorException extends Exception
    {
        public SqlEditorException(String string)
        {
            super(string);
        }
    }

    @Override
    public String getInfo()
    {
        if( username == null )
            return host;

        return username + "@" + host;
    }
}
