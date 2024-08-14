package biouml.workbench.graphsearch;

import java.util.ListResourceBundle;

import javax.swing.Action;

import biouml.workbench.graphsearch.actions.AddDiagramElementAction;
import biouml.workbench.graphsearch.actions.AddElementsToDiagramAction;
import biouml.workbench.graphsearch.actions.CleanAction;
import biouml.workbench.graphsearch.actions.CreateResultDiagramAction;
import biouml.workbench.graphsearch.actions.SearchAction;

/**
 * Constants for graph search package
 */
public class MessageBundle extends ListResourceBundle
{
    protected String[] directionTypes  = {"Up", "Down", "Both"};
    
    @Override
    protected Object[][] getContents()
    {
        return new Object[][] {
              //----- SearchElement constants ----------------------------------/
            {"CN_SEARCH_ELEMENT", "Element info"},
            {"CD_SEARCH_ELEMENT", "Element info"},
        
            {"PN_SEARCH_ELEMENT_ADD", "Add"},
            {"PD_SEARCH_ELEMENT_ADD", "Add to diagram"},
            
            {"PN_SEARCH_ELEMENT_USE", "Use"},
            {"PD_SEARCH_ELEMENT_USE", "Use as input elements for search"},
        
            {"PN_SEARCH_ELEMENT_BASE_NAME", "Id"},
            {"PD_SEARCH_ELEMENT_BASE_NAME", "Node id"},
        
            {"PN_SEARCH_ELEMENT_BASE_TITLE", "Title"},
            {"PD_SEARCH_ELEMENT_BASE_TITLE", "Title."},
        
            {"PN_SEARCH_ELEMENT_BASE_TYPE", "Type"},
            {"PD_SEARCH_ELEMENT_BASE_TYPE", "Type."},
            
            //----- QueryOptions constants ----------------------------------/
            {"PN_QUERY_OPTIONS_DIRECTION", "Direction"},
            {"PD_QUERY_OPTIONS_DIRECTION", "The direction of search"},
            {"PN_QUERY_OPTIONS_DEPTH", "Depth"},
            {"PD_QUERY_OPTIONS_DEPTH", "The depth of search"},
            {"DIRECTION_TYPES", directionTypes},
            
            //----- GraphSearchOptions constants ----------------------------------/
            {"PN_QUERY_TYPE", "Search type"},
            {"PD_QUERY_TYPE", "Graph search type"},
            {"PN_QUERY_OPTIONS", "Options"},
            {"PD_QUERY_OPTIONS", "Search options"},
            {"PN_TARGET_OPTIONS", "Target databases"},
            {"PD_TARGET_OPTIONS", "List of target databases"},
        
            //--- GraphSearchPane constants ------------------------------/
        
            {"BUTTON_SEARCH",           "Search"},
            {"BUTTON_SEARCH_ICON",      "search.gif"},
            {"BUTTON_ADD",              "Add"},
            {"BUTTON_ADD_ICON",         "add.gif"},
            {"BUTTON_ADD_ALL",          "Add all"},
            {"BUTTON_ADD_ALL_ICON",     "addAll.gif"},
            
            {"BUTTON_OPEN",             "Open"},
            
            {"ERROR_WRONG_DATABASE",      "Graph search cannot be applied to this database."},
            {"ERROR_CANNOT_FIND",         "Cannot find."},
            
            {"INCREMENTAL_SEARCH_TAB",    "Incremental search"},
            {"CLIPBOARD_TAB",             "Clipboard"},
            
            {"SEARCH_TAB",    "Search options"},
            {"LAYOUT_TAB",    "Layout options"},
            
            //--- AddDiagramElementAction  constants ------------------------------/
            { AddDiagramElementAction.KEY      + Action.SMALL_ICON           , "add.gif"},
            { AddDiagramElementAction.KEY      + Action.NAME                 , "Add input element"},
            { AddDiagramElementAction.KEY      + Action.SHORT_DESCRIPTION    , "Add selected diagram element to input elements"},
            { AddDiagramElementAction.KEY      + Action.LONG_DESCRIPTION     , "Add selected diagram element to input elements"},
            { AddDiagramElementAction.KEY      + Action.ACTION_COMMAND_KEY   , "cmd-graphsearch-adddiagramelement"},
            
            //--- SearchAction constants ------------------------------/
            { SearchAction.KEY      + Action.SMALL_ICON           , "search.gif"},
            { SearchAction.KEY      + Action.NAME                 , "Start search"},
            { SearchAction.KEY      + Action.SHORT_DESCRIPTION    , "Start graph search process"},
            { SearchAction.KEY      + Action.LONG_DESCRIPTION     , "Start graph search process"},
            { SearchAction.KEY      + Action.ACTION_COMMAND_KEY   , "cmd-graphsearch-search"},
            
            //--- AddElementsToDiagramAction constants ------------------------------/
            { AddElementsToDiagramAction.KEY      + Action.SMALL_ICON           , "addToDiagram.gif"},
            { AddElementsToDiagramAction.KEY      + Action.NAME                 , "Show elements"},
            { AddElementsToDiagramAction.KEY      + Action.SHORT_DESCRIPTION    , "Add elements to diagram"},
            { AddElementsToDiagramAction.KEY      + Action.LONG_DESCRIPTION     , "Add elements to diagram"},
            { AddElementsToDiagramAction.KEY      + Action.ACTION_COMMAND_KEY   , "cmd-graphsearch-addtodiagram"},
            {"CONFIRM_CREATE_DIAGRAM", "Do you want to create new diagram document for results?"},
            {"CONFIRM_USE_CURRENT_DIAGRAM", "Do you want to use current opened diagram to display search results?"},
            
            //--- CreateResultDiagramAction constants ------------------------------/
            { CreateResultDiagramAction.KEY      + Action.SMALL_ICON           , "createDiagram.gif"},
            { CreateResultDiagramAction.KEY      + Action.NAME                 , "New diagram"},
            { CreateResultDiagramAction.KEY      + Action.SHORT_DESCRIPTION    , "Create new diagram for search results"},
            { CreateResultDiagramAction.KEY      + Action.LONG_DESCRIPTION     , "Create new diagram for search results"},
            { CreateResultDiagramAction.KEY      + Action.ACTION_COMMAND_KEY   , "cmd-graphsearch-newdiagram"},
            
            //--- CleanDiagramAction constants ------------------------------/
            { CleanAction.KEY      + Action.SMALL_ICON           , "clean.gif"},
            { CleanAction.KEY      + Action.NAME                 , "Clean search pane"},
            { CleanAction.KEY      + Action.SHORT_DESCRIPTION    , "Remove all results"},
            { CleanAction.KEY      + Action.LONG_DESCRIPTION     , "Remove all results"},
            { CleanAction.KEY      + Action.ACTION_COMMAND_KEY   , "cmd-graphsearch-clean"},
        };
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
}
