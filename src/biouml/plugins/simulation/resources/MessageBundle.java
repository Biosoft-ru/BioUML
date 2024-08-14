package biouml.plugins.simulation.resources;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ListResourceBundle;
import java.util.ResourceBundle;

import javax.swing.Action;

import biouml.plugins.simulation.SimulationEnginePane;
import biouml.plugins.simulation.document.OpenInteractiveSimulationAction;
import biouml.plugins.simulation.plot.OpenPlotAction;

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
        //--- Common preferences ---/
        {"PREFERENCES_ENGINE_OUT_DIR",      "outdir"},
        {"PREFERENCES_ENGINE_OUT_DIR_PN",   "Output directory"},
        {"PREFERENCES_ENGINE_OUT_DIR_PD",   "Output directory where simulation engine will store generated code."},
        {"ERROR_CREATE_PREFERENCES",        "Could not create simulation engine preferences, error: {0}."},

        {"PN_ENGINE_NAME",      "Engine type"},
        {"PD_ENGINE_NAME",      "Engine type."},

        {"PN_ENGINE_OPTIONS",      "Engine options"},
        {"PD_ENGINE_OPTIONS",      "Engine options."},

        // Common simulation engine properties
        {"PN_OUTPUT_DIR",           "Output dir"},
        {"PD_OUTPUT_DIR",           "Ouptput directory where generted code will be stored."},

        {"PN_INITIAL_TIME",         "Initial time"},
        {"PD_INITIAL_TIME",         "Initial time."},

        {"PN_COMPLETION_TIME",      "Completion time"},
        {"PD_COMPLETION_TIME",      "Completion time."},

        {"PN_TIME_INCREMENT",      "Time increment"},
        {"PD_TIME_INCREMENT",      "Time increment."},

        {"PN_TIME_SCALE",      "Time scale"},
        {"PD_TIME_SCALE",      "Time scale."},

        {"PN_SIMULATOR_OPTIONS",      "Simulator options"},
        {"PD_SIMULATOR_OPTIONS",      "Simulator options."},

        {"PN_SIMULATOR_NAME",      "Simulator name"},
        {"PD_SIMULATOR_NAME",      "Simulator name."},

        {"PN_SIMULATION_ENGINE_NAME",      "Simulation engine name"},
        {"PD_SIMULATION_ENGINE_NAME",              "Simulation engine name."},

        //SimulationEnginePane
        { "SIMULATION_ENGINE",       "Simulation Engine:"},

        //--- Simulation engine errors and warnings ---------------------/
        {"WARN_MODEL_IS_STATIC",              "Model {0} is completely static, all parameters are constant."},

        {"ERROR_WRITE_MATH",                  "Error writing math expression: model {0}, expression={1}, error={2}"},

        {"ERROR_CODE_GENERATION",             "Can not generate code for model {0}, error: {1}."},
        {"ERROR_SIMULATION",                  "Some error have occured during simulation, model={0}, error: {1}."},

        {"ERROR_UNQULIFIED_VARIABLE",         "Can not qualify variable, model={0}, variable={1}."},
        {"ERROR_INVALID_CONST_VARIABLE",      "Undefined boundary variable, model={0}, variable={1}."},

        {"WARN_ALGEBRAIC_EQUATION",           "Algebraic equation will be ignored, model={0}, diagram element={1}, equation={2}"},
        {"ERROR_DIFFERENT_TYPE",              "Variable and equation has different types, model={0}, diagram element={1}, " +
                                              "variable name={2}, type={3}, equation type={4}, equation={5}."},
        {"WARNING_UNDEFINED_VARIABLE",        "Undefined variable, model={0}, variable={1}, type={2}."},
        {"ERROR_UNDEFINED_VARIABLE",        "Undefined variable, model={0}, variable={1}."},
        {"WARNING_TRYING_TO_CHANGE_CONSTANT", "Trying to change constant {1} with equation: \"{1} = {2}\", model={0}, " +
                                              "equation will be ignored."},
        {"WARNING_UNDEFINED_EQUATION_FORMULA","Model {0}: undefined equation formula, equation for {1}"},

        {"WARN_NO_VARIABLES_TO_PLOT",         "<html>There are no variables to plot. Are you sure you want to <br> simulate without plotting results ?</html>"  },
        {"WARN_WRONG_VARIABLES_TO_PLOT",      "<html>Some plot variables are missing or incorrect.<br>Please, change plot settings for {0} in the \"Plots\" tab."},
        {"WARN_WRONG_VARIABLES_TO_PLOT_TITLE","Error with plot variables"},

        {"ERROR_UNDEFINED_RULE_VARIABLE",     "Undefined rule variable, model={0}, variable={1}, equation={2}."},
        {"ERROR_UNDEFINED_PARAMETER",
        "Undefined parameter,  model={0}, parameter={1}."},
        {"ERROR_UNDEFINED_EQUATION_VARIABLE", "Undefined equation variable, model={0}, variable={1}, equation={2}."},

        {"ERROR_REORDERING_SCALAR_RULES",     "Model {0}: error occured while reordering scalar rules: error={1}."},

        {"ERROR_DUPLICATE_EQUATIONS", "Model {0}: duplicate equations for variable {1}."},

        {"WARN_VARIABLE_WITHOUT_TYPE", "Model {0}: Could not deduce type of the variable {1}."},

        {"CLICK_SIMULATION_RESULT", "(select simulation result)"},
        {"CLICK_EXPERIMENT", "(select experiment file)"},

        //--- Actions ---------------------------------------------------/
        { SimulationEnginePane.GENERATE_CODE_ACTION    + Action.SMALL_ICON           , "generate.gif"},
        { SimulationEnginePane.GENERATE_CODE_ACTION    + Action.NAME                 , "Generate code"},
        { SimulationEnginePane.GENERATE_CODE_ACTION    + Action.SHORT_DESCRIPTION    , "Generates code for the model simulation."},
        { SimulationEnginePane.GENERATE_CODE_ACTION    + Action.ACTION_COMMAND_KEY   , "cmd-generate-code"},

        { SimulationEnginePane.SIMULATE_ACTION         + Action.SMALL_ICON           , "simulate.gif"},
        { SimulationEnginePane.SIMULATE_ACTION         + Action.NAME                 , "Start simulation"},
        { SimulationEnginePane.SIMULATE_ACTION         + Action.SHORT_DESCRIPTION    , "Start simulation process"},
        { SimulationEnginePane.SIMULATE_ACTION         + Action.ACTION_COMMAND_KEY   , "cmd-simulate"},

        { SimulationEnginePane.STOP_SIMULATION_ACTION         + Action.SMALL_ICON           , "stop.gif"},
        { SimulationEnginePane.STOP_SIMULATION_ACTION         + Action.NAME                 , "Stop simulation"},
        { SimulationEnginePane.STOP_SIMULATION_ACTION         + Action.SHORT_DESCRIPTION    , "Stop simulation process"},
        { SimulationEnginePane.STOP_SIMULATION_ACTION         + Action.ACTION_COMMAND_KEY   , "cmd-simulate"},

        { SimulationEnginePane.SAVE_RESULT_ACTION      + Action.SMALL_ICON           , "saveResult.gif"},
        { SimulationEnginePane.SAVE_RESULT_ACTION      + Action.NAME                 , "Save result"},
        { SimulationEnginePane.SAVE_RESULT_ACTION      + Action.SHORT_DESCRIPTION    , "Saves simulation results into the database."},
        { SimulationEnginePane.SAVE_RESULT_ACTION      + Action.ACTION_COMMAND_KEY   , "cmd-save-result"},

        { SimulationEnginePane.PLOT_ACTION             + Action.SMALL_ICON           , "plot.gif"},
        { SimulationEnginePane.PLOT_ACTION             + Action.NAME                 , "Plot results"},
        { SimulationEnginePane.PLOT_ACTION             + Action.SHORT_DESCRIPTION    , "Opens plot dialog to visualise simulation results."},
        { SimulationEnginePane.PLOT_ACTION             + Action.ACTION_COMMAND_KEY   , "cmd-plot"},

        { SimulationEnginePane.CLEAR_LOG_ACTION             + Action.SMALL_ICON           , "clear.gif"},
        { SimulationEnginePane.CLEAR_LOG_ACTION             + Action.NAME                 , "Clear log"},
        { SimulationEnginePane.CLEAR_LOG_ACTION             + Action.SHORT_DESCRIPTION    , "Clear log."},
        { SimulationEnginePane.CLEAR_LOG_ACTION             + Action.ACTION_COMMAND_KEY   , "cmd-clear-log"},

        {OpenPlotAction.KEY + Action.SMALL_ICON, "plot.gif"},
        {OpenPlotAction.KEY + Action.NAME, "Open plot document"},
        {OpenPlotAction.KEY + Action.SHORT_DESCRIPTION, "Opens plot"},
        {OpenPlotAction.KEY + Action.LONG_DESCRIPTION, "Opens plot document"},
        {OpenPlotAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-open-nlss"},

        {OpenPlotAction.KEY2 + Action.SMALL_ICON, "plot.gif"},
        {OpenPlotAction.KEY2 + Action.NAME, "New plot document"},
        {OpenPlotAction.KEY2 + Action.SHORT_DESCRIPTION, "Creates new plot"},
        {OpenPlotAction.KEY2 + Action.LONG_DESCRIPTION, "Creates new plot document"},
        {OpenPlotAction.KEY2 + Action.ACTION_COMMAND_KEY, "cmd-open-nlss"},
        
        {OpenInteractiveSimulationAction.KEY + Action.SMALL_ICON, "plot.gif"},
        {OpenInteractiveSimulationAction.KEY + Action.NAME, "New simulation document"},
        {OpenInteractiveSimulationAction.KEY + Action.SHORT_DESCRIPTION, "Creates new simulation"},
        {OpenInteractiveSimulationAction.KEY + Action.LONG_DESCRIPTION, "Creates new simualation document"},
        {OpenInteractiveSimulationAction.KEY + Action.ACTION_COMMAND_KEY, "cmd-create-simulation"},


        // -- Save result issues ----------------------------------------/
        { "SAVE_RESULT_DIALOG_TITLE",        "Save simulation result"},
        { "SAVE_RESULT_CONFIRM_MESSAGE",     "You should save simulation result before plot editing"},
        { "ERROR_GET_DATABASE",              "Could not get database for diagram {1}."},
        { "ERROR_GET_RESULT_DC",             "Could not find data collection to save results in database {1}."},
        { "ERROR_SAVE_RESULT",               "Some error has occured during result saving, model={0}, error: {1}."},
        { "ERROR_CREATE_RESULT",             "Some error has occured during result creation, model={0}, error: {1}."},

        { "PLOT_DIALOG_TITLE",               "Plots"},
        { "PLOT_DESCRIPTION_TAB_TITLE",      "Plot parameters" },
        { "PLOT_PLOT_TAB_TITLE",             "Plot"},
        { "PLOT_TABLE_TAB_TITLE",            "Table"},
        { "PLOT_INSPECTOR_BORDER_TITLE",     "Plot properties" },
        { "PLOT_SERIES_BORDER_TITLE",        "Add series"},
        { "PLOT_SIMULATION_RESULT",          "Simulation Result"},
        { "PLOT_X_VARIABLE",                 "Variable X"},
        { "PLOT_Y_VARIABLE",                 "Variable Y"},
        { "PLOT_EXPERIMENTAL_DATA",          "Experimental Data"},
        { "PLOT_ADD_BUTTON",                 "Add"},
        { "PLOT_REMOVE_BUTTON",              "Remove"},
        { "PLOT_TAB_PROPERTIES_BORDER_TITLE","Series properties"},
        { "PLOT_SAVE_BUTTON",                "Save"},
        { "PLOT_PANE_SAVE_WARNING_TITLE",    "Warning"},
        { "PLOT_PANE_SAVE_WARNING_MESSAGE",  "The plot \"{0}\" already exists. Are you sure you want to rewrite it ?"},
        { "PLOT_PANE_PLOT_DC_ERROR_TITLE",   "Error"},
        { "PLOT_PANE_PLOT_DC_ERROR_MESSAGE", "Can not save plot: no appropriate data collection specified."},

        { "PLOT_PANE_PLOT_INCONSISTENT_ERROR_TITLE",   "Error"},
        { "PLOT_PANE_PLOT_INCONSISTENT_SR_ERROR_MESSAGE", "Series referes to inexistent simulation result \"{0}\"."},
        { "PLOT_PANE_PLOT_INCONSISTENT_EDF_ERROR_MESSAGE", "Series referes to inexistent experimental data file \"{0}\"."},

        // -- Plot issues
        { "SHOW_PLOT_ERROR_TITLE",            "Show plot error"},
        { "SHOW_PLOT_INVALID_DATA",           "Invalid plot data: {0}"},
        { "SHOW_PLOT_ERROR",                  "Could not show plot data {0}, error: {1}"},

        // extended plot issues
        { "CN_PLOT_EX",                 "Plot properties"},
        { "CD_PLOT_EX",                 ""},

        { "PN_PLOT_EX_DATABASE",          "Database name"},
        { "PD_PLOT_EX_DATABASE",          "Name of database comprising this plot."},

        { "PN_PLOT_EX_PLOT",            "Plot"},
        { "PD_PLOT_EX_PLOT",            "Plot properties."},

        { "PN_PLOT_EX_PLOT_NAME",       "Open plot"},
        { "PD_PLOT_EX_PLOT_NAME",       "Plot name."},


        { "PN_PLOT_EX_SAVE_NAME",       "Save as..."},
        { "PD_PLOT_EX_SAVE_NAME",       "The name of plot, under which it will be saved."},

        // -- Simulation analysis constants
        {"CN_SIMULATION_ANALYSIS", "Simulation analysis"},
        {"CD_SIMULATION_ANALYSIS", "Model simulation analysis"},

        {"PN_MODEL", "Model"},
        {"PD_MODEL", "Diagram with simulation model"},

        {"PN_SIMULATION_ENGINE", "Simulation engine"},
        {"PD_SIMULATION_ENGINE", "Simulation engine"},

        {"PN_SIMULATION_RESULT", "Simulation result"},
        {"PD_SIMULATION_RESULT", "Path to simulation result"},

        {"PN_SKIP_POINTS", "Skip points"},
        {"PD_SKIP_POINTS", "Number of points to skip"},

        {"PN_OUT_PATH", "Output directory"},
        {"PD_OUT_PATH", "Output directory"},

        {"PN_OUTPUT_START_TIME", "Output start time"},
        {"PD_IOUTPUT_START_TIME", "Output start time for non-uniform simulation"},
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
