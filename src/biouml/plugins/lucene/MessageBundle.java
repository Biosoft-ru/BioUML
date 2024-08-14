package biouml.plugins.lucene;

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
        // Lucene search action
        { LuceneSearchAction.KEY    + Action.SMALL_ICON           , "search.gif"},
        { LuceneSearchAction.KEY    + Action.NAME                 , "Text search"},
        { LuceneSearchAction.KEY    + Action.SHORT_DESCRIPTION    , "<html>For selected table finds entities<br>that satisfy to corresponding query string."},
        { LuceneSearchAction.KEY    + Action.LONG_DESCRIPTION     , ""},
//        { LuceneSearchAction.KEY    + Action.MNEMONIC_KEY         , "t"},
//        { LuceneSearchAction.KEY    + Action.ACCELERATOR_KEY      , KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.SHIFT_MASK) },
        { LuceneSearchAction.KEY    + Action.ACTION_COMMAND_KEY   , "cmd-text-search"},

        // Lucene index editor action
        { IndexEditorAction.KEY    + Action.SMALL_ICON           , "index.gif"},
        { IndexEditorAction.KEY    + Action.NAME                 , "Index editor"},
        { IndexEditorAction.KEY    + Action.SHORT_DESCRIPTION    , "<html>Edit list of indexes."},
        { IndexEditorAction.KEY    + Action.LONG_DESCRIPTION     , ""},
//        { IndexEditorAction.KEY    + Action.MNEMONIC_KEY         , "i"},
//        { IndexEditorAction.KEY    + Action.ACCELERATOR_KEY      , KeyStroke.getKeyStroke(KeyEvent.VK_I, Event.SHIFT_MASK) },
        { IndexEditorAction.KEY    + Action.ACTION_COMMAND_KEY   , "cmd-index-editor"},

        // Lucene index editor action
        { RebuildIndexAction.KEY    + Action.SMALL_ICON           , "rebuild.gif"},
        { RebuildIndexAction.KEY    + Action.NAME                 , "Rebuild index"},
        { RebuildIndexAction.KEY    + Action.SHORT_DESCRIPTION    , "<html>Rebuild index."},
        { RebuildIndexAction.KEY    + Action.LONG_DESCRIPTION     , ""},
//        { RebuildIndexAction.KEY    + Action.MNEMONIC_KEY         , "r"},
//        { RebuildIndexAction.KEY    + Action.ACCELERATOR_KEY      , KeyStroke.getKeyStroke(KeyEvent.VK_R, Event.SHIFT_MASK) },
        { RebuildIndexAction.KEY    + Action.ACTION_COMMAND_KEY   , "cmd-index-editor"},

        // Add element action
        { AddElementAction.KEY    + Action.SMALL_ICON           , "add.gif"},
        { AddElementAction.KEY    + Action.NAME                 , "Add on the diagram"},
        { AddElementAction.KEY    + Action.SHORT_DESCRIPTION    , "<html>Add new element on the diagram."},
        { AddElementAction.KEY    + Action.LONG_DESCRIPTION     , ""},
//        { AddElementAction.KEY    + Action.MNEMONIC_KEY         , "a"},
//        { AddElementAction.KEY    + Action.ACCELERATOR_KEY      , KeyStroke.getKeyStroke(KeyEvent.VK_A, Event.SHIFT_MASK) },
        { AddElementAction.KEY    + Action.ACTION_COMMAND_KEY   , "cmd-lucene-search-add"},
        
        // Export table action
        { ExportTableAction.KEY    + Action.SMALL_ICON           , "export.gif"},
        { ExportTableAction.KEY    + Action.NAME                 , "Export results"},
        { ExportTableAction.KEY    + Action.SHORT_DESCRIPTION    , "<html>Export search result table"},
        { ExportTableAction.KEY    + Action.LONG_DESCRIPTION     , ""},
        { ExportTableAction.KEY    + Action.ACTION_COMMAND_KEY   , "cmd-lucene-search-export"},
        
        // Export table action
        { LuceneSearchViewPart.FULL_MODE    + Action.SMALL_ICON           , "fullMode.gif"},
        { LuceneSearchViewPart.FULL_MODE    + Action.SMALL_ICON + "2"     , "fullMode2.gif"},
        { LuceneSearchViewPart.FULL_MODE    + Action.NAME                 , "Full mode"},
        { LuceneSearchViewPart.FULL_MODE    + Action.SHORT_DESCRIPTION    , "<html>Set full mode"},
        { LuceneSearchViewPart.FULL_MODE    + Action.SHORT_DESCRIPTION + "2", "<html>Set short mode"},
        { LuceneSearchViewPart.FULL_MODE    + Action.LONG_DESCRIPTION     , ""},
        { LuceneSearchViewPart.FULL_MODE    + Action.ACTION_COMMAND_KEY   , "cmd-lucene-search-export"},
        
        // Copy to clipboard action
        { LuceneSearchViewPart.COPY_CLIPBOARD    + Action.SMALL_ICON           , "copy.gif"},
        { LuceneSearchViewPart.COPY_CLIPBOARD    + Action.NAME                 , "Copy to clipboard"},
        { LuceneSearchViewPart.COPY_CLIPBOARD    + Action.SHORT_DESCRIPTION    , "<html>Copy selected element to clipboard"},
        { LuceneSearchViewPart.COPY_CLIPBOARD    + Action.LONG_DESCRIPTION     , ""},
        
        // LUCENE SEARCH
        {"LUCENE_SEARCH_TITLE", "Text search"},
        {"LUCENE_INDEX_EDITOR_TITLE", "Index editor"},
        {"LUCENE_REBUILD_INDEX_TITLE", "Rebuild index"},
        {"LUCENE_REBUILD_INDEX_PROGRESS_TITLE", "Rebuild index"},
        
        {"LUCENE_DIALOG_REBUILD_INDEX_QUESTION", "Do you want to rebuild index?"},
        
        {"LUCENE_DIALOG_REBUILD_INDEX_ONLY_CHANGED", "Only changed"},
        
        
        {"LUCENE_VIEW_PART_ADD_ACTION_NAME", "Add"},
        {"LUCENE_VIEW_PART_ADD_ACTION_SHORT_DESCRIPTION", "Add selected element"},
        
        {"LUCENE_DATA_COLLECTION_COOSER_NAME", "Data collection:"},
        {"LUCENE_PROPERTIES_LIST_NAME", "Data collection properties:"},
        {"LUCENE_INDEXES", "Indexes:"},
        {"LUCENE_SEARCH", "Search:"},
        {"LUCENE_ALTRNATIVE_VIEW_CONDITION", "Alternative view:"},
        {"LUCENE_VARIABLE_ROW_HEIGHT", "Variable row height:"},
        
        {"ADDING_NEW_DIAGRAM_ELEMENT_ERROR_TITLE", "Error"},
        {"ADDING_NEW_DIAGRAM_ELEMENT_ERROR", "The selected object cannot added."},
        
        {"LUCENE_TAB_NAME",  "Search"},
        {"LUCENE_AVAIBLE_PROPERTIES_TAB_NAME",  "Available properties"},
        {"LUCENE_COLUMNS_TAB_NAME", "Columns"},
        {"LUCENE_INDEX_TAB_NAME", "Have index"},
        {"LUCENE_RESULTS_TAB_NAME", "Search results"},
        
        {"LUCENE_EMPTY_DATABASE", "Empty database."},
        {"LUCENE_EMPTY_LUCENE_DIR", "Empty lucene directory."},
                
                
        {"BUTTON_ADD", "Add"},
        {"BUTTON_REMOVE", "Remove"},
                
        {"BUTTON_SAVE", "Save"},
        {"BUTTON_UPDATE_INDEX", "Update Index"},
        {"BUTTON_CANCEL", "Cancel"},
        
        {"BUTTON_SEARCH", "Search"},
        {"BUTTON_START", "Start"},
        {"BUTTON_CLOSE", "Close"},

        {"COLUMN_FULL_NAME", "Path"},
        {"COLUMN_ELEMENT_NAME", "Name"},
        {"COLUMN_SCORE", "Score"},
        {"COLUMN_FIELD_NAME", "Field"},
        {"COLUMN_FIELD_DATA", "Field data"},
        {"COLUMN_TITLE", "Title"},
      
        {"COLUMN_FIND_NOTHING", "Nothing Was Found"},
        
        {"LUCENE_PARSE_EXCEPTION", "Lucene parse error."},
        
        {"LUCENE_INFO_TEXT", "<html>Search <b>{0}</b> in <b>{1}</b>"},
    };
}


