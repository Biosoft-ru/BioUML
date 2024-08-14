package ru.biosoft.server.servlets.webservices;

import java.util.ListResourceBundle;
import java.util.ResourceBundle;

/**
 * @author lan
 */
public class MessageBundle extends ListResourceBundle
{
    private static final MessageBundle obj = new MessageBundle();
    
    @Override
    protected Object[][] getContents()
    {
        return new String[][] {
            // These messages indicate bug in BioUML server code, they cannot be displayed in normal situation
            // and the best thing user can do is contact support
            // On the web these messages will be wrapped into commonErrorInternalError
            {"EX_INTERNAL_STATE_CREATION", "Internal error: state cannot be created for $1"},
            {"EX_INTERNAL_DURING_ACTION", "Internal problem occured while doing the following action: $1"},
            {"EX_INTERNAL_INVALID_EDGE", "Internal error: edge has no path set: $1"},
            {"EX_INTERNAL_UPDATE_BEAN", "Internal error: bean update failed: $1"},
            {"EX_INTERNAL_READ_BEAN", "Internal error: cannot read bean: $1"},
            {"EX_INTERNAL_BEAN_NOT_FOUND", "Internal error: bean not found: $1"},
            {"EX_INTERNAL_LAYOUT_SAVE", "Internal error: image for layout cannot be generated"},
            {"EX_INTERNAL_CREATE_DIAGRAM_ELEMENT", "Internal error: cannot generate options dialog for new diagram element"},
            {"EX_INTERNAL_CREATE_SCRIPT", "Internal error: cannot create script: $1"},
            {"EX_INTERNAL_CUSTOM", "Internal error in $1: $2"},
        
            // These messages indicate bug in JavaScript code, they cannot be displayed in normal situation
            // and the best thing user can do is contact support
            // On the web these messages will be wrapped into commonErrorInvalidQuery
            {"EX_QUERY_PARAM_MISSING", "Invalid query: parameter '$1' must be given"},
            {"EX_QUERY_PARAM_MISSING_BOTH", "Invalid query: either parameter '$1' or '$2' must be given"},
            {"EX_QUERY_PARAM_INVALID", "Invalid query: parameter '$1' has an invalid format"},
            {"EX_QUERY_PARAM_INVALID_VALUE", "Invalid query: parameter '$1' has an invalid value"},
            {"EX_QUERY_PARAM_NO_JSON", "Invalid query: parameter '$1' must be in JSON format"},
            {"EX_QUERY_PARAM_OFFSET_MISSING", "Invalid query: parameters 'x' and 'y' are missing or equal to zero"},
            {"EX_QUERY_PARAM_ELEMENTS_MISSING", "Invalid query: elements list is empty"},
            {"EX_QUERY_PARAM_NO_SERVICE", "Unknown service requested: $1"},
            {"EX_QUERY_PARAM_NO_JOB", "Supplied job is not found: $1"},
        
            // These messages might indicate a bug, but also can be displayed in normal situation
            // like concurrent changes of the same data by different users
            {"EX_QUERY_NO_ELEMENT", "Cannot find data element: $1"},
            {"EX_QUERY_NO_ELEMENT_TYPE", "Cannot find $2: $1"},
            {"EX_QUERY_INVALID_ELEMENT_TYPE", "Element $1 has invalid type ($2 expected)"},
            {"EX_QUERY_NO_ACTION", "Cannot find action: $1"},
            {"EX_QUERY_UNSUPPORTED_ELEMENT", "Data element has an invalid or unsupported type: $1"},
            {"EX_QUERY_UNSUPPORTED_DIAGRAM", "Diagram is not applicable for this action: $1"},
            {"EX_QUERY_NO_DIAGRAM", "Cannot find diagram: $1"},
            {"EX_QUERY_NO_OPTIMIZATION", "Cannot find optimization: $1"},
            {"EX_QUERY_NO_TABLE", "Cannot find table: $1"},
            {"EX_QUERY_NO_TABLE_RESOLVED", "Cannot resolve table $1: $2"},
            {"EX_QUERY_INVALID_TABLE_TYPE", "Table $1 has non-applicable type for this diagram '$2' (type '$3' is expected). To convert a table to the expected type, please refer to 'Convert table' analysis."},
            {"EX_QUERY_NO_COLUMNS", "Please specify proper columns"},
            {"EX_QUERY_NOT_REAL_TABLE", "Element is not real table: $1"},
            {"EX_QUERY_NOT_EDGE", "Element is not an edge: $1"},
            {"EX_QUERY_INVALID_VERSION", "Specified version for $1 not found: $2"},
            {"EX_QUERY_FILTER_NOT_FOUND", "There's no mapping '$1'."},
            {"EX_QUERY_LAYOUTER_NOT_FOUND", "There's no layouter '$1'."},
            {"EX_QUERY_EMPTY_BRANCH", "Selected branch is empty: $1"},
            {"EX_QUERY_NO_EXPORT_ELEMENT", "Invalid element was specified to export: $1"},
            {"EX_QUERY_COPY_NOT_SUPPORTED", "Element doesn't support copying: $1"},
            {"EX_QUERY_SEARCH_NOT_SUPPORTED", "Search is not available for $1"},
            {"EX_QUERY_NO_SEARCH", "No previous search result"},
            {"EX_QUERY_CUSTOM_MESSAGE", "$1"},
            {"EX_QUERY_ELEMENT_EXIST", "Element $1 already exist in $2. Use copy action to duplicate element."},
            
            // These messages can appear when user input is invalid
            {"EX_INPUT_NAME_EMPTY", "Name should contain at least one character"},
            {"EX_INPUT_NAME_INVALID", "Name '$1' contains invalid characters like '/'"},
            {"EX_INPUT_FILTER_ERROR", "Filter compilation error: $1"},
            
            // These messages are displayed when user asked for action which cannot be performed for some reason
            // (either security limitation, or it's logically incorrect)
            {"EX_ACCESS_CANNOT_RESIZE", "Element is not resizable: $1"},
            {"EX_ACCESS_CANNOT_COPY", "Cannot copy element $1 to $2: $3"},
            {"EX_ACCESS_FILTER_EXISTS", "Mapping '$1' already exists. Please enter another name."},
            {"EX_ACCESS_READ_ONLY", "Element is read-only: $1"},
            {"EX_ACCESS_CANNOT_SAVE", "Cannot save to target collection $1: $2"},
            {"EX_ACCESS_INVALID_LOGIN", "Unable to login: $1"},
            {"EX_ACCESS_CANNOT_REMOVE_COLUMN", "Cannot remove column from $1"},
            {"EX_ACCESS_CANNOT_UPDATE", "Cannot update $1: $2"},
        };
    }
    
    public static ResourceBundle getInstance()
    {
        return obj;
    }
}
