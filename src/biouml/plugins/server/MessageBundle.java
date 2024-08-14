
package biouml.plugins.server;

import java.util.ListResourceBundle;

import javax.swing.Action;

import com.developmentontheedge.application.Application;

public class MessageBundle extends ListResourceBundle
{

    @Override
    protected Object[][] getContents ( )
    {
        return contents;
    }

    public String getResourceString ( String key )
    {
        try
        {
            return getString ( key );
        }
        catch ( Throwable th )
        {

        }
        return key;
    }

    private static final Object[][] contents =
    {
            { "DATABASE_TYPE_UNKNOWN_VERSION", "Unknown" },
            
            // ClientModule bean info
            { "CN_DATABASE", "Database" },
            { "CD_DATABASE", "Client Database" },
            
            // DataClientCollection bean info
            { "CN_DATA_DC", "Data Collection" },
            { "CD_DATA_DC", "Client Data Collection" },
            
            // DiagramClientCollection bean info
            { "CN_DIAGRAM_DC", "Diagram Collection" },
            { "CD_DIAGRAM_DC", "Client Diagram Collection" },
            
            // SqlModule bean info
            { "CN_SQLDATABASE", "Database" },
            { "CD_SQLDATABASE", "Database (SQL)" },
            
            // DataSqlCollection bean info
            { "CN_SQL_DC", "Data Collection" },
            { "CD_SQL_DC", "Data Collection (SQL)" },
            
            // DiagramSqlCollection bean info
            { "CN_SQLDIAGRAM_DC", "Diagram Collection" },
            { "CD_SQLDIAGRAM_DC", "Diagram Collection (SQL)" },
            
            //SQL view part actions
            {SqlEditorViewPart.EXECUTE_ACTION + Action.SMALL_ICON,         "execute.gif"},
            {SqlEditorViewPart.EXECUTE_ACTION + Action.NAME,               "Execute query"},
            {SqlEditorViewPart.EXECUTE_ACTION + Action.SHORT_DESCRIPTION,  "Execute query"},
            {SqlEditorViewPart.EXECUTE_ACTION + Action.ACTION_COMMAND_KEY, "cmd-sql-execute"},
            
            {SqlEditorViewPart.EXPLAIN_ACTION + Action.SMALL_ICON,         "explain.gif"},
            {SqlEditorViewPart.EXPLAIN_ACTION + Action.NAME,               "Explain query"},
            {SqlEditorViewPart.EXPLAIN_ACTION + Action.SHORT_DESCRIPTION,  "Explain query"},
            {SqlEditorViewPart.EXPLAIN_ACTION + Action.ACTION_COMMAND_KEY, "cmd-sql-explain"},
            
            {SqlEditorViewPart.CONNECT_ACTION + Action.SMALL_ICON,         "connect.gif"},
            {SqlEditorViewPart.CONNECT_ACTION + Action.NAME,               "Connect to server"},
            {SqlEditorViewPart.CONNECT_ACTION + Action.SHORT_DESCRIPTION,  "Connect to server"},
            {SqlEditorViewPart.CONNECT_ACTION + Action.ACTION_COMMAND_KEY, "cmd-sql-connect"},
            
            {SqlEditorViewPart.CLEAR_ACTION + Action.SMALL_ICON,           "clear.gif"},
            {SqlEditorViewPart.CLEAR_ACTION + Action.NAME,                 "Clear query field"},
            {SqlEditorViewPart.CLEAR_ACTION + Action.SHORT_DESCRIPTION,    "Clear query field"},
            {SqlEditorViewPart.CLEAR_ACTION + Action.ACTION_COMMAND_KEY,   "cmd-sql-clear"},
            
            // Load Module action
            {LoadDatabasesAction.KEY + Action.SMALL_ICON, "newModule.gif"},
            {LoadDatabasesAction.KEY + Action.NAME, "Load database"},
            {LoadDatabasesAction.KEY + Action.SHORT_DESCRIPTION, "Load database from server"},
            {LoadDatabasesAction.KEY + Action.LONG_DESCRIPTION, "Load database from server"},
            {LoadDatabasesAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-load-database"},

            //SQL Editor bean info
            { "RB_DIRECT_CONNECTION", "Direct SQL connection" },
            { "RB_SERVER_CONNECTION", Application.getGlobalValue("ApplicationName")+" server connection" },
            { "CURRENT_CONNECTION", "Current connection:" },
            
    };
}
