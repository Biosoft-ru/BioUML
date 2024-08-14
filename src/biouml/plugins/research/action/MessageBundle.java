// $ Id: $
package biouml.plugins.research.action;

import java.util.ListResourceBundle;

import javax.swing.Action;

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
        {"NEW_RESEARCH_DIALOG_TITLE",   "New Project"},
        {"NEW_RESEARCH_NAME",           "Project name"},
        {"NEW_RESEARCH_ERROR_TITLE",    "New project error"},
        {"NEW_RESEARCH_ERROR", "<html>Can not create new project {0}." + "<br>Error: {2}.</html>"},
        {"NEW_RESEARCH_EXISTS", "Project with the same name already exists"},
        {"JDBC_SQL_FIELD",        "SQL connection"},
        {"JDBC_DEFAULT_DRIVER",   "com.mysql.jdbc.Driver"},
        
        // New project menu action
        { NewProjectAction.KEY      + Action.SMALL_ICON           , "newResearch.gif"},
        { NewProjectAction.KEY      + Action.NAME                 , "New project"},
        { NewProjectAction.KEY      + Action.SHORT_DESCRIPTION    , "New project"},
        { NewProjectAction.KEY      + Action.LONG_DESCRIPTION     , "Creates new project"},
        { NewProjectAction.KEY      + Action.ACTION_COMMAND_KEY   , "cmd-new-research"},
        
        // NewResearchPane constants
        {"CN_COLLECTIONRECORD",         "Default collection"},
        {"CD_COLLECTIONRECORD",         "Default collection"},
        {"PN_COLLECTIONRECORD_NAME",    "Collection name"},
        {"PD_COLLECTIONRECORD_NAME",    "Collection name"},
        {"PN_COLLECTIONRECORD_USED",    "Create"},
        {"PD_COLLECTIONRECORD_USED",    "Indicates if collection should be created"},
        
        {"IMPORT_RESEARCH_DIALOG_TITLE", "Load Project"},
        
     // New project menu action
        { ImportResearchAction.KEY      + Action.SMALL_ICON           , "loadResearch.gif"},
        { ImportResearchAction.KEY      + Action.NAME                 , "Load project"},
        { ImportResearchAction.KEY      + Action.SHORT_DESCRIPTION    , "Load project from server"},
        { ImportResearchAction.KEY      + Action.LONG_DESCRIPTION     , "Load project from server"},
        { ImportResearchAction.KEY      + Action.ACTION_COMMAND_KEY   , "cmd-import-research"},
    };
}
