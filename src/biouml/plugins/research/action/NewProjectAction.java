package biouml.plugins.research.action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import com.developmentontheedge.application.Application;

@SuppressWarnings ( "serial" )
public class NewProjectAction extends AbstractAction
{
    public static final String KEY = "New project";
    
    public NewProjectAction()
    {
        super(KEY);
    }
    
    @Override
    public void actionPerformed(ActionEvent e)
    {
        new NewProjectDialog(Application.getApplicationFrame()).doModal();
    }
}
