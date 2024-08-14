package biouml.standard.state;

import java.util.logging.Level;
import java.util.ListResourceBundle;

import javax.swing.Action;

import java.util.logging.Logger;

/**
 *
 * @pending enhance short descriptions.
 */
public class MessageBundle extends ListResourceBundle
{
    private Logger log = Logger.getLogger(MessageBundle.class.getName());

    /**
     * Returns string from the resource bundle for the specified key.
     * If the sting is absent the key string is returned instead and
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
            log.log(Level.SEVERE, "Missing resource <" + key + "> in " + this.getClass());
        }
        return key;
    }

    @Override
    protected Object[][] getContents()
    {
        return contents;
    }

    private Object[][] contents = {
            // --- States tab actions ---------------------------------------------------/
            {StatesPane.APPLY + Action.SMALL_ICON, "apply.gif"},
            {StatesPane.APPLY + Action.NAME, "Apply"},
            {StatesPane.APPLY + Action.SHORT_DESCRIPTION, "Apply state to diagram."},
            {StatesPane.APPLY + Action.ACTION_COMMAND_KEY, "cmd-apply"},
            {StatesPane.APPLY + Action.SMALL_ICON + "2", "restore.gif"},
            {StatesPane.APPLY + Action.SHORT_DESCRIPTION + "2", "Restore diagram."},

            {StatesPane.ADD + Action.SMALL_ICON, "add.gif"},
            {StatesPane.ADD + Action.NAME, "Add state"},
            {StatesPane.ADD + Action.SHORT_DESCRIPTION, "Add new state to diagram and activate it."},
            {StatesPane.ADD + Action.ACTION_COMMAND_KEY, "cmd-add"},

            {StatesPane.REMOVE + Action.SMALL_ICON, "remove.gif"},
            {StatesPane.REMOVE + Action.NAME, "Remove state"},
            {StatesPane.REMOVE + Action.SHORT_DESCRIPTION, "Remove selected state from diagram."},
            {StatesPane.REMOVE + Action.ACTION_COMMAND_KEY, "cmd-remove"},

            {StatesPane.REMOVE_TRANSACTION + Action.SMALL_ICON, "remove_transaction.gif"},
            {StatesPane.REMOVE_TRANSACTION + Action.NAME, "Remove transaction(s)"},
            {StatesPane.REMOVE_TRANSACTION + Action.SHORT_DESCRIPTION, "Remove selected transactions from the state."},
            {StatesPane.REMOVE_TRANSACTION + Action.ACTION_COMMAND_KEY, "cmd-remove-transaction"},

            // State bean info
            {"CN_STATE", "State"},
            {"CD_STATE", "State"},
            {"PN_STATE_VERSION", "Version"},
            {"PD_STATE_VERSION", "Version"},
            {"PN_IDENTIFIER", "Identifier"},
            {"PD_IDENTIFIER", "Identifier of the object"},
            {"PN_TITLE", "Title"}, {"PD_TITLE", "The object title (generally it is object brief name)."},
            {"PN_DESCRIPTION", "Description"}, {"PD_DESCRIPTION", "The object textual description (plain text or HTML)."},
            
            //State analyses
            {"CN_APPLY_STATE", "Apply state"},
            {"CD_APPLY_STATE", "Apply state analysis"},
            {"PN_INPUT_DIAGRAM", "Diagram"},
            {"PD_INPUT_DIAGRAM", "Input diagram path"},
            {"PN_INPUT_STATE", "State"},
            {"PD_INPUT_STATE", "State element path"},
            {"PN_OUTPUT_DIAGRAM", "Output"},
            {"PD_OUTPUT_DIAGRAM", "Output diagram path"},
    
    
    };
}
