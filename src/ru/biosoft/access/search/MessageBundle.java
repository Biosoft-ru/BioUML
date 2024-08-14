package ru.biosoft.access.search;

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
        // Data search action
        { DataSearchAction.KEY    + Action.SMALL_ICON           , "search.gif"},
        { DataSearchAction.KEY    + Action.NAME                 , "Data search"},
        { DataSearchAction.KEY    + Action.SHORT_DESCRIPTION    , "<html>For selected table finds records<br>that satisfy to corresponding filter values."},
        { DataSearchAction.KEY    + Action.LONG_DESCRIPTION     , ""},
//        { DataSearchAction.KEY    + Action.MNEMONIC_KEY         , "S"},
//        { DataSearchAction.KEY    + Action.ACCELERATOR_KEY      , KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.CTRL_MASK) },
        { DataSearchAction.KEY    + Action.ACTION_COMMAND_KEY   , "cmd-database-search"},

        //DATA SEARCH
        {"DATA_SEARCH_TITLE", "Data search"},
        {"DATA_SEARCH_FILTER_TAB_NAME",  "Data filter"},
        {"DATA_SEARCH_COLUMNS_TAB_NAME", "Table columns"},

        {"BUTTON_SEARCH", "Search"},
        {"BUTTON_START", "Start"},
        {"BUTTON_CLOSE", "Close"},

        {"CN_FILTERING_SETTINGS", "Filtering settings"},
        {"CD_FILTERING_SETTINGS", "Filtering settings."},
        {"PN_FILTERING_SETTINGS_COLLECTION_NAME", "Collection"},
        {"PD_FILTERING_SETTINGS_COLLECTION_NAME", "Data collection name."},
        {"PN_FILTERING_SETTINGS_ELEMENT_NAME", "Name"},
        {"PD_FILTERING_SETTINGS_ELEMENT_NAME", "Data element name."},
        {"PN_FILTERING_SETTINGS_SCORE", "Score"},
        {"PD_FILTERING_SETTINGS_SCORE", "Score of search result."},
        {"PN_FILTERING_SETTINGS_NODE_TYPE", "Node type"},
        {"PD_FILTERING_SETTINGS_NODE_TYPE", "Node type"},

        {"PN_BEAN_VALUE_FILTER_FILTER",           "Filters"},
        {"PD_BEAN_VALUE_FILTER_FILTER",           "All filters."},
        {"PN_BEAN_VALUE_FILTER_FILTER_ENABLED",   "Enabled"},
        {"PD_BEAN_VALUE_FILTER_FILTER_ENABLED",   "Is this filter applied to data"}
    };
}


