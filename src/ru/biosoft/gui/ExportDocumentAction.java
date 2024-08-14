package ru.biosoft.gui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.application.dialog.OkCancelDialog;

public class ExportDocumentAction extends AbstractAction
{
    public static final String KEY = "Export document";

    public static final String DIALOG_CREATOR = "DialogCreator";

    public ExportDocumentAction()
    {
        super(KEY);
    }

    @Override
    public void actionPerformed(ActionEvent evt)
    {
        Document currentDocument = Document.getCurrentDocument();
        if( currentDocument == null )
        {
            ApplicationUtils.errorBox("No document is opened: export is not available");
            return;
        }
        try
        {
            OkCancelDialog dialog = currentDocument.getExportDialog();
            if( dialog != null )
                dialog.doModal();
            else
                ApplicationUtils.errorBox("Unable to export current document");
        }
        catch( Exception e )
        {
            JOptionPane.showMessageDialog(Application.getApplicationFrame(), e.toString(), "Unable to export", JOptionPane.ERROR_MESSAGE);
        }
    }
}
