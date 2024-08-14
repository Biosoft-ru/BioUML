package biouml.workbench.module;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import com.developmentontheedge.application.Application;

public class NewModuleAction extends AbstractAction
{
    public static final String KEY = "New module";

    public NewModuleAction()
    {
        super(KEY);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        NewModuleDialog dialog = new NewModuleDialog(Application.getApplicationFrame());
        dialog.doModal();
    }
}

