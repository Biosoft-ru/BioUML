// $ Id: $
package biouml.plugins.biopax;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import com.developmentontheedge.application.Application;

import biouml.model.Module;
import biouml.workbench.BioUMLApplication;

public class BioPAXExportAction extends AbstractAction
{

    public static final String KEY = "Export BioPAX";
    public static final String DATA_COLLECTION = "dataCollection";
    public static final String DATABASE = "Database";

    public BioPAXExportAction()
    {
        super(KEY);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        String title = BioUMLApplication.getMessageBundle().getResourceString("DATABASE_EXPORT_DIALOG_TITLE");
        Module module = (Module)getValue(DATABASE);
        BioPAXModuleExportDialog moduleExportDialog = new BioPAXModuleExportDialog(Application.getApplicationFrame(), title, module);
        try
        {
            moduleExportDialog.doModal();
        }
        catch(Exception ex)
        {
            JOptionPane.showMessageDialog(Application.getApplicationFrame(), ex.toString(), "Unable to export BioPAX module", JOptionPane.ERROR_MESSAGE);
        }
    }
}
