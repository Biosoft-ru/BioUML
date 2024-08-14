package biouml.plugins.chemoinformatics;

import java.util.ListResourceBundle;

import javax.swing.Action;

import biouml.plugins.chemoinformatics.access.OpenStructuresAction;
import biouml.plugins.chemoinformatics.document.actions.EraseAction;
import biouml.plugins.chemoinformatics.document.actions.FlipHAction;
import biouml.plugins.chemoinformatics.document.actions.FlipVAction;
import biouml.plugins.chemoinformatics.document.actions.LassoAction;
import biouml.plugins.chemoinformatics.document.actions.SelectAction;
import biouml.plugins.chemoinformatics.document.actions.ZoomInAction;
import biouml.plugins.chemoinformatics.document.actions.ZoomOutAction;

public class MessageBundle extends ListResourceBundle
{

    @Override
    protected Object[][] getContents()
    {
        return new Object[][] {
            
            // Open as table action
            { OpenStructuresAction.KEY + Action.SMALL_ICON, "table.gif"},
            { OpenStructuresAction.KEY + Action.NAME, "Open as table"},
            { OpenStructuresAction.KEY + Action.SHORT_DESCRIPTION, "Opens selected structures as table document"},
            { OpenStructuresAction.KEY + Action.LONG_DESCRIPTION, "Opens selected structures as table document"},
            { OpenStructuresAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-open-structures"},
            
            // Zoom In action
            {ZoomInAction.KEY + Action.SMALL_ICON, "zoomin.gif"},
            {ZoomInAction.KEY + Action.NAME, "Zoom in"},
            {ZoomInAction.KEY + Action.SHORT_DESCRIPTION, "Zoom in"},
            {ZoomInAction.KEY + Action.LONG_DESCRIPTION, "Zoom in"},
            {ZoomInAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-structure-zoom-in"},
        
            // Zoom Out action
            {ZoomOutAction.KEY + Action.SMALL_ICON, "zoomout.gif"},
            {ZoomOutAction.KEY + Action.NAME, "Zoom out"},
            {ZoomOutAction.KEY + Action.SHORT_DESCRIPTION, "Zoom out"},
            {ZoomOutAction.KEY + Action.LONG_DESCRIPTION, "Zoom out"},
            {ZoomOutAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-structure-zoom-out"},
            
             // Flip Horizontal action
            {FlipHAction.KEY + Action.SMALL_ICON, "fliph.gif"},
            {FlipHAction.KEY + Action.NAME, "Flip horizontal"},
            {FlipHAction.KEY + Action.SHORT_DESCRIPTION, "Flip horizontal"},
            {FlipHAction.KEY + Action.LONG_DESCRIPTION, "Flip horizontal"},
            {FlipHAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-structure-flip-h"},
            
            // Flip Vertical action
            {FlipVAction.KEY + Action.SMALL_ICON, "flipv.gif"},
            {FlipVAction.KEY + Action.NAME, "Flip vertical"},
            {FlipVAction.KEY + Action.SHORT_DESCRIPTION, "Flip vertical"},
            {FlipVAction.KEY + Action.LONG_DESCRIPTION, "Flip vertical"},
            {FlipVAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-structure-flip-v"},
            
            // Select action
            {SelectAction.KEY + Action.SMALL_ICON, "select.gif"},
            {SelectAction.KEY + Action.NAME, "Select"},
            {SelectAction.KEY + Action.SHORT_DESCRIPTION, "Select"},
            {SelectAction.KEY + Action.LONG_DESCRIPTION, "Select"},
            {SelectAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-structure-select"},
            
            // Lasso action
            {LassoAction.KEY + Action.SMALL_ICON, "lasso.gif"},
            {LassoAction.KEY + Action.NAME, "Lasso"},
            {LassoAction.KEY + Action.SHORT_DESCRIPTION, "Lasso"},
            {LassoAction.KEY + Action.LONG_DESCRIPTION, "Lasso"},
            {LassoAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-structure-lasso"},
            
            // Erase action
            {EraseAction.KEY + Action.SMALL_ICON, "erase.gif"},
            {EraseAction.KEY + Action.NAME, "Remove element"},
            {EraseAction.KEY + Action.SHORT_DESCRIPTION, "Remove element"},
            {EraseAction.KEY + Action.LONG_DESCRIPTION, "Remove element"},
            {EraseAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-structure-erase"},
        };
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
}
