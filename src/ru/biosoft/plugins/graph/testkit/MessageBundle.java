package ru.biosoft.plugins.graph.testkit;

import java.util.logging.Level;
import java.util.ListResourceBundle;

import javax.swing.Action;

import java.util.logging.Logger;

/**
 * Stores data for initialization of BioUMLEditor constant and resources.
 */
public class MessageBundle extends ListResourceBundle
{
    private final Logger log = Logger.getLogger(GraphViewer.class.getName());

    @Override
    protected Object[][] getContents()
    {
        return new Object[][] {
            { "PN_LAYOUTER_TYPE",       "layoter type"},
            { "PD_LAYOUTER_TYPE",       "layoter type"},
        
            { "PN_LAYOUTER",            "layoter properties"},
            { "PD_LAYOUTER",            "layoter properties"},
        
            // Zoom In action
            { GraphViewer.ZoomInAction.KEY      + Action.SMALL_ICON           , "zoomin.gif"},
            { GraphViewer.ZoomInAction.KEY      + Action.NAME                 , "Zoom in"},
            { GraphViewer.ZoomInAction.KEY      + Action.SHORT_DESCRIPTION    , "Zoom in"},
            { GraphViewer.ZoomInAction.KEY      + Action.LONG_DESCRIPTION     , "Zoom in"},
        //        { GraphViewer.ZoomInAction.KEY      + Action.MNEMONIC_KEY         , ">"},
        //        { GraphViewer.ZoomInAction.KEY      + Action.ACCELERATOR_KEY      , KeyStroke.getKeyStroke(KeyEvent.VK_GREATER, Event.CTRL_MASK) },
            { GraphViewer.ZoomInAction.KEY      + Action.ACTION_COMMAND_KEY   , "cmd-zoom-in"},
        
            // Zoom Out action
            { GraphViewer.ZoomOutAction.KEY      + Action.SMALL_ICON           , "zoomout.gif"},
            { GraphViewer.ZoomOutAction.KEY      + Action.NAME                 , "Zoom out"},
            { GraphViewer.ZoomOutAction.KEY      + Action.SHORT_DESCRIPTION    , "Zoom out"},
            { GraphViewer.ZoomOutAction.KEY      + Action.LONG_DESCRIPTION     , "Zoom out"},
        //        { GraphViewer.ZoomOutAction.KEY      + Action.MNEMONIC_KEY         , "<"},
        //        { GraphViewer.ZoomOutAction.KEY      + Action.ACCELERATOR_KEY      , KeyStroke.getKeyStroke(KeyEvent.VK_LESS, Event.CTRL_MASK) },
            { GraphViewer.ZoomOutAction.KEY      + Action.ACTION_COMMAND_KEY   , "cmd-zoom-out"},
        
            // Load graph action
            { GraphViewer.LoadGraphAction.KEY      + Action.SMALL_ICON           , "new.gif"},
            { GraphViewer.LoadGraphAction.KEY      + Action.NAME                 , "Load graph"},
            { GraphViewer.LoadGraphAction.KEY      + Action.SHORT_DESCRIPTION    , "Load graph"},
            { GraphViewer.LoadGraphAction.KEY      + Action.LONG_DESCRIPTION     , "Load graph"},
        //        { GraphViewer.LoadGraphAction.KEY      + Action.MNEMONIC_KEY         , "L"},
        //        { GraphViewer.LoadGraphAction.KEY      + Action.ACCELERATOR_KEY      , KeyStroke.getKeyStroke(KeyEvent.VK_L, Event.CTRL_MASK) },
            { GraphViewer.LoadGraphAction.KEY      + Action.ACTION_COMMAND_KEY   , "cmd-load-grap"},
        
            // Save image action
            { GraphViewer.SaveImageAction.KEY      + Action.SMALL_ICON           , "saveImage.gif"},
            { GraphViewer.SaveImageAction.KEY      + Action.NAME                 , "Save graph view as image..."},
            { GraphViewer.SaveImageAction.KEY      + Action.SHORT_DESCRIPTION    , "Save graph view as image"},
            { GraphViewer.SaveImageAction.KEY      + Action.LONG_DESCRIPTION     , "Save graph view as image"},
        //        { GraphViewer.SaveImageAction.KEY      + Action.MNEMONIC_KEY         , "I"},
        //        { GraphViewer.SaveImageAction.KEY      + Action.ACCELERATOR_KEY      , KeyStroke.getKeyStroke(KeyEvent.VK_I, Event.CTRL_MASK) },
            { GraphViewer.SaveImageAction.KEY      + Action.ACTION_COMMAND_KEY   , "cmd-save-image"},
        
            // special issues - remove dependencies from TestKit
            { "Add graph"      + Action.SMALL_ICON           , "add.gif"},
            { "Add graph"      + Action.NAME                 , "Add subgrap"},
            { "Add graph"      + Action.SHORT_DESCRIPTION    , "Add subgraph"},
            { "Add graph"      + Action.LONG_DESCRIPTION     , "Add subgraph"},
        //        { "Add graph"      + Action.MNEMONIC_KEY         , "A"},
        //        { "Add graph"      + Action.ACCELERATOR_KEY      , KeyStroke.getKeyStroke(KeyEvent.VK_A, Event.CTRL_MASK) },
            { "Add graph"      + Action.ACTION_COMMAND_KEY   , "cmd-add-grap"},
        
            // Apply layout action
            { "Apply layout"      + Action.SMALL_ICON           , "applyLayout.gif"},
            { "Apply layout"      + Action.NAME                 , "Apply layout"},
            { "Apply layout"      + Action.SHORT_DESCRIPTION    , "Apply layout"},
            { "Apply layout"      + Action.LONG_DESCRIPTION     , "Apply layout"},
        //        { "Apply layout"      + Action.MNEMONIC_KEY         , "A"},
        //        { "Apply layout"      + Action.ACCELERATOR_KEY      , KeyStroke.getKeyStroke(KeyEvent.VK_A, Event.CTRL_MASK | Event.ALT_MASK) },
            { "Apply layout"      + Action.ACTION_COMMAND_KEY   , "cmd-apply-layout"},
        
            // Generate graph text action
            { "generate graph"      + Action.SMALL_ICON           , "generate.gif"},
            { "generate graph"      + Action.NAME                 , "Generate test"},
            { "generate graph"      + Action.SHORT_DESCRIPTION    , "Generate grpah text"},
            { "generate graph"      + Action.LONG_DESCRIPTION     , "Apply layout"},
        //        { "generate graph"      + Action.MNEMONIC_KEY         , "G"},
        //        { "generate graph"      + Action.ACCELERATOR_KEY      , KeyStroke.getKeyStroke(KeyEvent.VK_G, Event.CTRL_MASK ) },
            { "generate graph"      + Action.ACTION_COMMAND_KEY   , "cmd-apply-layout"},
        
        };
    }

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
        catch (Throwable t)
        {
            log.log(Level.SEVERE, "Missing resource <" + key + "> in " + this.getClass());
        }
        return key;
    }
}
