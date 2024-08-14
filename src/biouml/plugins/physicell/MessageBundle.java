package biouml.plugins.physicell;

import java.util.ListResourceBundle;

import javax.swing.Action;

import biouml.plugins.physicell.CellDefinitionViewPart.AddRuleAction;
import biouml.plugins.physicell.CellDefinitionViewPart.RemoveRuleAction;

public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return new Object[][] {
                {AddRuleAction.KEY + Action.SMALL_ICON, "addRule.gif"},
                {AddRuleAction.KEY + Action.NAME, "Add Rule"},
                {AddRuleAction.KEY + Action.SHORT_DESCRIPTION, "Add new rule"},
                {AddRuleAction.KEY + Action.ACTION_COMMAND_KEY, "add-rule"},
                {RemoveRuleAction.KEY + Action.SMALL_ICON, "removeRule.gif"},
                {RemoveRuleAction.KEY + Action.NAME, "Remove Rule"},
                {RemoveRuleAction.KEY + Action.SHORT_DESCRIPTION, "Remove selected rule"},
                {RemoveRuleAction.KEY + Action.ACTION_COMMAND_KEY, "remove-rule"},};
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