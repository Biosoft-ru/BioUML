package biouml.plugins.research;

import java.util.ListResourceBundle;

import javax.swing.Action;

import com.developmentontheedge.application.Application;

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
        catch( Throwable th )
        {

        }
        return key;
    }

    private final static Object[][] contents =
    {
        //Paste action constants
        { JournalViewPart.PASTE_ACTION + Action.SMALL_ICON        , "paste.gif"},
        { JournalViewPart.PASTE_ACTION + Action.NAME              , "Paste"},
        { JournalViewPart.PASTE_ACTION + Action.SHORT_DESCRIPTION , "Paste element to diagram"},
        { JournalViewPart.PASTE_ACTION + Action.ACTION_COMMAND_KEY, "cmd-research-paste"},
        
        //Remove action constants
        { JournalViewPart.REMOVE_ACTION + Action.SMALL_ICON        , "remove.gif"},
        { JournalViewPart.REMOVE_ACTION + Action.NAME              , "Remove"},
        { JournalViewPart.REMOVE_ACTION + Action.SHORT_DESCRIPTION , "Remove element from journal"},
        { JournalViewPart.REMOVE_ACTION + Action.ACTION_COMMAND_KEY, "cmd-research-remove"},
        
        //Remove all action constants
        { JournalViewPart.REMOVEALL_ACTION + Action.SMALL_ICON        , "remove.gif"},
        { JournalViewPart.REMOVEALL_ACTION + Action.NAME              , "Clean journal"},
        { JournalViewPart.REMOVEALL_ACTION + Action.SHORT_DESCRIPTION , "Remove all records from journal"},
        { JournalViewPart.REMOVEALL_ACTION + Action.ACTION_COMMAND_KEY, "cmd-research-removeall"},
        
        //Journal(TaskInfo) BeanInfo constants
        {"PN_TYPE"               , "Type"},
        {"PD_TYPE"               , "Element type"},
        {"PN_SOURCE"             , "Source"},
        {"PD_SOURCE"             , "Element source"},
        {"PN_TIME"               , "Time"},
        {"PD_TIME"               , "Time of task end"},
        
        // Script properties constants
        {"CN_SCRIPT_PROPERTIES", "Script element"},
        {"CD_SCRIPT_PROPERTIES", "Script element"},
        {"PN_SCRIPT_SOURCE"     , "Script source"},
        {"PD_SCRIPT_SOURCE"     , "Text of the script to execute"},
        {"PN_SCRIPT_TYPE"     , "Script type"},
        {"PD_SCRIPT_TYPE"     , "Language of the script"},
        {"PN_SCRIPT_PATH"     , "Script path"},
        {"PD_SCRIPT_PATH"     , "Path to the JavaScript program to execute"},
        
        //Plot properties constants
        {"CN_PLOT_PROPERTIES", "Plot element"},
        {"CD_PLOT_PROPERTIES", "Plot element"},
        {"PN_PLOT_AUTO_OPEN"     , "Auto open"},
        {"PD_PLOT_AUTO_OPEN"     , "Open plot dialog automatically"},
        {"PN_PLOT_PATH"     , "Plot path"},
        {"PD_PLOT_PATH"     , "Path where plot should be stored"},
        
        //NewProjectWizard constants
        {"WIZARD_CREATE_TEXT",        "Create"},
        {"LOAD_RESEARCH_DIALOG_INFO_TEXT",
            "This dialog allows you to install projects located on "+Application.getGlobalValue("ApplicationName")+" server.\n"
            + "Corresponding project will be shown in \"Data\" section of repository tree.\n"
            + "To install project:\n" + "\t1) fill Server address, Server port\n" + "\t2) press 'Find databases' button\n"
            + "\t3) available projects will be shown \"Available databases\" table\n"
            + "\t4) select projects to be installed\n" + "\t5) press \"Ok\" button\n"},
    };
}
