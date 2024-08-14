package biouml.plugins.research.workflow;

import java.util.ListResourceBundle;
import javax.swing.Action;

/**
 * Message bundle for workflow functionality
 */
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

    private final static Object[][] contents = {
        //Start action constants
        { WorkflowViewPart.START_ACTION + Action.SMALL_ICON        , "start.gif"},
        { WorkflowViewPart.START_ACTION + Action.NAME              , "Start"},
        { WorkflowViewPart.START_ACTION + Action.SHORT_DESCRIPTION , "Run workflow execution"},
        { WorkflowViewPart.START_ACTION + Action.ACTION_COMMAND_KEY, "cmd-workflow-start"},
        
        //Stop action constants
        { WorkflowViewPart.STOP_ACTION + Action.SMALL_ICON        ,  "stop.gif"},
        { WorkflowViewPart.STOP_ACTION + Action.NAME              ,  "Stop"},
        { WorkflowViewPart.STOP_ACTION + Action.SHORT_DESCRIPTION ,  "Stop workflow execution"},
        { WorkflowViewPart.START_ACTION + Action.ACTION_COMMAND_KEY, "cmd-workflow-stop"},
        
        //Create port action constants
        { WorkflowPanel.BIND_PARAMETER_ACTION + Action.SMALL_ICON        ,  "bindparameter.gif"},
        { WorkflowPanel.BIND_PARAMETER_ACTION + Action.NAME              ,  "Bind parameter"},
        { WorkflowPanel.BIND_PARAMETER_ACTION + Action.SHORT_DESCRIPTION ,  "Link variable to selected parameter"},
        { WorkflowPanel.BIND_PARAMETER_ACTION + Action.ACTION_COMMAND_KEY, "cmd-workflow-bind-parameter"},
        
        {"CN_WORKFLOW_DIAGRAM", "Workflow"},
        {"CD_WORKFLOW_DIAGRAM", "Workflow diagram"},
        {"CN_RESEARCH_DIAGRAM", "Research"},
        {"CD_RESEARCH_DIAGRAM", "Research diagram"}
    };
}
