// $ Id: $
package biouml.plugins.obo.imports;

import java.util.ListResourceBundle;

import javax.swing.Action;

public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return contents;
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
    private static final Object[][] contents =
    {
        // Import BioPAX menu action
        { ImportOboAction.KEY      + Action.SMALL_ICON           , "importOBO.gif"},
        { ImportOboAction.KEY      + Action.NAME                 , "Import OBO"},
        { ImportOboAction.KEY      + Action.SHORT_DESCRIPTION    , "Import OBO"},
        { ImportOboAction.KEY      + Action.LONG_DESCRIPTION     , "Import OBO to a new database"},
        { ImportOboAction.KEY      + Action.ACTION_COMMAND_KEY   , "cmd-import-obo"},
    };
}
