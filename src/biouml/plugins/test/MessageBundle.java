package biouml.plugins.test;

import java.util.ListResourceBundle;

public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents() { return contents; }
    private final static Object[][] contents =
    {
        // AcceptanceTestSuite
        { "CN_ACCEPTANCE_TEST" ,  "Acceptance test"},
        { "CD_ACCEPTANCE_TEST",   "Set of functional tests for selected model state."},
        { "PN_ACCEPTANCE_TEST_NAME" , "Name"},
        { "PD_ACCEPTANCE_TEST_NAME",  "Test name"},
        { "PN_ACCEPTANCE_TEST_STATE", "State"},
        { "PD_ACCEPTANCE_TEST_STATE", "Model state"},
        { "PN_ACCEPTANCE_TEST_TIME",  "Time limit"},
        { "PD_ACCEPTANCE_TEST_TIME",  "Limit of test duration"},
        
        // TestModel
        { "CN_TEST_MODEL" ,  "Test document"},
        { "CD_TEST_MODEL",   "Test document"},
        { "PN_TEST_MODEL_NAME" , "Name"},
        { "PD_TEST_MODEL_NAME",  "Docunamt name"},
        { "PN_TEST_MODEL_PATH", "Model path"},
        { "PD_TEST_MODEL_PATH", "Path to simulation model"},
        { "PN_TEST_MODEL_ACCTEST", "Tests"},
        { "PD_TEST_MODEL_ACCTEST", "List of test suites"},
        
        { "PN_COMPLETION_TIME", "Completion time"},
        { "PD_COMPLETION_TIME", "Model simulation  compeltion time."},
        
        // Row in TableTestModelWrapper
        { "CN_ROW_TEST" ,  "Test"},
        { "CD_ROW_TEST",   "Row in tests table"},
        { "PN_ROW_TEST_NAME" , "Name"},
        { "PD_ROW_TEST_NAME",  "Test suite name"},
        { "PN_ROW_TEST_STATE" ,"State"},
        { "PD_ROW_TEST_STATE", "Model state"},
        { "PN_ROW_TEST_INFO" , "Info"},
        { "PD_ROW_TEST_INFO",  "Test info"},
        { "PN_ROW_TEST_TIME" , "Time limit"},
        { "PD_ROW_TEST_TIME",  "Time limit in milliseconds, 0 - no limit"},
        { "PN_ROW_TEST_DURATION","Test duration"},
        { "PD_ROW_TEST_DURATION","Test duration in milliseconds"},
        { "PN_ROW_TEST_ERROR","Error"},
        { "PD_ROW_TEST_ERROR","Error message"},
        { "PN_ROW_TEST_STATUS","Status"},
        { "PD_ROW_TEST_STATUS","Test status"},
        { "PN_ROW_TEST_PLOT","Plot"},
        { "PD_ROW_TEST_PLOT","Show test plot"},
        
        // TestDocument
        { "ERROR_MODEL_SAVING",  "Cannot save test document"},
    };
}
