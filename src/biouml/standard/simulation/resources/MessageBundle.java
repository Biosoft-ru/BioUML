package biouml.standard.simulation.resources;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ListResourceBundle;
import java.util.ResourceBundle;

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
        // Common simulation engine properties
        {"PN_SIMULATOR_NAME",       "Simulator"},
        {"PD_SIMULATOR_NAME",       "Simulator name."},

        {"PN_INITIAL_TIME",         "Initial time"},
        {"PD_INITIAL_TIME",         "Initial time."},

        {"PN_COMPLETION_TIME",      "Completion time"},
        {"PD_COMPLETION_TIME",      "Completion time."},

        {"PN_DIAGRAM_NAME",   "Diagram name"},
        {"PD_DIAGRAM_NAME",   "Name of related diagram."},

        {"PN_TITLE",   "Title"},
        {"PD_TITLE",   "Simulation result title."},

        //--- SimulationResult properties -----------------------------
        {"CN_SIMULATION_RESULT",    "Simulation result"},
        {"CD_SIMULATION_RESULT",    "Simulation result stores information about model initial values and " +
                                    "obtained variable values during simulation. This result can be stored in the database."},

        {"PN_SIMULATION_RESULT_NAME",          "Name"},
        {"PD_SIMULATION_RESULT_NAME",          "Name of simulation result how it will be stored in the database."},

        {"PN_SIMULATION_RESULT_DESCRIPTION",   "Description"},
        {"PD_SIMULATION_RESULT_DESCRIPTION",   "Arbitrary description of simulation result and initial parameters."},

        // -- Save result issues ----------------------------------------/
        { "SAVE_RESULT_DIALOG_TITLE",        "Save simulation result"},
        { "ERROR_GET_DATABASE",              "Could not get database for diagram {1}."},
        { "ERROR_GET_RESULT_DC",             "Could not find data collection to save results in database {1}."},
        { "ERROR_SAVE_RESULT",               "Some error has occured during result saving, model={0}, error: {1}."},
        { "ERROR_CREATE_RESULT",             "Some error has occured during result creation, model={0}, error: {1}."},

        { "PLOT_DIALOG_TITLE",               "Plots"},
        { "PLOT_DESCRIPTION_TAB_TITLE",      "Plot parameters" },
        { "PLOT_PLOT_TAB_TITLE",             "Plot"},
        { "PLOT_INSPECTOR_BORDER_TITLE",     "Plot properties" },
        { "PLOT_SERIES_BORDER_TITLE",        "Add series"},
        { "PLOT_SIMULATION_RESULT",          "Simulation Result"},
        { "PLOT_X_VARIABLE",                 "Variable X"},
        { "PLOT_Y_VARIABLE",                 "Variable Y"},
        { "PLOT_ADD_BUTTON",                 "Add"},
        { "PLOT_REMOVE_BUTTON",              "Remove"},
        { "PLOT_TAB_PROPERTIES_BORDER_TITLE","Series properties"},
        { "PLOT_SAVE_BUTTON",                "Save"},
        { "PLOT_PANE_SAVE_WARNING_TITLE",    "Warning"},
        { "PLOT_PANE_SAVE_WARNING_MESSAGE",  "The plot \"{0}\" already exists. Are you sure you want to rewrite it ?"},
        { "PLOT_PANE_PLOT_DC_ERROR_TITLE",   "Error"},
        { "PLOT_PANE_PLOT_DC_ERROR_MESSAGE", "Can not save plot: no appropriate data collection specified."},

        { "PLOT_PANE_PLOT_INCONSISTENT_ERROR_TITLE",   "Error"},
        { "PLOT_PANE_PLOT_INCONSISTENT_ERROR_MESSAGE", "Series referes to inexistent simulation result \"{0}\"."},


        // -- Plot issues
        { "SHOW_PLOT_ERROR_TITLE",            "Show plot error"},
        { "SHOW_PLOT_INVALID_DATA",           "Invalid plot data: {0}"},
        { "SHOW_PLOT_ERROR",                  "Could not show plot data {0}, error: {1}"},

        { "CN_PLOT",                    "Plot"},
        { "CD_PLOT",                    "Plot component."},

        { "PN_PLOT_NAME",               "Name"},
        { "PD_PLOT_NAME",               "Name of the current plot."},

        { "PN_PLOT_TITLE",              "Title"},
        { "PD_PLOT_TITLE",              "Title of the plot, how it will be named in the picture."},

        { "PN_PLOT_DESCRIPTION",        "Description"},
        { "PD_PLOT_DESCRIPTION",        "Description of the current plot."},

        { "PN_PLOT_X_TITLE",            "X axis title"},
        { "PD_PLOT_X_TITLE",            "Title of X axis of the plot."},

        { "PN_PLOT_X_AXIS_TYPE",        "X axis type"},
        { "PD_PLOT_X_AXIS_TYPE",        "Type of X axis of the plot."},

        { "PN_PLOT_X_AUTO_RANGE",       "X axis auto range"},
        { "PD_PLOT_X_AUTO_RANGE",       "X axis auto range"},

        { "PN_PLOT_X_FROM",             "   X: from"},
        { "PD_PLOT_X_FROM",             "Smallest value of X coordinate."},

        { "PN_PLOT_X_TO",               "   X: to "},
        { "PD_PLOT_X_TO",               "Largest value of X coordinate."},

        { "PN_PLOT_Y_TITLE",            "Y axis title"},
        { "PD_PLOT_Y_TITLE",            "Title of Y axis of the plot."},

        { "PN_PLOT_Y_AXIS_TYPE",        "Y axis type"},
        { "PD_PLOT_Y_AXIS_TYPE",        "Type of Y axis of the plot."},

        { "PN_PLOT_Y_AUTO_RANGE",       "Y axis auto range"},
        { "PD_PLOT_Y_AUTO_RANGE",       "Y axis auto range"},

        { "PN_PLOT_Y_FROM",             "   Y: from "},
        { "PD_PLOT_Y_FROM",             "Smallest value of Y coordinate."},

        { "PN_PLOT_Y_TO",               "   Y: to "},
        { "PD_PLOT_Y_TO",               "Largest value of Y coordinate."},

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

        // Plot series
        { "PN_PLOT_SERIES_X_VAR",           "X Variable" },
        { "PD_PLOT_SERIES_X_VAR",           "Variable to be mapped to X axis " },

        { "PN_PLOT_SERIES_Y_VAR",           "Y Variable" },
        { "PD_PLOT_SERIES_Y_VAR",           "Variable that will be mapped to Y axis " },

        { "PN_PLOT_SERIES_NAME",            "Name" },
        { "PD_PLOT_SERIES_NAME",            "Series name" },

        { "PN_PLOT_SERIES_LEGEND",          "Legend" },
        { "PD_PLOT_SERIES_LEGEND",          "Description of the plot" },

        { "PN_PLOT_SERIES_SPEC",            "Specification" },
        { "PD_PLOT_SERIES_SPEC",            "Specification of plot type" },

        { "PN_PLOT_SERIES_SOURCE",          "Source" },
        { "PD_PLOT_SERIES_SOURCE",          "Name of source, comprising required data" }
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
