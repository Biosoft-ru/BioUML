package biouml.workbench.diagram.viewpart;

import java.util.ListResourceBundle;

import javax.swing.Action;

import biouml.workbench.diagram.SetInitialValuesAction;
import biouml.workbench.diagram.viewpart.ModelViewPart.AddElementAction;
import biouml.workbench.diagram.viewpart.ModelViewPart.AddParameterAction;
import biouml.workbench.diagram.viewpart.ModelViewPart.AddToPlotAction;
import biouml.workbench.diagram.viewpart.ModelViewPart.DetectParametersAction;
import biouml.workbench.diagram.viewpart.ModelViewPart.HighlightAction;
import biouml.workbench.diagram.viewpart.ModelViewPart.HighlightOffAction;
import biouml.workbench.diagram.viewpart.ModelViewPart.RemoveParametersAction;
import biouml.workbench.diagram.viewpart.ModelViewPart.RemoveSelectedParameterAction;


public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return new Object[][] {{RemoveParametersAction.KEY + Action.SMALL_ICON, "removeUnused.png"},
                {RemoveParametersAction.KEY + Action.NAME, "Refresh"},
                {RemoveParametersAction.KEY + Action.SHORT_DESCRIPTION, "Remove unused parameters"},
                {RemoveParametersAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-refresh"},

                {DetectParametersAction.KEY + Action.SMALL_ICON, "detect.gif"}, {DetectParametersAction.KEY + Action.NAME, "Detect types"},
                {DetectParametersAction.KEY + Action.SHORT_DESCRIPTION, "Detect parameter types"},
                {DetectParametersAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-detect"},

                {AddParameterAction.KEY + Action.SMALL_ICON, "add.gif"}, {AddParameterAction.KEY + Action.NAME, "Add"},
                {AddParameterAction.KEY + Action.SHORT_DESCRIPTION, "Add parameter"},
                {AddParameterAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-add"},

                {HighlightAction.KEY + Action.SMALL_ICON, "highlight_on.png"}, {HighlightAction.KEY + Action.NAME, "Highlight"},
                {HighlightAction.KEY + Action.SHORT_DESCRIPTION, "Highlight diagram nodes containing selected parameters."},

                {HighlightOffAction.KEY + Action.SMALL_ICON, "highlight_off.png"},
                {HighlightOffAction.KEY + Action.NAME, "Clear highlight"},
                {HighlightOffAction.KEY + Action.SHORT_DESCRIPTION, "Clear diagram highlight."},

                {RemoveSelectedParameterAction.KEY + Action.SMALL_ICON, "removeSelected.png"},
                {RemoveSelectedParameterAction.KEY + Action.NAME, "Remove selectes"},
                {RemoveSelectedParameterAction.KEY + Action.SHORT_DESCRIPTION, "Remove selected parameters from model"},

                {AddToPlotAction.KEY + Action.SMALL_ICON, "add_to_plot.png"}, {AddToPlotAction.KEY + Action.NAME, "Add to plot"},
                {AddToPlotAction.KEY + Action.SHORT_DESCRIPTION, "Adds to plot during simulation."},

                {SetInitialValuesAction.KEY + Action.SMALL_ICON, "setValues.gif"},
                {SetInitialValuesAction.KEY + Action.NAME, "Set initial values"},
                {SetInitialValuesAction.KEY + Action.SHORT_DESCRIPTION, "Set initial values"},
                {SetInitialValuesAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-gnrt-dgr"},

                {AddElementAction.KEY + Action.SMALL_ICON, "add.gif"}, {AddElementAction.KEY + Action.NAME, "Add element"},
                {AddElementAction.KEY + Action.SHORT_DESCRIPTION, "Adds element to diagram."},
        };
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