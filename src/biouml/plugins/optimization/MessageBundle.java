package biouml.plugins.optimization;

import java.awt.Event;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.ListResourceBundle;
import java.util.ResourceBundle;

import javax.swing.Action;
import javax.swing.KeyStroke;

import java.util.logging.Logger;

import biouml.plugins.optimization.document.editors.OptimizationConstraintsViewPart;
import biouml.plugins.optimization.document.editors.DiagramParametersViewPart;
import biouml.plugins.optimization.document.editors.OptimizationExperimentViewPart;
import biouml.plugins.optimization.document.editors.OptimizationMethodViewPart;
import biouml.plugins.optimization.access.NewOptimizationAction;
import biouml.workbench.RemoveDataElementAction;


public class MessageBundle extends ListResourceBundle
{
    private Logger log = Logger.getLogger(MessageBundle.class.getName());
    
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
        catch( Throwable t )
        {
            log.log(Level.SEVERE, "Missing resource <" + key + "> in " + this.getClass());
        }
        return key;
    }
    
    protected static final MessageBundle resources = (MessageBundle)ResourceBundle.getBundle(MessageBundle.class.getName());
    public static String format(String messageBundleKey, Object[] params)
    {
        String message = getMessage(messageBundleKey);
        return  MessageFormat.format(message, params);
    }

    public static String getMessage(String messageBundleKey)
    {
        return resources.getResourceString(messageBundleKey);
    }
    
    @Override
    protected Object[][] getContents() { return contents; }
    private final static Object[][] contents =
    {
      //--- Actions properties -------------------------------------------------/
        { DiagramParametersViewPart.ADD                     + Action.SMALL_ICON            , "addParameter.gif"},
        { DiagramParametersViewPart.ADD                     + Action.NAME                  , "Add"},
        { DiagramParametersViewPart.ADD                     + Action.SHORT_DESCRIPTION     , "Add selected parameters to the fitting set."},
        { DiagramParametersViewPart.ADD                     + Action.ACTION_COMMAND_KEY    , "cmd-add"},

        { DiagramParametersViewPart.REMOVE                   + Action.SMALL_ICON           , "removeParameter.gif"},
        { DiagramParametersViewPart.REMOVE                   + Action.NAME                 , "Remove"},
        { DiagramParametersViewPart.REMOVE                   + Action.SHORT_DESCRIPTION    , "Remove selected parameters from the fitting set."},
        { DiagramParametersViewPart.REMOVE                   + Action.ACTION_COMMAND_KEY   , "cmd-remove"},
        
        { OptimizationExperimentViewPart.ADD                 + Action.SMALL_ICON           , "addExperiment.gif"},
        { OptimizationExperimentViewPart.ADD                 + Action.NAME                 , "Add"},
        { OptimizationExperimentViewPart.ADD                 + Action.SHORT_DESCRIPTION    , "Add new experimental data for analysis."},
        { OptimizationExperimentViewPart.ADD                 + Action.ACTION_COMMAND_KEY   , "cmd-add"},

        { OptimizationExperimentViewPart.REMOVE              + Action.SMALL_ICON           , "removeExperiment.gif"},
        { OptimizationExperimentViewPart.REMOVE              + Action.NAME                 , "Remove"},
        { OptimizationExperimentViewPart.REMOVE              + Action.SHORT_DESCRIPTION    , "Remove selected experimental data from analysis."},
        { OptimizationExperimentViewPart.REMOVE              + Action.ACTION_COMMAND_KEY   , "cmd-remove"},
        
        { OptimizationConstraintsViewPart.ADD                 + Action.SMALL_ICON           , "addConstraint.gif"},
        { OptimizationConstraintsViewPart.ADD                 + Action.NAME                 , "Add"},
        { OptimizationConstraintsViewPart.ADD                 + Action.SHORT_DESCRIPTION    , "Add new constraint for analysis."},
        { OptimizationConstraintsViewPart.ADD                 + Action.ACTION_COMMAND_KEY   , "cmd-add"},

        { OptimizationConstraintsViewPart.REMOVE              + Action.SMALL_ICON           , "removeConstraint.gif"},
        { OptimizationConstraintsViewPart.REMOVE              + Action.NAME                 , "Remove"},
        { OptimizationConstraintsViewPart.REMOVE              + Action.SHORT_DESCRIPTION    , "Remove selected constraint from analysis."},
        { OptimizationConstraintsViewPart.REMOVE              + Action.ACTION_COMMAND_KEY   , "cmd-remove"},
        
        { OptimizationMethodViewPart.RUN                     + Action.SMALL_ICON           , "run.gif"},
        { OptimizationMethodViewPart.RUN                     + Action.NAME                 , "Start optimization "},
        { OptimizationMethodViewPart.RUN                     + Action.SHORT_DESCRIPTION    , "Start optimization process."},
        { OptimizationMethodViewPart.RUN                     + Action.ACTION_COMMAND_KEY   , "cmd-run"},
       
        { OptimizationMethodViewPart.STOP                    + Action.SMALL_ICON           , "stop.gif"},
        { OptimizationMethodViewPart.STOP                    + Action.NAME                 , "Stop optimization "},
        { OptimizationMethodViewPart.STOP                    + Action.SHORT_DESCRIPTION    , "Stop optimization process."},
        { OptimizationMethodViewPart.STOP                    + Action.ACTION_COMMAND_KEY   , "cmd-run"},

        { OptimizationMethodViewPart.PLOT                    + Action.SMALL_ICON           , "plot.gif"},
        { OptimizationMethodViewPart.PLOT                    + Action.NAME                 , "Plot results"},
        { OptimizationMethodViewPart.PLOT                    + Action.SHORT_DESCRIPTION    , "Opens plot dialog to visualise optimization results."},
        { OptimizationMethodViewPart.PLOT                    + Action.ACTION_COMMAND_KEY   , "cmd-plot"},
        
        { OptimizationMethodViewPart.SAVE_INTERMEDIATE_RESULTS+ Action.SMALL_ICON           , "save.gif"},
        { OptimizationMethodViewPart.SAVE_INTERMEDIATE_RESULTS+ Action.NAME                 , "Save the intermediate results"},
        { OptimizationMethodViewPart.SAVE_INTERMEDIATE_RESULTS+ Action.SHORT_DESCRIPTION    , "Save the intermediate results"},
        { OptimizationMethodViewPart.SAVE_INTERMEDIATE_RESULTS+ Action.ACTION_COMMAND_KEY   , "cmd-gnrt-dgr"},

        { OptimizationMethodViewPart.OPEN_DIAGRAM            + Action.SMALL_ICON           , "generateDiagram.gif"},
        { OptimizationMethodViewPart.OPEN_DIAGRAM            + Action.NAME                 , "Open diagram"},
        { OptimizationMethodViewPart.OPEN_DIAGRAM            + Action.SHORT_DESCRIPTION    , "Open optimization diagram."},
        { OptimizationMethodViewPart.OPEN_DIAGRAM            + Action.ACTION_COMMAND_KEY   , "cmd-gnrt-dgr"},
        
        { NewOptimizationAction.KEY                          + Action.SMALL_ICON           , "optimization.gif"},
        { NewOptimizationAction.KEY                          + Action.NAME                 , "New optimization"},
        { NewOptimizationAction.KEY                          + Action.SHORT_DESCRIPTION    , "Creates a new optimization"},
        { NewOptimizationAction.KEY                          + Action.LONG_DESCRIPTION     , "Creates a new optimization for the selected data collection"},
        { NewOptimizationAction.KEY                          + Action.ACTION_COMMAND_KEY   , "cmd-new-opt"},
        
        { RemoveDataElementAction.KEY                        + Action.SMALL_ICON           , "remove.gif"},
        { RemoveDataElementAction.KEY                        + Action.NAME                 , "Remove"},
        { RemoveDataElementAction.KEY                        + Action.SHORT_DESCRIPTION    , "Removes specified data element"},
        { RemoveDataElementAction.KEY                        + Action.LONG_DESCRIPTION     , "Remove"},
        { RemoveDataElementAction.KEY                        + Action.MNEMONIC_KEY         , KeyEvent.VK_DELETE},
        { RemoveDataElementAction.KEY                        + Action.ACCELERATOR_KEY      , KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, Event.CTRL_MASK)},
        { RemoveDataElementAction.KEY                        + Action.ACTION_COMMAND_KEY   , "cmd-rm-de"},
        
     //--- Errors, warns and info -----------------------------------------------------/
        { "ERROR_ELEMENT_PROCESSING",           "Database {0}: cannot read the element <{2}> of <{1}>, error: {3}"},
        
        { "WARN_MULTIPLE_DECLARATION",          "Model {0}: the multiple declaration of the element {2} of {1}." +
                                                "\nOnly the first will be processed, but others will be ignored."},
                                                
        { "ERROR_PARSING_PROPERTY",             "Error occurred while parsing the property \"{0}\""},
        
        { "WARN_INCORRECT_DIAGRAM",             "The diagram {0} is incorrect. Please choose another diagram."},
        { "ERROR_OPTIMIZATION_CREATION",        "Error of the optimization creation"},
        { "ERROR_OPTIMIZATION_EXP_CREATION",    "Error of the optimization experiment creation."},
        
        { "WARN_EXP_EXISTENCE",                 "Experiment with the name {0} already exists. Do you realy want to rewrite it?"},
        
        { "ERROR_OPT_METHOD_INITIALIZATION",    "Cannot initialize the optimization method."},
        
        { "ERROR_OPTIMIZATION_WRITING",         "The null optimization cannot be written."},
        
        { "WARN_NEW_ITEM_ADDING_TO_FITTING_SET","The fitting set already contains the item {0}."},
        
        { "WARN_PARAMETER_SELECTION",           "Please select parameters to remove."},
        { "WARN_CONSTRAINT_SELECTION",          "Please select constraints to remove."},
        
        { "INFO_OPTIMIZATION_TERMINATION",      "The optimization analysis was terminated."},
        
        { "ERROR_OPTIMIZATION_SIMULATION",      "Cannot simulate."},
        { "ERROR_OPTIMIZATION_PLOT",            "Cannot plot."},
        
        { "WARN_OPTIMIZATION_EXECUTION_0",      "Please specify a collection to save results of the optimization."},
        { "WARN_OPTIMIZATION_EXECUTION_1",      "The optimization cannot be executed. The diagram is null."},
        { "WARN_OPTIMIZATION_EXECUTION_2",      "The optimization cannot be executed. The diagram {0} cannot be simulated."},
        { "WARN_OPTIMIZATION_EXECUTION_3",      "Please set parameters to fit."},
        { "WARN_OPTIMIZATION_EXECUTION_4",      "Please set experimental data."},
        { "WARN_OPTIMIZATION_EXECUTION_5",      "Wrong bounds for the fitting parameter '{0}' in the line {1}"},
        { "WARN_OPTIMIZATION_EXECUTION_6",      "Please init a time column for the time course experiment {0}."},
        { "WARN_OPTIMIZATION_EXECUTION_7",      "Experimental data {0} are null."},
        { "WARN_OPTIMIZATION_EXECUTION_8",      "Wrong format for steady state data of the experiment {0}. " +
                                                "The data must contain only one row with steady state values of the fitted variables. "},
        
        { "ERROR_OPTIMIZATION_SAVING",          "Error of the optimization saving."},
        { "INFO_PROPERTY_CHANGING",             "The property {0} was changed."},
        
        { "ERROR_VARIABLE_GETTING",             "There were some errors during the variable {0} processing."},
        { "ERROR_WRONG_CONSTRAINT",             "The wrong constraint in the line {0}."},
        { "ERROR_WRONG_CONSTRAINT_UNKNOWN_VAR", "The wrong constraint in the line {0}. The variable {1} is unknown."},
        { "ERROR_WRONG_CONSTRAINT_UNKNOWN_FUNC","The Illegal function {0}. Change the constraint in the line {1}."},
        { "WRONG_TYPE_OF_CONSTANT",             "Wrong type of the constant {0}. Change the constraint in the line {1}."},
        
        { "CHANGE_OPTIMIZATION_DIAGRAM",        "The optimization diagram does not coincide with the optimization document. " +
                                                "Do you want to rewrite it or the new diagram will be created?."},
        
        { "ERROR_EXPERIMENT_FILE_IS_INCORRECT", "The experimental data file is incorrect. Please select another file."},
        { "ERROR_EXPERIMENT_TABLE_GETTING",     "Cannot process the experiment table."},
        { "ERROR_EXPERIMENT_TABLE_GETTING",     "Cannot init weights."},
     
     // --- Titles -------------------------------------------------------------------/
        {"SAVE_RESULT_DIALOG_TITLE",            "Save optimization result"},
        
     // --- Properties ---------------------------------------------------------------/  
        { "CN_OPT_DIAGRAM",                     "Optimization diagram"},
        { "CD_OPT_DIAGRAM",                     "Optimization analysis diagram"},
        {"CN_CLASS", "Optimization"},
        {"CD_CLASS", "Optimization"},
    };
}
