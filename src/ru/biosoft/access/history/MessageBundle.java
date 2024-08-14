package ru.biosoft.access.history;

import java.util.ListResourceBundle;

import javax.swing.Action;

import ru.biosoft.access.history.gui.HistoryPane;

public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents() { return contents; }
    private final static Object[][] contents =
    {
        // HistoryElement
        { "CN_HISTORY" ,  "History element"},
        { "CD_HISTORY",   "History info for data element"},
        { "PN_HISTORY_TIMESTAMP" ,  "Date"},
        { "PD_HISTORY_TIMESTAMP",   "Date of change"},
        { "PN_HISTORY_VERSION",     "Version"},
        { "PD_HISTORY_VERSION",     "Element version"},
        { "PN_HISTORY_AUTHOR",      "Author"},
        { "PD_HISTORY_AUTHOR",      "Change author"},
        { "PN_HISTORY_COMMENT",     "Comment"},
        { "PD_HISTORY_COMMENT",     "Change comment"},
        
        // Restore history
        {HistoryPane.RESTORE + Action.SMALL_ICON, "cancel.gif"},
        {HistoryPane.RESTORE + Action.NAME, "Restore"},
        {HistoryPane.RESTORE + Action.SHORT_DESCRIPTION, "Restore current object."},
        {HistoryPane.RESTORE + Action.ACTION_COMMAND_KEY, "cmd-restore"},
    };
}
