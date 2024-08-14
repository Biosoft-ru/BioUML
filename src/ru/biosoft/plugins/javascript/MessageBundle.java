package ru.biosoft.plugins.javascript;

import java.awt.event.KeyEvent;
import java.util.ListResourceBundle;

import javax.swing.Action;
import javax.swing.KeyStroke;

public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return new Object[][] {
            //----- JScriptVisiblePlugin constants -----------------------------------/
            {"CN_JSCRIPT_PLUGIN",         "JavaScript"},
            {"CD_JSCRIPT_PLUGIN",         "JavaScript plugin."},
        
            // reuse VectorDataCollection constants
            { "PN_VECTOR_DC_NAME",        "Name"},
            { "PD_VECTOR_DC_NAME",        "Data collection name."},
            { "PN_VECTOR_DC_SIZE",        "Size"},
            { "PD_VECTOR_DC_SIZE",        "Number of data elements in data collection."},
            { "PN_VECTOR_DC_DESCRIPTION", "Description"},
            { "PD_VECTOR_DC_DESCRIPTION", "Data collection description."},
        
            //----- HostObjectInfo constants -----------------------------------------/
            {"CN_HOST_OBJECT_INFO",            "Host object"},
            {"CD_HOST_OBJECT_INFO",            "JavaScript host object."},
        
            {"PN_HOST_OBJECT_NAME",            "Name"},
            {"PD_HOST_OBJECT_NAME",            "Host object name."},
        
            {"PN_HOST_OBJECT_TYPE",            "Type"},
            {"PD_HOST_OBJECT_TYPE",            "Host object type."},
        
            {"PN_HOST_OBJECT_DESCRIPTION",     "Description"},
            {"PD_HOST_OBJECT_DESCRIPTION",     "Host object description."},
        
            //----- PropertyInfo constants -----------------------------------------/
            {"CN_PROPERTY_INFO",            "Property"},
            {"CD_PROPERTY_INFO",            "Property of JavaScript class or object."},
        
            {"PN_PROPERTY_NAME",            "Name"},
            {"PD_PROPERTY_NAME",            "Property name."},
        
            {"PN_PROPERTY_TYPE",            "Type"},
            {"PD_PROPERTY_TYPE",            "Property type."},
        
            {"PN_PROPERTY_READ_ONLY",       "Read only"},
            {"PD_PROPERTY_READ_ONLY",       "Indicates whether this property is read only."},
        
            {"PN_PROPERTY_DESCRIPTION",     "Description"},
            {"PD_PROPERTY_DESCRIPTION",     "Property description."},
        
            //----- FunctionInfo constants -----------------------------------------/
            {"CN_FUNCTION_INFO",            "Function"},
            {"CD_FUNCTION_INFO",            "JavaScript function."},
        
            {"PN_FUNCTION_NAME",            "Name"},
            {"PD_FUNCTION_NAME",            "Function name."},
        
            {"PN_FUNCTION_DESCRIPTION",     "Description"},
            {"PD_FUNCTION_DESCRIPTION",     "Function description."},
        
            {"PN_FUNCTION_ARGUMENTS",       "Arguments"},
            {"PD_FUNCTION_ARGUMENTS",       "Function arguments."},
        
            {"PN_FUNCTION_RETURNED_VALUE",  "Returned value"},
            {"PD_FUNCTION_RETURNED_VALUE",  "Returned value."},
        
            {"PN_FUNCTION_EXCEPTIONS",      "Throws"},
            {"PD_FUNCTION_EXCEPTIONS",      "Description of exceptios that function can throw."},
        
            {"PN_FUNCTION_EXAMPLES",        "Examples"},
            {"PD_FUNCTION_EXAMPLES",        "Examples of function usage."},
        
            //----- arguments -----/
            {"CN_ARGUMENT",                 "Argument"},
            {"CD_ARGUMENT",                 "Function argument."},
        
            {"PN_ARGUMENT_TYPE",            "Type"},
            {"PD_ARGUMENT_TYPE",            "Argument type."},
        
            {"PN_ARGUMENT_NAME",            "Name"},
            {"PD_ARGUMENT_NAME",            "Argument name."},
        
            {"PN_ARGUMENT_OBLIGATORY",      "Obligatory"},
            {"PD_ARGUMENT_OBLIGATORY",      "Indicates whether the argument is obligatory." +
                                            "<br>Generally it is concerns functions with variable argument number" +
                                            "<br> (varargs functions) where some arguments can be skipped."},
        
            {"PN_ARGUMENT_DESCRIPTION",     "Description"},
            {"PD_ARGUMENT_DESCRIPTION",     "The argument description."},
        
            //----- returned value -----/
            {"CN_RETURNED_VALUE",           "Returned value"},
            {"CD_RETURNED_VALUE",           "Decription of function returned value."},
        
            {"PN_RETURNED_VALUE_TYPE",      "Type"},
            {"PD_RETURNED_VALUE_TYPE",      "Returned value type."},
        
            {"PN_RETURNED_VALUE_DESCRIPTION", "Description"},
            {"PD_RETURNED_VALUE_DESCRIPTION", "Returned value description."},
        
            //----- exception -----/
            {"CN_EXCEPTION",                "Exception"},
            {"CD_EXCEPTION",                "Description of exception that can be thrown by function."},
        
            {"PN_EXCEPTION_TYPE",           "Type"},
            {"PD_EXCEPTION_TYPE",           "Exception type."},
        
            {"PN_EXCEPTION_DESCRIPTION",    "Description"},
            {"PD_EXCEPTION_DESCRIPTION",    "Description of exception that can be thrown by function."},
        
            //----- example -----/
            {"CN_EXAMPLE",                  "Example"},
            {"CD_EXAMPLE",                  "Example of function usage."},
        
            {"PN_EXAMPLE_CODE",             "Code"},
            {"PD_EXAMPLE_CODE",             "The example code."},
        
            {"PN_EXAMPLE_DESCRIPTION",      "Description"},
            {"PD_EXAMPLE_DESCRIPTION",      "The example code description."},
            
            //----- JSElement constants -----------------------------------------/
            {"CN_JSELEMENT",                "JavaScript"},
            {"CD_JSELEMENT",                "JavaScript source"},
        
            {"PN_JSELEMENT_NAME",           "Name"},
            {"PD_JSELEMENT_NAME",           "Script name."},
          
            //----- JSCommand constants -----------------------------------------/
            { "CN_JSCOMMAND",               "Command" },
            { "CD_JSCOMMAND",               "JavaScript command" },
            
            // ----- actions constants ------------------------------------------/
            {"JAVASCRIPT_ADD_ELEMENT", "Enter JavaScript name"},
            
            // Execute action
            {ExecuteAction.KEY + Action.SMALL_ICON, "run.gif"},
            {ExecuteAction.KEY + Action.NAME, "Run"},
            {ExecuteAction.KEY + Action.SHORT_DESCRIPTION, "Execute JavaScript"},
            {ExecuteAction.KEY + Action.MNEMONIC_KEY, KeyEvent.VK_F8},
            {ExecuteAction.KEY + Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)},
            {ExecuteAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-run"},
            
            // Execute part action
            {ExecutePartAction.KEY + Action.SMALL_ICON, "executePart.gif"},
            {ExecutePartAction.KEY + Action.NAME, "Run selected"},
            {ExecutePartAction.KEY + Action.SHORT_DESCRIPTION, "Execute selected part of document"},
            {ExecutePartAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-part-run"},
            
            // Step action
            {StepAction.KEY + Action.SMALL_ICON, "step.gif"},
            {StepAction.KEY + Action.NAME, "Step over"},
            {StepAction.KEY + Action.SHORT_DESCRIPTION, "Execute one line"},
            {StepAction.KEY + Action.MNEMONIC_KEY, KeyEvent.VK_F6},
            {StepAction.KEY + Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0)},
            {StepAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-step"},
            
            // Break action
            {StopAction.KEY + Action.SMALL_ICON, "break.gif"},
            {StopAction.KEY + Action.NAME, "Break"},
            {StopAction.KEY + Action.SHORT_DESCRIPTION, "Break execution"},
            {StopAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-stop"},
        };
    }
}

