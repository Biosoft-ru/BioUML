package biouml.plugins.antimony;

import java.util.ListResourceBundle;

import javax.swing.Action;

import biouml.plugins.antimony.AntimonyEditor.ApplyAntimony;
import biouml.plugins.antimony.AntimonyEditor.ClearLogAction;
import biouml.plugins.antimony.AntimonyEditor.EnableAntimony;
import biouml.plugins.antimony.AntimonyEditor.EnableAutoupdate;



public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return new Object[][] {

                {ApplyAntimony.KEY + Action.SMALL_ICON, "updates.gif"}, {ApplyAntimony.KEY + Action.NAME, "Import"},
                {ApplyAntimony.KEY + Action.SHORT_DESCRIPTION, "Apply Antimony"},
                {ApplyAntimony.KEY + Action.ACTION_COMMAND_KEY, "Apply antimony"},

                {EnableAntimony.KEY + Action.SMALL_ICON, "enableAntimony.gif"}, {EnableAntimony.KEY + Action.NAME, "Enable antimony"},
                {EnableAntimony.KEY + Action.SHORT_DESCRIPTION, "Enable antimony."},
                {EnableAntimony.KEY + Action.ACTION_COMMAND_KEY, "Enable antimony"},

                {EnableAutoupdate.KEY + Action.SMALL_ICON, "enableAuto.gif"}, {EnableAutoupdate.KEY + Action.NAME, "Enable autoupdaten"},
                {EnableAutoupdate.KEY + Action.SHORT_DESCRIPTION, "Enable autoupdate."},
                {EnableAutoupdate.KEY + Action.ACTION_COMMAND_KEY, "Enable autoupdate"},

                {ClearLogAction.KEY + Action.SMALL_ICON, "clear.gif"}, {ClearLogAction.KEY + Action.NAME, "Clear log"},
                {ClearLogAction.KEY + Action.SHORT_DESCRIPTION, "Clear log."},
                {ClearLogAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-clear-log"},};
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