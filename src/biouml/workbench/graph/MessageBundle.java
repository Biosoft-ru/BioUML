
package biouml.workbench.graph;

import java.util.ListResourceBundle;

import javax.swing.Action;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stores data for initialization of BioUMLEditor constant and resources.
 */
public class MessageBundle extends ListResourceBundle
{
    private final Logger cat = Logger.getLogger(MessageBundle.class.getName());


    @Override
    protected Object[][] getContents()
    {
        return new Object[][] {
            {"OPEN_DIAGRAM_DIALOG_TITLE", "Open diagram"},
            {"DATA_ELEMENT_COOSER_DIALOG_TITLE", "Search for {0}"},
            
            {"CELL_LAYOUT_MANAGER_NAME", "Cell"},
            {"FORCE_DIRECTED_LAYOUT_MANAGER_NAME", "Force directed"},
            {"LAYERED_LAYOUT_MANAGER_NAME", "Layered"},
            {"LAYERED_Y_OPTIMIZED_LAYOUT_MANAGER_NAME", "Layered Y-optimized"},
            {"LAYERED_X_OPTIMIZED_LAYOUT_MANAGER_NAME", "Layered X-optimized"},
            {"UNDEFINED_LAYOUT_MANAGER_NAME", "Undefined"},
            {"STAR_LAYOUT_MANAGER_NAME", "Star"},
        
            // Prepare layout action
            { PrepareLayoutAction.KEY      + Action.SMALL_ICON           , "run.gif"},
            { PrepareLayoutAction.KEY      + Action.NAME                 , "Prepare layout"},
            { PrepareLayoutAction.KEY      + Action.SHORT_DESCRIPTION    , "Prepare layout"},
            { PrepareLayoutAction.KEY      + Action.LONG_DESCRIPTION     , "Prepare layout"},
            { PrepareLayoutAction.KEY      + Action.ACTION_COMMAND_KEY   , "cmd-prepare-layout"},
            
            { PartialLayoutAction.KEY      + Action.SMALL_ICON           , "run.gif"},
            { PartialLayoutAction.KEY      + Action.NAME                 , "Prepare partial layout"},
            { PartialLayoutAction.KEY      + Action.SHORT_DESCRIPTION    , "Prepare layout of selected items"},
            { PartialLayoutAction.KEY      + Action.LONG_DESCRIPTION     , "Prepare layout of selected items"},
            { PartialLayoutAction.KEY      + Action.ACTION_COMMAND_KEY   , "cmd-partial-layout"},
            
            // Aplly layout action
            { ApplyLayoutAction.KEY      + Action.SMALL_ICON           , "applyLayout2.gif"},
            { ApplyLayoutAction.KEY      + Action.NAME                 , "Apply layout"},
            { ApplyLayoutAction.KEY      + Action.SHORT_DESCRIPTION    , "Apply layout"},
            { ApplyLayoutAction.KEY      + Action.LONG_DESCRIPTION     , "Apply layout"},
        //        { ApplyLayoutAction.KEY      + Action.MNEMONIC_KEY         , "A"},
        //        { ApplyLayoutAction.KEY      + Action.ACCELERATOR_KEY      , KeyStroke.getKeyStroke(KeyEvent.VK_A, Event.CTRL_MASK | Event.ALT_MASK) },
            { ApplyLayoutAction.KEY      + Action.ACTION_COMMAND_KEY   , "cmd-apply-layout"},
            
         // Save layout action
            { SaveLayoutAction.KEY      + Action.SMALL_ICON           , "saveLayout.gif"},
            { SaveLayoutAction.KEY      + Action.NAME                 , "save layout"},
            { SaveLayoutAction.KEY      + Action.SHORT_DESCRIPTION    , "save laoyut"},
            { SaveLayoutAction.KEY      + Action.LONG_DESCRIPTION     , "save laoyut"},
        //        { ApplyLayoutAction.KEY      + Action.ACTION_COMMAND_KEY   , "cmd-apply-layout"},
            
         // Expert options showing action
            { ExpertLayoutAction.KEY      + Action.SMALL_ICON           , "ExpertOptions.gif"},
            { ExpertLayoutAction.KEY      + Action.NAME                 , "Expert mode"},
            { ExpertLayoutAction.KEY      + Action.SHORT_DESCRIPTION    , "Show/hide expert options"},
            { ExpertLayoutAction.KEY      + Action.LONG_DESCRIPTION     , "Show/hide expert options for advanced users"},
        //        { ApplyLayoutAction.KEY      + Action.ACTION_COMMAND_KEY   , "cmd-apply-layout"},
            
            // Expert options showing action
            { StopLayoutAction.KEY      + Action.SMALL_ICON           , "stopLayout.gif"},
            { StopLayoutAction.KEY      + Action.NAME                 , "Stop layout"},
            { StopLayoutAction.KEY      + Action.SHORT_DESCRIPTION    , "stops current layouting"},
            { StopLayoutAction.KEY      + Action.LONG_DESCRIPTION     , "stops current layouting"},
        //        { ApplyLayoutAction.KEY      + Action.ACTION_COMMAND_KEY   , "cmd-apply-layout"},
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
            cat.log(Level.SEVERE, "Missing resource <" + key + "> in " + this.getClass());
        }
        return key;
    }
}

