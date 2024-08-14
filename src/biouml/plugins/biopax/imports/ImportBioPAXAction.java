package biouml.plugins.biopax.imports;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

public class ImportBioPAXAction extends AbstractAction
{
        public static final String KEY = "Import BioPAX";

        public ImportBioPAXAction()
        {
            super(KEY);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            ImportBioPAXDialog moduleExportDialog = new ImportBioPAXDialog();
            moduleExportDialog.doModal();
        }
}
