package ru.biosoft.access.support;

import java.util.ListResourceBundle;

import javax.swing.Action;

/**
 *
 * @pending description for RE
 */
public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents() { return contents; }
    
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

    private Object[][] contents =
    {
      //--- Actions properties -------------------------------------------------/
        { DataCollectionMultyChoicePane.ADD               + Action.SMALL_ICON            , "add.gif"},
        { DataCollectionMultyChoicePane.ADD               + Action.NAME                  , "Add"},
        { DataCollectionMultyChoicePane.ADD               + Action.SHORT_DESCRIPTION     , "Add selected reference."},
        { DataCollectionMultyChoicePane.ADD               + Action.ACTION_COMMAND_KEY    , "cmd-add"},
        
        { DataCollectionMultyChoicePane.REMOVE            + Action.SMALL_ICON            , "remove.gif"},
        { DataCollectionMultyChoicePane.REMOVE            + Action.NAME                  , "Remove"},
        { DataCollectionMultyChoicePane.REMOVE            + Action.SHORT_DESCRIPTION     , "Remove selected reference."},
        { DataCollectionMultyChoicePane.REMOVE            + Action.ACTION_COMMAND_KEY    , "cmd-remove"},
        
        { DataCollectionMultyChoicePane.REMOVE_ALL        + Action.SMALL_ICON            , "removeAll.gif"},
        { DataCollectionMultyChoicePane.REMOVE_ALL        + Action.NAME                  , "Remove all"},
        { DataCollectionMultyChoicePane.REMOVE_ALL        + Action.SHORT_DESCRIPTION     , "Remove all references."},
        { DataCollectionMultyChoicePane.REMOVE_ALL        + Action.ACTION_COMMAND_KEY    , "cmd-removeAll"},
        
        { DataCollectionMultyChoicePane.NEW_ELEMENT       + Action.SMALL_ICON            , "new.gif"},
        { DataCollectionMultyChoicePane.NEW_ELEMENT       + Action.NAME                  , "New"},
        { DataCollectionMultyChoicePane.NEW_ELEMENT       + Action.SHORT_DESCRIPTION     , "Create new element."},
        { DataCollectionMultyChoicePane.NEW_ELEMENT       + Action.ACTION_COMMAND_KEY    , "cmd-new"},

      // --- Properties --------------------------------------------------------/
        {"PN_FILTER",           "Filters"},
        {"PD_FILTER",           "All filters."},
        {"PN_FILTER_ENABLED",   "Enabled"},
        {"PD_FILTER_ENABLED",   "Is this filter applied to data"},
    };
}
