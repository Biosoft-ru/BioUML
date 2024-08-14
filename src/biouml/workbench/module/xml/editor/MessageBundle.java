package biouml.workbench.module.xml.editor;

import java.util.logging.Level;
import java.util.ListResourceBundle;

import java.util.logging.Logger;

public class MessageBundle extends ListResourceBundle
{
    private final Logger log = Logger.getLogger(MessageBundle.class.getName());

    @Override
    protected Object[][] getContents()
    {
        return new Object[][] {
                {"COMMON_TAB_NAME_FIELD",            "Name"},
                {"COMMON_TAB_TITLE_FIELD",           "Title"},
                {"COMMON_TAB_DESCRIPTION_FIELD",     "<html><font color=gray>Description"},
                {"COMMON_TAB_TYPE_FIELD",            "Type"},
                {"COMMON_TAB_DB_TYPE_FIELD",         "<html><font color=gray>Database type"},
                {"COMMON_TAB_DB_VERSION_FIELD",      "<html><font color=gray>Database version"},
                {"COMMON_TAB_DB_NAME_FIELD",         "<html><font color=gray>Database name"},
                {"COMMON_TAB_JDBC_DRIVER_FIELD",     "JDBC driver"},
                {"COMMON_TAB_JDBC_DEFAULT_DRIVER",   "com.mysql.jdbc.Driver"},
                {"COMMON_TAB_JDBC_URL_FIELD",        "JDBC URL"},
                {"COMMON_TAB_JDBC_DEFAULT_URL",      "jdbc:mysql://localhost:3306/dbName"},
                {"COMMON_TAB_JDBC_USERNAME_FIELD",   "JDBC username"},
                {"COMMON_TAB_JDBC_PASSWORD_FIELD",   "JDBC password"},
                
                {"COMMON_TAB_TITLE_ERROR",           "Common tab: Name and Title fields are necessarily for filling"},
                {"COMMON_TAB_JDBC_ERROR",            "Common tab: JDBC name and JDBC URL should be filled when Type is SQL"},
                
                {"TAB_ADD_BUTTON",                   "Add"},
                {"TAB_REMOVE_BUTTON",                "Remove"},
                
                {"TYPES_TAB_DETAILS",                "Details"},
                {"TYPES_TAB_NAME",                   "Name"},
                {"TYPES_TAB_SECTION",                "Section"},
                {"TYPES_TAB_CLASS",                  "Class"},
                {"TYPES_TAB_TRANSFORMER",            "Transformer"},
                {"TYPES_TAB_IDFORMAT",               "ID format"},
                
                {"TYPES_TAB_VALUES_ERROR",           "Types tab: Name, Section, Class and Transformer fields are necessarily for filling for each type"},
                
                {"EXTERNAL_TAB_VALUES_ERROR",        "External tab: Collection, Database and Section fields are necessarily for filling for each external type"},
                
                {"NOTATIONS_TAB_VALUES_ERROR",       "Notations tab: Name and Type fields are necessarily for filling for each notation"},
                {"NOTATIONS_TAB_CLASS_ERROR",        "Notations tab: Class field is necessarily for filling for each notation with Java type"},
                {"NOTATIONS_TAB_PATH_ERROR",         "Notations tab: Path field is necessarily for filling for each notation with XML type"},
                {"NOTATIONS_TAB_LIST_ERROR",         "Notations tab: notation list should contains more than 0 items"},
        };
    }

    /**
     * Returns string from the resource bundle for the specified key.
     * If the sting is absent the key string is returned instead and
     * the message is printed in <code>java.util.logging.Logger</code> for the component.
     */
    public String getResourceString(String key)
    {
        try
        {
            return getString(key);
        }
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Missing resource <" + key + "> in " + this.getClass());
        }
        return key;
    }
}
