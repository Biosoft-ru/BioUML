package biouml.plugins.microarray;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ListResourceBundle;
import java.util.ResourceBundle;

import javax.swing.Action;
import biouml.plugins.microarray.editors.FilterViewPane;

public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return contents;
    }

    private static Object[][] contents =
    {
        //--- Actions ---------------------------------------------------/
        { MicroarrayPane.APPLY    + Action.SMALL_ICON           , "apply.gif"},
        { MicroarrayPane.APPLY    + Action.NAME                 , "Refresh"},
        { MicroarrayPane.APPLY    + Action.SHORT_DESCRIPTION    , "Apply microarray to current diagram."},
        { MicroarrayPane.APPLY    + Action.ACTION_COMMAND_KEY   , "cmd-apply"},

        { MicroarrayPane.REMOVE_ACTION    + Action.SMALL_ICON           , "remove.gif"},
        { MicroarrayPane.REMOVE_ACTION    + Action.NAME                 , "Remove action"},
        { MicroarrayPane.REMOVE_ACTION    + Action.SHORT_DESCRIPTION    , "Remove action from filter."},
        { MicroarrayPane.REMOVE_ACTION    + Action.ACTION_COMMAND_KEY   , "cmd-remove-action"},

        { MicroarrayPane.NEW_FILTER    + Action.SMALL_ICON           , "new.gif"},
        { MicroarrayPane.NEW_FILTER    + Action.NAME                 , "New filter"},
        { MicroarrayPane.NEW_FILTER    + Action.SHORT_DESCRIPTION    , "Create new filter."},
        { MicroarrayPane.NEW_FILTER    + Action.ACTION_COMMAND_KEY   , "cmd-new-filter"},

        { MicroarrayPane.SAVE_FILTER    + Action.SMALL_ICON           , "save.gif"},
        { MicroarrayPane.SAVE_FILTER    + Action.NAME                 , "Save filter"},
        { MicroarrayPane.SAVE_FILTER    + Action.SHORT_DESCRIPTION    , "Save filter."},
        { MicroarrayPane.SAVE_FILTER    + Action.ACTION_COMMAND_KEY   , "cmd-save-filter"},

        { MicroarrayPane.REMOVE_FILTER    + Action.SMALL_ICON           , "remove.gif"},
        { MicroarrayPane.REMOVE_FILTER    + Action.NAME                 , "Remove filter"},
        { MicroarrayPane.REMOVE_FILTER    + Action.SHORT_DESCRIPTION    , "Remove filter from filter list."},
        { MicroarrayPane.REMOVE_FILTER    + Action.ACTION_COMMAND_KEY   , "cmd-remove-filter"},

        { FilterViewPane.APPLY_FILTER_ACTION + Action.SMALL_ICON        , "refresh.gif"},
        { FilterViewPane.APPLY_FILTER_ACTION + Action.NAME              , "Apply Filters"},
        { FilterViewPane.APPLY_FILTER_ACTION + Action.SHORT_DESCRIPTION , "Apply Filters"},
        { FilterViewPane.APPLY_FILTER_ACTION + Action.ACTION_COMMAND_KEY, "cmd-apply-filters"},

        { FilterViewPane.REMOVE_FILTER_ACTION + Action.SMALL_ICON        , "remove.gif"},
        { FilterViewPane.REMOVE_FILTER_ACTION + Action.NAME              , "Remove Filters"},
        { FilterViewPane.REMOVE_FILTER_ACTION + Action.SHORT_DESCRIPTION , "Remove Filters"},
        { FilterViewPane.REMOVE_FILTER_ACTION + Action.ACTION_COMMAND_KEY, "cmd-apply-filters"},

        { FilterViewPane.EXPORT_ACTION + Action.SMALL_ICON        , "export.gif"},
        { FilterViewPane.EXPORT_ACTION + Action.NAME              , "Export filtered table"},
        { FilterViewPane.EXPORT_ACTION + Action.SHORT_DESCRIPTION , "Export filtered table"},
        { FilterViewPane.EXPORT_ACTION + Action.ACTION_COMMAND_KEY, "cmd-export-filtered"},

        //--- Extensions ------------------------------------------------/
        { "ATT_MAT_FILE_EXT" , ".pvals .sif .exp" },
        { "ATT_MAT_FILE_EXT_TITLE" , "Experiments files" },
        { "MICROARRAY_FILE_EXT" , ".fac" },

        //--- Data Collection Name --------------------------------------/
        { "MICROARRAY_DC_NAME" ,         "microarray" },

        //--- Microarray pane properties
        {"MICROARRAY_LIST_LABEL", "Experiment:"},
        {"BINDER_LIST_LABEL", "Gene hub:"},

        //--- Microarray view dialog properties
        {"MICROARRAY_EDIT_DIALOG_TITLE", "Microarray Editor"},

        //--- Bean info parameters
        {"CN_MICROARRAY", "Microarray"},
        {"CD_MICROARRAY", "Microarray experiment"},
        {"PN_MA_PLATFORM", "Platform"},
        {"PD_MA_PLATFORM", "Platform"},
        {"PN_MA_CINFO", "Columns"},
        {"PD_MA_CINFO", "Columns info"},
        {"PN_MA_VALUES", "Values"},
        {"PD_MA_VALUES", "Values"},
        {"PN_MA_SPECIES", "Species"},
        {"PD_MA_SPECIES", "Species"},

        //--- MicroarrayCollection constants
        { "CN_MICROARRAY_DC",               "Microarray collection"},
        { "CD_MICROARRAY_DC",               "Microarray collection."},

        //Filter view pane
        {"FILTER_COLUMNS", "Column names (double-click to select)"},

        //--- Filter error dialog properties
        {"FILTER_ERROR_TITLE", "Filtering error"},
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
