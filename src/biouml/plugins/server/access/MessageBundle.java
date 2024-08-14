package biouml.plugins.server.access;

import java.util.ListResourceBundle;



public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return contents;
    }

    public String getResourceString(String key)
    {
        try
        {
            return getString(key);
        }
        catch (Throwable th)
        {

        }
        return key;
    }
    private static final Object[][] contents =
    {
        //--- Server registry preferences -------------------------------------------
        {"SERVER_PREFERENCES_PN", "Server preferences"}, {"SERVER_PREFERENCES_PD", "Server preferences."},
        {"LOAD_DATABASE_DIALOG_PREFERENCES_SERVERLIST_PN", "Server addresses list"},
        {"LOAD_DATABASE_DIALOG_PREFERENCES_SERVERLIST_PD", "Server addresses list to be shown at drop down."},
        
        //--- SQL preferences pane constants
        {"SQL_DIALOG_TYPE", "Type:"},
        {"SQL_DIALOG_HOST", "Host:"},
        {"SQL_DIALOG_DEFAULT_HOST", "localhost"},
        {"SQL_DIALOG_PORT", "Port:"},
        {"SQL_DIALOG_DEFAULT_PORT", "3306"},
        {"SQL_DIALOG_DATABASE", "Database:"},
        {"SQL_DIALOG_USERNAME", "Username:"},
        {"SQL_DIALOG_PASSWORD", "Password:"},
        {"SQL_DIALOG_ADD_BUTTON", "Add"},
        {"SQL_DIALOG_REMOVE_BUTTON", "Remove"},
        
        //--- SQLInfo BeanInfo constants
        {"CN_SQLINFO_DESCRIPTOR", "SQL info"},
        {"CD_SQLINFO_DESCRIPTOR", "SQL info"},
        {"PN_SQLINFO_TYPE", "Type"},
        {"PD_SQLINFO_TYPE", "Database type"},
        {"PN_SQLINFO_HOST", "Host"},
        {"PD_SQLINFO_HOST", "SQL server address"},
        {"PN_SQLINFO_PORT", "Port"},
        {"PD_SQLINFO_PORT", "SQL server port"},
        {"PN_SQLINFO_DATABASE", "Database"},
        {"PD_SQLINFO_DATABASE", "Database name"},
        {"PN_SQLINFO_USERNAME", "Username"},
        {"PD_SQLINFO_USERNAME", "Username"},

        {"CN_CLIENT_DC", "Client Data Collection"},
        {"CD_CLIENT_DC", "Client Data Collection"},
    };
}


