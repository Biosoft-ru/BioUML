package biouml.plugins.obo.imports;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

@SuppressWarnings ( "serial" )
public class ImportOboAction extends AbstractAction
{
        public static final String KEY = "Import OBO";

        public ImportOboAction()
        {
            super(KEY);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            ImportOboDialog moduleExportDialog = new ImportOboDialog();
            moduleExportDialog.doModal();
        }
}
