package biouml.plugins.research.action;

import javax.swing.JFrame;

import com.developmentontheedge.application.dialog.OkCancelDialog;

public class NewProjectDialog extends OkCancelDialog
{
    private MessageBundle resources = new MessageBundle();

    protected NewProjectPane content;

    public NewProjectDialog(JFrame frame)
    {
        super(frame, "");
        init();
    }

    protected void init()
    {
        setTitle(resources.getResourceString("NEW_RESEARCH_DIALOG_TITLE"));

        content = new NewProjectPane(okButton);

        setContent(content);
        okButton.setEnabled(false);
    }

    @Override
    protected void okPressed()
    {
        content.createNewResearch();
        super.okPressed();
    }
}
