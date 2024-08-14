package biouml.model;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ListResourceBundle;
import java.util.ResourceBundle;

public class MessageBundle extends ListResourceBundle
{
    protected String[] filterActions = {"hide", "highlight"};
    protected String[] shapeTypes    = {"rectangle", "round rectangle", "ellipse"};

    @Override
    protected Object[][] getContents()
    {
        return new Object[][] {
            //----- SemantiController constants ----------------------------------/
            {"ERROR_CAN_NOT_CLONE_NODE", "Can not clone the node."},
            {"ERROR_NODE_IS_DUPLICATED", "Compartment already contians such node."},


            //----- Module  constants ---------------------------------------/
            { "CN_DATABASE",          "Database"},
            { "CD_DATABASE",          "Database"},
            { "PN_DATABASE_NAME",     "Name"},
            { "PD_DATABASE_NAME",     "The name of data collection"},
            { "PN_DATABASE_VERSION",  "Version"},
            { "PD_DATABASE_VERSION",  "The database version"},
            { "PN_DATABASE_SIZE",     "Size"},
            { "PD_DATABASE_SIZE",     "The size of data collection"},

            {"PN_TITLE",            "Title"},
            {"PD_TITLE",            "The diagram element title.<br>" +
                                    "By default title is data entry name. " +
                                    "However a user can change the diagram element title."},

            {"PN_COMMENT",          "Comment"},
            {"PD_COMMENT",          "Arbitrary text comment."},

            {"PN_ATTRIBUTES", "Attributes"},
            {"PD_ATTRIBUTES", "Dynamic set of attributes. <br>" + "This attributes can be added:<br>" + "<ul>"
                 + "<li>during mapping of information from a database into Java objects"
                 + "<li>by plug-in for some specific usage" + "<li>by customer to store some specific information formally"
                 + "<li>during import of experimental data" + "</ul>"},

            {"FILTER_ACTIONS"          , filterActions},

            //Error messages
            { "ERROR_MATH_PARSING",                 "There were errors or warnings during math parsing\n" +
            "math={0}, errors: \n{1}"},
        };
    }

    public String getResourceString(String key)
    {
        try
        {
            return getString(key);
        }
        catch (Throwable th)
        {

        }
        return key;
    }

    private static final MessageBundle resources = (MessageBundle)ResourceBundle.getBundle(MessageBundle.class.getName());
    public static void error(Logger log, String messageBundleKey, Object[] params)
    {
        String message = resources.getResourceString(messageBundleKey);
        message = MessageFormat.format(message, params);
        log.log(Level.SEVERE, message);
    }
}
