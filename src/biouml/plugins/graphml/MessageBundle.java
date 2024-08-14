package biouml.plugins.graphml;

import java.util.logging.Level;
import java.util.ListResourceBundle;

import java.util.logging.Logger;

/**
 * Messages GraphML exporter
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

    @Override
    protected Object[][] getContents()
    {
        return new Object[][] {
            //--- Export dialog constants-----------------------------------------------/
            {"CN_EXPORT_PROPERTIES",           "Export properties"},
            {"CD_EXPORT_PROPERTIES",           "Export properties"},
        
            {"PN_USE_YFILES",      "yEd GraphML graphics compartibility"},
            {"PD_USE_YFILES",      "Use schema definitions for yEd GraphML graphics compartibility"},
            
        };
    }
}
