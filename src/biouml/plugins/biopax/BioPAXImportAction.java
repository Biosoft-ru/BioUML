package biouml.plugins.biopax;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import biouml.model.Module;
import biouml.plugins.biopax.imports.ImportBioPAXDialog;

public class BioPAXImportAction extends AbstractAction
{
    public static final String KEY = "Import BioPAX";
    public static final String DATA_COLLECTION = "dataCollection";
    public static final String DATABASE = "Database";

    public BioPAXImportAction()
    {
        super(KEY);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        Module module = (Module)getValue(DATABASE);
        ImportBioPAXDialog importDialog = new ImportBioPAXDialog(false, module);
        importDialog.doModal();
    }
}

