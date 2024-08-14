package biouml.plugins.server;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import com.developmentontheedge.application.Application;

@SuppressWarnings ( "serial" )
public class LoadDatabasesAction extends AbstractAction
{
    public static final String KEY = "Load database";

    public LoadDatabasesAction()
    {
        super(KEY);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        LoadDatabasesDialog dialog = new LoadDatabasesDialog(Application.getApplicationFrame());
        dialog.pack();
        dialog.setVisible(true);
    }
}