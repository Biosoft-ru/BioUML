package biouml.plugins.server;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.List;
import java.util.Map;

import java.util.logging.Logger;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.sql.SqlConnectionHolder;
import ru.biosoft.journal.JournalRegistry;
import ru.biosoft.server.Response;
import ru.biosoft.server.Service;
import ru.biosoft.server.SynchronizedServiceSupport;
import ru.biosoft.util.JsonUtils;

public class SqlEditorService extends SqlEditorProtocol implements Service
{
    protected static final Logger log = Logger.getLogger(SqlEditorService.class.getName());
    
    protected SynchronizedServiceSupport ss;
    protected Connection sqlconnection;
    protected Response connection;
    protected Map arguments;

    public SqlEditorService()
    {
        // TODO: use not synched mode
        ss = new SynchronizedServiceSupport()
        {
            @Override
            protected boolean processRequest(int command) throws Exception
            {
                return SqlEditorService.this.processRequest(command);
            }
        };
    }

    @Override
    public void processRequest(Integer command, Map data, Response out)
    {
        ss.processRequest(command, data, out);
    }

    protected boolean processRequest(int command) throws Exception
    {
        connection = ss.getSessionConnection();
        arguments = ss.getSessionArguments();
        sqlconnection = getCurrentUserConnection();
        try
        {
            if( sqlconnection == null )
            {
                return false;
            }
            switch( command )
            {
                case SqlEditorProtocol.DB_GET_TABLES:
                    sendTablesStructure();
                    break;
                case SqlEditorProtocol.DB_GET_TABLES_ONLY:
                    sendTables();
                    break;
                case SqlEditorProtocol.DB_GET_COLUMNS:
                    sendTableStructure(arguments.get(KEY_TABLE).toString());
                    break;
                case SqlEditorProtocol.DB_EXECUTE:
                    sendQueryResult();
                    break;
                default:
                    return false;
            }
            return true;
        }
        finally
        {
            connection = null;
            arguments = null;
            sqlconnection = null;
        }
    }
    
    public static Connection getCurrentUserConnection()
    {
        DataElementPath projectPath = JournalRegistry.getProjectPath();
        if(projectPath == null) return null;
        DataElement dataElement = projectPath.getChildPath("Data").optDataElement();
        try
        {
            dataElement = DataCollectionUtils.fetchPrimaryElementPrivileged(dataElement);
            if(dataElement instanceof SqlConnectionHolder) return ((SqlConnectionHolder)dataElement).getConnection();
        }
        catch( BiosoftSQLException e1 )
        {
            log.log(Level.SEVERE, "SqlEditor: cannot connect");
        }
        return null;
    }

    //////////////////////////////////////////////
    // Protocol implementation functions
    //

    protected void sendTables() throws Exception
    {
        try
        {
            JsonArray result = JsonUtils.fromCollection( SqlEditorUtils.getTables(sqlconnection) );
            connection.send(result.toString().getBytes("UTF-16BE"), ru.biosoft.server.Connection.FORMAT_SIMPLE);
        }
        catch( Exception ex )
        {
            connection.error("Can not read database tables structure");
            return;
        }
    }
    
    protected void sendTablesStructure() throws Exception
    {
        try
        {
            JsonObject result = new JsonObject();

            Map<String, List<String[]>> tables = SqlEditorUtils.getTablesStructure(sqlconnection);
            for( Map.Entry<String, List<String[]>> entry : tables.entrySet() )
            {
                String databaseName = entry.getKey();
                List<String[]> columns = entry.getValue();
                JsonArray cols = new JsonArray();
                for( String[] col : columns )
                {
                    cols.add(new JsonObject().add("name", col[0]).add("type", col[1]));
                }
                result.add(databaseName, cols);
            }
            connection.send(result.toString().getBytes("UTF-16BE"), ru.biosoft.server.Connection.FORMAT_SIMPLE);
        }
        catch( Exception ex )
        {
            connection.error("Can not read database tables structure");
            return;
        }
    }

    protected void sendTableStructure(String table) throws IOException
    {
        try
        {
            List<String[]> columns = SqlEditorUtils.getTableStructure(sqlconnection, table);
            JsonArray cols = new JsonArray();
            for( String[] col : columns )
            {
                cols.add(new JsonObject().add("name", col[0]).add("type", col[1]));
            }
            connection.send(cols.toString().getBytes("UTF-16BE"), ru.biosoft.server.Connection.FORMAT_SIMPLE);
        }
        catch( Exception ex )
        {
            connection.error("Can not read database tables structure");
            return;
        }
    }
    
    protected void sendQueryResult() throws Exception
    {
        Object sqlQueryObj = arguments.get(SqlEditorProtocol.KEY_QUERY);
        Object startObj = arguments.get(SqlEditorProtocol.KEY_START);
        Object lengthObj = arguments.get(SqlEditorProtocol.KEY_LENGTH);
        if( sqlQueryObj != null )
        {
            int start = 0;
            int length = Integer.MAX_VALUE;
            if( startObj != null && lengthObj != null )
            {
                try
                {
                    start = Integer.parseInt(startObj.toString());
                    length = Integer.parseInt(lengthObj.toString());
                }
                catch( NumberFormatException e )
                {
                }
            }

            try
            {
                List<List<String>> data = SqlEditorUtils.getQueryResult(sqlconnection, sqlQueryObj.toString(), start, length);
                List<String> columns = data.get(0);
                int numberOfColumns = columns.size();
                String columnName;
                JsonArray resultColumns = new JsonArray();
                for( int i = 0; i < numberOfColumns; i++ )
                {
                    columnName = columns.get(i);
                    if( "".equals(columnName) )
                        columnName = "Column" + i;
                    resultColumns.add(columnName);
                }
                JsonObject resultData = new JsonObject();
                int rowCnt = 0;
                for( int j = 1; j < data.size(); j++ )
                {
                    List<String> values = data.get(j);
                    JsonArray resultRow = new JsonArray();
                    for( int i = 0; i < numberOfColumns; i++ )
                    {
                        resultRow.add(values.get(i));
                    }
                    resultData.add(Integer.toString(rowCnt), resultRow);
                    rowCnt++;
                }
                JsonObject result = new JsonObject().add("columns", resultColumns).add("data", resultData);
                connection.send(result.toString().getBytes("UTF-16BE"), ru.biosoft.server.Connection.FORMAT_GZIP);
            }
            catch( SQLException sqlEx )
            {
                connection.error(sqlEx.getMessage());
                return;
            }
        }
        else
        {
            connection.error("Incorrect input parameters");
            return;
        }
    }
}
