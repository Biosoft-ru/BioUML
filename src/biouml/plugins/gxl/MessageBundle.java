package biouml.plugins.gxl;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.ListResourceBundle;
import java.util.ResourceBundle;

import java.util.logging.Logger;

/**
 * Messages for GXL models.
 */
public class MessageBundle extends ListResourceBundle
{
    private final Logger log = Logger.getLogger(MessageBundle.class.getName());

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
        return new Object[][] {
            {"GXL_PARSE_ERROR",                 "Parse error in file {0}, error: {1}" },
            {"ERROR_GRAPH_PROCESSING",         "Error occured when processing graph {0}." },
            {"ERROR_NODE_PROCESSING",           "Error occured when processing node {0}." },
            {"ERROR_EDGE_PROCESSING",           "Error occured when processing graph {0}." },
            {"ERROR_NO_FROM_NODE",                 "Edge {0} has no \"from\" node" },
            {"ERROR_NO_TO_NODE",                 "Edge {0} has no \"to\" node" },
            {"ERROR_RELEND_NOT_SUPPORTED",         "\"Relend\" element is not supported."},
            {"ERROR_TYPE_NOT_SUPPORTED",         "\"Type\" element is not supported."},
            {"ERROR_ATTRIBUTE_NOT_SUPPORTED",     "\"Attribute\" element is not supported."},
            {"ERROR_RELATIONS_NOT_SUPPORTED",     "Relations are not supported for now." },
            {"ERROR_DIAGRAM_WRITING",             "Error occured when writing diagram \"{0}\" to file."},
            {"ERROR_DIAGRAM_ELEMENT_WRITING",     "Error occured when writing diagram element \"{0}\" to file."}
        };
    }
}
