package biouml.workbench.module;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import biouml.model.Module;
import biouml.workbench.BioUMLApplication;

import com.developmentontheedge.application.Application;

public class ExportModuleAction extends AbstractAction
{
    public static final String KEY = "Export module";
    public static final String DATABASE = "Database";

    public ExportModuleAction()
    {
        super(KEY);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        String title = BioUMLApplication.getMessageBundle().getResourceString("DATABASE_EXPORT_DIALOG_TITLE");
        ModuleExportDialog moduleExportDialog = new ModuleExportDialog(Application.getApplicationFrame(), title, (Module)getValue(DATABASE));
        moduleExportDialog.doModal();
        putValue(DATABASE, null);
    }
}
