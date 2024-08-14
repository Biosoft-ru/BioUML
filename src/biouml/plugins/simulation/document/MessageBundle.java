package biouml.plugins.simulation.document;

import java.util.ListResourceBundle;

import javax.swing.Action;

import biouml.plugins.simulation.document.ParametersEditorPart.DecreaseParameterAction;
import biouml.plugins.simulation.document.ParametersEditorPart.IncreaseParameterAction;
import biouml.plugins.simulation.document.ParametersEditorPart.ResetParametersAction;
import biouml.plugins.simulation.document.ParametersEditorPart.SaveParametersAction;
import biouml.plugins.simulation.document.PlotsEditorPart.RunSimulationAction;
import biouml.plugins.simulation.document.PlotsEditorPart.SavePlotAction;
import biouml.plugins.simulation.document.PlotsEditorPart.UpdatePlotAction;
import biouml.plugins.simulation.document.SimulationOptionsEditorPart.RecompileAction;
import biouml.plugins.simulation.document.SimulationOptionsEditorPart.SaveEngineAction;

/**
 * @author axec
 *
 */
public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return new Object[][] {
                {RunSimulationAction.KEY + Action.SMALL_ICON, "simulate.gif"},
                {RunSimulationAction.KEY + Action.NAME, "Run"},
                {RunSimulationAction.KEY + Action.SHORT_DESCRIPTION, "Run simulation"},
                {RunSimulationAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-simulate"},

                {IncreaseParameterAction.KEY + Action.SMALL_ICON, "increase.gif"},
                {IncreaseParameterAction.KEY + Action.NAME, "Increase"},
                {IncreaseParameterAction.KEY + Action.SHORT_DESCRIPTION, "Increase parameter"},
                {IncreaseParameterAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-increase"},

                {DecreaseParameterAction.KEY + Action.SMALL_ICON, "decrease.gif"},
                {DecreaseParameterAction.KEY + Action.NAME, "Decrease"},
                {DecreaseParameterAction.KEY + Action.SHORT_DESCRIPTION, "Decrease parameter"},
                {DecreaseParameterAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-decrease"},
                
                {ResetParametersAction.KEY + Action.SMALL_ICON, "reset.gif"},
                {ResetParametersAction.KEY + Action.NAME, "Reset"},
                {ResetParametersAction.KEY + Action.SHORT_DESCRIPTION, "Reset parameters"},
                {ResetParametersAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-reset"},
                
                {SaveParametersAction.KEY + Action.SMALL_ICON, "save.gif"},
                {SaveParametersAction.KEY + Action.NAME, "Save to diagram"},
                {SaveParametersAction.KEY + Action.SHORT_DESCRIPTION, "Save parameters to diagram"},
                {SaveParametersAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-save"},
                                
                {UpdatePlotAction.KEY + Action.SMALL_ICON, "reset.gif"},
                {UpdatePlotAction.KEY + Action.NAME, "Update plots"},
                {UpdatePlotAction.KEY + Action.SHORT_DESCRIPTION, "Update plots"},
                {UpdatePlotAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-update"},
                
                {SavePlotAction.KEY + Action.SMALL_ICON, "save.gif"},
                {SavePlotAction.KEY + Action.NAME, "Save plots"},
                {SavePlotAction.KEY + Action.SHORT_DESCRIPTION, "Save plots"},
                {SavePlotAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-save"},
                
                {RecompileAction.KEY + Action.SMALL_ICON, "recompile.gif"},
                {RecompileAction.KEY + Action.NAME, "Recompile model"},
                {RecompileAction.KEY + Action.SHORT_DESCRIPTION, "Recompile model"},
                {RecompileAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-recompile"},
                
                {SaveEngineAction.KEY + Action.SMALL_ICON, "save.gif"},
                {SaveEngineAction.KEY + Action.NAME, "Save engine"},
                {SaveEngineAction.KEY + Action.SHORT_DESCRIPTION, "Save engine"},
                {SaveEngineAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-save"},
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
            return getString(key);
        }
        catch( Throwable t )
        {
            System.out.println("Missing resource <" + key + "> in " + this.getClass());
        }
        return key;
    }
}