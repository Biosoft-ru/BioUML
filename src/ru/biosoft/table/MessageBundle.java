package ru.biosoft.table;

import java.awt.Event;
import java.awt.event.KeyEvent;
import java.util.ListResourceBundle;

import javax.swing.Action;
import javax.swing.KeyStroke;

import ru.biosoft.gui.RedoAction;
import ru.biosoft.gui.UndoAction;
import ru.biosoft.table.document.editors.ColumnsViewPane;
import ru.biosoft.table.document.editors.GroupsViewPane;
import ru.biosoft.table.document.editors.SamplesViewPane;


public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return new Object[][] {
                //--- TableDataCollection constants ---------------------------------------/
                {"CN_TABLE_DC", "Table Data collection"},
                {"CD_TABLE_DC", "Table Data collection."},
        
                {"PN_TABLE_DC_NAME", "Name"},
                {"PD_TABLE_DC_NAME", "Data collection name."},
                {"PN_TABLE_DC_SIZE", "Size"},
                {"PD_TABLE_DC_SIZE", "Number of data elements in data collection."},
                {"PN_TABLE_DC_DESCRIPTION", "Description"},
                {"PD_TABLE_DC_DESCRIPTION", "Data collection description."},
                {"PN_TABLE_SUB_TYPE", "Contents type"},
                {"PD_TABLE_SUB_TYPE", "Type of table contents."},
        
                //--- Data Collection Names -----------------------------------------------/
                {"MICROARRAY_DC_NAME", "microarray"},
                {"MICROARRAY_RESULTS_DC_NAME", "microarray results"},
                {"OPT_FITTED_VALUES_DC_NAME", "Fitted values"},
        
                //--- Errors, warns and info ----------------------------------------------/
                {"CONFIRM_REMOVE_ELEMENT", "Are you sure want to remove element \"{0}\" from collection \"{1}\"?"},
        
                {"INFO_TABLE_NAME_ENTERING", "Enter table name"},
                {"WARN_TABLE_EXISTENCE", "Table with name {0} already exists. Do you realy want to rewrite it?"},
                {"ERROR_TABLE_CREATION", "Table creation error"},
        
                //--- Column editor tab constants
                {"CN_COLUMN_EDITOR", "Column"},
                {"CD_COLUMN_EDITOR", "Column"},
                {"PN_COLUMN_EDITOR_ROW_NUMBER", "#"},
                {"PD_COLUMN_EDITOR_ROW_NUMBER", "Column pos"},
                {"PN_COLUMN_EDITOR_COLUMN_NAME", "Column name"},
                {"PD_COLUMN_EDITOR_COLUMN_NAME", "Column name"},
                {"PN_COLUMN_EDITOR_COLUMN_TYPE", "Type"},
                {"PD_COLUMN_EDITOR_COLUMN_TYPE", "Column type"},
                {"PN_COLUMN_EDITOR_COLUMN_NATURE", "Nature"},
                {"PD_COLUMN_EDITOR_COLUMN_NATURE", "Column nature"},
                {"PN_COLUMN_EDITOR_COLUMN_DESCRIPTION", "Description"},
                {"PD_COLUMN_EDITOR_COLUMN_DESCRIPTION", "Description"},
                {"PN_COLUMN_EDITOR_COLUMN_EXPRESSION", "Expression"},
                {"PD_COLUMN_EDITOR_COLUMN_EXPRESSION", "Expression"},
                {"PN_COLUMN_EDITOR_COLUMN_VISIBLE", "Visible"},
                {"PD_COLUMN_EDITOR_COLUMN_VISIBLE", "Visible"},
        
                //--- Groups editor tab constants
                {"CN_GROUP_EDITOR", "Group"},
                {"CD_GROUP_EDITOR", "Group"},
                {"PN_GROUP_EDITOR_GROUP", "Group"},
                {"PD_GROUP_EDITOR_GROUP", "Group name"},
                {"PN_GROUP_EDITOR_DESCRIPTION", "Description"},
                {"PD_GROUP_EDITOR_DESCRIPTION", "Description"},
                {"PN_GROUP_EDITOR_SAMPLES", "Samples"},
                {"PD_GROUP_EDITOR_SAMPLES", "Samples"},
        
                //---Import properties
                {"PN_IMPORT_SPECIES", "Species"},
                {"PD_IMPORT_SPECIES", "Species associated with given data set"},
        
                //--- Actions -------------------------------------------------------------/
                {ColumnsViewPane.ADD_COLUMN_ACTION + Action.SMALL_ICON, "add.gif"},
                {ColumnsViewPane.ADD_COLUMN_ACTION + Action.NAME, "Add Column"},
                {ColumnsViewPane.ADD_COLUMN_ACTION + Action.SHORT_DESCRIPTION, "Add Column"},
                {ColumnsViewPane.ADD_COLUMN_ACTION + Action.ACTION_COMMAND_KEY, "cmd-add-column"},
        
                {ColumnsViewPane.REMOVE_COLUMN_ACTION + Action.SMALL_ICON, "remove.gif"},
                {ColumnsViewPane.REMOVE_COLUMN_ACTION + Action.NAME, "Remove Column"},
                {ColumnsViewPane.REMOVE_COLUMN_ACTION + Action.SHORT_DESCRIPTION, "Remove Column"},
                {ColumnsViewPane.REMOVE_COLUMN_ACTION + Action.ACTION_COMMAND_KEY, "cmd-remove-column"},
        
                {ColumnsViewPane.RECALCULATE_DOCUMENT_ACTION + Action.SMALL_ICON, "refresh.gif"},
                {ColumnsViewPane.RECALCULATE_DOCUMENT_ACTION + Action.NAME, "Recalculate"},
                {ColumnsViewPane.RECALCULATE_DOCUMENT_ACTION + Action.SHORT_DESCRIPTION, "Recalculate document"},
                {ColumnsViewPane.RECALCULATE_DOCUMENT_ACTION + Action.ACTION_COMMAND_KEY, "cmd-recalculate"},
        
                {GroupsViewPane.ADD_COLUMN_ACTION + Action.SMALL_ICON, "add.gif"},
                {GroupsViewPane.ADD_COLUMN_ACTION + Action.NAME, "Add Column"},
                {GroupsViewPane.ADD_COLUMN_ACTION + Action.SHORT_DESCRIPTION, "Add Column"},
                {GroupsViewPane.ADD_COLUMN_ACTION + Action.ACTION_COMMAND_KEY, "cmd-add-column"},
        
                {GroupsViewPane.REMOVE_COLUMN_ACTION + Action.SMALL_ICON, "remove.gif"},
                {GroupsViewPane.REMOVE_COLUMN_ACTION + Action.NAME, "Remove Column"},
                {GroupsViewPane.REMOVE_COLUMN_ACTION + Action.SHORT_DESCRIPTION, "Remove Column"},
                {GroupsViewPane.REMOVE_COLUMN_ACTION + Action.ACTION_COMMAND_KEY, "cmd-remove-column"},
        
                {SamplesViewPane.ADD_COLUMN_ACTION + Action.SMALL_ICON, "add.gif"},
                {SamplesViewPane.ADD_COLUMN_ACTION + Action.NAME, "Add Column"},
                {SamplesViewPane.ADD_COLUMN_ACTION + Action.SHORT_DESCRIPTION, "Add Column"},
                {SamplesViewPane.ADD_COLUMN_ACTION + Action.ACTION_COMMAND_KEY, "cmd-add-column"},
        
                {SamplesViewPane.REMOVE_COLUMN_ACTION + Action.SMALL_ICON, "remove.gif"},
                {SamplesViewPane.REMOVE_COLUMN_ACTION + Action.NAME, "Remove Column"},
                {SamplesViewPane.REMOVE_COLUMN_ACTION + Action.SHORT_DESCRIPTION, "Remove Column"},
                {SamplesViewPane.REMOVE_COLUMN_ACTION + Action.ACTION_COMMAND_KEY, "cmd-remove-column"},
        
                // Undo action
                {UndoAction.KEY + Action.SMALL_ICON, "undo.gif"},
                {UndoAction.KEY + Action.NAME, "Undo"},
                {UndoAction.KEY + Action.SHORT_DESCRIPTION, "Undo"},
                {UndoAction.KEY + Action.LONG_DESCRIPTION, "Undo"},
                {UndoAction.KEY + Action.MNEMONIC_KEY, KeyEvent.VK_Z},
                {UndoAction.KEY + Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Z, Event.CTRL_MASK)},
                {UndoAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-undo"},
        
                // Redo action
                {RedoAction.KEY + Action.SMALL_ICON, "redo.gif"}, {RedoAction.KEY + Action.NAME, "Redo"},
                {RedoAction.KEY + Action.SHORT_DESCRIPTION, "Redo"}, {RedoAction.KEY + Action.LONG_DESCRIPTION, "Redo"},
                {RedoAction.KEY + Action.MNEMONIC_KEY, KeyEvent.VK_Y},
                {RedoAction.KEY + Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Y, Event.CTRL_MASK)},
                {RedoAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-redo"},
        
                //Expression dialog
                {"FILTER_COLUMNS", "Column names (double-click to select)"},};
    }

    public String getResourceString(String key)
    {
        try
        {
            return getString(key);
        }
        catch( Throwable th )
        {

        }
        return key;
    }
}