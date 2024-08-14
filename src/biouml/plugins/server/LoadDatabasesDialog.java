package biouml.plugins.server;

import java.awt.Dimension;

import javax.swing.JDialog;
import javax.swing.JFrame;

import biouml.workbench.BioUMLApplication;
import biouml.workbench.module.AbstractLoadPane;
import biouml.workbench.resources.MessageBundle;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.application.dialog.OkCancelDialog;

public class LoadDatabasesDialog extends OkCancelDialog
{
    protected AbstractLoadPane content;
    protected static final MessageBundle resources = BioUMLApplication.getMessageBundle();

    public LoadDatabasesDialog(JDialog dialog)
    {
        super(Application.getApplicationFrame(), resources.getResourceString("LOAD_DATABASE_DIALOG_TITLE"), null, "Close", null);
        init();
    }

    public LoadDatabasesDialog(JFrame frame)
    {
        super(Application.getApplicationFrame(), resources.getResourceString("LOAD_DATABASE_DIALOG_TITLE"), null, "Close", null);
        init();
        setSize(600, 700);
        setPreferredSize(new Dimension(600, 700));
        ApplicationUtils.moveToCenter(this);
    }

    protected void init()
    {
        content = new LoadDatabasesPane(true);

        setModal(false);
        setContent(content);
        okButton.setEnabled(false);
    }

    @Override
    protected void okPressed()
    {
        content.okPressed();
        super.okPressed();
    }
}
