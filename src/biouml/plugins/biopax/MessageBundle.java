// $ Id: $
package biouml.plugins.biopax;

import java.util.ListResourceBundle;

import javax.swing.Action;

public class MessageBundle extends ListResourceBundle
{
    @Override
    protected Object[][] getContents()
    {
        return new Object[][] {
            // BioPAX export action
            { BioPAXExportAction.KEY    + Action.NAME                 , "Export to BioPAX"},
            { BioPAXExportAction.KEY    + Action.SHORT_DESCRIPTION    , "<html>Export selected database to BioPAX OWL file"},
            { BioPAXExportAction.KEY    + Action.LONG_DESCRIPTION     , ""},
            
            // BioPAX import action
            { BioPAXImportAction.KEY    + Action.NAME                 , "Import BioPAX"},
            { BioPAXImportAction.KEY    + Action.SHORT_DESCRIPTION    , "<html>Import BioPAX file to current database"},
            { BioPAXImportAction.KEY    + Action.LONG_DESCRIPTION     , ""},
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
