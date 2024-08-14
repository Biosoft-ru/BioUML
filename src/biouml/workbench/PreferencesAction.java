package biouml.workbench;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import com.developmentontheedge.application.Application;

public class PreferencesAction extends AbstractAction
{
    public static final String KEY = "Preferences";

    public PreferencesAction()
    {
        super(KEY);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        PreferencesDialog preferencesDialog = new PreferencesDialog(Application.getApplicationFrame());
        preferencesDialog.show();
    }
}
