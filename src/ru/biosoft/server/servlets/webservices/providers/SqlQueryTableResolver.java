package ru.biosoft.server.servlets.webservices.providers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONObject;

import biouml.plugins.server.SqlEditorProtocol;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.journal.Journal;
import ru.biosoft.journal.JournalRegistry;
import ru.biosoft.server.Response;
import ru.biosoft.server.Service;
import ru.biosoft.server.ServiceRegistry;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.WebException;
import ru.biosoft.server.servlets.webservices.providers.WebTablesProvider.CommonTableResolver;
import ru.biosoft.table.StandardTableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.access.TableResolver;
import ru.biosoft.tasks.TaskInfo;

public class SqlQueryTableResolver extends TableResolver implements CommonTableResolver
{
    protected String sqlQuery;
    protected int start;
    protected int length;
    protected boolean addToJournal;

    public SqlQueryTableResolver(BiosoftWebRequest arguments) throws WebException
    {
        this.start = arguments.optInt( "iDisplayStart", 0 );
        this.length = arguments.optInt( "iDisplayLength", 1 );
        this.sqlQuery = arguments.get( "query" );
        this.addToJournal = Boolean.valueOf( arguments.getOrDefault( "addToJournal", "false" ) );

    }
    public SqlQueryTableResolver(String sqlquery, int start, int length, boolean addToJournal)
    {
        this.start = start;
        this.length = length;
        this.sqlQuery = sqlquery;
        this.addToJournal = addToJournal;
    }

    @Override
    public DataCollection<?> getTable(DataElement de) throws Exception
    {
        TaskInfo taskInfo = null;
        Journal journal = JournalRegistry.getCurrentJournal();
        if( addToJournal && journal != null )
        {
            taskInfo = journal.getEmptyAction();
            taskInfo.setType( TaskInfo.SQL );
            taskInfo.setData( getQuery() );
        }
        try
        {
            Service service = ServiceRegistry.getService( SqlEditorProtocol.SQL_EDITOR_SERVICE );
            Map<String, String> map = new HashMap<>();
            map.put( SecurityManager.SESSION_ID, SecurityManager.getSession() );
            map.put( SqlEditorProtocol.KEY_QUERY, sqlQuery );
            map.put( SqlEditorProtocol.KEY_START, Integer.toString( start ) );
            map.put( SqlEditorProtocol.KEY_LENGTH, Integer.toString( length ) );
            SqlQueryTableResolver.SQLResponse response = new SQLResponse();
            if( service != null )
            {
                service.processRequest( SqlEditorProtocol.DB_EXECUTE, map, response );
            }

            if( response.getError() != null )
            {
                throw new Exception( response.getError() );
            }

            if( response.getJsonString() != null )
            {
                JSONObject json = new JSONObject( response.getJsonString() );
                JSONArray jsonColumns = json.getJSONArray( "columns" );
                JSONObject jsonData = json.getJSONObject( "data" );

                StandardTableDataCollection tableDataCollection = new StandardTableDataCollection( null, new Properties() );
                for( int i = 0; i < jsonColumns.length(); i++ )
                {
                    tableDataCollection.getColumnModel().addColumn( jsonColumns.getString( i ), String.class );
                }
                int rowCnt = 0;
                while( rowCnt < start + length )
                {
                    Object rowValues[] = new Object[jsonColumns.length()];
                    if( rowCnt >= start )
                    {
                        String key = Integer.toString( rowCnt );
                        if( !jsonData.has( key ) )
                        {
                            break;
                        }
                        JSONArray jsonRow = jsonData.getJSONArray( key );
                        for( int i = 0; i < jsonRow.length(); i++ )
                        {
                            rowValues[i] = jsonRow.getString( i );
                        }
                    }
                    TableDataCollectionUtils.addRow( tableDataCollection, Integer.toString( rowCnt ), rowValues );
                    rowCnt++;
                }
                return tableDataCollection;
            }
        }
        finally
        {
            if( addToJournal && journal != null && taskInfo != null )
            {
                taskInfo.setEndTime();
                journal.addAction( taskInfo );
            }
        }
        return null;
    }

    public static class SQLResponse extends Response
    {
        protected String error = null;
        protected String jsonString = null;

        public SQLResponse()
        {
            super( null, null );
        }

        @Override
        public void error(String message) throws IOException
        {
            error = message;
        }

        @Override
        public void send(byte[] message, int format) throws IOException
        {
            jsonString = new String( message, "UTF-16BE" );
        }

        public String getError()
        {
            return error;
        }

        public String getJsonString()
        {
            return jsonString;
        }
    }

    public String getQuery()
    {
        return sqlQuery;
    }
}