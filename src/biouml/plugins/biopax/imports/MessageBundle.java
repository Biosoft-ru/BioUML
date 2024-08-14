// $ Id: $
package biouml.plugins.biopax.imports;

import java.util.ListResourceBundle;

import javax.swing.Action;

import biouml.plugins.biopax.imports.ImportBioPAXAction;

public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return new Object[][] {
            // Import BioPAX menu action
            { ImportBioPAXAction.KEY      + Action.SMALL_ICON           , "importBioPAX.gif"},
            { ImportBioPAXAction.KEY      + Action.NAME                 , "Import BioPAX"},
            { ImportBioPAXAction.KEY      + Action.SHORT_DESCRIPTION    , "Import BioPAX"},
            { ImportBioPAXAction.KEY      + Action.LONG_DESCRIPTION     , "Import BioPAX to a new database"},
            { ImportBioPAXAction.KEY      + Action.ACTION_COMMAND_KEY   , "cmd-import-biopax"},
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
}
