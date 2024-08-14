package biouml.plugins.research.action;

import java.awt.Dimension;

import javax.swing.JDialog;
import javax.swing.JFrame;

import biouml.plugins.research.imports.ImportResearchPane;
import biouml.workbench.module.AbstractLoadPane;

import com.developmentontheedge.application.Application;
import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.application.dialog.OkCancelDialog;

public class ImportResearchDialog extends OkCancelDialog
{
    protected AbstractLoadPane content;
    protected static final MessageBundle resources = new MessageBundle();

    public ImportResearchDialog(JDialog dialog)
    {
        super(Application.getApplicationFrame(), resources.getResourceString("IMPORT_RESEARCH_DIALOG_TITLE"), null, "Close", null);
        init();
    }

    public ImportResearchDialog(JFrame frame)
    {
        super(Application.getApplicationFrame(), resources.getResourceString("IMPORT_RESEARCH_DIALOG_TITLE"), null, "Close", null);
        init();
        setSize(600, 700);
        setPreferredSize(new Dimension(600, 700));
        ApplicationUtils.moveToCenter(this);
    }

    protected void init()
    {
        content = new ImportResearchPane(true);

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
