package biouml.workbench.module.xml;

import java.util.logging.Level;
import java.util.ListResourceBundle;

import java.util.logging.Logger;

public class MessageBundle extends ListResourceBundle
{
    private final Logger log = Logger.getLogger(MessageBundle.class.getName());

    @Override
    protected Object[][] getContents()
    {
        return new Object[][] {
                //--- conctants --------------------------------------------/
        
                //          --- DiagramReader errors --------------------------------------------/
                {"ERROR_ELEMENT_PROCESSING",            "Database {0}: can not read element <{2}> in <{1}>, error: {3}"},
                {"WARN_MULTIPLE_DECLARATION",           "Model {0}: multiple declaration of element {2} in {1}." +
                                                        "\nOnly first will be processed, other will be ignored."},
                {"ERROR_READ_INTERNAL_TYPE",            "DatabaseType {0}: can not read internal type declaration, error: {1}."},
                {"ERROR_READ_QUERY_SYSTEM",             "DatabaseType {0}: can not read query system declaration, error: {1}."},
                {"ERROR_READ_INDEX",                    "DatabaseType {0}: can not read index declaration, error: {1}."},
                {"ERROR_READ_EXTERNAL_DATABASE",        "DatabaseType {0}: can not read external database declaration, error: {1}."},
                {"ERROR_READ_EXTERNAL_TYPE",            "DatabaseType {0}: can not read external type declaration, error: {1}."},
                {"ERROR_READ_GRAPHIC_NOTATION",         "DatabaseType {0}: can not read graphic notation declaration, error: {1}."},
                
                {"REQUIRED_ATTR_MISSING",               "DatabaseType {0}: required attribute {1} for {2} element is missing."},
                
                {"COLLECTIONDESCRIPTION_TYPE_STATES",   cdTypeStates},
                {"EDIT_DIALOG_TITLE",                   "Xml database type editor"},
        };
    }
    
    String[] cdTypeStates = {"Java", "XML"};

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
}
