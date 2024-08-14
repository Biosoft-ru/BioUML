package ru.biosoft.bsa.project;

import java.awt.Event;
import java.awt.event.KeyEvent;
import java.util.logging.Level;
import java.util.ListResourceBundle;

import javax.swing.Action;
import javax.swing.KeyStroke;

import java.util.logging.Logger;

import ru.biosoft.gui.ZoomInAction;
import ru.biosoft.gui.ZoomOutAction;

/**
 * Stores data for initialization of BioUMLEditor constant and resources.
 */
public class MessageBundle extends ListResourceBundle
{
    private Logger log = Logger.getLogger(MessageBundle.class.getName());

    @Override
    protected Object[][] getContents()
    {
        return contents;
    }

    private Object[][] contents = {
            // Zoom In action
            {ZoomInAction.KEY + Action.SMALL_ICON, "zoomin.gif"},
            {ZoomInAction.KEY + Action.NAME, "Zoom in"},
            {ZoomInAction.KEY + Action.SHORT_DESCRIPTION, "Zoom in"},
            {ZoomInAction.KEY + Action.LONG_DESCRIPTION, "Zoom in"},
            {ZoomInAction.KEY + Action.MNEMONIC_KEY, KeyEvent.VK_PERIOD},
            {ZoomInAction.KEY + Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, Event.CTRL_MASK)},
            {ZoomInAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-zoom-in"},

            // Zoom Out action
            {ZoomOutAction.KEY + Action.SMALL_ICON, "zoomout.gif"},
            {ZoomOutAction.KEY + Action.NAME, "Zoom out"},
            {ZoomOutAction.KEY + Action.SHORT_DESCRIPTION, "Zoom out"},
            {ZoomOutAction.KEY + Action.LONG_DESCRIPTION, "Zoom out"},
            {ZoomOutAction.KEY + Action.MNEMONIC_KEY, KeyEvent.VK_COMMA},
            {ZoomOutAction.KEY + Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, Event.CTRL_MASK)},
            {ZoomOutAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-zoom-out"},
            
            //semantic zoom in action
            { SemanticZoomInAction.KEY    + Action.SMALL_ICON           , "semanticzoomin.gif"},
            { SemanticZoomInAction.KEY    + Action.NAME                 , "Zoom in"},
            { SemanticZoomInAction.KEY    + Action.SHORT_DESCRIPTION    , "Semantic zoom in"},
            { SemanticZoomInAction.KEY    + Action.ACTION_COMMAND_KEY   , "cmd-sem-zoomin"},
            
            //semantic zoom out action
            { SemanticZoomOutAction.KEY    + Action.SMALL_ICON           , "semanticzoomout.gif"},
            { SemanticZoomOutAction.KEY    + Action.NAME                 , "Zoom out"},
            { SemanticZoomOutAction.KEY    + Action.SHORT_DESCRIPTION    , "Semantic zoom out"},
            { SemanticZoomOutAction.KEY    + Action.ACTION_COMMAND_KEY   , "cmd-sem-zoomout"},
            
            //overview action
            { SetModeAction.KEY_OVERVIEW    + Action.SMALL_ICON           , "overview.gif"},
            { SetModeAction.KEY_OVERVIEW    + Action.NAME                 , "Overview"},
            { SetModeAction.KEY_OVERVIEW    + Action.SHORT_DESCRIPTION    , "Overview"},
            { SetModeAction.KEY_OVERVIEW    + Action.ACTION_COMMAND_KEY   , "cmd-overview"},
            
            //default action
            { SetModeAction.KEY_DEFAULT    + Action.SMALL_ICON           , "default.gif"},
            { SetModeAction.KEY_DEFAULT    + Action.NAME                 , "Standard"},
            { SetModeAction.KEY_DEFAULT    + Action.SHORT_DESCRIPTION    , "Standard view"},
            { SetModeAction.KEY_DEFAULT    + Action.ACTION_COMMAND_KEY   , "cmd-default"},
            
            //detailed action
            { SetModeAction.KEY_DETAILED    + Action.SMALL_ICON           , "detailed.gif"},
            { SetModeAction.KEY_DETAILED    + Action.NAME                 , "Detailed"},
            { SetModeAction.KEY_DETAILED    + Action.SHORT_DESCRIPTION    , "Detailed view"},
            { SetModeAction.KEY_DETAILED    + Action.ACTION_COMMAND_KEY   , "cmd-detailed"},
    };

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
}
