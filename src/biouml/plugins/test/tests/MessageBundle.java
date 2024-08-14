package biouml.plugins.test.tests;

import java.util.ListResourceBundle;

public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return contents;
    }
    private final static Object[][] contents = {

            {"CN_VARIABLE", "Variable"},
            {"CD_VARIABLE", "Variable"},

            {"PN_VARIABLE_NAME", "Name"},
            {"PD_VARIABLE_NAME", "Variable name"},

            {"PN_SUBDIAGRAM_NAME", "Subdiagram"},
            {"PD_SUBDIAGRAM_NAME", "Subdiagram name"},


            // STEADY_STATE
            {"CN_STEADY_STATE", "Steady state test"},
            {"CD_STEADY_STATE", "Checks whether specified variable are in steady state, on the specified time interval."},
            {"PN_STEADY_STATE_FROM", "From"},
            {"PD_STEADY_STATE_FROM", "Beginning of time interval"},
            {"PN_STEADY_STATE_TO", "To"},
            {"PD_STEADY_STATE_TO", "End of time interval"},
            {"PN_STEADY_STATE_RTOL", "Relative tolerance"},
            {"PD_STEADY_STATE_RTOL", "Relative tolerance"},
            {"PN_STEADY_STATE_VARIABLES", "Variables"},
            {"PD_STEADY_STATE_VARIABLES", "List of steady state variables"},

            // INTERVAL
            {"CN_INTERVAL", "Interval test"},
            {"CD_INTERVAL", "Checks whether the specified variable is in specified interval during specified time from simulation result."},
            {"PN_INTERVAL_VALUE_FROM", "Min value"},
            {"PD_INTERVAL_VALUE_FROM", "Minimum interval value"},
            {"PN_INTERVAL_VALUE_TO", "Max value"},
            {"PD_INTERVAL_VALUE_TO", "Maximum interval value"},
            {"PN_INTERVAL_FROM", "From"},
            {"PD_INTERVAL_FROM", "Beginning of time interval"},
            {"PN_INTERVAL_TO", "To"},
            {"PD_INTERVAL_TO", "End of time interval"},
            {"PN_INTERVAL_VARIABLES", "Variables"},
            {"PD_INTERVAL_VARIABLES", "List of steady state variables"},

            // EXPERIMENT
            {"CN_EXPERIMENT", "Experiment value test"},
            {"CD_EXPERIMENT", "Checks whether the specified variable is equals to column in experiment table."},
            {"PN_EXPERIMENT_PATH", "Experiment"},
            {"PD_EXPERIMENT_PATH", "Path to experiment table"},
            {"PN_EXPERIMENT_EXP_VAR", "Column"},
            {"PD_EXPERIMENT_EXP_VAR", "Column name"},
            {"PN_EXPERIMENT_RESULT_VAR", "Variable"},
            {"PD_EXPERIMENT_RESULT_VAR", "Model variable name"},
            {"PN_EXPERIMENT_RELATIVE_TO", "Relative to"},
            {"PD_EXPERIMENT_RELATIVE_TO",
                    "The time point relative to which all experimental values will be recalculated. If the point is unspecified, the values are assumed to be exact."},
            {"PN_EXPERIMENT_WEIGHT_METHOD", "Weight method"}, {"PD_EXPERIMENT_WEIGHT_METHOD", "Weight method"},
            {"PN_EXPERIMENT_MAX_DEVIATION", "Max deviation"}, {"PD_EXPERIMENT_MAX_DEVIATION", "Max value for deviation"},};
}
