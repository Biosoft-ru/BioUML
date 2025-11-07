package biouml.plugins.wdl;

import java.util.ListResourceBundle;

import javax.swing.Action;

import biouml.plugins.wdl.WorkflowTextEditor.RunScriptAction;
import biouml.plugins.wdl.WorkflowTextEditor.UpdateDiagramAction;
import biouml.plugins.wdl.WorkflowTextEditor.UpdateWDLAction;



public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return new Object[][] {

                {UpdateWDLAction.KEY + Action.SMALL_ICON, "updateText.gif"}, 
                {UpdateWDLAction.KEY + Action.NAME, "Update WDL"},
                {UpdateWDLAction.KEY + Action.SHORT_DESCRIPTION, "Update WDL from Diagram."},
                {UpdateWDLAction.KEY + Action.ACTION_COMMAND_KEY, "update-wdl."},

                {UpdateDiagramAction.KEY + Action.SMALL_ICON, "updateDiagram.gif"},
                {UpdateDiagramAction.KEY + Action.NAME, "Update diagram"},
                {UpdateDiagramAction.KEY + Action.SHORT_DESCRIPTION, "Update Diagram from WDL."},
                {UpdateDiagramAction.KEY + Action.ACTION_COMMAND_KEY, "update-diagram"},
                
                {RunScriptAction.KEY + Action.SMALL_ICON, "run.gif"},
                {RunScriptAction.KEY + Action.NAME, "Run workflow"},
                {RunScriptAction.KEY + Action.SHORT_DESCRIPTION, "Run workflow."},
                {RunScriptAction.KEY + Action.ACTION_COMMAND_KEY, "run-workflow"}};
    }

    /**
     * Returns string from the resource bundle for the specified key.
     * If the string is absent the key string is returned instead and
     * the message is printed in <code>java.util.logging.Logger</code> for the component.
     */
    public String getResourceString(String key)
    {
        try
        {
            return getString( key );
        }
        catch( Throwable t )
        {
            System.out.println( "Missing resource <" + key + "> in " + this.getClass() );
        }
        return key;
    }
}