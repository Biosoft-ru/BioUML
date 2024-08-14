package biouml.plugins.sbml.composite;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.ListResourceBundle;
import java.util.ResourceBundle;

import java.util.logging.Logger;

/**
 * Messages for SBML models.
 */
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
        catch (Throwable t)
        {
            log.log(Level.SEVERE, "Missing resource <" + key + "> in " + this.getClass());
        }
        return key;
    }

    protected static final MessageBundle resources = (MessageBundle)ResourceBundle.getBundle(MessageBundle.class.getName());
    protected static void warn(Logger log, String messageBundleKey, Object[] params)
    {
        String message = resources.getResourceString(messageBundleKey);
        message = MessageFormat.format(message, params);
        log.warning(message);
    }
    protected static void error(Logger log, String messageBundleKey, Object[] params)
    {
        String message = resources.getResourceString(messageBundleKey);
        message = MessageFormat.format(message, params);
        log.log(Level.SEVERE, message);
    }

    ///////////////////////////////////////////////////////////////////
    // Contents
    //

    @Override
    protected Object[][] getContents()
    {
        return contents;
    }

    private Object[][] contents =
    {
        //--- SbmlReader error messages ----------------------------------------/
        {"ERROR_MODEL_DECLARATION_PROCESSING",                   "Model {0}: can not read model declaration {1}, error: {2}"},

        {"ERROR_SUB_MODEL_READING",          "Model {0}: can not read sub model {1}, error: {2}"},

        {"ERROR_MODEL_DEFINITION_WRITING",  "Model {0}: can not write model definition {1}, error: {2}"},
        {"ERROR_PORT_ELEMENT_WRITING",  "Model {0}: can not write port {1}, error: {2}"},

        {"ERROR_MODEL_DEFINITION_MISSING",  "Model {0}: can not find model definition {2}, for subdiagram {1}"},

        {"ERROR_MODEL_DEFINITION_READING",  "Model {0}: can not read model definition {1}, error: {2}"},
        {"ERROR_EXTERNAL_MODEL_DEFINITION_READING",  "Model {0}: can not read external model definition {1}, error: {2}"},

        //inherited from SbmlSupport
        {"WARN_MULTIPLE_DECLARATION",               "Model {0}: multiple declaration of element {2} in {1}." +
        "\nOnly first will be processed, other will be ignored."},

        {"ERROR_ELEMENT_PROCESSING",                "Model {0}: can not read element <{2}> in <{1}>, error: {3}"}

    };
}
