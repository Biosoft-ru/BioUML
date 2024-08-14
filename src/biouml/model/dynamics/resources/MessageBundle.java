package biouml.model.dynamics.resources;

import java.text.MessageFormat;
import java.util.ListResourceBundle;
import java.util.ResourceBundle;

import java.util.logging.Level;
import java.util.logging.Logger;

public class MessageBundle extends ListResourceBundle
{
    private final Logger cat = Logger.getLogger(MessageBundle.class.getName());

    protected static final MessageBundle resources = (MessageBundle)ResourceBundle.getBundle(MessageBundle.class.getName());
    public static void warn(Logger log, String messageBundleKey, String[] params)
    {
        String message = resources.getResourceString(messageBundleKey);
        message = MessageFormat.format(message, (Object[])params);
        log.warning(message);
    }
    public static void error(Logger log, String messageBundleKey, String[] params)
    {
        String message = resources.getResourceString(messageBundleKey);
        message = MessageFormat.format(message, (Object[])params);
        log.log(Level.SEVERE, message);
    }

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
            cat.log(Level.SEVERE, "Missing resource <" + key + "> in " + this.getClass());
        }
        return key;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Content
    //

    @Override
    protected Object[][] getContents()
    {
        return new Object[][] {
                // --- Common constants ---------------------------------------/
        
                {"PN_UNITS", "Units"},
                {"PD_UNITS", "Units."},
        
                {"PN_COMMENT", "Comment"},
                {"PD_COMMENT", "Comment."},
        
                //--- EModel --------------------------------------------------/
                {"CN_EMODEL", "Executable model"},
                {"CD_EMODEL", "Executable model (system of ordinary differential equations)."},
        
                {"PN_INITIAL_TIME", "Initial time"},
                {"PD_INITIAL_TIME", "Initial time."},
        
                {"PN_COMPLETION_TIME", "Completion time"},
                {"PD_COMPLETION_TIME", "Completion time."},
        
                //--- Parameter -----------------------------------------------/
                {"CN_PARAMETER", "Parameter"},
                {
                        "CD_PARAMETER",
                        "Parameter is used to declare a variable for use in mathematical formulas in a model definition. By default, parameters have constant value "
                                + "for the duration of a simulation."},
        
                {"PN_PARAMETER_NAME", "Name"},
                {"PD_PARAMETER_NAME", "Parameter name."},
        
                {"PN_PARAMETER_VALUE", "Value"},
                {"PD_PARAMETER_VALUE", "The parameter value."},
        
                //--- Variable ------------------------------------------------/
                {"CN_VARIABLE", "Variable"},
                {"CD_VARIABLE", "Variable."},
        
                {"PN_VARIABLE_NAME", "Name"},
                {"PD_VARIABLE_NAME", "Variable name."},
                
                {"PN_VARIABLE_TITLE", "Title"},
                {"PD_VARIABLE_TITLE", "Variable title."},
        
                {"PN_VARIABLE_COMMENT", "Comment"},
                {"PD_VARIABLE_COMMENT", "Comment."},
        
                //--- EModel messages -----------------------------------------/

                {"ERROR_MATH_PARSING",
                        "Model {0}: there were errors or warnings during math parsing\n" + "  component={1}, math={2}, errors: \n{3}"},

                {"ERROR_REMOVE_NOT_USED_PARAMS",
                        "Model {0}: some error have occured during unused parameters removing\n" + "  error: {1}"}};
    };
}
