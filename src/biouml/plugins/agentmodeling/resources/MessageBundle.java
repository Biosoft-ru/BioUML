package biouml.plugins.agentmodeling.resources;

import java.util.ListResourceBundle;
import javax.swing.Action;
import biouml.plugins.agentmodeling.simulation.ModularSimulationViewPart;

public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return contents;
    }

    private static Object[][] contents =
    {
        { ModularSimulationViewPart.ADD_ACTION    + Action.SMALL_ICON           , "add.gif"},
        { ModularSimulationViewPart.ADD_ACTION    + Action.NAME                 , "Add group"},
        { ModularSimulationViewPart.ADD_ACTION    + Action.SHORT_DESCRIPTION    , "Add module group."},
        { ModularSimulationViewPart.ADD_ACTION    + Action.ACTION_COMMAND_KEY   , "cmd-add-group"},
        
        { ModularSimulationViewPart.REMOVE_ACTION    + Action.SMALL_ICON           , "remove.gif"},
        { ModularSimulationViewPart.REMOVE_ACTION    + Action.NAME                 , "Remove group"},
        { ModularSimulationViewPart.REMOVE_ACTION    + Action.SHORT_DESCRIPTION    , "Remove module group."},
        { ModularSimulationViewPart.REMOVE_ACTION    + Action.ACTION_COMMAND_KEY   , "cmd-remove-group"},
        
        { ModularSimulationViewPart.SIMULATE_ACTION    + Action.SMALL_ICON           , "simulate.gif"},
        { ModularSimulationViewPart.SIMULATE_ACTION    + Action.NAME                 , "Simulate"},
        { ModularSimulationViewPart.SIMULATE_ACTION    + Action.SHORT_DESCRIPTION    , "Simulate."},
        { ModularSimulationViewPart.SIMULATE_ACTION    + Action.ACTION_COMMAND_KEY   , "cmd-simulate"},
        
        { ModularSimulationViewPart.STOP_SIMULATE_ACTION    + Action.SMALL_ICON           , "stop.gif"},
        { ModularSimulationViewPart.STOP_SIMULATE_ACTION    + Action.NAME                 , "Stop simulation"},
        { ModularSimulationViewPart.STOP_SIMULATE_ACTION    + Action.SHORT_DESCRIPTION    , "Stop simulation."},
        { ModularSimulationViewPart.STOP_SIMULATE_ACTION    + Action.ACTION_COMMAND_KEY   , "cmd-stop"},
        
        { ModularSimulationViewPart.CLEAR_LOG_ACTION    + Action.SMALL_ICON           , "clear.gif"},
        { ModularSimulationViewPart.CLEAR_LOG_ACTION    + Action.NAME                 , "Clear log"},
        { ModularSimulationViewPart.CLEAR_LOG_ACTION    + Action.SHORT_DESCRIPTION    , "Clear simulation log."},
        { ModularSimulationViewPart.CLEAR_LOG_ACTION    + Action.ACTION_COMMAND_KEY   , "cmd-clear-log"},
    };
}