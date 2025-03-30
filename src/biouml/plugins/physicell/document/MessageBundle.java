package biouml.plugins.physicell.document;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ListResourceBundle;
import java.util.ResourceBundle;

import javax.swing.Action;

import biouml.plugins.physicell.document.PhysicellResultViewPart;
import biouml.plugins.physicell.document.PhysicellResultViewPart.PauseAction;
import biouml.plugins.physicell.document.PhysicellResultViewPart.PlayAction;
import biouml.plugins.physicell.document.PhysicellResultViewPart.RecordAction;
import biouml.plugins.physicell.document.PhysicellResultViewPart.StopAction;

public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return contents;
    }

    /**
     * @pending remove unused constants (error messages for simulation engine)
     */
    private static Object[][] contents =
    {
        //--- Actions ---------------------------------------------------/
        { PlayAction.KEY    + Action.SMALL_ICON           , "play.gif"},
        { PlayAction.KEY    + Action.NAME                 , "Play"},
        { PlayAction.KEY    + Action.SHORT_DESCRIPTION    , "Plqy."},
        { PlayAction.KEY    + Action.ACTION_COMMAND_KEY   , "cmd-plqy"},

        { PauseAction.KEY   + Action.SMALL_ICON           , "pause.gif"},
        { PauseAction.KEY   + Action.NAME                 , "Pause"},
        { PauseAction.KEY   + Action.SHORT_DESCRIPTION    , "Pause"},
        { PauseAction.KEY   + Action.ACTION_COMMAND_KEY   , "cmd-pause"},

        { StopAction.KEY    + Action.SMALL_ICON           , "stop.gif"},
        { StopAction.KEY    + Action.NAME                 , "Stop"},
        { StopAction.KEY    + Action.SHORT_DESCRIPTION    , "Stop."},
        { StopAction.KEY    + Action.ACTION_COMMAND_KEY   , "cmd-stop"},
        
        { RecordAction.KEY    + Action.SMALL_ICON           , "record.gif"},
        { RecordAction.KEY    + Action.NAME                 , "Record"},
        { RecordAction.KEY    + Action.SHORT_DESCRIPTION    , "Record."},
        { RecordAction.KEY    + Action.ACTION_COMMAND_KEY   , "cmd-record"},     
    };

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
        catch (Throwable t)
        {
            System.out.println("Missing resource <" + key + "> in " + this.getClass());
        }
        return key;
    }

    protected static final MessageBundle resources = (MessageBundle)ResourceBundle.getBundle(MessageBundle.class.getName());
    public static void warn(Logger log, String messageBundleKey, Object[] params)
    {
        String message = resources.getResourceString(messageBundleKey);
        message = MessageFormat.format(message, params);
        log.warning(message);
    }
    public static void error(Logger log, String messageBundleKey, Object[] params)
    {
        String message = resources.getResourceString(messageBundleKey);
        message = MessageFormat.format(message, params);
        log.log(Level.SEVERE, message);
    }

    public static String getMessage(String messageBundleKey)
    {
        return resources.getResourceString(messageBundleKey);
    }
}
