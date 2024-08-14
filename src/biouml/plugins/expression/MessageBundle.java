package biouml.plugins.expression;

import ru.biosoft.util.ConstantResourceBundle;

/**
 * @author lan
 *
 */
public class MessageBundle extends ConstantResourceBundle
{
    public static final String CN_CLASS = "Add expression mapping";
    public static final String CD_CLASS = "Expression mapping parameters";
    public static final String PN_INPUT_DIAGRAM = "Input diagram";
    public static final String PD_INPUT_DIAGRAM = "Input diagram";
    public static final String PN_OUTPUT_DIAGRAM = "Output diagram";
    public static final String PD_OUTPUT_DIAGRAM = "Output diagram";
    public static final String PN_FILTER_PROPERTIES = "Mapping properties";
    public static final String PD_FILTER_PROPERTIES = "Mapping properties";

    // ExpressionFilterProperties parameters names
    public static final String PN_NAME = "Name";
    public static final String PD_NAME = "Name";
    public static final String PN_TABLE = "Data source";
    public static final String PD_TABLE = "Table and columns containing expression values";
    public static final String PN_TYPE = "Type";
    public static final String PD_TYPE = "Type";
    public static final String PN_AUTO_MIN_MAX = "Auto min/max";
    public static final String PD_AUTO_MIN_MAX = "Set minimum and maximum automatically to the most extreme value in the table";
    public static final String PN_MIN = "Minimum value";
    public static final String PD_MIN = "Lowest value in the selected table";
    public static final String PN_COLOR1 = "Start-color";
    public static final String PD_COLOR1 = "Color for minimum value";
    public static final String PN_MAX = "Maximum value";
    public static final String PD_MAX = "Highest value in the selected table";
    public static final String PN_COLOR2 = "End-color";
    public static final String PD_COLOR2 = "Color for maximum value";
    public static final String PN_USE_ZERO_COLOR = "Use color for zero";
    public static final String PD_USE_ZERO_COLOR = "If checked, zero-color will be used to represent zero value";
    public static final String PN_ZERO_COLOR = "Zero color";
    public static final String PD_ZERO_COLOR = "Color to represent zero value. Used only if minimum value < 0, maximum value > 0 and 'Use color for zero' is checked.";
}
