package biouml.plugins.test.editors;

import java.util.ListResourceBundle;

import javax.swing.Action;

public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents() { return contents; }
    private final static Object[][] contents =
    {
        { TestViewPart.CREATE_SUITE                 + Action.SMALL_ICON           , "createSuite.gif"},
        { TestViewPart.CREATE_SUITE                 + Action.NAME                 , "Create"},
        { TestViewPart.CREATE_SUITE                 + Action.SHORT_DESCRIPTION    , "Create new test suite."},
        { TestViewPart.CREATE_SUITE                 + Action.ACTION_COMMAND_KEY   , "cmd-create"},
        
        { TestViewPart.CREATE_TEST                  + Action.SMALL_ICON           , "createTest.gif"},
        { TestViewPart.CREATE_TEST                  + Action.NAME                 , "Add test"},
        { TestViewPart.CREATE_TEST                  + Action.SHORT_DESCRIPTION    , "Create test in current suite."},
        { TestViewPart.CREATE_TEST                  + Action.ACTION_COMMAND_KEY   , "cmd-create-test"},
        
        { TestViewPart.REMOVE_TEST                  + Action.SMALL_ICON           , "remove.gif"},
        { TestViewPart.REMOVE_TEST                  + Action.NAME                 , "Remove test"},
        { TestViewPart.REMOVE_TEST                  + Action.SHORT_DESCRIPTION    , "Remove test or test suite from document"},
        { TestViewPart.REMOVE_TEST                  + Action.ACTION_COMMAND_KEY   , "cmd-remove-test"},
        
        { TestViewPart.RUN_TESTS                    + Action.SMALL_ICON           , "runTest.gif"},
        { TestViewPart.RUN_TESTS                    + Action.NAME                 , "Run tests"},
        { TestViewPart.RUN_TESTS                    + Action.SHORT_DESCRIPTION    , "Execute all tests."},
        { TestViewPart.RUN_TESTS                    + Action.ACTION_COMMAND_KEY   , "cmd-run-test"},
        
        { "NEW_SUITE_CLICK",  "(click to set test suite path)"},
    };
}
