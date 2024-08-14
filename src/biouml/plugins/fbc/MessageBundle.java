package biouml.plugins.fbc;

import java.util.ListResourceBundle;

import javax.swing.Action;

import biouml.plugins.fbc.FbcReactionsEditor.EditOptions;
import biouml.plugins.fbc.FbcReactionsEditor.SaveTable;
import biouml.plugins.fbc.FbcReactionsEditor.ShowOptimalValues;
import biouml.plugins.fbc.FbcReactionsEditor.ShowTable;


public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return new Object[][] {
                //--- Actions ---------------------------------------------------/
                {ShowTable.KEY + Action.SMALL_ICON, "table.gif"},
                {ShowTable.KEY + Action.NAME, "Table"},
                {ShowTable.KEY + Action.SHORT_DESCRIPTION, "Show table"},
                {ShowTable.KEY + Action.ACTION_COMMAND_KEY, "cmd-table"},
            
                {ShowOptimalValues.KEY + Action.SMALL_ICON, "process.gif"},
                {ShowOptimalValues.KEY + Action.NAME, "Optimal values"},
                {ShowOptimalValues.KEY + Action.SHORT_DESCRIPTION, "Show optimal values"},
                {ShowOptimalValues.KEY + Action.ACTION_COMMAND_KEY, "cmd-optimal"},
                
                {EditOptions.KEY + Action.SMALL_ICON, "note.gif"},
                {EditOptions.KEY + Action.NAME, "Options"},
                {EditOptions.KEY + Action.SHORT_DESCRIPTION, "Show options"},
                {EditOptions.KEY + Action.ACTION_COMMAND_KEY, "cmd-type"},
                
                {SaveTable.KEY + Action.SMALL_ICON, "saveResult.gif"},
                {SaveTable.KEY + Action.NAME, "Save table"},
                {SaveTable.KEY + Action.SHORT_DESCRIPTION, "Save table"},
                {SaveTable.KEY + Action.ACTION_COMMAND_KEY, "cmd-save"}};
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
            return getString(key);
        }
        catch( Throwable t )
        {
            System.out.println("Missing resource <" + key + "> in " + this.getClass());
        }
        return key;
    }
}