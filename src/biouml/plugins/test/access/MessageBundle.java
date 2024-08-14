package biouml.plugins.test.access;

import java.util.ListResourceBundle;

import javax.swing.Action;

public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents() { return contents; }
    private final static Object[][] contents =
    {
//        // actions
//        { NewTestDocumentAction.KEY  + Action.SMALL_ICON           , "newtest.gif"},
//        { NewTestDocumentAction.KEY  + Action.NAME                 , "New test document"},
//        { NewTestDocumentAction.KEY  + Action.SHORT_DESCRIPTION    , "Creates a new test document"},
//        { NewTestDocumentAction.KEY  + Action.LONG_DESCRIPTION     , "Creates a new test document for the selected data collection"},
//        { NewTestDocumentAction.KEY  + Action.ACTION_COMMAND_KEY   , "cmd-new-test"},
    
        // NewTestDocumentDialog
        { "NEW_TEST_NAME" ,  "Name: "},
        { "NEW_TEST_CLICK",  "(click to enter new test path)"},
        { "NEW_MODEL_PATH" , "Model"},
        { "NEW_MODEL_CLICK", "(click to enter model path)"},
        { "WARN_INCORRECT_MODEL",  "Diagram {0} is incorrect or does not contain a model. Please choose another diagram."},
        { "ERROR_TEST_CREATION",   "Cannot create test document"}
    };
}
